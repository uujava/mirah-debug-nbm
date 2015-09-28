package ru.programpark.mirah.editor.ast;

import ca.weblite.netbeans.mirah.lexer.MirahParser;
import ca.weblite.netbeans.mirah.lexer.MirahTokenId;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.BadLocationException;
import mirah.impl.Tokens;
import mirah.lang.ast.ClassDefinition;
import mirah.lang.ast.MethodDefinition;
import mirah.lang.ast.Node;
import mirah.lang.ast.NodeScanner;
import mirah.lang.ast.Script;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.api.lexer.TokenUtilities;
import org.netbeans.editor.BaseDocument;
import org.netbeans.editor.FinderFactory;
import org.netbeans.editor.Utilities;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.csl.spi.ParserResult;
import org.netbeans.modules.parsing.api.ParserManager;
import org.netbeans.modules.parsing.api.ResultIterator;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.api.UserTask;
import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.modules.parsing.spi.Parser;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import ru.programpark.mirah.editor.utils.LexUtilities;
import ru.programpark.mirah.index.elements.IndexedElement;

/**
 *
 * @author Martin Adamek
 */
public class ASTUtils {

    private static final Logger LOGGER = Logger.getLogger(ASTUtils.class.getName());

    public static int getAstOffset(Parser.Result info, int lexOffset) {
        MirahParser.NBMirahParserResult result = getParseResult(info);
        if (result != null) {
            return result.getSnapshot().getEmbeddedOffset(lexOffset);
        }
        return lexOffset;
    }

    public static MirahParser.NBMirahParserResult getParseResult(Parser.Result info) {
        assert info instanceof MirahParser.NBMirahParserResult : "Expecting MirahParseResult, but have " + info; //NOI18N
        return (MirahParser.NBMirahParserResult) info;
    }

    public static Node getRoot( ParserResult r) {
        
        assert r instanceof MirahParser.NBMirahParserResult;

        MirahParser.NBMirahParserResult result = (MirahParser.NBMirahParserResult)r;

        if (result.getParsedNodes() == null) {
            return null;
        }
        return result.getRoot();
//        return new Script((Script)result.getRoot());
//        return result.getRootElement().getModuleNode();
    }

    public static AstPath getPath(ParserResult parseResult, BaseDocument doc, int astOffset) {
        
        Node root = ASTUtils.getRoot(parseResult);

        // in some cases we can not repair the code, therefore root == null
        // therefore we can not complete. See # 131317
        if (root == null) {
            return null;
        }
        return new AstPath(root, astOffset, doc);
    }

    public static OffsetRange getRangeFull(Node node, BaseDocument doc) {
            if (node.position().startLine() < 0 || node.position().startColumn() < 0 || node.position().endLine() < 0 || node.position().endColumn() < 0) {
                return OffsetRange.NONE;
            }
            int start = getOffset(doc, node.position().startLine(), node.position().startColumn());
            if (start < 0) {
                start = 0;
            }
            int end = getOffset(doc, node.position().endLine(), node.position().endColumn());
            if (end < 0) {
                end = 0;
            }
            if (start > end) {
                return OffsetRange.NONE;
            }
            return new OffsetRange(start, end);
    }

    @NonNull
    public static OffsetRange getRange(Node node, BaseDocument doc) {

        // Warning! The implicit class and some other nodes has line/column numbers below 1
        // if line is wrong, let's invalidate also column and vice versa
        int lineNumber = node.position().startLine();
        int columnNumber = node.position().startColumn();
        if (lineNumber < 1 || columnNumber < 1) {
            return OffsetRange.NONE;
        }
        if (doc == null) {
            LOGGER.log(Level.INFO, "Null document in getRange()");
            return OffsetRange.NONE;
        }
        /*
        if (node instanceof FieldDeclaration) {
            int start = getOffset(doc, lineNumber, columnNumber);
            FieldDeclaration fieldNode = (FieldDeclaration) node;
            return getNextIdentifierByName(doc, fieldNode.getName(), start);
        } else */
        if (node instanceof ClassDefinition) {
            final ClassDefinition classNode = (ClassDefinition) node;
            int start = getOffset(doc, lineNumber, columnNumber);

            // classnode for script does not have real declaration and thus location
//            if (classNode.isScript()) {
//                return getNextIdentifierByName(doc, classNode.getNameWithoutPackage(), start);
//            }

            // ok, here we have to move the Range to the first character
            // after the "class" keyword, plus an indefinite nuber of spaces
            // FIXME: have to check what happens with other whitespaces between
            // the keyword and the identifier (like newline)

            // happens in some cases when groovy source uses some non-imported java class
            if (doc != null) {

                // if we are dealing with an empty groovy-file, we have take into consideration,
                // that even though we're running on an ClassDefinition, there is no "class " String
                // in the sourcefile. So take doc.getLength() as maximum.

                int docLength = doc.getLength();
                int limit = getLimit(node, doc, docLength);

                try {
                    // we have to really search for class keyword other keyword
                    // (such as abstract) can precede class
                    start = doc.find(new FinderFactory.StringFwdFinder("class", true), start, limit) + "class".length(); // NOI18N
                } catch (BadLocationException ex) {
                    LOGGER.log(Level.WARNING, null, ex);
                }

                if (start > docLength) {
                    start = docLength;
                }

                try {
                    start = Utilities.getFirstNonWhiteFwd(doc, start);
                } catch (BadLocationException ex) {
                    Exceptions.printStackTrace(ex);
                }

                // This seems to happen every now and then ...
                if (start < 0) {
                    start = 0;
                }

                int end = start + 0; //classNode.getNameWithoutPackage().length();

                if (end > docLength) {
                    end = docLength;
                }

                if (start == end) {
                    return OffsetRange.NONE;
                }
                return new OffsetRange(start, end);
            }
            /*
        } else if (node instanceof ConstructorNode) {
            int start = getOffset(doc, lineNumber, columnNumber);
            ConstructorNode constructorNode = (ConstructorNode) node;
            return getNextIdentifierByName(doc, constructorNode.getDeclaringClass().getNameWithoutPackage(), start);
        } else if (node instanceof MethodDefinition) {
            int start = getOffset(doc, lineNumber, columnNumber);
            MethodDefinition methodNode = (MethodDefinition) node;
            return getNextIdentifierByName(doc, methodNode.getName(), start);
        } else if (node instanceof VariableExpression) {
            int start = getOffset(doc, lineNumber, columnNumber);
            VariableExpression variableExpression = (VariableExpression) node;
            return getNextIdentifierByName(doc, variableExpression.getName(), start);
        } else if (node instanceof Parameter) {

            int docLength = doc.getLength();
            int start = getOffset(doc, node.getLineNumber(), node.getColumnNumber());
            int limit = 0; //getLimit(node, doc, docLength);

            Parameter parameter = (Parameter) node;
            String name = parameter.getName();

            try {
                // we have to really search for the name
                start = doc.find(new FinderFactory.StringFwdFinder(name, true), start, limit);
            } catch (BadLocationException ex) {
                Exceptions.printStackTrace(ex);
            }

            int end = start + name.length();
            if (end > docLength) {
                return OffsetRange.NONE;
            }
            return getNextIdentifierByName(doc, name, start);
        } */
        else if (node instanceof MethodDefinition ) {
            MethodDefinition methodCall = (MethodDefinition) node;
//            Expression method = methodCall.getMethod();
//            lineNumber = method.getLineNumber();
//            columnNumber = method.getColumnNumber();
            if (lineNumber < 1 || columnNumber < 1) {
                lineNumber = 1;
                columnNumber = 1;
            }
//            int start = getOffset(doc, lineNumber, columnNumber);
            return OffsetRange.NONE; //(start, start + methodCall.getMethodAsString().length());
        } 
/*        
        else if (node instanceof ConstructorCallExpression) {
            ConstructorCallExpression methodCall = (ConstructorCallExpression) node;
            String name = methodCall.getType().getNameWithoutPackage();
            // +4 because we don't want to have "new " in the offset
            // would be good to do this in more sofisticated way than this shit
            int start = getOffset(doc, lineNumber, columnNumber + 4);
            return getNextIdentifierByName(doc, name, start);
        } else if (node instanceof ClassExpression) {
            ClassExpression clazz = (ClassExpression) node;
            String name = clazz.getType().getNameWithoutPackage();
            int start = getOffset(doc, lineNumber, columnNumber);
            return getNextIdentifierByName(doc, name, start);
        } else if (node instanceof ConstantExpression) {
            ConstantExpression constantExpression = (ConstantExpression) node;
            int start = getOffset(doc, lineNumber, columnNumber);
            return new OffsetRange(start, start + constantExpression.getText().length());
        } 
//        else if (node instanceof FakeNode) {
//            final String typeName = ElementUtils.getTypeNameWithoutPackage(((FakeNode) node).getOriginalNode());
//            final int start = getOffset(doc, lineNumber, columnNumber);
//            
//            return getNextIdentifierByName(doc, typeName, start);
//        }
*/        
        }
        return OffsetRange.NONE;
    }

    @SuppressWarnings("unchecked")
    public static List<Node> children(Node root) {
        List<Node> children = new ArrayList<>();

        if (root instanceof Script) {
            Script script = (Script) root;
//            children.addAll(script.getClasses());
//            children.add(script.getStatementBlock());
        } else if (root instanceof ClassDefinition) {
            ClassDefinition classNode = (ClassDefinition) root;

            Set<String> possibleMethods = new HashSet<>();
            /*
            for (Object object : classNode.getProperties()) {
                PropertyNode property = (PropertyNode) object;
                if (property.getLineNumber() >= 0) {
                    children.add(property);

                    FieldDeclaration field = property.getField();
                    String fieldName = field.getName();
                    String fieldTypeName = field.getType().getNameWithoutPackage();

                    if (fieldName.length() > 0 && !field.isStatic() && (field.getModifiers() & Opcodes.ACC_PRIVATE) != 0) {

                        fieldName = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1, fieldName.length());
                        if (!field.isFinal()) {
                            possibleMethods.add("set" + fieldName); // NOI18N
                        }
                        possibleMethods.add("get" + fieldName); // NOI18N

                        if ("Boolean".equals(fieldTypeName) || "boolean".equals(fieldTypeName)) { // NOI18N
                            possibleMethods.add("is" + fieldName); // NOI18N
                        }
                    }
                }

            }
            */
/*
            for (FieldDeclaration field : classNode.getFields()) {
                if (field.getLineNumber() >= 0) {
                    children.add(field);
                }
            }

            for (MethodDefinition method : classNode.getMethods()) {
                // getMethods() returns all methods also from superclasses
                // how to get only methods from source?
                // for now, just check line number, if < 0 it is not from source
                // Second part of condition is for generated accessors
                if ((!method.isSynthetic() && method.getCode() != null)
                        || (method.isSynthetic() && possibleMethods.contains(method.getName()))) {
                    children.add(method);
                }

            }

            for (ConstructorNode constructor : classNode.getDeclaredConstructors()) {
                if (constructor.getLineNumber() >= 0) {
                    children.add(constructor);
                }
            }
*/
//        } else if (root instanceof MethodDefinition) {
//            MethodDefinition methodNode = (MethodDefinition) root;
//            children.add(methodNode.getCode());
//            children.addAll(Arrays.asList(methodNode.getParameters()));
//        } else if (root instanceof Parameter) {
//        } else if (root instanceof FieldDeclaration) {
//            FieldDeclaration fieldNode = (FieldDeclaration) root;
//            Expression expression = fieldNode.getInitialExpression();
//            if (expression != null) {
//                children.add(expression);
//            }
//        } else if (root instanceof PropertyNode) {
//            // FIXME (?)
        } else if (root != null) {
//            ASTChildrenVisitor astChildrenSupport = new ASTChildrenVisitor();
//            root.visit(astChildrenSupport);
//            children = astChildrenSupport.children();
        }

        return children;
    }

    /**
     * Find offset in text for given line and column
     * Never returns negative number
     */
    

    public static int getOffset(BaseDocument doc, int lineNumber, int columnNumber) 
    {
        
        assert lineNumber > 0 : "Line number must be at least 1 and was: " + lineNumber;
        assert columnNumber > 0 : "Column number must be at least 1 ans was: " + columnNumber;

        int offset = Utilities.getRowStartFromLineOffset(doc, lineNumber - 1);
        offset += (columnNumber - 1);

        // some sanity checks
        if (offset < 0){
            offset = 0;
        }

        return offset;
    }

    public static Node getForeignNode(final IndexedElement o) {

        final Node[] nodes = new Node[1];
        FileObject fileObject = o.getFileObject();
        assert fileObject != null : "null FileObject for IndexedElement " + o;

        try {
            Source source = Source.create(fileObject);
            // FIXME can we move this out of task (?)
            ParserManager.parse(Collections.singleton(source), new UserTask() {
                @Override
                public void run(ResultIterator resultIterator) throws Exception {
                    MirahParser.NBMirahParserResult result = ASTUtils.getParseResult(resultIterator.getParserResult());
                    
                    String signature = o.getSignature();
                    if (signature == null) {
                        return;
                    }
                    // strip class name from signature: Foo#method1() -> method1()
                    int index = signature.indexOf('#');
                    if (index != -1) {
                        signature = signature.substring(index + 1);
                    }
//                    for (ASTElement element : result.getStructure().getElements()) {
//                        Node node = findBySignature(element, signature);
//                        if (node != null) {
//                            nodes[0] = node;
//                            return;
//                        }
                  //  }
                }
            });
        } catch (ParseException ex) {
            Exceptions.printStackTrace(ex);
        }
        return nodes[0];
    }
/*
    private static Node findBySignature(ASTElement root, String signature) {

        if (signature.equals(root.getSignature())) {
            return root.getNode();
        } else {
//            for (ASTElement element : root.getChildren()) {
//                Node node = findBySignature(element, signature);
//                if (node != null) {
//                    return node;
//                }
//            }
        }
        return null;
    }
*/
    public static String getDefSignature(MethodDefinition node) {
        StringBuilder sb = new StringBuilder();
        /*
        sb.append(node.getName());

        Parameter[] parameters = node.getParameters();
        if (parameters.length > 0) {
            sb.append('('); // NOI18N
            Iterator<Parameter> it = Arrays.asList(parameters).iterator();
            sb.append(ru.programpark.mirah.editor.java.Utilities.translateClassLoaderTypeName(
                    it.next().getType().getName()));

            while (it.hasNext()) {
                sb.append(','); // NOI18N
                sb.append(ru.programpark.mirah.editor.java.Utilities.translateClassLoaderTypeName(
                        it.next().getType().getName()));
            }
            sb.append(')'); // NOI18N
        }
        */
        return sb.toString();
    }

    public static OffsetRange getNextIdentifierByName(final BaseDocument doc, final String fieldName, final int startOffset) {
        final String identifier;
        if (fieldName.endsWith("[]")) { // NOI18N
            identifier = fieldName.substring(0, fieldName.length() - 2);
        } else {
            identifier = fieldName;
        }

        // since Groovy 1.5.6 the start offset is on 'def' on field/method declaration:
        // ^def foo = ...
        // ^Map bar = ...
        // find first token that is identifier and that matches given name
        final OffsetRange[] result = new OffsetRange[] { OffsetRange.NONE };
        doc.render(new Runnable() {
            @Override
            public void run() {
                TokenSequence<MirahTokenId> ts = LexUtilities.getPositionedSequence(doc, startOffset);
                if (ts != null) {
                    Token<MirahTokenId> token = ts.token();
                    if (token != null && token.id().is(Tokens.tIDENTIFIER) && TokenUtilities.endsWith(identifier, token.text())) {
                        result[0] = computeRange(ts, token);
                        return;
                    }
                    while (ts.moveNext()) {
                        token = ts.token();
                        if (token != null && token.id().is(Tokens.tIDENTIFIER) && TokenUtilities.endsWith(identifier, token.text())) {
                            result[0] = computeRange(ts, token);
                            return;
                        }
                    }
                }
            }

            private OffsetRange computeRange(TokenSequence<MirahTokenId> ts, Token<MirahTokenId> token) {
                int start = ts.offset() + token.text().length() - identifier.length();
                int end = ts.offset() + token.text().length();

                if (start < 0) {
                    start = 0;
                }

                return new OffsetRange(start, end);
            }
        });
        return result[0];
    }

    /**
     * Compute the surrounding class name for the given node path or empty string
     * if none was found
     */
    public static String getFqnName(AstPath path) {
        ClassDefinition classNode = getOwningClass(path);
        return classNode == null ? "" : classNode.name().identifier(); // NOI18N
    }

    public static ClassDefinition getOwningClass(AstPath path) {
        Iterator<Node> it = path.rootToLeaf();
        while (it.hasNext()) {
            Node node = it.next();
            if (node instanceof ClassDefinition) {
                return (ClassDefinition) node;

            }
        }
        return null;
    }

    public static Node getScope(AstPath path, Variable variable) {
        for (Iterator<Node> it = path.iterator(); it.hasNext();) {
            Node scope = it.next();
//            if (scope instanceof ClosureExpression) {
//                VariableScope variableScope = ((ClosureExpression) scope).getVariableScope();
//                if (variableScope.getDeclaredVariable(variable.getName()) != null) {
//                    return scope;
//                } else {
//                    // variables defined inside closure are not catched in VariableScope
//                    // let's get the closure's code block and try there
//                    Statement statement = ((ClosureExpression) scope).getCode();
//                    if (statement instanceof BlockStatement) {
//                        variableScope = ((BlockStatement) statement).getVariableScope();
//                        if (variableScope.getDeclaredVariable(variable.getName()) != null) {
//                            return scope;
//                        }
//                    }
//                }
//            } else 
            /*
            if (scope instanceof MethodDefinition || scope instanceof ConstructorNode) {
                VariableScope variableScope = ((MethodDefinition) scope).getVariableScope();
                if (variableScope.getDeclaredVariable(variable.getName()) != null) {
                    return scope;
                } else {
                    // variables defined inside method are not catched in VariableScope
                    // let's get the method's code block and try there
                    Statement statement = ((MethodDefinition) scope).getCode();
                    if (statement instanceof BlockStatement) {
                        variableScope = ((BlockStatement) statement).getVariableScope();
                        if (variableScope.getDeclaredVariable(variable.getName()) != null) {
                            return scope;
                        }
                    }
                }
//            } else if (scope instanceof ForStatement) {
//                VariableScope variableScope = ((ForStatement) scope).getVariableScope();
//                if (variableScope.getDeclaredVariable(variable.getName()) != null) {
//                    return scope;
//                }
//            } else if (scope instanceof BlockStatement) {
//                VariableScope variableScope = ((BlockStatement) scope).getVariableScope();
//                if (variableScope.getDeclaredVariable(variable.getName()) != null) {
//                    return scope;
//                }
            } else 
//            if (scope instanceof ClosureListExpression) {
//                VariableScope variableScope = ((ClosureListExpression) scope).getVariableScope();
//                if (variableScope.getDeclaredVariable(variable.getName()) != null) {
//                    return scope;
//                }
//            } else 
            if (scope instanceof ClassDefinition) {
                ClassDefinition classNode = (ClassDefinition) scope;
                if (classNode.getField(variable.getName()) != null) {
                    return scope;
                }
            } else if (scope instanceof Script) {
                Script script = (Script) scope;
                BlockStatement blockStatement = script.getStatementBlock();
                VariableScope variableScope = blockStatement.getVariableScope();
                if (variableScope.getDeclaredVariable(variable.getName()) != null) {
                    return blockStatement;
                }
                // probably in script where variable is defined withoud 'def' keyword:
                // myVar = 1
                // echo myVar
                Variable classVariable = variableScope.getReferencedClassVariable(variable.getName());
                if (classVariable != null) {
                    return script;
                }
            }
            */
        }
        return null;
    }

    /**
     * Doesn't check VariableScope if variable is declared there,
     * but assumes it is there and makes search for given variable
     */
    public static Node getVariable(Node scope, String variable, AstPath path, BaseDocument doc, int cursorOffset) {
/*        
        if (scope instanceof ClosureExpression) {
            ClosureExpression closure = (ClosureExpression) scope;
            for (Parameter parameter : closure.getParameters()) {
                if (variable.equals(parameter.getName())) {
                    return parameter;
                }
            }
            Statement code = closure.getCode();
            if (code instanceof BlockStatement) {
                return getVariableInBlockStatement((BlockStatement) code, variable);
            }
        } else if (scope instanceof MethodDefinition) {
            MethodDefinition method = (MethodDefinition) scope;
            for (Parameter parameter : method.getParameters()) {
                if (variable.equals(parameter.getName())) {
                    return parameter;
                }
            }
            Statement code = method.getCode();
            if (code instanceof BlockStatement) {
                return getVariableInBlockStatement((BlockStatement) code, variable);
            }
        } else if (scope instanceof ConstructorNode) {
            ConstructorNode constructor = (ConstructorNode) scope;
            for (Parameter parameter : constructor.getParameters()) {
                if (variable.equals(parameter.getName())) {
                    return parameter;
                }
            }
            Statement code = constructor.getCode();
            if (code instanceof BlockStatement) {
                return getVariableInBlockStatement((BlockStatement) code, variable);
            }
        } else if (scope instanceof ForStatement) {
            ForStatement forStatement = (ForStatement) scope;
            Parameter parameter = forStatement.getVariable();
            if (variable.equals(parameter.getName())) {
                return parameter;
            }
            Expression collectionExpression = forStatement.getCollectionExpression();
//            if (collectionExpression instanceof ClosureListExpression) {
//                Node result = getVariableInClosureList((ClosureListExpression) collectionExpression, variable);
//                if (result != null) {
//                    return result;
//                }
//            }
            Statement code = forStatement.getLoopBlock();
            if (code instanceof BlockStatement) {
                Node result = getVariableInBlockStatement((BlockStatement) code, variable);
                if (result != null) {
                    return result;
                }
            }
        } else if (scope instanceof BlockStatement) {
            return getVariableInBlockStatement((BlockStatement) scope, variable);
        } 
//        else if (scope instanceof ClosureListExpression) {
//            return getVariableInClosureList((ClosureListExpression) scope, variable);
//        } 
        else if (scope instanceof ClassDefinition) {
            return ((ClassDefinition) scope).getField(variable);
        } else if (scope instanceof Script) {
            Script script = (Script) scope;
            BlockStatement blockStatement = script.getStatementBlock();
            Node result = getVariableInBlockStatement(blockStatement, variable);
            if (result == null) {
                // probably in script where variable is defined withoud 'def' keyword:
                // myVar = 1
                // echo myVar
                VariableScope variableScope = blockStatement.getVariableScope();
                if (variableScope.getReferencedClassVariable(variable) != null) {
                    // let's take first occurrence of the variable
//                    VariableScopeVisitor scopeVisitor = new VariableScopeVisitor(script.getContext(), path, doc, cursorOffset);
//                    scopeVisitor.collect();
//                    Set<Node> occurrences = scopeVisitor.getOccurrences();
//                    if (!occurrences.isEmpty()) {
//                        result = occurrences.iterator().next();
//                    }
                }
            }
            return result;
        }
*/        
        return null;
    }
/*
    private static Node getVariableInBlockStatement(BlockStatement block, String variable) {
        for (Object object : block.getStatements()) {
//            if (object instanceof ExpressionStatement) {
//                ExpressionStatement expressionStatement = (ExpressionStatement) object;
//                Expression expression = expressionStatement.getExpression();
//                if (expression instanceof DeclarationExpression) {
//                    DeclarationExpression declaration = (DeclarationExpression) expression;
//                    if (variable.equals(declaration.getVariableExpression().getName())) {
//                        return declaration.getVariableExpression();
//                    }
//                }
//            }
        }
        return null;
    }
*/    
/*
    private static Node getVariableInClosureList(ClosureListExpression closureList, String variable) {
        for (Object object : closureList.getExpressions()) {
            if (object instanceof DeclarationExpression) {
                DeclarationExpression declaration = (DeclarationExpression) object;
                if (variable.equals(declaration.getVariableExpression().getName())) {
                    return declaration.getVariableExpression();
                }
            }
        }
        return null;
    }
*/
    private static int getLimit(Node node, BaseDocument doc, int docLength) {
        int limit = 0;
                
//                (node.getLastLineNumber() > 0 && node.getLastColumnNumber() > 0)
//                ? getOffset(doc, node.getLastLineNumber(), node.getLastColumnNumber())
//                : docLength;

        if (limit > docLength) {
            limit = docLength;
        }
        return limit;
    }

    /**
     * Use this if you need some part of node that is not available as node.
     * For example return type of method definition is not accessible as node,
     * so I am wrapping MethodDefinition in this FakeNode and I also provide
     * text to compute OffsetRange for...
     *
     * This class is heavily used across both editor and refactoring module. In
     * a lot of cases it makes no sense to use it and a lot of those cases should
     * be removed.
     */
    public static final class FakeNode { //extends Node {

        private final String name;
        private final Node node;

        public FakeNode(Node node) {
//            this(node, node.getText());
            this.node = node;
            this.name = node.toString();
        }

        public FakeNode(Node node, String name) {
            this.node = node;
            this.name = name;
/*
            setLineNumber(node.getLineNumber());
            setColumnNumber(node.getColumnNumber());
            setLastLineNumber(node.getLastLineNumber());
            setLastColumnNumber(node.getLastColumnNumber());
*/        
        }

        public Node getOriginalNode() {
            return node;
        }

//        @Override
        public String getText() {
            return name;
        }

//        @Override
//        public void visit(GroovyCodeVisitor visitor) {}

        @Override
        public int hashCode() {
            int hash = 7;
//            hash = 71 * hash + (this.name != null ? this.name.hashCode() : 0);
//            hash = 71 * hash + this.getLineNumber();
//            hash = 71 * hash + this.getColumnNumber();
//            hash = 71 * hash + this.getLastLineNumber();
//            hash = 71 * hash + this.getLastColumnNumber();
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final FakeNode other = (FakeNode) obj;
            if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
                return false;
            }
            /*
            if (this.getLineNumber() != other.getLineNumber()) {
                return false;
            }
            if (this.getColumnNumber() != other.getColumnNumber()) {
                return false;
            }
            if (this.getLastLineNumber() != other.getLastLineNumber()) {
                return false;
            }
            if (this.getLastColumnNumber() != other.getLastColumnNumber()) {
                return false;
            }
                    */
            return true;
        }
    }
    
    public static Node findLeaf( ParserResult parseResult, BaseDocument doc, final int offset) {

        Node root = ASTUtils.getRoot(parseResult);
        if (root == null) return null;

        final Node leaf[] = new Node[1];
        leaf[0] = null;
        root.accept(new NodeScanner() {
            @Override
            public boolean enterDefault(Node node, Object arg) {
                if ( node != null && node.position() != null 
                    && offset >= node.position().startChar()
                    && offset < node.position().endChar())
                {
                    if (leaf[0] == null
                    || node.position().endChar() - node.position().startChar()
                    < leaf[0].position().endChar() - leaf[0].position().startChar())
                    leaf[0] = node;
                }
                return super.enterDefault(node, arg); //To change body of generated methods, choose Tools | Templates.
            }

        }, null);

        return leaf[0];
    }

    
}
