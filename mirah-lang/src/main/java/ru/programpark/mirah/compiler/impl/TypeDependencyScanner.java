package ru.programpark.mirah.compiler.impl;

import mirah.lang.ast.MethodDefinition;
import mirah.lang.ast.Node;
import mirah.lang.ast.NodeScanner;

/**
 * Scan for
 * - super class
 * - direct super interfaces
 * - method parameters
 * - method return types
 * - skip other Nodes
 */
public class TypeDependencyScanner extends NodeScanner{
    @Override
    public boolean enterDefault(Node node, Object arg) {
        return false;
    }

    @Override
    public boolean enterMethodDefinition(MethodDefinition node, Object arg) {
        return super.enterMethodDefinition(node, arg);
    }
}
