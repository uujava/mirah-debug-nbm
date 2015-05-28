package ru.programpark.mirah.editor.spi.completion;

import java.util.Map;
import ru.programpark.mirah.editor.api.completion.CompletionItem;
import ru.programpark.mirah.editor.api.completion.FieldSignature;
import ru.programpark.mirah.editor.api.completion.MethodSignature;
import ru.programpark.mirah.editor.api.completion.util.CompletionContext;

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
