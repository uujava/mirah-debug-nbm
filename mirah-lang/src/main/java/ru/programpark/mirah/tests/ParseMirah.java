/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.programpark.mirah.tests;

import ca.weblite.netbeans.mirah.lexer.MirahParser;
import ca.weblite.netbeans.mirah.lexer.MirahTokenId;
import java.util.Collections;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import mirah.lang.ast.ClosureDefinition;
import mirah.lang.ast.MethodDefinition;
import mirah.lang.ast.Node;
import mirah.lang.ast.NodeScanner;
import org.mirah.typer.ResolvedType;
import org.netbeans.api.editor.EditorRegistry;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.modules.parsing.api.ParserManager;
import org.netbeans.modules.parsing.api.ResultIterator;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.api.UserTask;
import org.netbeans.modules.parsing.spi.Parser.Result;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import ru.programpark.mirah.editor.utils.LexUtilities;

/**
 *
 * @author savushkin
 */
public class ParseMirah {

    private static int getLevels( Node node )
    {
        int levels = 0;
        Node parent = node.parent();
        while( parent != null )
        {
            parent = parent.parent();
            levels++;
        }
        return levels;
    }
    
    private static void dumpNodes( final MirahParser.NBMirahParserResult pres, final InputOutput io )
    {
        Node root = pres.getRoot();
        if ( root == null ) return;
        io.getOut().println();
        io.getOut().println();
        io.getOut().println("------------- DUMP NODES ---------------------");
        io.getOut().println();
        io.getOut().println();
        root.accept(new NodeScanner(){
          @Override
            public boolean enterDefault(Node node, Object arg) {
                ResolvedType type = pres.getResolvedType(node);
                int levels = getLevels(node);
                StringBuffer sb = new StringBuffer();
                for( int i = 0 ; i < levels ; i++ ) sb.append('.');
                String prefix = sb.toString();
                if ( node != null && node.position() != null )
                    io.getOut().println(prefix + " "+node+
                        " [" 
                        + node.position().startLine() + "," + node.position().startColumn() + "-" + node.position().endLine()
                        + "," + node.position().endColumn() + "] {" + node.position().startChar() + "-" + node.position().endChar() + "} " 
                        + node.hashCode() + "/ parent="+node.parent()+"/ type="+type);
                else
                    io.getOut().println(prefix + " "+node+" pos = null " + node.hashCode() + " parent=" + node.parent());
                
                if ( node instanceof ClosureDefinition )
                {
                    ClosureDefinition closure = (ClosureDefinition)node;
                    io.getOut().println(prefix + "???? Closure: " + node);
                    for( Object n : closure.body() )
                    {
                        if ( n instanceof Node ) enterDefault((Node)n,arg);
                    }
                    io.getOut().println(prefix + "???? END OF Closure: " + node);
                }
                if ( node instanceof MethodDefinition )
                {
                    MethodDefinition closure = (MethodDefinition)node;
                    io.getOut().println(prefix + "???? MethodDefinition: " + node);
                    for( Object n : closure.body() )
                    {
                        if ( n instanceof Node ) enterDefault((Node)n,arg);
                    }
                    io.getOut().println(prefix + "???? END OF MethodDefinition: " + node);
                }
                
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
    public static void dumpErrors( MirahParser.NBMirahParserResult pres, final InputOutput io )
    {
        if ( pres.getErrors() == null ) return;
        io.getOut().println();
        io.getOut().println();
        io.getOut().println("------------- DUMP ERRORS ---------------------");
        io.getOut().println();
        io.getOut().println();
        for( org.netbeans.modules.csl.api.Error err : pres.getErrors() )
        {
            io.getErr().println("ERROR: "+err.getDescription());
        }
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
            
            io.getOut().println();
            io.getOut().println();
            io.getOut().println("------------- RESOLVED TYPES ---------------------");
            for( Node key : pres.getResolvedTypes().keySet() )
            {
                ResolvedType tp = pres.getResolvedTypes().get(key);
                int t = 0;
                io.getOut().println("TYPE: "+key+"("+key.getClass()+") -> "+tp + "(" + tp.getClass() + ")");
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
        
        final InputOutput io = IOProvider.getDefault().getIO(fo.getName()+".mirah", false);
        
        long start = System.currentTimeMillis();
        
        try {
            io.select();
            io.getOut().println("CaretPosition = "+focused.getCaretPosition());
            dumpTokens(doc,io);
            final long curr = System.currentTimeMillis();

            ParserManager.parse(Collections.singleton (Source.create(doc)), new UserTask() {
                @Override
                public void run(ResultIterator resultIterator) throws Exception {
                    Result res = resultIterator.getParserResult(0); // (offset);
                    if ( res instanceof MirahParser.NBMirahParserResult )
                    {
                        io.getOut().println("==== PARSING TIME: " + (System.currentTimeMillis() - curr) + " msec");
                        MirahParser.NBMirahParserResult pres = (MirahParser.NBMirahParserResult)res;
//                       Node node = pres.getRoot();
                        dumpNodes(pres,io);
                        dumpResolvedTypes(pres,io);
                        dumpErrors(pres,io);
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
            io.getOut().println("==== ELAPSED TIME : "+(System.currentTimeMillis()-start)+" msec");
            if ( io != null ) io.getOut().close();
            if ( io != null ) io.getErr().close();
        }
    }
}
