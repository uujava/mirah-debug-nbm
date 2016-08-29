package ca.weblite.netbeans.mirah.lexer;

import ca.weblite.asm.WLMirahCompiler;
import ca.weblite.netbeans.mirah.LOG;
import ca.weblite.netbeans.mirah.support.spi.MirahExtenderImplementation;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.WeakHashMap;
import java.util.jar.JarOutputStream;
import java.util.prefs.Preferences;
import java.util.zip.ZipEntry;
//import javax.lang.model.element.ElementKind;
import javax.swing.event.ChangeListener;
import javax.swing.text.Document;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;

import mirah.lang.ast.*;
import mirah.lang.ast.Boolean;
import mirah.lang.ast.Float;
import mirah.lang.ast.Package;
import org.codehaus.plexus.util.FileUtils;
import org.mirah.jvm.mirrors.debug.DebuggerInterface;
import org.mirah.tool.Mirahc;
import org.mirah.typer.ResolvedType;
import org.mirah.typer.TypeFuture;
import org.mirah.typer.TypeListener;
import org.mirah.util.Context;
import org.mirah.util.SimpleDiagnostics;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.ClassPath.Entry;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.netbeans.modules.csl.api.Severity;
import org.netbeans.modules.csl.spi.ParserResult;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.api.Task;
import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.modules.parsing.spi.Parser;
import org.netbeans.modules.parsing.spi.SourceModificationEvent;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;

/**
 *
 * @author shannah
 */
public class MirahParser extends Parser {

    private static WeakHashMap<Document, DocumentDebugger> documentDebuggers = new WeakHashMap<>();
    private static WeakHashMap<Document, String> lastContent = new WeakHashMap<>();
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

        long start = System.currentTimeMillis();
        
        String oldContent = lastContent.get(
                snapshot.getSource().getDocument(false)
        );

        String newContent = snapshot.getText().toString();
//        LOG.info(null, "----- Parsing Start: " + snapshot.getSource().getFileObject().getNameExt() + " -----");
//        LOG.putStack("***");
        boolean changed = oldContent == null || !oldContent.equals(newContent);
        if (sme.sourceChanged() && changed) {
            lastContent.put(
                    snapshot.getSource().getDocument(false),
                    newContent
            );
            reparse(snapshot);
        } else if (result == null) {
            result = new NBMirahParserResult(snapshot, diag);
            getBlocks(result, newContent);
            try {            
                DocumentDebugger dbg = getDocumentDebugger(snapshot.getSource().getDocument(false));
                if ( dbg != null )
                {
                    // сохраняю карту распознанных типов
                    result.setResolvedTypes(dbg.resolvedTypes);
                    // Сохраняю дерево разбора - это список List<Node>
                    if ( dbg.compiler != null && dbg.compiler.compiler() != null )
                    result.setParsedNodes(dbg.compiler.compiler().getParsedNodes());
                }
            }
            catch( Exception e )
            {
                LOG.exception(this,e);
            }
        }
        LOG.info(null, "Parsing " + snapshot.getSource().getFileObject().getNameExt() + " Elapsed: "+(System.currentTimeMillis()-start)+" msec");
    }

    public void reparse(Snapshot snapshot) throws ParseException {
        try
        {
            if ( snapshot == null || snapshot.getText() == null ) return;
            reparse(snapshot, snapshot.getText().toString());
        }
        catch( Exception ex )
        {
            LOG.info(this, "####### PARSE EXCEPTION File: "+snapshot.getSource().getFileObject().getName()+" ex: "+ex+" #######");
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
      private static void walkTree(Node node,String indent, final NodeFilter filter){
          
        if ( node == null ) return;

          List<Node> children = (List<Node>) node.accept(new NodeScanner() {
            @Override
            public boolean enterDefault(Node node, Object arg) {
                if(filter.matchesNode(node)){
                    ((List<Node>)arg).add(node);
                }
                return true;
            }
        }, new ArrayList<Node>());
        if ( children == null ) return;
        for ( Object c : children ){
            if ( c instanceof Node ){
                walkTree((Node)c,indent+".",filter);
            }
        }
        
    }

    void dumpNode(Node node) {
        NodeFilter filter = new NodeFilter() {
            @Override
            public boolean matchesNode(Node node) {
//                LOG.info(MirahParser.class,"node = "+node);
                return true;
            }
        };
        try {
            walkTree(node, "", filter);
        } catch (Exception ex) {
            LOG.exception(this, ex);
        }
        LOG.info(this, "----- end of dump -----");
    }

    // При Array.size <= 7 поиск перебором по массиву не медленнее индексированного поиска в Map
//    private static final Collection<String> macroNames = new ArrayList<>();
//    static {
//        macroNames.add("attr_reader");
//        macroNames.add("attr_writer");
//        macroNames.add("attr_accessor");
//        macroNames.add("fx_component");
//        macroNames.add("entity_attr");
//    }

    void getBlocks(final NBMirahParserResult res, String content) {
        
        if ( true ) return;
        
        mirah.impl.MirahParser parser = new mirah.impl.MirahParser();

        final LinkedList<Block> blockStack = new LinkedList<>();
        Object ast;
        try {
            ast = parser.parse(new StringCodeSource(snapshot.getSource().getFileObject().getName(), content));
        } catch (Throwable ex) {
            // ex.printStackTrace();
            return;
        }
//        BlockCollector coll = new BlockCollector();
//        coll.prepareBlocks(res);
//        res.setBlocks(coll.getBlocks());
        /*
        if ( ast instanceof Node ) {            
            Node node = (Node)ast;

            node.accept(new NodeScanner() {

                @Override
                public boolean enterImport(Import node, Object arg) {
                    final BlockNode parent = blockStack.isEmpty() ? res : blockStack.peek();
                    int offset = 0;
                    int length = 0;
                    if (node != null && node.position() != null) {
                        offset = node.position().startChar();
                        length = node.position().endChar() - node.position().startChar();
                    }
                    blockStack.push(parent.addBlock(node, node.fullName().identifier(), offset, length, "", ElementKind.OTHER));
//                    blockStack.push(parent.addImport(node, node.fullName().identifier(), node.position().startChar(), node.position().endChar() - node.position().startChar(), "", ElementKind.OTHER));
                    return super.enterImport(node, arg);
                }

                @Override
                public Object exitImport(Import node, Object arg) {
                    blockStack.pop();
                    return super.exitImport(node, arg);
                }

                @Override
                public boolean enterBlock(mirah.lang.ast.Block node, Object arg) {
                    final BlockNode parent = blockStack.isEmpty() ? res : blockStack.peek();
                    int offset = 0;
                    int length = 0;
                    if (node != null && node.position() != null) {
                        offset = node.position().startChar();
                        length = node.position().endChar() - node.position().startChar();
                    }
                    blockStack.push(parent.addDSL(node, "DSL", offset, length, "", ElementKind.METHOD));
//                    blockStack.push(parent.addDSL(node, "DSL", node.position().startChar(), node.position().endChar() - node.position().startChar(), "", ElementKind.METHOD));
                    return super.enterBlock(node, arg);
                }

                @Override
                public Object exitBlock(mirah.lang.ast.Block node, Object arg) {
                    blockStack.pop();
                    return super.exitBlock(node, arg);
                }

                @Override
                public boolean enterPackage(Package node, Object arg) {
                    if (!blockStack.isEmpty()) {
                        blockStack.pop();
                    }
                    final BlockNode parent = blockStack.isEmpty() ? res : blockStack.peek();
                    int offset = 0;
                    int length = 0;
                    if (node != null && node.position() != null) {
                        offset = node.position().startChar();
                        length = node.position().endChar() - node.position().startChar();
                    }
                    blockStack.push(parent.addBlock(node, node.name().identifier(), offset, length, "", ElementKind.PACKAGE));
//                    blockStack.push(parent.addBlock(node, node.name().identifier(), node.position().startChar(), node.position().endChar() - node.position().startChar(), "", ElementKind.PACKAGE));
                    return super.enterPackage(node, arg);
                }

                @Override
                public boolean enterClassDefinition(ClassDefinition node, Object arg) {
                    final BlockNode parent = blockStack.isEmpty() ? res : blockStack.peek();
                    int offset = 0;
                    int length = 0;
                    if (node != null && node.position() != null) {
                        offset = node.position().startChar();
                        length = node.position().endChar() - node.position().startChar();
                    }
                    blockStack.push(parent.addBlock(node, node.name().identifier(), offset, length, "", ElementKind.CLASS));
//                    blockStack.push(parent.addBlock(node, node.name().identifier(), node.position().startChar(), node.position().endChar() - node.position().startChar(), "", ElementKind.CLASS));
                    return super.enterClassDefinition(node, arg);
                }

                @Override
                public Object exitClassDefinition(ClassDefinition node, Object arg) {
                    blockStack.pop();
                    return super.exitClassDefinition(node, arg);
                }

                @Override
                public boolean enterMethodDefinition(MethodDefinition node, Object arg) {
                    BlockNode parent = blockStack.isEmpty() ? res : blockStack.peek();
                    int offset = 0;
                    int length = 0;
                    if ( node != null && node.position() != null) {
                        offset = node.position().startChar();
                        length = node.position().endChar() - node.position().startChar();
                    }
                    blockStack.push(parent.addBlock(node, node.name().identifier(), offset, length, "", ElementKind.METHOD));
//                    blockStack.push(parent.addBlock(node, node.name().identifier(), node.position().startChar(), node.position().endChar() - node.position().startChar(), "", ElementKind.METHOD));
                    return super.enterMethodDefinition(node, arg);
                }

                @Override
                public Object exitMethodDefinition(MethodDefinition node, Object arg) {
                    blockStack.pop();
                    return super.exitMethodDefinition(node, arg);
                }

                @Override
                public boolean enterConstructorDefinition(ConstructorDefinition node, Object arg) {
                    final BlockNode parent = blockStack.isEmpty() ? res : blockStack.peek();
                    int offset = 0;
                    int length = 0;
                    if (node != null && node.position() != null) {
                        offset = node.position().startChar();
                        length = node.position().endChar() - node.position().startChar();
                    }
                    blockStack.push(parent.addBlock(node, node.name().identifier(), offset, length, "", ElementKind.CONSTRUCTOR));
//                    blockStack.push(parent.addBlock(node, node.name().identifier(), node.position().startChar(), node.position().endChar() - node.position().startChar(), "", ElementKind.CONSTRUCTOR));
                    return super.enterConstructorDefinition(node, arg);
                }

                @Override
                public Object exitConstructorDefinition(ConstructorDefinition node, Object arg) {
                    blockStack.pop();
                    return super.exitConstructorDefinition(node, arg);
                }

                @Override
                public boolean enterInterfaceDeclaration(InterfaceDeclaration node, Object arg) {
                    final BlockNode parent = blockStack.isEmpty() ? res : blockStack.peek();
                    int offset = 0;
                    int length = 0;
                    if (node != null && node.position() != null) {
                        offset = node.position().startChar();
                        length = node.position().endChar() - node.position().startChar();
                    }
                    blockStack.push(parent.addBlock(node, node.name().identifier(), offset, length, "", ElementKind.INTERFACE));
//                    blockStack.push(parent.addBlock(node, node.name().identifier(), node.position().startChar(), node.position().endChar() - node.position().startChar(), "", ElementKind.INTERFACE));
                    return super.enterInterfaceDeclaration(node, arg);
                }

                @Override
                public Object exitInterfaceDeclaration(InterfaceDeclaration node, Object arg) {
                    blockStack.pop();
                    return super.exitInterfaceDeclaration(node, arg);
                }

                @Override
                public boolean enterStaticMethodDefinition(StaticMethodDefinition node, Object arg) {
                    final BlockNode parent = blockStack.isEmpty() ? res : blockStack.peek();
                    int offset = 0;
                    int length = 0;
                    if (node != null && node.position() != null) {
                        offset = node.position().startChar();
                        length = node.position().endChar() - node.position().startChar();
                    }
                    blockStack.push(parent.addBlock(node, node.name().identifier(), offset, length, "", ElementKind.METHOD));
//                    blockStack.push(parent.addBlock(node, node.name().identifier(), node.position().startChar(), node.position().endChar() - node.position().startChar(), "", ElementKind.METHOD));
                    return super.enterStaticMethodDefinition(node, arg);
                }

                @Override
                public Object exitStaticMethodDefinition(StaticMethodDefinition node, Object arg) {
                    blockStack.pop();
                    return super.exitStaticMethodDefinition(node, arg);
                }

                @Override
                public boolean enterFieldAssign(FieldAssign node, Object arg) {
                    final BlockNode parent = blockStack.isEmpty() ? res : blockStack.peek();
                    int offset = 0;
                    int length = 0;
                    if (node != null && node.position() != null) {
                        offset = node.position().startChar();
                        length = node.position().endChar() - node.position().startChar();
                    }
                    blockStack.push(parent.addBlock(node, node.name().identifier(), offset, length, "", ElementKind.FIELD));
//                    blockStack.push(parent.addBlock(node, node.name().identifier(), node.position().startChar(), node.position().endChar() - node.position().startChar(), "", ElementKind.FIELD));
                    return super.enterFieldAssign(node, arg);
                }

                @Override
                public Object exitFieldAssign(FieldAssign node, Object arg) {
                    blockStack.pop();
                    return super.exitFieldAssign(node, arg);
                }

                @Override
                public boolean enterConstantAssign(ConstantAssign node, Object arg) {
                    final BlockNode parent = blockStack.isEmpty() ? res : blockStack.peek();
                    int offset = 0;
                    int length = 0;
                    if (node != null && node.position() != null) {
                        offset = node.position().startChar();
                        length = node.position().endChar() - node.position().startChar();
                    }
                    blockStack.push(parent.addBlock(node, node.name().identifier(), offset, length, "", ElementKind.CONSTANT));
//                    blockStack.push(parent.addBlock(node, node.name().identifier(), node.position().startChar(), node.position().endChar() - node.position().startChar(), "", ElementKind.CONSTANT));
                    return super.enterConstantAssign(node, arg);
                }

                @Override
                public Object exitConstantAssign(ConstantAssign node, Object arg) {
                    blockStack.pop();
                    return super.exitConstantAssign(node, arg);
                }

                @Override
                public boolean enterMacroDefinition(MacroDefinition node, Object arg) {
                    final BlockNode parent = blockStack.isEmpty() ? res : blockStack.peek();
                    int offset = 0;
                    int length = 0;
                    if (node != null && node.position() != null) {
                        offset = node.position().startChar();
                        length = node.position().endChar() - node.position().startChar();
                    }
                    blockStack.push(parent.addBlock(node, node.name().identifier(), offset, length, "", ElementKind.METHOD));
//                    blockStack.push(parent.addBlock(node, node.name().identifier(), node.position().startChar(), node.position().endChar() - node.position().startChar(), "", ElementKind.METHOD));
                    return super.enterMacroDefinition(node, arg);
                }

                @Override
                public Object exitMacroDefinition(MacroDefinition node, Object arg) {
                    blockStack.pop();
                    return super.exitMacroDefinition(node, arg);
                }

                @Override
                public boolean enterCall(Call node, Object arg) {
                    final String identifier = node.name().identifier();
                    if (macroNames.contains(identifier)) {
                        int offset = 0;
                        int length = 0;
                        if (node != null && node.position() != null) {
                            offset = node.position().startChar();
                            length = node.position().endChar() - node.position().startChar();
                        }
                        blockStack.push(res.addMacro(node, node.name().identifier(), offset, length, "", ElementKind.CALL));
//                        blockStack.push(res.addMacro(node, identifier, node.position().startChar(), node.position().endChar() - node.position().startChar(), "", ElementKind.CALL));
                    } else {
                        res.macroBlock = false;
                    }
                    return super.enterCall(node, arg);
                }

                @Override
                public Object exitCall(Call node, Object arg) {
                    final String identifier = node.name().identifier();
                    if (macroNames.contains(identifier)) {
                        blockStack.pop();
                    }
                    return super.exitCall(node, arg);
                }

                @Override
                public boolean enterFunctionalCall(FunctionalCall node, Object arg) {
                    final String identifier = node.name().identifier();
                    if (macroNames.contains(identifier)) {
                        int offset = 0;
                        int length = 0;
                        if (node != null && node.position() != null) {
                            offset = node.position().startChar();
                            length = node.position().endChar() - node.position().startChar();
                        }
                        blockStack.push(res.addMacro(node, node.name().identifier(), offset, length, "", ElementKind.CALL));
//                        blockStack.push(res.addMacro(node, identifier, node.position().startChar(), node.position().endChar() - node.position().startChar(), "", ElementKind.CALL));
                    } else {
                        res.macroBlock = false;
                    }
                    return super.enterFunctionalCall(node, arg);
                }

                @Override
                public Object exitFunctionalCall(FunctionalCall node, Object arg) {
                    final String identifier = node.name().identifier();
                    if (macroNames.contains(identifier)) {
                        blockStack.pop();
                    }
                    return super.exitFunctionalCall(node, arg);
                }
            }, null);
        }
        final Document doc = res.getSnapshot().getSource().getDocument(false);
        if ( doc == null ) 
        {
//            LOG.info(this,"doc = null file="+res.getSnapshot().getSource().getFileObject().getPath());
            return;
        }
        final DocumentQuery dq = new DocumentQuery(doc);
        if ( dq == null )
        {
//            LOG.info(this,"dq = null file="+res.getSnapshot().getSource().getFileObject().getPath());
            return;
        }
        final TokenSequence<MirahTokenId> seq = dq.getTokens(0, false);
        final MirahTokenId tComment = MirahTokenId.get(Tokens.tComment);
        final MirahTokenId tNL = MirahTokenId.get(Tokens.tNL);
        do {
            final MirahTokenId tokenId = seq.token().id();
            if (!tokenId.equals(tComment)) {
                if (!tokenId.equals(tNL)) {
//                    res.commentsBlock = false;
                }
                continue;
            }
            // К сожалению, это не работает
            // boolean inJavadoc = seq.token().id().ordinal() == Tokens.tJavaDoc.ordinal();
            String comment = seq.token().text().toString().trim();
            if (comment.startsWith("/*") && comment.endsWith("*??")) {
                res.addBlockComment(null, "BlockComment", seq.offset(), seq.token().length(), "", ElementKind.DB);
            } else if (comment.startsWith("#")) {
                res.addLineComment(null, "LineComment", seq.offset(), seq.token().length(), "", ElementKind.TAG);
            }
        } while (seq.moveNext());
        */
        
    }

    public void appendClassPath( ClassPath cp, StringBuffer sb, HashMap<Entry,Entry> map )
    {
        for( Entry e : cp.entries() )
        if ( !map.containsKey(e) ) {
            map.put(e, null);
            if (sb.length() != 0) sb.append(File.pathSeparator);
            File f = FileUtil.archiveOrDirForURL(e.getURL());
            sb.append(f.getAbsolutePath());
        }
    }
    
    public void reparse(Snapshot snapshot, String content) throws ParseException {

        this.snapshot = snapshot;
        diag = new MirahParseDiagnostics();
        NBMirahParserResult parserResult = new NBMirahParserResult(snapshot, diag);
        result = parserResult;
        getBlocks(parserResult, content);
        
//        LOG.info(this,"REPARSE:\n"+content+"\n");
        WLMirahCompiler compiler = new WLMirahCompiler();

        compiler.setPrecompileJavaStubs(false);

        FileObject src = snapshot.getSource().getFileObject();
        Project project = FileOwnerQuery.getOwner(src);
//        LOG.info(this,"reparse project = " + project);

        FileObject projectDirectory = project.getProjectDirectory();
        FileObject buildDir = projectDirectory.getFileObject("build");
        Preferences projPrefs = ProjectUtils.getPreferences(project, MirahExtenderImplementation.class, true);
        String projectType = projPrefs.get("project_type", "unknown");
//        LOG.info(this,"reparse project type is "+projectType);
        if ("maven".equals(projectType)) {
            try {
                // It's a maven project so we want to build our sources to a different location
                FileObject cacheDir = ProjectUtils.getCacheDirectory(project, MirahExtenderImplementation.class);
                buildDir = cacheDir.getFileObject("build");
                if (buildDir == null) {
                    buildDir = cacheDir.createFolder("build");
                }
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        
        ClassPath compileClassPath = ClassPath.getClassPath(src, ClassPath.COMPILE);
        ClassPath buildClassPath = ClassPath.getClassPath(src, ClassPath.EXECUTE);
        ClassPath srcClassPath = ClassPath.getClassPath(src, ClassPath.SOURCE);
        ClassPath bootClassPath = ClassPath.getClassPath(src, ClassPath.BOOT);

        // для скриптов
        if ( compileClassPath == null ) 
            compileClassPath = ClassPath.getClassPath(projectDirectory, ClassPath.COMPILE);
        if ( buildClassPath == null )
            buildClassPath = ClassPath.getClassPath(projectDirectory, ClassPath.EXECUTE);
        if ( srcClassPath == null )
            srcClassPath = ClassPath.getClassPath(projectDirectory, ClassPath.SOURCE);
        if ( bootClassPath == null )
            bootClassPath = ClassPath.getClassPath(projectDirectory, ClassPath.BOOT);
//        LOG.info(this, "classPath:");
//        LOG.info(this, compileClassPath.toString());
//        LOG.info(this, "buildClassPath:");
//        LOG.info(this, buildClassPath.toString());
//        LOG.info(this, "srcClassPath:");
//        LOG.info(this, srcClassPath.toString());
//        LOG.info(this, "bootClassPath:");
//        LOG.info(this, bootClassPath.toString());
        
//        String changedSourcePaths = RecompileQueue.getProjectQueue(project).getAndClearChangedSourcePaths();
//        if (changedSourcePaths != null) {
//            Set<String> set = new HashSet<>();
//            set.addAll(Arrays.asList(changedSourcePaths.split(Pattern.quote(File.pathSeparator))));
//            set.addAll(Arrays.asList(srcClassPathStr.split(Pattern.quote(File.pathSeparator))));
//            StringBuilder sb = new StringBuilder();
//            for (String p : set) {
//                sb.append(p).append(File.pathSeparator);
//            }
//            srcClassPathStr = sb.substring(0, sb.length() - File.pathSeparator.length());
//        }
        compiler.setSourcePath(srcClassPath == null ? "" : srcClassPath.toString());

        String dest = buildClassPath == null ? "" : buildClassPath.toString();
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
        compiler.setDestinationDirectory(new File(dest));
        compiler.setDiagnostics(diag);
        
        //todo убрать повторы в classpath
        // не надо приклеивать macroPath - это делается в WLMirahCompiler
        HashMap<Entry, Entry> map = new HashMap<Entry, Entry>();
        StringBuffer sb = new StringBuffer();
        appendClassPath(compileClassPath,sb,map);
        appendClassPath(buildClassPath,sb,map);
        appendClassPath(bootClassPath,sb,map);
//        LOG.info(this, "cp:");
//        LOG.info(this, sb.toString());

        String cp = sb.toString();
        
        compiler.setClassPath(cp);
//        compiler.setClassPath(macroPath + File.pathSeparator + cp);

        compiler.setMacroClassPath("");
//        compiler.setMacroClassPath(macroPath);
        
        DocumentDebugger debugger = new DocumentDebugger();

        compiler.setDebugger(debugger);

        compiler.setBootClassPath(cp);
        String srcText = content;
        FileObject fakeFileRoot = getRoot(src);
        if ( fakeFileRoot == null )
        {
//            LOG.info(this,"fakeFileRoot == NULL for src = " + src);
            return;
        }
        String relPath = FileUtil.getRelativePath(fakeFileRoot, src);
        relPath = relPath.substring(0, relPath.lastIndexOf("."));
        compiler.addFakeFile(relPath, srcText);
        FileChangeAdapter fileChangeListener = null;
        try {
            if (compileClassPath != null) {
                compiler.compile(new String[]{"--new-closures"});
            }
            if (mirahDir != null) {

                for (FileObject compileRoot : compileClassPath.getRoots()) {
                    if (!compileRoot.getPath().endsWith(".jar") && compileRoot.isFolder() && !mirahDir.equals(compileRoot)) {
                        copyIfChanged(new File(mirahDir.getPath()), new File(compileRoot.getPath()), new File(mirahDir.getPath()));
                    }
                }
            }
            if ("maven".equals(projectType)){
                // If its a maven project, we need to copy the build files into 
                FileObject libDir = projectDirectory.getFileObject("lib");
                if (libDir == null) {
                    libDir = projectDirectory.createFolder("lib");
                }
                FileObject mirahTmpClassesDir = libDir.getFileObject("mirah-tmp-classes");
                if ( mirahTmpClassesDir == null ){
                    mirahTmpClassesDir = libDir.createFolder("mirah-tmp-classes");
                }
                File jarFile = new File(FileUtil.toFile(libDir), "mirah-tmp-classes.jar");
                FileUtils.copyDirectoryStructureIfModified(new File(dest), FileUtil.toFile(mirahTmpClassesDir));
                createJar(FileUtil.toFile(mirahTmpClassesDir), FileUtil.toFile(mirahTmpClassesDir).getPath(), jarFile);
            }
        } catch (Exception ex) {
            LOG.info(this,"REPARSE ex = "+ex);
//            ex.printStackTrace();
        }
        Document doc = snapshot.getSource().getDocument(true);
        if ( doc == null ) //todo РЎР‚Р В°Р В·Р С•Р В±РЎР‚Р В°РЎвЂљРЎРЉРЎРѓРЎРЏ?
        {
//            LOG.info(this,"doc = null file="+snapshot.getSource().getFileObject().getPath());
            return;
        }
        if ( debugger != null && result != null )
        {
            // сохраняю карту распознанныч типов
            result.setResolvedTypes(debugger.resolvedTypes);
        }

        if (debugger.resolvedTypes.size() > 0) {
            debugger.compiler = compiler.getMirahc();
            documentDebuggers.put(doc, debugger);
            // Сохраняю дерево разбора - это список List<Node>
            if ( debugger.compiler.compiler() != null )
            result.setParsedNodes(debugger.compiler.compiler().getParsedNodes());
            fireOnParse(doc);
        }
    }

    private FileObject getRoot(FileObject file) {
        Project project = FileOwnerQuery.getOwner(file);
        Sources sources = ProjectUtils.getSources(project);
        for (SourceGroup sourceGroup : sources.getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA)) {
            FileObject root = sourceGroup.getRootFolder();
            if (FileUtil.isParentOf(root, file) || root.equals(file)) {
                    return root;
            }
        }
        ClassPath path = ClassPath.getClassPath(file, ClassPath.SOURCE);
        return path != null ? path.findOwnerRoot(file) : null;
    }

    @Override
    public Result getResult(Task task) {
        if (result != null) {
            return result;
        }
        if (snapshot == null) {
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

    public static class NBMirahParserResult extends ParserResult /*implements BlockNode*/ {

        private final MirahParseDiagnostics diagnostics;
        private boolean valid = true;
        private BlockCollector blockCollector = null;
        List<Error> errorList = new ArrayList<>();
//        List<Block> blockList = new ArrayList<>();
//        List<Block> importList = new ArrayList<>();
//        List<Block> dslList = new ArrayList<>();
//        List<Block> lineComments = new ArrayList<>();
//        List<Block> blockComments = new ArrayList<>();
//        List<Block> macroList = new ArrayList<>();
//        boolean importsBlock = false;
//        boolean commentsBlock = false;
//        boolean macroBlock = false;

        Node rootNode;
        List parsedNodes;
        HashMap<Node, ResolvedType> resolvedTypes = null;

        NBMirahParserResult(Snapshot snapshot, MirahParseDiagnostics diagnostics) {
            super(snapshot);
            this.diagnostics = diagnostics;
        }

        public MirahParseDiagnostics getMirahDiagnostics() {
            return diagnostics;
        }

        @Override
        public List<? extends Error> getDiagnostics() {
            if ( errorList.isEmpty() ) {
                for( MirahParseDiagnostics.SyntaxError syntaxError : this.diagnostics.errors )
                {
//                    addError(syntaxError.message,(int)syntaxError.start,(int)(syntaxError.end - syntaxError.start));
                }
            }
            return errorList; //new ArrayList<>();
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

        public synchronized BlockCollector getBlockCollection() {
            if ( blockCollector == null ) {
                blockCollector = new BlockCollector();
                blockCollector.prepareBlocks(this);
            }
            return blockCollector;
        }
        /*
        public List<Block> getBlocks() {
//            return blockList;
            return blockCollector.getBlocks();
        }

//        public void setBlocks(List<Block> blockList ) {
//            this.blockList = blockList;
//        }

        public List<Block> getImports() {
//            return importList;
            return blockCollector.getImports();
        }

        public List<Block> getDSLs() {
//            return dslList;
            return blockCollector.getDSLs();
        }

        public List<Block> getMacroses() {
//            return macroList;
            return blockCollector.getMacroses();
        }

        public List<Block> getLineComments() {
//            return lineComments;
            return blockCollector.getLineComments();
        }

        public List<Block> getBlockComments() {
//            return blockComments;s
            return blockCollector.getBlockComments();
        }
        @Override
        public Block addBlock(Node node, CharSequence function, int offset, int length, CharSequence extra, ElementKind kind) {
            importsBlock = false;
            commentsBlock = false;
            macroBlock = false;
            Block block = new Block(node, function, offset, length, extra, kind);
            blockList.add(block);
            return block;
        }

        @Override
        public Block addDSL(Node node, CharSequence function, int offset, int length, CharSequence extra, ElementKind kind) {
            importsBlock = false;
            commentsBlock = false;
            macroBlock = false;
            Block block = new Block(node, function, offset, length, extra, kind);
            dslList.add(block);
            return block;
        }

        @Override
        public Block addImport(Node node, CharSequence function, int offset, int length, CharSequence extra, ElementKind kind) {
            commentsBlock = false;
            macroBlock = false;
            if (importsBlock && !importList.isEmpty()) {
                Block block = importList.get(importList.size() - 1);
                block.length = offset + length - block.offset;
                return block;
            } else {
                importsBlock = true;
                Block block = new Block(node, function, offset, length, extra, kind);
                importList.add(block);
                return block;
            }
        }

        public Block addMacro(Node node, CharSequence function, int offset, int length, CharSequence extra, ElementKind kind) {
            commentsBlock = false;
            importsBlock = false;
            if (macroBlock && !macroList.isEmpty()) {
                Block block = macroList.get(macroList.size() - 1);
                block.length = offset + length - block.offset;
                return block;
            } else {
                macroBlock = true;
                Block block = new Block(node, function, offset, length, extra, kind);
                macroList.add(block);
                return block;
            }
        }

        public Block addLineComment(Node node, CharSequence function, int offset, int length, CharSequence extra, ElementKind kind) {
            // комментарии не прерывают блоков импорта
            if (commentsBlock && !lineComments.isEmpty()) {
                Block block = lineComments.get(lineComments.size() - 1);
                block.length = offset + length - block.offset;
                return block;
            } else {
                commentsBlock = true;
                Block block = new Block(node, function, offset, length, extra, kind);
                lineComments.add(block);
                return block;
            }
        }

        public Block addBlockComment(Node node, CharSequence function, int offset, int length, CharSequence extra, ElementKind kind) {
            // комментарии не прерывают другие блоки, в т.ч. блоки импорта и блоки макросов атрибутики
            commentsBlock = false;
            Block block = new Block(node, function, offset, length, extra, kind);
            blockComments.add(block);
            return block;
        }

        public Block addBlock(Block parent, Node node, CharSequence function, int offset, int length, CharSequence extra, ElementKind kind) {
            return parent.addBlock(node, function, offset, length, extra, kind);
        }
//        public void setRoot( Node root )
//        {
//            this.rootNode = root;
//        }
//        
        */
        public Node getRoot() {
            if ( parsedNodes == null ) return null;
            for( Object node : parsedNodes )
            {
                if ( node instanceof Node ) return (Node)node;
            }
            return null;
//            return rootNode;
        }

        public void setParsedNodes( List parsed )
        {
            this.parsedNodes = parsed;
        }
        
        public  List getParsedNodes()
        {
            return parsedNodes;
        }

        public void setResolvedTypes( HashMap<Node, ResolvedType> resolvedTypes )
        {
            this.resolvedTypes = resolvedTypes;
        }
        
        public  HashMap<Node, ResolvedType> getResolvedTypes()
        {
            return resolvedTypes;
        }
        
        public ResolvedType getResolvedType( Node node )
        {
            if ( resolvedTypes == null ) return null;
            return resolvedTypes.get(node);
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
       
    }

    public static class MirahParseDiagnostics implements DiagnosticListener {

        public static class SyntaxError {

            SyntaxError(Diagnostic.Kind k, String pos, String msg) {
                kind = k;
                position = pos;
                message = msg;
            }
            SyntaxError(Diagnostic.Kind k, long start, long end, long line, String msg) {
                kind = k;
                position = null;
                message = msg;
                this.start = start;
                this.end = end;
                this.line = line;
            }
            public Diagnostic.Kind kind;
            public String position;
            public String message;
            public long start;
            public long end;
            public long line;
        }

        private List<SyntaxError> errors = new ArrayList<>();
        private int errorCount = 0;

        public int errorCount() {
            return errorCount;
        }

        public List<SyntaxError> getErrors() {
            return errors;
        }
        
        @Override
        public void report(Diagnostic dgnstc) {
            String message = dgnstc.getMessage(Locale.getDefault());
            errors.add(new SyntaxError(dgnstc.getKind(), dgnstc.getStartPosition(), dgnstc.getEndPosition(), dgnstc.getLineNumber(), message));
            if ( dgnstc.getKind() == Diagnostic.Kind.ERROR ) errorCount++;
        }        
    }

    public static class DocumentDebugger implements DebuggerInterface {

        public static class PositionType {

            public int startPos, endPos;
            public ResolvedType type;
            public Node node;

            @Override
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
        final private HashMap<Node, ResolvedType> resolvedTypes = new HashMap<>();

        public Mirahc compiler;

        public DocumentDebugger() {
            leftEdges = new TreeSet<>(new Comparator<PositionType>() {

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
            rightEdges = new TreeSet<>(new Comparator<PositionType>() {

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

        public HashMap<Node, ResolvedType> getResolvedTypes() {
            return resolvedTypes;
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
            tf.onUpdate(new TypeListener() {

                @Override
                public void updated(TypeFuture tf, ResolvedType rt) {
                    if (!tf.isResolved()) {
                        return;
                    }

                    if (node.position() == null) {
                        return;
                    }
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
        if (n == null) {
            return "";
        }
        if (n.position() == null) {
            return "" + n;
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
                if (jos != null) {
                    jos.close();
                }
            } catch (Throwable t) {
                // eat it
            }
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (Throwable t) {
                // eat it
            }
        }
    }

    private void addToJar(File source, String sourceRoot, JarOutputStream jos) throws IOException {
        if (source.getName().endsWith(".class")) {
            String fileName = formatEntry(source, sourceRoot, false);
            ZipEntry entry = new ZipEntry(fileName);
            jos.putNextEntry(entry);
            InputStream fis = null;
            try {
                fis = new FileInputStream(source);
                byte[] buf = new byte[4096];
                int len;
                while ((len = fis.read(buf)) != -1) {
                    jos.write(buf, 0, len);
                }
                jos.closeEntry();
            } finally {
                try {
                    if (fis != null) {
                        fis.close();
                    }
                } catch (Exception ex) {
                    // eat it
                }
            }
        } else if (source.isDirectory()) {
            String dirName = formatEntry(source, sourceRoot, true);
            for (File child : source.listFiles()) {
                addToJar(child, sourceRoot, jos);
            }
            //jos.closeEntry();
        }
    }

    private String formatEntry(File f, String sourceRoot, boolean directory) {
        if (directory) {
            String name = f.getPath().substring(sourceRoot.length());
            name = name.replace("\\", "/");
            if (!name.endsWith("/")) {
                name += "/";
            }
            if (name.startsWith("/")) {
                name = name.substring(1);
            }
            return name;
        } else {
            String name = f.getPath().substring(sourceRoot.length());
            name = name.replace("\\", "/");
            if (name.startsWith("/")) {
                name = name.substring(1);
            }
            return name;
        }
    }
}
