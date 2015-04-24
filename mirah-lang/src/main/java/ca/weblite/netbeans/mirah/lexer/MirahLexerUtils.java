/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.weblite.netbeans.mirah.lexer;

import ca.weblite.netbeans.mirah.lexer.MirahTokenId;
import java.util.List;
import javax.swing.text.Document;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.editor.BaseDocument;
import org.netbeans.modules.csl.api.OffsetRange;

/**
 *
 * @author savushkin
 */
public class MirahLexerUtils {
    
    public static TokenSequence<MirahTokenId> mirahTokenSequence(Document doc, int caretOffset, boolean backwardBias) {
        BaseDocument bd = (BaseDocument) doc;
        try {
            bd.readLock();
            TokenHierarchy<?> hi = TokenHierarchy.get(doc);
            List<TokenSequence<?>> tsList = hi.embeddedTokenSequences(caretOffset, backwardBias);
            // Go from inner to outer TSes
            for (int i = tsList.size() - 1; i >= 0; i--) {
                TokenSequence<?> ts = tsList.get(i);
                if (ts.languagePath().innerLanguage() == MirahTokenId.getLanguage()) {
                    TokenSequence<MirahTokenId> javaInnerTS = (TokenSequence<MirahTokenId>) ts;
                    //bd.readUnlock();
                    return javaInnerTS;
                }
            }
        } finally {
            bd.readUnlock();
        }
        return null;
    }
    
    /**
     * Search backwards in the token sequence until a token of type
     * <code>up</code> is found
     */
    public static OffsetRange findBwd(TokenSequence<MirahTokenId> tokens, MirahTokenId up, MirahTokenId down )
    {
        int balance = 0;
        while (tokens.moveNext ()) {
            Token<MirahTokenId> token = tokens.token();
            MirahTokenId id = token.id();

            if (id == up) {
                if (balance == 0) {
                    return new OffsetRange(tokens.offset(), tokens.offset() + token.length());
                }
                balance++;
            } else if (id == down) {
                balance--;
            }
        }
        return OffsetRange.NONE;
    }

    /**
     * Search forwards in the token sequence until a token of type
     * <code>down</code> is found
     */
    public static OffsetRange findFwd(TokenSequence<MirahTokenId> tokens, MirahTokenId up, MirahTokenId down )
    {
        int balance = 0;
        while (tokens.moveNext()) {
            Token<MirahTokenId> token = tokens.token();
            MirahTokenId id = token.id();
            String text = token.text().toString();

            if (text.equals(up)) {
                balance++;
            } else if (text.equals(down)) {
                if (balance == 0) {
                    return new OffsetRange(tokens.offset(), tokens.offset() + token.length());
                }
                balance--;
            }
        }
        return OffsetRange.NONE;
    }
}
/*
    int tBegin = Tokens.tBegin.ordinal();
    int tBreak = Tokens.tBreak.ordinal();
    int tClass = Tokens.tClass.ordinal();
    int tDef = Tokens.tDef.ordinal();
    int tDefined = Tokens.tDefined.ordinal();
    int tDefmacro = Tokens.tDefmacro.ordinal();
    int tDo = Tokens.tDo.ordinal();
    int tEnd = Tokens.tEnd.ordinal();
    int tLParen = Tokens.tLParen.ordinal();
    int tRParen = Tokens.tRParen.ordinal();
    int tLBrack = Tokens.tLBrack.ordinal();
    int tRBrack = Tokens.tRBrack.ordinal();
    int tLBrace = Tokens.tLBrace.ordinal();
    int tRBrace = Tokens.tRBrace.ordinal();
*/