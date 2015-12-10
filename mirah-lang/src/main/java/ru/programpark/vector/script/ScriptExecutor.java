package ru.programpark.vector.script;

import ca.weblite.asm.WLMirahCompiler;
import ca.weblite.netbeans.mirah.lexer.MirahParser;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Writer;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import javax.swing.text.Document;
import javax.tools.Diagnostic;
import mirah.lang.ast.StringCodeSource;
import org.mirah.jvm.compiler.BytecodeConsumer;
import org.mirah.jvm.compiler.JvmVersion;
import org.mirah.jvm.mirrors.debug.ConsoleDebugger;
import org.mirah.jvm.mirrors.debug.DebugController;
import org.mirah.tool.MirahArguments;
import org.mirah.tool.MirahCompiler;
import org.mirah.util.SimpleDiagnostics;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.ClassPath.Entry;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.text.Line;
import org.openide.util.Exceptions;
import org.openide.windows.InputOutput;
import org.openide.windows.OutputEvent;
import org.openide.windows.OutputListener;

class WriterOutputStream extends OutputStream {

    private final Writer writer;

    public WriterOutputStream(Writer writer) {
        this.writer = writer;
    }

    public void write(int b) throws IOException {
        // It's tempting to use writer.write((char) b), but that may get the encoding wrong
        // This is inefficient, but it works
        write(new byte[]{(byte) b}, 0, 1);
    }

    public void write(byte b[], int off, int len) throws IOException {
        writer.write(new String(b, off, len));
    }

    public void flush() throws IOException {
        writer.flush();
    }

    public void close() throws IOException {
        writer.close();
    }
}

final class ErrorHyperlink implements OutputListener {

    private final int offset;
    private final FileObject fileObj;

    public ErrorHyperlink(FileObject fileObj, int offset) {
        this.offset = offset;
        this.fileObj = fileObj;
    }

    @Override
    public void outputLineSelected(OutputEvent ev) {
        goToLine(false);
    }

    @Override
    public void outputLineCleared(OutputEvent ev) {
    }

    @Override
    public void outputLineAction(OutputEvent ev) {
        goToLine(true);
    }

    @SuppressWarnings("deprecation")
    private void goToLine(boolean focus) {
        Line xline = null;
        try {
            DataObject dobj = DataObject.find(fileObj);

            EditorCookie cookie = dobj.getCookie(EditorCookie.class);
            Document doc = cookie.openDocument();
            xline = NbEditorUtilities.getLine(doc, offset, false);
            if (cookie == null) {
                throw new java.io.FileNotFoundException();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (xline != null) {
            xline.show(Line.SHOW_GOTO);
        }

    }
}

class ErrorsList extends SimpleDiagnostics {
    public ArrayList<Diagnostic> errors = new ArrayList <Diagnostic>();
    
    public ErrorsList() {
        super(false);
    }
    @Override
    public void report(Diagnostic dgnstc) {
        errors.add(dgnstc);
    }
    @Override
    public int errorCount() {
        return errors.size();
    }
    public ArrayList<Diagnostic> list() {
        return errors;
    }
}

public class ScriptExecutor implements BytecodeConsumer {

    InputOutput console;

    public ScriptExecutor(InputOutput console) {
        this.console = console;
    }
    public void runScript(FileObject fo)
    {
        buildFolder = fo.getParent().getPath() + "/" + fo.getName() + ".classes";
        loader = new ByteClassLoader(this.getClass().getClassLoader());
//        loader = new ByteClassLoader(this.getClass().getClassLoader(),buildFolder);
        console.getOut().println("Start script execution!"+Thread.currentThread());

        long start = System.currentTimeMillis();
        
        PrintStream ps = null;
        try
        {
            if ( ! compileScript(fo) ) return;
            
            loader.loadAllClasses(buildFolder);
            
            Class cls = loader.findClass(fo.getName());
            if ( cls == null ) {
                console.getOut().println("class \'"+fo.getName()+"\' not found");
                return;
            }
            Script script = (Script)cls.newInstance();
            ps = System.out;
            System.setOut( new PrintStream(new WriterOutputStream(console.getOut())));
            script.run();
            script = null;
            console.getOut().println("Время выполнения: " + (System.currentTimeMillis() - start) + " мс");
        } 
        catch(Exception ex) {
            if ( ex instanceof InterruptedException )
                console.getOut().println("\nПрервано пользователем");
            else
                console.getOut().println("unable to eval: " + ex);
            ex.printStackTrace();
        }
        finally {
            if ( ps != null ) System.setOut(ps);
            loader.close();
            loader = null;
        }

    }
    
    private ArrayList<URL> preparePath( ClassPath... paths )
    {
        ArrayList<URL> urls = new ArrayList<URL>();
        for( ClassPath cp : paths) {
            if ( cp != null )
            {
                for( Entry e : cp.entries() )
                {
                    urls.add(e.getURL());
                }
            }
        }
        return urls;
//        return urls.toArray(new URL[urls.size()]);
    }
    
    private boolean compileScript( FileObject fo ) throws IOException, URISyntaxException 
    {
        long start = System.currentTimeMillis();
        
        /*
        WLMirahCompiler compiler = new WLMirahCompiler();
        compiler.setPrecompileJavaStubs(false);
        */
        ClassPath compileCP = ClassPath.getClassPath(fo, ClassPath.COMPILE);
        ClassPath buildCP = ClassPath.getClassPath(fo, ClassPath.EXECUTE);
        ClassPath sourceCP = ClassPath.getClassPath(fo, ClassPath.SOURCE);
        ClassPath bootCP = ClassPath.getClassPath(fo, ClassPath.BOOT);

        Project project = FileOwnerQuery.getOwner(fo);
        if (project == null) return false;

        FileObject root = project.getProjectDirectory();
        compileCP = ClassPath.getClassPath(root, ClassPath.COMPILE);
        buildCP = ClassPath.getClassPath(root, ClassPath.EXECUTE);
        sourceCP = ClassPath.getClassPath(root, ClassPath.SOURCE);
        bootCP = ClassPath.getClassPath(root, ClassPath.BOOT);

        StringBuffer sb = new StringBuffer();
        if ( compileCP != null ) sb.append(compileCP.toString()).append(";");
        if ( buildCP != null ) sb.append(buildCP.toString()).append(";");
        if ( bootCP != null ) sb.append(bootCP.toString()).append(";");

        FileObject scriptDir = fo.getParent();

        sb.append(scriptDir.getPath()).append(";");

        // добавить ссылка на базовый интерфейс - он, возможно, в другом классе
        URL url = this.getClass().getResource("Script.class");
        if ( url != null ) sb.append(url.toURI().getPath());
            
        String cp = sb.toString();
        StringBuffer sbt = new StringBuffer();
        sbt.append("import ru.programpark.vector.script.Script\n");
        sbt.append("public class ").append(fo.getName()).append("\n");
        sbt.append("\timplements Script\n\n\tdef run:void\n");
        sbt.append(fo.asText());
        sbt.append("\n\tend\nend\n");
        String ss = sbt.toString();
        
        console.getOut().println(ss);
        MirahArguments compiler_args = new MirahArguments();
        ErrorsList errors = new ErrorsList();

        compiler_args.bootclasspath_set(bootCP.toString());
        compiler_args.classpath_set(cp);
        compiler_args.macroclasspath_set("");
        compiler_args.diagnostics_set(errors);
        compiler_args.destination_set(null); //buildFolder);
        compiler_args.setup_logging();

//        ArrayList<URL> cp_urls = preparePath(compileCP, buildCP, bootCP);
//        cp_urls.add(url);
//        ArrayList<URL> boot_urls = preparePath(bootCP);
        MirahCompiler compiler = new MirahCompiler(errors, compiler_args, null);
        
        try {
            compiler.parse(new StringCodeSource(fo.getName(), ss));
            compiler.infer();
            compiler.compile(this);
            List list = compiler.getParsedNodes();
            
        } catch (Throwable ex) {
            System.out.println("Unable to compile: " + ex);
            ex.printStackTrace();
        }
        for (Diagnostic err : errors.list()) {
            console.getOut().println("" + err.getKind() + ": " + err.getMessage(Locale.getDefault()), new ErrorHyperlink(fo, (int) err.getPosition()));
        }
        console.getOut().println("Время компиляции: " + (System.currentTimeMillis() - start) + " мс");
        
        return errors.list().isEmpty();
    }
    
    public void eval(String code) 
    {
        try {
            gen = gen + 1;
            String scriptName = (new StringBuilder()).append(SCRIPT_PREFIX).append(gen).toString();
            code = (new StringBuilder()).append("class ").append(scriptName).append(" implements dsl.Script\n             def eval\n               ").append(code).append("\n             end\n        end").toString();
//            if (call_compile(code) == 0) {
//                ((Script) loader.findClass(scriptName).newInstance()).run();
//                ((Script) loader.findClass(scriptName).newInstance()).run();
//            }
        } catch (Throwable ex) {
            System.out.println("unable to eval: "+ex);
            ex.printStackTrace();
        }
    }

    public void compile(String code) {
        call_compile(code);
    }

    public int call_compile(String code) {
        return 0;
    }

    public void consumeClass(String filename, byte bytes[]) {
        try {
            if (!loader.contains(filename)) {
                System.out.println("defining class: "+filename);
                loader.add(filename, bytes);
            }
        } catch (Throwable e) {
            System.out.println("Unable to eval: "+e);
            e.printStackTrace();
        }
    }

    private int gen;
    private static String SCRIPT_PREFIX = "Gen_S";
    private ByteClassLoader loader;
    private DebugController debugger;
//    private MirahCompiler compiler;
//    private MirahArguments compiler_args;
    
    private String buildFolder;
}
