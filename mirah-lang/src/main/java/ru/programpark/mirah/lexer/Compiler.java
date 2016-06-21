package ru.programpark.mirah.lexer;

import mirah.lang.ast.Node;
import org.mirah.jvm.mirrors.debug.DebuggerInterface;

import javax.tools.DiagnosticListener;
import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * Created by kozyr on 04.03.2016.
 */
public class Compiler {
    private String sourcePath;
    private File destinationDirectory;
    private DiagnosticListener diagnostics;
    private String classPath;
    private DebuggerInterface debugger;
    private String content;
    private String path;

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public void setDestinationDirectory(File destinationDirectory) {
        this.destinationDirectory = destinationDirectory;
    }

    public void setDiagnostics(DiagnosticListener diagnostics) {
        this.diagnostics = diagnostics;
    }

    public void setClassPath(String classPath) {
        this.classPath = classPath;
    }

    public void setDebugger(DebuggerInterface debugger) {
        this.debugger = debugger;
    }

    public void addFakeFile(String relPath, String srcText) {
        content = srcText;
        path = relPath;
    }

    public List<Node> getParsedNodes() {
        return Collections.EMPTY_LIST;
    }

    public void compile() {

    }
}
