package ru.programpark.mirah.compiler;

import mirah.lang.ast.CodeSource;
import mirah.lang.ast.Node;
import org.mirah.jvm.compiler.BytecodeConsumer;
import org.mirah.jvm.mirrors.ResourceLoader;
import org.mirah.typer.ResolvedType;
import ru.programpark.mirah.compiler.loaders.URLResourceLoader;

import java.util.List;
import java.util.Map;

/**
 * Created by kozyr on 19.09.2016.
 */
public interface InteractiveCompiler {
    void registerReader(SourcePathReader reader);

    List<Node> getParsedNodes();

    void add(CodeSource code);

    boolean run(CacheConsumer bytecodeConsumer);

    Map<Node, ResolvedType> getResolvedTypes();

    PathMapper getTypePathMapper();

    void registerLoader(ResourceLoader url);

}
