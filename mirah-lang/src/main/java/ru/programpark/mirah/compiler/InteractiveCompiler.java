package ru.programpark.mirah.compiler;

import mirah.lang.ast.CodeSource;
import mirah.lang.ast.Node;
import org.mirah.jvm.compiler.BytecodeConsumer;
import org.mirah.jvm.mirrors.ResourceLoader;
import org.mirah.typer.ResolvedType;
import ru.programpark.mirah.compiler.loaders.IndexedResourceLoader;
import ru.programpark.mirah.compiler.loaders.URLResourceLoader;

import java.util.List;
import java.util.Map;

/**
 * Facade for mirah compiler for use in IDE
 */
public interface InteractiveCompiler {

    List<Node> getParsedNodes();

    void add(CodeSource code);

    boolean run(CacheConsumer cache);

    Map<Node, ResolvedType> getResolvedTypes();

    void registerLoader(IndexedResourceLoader url);
}
