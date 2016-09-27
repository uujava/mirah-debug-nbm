package ru.programpark.mirah.compiler;

import org.mirah.jvm.compiler.BytecodeConsumer;
import ru.programpark.mirah.compiler.stat.CacheStatistics;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cache class bytes aggregating by Path and type
 */
public class MapCacheConsumer implements BytecodeConsumer, CacheConsumer {

    private static final Logger logger = Logger.getLogger(MapCacheConsumer.class.getName());
    private final Map<Path, Set<String>> path2types = new HashMap<>();
    private final Map<String, byte[]> type2bytes = new HashMap<>();
    private final Map<Path, CacheStatistics> stat = new HashMap<>();
    private PathMapper mapper;

    @Override
    public void setMapper(PathMapper mapper) {
        this.mapper = mapper;
    }

    public MapCacheConsumer() {
    }

    @Override
    public void consumeClass(String type, byte[] bytes) {
        Path p = mapper.path(type);
        updatePath(p, type);
        if (type2bytes.put(type, bytes) != null) {
            if (logger.isLoggable(Level.WARNING))
                logger.log(Level.WARNING, "possible override for type: " + type + " path: " + p);
        }
        stat(p).inserted();
    }

    @Override
    public void remove(Path path) {
        Set<String> toRemove = path2types.remove(path);
        if (toRemove != null) {
            for (String type : toRemove) {
                type2bytes.remove(type);
            }
            stat(path).deleted();
        }
    }

    public void printStat(PrintStream out) {
        Collection<CacheStatistics> values = stat.values();
        Collection<byte[]> bytes = type2bytes.values();
        long total =0;
        for (byte[] b : bytes) {
            total+=b.length;
        }
        out.print("total bytes: " + total + " => ");
        for (CacheStatistics next : values) {
            out.print(next.toString());
        }
        out.println();
    }

    private void updatePath(Path p, String type) {
        Set<String> strings = path2types.get(p);
        if (strings == null) {
            strings = new HashSet<>();
            path2types.put(p, strings);
        }
        strings.add(type);
    }

    private CacheStatistics stat(Path p) {
        CacheStatistics cacheStatistics = stat.get(p);
        if (cacheStatistics == null) {
            stat.put(p, cacheStatistics = new CacheStatistics(p));
        }
        return cacheStatistics;
    }

    @Override
    public Map<String, byte[]> getType2Bytes() {
        return type2bytes;
    }
}
