/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.weblite.netbeans.mirah.typinghooks;

import ca.weblite.netbeans.mirah.lexer.DocumentQuery;
import ca.weblite.netbeans.mirah.lexer.MirahTokenId;
import java.awt.event.ActionEvent;
import java.util.prefs.Preferences;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.TextAction;
import mirah.impl.Tokens;
import org.netbeans.api.editor.mimelookup.MimeLookup;
import org.netbeans.api.editor.mimelookup.MimePath;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.editor.mimelookup.MimeRegistrations;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.editor.BaseDocument;
import org.netbeans.modules.editor.indent.api.Indent;
import org.netbeans.modules.editor.indent.spi.CodeStylePreferences;
import org.netbeans.spi.editor.typinghooks.TypedBreakInterceptor;
import org.openide.util.Lookup;

/**
 *
 * @author shannah
 */
public class MirahTypedBreakInterceptor implements TypedBreakInterceptor{
    private boolean isJavadocTouched = false;
    
    
    
    @Override
    public boolean beforeInsert(Context cntxt) throws BadLocationException {
        return false;
    }

    @Override
    public void insert(MutableContext context) throws BadLocationException {
        int dotPos = context.getCaretOffset();
        Document doc = context.getDocument();
        DocumentQuery dq = new DocumentQuery(doc);
        TokenSequence<MirahTokenId> seq = dq.getTokens(dotPos, false);
        boolean inJavadoc = false;
        if ( seq.token() != null ){
            inJavadoc = (seq.token().id().ordinal() == Tokens.tJavaDoc.ordinal());
        }
        BaseDocument baseDoc = (BaseDocument) context.getDocument();
        if (MirahTypingCompletion.isAddRightBrace(baseDoc, dotPos)) {
            boolean insert[] = {true};
            int end = MirahTypingCompletion.getRowOrBlockEnd(baseDoc, dotPos, insert);
            if (insert[0]) {
                doc.insertString(end, "}", null); // NOI18N
                Indent.get(doc).indentNewLine(end);
            }
            context.getComponent().getCaret().setDot(dotPos);
        } else if (MirahTypingCompletion.isAddEnd(baseDoc, dotPos)){
            // Позиция начала строки для которой пытаемся добавить автозавершение end
            int caretRowStartOffset = org.netbeans.editor.Utilities.getRowStart(baseDoc, dotPos);
            TokenSequence<MirahTokenId> ts = DocumentQuery.mirahTokenSequence(doc, caretRowStartOffset, false);
            Preferences prefs = CodeStylePreferences.get(doc).getPreferences();
            // prefs.get(org.netbeans.api.editor.settings.SimpleValueNames.SPACES_PER_TAB, null); 
            // Размер табуляции соответствует настройкам.
            int spacesPerTab = prefs.getInt("spaces-per-tab", 2);
            String indent;
            // Токен пробелов\табуляций и т.п. до первого ключевого слова в обрабатываемой строке, к примеру, отступ перед def
            if (ts.token().id().is(Tokens.tComment)) {
                StringBuilder tab = new StringBuilder();
                for (int i = 0; i < spacesPerTab; i++) {
                    tab.append(' ');
                }
                Token<MirahTokenId> token = ts.token();
                indent = token.text().toString();
                // Чтобы правильно посчитать отступы
                indent = indent.replaceAll("\t", tab.toString());
            } else {
                // Текст начинается с начала строки, к примеру в определении класса или интерфейса
                indent = "";
            }   
            // Количество позиций на отступ
            int length = indent.length();
            StringBuilder text = new StringBuilder("\n");
            for (int i = 0; i < length + spacesPerTab; i++) {
                text.append(' ');
            }
            int caretPosition = text.length();
            text.append("\n");
            for (int i = 0; i < length; i++) {
                text.append(' ');
            }
            text.append("end");
            // Одновременно выставляем позицию каретки
            context.setText(text.toString(), 0, caretPosition);
        } else if (MirahTypingCompletion.blockCommentCompletion(context)) {
            blockCommentComplete(doc, dotPos, context);
        } else if ( inJavadoc ){
            context.setText("\n * ", 0, 4);
            //System.out.println("Adding *");
            
            //doc.insertString(context.getBreakInsertOffset(), "* ", null);
            
            //Indent.get(doc).indentNewLine(dotPos);
            //context.getComponent().getCaret().setDot(dotPos);
            //System.out.println("* added");
        }
        isJavadocTouched = MirahTypingCompletion.javadocBlockCompletion(context);
        if (isJavadocTouched) {
            blockCommentComplete(doc, dotPos, context);
        }
    }
    private void blockCommentComplete(Document doc, int dotPos, MutableContext context) throws BadLocationException {
        // note that the formater will add one line of javadoc
        //doc.insertString(dotPos, "*/", null); // NOI18N
        //Indent.get(doc).indentNewLine(dotPos);
        //context.getComponent().getCaret().setDot(dotPos);
        DocumentQuery dq = new DocumentQuery(doc);
        int bol = dq.getBOL(dotPos-1);
        int eol = dq.getEOL(bol);
        String sep = "\n";
        
        int indent = 1;
        int i =bol;
        while ( i > 0 && i< eol && Character.isWhitespace(doc.getText(i, 1).charAt(0) ) ){
            indent++;
            i++;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(sep);
        //for ( int j=0; j<indent; j++){
        //    sb.append(" ");
       // }
        sb.append(" * ").append(sep);
        int cursorPos = sb.length()-1;
        for ( int j=0; j<indent; j++){
            sb.append(" ");
        }
        sb.append("*/");
        String tx = sb.toString();
        context.setText(tx, 0, cursorPos);
        
    }


       

    @Override
    public void afterInsert(Context context) throws BadLocationException {
        /*if (isJavadocTouched) {
                Lookup.Result<TextAction> res = MimeLookup.getLookup(MimePath.parse("text/x-javadoc")).lookupResult(TextAction.class); // NOI18N
                ActionEvent newevt = new ActionEvent(context.getComponent(), ActionEvent.ACTION_PERFORMED, "fix-javadoc"); // NOI18N
                for (TextAction action : res.allInstances()) {
                    action.actionPerformed(newevt);
                }
                isJavadocTouched = false;
            }*/
    }

    @Override
    public void cancelled(Context cntxt) {
        
    }
    
    
  
    @MimeRegistration(mimeType = "text/x-mirah", service = TypedBreakInterceptor.Factory.class)
    public static class MirahFactory implements TypedBreakInterceptor.Factory {

        @Override
        public TypedBreakInterceptor createTypedBreakInterceptor(MimePath mimePath) {
            return new MirahTypedBreakInterceptor();
        }
    }
    
}
