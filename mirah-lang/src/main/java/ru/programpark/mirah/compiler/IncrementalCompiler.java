package ru.programpark.mirah.compiler;

import mirah.impl.MirahParser;
import mirah.lang.ast.*;
import org.mirah.IsolatedResourceLoader;
import org.mirah.MirahClassLoader;
import org.mirah.jvm.compiler.Backend;
import org.mirah.jvm.compiler.ExtensionCleanup;
import org.mirah.jvm.compiler.JvmVersion;
import org.mirah.jvm.mirrors.*;
import org.mirah.jvm.mirrors.debug.DebuggerInterface;
import org.mirah.macros.JvmBackend;
import org.mirah.mmeta.SyntaxError;
import org.mirah.tool.MirahArguments;
import org.mirah.tool.MirahCompiler;
import org.mirah.tool.Mirahc;
import org.mirah.typer.*;
import org.mirah.util.AstChecker;
import org.mirah.util.Context;
import org.mirah.util.SimpleDiagnostics;

import javax.tools.DiagnosticListener;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Created by kozyr on 04.03.2016.
 */
public class IncrementalCompiler {

    private static final Logger logger = Logger.getLogger(IncrementalCompiler.class.getName());
    private final SafeTyper typer;
    private final Backend backend;
    private final MirahParser parser;
    private final Context context;
    private final List<Node> asts;
    private final Backend macro_backend;
    private final Context macro_context;
    private final SafeTyper macro_typer;
    private String sourcePath;
    private String classPath;
    //    private DebuggerInterface debugger;
    private String content;
    private String path;
    private HashMap<String, Class> extension_classes;
    private MirrorTypeSystem macro_types;
    private MirrorTypeSystem types;

    public IncrementalCompiler() {
        //thisTypeInferenceListener debugger = new TypeInferenceListener();
//        @diagnostics = diagnostics
        MirahArguments compiler_args = new MirahArguments();
        compiler_args.applyArgs(new String[0]);

        JvmVersion jvm = compiler_args.jvm_version();
        ErrorCountListener error_listener = new ErrorCountListener();
        CachedJvmBackend cachedJvmBackend = new CachedJvmBackend(this);
        DebuggerInterface debugger = null;

        context = createContext(compiler_args, jvm, error_listener, cachedJvmBackend, debugger);
        macro_context = createContext(compiler_args, jvm, error_listener, cachedJvmBackend, debugger);

        // The main type system needs access to the macro one to call macros.
        context.add(Context.class, macro_context);

        createTypeSystems();

        // TODO allow this. ambiguous for parser?
        SimpleScoper scoper = new SimpleScoper(new BetterScopeFactory());
        context.add(Scoper.class, scoper);
        macro_context.add(Scoper.class, scoper);

        parser = new MirahParser();
        context.add(MirahParser.class, parser);
        macro_context.add(MirahParser.class, parser);

        macro_typer = createTyper(macro_context);
        typer = createTyper(context);

        // Make sure macros are compiled using the correct type system.
        typer.macro_compiler_set(macro_typer.macro_compiler());

        // Ugh -- we have to use separate type systems for compiling and loading
        // macros.
        typer.macro_compiler().setMacroLoader(typer);

        backend = new Backend(context);
        macro_backend = new Backend(macro_context);
        asts = new LinkedList<Node>();
    }

    private static Context createContext(MirahArguments compiler_args, JvmVersion jvm, ErrorCountListener error_listener, CachedJvmBackend cachedJvmBackend, DebuggerInterface debugger) {
        Context macro_context = new Context();
        macro_context.add(MirahArguments.class, compiler_args);
        macro_context.add(JvmBackend.class, cachedJvmBackend);
        macro_context.add(DiagnosticListener.class, error_listener);
        macro_context.add(SimpleDiagnostics.class, error_listener);
        macro_context.add(JvmVersion.class, jvm);
        macro_context.add(DebuggerInterface.class, debugger);
        return macro_context;
    }

    private static SafeTyper createTyper(org.mirah.util.Context context) {
        SafeTyper safeTyper = new SafeTyper(context,
                fromContext(context, TypeSystem.class),
                fromContext(context, SimpleScoper.class),
                fromContext(context, JvmBackend.class),
                fromContext(context, MirahParser.class)
        );
        context.add(Typer.class, safeTyper);
        return safeTyper;
    }

    private static <T> T fromContext(Context macro_context, Class<T> clazz) {
        return (T) macro_context.get(clazz);
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public void setClassPath(String classPath) {
        this.classPath = classPath;
    }

    public List<Node> getParsedNodes() {
        return Collections.EMPTY_LIST;
    }

    public void compile() {

    }

    public HashMap<Node, ResolvedType> getResolvedTypes() {
//        TODO implement
        return null;
    }

    public Node parse(CodeSource code) {
        try {
            return (Node) parser.parse(code);
        } catch (SyntaxError ex) {
            return new ErrorNode(makePosition(code, ex), ex.getMessage());
        }
    }

    private Position makePosition(CodeSource source, SyntaxError ex) {
        return new PositionImpl(source, ex.getOffset(), ex.getLine(), ex.getColumn(), ex.getOffset(), ex.getLine(), ex.getColumn());
    }

    public void infer(Node... nodes) {
        for (int i = 0; i < nodes.length; i++) {
            Node node = nodes[i];
            if (logger.isLoggable(Level.FINE))
                logger.log(Level.FINE, "node added to scope; " + node.position());
            asts.add(node);
            CompilerUtil.logAst(node);
            AstChecker.maybe_check(node);
            typer.infer(node, false);
            AstChecker.maybe_check(node);
        }
    }

    public BetterClosureBuilder finish() {
        return typer.finish_closures();
    }

    public void clean() {
        for (Node node : asts) {
            backend.clean((Script) node, null);
            node.accept(new ExtensionCleanup(macro_backend,
                            extension_classes,
                            null,
                            macro_typer),
                    new HashMap());
            processInferenceErrors(node, context);
        }

        failIfErrors();

    }

    public ClassLoaderResourceLoader createMacroLoader(URL[] macrocp) {
        ClassResourceLoader bootloader = new ClassResourceLoader(System.class);
        return new ClassLoaderResourceLoader(new IsolatedResourceLoader(macrocp), new FilteredResources(new ClassResourceLoader(Mirahc.class), Pattern.compile("^/?(mirah/|org/mirah)"), bootloader));
    }

    public ClassLoaderResourceLoader createClassLoader(URL[] classpath, URL[] bootcp) {
        Object boot = bootcp != null ? new ClassLoaderResourceLoader(new IsolatedResourceLoader(bootcp)) : new NegativeFilteredResources(new ClassResourceLoader(System.class), Pattern.compile("^/?(mirah/|org/mirah|org/jruby)"));
        FilteredResources bootloader = new FilteredResources(new ClassResourceLoader(Mirahc.class), Pattern.compile("^/?org/mirah/jvm/(types/(Flags|Member|Modifiers))|compiler/Cleaned"), (ResourceLoader) boot);
        return new ClassLoaderResourceLoader(new IsolatedResourceLoader(classpath), bootloader);
    }


    public void createTypeSystems() {
        // Construct a loader with the standard Java classes plus the classpath
        URL[] classpath = new URL[0];
        URL[] bootcp = new URL[0];
        ClassLoaderResourceLoader classloader = createClassLoader(classpath, bootcp);

        // Now one for macros: These will be loaded into this JVM,
        // so we don't support bootclasspath.
        URL[] macrocp = classpath;

        ClassLoaderResourceLoader macroloader = createMacroLoader(macrocp);

        URLClassLoader macro_class_loader = new URLClassLoader(macrocp, MirahCompiler.class.getClassLoader());

        context.add(ClassLoader.class, macro_class_loader);
        macro_context.add(ClassLoader.class, macro_class_loader);

        extension_classes = new HashMap<String, Class>();
        URLClassLoader extension_parent = new URLClassLoader(macrocp, Mirahc.class.getClassLoader());
        MirahClassLoader extension_loader = new MirahClassLoader(extension_parent, extension_classes);

        macro_context.add(TypeSystem.class, macro_types = new MirrorTypeSystem(macro_context, macroloader));
        context.add(TypeSystem.class, types = new MirrorTypeSystem(context, classloader));
    }

}



