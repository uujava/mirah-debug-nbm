package ru.programpark.mirah.editor.completion.provider;

import java.util.Map;
import ru.programpark.mirah.editor.completion.CompletionItem;
import ru.programpark.mirah.editor.completion.FieldSignature;
import ru.programpark.mirah.editor.completion.MethodSignature;
import ru.programpark.mirah.editor.completion.context.CompletionContext;

/**
 * Provides additional code completion items for the given {@link CompletionContext}.
 * 
 * @author Petr Hejl
 * @author Martin Janicek
 */
public interface CompletionProvider {

    Map<MethodSignature, CompletionItem> getMethods(CompletionContext context);
    
    Map<MethodSignature, CompletionItem> getStaticMethods(CompletionContext context);

    Map<FieldSignature, CompletionItem> getFields(CompletionContext context);
    
    Map<FieldSignature, CompletionItem> getStaticFields(CompletionContext context);

}
