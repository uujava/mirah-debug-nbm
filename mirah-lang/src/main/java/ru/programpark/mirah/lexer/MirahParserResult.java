package ru.programpark.mirah.lexer;

import mirah.lang.ast.Node;
import org.mirah.typer.ResolvedType;
import org.netbeans.modules.csl.spi.ParserResult;
import org.netbeans.modules.parsing.api.Snapshot;
import org.openide.filesystems.FileObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by kozyr on 20.06.2016.
 */
public class MirahParserResult extends ParserResult {
    private static final Logger logger = Logger.getLogger(MirahParserResult.class.getName());
    private final ParseErrorListener diagnostics;
    List<ParserError> errorList = null;
    List parsedNodes;
    Map<Node, ResolvedType> resolvedTypes = null;
    private BlockCollector blockCollector = null;
    private FileObject fileObject;

    MirahParserResult(Snapshot snapshot, ParseErrorListener diagnostics) {
        super(snapshot);
        fileObject = snapshot.getSource().getFileObject();
        this.diagnostics = diagnostics;
    }

    @Override
    public List<ParserError> getDiagnostics() {
        if (errorList == null) {
            errorList = new ArrayList<>();
            if (this.diagnostics != null) {
                for (ParserError syntaxError : this.diagnostics.getErrors()) {
                    addError(syntaxError);
                }
            }
        }
        return errorList;
    }

    @Override
    protected void invalidate() {
        if (logger.isLoggable(Level.FINE))
            logger.log(Level.FINE, "invalidate: " + this);
    }

    public synchronized BlockCollector getBlockCollection() {
        if (blockCollector == null) {
            blockCollector = new BlockCollector();
            blockCollector.prepareBlocks(this);
        }
        return blockCollector;
    }

    public Node getRoot() {
        if (parsedNodes == null) return null;
        for (Object node : parsedNodes) {
            if (node instanceof Node) return (Node) node;
        }
        return null;
    }

    public List getParsedNodes() {
        return parsedNodes;
    }

    public void setParsedNodes(List parsed) {
        this.parsedNodes = parsed;
    }

    public Map<Node, ResolvedType> getResolvedTypes() {
        return resolvedTypes;
    }

    public void setResolvedTypes(Map<Node, ResolvedType> resolvedTypes) {
        this.resolvedTypes = resolvedTypes;
    }

    public ResolvedType getResolvedType(Node node) {
        if (resolvedTypes == null) return null;
        return resolvedTypes.get(node);
    }

    private void addError(ParserError error) {
        errorList.add(error);
    }

    public FileObject getFileObject() {
        return fileObject;
    }

    @Override
    public String toString() {
        return "MirahParserResult{" +
                "diagnostics=" + diagnostics +
                ", errorList=" + errorList +
                ", parsedNodes=" + parsedNodes +
                ", resolvedTypes=" + resolvedTypes +
                ", blockCollector=" + blockCollector +
                ", fileObject=" + fileObject +
                '}';
    }
}
