package ru.programpark.mirah.compiler.stat;

import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

/**
 * Created by kozyr on 22.09.2016.
 */
public class CacheStatistics {
    private final Path path;
    private int inserts;
    private int deleted;
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
        this.timeStamp = System.currentTimeMillis();
    }

    @Override
    public String toString() {

        return "{" + path +
                "=> inserts=" + inserts +
                ", deleted=" + deleted +
                ", modified=" + new SimpleDateFormat("YYYYddMM-HH:mm:ss.SSS").format(new Date(timeStamp)) +
                '}';
    }

}
