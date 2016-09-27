package ru.programpark.mirah.compiler.impl;

import ru.programpark.mirah.compiler.MirahIncrementalCompiler;
import ru.programpark.mirah.compiler.PathMapper;
import ru.programpark.mirah.compiler.TypePathScanner;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Map type to path using node position and inference information from compiler
 */
public class CompiledPathMapper implements PathMapper {

    private final MirahIncrementalCompiler compiler;
    private final Map<String, Path> type2path;

    public CompiledPathMapper(MirahIncrementalCompiler compiler) {
        this.compiler = compiler;
        type2path = new HashMap<>();
    }

    @Override
    public Path path(String type) {
        Path path = type2path.get(type);
        if (path != null) {
            return path;
        } else {
            TypePathScanner typePathScanner = new TypePathScanner(compiler, type2path);
            compiler.accept(typePathScanner, false);
            return type2path.get(type);
        }
    }
}
