package ru.programpark.mirah.lexer;

import org.netbeans.api.java.classpath.ClassPath;
import org.openide.filesystems.FileObject;
import ru.programpark.mirah.compiler.loaders.IndexedResourceLoader;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

/**
 * Created by kozyr on 27.09.2016.
 */
public class ClassPathResourceLoader implements IndexedResourceLoader {
    private ClassPath cp;

    public ClassPathResourceLoader(ClassPath cp) {
        this.cp = cp;
    }

    @Override
    public boolean contains(String path) {
        return cp != null ? cp.findResource(path) != null : false;
    }

    @Override
    public InputStream open(String path) throws FileNotFoundException {
        FileObject resource = cp.findResource(path);
        if (resource != null) {
            return resource.getInputStream();
        } else {
            return null;
        }
    }

    @Override
    public URL find(String path) {
        FileObject resource = cp.findResource(path);
        if (resource != null) {
            return resource.toURL();
        } else {
            return null;
        }
    }

    @Override
    public Enumeration<URL> findAll(String path) {
        List<FileObject> allResources = cp.findAllResources(path);
        final Iterator<FileObject> iterator = allResources.iterator();
        return new Enumeration<URL>() {
            URL url = null;

            @Override
            public boolean hasMoreElements() {
                return iterator.hasNext();
            }

            @Override
            public URL nextElement() {
                return iterator.next().toURL();
            }
        };
    }

    @Override
    public String toString() {
        return "ClassPathResourceLoader{" +
                "cp=" + cp +
                '}';
    }
}
