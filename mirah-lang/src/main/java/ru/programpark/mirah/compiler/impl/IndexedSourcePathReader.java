package ru.programpark.mirah.compiler.impl;

import org.openide.filesystems.FileUtil;
import ru.programpark.mirah.compiler.SourcePathReader;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Created by kozyr on 22.09.2016.
 */
public class IndexedSourcePathReader implements SourcePathReader {

    private static final Logger logger = Logger.getLogger(IndexedSourcePathReader.class.getName());
    private final Set<String> extensions;
    private String base;
    private Map<String, String> fileNames = new HashMap<>();

    public IndexedSourcePathReader(String base) {
        this.base = base;
        extensions = Collections.emptySet();
    }

    public IndexedSourcePathReader(String base, Set<String> extensions) {
        this.base = base;
        this.extensions = extensions;
        if(extensions == null)  new IllegalArgumentException("Null extensions for base path: " + base);
        collectNames();
    }

    @Override
    public String read(Path p) throws IOException {
        byte[] bytes = Files.readAllBytes(p);
        return new String(bytes);
    }

    @Override
    public String getBasePath() {
        return base;
    }

    @Override
    public Path resolve(String relative) {
        String sourceName = fileNames.get(relative);
        if (sourceName != null) {
            Path path = Paths.get(base, sourceName);
            if (Files.exists(path)) {
                return path;
            }
        }
        return null;
    }

    protected void collectNames() {
        final Path basePath = Paths.get(base);
        try {
            Files.walkFileTree(basePath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (attrs.isRegularFile()) {
                        String extension = FileUtil.getExtension(file.toString());
                        if (extensions.isEmpty()) {
                            String ext = extension.toLowerCase();
                            if (extensions.contains(ext)) {
                                registerClassPath(file, extension, basePath);
                            }
                        } else {
                            registerClassPath(file, extension, basePath);
                        }
                    }
                    return super.visitFile(file, attrs);
                }
            });
        } catch (IOException e) {
            logger.warning("unable to collect files for: " + base);
        }
    }

    private void registerClassPath(Path file, String extension, Path basePath) {
        Path relativize = basePath.relativize(file);
        String sourceName = relativize.toString().replace("\\", "/");
        String className = sourceName.substring(0, sourceName.length() - extension.length()) + "class";
        fileNames.put(className, sourceName);
    }

}
