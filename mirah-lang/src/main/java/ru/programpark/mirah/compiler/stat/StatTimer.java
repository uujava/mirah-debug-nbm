package ru.programpark.mirah.compiler.stat;

import java.util.Map;

/**
 * Created by kozyr on 22.09.2016.
 */
public class StatTimer {
    private long ts;
    private long duration;

    public StatTimer(Map<String, StatTimer> pool) {
    }

    public void start() {
        ts = System.nanoTime();
    }

    public void stop() {
        duration += (System.nanoTime() - ts);
    }


}
