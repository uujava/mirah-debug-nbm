package ru.programpark.mirah.lexer;

import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import ru.programpark.mirah.compiler.SourcePathOpener;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by kozyr on 12.09.2016.
 */
public class SnapshotReader implements SourcePathOpener {

    @Override
    public String open(Path p) throws IOException {
        return FileUtil.toFileObject(p.toFile()).asText();
    }

    public void register(FileObject fo) {
        return null;
    }
}
