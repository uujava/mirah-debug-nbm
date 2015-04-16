/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.weblite.netbeans.mirah.cc;

import ca.weblite.netbeans.mirah.LOG;
import ca.weblite.netbeans.mirah.lexer.DocumentQuery;
import ca.weblite.netbeans.mirah.lexer.MirahLanguageHierarchy;
import ca.weblite.netbeans.mirah.lexer.MirahParser.DocumentDebugger;
import ca.weblite.netbeans.mirah.lexer.MirahTokenId;
import ca.weblite.netbeans.mirah.lexer.SourceQuery;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import mirah.impl.Tokens;
import mirah.lang.ast.FieldDeclaration;
import mirah.lang.ast.Node;
import mirah.lang.ast.NodeFilter;
import mirah.lang.ast.NodeScanner;
import mirah.lang.ast.Position;
import org.mirah.typer.ResolvedType;

import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.java.classpath.ClassPath;

import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.editor.BaseDocument;

import org.netbeans.modules.editor.NbEditorUtilities;
import org.netbeans.spi.editor.completion.CompletionProvider;
import org.netbeans.spi.editor.completion.CompletionTask;
import org.netbeans.spi.editor.completion.support.AsyncCompletionTask;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.filesystems.FileObject;


/**
 *
 * @author shannah
 */
@MimeRegistration(mimeType="text/x-mirah", service=CompletionProvider.class)
public class MirahCodeCompleter implements CompletionProvider {
    
    static FieldDeclaration[] findFields(final DocumentDebugger dbg, final int rightEdge, final boolean isClassVar){
        final ArrayList<FieldDeclaration> foundNodes = new ArrayList<FieldDeclaration>();
        for( Object node : dbg.compiler.compiler().getParsedNodes() ){
            if ( node instanceof Node ){
                ((Node)node).accept(new NodeScanner(){
                    @Override
                    public boolean enterFieldDeclaration(FieldDeclaration node, Object arg) {
                        if ( node.isStatic() == isClassVar ){
                            foundNodes.add(node);
                        }
                        LOG.info(this,"findFields fields = "+super.enterFieldDeclaration(node, arg));

                        return super.enterFieldDeclaration(node, arg);
                    }
                    
                    
                  
                }, null);
            }
        };
        LOG.info(MirahCodeCompleter.class,"findFields = [[]");
        
        return foundNodes.toArray(new FieldDeclaration[0]);
    }
    public static Node findNode(final DocumentDebugger dbg, final int rightEdge){
        final Node[] foundNode = new Node[1];
        for( Object node : dbg.compiler.compiler().getParsedNodes() ){
            if ( node instanceof Node ){
                ((Node)node).accept(new NodeScanner(){

                    @Override
                    public boolean enterDefault(Node node, Object arg) {
                        if ( node != null ){
                            
                            Position nodePos = node.position();
                            ResolvedType type = dbg.getType(node);
                            
                        if ( nodePos != null )
                        LOG.info(MirahCodeCompleter.class,"enterDefault node = " + node + " ["+nodePos.startLine() +"," + nodePos.startChar()+"," + nodePos.startColumn()+"-"+nodePos.endLine()+","+nodePos.endChar()+"," + nodePos.endColumn()+"] type ="+type+" rightEdge="+rightEdge);
                        else
                        LOG.info(MirahCodeCompleter.class,"enterDefault node = " + node + " pos="+nodePos +" type ="+type+" rightEdge="+rightEdge);
                            if ( type != null && nodePos != null && nodePos.endChar() == rightEdge ){
                                foundNode[0] = node;
                            } else if ( nodePos != null && nodePos.endChar() == rightEdge ){
                                
                            } else {
                                
                            }
                        } 
                        return super.enterDefault(node, arg); //To change body of generated methods, choose Tools | Templates.
                    }
                }, null);
                //walkTree((Node)node);

            }
        };
        LOG.info(MirahCodeCompleter.class,"findNode node[0] = " + foundNode[0]);
        return foundNode[0];
    }
    
    //@Override
    public CompletionTask createTask2(int queryType, final JTextComponent jtc) {
        
        LOG.info(this,"createTask2 queryType = " + queryType);
        
        if ( queryType == CompletionProvider.COMPLETION_QUERY_TYPE){
            //System.out.println("Request for completion query");
        } else if ( queryType == CompletionProvider.DOCUMENTATION_QUERY_TYPE){
            //System.out.println("Request for documentation");
        } else if ( queryType == CompletionProvider.TOOLTIP_QUERY_TYPE){
            //System.out.println("Request for tooltip");
        }
        
        FileObject fileObject = NbEditorUtilities.getFileObject(jtc.getDocument());
        LOG.info(this,"createTask2 fileObject = " + fileObject);
        int caretOffset = jtc.getCaretPosition();
        
        DocumentQuery dq = new DocumentQuery(jtc.getDocument());
        SourceQuery sq = new SourceQuery(jtc.getDocument());
        
        TokenSequence<MirahTokenId> seq = dq.getTokens(caretOffset, true);
        if ( seq.token() != null ){
            int startPos = seq.offset();
            int len = seq.token().length();
            //System.out.println("Start: "+startPos+" len "+len+" caret "+caretOffset);
            LOG.info(this,"Start: "+startPos+" len "+len+" caret "+caretOffset);
            
            if ( seq.token().id().ordinal() == Tokens.tIDENTIFIER.ordinal()){
                //String id = seq.token().text().toString();
                //while ( seq.movePrevious() && MirahTokenId.WHITESPACE_AND_COMMENTS.contains(seq.token().id())){}
                String type = dq.guessType(seq, fileObject);
                //System.out.println("Guessed type "+type);
                LOG.info(this,"Guessed type "+type);
                
                SourceQuery method = sq.findMethod(caretOffset);
                //System.out.println("Finding local var "+seq.token().text());
                LOG.info(this,"Finding local var "+seq.token().text());
                SourceQuery localVar = method.findLocalVars(String.valueOf(seq.token().text()));
                if ( localVar.size() > 0 ){
                    //System.out.println("Found local var "+localVar.getType());
                    LOG.info(this,"Found local var "+localVar.getType());
                }
                
            } else if ( seq.token().id().ordinal() == Tokens.tCONSTANT.ordinal()){
                //System.out.println("Constant");
                LOG.info(this,"Constant");
            } else {
                //System.out.println("other");
                LOG.info(this,"other");
            }
        }
        
        
        
        
        return null;
    }
    
    
    @Override
    public CompletionTask createTask(int queryType, final JTextComponent jtc) {
        LOG.info(this,"createTask queryType = " + queryType);
        if ( queryType != CompletionProvider.COMPLETION_QUERY_TYPE){
            
            return null;
        }
        final int initialOffset = jtc.getCaretPosition();
        try {
            int caretOffset = jtc.getCaretPosition();
            LOG.info(this,"createTask caretOffset = " + caretOffset);
        
            int p = caretOffset-1;
            if ( p < 0 ){
                return null;
            }
            String lastChar = jtc.getDocument().getText(p, 1);
            FileObject fileObject = NbEditorUtilities.getFileObject(jtc.getDocument());
            LOG.info(this,"createTask fileObject = " + fileObject);
            while ( p > 0 && lastChar.trim().isEmpty()){
                p--;
                lastChar = jtc.getDocument().getText(p, 1);
            }
            
            TokenSequence<MirahTokenId> toks = mirahTokenSequence(jtc.getDocument(), caretOffset, true);
            // Tokens that activate code completion
            MirahTokenId tWhitespace = MirahTokenId.WHITESPACE;
            MirahTokenId tComment = MirahTokenId.get(Tokens.tComment.ordinal());
            MirahTokenId tIdentifier = MirahTokenId.get(Tokens.tIDENTIFIER.ordinal());
            MirahTokenId tInstanceVar = MirahTokenId.get(Tokens.tInstVar.ordinal());
            MirahTokenId tClassVar = MirahTokenId.get(Tokens.tClassVar.ordinal());
            MirahTokenId tAt = MirahTokenId.get(Tokens.tAt.ordinal());
            MirahTokenId tDot = MirahTokenId.get(Tokens.tDot.ordinal());
            MirahTokenId tDef = MirahTokenId.get(Tokens.tDef.ordinal());
            
            Set<MirahTokenId> activators = new HashSet<MirahTokenId>();
            activators.add(tDot);
            activators.add(tAt);
            activators.add(tInstanceVar);
            activators.add(tDef);
            activators.add(tClassVar);
            
            Token<MirahTokenId> activator = null;
            int activatorOffset = -1;
            int activatorLen = -1;
            boolean hasWhitespace = false;
            boolean hasIdentifier = false;

            TokenSequence<MirahTokenId> toks3 = mirahTokenSequence(jtc.getDocument(), caretOffset, true);
            while( true )
            {
                String text = toks3.token().text().toString();
                if ( text != null ) text = text.replaceAll("\n","\\n");
                LOG.info(this,"toks3: " + toks3.token().id().name()+" text:"+ text);
                if ( ! toks3.moveNext() ) break;
            }
            while ( toks.token().id() == tIdentifier || toks.token().id() == tComment || toks.token().id() == tWhitespace || toks.token().id().ordinal() == MirahLanguageHierarchy.METHOD_DECLARATION || activators.contains(toks.token().id()) )
            {
                Token<MirahTokenId> curr = toks.token();
                LOG.info(this,"createTask curr = " + curr);
                if ( curr.id() == tWhitespace || curr.id() == tComment){
                    hasWhitespace = true;
                } else if ( curr.id() == tIdentifier || curr.id().ordinal() == MirahLanguageHierarchy.METHOD_DECLARATION ){
                    hasIdentifier = true;
                } else {
                    activator = toks.token();
                    activatorOffset = toks.offset();
                    activatorLen = curr.length();
                    break;
                }
                if ( !toks.movePrevious() ){
                    break;
                }
            }
            LOG.info(this,"createTask activator = " + activator);
            if ( activator == null ){
                return null;
            }
            LOG.info(this,"createTask activator.id = " + activator.id());
            
            if ( activator.id() == tAt || activator.id() == tInstanceVar || activator.id() == tClassVar ){
                if ( hasWhitespace ){
                    return null;
                }
                return new AsyncCompletionTask(new PropertyCompletionQuery(activatorOffset-activatorLen), jtc);
            
                
            } else if ( activator.id() == tDot ){
                if ( hasWhitespace && hasIdentifier ){
                    return null;
                }
                LOG.info(this,"createTask activator.id = tDot");
                
                return new AsyncCompletionTask(new MethodCompletionQuery(initialOffset, fileObject), jtc);
                
                
            } else if ( activator.id() == tDef ){
                LOG.info(this,"createTask activator.id = tDef");
                return new AsyncCompletionTask(new DefCompletionQuery(initialOffset), jtc);
            }
            
        } catch ( BadLocationException ble){
            return null;
        }
        LOG.info(this,"createTask return null");
        
        return null;
        
    }

    @Override
    public int getAutoQueryTypes(JTextComponent jtc, String string) {
        return 0;
    }
    
    static Class findClass(FileObject o, String name){
        LOG.info(MirahCodeCompleter.class,"findClass o="+o+" name="+name);
        return findClass(o, name, true);
    }
    
    static Class findClass(FileObject o, String name, boolean cache){
        Project proj = FileOwnerQuery.getOwner(o);
        FileObject projectDirectory = proj.getProjectDirectory();
        LOG.info(MirahCodeCompleter.class,"findClass o=" + o + " name=" + name);
        LOG.info(MirahCodeCompleter.class,"findClass proj=" + proj + " projectDirectory=" + projectDirectory);
        
        ClassPath[] paths = new ClassPath[]{
            ClassPath.getClassPath(o, ClassPath.SOURCE),
            ClassPathSupport.createClassPath(new File(projectDirectory.getPath(), "src").getPath()),
            ClassPath.getClassPath(o, ClassPath.EXECUTE),
            ClassPath.getClassPath(o, ClassPath.COMPILE),
            ClassPath.getClassPath(o, ClassPath.BOOT),
            
        };
        
        for ( int i=0; i<paths.length; i++){
            ClassPath cp = paths[i];
            LOG.info(MirahCodeCompleter.class,"findClass cp["+i+"] =" + cp);
            
            try {
                
                Class c = cp.getClassLoader(true).loadClass(name);
                if ( c != null ){
                    LOG.info(MirahCodeCompleter.class,"findClass c =" + c);
                    return c;
                }
            } catch ( ClassNotFoundException ex){
                
            }
        }
        
        LOG.info(MirahCodeCompleter.class,"findClass NULL");
        
        return null;
    }
    
    private static void walkTree(Node node){
        NodeFilter f = new NodeFilter(){

            @Override
            public boolean matchesNode(Node node) {
                
                return true;
            }
            
        };
        
        List nodes = node.findChildren(f);
       
        for ( Object o : nodes ){
            if ( o instanceof Node ){
                walkTree((Node)o);
            }
        }
        
    }
    
    private static String nodeToString(Node n){
        if ( n == null || n.position() == null ){
            if ( n != null ){
                return ""+n;
            }
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Node ").append(n)
                .append(n.position().startLine())
                .append(".")
                .append(n.position().startColumn())
                .append(":")
                .append(n.position().startChar())
                .append("-")
                .append(n.position().endLine())
                .append(".")
                .append(n.position().endColumn())
                .append(":")
                .append(n.position().endChar())
                .append(" # ");
        
       return sb.toString();
                
    }
    
    
     static TokenSequence<MirahTokenId> mirahTokenSequence(Document doc, int caretOffset, boolean backwardBias) {
        BaseDocument bd = (BaseDocument)doc;
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
    
     static int getEndOfLine(Document doc, int caretOffset){
        BaseDocument bd = (BaseDocument)doc;
        bd.readLock();
        try {
            TokenHierarchy<?> hi = TokenHierarchy.get(doc);
            TokenSequence<MirahTokenId> toks = mirahTokenSequence(doc, caretOffset, false);
            MirahTokenId eol = MirahTokenId.get(Tokens.tNL.ordinal());
            MirahTokenId eof = MirahTokenId.get(Tokens.tEOF.ordinal());
            while ( !eol.equals(toks.token().id()) && !eof.equals(toks.token().id())){
                if ( !toks.moveNext() ){
                    break;
                }
            }
            int off = toks.token().offset(hi);
            return off;
        } finally {
            bd.readUnlock();
        }
        
        
    }
    
     static int getBeginningOfLine(Document doc, int caretOffset){
        BaseDocument bd = (BaseDocument)doc;
        bd.readLock();
        
         LOG.info(MirahCodeCompleter.class,"getBeginningOfLine doc=" + doc + " caretOffset =" + caretOffset);
        
        try {
            TokenHierarchy<?> hi = TokenHierarchy.get(doc);
            TokenSequence<MirahTokenId> toks = mirahTokenSequence(doc, caretOffset, true);
            MirahTokenId eol = MirahTokenId.get(Tokens.tNL.ordinal());
            //MirahTokenId eof = MirahTokenId.get(Tokens.tEOF.ordinal());
            while ( !eol.equals(toks.token().id())){
                if ( !toks.movePrevious() ){
                    break;
                }
            }
            int off = toks.token().offset(hi)+toks.token().length();
            
            return off;
        } finally {
            bd.readUnlock();
        }
    }
    
    
    
    
    
    
    
    
}
