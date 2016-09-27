package ru.programpark.mirah.compiler.loaders;

import org.mirah.jvm.mirrors.ResourceLoader;

import java.io.InputStream;
import java.util.logging.Logger;

/**
 * Wrap and chain resource loaders
 */
public class ChainedResourceLoader extends ResourceLoader {
    private static final Logger logger = Logger.getLogger(ChainedResourceLoader.class.getName());
    private final IndexedResourceLoader self;
    private ResourceLoader next;

    public ChainedResourceLoader(IndexedResourceLoader parent) {
        this.self = parent;
    }

    @Override
    protected InputStream findResource(String name) {
        InputStream resourceAsStream = null;
        if (next != null) {
            resourceAsStream = next.getResourceAsStream(name);
        }

        if (resourceAsStream != null) return resourceAsStream;

        if (self != null) {
            try {
                return self.open(name);
            } catch (Exception ex) {
                logger.severe("Unable to open resource :" + name + " " + ex);
            }
        }
        return null;
    }

    public void setNext(ResourceLoader next) {
        this.next = next;
    }
}
