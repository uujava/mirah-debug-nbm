package ru.programpark.mirah.compiler;

import org.apache.commons.io.output.WriterOutputStream;
import org.junit.Test;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;

/**
 * Created by user on 9/18/2016.
 */
public class CacheConsumerTest {
    @Test
    public void testPath2Type() {
        MapCacheConsumer cacheConsumer = new MapCacheConsumer();
        cacheConsumer.setMapper(new PathMapper() {
            @Override
            public Path path(String type) {
                return Paths.get("X");
            }
        });
        cacheConsumer.consumeClass("A", new byte[1]);
        assertStat(cacheConsumer, 1, "X", 1, 0);
        cacheConsumer.consumeClass("B", new byte[2]);
        assertStat(cacheConsumer, 3, "X", 2, 0);
        cacheConsumer.remove(Paths.get("X"));
        assertStat(cacheConsumer, 0, "X", 2, 1);
    }

    private void assertStat(MapCacheConsumer cacheConsumer, Object... data) {
        String stat = String.format("total bytes: %d => {%s=> inserts=%d, deleted=%d, modified=", data);
        StringWriter writer = new StringWriter();
        OutputStream os = new WriterOutputStream(writer);
        PrintStream ps = new PrintStream(os);
        cacheConsumer.printStat(ps);
        ps.close();
        assertThat(writer.toString(), startsWith(stat));
    }

}
