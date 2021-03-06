package ru.programpark.mirah.typinghooks;

import ru.programpark.mirah.lexer.DocumentQuery;
import ru.programpark.mirah.lexer.MirahTokenId;
import java.util.prefs.Preferences;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import mirah.impl.Tokens;
import org.netbeans.api.editor.mimelookup.MimePath;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.editor.BaseDocument;
import org.netbeans.modules.editor.indent.spi.CodeStylePreferences;
import org.netbeans.spi.editor.typinghooks.TypedBreakInterceptor;
//import java.awt.event.ActionEvent;
//import javax.swing.text.TextAction;
//import org.openide.util.Lookup;
//import org.netbeans.api.editor.mimelookup.MimeLookup;

/**
 *
 * @author shannah
 */
public class MirahTypedBreakInterceptor implements TypedBreakInterceptor{
    
    @Override
    public boolean beforeInsert(Context cntxt) throws BadLocationException {
        return false;
    }

    /**
     * Вызов метода всегда связан с переводом строки - пользователь нажал <Enter> в конце некоторой строки.
     * 
     * @param context
     * @throws BadLocationException 
     */
    @Override
    public void insert(MutableContext context) throws BadLocationException {
        int dotPos = context.getCaretOffset();
        Document doc = context.getDocument();
        DocumentQuery dq = new DocumentQuery(doc);
        TokenSequence<MirahTokenId> seq = dq.getTokens(dotPos, false);
        BaseDocument baseDoc = (BaseDocument) context.getDocument();
        
        if (MirahTypingCompletion.startTokenOffset(baseDoc, dotPos, Tokens.tLBrace) >= 0) {
            completeByRightBrace(baseDoc, dotPos, context);
        } else if (MirahTypingCompletion.isStartToken(baseDoc, dotPos)) {
            completeByEnd(baseDoc, dotPos, context);
        } else if (MirahTypingCompletion.blockCommentCompletion(context)) {
            // Перехват и завершение начала java-комментариев видов /* и /**
            blockCommentComplete(doc, dotPos, context);
        } else if (seq.token() != null && (seq.token().id().is(Tokens.tComment) || seq.token().id().is(Tokens.tJavaDoc))) {
            // К сожалению, это не работает...
            boolean inJavadoc = seq.token().id().is(Tokens.tJavaDoc);
//            boolean inJavadoc = seq.token().id().ordinal() == Tokens.tJavaDoc.ordinal();
            String comment = seq.token().text().toString().trim();
            if (comment.startsWith("/*") && comment.endsWith("*/")) {
                blockCommentExtend(doc, dotPos, context);
            } else if (comment.startsWith("#")) {
                lineCommentExtend(doc, dotPos, context);
            } else {
                // Во всех остальных случаях просто выравниваем отступ по предыдущей строке
                createIndent(baseDoc, dotPos, context);                
            }
        } else {
            // Во всех остальных случаях просто выравниваем отступ по предыдущей строке
            createIndent(baseDoc, dotPos, context);
        }
    }

    private void completeByEnd(BaseDocument doc, int dotPos, MutableContext context) throws BadLocationException {
        DocumentQuery dq = new DocumentQuery(doc);
        int bol = dq.getBOL(dotPos - 1);
        int eol = dq.getEOL(bol);

        // Отступ в пробелах от начала строки
        int indent = 0;
        for (int i = bol; i > 0 && i < eol && Character.isWhitespace(doc.getText(i, 1).charAt(0)); i++, indent++);
        // Если курсор стоял в серии пробелов до начала значимого контента строки - ничего не делаем, работают 
        // обычные правила переноса строки.
        if (bol + indent >= dotPos) {
            return;
        }
        
        // Если же курсор находился уже внутри значимых тегов строки - обрабатываем.
        int spacesPerTab = getSpacesPerTab(doc);
        StringBuilder sb = new StringBuilder();
        sb.append('\n');
        for (int j = 0; j < indent; j++) { sb.append(' '); }
        int cursorPos = sb.length();
        // Далее вопрос - либо автозавершение end с выставлением каретки в определенную позицию, либо только выставление 
        // каретки. Метод возвращает true в случае нарушения баланса, однако не всегда при нарушенном балансе мы добавляем end.
        if (MirahTypingCompletion.isSkipEnd(doc, dotPos)) {
            // Ничего не добавляем.
            System.out.println("Skip end");
        } else if (MirahTypingCompletion.isAddEnd(doc, dotPos)) {
            for (int j = 0; j < spacesPerTab; j++) { sb.append(' '); }
            cursorPos = sb.length();
            // Добавляем end только в случае, если Enter нажат в конце строки, возможно в блоке незначащих пробельных символов.
            // Незначащие (пробельные) символы в конце строки.
            int suffix = 0;
            for (int i = eol - 1; i > 0 && i > bol && Character.isWhitespace(doc.getText(i, 1).charAt(0)); i--, suffix++);
            if (eol - suffix <= dotPos) {
                if (MirahTypingCompletion.startTokenOffset(doc, dotPos, Tokens.tBegin) >= 0) {
                    sb.append('\n');
                    for (int j = 0; j < indent; j++) { sb.append(' '); }
                    sb.append("rescue Exception => e\n");
                    for (int j = 0; j < indent + spacesPerTab; j++) { sb.append(' '); }
                    sb.append("e.printStackTrace");
                }
                sb.append('\n');
                for (int j = 0; j < indent; j++) { sb.append(' '); }
                sb.append("end");
            }
        } else {
            for (int j = 0; j < spacesPerTab; j++) { sb.append(' '); }
            cursorPos = sb.length();            
        }
        // Одновременно выставляем позицию каретки
        context.setText(sb.toString(), 0, cursorPos);
    }

    private void completeByRightBrace(BaseDocument doc, int dotPos, MutableContext context) throws BadLocationException {
        DocumentQuery dq = new DocumentQuery(doc);
        int bol = dq.getBOL(dotPos - 1);
        int eol = dq.getEOL(bol);

        // Отступ в пробелах от начала строки
        int indent = 0;
        for (int i = bol; i > 0 && i < eol && Character.isWhitespace(doc.getText(i, 1).charAt(0)); i++, indent++);
        // Если курсор стоял в серии пробелов до начала значимого контента строки - ничего не делаем, работают 
        // обычные правила переноса строки.
        if (bol + indent >= dotPos) {
            return;
        }
        
        int spacesPerTab = getSpacesPerTab(doc);
        StringBuilder sb = new StringBuilder();
        sb.append('\n');
        for (int j = 0; j < indent + spacesPerTab; j++) { sb.append(' '); }
        int cursorPos = sb.length();
        // Далее вопрос - либо автозавершение end с выставлением каретки в определенную позицию, либо только выставление 
        // каретки. Метод возвращает true в случае нарушения баланса, однако не всегда при нарушенном балансе мы добавляем end.
        if (MirahTypingCompletion.isAddRightBrace(doc, dotPos)) {
            // Добавляем end только в случае, если Enter нажат в конце строки, возможно в блоке незначащих пробельных символов.
            // Незначащие (пробельные) символы в конце строки.
            int suffix = 0;
            for (int i = eol - 1; i > 0 && i > bol && Character.isWhitespace(doc.getText(i, 1).charAt(0)); i--, suffix++);
            if (eol - suffix <= dotPos) {
                sb.append('\n');
                for (int j = 0; j < indent; j++) { sb.append(' '); }
                sb.append('}');
            }
        }
        // Одновременно выставляем позицию каретки
        context.setText(sb.toString(), 0, cursorPos);
    }

    private int getSpacesPerTab(BaseDocument doc) {
        Preferences prefs = CodeStylePreferences.get(doc).getPreferences();
        // Размер табуляции соответствует настройкам.
        // int spacesPerTab = prefs.get(org.netbeans.api.editor.settings.SimpleValueNames.SPACES_PER_TAB, null);
        return prefs.getInt("spaces-per-tab", 2);
    }
    
    private void createIndent(BaseDocument doc, int dotPos, MutableContext context) throws BadLocationException {
        DocumentQuery dq = new DocumentQuery(doc);
        int bol = dq.getBOL(dotPos - 1);
        int eol = dq.getEOL(bol);

        int indent = 0;
        for (int i = bol; i > 0 && i < eol && Character.isWhitespace(doc.getText(i, 1).charAt(0)); i++, indent++);
        // Если курсор стоял в серии пробелов до начала значимого контента строки - ничего не делаем, работают 
        // обычные правила переноса строки.
        if (bol + indent >= dotPos) {
            if (bol + indent == eol) {
                // Для пустой, а точнее состоящей из пробелов, строки нужна дополнительная обработка.
                if (dotPos != eol) {
                    return;
                }
            } else {
                // Если строка в принципе не пустая.
                return;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append('\n');
        for (int j = 0; j < indent; j++) { sb.append(' '); }
        // Определяем наличие или отсутствие одного из интересующих токенов в строке в принципе
        int offset = MirahTypingCompletion.indentTokenOffset(doc, dotPos);
        if (offset > 0) {
            // Если каретка расположена перед токеном - ничего не делаем, работают обычные правила переноса строки.
            if (offset >= dotPos) {
                return;
            }
            int spacesPerTab = getSpacesPerTab(doc);
            for (int j = 0; j < spacesPerTab; j++) { sb.append(' '); }                
        }
        context.setText(sb.toString(), 0, sb.length());        
    }
    
    private void lineCommentExtend(Document doc, int dotPos, MutableContext context) throws BadLocationException {
        DocumentQuery dq = new DocumentQuery(doc);
        int bol = dq.getBOL(dotPos - 1);
        int eol = dq.getEOL(bol);
        // Если каретка расположена до начала комментария - ничего не делаем.
        TokenSequence<MirahTokenId> seq = dq.getTokens(dotPos, false);
        String comment = seq.token().text().toString();
        if (bol + comment.indexOf("#") >= dotPos) {
            return;
        }

        int indent = 0;
        for (int i = bol; i > 0 && i < eol && Character.isWhitespace(doc.getText(i, 1).charAt(0)); i++, indent++);

        StringBuilder sb = new StringBuilder();
        sb.append('\n');
        for (int j = 0; j < indent; j++) { sb.append(' '); }
        // Если при нажатии <Enter> курсор стоит внутри строки комментария - добавляем префикс # и переносим 
        // остаток комментария на следующую строку
        if (dotPos < eol) {
            sb.append("# ");
        }
        context.setText(sb.toString(), 0, sb.length());
    }   
    
    private void blockCommentExtend(Document doc, int dotPos, MutableContext context) throws BadLocationException {        
        DocumentQuery dq = new DocumentQuery(doc);
        int bol = dq.getBOL(dotPos - 1);
        int eol = dq.getEOL(bol);
        // Если каретка расположена до начала комментария - ничего не делаем.
        TokenSequence<MirahTokenId> seq = dq.getTokens(dotPos, false);
        String comment = seq.token().text().toString();
        if (bol + comment.indexOf("/*") >= dotPos || bol + comment.indexOf("*") >= dotPos) {
            return;
        }
                
        int indent = 0;
        int i = bol;
        for ( ; i > 0 && i < eol && Character.isWhitespace(doc.getText(i, 1).charAt(0)); i++, indent++); 
        // Возможно вследствие алгоритма нахождения bol в первой строке документа.
        if (i < 0) {
            i = 0;
        }
        if ('/' == doc.getText(i, 1).charAt(0)) {
            indent++; // т.к. '*'-ки стоят на одной линии, а первая строка начинается со '/'
        }

        StringBuilder sb = new StringBuilder();
        sb.append('\n');
        for (int j = 0; j < indent; j++) { sb.append(' '); }
        sb.append("* ");
        context.setText(sb.toString(), 0, sb.length());
    }
    
    private void blockCommentComplete(Document doc, int dotPos, MutableContext context) throws BadLocationException {
        DocumentQuery dq = new DocumentQuery(doc);
        int bol = dq.getBOL(dotPos - 1);
        int eol = dq.getEOL(bol);
        
        int indent = 1; // т.к. '*'-ки стоят на одной линии, а первая строка начинается со '/'
        for (int i = bol; i > 0 && i < eol && Character.isWhitespace(doc.getText(i, 1).charAt(0)); i++, indent++);
        
        StringBuilder sb = new StringBuilder();
        sb.append('\n');
        for (int j = 0; j < indent; j++) { sb.append(' '); }
        sb.append("* \n");
        int cursorPos = sb.length() - 1;
        for (int j = 0; j < indent; j++) { sb.append(' '); }
        sb.append("*/");
        context.setText(sb.toString(), 0, cursorPos);        
    }
       
    @Override
    public void afterInsert(Context context) throws BadLocationException {
//        if (isJavadocTouched) {
//            Lookup.Result<TextAction> res = MimeLookup.getLookup(MimePath.parse("text/x-javadoc")).lookupResult(TextAction.class); // NOI18N
//            ActionEvent newevt = new ActionEvent(context.getComponent(), ActionEvent.ACTION_PERFORMED, "fix-javadoc"); // NOI18N
//            for (TextAction action : res.allInstances()) {
//                action.actionPerformed(newevt);
//            }
//            isJavadocTouched = false;
//        }
    }

    @Override
    public void cancelled(Context cntxt) {        
    }        
  
    @MimeRegistration(mimeType = "text/x-vruby", service = TypedBreakInterceptor.Factory.class)
    public static class MirahFactory implements TypedBreakInterceptor.Factory {

        @Override
        public TypedBreakInterceptor createTypedBreakInterceptor(MimePath mimePath) {
            return new MirahTypedBreakInterceptor();
        }
    }    
}
