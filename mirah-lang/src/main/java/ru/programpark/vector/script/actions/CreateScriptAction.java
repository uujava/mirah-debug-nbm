/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.programpark.vector.script.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.project.Project;
import org.netbeans.api.editor.EditorRegistry;
import org.netbeans.api.project.FileOwnerQuery;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "Edit",
        id = "ru.programpark.vector.script.actions.CreateScriptAction"
)
@ActionRegistration(
        iconBase = "ru/programpark/vector/script/ruby.png",
        displayName = "#CTL_CreateScriptAction"
)
@ActionReferences({
    @ActionReference(path = "Menu/File", position = 0),
    @ActionReference(path = "Toolbars/VectorTools", position = 0)
})
@Messages("CTL_CreateScriptAction=Новый скрипт")
public final class CreateScriptAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {

        JTextComponent focused = EditorRegistry.lastFocusedComponent();
        if (focused == null) return;
        
        Document doc = focused.getDocument();
        if (doc == null) return;

        FileObject fo = getFileObject(doc);
        Project project = FileOwnerQuery.getOwner(fo);
        if ( project == null ) return;

//        Project project = OpenProjects.getDefault().getMainProject();
        FileObject folder = project.getProjectDirectory();
        createScript(folder,"Скрипт","mirah");
    }
    
    public static FileObject getFileObject(Document doc) {
        DataObject od = (DataObject) doc.getProperty(Document.StreamDescriptionProperty);

        return od != null ? od.getPrimaryFile() : null;
    }

    public void createScript( FileObject folder, String name, String ext ) 
    {
        if ( folder == null ) return;
        
        try {
            folder = FileUtil.createFolder(folder, "src/main/scripts");
//            folder = FileUtil.createFolder(folder, "src/main");
            if (folder == null) return;
            String path = FileUtil.findFreeFileName(folder, name, ext);
            FileObject fo = folder.createData(path,ext);
            DataObject dataObj = DataObject.find(fo);
            Lookup lookup = dataObj.getLookup();
            if ( lookup == null ) return;
            EditorCookie cookie = lookup.lookup(EditorCookie.class);
            if ( cookie != null ) cookie.open();
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
    /*

    File.write (file, 
"")
					fo = NBPlugins::FileUtil.to_file_object(file)
				end
				return unless fo

				dobj = DataObject.find(fo)
				return unless dobj
				node = dobj.getNodeDelegate()
				return unless node

				node.setDisplayName("#{NBPlugins::LayerPathMapping.restore_illegal_characters(entity.name)}") if entity

				#puts "OLD ICON BASE--1 = #{fo.getAttribute("iconBase")}"
				icon_base = NBDesigner::EntityInspector.get_icon_base(entity) unless icon_base
				icon_base = "ruby.png" unless icon_base
				if fo.getAttribute("iconBase").nil?
					fo.setAttribute("iconBase",icon_base) #if icon_base && fo.getAttribute("iconBase").nil?
					#node.set_icon_base_with_extension(icon_base) if icon_base #&& fo.getAttribute("iconBase").nil?
				else
					#node.set_icon_base_with_extension(fo.getAttribute("iconBase"))
				end
				lookup = dobj.getLookup()
				cookie = lookup.lookup(EditorCookie.java_class) if lookup
				cookie.open() if cookie
			rescue Exception => e
				puts "LayerActionProvider.open_file #{entity} #{file} error: #{e}"
			end

		end

    */
}
