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
import mirah.lang.ast.AnnotationList;
import mirah.lang.ast.Node;
import mirah.lang.ast.*;
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
import org.openide.cookies.LineCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.text.Line;
import org.openide.util.Exceptions;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import org.openide.windows.OutputEvent;
import org.openide.windows.OutputListener;
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
    public static void putAstPath( MirahParser.NBMirahParserResult parsed, AstPath path, String tabName, int caretOffset )
    {
        final InputOutput io = IOProvider.getDefault().getIO(tabName, false);
        try {
            io.select();
            io.getOut().println("------------- caretOffset="+caretOffset+" ---------------------");
            if ( path != null ) {
                for (Iterator<Node> it = path.iterator(); it.hasNext();) {
                    Node node = it.next();
                    if (node instanceof AnnotationList
                    || node instanceof TypeNameList
                    || node instanceof Arguments
                    || node instanceof OptionalArgumentList
                    || node instanceof RequiredArgumentList
                    || node instanceof ModifierList) {
                       // continue;
                    }
                    ResolvedType type = parsed.getResolvedTypes().get(node);
                    if ( type != null )
                    io.getOut().println("AST: "+node+" type: "+type
                                + " ["+ node.position().startLine()+"," + node.position().startColumn() + "-"+node.position().endLine() 
                                + "," + node.position().endColumn() +"] {" + node.position().startChar() + "-" + node.position().startChar() + "} parent=" + node.parent());
                    else
                    io.getOut().println("AST: "+node
                                + " ["+ node.position().startLine()+","+node.position().startColumn()+"-"+node.position().endLine() 
                                + "," + node.position().endColumn() +"] {"+node.position().startChar() + "-" + node.position().startChar() + "} parent="+node.parent());
                }
            }
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
        finally
        {
            addHyperlink(io);
            if ( io != null ) io.getOut().close();
            if ( io != null ) io.getErr().close();
        }
    }

    /**
     * Represents a hyperlinked line in an InputOutput.
     */
    static final class Hyperlink implements OutputListener {

        private final int line;
        private final int column;

        public Hyperlink(int line, int column) {
            this.line = line;
            this.column = column;
        }

        @Override
        public void outputLineSelected(OutputEvent ev) {
            goToLine(false);
        }

        @Override
        public void outputLineCleared(OutputEvent ev) {
        }

        @Override
        public void outputLineAction(OutputEvent ev) {
            goToLine(true);
        }

        @SuppressWarnings("deprecation")
        private void goToLine(boolean focus) {
//            Line l = new Line();
            
            System.out.println("??????????????????????????????????????");
            System.out.println("??????????????????????????????????????");
            System.out.println("??????????????????????????????????????");
            System.out.println("??????????????????????????????????????");
            System.out.println("??????????????????????????????????????");
            System.out.println("??????????????????????????????????????");
//         lineCookie.getLineSet().getOriginal(line);
//            if (!l.isDeleted()) {
//                l.show(focus ? Line.SHOW_GOTO : Line.SHOW_TRY_SHOW, column);
//            }
            
            String name ="c:\\java-dao\\samples\\src\\main\\mirah\\ru\\programpark\\vector\\samples\\jfx\\application\\CustomApplication.mirah";

            Line xline = null;
            try {
                FileObject fo = FileUtil.createData(new File(name));
                DataObject dobj = DataObject.find(fo);
                LineCookie cookie = dobj.getCookie(LineCookie.class);
                if (cookie == null) {
                    throw new java.io.FileNotFoundException();
                } else {
                    xline = cookie.getLineSet().getCurrent(1);
                }
                
//                dobj = DataObject.find(fileObject);
            }
            catch( Exception e )
            {
                e.printStackTrace();
            }
            xline.show(Line.SHOW_GOTO);
//            
//            try {
//                String urlText = "file://c:/java-dao/mirah-jfxui/src/main/mirah/ru/programpark/vector/jfx/controller/framework/dsl/BaseControllerDSL.mirah"
//                URL url = new URL(urlText);
//                URLDisplayer.getDefault().showURL(url);
//            } catch (BadLocationException ex) {
//                Exceptions.printStackTrace(ex);
//            } catch (MalformedURLException ex) {
//                StatusDisplayer.getDefault().setStatusText(NbBundle.getMessage(HyperlinkImpl.class, "WARN_Invalid_URL", ex.getMessage()));
//                LOG.log(Level.FINE, null, ex);
//            }
        }
    }
    public static void addHyperlink( final InputOutput io )
    {
        try {
            io.getOut().println("MY HYPERLINK!", new Hyperlink(0,0));
//        CompilerError("c:\\java-dao\\mirah-jfxui\\src\\main\\mirah\\ru\\programpark\\vector\\jfx\\controller\\framework\\dsl\\BaseControllerDSL.mirah",
//                true, 63, 19, 64, 25, "It is error!");
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

    }
    
    public static void testAstPath( /*Document doc,*/ BaseDocument bdoc, ParserResult pres, int caretOffset)
    {
        try {
            MirahParser.NBMirahParserResult parsed = (MirahParser.NBMirahParserResult)pres;
            FileObject fo = pres.getSnapshot().getSource().getFileObject();
            AstPath path = ASTUtils.getPath(pres, bdoc, caretOffset);

            putAstPath(parsed, path, fo.getNameExt(), caretOffset);
            
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
