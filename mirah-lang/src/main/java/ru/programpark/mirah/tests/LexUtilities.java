package ru.programpark.mirah.tests;

import ca.weblite.netbeans.mirah.lexer.MirahLexerUtils;
import ca.weblite.netbeans.mirah.lexer.MirahParser;
import ca.weblite.netbeans.mirah.lexer.MirahTokenId;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import mirah.impl.Tokens;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenId;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.editor.BaseDocument;
import org.netbeans.editor.Utilities;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.parsing.api.Source;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;


/**
 * Utilities associated with lexing or analyzing the document at the
 * lexical level, unlike AstUtilities which is contains utilities
 * to analyze parsed information about a document.
 *
 * @author Tor Norbye
 */
public final class LexUtilities {
    /** Tokens that match a corresponding END statement. Even though while, unless etc.
     * can be statement modifiers, those luckily have different token ids so are not a problem
     * here.
     */
    private static final Set<TokenId> END_PAIRS = new HashSet<TokenId>();

    /**
     * Tokens that should cause indentation of the next line. This is true for all {@link #END_PAIRS},
     * but also includes tokens like "else" that are not themselves matched with end but also contribute
     * structure for indentation.
     *
     */
    private static final Set<TokenId> INDENT_WORDS = new HashSet<TokenId>();
    private static final Set<TokenId> WHITESPACES_AND_COMMENTS = new HashSet<TokenId>();

    static {
        WHITESPACES_AND_COMMENTS.add(MirahTokenId.WHITESPACE);
        WHITESPACES_AND_COMMENTS.add(MirahTokenId.NL);
//        WHITESPACES_AND_COMMENTS.add(Tokens.tEId..EOL);
//        WHITESPACES_AND_COMMENTS.add(Tokens.tComment);
//        WHITESPACES_AND_COMMENTS.add(MirahTokenId..EOL);
//        WHITESPACES_AND_COMMENTS.add(MirahTokenId.LINE_COMMENT);
//        WHITESPACES_AND_COMMENTS.add(MirahTokenId.BLOCK_COMMENT);

        END_PAIRS.add(MirahTokenId.LBRACE);

        INDENT_WORDS.addAll(END_PAIRS);
//        INDENT_WORDS.add(MirahTokenId.COLON);
//        INDENT_WORDS.add(MirahTokenId.LITERAL_case);
//        INDENT_WORDS.add(MirahTokenId.LITERAL_default);
    }

    @CheckForNull
    public static BaseDocument getDocument(MirahParser.NBMirahParserResult info, boolean forceOpen) {
        if (info != null) {
            Source source = info.getSnapshot().getSource();
            return getDocument(source, forceOpen);
        }

        return null;
    }

    @CheckForNull
    public static BaseDocument getDocument(Source source, boolean forceOpen) {
        BaseDocument bdoc = null;

        Document doc = source.getDocument(true);
        if (doc instanceof BaseDocument) {
            bdoc = (BaseDocument) doc;
        }

        return bdoc;
    }

    public static BaseDocument getDocument(FileObject fileObject, boolean forceOpen) {
        DataObject dobj;

        try {
            dobj = DataObject.find(fileObject);

            EditorCookie ec = dobj.getLookup().lookup(EditorCookie.class);

            if (ec == null) {
                throw new IOException("Can't open " + fileObject.getNameExt());
            }

            Document document;

            if (forceOpen) {
                document = ec.openDocument();
            } else {
                document = ec.getDocument();
            }

            if (document instanceof BaseDocument) {
                return ((BaseDocument) document);
            } else {
                // Must be testsuite execution
                try {
                    Class c = Class.forName("org.netbeans.modules.groovy.editor.test.GroovyTestBase");
                    if (c != null) {
                        @SuppressWarnings("unchecked")
                        Method m = c.getMethod("getDocumentFor", new Class[] {FileObject.class});
                        return (BaseDocument) m.invoke(null, (Object[]) new FileObject[] {fileObject});
                    }
                } catch (Exception ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        } catch (IOException ioe) {
            Exceptions.printStackTrace(ioe);
        }

        return null;
    }

    private LexUtilities() {
    }

    public static OffsetRange getLexerOffsets(MirahParser.NBMirahParserResult info, OffsetRange astRange) {
        int rangeStart = astRange.getStart();
        int start = info.getSnapshot().getOriginalOffset(rangeStart);
        if (start == rangeStart) {
            return astRange;
        } else if (start == -1) {
            return OffsetRange.NONE;
        } else {
            // Assumes the translated range maintains size
            return new OffsetRange(start, start + astRange.getLength());
        }
    }

    static String mirah_file_name = "c:\\mirah-debug\\mavenproject1\\src\\main\\java\\ru\\pp\\MirahClass22.mirah";
    
    /** Find the Mirah token sequence (in case it's embedded in something else at the top level. */
    @SuppressWarnings("unchecked")
    public static TokenSequence<MirahTokenId> getMirahTokenSequence(Document doc, int offset) {
//        if ( true ) return MirahLexerUtils.mirahTokenSequence(mirah_file_name, 0, false);
        
        final BaseDocument baseDocument = (BaseDocument) doc;
        try {
            baseDocument.readLock();
            return getMirahTokenSequence(TokenHierarchy.get(doc), offset);
        } finally {
            baseDocument.readUnlock();
        }
    }

    @SuppressWarnings("unchecked")
    private static TokenSequence<MirahTokenId> findRhtmlDelimited(TokenSequence t, int offset) {
        if (t.language().mimeType().equals("text/x-gsp")) {
            t.move(offset);
            if (t.moveNext() && t.token() != null && "groovy-delimiter".equals(t.token().id().primaryCategory())) { // NOI18N
                // It's a delimiter - move ahead and see if we find it
                if (t.moveNext() && t.token() != null && "groovy".equals(t.token().id().primaryCategory())) { // NOI18N
                    TokenSequence<?> ets = t.embedded();
                    if (ets != null) {
                        return (TokenSequence<MirahTokenId>) ets;
                    }
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static TokenSequence<MirahTokenId> getMirahTokenSequence(TokenHierarchy<Document> th, int offset) {
        TokenSequence<MirahTokenId> ts = th.tokenSequence(MirahTokenId.getLanguage());

        if (ts == null) {
            // Possibly an embedding scenario such as an RHTML file
            // First try with backward bias true
            List<TokenSequence<?>> list = th.embeddedTokenSequences(offset, true);

            for (TokenSequence t : list) {
                if (t.language() == MirahTokenId.getLanguage()) {
                    ts = t;

                    break;
                } else {
                    TokenSequence<MirahTokenId> ets = findRhtmlDelimited(t, offset);
                    if (ets != null) {
                        return ets;
                    }
                }
            }

            if (ts == null) {
                list = th.embeddedTokenSequences(offset, false);

                for (TokenSequence t : list) {
                    if (t.language() == MirahTokenId.getLanguage()) {
                        ts = t;

                        break;
                    } else {
                        TokenSequence<MirahTokenId> ets = findRhtmlDelimited(t, offset);
                        if (ets != null) {
                            return ets;
                        }
                    }
                }
            }
        }

        return ts;
    }

    public static TokenSequence<MirahTokenId> getPositionedSequence(BaseDocument doc, int offset) {
        return getPositionedSequence(doc, offset, true);
    }

    public static TokenSequence<MirahTokenId> getPositionedSequence(BaseDocument doc, int offset, boolean lookBack) {
        TokenSequence<MirahTokenId> ts = getMirahTokenSequence(doc, offset);

        if (ts != null) {
            try {
                ts.move(offset);
            } catch (AssertionError e) {
                DataObject dobj = (DataObject) doc.getProperty(Document.StreamDescriptionProperty);

                if (dobj != null) {
                    Exceptions.attachMessage(e, FileUtil.getFileDisplayName(dobj.getPrimaryFile()));
                }

                throw e;
            }

            if (!lookBack && !ts.moveNext()) {
                return null;
            } else if (lookBack && !ts.moveNext() && !ts.movePrevious()) {
                return null;
            }

            return ts;
        }

        return null;
    }

    public static Token<MirahTokenId> getToken(BaseDocument doc, int offset) {
        TokenSequence<MirahTokenId> ts = getMirahTokenSequence(doc, offset);

        if (ts != null) {
            try {
                ts.move(offset);
            } catch (AssertionError e) {
                DataObject dobj = (DataObject) doc.getProperty(Document.StreamDescriptionProperty);

                if (dobj != null) {
                    Exceptions.attachMessage(e, FileUtil.getFileDisplayName(dobj.getPrimaryFile()));
                }

                throw e;
            }

            if (!ts.moveNext() && !ts.movePrevious()) {
                return null;
            }

            Token<MirahTokenId> token = ts.token();

            return token;
        }

        return null;
    }

    public static char getTokenChar(BaseDocument doc, int offset) {
        Token<MirahTokenId> token = getToken(doc, offset);

        if (token != null) {
            String text = token.text().toString();

            if (text.length() > 0) { // Usually true, but I could have gotten EOF right?

                return text.charAt(0);
            }
        }

        return 0;
    }

    /** Search forwards in the token sequence until a token of type <code>down</code> is found. */
    public static OffsetRange findFwd(BaseDocument doc, TokenSequence<MirahTokenId> ts, TokenId up,
        TokenId down) {
        int balance = 0;

        while (ts.moveNext()) {
            Token<MirahTokenId> token = ts.token();
            TokenId id = token.id();

            if (id == up) {
                balance++;
            } else if (id == down) {
                if (balance == 0) {
                    return new OffsetRange(ts.offset(), ts.offset() + token.length());
                }

                balance--;
            }
        }

        return OffsetRange.NONE;
    }

    /** Search backwards in the token sequence until a token of type <code>up</code> is found. */
    public static OffsetRange findBwd(BaseDocument doc, TokenSequence<MirahTokenId> ts, TokenId up,
        TokenId down) {
        int balance = 0;

        while (ts.movePrevious()) {
            Token<MirahTokenId> token = ts.token();
            TokenId id = token.id();

            if (id == up) {
                if (balance == 0) {
                    return new OffsetRange(ts.offset(), ts.offset() + token.length());
                }

                balance++;
            } else if (id == down) {
                balance--;
            }
        }

        return OffsetRange.NONE;
    }

    /** Find the token that begins a block terminated by "end". This is a token
     * in the END_PAIRS array. Walk backwards and find the corresponding token.
     * It does not use indentation for clues since this could be wrong and be
     * precisely the reason why the user is using pair matching to see what's wrong.
     */
    public static OffsetRange findBegin(BaseDocument doc, TokenSequence<MirahTokenId> ts) {
        int balance = 0;

        while (ts.movePrevious()) {
            Token<MirahTokenId> token = ts.token();
            TokenId id = token.id();

            if (isBeginToken(id, doc, ts)) {
                // No matching dot for "do" used in conditionals etc.)) {
                if (balance == 0) {
                    return new OffsetRange(ts.offset(), ts.offset() + token.length());
                }

                balance--;
            } else if (id == MirahTokenId.RBRACE) {
                balance++;
            }
        }

        return OffsetRange.NONE;
    }

    public static OffsetRange findEnd(BaseDocument doc, TokenSequence<MirahTokenId> ts) {
        int balance = 0;

        while (ts.moveNext()) {
            Token<MirahTokenId> token = ts.token();
            TokenId id = token.id();

            if (isBeginToken(id, doc, ts)) {
                balance--;
            } else if (id == MirahTokenId.RBRACE) {
                if (balance == 0) {
                    return new OffsetRange(ts.offset(), ts.offset() + token.length());
                }

                balance++;
            }
        }

        return OffsetRange.NONE;
    }

    /** Determine whether "do" is an indent-token (e.g. matches an end) or if
     * it's simply a separator in while,until,for expressions)
     */
    public static boolean isEndmatchingDo(BaseDocument doc, int offset) {
        // In the following case, do is dominant:
        //     expression.do
        //        whatever
        //     end
        //
        // However, not here:
        //     while true do
        //        whatever
        //     end
        //
        // In the second case, the end matches the while, but in the first case
        // the end matches the do

        // Look at the first token of the current line
        try {
            int first = Utilities.getRowFirstNonWhite(doc, offset);
            if (first != -1) {
                Token<MirahTokenId> token = getToken(doc, first);
                if (token != null) {
                    MirahTokenId id = token.id();
                    if (id.is(Tokens.tWhile) || id.is(Tokens.tFor)) {
                        return false;
                    }
                }
            }
        } catch (BadLocationException ble) {
            Exceptions.printStackTrace(ble);
        }

        return true;
    }

    /**
     * Return true iff the given token is a token that should be matched
     * with a corresponding "end" token, such as "begin", "def", "module",
     * etc.
     */
    public static boolean isBeginToken(TokenId id, BaseDocument doc, int offset) {
        return END_PAIRS.contains(id);
    }

    /**
     * Return true iff the given token is a token that should be matched
     * with a corresponding "end" token, such as "begin", "def", "module",
     * etc.
     */
    public static boolean isBeginToken(TokenId id, BaseDocument doc, TokenSequence<MirahTokenId> ts) {
        return END_PAIRS.contains(id);
    }

    /**
     * Return true iff the given token is a token that indents its content,
     * such as the various begin tokens as well as "else", "when", etc.
     */
    public static boolean isIndentToken(TokenId id) {
        return INDENT_WORDS.contains(id);
    }

    /** Compute the balance of begin/end tokens on the line.
     * @param doc the document
     * @param offset The offset somewhere on the line
     * @param upToOffset If true, only compute the line balance up to the given offset (inclusive),
     *   and if false compute the balance for the whole line
     */
    public static int getBeginEndLineBalance(BaseDocument doc, int offset, boolean upToOffset) {
        try {
            int begin = Utilities.getRowStart(doc, offset);
            int end = upToOffset ? offset : Utilities.getRowEnd(doc, offset);

            TokenSequence<MirahTokenId> ts = LexUtilities.getMirahTokenSequence(doc, begin);
            if (ts == null) {
                return 0;
            }

            ts.move(begin);

            if (!ts.moveNext()) {
                return 0;
            }

            int balance = 0;

            do {
                Token<MirahTokenId> token = ts.token();
                TokenId id = token.id();

                if (isBeginToken(id, doc, ts)) {
                    balance++;
                } else if (id == MirahTokenId.RBRACE) {
                    balance--;
                }
            } while (ts.moveNext() && (ts.offset() <= end));

            return balance;
        } catch (BadLocationException ble) {
            Exceptions.printStackTrace(ble);

            return 0;
        }
    }

    /** Compute the balance of begin/end tokens on the line. */
    public static int getLineBalance(BaseDocument doc, int offset, TokenId up, TokenId down) {
        try {
            int begin = Utilities.getRowStart(doc, offset);
            int end = Utilities.getRowEnd(doc, offset);

            TokenSequence<MirahTokenId> ts = LexUtilities.getMirahTokenSequence(doc, begin);
            if (ts == null) {
                return 0;
            }

            ts.move(begin);

            if (!ts.moveNext()) {
                return 0;
            }

            int balance = 0;

            do {
                Token<MirahTokenId> token = ts.token();
                TokenId id = token.id();

                if (id == up) {
                    balance++;
                } else if (id == down) {
                    balance--;
                }
            } while (ts.moveNext() && (ts.offset() <= end));

            return balance;
        } catch (BadLocationException ble) {
            Exceptions.printStackTrace(ble);

            return 0;
        }
    }

    /**
     * The same as braceBalance but generalized to any pair of matching
     * tokens.
     * @param open the token that increses the count
     * @param close the token that decreses the count
     */
    public static int getTokenBalance(BaseDocument doc, TokenId open, TokenId close, int offset)
        throws BadLocationException {
        TokenSequence<MirahTokenId> ts = LexUtilities.getMirahTokenSequence(doc, 0);
        if (ts == null) {
            return 0;
        }

        // XXX Why 0? Why not offset?
        ts.moveIndex(0);

        if (!ts.moveNext()) {
            return 0;
        }

        int balance = 0;

        do {
            Token t = ts.token();

            if (t.id() == open) {
                balance++;
            } else if (t.id() == close) {
                balance--;
            }
        } while (ts.moveNext());

        return balance;
    }

    /**
     * Return true iff the line for the given offset is a comment line.
     * This will return false for lines that contain comments (even when the
     * offset is within the comment portion) but also contain code.
     */
    public static boolean isCommentOnlyLine(BaseDocument doc, int offset)
        throws BadLocationException {
        int begin = Utilities.getRowFirstNonWhite(doc, offset);

        if (begin == -1) {
            return false; // whitespace only
        }

        if (begin == doc.getLength()) {
            return false;
        }

        return false; //doc.getText(begin, 2).equals("//") || doc.getText(begin, 1).equals("*");
    }

    /**
     * Determine if the caret is inside a literal string, and if so, return its starting
     * offset. Return -1 otherwise.
     */
    @SuppressWarnings("unchecked")
    private static int getLiteralStringOffset(int caretOffset, TokenHierarchy<Document> th,
        MirahTokenId begin) {
        TokenSequence<MirahTokenId> ts = getMirahTokenSequence(th, caretOffset);

        if (ts == null) {
            return -1;
        }

        ts.move(caretOffset);

        if (!ts.moveNext() && !ts.movePrevious()) {
            return -1;
        }

        if (ts.offset() == caretOffset) {
            // We're looking at the offset to the RIGHT of the caret
            // and here I care about what's on the left
            ts.movePrevious();
        }

        Token<MirahTokenId> token = ts.token();

        if (token != null) {
            MirahTokenId id = token.id();

            // Skip over embedded Groovy segments and literal strings until you find the beginning
            while ( //id == Tokens.te.ERROR         || 
                   id.is(Tokens.tStringContent) //.STRING_LITERAL
                || id.is(Tokens.tRegexBegin)) { //.REGEXP_LITERAL) {

                ts.movePrevious();
                token = ts.token();
                id = token.id();
            }

            if (id == begin) {
                if (!ts.moveNext()) {
                    return -1;
                }

                return ts.offset();
            }
        }

        return -1;
    }

    public static OffsetRange getCommentBlock(BaseDocument doc, int caretOffset) {
        // Check if the caret is within a comment, and if so insert a new
        // leaf "node" which contains the comment line and then comment block
        try {
            Token<MirahTokenId> token = getToken(doc, caretOffset);

            if ((token != null) && (token.id().is(Tokens.tComment)) ) { //Id.LINE_COMMENT)) {
                // First add a range for the current line
                int begin = Utilities.getRowStart(doc, caretOffset);
                int end = Utilities.getRowEnd(doc, caretOffset);

                if (LexUtilities.isCommentOnlyLine(doc, caretOffset)) {

                    while (begin > 0) {
                        int newBegin = Utilities.getRowStart(doc, begin - 1);

                        if ((newBegin < 0) || !LexUtilities.isCommentOnlyLine(doc, newBegin)) {
                            begin = Utilities.getRowFirstNonWhite(doc, begin);
                            break;
                        }

                        begin = newBegin;
                    }

                    int length = doc.getLength();

                    while (true) {
                        int newEnd = Utilities.getRowEnd(doc, end + 1);

                        if ((newEnd >= length) || !LexUtilities.isCommentOnlyLine(doc, newEnd)) {
                            end = Utilities.getRowLastNonWhite(doc, end) + 1;
                            break;
                        }

                        end = newEnd;
                    }

                    if (begin < end) {
                        return new OffsetRange(begin, end);
                    }
                } else {
                    // It's just a line comment next to some code
                    TokenHierarchy<Document> th = TokenHierarchy.get((Document) doc);
                    int offset = token.offset(th);
                    return new OffsetRange(offset, offset + token.length());
                }
            }
        } catch (BadLocationException ble) {
            Exceptions.printStackTrace(ble);
        }

        return OffsetRange.NONE;
    }

    /**
     * Back up to the first space character prior to the given offset - as long as
     * it's on the same line!  If there's only leading whitespace on the line up
     * to the lex offset, return the offset itself
     */
    public static int findSpaceBegin(BaseDocument doc, int lexOffset) {
        TokenSequence ts = LexUtilities.getMirahTokenSequence(doc, lexOffset);
        if (ts == null) {
            return lexOffset;
        }
        boolean allowPrevLine = false;
        int lineStart;
        try {
            lineStart = Utilities.getRowStart(doc, Math.min(lexOffset, doc.getLength()));
            int prevLast = lineStart - 1;
            if (lineStart > 0) {
                prevLast = Utilities.getRowLastNonWhite(doc, lineStart - 1);
                if (prevLast != -1) {
                    char c = doc.getText(prevLast, 1).charAt(0);
                    if (c == ',') {
                        // Arglist continuation? // TODO : check lexing
                        allowPrevLine = true;
                    }
                }
            }
            if (!allowPrevLine) {
                int firstNonWhite = Utilities.getRowFirstNonWhite(doc, lineStart);
                if (lexOffset <= firstNonWhite || firstNonWhite == -1) {
                    return lexOffset;
                }
            } else {
                // Make lineStart so small that Math.max won't cause any problems
                int firstNonWhite = Utilities.getRowFirstNonWhite(doc, lineStart);
                if (prevLast >= 0 && (lexOffset <= firstNonWhite || firstNonWhite == -1)) {
                    return prevLast + 1;
                }
                lineStart = 0;
            }
        } catch (BadLocationException ble) {
            Exceptions.printStackTrace(ble);
            return lexOffset;
        }
        ts.move(lexOffset);
        if (ts.moveNext()) {
            if (lexOffset > ts.offset()) {
                // We're in the middle of a token
                return Math.max((ts.token().id() == MirahTokenId.WHITESPACE) ? ts.offset() : lexOffset, lineStart);
            }
            while (ts.movePrevious()) {
                Token token = ts.token();
                if (token.id() != MirahTokenId.WHITESPACE) {
                    return Math.max(ts.offset() + token.length(), lineStart);
                }
            }
        }

        return lexOffset;
    }

    public static Token<MirahTokenId> findPreviousNonWsNonComment(TokenSequence<MirahTokenId> ts) {
        return findPrevious(ts, WHITESPACES_AND_COMMENTS);
    }

    private static Token<MirahTokenId> findPrevious(TokenSequence<MirahTokenId> ts, Set<TokenId> ignores) {
        ts.movePrevious();
        if (ignores.contains(ts.token().id())) {
            while (ts.movePrevious() && ignores.contains(ts.token().id())) {}
        }
        return ts.token();
    }

}