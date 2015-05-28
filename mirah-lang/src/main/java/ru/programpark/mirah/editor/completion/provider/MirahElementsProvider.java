package ru.programpark.mirah.editor.completion.provider;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.Modifier;
import org.netbeans.api.java.classpath.ClassPath;
import ru.programpark.mirah.editor.completion.AccessLevel;
import org.netbeans.modules.parsing.spi.indexing.support.QuerySupport;
import org.openide.filesystems.FileObject;
import ru.programpark.mirah.editor.api.completion.FieldSignature;
import ru.programpark.mirah.editor.api.completion.util.CompletionContext;
import ru.programpark.mirah.editor.api.completion.CompletionItem;
import ru.programpark.mirah.editor.api.completion.MethodSignature;
import ru.programpark.mirah.editor.spi.completion.CompletionProvider;
import ru.programpark.mirah.editor.utils.Utilities;
import ru.programpark.mirah.index.MirahIndex;
import ru.programpark.mirah.index.elements.IndexedElement;
import ru.programpark.mirah.index.elements.IndexedField;
import ru.programpark.mirah.index.elements.IndexedMethod;

/**
 *
 * @author Petr Hejl
 * @author Martin Janicek
 */
public final class MirahElementsProvider implements CompletionProvider {

//    @Override
    public Map<MethodSignature, CompletionItem> getMethods(CompletionContext context) {
        final MirahIndex index = getIndex(context);
        final Map<MethodSignature, CompletionItem> result = new HashMap<>();
        
        if (index != null) {
            Set<IndexedMethod> methods;

            if ("".equals(context.getPrefix())) { // NOI18N
                methods = index.getMethods(".*", context.getTypeName(), QuerySupport.Kind.REGEXP); // NOI18N
            } else {
                methods = index.getMethods(context.getPrefix(), context.getTypeName(), QuerySupport.Kind.PREFIX);
            }

            for (IndexedMethod indexedMethod : methods) {
                if (accept(context.access, indexedMethod)) {
                    result.put(getMethodSignature(indexedMethod), CompletionItem.forJavaMethod(
                            context.getTypeName(),
                            indexedMethod.getName(),
                            indexedMethod.getParameterTypes(),
                            indexedMethod.getReturnType(),
                            Utilities.gsfModifiersToModel(indexedMethod.getModifiers(), Modifier.PUBLIC),
                            context.getAnchor(),
                            false,
                            context.isNameOnly()));
                }
            }
        }

        return result;
    }

    @Override
    public Map<MethodSignature, CompletionItem> getStaticMethods(CompletionContext context) {
        return Collections.emptyMap();
    }

    @Override
    public Map<FieldSignature, CompletionItem> getFields(CompletionContext context) {
        final MirahIndex index = getIndex(context);
        final Map<FieldSignature, CompletionItem> result = new HashMap<>();
        
        if (index != null) {
            Set<IndexedField> fields;

            if ("".equals(context.getPrefix())) { // NOI18N
                fields = index.getAllFields(context.getTypeName());
            } else {
                fields = index.getFields(context.getPrefix(), context.getTypeName(), QuerySupport.Kind.PREFIX);
            }

            for (IndexedField indexedField : fields) {
                result.put(getFieldSignature(indexedField), new CompletionItem.FieldItem(
                        indexedField.getTypeName(),
                        indexedField.getName(),
                        indexedField.getModifiers(),
                        context.getAnchor()));
            }
        }

        return result;
    }

    @Override
    public Map<FieldSignature, CompletionItem> getStaticFields(CompletionContext context) {
        return Collections.emptyMap();
    }
    
    private MirahIndex getIndex(CompletionContext context) {
        final FileObject fo = context.getSourceFile();
        
        if (fo != null) {
//            return MirahIndex.get(QuerySupport.findRoots(fo, Collections.singleton(ClassPath.SOURCE), null, null));
            return MirahIndex.get(fo);
        }
        return null;
    }

    private MethodSignature getMethodSignature(IndexedMethod method) {
        String[] parameters = method.getParameterTypes().toArray(new String[method.getParameterTypes().size()]);
        return new MethodSignature(method.getName(), parameters);
    }

    private FieldSignature getFieldSignature(IndexedField field) {
        return new FieldSignature(field.getName());
    }

    private boolean accept(Set<AccessLevel> levels, IndexedElement element) {
        for (AccessLevel accessLevel : levels) {
            if (accessLevel.accept(element.getModifiers())) {
                return true;
            }
        }

        return false;
    }
}
