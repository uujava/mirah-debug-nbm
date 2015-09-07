package ru.programpark.mirah.editor.jumpto;

import javax.lang.model.element.ElementKind;
import javax.swing.Icon;
import javax.swing.text.Document;
import org.netbeans.api.java.source.ui.ElementIcons;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.netbeans.spi.jumpto.symbol.SymbolDescriptor;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.text.Line;
import org.openide.util.NbBundle;
import org.openide.util.Parameters;
import org.openide.util.RequestProcessor;

/**
 * SymbolDescriptor implementation for Mirah
 * 
 */
public class MirahSymbolDescriptor extends SymbolDescriptor implements Runnable {

    private final Icon icon;
    private final Project project;
//    private final CharSequence filePath;
    private final int offset;
    private final String ownerName;
    private final String dispayName;
    private final ElementKind kind;
    private final FileObject fileObject;
    
    public MirahSymbolDescriptor(String dispayName, ElementKind kind, FileObject fileObject, int offset, String ownerName ) {
        this.dispayName = dispayName;
        this.kind = kind;
        this.fileObject = fileObject;
        this.ownerName = ownerName;
        this.offset = offset;
        this.project = FileOwnerQuery.getOwner(fileObject);
        this.icon = ElementIcons.getElementIcon(kind, null);
        /*
        Parameters.notNull("csmObj", csmObj);
        CsmFile csmFile = csmObj.getContainingFile();
        filePath = csmFile.getAbsolutePath();
        offset = csmObj.getStartOffset();
        project = csmFile.getProject();
        if (CsmKindUtilities.isClass(csmObj) && CsmKindUtilities.isTemplate(csmObj)) {
            name = ((CsmTemplate)csmObj).getDisplayName();
        } else if (CsmKindUtilities.isFunction(csmObj)) {
            name = ((CsmFunction) csmObj).getSignature();
        } else if (CsmKindUtilities.isNamedElement(csmObj)) {
            name = ((CsmNamedElement) csmObj).getName();
        } else {
            throw new IllegalArgumentException("should be CsmNamedElement, in fact " + csmObj.getClass().getName()); //NOI18N
        }

        CharSequence fileName = csmFile.getName();
        if (CsmKindUtilities.isMacro(csmObj)) {
            //CsmMacro macro = (CsmMacro)  csmObj;
            ownerName = fileName;
        } else if (CsmKindUtilities.isOffsetableDeclaration(csmObj)) {
            CsmOffsetableDeclaration decl = (CsmOffsetableDeclaration) csmObj;
            CsmScope scope = decl.getScope();
            if (CsmKindUtilities.isFile(scope)) {
                ownerName = fileName;
            }
            else if (CsmKindUtilities.isQualified(scope)) {
                CharSequence qName = ((CsmQualifiedNamedElement) scope).getQualifiedName();
                if (qName.length() > 0) {
                    ownerName = NbBundle.getMessage(getClass(), "CPP_Descriptor_In_Compound", qName, fileName);
                } else {
                    ownerName = fileName;
                }
            } else {
                throw new IllegalArgumentException("should be either CsmFile or CsmQualifiedNamedElement, in fact " + csmObj.getClass().getName()); //NOI18N
            }
        } else {
            throw new IllegalArgumentException("should be either CsmMacro or CsmDeclaration, in fact " + csmObj.getClass().getName()); //NOI18N
        }
        icon = CsmImageLoader.getIcon(csmObj);
        */
    }
    
    @Override
    public FileObject getFileObject() {
        return fileObject;
    }

    @Override
    public String getFileDisplayPath() {
        return fileObject.getPath();
    }

    @Override
    public Icon getIcon() {
        return icon;
    }

    @Override
    public int getOffset() {
        return offset;
    }

    @Override
    public String getOwnerName() {
        return ownerName;
    }

    @Override
    public Icon getProjectIcon() {
        ProjectInformation pinfo = ProjectUtils.getInformation(project);
        return pinfo.getIcon();
    }

    @Override
    public String getProjectName() {
        ProjectInformation pinfo = ProjectUtils.getInformation(project);
        return pinfo.getDisplayName();
    }

    @Override
    public String getSymbolName() {
        return dispayName;
    }

    /** gets name as such */
    public CharSequence getRawName() {
        for (int i = 0; i < dispayName.length(); i++) {
            if (dispayName.charAt(i) == '(') {
                return dispayName.subSequence(0, i);
            }
        }
        return dispayName;
    }

    @Override
    public void open() {
        RequestProcessor.getDefault().post(this);
    }

    @Override
    public void run() {
//        CsmUtilities.openSource(getFileObject(), offset);
        try {
            DataObject dobj = DataObject.find(fileObject);
            EditorCookie cookie = dobj.getCookie(EditorCookie.class);
            if (cookie == null) {
                throw new java.io.FileNotFoundException();
            }
            Document doc = cookie.openDocument();
            Line line = NbEditorUtilities.getLine(doc, offset, false);
            if ( line != null ) line.show(Line.SHOW_GOTO);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
}
