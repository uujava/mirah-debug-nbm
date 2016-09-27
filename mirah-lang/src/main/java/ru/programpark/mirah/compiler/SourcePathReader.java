package ru.programpark.mirah.compiler;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by user on 9/11/2016.
 */
public interface SourcePathReader {
    String open(Path p) throws IOException;
}
