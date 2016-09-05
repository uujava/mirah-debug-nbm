package ru.programpark.mirah.compiler;

import org.mirah.util.SimpleDiagnostics;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;

/**
* Created by user on 6/22/2016.
*/
public class ErrorCountListener extends SimpleDiagnostics {

    int errors = 0;

    public ErrorCountListener() {
        super(false);
    }

    @Override
    public int errorCount() {
        return errors;
    }

    @Override

    public void report(Diagnostic diagnostic) {
        if(diagnostic.getKind() == Diagnostic.Kind.ERROR){
            errors +=1;
        }
    }
}
