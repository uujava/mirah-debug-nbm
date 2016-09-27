package ru.programpark.mirah.compiler.impl;

import mirah.lang.ast.*;
import org.mirah.typer.ResolvedType;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by kozyr on 12.09.2016.
 */
class NodeTypeScanner extends NodeScanner {
    private static final Logger logger = Logger.getLogger(NodeTypeScanner.class.getName());
    private MirahIncrementalCompiler incrementalCompiler;

    public NodeTypeScanner(MirahIncrementalCompiler incrementalCompiler) {
        this.incrementalCompiler = incrementalCompiler;
    }

    @Override
    public boolean enterDefault(Node node, Object typeMap) {
        processNode(node, (Map<Node, ResolvedType>) typeMap);
        return true;
    }

    @Override
    public boolean enterUnquote(Unquote node, Object typeMap) {
        processNode(node, (Map<Node, ResolvedType>) typeMap);
        return false;
    }

    @Override
    public boolean enterOptionalArgumentList(OptionalArgumentList node, Object arg) {
        return false;
    }

    @Override
    public boolean enterRequiredArgumentList(RequiredArgumentList node, Object arg) {
        return false;
    }

    @Override
    public boolean enterFieldAnnotationRequest(FieldAnnotationRequest node, Object arg) {
        return false;
    }

    @Override
    public boolean enterStringPieceList(StringPieceList node, Object arg) {
        return false;
    }

    @Override
    public boolean enterModifierList(ModifierList node, Object arg) {
        return false;
    }

    @Override
    public boolean enterTypeNameList(TypeNameList node, Object arg) {
        return false;
    }

    @Override
    public boolean enterAnnotationList(AnnotationList node, Object arg) {
        return false;
    }

    @Override
    public boolean enterNodeList(NodeList node, Object arg) {
        return true;
    }

    private void processNode(Node node, Map<Node, ResolvedType> typeMap) {
        ResolvedType resolvedType = incrementalCompiler.type(node);
        typeMap.put(node, resolvedType);
    }

}
