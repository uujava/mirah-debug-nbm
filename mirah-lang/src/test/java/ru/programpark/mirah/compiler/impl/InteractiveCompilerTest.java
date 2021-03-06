package ru.programpark.mirah.compiler.impl;

import mirah.lang.ast.StringCodeSource;
import org.junit.Test;
import org.openjdk.jol.vm.VM;
import ru.programpark.mirah.compiler.*;
import ru.programpark.mirah.compiler.loaders.CacheResourceLoader;
import ru.programpark.mirah.compiler.loaders.SourceInjectorLoader;
import ru.programpark.mirah.compiler.stat.StatTimer;

import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class InteractiveCompilerTest extends CompilerTestHelper {

    @Test
    public void testSimpleCompile() {
        mirah.add(new StringCodeSource("A.mirah", "class A;end"));
        assertThat("no errors", mirah.run(null), is(true));
        assertThat("errors", 0, equalTo(errorLogger.getErrorCount()));
        printMemoryInfo(mirah);
    }

    @Test
    public void testDependentCompile() {
        mirah.add(new StringCodeSource("A.mirah", "class A; def b:B;B.new;end; end"));
        mirah.add(new StringCodeSource("B.mirah", "class B; def a:A;A.new;end;end"));
        assertThat("no errors", mirah.run(null), is(true));
        PathMapper mapper = mirah.getTypePathMapper();
        assertThat("errors", errorLogger.getErrorCount(), equalTo(0));
        assertThat("path A", mapper.path("A"), equalTo(Paths.get("A.mirah")));
        assertThat("path B", mapper.path("B"), equalTo(Paths.get("B.mirah")));
    }

    @Test
    public void testSourceReader() {
        mirah.add(new StringCodeSource("A.mirah", "class A; def b:B;B.new;end; end"));
        TestSourcePathReader reader = new TestSourcePathReader(new String[][]{{"B.mirah", "class B; def a:A;A.new;end;end"}});
        mirah.registerLoader(new SourceInjectorLoader(mirah, reader));
        assertThat("no errors", mirah.run(null), is(true));
        PathMapper mapper = mirah.getTypePathMapper();
        assertThat("errors", errorLogger.getErrorCount(), equalTo(0));
        assertThat("path A", mapper.path("A"), equalTo(Paths.get("A.mirah")));
        assertThat("path B", mapper.path("B"), equalTo(Paths.get("B.mirah")));
    }

    @Test
    public void testCaching() {
        mirah.add(new StringCodeSource("A.mirah", "class A; def b:B;B.new;end; end"));
        mirah.add(new StringCodeSource("B.mirah", "class B; def a:A;A.new;end;end"));
        MapCacheConsumer cacheConsumer = new MapCacheConsumer();
        boolean run = mirah.run(cacheConsumer);
        assertThat("no errors", run, is(true));

        errorLogger = new ErrorLogger();
        mirah = new MirahInteractiveCompiler(errorLogger);
        mirah.add(new StringCodeSource("C.mirah", "class C; def a;A.new.b.a;end; end"));
        mirah.registerLoader(new CacheResourceLoader(cacheConsumer.getType2Bytes()));
        run = mirah.run(cacheConsumer);
        assertThat("no errors if referenced from cache", run, is(true));

        errorLogger = new ErrorLogger();
        mirah = new MirahInteractiveCompiler(errorLogger);
        cacheConsumer.remove(Paths.get("B.mirah"));
        cacheConsumer.remove(Paths.get("C.mirah"));
        mirah.add(new StringCodeSource("C.mirah", "class C; def a;A.new.b.a;end; end"));
        mirah.registerLoader(new CacheResourceLoader(cacheConsumer.getType2Bytes()));
        run = mirah.run(cacheConsumer);
        withErrors("C not compiled if B removed from cache ", run, is(false));
        withErrors("no B in cache ", cacheConsumer.getType2Bytes().get("B"), nullValue());
        withErrors("no C in cache ", cacheConsumer.getType2Bytes().get("C"), nullValue());
        withErrors("A in cache ", cacheConsumer.getType2Bytes().get("A"), notNullValue());

        errorLogger = new ErrorLogger();
        mirah = new MirahInteractiveCompiler(errorLogger);
        mirah.add(new StringCodeSource("C.mirah", "class C; def a;A.new.b.a;end; end"));
        TestSourcePathReader reader = new TestSourcePathReader(new String[][]{
                {"B.mirah", "class B; def a:A;A.new;end;end"},
                {"A.mirah", "class A; def b:B;B.new;end; end"}
        });
        mirah.registerLoader(new SourceInjectorLoader(mirah, reader));
        mirah.registerLoader(new CacheResourceLoader(cacheConsumer.getType2Bytes()));

        run = mirah.run(cacheConsumer);
        withErrors("C is not compiled as A reference B from bytecode", run, is(false));
        withErrors("B not in cache", cacheConsumer.getType2Bytes().get("B"), nullValue());
        withErrors("C not in cache", cacheConsumer.getType2Bytes().get("C"), nullValue());
        withErrors("A in cache", cacheConsumer.getType2Bytes().get("A"), notNullValue());

        cacheConsumer.remove(Paths.get("A.mirah"));
        errorLogger = new ErrorLogger();
        mirah = new MirahInteractiveCompiler(errorLogger);
        mirah.add(new StringCodeSource("C.mirah", "class C; def a;A.new.b.a;end; end"));
        reader = new TestSourcePathReader(new String[][]{
                {"B.mirah", "class B; def a:A;A.new;end;end"},
                {"A.mirah", "class A; def b:B;B.new;end; end"}
        });
        mirah.registerLoader(new CacheResourceLoader(cacheConsumer.getType2Bytes()));
        mirah.registerLoader(new SourceInjectorLoader(mirah, reader));

        run = mirah.run(cacheConsumer);
        withErrors("C is compiled along with A and B", run, is(true));
        withErrors("B in cache", cacheConsumer.getType2Bytes().get("B"), notNullValue());
        withErrors("C in cache", cacheConsumer.getType2Bytes().get("C"), notNullValue());
        withErrors("A in cache", cacheConsumer.getType2Bytes().get("A"), notNullValue());

    }

    @Test
    public void testStat() {
        mirah.add(new StringCodeSource("A.mirah", "class A; def b:B;B.new;end; end"));
        mirah.add(new StringCodeSource("B.mirah", "class B; def a:A;A.new;end;end"));
        MapCacheConsumer cacheConsumer = new MapCacheConsumer();
        assertThat("no errors", mirah.run(cacheConsumer), is(true));
        final AtomicLong totalStat = new AtomicLong();
        final Set<String> stats = new HashSet<>();
        CompilerUtil.vizitStat(new StatTimer.Visitor<StatTimer>() {
            @Override
            public void visit(StatTimer statTimer) {
                totalStat.addAndGet(statTimer.getTotal());
                String str = statTimer.getName() + ":" + TimeUnit.NANOSECONDS.toMillis(statTimer.getTotal());
                stats.add(str);
            }
        });
        long total = TimeUnit.NANOSECONDS.toMillis(totalStat.get());
        System.out.println("total time: " + total + " timers: " + stats);
        // TODO better perfromance testing
        assertThat("total time: " + total + " timers: " + stats, total < 1500, is(true));
    }

    @Test
    public void vmDetails() {
        System.out.println(VM.current().details());
    }


}
