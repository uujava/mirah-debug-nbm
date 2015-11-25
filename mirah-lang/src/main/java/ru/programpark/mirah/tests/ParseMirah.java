/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.programpark.mirah.tests;

import ca.weblite.netbeans.mirah.lexer.MirahParser;
import ca.weblite.netbeans.mirah.lexer.MirahTokenId;
import java.io.IOException;
import java.util.Collections;
import javax.lang.model.type.ErrorType;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import mirah.lang.ast.ClosureDefinition;
import mirah.lang.ast.MethodDefinition;
import mirah.lang.ast.Node;
import mirah.lang.ast.NodeScanner;
import org.mirah.jvm.mirrors.DebugError;
import org.mirah.typer.ResolvedType;
import org.netbeans.api.editor.EditorRegistry;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.modules.parsing.api.ParserManager;
import org.netbeans.modules.parsing.api.ResultIterator;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.api.UserTask;
import org.netbeans.modules.parsing.spi.Parser.Result;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;
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
                Node p = node.parent();
                if ( node != null && node.position() != null )
                    io.getOut().println(prefix + " "+node+
                        " [" 
                        + node.position().startLine() + "," + node.position().startColumn() + "-" + node.position().endLine()
                        + "," + node.position().endColumn() + "] {" + node.position().startChar() + "-" + node.position().endChar() + "} " 
                        + node.hashCode() + "/ parent="+p+"[" + (p != null ? p.hashCode() : "0") + "] / type="+type);
                else
                    io.getOut().println(prefix + " "+node+" pos = null " + node.hashCode() + " parent=" + p + "[" + (p != null ? p.hashCode() : "0")+ "]");
                
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
        
//        io.getErr().println("getErrors: " + pres.getErrors());
//        io.getErr().println("getMirahDiagnostics: " + pres.getMirahDiagnostics());
//        io.getErr().println("getDiagnostics: " + pres.getDiagnostics());
//        io.getErr().println("getMirahDiagnostics.errorCount: " + pres.getMirahDiagnostics().errorCount());
//        io.getErr().println("getMirahDiagnostics.getErrors: " + pres.getMirahDiagnostics().getErrors());

        
        for( MirahParser.MirahParseDiagnostics.SyntaxError err : pres.getMirahDiagnostics().getErrors())
        {
//            io.getErr().println("SYNTAX: " + err.message);
//            io.getErr().println("SYNTAX: " + err.kind+" "+err.getClass());
//            io.getErr().println("SYNTAX: " + err.position);
            try {
                io.getOut().println(""+err.kind + ": "+err.message, new IndexHyperlink(pres.getSnapshot().getSource().getFileObject(),(int)err.start));
//                io.getOut().println(""+err.kind + ": "+err.message, new IndexHyperlink(pres.getSnapshot().getSource().getFileObject(), Integer.valueOf(err.position).intValue()));
            } catch (IOException ex) {}
        }
        /*
        for( org.netbeans.modules.csl.api.Error err : pres.getErrors() )
        {
            io.getErr().println("ERROR: "+err.getDescription());
        }
        io.getErr().println("===========================");
        try {
            for( ResolvedType type : pres.getResolvedTypes().values() )
            {
                if ( type instanceof DebugError )
                {
                    if ( ((DebugError) type).isError() )
                    {
                        io.getErr().println("DebugError: " + ((DebugError)type));
                        for( Object e : ((DebugError) type).message())
                        {
                            io.getErr().println("DebugError: " + ((DebugError)type).name()+" e="+e);
                        }
                    }
                    
                }
                else if (type instanceof ErrorType) {
                    io.getErr().println("ErrorType: " + ((ErrorType)type)+" kind="+((ErrorType) type).getKind());

                }
//                io.getErr().println("RESOLVED: " + type.getClass() + " "+type);
            }
        }
        catch( Exception ex ) {
            io.getErr().println("EXCEPTION = "+ex);
        }
        io.getErr().println("===========================");
        */
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

    public static void dumpClassPath( FileObject fo, String group, final InputOutput io )
    {
        ClassPath cp = ClassPath.getClassPath(fo,group);
        if ( cp != null && io != null )
            io.getOut().println("CP["+group+"] = "+cp.toString());
        
    }
    public static void dumpClassPathes( FileObject fo, final InputOutput io )
    {
        dumpClassPath(fo,ClassPath.COMPILE,io);
        dumpClassPath(fo,ClassPath.BOOT,io);
        dumpClassPath(fo,ClassPath.EXECUTE,io);
        dumpClassPath(fo,ClassPath.SOURCE,io);
        dumpClassPath(fo,ClassPath.PROP_ENTRIES,io);
        dumpClassPath(fo,ClassPath.PROP_INCLUDES,io);
        dumpClassPath(fo,ClassPath.PROP_ROOTS,io);
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
            io.getOut().reset();
            dumpClassPathes(fo,io);
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
