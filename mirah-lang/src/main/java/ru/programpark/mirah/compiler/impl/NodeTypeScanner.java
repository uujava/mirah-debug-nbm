package ru.programpark.mirah.compiler.impl;

import mirah.lang.ast.*;
import mirah.lang.ast.Package;
import mirah.objectweb.asm.Type;
import org.mirah.jvm.types.JVMType;
import org.mirah.typer.ErrorType;
import org.mirah.typer.MethodType;
import org.mirah.typer.ResolvedType;
import ru.programpark.mirah.compiler.types.ErrorJType;
import ru.programpark.mirah.compiler.types.JType;
import ru.programpark.mirah.compiler.types.MType;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by kozyr on 12.09.2016.
 */
class NodeTypeScanner extends NodeScanner {
    private static final Logger logger = Logger.getLogger(NodeTypeScanner.class.getName());
    private MirahInteractiveCompiler incrementalCompiler;

    public NodeTypeScanner(MirahInteractiveCompiler incrementalCompiler) {
        this.incrementalCompiler = incrementalCompiler;
    }

    @Override
    public boolean enterDefault(Node node, Object typeMap) {
        processNode(node, (Map<Node, ResolvedType>) typeMap);
        return true;
    }

    @Override
    public boolean enterColon2(Colon2 node, Object arg) {
        return false;
    }

    @Override
    public boolean enterUnquote(Unquote node, Object typeMap) {
        processNode(node, (Map<Node, ResolvedType>) typeMap);
        return false;
    }

    @Override
    public boolean enterConstant(Constant node, Object arg) {
        return false;
    }

    @Override
    public boolean enterPackage(Package node, Object arg) {
        enterDefault(node, arg);
        return false;
    }

    @Override
    public boolean enterImport(Import node, Object arg) {
        enterDefault(node, arg);
        return false;
    }

    @Override
    public boolean enterTypeRefImpl(TypeRefImpl node, Object arg) {
        return false;
    }

    @Override
    public boolean enterSimpleString(SimpleString node, Object arg) {
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
        resolvedType = convert(resolvedType);
        typeMap.put(node, resolvedType);
    }

    private ResolvedType convert(ResolvedType resolvedType) {
        if (resolvedType instanceof JVMType) {
            Type asmType = ((JVMType) resolvedType).getAsmType();
            resolvedType = new JType(asmType, resolvedType.isBlock());
        } else if (resolvedType instanceof ErrorType) {
            resolvedType = new ErrorJType(resolvedType.name(), resolvedType.toString());
        } else if (resolvedType instanceof MethodType) {
            ResolvedType returnType = ((MethodType) resolvedType).returnType();
            if(returnType instanceof JVMType){
                resolvedType = new MType(((JVMType)returnType).getAsmType());
            } else {
                logger.severe("unknown method return type: " + returnType);
                resolvedType = new ErrorJType(resolvedType.name(), "Bad method type: " + returnType);
            }
        } else {
            logger.severe("skip unknown type: " + resolvedType);
        }
        return resolvedType;
    }

}
