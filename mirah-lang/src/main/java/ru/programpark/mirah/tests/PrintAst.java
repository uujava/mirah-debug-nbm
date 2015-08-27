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
    public static void putNode( MirahParser.NBMirahParserResult parsed, Node node, InputOutput io )
    {
        ResolvedType type = parsed.getResolvedTypes().get(node);
        if (type != null) {
            io.getOut().println("AST: " + node
                    + " [" + node.position().startLine() + "," + node.position().startColumn() + "-" + node.position().endLine()
                    + "," + node.position().endColumn() + "] {" + node.position().startChar() + "-" + node.position().endChar() + "} "
                    + node.hashCode() + "/ parent=" + node.parent() + "/ type: " + type);
        } else {
            io.getOut().println("AST: " + node
                    + " [" + node.position().startLine() + "," + node.position().startColumn() + "-" + node.position().endLine()
                    + "," + node.position().endColumn() + "] {" + node.position().startChar() + "-" + node.position().endChar() + "} "
                    + node.hashCode() + "/ parent=" + node.parent());
        }
    }
    public static void putAstPath( BaseDocument bdoc, MirahParser.NBMirahParserResult parsed, int caretOffset, InputOutput io )
    {
        long curr = System.currentTimeMillis();
        try {
            io.getOut().println("------------- caretOffset=" + caretOffset + " ---------------------");
            /*
            AstPath path = ASTUtils.getPath(parsed, bdoc, caretOffset);
            io.getOut().println("==== PREPARE AST PATH TIME: " + (System.currentTimeMillis() - curr) + " msec");
            if (path == null) {
                return;
            }
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
                putNode(parsed,node,io);
                /*
                ResolvedType type = parsed.getResolvedTypes().get(node);
                if ( type != null )
                io.getOut().println("AST: "+node
                            + " ["+ node.position().startLine()+"," + node.position().startColumn() + "-"+node.position().endLine() 
                            + "," + node.position().endColumn() +"] {" + node.position().startChar() + "-" + node.position().endChar() + "} "
                            + node.hashCode() + "/ parent=" + node.parent()+ "/ type: " + type);
                else
                io.getOut().println("AST: "+node
                            + " ["+ node.position().startLine()+","+node.position().startColumn()+"-"+node.position().endLine() 
                            + "," + node.position().endColumn() +"] {"+node.position().startChar() + "-" + node.position().endChar() + "} " 
                            + node.hashCode() + "/ parent="+node.parent());
                *
            }
            
            io.getOut().println("==== LEAF: ");
            */
            /*
            Node leaf = path.findLeaf();
            while( l1 != null ) 
            {
                putNode(parsed, l1, io);
                leaf = l1.parent();
            }
            io.getOut().println("==== LEAF2: ");
            */
            Node leaf = ASTUtils.findLeaf(parsed, bdoc, caretOffset);
            while (leaf != null) {
                putNode(parsed, leaf, io);
                leaf = leaf.parent();
            }
        }
        catch( Exception e )
        {
            e.printStackTrace();
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
            
            String name ="C:\\java-dao\\samples\\src\\main\\mirah\\ru\\programpark\\vector\\samples\\jfx\\controller\\tabs\\LabelsTab.mirah";
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
            }
            catch( Exception e )
            {
                e.printStackTrace();
            }
            xline.show(Line.SHOW_GOTO);
        }
    }
    public static void addHyperlink( final InputOutput io )
    {
        try {
            io.getOut().println("MY HYPERLINK!", new Hyperlink(0,0));
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
    /*
    public static void printAst()
    {
        JTextComponent focused = EditorRegistry.lastFocusedComponent();
        Document doc = null;
        if (focused != null ) doc = focused.getDocument();
        if ( doc == null ) return;
        /*
        DocumentDebugger dbg = MirahParser.getDocumentDebugger(doc);
        if ( dbg == null ) return;
        
        List nodes = dbg.compiler.compiler().getParsedNodes();
        *
        
        long start = System.currentTimeMillis();

        InputOutput io = null;

        MirahParser parser = new MirahParser();
        Snapshot snapshot = null;
        try {
//            FileObject fo = FileUtil.createData(new File(mirah_file_name));
//            Source src = Source.create(fo);
            Source src = Source.create(doc);
            snapshot = src.createSnapshot();
            FileObject fo = snapshot.getSource().getFileObject();
            if (fo == null || !fo.getExt().equals("mirah")) return;
            
            io = IOProvider.getDefault().getIO(fo.getNameExt(), false);
            io.select();

            long curr = System.currentTimeMillis();
            
            parser.reparse(snapshot); //, sb.toString());
            Result res = parser.getResult(null);
            Node node = null;
            if ( res instanceof MirahParser.NBMirahParserResult )
            {
                MirahParser.NBMirahParserResult pres = (MirahParser.NBMirahParserResult)res;
                node = pres.getRoot();
//           dumpNode(node);
            }
            io.getOut().println("==== PARSING TIME: " + (System.currentTimeMillis() - curr) + " msec");
            boolean b = doc instanceof BaseDocument;
            int offset = focused.getCaretPosition();
            BaseDocument bdoc = (BaseDocument)doc;
            putAstPath(bdoc,(MirahParser.NBMirahParserResult)res,offset,io);

        } catch (Exception ex){
            ex.printStackTrace();
        }
         finally {
            if (io != null) {
                io.getOut().println("==== ELAPSED TIME: " + (System.currentTimeMillis() - start) + " msec");
                io.getOut().close();
                io.getErr().close();
            }
        }
    }
    */
    public static void printAst() 
    {
        final JTextComponent focused = EditorRegistry.lastFocusedComponent();
        if ( focused == null ) return;
        final Document doc = focused.getDocument();
        if (doc == null) return;

        FileObject fo = getFileObject(doc);
        if (!fo.getExt().equals("mirah")) return;

        final InputOutput io = IOProvider.getDefault().getIO(fo.getNameExt(), false);

        final long start = System.currentTimeMillis();

        try {
            io.select();
            io.getOut().println("CaretPosition = " + focused.getCaretPosition());
            final long curr = System.currentTimeMillis();
            ParserManager.parse(Collections.singleton(Source.create(doc)), new UserTask() {
                @Override
                public void run(ResultIterator resultIterator) throws Exception {
                    Result res = resultIterator.getParserResult(0); // (offset);
                    if (res instanceof MirahParser.NBMirahParserResult) {
                        io.getOut().println("==== PARSING TIME: " + (System.currentTimeMillis() - start) + " msec");
                        MirahParser.NBMirahParserResult pres = (MirahParser.NBMirahParserResult) res;
                        int offset = focused.getCaretPosition();
                        BaseDocument bdoc = (BaseDocument) doc;
                        putAstPath(bdoc, pres, offset, io);
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            io.getOut().println("==== ELAPSED TIME : " + (System.currentTimeMillis() - start) + " msec");
            if (io != null) {
                io.getOut().close();
            }
            if (io != null) {
                io.getErr().close();
            }
        }
    }
    
}
