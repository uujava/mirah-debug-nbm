/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.programpark.mirah;

import java.io.IOException;
import org.netbeans.core.spi.multiview.MultiViewElement;
import org.netbeans.core.spi.multiview.text.MultiViewEditorElement;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.MIMEResolver;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.MultiFileLoader;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.TopComponent;

@Messages({
    "LBL_Mirah_LOADER=Files of VRuby"
})
@MIMEResolver.Registration(displayName="#LBL_Mirah_LOADER",
        resource="MirahMimeResolver.xml",
        position=100
)
@MIMEResolver.ExtensionRegistration(
        displayName = "#LBL_Mirah_LOADER",
        mimeType = "text/x-vruby",
        extension = {"vrb"}
)
@DataObject.Registration(
        mimeType = "text/x-vruby",
        iconBase = "ru/programpark/mirah/1391571312_application-x-ruby.png",
        displayName = "#LBL_Mirah_LOADER",
        position = 300
)
@ActionReferences({
        @ActionReference(
            path = "Loaders/text/x-vruby/Actions",
                id = @ActionID(category = "System", id = "org.openide.actions.OpenAction"),
                position = 100,
                separatorAfter = 200
        ),
        @ActionReference(
            path = "Loaders/text/x-vruby/Actions",
                id = @ActionID(category = "Edit", id = "org.openide.actions.CutAction"),
                position = 300
        ),
        @ActionReference(
            path = "Loaders/text/x-vruby/Actions",
                id = @ActionID(category = "Edit", id = "org.openide.actions.CopyAction"),
                position = 400,
                separatorAfter = 500
        ),
        @ActionReference(
            path = "Loaders/text/x-vruby/Actions",
                id = @ActionID(category = "Edit", id = "org.openide.actions.DeleteAction"),
                position = 600
        ),
        @ActionReference(
            path = "Loaders/text/x-vruby/Actions",
                id = @ActionID(category = "System", id = "org.openide.actions.RenameAction"),
                position = 700,
                separatorAfter = 800
        ),
        @ActionReference(
            path = "Loaders/text/x-vruby/Actions",
                id = @ActionID(category = "System", id = "org.openide.actions.SaveAsTemplateAction"),
                position = 900,
                separatorAfter = 1000
        ),
        @ActionReference(
            path = "Loaders/text/x-vruby/Actions",
                id = @ActionID(category = "System", id = "org.openide.actions.FileSystemAction"),
                position = 1100,
                separatorAfter = 1200
        ),
        @ActionReference(
            path = "Loaders/text/x-vruby/Actions",
                id = @ActionID(category = "System", id = "org.openide.actions.ToolsAction"),
                position = 1300
        ),
        @ActionReference(
            path = "Loaders/text/x-vruby/Actions",
                id = @ActionID(category = "System", id = "org.openide.actions.PropertiesAction"),
                position = 1400
        )
})
public class MirahDataObject extends MultiDataObject {

    public MirahDataObject(FileObject pf, MultiFileLoader loader) throws DataObjectExistsException, IOException {
        super(pf, loader);
        registerEditor("text/x-vruby", true);

    }

    @Override
    protected int associateLookup() {
        return 1;
    }

    @MultiViewElement.Registration(
            displayName = "#LBL_Mirah_EDITOR",
            iconBase = "ru/programpark/mirah/1391571312_application-x-ruby.png",
            mimeType = "text/x-vruby",
            persistenceType = TopComponent.PERSISTENCE_ONLY_OPENED,
            preferredID = "VRuby",
            position = 1000
    )
    @Messages("LBL_Mirah_EDITOR=Source")
    public static MultiViewEditorElement createEditor(Lookup lkp) {
        MultiViewEditorElement el = new MultiViewEditorElement(lkp);
        return el;
    }



}
