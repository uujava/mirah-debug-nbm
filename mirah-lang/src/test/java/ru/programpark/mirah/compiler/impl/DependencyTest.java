package ru.programpark.mirah.compiler.impl;

import mirah.lang.ast.Node;
import mirah.lang.ast.StringCodeSource;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.mirah.typer.ResolvedType;
import org.mirah.util.ErrorCounter;
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.info.GraphLayout;
import org.openjdk.jol.vm.VM;
import ru.programpark.mirah.compiler.CompilerTestHelper;
import ru.programpark.mirah.compiler.impl.MirahInteractiveCompiler;
import ru.programpark.mirah.compiler.loaders.CacheResourceLoader;
import ru.programpark.mirah.compiler.loaders.SourceInjectorLoader;
import ru.programpark.mirah.compiler.stat.ResetStatVizitor;
import ru.programpark.mirah.compiler.stat.StatTimer;

import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class DependencyTest extends CompilerTestHelper {

    @Test
    public void testSimpleCycle() {
        mirah.add(new StringCodeSource("A.mirah", "class A;def b:B;B.newend;end"));
        mirah.add(new StringCodeSource("B.mirah", "class B;def a:A;A.new;end;end"));
        assertThat("no errors", mirah.run(null), is(true));
        assertThat("errors", 0, equalTo(errorCounter.errorCount()));
        mirah.getResolvedTypes();
    }

}
