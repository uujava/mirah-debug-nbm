package ru.programpark.mirah.editor.completion;

import ca.weblite.netbeans.mirah.lexer.MirahTokenId;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import mirah.lang.ast.Node;
import org.netbeans.modules.csl.api.ElementHandle;
import org.netbeans.modules.csl.api.ElementKind;
import org.netbeans.modules.csl.api.Modifier;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.csl.spi.ParserResult;
import org.openide.filesystems.FileObject;
import ru.programpark.mirah.editor.ast.ASTUtils;
import ru.programpark.mirah.index.elements.IndexedElement;

/**
 *
 * @author Martin Adamek
 */
public class ElementHandleSupport {

    public static ElementHandle createHandle(ParserResult info, final MirahElement object) {
        
        /*
        if (object instanceof KeywordElement || object instanceof CommentElement) {
            // Not tied to an AST - just pass it around
            return new MirahElementHandle(null, object, info.getSnapshot().getSource().getFileObject());
        }

        if (object instanceof IndexedElement) {
            // Probably a function in a "foreign" file (not parsed from AST),
            // such as a signature returned from the index of the Groovy libraries.
            // TODO - make sure this is infrequent! getFileObject is expensive!            
            // Alternatively, do this in a delayed fashion - e.g. pass in null and in getFileObject
            // look up from index            
            return new MirahElementHandle(null, object, ((IndexedElement)object).getFileObject());
        }

        if (!(object instanceof ASTElement)) {
            return null;
        }
*/
        // XXX Gotta fix this
        if (info == null) {
            return null;
        }
        
        ParserResult result = ASTUtils.getParseResult(info);

        if (result == null) {
            return null;
        }

        Node root = ASTUtils.getRoot(info);

        return null;
//        return new MirahElementHandle(root, object, info.getSnapshot().getSource().getFileObject());
    }
/*
    @SuppressWarnings("unchecked")
//    public static ElementHandle createHandle(ParserResult result, final ASTElement object) {
    public static ElementHandle createHandle(ParserResult result, final MirahElement object) {
        Node root = ASTUtils.getRoot(result);

        return new MirahElementHandle(root, object, result.getSnapshot().getSource().getFileObject());
    }`
  */  
    public static MirahElement resolveHandle(ParserResult info, ElementHandle handle) {
        MirahElementHandle h = (MirahElementHandle) handle;
        Node oldRoot = h.root;
        Node oldNode;
/*
        if (h.object instanceof KeywordElement || h.object instanceof IndexedElement || h.object instanceof CommentElement) {
            // Not tied to a tree
            return h.object;
        }
        if (h.object instanceof ASTElement) {
            oldNode = ((ASTElement)h.object).getNode(); // XXX Make it work for DefaultComObjects...
        } else {
            return null;
        }
*/

        Node newRoot = ASTUtils.getRoot(info);
        if (newRoot == null) {
            return null;
        }
/*
        // Find newNode
        Node newNode = find(oldRoot, oldNode, newRoot);

        if (newNode != null) {
            MirahElement co = ASTElement.create(newNode);

            return co;
        }
*/
        return null;
    }

    public static ElementHandle createHandle(String className, String elementName, ElementKind kind,
                Set<Modifier> modifiers) {
        return new SimpleElementHandle(className, elementName, kind, modifiers);
    }

    private static Node find(Node oldRoot, Node oldObject, Node newRoot) {
        // Walk down the tree to locate oldObject, and in the process, pick the same child for newRoot
        @SuppressWarnings("unchecked")
        List<?extends Node> oldChildren = ASTUtils.children(oldRoot);
        @SuppressWarnings("unchecked")
        List<?extends Node> newChildren = ASTUtils.children(newRoot);
        Iterator<?extends Node> itOld = oldChildren.iterator();
        Iterator<?extends Node> itNew = newChildren.iterator();

        while (itOld.hasNext()) {
            if (!itNew.hasNext()) {
                return null; // No match - the trees have changed structure
            }

            Node o = itOld.next();
            Node n = itNew.next();

            if (o == oldObject) {
                // Found it!
                return n;
            }

            // Recurse
            Node match = find(o, oldObject, n);

            if (match != null) {
                return match;
            }
        }

        if (itNew.hasNext()) {
            return null; // No match - the trees have changed structure
        }

        return null;
    }

    private static class MirahElementHandle implements ElementHandle {
        private final Node root;
        private final MirahElement object;
        private final FileObject fileObject;

        private MirahElementHandle(Node root, MirahElement object, FileObject fileObject) {
            this.root = root;
            this.object = object;
            this.fileObject = fileObject;
        }

        public boolean signatureEquals(ElementHandle handle) {
            // XXX TODO
            return false;
        }

        public FileObject getFileObject() {
            /*
            if (object instanceof IndexedElement) {
                return ((IndexedElement) object).getFileObject();
            }
*/
            return fileObject;
        }

        public String getMimeType() {
            return MirahTokenId.MIME_TYPE;
        }

        public String getName() {
            return object.getName();
        }

        public String getIn() {
            return object.getIn();
        }

        public ElementKind getKind() {
            return object.getKind();
        }

        public Set<Modifier> getModifiers() {
            return object.getModifiers();
        }

        // FIXME parsing API
        public OffsetRange getOffsetRange(ParserResult result) {
            return OffsetRange.NONE;
        }

    }

    // FIXME could it be ElementKind.OTHER or can we use url?
    private static class SimpleElementHandle implements ElementHandle {

        private final String className;

        private final String elementName;

        private final ElementKind kind;

        private final Set<Modifier> modifiers;

        public SimpleElementHandle(String className, String elementName, ElementKind kind,
                Set<Modifier> modifiers) {
            this.className = className;
            this.elementName = elementName;
            this.kind = kind;
            this.modifiers = modifiers;
        }

        public FileObject getFileObject() {
            return null;
        }

        public String getIn() {
            return className;
        }

        public ElementKind getKind() {
            return kind;
        }

        public String getMimeType() {
            return MirahTokenId.MIME_TYPE;
        }

        public Set<Modifier> getModifiers() {
            return modifiers;
        }

        public String getName() {
            return elementName;
        }

        public boolean signatureEquals(ElementHandle handle) {
            // FIXME
            return false;
        }

        public OffsetRange getOffsetRange(ParserResult result) {
            return OffsetRange.NONE;
        }


    }

}
