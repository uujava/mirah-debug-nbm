package ru.programpark.mirah.compiler;

import java.nio.file.Path;

/**
 * Map class name to source file path
 */
public interface PathMapper {
    /**
     * @param type - FQN class name
     * @return path to a file if type is known for mapper or null
     */
    Path path(String type);
}
