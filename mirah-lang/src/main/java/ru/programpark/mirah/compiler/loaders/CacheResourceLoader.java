package ru.programpark.mirah.compiler.loaders;

import org.mirah.jvm.mirrors.ResourceLoader;
import ru.programpark.mirah.compiler.CompilerUtil;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

/**
 * Loads classes from byte array cache
 */
public class CacheResourceLoader extends ResourceLoader implements IndexedResourceLoader {

    private final Map<String, byte[]> classBytes;

    public CacheResourceLoader(Map<String, byte[]> classBytes) {
        this.classBytes = classBytes == null ? Collections.<String, byte[]>emptyMap() : classBytes;
    }

    @Override
    protected InputStream findResource(String path) {
        return open(path);
    }

    @Override
    public boolean contains(String path) {
        return classBytes.get(CompilerUtil.typeName(path)) != null;
    }

    @Override
    public InputStream open(String path) {
        byte[] bytes = classBytes.get(CompilerUtil.typeName(path));
        if (bytes != null) {
            return new ByteArrayInputStream(bytes);
        } else {
            return null;
        }
    }

    @Override
    public URL find(String path) {
        return null;
    }

    @Override
    public Enumeration<URL> findAll(String path) {
        return Collections.emptyEnumeration();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CacheResourceLoader)) return false;

        CacheResourceLoader that = (CacheResourceLoader) o;

        return classBytes.equals(that.classBytes);

    }

    @Override
    public int hashCode() {
        return classBytes.hashCode();
    }

    @Override
    public String toString() {
        return "CacheResourceLoader{" +
                "classBytes=" + classBytes.keySet() +
                '}';
    }
}
