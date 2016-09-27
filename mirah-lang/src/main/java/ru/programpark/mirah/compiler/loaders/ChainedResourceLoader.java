package ru.programpark.mirah.compiler.loaders;

import org.mirah.jvm.mirrors.ResourceLoader;

import java.io.InputStream;

/**
 * Created by kozyr on 19.09.2016.
 */
class ChainedResourceLoader extends ResourceLoader {
    private final ResourceLoader p;
    private ResourceLoader next;

    public ChainedResourceLoader(ResourceLoader parent) {
        this.p = parent;
    }

    @Override
    protected InputStream findResource(String name) {
        InputStream resourceAsStream = null;
        if (next != null) {
            resourceAsStream = next.getResourceAsStream(name);
        }

        if (resourceAsStream != null) return resourceAsStream;

        if (p != null) {
            return p.getResourceAsStream(name);
        } else {
            return null;
        }
    }

    public void setNext(ResourceLoader next) {
        this.next = next;
    }
}
