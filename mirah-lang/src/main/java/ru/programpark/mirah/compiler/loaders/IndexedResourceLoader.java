package ru.programpark.mirah.compiler.loaders;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

/**
 * ResourceLoader with indexed content
 * Easier converts into ClassLoader.
 */
public interface IndexedResourceLoader {

    /**
     * check if resource identified by path known to this loader
     *
     * @param path - resource path, usually relative to some root folder
     * @return
     */
    boolean contains(String path);

    /**
     * Open stream to known resource or null
     *
     * @param path- resource path, usually relative to some root folder
     * @return
     */
    InputStream open(String path) throws IOException;

    /**
     * URL for known resource or null
     *
     * @param path- resource path, usually relative to some root folder
     * @return
     */
    URL find(String path);

    /**
     * Intended mainly for META-INF/services lookup
     * Should also work for ALL file/jar based resources
     *
     * @param path- resource path, usually relative to some root folder
     * @return
     */
    Enumeration<URL> findAll(String path) throws IOException;

}
