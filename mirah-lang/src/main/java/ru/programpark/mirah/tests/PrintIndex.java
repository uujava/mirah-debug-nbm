/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.programpark.mirah.tests;

import java.util.Set;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.EditorRegistry;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.modules.parsing.spi.indexing.support.QuerySupport;
import org.openide.cookies.LineCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.text.Line;
import org.openide.util.Exceptions;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import org.openide.windows.OutputEvent;
import org.openide.windows.OutputListener;
import ru.programpark.mirah.index.MirahIndex;
import ru.programpark.mirah.index.elements.IndexedClass;
import ru.programpark.mirah.index.elements.IndexedMethod;
import static ru.programpark.mirah.tests.ParseMirah.getFileObject;


/**
 *
 * @author savushkin
 */
/**
 * Represents a hyperlinked line in an InputOutput.
 */
final class IndexHyperlink implements OutputListener {

    private final int line;
    private final FileObject fileObj;

    public IndexHyperlink(FileObject fileObj, int line ) {
        this.line = line;
        this.fileObj = fileObj;
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
        Line xline = null;
        try {
            DataObject dobj = DataObject.find(fileObj);
            LineCookie cookie = dobj.getCookie(LineCookie.class);
            if (cookie == null) {
                throw new java.io.FileNotFoundException();
            } else {
                xline = cookie.getLineSet().getCurrent(line-1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if ( xline != null ) xline.show(Line.SHOW_GOTO);
    }
}


public class PrintIndex {
    
    public static void printIndex()
    {
        InputOutput io = null;
        try
        {
            JTextComponent focused = EditorRegistry.lastFocusedComponent();
            Document doc = null;
            if (focused != null ) doc = focused.getDocument();
            if ( doc == null ) return;

            FileObject fo = getFileObject(doc);
            if ( ! fo.getExt().equals("mirah") ) return;
            
            Project project = FileOwnerQuery.getOwner(fo);
            
            io = IOProvider.getDefault().getIO("Index of "+project.getProjectDirectory().getName(), false);
            io.select();
            
            io.getOut().println("---------------------------------------");
            MirahIndex index = MirahIndex.get(fo);
            Set<IndexedClass> classes = index.getAllClasses();
            for (IndexedClass cls : classes) {
                String fqName = cls.getFqn();
                String signature = cls.getSignature();
                io.getOut().println("class: " + signature, new IndexHyperlink(cls.getFileObject(), cls.getLine()));
//                io.getOut().println("class: "+signature+" "+cls.getFileObject().getPath()+"["+cls.getLine()+"]");
                Set<IndexedMethod> constructors = index.getConstructors(fqName);
                for (IndexedMethod con : constructors) {
                    String name = con.getName();
                    io.getOut().println("... " + con.getSignature());
                }
                Set<IndexedMethod> methods = index.getMethods(null,fqName,QuerySupport.Kind.PREFIX);
                for( IndexedMethod meth : methods )
                {
                    String name = meth.getName();
                    io.getOut().println("... "+meth.getSignature());
                }
            }
//            Set<IndexedMethod> methods = index.getMethods(null, null, QuerySupport.Kind.PREFIX);
//            for( IndexedMethod me : methods )
//            {
//                String name = me.getName();
//                String signature = me.getSignature();
//                io.getOut().println("method: "+signature);
//            }
        }
        catch( Exception e ) 
        {
            io.getOut().println("EX: "+e);
            e.printStackTrace();
        }
        finally
        {
            if ( io != null ) io.getOut().close();
            if ( io != null ) io.getErr().close();
        }
    }
}
