package ru.programpark.mirah.editor.completion;

import ca.weblite.netbeans.mirah.lexer.MirahTokenId;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import mirah.impl.Tokens;
import org.netbeans.api.java.source.ClassIndex;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.modules.csl.api.CompletionProposal;
import org.openide.filesystems.FileObject;
import ru.programpark.mirah.editor.api.completion.util.CompletionContext;
import ru.programpark.mirah.tests.LexUtilities;

/**
 *
 * @author Martin Janicek
 */
public abstract class BaseCompletion {

    protected static final Logger LOG = Logger.getLogger(BaseCompletion.class.getName());


    public abstract boolean complete(List<CompletionProposal> proposals, CompletionContext request, int anchor);


    protected class PackageCompletionRequest {
        String fullString = "";
        String basePackage = "";
        String prefix = "";
    }

    protected final ClasspathInfo getClasspathInfoFromRequest(final CompletionContext request) {
        FileObject fileObject = request.getSourceFile();

        if (fileObject != null) {
            return ClasspathInfo.create(fileObject);
        }
        return null;
    }

    protected final boolean isValidPackage(ClasspathInfo pathInfo, String pkg) {
        assert pathInfo != null : "ClasspathInfo can not be null";

        // the following statement gives us all the packages *starting* with the
        // first parameter. We have to check for exact matches ourselves. See # 142372

        Set<String> pkgSet = pathInfo.getClassIndex().getPackageNames(pkg, true, EnumSet.allOf(ClassIndex.SearchScope.class));

        if (pkgSet.size() > 0) {
            LOG.log(Level.FINEST, "Packages with prefix : {0}", pkg);
            LOG.log(Level.FINEST, "               found : {0}", pkgSet);

            for (String singlePkg : pkgSet) {
                if (singlePkg.equals(pkg)){
                    LOG.log(Level.FINEST, "Exact match found.");
                    return true;
                }
            }

            return false;
        } else {
            return false;
        }
    }

    protected boolean isPrefixed(CompletionContext request, String name) {
        return name.toUpperCase(Locale.ENGLISH).startsWith(request.getPrefix().toUpperCase(Locale.ENGLISH));
    }

    protected boolean isPrefixedAndNotEqual(CompletionContext request, String name) {
        return isPrefixed(request, name) && !(name.equals(request.getPrefix()));
    }

    protected final PackageCompletionRequest getPackageRequest(final CompletionContext request) {
        int position = request.lexOffset;
        PackageCompletionRequest result = new PackageCompletionRequest();

        TokenSequence<MirahTokenId> ts = LexUtilities.getMirahTokenSequence(request.doc, position);
        ts.move(position);

        // travel back on the token string till the token is neither a
        // DOT nor an IDENTIFIER

        Token<MirahTokenId> token = null;
        boolean remainingTokens = true;
        while (ts.isValid() && (remainingTokens = ts.movePrevious()) && ts.offset() >= 0) {
            Token<MirahTokenId> t = ts.token();

            // Keyword check needs to be here because of issue #209453
            if (!(t.id().is(Tokens.tDot) || t.id().is(Tokens.tIDENTIFIER) || "keyword".equals(t.id().primaryCategory()))) {
                break;
            } else {
                token = t;
            }
        }

        // now we are travelling in the opposite direction to construct
        // the result

        StringBuilder sb = new StringBuilder();
        Token<MirahTokenId> lastToken = null;

        // if we reached the beginning in the previous iteration we have to get
        // the first token too (without call to moveNext())
        if (!remainingTokens && token != null && ts.isValid()) {
            sb.append(token.text().toString());
            lastToken = token;
        }

        // iterate the rest of the sequence
        while (ts.isValid() && ts.moveNext() && ts.offset() < position) {
            Token<MirahTokenId> t = ts.token();
            if (t.id().is(Tokens.tDot) || t.id().is(Tokens.tIDENTIFIER)) {
                sb.append(t.text().toString());
                lastToken = t;
            } else {
                break;
            }
        }

        // construct the return value. These are the combinations:
        // string           basePackage prefix
        // ""               ""          ""
        // "java"           ""          "java"
        // "java."          "java"      ""
        // "java.lan"       "java"      "lan"
        // "java.lang"      "java"      "lang"
        // "java.lang."     "java.lang" ""

        result.fullString = sb.toString();

        if (sb.length() == 0) {
            result.basePackage = "";
            result.prefix = "";

            // This might happened if we are trying to get completion on prefix which match to some keyword
            // In that case 'sb' is empty, but we want to have result.prefix initialized (see issue #209453)
            if (token != null && "keyword".equals(token.id().primaryCategory())) {
                result.prefix = request.getPrefix();
            }
        } else if (lastToken != null && lastToken.id().is(Tokens.tDot)) {
            String pkgString = sb.toString();
            result.basePackage = pkgString.substring(0, pkgString.length() - 1);
            result.prefix = "";
        } else if (lastToken != null && lastToken.id().is(Tokens.tIDENTIFIER)) {
            String pkgString = sb.toString();
            result.prefix = lastToken.text().toString();

            result.basePackage = pkgString.substring(0, pkgString.length() - result.prefix.length());

            if (result.basePackage.endsWith(".")) {
                result.basePackage = result.basePackage.substring(0, result.basePackage.length() - 1);
            }
        }

        LOG.log(Level.FINEST, "-- fullString : >{0}<", result.fullString);
        LOG.log(Level.FINEST, "-- basePackage: >{0}<", result.basePackage);
        LOG.log(Level.FINEST, "-- prefix:      >{0}<", result.prefix);

        return result;
    }
}
