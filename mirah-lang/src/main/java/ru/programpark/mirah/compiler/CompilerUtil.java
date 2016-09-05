package ru.programpark.mirah.compiler;

import org.mirah.util.AstFormatter;
import org.mirah.util.LazyTypePrinter;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by kozyr on 27.06.2016.
 */
public class CompilerUtil {
    private static final Logger logger = Logger.getLogger(CompilerUtil.class.getName());

    public static void logInferred(mirah.lang.ast.Node node, org.mirah.typer.Typer typer) {
        if (logger.isLoggable(Level.FINE))
            logger.log(Level.FINE, "Inferred types: " + node.position() +  "\n " + new LazyTypePrinter(typer, node));
    }

    public static void logAst(mirah.lang.ast.Node ast) {
        if (logger.isLoggable(Level.FINE))
            logger.log(Level.FINE, "AST: " + ast.position() + "\n" + new AstFormatter(ast));
    }
}
