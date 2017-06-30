package ru.programpark.mirah.lexer;

import org.netbeans.modules.csl.api.Severity;
import org.openide.filesystems.FileObject;

import javax.tools.Diagnostic;

/**
 * Created by kozyr on 20.06.2016.
 */
public class ParserError implements org.netbeans.modules.csl.api.Error {

    final int line;
    final int startPos;
    final String description;
    final FileObject file;
    final int endPos;
    private final Diagnostic.Kind kind;

    public ParserError(Diagnostic.Kind kind, long startPosition, long endPosition, long lineNumber, String message, FileObject file) {
        this.line = (int) lineNumber;
        this.description = message;
        this.file = file;
        this.startPos = (int) startPosition;
        this.endPos = (int) endPosition;
        this.kind = kind;

    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getDisplayName() {
        return description;
    }

    @Override
    public String getKey() {
        return description;
    }

    @Override
    public FileObject getFile() {
        return file;
    }

    @Override
    public int getStartPosition() {
        return startPos;
    }

    @Override
    public int getEndPosition() {
        return endPos;
    }

    @Override
    public boolean isLineError() {
        return false;
    }

    @Override
    public Severity getSeverity() {
        switch (kind) {
            case OTHER: return Severity.INFO;
            case WARNING: return Severity.WARNING;
            case NOTE: return Severity.INFO;
            default: return Severity.ERROR;
        }
    }

    @Override
    public Object[] getParameters() {
        return null;
    }

}
