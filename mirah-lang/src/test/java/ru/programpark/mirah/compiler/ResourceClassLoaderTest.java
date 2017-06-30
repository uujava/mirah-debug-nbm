package ru.programpark.mirah.compiler;

import org.junit.Before;
import org.junit.Test;
import org.mirah.tool.MirahCompiler;
import ru.programpark.mirah.compiler.impl.MirahInteractiveCompiler;
import ru.programpark.mirah.compiler.loaders.ResourceClassLoader;
import ru.programpark.mirah.compiler.loaders.URLResourceLoader;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class ResourceClassLoaderTest {

    public static final Class MIRAHC_CLASS = MirahCompiler.class;
    public static final String MIRAH_CLASS_NAME = MIRAHC_CLASS.getName();
    public static final String TEST_CLASS_NAME = ResourceClassLoaderTest.class.getName();
    private URLResourceLoader classes;
    private URLResourceLoader testClasses;

    @Before
    public void init() throws MalformedURLException {
        classes = new URLResourceLoader(new File("target/classes").toURI().toURL());
        testClasses = new URLResourceLoader(new File("target/test-classes").toURI().toURL());
    }

    @Test
    public void test() throws ClassNotFoundException, IOException, URISyntaxException {
        ResourceClassLoader loader = new ResourceClassLoader(classes, testClasses);
        assertNotNull(loader.getResource("META-INF/generated-layer.xml"));
        assertNotNull(loader.loadClass(TEST_CLASS_NAME));
        String manifest = "META-INF/MANIFEST.MF";
        assertThat(count(loader.getResources(manifest)), equalTo(1));
        loader = new ResourceClassLoader(MIRAHC_CLASS.getClassLoader(),
                MirahInteractiveCompiler.MIRAH_CLASS_PATTERN,
                classes,
                testClasses);
        assertThat(loader.getResources(manifest).nextElement().toString(), equalTo(new File("target/classes").toURI().toURL() + manifest));
    }

    @Test
    public void testMirahcProperlyLoaded() throws MalformedURLException, ClassNotFoundException {
        String mirahUrlString = MIRAHC_CLASS.getClassLoader().getResource(MIRAH_CLASS_NAME.replace(".", "/") + ".class").toString();
        URL mirahUrl = new URL(mirahUrlString.substring(0, mirahUrlString.length() - MIRAH_CLASS_NAME.length() - ".class".length()));
        ResourceClassLoader loader = new ResourceClassLoader(MIRAHC_CLASS.getClassLoader(),
                MirahInteractiveCompiler.MIRAH_CLASS_PATTERN,
                new URLResourceLoader(mirahUrl),
                classes,
                testClasses);
        assertThat(loader.loadClass(MIRAH_CLASS_NAME), equalTo(MIRAHC_CLASS));
        assertNotNull(loader.loadClass(TEST_CLASS_NAME));
    }

    private int count(Enumeration<URL> resources) {
        int count = 0;
        while (resources.hasMoreElements()) {
            resources.nextElement();
            count += 1;
        }
        return count;
    }

    private URL last(Enumeration<URL> resources) {
        URL last = null;
        while (resources.hasMoreElements()) {
            last = resources.nextElement();
        }
        return last;
    }
}
