package ru.programpark.mirah.editor.completion;

import ca.weblite.netbeans.mirah.cc.AstSupport;
import ca.weblite.netbeans.mirah.lexer.MirahTokenId;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import mirah.impl.Tokens;
import mirah.lang.ast.ClassDefinition;
import mirah.lang.ast.Import;
import mirah.lang.ast.NodeScanner;
import mirah.lang.ast.Script;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.Task;
import org.netbeans.api.lexer.Token;
import org.netbeans.modules.csl.api.CompletionProposal;
import ru.programpark.mirah.editor.completion.util.CamelCaseUtil;
import org.netbeans.modules.parsing.spi.indexing.support.QuerySupport;
import org.openide.filesystems.FileObject;
import ru.programpark.mirah.editor.api.completion.CompletionItem;
import ru.programpark.mirah.editor.api.completion.util.CompletionContext;
import ru.programpark.mirah.editor.api.completion.util.ContextHelper;
import ru.programpark.mirah.editor.utils.ImportUtils;
import ru.programpark.mirah.editor.utils.MirahUtils;
import ru.programpark.mirah.index.MirahIndex;
import ru.programpark.mirah.index.elements.IndexedClass;

/**
 * Complete the Groovy and Java types available at this position.
 * 
 * This could be either:
 * 1.) Completing all available Types in a given package. This is used for:
 * 1.1) import statements completion
 * 1.2) If you simply want to give the fq-name for something.
 *
 * 2.) Complete the types which are available without having to give a fqn:
 * 2.1.) Types defined in the Groovy File where the completion is invoked. (INDEX)
 * 2.2.) Types located in the same package (source or binary). (INDEX)
 * 2.3.) Types manually imported via the "import" statement. (AST)
 * 2.4.) The Default imports for Groovy, which are a super-set of Java. (NB JavaSource)
 *
 * These are the Groovy default imports:
 *
 * java.io.*
 * java.lang.*
 * java.math.BigDecimal
 * java.math.BigInteger
 * java.net.*
 * java.util.*
 * groovy.lang.*
 * groovy.util.*
 *
 * @author Martin Janicek
 */
public class TypesCompletion extends BaseCompletion {

    // There attributes should be initiated for each complete() method call
    private List<CompletionProposal> proposals;
    private CompletionContext request;
    private int anchor;
    private boolean constructorCompletion;

    
    @Override
    public boolean complete(List<CompletionProposal> proposals, CompletionContext request, int anchor) {
        LOG.log(Level.FINEST, "-> completeTypes"); // NOI18N

        this.proposals = proposals;
        this.request = request;
        this.anchor = anchor;

        if (request.dotContext != null) {
            if (request.dotContext.isFieldsOnly() || request.dotContext.isMethodsOnly()) {
                return false;
            }
        }
        
        final PackageCompletionRequest packageRequest = getPackageRequest(request);

        // todo: we don't handle single dots in the source. In that case we should
        // find the class we are living in. Disable it for now.

        if (packageRequest.basePackage.length() == 0
                && packageRequest.prefix.length() == 0
                && packageRequest.fullString.equals(".")) {
            return false;
        }

        // check for a constructor call
        if (ContextHelper.isConstructorCall(request)) {
            constructorCompletion = true;
        } else {
            constructorCompletion = false;
        }

        // are we dealing with a class xyz implements | {
        // kind of completion?

        boolean onlyInterfaces = false;

        Token<? extends MirahTokenId> literal = request.context.beforeLiteral;
        if (literal != null) {
            
            // We don't need to complete Types after class definition
            if (literal.id().is(Tokens.tClass)) {
                return false;
            }

            if (literal.id().is(Tokens.tImplements)) {
                LOG.log(Level.FINEST, "Completing only interfaces after implements keyword.");
                onlyInterfaces = true;
            }
        }


        Set<TypeHolder> addedTypes = new HashSet<>();

        // This Script is used to retrieve the types defined here and the package name.
        Script script = ContextHelper.getSurroundingScriptNode(request);
        String currentPackage = getCurrentPackageName(script);
        JavaSource javaSource = getJavaSourceFromRequest();

        MirahIndex index = null;
        FileObject fo = request.getSourceFile();
        if (fo != null) {
//            index = MirahIndex.get(QuerySupport.findRoots(fo,
//                    Collections.singleton(ClassPath.SOURCE),
//                    Collections.<String>emptyList(),
//                    Collections.<String>emptyList()));
            index = MirahIndex.get(fo);
        }

        // if we are dealing with a basepackage we simply complete all the packages given in the basePackage
        if (packageRequest.basePackage.length() > 0 || request.isBehindImportStatement()) {
            List<TypeHolder> typeList = getTypeHoldersForPackage(javaSource, packageRequest.basePackage, currentPackage);

            LOG.log(Level.FINEST, "Number of types found:  {0}", typeList.size());

            for (TypeHolder singleType : typeList) {
                addToProposalUsingFilter(addedTypes, singleType, onlyInterfaces);
            }

            if (index != null) {
                Set<IndexedClass> classes = index.getClassesFromPackage(packageRequest.basePackage);
                for (IndexedClass indexedClass : classes) {
                    addToProposalUsingFilter(addedTypes, new TypeHolder(indexedClass), onlyInterfaces);
                }
            }

            return true;
        }

        // dont want types for objectExpression.something
        if (request.isBehindDot()) {
            return false;
        }

        // Retrieve the package we are living in from AST and then
        // all classes from that package using the Groovy Index.

        if (script != null) {
            LOG.log(Level.FINEST, "We are living in package : {0} ", currentPackage);

            if (index != null) {
                String camelCaseFirstWord = CamelCaseUtil.getCamelCaseFirstWord(request.getPrefix());
                Set<IndexedClass> classes = index.getClasses(camelCaseFirstWord, QuerySupport.Kind.PREFIX);

                if (!classes.isEmpty()) {
                    for (IndexedClass indexedClass : classes) {
                        addToProposalUsingFilter(addedTypes, new TypeHolder(indexedClass), onlyInterfaces);
                    }
                }
            }
        }

        List<String> localDefaultImports = new ArrayList<>();

        // Are there any manually imported types?

        if (script != null) {

            List<String> imports = AstSupport.collectImports(script);
            for( String imp : imports )
            {
                int last = imp.lastIndexOf('.');
                String packageName = last == -1 ? imp : imp.substring(0,last);
                localDefaultImports.add(packageName);
            }

/*
            // this gets the list of full-qualified names of imports.
            List<Import> imports = null; //script.getImports();

            if (imports != null) {
                for (Import importNode : imports) {
                    ElementKind ek;
//                    if (importNode.getType().isInterface()) {
//                        ek = ElementKind.INTERFACE;
//                    } else {
//                        ek = ElementKind.CLASS;
//                    }

//                    addToProposalUsingFilter(addedTypes, new TypeHolder(importNode.getClassName(), ek), onlyInterfaces);
                }
            }

            // this returns a list of String's of wildcard-like included types.
            List<Import> importNodes = null; //script.getStarImports();

            for (Import wildcardImport : importNodes) {
                String packageName = null; //wildcardImport.getPackageName();
                if (packageName.endsWith(".")) {
                    packageName = packageName.substring(0, packageName.length() - 1);
                }
                localDefaultImports.add(packageName);
            }
*/
        }


        // Now we compute the type-proposals for the default imports.
        // First, create a list of default JDK packages. These are reused,
        // so they are defined elsewhere.

        localDefaultImports.addAll(ImportUtils.getDefaultImportPackages());

        // adding types from default import, optionally filtered by
        // prefix

        for (String singlePackage : localDefaultImports) {
            List<TypeHolder> typeList = getTypeHoldersForPackage(javaSource, singlePackage, currentPackage);

            LOG.log(Level.FINEST, "Number of types found:  {0}", typeList.size());

            for (TypeHolder element : typeList) {
                addToProposalUsingFilter(addedTypes, element, onlyInterfaces);
            }
        }

        // Adding single classes
        for (String className : ImportUtils.getDefaultImportClasses()) {
            addToProposalUsingFilter(addedTypes, new TypeHolder(className, ElementKind.CLASS), onlyInterfaces);
        }

        // Adding declared classes
        for (ClassDefinition declaredClass : ContextHelper.getDeclaredClasses(request)) {
            addToProposalUsingFilter(addedTypes, new TypeHolder(declaredClass.name().identifier(), ElementKind.CLASS), onlyInterfaces);
        }

        return true;
    }

    private String getCurrentPackageName(Script script) {
        if (script != null) {
            final String packages[] = new String[1];
               
            script.accept(new NodeScanner(){
               @Override
               public boolean enterPackage(mirah.lang.ast.Package node, Object arg) {
                   packages[0] = node.name().identifier();
                   return super.enterPackage(node, arg); //To change body of generated methods, choose Tools | Templates.
               }
            },null);
            return packages[0];
        } else {
            ClassDefinition node = ContextHelper.getSurroundingClassDefinition(request);
            if (node != null) {
                return null; //node.getPackageName();
            }
        }
        return "";
    }

    private JavaSource getJavaSourceFromRequest() {
        ClasspathInfo pathInfo = getClasspathInfoFromRequest(request);
        assert pathInfo != null;

        JavaSource javaSource = JavaSource.create(pathInfo);
        if (javaSource == null) {
            LOG.log(Level.FINEST, "Problem retrieving JavaSource from ClassPathInfo, exiting.");
            return null;
        }
        return javaSource;
    }

    /**
     * Adds the type given in fqn with its simple name to the proposals, filtered by
     * the prefix and the package name.
     * 
     * @param alreadyPresent already presented proposals
     * @param type type we want to add into proposals
     * @param onlyInterfaces true, if we are dealing with only interfaces completion
     */
    private void addToProposalUsingFilter(Set<TypeHolder> alreadyPresent, TypeHolder type, boolean onlyInterfaces) {
        if ((onlyInterfaces && (type.getKind() != ElementKind.INTERFACE)) || alreadyPresent.contains(type)) {
            return;
        }

        String fqnTypeName = type.getName();
        String typeName = MirahUtils.stripPackage(fqnTypeName);

        // If we are in situation: "String s = new String|" we don't want to show
        // String type as a option - we want to show String constructors + types
        // prefixed with String (e.g. StringBuffer)
        if (constructorCompletion && typeName.toUpperCase().equals(request.getPrefix().toUpperCase())) {
            return;
        }

        // We are dealing with prefix for some class type
        if (isPrefixed(request, typeName)) {
            alreadyPresent.add(type);
            proposals.add(new CompletionItem.TypeItem(fqnTypeName, typeName, anchor, type.getKind()));
        }

        // We are dealing with CamelCase completion for some class type
        if (CamelCaseUtil.compareCamelCase(typeName, request.getPrefix())) {
            CompletionItem.TypeItem camelCaseProposal = new CompletionItem.TypeItem(fqnTypeName, typeName, anchor, ElementKind.CLASS);
            
            if (!proposals.contains(camelCaseProposal)) {
                proposals.add(camelCaseProposal);
            }
        }
    }

    @NonNull
    private List<TypeHolder> getTypeHoldersForPackage(final JavaSource javaSource, final String pkg, final String currentPackage) {
        LOG.log(Level.FINEST, "getElementListForPackageAsString(), Package :  {0}", pkg);

        final List<TypeHolder> result = new ArrayList<>();

        if (javaSource != null) {

            try {
                javaSource.runUserActionTask(new Task<CompilationController>() {
                    @Override
                    public void run(CompilationController info) {
                        Elements elements = info.getElements();

                        addPackageElements(elements.getPackageElement(pkg));
                        addTypeElements(elements.getTypeElement(pkg));
                    }

                    private void addPackageElements(PackageElement packageElement) {
                        if (packageElement != null) {
                            List<? extends Element> typelist = packageElement.getEnclosedElements();
                            boolean samePackage = pkg.equals(currentPackage);

                            for (Element element : typelist) {
                                Set<Modifier> modifiers = element.getModifiers();
                                if (modifiers.contains(Modifier.PUBLIC)
                                    || samePackage && (modifiers.contains(Modifier.PROTECTED)
                                    || (!modifiers.contains(Modifier.PUBLIC) && !modifiers.contains(Modifier.PRIVATE)))) {

                                    result.add(new TypeHolder(element.toString(), element.getKind()));
                                }
                            }
                        }
                    }

                    private void addTypeElements(TypeElement typeElement) {
                        if (typeElement != null) {
                            List<? extends Element> typelist = typeElement.getEnclosedElements();
                            boolean samePackage = pkg.equals(currentPackage);

                            for (Element element : typelist) {
                                Set<Modifier> modifiers = element.getModifiers();
                                if (modifiers.contains(Modifier.PUBLIC)
                                    || samePackage && (modifiers.contains(Modifier.PROTECTED)
                                    || (!modifiers.contains(Modifier.PUBLIC) && !modifiers.contains(Modifier.PRIVATE)))) {

                                    result.add(new TypeHolder(element.toString(), element.getKind()));
                                }
                            }
                        }

                    }
                }, true);
            } catch (IOException ex) {
                LOG.log(Level.FINEST, "IOException : {0}", ex.getMessage());
            }
        }
        return result;
    }

    private static class TypeHolder {

        private final String name;
        private final ElementKind kind;

        public TypeHolder(IndexedClass indexedClass) {
            this.name = indexedClass.getFqn();

            if (indexedClass.getKind() == org.netbeans.modules.csl.api.ElementKind.CLASS) {
                this.kind = ElementKind.CLASS;
            } else {
                this.kind = ElementKind.INTERFACE;
            }
        }
        
        public TypeHolder(String name, ElementKind kind) {
            this.name = name;
            this.kind = kind;
        }

        public ElementKind getKind() {
            return kind;
        }

        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final TypeHolder other = (TypeHolder) obj;
            if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
                return false;
            }
            if (this.kind != other.kind) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 59 * hash + (this.name != null ? this.name.hashCode() : 0);
            hash = 59 * hash + (this.kind != null ? this.kind.hashCode() : 0);
            return hash;
        }
    }
}
