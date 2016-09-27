package ru.programpark.mirah.compiler;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import java.util.ArrayList;
import java.util.Locale;
import mirah.lang.ast.CodeSource;
/**
 * Created by kozyr on 22.09.2016.
 */
public class ErrorLogger implements DiagnosticListener {
    private ArrayList<String> errors = new ArrayList<>();

    private int count = 0;
    @Override
    public void report(Diagnostic diagnostic) {
        errors.add(diagnostic.getMessage(Locale.getDefault()) + " at: " +((CodeSource)diagnostic.getSource()).name() + " line:" + diagnostic.getLineNumber() + " col:"+diagnostic.getColumnNumber());
        count++;
    }

    @Override
    public String toString() {
        return "ErrorLogger{" +
                "errors=" + errors +
                '}';
    }

    public int getErrorCount(){
        return count;
    }
}
