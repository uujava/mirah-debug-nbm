package ru.programpark.mirah.lexer;

import org.openide.filesystems.FileObject;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by kozyr on 20.06.2016.
 */
public class ParseErrorListener implements DiagnosticListener {

    private final FileObject fo;
    private List<ParserError> errors = new ArrayList<>();
    private int errorCount = 0;

    public ParseErrorListener(FileObject fo){
        this.fo = fo;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public List<ParserError> getErrors() {
        return errors;
    }

    @Override
    public void report(Diagnostic dgnstc) {
        String message = dgnstc.getMessage(Locale.getDefault());
        errors.add(new ParserError(dgnstc.getKind(), dgnstc.getStartPosition(), dgnstc.getEndPosition(), dgnstc.getLineNumber(), message, fo));
        if (dgnstc.getKind() == Diagnostic.Kind.ERROR) errorCount++;
    }
}
