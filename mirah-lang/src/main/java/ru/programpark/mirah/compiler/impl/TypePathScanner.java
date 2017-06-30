package ru.programpark.mirah.compiler.impl;

import mirah.lang.ast.*;
import org.mirah.typer.ResolvedType;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by kozyr on 12.09.2016.
 */
class TypePathScanner extends NodeScanner {
    private static final Logger logger = Logger.getLogger(TypePathScanner.class.getName());
    private final Map<String, Path> type2path;
    private MirahInteractiveCompiler compiler;

    public TypePathScanner(MirahInteractiveCompiler incrementalCompiler, Map<String, Path> type2path) {
        this.compiler = incrementalCompiler;
        this.type2path = type2path;
    }

    @Override
    public boolean enterDefault(Node node, Object typeMap) {
        return true;
    }

    @Override
    public boolean enterClassDefinition(ClassDefinition node, Object arg) {
        updateMap(node);
        return false;
    }

    @Override
    public boolean enterInterfaceDeclaration(InterfaceDeclaration node, Object arg) {
        updateMap(node);
        return false;
    }

    private void updateMap(ClassDefinition node) {
        String path = node.position().source().name();
        ResolvedType future = compiler.type(node);
        String typeName = future.name();
        Path newPath = Paths.get(path);
        Path oldPath = type2path.put(typeName, newPath);
        if(oldPath !=null && !oldPath.equals(newPath)){
            logger.warning("possible source path change: " + typeName + "=>" + path);
        }
    }

}
