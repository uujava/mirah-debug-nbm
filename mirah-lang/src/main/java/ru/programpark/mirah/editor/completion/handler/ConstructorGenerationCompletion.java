package ru.programpark.mirah.editor.completion.handler;

import ru.programpark.mirah.editor.completion.context.CompletionContext;
import ru.programpark.mirah.editor.completion.context.CaretLocation;
import ru.programpark.mirah.editor.completion.context.ContextHelper;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import mirah.impl.Tokens;
import mirah.lang.ast.ClassDefinition;
import org.netbeans.api.lexer.Token;
import org.netbeans.modules.csl.api.CompletionProposal;
import ru.programpark.mirah.editor.completion.CompletionItem;
import ru.programpark.mirah.editor.utils.MirahUtils;

/**
 * This should complete constructor generation.
 *  We are processing e.g. SB, StrB both results in:
 *
 *      StringBuilder() {
 *      }
 * 
 * @author Martin Janicek
 */
public class ConstructorGenerationCompletion extends BaseCompletion {

    @Override
    public boolean complete(List<CompletionProposal> proposals, CompletionContext request, int anchor) {
        LOG.log(Level.FINEST, "-> constructor generation completion"); // NOI18N

        if (!isValidLocation(request)) {
            return false;
        }

        ClassDefinition requestedClass = ContextHelper.getSurroundingClassDefinition(request);
        if (requestedClass == null) {
            LOG.log(Level.FINEST, "No surrounding class found, bail out ..."); // NOI18N
            return false;
        }
        String className = MirahUtils.stripPackage(requestedClass.name().identifier());

        boolean camelCaseMatch = CamelCaseUtil.compareCamelCase(className, request.getPrefix());
        if (camelCaseMatch) {
            LOG.log(Level.FINEST, "Prefix matches Class's CamelCase signature. Adding."); // NOI18N
            proposals.add(new CompletionItem.ConstructorItem(className, Collections.EMPTY_LIST, anchor, true));
        }

        return camelCaseMatch;
    }

    private boolean isValidLocation(CompletionContext request) {
        if (!(request.location == CaretLocation.INSIDE_CLASS)) {
            LOG.log(Level.FINEST, "Not inside a class"); // NOI18N
            return false;
        }

        // We don't want to offer costructor generation when creating new instance
        if (request.context.before1 != null && request.context.before1.text().toString().equals("new") && request.getPrefix().length() > 0) {
            return false;
        }

        // We are after either implements or extends keyword
        if ((request.context.beforeLiteral != null && Tokens.tImplements.equals(request.context.beforeLiteral.id())) //||
//            (request.context.beforeLiteral != null && request.context.beforeLiteral.id() == Tokens.LITERAL_extends)) {
                ) {
            return false;
        }

        if (request.getPrefix() == null || request.getPrefix().length() < 0) {
            return false;
        }

        // We can be either in 'String ^' or in 'NoSE^' situation
        if (request.context.before1 != null && request.context.before1.id().is(Tokens.tIDENTIFIER)) {
            request.context.ts.movePrevious();
            Token<?> caretToken = request.context.ts.token();

            if (" ".equals(caretToken.text().toString())) {
                // 'String ^' situation --> No constructor generation proposals
                return false;
            }
        }

        // We are after class definition
        if (request.context.beforeLiteral != null && (request.context.beforeLiteral.id().is(Tokens.tClass) || request.context.beforeLiteral.id().is(Tokens.tEnum)) ) {
            return false;
        }

        return true;
    }
}
