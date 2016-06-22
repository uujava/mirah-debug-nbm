/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ru.programpark.mirah;


import ru.programpark.mirah.support.api.MirahExtender;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.netbeans.spi.editor.document.OnSaveTask;
import org.openide.filesystems.FileObject;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author shannah
 */
public class MirahOnSaveTask implements OnSaveTask {

    private static final Logger logger = Logger.getLogger(MirahOnSaveTask.class.getName());
    Context context;
    
    private MirahOnSaveTask(Context context){
        this.context = context;
    }
    
    @Override
    public void performTask() {
        
//        RepositoryUpdater.getDefault().enforcedFileListUpdate(null, null);
        
        FileObject fo = NbEditorUtilities.getFileObject(context.getDocument());
//        if (logger.isLoggable(Level.FINE)) logger.log(Level.FINE, "performTask fo="+fo.getNameExt());
        Project project = FileOwnerQuery.getOwner(fo);
//        if (logger.isLoggable(Level.FINE)) logger.log(Level.FINE, "fo = "+fo+" project = "+project);
        if (!MirahExtender.isActive(project)) {
//            if (logger.isLoggable(Level.FINE)) logger.log(Level.FINE, "MirahExtender.activate!!");
            MirahExtender.activate(project);
        }
//        if (logger.isLoggable(Level.FINE)) logger.log(Level.FINE, "About to check if mirah is current");
        //LOG.info("About to check if mirah is current");
        if (!MirahExtender.isCurrent(project)){
//  TODO fix gradle/maven project updater
        }
        if (logger.isLoggable(Level.FINE)) logger.log(Level.FINE,  "------------------ end of save task --------------------");
    }

    @Override
    public void runLocked(Runnable r) {
        performTask();
    }

    @Override
    public boolean cancel() {
        System.out.println("Cancelling....");
        return false;
    }
    
    
    @MimeRegistration(mimeType="text/x-mirah", service=OnSaveTask.Factory.class, position=1500)
    public static class Factory implements OnSaveTask.Factory {

        @Override
        public OnSaveTask createTask(Context cntxt) {
            
            return new MirahOnSaveTask(cntxt);
        }
        
    }
    
   
    
    
}
