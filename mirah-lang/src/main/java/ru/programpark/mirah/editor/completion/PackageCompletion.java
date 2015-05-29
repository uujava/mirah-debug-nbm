package ru.programpark.mirah.editor.completion;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.netbeans.api.java.source.ClassIndex;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.lib.editor.util.CharSequenceUtilities;
import org.netbeans.modules.csl.api.CompletionProposal;
import ru.programpark.mirah.editor.api.completion.CompletionItem;
import ru.programpark.mirah.editor.api.completion.util.CompletionContext;

/**
 * Here we complete package-names like java.lan to java.lang ...
 * 
 * @author Martin Janicek
 */
public class PackageCompletion extends BaseCompletion {

    @Override
    public boolean complete(List<CompletionProposal> proposals, CompletionContext request, int anchor) {
        LOG.log(Level.FINEST, "-> completePackages"); // NOI18N

        // this can happen for ?. or similar constructs
        PackageCompletionRequest packageRequest = getPackageRequest(request);
        if (request.isBehindDot() && packageRequest.basePackage.length() <= 0) {
            return false;
        }

        LOG.log(Level.FINEST, "Token fullString = >{0}<", packageRequest.fullString);

        ClasspathInfo pathInfo = getClasspathInfoFromRequest(request);
        assert pathInfo != null : "Can not get ClasspathInfo";

        if (request.context.before1 != null
                && CharSequenceUtilities.textEquals(request.context.before1.text(), "*")
                && request.isBehindImportStatement()) {
            return false;
        }

        // try to find suitable packages ...

        Set<String> pkgSet = pathInfo.getClassIndex().getPackageNames(packageRequest.fullString, true, EnumSet.allOf(ClassIndex.SearchScope.class));

        for (String singlePackage : pkgSet) {
            LOG.log(Level.FINEST, "PKG set item: {0}", singlePackage);

            if (packageRequest.prefix.equals("")) {
                singlePackage = singlePackage.substring(packageRequest.fullString.length());
            } else if (!packageRequest.basePackage.equals("")) {
                singlePackage = singlePackage.substring(packageRequest.basePackage.length() + 1);
            }

            if (singlePackage.startsWith(packageRequest.prefix) && singlePackage.length() > 0) {
                CompletionItem.PackageItem item = new CompletionItem.PackageItem(singlePackage, anchor, request.getParserResult());

                if (request.isBehindImportStatement()) {
                    item.setSmart(true);
                }
                proposals.add(item);
            }
        }

        return false;
    }
}