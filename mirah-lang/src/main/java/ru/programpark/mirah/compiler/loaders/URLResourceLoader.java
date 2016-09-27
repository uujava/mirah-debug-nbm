package ru.programpark.mirah.compiler.loaders;

import org.mirah.jvm.mirrors.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.logging.Logger;

/**
 * Created by kozyr on 19.09.2016.
 */
public class URLResourceLoader extends ResourceLoader implements IndexedResourceLoader {
    private static final Logger logger = Logger.getLogger(URLResourceLoader.class.getName());
    private final URLClassLoader loader;
    private URL res;

    public URLResourceLoader(URL res) {
        if (res == null) throw new IllegalArgumentException("Url must not be null");
        this.res = res;
        loader = new URLClassLoader(new URL[]{res});
    }

    @Override
    protected InputStream findResource(String s) {
        if (!contains(s)) return null;
        try {
            return open(s);
        } catch (Exception ex) {
            logger.severe("unable to open known resource: " + s + " " + ex);
            return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        URLResourceLoader that = (URLResourceLoader) o;

        return res.equals(that.res);

    }

    @Override
    public int hashCode() {
        return res.hashCode();
    }

    @Override
    public boolean contains(String path) {
        return loader.findResource(path) != null;
    }

    @Override
    public InputStream open(String path) throws IOException {
        return loader.getResourceAsStream(path);
    }

    @Override
    public URL find(String path) {
        return loader.findResource(path);
    }

    @Override
    public Enumeration<URL> findAll(String path) throws IOException {
        return loader.findResources(path);
    }

    @Override
    public String toString() {
        return "URLResourceLoader{" +
                "" + res +
                '}';
    }
}
