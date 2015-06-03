/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.programpark.mirah.tests;

import ca.weblite.netbeans.mirah.LOG;
import ca.weblite.netbeans.mirah.hyperlinks.HyperlinkElement;
import ca.weblite.netbeans.mirah.lexer.MirahParser;
import ca.weblite.netbeans.mirah.lexer.MirahTokenId;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import mirah.lang.ast.Node;
import mirah.lang.ast.NodeScanner;
import org.mirah.typer.ResolvedType;
import org.netbeans.api.editor.EditorRegistry;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.editor.BaseDocument;
import org.netbeans.modules.csl.spi.ParserResult;
import org.netbeans.modules.parsing.api.ParserManager;
import org.netbeans.modules.parsing.api.ResultIterator;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.api.UserTask;
import org.netbeans.modules.parsing.api.indexing.IndexingManager;
import org.netbeans.modules.parsing.spi.Parser;
import org.netbeans.modules.parsing.spi.Parser.Result;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.Lookup;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import ru.programpark.mirah.index.MirahIndex;
import ru.programpark.mirah.index.elements.IndexedClass;

/**
 *
 * @author savushkin
 */
public class MirahTestActions {

//    static String project_dir = "c:/mirah-debug/mirah-dsl-parent/jfx-controller-framework-test/";
//    static String mirah_file_name = "c:\\mirah-debug\\mirah-dsl-parent\\jfx-controller-framework-test\\src\\main\\mirah\\ru\\programpark\\vector\\jfx\\controller\\framework\\BaseController.mirah";
    
    private static void dumpNodes( final MirahParser.NBMirahParserResult pres, final InputOutput io )
    {
        Node root = pres.getRoot();
        root.accept(new NodeScanner(){
          @Override
            public boolean enterDefault(Node node, Object arg) {
                ResolvedType type = pres.getResolvedType(node);
                if ( node != null && node.position() != null )
                    io.getOut().println(":"+node+"["+node.position().startChar()+","+node.position().endChar()+"] "+node.parent()+" type="+type);
                else
                    io.getOut().println(":"+node+" pos = null "+node.parent());
                return super.enterDefault(node, arg);
            }
        }, null);
    }
    
    public static void dumpTokens( Document doc, final InputOutput io )
    {
        TokenHierarchy<?> hi = TokenHierarchy.get(doc);
        TokenSequence<MirahTokenId> ts = LexUtilities.getMirahTokenSequence(doc, 0);
        if ( ts == null ) return;
        ts.moveStart();
        while( true )
        {
            String text = ts.token() != null ? ts.token().text().toString() : null;
            if ( text != null ) text = text.replaceAll("\n","\\n");
            if ( ts.token() != null )
            io.getOut().println(": " + ts.token().id().name()+" offset="+ts.token().offset(hi)+ " text:"+ text);
            if ( ! ts.moveNext() ) break;
        }
    }

    public static FileObject getFileObject(Document doc) {
        DataObject od = (DataObject) doc.getProperty(Document.StreamDescriptionProperty);
        
        return od != null ? od.getPrimaryFile() : null;
    }
    
    public static void dumpResolvedTypes( MirahParser.NBMirahParserResult pres, final InputOutput io )
    {
        if ( pres.getResolvedTypes() != null )
        {
//            Collection<ResolvedType> types = pres.getResolvedTypes().values();
//            for( ResolvedType rt : types )
//            {
//                String s = rt.name();
//                int t = 0;
//            }
            for( Node key : pres.getResolvedTypes().keySet() )
            {
                ResolvedType tp = pres.getResolvedTypes().get(key);
                int t = 0;
                io.getOut().println(">"+key+" -> "+tp);
            }
        }
    }
    
    public static void dumpDocument()
    {
        JTextComponent focused = EditorRegistry.lastFocusedComponent();
        Document doc = null;
        if (focused != null ) doc = focused.getDocument();
        if ( doc == null ) return;

        FileObject fo = getFileObject(doc);
        if ( ! fo.getExt().equals("mirah") ) return;
        
        final InputOutput io = IOProvider.getDefault().getIO(fo.getName()+".mirah", true);
        try {
            io.getOut().println("CaretPosition = "+focused.getCaretPosition());
            dumpTokens(doc,io);
            ParserManager.parse(Collections.singleton (Source.create(doc)), new UserTask() {
                @Override
                public void run(ResultIterator resultIterator) throws Exception {
                    Result res = resultIterator.getParserResult(0); // (offset);
                    if ( res instanceof MirahParser.NBMirahParserResult )
                    {
                       MirahParser.NBMirahParserResult pres = (MirahParser.NBMirahParserResult)res;
//                       Node node = pres.getRoot();
                       dumpNodes(pres,io);
                       dumpResolvedTypes(pres,io);
                    }
                }
            });
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
        finally
        {
            if ( io != null ) io.getOut().close();
            if ( io != null ) io.getErr().close();
        }
    }
}
