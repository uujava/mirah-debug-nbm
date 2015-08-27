package ru.programpark.mirah.editor.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import mirah.lang.ast.AnnotationList;
import mirah.lang.ast.Array;
import mirah.lang.ast.AttrAssign;
import mirah.lang.ast.Boolean;
import mirah.lang.ast.Break;
import mirah.lang.ast.Call;
import mirah.lang.ast.Case;
import mirah.lang.ast.Cast;
import mirah.lang.ast.ClosureDefinition;
import mirah.lang.ast.ConstructorDefinition;
import mirah.lang.ast.FieldAccess;
import mirah.lang.ast.FieldAssign;
import mirah.lang.ast.FieldDeclaration;
import mirah.lang.ast.FunctionalCall;
import mirah.lang.ast.If;
import mirah.lang.ast.LocalDeclaration;
import mirah.lang.ast.MethodDefinition;
import mirah.lang.ast.ModifierList;
import mirah.lang.ast.Node;
import mirah.lang.ast.NodeList;
import mirah.lang.ast.NodeScanner;
import mirah.lang.ast.Return;
import mirah.lang.ast.Script;
import mirah.lang.ast.TypeNameList;
import mirah.lang.ast.VCall;
import mirah.objectweb.asm.tree.ClassNode;

/**
 * Visitor that builds path to element identified at given position
 *
 * @todo skipping irrelevant subtrees, see IsInside(...) method
 *
 * @author Martin Adamek
 */
public class PathFinderVisitor extends NodeScanner {

    private static final Logger LOG = Logger.getLogger(PathFinderVisitor.class.getName());

//    private final SourceUnit sourceUnit;

//    private final int line;
//    
//    private final int column;
    private final int offset;
    private int line_min = -1;
    private int line_max = -1;

    private final List<Node> path = new ArrayList<Node>();

    public PathFinderVisitor( int offset ) {
        this.offset = offset;
    }
//    public PathFinderVisitor( int line, int column ) {
//        this.line = line;
//        this.column = column;
//    }
//    public PathFinderVisitor(SourceUnit sourceUnit, int line, int column) {
//        this.sourceUnit = sourceUnit;
//        this.line = line;
//        this.column = column;
//    }
    public List<Node> getPath() {
        return new ArrayList<Node>(path);
    }
/*

    @Override
    protected SourceUnit getSourceUnit() {
        return sourceUnit;
    }

    // super visitor doesn't visit parameters
    @Override
    protected void visitConstructorOrMethod(MethodDefinition node, boolean isConstructor) {
        super.visitConstructorOrMethod(node, isConstructor);
        for (Parameter parameter : node.getParameters()) {
            isInside(parameter, line, column);
        }
    }

    @Override
    protected void visitStatement(Statement statement) {
    }

    @Override
    public void visitBlockStatement(BlockStatement node) {
        if (isInside(node, line, column, false)) {
            path.add(node);
        } else {
            for (Object object : node.getStatements()) {
                if (isInside((Node) object, line, column, false)) {
                    path.add(node);
                    break;
                }
            }
        }

        for (Object object : node.getStatements()) {
            Statement statement = (Statement) object;
            statement.visit(this);
        }
    }

//    @Override
//    public void visitForLoop(ForStatement node) {
//        if (isInside(node, line, column)) {
//            super.visitForLoop(node);
//        }
//    }

//    @Override
//    public void visitWhileLoop(WhileStatement node) {
//        if (isInside(node, line, column)) {
//            super.visitWhileLoop(node);
//        }
//    }

//    @Override
//    public void visitDoWhileLoop(DoWhileStatement node) {
//        if (isInside(node, line, column)) {
//            super.visitDoWhileLoop(node);
//        }
//    }

    @Override
    public boolean enterIf(If node, Object arg) {
        if (isInside(node, line, column)) {
//            super.visitIfElse(node);
        }
    		return super.enterIf(node,arg);
    }

//    @Override
//    public void visitExpressionStatement(ExpressionStatement node) {
//        if (isInside(node, line, column)) {
//            super.visitExpressionStatement(node);
//        }
//    }

    @Override
    public boolean enterReturn(Return node, Object arg) {
        if (isInside(node, line, column)) {
//            super.visitReturnStatement(node);
        }
        return super.enterReturn(node,arg);
    }

//    @Override
//    public void visitAssertStatement(AssertStatement node) {
//        if (isInside(node, line, column)) {
//            super.visitAssertStatement(node);
//        }
//    }

//    @Override
//    public void visitTryCatchFinally(TryCatchStatement node) {
//        if (isInside(node, line, column)) {
//            super.visitTryCatchFinally(node);
//        }
//    }

//    @Override
//    public void visitSwitch(SwitchStatement node) {
//        if (isInside(node, line, column)) {
//            super.visitSwitch(node);
//        }
//    }

    @Override
    public boolean enterCase(Case node, Object arg) {
        if (isInside(node, line, column)) {
//            super.visitCaseStatement(node);
        }
    		return super.enterCase(node,arg);
    }

    @Override
    public boolean enterBreak(Break node, Object arg) {
        if (isInside(node, line, column)) {
        }
	return super.enterBreak(node,arg);
    }


//    @Override
//    public void visitContinueStatement(ContinueStatement node) {
//        if (isInside(node, line, column)) {
//            super.visitContinueStatement(node);
//        }
//    }

//    @Override
//    public void visitThrowStatement(ThrowStatement node) {
//        if (isInside(node, line, column)) {
//            super.visitThrowStatement(node);
//        }
//    }

//    @Override
//    public void visitSynchronizedStatement(SynchronizedStatement node) {
//        if (isInside(node, line, column)) {
//            super.visitSynchronizedStatement(node);
//        }
//    }

//    @Override
//    public void visitCatchStatement(CatchStatement node) {
//        if (isInside(node, line, column)) {
//            super.visitCatchStatement(node);
//        }
//    }

    @Override
    public boolean enterFunctionalCall(FunctionalCall node, Object arg) {
    		return super.enterFunctionalCall(node,arg);
    }

    @Override
    public boolean enterVCall(VCall node, Object arg) {
    		return super.enterVCall(node,arg);
    }

    @Override
    public boolean enterCall(Call node, Object arg) {
    		return super.enterCall(node,arg);
    }
    
    
    @Override
    public void visitMethodCallExpression(MethodCallExpression node) {
        if (isInside(node, line, column)) {
            // FIXME http://jira.codehaus.org/browse/GROOVY-3263
            if (node.isImplicitThis()) {
                node.getMethod().visit(this);
                node.getArguments().visit(this);
            } else {
                super.visitMethodCallExpression(node);
            }
        }
    }

//    @Override
//    public void visitStaticMethodCallExpression(StaticMethodCallExpression node) {
//        if (isInside(node, line, column)) {
//            super.visitStaticMethodCallExpression(node);
//        }
//    }

//    @Override
//    public void visitConstructorCallExpression(ConstructorCallExpression node) {
//        if (isInside(node, line, column)) {
//            super.visitConstructorCallExpression(node);
//        }
//    }

//    @Override
//    public void visitTernaryExpression(TernaryExpression node) {
//        if (isInside(node, line, column)) {
//            super.visitTernaryExpression(node);
//        }
//    }

//    @Override
//    public void visitShortTernaryExpression(ElvisOperatorExpression node) {
//        if (isInside(node, line, column)) {
//            super.visitShortTernaryExpression(node);
//        }
//    }

//    @Override
//    public void visitBinaryExpression(BinaryExpression node) {
//        if (isInside(node, line, column)) {
//            super.visitBinaryExpression(node);
//        }
//    }

//    @Override
//    public void visitPrefixExpression(PrefixExpression node) {
//        if (isInside(node, line, column)) {
//            super.visitPrefixExpression(node);
//        }
//    }

//    @Override
//    public void visitPostfixExpression(PostfixExpression node) {
//        if (isInside(node, line, column)) {
//            super.visitPostfixExpression(node);
//        }
//    }

    @Override
    public boolean enterBoolean(Boolean node, Object arg) {
        if (isInside(node, line, column)) {
//            super.visitBooleanExpression(node);
        }
    		return super.enterBoolean(node,arg);
    }

//    @Override
//    public void visitClosureExpression(ClosureExpression node) {
//        if (isInside(node, line, column)) {
//            super.visitClosureExpression(node);
//            if (node.isParameterSpecified()) {
//                for (Parameter parameter : node.getParameters()) {
//                    isInside(parameter, line, column);
//                }
//            }
//        }
//    }

//    @Override
//    public void visitTupleExpression(TupleExpression node) {
//        if (isInside(node, line, column)) {
//            super.visitTupleExpression(node);
//        }
//    }

//    @Override
//    public void visitMapExpression(MapExpression node) {
//        if (isInside(node, line, column)) {
//            super.visitMapExpression(node);
//        }
//    }

//    @Override
//    public void visitMapEntryExpression(MapEntryExpression node) {
//        if (isInside(node, line, column)) {
//            super.visitMapEntryExpression(node);
//        }
//    }

    @Override
    public boolean enterNodeList(NodeList node, Object arg) {
        if (isInside(node, line, column)) {
//            super.visitListExpression(node);
        }
        return super.enterNodeList(node, arg);
    }

//    @Override
//    public void visitRangeExpression(RangeExpression node) {
//        if (isInside(node, line, column)) {
//            super.visitRangeExpression(node);
//        }
//    }

/* -----------------------------------------------------    
    @Override
    public void visitPropertyExpression(PropertyExpression node) {

        // XXX PropertyExpression has wrong offsets, e.g. 4-4 for 'this.field1 = 77'
        // and was never added to path,
        // therefore let's check if its children are wraping given position
        // and add it then

        Expression objectExpression = node.getObjectExpression();
        Expression property = node.getProperty();

        if (isInside(node, line, column, false)) {
            path.add(node);
        } else {
            boolean nodeAdded = false;
            if (isInside(objectExpression, line, column, false)) {
                path.add(node);
                nodeAdded = true;
            }
            if (isInside(property, line, column, false)) {
                if (!nodeAdded) {
                    path.add(node);
                }
            }
        }

        objectExpression.visit(this);
        property.visit(this);
    }
*
    @Override
    public boolean enterAttrAssign(AttrAssign node, Object arg) {
        if (isInside(node, line, column)) {
//            super.visitAttributeExpression(node);
        }
    		return super.enterAttrAssign(node,arg);
    }

    @Override
    public boolean enterFieldDeclaration(FieldDeclaration node, Object arg) {
        if (isInside(node, line, column)) {
//            super.visitFieldExpression(node);
        }
    		return super.enterFieldDeclaration(node,arg);
    }

    @Override
    public boolean enterFieldAssign(FieldAssign node, Object arg) {
        if (isInside(node, line, column)) {
//            super.visitFieldExpression(node);
        }
    		return super.enterFieldAssign(node,arg);
    }

    @Override
    public boolean enterFieldAccess(FieldAccess node, Object arg) {
        if (isInside(node, line, column)) {
//            super.visitFieldExpression(node);
        }
    		return super.enterFieldAccess(node,arg);
    }

//    @Override
//    public void visitMethodPointerExpression(MethodPointerExpression node) {
//        if (isInside(node, line, column)) {
//            super.visitMethodPointerExpression(node);
//        }
//    }

//    @Override
//    public void visitConstantExpression(ConstantExpression node) {
//        if (isInside(node, line, column)) {
//            super.visitConstantExpression(node);
//        }
//    }

//    @Override
//    public void visitClassExpression(ClassExpression node) {
//        if (isInside(node, line, column)) {
//            super.visitClassExpression(node);
//        }
//    }

//    @Override
//    public void visitVariableExpression(VariableExpression node) {
//        if (isInside(node, line, column)) {
//            super.visitVariableExpression(node);
//        }
//    }

    @Override
    public boolean enterLocalDeclaration(LocalDeclaration node, Object arg) {
        if (isInside(node, line, column)) {
//            super.visitDeclarationExpression(node);
        }
    		return super.enterLocalDeclaration(node,arg);
    }

//    @Override
//    public void visitGStringExpression(GStringExpression node) {
//        if (isInside(node, line, column)) {
//            super.visitGStringExpression(node);
//        }
//    }

    @Override
    public boolean enterArray(Array node, Object arg) {
        if (isInside(node, line, column)) {
//            super.visitArrayExpression(node);
        }
    	return super.enterArray(node,arg);
    }

    
//    public void visitSpreadExpression(SpreadExpression node) {
//        if (isInside(node, line, column)) {
//            super.visitSpreadExpression(node);
//        }
//    }

//    @Override
//    public void visitSpreadMapExpression(SpreadMapExpression node) {
//        if (isInside(node, line, column)) {
//            super.visitSpreadMapExpression(node);
//        }
//    }

    @Override
    public void visitNotExpression(NotExpression node) {
        if (isInside(node, line, column)) {
            super.visitNotExpression(node);
        }
    }

//    @Override
//    public void visitUnaryMinusExpression(UnaryMinusExpression node) {
//        if (isInside(node, line, column)) {
//            super.visitUnaryMinusExpression(node);
//        }
//    }

//    @Override
//    public void visitUnaryPlusExpression(UnaryPlusExpression node) {
//        if (isInside(node, line, column)) {
//            super.visitUnaryPlusExpression(node);
//        }
//    }

//    @Override
//    public void visitBitwiseNegationExpression(BitwiseNegationExpression node) {
//        if (isInside(node, line, column)) {
//            super.visitBitwiseNegationExpression(node);
//        }
//    }

    @Override
    public boolean enterCast(Cast node, Object arg) {
        if (isInside(node, line, column)) {
        }
    		return super.enterCast(node,arg);
    }
    @Override
    public void visitArgumentlistExpression(ArgumentListExpression node) {
        if (isInside(node, line, column)) {
            super.visitArgumentlistExpression(node);
        }
    }

    @Override
    public boolean enterClosureDefinition(ClosureDefinition node, Object arg) {
        if (isInside(node, line, column)) {
//            super.visitClosureListExpression(node);
        }
    		return super.enterClosureDefinition(node,arg);
    }

    @Override
    public void visitClass(ClassNode node) {
        if (isInside(node, line, column)) {
            super.visitClass(node);
        }
    }

    @Override
    public boolean enterConstructorDefinition(ConstructorDefinition node, Object arg) {
        // we don't want synthetic constructors duplicating field initial expressions
        if (!node.isSynthetic() && isInside(node, line, column)) {
//            super.visitConstructor(node);
        }
        return super.enterLoop(node,arg);
    }
    

    @Override
    public boolean enterMethodDefinition(MethodDefinition node, Object arg) {
        if (isInside(node, line, column)) {
//            super.visitMethod(node);
        }
    		return super.enterMethodDefinition(node,arg);
    }

    @Override
    public void visitField(FieldNode node) {
        // we don't want synthetic fields duplicating property initial expressions
        if (!node.isSynthetic() && isInside(node, line, column)) {
            super.visitField(node);
        }
    }

    @Override
    public void visitProperty(PropertyNode node) {
        // we don't want synthetic static initializers introducing this variables
        if (!node.isSynthetic() && isInside(node, line, column)) {
            super.visitProperty(node);
        }
    }
*/
    
    @Override
    public boolean enterClosureDefinition(ClosureDefinition node, Object arg) {
        if (node != null && node.body() != null)
        {
            for (Object n : node.body()) {
                if (n instanceof Node) {
                    enterDefault((Node) n, arg);
                }
            }
        }
        return super.enterClosureDefinition(node, arg);
    }

    @Override
    public boolean enterMethodDefinition(MethodDefinition node, Object arg) {
        if ( node != null && node.body() != null )
        {
            for (Object n : node.body()) {
                if (n instanceof Node) {
                    enterDefault((Node) n, arg);
                }
            }
        }
        return super.enterMethodDefinition(node,arg);       
    }
    
    @Override
    public boolean enterDefault(Node node, Object arg) {
//        if ( node.position() != null )
//            System.out.println(""+node+"["+node.position().startChar()+","+node.position().endChar()+"] "+node.parent());
//        else
//            System.out.println(""+node+"[] "+node.parent());
        if ( ! isInside(node, offset) ) return false;
//      isInside(node, line, column);

        return super.enterDefault(node, arg);
    }
    
    private boolean isInside(Node node, int offset) {
        return isInside(node, offset, true);
    }
//    private boolean isInside(Node node, int line, int column) {
//        return isInside(node, line, column, true);
//    }

    private boolean isInside(Node node, int offset, boolean addToPath) {
        if (node == null || !isInSource(node) ) { //|| node.position() == null) {
            return true;
        }
        if ( node.position() == null) {
            return true;
        }

        fixNode(node);

        int startChar = node.position().startChar();
        int endChar = node.position().endChar();

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.log(Level.FINEST, "isInside: " + node + " - " + startChar + ", " + endChar);
        }

        if (startChar == -1 || endChar == -1) {
            // this node doesn't provide its coordinates, some wrappers do that
            // let's say yes and visit its children
            return addToPath ? true : false;
        }

        boolean result = (startChar <= offset && endChar > offset);

        if (result && addToPath) {
            
            // корректирую размер блока отслеживания
            if ( line_min < node.position().startLine() ) line_min = node.position().startLine();
            if ( line_max == -1 || line_max > node.position().endLine() ) line_max = node.position().endLine();
            
            // Завершить обход дерева, если обрабатываются узлы, относящиеся к большим номерам строк
            if ( line_max < node.position().startLine() ) return false;
            checkNode(node);
            LOG.log(Level.FINEST, "Path: {0}", path);
        }

        // if addToPath is false, return result, we want to know real state of affairs
        // and not to continue traversing
        return addToPath ? true : result;
    }
    /*
    private boolean isInside(Node node, int line, int column, boolean addToPath) {
        if (node == null || !isInSource(node) ) { //|| node.position() == null) {
            return false;
        }
        if ( node.position() == null) {
            return false;
        }

        fixNode(node);

        int beginLine = node.position().startLine();
        int beginColumn = node.position().startColumn();
        int endLine = node.position().endLine();
        int endColumn = node.position().endColumn();

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.log(Level.FINEST, "isInside: " + node + " - "
                    + beginLine + ", " + beginColumn + ", " + endLine + ", " + endColumn);
        }

        if (beginLine == -1 || beginColumn == -1 || endLine == -1 || endColumn == -1) {
            // this node doesn't provide its coordinates, some wrappers do that
            // let's say yes and visit its children
            return addToPath ? true : false;
        }

        boolean result = false;

        if (beginLine == endLine) {
            if (line == beginLine && column >= beginColumn && column < endColumn) {
                result = true;
            }
        } else if (line == beginLine) {
            if (column >= beginColumn) {
                result = true;
            }
        } else if (line == endLine) {
            if (column < endColumn) {
                result = true;
            }
        } else if (beginLine < line && line < endLine) {
            result = true;
        } else {
            result = false;
        }

        if (result && addToPath) {
            checkNode(node);
            LOG.log(Level.FINEST, "Path: {0}", path);
        }

        // if addToPath is false, return result, we want to know real state of affairs
        // and not to continue traversing
        return addToPath ? true : result;
    }
    */
    private void checkNode(Node node)
    {
        if ( node instanceof Script ) return;
        /*
         if ( node instanceof NodeList ) return;
        //todo разобраться что это такое
        if ( node instanceof ModifierList ) return;
        if ( node instanceof AnnotationList ) return;
        if ( node instanceof TypeNameList ) return;
        */
        path.add(node);
    }

    private void fixNode(Node node) {
        /*
        // FIXME http://jira.codehaus.org/browse/GROOVY-3263
        if (node instanceof MethodCallExpression && !((MethodCallExpression) node).isImplicitThis()) {
            MethodCallExpression call = (MethodCallExpression) node;
            if (call.getObjectExpression() == VariableExpression.THIS_EXPRESSION
                    || call.getObjectExpression() == VariableExpression.SUPER_EXPRESSION) {
                // this is not bulletproof but fix most of the problems
                VariableExpression var = new VariableExpression(
                        call.getObjectExpression() == VariableExpression.THIS_EXPRESSION ? "this" : "super", // NOI18N
                        call.getObjectExpression().getType()); // NOI18N
                var.setLineNumber(call.getLineNumber());
                var.setColumnNumber(call.getColumnNumber());
                var.setLastLineNumber(call.getMethod().getLineNumber());
                var.setLastColumnNumber(call.getMethod().getColumnNumber());
                call.setObjectExpression(var);
            }
        // FIXME http://jira.codehaus.org/browse/GROOVY-3472
        } else if (node instanceof MethodDefinition || node instanceof ClosureExpression) {
            Statement code = null;
            if (node instanceof MethodDefinition) {
                code = ((MethodDefinition) node).getCode();
            } else {
                code = ((ClosureExpression) node).getCode();
            }

            if (code != null && code instanceof BlockStatement
                    && ((code.getLineNumber() < 0 && code.getColumnNumber() < 0)
                    || (code.getLastLineNumber() < 0 && code.getLastColumnNumber() < 0))) {
                BlockStatement block = (BlockStatement) code;
                List statements = block.getStatements();
                if (statements != null && !statements.isEmpty()) {
                    if (code.getLineNumber() < 0 && code.getColumnNumber() < 0) {
                        Statement first = (Statement) statements.get(0);
                        code.setLineNumber(first.getLineNumber());
                        code.setColumnNumber(first.getColumnNumber());
                    }
                    if (code.getLastLineNumber() < 0 && code.getLastColumnNumber() < 0) {
                        // maybe not accurate
                        code.setLastLineNumber(node.getLastLineNumber());
                        int lastColumn = node.getLastColumnNumber();
                        if (lastColumn > 0) {
                            lastColumn--;
                        }
                        code.setLastColumnNumber(lastColumn);
                    }
                }
            }
        }
                */
    }

    private boolean isInSource(Node node) {
        /*
        if (node instanceof AnnotatedNode) {
            if (((AnnotatedNode) node).hasNoRealSourcePosition()) {
                return false;
            }
        }

        // FIXME probably http://jira.codehaus.org/browse/GROOVY-3263
        if (node instanceof StaticMethodCallExpression && node.getLineNumber() == -1
                && node.getLastLineNumber() == -1 && node.getColumnNumber() == -1
                && node.getLastColumnNumber() == -1) {

            StaticMethodCallExpression methodCall = (StaticMethodCallExpression) node;
            if ("initMetaClass".equals(methodCall.getMethod())) { // NOI18N
                Expression args = methodCall.getArguments();
                if (args instanceof VariableExpression) {
                    VariableExpression var = (VariableExpression) args;
                    if ("this".equals(var.getName())) { // NOI18N
                        return false;
                    }
                }
            }
        }
        */
        return true;
    }

}
