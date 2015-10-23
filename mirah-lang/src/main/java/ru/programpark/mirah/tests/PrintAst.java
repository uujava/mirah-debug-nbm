/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.programpark.mirah.tests;

import ca.weblite.netbeans.mirah.lexer.MirahParser;
import java.util.Collections;
import java.util.Iterator;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import mirah.lang.ast.AnnotationList;
import mirah.lang.ast.Node;
import org.mirah.typer.ResolvedType;
import org.netbeans.api.editor.EditorRegistry;
import org.netbeans.editor.BaseDocument;
import org.netbeans.modules.parsing.api.ParserManager;
import org.netbeans.modules.parsing.api.ResultIterator;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.api.UserTask;
import org.netbeans.modules.parsing.spi.Parser.Result;
import org.openide.filesystems.FileObject;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import static ru.programpark.mirah.tests.ParseMirah.getFileObject;
import ru.programpark.mirah.editor.ast.ASTUtils;
import ru.programpark.mirah.editor.ast.AstPath;
import ru.programpark.mirah.editor.completion.util.VariablesCollector;

/**
 *
 * @author savushkin
 */
public class PrintAst {

    public static void putException( Exception e)
    {
        final InputOutput io = IOProvider.getDefault().getIO("Proposals", false);
        try {
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
            Node leaf = ASTUtils.findLeaf(parsed, bdoc, caretOffset);
            while (leaf != null) {
                putNode(parsed, leaf, io);
                leaf = leaf.parent();
            }
            io.getOut().println("====================================================================");
            AstPath path = ASTUtils.getPath(parsed, bdoc, caretOffset);
            if (path != null) {
                for (Iterator<Node> it = path.iterator(); it.hasNext();) {
                    Node node = it.next();
                    putNode(parsed, node, io);
                }
            }
            io.getOut().println("====================================================================");
            VariablesCollector vc = new VariablesCollector(ASTUtils.findLeaf(parsed, bdoc, caretOffset),bdoc,caretOffset);
//            VariablesCollector vc = new VariablesCollector(path,bdoc,caretOffset);
            vc.collect();
            for( String name : vc.getVariables())
                io.getOut().println("VARIABLE: " + name);
            io.getOut().println("====================================================================");
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
    }


    /**
     * Represents a hyperlinked line in an InputOutput.
     */
    /*
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
