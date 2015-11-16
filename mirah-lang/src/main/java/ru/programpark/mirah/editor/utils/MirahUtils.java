package ru.programpark.mirah.editor.utils;

import ca.weblite.netbeans.mirah.LOG;
import ca.weblite.netbeans.mirah.lexer.MirahParser;
import ca.weblite.netbeans.mirah.lexer.MirahTokenId;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import mirah.impl.Tokens;
import mirah.lang.ast.FieldDeclaration;
import mirah.lang.ast.Node;
import mirah.lang.ast.NodeFilter;
import mirah.lang.ast.NodeScanner;
import mirah.lang.ast.Position;
import org.mirah.typer.ResolvedType;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.editor.BaseDocument;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.filesystems.FileObject;

/**
 *
 * @author Tor Norbye
 * @author Martin Adamek
 * @author Gopala Krishnan Sankaran
 */
public final class MirahUtils {

    /**
     * Return substring after last dot.
     * @param fqn fully qualified type name
     * @return singe typename without package, or method without type
     */
    public static String stripPackage(String fqn) {
        if (fqn.contains(".")) {
            int idx = fqn.lastIndexOf(".");
            fqn = fqn.substring(idx + 1);
        }
        return fqn.replace(";", "");
    }

    /**
     * Gets only package name for the given fully qualified name.
     *
     * @param fqn fully qualified name
     * @return only package name or empty string if the default package is used
     */
    public static String stripClassName(String fqn) {
        // In case of default package
        if (!fqn.contains(".")) { // NOI18N
            return ""; // NOI18N
        } else {
            return fqn.substring(0, fqn.lastIndexOf(".") + 1); // NOI18N
        }
    }

    /**
     * Gets only package name for the given fully qualified name.
     *
     * @param fqn fully qualified name
     * @return only package name or empty string if the default package is used
     */
    public static String getPackageName(String fqn) {
        // In case of default package
        if (!fqn.contains(".")) { // NOI18N
            return ""; // NOI18N
        } else {
            return fqn.substring(0, fqn.lastIndexOf(".")); // NOI18N
        }
    }

    public static boolean isRowWhite(String text, int offset) throws BadLocationException {
        try {
            // Search forwards
            for (int i = offset; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '\n') {
                    break;
                }
                if (!Character.isWhitespace(c)) {
                    return false;
                }
            }
            // Search backwards
            for (int i = offset-1; i >= 0; i--) {
                char c = text.charAt(i);
                if (c == '\n') {
                    break;
                }
                if (!Character.isWhitespace(c)) {
                    return false;
                }
            }
            
            return true;
        } catch (IndexOutOfBoundsException ex) {
            throw getBadLocationException(ex, text, offset);
        }
    }

    public static boolean isRowEmpty(String text, int offset) throws BadLocationException {
        try {
            if (offset < text.length()) {
                char c = text.charAt(offset);
                if (!(c == '\n' || (c == '\r' && (offset == text.length()-1 || text.charAt(offset+1) == '\n')))) {
                    return false;
                }
            }
            
            if (!(offset == 0 || text.charAt(offset-1) == '\n')) {
                // There's previous stuff on this line
                return false;
            }

            return true;
        } catch (IndexOutOfBoundsException ex) {
            throw getBadLocationException(ex, text, offset);
        }
    }

    public static int getRowLastNonWhite(String text, int offset) throws BadLocationException {
        try {
            // Find end of line
            int i = offset;
            for (; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '\n' || (c == '\r' && (i == text.length()-1 || text.charAt(i+1) == '\n'))) {
                    break;
                }
            }
            // Search backwards to find last nonspace char from offset
            for (i--; i >= 0; i--) {
                char c = text.charAt(i);
                if (c == '\n') {
                    return -1;
                }
                if (!Character.isWhitespace(c)) {
                    return i;
                }
            }

            return -1;
        } catch (IndexOutOfBoundsException ex) {
            throw getBadLocationException(ex, text, offset);
        }
    }

    public static int getRowFirstNonWhite(String text, int offset) throws BadLocationException {
        try {
            // Find start of line
            int i = offset-1;
            if (i < text.length()) {
                for (; i >= 0; i--) {
                    char c = text.charAt(i);
                    if (c == '\n') {
                        break;
                    }
                }
                i++;
            }
            // Search forwards to find first nonspace char from offset
            for (; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '\n') {
                    return -1;
                }
                if (!Character.isWhitespace(c)) {
                    return i;
                }
            }

            return -1;
        } catch (IndexOutOfBoundsException ex) {
            throw getBadLocationException(ex, text, offset);
        }
    }

    public static int getRowStart(String text, int offset) throws BadLocationException {
        try {
            // Search backwards
            for (int i = offset-1; i >= 0; i--) {
                char c = text.charAt(i);
                if (c == '\n') {
                    return i+1;
                }
            }

            return 0;
        } catch (IndexOutOfBoundsException ex) {
            throw getBadLocationException(ex, text, offset);
        }
    }
    
    private static BadLocationException getBadLocationException(IndexOutOfBoundsException ex, String text, int offset) {
        BadLocationException ble = new BadLocationException(offset + " out of " + text.length(), offset);
        ble.initCause(ex);
        return ble;
    }
    public static FieldDeclaration[] findFields(final MirahParser.DocumentDebugger dbg, final int rightEdge, final boolean isClassVar) {
        final ArrayList<FieldDeclaration> foundNodes = new ArrayList<FieldDeclaration>();
        for (Object node : dbg.compiler.compiler().getParsedNodes()) {
            if (node instanceof Node) {
                ((Node) node).accept(new NodeScanner() {
                    @Override
                    public boolean enterFieldDeclaration(FieldDeclaration node, Object arg) {
                        if (node.isStatic() == isClassVar) {
                            foundNodes.add(node);
                        }
                        LOG.info(this, "findFields fields = " + super.enterFieldDeclaration(node, arg));

                        return super.enterFieldDeclaration(node, arg);
                    }

                }, null);
            }
        };
        LOG.info(MirahUtils.class, "findFields = [[]");

        return foundNodes.toArray(new FieldDeclaration[0]);
    }

    public static Node findNode(final MirahParser.DocumentDebugger dbg, final int rightEdge) {
        final Node[] foundNode = new Node[1];

        for (Object node : dbg.compiler.compiler().getParsedNodes()) {

            if (node instanceof Node) {
                ((Node) node).accept(new NodeScanner() {

                    @Override
                    public boolean enterDefault(Node node, Object arg) {
                        if (node != null) {
                            Position nodePos = node.position();
                            ResolvedType type = dbg.getType(node);

//                            if ( nodePos != null )
//                            LOG.info(MirahUtils.class,"enterDefault node = " + node + " ["+nodePos.startLine() +"," + nodePos.startChar()+"," + nodePos.startColumn()+"-"+nodePos.endLine()+","+nodePos.endChar()+"," + nodePos.endColumn()+"] type ="+type+" rightEdge="+rightEdge);
//                            else
//                            LOG.info(MirahUtils.class,"enterDefault node = " + node + " pos="+nodePos +" type ="+type+" rightEdge="+rightEdge);
                            if (type != null && nodePos != null && nodePos.endChar() == rightEdge) {
                                foundNode[0] = node;
                            } else if (nodePos != null && nodePos.endChar() == rightEdge) {

                            } else {

                            }
                        }
                        return super.enterDefault(node, arg); //To change body of generated methods, choose Tools | Templates.
                    }
                }, null);
                //walkTree((Node)node);

            }
        };
//        LOG.info(MirahUtils.class,"findNode node[0] = " + foundNode[0]);

        return foundNode[0];
    }
    static public Class findClass(FileObject o, String name) {
        LOG.info(MirahUtils.class, "findClass o=" + o + " name=" + name);
        return findClass(o, name, true);
    }

    static Class findClass(FileObject o, String name, boolean cache) {
        Project proj = FileOwnerQuery.getOwner(o);
        FileObject projectDirectory = proj.getProjectDirectory();
        LOG.info(MirahUtils.class, "findClass o=" + o + " name=" + name);
        LOG.info(MirahUtils.class, "findClass proj=" + proj + " projectDirectory=" + projectDirectory);

        ClassPath[] paths = new ClassPath[]{
            ClassPath.getClassPath(o, ClassPath.SOURCE),
            ClassPathSupport.createClassPath(new File(projectDirectory.getPath(), "src").getPath()),
            ClassPath.getClassPath(o, ClassPath.EXECUTE),
            ClassPath.getClassPath(o, ClassPath.COMPILE),
            ClassPath.getClassPath(o, ClassPath.BOOT),};

        for (int i = 0; i < paths.length; i++) {
            ClassPath cp = paths[i];
            LOG.info(MirahUtils.class, "findClass cp[" + i + "] =" + cp);
            try {

                Class c = cp.getClassLoader(true).loadClass(name);
                if (c != null) {
                    LOG.info(MirahUtils.class, "findClass c =" + c);
                    return c;
                }
            } catch (ClassNotFoundException ex) {

            }
        }

        LOG.info(MirahUtils.class, "findClass NULL");

        return null;
    }

    private static void walkTree(Node node) {
        NodeFilter f = new NodeFilter() {

            @Override
            public boolean matchesNode(Node node) {

                return true;
            }

        };

        List nodes = node.findChildren(f);

        for (Object o : nodes) {
            if (o instanceof Node) {
                walkTree((Node) o);
            }
        }

    }

    private static String nodeToString(Node n) {
        if (n == null || n.position() == null) {
            if (n != null) {
                return "" + n;
            }
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Node ").append(n)
                .append(n.position().startLine())
                .append(".")
                .append(n.position().startColumn())
                .append(":")
                .append(n.position().startChar())
                .append("-")
                .append(n.position().endLine())
                .append(".")
                .append(n.position().endColumn())
                .append(":")
                .append(n.position().endChar())
                .append(" # ");

        return sb.toString();

    }

    public static TokenSequence<MirahTokenId> mirahTokenSequence(Document doc, int caretOffset, boolean backwardBias) {
        BaseDocument bd = (BaseDocument) doc;
        try {
            bd.readLock();
            TokenHierarchy<?> hi = TokenHierarchy.get(doc);
            List<TokenSequence<?>> tsList = hi.embeddedTokenSequences(caretOffset, backwardBias);
            // Go from inner to outer TSes
            for (int i = tsList.size() - 1; i >= 0; i--) {
                TokenSequence<?> ts = tsList.get(i);
                if (ts.languagePath().innerLanguage() == MirahTokenId.getLanguage()) {
                    TokenSequence<MirahTokenId> javaInnerTS = (TokenSequence<MirahTokenId>) ts;
                    //bd.readUnlock();

//                    while( true )
//                    {
//                        String text = javaInnerTS.token().text().toString();
//                        if ( text != null ) text = text.replaceAll("\n","\\n");
//                        LOG.info(MirahUtils.class,"toks3: " + javaInnerTS.token().id().name()+" offset="+javaInnerTS.token().offset(hi)+ " text:"+ text);
//                        if ( ! javaInnerTS.moveNext() ) break;
//                    }
                    javaInnerTS.moveStart();
                    return javaInnerTS;
                }
            }
        } finally {
            bd.readUnlock();
        }
        return null;
    }

    public static int getEndOfLine(Document doc, int caretOffset) {
        BaseDocument bd = (BaseDocument) doc;
        bd.readLock();
        try {
            TokenHierarchy<?> hi = TokenHierarchy.get(doc);
            TokenSequence<MirahTokenId> toks = mirahTokenSequence(doc, caretOffset, false);
            toks.moveNext();
            MirahTokenId eol = MirahTokenId.get(Tokens.tNL.ordinal());
            MirahTokenId eof = MirahTokenId.get(Tokens.tEOF.ordinal());
            while (!eol.equals(toks.token().id()) && !eof.equals(toks.token().id())) {
                String text = toks.token() == null ? null : toks.token().id().name();
                String text2 = toks.token() == null ? null : toks.token().text().toString();

                if (!toks.moveNext()) {
                    break;
                }
            }
            int off = toks.token().offset(hi);
            return off;
        } finally {
            bd.readUnlock();
        }

    }

    public static int getBeginningOfLine(Document doc, int caretOffset) {
        BaseDocument bd = (BaseDocument) doc;
        bd.readLock();

        LOG.info(MirahUtils.class, "getBeginningOfLine doc=" + doc + " caretOffset =" + caretOffset);

        try {
            TokenHierarchy<?> hi = TokenHierarchy.get(doc);
            TokenSequence<MirahTokenId> toks = mirahTokenSequence(doc, caretOffset, true);
            toks.moveNext();
            MirahTokenId eol = MirahTokenId.get(Tokens.tNL.ordinal());
            //MirahTokenId eof = MirahTokenId.get(Tokens.tEOF.ordinal());
            while (!eol.equals(toks.token().id())) {
                String text = toks.token() == null ? null : toks.token().id().name();
                String text2 = toks.token() == null ? null : toks.token().text().toString();
                if (!toks.movePrevious()) {
                    break;
                }
            }
            int off = toks.token().offset(hi) + toks.token().length();

            return off;
        } finally {
            bd.readUnlock();
        }
    }

}
