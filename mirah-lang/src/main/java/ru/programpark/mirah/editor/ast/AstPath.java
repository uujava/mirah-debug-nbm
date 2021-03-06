package ru.programpark.mirah.editor.ast;

import java.beans.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Scanner;
import javax.swing.text.BadLocationException;
import mirah.lang.ast.ClassDefinition;
import mirah.lang.ast.Node;
import mirah.lang.ast.NodeScanner;
import mirah.lang.ast.Script;
import org.netbeans.editor.BaseDocument;
import org.openide.util.Exceptions;

/**
 * This represents a path in a Mirah AST.
 *
 * @author Tor Norbye
 * @author Martin Adamek
 */
public class AstPath implements Iterable<Node> {
    
    private ArrayList<Node> path = new ArrayList<Node>(30);

//    private int lineNumber = -1;

//    private int columnNumber = -1;

    public AstPath() {
        super();
    }

    public AstPath(Node root, int caretOffset, BaseDocument document) {
        try {
            // make sure offset is not higher than document length, see #138353
            int length = document.getLength();
            int offset = length == 0 ? 0 : caretOffset + 1;
            if (length > 0 && offset >= length) {
                offset = length - 1;
            }
            findPathTo(root, caretOffset);
        } 
//        catch (BadLocationException ble) {
        catch (Exception ble) {
            Exceptions.printStackTrace(ble);
        }
    }

    /**
     * Find the path to the given node in the AST
     */
    @SuppressWarnings("unchecked")
    public AstPath(Node node, Node target) {
        if (!find(node, target)) {
            path.clear();
        } else {
            // Reverse the list such that node is on top
            // When I get time rewrite the find method to build the list that way in the first place
            Collections.reverse(path);
        }
    }
    public void descend(Node node) {
        path.add(node);
    }

    public void ascend() {
        path.remove(path.size() - 1);
    }

    /**
     * Find the position closest to the given offset in the AST. Place the path from the leaf up to the path in the
     * passed in path list.
     */
    @SuppressWarnings("unchecked")
    private Node findPathTo(Node node, int offset) {
        
        assert node != null : "Node should not be null";
        assert node instanceof Script : "Node must be a ModuleNode";
//        assert line >=0 : "line number was negative: " + line + " on the ModuleNode node with main class name: " + ((Script)node).getMainClassName();
        assert offset >= 0 : "offset was negative: " + offset;
        
        path.addAll(find(node, offset));

        // in scripts ClassDefinition is not in path, let's add it
        // find class that has same name as the file
        if (path.isEmpty() || !(path.get(0) instanceof ClassDefinition)) {
            Script script = (Script) node;
            String name = null; //script.getContext().getName();
            int index = name == null ? -1 : name.lastIndexOf(".vrb"); // NOI18N
            if (index != -1) {
                name = name.substring(0, index);
            }
            index = name == null ? -1 : name.lastIndexOf('.');
            if (index != -1) {
                name = name.substring(index + 1);
            }
        }

        // let's fix script class - run method
        // FIXME this should be more accurate - package
        // and imports are not in the method ;)
        if (!path.isEmpty() && (path.get(0) instanceof ClassDefinition)) {
            ClassDefinition clazz = (ClassDefinition) path.get(0);
        }

        path.add(0, node);

        Node result = path.get(path.size() - 1);

        return result;
    }
    
    // находим самый маленький узел, на котором смещение
    public Node findLeaf() {
        Node leaf = null;
        for( Node node : path )
        {
            // для гиперпереходов из параметром макросов
            if ( node.position() == null ) {
                node = node.originalNode();
            }
            if (node == null || node.position() == null) {
                continue;
            }
            if ( leaf == null 
                || node.position().endChar() - node.position().startChar() 
                    < leaf.position().endChar() - leaf.position().startChar())
            leaf = node;
        }
        return leaf;
    }
    
    @SuppressWarnings("unchecked")
    private List<Node> find(Node node, int offset) {
        
        assert offset >=0 : "offset was negative: " + offset;
        assert node != null : "Node should not be null";
        assert node instanceof Script : "Node must be a ModuleNode";
        
        Script script = (Script) node;
        
        PathFinderVisitor pathFinder = new PathFinderVisitor(offset);
        script.accept(pathFinder, null);
        return pathFinder.getPath();
//        return new ArrayList<Node>();
    }
    /**
     * Find the path to the given node in the AST
     */
    @SuppressWarnings("unchecked")
    public boolean find(Node node, Node target) {
        if (node == target) {
            return true;
        }

        List<Node> children = ASTUtils.children(node);

        for (Node child : children) {
            boolean found = find(child, target);

            if (found) {
                path.add(child);

                return found;
            }
        }

        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Path(");
        sb.append(path.size());
        sb.append(")=[");

        for (Node n : path) {
            String name = n.getClass().getName();
            name = name.substring(name.lastIndexOf('.') + 1);
            sb.append(name);
            sb.append("\n");
        }

        sb.append("]");

        return sb.toString();
    }

    public Node leaf() {
        if (path.isEmpty()) {
            return null;
        } else {
            return path.get(path.size() - 1);
        }
    }

    public Node leafParent() {
        if (path.size() < 2) {
            return null;
        } else {
            return path.get(path.size() - 2);
        }
    }

    public Node leafGrandParent() {
        if (path.size() < 3) {
            return null;
        } else {
            return path.get(path.size() - 3);
        }
    }

    public Node root() {
        if (path.isEmpty()) {
            return null;
        } else {
            return path.get(0);
        }
    }

    /** Return an iterator that returns the elements from the leaf back up to the root */
    @Override
    public Iterator<Node> iterator() {
        return new LeafToRootIterator(path);
    }

    /** REturn an iterator that starts at the root and walks down to the leaf */
    public ListIterator<Node> rootToLeaf() {
        return path.listIterator();
    }

    /** Return an iterator that walks from the leaf back up to the root */
    public ListIterator<Node> leafToRoot() {
        return new LeafToRootIterator(path);
    }

    private static class LeafToRootIterator implements ListIterator<Node> {
        private final ListIterator<Node> it;

        private LeafToRootIterator(ArrayList<Node> path) {
            it = path.listIterator(path.size());
        }

        @Override
        public boolean hasNext() {
            return it.hasPrevious();
        }

        @Override
        public Node next() {
            return it.previous();
        }

        @Override
        public boolean hasPrevious() {
            return it.hasNext();
        }

        @Override
        public Node previous() {
            return it.next();
        }

        @Override
        public int nextIndex() {
            return it.previousIndex();
        }

        @Override
        public int previousIndex() {
            return it.nextIndex();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void set(Node arg0) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void add(Node arg0) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

}
