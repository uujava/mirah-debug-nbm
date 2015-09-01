package ru.programpark.mirah.editor.jumpto;

import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.swing.Icon;
import javax.swing.text.Document;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.annotations.common.NullAllowed;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.platform.JavaPlatformManager;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.SourceUtils;
import org.netbeans.api.java.source.ui.ElementOpen;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.netbeans.modules.java.source.parsing.FileObjects;
import org.netbeans.modules.java.source.usages.ClassIndexImpl;
import org.netbeans.modules.java.ui.Icons;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.netbeans.spi.jumpto.type.TypeDescriptor;
import org.openide.awt.StatusDisplayer;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.text.Line;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;

/**
 * 
 * @todo Resolve with TypeDescription
 *
 * @author Petr Hrebejk
 */
public class MirahTypeDescription extends TypeDescriptor {

    private static final Logger LOG = Logger.getLogger(MirahTypeDescription.class.getName());

    public Icon icon;
    public Icon projectIcon;
    public String simpleName;
    public String outerName;
    public String packageName;
    public FileObject root;
    public int offset;
    public String projectName;
    
    MirahTypeDescription() {
       init();
       try {
        icon = Icons.getElementIcon(ElementKind.CLASS, null);
       }
       catch( Exception e )
       {
           e.printStackTrace();
       }
    }
    
    @Override
    public void open() {        
        try {
            DataObject dobj = DataObject.find(root);

            EditorCookie cookie = dobj.getCookie(EditorCookie.class);
            Document doc = cookie.openDocument();
            Line xline = NbEditorUtilities.getLine(doc, offset, false);
            if (cookie == null) {
                throw new java.io.FileNotFoundException();
            }
            if (xline != null) {
                xline.show(Line.SHOW_GOTO);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getSimpleName() {
        return simpleName;
    }
    
    @Override
    public String getOuterName() {
        return outerName;
    }

    @Override
    public FileObject getFileObject() {
        return root;
    }

    @Override
    public String getFileDisplayPath() {
        return root.getPath();
    }

    @Override
    public String getTypeName() {
		return simpleName;
//        StringBuilder sb = new StringBuilder( simpleName );
//        if( outerName != null  ) {
//            sb.append(" in ").append( outerName );
//        }
//        return sb.toString();
    }
    
    @Override
    public String getContextName() {
        StringBuilder sb = new StringBuilder();
        sb.append( " (").append( packageName == null ? "Default Package" : packageName).append(")");
        return sb.toString();
                
        
    }
    
    @Override
    public String getProjectName() {
        return projectName; // NOI18N        
    }
    
    @Override
    public Icon getProjectIcon() {        
        return projectIcon;
    }

    @Override
    public synchronized Icon getIcon() {
        return icon;
    }

    @Override
    public int getOffset() {
        return offset;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder( simpleName );
        if( outerName != null  ) {
            sb.append(" in ").append( outerName );
        }
        sb.append( " (").append( packageName == null ? "Default Package" : packageName).append(")");
        sb.append( " [").append(getProjectName()).append("]");
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int hc = 17;
//        hc = hc * 31 + handle.hashCode();
//        hc = hc * 31 + handle.hashCode();
        return hc;
    }

    @Override
    public boolean equals (@NullAllowed final Object other) {
        if (other == this) {
            return true;
        }
//        if (!(other instanceof MirahTypeDescription)) {
            return false;
//        }
//        MirahTypeDescription otherJTD = (MirahTypeDescription) other;
//        return handle.equals(otherJTD.handle) && cacheItem.equals(otherJTD.cacheItem);
    }
    
//    public ElementHandle<TypeElement> getHandle() {
//        return handle;
//    }
    
    private void init() {
//        final String typeName = this.handle.getBinaryName();
//        int lastDot = typeName.lastIndexOf('.'); // NOI18N
//        int lastDollar = typeName.lastIndexOf('$'); // NOI18N
//        if ( lastDot == -1 ) {
//            if ( lastDollar == -1 ) {
//                simpleName = typeName;
//            }
//            else {
//                simpleName = typeName.substring(lastDollar + 1);
//                outerName = typeName.substring(0, lastDollar ).replace( '$', '.');  //NOI18N;
//            }
//        }
//        else {
//            packageName = typeName.substring( 0, lastDot );
//            
//            if (lastDollar < lastDot) {
//                simpleName = typeName.substring( lastDot + 1 ).replace( '$', '.');  //NOI18N
//            }
//            else {
//                simpleName = typeName.substring(lastDollar + 1);
//                outerName = typeName.substring(lastDot + 1, lastDollar ).replace( '$', '.');  //NOI18N;
//            }
//                        
//        }
//        icon = Icons.getElementIcon (handle.getKind(), null);
    }

    private static String getRelativePath(
        @NonNull final String binaryName,
        @NullAllowed final ClassIndexImpl ci,
        final boolean isBinary,
        @NullAllowed final URI root) {
        String relativePath = null;
        if (ci == null) {
            LOG.log (
                Level.WARNING,
                "No ClassIndex for {0} in {1}", //NOI18N
                new Object[]{
                    binaryName,
                    root});
        } else {
            try {
                relativePath = ci.getSourceName(binaryName);
            } catch (IOException | InterruptedException ex) {
                LOG.log (
                    Level.WARNING,
                    "Broken ClassIndex for {0} in {1}", //NOI18N
                    new Object[]{
                        binaryName,
                        root});
            }
        }
        if (relativePath == null) {
            relativePath = binaryName;
            int lastDot = relativePath.lastIndexOf('.');    //NOI18N
            int csIndex = relativePath.indexOf('$', lastDot);     //NOI18N
            if (csIndex > 0 && csIndex < relativePath.length()-1) {
                relativePath = binaryName.substring(0, csIndex);
            }
            relativePath = String.format(
                "%s.%s",    //NOI18N
                FileObjects.convertPackage2Folder(relativePath, File.separatorChar),
                isBinary ?
                   FileObjects.CLASS :
                   FileObjects.JAVA);
        }
        return relativePath;
    }
}
