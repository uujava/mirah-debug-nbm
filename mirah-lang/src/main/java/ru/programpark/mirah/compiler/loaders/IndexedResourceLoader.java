package ru.programpark.mirah.compiler.loaders;

import org.mirah.jvm.mirrors.ResourceLoader;

import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

/**
 * Created by kozyr on 27.09.2016.
 */
public abstract class IndexedResourceLoader extends ResourceLoader {

    public abstract boolean contains(String path);

    public abstract InputStream asStream(String path);

    public abstract Enumeration<URL> findAll(String path);

    @Override
    protected InputStream findResource(String path) {
        if (!contains(path)) {
            return asStream(path);
        } else {
            return null;
        }
    }
}
