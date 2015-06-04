/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.programpark.mirah.tests;

import ca.weblite.netbeans.mirah.LOG;
import ca.weblite.netbeans.mirah.lexer.MirahParser;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import mirah.lang.ast.Node;
import org.mirah.typer.ResolvedType;
import org.netbeans.api.editor.EditorRegistry;
import org.netbeans.editor.BaseDocument;
import org.netbeans.modules.csl.api.CodeCompletionContext;
import org.netbeans.modules.csl.api.CodeCompletionHandler;
import org.netbeans.modules.csl.api.CodeCompletionResult;
import org.netbeans.modules.csl.api.CompletionProposal;
import org.netbeans.modules.csl.spi.ParserResult;
import org.netbeans.modules.parsing.api.ParserManager;
import org.netbeans.modules.parsing.api.ResultIterator;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.api.UserTask;
import org.netbeans.modules.parsing.spi.Parser.Result;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import static ru.programpark.mirah.tests.ParseMirah.dumpTokens;
import static ru.programpark.mirah.tests.ParseMirah.getFileObject;
import ru.programpark.mirah.editor.api.completion.CompletionHandler;
import ru.programpark.mirah.editor.ast.ASTUtils;
import ru.programpark.mirah.editor.ast.AstPath;

/**
 *
 * @author savushkin
 */
public class PrintAst {

//    static String mirah_file_name = "c:\\mirah-debug\\mavenproject1\\src\\main\\java\\ru\\pp\\MirahClass22.mirah";
    
//    public static void putProposals(List<CompletionProposal> proposals)
    
    
    public static void putException( Exception e)
    {
        final InputOutput io = IOProvider.getDefault().getIO("Proposals", false);
        try {
//            io.getOut().println("proposal count = "+proposals.size());
//            for( CompletionProposal proposal : proposals )
//            {
//                io.getOut().println("proposal = "+proposal);
//            }
            for( StackTraceElement st : e.getStackTrace() )
                io.getOut().println(">>"+st);
        }
        catch( Exception ee )
        {
            e.printStackTrace();
        }
        finally
        {
            if ( io != null ) io.getOut().close();
            if ( io != null ) io.getErr().close();
        }
    }
    public static void putAstPath( MirahParser.NBMirahParserResult parsed, AstPath path, String tabName )
    {
        final InputOutput io = IOProvider.getDefault().getIO(tabName, false);
        try {
            io.select();
            io.getOut().println("----------------------------------");
            if ( path != null ) {
                for (Iterator<Node> it = path.iterator(); it.hasNext();) {
                    Node node = it.next();
                    ResolvedType type = parsed.getResolvedTypes().get(node);
                    io.getOut().println("AST: "+node+" type: "+type);
                }
            }
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
    
    
    public static void testAstPath( /*Document doc,*/ BaseDocument bdoc, ParserResult pres, int caretOffset)
    {
        try {
            MirahParser.NBMirahParserResult parsed = (MirahParser.NBMirahParserResult)pres;
            FileObject fo = pres.getSnapshot().getSource().getFileObject();
            AstPath path = ASTUtils.getPath(pres, bdoc, caretOffset);

            putAstPath(parsed, path, fo.getNameExt());
            
//            HyperlinkContext hc = new HyperlinkContext(bdoc,pres,caretOffset);
//            hc.analyzeContext(caretOffset);
        }
        catch( Exception e )
        {
            putException(e);
        }
    }
    
    public static void printAst()
    {
        JTextComponent focused = EditorRegistry.lastFocusedComponent();
        Document doc = null;
        if (focused != null ) doc = focused.getDocument();
        if ( doc == null ) return;
        
        MirahParser parser = new MirahParser();
        Snapshot snapshot = null;
        try {
//            FileObject fo = FileUtil.createData(new File(mirah_file_name));
//            Source src = Source.create(fo);
            Source src = Source.create(doc);
            snapshot = src.createSnapshot();
            if ( snapshot.getSource().getFileObject() == null || ! snapshot.getSource().getFileObject().getExt().equals("mirah") )
               return; 
            parser.reparse(snapshot); //, sb.toString());
            Result res = parser.getResult(null);
            Node node = null;
            if ( res instanceof MirahParser.NBMirahParserResult )
            {
                MirahParser.NBMirahParserResult pres = (MirahParser.NBMirahParserResult)res;
                node = pres.getRoot();
//           dumpNode(node);
            }
            boolean b = doc instanceof BaseDocument;
            int offset = focused.getCaretPosition();
            BaseDocument bdoc = (BaseDocument)doc;
            ParserResult pres = (ParserResult)res;
            snapshot = pres.getSnapshot();
            testAstPath(bdoc,pres,offset);

        } catch (Exception ex){
            ex.printStackTrace();
        }
    }
}
