package ru.programpark.mirah.lexer;

import org.netbeans.editor.BaseDocument;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;
import ru.programpark.mirah.compiler.impl.IndexedSourcePathReader;

import javax.swing.text.Document;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

/**
 * Created by kozyr on 12.09.2016.
 */
public class SnapshotReader extends IndexedSourcePathReader {

    public SnapshotReader(String basePath, Set<String> extensions) {
        super(basePath, extensions);
        if(basePath == null) throw new IllegalArgumentException("base path is null");
    }

    @Override
    public String read(Path p) throws IOException {
        FileObject fileObject = FileUtil.toFileObject(p.toFile());
        BaseDocument document = getDocument(fileObject);
        if (document != null) {
            return document.getText().toString();
        } else {
            return fileObject.asText();
        }
    }

    @Override
    public Path resolve(String relative) {
        //TODO don't we have issues with unsaved files?
        return super.resolve(relative);
    }

    private static BaseDocument getDocument(FileObject fileObject) {
        DataObject dobj;

        try {
            dobj = DataObject.find(fileObject);

            EditorCookie ec = dobj.getLookup().lookup(EditorCookie.class);

            if (ec == null) {
                throw new IOException("Can't open " + fileObject.getNameExt());
            }

            Document document = ec.openDocument();

            if (document instanceof BaseDocument) {
                return ((BaseDocument) document);
            } else {
                return null;
            }
        } catch (IOException ioe) {
            Exceptions.printStackTrace(ioe);
        }

        return null;
    }

}
