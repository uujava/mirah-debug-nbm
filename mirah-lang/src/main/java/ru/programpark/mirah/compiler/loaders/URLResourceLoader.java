package ru.programpark.mirah.compiler.loaders;

import org.mirah.jvm.mirrors.ResourceLoader;

import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Created by kozyr on 19.09.2016.
 */
public class URLResourceLoader extends ResourceLoader{
    private URL res;
    private final URLClassLoader loader;

    public URLResourceLoader(URL res) {
        if(res == null) throw new IllegalArgumentException("Url must not be null");
        this.res = res;
        loader = new URLClassLoader(new URL[]{res});
    }

    @Override
    protected InputStream findResource(String s) {
        return loader.getResourceAsStream(s);
    }

    public URL getUrl(){
        return res;
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
}
