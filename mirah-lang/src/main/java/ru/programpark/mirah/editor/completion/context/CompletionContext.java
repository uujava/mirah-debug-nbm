package ru.programpark.mirah.editor.completion.context;

import ru.programpark.mirah.lexer.MirahTokenId;
import java.util.Iterator;
import java.util.Set;
import javax.swing.text.BadLocationException;
import mirah.impl.Tokens;
import mirah.lang.ast.ClassDefinition;
import mirah.lang.ast.MethodDefinition;
import mirah.lang.ast.Node;
import mirah.lang.ast.Script;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.editor.BaseDocument;
import org.netbeans.editor.Utilities;
import org.netbeans.modules.csl.spi.ParserResult;
import org.openide.filesystems.FileObject;
import ru.programpark.mirah.editor.ast.ASTUtils;
import ru.programpark.mirah.editor.ast.AstPath;
import ru.programpark.mirah.editor.completion.AccessLevel;
import ru.programpark.mirah.editor.utils.LexUtilities;

/**
 *
 * @author Petr Hejl
 * @author Martin Janicek
 */
public final class CompletionContext {

    private final ParserResult parserResult;
    private final FileObject sourceFile;
    
    private String typeName;
    private String prefix;
    private int anchor;
    private boolean nameOnly;
    
    public final int lexOffset;
    public final int astOffset;
    public final BaseDocument doc;
    
    public boolean scriptMode;
    public CaretLocation location;
    public CompletionSurrounding context;
    public AstPath path;
    public ClassDefinition declaringClass;
    public DotCompletionContext dotContext;
    public Set<AccessLevel> access;
//    private Object LexUtilities;


    public CompletionContext(
            ParserResult parseResult,
            String prefix,
            int anchor,
            int lexOffset,
            int astOffset,
            BaseDocument doc) {

        this.parserResult = parseResult;
        this.sourceFile = parseResult.getSnapshot().getSource().getFileObject();
        this.prefix = prefix;
        this.anchor = anchor;
        this.lexOffset = lexOffset;
        this.astOffset = astOffset;
        this.doc = doc;
        
        this.path = getPathFromRequest();
        this.location = getCaretLocationFromRequest();

        // now let's figure whether we are in some sort of definition line
        this.context = getCompletionContext();

        // Are we invoked right behind a dot? This is information is used later on in
        // a couple of completions.
        this.dotContext = getDotCompletionContext();
        this.nameOnly = dotContext != null && dotContext.isMethodsOnly();

        this.declaringClass = getBeforeDotDeclaringClass();
    }
    
    // TODO: Move this to the constructor and change ContextHelper.getSurroundingClassDefinition()
    // to prevent NPE caused by leaking this in constructor
    public void init() {
        if (declaringClass != null) {
            this.access = AccessLevel.create(ContextHelper.getSurroundingClassDefinition(this), declaringClass);
        } else {
            this.access = null;
        }
    }

    public ParserResult getParserResult() {
        return parserResult;
    }
    
    public FileObject getSourceFile() {
        return sourceFile;
    }

    public ClassDefinition getSurroundingClass() {
        return ContextHelper.getSurroundingClassDefinition(this);
    }

    public String getTypeName() {
        return typeName;
    }

    public String getPrefix() {
        return prefix;
    }

    public int getAnchor() {
        return anchor;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }
    
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void setAnchor(int anchor) {
        this.anchor = anchor;
    }
    
    public boolean isBehindDot() {
        return dotContext != null;
    }

    public boolean isNameOnly() {
        return nameOnly;
    }

    /**
     * Calculate an AstPath from a given request or null if we can not get a
     * AST root-node from the request.
     *
     * @return a freshly created AstPath object for the offset given in the request
     */
    private AstPath getPathFromRequest() {
        Node root = ASTUtils.getRoot(parserResult);

        // in some cases we can not repair the code, therefore root == null
        // therefore we can not complete. See # 131317
        if (root == null) {
            return null;
        }

        return new AstPath(root, astOffset, doc);
    }

    private AstPath getPath(ParserResult parseResult, BaseDocument doc, int astOffset) {
        Node root = ASTUtils.getRoot(parseResult);

        // in some cases we can not repair the code, therefore root == null
        // therefore we can not complete. See # 131317
        if (root == null) {
            return null;
        }
        return new AstPath(root, astOffset, doc);
    }
    /**
     * Figure-out, where we are in the code (comment, CU, class, method, etc.).
     *
     * @return concrete caret location type
     */
    private CaretLocation getCaretLocationFromRequest() {
        int position = lexOffset;
        TokenSequence<MirahTokenId> ts = LexUtilities.getMirahTokenSequence(doc, position);
        
        // are we living inside a comment?

        ts.move(position);

        if (ts.isValid() && ts.moveNext() && ts.offset() < doc.getLength()) {
            Token<MirahTokenId> t = ts.token();

            MirahTokenId mt = t.id();
            
            Class c = Tokens.tNL.getClass();
            
//            if (t.id() == MirahTokenId.LINE_COMMENT || t.id() == MirahTokenId.BLOCK_COMMENT) {
            if (t.id().is(Tokens.tComment) ) { //|| t.id() == MirahTokenId.BLOCK_COMMENT) {
                return CaretLocation.INSIDE_COMMENT;
            }

//            if (t.id() == MirahTokenId.STRING_LITERAL) {
            if (t.id().is(Tokens.tStringContent)) {
                return CaretLocation.INSIDE_STRING;
            }
            // This is a special case. If we have a NLS right behind a LINE_COMMENT it
            // should be treated as a CaretLocation.INSIDE_COMMENT. Therefore we have to rewind.

            if (t.id().is(Tokens.tNL)) {
                if ((ts.isValid() && ts.movePrevious() && ts.offset() >= 0)) {
                    Token<MirahTokenId> tparent = ts.token();
//                    if (tparent.id() == MirahTokenId.LINE_COMMENT) {
                    if (tparent.id().is(Tokens.tComment)) { //.LINE_COMMENT) {
                        return CaretLocation.INSIDE_COMMENT;
                    }
                }
            }
        }


        // Are we above the package statement?
        // We try to figure this out by moving down the lexer Stream

        ts.move(position);

        while (ts.isValid() && ts.moveNext() && ts.offset() < doc.getLength()) {
            Token<MirahTokenId> t = ts.token();
            MirahTokenId mt = t.id();

            if (t.id().is(Tokens.tPackage)) {
                return CaretLocation.ABOVE_PACKAGE;
            }
        }

        // Are we before the first class or interface statement?
        // now were heading to the beginning to the document ...

        boolean classDefBeforePosition = false;

        ts.move(position);

        while (ts.isValid() && ts.movePrevious() && ts.offset() >= 0) {
            Token<MirahTokenId> t = ts.token();
            MirahTokenId mt = t.id();
            if (t.id().is(Tokens.tClass) || t.id().is(Tokens.tInterface)) {
                classDefBeforePosition = true;
                break;
            }
        }


        boolean classDefAfterPosition = false;

        ts.move(position);

        while (ts.isValid() && ts.moveNext() && ts.offset() < doc.getLength()) {
            Token<MirahTokenId> t = ts.token();
            MirahTokenId mt = t.id();
            if (t.id().is(Tokens.tClass) || t.id().is(Tokens.tInterface)) {
                classDefAfterPosition = true;
                break;
            }
        }

        if (path != null) {
            Node node = path.root();
            if (node instanceof Script) {
                Script module = (Script) node;
                String name = null;
                /*
                for (ClassDefinition clazz : module.getClasses()) {
                    if (clazz.isScript()) {
                        name = clazz.getName();
                        scriptMode = true;
                        break;
                    }
                }
*/
                // we have a script class - lets see if there is another
                // non-script class with same name that would mean we are just
                // broken class, not a script
                if (name != null) {
//                    for (ClassDefinition clazz : module.getClasses()) {
//                        if (!clazz.isScript() && name.equals(clazz.getName())) {
//                            scriptMode = false;
//                            break;
//                        }
//                    }
                }
            }
        }

        if (!scriptMode && !classDefBeforePosition && classDefAfterPosition) {
            return CaretLocation.ABOVE_FIRST_CLASS;
        }

        // If there's *no* class definition in the file we are running in a
        // script with synthetic wrapper class and wrapper method: run().
        // See GINA, ch. 7

        if (!classDefBeforePosition && scriptMode) {
            return CaretLocation.INSIDE_METHOD;
        }


        if (path == null) {
            return null;
        }



        /* here we loop from the tail of the path (innermost element)
        up to the root to figure out where we are. Some of the trails are:

        In main method:
        Path(4)=[Script:ClassDefinition:MethodDefinition:ConstantExpression:]

        In closure, which sits in a method:
        Path(7)=[Script:ClassDefinition:MethodDefinition:DeclarationExpression:DeclarationExpression:VariableExpression:ClosureExpression:]

        In closure directly attached to class:
        Path(4)=[Script:ClassDefinition:PropertyNode:FieldDeclaration:]

        In a class, outside method, right behind field declaration:
        Path(4)=[Script:ClassDefinition:PropertyNode:FieldDeclaration:]

        Right after a class declaration:
        Path(2)=[Script:ClassDefinition:]

        Inbetween two classes:
        [Script:ConstantExpression:]

        Outside of any class:
        Path(1)=[Script:]

        Start of Parameter-list:
        Path(4)=[Script:ClassDefinition:MethodNode:Parameter:]

         */

        for (Iterator<Node> it = path.iterator(); it.hasNext();) {
            Node current = it.next();
//            if (current instanceof ClosureExpression) {
//                return CaretLocation.INSIDE_CLOSURE;
//            } else if (current instanceof FieldDeclaration) {
//                FieldDeclaration fn = (FieldDeclaration) current;
//                if (fn.isClosureSharedVariable()) {
//                    return CaretLocation.INSIDE_CLOSURE;
//                }
//            } else 
            if (current instanceof MethodDefinition) {
                return CaretLocation.INSIDE_METHOD;
            } else if (current instanceof ClassDefinition) {
                return CaretLocation.INSIDE_CLASS;
            } else if (current instanceof Script) {
                return CaretLocation.OUTSIDE_CLASSES;
            } 
            else {
//            else if (current instanceof Parameter) {
//                return CaretLocation.INSIDE_PARAMETERS;
//            } else if (current instanceof ConstructorCallExpression || current instanceof NamedArgumentListExpression) {
//                ts.move(position);

                boolean afterLeftParen = false;

                WHILE_CYCLE:
                while (ts.isValid() && ts.movePrevious() && ts.offset() >= 0) {
                    Token<MirahTokenId> t = ts.token();
                    if ( t.id().is(Tokens.tLParen) )
                    {
//                        case LPAREN:
                            afterLeftParen = true;
                            break WHILE_CYCLE;
                    }
                    if ( t.id().is(Tokens.tLParen) ) //???
                    {
//                        case LITERAL_new:
                            break WHILE_CYCLE;
                    }
                }

                ts.move(position);
                boolean beforeRightParen = false;

                WHILE_CYCLE_2:
                while (ts.isValid() && ts.moveNext() && ts.offset() >= 0) {
                    Token<MirahTokenId> t = ts.token();
                    if  ( t.id().is(Tokens.tRParen) )
                    {
//                        case RPAREN:
                            beforeRightParen = true;
                            break WHILE_CYCLE_2;
                    }
                    if  ( t.id().is(Tokens.tSemi) )
                    {
//                        case SEMI:
                          break WHILE_CYCLE_2;
                    }
                }

                // We are almost certainly inside of constructor call
                if (afterLeftParen && beforeRightParen) {
                    return CaretLocation.INSIDE_CONSTRUCTOR_CALL;
                }
            }
        }
        return CaretLocation.UNDEFINED;
    }

    /**
     * Computes an CompletionContext which surrounds the request.
     * Three tokens in front and three after the request.
     *
     * @return completion context
     */
    private CompletionSurrounding getCompletionContext() {
        int position = lexOffset;

        Token<MirahTokenId> beforeLiteral = null;
        Token<MirahTokenId> before2 = null;
        Token<MirahTokenId> before1 = null;
        Token<MirahTokenId> active = null;
        Token<MirahTokenId> after1 = null;
        Token<MirahTokenId> after2 = null;
        Token<MirahTokenId> afterLiteral = null;

        TokenSequence<MirahTokenId> ts = LexUtilities.getMirahTokenSequence(doc, position);

        int difference = ts.move(position);

        // get the active token:

        if (ts.isValid() && ts.moveNext() && ts.offset() >= 0) {
            active = ts.token();
        }

        // if we are right at the end of a line, a separator or a whitespace we gotta rewind.

        // 1.) NO  str.^<NLS>
        // 2.) NO  str.^subString
        // 3.) NO  str.sub^String
        // 4.) YES str.subString^<WHITESPACE-HERE>
        // 5.) YES str.subString^<NLS>
        // 6.) YES str.subString^()


        if (active != null) {
            if ((active.id().is(Tokens.tWhitespace) && difference == 0)) {
                ts.movePrevious();
            } 
            /*
            else if (active.id() == MirahTokenId.NLS ) {
                ts.movePrevious();
                if (ts.token().id() == MirahTokenId.AT ||
                    ts.token().id() == MirahTokenId.DOT ||
                    ts.token().id() == MirahTokenId.SPREAD_DOT ||
                    ts.token().id() == MirahTokenId.OPTIONAL_DOT ||
                    ts.token().id() == MirahTokenId.MEMBER_POINTER ||
                    ts.token().id() == MirahTokenId.ELVIS_OPERATOR) {
                    ts.moveNext();
                }
            }
            */
        }


        // Travel to the beginning to get before2 and before1

        int stopAt = 0;

        while (ts.isValid() && ts.movePrevious() && ts.offset() >= 0) {
            Token<MirahTokenId> t = ts.token();
//            if (t.id() == MirahTokenId.NLS) {
            if (t.id() == MirahTokenId.NL) {
                break;
            } else if (!t.id().is(Tokens.tWhitespace)) {
                if (stopAt == 0) {
                    before1 = t;
                } else if (stopAt == 1) {
                    before2 = t;
                } else if (stopAt == 2) {
                    break;
                }

                stopAt++;
            }
        }

        // Move to the beginning (again) to get the next left-hand-sight literal

        ts.move(position);

        while (ts.isValid() && ts.movePrevious() && ts.offset() >= 0) {
            Token<MirahTokenId> t = ts.token();
            if (t.id() == MirahTokenId.NL ||
                t.id() == MirahTokenId.LBRACE) {
                break;
            } else if (t.id().primaryCategory().equals("keyword")) {
                beforeLiteral = t;
                break;
            }
        }

        // now looking for the next right-hand-sight literal in the opposite direction

        ts.move(position);

        while (ts.isValid() && ts.moveNext() && ts.offset() < doc.getLength()) {
            Token<MirahTokenId> t = ts.token();
            if (t.id() == MirahTokenId.NL ||
                t.id() == MirahTokenId.RBRACE) {
                break;
            } else if (t.id().primaryCategory().equals("keyword")) {
                afterLiteral = t;
                break;
            }
        }


        // Now we're heading to the end of that stream

        ts.move(position);
        stopAt = 0;

        while (ts.isValid() && ts.moveNext() && ts.offset() < doc.getLength()) {
            Token<MirahTokenId> t = ts.token();

            if (t.id() == MirahTokenId.NL) {
                break;
            } else if (!t.id().is(Tokens.tWhitespace) && t != active) {
                if (stopAt == 0) {
                    after1 = t;
                } else if (stopAt == 1) {
                    after2 = t;
                    break;
                } else if (stopAt == 2) {
                    break;
                }
                stopAt++;
            }
        }
        return new CompletionSurrounding(beforeLiteral, before2, before1, active, after1, after2, afterLiteral, ts);
    }

    private DotCompletionContext getDotCompletionContext() {
        
//        if ( true ) return null;
        
        if (dotContext != null) {
            return dotContext;
        }

        TokenSequence<MirahTokenId> ts = LexUtilities.getMirahTokenSequence(doc, lexOffset);
        ts.move(lexOffset);

        // get the active token:
        Token<MirahTokenId> active = null;
        if (ts.isValid() && ts.moveNext() && ts.offset() >= 0) {
            active = ts.token();
        }

        // this should move us to dot or whitespace or NLS or prefix
        if (ts.isValid() && ts.movePrevious() && ts.offset() >= 0) {
            MirahTokenId tokenID = ts.token().id();
            if ( !tokenID.is(Tokens.tDot) && !tokenID.is(Tokens.tNL) && ! tokenID.is(Tokens.tWhitespace)) 
            {
                // is it prefix
                // keyword check is here because of issue #150862
                if (tokenID.is(Tokens.tIDENTIFIER) && !tokenID.primaryCategory().equals("keyword")) {
                    return null;
                } else {
                    ts.movePrevious();
                }
            }
        }

        boolean fieldsOnly = false;
        if (ts.token().id().is(Tokens.tAt)) {
            // We are either on Java Field Override operator *. or Spread Java Field operator *.@
            // Just move to previous and handle in the same way as DOT/SPREAD_DOT
            ts.movePrevious();
            fieldsOnly = true;
        }
        // now we should be on dot or in whitespace or NLS after the dot
        boolean remainingTokens = true;
        if ( !ts.token().id().is(Tokens.tDot) ) {
            // travel back on the token string till the token is neither a
            // WHITESPACE nor NLS
            while (ts.isValid() && (remainingTokens = ts.movePrevious()) && ts.offset() >= 0) {
                Token<MirahTokenId> t = ts.token();
                if (!t.id().is(Tokens.tWhitespace) && !t.id().is(Tokens.tNL)) {
                    break;
                }
            }
        }
        if (!ts.token().id().is(Tokens.tDot) || !remainingTokens) {
            return null; // no astpath
        }

        boolean methodsOnly = false;
//        if (ts.token().id() == MirahTokenId.MEMBER_POINTER) {
//            methodsOnly = true;
//        }

        // travel back on the token string till the token is neither a
        // WHITESPACE nor NLS
        while (ts.isValid() && ts.movePrevious() && ts.offset() >= 0) {
            Token<MirahTokenId> t = ts.token();
            if (!t.id().is(Tokens.tWhitespace) && !t.id().is(Tokens.tNL) ) {
                break;
            }
        }

        int lexOffset = ts.offset();
        int astOffset = ASTUtils.getAstOffset(parserResult, lexOffset);
        AstPath realPath = getPath(parserResult, doc, astOffset);

        return new DotCompletionContext(lexOffset, astOffset, realPath, fieldsOnly, methodsOnly);
    }

    /**
     * Get the ClassDefinition for the before-dot expression. This is important for
     * field and method completion.
     * <p>
     * If the <code>request.declaringClass</code> is not <code>null</code>
     * this value is immediately returned.
     * <p>
     * Returned value is stored to <code>request.declaringClass</code> too.
     *
     * Here are some sample paths:
     *
     * new String().
     * [Script:ConstructorCallExpression:ExpressionStatement:ConstructorCallExpression:]
     *
     * new String().[caret] something_unrelated
     * [Script:ClassDefinition:MethodCallExpression]
     * for this case we have to go for object expression of the method call
     *
     * s.
     * [Script:VariableExpression:ExpressionStatement:VariableExpression:]
     *
     * s.spli
     * [Script:PropertyExpression:ConstantExpression:ExpressionStatement:PropertyExpression:ConstantExpression:]
     *
     * l.
     * [Script:ClassDefinition:MethodDefinition:ExpressionStatement:VariableExpression:]
     *
     * l.ab
     * [Script:ClassDefinition:MethodDefinition:ExpressionStatement:PropertyExpression:ConstantExpression:]
     *
     * l.M
     * [Script:ClassDefinition:MethodDefinition:ExpressionStatement:PropertyExpression:VariableExpression:ConstantExpression:]
     *
     * @return a valid Node or null
     */
    private ClassDefinition getBeforeDotDeclaringClass() {
        if (declaringClass != null && declaringClass instanceof ClassDefinition) {
            return declaringClass;
        }
        
        // This basically means we are not interested in classic type interference (because there will
        // be list, map or something like that) and we rather want to know what type is collected there
        if (isAfterSpreadOperator() || isAfterSpreadJavaFieldOperator()) {
/*            
            if (dotContext.getAstPath().leaf() instanceof Expression) {
                Expression expression = (Expression) dotContext.getAstPath().leaf();

                if (expression instanceof ListExpression) {
                    ListExpression listExpression = (ListExpression) expression;
                    for (Expression expr : listExpression.getExpressions()) {
                        return expr.getType();
                    }
                }
            }
*/            
        }

        // FIXME move this up
        DotCompletionContext dotCompletionContext = getDotCompletionContext();

        // FIXME static/script context...
//        if (!isBehindDot() && context.before1 == null
//                && (location == CaretLocation.INSIDE_CLOSURE || location == CaretLocation.INSIDE_METHOD)) {
            declaringClass = ContextHelper.getSurroundingClassDefinition(this);
            if ( true ) return declaringClass;
//        }

        if (dotCompletionContext == null || dotCompletionContext.getAstPath() == null
                || dotCompletionContext.getAstPath().leaf() == null) {
            return null;
        }

        ClassDefinition declClass = null;

        // experimental type inference
/*        
        MirahTypeAnalyzer typeAnalyzer = new GroovyTypeAnalyzer(doc);
        Set<ClassDefinition> infered = typeAnalyzer.getTypes(dotCompletionContext.getAstPath(),
                dotCompletionContext.getAstOffset());
        
        // FIXME multiple types
        // FIXME is there any test (?)
        if (!infered.isEmpty()) {
            return infered.iterator().next();
        }
*/
        // type inferred
        if (declClass != null) {
            declaringClass = declClass;
            return declaringClass;
        }
/*
        if (dotCompletionContext.getAstPath().leaf() instanceof VariableExpression) {
            VariableExpression variable = (VariableExpression) dotCompletionContext.getAstPath().leaf();
            if ("this".equals(variable.getName())) { // NOI18N
                declaringClass = ContextHelper.getSurroundingClassDefinition(this);
                return declaringClass;
            }
            if ("super".equals(variable.getName())) { // NOI18N
                ClassDefinition thisClass = ContextHelper.getSurroundingClassDefinition(this);
                declaringClass = thisClass.getSuperClass();
                if (declaringClass == null) {
                    return new ClassDefinition("java.lang.Object", ClassDefinition.ACC_PUBLIC, null);
                }
                return declaringClass;
            }
        }
*/

        Node leaf = dotCompletionContext.getAstPath().leaf();
        /*
        if (dotCompletionContext.getAstPath().leaf() instanceof Expression) {
            Expression expression = (Expression) dotCompletionContext.getAstPath().leaf();

            // see http://jira.codehaus.org/browse/GROOVY-3050
            if (expression instanceof RangeExpression
                    && "java.lang.Object".equals(expression.getType().getName())) { // NOI18N
                try {
                    expression.setType(
                            new ClassDefinition(Class.forName("groovy.lang.Range"))); // NOI18N
                } catch (ClassNotFoundException ex) {
                    expression.setType(
                            new ClassDefinition("groovy.lang.Range", ClassDefinition.ACC_PUBLIC | ClassDefinition.ACC_INTERFACE, null)); // NOI18N
                }
            // FIXME report issue
            } else 
            if (expression instanceof ConstantExpression) {
                ConstantExpression constantExpression = (ConstantExpression) expression;
                if (!constantExpression.isNullExpression()) {
                    constantExpression.setType(new ClassDefinition(constantExpression.getValue().getClass()));
                }
            }
            
            declaringClass = expression.getType();
        }
        */
        return declaringClass;
    }

    /**
     * Returns true if the code completion were invoked right after the Spread Java
     * Field operator *.@
     * 
     * @return true if invoked after *.@ operator, false otherwise
     */
    private boolean isAfterSpreadJavaFieldOperator() {
        if (context.before1 != null &&
            context.before2 != null &&
            context.before1.id().is(Tokens.tAt) //&&
//            context.before2.id().equals(MirahTokenId.SPREAD_DOT)
                ) {
            
            return true;
        }
        return false;
    }
    
    /**
     * Returns true if the code completion were invoked right after the Spread operator *.
     * 
     * @return true if invoked after *. operator, false otherwise
     */
    private boolean isAfterSpreadOperator() {
//        if (context.before1 != null && context.before1.id().equals(MirahTokenId.SPREAD_DOT)) {
//            return true;
//        }
        return false;
    }

    /**
     * Check whether this completion request was issued behind an import statement.
     * In such cases we are typically in context of completing packages/types within
     * an import statement. Few examples:
     * <br/><br/>
     * 
     * {@code import java.^}<br/>
     * {@code import java.lan^}<br/>
     * {@code import java.lang.In^}<br/>
     *
     * @return true if we are on the line that starts with an import keyword, false otherwise
     */
    public boolean isBehindImportStatement() {
        int rowStart = 0;
        int nonWhite = 0;

        try {
            rowStart = Utilities.getRowStart(doc, lexOffset);
            nonWhite = Utilities.getFirstNonWhiteFwd(doc, rowStart);

        } catch (BadLocationException ex) {
        }

        Token<MirahTokenId> importToken = LexUtilities.getToken(doc, nonWhite);

        if (importToken != null && importToken.id().is(Tokens.tImport)) {
            return true;
        }

        return false;
    }
}
