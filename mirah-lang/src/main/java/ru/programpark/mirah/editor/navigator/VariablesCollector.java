/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.programpark.mirah.editor.navigator;

import ru.programpark.mirah.lexer.MirahTokenId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import ru.programpark.mirah.lexer.MirahParserResult;
import mirah.lang.ast.ClassDefinition;
import mirah.lang.ast.ClosureDefinition;
import mirah.lang.ast.ConstructorDefinition;
import mirah.lang.ast.FieldAssign;
import mirah.lang.ast.FieldDeclaration;
import mirah.lang.ast.LocalAssignment;
import mirah.lang.ast.MethodDefinition;
import mirah.lang.ast.Node;
import mirah.lang.ast.NodeScanner;
import mirah.lang.ast.RequiredArgument;
import mirah.lang.ast.StaticMethodDefinition;
import mirah.lang.ast.Unquote;
import org.mirah.typer.ResolvedType;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.editor.BaseDocument;
import ru.programpark.mirah.editor.utils.LexUtilities;

/**
 *
 * @author savushkin savushkin@programpark.ru
 */
public class VariablesCollector extends NodeScanner {
    
    private final BaseDocument doc;

//    private final AstPath path;

    private final int cursorOffset;

    private Set<Node> blocks = new HashSet<Node>();

    private ArrayList<String> variables = new ArrayList<String>();
    private Node leaf;
    private MirahParserResult parsed;
    private HashMap uniqueNames = new HashMap();
    
//    public VariablesCollector( AstPath path, BaseDocument doc, int cursorOffset) {
    public VariablesCollector( MirahParserResult parsed, Node leaf, BaseDocument doc, int cursorOffset) {
//        this.path = path;
        this.parsed = parsed;
        this.leaf = leaf;
        this.doc = doc;
        this.cursorOffset = cursorOffset;
    }

    public ArrayList<String> getVariables() {
        return variables;
    }
    // сканирует AST путь, выделяя локальные переменные методов, переменные класса
    public void collect() {
        TokenSequence<MirahTokenId> ts = LexUtilities.getMirahTokenSequence(doc, cursorOffset);
        if (ts == null) {
//            return;
        }
        Token<MirahTokenId> token = ts.token();
        if (token == null) {
//            return;
        }

        blocks.clear();
        variables.clear();
        
        Node last = null;
        while (leaf != null) {
            if (leaf instanceof ClosureDefinition
            || leaf instanceof ClassDefinition
            || leaf instanceof MethodDefinition
            || leaf instanceof ConstructorDefinition) {
                last = leaf;
            }
            leaf = leaf.parent();
        }
        if ( last != null ) 
            last.accept(this, this);


        // We are going through the path up marking all the declaration
        // blocks and the top (last) one.
        /*
        for (Iterator<Node> it = path.iterator(); it.hasNext();) {
            Node scope = it.next();
            scope.accept(this, this);
            if ((scope instanceof ClosureDefinition) 
                || (scope instanceof MethodDefinition)
                || (scope instanceof ConstructorDefinition)) {
//                || (scope instanceof ForStatement)
//                || (scope instanceof BlockStatement) 
//                || (scope instanceof ClosureListExpression)
//                || (scope instanceof CatchStatement)) {

                last = scope;
                blocks.add(scope);

                // In for loop we have to allow visitor to visit ClosureListExpression
//                if ((scope instanceof ForStatement)
//                        && (((ForStatement) scope).getCollectionExpression() instanceof ClosureListExpression)) {
//                    blocks.add(((ForStatement) scope).getCollectionExpression());
//                }
            }
        }
        */
        // Lets visit the code from top. We visit only allowed blocks
        // to avoid visiting subtrees declared before offset, but not usable.
        // ie
        // def clos = {
        //     def x = {
        //         String str
        //     }
        //     ^ // we are here and we dont want to get str as possibility
        // }
//        if (last instanceof ClosureDefinition) {
//            enterClosureDefinition((ClosureDefinition) last,this);
//        } else if (last instanceof MethodDefinition) {
//            this.enterMethodDefinition((MethodDefinition) last,this);
//        } else if (last instanceof ConstructorDefinition) {
//            enterConstructorDefinition((ConstructorDefinition) last,this);
//        } 
//        else if (last instanceof ForStatement) {
//            visitForLoop((ForStatement) last);
//        } else if (last instanceof BlockStatement) {
//            visitBlockStatement((BlockStatement) last);
//        } else if (last instanceof ClosureListExpression) {
//            visitClosureListExpression((ClosureListExpression) last);
//        } else if (last instanceof CatchStatement) {
//            visitCatchStatement((CatchStatement) last);
//        }
    }
    
//    @Override
//    public boolean enterDefault(Node node, Object arg) {
//        return super.enterDefault(node, arg);
//  }
    
    
    //todo анализировать суперкласс замыкания
    @Override
    public boolean enterClosureDefinition(ClosureDefinition node, Object arg) {
//        if (className == null) {
//            found.add(node);
//        } else if (node.superclass() != null) {
//            if ("*".equals(className)) {
//                found.add(node);
//            } else if (className.equals(node.superclass().typeref().name())) {
//                found.add(node);
//            }
//        } else if (node.interfaces() != null && node.interfaces_size() > 0) {
//            if ("*".equals(className)) {
//                found.add(node);
//            } else {
//                for (int i = 0; i < node.interfaces_size(); i++) {
//                    if (className.equals(node.interfaces(i).typeref().name())) {
//                        found.add(node);
//                        break;
//                    }
//                }
//            }
//        }
//        for (int j = 0; j < node.arguments().required_size(); j++) {
//            RequiredArgument req = node.arguments().required(j);
//            variables.add(req.name().identifier());
//        }
        return super.enterClosureDefinition(node, arg);
    }

    //todo отбирать только методы, определяющие поля класса 
    // наверное, нужно перенести в MethodCompletion?
    @Override
    public boolean enterMethodDefinition(MethodDefinition node, Object arg) {
//        if (methodName == null) {
//            found.add(node);
//        } else if (node.name() != null) {
//            if ("*".equals(methodName)) {
//                found.add(node);
//            } else if (methodName.equals(node.name().identifier())) {
//                found.add(node);
//            }
//        }
        Object v = node.name();
        //todo это метод доступа к переменной?
        if ( v instanceof Unquote ) {
            if ( ! uniqueNames.containsKey(node.name().identifier()) )
            {
                variables.add("@"+node.name().identifier());
                uniqueNames.put(node.name().identifier(),null);
            }
        }
        
        ResolvedType type = parsed.getResolvedType(node);
        
        String nn = node.name().identifier();
        for (int j = 0; j < node.arguments().required_size(); j++) {
            RequiredArgument req = node.arguments().required(j);
            variables.add(req.name().identifier());
        }

        return super.enterMethodDefinition(node, arg);
    }

    //todo удалять дубли
    @Override
    public boolean enterFieldAssign(FieldAssign node, Object arg) {
        if (!uniqueNames.containsKey(node.name().identifier()))
        {
            variables.add(node.name().identifier());
            uniqueNames.put(node.name().identifier(), null);
        }
        return super.enterFieldAssign(node,arg);     
    }

    //todo - добавлять поля только из текущего класса
    @Override
    public boolean enterFieldDeclaration(FieldDeclaration node, Object arg) {
//        if (fieldName == null) {
//            found.add(node);
//        } else if (node.name() != null) {
//            if ("*".equals(fieldName)) {
//                found.add(node);
//            } else if (fieldName.equals(node.name().identifier())
//                    || fieldName.equals("@" + node.name().identifier())) {
//                found.add(node);
//            }
//        }
        if (!uniqueNames.containsKey(node.name().identifier())) {
            variables.add(node.name().identifier());
            uniqueNames.put(node.name().identifier(), null);
        }
        return super.enterFieldDeclaration(node, arg);
    }

    @Override
    public boolean enterClassDefinition(ClassDefinition node, Object arg) {

//        if (node.position() != null
//                && node.position().startChar() <= offset
//                && node.position().endChar() > offset) {
//            found.add(node);
//        }
        return super.enterClassDefinition(node, arg);
    }
    //todo проверять область в которой курсор - локальные переменные только из текущего метода
    @Override
    public boolean enterLocalAssignment(LocalAssignment node, Object arg) {
//        if (varName == null) {
//            found.add(node);
//        } else if (node.name() != null) {
//            if ("*".equals(varName)) {
//                found.add(node);
//            } else if (varName.equals(node.name().identifier())) {
//                found.add(node);
//            }
//        }
        if (!uniqueNames.containsKey(node.name().identifier())) {
            variables.add(node.name().identifier());
            uniqueNames.put(node.name().identifier(), null);
        }
        return super.enterLocalAssignment(node, arg);
    }      

    @Override
    public boolean enterStaticMethodDefinition(StaticMethodDefinition node, Object arg) {
        return super.enterStaticMethodDefinition(node, arg);
    }

    
}
/*
    @Override
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
        super.visitDeclarationExpression(expression);
    }

    @Override
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

        Expression leftExpression = expression.getLeftExpression();
        if (leftExpression instanceof VariableExpression) {
            if (expression.getOperation().isA(Types.EQUAL)) {
                VariableExpression variableExpression = (VariableExpression) leftExpression;
                if (variableExpression.getAccessedVariable() != null) {
                    String name = variableExpression.getAccessedVariable().getName();
                    if (!variables.containsKey(name)) {
                        variables.put(name, variableExpression.getAccessedVariable());
                    }
                }
            }
        }
        super.visitBinaryExpression(expression);
    }

    @Override
    public void visitMethod(MethodNode node) {
        if (!blocks.remove(node)) {
            return;
        }

        for (Parameter param : node.getParameters()) {
            variables.put(param.getName(), param);
        }
        super.visitMethod(node);
    }

    @Override
    public void visitCatchStatement(CatchStatement statement) {
        if (!blocks.remove(statement)) {
            return;
        }

        if (statement.getVariable() != null) {
            String name = statement.getVariable().getName();
            variables.put(name, statement.getVariable());
        }
        super.visitCatchStatement(statement);
    }

    @Override
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
        super.visitClosureExpression(expression);
    }

    @Override
    public void visitConstructor(ConstructorNode node) {
        if (!blocks.remove(node)) {
            return;
        }

        for (Parameter param : node.getParameters()) {
            variables.put(param.getName(), param);
        }

        super.visitConstructor(node);
    }

    @Override
    public void visitForLoop(ForStatement forLoop) {
        if (!blocks.remove(forLoop)) {
            return;
        }

        Parameter param = forLoop.getVariable();
        if (param != ForStatement.FOR_LOOP_DUMMY) {
            variables.put(param.getName(), param);
        }
        super.visitForLoop(forLoop);
    }

    @Override
    public void visitBlockStatement(BlockStatement block) {
        if (!blocks.remove(block)) {
            return;
        }

        super.visitBlockStatement(block);
    }

    @Override
    public void visitClosureListExpression(ClosureListExpression cle) {
        // FIXME whole tree allowed ?
        if (!blocks.remove(cle)) {
            return;
        }

        super.visitClosureListExpression(cle);
    }
}
*/
