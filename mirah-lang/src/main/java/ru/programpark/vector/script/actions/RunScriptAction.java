/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.programpark.vector.script.actions;

import ca.weblite.netbeans.mirah.MirahDataObject;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Action;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.FileOwnerQuery;
import ru.programpark.vector.script.ScriptExecutor;

@ActionID(
        category = "Edit",
        id = "ru.programpark.vector.script.actions.RunScriptAction"
)
@ActionRegistration(
        iconBase = "ru/programpark/vector/script/runProject.png",
        displayName = "#CTL_RunScriptAction"
)
@ActionReferences({
    @ActionReference(path = "Menu/File", position = 1),
    @ActionReference(path = "Toolbars/VectorTools", position = 1)
})
@Messages("CTL_RunScriptAction=Выполнить скрипт")
public final class RunScriptAction implements ActionListener {

    private final MirahDataObject context;
    public RunScriptAction(MirahDataObject context) {
        this.context = context;
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        
        final FileObject fo = context.getPrimaryFile();
        Project project = FileOwnerQuery.getOwner(fo);
        if (project == null) return;
        
        InputOutput io = null;
        try {
            final StopAction stopAction = new StopAction();
            String name = "Выполнение " + fo.getNameExt();
            io = IOProvider.getDefault().getIO(name, new Action[]{stopAction});
            io.select();
            io.getOut().reset();
            final ScriptExecutor se = new ScriptExecutor(io);
            Thread t = new Thread( 
                new Runnable() {
                    @Override
                    public void run() {
                        se.runScript(fo);
                        stopAction.setEnabled(false);
                    }
                });
//            stopAction.setTask( new FutureTask(t,null) );
            stopAction.setThread(t);
            stopAction.setEnabled(true);
            t.start();
            
        }
        catch (Exception e ) {
            io.getOut().println(""+this+" ex="+e);
            e.printStackTrace();
        }
        finally {
//            if (io != null) io.closeInputOutput();
            if (io != null) io.getOut().close();
            if (io != null) io.getErr().close();
        }
    }
}
