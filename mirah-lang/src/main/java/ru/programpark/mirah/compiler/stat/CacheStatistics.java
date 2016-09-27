package ru.programpark.mirah.compiler.stat;

import java.nio.file.Path;
import java.util.Date;
import java.util.Set;

/**
 * Created by kozyr on 22.09.2016.
 */
public class CacheStatistics {
    private final Path path;
    private long inserts;
    private long deleted;
    private long timeStamp;

    public CacheStatistics(Path path) {
        this.path = path;
    }

    public void inserted() {
        this.inserts += 1;
        this.timeStamp = System.currentTimeMillis();
    }

    public void deleted() {
        this.deleted += 1;
    }

    @Override
    public String toString() {
        long total = 0;
        Set<String> strings = path2types.get(path);
        if (strings != null) {
            for (String type : strings) {
                byte[] bytes = type2bytes.get(type);
                if (bytes != null) {
                    total += bytes.length;
                }
            }
        }
        return "CacheStatistics{" + path +
                "=> totalSize=" + total +
                ", classCount=" + (strings != null ? strings.size() : 0) +
                ", inserts=" + inserts +
                ", deleted=" + deleted +
                ", modified=" + new Date(timeStamp) +
                '}';
    }

}
