package ru.programpark.mirah.editor.completion.provider;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import mirah.lang.ast.ClassDefinition;
import mirah.lang.ast.Node;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.modules.csl.spi.ParserResult;
import ru.programpark.mirah.editor.completion.AccessLevel;
import org.netbeans.modules.parsing.spi.indexing.support.QuerySupport;
import org.openide.filesystems.FileObject;
import ru.programpark.mirah.editor.completion.CompletionItem;
import ru.programpark.mirah.editor.completion.FieldSignature;
import ru.programpark.mirah.editor.completion.MethodSignature;
import ru.programpark.mirah.editor.completion.context.CompletionContext;
import ru.programpark.mirah.editor.ast.ASTUtils;
import ru.programpark.mirah.editor.ast.GenericsType;
import ru.programpark.mirah.editor.utils.Utilities;
import ru.programpark.mirah.index.MirahIndex;
import ru.programpark.mirah.index.elements.IndexedClass;

/**
 *
 * @author Petr Hejl
 */
public final class CompleteElementHandler {

    private final CompletionContext context;
    private final ParserResult info;
    private final MirahIndex index;

    
    public CompleteElementHandler(CompletionContext context) {
        this.context = context;
        this.info = context.getParserResult();

        FileObject fo = info.getSnapshot().getSource().getFileObject();
        
//        Collection<FileObject> coll = QuerySupport.findRoots(fo, Collections.singleton(ClassPath.SOURCE), null, null);
//        for( FileObject f1 : coll)
//        {
//            String n = f1.getPath();
//            int t = 0;
//        }
//        Project project = FileOwnerQuery.getOwner(fo);
//        if ( project != null ) coll.add(project.getProjectDirectory());
        
        if (fo != null) {
            // FIXME index is broken when invoked on start
//            this.index = MirahIndex.get(QuerySupport.findRoots(fo, Collections.singleton(ClassPath.SOURCE), null, null));
            this.index = MirahIndex.get(fo);
        } else {
            this.index = null;
        }
    }

    public Map<MethodSignature, CompletionItem> getMethods() {
        final ClassDefinition source = context.getSurroundingClass();
        final ClassDefinition node = context.declaringClass;
        
        if (node == null) {
            return Collections.emptyMap();
        }
        
        Map<MethodSignature, CompletionItem> result = getMethodsInner(
                source, 
                node,
                context.getPrefix(), 
                context.getAnchor(),
                0,
                AccessLevel.create(source, node),
                context.dotContext != null && context.dotContext.isMethodsOnly());

        return result;
    }

    public Map<FieldSignature, CompletionItem> getFields() {
        final ClassDefinition source = context.getSurroundingClass();
        final ClassDefinition node = context.declaringClass;
        
        if (node == null) {
            return Collections.emptyMap();
        }
        
        Map<FieldSignature, CompletionItem> result = getFieldsInner(
                source, 
                node,
                context.getPrefix(), 
                context.getAnchor(),
                0);

        return result;
    }

    // FIXME configure acess levels
    private Map<MethodSignature, CompletionItem> getMethodsInner(
            ClassDefinition source,
            ClassDefinition node,
            String prefix,
            int anchor,
            int level,
            Set<AccessLevel> access,
            boolean nameOnly) {

        boolean leaf = (level == 0);
        Set<AccessLevel> modifiedAccess = AccessLevel.update(access, source, node);

        Map<MethodSignature, CompletionItem> result = new TreeMap<>(new Comparator<MethodSignature>() {

            @Override
            public int compare(MethodSignature method1, MethodSignature method2) {
                // Different method name --> just compare as normal Strings
                if (!method1.getName().equals(method2.getName())) {
                    return method1.getName().compareTo(method2.getName());
                }
                // Method with lower 'parameter count' should be always first
                if (method1.getParameters().length < method2.getParameters().length) {
                    return -1;
                }
                if (method1.getParameters().length > method2.getParameters().length) {
                    return 1;
                }
                // Same number of parameters --> compare param by param as normal Strings
                for (int i = 0; i < method1.getParameters().length; i++) {
                    String param1 = method1.getParameters()[i];
                    String param2 = method2.getParameters()[i];
                    
                    int comparedValue = param1.compareTo(param2);
                    if (comparedValue != 0) {
                        return comparedValue;
                    }
                }
                // This should happened only if there are two absolutely identical methods
                return 0;
            }
        });
        ClassHolder definition = loadDefinition(node);
        
        ClassDefinition typeNode = definition.getNode();
        String typeName = typeNode.name().identifier();
        
        typeName = "java.lang.Object";
        
        // In cases like 1.^ we have current type name "int" but indexer won't find anything for such a primitive
        if ("int".equals(typeName)) { // NOI18N
            typeName = "java.lang.Integer"; // NOI18N
        }
        context.setTypeName(typeName);

        MirahElementsProvider mirahProvider = new MirahElementsProvider();
        fillSuggestions(mirahProvider.getMethods(context), result);
        
        // we can't go groovy and java - helper methods would be visible
        if (result.isEmpty()) {
/*            
            String[] typeParameters = new String[(typeNode.isUsingGenerics() && typeNode.getGenericsTypes() != null)
                    ? typeNode.getGenericsTypes().length : 0];
            for (int i = 0; i < typeParameters.length; i++) {
                GenericsType genType = typeNode.getGenericsTypes()[i];
                if (genType.getUpperBounds() != null) {
                    typeParameters[i] = Utilities.translateClassLoaderTypeName(genType.getUpperBounds()[0].getName());
                } else {
                    typeParameters[i] = Utilities.translateClassLoaderTypeName(genType.getName());
                }
            }
*/
//            fillSuggestions(JavaElementHandler.forCompilationInfo(info)
//                    .getMethods(typeName, prefix, anchor, typeParameters,
//                            leaf, modifiedAccess, nameOnly), result);
        }

        CompletionProviderHandler providerHandler = new CompletionProviderHandler();
        fillSuggestions(providerHandler.getMethods(context), result);
        fillSuggestions(providerHandler.getStaticMethods(context), result);
/*
        if (typeNode.getSuperClass() != null) {
            fillSuggestions(getMethodsInner(source, typeNode.getSuperClass(), prefix, anchor, level + 1, modifiedAccess, nameOnly), result);
        } else if (leaf) {
            fillSuggestions(JavaElementHandler.forCompilationInfo(info).getMethods("java.lang.Object", prefix, anchor, new String[]{}, false, modifiedAccess, nameOnly), result); // NOI18N
        }
*/        
/*
        for (ClassDefinition inter : typeNode.getInterfaces()) {
            fillSuggestions(getMethodsInner(source, inter, prefix, anchor, level + 1, modifiedAccess, nameOnly), result);
        }
*/        
//        fillSuggestions(TransformationHandler.getMethods(index, typeNode, prefix, anchor), result);
        
        return result;
    }

    private Map<FieldSignature, CompletionItem> getFieldsInner(ClassDefinition source, ClassDefinition node, String prefix, int anchor, int level) {
        boolean leaf = (level == 0);

        /* Move this whole block away, context information should be in CompletionContext */
        ClassHolder definition = loadDefinition(node);
        ClassDefinition typeNode = definition.getNode();
        String typeName = typeNode.name().identifier();
        // In cases like 1.^ we have current type name "int" but indexer won't find anything for such a primitive
        if ("int".equals(typeName)) { // NOI18N
            typeName = "java.lang.Integer"; // NOI18N
        }
        context.setTypeName(typeName);
        /**/
        
        Map<FieldSignature, CompletionItem> result = new HashMap<>();

        MirahElementsProvider mirahProvider = new MirahElementsProvider();
        fillSuggestions(mirahProvider.getFields(context), result);
        fillSuggestions(mirahProvider.getStaticFields(context), result);

        fillSuggestions(JavaElementHandler.forCompilationInfo(info).getFields(typeNode.name().identifier(), prefix, anchor, leaf), result);

        CompletionProviderHandler providerHandler = new CompletionProviderHandler();
        fillSuggestions(providerHandler.getFields(context), result);
        fillSuggestions(providerHandler.getStaticFields(context), result);
/*
        if (typeNode.getSuperClass() != null) {
            fillSuggestions(getFieldsInner(source, typeNode.getSuperClass(), prefix, anchor, level + 1), result);
        } else if (leaf) {
            fillSuggestions(JavaElementHandler.forCompilationInfo(info).getFields("java.lang.Object", prefix, anchor, false), result); // NOI18N
        }

        for (ClassDefinition inter : typeNode.getInterfaces()) {
            fillSuggestions(getFieldsInner(source, inter, prefix, anchor, level + 1), result);
        }
        
        fillSuggestions(TransformationHandler.getFields(index, typeNode, prefix, anchor), result);
*/
        return result;
    }

    private ClassHolder loadDefinition(ClassDefinition node) {
        if (index == null) {
            return new ClassHolder(node, null);
        }

        Set<IndexedClass> classes = index.getClasses(node.name().identifier(), QuerySupport.Kind.EXACT);

        if (!classes.isEmpty()) {
            IndexedClass indexed = classes.iterator().next();
            Node astNode = ASTUtils.getForeignNode(indexed);
            if (astNode instanceof ClassDefinition) {
                return new ClassHolder((ClassDefinition) astNode, indexed);
            }
        }

        return new ClassHolder(node, null);
    }

    private static <T> void fillSuggestions(Map<T, ? extends CompletionItem> input, Map<T, ? super CompletionItem> result) {
        for (Map.Entry<T, ? extends CompletionItem> entry : input.entrySet()) {
            if (!result.containsKey(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
    }

    private static class ClassHolder {

        private final ClassDefinition node;

        private final IndexedClass indexed;

        public ClassHolder(ClassDefinition node, IndexedClass indexed) {
            this.node = node;
            this.indexed = indexed;
        }

        public ClassDefinition getNode() {
            return node;
        }

        public FileObject getFileObject() {
            return indexed != null ? indexed.getFileObject() : null;
        }
    }
}
