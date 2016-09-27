package ru.programpark.mirah.compiler;

import mirah.lang.ast.Node;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.mirah.typer.ResolvedType;
import org.mirah.util.ErrorCounter;
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.info.GraphLayout;
import ru.programpark.mirah.compiler.impl.MirahInteractiveCompiler;
import ru.programpark.mirah.compiler.stat.ResetStatVizitor;

import java.io.PrintStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertThat;

/**
 * Created by kozyr on 26.09.2016.
 */
public class CompilerTestHelper {
    protected ErrorLogger errorLogger;
    protected MirahInteractiveCompiler mirah;

    @Before
    public void init() {
        errorLogger = new ErrorLogger();
        mirah = new MirahInteractiveCompiler(errorLogger);
        CompilerUtil.vizitStat(new ResetStatVizitor());
    }

    protected <T> void withErrors(String message, T value, Matcher<T> objectMatcher) {
        assertThat(message + "=>" + errorLogger, value, objectMatcher);
    }

    private void enableDebug() {
        Logger logger = Logger.getLogger("org.mirah");
        logger.setLevel(Level.FINE);
    }

    private void memoryLayout(Set<Class> nodeClasses) {
        ArrayList<Class> objects = new ArrayList<>(nodeClasses);
        Collections.sort(objects, new Comparator<Class>() {
            @Override
            public int compare(Class o1, Class o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        for (Class cls : objects) {
            memoryLayout(cls, System.out);
        }
    }

    public void memoryLayout(Class type, PrintStream out) {
        out.println(ClassLayout.parseClass(type).toPrintable());
    }

    public void memoryFootprint(String mesage, Object... objects) {
        if (objects.length == 0) return;
        System.out.print(mesage);
        for (Object obj : objects) {
            System.out.print(" " + GraphLayout.parseInstance(obj).totalSize());
        }
        System.out.println();
    }

    protected void printMemoryInfo(InteractiveCompiler mirah) {
        Map<Node, ResolvedType> resolvedTypes = mirah.getResolvedTypes();
        Set<Map.Entry<Node, ResolvedType>> entries = resolvedTypes.entrySet();
        Set<Class> nodeClasses = new HashSet<>();
        Set<Class> typeClasses = new HashSet<>();
        for (Map.Entry<Node, ResolvedType> entry : entries) {
            System.out.println("entry = " + entry);
            nodeClasses.add(entry.getKey().getClass());
            typeClasses.add(entry.getValue().getClass());
            memoryFootprint("size(node, type):", entry.getKey(), entry.getValue());
        }

        memoryLayout(nodeClasses);
        memoryLayout(typeClasses);
    }
}
