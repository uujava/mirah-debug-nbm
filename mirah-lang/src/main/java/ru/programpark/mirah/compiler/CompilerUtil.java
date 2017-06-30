package ru.programpark.mirah.compiler;

import mirah.lang.ast.Node;
import org.mirah.jvm.mirrors.ResourceLoader;
import org.mirah.typer.ErrorType;
import org.mirah.typer.ResolvedType;
import org.mirah.util.AstFormatter;
import org.mirah.util.LazyTypePrinter;
import ru.programpark.mirah.compiler.loaders.ChainedResourceLoader;
import ru.programpark.mirah.compiler.loaders.IndexedResourceLoader;
import ru.programpark.mirah.compiler.stat.StatTimer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by kozyr on 27.06.2016.
 */
public class CompilerUtil {
    public static final String CLASS_EXT = ".class";
    private static final Logger logger = Logger.getLogger(CompilerUtil.class.getName());
    private static Map<String, StatTimer> stats = new HashMap<>();

    public static void logInferred(mirah.lang.ast.Node node, org.mirah.typer.Typer typer) {
        if (logger.isLoggable(Level.FINE))
            logger.log(Level.FINE, "Inferred types: " + node.position() + "\n " + new LazyTypePrinter(typer, node));
    }

    public static void logAst(mirah.lang.ast.Node ast) {
        if (logger.isLoggable(Level.FINE))
            logger.log(Level.FINE, "AST: " + ast.position() + "\n" + new AstFormatter(ast));
    }

    public static ResolvedType errorType(Node node) {
        ArrayList<Object> message = new ArrayList<>();
        message.add("Unresolved node: '" + node + "'");
        if (node != null) {
            message.add(node.position());
        }
        ArrayList messages = new ArrayList();
        messages.add(message);
        return new ErrorType(messages);
    }

    /**
     * @param loaders - loaders to chain
     * @return loader having last to first loading order from the supplied loader list
     */
    public static ChainedResourceLoader chain(ResourceLoader parent, IndexedResourceLoader... loaders) {
        ChainedResourceLoader first = new ChainedResourceLoader(null);

        for (IndexedResourceLoader orig : loaders) {
            ChainedResourceLoader next = new ChainedResourceLoader(orig);
            first.setNext(next);
            first = next;
        }
        first.setNext(parent);
        return first;
    }

    public static String toPath(String typeName) {
        return typeName.replace(".", "/") + CLASS_EXT;
    }

    public static String typeName(String path) {
        int i = path.lastIndexOf(CLASS_EXT);
        if (i > 0) {
            int length = CLASS_EXT.length();
            int pl = path.length();
            if ((i + length) == pl) {
                path = path.substring(0, i);
            }
        }
        return path.replace('/', '.');
    }

    public static String changeExt(String path, String origExt, String newExt) {
        int i = path.lastIndexOf(origExt);
        if (i > 0) {
            int length = origExt.length();
            int pl = path.length();
            if ((i + length) == pl) {
                return path.substring(0, i) + newExt;
            } else {
                return path;
            }
        } else {
            return path;
        }
    }

    public static StatTimer start(String name) {
        StatTimer statTimer = stats.get(name);
        if (statTimer == null) {
            statTimer = new StatTimer(name);
            stats.put(name, statTimer);
        }
        return statTimer.start();
    }

    public static void vizitStat(StatTimer.Visitor<StatTimer> visitor) {
        for (StatTimer statTimer : stats.values()) {
            visitor.visit(statTimer);
        }
    }
}
