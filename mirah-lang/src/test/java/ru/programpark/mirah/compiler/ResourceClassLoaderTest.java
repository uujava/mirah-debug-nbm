package ru.programpark.mirah.compiler;

import org.junit.Test;
import org.mirah.tool.MirahCompiler;
import ru.programpark.mirah.compiler.loaders.ResourceClassLoader;
import ru.programpark.mirah.compiler.loaders.URLResourceLoader;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;

public class ResourceClassLoaderTest {
    @Test
    public void test() throws ClassNotFoundException, IOException, URISyntaxException {
        URL classes = new File("target/classes").toURI().toURL();
        URL testClasses = new File("target/test-classes").toURI().toURL();
        ResourceClassLoader loader = new ResourceClassLoader(null, new URLResourceLoader(classes), new URLResourceLoader(testClasses));
        assertNotNull(loader.getResource("META-INF/generated-layer.xml"));
        String className = "ru.programpark.mirah.compiler.ChainedClassLoaderTest";
        assertNotNull(loader.loadClass(className));
        String manifest = "META-INF/MANIFEST.MF";
        assertThat(count(loader.getResources(manifest)), equalTo(1));
        loader = new ResourceClassLoader(MirahCompiler.class.getClassLoader(), new URLResourceLoader(classes), new URLResourceLoader(testClasses));
        assertThat(loader.getResources(manifest).nextElement().toString(), equalTo(classes.toString() + manifest));

//        assertThat(resources.nextElement().toString(), containsString("surefire"));
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
