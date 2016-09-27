package ru.programpark.mirah.compiler.loaders;

import mirah.lang.ast.CodeSource;
import mirah.lang.ast.StringCodeSource;
import ru.programpark.mirah.compiler.InteractiveCompiler;
import ru.programpark.mirah.compiler.SourcePathReader;

import java.io.File;
import java.io.InputStream;
import java.nio.file.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by user on 9/10/2016.
 */
public class SourceInjectorLoader extends ChainedSourceLoader {
    private static final Logger logger = Logger.getLogger(SourceInjectorLoader.class.getName());
    private final String base;
    private final InteractiveCompiler compiler;
    private final boolean valid;
    private final SourcePathReader reader;

    public SourceInjectorLoader(InteractiveCompiler compiler, SourcePathReader reader) {
        if (reader.getBasePath() == null) throw new RuntimeException("source root is null: " + reader);
        this.base = reader.getBasePath();
        File baseFile = new File(base);
        valid = baseFile.exists() && baseFile.isDirectory();
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
        InputStream resource = super.findResource(s);
        if (resource != null) return resource;
        if (!valid) return null;
        if (processed.containsKey(s)) return null;
        Path p = reader.resolve(s);
        if (p != null) {
            try {
                String read = reader.read(p);
                if (read != null) {
                    CodeSource open = new StringCodeSource(p.normalize().toFile().toString(), read);
                    compiler.add(open);
                    markProcessed(s);
                }
            } catch (Exception ex) {
                if (logger.isLoggable(Level.FINE))
                    logger.log(Level.FINE, "error on open " + s + " " + ex, ex);
                markProcessed(s);
            }
        }
        return null;
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

    public boolean isValid() {
        return valid;
    }
}
