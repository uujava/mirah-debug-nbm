package ru.programpark.mirah.editor.completion;

import ca.weblite.netbeans.mirah.LOG;
import ca.weblite.netbeans.mirah.lexer.DocumentQuery;
import ca.weblite.netbeans.mirah.lexer.MirahParser;
import ca.weblite.netbeans.mirah.lexer.MirahParser.DocumentDebugger;
import ca.weblite.netbeans.mirah.lexer.MirahTokenId;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import mirah.impl.Tokens;
import mirah.lang.ast.Block;
import mirah.lang.ast.BlockArgument;
import mirah.lang.ast.Call;
import mirah.lang.ast.ClassDefinition;
import mirah.lang.ast.ClosureDefinition;
import mirah.lang.ast.Constant;
import mirah.lang.ast.ConstructorDefinition;
import mirah.lang.ast.FieldAssign;
import mirah.lang.ast.FieldDeclaration;
import mirah.lang.ast.FunctionalCall;
import mirah.lang.ast.Import;
import mirah.lang.ast.InterfaceDeclaration;
import mirah.lang.ast.LocalAccess;
import mirah.lang.ast.LocalAssignment;
import mirah.lang.ast.LocalDeclaration;
import mirah.lang.ast.Loop;
import mirah.lang.ast.MacroDefinition;
import mirah.lang.ast.MethodDefinition;
import mirah.lang.ast.Node;
import mirah.lang.ast.NodeScanner;
import mirah.lang.ast.NodeVisitor;
import mirah.lang.ast.OptionalArgument;
import mirah.lang.ast.Package;
import mirah.lang.ast.Position;
import mirah.lang.ast.RequiredArgument;
import mirah.lang.ast.Script;
import mirah.lang.ast.TypeName;
import org.mirah.typer.ResolvedType;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.editor.completion.Completion;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.Task;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.editor.BaseDocument;
import org.netbeans.modules.csl.api.CompletionProposal;
import org.netbeans.modules.csl.spi.ParserResult;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.modules.parsing.spi.indexing.support.QuerySupport;
import org.netbeans.spi.editor.completion.CompletionResultSet;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import ru.programpark.mirah.editor.api.completion.CaretLocation;
import ru.programpark.mirah.editor.api.completion.CompletionItem;
import ru.programpark.mirah.editor.api.completion.CompletionItem.ConstructorItem;
import ru.programpark.mirah.editor.api.completion.MethodSignature;
import ru.programpark.mirah.editor.api.completion.util.CompletionContext;
import ru.programpark.mirah.editor.api.completion.util.ContextHelper;
import ru.programpark.mirah.editor.ast.AstPath;
import ru.programpark.mirah.editor.completion.provider.CompleteElementHandler;
import ru.programpark.mirah.editor.utils.ImportUtils;
import ru.programpark.mirah.editor.utils.LexUtilities;
import ru.programpark.mirah.editor.utils.MirahUtils;
import ru.programpark.mirah.index.MirahIndex;
import ru.programpark.mirah.index.elements.IndexedClass;
import ru.programpark.mirah.index.elements.IndexedMethod;
import ru.programpark.mirah.index.elements.MethodElement.MethodParameter;

public class MethodCompletion extends BaseCompletion {

    // There attributes should be initiated after each complete() method call
    private List<CompletionProposal> proposals;
    private CompletionContext context;
    private int anchor;


    public MethodCompletion() {
    }


    @Override
    public boolean complete(final List<CompletionProposal> proposals, final CompletionContext context, final int anchor) {
        LOG.log(Level.FINEST, "-> completeMethods"); // NOI18N

        this.proposals = proposals;
        this.context = context;
        this.anchor = anchor;

        getCompletionType(context,context.getAnchor());
        getScope(context);

        if (context == null || context.context == null || context.location == CaretLocation.INSIDE_PARAMETERS) {
            return false;
        }
        
        if (context.dotContext != null && context.dotContext.isFieldsOnly()) {
            return false;
        }

        // check whether we are either:
        //
        // 1.) This is a constructor-call like: String s = new String|
        // 2.) right behind a dot. Then we look for:
        //     2.1  method on collection type: List, Map or Range
        //     2.2  static/instance method on class or object
        //     2.3  Get apropriate groovy-methods from index.
        //     2.4  dynamic, mixin method on Groovy-object like getXbyY()


        // 1.) Test if this is a Constructor-call?
        if (ContextHelper.isConstructorCall(context)) {
            return completeConstructor();
        }

        // 2.2  static/instance method on class or object
        if (!context.isBehindDot() && context.context.before1 != null) {
            return false;
        }

        if (context.declaringClass == null) {
            LOG.log(Level.FINEST, "No declaring class found"); // NOI18N
            return false;
        }

        
        
        /*
            Here we need to figure out, whether we want to complete a variable:

            s.|

            where we want to complete fields and methodes *OR* a package prefix like:

            java.|

            To achive this we only complete methods if there is no basePackage, which is a valid
            package.
         */

        PackageCompletionRequest packageRequest = getPackageRequest(context);

        if (packageRequest.basePackage.length() > 0) {
            ClasspathInfo pathInfo = getClasspathInfoFromRequest(context);

            if (isValidPackage(pathInfo, packageRequest.basePackage)) {
                LOG.log(Level.FINEST, "The string before the dot seems to be a valid package"); // NOI18N
                return false;
            }
        }

        Map<MethodSignature, CompletionItem> result = new CompleteElementHandler(context).getMethods();
        proposals.addAll(result.values());

        analyzeContext(anchor);
        
        return true;
    }

    /**
     * Constructor completion works for following types.
     *  1) Types in the same package
     *  2) Already imported types
     *  3) Groovy default imports
     *
     * @return true if we found some constructor proposal, false otherwise
     */
    private boolean completeConstructor() {
        LOG.log(Level.FINEST, "This looks like a constructor ...");

        // look for all imported types starting with prefix, which have public constructors
        final JavaSource javaSource = getJavaSourceFromRequest();
        if (javaSource != null) {
            try {
                javaSource.runUserActionTask(new Task<CompilationController>() {
                    @Override
                    public void run(CompilationController info) {

                        List<Element> typelist = new ArrayList<>();
                        for (String importName : getAllImports()) {
                            typelist.addAll(getElementListFor(info.getElements(), importName));
                        }
                        LOG.log(Level.FINEST, "Number of types found:  {0}", typelist.size());

                        if (exactConstructorExists(typelist, context.getPrefix())) {
                            // if we are in situation like "String s = new String|" we want to
                            // show only String constructors (not StringBuffer constructors etc.)
                            addExactProposals(typelist);
                        }
                        addConstructorProposalsForDeclaredClasses();
                    }
                }, true);
            } catch (IOException ex) {
                LOG.log(Level.FINEST, "IOException : {0}", ex.getMessage());
            }
        }
//        MirahIndex index = MirahIndex.get(QuerySupport.findRoots(context.getSourceFile(), Collections.singleton(ClassPath.SOURCE), null, null));
        MirahIndex index = MirahIndex.get(context.getSourceFile());

        if (exactClassExists(index)) {
            // Now we know prefix is the exact name of the class/constructor
            String name = context.getPrefix();

            // Explicitely declared constructors
            Set<IndexedMethod> constructors = index.getConstructors(name);
            for (IndexedMethod indexedMethod : constructors) {
                List<MethodParameter> parameters = indexedMethod.getParameters();

                ConstructorItem constructor = new ConstructorItem(name, parameters, anchor, false);
                if (!proposals.contains(constructor)) {
                    proposals.add(constructor);
                }
            }

            // If we didn't find any proposal, it means the instatiate class does not have any constructor
            // explicitely declared - in such case add defaultly generated no-parameter constructor
            if (proposals.isEmpty()) {
                proposals.add(new ConstructorItem(name, Collections.<MethodParameter>emptyList(), anchor, false));
            }
        }

        return !proposals.isEmpty();
    }

    private boolean exactClassExists(MirahIndex index) {
        Set<IndexedClass> classes = index.getClasses(context.getPrefix(), QuerySupport.Kind.PREFIX);
        for (IndexedClass indexedClass : classes) {
            if (indexedClass.getName().equals(context.getPrefix())) {
                return true;
            }
        }
        return false;
    }

    private List<String> getAllImports() {
        List<String> imports = new ArrayList<>();
        imports.addAll(ImportUtils.getDefaultImportPackages());
        imports.addAll(getImportedTypes());
        imports.addAll(getCurrentPackage());
        imports.addAll(getTypesInSameFile());

        return imports;
    }

    private List<String> getImportedTypes() {
        final List<String> importedTypes = new ArrayList<>();

        Script script = ContextHelper.getSurroundingScriptNode(context);
        script.accept(new NodeScanner() {
            @Override
            public boolean enterImport(Import node, Object arg) {
                importedTypes.add(node.fullName().identifier());
                return true;
            }
        }, null );
        
        if (script != null) {
            // this gets the list of full-qualified names of imports.
            /*
            for (Import importNode : script.getImports()) {
                importedTypes.add(importNode.getClassName());
            }

            // this returns a list of String's of wildcard-like included types.
            for (ImportNode wildcardImport : script.getStarImports()) {
                importedTypes.add(wildcardImport.getPackageName());
            }
            */
        }
        return importedTypes;
    }

    private List<String> getCurrentPackage() {
        Script script = ContextHelper.getSurroundingScriptNode(context);
        if (script != null) {
            final String[] packages = new String[1];
            script.accept(new NodeScanner() {
                @Override
                public boolean enterPackage( Package node, Object arg ) {
                    packages[0] = node.name().identifier();
                    return true; //super.enterPackage(node,arg);
                }
            }, null );
            return Collections.singletonList(packages[0]);
//            String packageName = null; //script.getPackageName();
//            if (packageName != null) {
//                packageName = packageName.substring(0, packageName.length() - 1); // Removing last '.' char
//
//                return Collections.singletonList(packageName);
//            }
        }
        return Collections.emptyList();
    }

    private List<String> getTypesInSameFile() {
        List<String> declaredClassNames = new ArrayList<>();
        List<ClassDefinition> declaredClasses = ContextHelper.getDeclaredClasses(context);

        for (ClassDefinition declaredClass : declaredClasses) {
            declaredClassNames.add(declaredClass.name().identifier());
        }
        return declaredClassNames;
    }

    /**
     * Finds out if the given prefix has an exact match to a one of given types.
     *
     * @param typelist list of types for comparison
     * @param prefix prefix we are looking for
     * @return true if there is an exact match, false otherwise
     */
    private boolean exactConstructorExists(List<? extends Element> typelist, String prefix) {
        for (Element element : typelist) {
            if (prefix.toUpperCase().equals(element.getSimpleName().toString().toUpperCase())) {
                return true;
            }
        }
        return false;
    }

    private void addExactProposals(List<? extends Element> typelist) {
        for (Element element : typelist) {
            // only look for classes rather than enums or interfaces
            if (element.getKind() == ElementKind.CLASS) {
                for (Element encl : element.getEnclosedElements()) {
                    if (encl.getKind() == ElementKind.CONSTRUCTOR) {
                        // we gotta get the constructors name from the type itself, since
                        // all the constructors are named <init>.
                        String constructorName = element.getSimpleName().toString();

                        if (constructorName.toUpperCase().equals(context.getPrefix().toUpperCase())) {
                            addConstructorProposal(constructorName, (ExecutableElement) encl);
                        }
                    }
                }
            }
        }
    }

    private void addConstructorProposal(String constructorName, ExecutableElement encl) {
        List<MethodParameter> paramList = getParameterList(encl);
        
        ConstructorItem constructor = new ConstructorItem(constructorName, paramList, anchor, false);
        if (!proposals.contains(constructor)) {
            proposals.add(constructor);
        }
    }

    private void addConstructorProposalsForDeclaredClasses() {
        for (ClassDefinition declaredClass : ContextHelper.getDeclaredClasses(context)) {
            addConstructorProposal(declaredClass);
        }
    }

    private void addConstructorProposal(ClassDefinition classNode) {
        String constructorName = null; //classNode.getNameWithoutPackage();
/*
        if (isPrefixed(context, constructorName)) {
            for (ConstructorNode constructor : classNode.getDeclaredConstructors()) {
                Parameter[] parameters = constructor.getParameters();
                List<MethodParameter> paramList = getParameterListForMethod(parameters);

                proposals.add(new ConstructorItem(constructorName, paramList, anchor, false));
            }
        }
*/        
    }

    private JavaSource getJavaSourceFromRequest() {
        ClasspathInfo pathInfo = getClasspathInfoFromRequest(context);
        assert pathInfo != null;

        JavaSource javaSource = JavaSource.create(pathInfo);

        if (javaSource == null) {
            LOG.log(Level.FINEST, "Problem retrieving JavaSource from ClassPathInfo, exiting.");
            return null;
        }

        return javaSource;
    }

    @NonNull
    private List<? extends Element> getElementListFor(Elements elements, final String importName) {
        if (elements != null && importName != null) {
            PackageElement packageElement = elements.getPackageElement(importName);

            if (packageElement != null) {
                return packageElement.getEnclosedElements();
            } else {
                TypeElement typeElement = elements.getTypeElement(importName);
                if (typeElement != null) {
                    return Collections.singletonList(typeElement);
                }
            }
        }
        return Collections.emptyList();
    }

    /**
     * Get the list of parameters of this executable as a List of <code>ParameterDescriptor</code>'s
     * To be used in insert templates and pretty-printers.
     * 
     * @param exe executable element
     * @return list of <code>ParameterDescriptor</code>'s
     */
    private List<MethodParameter> getParameterList(ExecutableElement exe) {
        List<MethodParameter> paramList = new ArrayList<>();

        if (exe != null) {
            try {
                // generate a list of parameters
                // unfortunately, we have to work around # 139695 in an ugly fashion
                List<? extends VariableElement> params = exe.getParameters(); // this can cause NPE's
                int i = 1;

                for (VariableElement variableElement : params) {
                    TypeMirror tm = variableElement.asType();

                    String fullName = tm.toString();
                    String name = fullName;

                    if (tm.getKind() == javax.lang.model.type.TypeKind.DECLARED) {
                        name = MirahUtils.stripPackage(fullName);
                    }

                    // todo: this needs to be replaced with values from the JavaDoc
                    String varName = "param" + String.valueOf(i);

                    paramList.add(new MethodParameter(fullName, name, varName));

                    i++;
                }
            } catch (NullPointerException e) {
                // simply do nothing.
            }
        }

        return paramList;
    }

    /**
     * Get the parameter-list of this executable as String.
     *
     * @param exe
     * @return
     */
    private String getParameterListForMethod(ExecutableElement exe) {
        StringBuilder sb = new StringBuilder();

        if (exe != null) {
            try {
                // generate a list of parameters
                // unfortunately, we have to work around # 139695 in an ugly fashion
                List<? extends VariableElement> params = exe.getParameters(); // this can cause NPE's

                for (VariableElement variableElement : params) {
                    TypeMirror tm = variableElement.asType();

                    if (sb.length() > 0) {
                        sb.append(", ");
                    }

                    if (tm.getKind() == TypeKind.DECLARED || tm.getKind() == TypeKind.ARRAY) {
                        sb.append(MirahUtils.stripPackage(tm.toString()));
                    } else {
                        sb.append(tm.toString());
                    }
                }
            } catch (NullPointerException e) {
                // simply do nothing.
            }
        }
        return sb.toString();
    }

    /**
     * Convert given parameter array into the list of <code>ParameterDescription</code>'s
     *
     * @param parameters array of parameters
     * @return list of <code>ParameterDescription</code>'s
     */
    /*
    private List<MethodParameter> getParameterListForMethod(Parameter[] parameters) {
        if (parameters.length == 0) {
            return Collections.EMPTY_LIST;
        }

        List<MethodParameter> paramDescriptors = new ArrayList<>();
        
        for (Parameter param : parameters) {
            String fullTypeName = param.getType().getName();
            String typeName = param.getType().getNameWithoutPackage();
            String name = param.getName();

            paramDescriptors.add(new MethodParameter(fullTypeName, typeName, name));
        }
        return paramDescriptors;
    }
    */
// --------------------------------------------------------------------------------------
    
    public void printMethodParameters( MethodDefinition method )
    {
        for( int i = 0 ; i < method.arguments().required().size() ; i++ )
        {
            RequiredArgument a = method.arguments().required().get(i);
           
            String name = a.name().identifier();
            Object c = a.getClass();
            TypeName t = a.type();
            int v = 0;
        }
        for( int i = 0 ; i < method.arguments().optional().size() ; i++ )
        {
            OptionalArgument a = method.arguments().optional().get(i);
            Object c = a.getClass();
            TypeName t = a.type();
            int v = 0;
        }
    }
    
    public Node getScope( CompletionContext context )
    {
        AstPath p = context.path;
        if ( p == null ) return null;
        
        
        Node scope = null;
        for (Iterator<Node> it = p.iterator(); it.hasNext();) {
            Node n = it.next();
            if ( n instanceof ClosureDefinition );
            if ( n instanceof MethodDefinition )
                printMethodParameters((MethodDefinition)n);
            if ( n instanceof ConstructorDefinition );
            if ( n instanceof FieldAssign );
            if ( n instanceof Call );
            if ( n instanceof InterfaceDeclaration );
            if ( n instanceof LocalAssignment );
            if ( n instanceof LocalDeclaration );
            if ( n instanceof Loop );
            if ( n instanceof LocalAccess );
            if ( n instanceof BlockArgument );
            if ( n instanceof FunctionalCall );
            if ( n instanceof FieldDeclaration );
            if ( n instanceof Block );
            if ( n instanceof MacroDefinition );
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
    
    public void getCompletionType( final CompletionContext context, final int anchor)    
    {
        if ( context.getParserResult() == null ) return;
        
        if ( !(context.getParserResult() instanceof MirahParser.NBMirahParserResult)) return;
                
        MirahParser.NBMirahParserResult parserResult = (MirahParser.NBMirahParserResult)context.getParserResult();

        if ( parserResult.getResolvedTypes() != null )
        {
            Collection<ResolvedType> types = parserResult.getResolvedTypes().values();
            for( ResolvedType rt : types )
            {
                String s = rt.name();
                int t = 0;
            }
        }
        
        String prefix = context.getPrefix();
        
        // Documentation says that @NonNull is return from getPrefix() but it's not true
        // Invoking "this.^" makes the return value null
        if (prefix == null) {
            prefix = "";
        }
        
        int lexOffset = context.lexOffset;  
        //int anchor = context.getAnchor(); 
//.getCaretOffset();
//        int astOffset = ASTUtils.getAstOffset(parserResult, lexOffset);
//        int anchor = lexOffset - prefix.length();

//        LOG.log(Level.FINEST, "complete(...), prefix      : {0}", prefix); // NOI18N
//        LOG.log(Level.FINEST, "complete(...), lexOffset   : {0}", lexOffset); // NOI18N
//        LOG.log(Level.FINEST, "complete(...), astOffset   : {0}", astOffset); // NOI18N

        final BaseDocument doc = (BaseDocument)parserResult.getSnapshot().getSource().getDocument(false);
        if (doc == null) {
            return; //CodeCompletionResult.NONE;
        }
//        doc.readLock(); // Read-lock due to Token hierarchy use

        TokenHierarchy<?> hi = TokenHierarchy.get(doc);
        TokenSequence<MirahTokenId> ts = hi.tokenSequence(MirahTokenId.getLanguage());
//        TokenSequence<MirahTokenId> ts = LexUtilities.getMirahTokenSequence(doc,0); // lexOffset);
        ts.moveStart();
        ts.move(0);
        Token<MirahTokenId> t = ts.token();
        int count = ts.tokenCount();
        
        ts.moveIndex(5);
        t = ts.token();
        
        ts.moveNext();
        t = ts.token();
        
        
        ts.move(lexOffset);
        Token<MirahTokenId> t2 = ts.token();
        int index = ts.index();
        boolean valid = ts.isValid();
        boolean empty = ts.isEmpty();
        
        AstPath p = context.path;

//        String lastChar = doc.getText(p, 1);

        // Will hold the "dot" token that we want to do code
        // completion for
        Token<MirahTokenId> dotToken = null;

        // Will hold the subject token (i.e. the last token before 
        // the dot that we will be checking for a type
        Token<MirahTokenId> subjectToken = null;
        Token<MirahTokenId> thisTok = ts.token();

//        LOG.info(this,"query subjectToken=" + subjectToken);
//        LOG.info(this,"query thisTok=" + thisTok);

        if ( thisTok == null ) 
        {
            ts.moveNext();
            thisTok = ts.token();
        }
        
        String filter = "";
        
        if ( thisTok != null ){
            MirahTokenId thisTokType = thisTok.id();

            Token<MirahTokenId> prevTok = null;
            MirahTokenId prevTokType = null;
            if ( ts.movePrevious() ){
                prevTok = ts.token();
                prevTokType = prevTok.id();
                ts.moveNext();
            }

            if ( thisTokType.is(Tokens.tIDENTIFIER) && prevTokType.is(Tokens.tDot) ){

                filter = ts.token().text().toString();
                dotToken = prevTok;
                ts.movePrevious();

            } else if (thisTokType.is(Tokens.tDot)){
                filter = "";
                dotToken = thisTok;

            }

        }
//      LOG.info(this,"query dotToken=" + dotToken);

        if ( dotToken == null ){
//            doc.readUnlock();
            return;
        }

        // Now to find the subject token.
        while ( ts.movePrevious() ){
            Token<MirahTokenId> tok = ts.token();
            if ( tok.id().is(Tokens.tWhitespace) || tok.id().is(Tokens.tNL) ){
            } else {
                subjectToken = tok;
                break;
            }
        }

        if ( dotToken == null || subjectToken == null ){
//            doc.readUnlock();
           return;
        }

//        doc.readLock();

//        TokenHierarchy<?> hi = TokenHierarchy.get(doc);
//        doc.readUnlock();

        
        Node foundNode =findNode(parserResult,subjectToken.offset(hi)+subjectToken.length());
        ResolvedType type = parserResult.getResolvedType(foundNode);

//        doc.readUnlock();

        
/*        
//        int bol = MirahUtils.getBeginningOfLine(doc, lexOffset);
//        int eol = MirahUtils.getEndOfLine(doc, lexOffset);
        int dotPos = dotToken.offset(hi);

        DocumentDebugger dbg = MirahParser.getDocumentDebugger(doc);


        Node foundNode = MirahUtils.findNode(dbg, subjectToken.offset(hi)+subjectToken.length());

        ResolvedType type = null;
        if ( foundNode != null ){
            type = dbg.getType(foundNode);

            if ( type == null ){
                DocumentQuery dq = new DocumentQuery(doc);
                TokenSequence<MirahTokenId> seq = dq.getTokens(foundNode.position().endChar(), true);
                String typeName = dq.guessType(seq, file);
                //System.out.println("Type name guessed to be "+typeName);
            }
        }

        if ( foundNode == null || type == null ){
            Source src = Source.create(doc);
            MirahParser parser = new MirahParser();
            try {
                Snapshot snapshot = src.createSnapshot();
                String text = snapshot.getText().toString();
                StringBuilder sb = new StringBuilder();
                sb.append(text.substring(0, dotPos));
//                for ( int i=dotPos; i<eol; i++){
//                    sb.append(' ');
//                }
//                sb.append(text.substring(eol));
//                LOG.info(this,"query reparse=" + sb.toString());

                parser.reparse(snapshot, sb.toString());

            } catch (ParseException ex){
                Exceptions.printStackTrace(ex);
            }

            //printNodes(dbg.compiler.compiler(), rightEdgeFinal);
            foundNode = MirahUtils.findNode(dbg, subjectToken.offset(hi)+subjectToken.length());
            if ( foundNode != null ){
                type = dbg.getType(foundNode);
            }
        }
//        LOG.info(this,"query foundNode3=" + foundNode);

        Class currentType = null;
        if ( foundNode != null ){

            type = dbg.getType(foundNode);
//            LOG.info(this,"query type=" + type);

*/        
            if ( type != null ){
                FileObject fileObject = NbEditorUtilities.getFileObject(doc);
                Class cls = MirahUtils.findClass(fileObject, type.name());
//                currentType = cls;
//                LOG.info(this,"query cls=" + cls);

                boolean isStatic = foundNode instanceof Constant;
                if ( cls != null ){
                    if ( isStatic && filter == null || "new".startsWith(filter)){
                        for ( Constructor c : cls.getConstructors()){
//                            LOG.info(this,"query Constructor=" + c);
//                            crs.addItem(new MirahConstructorCompletionItem(c, caretOffset-filter.length(), filter.length()));
                        }
                    }
                    for ( Method m : cls.getMethods()){
//                        LOG.info(this,"query Method=" + m);
                        int y = 0;
//                        if ( m.getName().startsWith(filter) && isStatic == Modifier.isStatic(m.getModifiers())){
//                            crs.addItem(new MirahMethodCompletionItem(fileObject, m, caretOffset-filter.length(), filter.length(), cls));
//                        }
                    }
                }
            }
//        } 
    }    

    public static Node findNode(final MirahParser.NBMirahParserResult parserResult, final int rightEdge){

	if ( parserResult.getParsedNodes() == null ) return null;

        final Node[] foundNode = new Node[1];
//        parserResult.getRoot().accept(new NodeScanner(){
        for( Object node : parserResult.getParsedNodes() ){
            if ( node instanceof Node ){
                ((Node)node).accept(new NodeScanner(){
                    @Override
                    public boolean enterDefault(Node node, Object arg) {
                        if ( node != null ){
                            Position nodePos = node.position();
                            ResolvedType type = parserResult.getResolvedType(node);
                            if ( type != null && nodePos != null && nodePos.endChar() == rightEdge ){
                                foundNode[0] = node;
                            } else if ( nodePos != null && nodePos.endChar() == rightEdge ){
                            } else {

                            }
                        } 
                        return super.enterDefault(node, arg); //To change body of generated methods, choose Tools | Templates.
                    }
                }, null);
            }
        }
        return foundNode[0];
    }
    
    private void addClassProposals( Class cls, String filter, int offset, boolean isStatic )
    {
        if ( cls == null ) return;
        
//        Modifier modifier = new Modifier();
        
        if ( isStatic && filter == null || "new".startsWith(filter)){
            for ( Constructor c : cls.getConstructors()){
                ArrayList<MethodParameter> parameters = new ArrayList<MethodParameter>();
                Class<?>[] pars = c.getParameterTypes();
                for( int i = 0 ; i < pars.length ; i++ )
                    parameters.add(new MethodParameter(pars[i].getCanonicalName(),pars[i].getSimpleName()));
                proposals.add(new ConstructorItem("new", parameters, anchor, false));
            }
        }
        for ( Method m : cls.getMethods())
        if ( m.getName().startsWith(filter) && isStatic == java.lang.reflect.Modifier.isStatic(m.getModifiers()))
        {
            Class<?>[] pars = m.getParameterTypes();
//            String [] parameters = new String[pars.length];                                    
            List<String> parameters = new ArrayList<String>();
            for( int i = 0 ; i < pars.length ; i++ )
            {
//                parameters[i] = pars[i].getSimpleName();
                parameters.add(pars[i].getSimpleName());
            }
            int mody = m.getModifiers();
            Set<javax.lang.model.element.Modifier> modifiers = new HashSet<javax.lang.model.element.Modifier>();
            if ( java.lang.reflect.Modifier.isStatic(mody) ) modifiers.add(javax.lang.model.element.Modifier.STATIC);
            if ( java.lang.reflect.Modifier.isProtected(mody) ) modifiers.add(javax.lang.model.element.Modifier.PROTECTED);
            if ( java.lang.reflect.Modifier.isPrivate(mody) ) modifiers.add(javax.lang.model.element.Modifier.PRIVATE);
            if ( java.lang.reflect.Modifier.isPublic(mody) ) modifiers.add(javax.lang.model.element.Modifier.PUBLIC);
            if ( java.lang.reflect.Modifier.isAbstract(mody) ) modifiers.add(javax.lang.model.element.Modifier.ABSTRACT);
            proposals.add(CompletionItem.forJavaMethod(cls.getCanonicalName(),m.getName(),parameters, m.getReturnType().getName(), modifiers, offset, true, false));
//          proposals.add(CompletionItem.forDynamicMethod(offset, m.getName(), parameters, m.getReturnType().getName(), isStatic));
        }
        for ( Field f : cls.getFields())
        {
            proposals.add(CompletionItem.forDynamicField(offset, f.getName(), f.getGenericType().toString()));
        }

    }
    protected void analyzeContext(final int initialOffset) 
    {
        BaseDocument doc = (BaseDocument)context.doc;
        int caretOffset = context.astOffset;
        FileObject fileObject = context.getSourceFile();
        if ( caretOffset < initialOffset ) return;

        MirahParser.NBMirahParserResult parserResult = (MirahParser.NBMirahParserResult)context.getParserResult();

        try
        {
            doc.readLock();

            MirahParser.DocumentDebugger dbg = MirahParser.getDocumentDebugger(doc);
            if ( dbg == null ) return;

            int p = caretOffset-1;
            if ( p < 0 ) return;
            TokenSequence<MirahTokenId> toks = MirahUtils.mirahTokenSequence(doc, caretOffset, true);

//                ts.move(caretOffset == 0 ? 0 : caretOffset - 1);
                
            // Will hold the "dot" token that we want to do code
            // completion for
            Token<MirahTokenId> dotToken = null;

//                toks.moveNext();
            toks.move(caretOffset);
                
//            int index1 = toks.index();
                
                
            // Will hold the subject token (i.e. the last token before 
            // the dot that we will be checking for a type
            Token<MirahTokenId> subjectToken = null;
            Token<MirahTokenId> thisTok = toks.token();

//                String text = thisTok == null ? null : thisTok.id().name();
//                String text2 = thisTok == null ? null : thisTok.text().toString();
                
            if ( thisTok == null ) 
            {
                toks.movePrevious();
                thisTok = toks.token();
            }
            String filter = null;
            if ( thisTok != null ){
                MirahTokenId thisTokType = thisTok.id();
                Token<MirahTokenId> prevTok = null;
                MirahTokenId prevTokType = null;
                if ( toks.movePrevious() ){
                    prevTok = toks.token();
                    prevTokType = prevTok.id();
//                        text = prevTok == null ? null : prevTok.id().name();
//                        text2 = prevTok == null ? null : prevTok.text().toString();
                    toks.moveNext();
                }
                if ( thisTokType.is(Tokens.tIDENTIFIER) && prevTokType.is(Tokens.tDot) )
                {
                    filter = toks.token().text().toString();
                    dotToken = prevTok;
                    toks.movePrevious();
                } 
                else if ( thisTokType.is(Tokens.tDot) ){
                    filter = "";
                    dotToken = thisTok;
                }
            }
            if ( dotToken == null ) return;

            // Now to find the subject token.
            while ( toks.movePrevious() ){
                Token<MirahTokenId> tok = toks.token();
                if ( tok.id().is(Tokens.tWhitespace) || tok.id().is(Tokens.tNL) ){
                    // 
                } else {
                    subjectToken = tok;
                    break;
                }
            }
            if ( dotToken == null || subjectToken == null ) return;

            TokenHierarchy<?> hi = TokenHierarchy.get(doc);

            int dotPos = dotToken.offset(hi);
            int rightEdge = subjectToken.offset(hi)+subjectToken.length();
//                Node foundNode = MirahUtils.findNode(dbg, rightEdge);
            Node foundNode = findNode(parserResult, rightEdge);
            ResolvedType type = null;
            if ( foundNode != null ){
//                    type = dbg.getType(foundNode);
                type = parserResult.getResolvedType(foundNode);
                if ( type == null ){
                    DocumentQuery dq = new DocumentQuery(doc);
                    TokenSequence<MirahTokenId> seq = dq.getTokens(foundNode.position().endChar(), true);
                    String typeName = dq.guessType(seq, fileObject);
                    //System.out.println("Type name guessed to be "+typeName);
                }
            }
/*                
                if ( foundNode == null || type == null ){
                    
                    int bol = MirahUtils.getBeginningOfLine(doc, caretOffset);
                    int eol = MirahUtils.getEndOfLine(doc, caretOffset);
                
                    Source src = Source.create(doc);
                    MirahParser parser = new MirahParser();
                    try {
                        Snapshot snapshot = src.createSnapshot();
                        text = snapshot.getText().toString();
                        StringBuilder sb = new StringBuilder();
                        sb.append(text.substring(0, dotPos));
                        for ( int i=dotPos; i<eol; i++){
                            sb.append(' ');
                        }
                        sb.append(text.substring(eol));
//                        LOG.info(this,"query reparse=" + sb.toString());
                        parser.reparse(snapshot, sb.toString());

                    } catch (ParseException ex){
                        Exceptions.printStackTrace(ex);
                    }
                    dbg = MirahParser.getDocumentDebugger(doc);
                    //printNodes(dbg.compiler.compiler(), rightEdgeFinal);
//                    foundNode = MirahUtils.findNode(dbg, subjectToken.offset(hi)+subjectToken.length());
                    foundNode = findNode(parserResult, subjectToken.offset(hi)+subjectToken.length());
                    if ( foundNode != null ){
                        type = dbg.getType(foundNode);
                    }
                }
*/        
            if ( foundNode != null ){

//                    type = dbg.getType(foundNode);
//                    type = parserResult.getResolvedType(foundNode);
                if ( type != null ){
//                        FileObject fileObject = NbEditorUtilities.getFileObject(doc);
                    Class cls = MirahUtils.findClass(fileObject, dbg.getType(foundNode).name());
                    addClassProposals(cls,filter,initialOffset,foundNode instanceof Constant);
                }
            } 
//            } 
//            catch ( BadLocationException ble ){
//                ble.printStackTrace();
//            }
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
        finally {
            doc.readUnlock();
        }
    }

}
