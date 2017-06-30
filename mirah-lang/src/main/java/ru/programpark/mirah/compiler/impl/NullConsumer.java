package ru.programpark.mirah.compiler.impl;

import ru.programpark.mirah.compiler.CacheConsumer;
import ru.programpark.mirah.compiler.PathMapper;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

public class NullConsumer implements CacheConsumer {
    @Override
    public void consumeClass(String s, byte[] bytes) {
        // do nothing
    }

    @Override
    public void setMapper(PathMapper mapper) {
        // do nothing
    }

    @Override
    public void remove(Path path) {
        //do nothing
    }

    @Override
    public Map<String, byte[]> getType2Bytes() {
        return Collections.emptyMap();
    }
}
