package ru.programpark.mirah.editor.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import mirah.lang.ast.ClassDefinition;
import ru.programpark.mirah.editor.ast.ClassHelper;
import ru.programpark.mirah.editor.ast.Variable;

/**
 * Handy methods when working with the Mirah AST
 *
 * @author Guillaume Laforge
 * @author Paul King
 * @author Andre Steingress
 * @author Graeme Rocher
 */
public class GeneralUtils {
//    public static final Token ASSIGN = Token.newSymbol(Types.ASSIGN, -1, -1);
//    public static final Token EQ = Token.newSymbol(Types.COMPARE_EQUAL, -1, -1);
//    public static final Token NE = Token.newSymbol(Types.COMPARE_NOT_EQUAL, -1, -1);
//    public static final Token LT = Token.newSymbol(Types.COMPARE_LESS_THAN, -1, -1);
//    public static final Token AND = Token.newSymbol(Types.LOGICAL_AND, -1, -1);
//    public static final Token OR = Token.newSymbol(Types.LOGICAL_OR, -1, -1);
//    public static final Token CMP = Token.newSymbol(Types.COMPARE_TO, -1, -1);
//    private static final Token INSTANCEOF = Token.newSymbol(Types.KEYWORD_INSTANCEOF, -1, -1);
//    private static final Token PLUS = Token.newSymbol(Types.PLUS, -1, -1);
//    private static final Token INDEX = Token.newSymbol("[", -1, -1);

//    public static BinaryExpression andX(Expression lhv, Expression rhv) {
//        return new BinaryExpression(lhv, AND, rhv);
//    }
/*
    public static ArgumentListExpression args(Expression... expressions) {
        List<Expression> args = new ArrayList<Expression>();
        Collections.addAll(args, expressions);
        return new ArgumentListExpression(args);
    }

    public static ArgumentListExpression args(List<Expression> expressions) {
        return new ArgumentListExpression(expressions);
    }

    public static ArgumentListExpression args(Parameter[] parameters) {
        return new ArgumentListExpression(parameters);
    }

    public static ArgumentListExpression args(String... names) {
        List<Expression> vars = new ArrayList<Expression>();
        for (String name : names) {
            vars.add(varX(name));
        }
        return new ArgumentListExpression(vars);
    }

    public static Statement assignS(Expression target, Expression value) {
        return null; //new ExpressionStatement(assignX(target, value));
    }
*/
//    public static Expression assignX(Expression target, Expression value) {
//        return new BinaryExpression(target, ASSIGN, value);
//    }
//
//    public static Expression attrX(Expression oe, Expression prop) {
//        return new AttributeExpression(oe, prop);
//    }
//
//    public static BlockStatement block(VariableScope varScope, Statement... stmts) {
//        BlockStatement block = new BlockStatement();
//        block.setVariableScope(varScope);
//        for (Statement stmt : stmts) block.addStatement(stmt);
//        return block;
//    }
//
//    public static BlockStatement block(Statement... stmts) {
//        BlockStatement block = new BlockStatement();
//        for (Statement stmt : stmts) block.addStatement(stmt);
//        return block;
//    }
/*
    public static MethodCallExpression callSuperX(String methodName, Expression args) {
        return callX(varX("super"), methodName, args);
    }

    public static MethodCallExpression callSuperX(String methodName) {
        return callSuperX(methodName, MethodCallExpression.NO_ARGUMENTS);
    }

    public static MethodCallExpression callThisX(String methodName, Expression args) {
        return callX(varX("this"), methodName, args);
    }

    public static MethodCallExpression callThisX(String methodName) {
        return callThisX(methodName, MethodCallExpression.NO_ARGUMENTS);
    }

    public static MethodCallExpression callX(Expression receiver, String methodName, Expression args) {
        return new MethodCallExpression(receiver, methodName, args);
    }

    public static MethodCallExpression callX(Expression receiver, Expression method, Expression args) {
        return new MethodCallExpression(receiver, method, args);
    }

    public static MethodCallExpression callX(Expression receiver, String methodName) {
        return callX(receiver, methodName, MethodCallExpression.NO_ARGUMENTS);
    }
*/
//    public static StaticMethodCallExpression callX(ClassDefinition receiver, String methodName, Expression args) {
//        return new StaticMethodCallExpression(receiver, methodName, args);
//    }
//
//    public static StaticMethodCallExpression callX(ClassDefinition receiver, String methodName) {
//        return callX(receiver, methodName, MethodCallExpression.NO_ARGUMENTS);
//    }
//
//    public static CastExpression castX(ClassDefinition type, Expression expression) {
//        return new CastExpression(type, expression);
//    }
//
//    public static CastExpression castX(ClassDefinition type, Expression expression, boolean ignoreAutoboxing) {
//        return new CastExpression(type, expression, ignoreAutoboxing);
//    }
/*
    public static ClassExpression classX(ClassDefinition clazz) {
        return new ClassExpression(clazz);
    }
*/
//    public static ClassExpression classX(Class clazz) {
//        return classX(ClassHelper.make(clazz).getPlainNodeReference());
//    }
//
//    public static ClosureExpression closureX(Parameter[] params, Statement code) {
//        return new ClosureExpression(params, code);
//    }
//
//    public static ClosureExpression closureX(Statement code) {
//        return closureX(Parameter.EMPTY_ARRAY, code);
//    }
//
//    public static Parameter[] cloneParams(Parameter[] source) {
//        Parameter[] result = new Parameter[source.length];
//        for (int i = 0; i < source.length; i++) {
//            Parameter srcParam = source[i];
//            Parameter dstParam = new Parameter(srcParam.getOriginType(), srcParam.getName());
//            result[i] = dstParam;
//        }
//        return result;
//    }

//    public static BinaryExpression cmpX(Expression lhv, Expression rhv) {
//        return new BinaryExpression(lhv, CMP, rhv);
//    }

//    public static ConstantExpression constX(Object val) {
//        return new ConstantExpression(val);
//    }
//
//    public static ConstantExpression constX(Object val, boolean keepPrimitive) {
//        return new ConstantExpression(val, keepPrimitive);
//    }

    /**
     * Copies all <tt>candidateAnnotations</tt> with retention policy {@link java.lang.annotation.RetentionPolicy#RUNTIME}
     * and {@link java.lang.annotation.RetentionPolicy#CLASS}.
     * <p>
     * Annotations with {@link org.codehaus.groovy.runtime.GeneratedClosure} members are not supported at present.
     */
    /*
    public static void copyAnnotatedNodeAnnotations(final AnnotatedNode annotatedNode, final List<AnnotationNode> copied, List<AnnotationNode> notCopied) {
        List<AnnotationNode> annotationList = annotatedNode.getAnnotations();
        for (AnnotationNode annotation : annotationList)  {

            List<AnnotationNode> annotations = null; //annotation.getClassDefinition().getAnnotations(AbstractASTTransformation.RETENTION_CLASSNODE);
            if (annotations.isEmpty()) continue;

            if (hasClosureMember(annotation)) {
                notCopied.add(annotation);
                continue;
            }

            AnnotationNode retentionPolicyAnnotation = annotations.get(0);
            Expression valueExpression = retentionPolicyAnnotation.getMember("value");
//            if (!(valueExpression instanceof PropertyExpression)) continue;

//            PropertyExpression propertyExpression = (PropertyExpression) valueExpression;
//            boolean processAnnotation =
//                    propertyExpression.getProperty() instanceof ConstantExpression &&
//                            (
//                                    "RUNTIME".equals(((ConstantExpression) (propertyExpression.getProperty())).getValue()) ||
//                                            "CLASS".equals(((ConstantExpression) (propertyExpression.getProperty())).getValue())
//                            );
//
//            if (processAnnotation)  {
//                AnnotationNode newAnnotation = new AnnotationNode(annotation.getClassDefinition());
//                for (Map.Entry<String, Expression> member : annotation.getMembers().entrySet())  {
//                    newAnnotation.addMember(member.getKey(), member.getValue());
//                }
//                newAnnotation.setSourcePosition(annotatedNode);
//
//                copied.add(newAnnotation);
//            }
        }
    }

    public static Statement createConstructorStatementDefault(FieldDeclaration fNode) {
        final String name = fNode.getName();
        final ClassDefinition fType = fNode.getType();
        final Expression fieldExpr = null; //propX(varX("this"), name);
        Expression initExpr = fNode.getInitialValueExpression();
        Statement assignInit;
        if (initExpr == null || (initExpr instanceof ConstantExpression && ((ConstantExpression)initExpr).isNullExpression())) {
            if (ClassHelper.isPrimitiveType(fType)) {
                assignInit = EmptyStatement.INSTANCE;
            } else {
                assignInit = assignS(fieldExpr, ConstantExpression.EMPTY_EXPRESSION);
            }
        } else {
            assignInit = assignS(fieldExpr, initExpr);
        }
        fNode.setInitialValueExpression(null);
        Expression value = null; //findArg(name);
        return null; //ifElseS(equalsNullX(value), assignInit, assignS(fieldExpr, castX(fType, value)));
    }

    public static ConstructorCallExpression ctorX(ClassDefinition type, Expression args) {
        return new ConstructorCallExpression(type, args);
    }

    public static ConstructorCallExpression ctorX(ClassDefinition type) {
        return new ConstructorCallExpression(type, ArgumentListExpression.EMPTY_ARGUMENTS);
    }

    public static Statement ctorSuperS(Expression args) {
        return stmt(ctorX(ClassDefinition.SUPER, args));
    }

    public static Statement ctorThisS(Expression args) {
        return stmt(ctorX(ClassDefinition.THIS, args));
    }

    public static Statement ctorSuperS() {
        return stmt(ctorX(ClassDefinition.SUPER));
    }

    public static Statement ctorThisS() {
        return stmt(ctorX(ClassDefinition.THIS));
    }

//    public static Statement declS(Expression target, Expression init) {
//        return new ExpressionStatement(new DeclarationExpression(target, ASSIGN, init));
//    }
//
//    public static BinaryExpression eqX(Expression lhv, Expression rhv) {
//        return new BinaryExpression(lhv, EQ, rhv);
//    }

    public static BooleanExpression equalsNullX(Expression argExpr) {
        return null; //new BooleanExpression(eqX(argExpr, new ConstantExpression(null)));
    }

//    public static FieldExpression fieldX(FieldDeclaration fieldNode) {
//        return new FieldExpression(fieldNode);
//    }

//    public static FieldExpression fieldX(ClassDefinition owner, String fieldName) {
//        return new FieldExpression(owner.getField(fieldName));
//    }

//    public static Expression findArg(String argName) {
//        return new PropertyExpression(new VariableExpression("args"), argName);
//    }

    public static List<MethodDefinition> getAllMethods(ClassDefinition type) {
        ClassDefinition node = type;
        List<MethodDefinition> result = new ArrayList<MethodDefinition>();
        while (node != null) {
            result.addAll(node.getMethods());
            node = node.getSuperClass();
        }
        return result;
    }

    public static List<PropertyNode> getAllProperties(ClassDefinition type) {
        ClassDefinition node = type;
        List<PropertyNode> result = new ArrayList<PropertyNode>();
        while (node != null) {
            result.addAll(node.getProperties());
            node = node.getSuperClass();
        }
        return result;
    }

    public static String getGetterName(PropertyNode pNode) {
        return "get"; // + Verifier.capitalize(pNode.getName());
    }

    public static List<FieldDeclaration> getInstanceNonPropertyFields(ClassDefinition cNode) {
        final List<FieldDeclaration> result = new ArrayList<FieldDeclaration>();
        for (FieldDeclaration fNode : cNode.getFields()) {
            if (!fNode.isStatic() && cNode.getProperty(fNode.getName()) == null) {
                result.add(fNode);
            }
        }
        return result;
    }

    public static List<PropertyNode> getInstanceProperties(ClassDefinition cNode) {
        final List<PropertyNode> result = new ArrayList<PropertyNode>();
        for (PropertyNode pNode : cNode.getProperties()) {
            if (!pNode.isStatic()) {
                result.add(pNode);
            }
        }
        return result;
    }

    public static List<FieldDeclaration> getInstancePropertyFields(ClassDefinition cNode) {
        final List<FieldDeclaration> result = new ArrayList<FieldDeclaration>();
        for (PropertyNode pNode : cNode.getProperties()) {
            if (!pNode.isStatic()) {
                result.add(pNode.getField());
            }
        }
        return result;
    }

    public static Set<ClassDefinition> getInterfacesAndSuperInterfaces(ClassDefinition type) {
        Set<ClassDefinition> res = new HashSet<ClassDefinition>();
        if (type.isInterface()) {
            res.add(type);
            return res;
        }
        ClassDefinition next = type;
        while (next != null) {
            Collections.addAll(res, next.getInterfaces());
            next = next.getSuperClass();
        }
        return res;
    }

    public static List<FieldDeclaration> getSuperNonPropertyFields(ClassDefinition cNode) {
        final List<FieldDeclaration> result;
        if (cNode == ClassHelper.OBJECT_TYPE) {
            result = new ArrayList<FieldDeclaration>();
        } else {
            result = getSuperNonPropertyFields(cNode.getSuperClass());
        }
        for (FieldDeclaration fNode : cNode.getFields()) {
            if (!fNode.isStatic() && cNode.getProperty(fNode.getName()) == null) {
                result.add(fNode);
            }
        }
        return result;
    }

    public static List<FieldDeclaration> getSuperPropertyFields(ClassDefinition cNode) {
        final List<FieldDeclaration> result;
        if (cNode == ClassHelper.OBJECT_TYPE) {
            result = new ArrayList<FieldDeclaration>();
        } else {
            result = getSuperPropertyFields(cNode.getSuperClass());
        }
        for (PropertyNode pNode : cNode.getProperties()) {
            if (!pNode.isStatic()) {
                result.add(pNode.getField());
            }
        }
        return result;
    }

    public static BinaryExpression hasClassX(Expression instance, ClassDefinition cNode) {
        return null; //eqX(classX(cNode), callX(instance, "getClass"));
    }

    private static boolean hasClosureMember(AnnotationNode annotation) {

        Map<String, Expression> members = annotation.getMembers();
        for (Map.Entry<String, Expression> member : members.entrySet())  {
            if (member.getValue() instanceof ClosureExpression) return true;

            if (member.getValue() instanceof ClassExpression)  {
                ClassExpression classExpression = (ClassExpression) member.getValue();
                Class<?> typeClass = classExpression.getType().isResolved() ? classExpression.getType().redirect().getTypeClass() : null;
//                if (typeClass != null && GeneratedClosure.class.isAssignableFrom(typeClass)) return true;
            }
        }

        return false;
    }
*/
    public static boolean hasDeclaredMethod(ClassDefinition cNode, String name, int argsCount) {
//        List<MethodDefinition> ms = cNode.getDeclaredMethods(name);
//        for (MethodDefinition m : ms) {
//            Parameter[] paras = m.getParameters();
//            if (paras != null && paras.length == argsCount) {
//                return true;
//            }
//        }
        return false;
    }

//    public static BinaryExpression hasEqualFieldX(FieldDeclaration fNode, Expression other) {
//        return eqX(varX(fNode), propX(other, fNode.getName()));
//    }

//    public static BinaryExpression hasEqualPropertyX(PropertyNode pNode, Expression other) {
//        String getterName = getGetterName(pNode);
//        return eqX(callThisX(getterName), callX(other, getterName));
//    }

//    public static BooleanExpression hasSameFieldX(FieldDeclaration fNode, Expression other) {
//        return sameX(varX(fNode), propX(other, fNode.getName()));
//    }
/*
    public static BooleanExpression hasSamePropertyX(PropertyNode pNode, Expression other) {
        String getterName = getGetterName(pNode);
        return sameX(callThisX(getterName), callX(other, getterName));
    }

    public static Statement ifElseS(Expression cond, Statement thenStmt, Statement elseStmt) {
        return new IfStatement(
                cond instanceof BooleanExpression ? (BooleanExpression) cond : new BooleanExpression(cond),
                thenStmt,
                elseStmt
        );
    }

    public static Statement ifS(Expression cond, Expression trueExpr) {
        return ifS(cond, new ExpressionStatement(trueExpr));
    }

    public static Statement ifS(Expression cond, Statement trueStmt) {
        return new IfStatement(
                cond instanceof BooleanExpression ? (BooleanExpression) cond : new BooleanExpression(cond),
                trueStmt,
                EmptyStatement.INSTANCE
        );
    }
*/
//    public static Expression indexX(Expression target, Expression value) {
//        return new BinaryExpression(target, INDEX, value);
//    }

//    public static BooleanExpression isInstanceOfX(Expression objectExpression, ClassDefinition cNode) {
//        return new BooleanExpression(new BinaryExpression(objectExpression, INSTANCEOF, classX(cNode)));
//    }

//    public static BooleanExpression isOneX(Expression expr) {
//        return new BooleanExpression(new BinaryExpression(expr, EQ, new ConstantExpression(1)));
//    }

    public static boolean isOrImplements(ClassDefinition type, ClassDefinition interfaceType) {
        return true; //type.equals(interfaceType) || type.implementsInterface(interfaceType);
    }

//    public static BooleanExpression isTrueX(Expression argExpr) {
//        return new BooleanExpression(new BinaryExpression(argExpr, EQ, new ConstantExpression(Boolean.TRUE)));
//    }

//    public static BooleanExpression isZeroX(Expression expr) {
//        return new BooleanExpression(new BinaryExpression(expr, EQ, new ConstantExpression(0)));
//    }

//    public static BinaryExpression ltX(Expression lhv, Expression rhv) {
//        return new BinaryExpression(lhv, LT, rhv);
//    }
/*
    public static String makeDescriptorWithoutReturnType(MethodDefinition mn) {
        StringBuilder sb = new StringBuilder();
        sb.append(mn.getName()).append(':');
        for (Parameter p : mn.getParameters()) {
            sb.append(p.getType()).append(',');
        }
        return sb.toString();
    }
*/
//    public static BinaryExpression neX(Expression lhv, Expression rhv) {
//        return new BinaryExpression(lhv, NE, rhv);
//    }

//    public static BooleanExpression notNullX(Expression argExpr) {
//        return new BooleanExpression(new BinaryExpression(argExpr, NE, new ConstantExpression(null)));
//    }

//    public static NotExpression notX(Expression expr) {
//        return new NotExpression(expr instanceof BooleanExpression ? expr : new BooleanExpression(expr));
//    }

//    public static BinaryExpression orX(Expression lhv, Expression rhv) {
//        return new BinaryExpression(lhv, OR, rhv);
//    }
/*
    public static Parameter param(ClassDefinition type, String name) {
        return param(type, name, null);
    }

    public static Parameter param(ClassDefinition type, String name, Expression initialExpression) {
        Parameter param = new Parameter(type, name);
        if (initialExpression != null) {
            param.setInitialExpression(initialExpression);
        }
        return param;
    }

    public static Parameter[] params(Parameter... params) {
        return params != null ? params : Parameter.EMPTY_ARRAY;
    }
*/
//    public static BinaryExpression plusX(Expression lhv, Expression rhv) {
//        return new BinaryExpression(lhv, PLUS, rhv);
//    }

//    public static Expression propX(Expression owner, String property) {
//        return new PropertyExpression(owner, property);
//    }

//    public static Expression propX(Expression owner, Expression property) {
//        return new PropertyExpression(owner, property);
//    }
/*
    public static Statement returnS(Expression expr) {
        return new ReturnStatement(new ExpressionStatement(expr));
    }

    public static Statement safeExpression(Expression fieldExpr, Expression expression) {
        return new IfStatement(
                equalsNullX(fieldExpr),
                new ExpressionStatement(fieldExpr),
                new ExpressionStatement(expression));
    }

    public static BooleanExpression sameX(Expression self, Expression other) {
        return new BooleanExpression(callX(self, "is", args(other)));
    }

    public static Statement stmt(Expression expr) {
        return new ExpressionStatement(expr);
    }
*/
//    public static TernaryExpression ternaryX(Expression cond, Expression trueExpr, Expression elseExpr) {
//        return new TernaryExpression(
//                cond instanceof BooleanExpression ? (BooleanExpression) cond : new BooleanExpression(cond),
//                trueExpr,
//                elseExpr);
//    }
/*
    public static VariableExpression varX(String name) {
        return new VariableExpression(name);
    }

    public static VariableExpression varX(Variable variable) {
        return new VariableExpression(variable);
    }

    public static VariableExpression varX(String name, ClassDefinition type) {
        return new VariableExpression(name, type);
    }
*/
    /**
     * This method is similar to {@link #propX(Expression, Expression)} but will make sure that if the property
     * being accessed is defined inside the classnode provided as a parameter, then a getter call is generated
     * instead of a field access.
     * @param annotatedNode the class node where the property node is accessed from
     * @param pNode the property being accessed
     * @return a method call expression or a property expression
     */
//    public static Expression getterX(ClassDefinition annotatedNode, PropertyNode pNode) {
//        ClassDefinition owner = pNode.getDeclaringClass();
//        if (annotatedNode.equals(owner)) {
//            String getterName = "get" + MetaClassHelper.capitalize(pNode.getName());
//            if (ClassHelper.boolean_TYPE.equals(pNode.getOriginType())) {
//                getterName = "is" + MetaClassHelper.capitalize(pNode.getName());
//            }
//            return callX(new VariableExpression("this"), getterName, ArgumentListExpression.EMPTY_ARGUMENTS);
//        }
//        return propX(new VariableExpression("this"), pNode.getName());
//    }
}
