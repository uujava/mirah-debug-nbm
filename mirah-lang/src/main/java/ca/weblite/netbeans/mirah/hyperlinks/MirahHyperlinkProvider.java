/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.weblite.netbeans.mirah.hyperlinks;

import ca.weblite.netbeans.mirah.LOG;
import ca.weblite.netbeans.mirah.cc.MirahCodeCompleter;
import ca.weblite.netbeans.mirah.lexer.MirahLanguageHierarchy;
import ca.weblite.netbeans.mirah.lexer.MirahParser;
import ca.weblite.netbeans.mirah.lexer.MirahTokenId;
import ca.weblite.netbeans.mirah.lexer.SourceQuery;
import java.io.File;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import javax.swing.text.Document;
import mirah.impl.Tokens;
import mirah.lang.ast.FieldDeclaration;
import mirah.lang.ast.Node;
import org.mirah.typer.ResolvedType;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.editor.BaseDocument;
import org.netbeans.lib.editor.hyperlink.spi.HyperlinkProviderExt;
import org.netbeans.lib.editor.hyperlink.spi.HyperlinkType;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;

/**
 *
 * @author shannah
 */
//@MimeRegistration(mimeType = "text/x-mirah", service = HyperlinkProviderExt.class)
public class MirahHyperlinkProvider implements HyperlinkProviderExt  {

    
    private enum LinkType {
        Constant,
        Identifier,
        InstanceVar,
        ClassVar
    }
    
    private int spanStart = -1;
    private int spanEnd = -1;
    private LinkType linkType;
    private String target;
    
    @Override
    public Set<HyperlinkType> getSupportedHyperlinkTypes() {
        
        return EnumSet.of(HyperlinkType.GO_TO_DECLARATION);
    }

    @Override
    public boolean isHyperlinkPoint(Document doc, int offset, HyperlinkType type) {
        return getHyperlinkSpan(doc, offset, type) != null;
    }

    @Override
    public int[] getHyperlinkSpan(Document dcmnt, int i, HyperlinkType ht) {
        if ( verifyState(dcmnt, i)){
            return new int[]{spanStart, spanEnd};
        } else {
            return null;
        }
    }
    static String project_dir = "c:\\mirah-debug\\mavenproject1";

    @Override
    public void performClickAction(Document doc, int offset, HyperlinkType type) {
        
//    Collection<? extends ProjectFactory> f = Lookup.getDefault().lookupAll(ProjectFactory.class);
//    Object[] o = f.toArray();
//    for( int i = 0 ; i < o.length ; i++ )
//        System.out.println("o["+i+"] ="+o[i]);
//        
//    Project p1 = null;
//    try {
//        FileObject fo = FileUtil.createFolder(new File(project_dir));
//        p1 = ProjectManager.getDefault().findProject(fo);
//    }
//    catch( Exception e )
//    {
//        e.printStackTrace();
//    }
//        
        LOG.info(this,"performClickAction doc="+doc+" offset="+offset+" type="+type);
        switch (type) {
            case GO_TO_DECLARATION:
//                GoToSupport.goTo(doc, offset, false);
                break;
            case ALT_HYPERLINK:
//                JTextComponent focused = EditorRegistry.focusedComponent();
//                
//                if (focused != null && focused.getDocument() == doc) {
//                    focused.setCaretPosition(offset);
//                    GoToImplementation.goToImplementation(focused);
//                }
                break;
        }         
    }

    @Override
    public String getTooltipText(Document dcmnt, int i, HyperlinkType ht) {
        SourceQuery sq = new SourceQuery(dcmnt);
        if ( verifyState(dcmnt, i)){
            String astResult = getTypeFromAST(dcmnt, spanStart, spanEnd);
            if ( astResult != null ){
                return astResult;
            }
            switch ( linkType ){
                case Constant:
                    return sq.getFQN(target, i);
                    
                    
                case InstanceVar:
                    sq = sq.findClass(i);
                    for ( Node n : sq.findFieldDefinitions(target)){
                        FieldDeclaration fd = (FieldDeclaration)n;
                        if ( fd.type() != null ){
                            return fd.type().typeref().name();
                        }
                    }
                    break;
                
                case Identifier:
                    sq = sq.findMethod(i);
                    sq = sq.findLocalVars(target);
                    String type = sq.getType();
                    if ( type != null ){
                        return type;
                    } 
                    break;
                    
            }
        }
        return null;
    }
    
    
    public void resetState(){
        spanStart = -1;
        spanEnd = -1;
        linkType = null;
        target = null;
    }
    
    public boolean verifyState(Document doc, int offset) {
        resetState();
        BaseDocument bdoc = (BaseDocument)doc;
        bdoc.readLock();
        try {
            TokenHierarchy hi = TokenHierarchy.get(doc);
            TokenSequence<MirahTokenId> ts = hi.tokenSequence(MirahTokenId.getLanguage());
            if (ts != null) {
                ts.move(offset);
                ts.moveNext();
                Token<MirahTokenId> tok = ts.token();
                if ( tok.id().ordinal() == Tokens.tCONSTANT.ordinal()){
                    linkType = LinkType.Constant;
                } else if ( tok.id().ordinal() == Tokens.tIDENTIFIER.ordinal()){
                    linkType = LinkType.Identifier;
                } else if ( tok.id().ordinal() == MirahLanguageHierarchy.TYPE_HINT){
                    linkType = LinkType.Constant;
                } else if ( tok.id().ordinal() == Tokens.tInstVar.ordinal()){
                    linkType = LinkType.InstanceVar;
                } else if ( tok.id().ordinal() == Tokens.tClassVar.ordinal()){
                    linkType = LinkType.ClassVar;
                }
                
                if ( linkType != null ){
                    spanStart = tok.offset(hi);
                    target = tok.text().toString();
                    spanEnd = spanStart+target.length();
                    return true;
                }

            }
            return false;
        } finally {
            bdoc.readUnlock();
        }
    }
    
    private String getTypeFromAST(final Document doc, final int spanStart, final int spanEnd) {
        MirahParser.DocumentDebugger dbg = MirahParser.getDocumentDebugger(doc);

        if ( dbg != null ){


            ResolvedType type = null;
            Node foundNode = MirahCodeCompleter.findNode(dbg, spanEnd);

            if ( foundNode != null ){
                type = dbg.getType(foundNode);
            }

            if (type != null ){
                return type.name();
            }
            

        }
        
        return null;


    }
    
}
