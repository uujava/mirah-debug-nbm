package ru.programpark.mirah.compiler;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import java.util.ArrayList;

/**
 * Created by kozyr on 22.09.2016.
 */
class ErrorLogger implements DiagnosticListener {
    private ArrayList<Diagnostic> errors = new ArrayList<>();

    @Override
    public void report(Diagnostic diagnostic) {

    }
}
