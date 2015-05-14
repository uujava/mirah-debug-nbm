/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.weblite.netbeans.mirah.lexer;

import ca.weblite.asm.WLMirahCompiler;
import ca.weblite.netbeans.mirah.LOG;
import ca.weblite.netbeans.mirah.RecompileQueue;
import ca.weblite.netbeans.mirah.support.spi.MirahExtenderImplementation;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.WeakHashMap;
import java.util.jar.JarOutputStream;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
//import javax.lang.model.element.ElementKind;
import javax.swing.event.ChangeListener;
import javax.swing.text.Document;
import javax.tools.Diagnostic;
import mirah.lang.ast.ClassDefinition;
import mirah.lang.ast.FieldDeclaration;
import mirah.lang.ast.Import;
import mirah.lang.ast.InterfaceDeclaration;
import mirah.lang.ast.MacroDefinition;
import mirah.lang.ast.MethodDefinition;
import mirah.lang.ast.Node;
import mirah.lang.ast.NodeFilter;
import mirah.lang.ast.NodeScanner;
import mirah.lang.ast.Package;
import mirah.lang.ast.Script;
import mirah.lang.ast.StaticMethodDefinition;
import mirah.lang.ast.StringCodeSource;
import mirah.lang.ast.Super;
import org.codehaus.plexus.util.FileUtils;
import org.mirah.jvm.mirrors.debug.DebuggerInterface;
import org.mirah.tool.Mirahc;
import org.mirah.typer.ResolvedType;
import org.mirah.typer.TypeFuture;
import org.mirah.typer.TypeListener;
import org.mirah.util.Context;
import org.mirah.util.SimpleDiagnostics;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.netbeans.modules.csl.api.ElementKind;
import org.netbeans.modules.csl.api.Error;
import org.netbeans.modules.csl.api.Severity;
import org.netbeans.modules.csl.spi.ParserResult;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.api.Task;
import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.modules.parsing.spi.Parser;
import org.netbeans.modules.parsing.spi.SourceModificationEvent;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;

/**
 *
 * @author shannah
 */
public class MirahParser extends Parser {

    private static WeakHashMap<Document, DocumentDebugger> documentDebuggers
            = new WeakHashMap<Document, DocumentDebugger>();
    private static WeakHashMap<Document, String> lastContent
            = new WeakHashMap<Document, String>();
    private Snapshot snapshot;
    private MirahParseDiagnostics diag;
    private NBMirahParserResult result;
    private static int count = 0;

    public static DocumentDebugger getDocumentDebugger(Document doc) {
//        synchronized( documentDebuggers )
        {
            //LOG.info(MirahParser.class,"getDocumentDebugger doc="+doc);
            //LOG.putStack(null);
            return documentDebuggers.get(doc);
        }
    }

    private static WeakHashMap<Document, Queue<Runnable>> parserCallbacks
            = new WeakHashMap<Document, Queue<Runnable>>();

    public static void addCallback(Document d, Runnable r) {
        synchronized (parserCallbacks) {
            if (!parserCallbacks.containsKey(d)) {
                parserCallbacks.put(d, new LinkedList<Runnable>());
            }
            parserCallbacks.get(d).add(r);
        }
    }

    private static void fireOnParse(Document d) {
        List<Runnable> tasks = new ArrayList<Runnable>();
        synchronized (parserCallbacks) {
            Queue<Runnable> queue = parserCallbacks.get(d);
            if (queue != null) {
                tasks.addAll(queue);
            }
            parserCallbacks.remove(d);
        }

        for (Runnable r : tasks) {
            r.run();
        }
    }

    String lastSource = "";

    @Override
    public void parse(Snapshot snapshot, Task task, SourceModificationEvent sme)
            throws ParseException {

        String oldContent = lastContent.get(
                snapshot.getSource().getDocument(false)
        );

        String newContent = snapshot.getText().toString();
        LOG.info(null,"===== PARSE: "+snapshot.getSource().getFileObject().getPath()+" ======");
//        LOG.info(this, "newContent = "+newContent);
        boolean changed = oldContent == null || !oldContent.equals(newContent);
//        LOG.info(this, "changed = " + changed);
//        LOG.info(this, "sme = " + sme);
//        LOG.info(this, "task = " + task);
//        LOG.info(this, "sme.sourceChanged() = " + sme.sourceChanged());

        if ( sme.sourceChanged() && changed ) {
            lastContent.put(
                    snapshot.getSource().getDocument(false),
                    newContent
            );
            reparse(snapshot);
        } else if ( result == null ){
            result = new NBMirahParserResult(snapshot, diag);
/*
            List<NBMirahParserResult.Error> errors = result.getErrors();
            for( NBMirahParserResult.Error error : errors )
            {
                LOG.info(this, "******************************** descr="+error.getDescription());
                LOG.info(this, "******************************** name ="+error.getDisplayName());
                LOG.info(this, "******************************** key  ="+error.getKey());
            }
*/
            getBlocks(result, newContent);
            
        }
        LOG.info(null,"----- Parsing End: "+snapshot.getSource().getFileObject().getPath()+" -----");
    }

    public void reparse(Snapshot snapshot) throws ParseException {
try
{
        reparse(snapshot, snapshot.getText().toString());
}
catch( Exception ex )
{
    LOG.info(this, "####### PARSE EXCEPTION "+ex+" #######");
    LOG.exception(this, ex);
}

    }

    private void copyIfChanged(File sourceRoot, File destRoot, File sourceFile) throws IOException {
        if (sourceFile.getName().endsWith(".class")) {
            String relativePath = sourceFile.getPath().substring(sourceRoot.getPath().length());
            if (relativePath.indexOf(File.separator) == 0) {
                relativePath = relativePath.substring(File.separator.length());
            }
            File destFile = new File(destRoot, relativePath);
            if (destFile.exists() && destFile.lastModified() < sourceFile.lastModified()) {
                FileInputStream fis = null;
                FileOutputStream fos = null;
                try {
                    fis = new FileInputStream(sourceFile);
                    fos = new FileOutputStream(destFile);
                    FileUtil.copy(fis, fos);
                } finally {
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (Exception ex) {
                        }
                    }
                    if (fos != null) {
                        try {
                            fos.close();

                        } catch (Exception ex) {
                        }
                    }
                }
            }
        } else if (sourceFile.isDirectory()) {
            for (File child : sourceFile.listFiles()) {
                copyIfChanged(sourceRoot, destRoot, child);
            }
        }
    }
/*
    void dumpNode( Node node, String indent )
    {
        LOG.info(this,indent+" node="+node);
        if ( node == null ) return;
        List children = node.findChildren(null);
        if ( children != null )
        {
            for( Object c : children)
            {
                LOG.info(this,"c="+c);
                if ( c instanceof Node ) dumpNode((Node)c,indent+".");
            }
        }
    }
*/    
      private static void walkTree(Node node,String indent,NodeFilter filter){
          
        if ( node == null ) return;
        
        LOG.info(MirahParser.class, indent+" node33 ="+node);
        List children = node.findChildren(filter);
        LOG.info(MirahParser.class, indent+" children ="+children);
        if ( children == null ) return;
        for ( Object c : children ){
            if ( c instanceof Node ){
                walkTree((Node)c,indent+".",filter);
            }
        }
        
    }

    void dumpNode( Node node )
    {
        NodeFilter filter = new NodeFilter(){
            @Override
            public boolean matchesNode(Node node) {
//                LOG.info(MirahParser.class,"node = "+node);
                return true;
            }
        };
        try {            
            walkTree(node,"",filter);
        }
        catch( Exception ex )
        {
            LOG.exception(this, ex);
        }
        LOG.info(this,"----- end of dump -----");
    }

    void getBlocks(final NBMirahParserResult res, String content){
        mirah.impl.MirahParser parser = new mirah.impl.MirahParser();
        
        final LinkedList<NBMirahParserResult.Block> blockStack = new LinkedList<NBMirahParserResult.Block>();
        Object ast = null;
        try {
            ast = parser.parse(new StringCodeSource(snapshot.getSource().getFileObject().getName(), content));
        } catch ( Throwable ex){
            //ex.printStackTrace();
            return;
        }
        if ( ast instanceof Node ){
            
            Node node = (Node)ast;
            res.setRoot(node);
//            dumpNode(node);

            node.accept(new NodeScanner(){

                @Override
                public boolean enterClassDefinition(ClassDefinition node, Object arg) {
                    NBMirahParserResult.Block block = null;
                    if ( blockStack.isEmpty()){
                        block = res.addBlock(node.name().identifier(), node.position().startChar(), node.position().endChar()-node.position().startChar(), "", ElementKind.CLASS);
                    } else {
                        block = blockStack.peek().addBlock(node.name().identifier(), node.position().startChar(), node.position().endChar()-node.position().startChar(), "", ElementKind.CLASS);
                    }
                    blockStack.push(block);
                    return super.enterClassDefinition(node, arg); 
                }

                @Override
                public Object exitClassDefinition(ClassDefinition node, Object arg) {
                    blockStack.pop();
                    return super.exitClassDefinition(node, arg); //To change body of generated methods, choose Tools | Templates.
                }
                
                //todo - add folding to multiline comments
                //todo - add folding for import statements
                //todo - javadoc creation

                @Override
                public boolean enterMethodDefinition(MethodDefinition node, Object arg) {
                    NBMirahParserResult.Block block = null;
                    if (blockStack.isEmpty()){
                        block = res.addBlock(node.name().identifier(), node.position().startChar(), node.position().endChar()-node.position().startChar(), "", ElementKind.METHOD);
                    } else {
                        block = blockStack.peek().addBlock(node.name().identifier(), node.position().startChar(), node.position().endChar()-node.position().startChar(), "", ElementKind.METHOD);
                    }
                    blockStack.push(block);
                    return super.enterMethodDefinition(node, arg); //To change body of generated methods, choose Tools | Templates.
                }

                @Override
                public Object exitMethodDefinition(MethodDefinition node, Object arg) {
                    blockStack.pop();
                    return super.exitMethodDefinition(node, arg); //To change body of generated methods, choose Tools | Templates.
                }
                
                

                
                @Override
                public boolean enterInterfaceDeclaration(InterfaceDeclaration node, Object arg) {
                    NBMirahParserResult.Block block = null;
                    if (blockStack.isEmpty()){
                        block = res.addBlock(node.name().identifier(), node.position().startChar(), node.position().endChar()-node.position().startChar(), "", ElementKind.INTERFACE);
                    } else {
                        block = blockStack.peek().addBlock(node.name().identifier(), node.position().startChar(), node.position().endChar()-node.position().startChar(), "", ElementKind.INTERFACE);
                    }
                    blockStack.push(block);
                    return super.enterInterfaceDeclaration(node, arg); //To change body of generated methods, choose Tools | Templates.
                }

                @Override
                public Object exitInterfaceDeclaration(InterfaceDeclaration node, Object arg) {
                    blockStack.pop();
                    return super.exitInterfaceDeclaration(node, arg); //To change body of generated methods, choose Tools | Templates.
                }
                
                

                @Override
                public boolean enterStaticMethodDefinition(StaticMethodDefinition node, Object arg) {
                    NBMirahParserResult.Block block = null;
                    if (blockStack.isEmpty()){
                        block = res.addBlock(node.name().identifier(), node.position().startChar(), node.position().endChar()-node.position().startChar(), "", ElementKind.METHOD);
                    } else {
                        block = blockStack.peek().addBlock(node.name().identifier(), node.position().startChar(), node.position().endChar()-node.position().startChar(), "", ElementKind.METHOD);
                    }
                    blockStack.push(block);
                    return super.enterStaticMethodDefinition(node, arg); //To change body of generated methods, choose Tools | Templates.
                }

                @Override
                public Object exitStaticMethodDefinition(StaticMethodDefinition node, Object arg) {
                    blockStack.pop();
                    return super.exitStaticMethodDefinition(node, arg); //To change body of generated methods, choose Tools | Templates.
                }

                
                /*
                @Override
                public boolean enterScript(Script node, Object arg) {
                    NBMirahParserResult.Block block = res.addBlock("Mirah File", node.position().startChar(), node.position().endChar()-node.position().startChar(), "", ElementKind.OTHER);
                    blockStack.push(block);
                    return super.enterScript(node, arg); //To change body of generated methods, choose Tools | Templates.
                }

                @Override
                public Object exitScript(Script node, Object arg) {
                    blockStack.pop();
                    return super.exitScript(node, arg); //To change body of generated methods, choose Tools | Templates.
                }
                
                */

                @Override
                public boolean enterPackage(Package node, Object arg) {
                    if ( !blockStack.isEmpty() ){
                        blockStack.pop();
                    }
                    NBMirahParserResult.Block block = null;
                    if (blockStack.isEmpty()){
                        block = res.addBlock(node.name().identifier(), node.position().startChar(), node.position().endChar()-node.position().startChar(), "", ElementKind.PACKAGE);
                    } else {
                        block = blockStack.peek().addBlock(node.name().identifier(), node.position().startChar(), node.position().endChar()-node.position().startChar(), "", ElementKind.PACKAGE);
                    }
                    blockStack.push(block);
                    return super.enterPackage(node, arg); //To change body of generated methods, choose Tools | Templates.
                }

                
                
                

                @Override
                public boolean enterFieldDeclaration(FieldDeclaration node, Object arg) {
                    NBMirahParserResult.Block block = null;
                    if (blockStack.isEmpty()){
                        block = res.addBlock(node.name().identifier(), node.position().startChar(), node.position().endChar()-node.position().startChar(), "", ElementKind.FIELD);
                    } else {
                        block = blockStack.peek().addBlock(node.name().identifier(), node.position().startChar(), node.position().endChar()-node.position().startChar(), "", ElementKind.FIELD);
                    }
                    blockStack.push(block);
                    return super.enterFieldDeclaration(node, arg); //To change body of generated methods, choose Tools | Templates.
                }

                @Override
                public Object exitFieldDeclaration(FieldDeclaration node, Object arg) {
                    blockStack.pop();
                    return super.exitFieldDeclaration(node, arg); //To change body of generated methods, choose Tools | Templates.
                }
                /*
                @Override
                public boolean enterImport(Import node, Object arg) {
                    NBMirahParserResult.Block block = null;
                    if (blockStack.isEmpty()){
                        block = res.addBlock(node.name().identifier(), node.position().startChar(), node.position().endChar()-node.position().startChar(), "", ElementKind..INTERFACE);
                    } else {
                        block = blockStack.peek().addBlock(node.name().identifier(), node.position().startChar(), node.position().endChar()-node.position().startChar(), "", ElementKind.INTERFACE);
                    }
                    blockStack.push(block);
                    return super.enterImport(node, arg); //To change body of generated methods, choose Tools | Templates.
                }
                
                @Override
                public Object exitImport(Import node, Object arg) {
                    blockStack.pop();
                    return super.exitImport(node, arg); //To change body of generated methods, choose Tools | Templates.
                }
                */
                @Override
                public boolean enterMacroDefinition(MacroDefinition node, Object arg) {
                    NBMirahParserResult.Block block = null;
                    if (blockStack.isEmpty()){
                        block = res.addBlock(node.name().identifier(), node.position().startChar(), node.position().endChar()-node.position().startChar(), "", ElementKind.METHOD);
                    } else {
                        block = blockStack.peek().addBlock(node.name().identifier(), node.position().startChar(), node.position().endChar()-node.position().startChar(), "", ElementKind.METHOD);
                    }
                    blockStack.push(block);
                    return super.enterMacroDefinition(node, arg); //To change body of generated methods, choose Tools | Templates.
                }

                @Override
                public Object exitMacroDefinition(MacroDefinition node, Object arg) {
                    blockStack.pop();
                    return super.exitMacroDefinition(node, arg); //To change body of generated methods, choose Tools | Templates.
                }
                
                
                
                
                
                
                
                
                
                
                
                
                
            }, null);
        }
        
    }

    public void reparse(Snapshot snapshot, String content)
            throws ParseException {

        LOG.info(this, "reparse222 = " + snapshot.getSource().getFileObject().getPath());
        LOG.putStack(null);
        this.snapshot = snapshot;
        diag = new MirahParseDiagnostics();
        NBMirahParserResult parserResult = new NBMirahParserResult(snapshot, diag);
        result = parserResult;
        getBlocks(parserResult, content);
        
//        LOG.info(this,"REPARSE:\n"+content+"\n");
        
        WLMirahCompiler compiler = new WLMirahCompiler();
        
        compiler.setPrecompileJavaStubs(false);
        
//        LOG.info(this,"WLMirahCompiler compiler="+compiler);
        
        //URL location = compiler.getClass().getResource('/' + compiler.getClass().getName().replace('.', '/') + ".class");
//        LOG.info(this,"WLMirahCompiler compiler location="+location);
//        URL location = compiler.getClass().getResource('/' + compiler.getClass().getName().replace('.', '/') + ".class");
//        LOG.info(this,"WLMirahCompiler compiler location="+location);
        
        FileObject src = snapshot.getSource().getFileObject();
        LOG.info(this,"src = " + src);

        Project project = FileOwnerQuery.getOwner(src);
        LOG.info(this,"reparse project = " + project);

        FileObject projectDirectory = project.getProjectDirectory();
        FileObject buildDir = projectDirectory.getFileObject("build");
        Preferences projPrefs = ProjectUtils.getPreferences(project, MirahExtenderImplementation.class, true);
        String projectType = projPrefs.get("project_type", "unknown");
        LOG.info(this,"reparse project type is "+projectType);
        if ( "maven".equals(projectType)){
            try {
                // It's a maven project so we want to build our sources to a different location
                FileObject cacheDir = ProjectUtils.getCacheDirectory(project, MirahExtenderImplementation.class);
                buildDir = cacheDir.getFileObject("build");
                if ( buildDir == null ){
                    buildDir = cacheDir.createFolder("build");
                }
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        

        ClassPath compileClassPath
                = ClassPath.getClassPath(src, ClassPath.COMPILE);
        String compileClassPathStr = "";
        if (compileClassPath != null) {
            compileClassPathStr = compileClassPath.toString();
        }
        
        LOG.info(this,"=> compileClassPath = "+compileClassPath);
        
        ClassPath buildClassPath
                = ClassPath.getClassPath(src, ClassPath.EXECUTE);
        String buildClassPathStr = "";
        if (buildClassPath != null) {
            buildClassPathStr = buildClassPath.toString();
        }
        LOG.info(this,"=> buildClassPath = " + buildClassPath);

        ClassPath srcClassPath = ClassPath.getClassPath(src, ClassPath.SOURCE);
        String srcClassPathStr = "";

        LOG.info(this,"=> srcClassPath = " + srcClassPath);
        
        if (srcClassPath != null) {
            srcClassPathStr = srcClassPath.toString();
        }
        String changedSourcePaths = RecompileQueue.getProjectQueue(project).getAndClearChangedSourcePaths();
        //LOG.info("=> changedSourcePaths = " + changedSourcePaths);
        if (changedSourcePaths != null) {
            Set<String> set = new HashSet<String>();
            set.addAll(Arrays.asList(changedSourcePaths.split(Pattern.quote(File.pathSeparator))));
            set.addAll(Arrays.asList(srcClassPathStr.split(Pattern.quote(File.pathSeparator))));
            StringBuilder sb = new StringBuilder();
            for (String p : set) {
                sb.append(p).append(File.pathSeparator);
            }
            srcClassPathStr = sb.substring(0, sb.length() - File.pathSeparator.length());
        }
        LOG.info(this,"=> srcClassPathStr = " + srcClassPathStr);
       
        compiler.setSourcePath(srcClassPathStr);

        String dest = buildClassPathStr;
        FileObject mirahDir = null;
        try {
            if (buildDir == null) {
                buildDir = projectDirectory.createFolder("build");
            }
            mirahDir = buildDir.getFileObject("mirah");
            if (mirahDir == null) {
                mirahDir = buildDir.createFolder("mirah");
            }
            File javaStubDir = new File(buildDir.getPath(), "mirah_tmp" + File.separator + "java_stub_dir");
            javaStubDir.mkdirs();
            compiler.setJavaStubDirectory(javaStubDir);
            dest = mirahDir.getPath();
        } catch (IOException ex) {

        }
        
//        LOG.info(this,"=> dest = " + dest);
//        LOG.info(this,"=> diag = " + diag);
        
        compiler.setDestinationDirectory(new File(dest));
        compiler.setDiagnostics(diag);

        List<String> paths = new ArrayList<String>();
        if (!"".equals(srcClassPathStr)) {
            paths.add(srcClassPathStr);
        }
        if (!"".equals(buildClassPathStr)) {
            paths.add(buildClassPathStr);
        }
        if (!"".equals(compileClassPathStr)) {
            paths.add(compileClassPathStr);
        }

        StringBuilder classPath = new StringBuilder();
        for (String path : paths) {
            classPath.append(path);
            classPath.append(File.pathSeparator);

        }

        String macroPath = new StringBuilder()
                .append(classPath.toString())
                .append(buildDir.getPath())
                .append(File.separator)
                .append("mirah_tmp")
                .append(File.separator)
                .append("macros")
                .append(File.separator)
                .append("classes")
                .append(File.pathSeparator)
                .toString();

        FileObject macroJarDir = projectDirectory.getFileObject("lib/mirah/macros");
        
        if ( macroJarDir != null ){
            
            File jarDir = FileUtil.toFile(macroJarDir);
            File[] jars = jarDir.listFiles(new FilenameFilter(){

                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".jar");
                }

            });
            if ( jars != null ){
                for ( File jar : jars ){
                    macroPath += jar.getAbsolutePath() + File.pathSeparator;
                    
                }
            }
        }
        
        
        if (macroPath.length() >= 1) {
            macroPath = macroPath.substring(0, macroPath.length() - 1);
        }
        
        String cp = ".";
        if (classPath.length() >= 1) {
            cp = classPath.toString().substring(0, classPath.length() - 1);
        }
        compiler.setClassPath(macroPath + File.pathSeparator + cp);

        compiler.setMacroClassPath(macroPath);
        
        
        
        DocumentDebugger debugger = new DocumentDebugger();

        compiler.setDebugger(debugger);

        ClassPath bootClassPath = ClassPath.getClassPath(src, ClassPath.BOOT);
        String bootClassPathStr = "";
        if (bootClassPath != null) {
            bootClassPathStr = bootClassPath.toString();
        }
        if (!"".equals(bootClassPathStr)) {
            compiler.setBootClassPath(bootClassPathStr);
        }
        String srcText = content;
        FileObject fakeFileRoot = getRoot(src);
//        LOG.info(this,"fakeFileRoot == "+fakeFileRoot);
        if ( fakeFileRoot == null )
        {
            LOG.info(this,"fakeFileRoot == NULL for src = " + src);
            return;
        }
        String relPath = FileUtil.getRelativePath(fakeFileRoot, src);
        relPath = relPath.substring(0, relPath.lastIndexOf("."));
//        LOG.info(this,"relPath == "+relPath);
        compiler.addFakeFile(relPath, srcText);
        FileChangeAdapter fileChangeListener = null;
        try {

//        LOG.info(this,"relPath compiler == "+compiler);
//        LOG.info(this,"relPath mirahDir == "+mirahDir);
//        LOG.info(this,"relPath compileClassPath == "+compileClassPath);
        if ( compileClassPath != null )
//            LOG.info(this,"relPath compileClassPath.getRoots() == "+compileClassPath.getRoots());
            compiler.compile(new String[0]);
            if (mirahDir != null) {
                
                for (FileObject compileRoot : compileClassPath.getRoots()) {
//                    LOG.info(this,"compileRoot = "+compileRoot);
                    if (!compileRoot.getPath().endsWith(".jar") && compileRoot.isFolder() && !mirahDir.equals(compileRoot)) {
                        copyIfChanged(new File(mirahDir.getPath()), new File(compileRoot.getPath()), new File(mirahDir.getPath()));
                    }
                }
            }
            
            if ("maven".equals(projectType)){
                // If its a maven project, we need to copy the build files into 
//                LOG.info(this,"This is a MAVEN PROJECT");
//                LOG.putStack(null);
                FileObject libDir = projectDirectory.getFileObject("lib");
                if ( libDir == null ){
                    libDir = projectDirectory.createFolder("lib");
                    
                }
                //LOG.info("libDir = "+libDir);
                FileObject mirahTmpClassesDir = libDir.getFileObject("mirah-tmp-classes");
                //LOG.info("mirahTmpClassesDir = " + mirahTmpClassesDir);
                if ( mirahTmpClassesDir == null ){
                    mirahTmpClassesDir = libDir.createFolder("mirah-tmp-classes");
                }
                
                File jarFile = new File(FileUtil.toFile(libDir), "mirah-tmp-classes.jar");
                FileUtils.copyDirectoryStructureIfModified(new File(dest), FileUtil.toFile(mirahTmpClassesDir));
                createJar(FileUtil.toFile(mirahTmpClassesDir), FileUtil.toFile(mirahTmpClassesDir).getPath(), jarFile);
            }

        } catch (Exception ex) {
            LOG.info(this,"REPARSE ex = "+ex);
            ex.printStackTrace();
        }

        //LOG.info(this,"resolvedTypes = "+debugger.resolvedTypes.size());
        //LOG.info(this,"-------------------------------------------------------------------------------------------------------------------");
//        synchronized (documentDebuggers) {

            Document doc = snapshot.getSource().getDocument(true);

            if (debugger.resolvedTypes.size() > 0) {
                debugger.compiler = compiler.getMirahc();
//LOG.putStack("8");
                documentDebuggers.put(doc, debugger);
                fireOnParse(doc);
            }
//        }

    }

    private FileObject getRoot(FileObject file) {
        Project project = FileOwnerQuery.getOwner(file);
//        LOG.info(this,"getRoot file="+file+" project="+project);
        Sources sources = ProjectUtils.getSources(project);
        for (SourceGroup sourceGroup : sources.getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA)) {
            FileObject root = sourceGroup.getRootFolder();
//            LOG.info(this,"getRoot root=" + root+" sourceGroup=" + sourceGroup);
		    if (FileUtil.isParentOf(root, file) || root.equals(file)) {
//                LOG.info(this, "getRoot RETURN-1 root=" + root);
			    return root;
		    }
	    }
//        LOG.info(this, "getRoot RETURN-2 root=" + ClassPath.getClassPath(file, ClassPath.SOURCE).findOwnerRoot(file));
        return ClassPath.getClassPath(file, ClassPath.SOURCE).findOwnerRoot(file);
    }

    @Override
    public Result getResult(Task task) {
        if ( result != null ) {
            return result;
        }
        if ( snapshot == null ){
            
        }
        return new NBMirahParserResult(snapshot, diag);
    }

    @Override
    public void cancel() {
    }

    @Override
    public void addChangeListener(ChangeListener cl) {

    }

    @Override
    public void removeChangeListener(ChangeListener changeListener) {
    }

    public static class NBMirahParserResult extends ParserResult {

        private MirahParseDiagnostics diagnostics;
        private boolean valid = true;
        List<Error> errorList = new ArrayList<Error>();
        List<Block> blockList = new ArrayList<Block>();
        Node rootNode;

        NBMirahParserResult(
                Snapshot snapshot,
                MirahParseDiagnostics diagnostics) {
            super(snapshot);
            this.diagnostics = diagnostics;
        }

        public MirahParseDiagnostics getMirahDiagnostics() {
            return diagnostics;
        }

        @Override
        public List<? extends Error> getDiagnostics() {
            return new ArrayList<Error>();
        }

        @Override
        protected void invalidate() {
            valid = false;
        }

        public List<Error> getErrors() {
            return errorList;
        }

        public void addError(String description, int offset, int length) {
            errorList.add(new Error(description, offset, length, getSnapshot()));
        }

        public List<Block> getBlocks() {
            return blockList;
        }

        public Block  addBlock(CharSequence function, int offset, int length, CharSequence extra, ElementKind kind) {
            Block block = new Block(function, offset, length, extra, kind);
            blockList.add(block);
            return block;
        }
        
        public Block addBlock(Block parent, CharSequence function, int offset, int length, CharSequence extra, ElementKind kind){
            return parent.addBlock(function, offset, length, extra, kind);
        }
        
        public void setRoot( Node root )
        {
            this.rootNode = root;
        }
        
        public Node getRoot() {
            return rootNode;
        }
        
        public class Error implements org.netbeans.modules.csl.api.Error {

            String description;
            int offset;
            int length;
            Snapshot snapshot;

            public Error(String description, int offset, int length, Snapshot snapshot) {
                this.description = description;
                this.offset = offset;
                this.length = length;
                this.snapshot = snapshot;
            }

            @Override
            public String getDescription() {
                return description;
            }

            public int getOffset() {
                return offset;
            }

            public int getLength() {
                return length;
            }

            @Override
            public String getDisplayName() {
                return description;
            }

            @Override
            public String getKey() {
                return description;
            }

            @Override
            public FileObject getFile() {
                return snapshot.getSource().getFileObject();
            }

            @Override
            public int getStartPosition() {
                return offset;
            }

            @Override
            public int getEndPosition() {
                return offset + length;
            }

            @Override
            public boolean isLineError() {
                return false;
            }

            @Override
            public Severity getSeverity() {
                return Severity.ERROR;
            }

            @Override
            public Object[] getParameters() {
                return null;
            }

        }

        public class Block {

            
            ElementKind kind;
            CharSequence function;
            int offset;
            int length;
            CharSequence extra;
            List<Block> children = new ArrayList<Block>();

            public Block(CharSequence function, int offset, int length, CharSequence extra, ElementKind kind) {
                this.function = function;
                this.offset = offset;
                this.length = length;
                this.extra = extra;
                this.kind = kind;
            }

            public Block addBlock(CharSequence function, int offset, int length, CharSequence extra, ElementKind kind){
                Block block = new Block(function, offset, length, extra, kind);
                children.add(block);
                return block;
            }
            
            public List<Block> getChildren(){
                return Collections.unmodifiableList(children);
            }
            
            public CharSequence getExtra() {
                return extra;
            }

            public CharSequence getDescription() {
                return function;
            }

            public int getOffset() {
                return offset;
            }

            public int getLength() {
                return length;
            }
            
            public ElementKind getKind(){
                return kind;
            }

        }

    }

    public static class MirahParseDiagnostics extends SimpleDiagnostics {

        static class SyntaxError {

            SyntaxError(Diagnostic.Kind k, String pos, String msg) {
                kind = k;
                position = pos;
                message = msg;
            }
            Diagnostic.Kind kind;
            String position;
            String message;
        }

        private List<SyntaxError> errors = new ArrayList<SyntaxError>();

        public MirahParseDiagnostics() {
            super(false);
        }

        @Override
        public int errorCount() {
            return super.errorCount();
        }

        @Override
        public void log(Diagnostic.Kind kind, String position, String message) {

            super.log(kind, position, message);
            if (!"ERROR_TO_PREVENT_COMPILING".equals(message)) {
                errors.add(new SyntaxError(kind, position, message));
            }
        }

        public List<SyntaxError> getErrors() {
            return errors;
        }
    }

    public static class DocumentDebugger implements DebuggerInterface {

        public static class PositionType {

            public int startPos, endPos;
            public ResolvedType type;
            public Node node;

            public String toString() {
                if (type == null) {
                    return "[" + startPos + "," + endPos + "]";
                } else {
                    return "[" + type.name() + " " + startPos + "," + endPos + "]";
                }
            }

        }

        final private TreeSet<PositionType> leftEdges;
        final private TreeSet<PositionType> rightEdges;
        final private HashMap<Node, ResolvedType> resolvedTypes
                = new HashMap<Node, ResolvedType>();

        public Mirahc compiler;

        public DocumentDebugger() {

            leftEdges = new TreeSet<PositionType>(
                    new Comparator<PositionType>() {

                        @Override
                        public int compare(PositionType o1, PositionType o2) {
                            if (o1.startPos < o2.startPos) {
                                return -1;
                            } else if (o2.startPos < o1.startPos) {
                                return 1;
                            } else if (o1.endPos < o2.endPos) {
                                return -1;
                            } else if (o2.endPos < o1.endPos) {
                                return 1;
                            } else {
                                return 0;
                            }
                        }

                    });
            rightEdges = new TreeSet<PositionType>(
                    new Comparator<PositionType>() {

                        @Override
                        public int compare(PositionType o1, PositionType o2) {
                            if (o1.endPos < o2.endPos) {
                                return -1;
                            } else if (o2.endPos < o1.endPos) {
                                return 1;
                            } else if (o1.startPos < o2.startPos) {
                                return -1;
                            } else if (o2.startPos < o1.startPos) {
                                return 1;
                            } else {
                                return 0;
                            }
                        }

                    });
        }

        public PositionType findNearestPositionOccurringAfter(int pos) {
            PositionType t = new PositionType();
            t.startPos = pos;
            return leftEdges.ceiling(t);
        }

        public PositionType findNearestPositionOccuringBefore(int pos) {
            PositionType t = new PositionType();
            t.endPos = pos;
            return rightEdges.lower(t);
        }

        public ResolvedType getType(Node node) {
            return resolvedTypes.get(node);
        }

        public SortedSet<PositionType> findPositionsWithRightEdgeInRange(
                int start,
                int end) {
            PositionType p1 = new PositionType();
            p1.endPos = start;
            p1.startPos = 0;

            PositionType p2 = new PositionType();
            p2.endPos = end;
            p2.startPos = end;

            SortedSet<PositionType> o1 = rightEdges.subSet(p1, p2);
            return o1;
        }

        public int countNodes() {
            return rightEdges.size();
        }

        public Node firstNode() {
            return leftEdges.first().node;
        }

        @Override
        public void parsedNode(Node node) {

        }

        @Override
        public void enterNode(Context cntxt, Node node, boolean bln) {

        }

        @Override
        public void exitNode(Context cntxt, final Node node, TypeFuture tf) {
//            LOG.info(this,"exitNode Context=" + cntxt + " node=" + node+" tf="+tf);
            tf.onUpdate(new TypeListener() {

                @Override
                public void updated(TypeFuture tf, ResolvedType rt) {
                    
//                    LOG.info(MirahParser.class,"exitNode:updated TypeFuture="+tf+" ResolvedType="+rt);
                    if (!tf.isResolved()) {
                        return;
                    }
//                    LOG.info(MirahParser.class,">>>> exitNode:updated TypeFuture=" + tf + " ResolvedType=" + rt);
                    if (node.position() == null) {
                        return;
                    }
//                    LOG.info(this,"<<<< exitNode:updated TypeFuture=" + tf + " ResolvedType=" + rt);
//                    LOG.info(this,"<<<< exitNode:updated TypeFuture=" + tf + " ResolvedType=" + rt);
                    PositionType t = new PositionType();
                    t.startPos = node.position().startChar();
                    t.endPos = node.position().endChar();
                    t.type = rt;
                    t.node = node;
                    if (leftEdges.contains(t)) {
                        leftEdges.remove(t);
                    }
                    if (rightEdges.contains(t)) {
                        rightEdges.remove(t);
                    }
                    leftEdges.add(t);
                    rightEdges.add(t);

                    resolvedTypes.put(node, rt);

                }

            });
        }

        @Override
        public void inferenceError(Context cntxt, Node node, TypeFuture tf) {

        }

    }

    private static String nodeToString(Node n) {
        if (n == null || n.position() == null) {
            if (n != null) {
                return "" + n;
            }
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Node ").append(n)
                .append(n.position().startLine())
                .append(".")
                .append(n.position().startColumn())
                .append(":")
                .append(n.position().startChar())
                .append("-")
                .append(n.position().endLine())
                .append(".")
                .append(n.position().endColumn())
                .append(":")
                .append(n.position().endChar())
                .append(" # ");

        return sb.toString();

    }
    
    private void createJar(File source, String sourceRoot, File jarFile) throws IOException {
        FileOutputStream fos = null;
        JarOutputStream jos = null;
        try {
            fos = new FileOutputStream(jarFile);
            jos = new JarOutputStream(fos);
            jos.setLevel(0);
            
            addToJar(source, sourceRoot, jos);
        } finally {
            try {
                if ( jos != null ) jos.close();
            } catch ( Throwable t ){}
            try {
                if ( fos != null ) fos.close();
            } catch ( Throwable t){}
        }
              
        
        
    }
    
    private void addToJar(File source, String sourceRoot, JarOutputStream jos) throws IOException {
        if ( source.getName().endsWith(".class")){
            String fileName = formatEntry(source, sourceRoot, false);
//            LOG.info(this,"Adding file "+fileName+" to jar ("+source+")");
            ZipEntry entry = new ZipEntry(fileName);
            jos.putNextEntry(entry);
            InputStream fis = null;
            try {
                fis = new FileInputStream(source);
                byte[] buf = new byte[4096];
                int len;
                while ( (len = fis.read(buf)) != -1 ){
//                    LOG.info(this,"Writing "+len+" bytes");
                    jos.write(buf, 0, len);
                }
                jos.closeEntry();
            } finally {
                try {
                    if ( fis != null ){
                        fis.close();
                    }
                } catch ( Exception ex){}
            }
        } else if ( source.isDirectory() ){
            String dirName = formatEntry(source, sourceRoot, true);
//            LOG.info(this,"Adding "+dirName+" to jar");
            //ZipEntry entry = new ZipEntry(dirName);
            //jos.putNextEntry(entry);
            for ( File child : source.listFiles()){
                addToJar(child, sourceRoot, jos);
            }
            //jos.closeEntry();
        }
    }
    
    private String formatEntry(File f, String sourceRoot, boolean directory){
        if ( directory ){
            String name = f.getPath().substring(sourceRoot.length());
            name = name.replace("\\", "/");
            if ( !name.endsWith("/")){
                name += "/";
            }
            if ( name.startsWith("/")){
                name = name.substring(1);
            }
            return name;
        } else {
            String name = f.getPath().substring(sourceRoot.length());
            name = name.replace("\\", "/");
            
            if ( name.startsWith("/")){
                name = name.substring(1);
            }
            return name;
        }
    }

}
