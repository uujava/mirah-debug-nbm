package ru.programpark.mirah.editor.completion.handler;

import ru.programpark.mirah.editor.completion.context.CompletionSurrounding;
import ru.programpark.mirah.editor.completion.context.CompletionContext;
import ru.programpark.mirah.editor.completion.context.CaretLocation;
import ru.programpark.mirah.lexer.MirahTokenId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import mirah.impl.Tokens;
import org.netbeans.modules.csl.api.CompletionProposal;
import ru.programpark.mirah.editor.completion.CompletionItem;

/**
 *
 * @author Martin Janicek
 */
public class NewVarCompletion extends BaseCompletion {


//    @Override
    public boolean complete(List<CompletionProposal> proposals, CompletionContext request, int anchor) {
        LOG.log(Level.FINEST, "-> completeNewVars"); // NOI18N

        List<String> newVars = getNewVarNameSuggestion(request.context);

        if (isValid(request, newVars) == false) {
            return false;
        }

        boolean stuffAdded = false;
        for (String var : newVars) {
            LOG.log(Level.FINEST, "Variable candidate: {0}", var); // NOI18N
            if (var.startsWith(request.getPrefix()) && !var.equals(request.getPrefix())) {
                proposals.add(new CompletionItem.NewVarItem(var, anchor));
                stuffAdded = true;
            }
        }
        return stuffAdded;
    }

    /**
     * Finds out if we are in the right place to complete new variables.
     *
     * @param request completion request
     * @return true if there is a possibility that something could be proposed, false otherwise
     */
    private boolean isValid(CompletionContext request, List<String> newVars) {
        if (request.location == CaretLocation.OUTSIDE_CLASSES) {
            LOG.log(Level.FINEST, "outside of any class, bail out."); // NOI18N
            return false;
        }

        if (request.isBehindDot()) {
            LOG.log(Level.FINEST, "We are invoked right behind a dot."); // NOI18N
            return false;
        }

        if (newVars == null) {
            LOG.log(Level.FINEST, "Can not propose with newVars == null"); // NOI18N
            return false;
        }
        
        return true;
    }

    /**
     * This is a minimal version of Utilities.varNamesForType() to suggest variable names.
     *
     * See:
     * java.editor/src/org/netbeans/modules/editor/java/JavaCompletionProvider.java
     * java.editor/src/org/netbeans/modules/editor/java/Utilities.varNamesSuggestions()
     * how to do this right.
     *
     * todo: recurse to look at arrays. For example: Long [] gives longs
     *
     * @param ctx
     * @return
     */
    private List<String> getNewVarNameSuggestion(CompletionSurrounding ctx) {
        LOG.log(Level.FINEST, "getNewVarNameSuggestion()"); // NOI18N

        List<String> result = new ArrayList<String>();

        if (ctx == null || ctx.before1 == null) {
            return result;
        }

        // Check for primitive types first:
        // int long char byte double float short boolean
        MirahTokenId tokenBefore = ctx.before1.id();
        /*
        switch (tokenBefore) {
            case LITERAL_boolean:
                result.add("b"); break;
            case LITERAL_byte:
                result.add("b"); break;
            case LITERAL_char:
                result.add("c"); break;
            case LITERAL_double:
                result.add("d"); break;
            case LITERAL_float:
                result.add("f"); break;
            case LITERAL_int:
                result.add("i"); break;
            case LITERAL_long:
                result.add("l"); break;
            case LITERAL_short:
                result.add("s"); break;
        }
                */
        // now we propose variable names based on the type

        if (ctx.before1.id().is(Tokens.tIDENTIFIER)) {

            String typeName = ctx.before1.text().toString();

            if (typeName != null) {
                // Only First char, lowercase
                addIfNotIn(result, typeName.substring(0, 1).toLowerCase(Locale.ENGLISH));
                // name lowercase
                addIfNotIn(result, typeName.toLowerCase(Locale.ENGLISH));
                // camelcase hunches put together
                addIfNotIn(result, camelCaseHunch(typeName));
                // first char switched to lowercase
                addIfNotIn(result, typeName.substring(0, 1).toLowerCase(Locale.ENGLISH) + typeName.substring(1));
            }
        }
        return result;
    }

    void addIfNotIn(List<String> result, String name) {
        if (name.length() > 0) {
            if (!result.contains(name)) {
                LOG.log(Level.FINEST, "Adding new-var suggestion : {0}", name); // NOI18N
                result.add(name);
            }
        }
    }

    // this was: Utilities.nextName()
    private static String camelCaseHunch(CharSequence name) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                char lc = Character.toLowerCase(c);
                sb.append(lc);
            }
        }
        return sb.toString();
    }
}
