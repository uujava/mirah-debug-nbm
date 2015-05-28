package ru.programpark.mirah.editor.ast;

import mirah.lang.ast.ClassDefinition;


/**
 * interface to mark a AstNode as Variable. Typically these are 
 * VariableExpression, FieldNode, PropertyNode and Parameter
 * 
 * @author Jochen Theodorou
 */
public interface Variable {
    
    /**
     * the type of the variable
     */
    ClassDefinition getType();
    
    /**
     * the type before wrapping primitives type of the variable
     */
    ClassDefinition getOriginType();
    
    /**
     * the name of the variable
     */
    String getName();
    
    /**
     * expression used to initialize the variable or null of there
     * is no initialization.
     */
//    Expression getInitialExpression();
    
    /**
     * returns true if there is an initialization expression
     */
    boolean hasInitialExpression();
    
    /**
     * returns true if this variable is used in a static context.
     * A static context is any static initializer block, when this variable
     * is declared as static or when this variable is used in a static method 
     */
    boolean isInStaticContext();

    boolean isDynamicTyped();
    boolean isClosureSharedVariable();
    void setClosureSharedVariable(boolean inClosure);

    int getModifiers();
}
