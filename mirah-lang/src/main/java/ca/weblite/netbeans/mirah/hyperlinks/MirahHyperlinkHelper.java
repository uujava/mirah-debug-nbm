/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.weblite.netbeans.mirah.hyperlinks;

import ca.weblite.netbeans.mirah.cc.AstSupport;
import ca.weblite.netbeans.mirah.lexer.MirahParser;
import java.util.ArrayList;
import java.util.List;
import javax.swing.text.Document;
import mirah.lang.ast.Import;
import mirah.lang.ast.Node;
import org.netbeans.editor.BaseDocument;
import org.netbeans.modules.csl.spi.ParserResult;
import org.openide.filesystems.FileObject;
import ru.programpark.mirah.editor.ast.ASTUtils;
import ru.programpark.mirah.editor.ast.AstPath;
import ru.programpark.mirah.index.MirahIndex;
import ru.programpark.mirah.index.elements.IndexedClass;

/**
 *
 * @author savushkin
 */
public class MirahHyperlinkHelper {
 
    private MirahIndex index = null;
    
    public MirahHyperlinkHelper( FileObject fo )
    {
        if ( fo != null ) {
//            List<FileObject> roots = new ArrayList<FileObject>();
//            roots.add(fo);
//            index = MirahIndex.get(roots);

            index = MirahIndex.get(fo);
        }
    }
    public HyperlinkElement findByFqn( String fqn )
    {
        if ( index == null ) return null;
        IndexedClass ic = index.findClassByFqn(fqn);
        if ( ic == null ) 
        {
//            HyperlinkElement he = new HyperlinkElement(null,-1);
//            he.setFqn(fqn);
//            return he;
            return null;
        }
        String url = ic.getUrl();
        int offset = ic.getOffset();
        return new HyperlinkElement(url,offset);
    }
    public AstPath getPath(ParserResult parseResult, BaseDocument doc, int astOffset) {
        Node root = ASTUtils.getRoot(parseResult);

        // in some cases we can not repair the code, therefore root == null
        // therefore we can not complete. See # 131317
        if (root == null) {
            return null;
        }
        return new AstPath(root, astOffset, doc);
    }
    public HyperlinkElement analyze(Document doc, final ParserResult parserResult, final int offset)
    {
        AstPath path = getPath(parserResult,(BaseDocument)doc,offset);
        
        Node node = null;
        if ( parserResult instanceof MirahParser.NBMirahParserResult )
        {
           MirahParser.NBMirahParserResult pres = (MirahParser.NBMirahParserResult)parserResult;
//           node = pres.getRoot();
//            Node found = AstSupport.findByOffset(node,offset);
            Node found = AstSupport.findByOffset(pres,offset);
            if ( found instanceof Import )
            {
                Import imp = (Import)found;
                String fqn = imp.fullName().identifier();
                fqn = fqn.replace('.','/');
                return findByFqn(fqn);
            }
        }
        return null;
    }
    
}
