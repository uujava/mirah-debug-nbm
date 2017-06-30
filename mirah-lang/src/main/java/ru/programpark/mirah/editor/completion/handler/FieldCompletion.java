package ru.programpark.mirah.editor.completion.handler;

import ru.programpark.mirah.lexer.MirahParserResult;
import ru.programpark.mirah.editor.completion.context.CompletionContext;
import ru.programpark.mirah.editor.completion.context.CaretLocation;
import ru.programpark.mirah.editor.completion.context.ContextHelper;
import ru.programpark.mirah.lexer.MirahTokenId;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import mirah.impl.Tokens;
import mirah.lang.ast.FieldDeclaration;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.editor.BaseDocument;
import org.netbeans.modules.csl.api.CompletionProposal;
import ru.programpark.mirah.editor.completion.CompletionItem;
import ru.programpark.mirah.editor.completion.FieldSignature;
import ru.programpark.mirah.editor.completion.provider.CompleteElementHandler;
import ru.programpark.mirah.editor.utils.MirahUtils;

/**
 * Complete the fields for a class. There are two principal completions for fields:
 *
 * 1.) We are invoked right behind a dot. Then we have to retrieve the type in front of this dot.
 * 2.) We are located inside a type. Then we gotta get the fields for this class.
 *
 * @author Martin Janicek
 */
public class FieldCompletion extends BaseCompletion {

    
    // There attributes should be initiated after each complete() method call
    private List<CompletionProposal> proposals;
    private CompletionContext context;
    private int anchor;
    
    
//    @Override
    public boolean complete(List<CompletionProposal> proposals, CompletionContext context, int anchor) {
        LOG.log(Level.FINEST, "-> completeFields"); // NOI18N

        this.proposals = proposals;
        this.context = context;
        this.anchor = anchor;
        
        if (context.location == CaretLocation.INSIDE_PARAMETERS && context.isBehindDot() == false) {
            LOG.log(Level.FINEST, "no fields completion inside of parameters-list"); // NOI18N
            return false;
        }

        if (context.dotContext != null && context.dotContext.isMethodsOnly()) {
            return false;
        }

        // We are after either implements or extends keyword
        if ((context.context.beforeLiteral != null && context.context.beforeLiteral.id().is(Tokens.tImplements))) { //||
            return false;
        }
        
        if (context.context.beforeLiteral != null && context.context.beforeLiteral.id().is(Tokens.tClass)) {
            return false;
        }

        if (context.isBehindDot()) {
            LOG.log(Level.FINEST, "We are invoked right behind a dot."); // NOI18N

            PackageCompletionRequest packageRequest = getPackageRequest(context);

            if (packageRequest.basePackage.length() > 0) {
                ClasspathInfo pathInfo = getClasspathInfoFromRequest(context);

                if (isValidPackage(pathInfo, packageRequest.basePackage)) {
                    LOG.log(Level.FINEST, "The string before the dot seems to be a valid package"); // NOI18N
                    return false;
                }
            }
        } else {
            context.declaringClass = ContextHelper.getSurroundingClassDefinition(context);
        }

        // If we are dealing with GStrings, the prefix is prefixed ;-)
        // ... with the dollar sign $ See # 143295
        if (context.getPrefix().startsWith("$")) {
            context.setPrefix(context.getPrefix().substring(1)); // Remove $ from prefix
            context.setAnchor(context.getAnchor() + 1);          // Add 1 for anchor position
        }

        Map<FieldSignature, CompletionItem> result = new CompleteElementHandler(context).getFields();
        
        FieldSignature prefixFieldSignature = new FieldSignature(context.getPrefix());
        if (result.containsKey(prefixFieldSignature)) {
            result.remove(prefixFieldSignature);
        }
        proposals.addAll(result.values());
        
        
        analyzeContext(anchor);

        return true;
    }
    
    private void analyzeContext( final int initialOffset )
    {
        BaseDocument doc = (BaseDocument)context.doc;
        int caretOffset = context.astOffset;
        if ( caretOffset < initialOffset ) return;

        String filter = null;
        MirahParserResult parserResult = (MirahParserResult)context.getParserResult();

        try
        {
            doc.readLock();

            if ( parserResult == null ) return;

            int p = caretOffset-1;
            if ( p < 0 ) return;
            
            TokenSequence<MirahTokenId> toks = MirahUtils.mirahTokenSequence(doc, caretOffset, true);
            if ( toks == null ) return;
            
            Token<MirahTokenId> foundToken = null;


            boolean isClassVar = false;
            while ( toks.token() != null && toks.offset() >= anchor ){
                int tokenStart;
                Token<MirahTokenId> tok = toks.token();
                if ( tok.id().is(Tokens.tInstVar) ){
                    foundToken = tok;
                    tokenStart = toks.offset();
                    filter = doc.getText(tokenStart+1, caretOffset-tokenStart-1);
                    break;

                }
                if ( tok.id().is(Tokens.tClassVar) ){
                    isClassVar = true;
                    foundToken = tok;
                    tokenStart = toks.offset();
                    filter = doc.getText(tokenStart+2, caretOffset-tokenStart-2);
                    break;
                }
                if ( ! tok.id().is(Tokens.tIDENTIFIER) && ! tok.id().is(Tokens.tAt) ){
                    return;
                }
                if ( !toks.movePrevious() ){
                    return;
                }
                    
                if ( tok.id().is(Tokens.tAt)){
                    foundToken = tok;
                    tokenStart = anchor+1; // Not sure why we need to do +1.  May be a bug in lexer
                    if ( "@".equals(doc.getText(tokenStart-1,1)) ){
                        isClassVar = true;
                        tokenStart--;
                    }
                    break;
                }
            }
                
            if ( foundToken == null ) return;
                
            FieldDeclaration[] fields = MirahUtils.findFields(parserResult, anchor, isClassVar);

            for ( FieldDeclaration declaration : fields ){
            if ( filter == null || declaration.name().identifier().startsWith(filter))
                proposals.add(CompletionItem.forDynamicField(anchor, declaration.name().identifier(), declaration.type().toString()));
            }
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
        finally {
            doc.readUnlock();
        }
    
        
    }
    
}
