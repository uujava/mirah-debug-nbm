package ru.programpark.mirah.compiler.loaders;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static ru.programpark.mirah.compiler.CompilerUtil.toPath;

/**
 * Use a set of loaders to load classes and resolve resources
 * Delegates to parent loader if set in constructor
 */
public class ResourceClassLoader extends ClassLoader {
    static final Logger logger = Logger.getLogger(ResourceClassLoader.class.getName());
    private final Pattern pattern;
    private Set<IndexedResourceLoader> loaders = new HashSet<>();

    /**
     * Uses pattern to filter resources  need to load from parent
     *
     * @param parent  - parent classloader
     * @param pattern - regexp pattern to identify parent resources
     * @param loaders - other loaders to lookup classes
     */
    public ResourceClassLoader(ClassLoader parent, String pattern, IndexedResourceLoader... loaders) {
        super(parent);
        this.pattern = Pattern.compile(pattern);
        this.loaders.addAll(Arrays.asList(loaders));
    }

    public ResourceClassLoader(IndexedResourceLoader... loaders) {
        super(null);
        this.pattern = null;
        this.loaders.addAll(Arrays.asList(loaders));
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        if (getParent() != null && pattern != null && pattern.matcher(name).find()) {
            return super.loadClass(name);
        } else {
            String path = toPath(name);
            for (IndexedResourceLoader loader : loaders) {
                if (loader.contains(path)) {
                    synchronized (getClassLoadingLock(name)) {
                        Class<?> loadedClass = findLoadedClass(name);
                        if (loadedClass != null) return loadedClass;
                        try {
                            InputStream stream = loader.open(path);
                            if (stream != null) {
                                byte[] bytes;
                                try {
                                    bytes = IOUtils.toByteArray(stream);
                                } catch (Exception ex) {
                                    logger.severe("Unable to define class: " + ex);
                                    throw new NoClassDefFoundError(name);
                                }
                                return defineClass(name, bytes, 0, bytes.length);
                            }
                        } catch (Exception ex) {
                            logger.severe("Unable to load class " + name + " from loader: " + loader + " " + ex);
                        }
                    }
                }
            }
            // finally fall back to super
            return super.loadClass(name);
        }
    }

    @Override
    public URL getResource(String path) {
        for (IndexedResourceLoader loader : loaders) {
            URL url = loader.find(path);
            if (url != null) return url;
        }
        return super.findResource(path);
    }

    @Override
    public InputStream getResourceAsStream(String path) {
        for (IndexedResourceLoader loader : loaders) {
            if (loader.contains(path)) {
                try {
                    return loader.open(path);
                } catch (IOException e) {
                    logger.severe("unable to load resource " + path + " from loader: " + loader + " " + e);
                }
            }
        }
        return super.getResourceAsStream(path);
    }

    @Override
    public Enumeration<URL> getResources(String path) throws IOException {
        List<URL> resources = new LinkedList<>();
        for (IndexedResourceLoader loader : loaders) {
            URL url = loader.find(path);
            if (url != null) {
                resources.add(url);
            }
        }
        final Iterator<URL> iterator = resources.iterator();
        final Enumeration<URL> parentResources = super.getResources(path);
        return new Enumeration<URL>() {
            private URL url = null;

            private boolean next() {
                if (url != null) {
                    return true;
                }
                if (iterator != null && iterator.hasNext()) {
                    url = iterator.next();
                } else if (parentResources != null && parentResources.hasMoreElements()) {
                    url = parentResources.nextElement();
                }
                return url != null;
            }

            public URL nextElement() {
                if (!next()) {
                    throw new NoSuchElementException();
                }
                URL u = url;
                url = null;
                return u;
            }

            public boolean hasMoreElements() {
                return next();
            }
        };
    }


}
