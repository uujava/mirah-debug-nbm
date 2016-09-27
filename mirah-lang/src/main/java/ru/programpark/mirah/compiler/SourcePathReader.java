package ru.programpark.mirah.compiler;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Abstract Source code access by path/resource name
 */
public interface SourcePathReader {
    /**
     * @param p path to read
     * @return path content as string
     * @throws IOException
     */
    String read(Path p) throws IOException;

    /**
     * Base path to resolve relative paths
     * @return
     */
    String getBasePath();

    /**
      resolve resource class file name to source path or null if this reader does not know this path
     */
    Path resolve(String relativePath);
}
