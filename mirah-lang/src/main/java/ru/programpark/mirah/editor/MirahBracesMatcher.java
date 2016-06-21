package ru.programpark.mirah.editor;

import ru.programpark.mirah.LOG;
import ru.programpark.mirah.lexer.MirahLexerUtils;
import ru.programpark.mirah.lexer.MirahTokenId;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.spi.editor.bracesmatching.BracesMatcher;
import org.netbeans.spi.editor.bracesmatching.MatcherContext;

/**
 *
 * @author Caoyuan Deng
 */
class MirahBracesMatcher implements BracesMatcher {

    private MatcherContext context;
    
//    int tBegin = Tokens.tBegin.ordinal();
//    int tBreak = Tokens.tBreak.ordinal();
//    int tClass = Tokens.tClass.ordinal();
//    int tDef = Tokens.tDef.ordinal();
//    int tDefined = Tokens.tDefined.ordinal();
//    int tDefmacro = Tokens.tDefmacro.ordinal();
//    int tDo = Tokens.tDo.ordinal();
//    int tEnd = Tokens.tEnd.ordinal();
//    int tLParen = Tokens.tLParen.ordinal();
//    int tRParen = Tokens.tRParen.ordinal();
//    int tLBrack = Tokens.tLBrack.ordinal();
//    int tRBrack = Tokens.tRBrack.ordinal();
//    int tLBrace = Tokens.tLBrace.ordinal();
//    int tRBrace = Tokens.tRBrace.ordinal();
    
    public MirahBracesMatcher( MatcherContext context )
    {
        this.context = context;
    }
    
    @Override
    public int[] findOrigin() throws InterruptedException, BadLocationException {
        int caretOffset = context.getSearchOffset();
        Document doc = context.getDocument(); //.asInstanceOf[BaseDocument] //doc.readLock
        try {
            
            TokenSequence<MirahTokenId> tokens = MirahLexerUtils.mirahTokenSequence(doc, caretOffset, false);
            while (true) {
                Token<MirahTokenId> current = tokens.token();
//                current.
                String text = tokens.token().text().toString();
                if (text != null) {
                    text = text.replaceAll("\n", "\\n");
                }
                LOG.info(this, "toks3: " + tokens.token().id().name() + " text:" + text);
                if (!tokens.moveNext()) break;
                
                if ( current.id() == MirahTokenId.LPAREN
                || current.id() == MirahTokenId.RPAREN
                || current.id() == MirahTokenId.LBRACK
                || current.id() == MirahTokenId.RBRACK
                || current.id() == MirahTokenId.LBRACE
                || current.id() == MirahTokenId.RBRACE ) {
                    return new int[] {tokens.offset(), tokens.offset() + current.length() };
                }
            }
            return null;
        } finally {
//            doc.readUnlock();
        }
    }

    @Override
    public int[] findMatches() throws InterruptedException, BadLocationException {
        int caretOffset = context.getSearchOffset();
        Document doc = context.getDocument(); //.asInstanceOf[BaseDocument] //doc.readLock
        try {
            OffsetRange range = null;
            TokenSequence<MirahTokenId> tokens = MirahLexerUtils.mirahTokenSequence(doc, caretOffset, false);
            while (true) {
                Token<MirahTokenId> current = tokens.token();
//                current.
                String text = tokens.token().text().toString();
                if (text != null) {
                    text = text.replaceAll("\n", "\\n");
                }
                LOG.info(this, "toks3: " + tokens.token().id().name() + " text:" + text);
                if (!tokens.moveNext()) {
                    break;
                }
                /*
                if (current.id().ordinal() ==  tBegin ) {

                up = MirahTokenId..STRING_BEGIN;
                down = MirahTokenId.STRING_END)
                        }
                if (current.id().ordinal() == tEnd ) {
                    forward = false;
                up = MirahTokenId.STRING_BEGIN;
                down = MirahTokenId.STRING_END;
                   }*/
                
                if (current.id() == MirahTokenId.LPAREN )
                    range = MirahLexerUtils.findFwd(tokens, MirahTokenId.LPAREN, MirahTokenId.RPAREN);
                else if (current.id() == MirahTokenId.RPAREN )
                    range = MirahLexerUtils.findBwd(tokens, MirahTokenId.LPAREN, MirahTokenId.RPAREN);
                else if (current.id() == MirahTokenId.LBRACE )
                    range = MirahLexerUtils.findFwd(tokens, MirahTokenId.LBRACE, MirahTokenId.RBRACE);
                else if (current.id() == MirahTokenId.RBRACE )
                    range = MirahLexerUtils.findBwd(tokens, MirahTokenId.RBRACE, MirahTokenId.RBRACE);
                else if (current.id() == MirahTokenId.LBRACK )
                    range = MirahLexerUtils.findFwd(tokens, MirahTokenId.LBRACK, MirahTokenId.RBRACK);
                else if ( current.id() == MirahTokenId.RBRACK )
                    range = MirahLexerUtils.findBwd(tokens, MirahTokenId.LBRACK, MirahTokenId.RBRACK);
                if ( range != null ) break;
            }
            return range == null ? null : new int[] {range.getStart(), range.getEnd()};
        } finally {
            //doc.readUnlock
        }
    }
/*    
  override def findOrigin: Array[Int] = {
    var offset = context.getSearchOffset
    val doc = context.getDocument.asInstanceOf[BaseDocument]

    //doc.readLock
    try {
      ScalaLexUtil.getTokenSequence(doc, offset) foreach { ts =>
        ts.move(offset)
        if (!ts.moveNext) {
          return null
        }

        var token = ts.token
        if (token eq null) {
          return null
        }

        var id = token.id

        if (id == ScalaTokenId.Ws) {
          // ts.move(offset) gives the token to the left of the caret.
          // If you have the caret right at the beginning of a token, try
          // the token to the right too - this means that if you have
          //  "   |def" it will show the matching "end" for the "def".
          offset += 1
          ts.move(offset)
          if (ts.moveNext && ts.offset <= offset) {
            token = ts.token
            id = token.id
          }
        }

        id match {
          case ScalaTokenId.STRING_BEGIN =>
            return Array(ts.offset, ts.offset + token.length)
          case ScalaTokenId.STRING_END =>
            return Array(ts.offset, ts.offset + token.length)
          case ScalaTokenId.REGEXP_BEGIN =>
            return Array(ts.offset, ts.offset + token.length)
          case ScalaTokenId.REGEXP_END =>
            return Array(ts.offset, ts.offset + token.length)
          case ScalaTokenId.LParen =>
            return Array(ts.offset, ts.offset + token.length)
          case ScalaTokenId.RParen =>
            return Array(ts.offset, ts.offset + token.length)
          case ScalaTokenId.LBrace =>
            return Array(ts.offset, ts.offset + token.length)
          case ScalaTokenId.RBrace =>
            return Array(ts.offset, ts.offset + token.length)
          case ScalaTokenId.LBracket =>
            return Array(ts.offset, ts.offset + token.length)
          //            } else if (id == ScalaTokenId.DO && !ScalaLexUtil.isEndmatchingDo(doc, ts.offset())) {
          //                // No matching dot for "do" used in conditionals etc.
          //                return OffsetRange.NONE;
          case ScalaTokenId.RBracket =>
            return Array(ts.offset, ts.offset + token.length)
          //            } else if (id.primaryCategory().equals("keyword")) {
          //                if (ScalaLexUtil.isBeginToken(id, doc, ts)) {
          //                    return ScalaLexUtil.findEnd(doc, ts);
          //                } else if ((id == ScalaTokenId.END) || ScalaLexUtil.isIndentToken(id)) { // Find matching block
          //
          //                    return ScalaLexUtil.findBegin(doc, ts);
          //                }
          case _ =>
        }
      }

      null
    } finally {
      //doc.readUnlock
    }
  }

  @throws(classOf[InterruptedException])
  @throws(classOf[BadLocationException])
  override def findMatches: Array[Int] = {
    var offset = context.getSearchOffset
    val doc = context.getDocument.asInstanceOf[BaseDocument]

    //doc.readLock
    try {
      ScalaLexUtil.getTokenSequence(doc, offset) foreach { ts =>
        ts.move(offset)
        if (!ts.moveNext) {
          return null
        }

        var token = ts.token
        if (token eq null) {
          return null
        }

        var id = token.id

        if (id == ScalaTokenId.Ws) {
          // ts.move(offset) gives the token to the left of the caret.
          // If you have the caret right at the beginning of a token, try
          // the token to the right too - this means that if you have
          //  "   |def" it will show the matching "end" for the "def".
          offset += 1
          ts.move(offset)
          if (ts.moveNext && ts.offset <= offset) {
            token = ts.token
            id = token.id
          }
        }

        id match {
          case ScalaTokenId.STRING_BEGIN =>
            val range = ScalaLexUtil.findFwd(ts, ScalaTokenId.STRING_BEGIN, ScalaTokenId.STRING_END)
            return Array(range.getStart, range.getEnd)
          case ScalaTokenId.STRING_END =>
            val range = ScalaLexUtil.findBwd(ts, ScalaTokenId.STRING_BEGIN, ScalaTokenId.STRING_END);
            return Array(range.getStart, range.getEnd)
          case ScalaTokenId.REGEXP_BEGIN =>
            val range = ScalaLexUtil.findFwd(ts, ScalaTokenId.REGEXP_BEGIN, ScalaTokenId.REGEXP_END);
            return Array(range.getStart, range.getEnd)
          case ScalaTokenId.REGEXP_END =>
            val range = ScalaLexUtil.findBwd(ts, ScalaTokenId.REGEXP_BEGIN, ScalaTokenId.REGEXP_END);
            return Array(range.getStart, range.getEnd)
          case ScalaTokenId.LParen =>
            val range = ScalaLexUtil.findFwd(ts, ScalaTokenId.LParen, ScalaTokenId.RParen);
            return Array(range.getStart, range.getEnd)
          case ScalaTokenId.RParen =>
            val range = ScalaLexUtil.findBwd(ts, ScalaTokenId.LParen, ScalaTokenId.RParen);
            return Array(range.getStart, range.getEnd)
          case ScalaTokenId.LBrace =>
            val range = ScalaLexUtil.findFwd(ts, ScalaTokenId.LBrace, ScalaTokenId.RBrace);
            return Array(range.getStart, range.getEnd)
          case ScalaTokenId.RBrace =>
            val range = ScalaLexUtil.findBwd(ts, ScalaTokenId.LBrace, ScalaTokenId.RBrace);
            return Array(range.getStart, range.getEnd)
          case ScalaTokenId.LBracket =>
            val range = ScalaLexUtil.findFwd(ts, ScalaTokenId.LBracket, ScalaTokenId.RBracket);
            return Array(range.getStart, range.getEnd)
          //            } else if (id == ScalaTokenId.DO && !ScalaLexUtil.isEndmatchingDo(doc, ts.offset())) {
          //                // No matching dot for "do" used in conditionals etc.
          //                return OffsetRange.NONE;
          case ScalaTokenId.RBracket =>
            val range = ScalaLexUtil.findBwd(ts, ScalaTokenId.LBracket, ScalaTokenId.RBracket);
            return Array(range.getStart, range.getEnd)
          //            } else if (id.primaryCategory().equals("keyword")) {
          //                if (ScalaLexUtil.isBeginToken(id, doc, ts)) {
          //                    return ScalaLexUtil.findEnd(doc, ts);
          //                } else if ((id == ScalaTokenId.END) || ScalaLexUtil.isIndentToken(id)) { // Find matching block
          //
          //                    return ScalaLexUtil.findBegin(doc, ts);
          //                }
          case _ =>
        }
      }

      null
    } finally {
      //doc.readUnlock
    }
  }
*/
}
