package ru.programpark.mirah.compiler.loaders;

import mirah.lang.ast.CodeSource;
import mirah.lang.ast.StringCodeSource;
import org.mirah.jvm.mirrors.ResourceLoader;
import ru.programpark.mirah.compiler.InteractiveCompiler;
import ru.programpark.mirah.compiler.SourcePathReader;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by user on 9/10/2016.
 */
public class SourceInjectorLoader extends ResourceLoader implements IndexedResourceLoader {
    private static final Logger logger = Logger.getLogger(SourceInjectorLoader.class.getName());
    private final String base;
    private final InteractiveCompiler compiler;
    private final SourcePathReader reader;
    protected Map<String, Boolean> processed = new HashMap<>();

    public SourceInjectorLoader(InteractiveCompiler compiler, SourcePathReader reader) {
        if (reader.getBasePath() == null) throw new RuntimeException("source root is null: " + reader);
        this.base = reader.getBasePath();
        this.compiler = compiler;
        this.reader = reader;
    }


    /**
     * Add source file to the compiler code queue once per key
     *
     * @param s
     * @return
     */
    @Override
    protected InputStream findResource(String s) {
        return open(s);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SourceInjectorLoader that = (SourceInjectorLoader) o;
        return base.equals(that.base);
    }

    @Override
    public int hashCode() {
        return base.hashCode();
    }

    private void markProcessed(String resourceName) {
        processed.put(resourceName, true);
    }

    @Override
    public boolean contains(String path) {
        return false;
    }

    @Override
    public InputStream open(String path) {
        if (processed.containsKey(path)) return null;
        Path p = reader.resolve(path);
        if (p != null) {
            try {
                String read = reader.read(p);
                if (read != null) {
                    CodeSource open = new StringCodeSource(p.normalize().toFile().toString(), read);
                    compiler.add(open);
                    markProcessed(path);
                }
            } catch (Exception ex) {
                if (logger.isLoggable(Level.FINE))
                    logger.log(Level.FINE, "error on open " + path + " " + ex, ex);
                markProcessed(path);
            }
        }
        return null;
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
    public String toString() {
        return "SourceInjectorLoader{" + base + '}';
    }
}
