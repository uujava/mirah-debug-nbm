package ru.programpark.mirah.compiler;

import mirah.lang.ast.Node;
import org.mirah.jvm.mirrors.ResourceLoader;
import org.mirah.typer.ErrorType;
import org.mirah.typer.ResolvedType;
import org.mirah.util.AstFormatter;
import org.mirah.util.LazyTypePrinter;
import ru.programpark.mirah.compiler.loaders.ChainedResourceLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by kozyr on 27.06.2016.
 */
public class CompilerUtil {
    private static final Logger logger = Logger.getLogger(CompilerUtil.class.getName());

    public static void logInferred(mirah.lang.ast.Node node, org.mirah.typer.Typer typer) {
        if (logger.isLoggable(Level.FINE))
            logger.log(Level.FINE, "Inferred types: " + node.position() + "\n " + new LazyTypePrinter(typer, node));
    }

    public static void logAst(mirah.lang.ast.Node ast) {
        if (logger.isLoggable(Level.FINE))
            logger.log(Level.FINE, "AST: " + ast.position() + "\n" + new AstFormatter(ast));
    }

    static ResolvedType errorType(Node node) {
        ArrayList<Object> message = new ArrayList<>();
        message.add("Unresolved node: '" + node + "'");
        message.add(node.position());
        ArrayList messages = new ArrayList();
        messages.add(message);
        return new ErrorType(messages);
    }

    /**
     * @param loaders - loaders to chain
     * @return loader having last to first loading order from the supplied loader list
     */
    static ChainedResourceLoader chain(ResourceLoader loader, List<? extends ResourceLoader> loaders) {
        ChainedResourceLoader first = new ChainedResourceLoader(loader);
        for (ResourceLoader orig : loaders) {
            ChainedResourceLoader next = new ChainedResourceLoader(orig);
            next.setNext(first);
            first = next;
        }
        return first;
    }

}
