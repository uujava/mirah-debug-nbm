package ru.programpark.mirah.editor.completion;

import ca.weblite.netbeans.mirah.lexer.MirahTokenId;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Level;
import mirah.impl.Tokens;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.modules.csl.api.CompletionProposal;
import ru.programpark.mirah.editor.api.completion.util.CompletionContext;
import ru.programpark.mirah.editor.api.completion.util.CompletionSurrounding;
import ru.programpark.mirah.editor.api.completion.CaretLocation;
import ru.programpark.mirah.editor.api.completion.CompletionItem;
import ru.programpark.mirah.editor.api.completion.KeywordCategory;
import ru.programpark.mirah.editor.api.completion.MirahKeyword;
import ru.programpark.mirah.tests.LexUtilities;

/**
 * Complete Groovy or Java Keywords.
 *
 * @see MirahKeyword for matrix of capabilities, scope and allowed usage.
 * @author Martin Janicek
 */
class KeywordCompletion extends BaseCompletion {

    private EnumSet<MirahKeyword> keywords;
    private CompletionContext request;


    @Override
    public boolean complete(List<CompletionProposal> proposals, CompletionContext request, int anchor) {
        this.request = request;

        LOG.log(Level.FINEST, "-> completeKeywords"); // NOI18N
        String prefix = request.getPrefix();

        if (request.location == CaretLocation.INSIDE_PARAMETERS) {
            return false;
        }
        
        if (request.dotContext != null) {
            if (request.dotContext.isFieldsOnly() || request.dotContext.isMethodsOnly()) {
                return false;
            }
        }

        // We are after either implements or extends keyword
        if ((request.context.beforeLiteral != null && request.context.beforeLiteral.id().is(Tokens.tImplements)) //||
//            (request.context.beforeLiteral != null && request.context.beforeLiteral.id() == MirahTokenId.LITERAL_extends)
                ) {
            return false;
        }

        if (request.isBehindDot()) {
            LOG.log(Level.FINEST, "We are invoked right behind a dot."); // NOI18N
            return false;
        }

        // Is there already a "package"-statement in the sourcecode?
        boolean havePackage = checkForPackageStatement(request);

        keywords = EnumSet.allOf(MirahKeyword.class);

        // filter-out keywords in a step-by-step approach
        filterPackageStatement(havePackage);
        filterPrefix(prefix);
        filterLocation(request.location);
        filterClassInterfaceOrdering(request.context);
        filterMethodDefinitions(request.context);
        filterKeywordsNextToEachOther(request.context);

        // add the remaining keywords to the result

        for (MirahKeyword miarhKeyword : keywords) {
            LOG.log(Level.FINEST, "Adding keyword proposal : {0}", miarhKeyword.getName()); // NOI18N
            proposals.add(new CompletionItem.KeywordItem(miarhKeyword.getName(), null, anchor, request.getParserResult(), miarhKeyword.isMirahKeyword()));
        }

        return true;
    }

    boolean checkForPackageStatement(final CompletionContext request) {
        TokenSequence<MirahTokenId> ts = LexUtilities.getMirahTokenSequence(request.doc, 1);

        if (ts != null) {
            ts.move(1);

            while (ts.isValid() && ts.moveNext() && ts.offset() < request.doc.getLength()) {
                Token<MirahTokenId> t = ts.token();

                if (t.id().is(Tokens.tPackage)) {
                    return true;
                }
            }
        }

        return false;
    }

    // filter-out package-statemen, if there's already one
    void filterPackageStatement(boolean havePackage) {
        for (MirahKeyword miarhKeyword : keywords) {
            if (miarhKeyword.getName().equals("package") && havePackage) {
                keywords.remove(miarhKeyword);
            }
        }
    }

    void filterPrefix(String prefix) {
        for (MirahKeyword miarhKeyword : keywords) {
            if (!miarhKeyword.getName().startsWith(prefix)) {
                keywords.remove(miarhKeyword);
            }
        }
    }

    void filterLocation(CaretLocation location) {
        for (MirahKeyword miarhKeyword : keywords) {
            if (!checkKeywordAllowance(miarhKeyword, location)) {
                keywords.remove(miarhKeyword);
            }
        }
    }

    // Filter right Keyword ordering
    void filterClassInterfaceOrdering(CompletionSurrounding ctx) {
        if (ctx == null || ctx.beforeLiteral == null) {
            return;
        }

        if (ctx.beforeLiteral.id().is(Tokens.tInterface)) {
            keywords.clear();
            addIfPrefixed(MirahKeyword.KEYWORD_extends);
        } else if (ctx.beforeLiteral.id().is(Tokens.tClass)) {
            keywords.clear();
            addIfPrefixed(MirahKeyword.KEYWORD_extends);
            addIfPrefixed(MirahKeyword.KEYWORD_implements);
        }
    }

    private void addIfPrefixed(MirahKeyword keyword) {
        if (isPrefixed(request, keyword.getName())) {
            keywords.add(keyword);
        }
    }

    // Filter-out modifier/datatype ordering in method definitions
    void filterMethodDefinitions(CompletionSurrounding ctx) {
        if (ctx == null || ctx.afterLiteral == null) {
            return;
        }

        if ( //ctx.afterLiteral.id() == Tokens.tVoid. ||
            ctx.afterLiteral.id().is(Tokens.tIDENTIFIER) ||
            ctx.afterLiteral.id().primaryCategory().equals("number")) {

            // we have to filter-out the primitive types

            for (MirahKeyword miarhKeyword : keywords) {
                if (miarhKeyword.getCategory() == KeywordCategory.PRIMITIVE) {
                    LOG.log(Level.FINEST, "filterMethodDefinitions - removing : {0}", miarhKeyword.getName());
                    keywords.remove(miarhKeyword);
                }
            }
        }
    }

    // Filter-out keywords, if we are surrounded by others.
    // This can only be an approximation.
    void filterKeywordsNextToEachOther(CompletionSurrounding ctx) {
        if (ctx == null) {
            return;
        }

        boolean filter = false;
        if (ctx.after1 != null && ctx.after1.id().primaryCategory().equals("keyword")) {
            filter = true;
        }
        if (ctx.before1 != null && ctx.before1.id().primaryCategory().equals("keyword")) {
            filter = true;
        }
        if (filter) {
            for (MirahKeyword miarhKeyword : keywords) {
                if (miarhKeyword.getCategory() == KeywordCategory.KEYWORD) {
                    LOG.log(Level.FINEST, "filterMethodDefinitions - removing : {0}", miarhKeyword.getName());
                    keywords.remove(miarhKeyword);
                }
            }
        }
    }

    boolean checkKeywordAllowance(MirahKeyword miarhKeyword, CaretLocation location) {
        if (location == null) {
            return false;
        }

        switch (location) {
            case ABOVE_FIRST_CLASS:
                if (miarhKeyword.isAboveFistClass()) {
                    return true;
                }
                break;
            case OUTSIDE_CLASSES:
                if (miarhKeyword.isOutsideClasses()) {
                    return true;
                }
                break;
            case INSIDE_CLASS:
                if (miarhKeyword.isInsideClass()) {
                    return true;
                }
                break;
            case INSIDE_METHOD: // intentionally fall-through
            case INSIDE_CLOSURE:
                if (miarhKeyword.isInsideCode()) {
                    return true;
                }
                break;
        }
        return false;
    }
}
