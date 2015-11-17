package ru.programpark.mirah.editor.completion.provider;

import java.util.HashMap;
import java.util.Map;
import org.openide.util.Lookup;
import ru.programpark.mirah.editor.completion.context.CompletionContext;
import ru.programpark.mirah.editor.completion.CompletionItem;
import ru.programpark.mirah.editor.completion.FieldSignature;
import ru.programpark.mirah.editor.completion.MethodSignature;

/**
 *
 * @author Petr Hejl
 */
public final class CompletionProviderHandler implements CompletionProvider {

    @Override
    public Map<MethodSignature, CompletionItem> getMethods(CompletionContext context) {
        Map<MethodSignature, CompletionItem> result = new HashMap<>();

        if (context.getSourceFile() != null) {
            for (CompletionProvider provider : Lookup.getDefault().lookupAll(CompletionProvider.class)) {
                for (Map.Entry<MethodSignature, CompletionItem> entry : provider.getMethods(context).entrySet()) {
                    if (entry.getKey().getName().startsWith(context.getPrefix())) {
                        result.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }
        return result;
    }

    @Override
    public Map<FieldSignature, CompletionItem> getFields(CompletionContext context) {
        Map<FieldSignature, CompletionItem> result = new HashMap<>();
        
        if (context.getSourceFile() != null) {
            for (CompletionProvider provider : Lookup.getDefault().lookupAll(CompletionProvider.class)) {
                for (Map.Entry<FieldSignature, CompletionItem> entry : provider.getFields(context).entrySet()) {
                    if (entry.getKey().getName().startsWith(context.getPrefix())) {
                        result.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }
        return result;
    }
    
    @Override
    public Map<MethodSignature, CompletionItem> getStaticMethods(CompletionContext context) {
        Map<MethodSignature, CompletionItem> result = new HashMap<>();
        
        if (context.getSourceFile() != null) {
            for (CompletionProvider provider : Lookup.getDefault().lookupAll(CompletionProvider.class)) {
                for (Map.Entry<MethodSignature, CompletionItem> entry : provider.getStaticMethods(context).entrySet()) {
                    if (entry.getKey().getName().startsWith(context.getPrefix())) {
                        result.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }
        return result;
    }
    
    @Override
    public Map<FieldSignature, CompletionItem> getStaticFields(CompletionContext context) {
        Map<FieldSignature, CompletionItem> result = new HashMap<>();
        
        if (context.getSourceFile() != null) {
            for (CompletionProvider provider : Lookup.getDefault().lookupAll(CompletionProvider.class)) {
                for (Map.Entry<FieldSignature, CompletionItem> entry : provider.getStaticFields(context).entrySet()) {
                    if (entry.getKey().getName().startsWith(context.getPrefix())) {
                        result.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }
        return result;
    }
}
