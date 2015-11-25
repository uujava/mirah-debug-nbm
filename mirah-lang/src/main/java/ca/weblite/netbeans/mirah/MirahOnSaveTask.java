/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.weblite.netbeans.mirah;


import ca.weblite.netbeans.mirah.support.api.MirahExtender;
import java.net.URL;
import java.util.Vector;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.netbeans.modules.parsing.api.indexing.IndexingManager;
import org.netbeans.modules.parsing.impl.indexing.RepositoryUpdater;
import org.netbeans.spi.editor.document.OnSaveTask;
import org.openide.filesystems.FileObject;

/**
 *
 * @author shannah
 */
public class MirahOnSaveTask implements OnSaveTask {

    
    Context context;
    
    private MirahOnSaveTask(Context context){
        this.context = context;
    }
    
    @Override
    public void performTask() {
        
//        RepositoryUpdater.getDefault().enforcedFileListUpdate(null, null);
        
        FileObject fo = NbEditorUtilities.getFileObject(context.getDocument());
//        LOG.info(this,"performTask fo="+fo.getNameExt());
        Project project = FileOwnerQuery.getOwner(fo);
//        LOG.info(this,"fo = "+fo+" project = "+project);
        if (!MirahExtender.isActive(project)) {
//            LOG.info(this,"MirahExtender.activate!!");
            MirahExtender.activate(project);
        }
//        LOG.info(this,"About to check if mirah is current");
        //LOG.info("About to check if mirah is current");
        
        if (!MirahExtender.isCurrent(project)){
//            LOG.info(this,"Mirah is not current");
            //LOG.info("Mirah is not current!!!");
            MirahExtender.update(project);
        }

                
        //svd - force file reindexing
        Vector<URL> urls = new Vector<URL>();
        urls.add(fo.toURL());
        IndexingManager.getDefault().refreshIndex(project.getProjectDirectory().toURL(),urls,false);
        LOG.info(this, "------------------ end of save task --------------------");        
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
