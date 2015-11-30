/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.programpark.vector.script.actions;

import ca.weblite.netbeans.mirah.MirahDataObject;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
        long start = System.currentTimeMillis();
        
        FileObject fo = context.getPrimaryFile();
        Project project = FileOwnerQuery.getOwner(fo);
        if (project == null) return;
        
        ClassPath compileClassPath = ClassPath.getClassPath(fo, ClassPath.COMPILE);
        ClassPath buildClassPath = ClassPath.getClassPath(fo, ClassPath.EXECUTE);
        ClassPath srcClassPath = ClassPath.getClassPath(fo, ClassPath.SOURCE);
        ClassPath bootClassPath = ClassPath.getClassPath(fo, ClassPath.BOOT);
        
        InputOutput io = IOProvider.getDefault().getIO("Выполнение "+fo.getNameExt(), false);
        try {
            io.select();
            io.getOut().reset();
            io.getOut().println("Start script execution!");
//            io.getOut().println(fo.asText());
            for( String line : fo.asLines() )
                io.getOut().println(line);

            ScriptExecutor se = new ScriptExecutor();
            se.compileScript(project.getProjectDirectory(),fo);
        }
        catch (Exception e ) {
            e.printStackTrace();
        }
        finally {
            io.getOut().println("==== ELAPSED TIME : " + (System.currentTimeMillis() - start) + " msec");
            if (io != null) io.getOut().close();
            if (io != null) io.getErr().close();
        }
    }
}
