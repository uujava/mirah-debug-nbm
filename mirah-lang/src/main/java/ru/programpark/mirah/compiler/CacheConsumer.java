package ru.programpark.mirah.compiler;

import org.mirah.jvm.compiler.BytecodeConsumer;

import java.nio.file.Path;
import java.util.Map;

/**
 * Created by kozyr on 22.09.2016.
 */
public interface CacheConsumer extends BytecodeConsumer {
    void setMapper(PathMapper mapper);

    void remove(Path path);

    Map<String, byte[]> getType2Bytes();
}
