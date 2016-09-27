package ru.programpark.mirah.compiler.stat;

import java.util.concurrent.TimeUnit;

/**
 * Created by kozyr on 22.09.2016.
 */
public class StatTimer {
    private final String name;
    private long ts;
    private long duration;
    private long count;

    public StatTimer(String name) {
        this.name = name;
    }

    public StatTimer start() {
        ts = System.nanoTime();
        count++;
        return this;
    }

    public StatTimer stop() {
        duration += (System.nanoTime() - ts);
        return this;
    }

    /**
     * nono seconds
     *
     * @return
     */
    public long getTotal() {
        return duration;
    }

    public long getCount() {
        return count;
    }

    @Override
    public String toString() {
        return "StatTimer{" +
                "name='" + name + '\'' +
                ", duration=" + TimeUnit.NANOSECONDS.toMillis(duration) +
                ", count=" + count +
                '}';
    }

    public void reset(){
        duration = 0;
        count = 0;
    }

    public String getName() {
        return name;
    }

    public interface Visitor<E> {
        void visit(E statTimer);
    }
}
