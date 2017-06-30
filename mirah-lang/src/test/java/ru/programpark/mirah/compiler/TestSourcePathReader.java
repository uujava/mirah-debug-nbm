package ru.programpark.mirah.compiler;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static ru.programpark.mirah.compiler.CompilerUtil.CLASS_EXT;
import static ru.programpark.mirah.compiler.CompilerUtil.changeExt;

/**
 * Created by kozyr on 22.09.2016.
 */
public class TestSourcePathReader implements SourcePathReader {
    // class name => source code map
    Map<Path, String> code = new HashMap<>();

    public TestSourcePathReader(String[]... code) {
        for (String[] strings : code) {
            this.code.put(Paths.get(strings[0]), strings[1]);
        }
    }

    @Override
    public Path resolve(String relativePath) {
        Path key = Paths.get(changeExt(relativePath, CLASS_EXT, ".mirah"));
        if (code.get(key) != null) {
            return key;
        } else {
            return null;
        }
    }

    @Override
    public String read(Path p) throws IOException {
        return code.get(p);
    }

    @Override
    public String getBasePath() {
        return ".";
    }
}
