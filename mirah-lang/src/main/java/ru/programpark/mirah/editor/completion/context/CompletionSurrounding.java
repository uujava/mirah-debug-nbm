package ru.programpark.mirah.editor.completion.context;

import ru.programpark.mirah.lexer.MirahTokenId;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenSequence;

/**
 * Holder class for the context of a given completion.
 * This means the two surrounding Lexer-tokens before and after the completion point are stored here.
 *
 * @author Martin Janicek
 */
public class CompletionSurrounding {

    // b2    b1      |       a1        a2
    // class MyClass extends BaseClass {
    public Token<MirahTokenId> beforeLiteral;
    public Token<MirahTokenId> before2;
    public Token<MirahTokenId> before1;
    public Token<MirahTokenId> active;
    public Token<MirahTokenId> after1;
    public Token<MirahTokenId> after2;
    public Token<MirahTokenId> afterLiteral;
    public TokenSequence<MirahTokenId> ts; // we keep the sequence with us.

    
    public CompletionSurrounding(
        Token<MirahTokenId> beforeLiteral,
        Token<MirahTokenId> before2,
        Token<MirahTokenId> before1,
        Token<MirahTokenId> active,
        Token<MirahTokenId> after1,
        Token<MirahTokenId> after2,
        Token<MirahTokenId> afterLiteral,
        TokenSequence<MirahTokenId> ts) {

        this.beforeLiteral = beforeLiteral;
        this.before2 = before2;
        this.before1 = before1;
        this.active = active;
        this.after1 = after1;
        this.after2 = after2;
        this.afterLiteral = afterLiteral;
        this.ts = ts;
    }
}
