package ru.programpark.mirah.compiler.impl;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import mirah.lang.ast.*;
import mirah.objectweb.asm.Type;
import org.mirah.jvm.types.JVMType;
import org.mirah.typer.ResolvedType;

import java.util.Map;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Scan for external dependencies
 * - super class
 * - direct super interfaces
 * - method parameters
 * - method return types
 * - skip other Nodes
 */
public class TypeDependencyScanner extends NodeScanner {

    static final Logger logger = Logger.getLogger(TypeDependencyScanner.class.getName());
    private final Map<Node, ResolvedType> nodeTypes;
    private final MutableGraph<TypeNode> typeGraph;
    private Stack<TypeNode> current;

    public TypeDependencyScanner(Map<Node, ResolvedType> nodeTypes) {
        this.nodeTypes = nodeTypes;
        typeGraph = GraphBuilder.directed().allowsSelfLoops(true).build();
        current = new Stack<>();
    }

    @Override
    public boolean enterDefault(Node node, Object arg) {
        return false;
    }

    @Override
    public boolean enterMethodDefinition(MethodDefinition node, Object arg) {
        ResolvedType target = nodeTypes.get(node);
        if (target instanceof JVMType) {
            if (logger.isLoggable(Level.FINEST)) logger.finest("method def: " + node + " target: " + target);
            Type asmType = ((JVMType) target).getAsmType();
            TypeNode methodNode = new TypeNode(asmType, NodeKind.METHOD);
            typeGraph.putEdge(current.peek(), methodNode);
            Type[] argumentTypes = asmType.getArgumentTypes();
            for (Type argumentType : argumentTypes) {
                typeGraph.putEdge(methodNode, new TypeNode(argumentType, NodeKind.PARAM));
            }
            typeGraph.putEdge(methodNode, new TypeNode(asmType.getReturnType(), NodeKind.RETURN));
        }
        return false;
    }

    @Override
    public boolean enterStaticMethodDefinition(StaticMethodDefinition node, Object arg) {
        return enterMethodDefinition(node, arg);
    }


    @Override
    public Object exitStaticMethodDefinition(StaticMethodDefinition node, Object arg) {
        return exitMethodDefinition(node, arg);
    }

    @Override
    public Object exitMethodDefinition(MethodDefinition node, Object arg) {
        return current.peek();
    }

    @Override
    public boolean enterClassDefinition(ClassDefinition node, Object arg) {
        ResolvedType target = nodeTypes.get(node);
        if (target instanceof JVMType) {
            if (logger.isLoggable(Level.FINEST)) logger.finest("method def: " + node + " target: " + target);
            Type asmType = ((JVMType) target).getAsmType();
            TypeNode classNode = new TypeNode(asmType, NodeKind.METHOD);
            typeGraph.addNode(classNode);
            current.push(classNode);
        }
        return true;
    }

    @Override
    public boolean enterInterfaceDeclaration(InterfaceDeclaration node, Object arg) {
        return enterClassDefinition(node, arg);
    }

    @Override
    public Object exitInterfaceDeclaration(InterfaceDeclaration node, Object arg) {
        return exitClassDefinition(node, arg);
    }

    @Override
    public Object exitClassDefinition(ClassDefinition node, Object arg) {
        return current.pop();
    }

    public enum NodeKind {
        TYPE, PARAM, METHOD, RETURN
    }

    private static class TypeNode {
        private final NodeKind kind;
        private Type name;

        public TypeNode(Type name, NodeKind kind) {
            if (name == null || kind == null)
                throw new IllegalArgumentException("null kind: " + name + " or kind: " + name);
            this.name = name;
            this.kind = kind;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TypeNode)) return false;

            TypeNode typeNode = (TypeNode) o;

            if (kind != typeNode.kind) return false;
            return name.equals(typeNode.name);

        }

        @Override
        public int hashCode() {
            int result = kind.hashCode();
            result = 31 * result + name.hashCode();
            return result;
        }
    }

}
