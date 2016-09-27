/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.programpark.mirah.tests;

import java.util.Collections;
import java.util.Iterator;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import ru.programpark.mirah.lexer.MirahParserResult;
import mirah.lang.ast.Node;
import org.mirah.typer.ResolvedType;
import org.netbeans.api.editor.EditorRegistry;
import org.netbeans.editor.BaseDocument;
import org.netbeans.modules.parsing.api.ParserManager;
import org.netbeans.modules.parsing.api.ResultIterator;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.api.UserTask;
import org.netbeans.modules.parsing.spi.Parser.Result;
import org.openide.filesystems.FileObject;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;

import static ru.programpark.mirah.tests.ParseMirah.getFileObject;
import ru.programpark.mirah.editor.ast.ASTUtils;
import ru.programpark.mirah.editor.ast.AstPath;
import ru.programpark.mirah.editor.navigator.VariablesCollector;

/**
 *
 * @author savushkin
 */
public class PrintAst {
/*
    private static class SourceLocator implements Task<CompilationController> {

        private final String fqName;

        private final ClasspathInfo cpi;

        private final CountDownLatch latch;

        private DeclarationFinder.DeclarationLocation location = DeclarationFinder.DeclarationLocation.NONE;
//        private Object ElementDeclaration;

        public SourceLocator(String fqName, ClasspathInfo cpi, CountDownLatch latch) {
            this.fqName = fqName;
            this.cpi = cpi;
            this.latch = latch;
        }

        public void run(CompilationController info) throws Exception {
            Elements elements = info.getElements();

            if (elements != null) {
                final javax.lang.model.element.TypeElement typeElement = ElementSearch.getClass(elements, fqName);

                if (typeElement != null) {
                    DeclarationFinder.DeclarationLocation found = ElementDeclaration.getDeclarationLocation(cpi, typeElement);
                    synchronized (this) {
                        location = found;
                    }
                } else {
//                    LOG.log(Level.FINEST, "typeElement == null"); // NOI18N
                }
            } else {
//                LOG.log(Level.FINEST, "elements == null"); // NOI18N
            }
            latch.countDown();
        }

        public synchronized DeclarationFinder.DeclarationLocation getLocation() {
            return location;
        }
    }
    */
    public static void putException(Exception e) {
        final InputOutput io = IOProvider.getDefault().getIO("Proposals", false);
        try {
            for (StackTraceElement st : e.getStackTrace()) {
                io.getOut().println(">>" + st);
            }
        } catch (Exception ee) {
            e.printStackTrace();
        } finally {
            if (io != null) {
                io.getOut().close();
            }
            if (io != null) {
                io.getErr().close();
            }
        }
    }

    public static void putNode(MirahParserResult parsed, Node node, InputOutput io) {
        ResolvedType type = parsed.getResolvedTypes().get(node);
        Node p = node.parent();
        if (type != null) {
            io.getOut().println("AST: " + node
                    + " [" + node.position().startLine() + "," + node.position().startColumn() + "-" + node.position().endLine()
                    + "," + node.position().endColumn() + "] {" + node.position().startChar() + "-" + node.position().endChar() + "} "
                    + node.hashCode() + "/ parent=" + p + "["+( p != null ? p.hashCode() : "0")+ "] / type: " + type);
        } else {
            io.getOut().println("AST: " + node
                    + " [" + node.position().startLine() + "," + node.position().startColumn() + "-" + node.position().endLine()
                    + "," + node.position().endColumn() + "] {" + node.position().startChar() + "-" + node.position().endChar() + "} "
                    + node.hashCode() + "/ parent=" + p+ "[" + (p != null ? p.hashCode() : "0") + "]");
        }
    }
/*
    private static DeclarationFinder.DeclarationLocation findJavaClass(String fqName,
            FileObject fileObject ) throws BadLocationException {

            final ClasspathInfo cpi = ClasspathInfo.create(fileObject);

            if (cpi != null) {
                JavaSource javaSource = JavaSource.create(cpi);

                if (javaSource != null) {
                    CountDownLatch latch = new CountDownLatch(1);
                    SourceLocator locator = new SourceLocator(fqName, cpi, latch);
                    try {
                        javaSource.runUserActionTask(locator, true);
                    } catch (IOException ex) {
//                            LOG.log(Level.FINEST, "Problem in runUserActionTask :  {0}", ex.getMessage());
                        return DeclarationFinder.DeclarationLocation.NONE;
                    }
                    try {
                        latch.await();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        return DeclarationFinder.DeclarationLocation.NONE;
                    }
                    return locator.getLocation();
                } else {
//                        LOG.log(Level.FINEST, "javaSource == null"); // NOI18N
                }
            } else {
//                    LOG.log(Level.FINEST, "classpathinfo == null"); // NOI18N
            }
        return DeclarationFinder.DeclarationLocation.NONE;
    }

    public static void putClasspathInfo( String fileName, InputOutput io )
    {
        try {
            FileObject fileObject = FileUtil.toFileObject(new File(fileName));
            ClasspathInfo cpi = ClasspathInfo.create(fileObject);
            io.getOut().println("------------- ClasspathInfo=" + fileName + " ---------------------");
            io.getOut().println("CPI: "+cpi.toString());
            DeclarationFinder.DeclarationLocation location = findJavaClass("javafx.scene.control.ContextMenu",fileObject);
            io.getOut().println("LOCATION: " + location);
        }
        catch( Exception ex ) {
            ex.printStackTrace();
            io.getOut().println("CPI: " + ex);
        }
    } 
    */
    public static void putAstPath(BaseDocument bdoc, MirahParserResult parsed, int caretOffset, InputOutput io) {
        long curr = System.currentTimeMillis();
        try {
            io.getOut().println("------------- caretOffset=" + caretOffset + " ---------------------");
            Node leaf = ASTUtils.findLeaf(parsed, bdoc, caretOffset);
            while (leaf != null) {
                putNode(parsed, leaf, io);
                leaf = leaf.parent();
            }
            io.getOut().println("====================================================================");
            AstPath path = ASTUtils.getPath(parsed, bdoc, caretOffset);
            if (path != null) {
                for (Iterator<Node> it = path.iterator(); it.hasNext();) {
                    Node node = it.next();
                    putNode(parsed, node, io);
                }
            }
            io.getOut().println("====================================================================");
            VariablesCollector vc = new VariablesCollector(parsed,ASTUtils.findLeaf(parsed, bdoc, caretOffset), bdoc, caretOffset);
//            VariablesCollector vc = new VariablesCollector(path,bdoc,caretOffset);
            vc.collect();
            for (String name : vc.getVariables()) {
                io.getOut().println("VARIABLE: " + name);
            }
            io.getOut().println("====================================================================");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Represents a hyperlinked line in an InputOutput.
     */
    public static void printAst() {
        final JTextComponent focused = EditorRegistry.lastFocusedComponent();
        if (focused == null) {
            return;
        }
        final Document doc = focused.getDocument();
        if (doc == null) {
            return;
        }

        FileObject fo = getFileObject(doc);
        if (!fo.getExt().equals("vrb")) {
            return;
        }

        final InputOutput io = IOProvider.getDefault().getIO(fo.getNameExt(), false);

        final long start = System.currentTimeMillis();

        try {
            io.select();
            io.getOut().println("CaretPosition = " + focused.getCaretPosition());
            final long curr = System.currentTimeMillis();
            ParserManager.parse(Collections.singleton(Source.create(doc)), new UserTask() {
                @Override
                public void run(ResultIterator resultIterator) throws Exception {
                    Result res = resultIterator.getParserResult(0); // (offset);
                    if (res instanceof MirahParserResult) {
                        io.getOut().println("==== PARSING TIME: " + (System.currentTimeMillis() - start) + " msec");
                        MirahParserResult pres = (MirahParserResult) res;
                        int offset = focused.getCaretPosition();
                        BaseDocument bdoc = (BaseDocument) doc;
                        putAstPath(bdoc, pres, offset, io);
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            io.getOut().println("==== ELAPSED TIME : " + (System.currentTimeMillis() - start) + " msec");
            if (io != null) {
                io.getOut().close();
            }
            if (io != null) {
                io.getErr().close();
            }
        }
    }

}
