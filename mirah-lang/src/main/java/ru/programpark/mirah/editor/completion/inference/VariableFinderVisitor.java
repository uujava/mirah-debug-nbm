package ru.programpark.mirah.editor.completion.inference;

import ca.weblite.netbeans.mirah.lexer.MirahTokenId;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import mirah.lang.ast.Node;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.editor.BaseDocument;
import ru.programpark.mirah.editor.ast.AstPath;
import ru.programpark.mirah.editor.ast.Variable;
import ru.programpark.mirah.tests.LexUtilities;
import sun.tools.tree.CatchStatement;

/**
 * Visitor that find variables usable at given offset. This include
 * method/constructor parameters, closure parameters, variables defined in
 * for loop and of course local variables.
 * <p>
 * For local variables it handles:
 * <ul>
 *   <li>test = "something"
 *   <li>def test = "something"
 *   <li>def test
 *   <li>String test = "something"
 *   <li>String test
 * </ul>
 *
 * @author Petr Hejl
 */
// FIXME scriptMode ?
public class VariableFinderVisitor /*extends ClassCodeVisitorSupport*/ {

//    private final  SourceUnit sourceUnit;

    private final AstPath path;

    private final BaseDocument doc;

    private final int cursorOffset;

    private Set<Node> blocks = new HashSet<Node>();

    private Map<String, Variable> variables = new HashMap<String, Variable>();

    public VariableFinderVisitor(/*SourceUnit sourceUnit,*/ AstPath path, BaseDocument doc, int cursorOffset) {
//        this.sourceUnit = sourceUnit;
        this.path = path;
        this.doc = doc;
        this.cursorOffset = cursorOffset;
    }

//    @Override
//    protected SourceUnit getSourceUnit() {
//        return sourceUnit;
//    }

    public Collection<Variable> getVariables() {
        return variables.values();
    }

    public void collect() {
        TokenSequence<MirahTokenId> ts = LexUtilities.getPositionedSequence(doc, cursorOffset);
        if (ts == null) {
            return;
        }
        Token<MirahTokenId> token = ts.token();
        if (token == null) {
            return;
        }

        Node last = null;

        blocks.clear();
        variables.clear();

        // We are going through the path up marking all the declaration
        // blocks and the top (last) one.
        for (Iterator<Node> it = path.iterator(); it.hasNext();) {
            Node scope = it.next();
            /*
            if ((scope instanceof ClosureExpression) || (scope instanceof MethodDefinition)
                    || (scope instanceof ConstructorNode) || (scope instanceof ForStatement)
                    || (scope instanceof BlockStatement)  
//                    || (scope instanceof ClosureListExpression)
                    ) {
//                    || (scope instanceof CatchStatement)) {

                last = scope;
                blocks.add(scope);

                // In for loop we have to allow visitor to visit ClosureListExpression
                if ((scope instanceof ForStatement)
                        ) {
//                        && (((ForStatement) scope).getCollectionExpression() instanceof ClosureListExpression)) {
                    blocks.add(((ForStatement) scope).getCollectionExpression());
                }
            }
            */
        }

        // Lets visit the code from top. We visit only allowed blocks
        // to avoid visiting subtrees declared before offset, but not usable.
        // ie
        // def clos = {
        //     def x = {
        //         String str
        //     }
        //     ^ // we are here and we dont want to get str as possibility
        // }
        /*
        if (last instanceof ClosureExpression) {
            visitClosureExpression((ClosureExpression) last);
        } else if (last instanceof MethodDefinition) {
            visitMethod((MethodDefinition) last);
        } else if (last instanceof ConstructorNode) {
            visitConstructor((ConstructorNode) last);
        } else if (last instanceof ForStatement) {
            visitForLoop((ForStatement) last);
        } else if (last instanceof BlockStatement) {
            visitBlockStatement((BlockStatement) last);
//        } else if (last instanceof ClosureListExpression) {
//            visitClosureListExpression((ClosureListExpression) last);
        } 
//        else if (last instanceof CatchStatement) {
//            visitCatchStatement((CatchStatement) last);
//        }
        */

    }

//    @Override
    /*
    public void visitDeclarationExpression(DeclarationExpression expression) {
        // if we are in the same block we check position, if it occurs after
        // current position we ignore it
        if (blocks.isEmpty()
                && expression.getLineNumber() >= 0 && expression.getColumnNumber() >= 0
                && path.getLineNumber() >= 0 && path.getColumnNumber() >= 0
                && (expression.getLineNumber() > path.getLineNumber()
                || (expression.getLineNumber() == path.getLineNumber() && expression.getColumnNumber() >= path.getColumnNumber()))) {
            return;
        }

        if (!expression.isMultipleAssignmentDeclaration()) {
            VariableExpression variableExpression = expression.getVariableExpression();
            if (variableExpression.getAccessedVariable() != null) {
                String name = variableExpression.getAccessedVariable().getName();
                variables.put(name, variableExpression.getAccessedVariable());
            }
        }
        // perhaps we could visit just declaration or do nothing
//        super.visitDeclarationExpression(expression);
    }
    */
//    @Override
/*    
    public void visitBinaryExpression(BinaryExpression expression) {
        // if we are in the same block we check position, if it occurs after
        // current position we ignore it
        if (blocks.isEmpty()
                && expression.getLineNumber() >= 0 && expression.getColumnNumber() >= 0
                && path.getLineNumber() >= 0 && path.getColumnNumber() >= 0
                && (expression.getLineNumber() > path.getLineNumber()
                || (expression.getLineNumber() == path.getLineNumber() && expression.getColumnNumber() >= path.getColumnNumber()))) {
            return;
        }

//        Expression leftExpression = expression.getLeftExpression();
//        if (leftExpression instanceof VariableExpression) {
            /*
            if (expression.getOperation().isA(Types.EQUAL)) {
                VariableExpression variableExpression = (VariableExpression) leftExpression;
                if (variableExpression.getAccessedVariable() != null) {
                    String name = variableExpression.getAccessedVariable().getName();
                    if (!variables.containsKey(name)) {
                        variables.put(name, variableExpression.getAccessedVariable());
                    }
                }
            }
                    *
//        }
//        super.visitBinaryExpression(expression);
    }
*/
    /*
//    @Override
    public void visitMethod(MethodDefinition node) {
        if (!blocks.remove(node)) {
            return;
        }

        for (Parameter param : node.getParameters()) {
            variables.put(param.getName(), param);
        }
//        super.visitMethod(node);
    }
*/
    /*
//    @Override
    public void visitCatchStatement(CatchStatement statement) {
        if (!blocks.remove(statement)) {
            return;
        }
        if (statement.getVariable() != null) {
            String name = statement.getVariable().getName();
            variables.put(name, statement.getVariable());
        }
//        super.visitCatchStatement(statement);
    }
/*
//    @Override
    public void visitClosureExpression(ClosureExpression expression) {
        if (!blocks.remove(expression)) {
            return;
        }

        if (expression.isParameterSpecified()) {
            for (Parameter param : expression.getParameters()) {
                variables.put(param.getName(), param);
            }
        } else {
            variables.put("it", new VariableExpression("it")); // NOI18N
        }
//        super.visitClosureExpression(expression);
    }
*/
    /*
//    @Override
    public void visitConstructor(ConstructorNode node) {
        if (!blocks.remove(node)) {
            return;
        }

        for (Parameter param : node.getParameters()) {
            variables.put(param.getName(), param);
        }

//        super.visitConstructor(node);
    }
*/
    
    /*
//    @Override
    public void visitForLoop(ForStatement forLoop) {
        if (!blocks.remove(forLoop)) {
            return;
        }

        Parameter param = forLoop.getVariable();
        if (param != ForStatement.FOR_LOOP_DUMMY) {
            variables.put(param.getName(), param);
        }
//        super.visitForLoop(forLoop);
    }
*/
    /*
//    @Override
    public void visitBlockStatement(BlockStatement block) {
        if (!blocks.remove(block)) {
            return;
        }

//        super.visitBlockStatement(block);
    }
*/
    
//    @Override
//    public void visitClosureListExpression(ClosureListExpression cle) {
//        // FIXME whole tree allowed ?
//        if (!blocks.remove(cle)) {
//            return;
//        }
//
//        super.visitClosureListExpression(cle);
//    }
}
