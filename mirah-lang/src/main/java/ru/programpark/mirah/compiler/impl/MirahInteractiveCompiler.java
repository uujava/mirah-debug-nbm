package ru.programpark.mirah.compiler.impl;

import mirah.impl.MirahParser;
import mirah.lang.ast.*;
import org.mirah.jvm.compiler.Backend;
import org.mirah.jvm.compiler.ExtensionCleanup;
import org.mirah.jvm.compiler.JvmVersion;
import org.mirah.jvm.compiler.MacroConsumer;
import org.mirah.jvm.mirrors.*;
import org.mirah.macros.JvmBackend;
import org.mirah.mmeta.SyntaxError;
import org.mirah.tool.ErrorCollector;
import org.mirah.tool.MirahArguments;
import org.mirah.tool.MirahCompiler;
import org.mirah.tool.Mirahc;
import org.mirah.typer.*;
import org.mirah.util.AstChecker;
import org.mirah.util.Context;
import org.mirah.util.ErrorCounter;
import org.mirah.util.MirahDiagnostic;
import ru.programpark.mirah.compiler.CacheConsumer;
import ru.programpark.mirah.compiler.CompilerUtil;
import ru.programpark.mirah.compiler.InteractiveCompiler;
import ru.programpark.mirah.compiler.PathMapper;
import ru.programpark.mirah.compiler.loaders.*;
import ru.programpark.mirah.compiler.stat.StatTimer;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static ru.programpark.mirah.compiler.CompilerUtil.chain;
import static ru.programpark.mirah.compiler.CompilerUtil.errorType;

/**
 * A bit more interactive compiler than MirahCompiler
 */
public class MirahInteractiveCompiler implements JvmBackend, InteractiveCompiler {

    private static final Logger logger = Logger.getLogger(MirahInteractiveCompiler.class.getName());
    public static final String MIRAH_CLASS_PATTERN = "^/?(mirah[\\./]|org[\\./]mirah[\\./])";
    private final Map<String, Node> asts = new LinkedHashMap<>();
    private final Queue<CodeSource> source_queue = new LinkedList<>();
    private final Queue<Node> node_queue = new LinkedList<>();
    private final HashSet<IndexedResourceLoader> resourceLoaders = new LinkedHashSet<>();
    private final CompiledPathMapper path_mapper;
    private SafeTyper typer;
    private Backend backend;
    private MirahParser parser;
    private Context context;
    private Backend macro_backend;
    private Context macro_context;
    private SafeTyper macro_typer;
    private ErrorCounter error_listener;
    private MacroConsumer macro_consumer;
    private Map<Node, ResolvedType> typeMap;

    public MirahInteractiveCompiler(DiagnosticListener diagnostics) {
        this.error_listener = new ErrorCounter(diagnostics);
        this.path_mapper = new CompiledPathMapper(this);
    }

    private static SafeTyper createTyper(org.mirah.util.Context context) {
        SafeTyper safeTyper = new SafeTyper(context,
                fromContext(context, TypeSystem.class),
                fromContext(context, Scoper.class),
                fromContext(context, JvmBackend.class),
                fromContext(context, MirahParser.class)
        );
        context.add(Typer.class, safeTyper);
        return safeTyper;
    }

    private static <T> T fromContext(Context macro_context, Class<T> clazz) {
        return (T) macro_context.get(clazz);
    }

    private Context createContext(MirahArguments compiler_args, JvmVersion jvm, ErrorCounter error_listener) {
        Context macro_context = new Context();
        macro_context.add(MirahArguments.class, compiler_args);
        macro_context.add(JvmBackend.class, this);
        macro_context.add(DiagnosticListener.class, error_listener);
        macro_context.add(JvmVersion.class, jvm);
        return macro_context;
    }

    @Override
    public List<Node> getParsedNodes() {
        return new LinkedList<>(asts.values());
    }

    /**
     * code could be added before start and implicitly by ResourceLoaders at infer time
     *
     * @param code
     */

    @Override
    public void add(CodeSource code) {
        source_queue.add(code); //queue processed at infer
    }

    @Override
    public boolean run(CacheConsumer cache) {
        if (cache == null) cache = new NullConsumer();
        cache.setMapper(getTypePathMapper());
        init();
        typeMap = null;
        macro_consumer = new MacroConsumer((ClassLoader) context.get(ClassLoader.class), cache);
        parse();
        infer();
        if (errorCount() > 0) return false;
        finish();
        if (errorCount() > 0) return false;
        clean();
        if (errorCount() > 0) return false;
        compile();
        if (errorCount() > 0) return false;
        backend.generate(cache);
        return errorCount() == 0;
    }

    private void parse() {
        CodeSource codeSource;
        StatTimer stat = CompilerUtil.start("ic.parse");
        while ((codeSource = source_queue.poll()) != null) {
            String name = codeSource.name();
            if (asts.get(name) != null) {
                if (logger.isLoggable(Level.WARNING))
                    logger.log(Level.WARNING, "code source already processed: " + name);
            } else {

                Node parsed = parse(codeSource);
                node_queue.add(parsed);
                asts.put(name, parsed);
            }
        }
        stat.stop();
    }

    private void init() {
        StatTimer stat = CompilerUtil.start("ic.init");
        MirahArguments compiler_args = new MirahArguments();
        compiler_args.applyArgs(new String[0]);

        JvmVersion jvm = compiler_args.jvm_version();

        context = createContext(compiler_args, jvm, error_listener);
        macro_context = createContext(compiler_args, jvm, error_listener);

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
        stat.stop();
    }

    private void compile() {
        for (Node ast : asts.values()) {
            StatTimer stat = CompilerUtil.start("ic.generate");
            if (ast instanceof Script) {
                backend.compile((Script) ast, null);
            } else {
                error_listener.report(new MirahDiagnostic(Diagnostic.Kind.ERROR, ast.position(), "Parsing problem"));
            }
            stat.stop();
        }
    }

    @Override
    public Map<Node, ResolvedType> getResolvedTypes() {

        if (typeMap != null) return typeMap;
        typeMap = new HashMap<>();
        StatTimer stat = CompilerUtil.start("ic.resolved_types");
        for (Iterator<Node> iterator = asts.values().iterator(); iterator.hasNext(); ) {
            Node next = iterator.next();
            next.accept(new NodeTypeScanner(this), typeMap);
        }
        stat.stop();
        return typeMap;
    }

    public Node parse(CodeSource code) {
        try {
            return (Script) parser.parse(code);
        } catch (SyntaxError ex) {
            return new ErrorNode(makePosition(code, ex), ex.getMessage());
        }
    }

    public PathMapper getTypePathMapper() {
        return path_mapper;
    }

    @Override
    public Class compileAndLoadExtension(Script ast) {
        StatTimer stat = CompilerUtil.start("ic.extensions");
        logAst(ast);

        macro_typer.infer(ast, true);
        stat.stop();
        processInferenceErrors(ast, macro_context);

        failIfErrors();
        stat.start();
        macro_backend.clean(ast, null);
        stat.stop();
        processInferenceErrors(ast, macro_context);

        failIfErrors();
        stat.start();
        macro_backend.compile(ast, null);
        macro_consumer.reset();
        stat.stop();
        macro_backend.generate(macro_consumer);
        stat.start();
        Class extClass = macro_consumer.load_class();
        stat.start();
        return extClass;
    }

    @Override
    public void logAst(Node node) {
        CompilerUtil.logAst(node);
    }

    private Position makePosition(CodeSource source, SyntaxError ex) {
        return new PositionImpl(source, ex.getOffset(), ex.getLine(), ex.getColumn(), ex.getOffset(), ex.getLine(), ex.getColumn());
    }

    private void infer() {
        Node node;
        while ((node = node_queue.poll()) != null) {
            StatTimer check = CompilerUtil.start("ic.infer.check");
            if (logger.isLoggable(Level.FINE))
                logger.log(Level.FINE, "node added to scope; " + node.position());
            CompilerUtil.logAst(node);
            AstChecker.maybe_check(node);
            check.stop();
            StatTimer stat = CompilerUtil.start("ic.infer");
            typer.infer(node, false);
            stat.stop();
            check.start();
            AstChecker.maybe_check(node);
            check.stop();
            // parse sources added by SourceResourceLoader
            parse();
        }
        for (Node script : asts.values()) {
            processInferenceErrors(script, context);
        }
    }

    private void finish() {
        StatTimer stat = CompilerUtil.start("ic.closures");
        typer.finish_closures();
        stat.stop();
    }

    private void clean() {
        for (Node node : asts.values()) {
            StatTimer stat = CompilerUtil.start("ic.clean");
            backend.clean((Script) node, null);
            node.accept(new ExtensionCleanup(macro_backend,
                            macro_typer,
                            macro_consumer),
                    new HashMap());
            stat.stop();
            processInferenceErrors(node, context);
        }
    }

    private ResourceLoader createMacroLoader() {
        ClassResourceLoader bootloader = new ClassResourceLoader(System.class);
        ClassResourceLoader baseloader = new ClassResourceLoader(Mirahc.class);
        FilteredResources filteredResources = new FilteredResources(
                baseloader,
                Pattern.compile("^/?(mirah/|org/mirah)"), bootloader
        );
        return chainToResources(filteredResources);
    }

    private ResourceLoader chainToResources(ResourceLoader parent) {
        ChainedResourceLoader chain = chain(parent, resourceLoaders.toArray(new IndexedResourceLoader[resourceLoaders.size()]));
        return chain;
    }



    private ResourceLoader createMainLoader() {
        ResourceLoader boot = new NegativeFilteredResources(new ClassResourceLoader(System.class), Pattern.compile("^/?(mirah/|org/mirah|org/jruby)"));
        ResourceLoader bootloader = new FilteredResources(
                new ClassResourceLoader(Mirahc.class),
                Pattern.compile("^/?org/mirah/jvm/(types/(Flags|Member|Modifiers))|compiler/Cleaned"),
                boot
        );
        return chainToResources(bootloader);
    }


    private void createTypeSystems() {
        // Construct a loader with the standard Java classes plus the classpath
        ResourceLoader classloader = createMainLoader();

        // Now one for macros: These will be loaded into this JVM,
        // so we don't support bootclasspath.
        ResourceLoader macroloader = createMacroLoader();

        ClassLoader macro_class_loader = createMacroClassLoader();

        context.add(ClassLoader.class, macro_class_loader);
        macro_context.add(ClassLoader.class, macro_class_loader);

        macro_context.add(TypeSystem.class, new MirrorTypeSystem(macro_context, macroloader));
        context.add(TypeSystem.class, new MirrorTypeSystem(context, classloader));
    }

    private ClassLoader createMacroClassLoader() {
        return new ResourceClassLoader(MirahCompiler.class.getClassLoader(),
                MIRAH_CLASS_PATTERN,
                resourceLoaders.toArray(new IndexedResourceLoader[resourceLoaders.size()]));
    }

    private void processInferenceErrors(Node node, Context context) {
        StatTimer stat = CompilerUtil.start("ic.error.collector");
        NodeScanner errors = new ErrorCollector(context);
        errors.scan(node, null);
        stat.stop();
    }

    private int errorCount() {
        return error_listener.errorCount();
    }

    // raise exceptions in macro compiler
    private void failIfErrors() {
        if (errorCount() > 0) {
            throw new TooManyErrors(errorCount());
        }
    }

    @Override
    public void registerLoader(IndexedResourceLoader loader) {
        resourceLoaders.add(loader);
    }

    ResolvedType type(Node node) {
        if(node == null) return errorType(null);
        TypeFuture future = this.typer.getInferredType(node);
        if (future == null) {
            if (logger.isLoggable(Level.FINE))
                logger.log(Level.FINE, "defer type to parent: " + node + " " + node.position());
            return type(node.parent());
        }
        if (future.isResolved()) {
            return future.peekInferredType();
        } else {
            return errorType(node);
        }
    }

    void accept(NodeVisitor nodeVisitor, Object object) {
        for (Iterator<Node> iterator = asts.values().iterator(); iterator.hasNext(); ) {
            Node script = iterator.next();
            script.accept(nodeVisitor, object);
        }
    }

}



