package ca.weblite.netbeans.mirah.hyperlinks;

import ca.weblite.netbeans.mirah.LOG;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.Scope;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.AbstractElementVisitor6;
import javax.lang.model.util.ElementFilter;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.EditorRegistry;
import org.netbeans.api.editor.completion.Completion;
import org.netbeans.api.java.lexer.JavaTokenId;
import org.netbeans.api.java.lexer.JavadocTokenId;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.SourceUtils;
import org.netbeans.api.java.source.TreeUtilities;
import org.netbeans.api.java.source.UiUtils;
import org.netbeans.api.java.source.ui.ElementJavadoc;
import org.netbeans.api.java.source.ui.ElementOpen;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.api.progress.ProgressUtils;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.editor.BaseDocument;
import org.netbeans.editor.ext.ToolTipSupport;
import org.netbeans.lib.editor.hyperlink.spi.HyperlinkType;
import org.netbeans.lib.editor.util.StringEscapeUtils;
import org.netbeans.modules.csl.spi.ParserResult;
//import org.netbeans.modules.java.editor.javadoc.JavadocImports;
import org.netbeans.modules.parsing.api.ParserManager;
import org.netbeans.modules.parsing.api.ResultIterator;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.api.UserTask;
import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.modules.parsing.spi.Parser.Result;
import org.openide.awt.HtmlBrowser;
import org.openide.awt.StatusDisplayer;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;

/**
 *
 * @author Jan Lahoda
 */
public class GoToSupport {

    static String mirah_file_name = "c:\\mirah-debug\\mirah-dsl-parent\\jfx-controller-framework-test\\src\\main\\mirah\\ru\\programpark\\vector\\jfx\\controller\\framework\\BaseController.mirah";

    
    /** Creates a new instance of GoToSupport */
    public GoToSupport() {
    }
    
    public static FileObject getFileObject(Document doc) {
        DataObject od = (DataObject) doc.getProperty(Document.StreamDescriptionProperty);
        
        return od != null ? od.getPrimaryFile() : null;
    }
    
    public static String getGoToElementTooltip(final Document doc, final int offset, final boolean goToSource, final HyperlinkType type) {
        try {
            final FileObject fo = getFileObject(doc);

            if (fo == null)
                return null;

            final String[] result = new String[1];

            ParserManager.parse(Collections.singleton (Source.create(doc)), new UserTask() {
                @Override
                public void run(ResultIterator resultIterator) throws Exception {
                    Result res = resultIterator.getParserResult (offset);
                    CompilationController controller = res != null ? CompilationController.get(res) : null;
                    if (controller == null || controller.toPhase(Phase.RESOLVED).compareTo(Phase.RESOLVED) < 0)
                        return;

                    Context resolved = resolveContext(controller, doc, offset, goToSource);

                    if (resolved != null) {
                        result[0] = computeTooltip(controller, doc, resolved, type);
                    }
                }
            });

            return result[0];
        } catch (ParseException ex) {
            throw new IllegalStateException(ex);
        }
    }
    
    private static boolean isError(Element el) {
        return el == null || el.asType() == null || el.asType().getKind() == TypeKind.ERROR;
    }
    
    private static void performGoTo(final Document doc, final int offset, final boolean goToSource, final boolean javadoc) {
        final AtomicBoolean cancel = new AtomicBoolean();
//        performGoToImpl(doc, offset, goToSource, javadoc, cancel);
//		if ( true ) return;
		ProgressUtils.runOffEventDispatchThread(new Runnable() {
            public void run() {
//                openMe();
                performGoToImpl(doc, offset, goToSource, javadoc, cancel);
            }
//        }, NbBundle.getMessage(GoToSupport.class, javadoc ? "LBL_GoToJavadoc" : goToSource ? "LBL_GoToSource" : "LBL_GoToDeclaration"), cancel, false);
        }, "L_GoToDeclaration", cancel, false);
    }

//    static String mirah_file_name = "c:\\mirah-debug\\mirah-dsl-parent\\jfx-controller-framework-test\\src\\main\\mirah\\ru\\programpark\\vector\\jfx\\controller\\framework\\BaseController.mirah";
    
    private static void openMe()
    {
        try {
            FileObject f1 = FileUtil.createData(new File(mirah_file_name));
            LOG.info(GoToSupport.class,"fo="+f1);
            if ( f1 != null ) CALLER.open(f1,946);
        }
        catch( Exception e) 
        {
            e.printStackTrace();
//            LOG.exception(GoToSupport.class, e);
        }
    }
    
    private static void performGoToImpl (final Document doc, final int offset, final boolean goToSource, final boolean javadoc, final AtomicBoolean cancel) {
    
        try {
//        ((BaseDocument)doc).readLock();
            final FileObject fo = getFileObject(doc);
//            LOG.info(GoToSupport.class,"performClickAction doc="+doc);
//            LOG.info(GoToSupport.class,"performClickAction fo="+fo);
//            LOG.info(GoToSupport.class,"performClickAction offset="+offset);

    
            
            if (fo == null)
                return ;
        Project project = FileOwnerQuery.getOwner(fo);
//        LOG.info(GoToSupport.class,"performClickAction project33="+project);
        
                    
        final MirahHyperlinkHelper helper = new MirahHyperlinkHelper(project.getProjectDirectory());
            
            final int[] offsetToOpen = new int[] {-1};
            final ElementHandle[] elementToOpen = new ElementHandle[1];
            final String[] displayNameForError = new String[1];
            final boolean[] tryToOpen = new boolean[1];
            final ClasspathInfo[] cpInfo = new ClasspathInfo[1];
            
            ParserManager.parse(Collections.singleton (Source.create(doc)), new UserTask() {
                @Override
                public void run(ResultIterator resultIterator) throws Exception {
                    Result res = resultIterator.getParserResult(0); // (offset);

                    HyperlinkElement he = helper.analyze(doc, (ParserResult)res, offset);
                    if ( he != null )
                    {
                        try {
                            FileObject f1 = FileUtil.toFileObject(new File(he.getUrl()));
                            CALLER.open(f1, offset);
                        }
                        catch( Exception e)
                        {
                            LOG.exception(null, e);
                            e.printStackTrace();
                        }
                    }
                    if (cancel != null && cancel.get())
                        return ;
                    CompilationController controller = res != null ? CompilationController.get(res) : null;
                    if (controller == null || controller.toPhase(Phase.RESOLVED).compareTo(Phase.RESOLVED) < 0)
                        return;
                    cpInfo[0] = controller.getClasspathInfo();

                    Context resolved = resolveContext(controller, doc, offset, goToSource);

                    if (resolved == null) {
//                        CALLER.beep(goToSource, javadoc);
                        return;
                    }
                    
                    if (javadoc) {
                        final URL url = SourceUtils.getPreferredJavadoc(resolved.resolved);
                        if (url != null) {
                            HtmlBrowser.URLDisplayer.getDefault().showURL(url);
                        } else {
//                            CALLER.beep(goToSource, javadoc);
                        }
                    } else {
                        TreePath elpath = getPath(controller, resolved.resolved);
                        
                        if (elpath != null) {
                            Tree tree = elpath.getLeaf();
                            long startPos = controller.getTrees().getSourcePositions().getStartPosition(controller.getCompilationUnit(), tree);
                            
                            if (startPos != (-1)) {
                                //check if the caret is inside the declaration itself, as jump in this case is not very usefull:
                                if (isCaretInsideDeclarationName(controller, tree, elpath, offset)) {
//                                    CALLER.beep(goToSource, javadoc);
                                } else {
                                    //#71272: it is necessary to translate the offset:
                                    offsetToOpen[0] = controller.getSnapshot().getOriginalOffset((int) startPos);
                                    displayNameForError[0] = null; //Utilities.getElementName(resolved.resolved, false).toString();
                                    tryToOpen[0] = true;
                                }
                            } else {
//                                CALLER.beep(goToSource, javadoc);
                            }
                        } else {
                            elementToOpen[0] = ElementHandle.create(resolved.resolved);
                            displayNameForError[0] = null; //Utilities.getElementName(resolved.resolved, false).toString();
                            tryToOpen[0] = true;
                        }
                    }
                }
            });
            
            if (tryToOpen[0]) {
                boolean openSucceeded = false;

                if (cancel.get()) return ;

                if (offsetToOpen[0] >= 0) {
                    openSucceeded = CALLER.open(fo, offsetToOpen[0]);
                } else {
                    if (elementToOpen[0] != null) {
                        openSucceeded = CALLER.open(cpInfo[0], elementToOpen[0]);
                    }
                }
                if (!openSucceeded) {
                    CALLER.warnCannotOpen(displayNameForError[0]);
                }
            }
        } catch (ParseException ex) {
            throw new IllegalStateException(ex);
        }
    }
    public static void goTo(final Document doc, final int offset, final boolean goToSource) {
//        FileObject fo = null;
//        try {
//            fo = FileUtil.createData(new File(mirah_file_name));
//            CALLER.open(fo, 23);
//        } catch (Exception ex){
//            ex.printStackTrace();
//        }
//        LOG.info(GoToSupport.class,"----------------goTo doc="+doc);
        try {
            performGoTo(doc, offset, goToSource, false);
        }
        catch( Exception e )
        {
//            LOG.exception(null, e);
        }
    }
    
    public static void goToJavadoc(Document doc, int offset) {
        performGoTo(doc, offset, false, true);
    }

    public static Context resolveContext(CompilationInfo controller, Document doc, int offset, boolean goToSource) {
        Token<JavaTokenId>[] token = new Token[1];
        int[] span = getIdentifierSpan(doc, offset, token);

        if (span == null) {
            return null;
        }

        int exactOffset = controller.getSnapshot().getEmbeddedOffset(span[0] + 1);

        Element el = null;
        TypeMirror classType = null;
        boolean insideImportStmt = false;
        TreePath path = controller.getTreeUtilities().pathFor(exactOffset);

        if (token[0] != null && token[0].id() == JavaTokenId.JAVADOC_COMMENT) {
            el = null; //JavadocImports.findReferencedElement(controller, offset);
        } else {
            TreePath parent = path.getParentPath();

            if (parent != null) {
                Tree parentLeaf = parent.getLeaf();

                if (parentLeaf.getKind() == Kind.NEW_CLASS && ((NewClassTree) parentLeaf).getIdentifier() == path.getLeaf()) {
                    if (!isError(controller.getTrees().getElement(path.getParentPath()))) {
                        path = path.getParentPath();
                    }
                } else if (parentLeaf.getKind() == Kind.IMPORT && ((ImportTree) parentLeaf).isStatic()) {
                    el = null; //handleStaticImport(controller, (ImportTree) parentLeaf);
                    insideImportStmt = true;
                } else {
                    if (   parentLeaf.getKind() == Kind.PARAMETERIZED_TYPE
                        && parent.getParentPath().getLeaf().getKind() == Kind.NEW_CLASS
                        && ((ParameterizedTypeTree) parentLeaf).getType() == path.getLeaf()) {
                        if (!isError(controller.getTrees().getElement(parent.getParentPath()))) {
                            path = parent.getParentPath();
                            classType = controller.getTrees().getTypeMirror(path);
                        }
                    }
                }

                if (el == null) {
                    el = controller.getTrees().getElement(path);

                    if (parentLeaf.getKind() == Kind.METHOD_INVOCATION && isError(el)) {
                        List<ExecutableElement> ee = null; //Utilities.fuzzyResolveMethodInvocation(controller, path.getParentPath(), new ArrayList<TypeMirror>(), new int[1]);

                        if (!ee.isEmpty()) {
                            el = ee.iterator().next();
                        } else {
                            ExpressionTree select = ((MethodInvocationTree)parentLeaf).getMethodSelect();
                            Name methodName = null;
                            switch (select.getKind()) {
                                case IDENTIFIER:
                                    Scope s = controller.getTrees().getScope(path);
                                    el = s.getEnclosingClass();
                                    methodName = ((IdentifierTree)select).getName();
                                    break;
                                case MEMBER_SELECT:
                                    el = controller.getTrees().getElement(new TreePath(path, ((MemberSelectTree)select).getExpression()));
                                    methodName = ((MemberSelectTree)select).getIdentifier();
                                    break;
                            }
                            if (el != null) {
                                for (ExecutableElement m : ElementFilter.methodsIn(el.getEnclosedElements())) {
                                    if (m.getSimpleName() == methodName) {
                                        el = m;
                                        break;
                                    }
                                }
                            }
                        }
                    } else if (el != null && el.getKind() == ElementKind.ENUM_CONSTANT && path.getLeaf().getKind() == Kind.VARIABLE) {
                        Element e = controller.getTrees().getElement(new TreePath(path, ((VariableTree)path.getLeaf()).getInitializer()));
                        if (!controller.getElementUtilities().isSynthetic(e)) {
                            el = e;
                        }
                    }
                }
            } else {
                return null;
            }
        }

        if (isError(el)) {
            return null;
        }

        if (goToSource && !insideImportStmt) {
            TypeMirror type = null;

            if (el instanceof VariableElement)
                type = el.asType();

            if (type != null && type.getKind() == TypeKind.DECLARED) {
                el = ((DeclaredType)type).asElement();
            }
        }

        if (isError(el)) {
            return null;
        }

        if (controller.getElementUtilities().isSynthetic(el) && el.getKind() == ElementKind.CONSTRUCTOR) {
            //check for annonymous innerclasses:
            el = null; //handlePossibleAnonymousInnerClass(controller, el);
        }

        if (isError(el)) {
            return null;
        }

        if (el.getKind() != ElementKind.CONSTRUCTOR && (token[0].id() == JavaTokenId.SUPER || token[0].id() == JavaTokenId.THIS)) {
            return null;
        }

        return new Context(classType, el);
    }

    private static String computeTooltip(final CompilationInfo controller, final Document doc, Context resolved, HyperlinkType type) {
        DisplayNameElementVisitor v = new DisplayNameElementVisitor(controller);

        if (resolved.resolved.getKind() == ElementKind.CONSTRUCTOR && resolved.classType != null && resolved.classType.getKind() == TypeKind.DECLARED) {
            v.printExecutable(((ExecutableElement) resolved.resolved), (DeclaredType) resolved.classType, true);
        } else  {
            v.visit(resolved.resolved, true);
        }

        String result = null;
        try {
            // attempt to cancel the background task once the tooltip waiting period is over
            class Ctrl implements Callable<Boolean> {
                private volatile boolean cancel;
                
                @Override
                public Boolean call() throws Exception {
                    return cancel;
                }
                
            };
            
            final Ctrl control = new Ctrl();
            // #240060: if non-null cancel is passed, the actual URL fetch is done on background, allowing us to proceed without blocking
            final ElementJavadoc jdoc = ElementJavadoc.create(controller, resolved.resolved, control);
            Future<String> text = jdoc != null ? jdoc.getTextAsync() : null;
            result = text != null ? text.get(1, TimeUnit.SECONDS) : null;
            // signal that the task should be cancelled
            if (result != null) {
                int idx = 0;
                for (int i = 0; i < 3 && idx >= 0; i++) {
                    idx = result.indexOf("<p>", idx + 1); //NOI18N
                }
                if (idx >= 0) {
                    result = result.substring(0, idx + 3);
                    result += "<a href='***'>more...</a>"; //NOI18N
                }
                idx = result.indexOf("<p id=\"not-found\">"); //NOI18N
                if (idx >= 0) {
                    result = result.substring(0, idx);
                }
                doc.putProperty("TooltipResolver.hyperlinkListener", new HyperlinkListener() { //NOI18N
                    @Override
                    public void hyperlinkUpdate(HyperlinkEvent e) {                    
                        if (e != null && HyperlinkEvent.EventType.ACTIVATED.equals(e.getEventType())) {
                            String desc = e.getDescription();
                            if (desc != null) {
                                ElementJavadoc link = "***".contentEquals(desc) ? jdoc : jdoc.resolveLink(desc); //NOI18N
                                if (link != null) {
                                    JTextComponent comp = EditorRegistry.lastFocusedComponent();
                                    if (comp != null && comp.getDocument() == doc) {
                                        ToolTipSupport tts = org.netbeans.editor.Utilities.getEditorUI(comp).getToolTipSupport();
                                        if (tts != null) {
                                            tts.setToolTipVisible(false);
                                        }
                                    }
//                                    JavaCompletionProvider.JavaCompletionQuery.outerDocumentation.set(new JavaCompletionDoc(link));
                                    Completion.get().showDocumentation();
                                }
                            }
                        }
                    }
                });
            }
            control.cancel = true;
        } catch (Exception ex) {}
        
        
        if (result == null) {
            result = v.result.toString();
        }
        
        int overridableKind = overridableKind(resolved.resolved);

        if (overridableKind != (-1) && type != null) {
            if (type == HyperlinkType.GO_TO_DECLARATION) {
                StringBuilder sb = new StringBuilder();
                sb.append(KeyEvent.getKeyText(org.openide.util.Utilities.isMac() ? KeyEvent.VK_META : KeyEvent.VK_CONTROL)).append('+').append(KeyEvent.getKeyText(KeyEvent.VK_ALT)).append('+');
                result = NbBundle.getMessage(GoToSupport.class, "TP_OverriddenTooltipSugg", sb.toString(), overridableKind, result);//NOI18N
            } else {
                result = NbBundle.getMessage(GoToSupport.class, "TP_GoToOverriddenTooltipSugg", overridableKind, result);//NOI18N
            }
        }

        result = "<html><body>" + result;

        return result;
    }
    
    private static final Set<JavaTokenId> USABLE_TOKEN_IDS = EnumSet.of(JavaTokenId.IDENTIFIER, JavaTokenId.THIS, JavaTokenId.SUPER);
    
    public static int[] getIdentifierSpan(final Document doc, final int offset, final Token<JavaTokenId>[] token) {
        if (getFileObject(doc) == null) {
            //do nothing if FO is not attached to the document - the goto would not work anyway:
            return null;
        }
        final int[][] ret = new int[][] {null}; 
        doc.render(new Runnable() {
            @Override
            public void run() {
                TokenHierarchy th = TokenHierarchy.get(doc);
                TokenSequence<JavaTokenId> ts = SourceUtils.getJavaTokenSequence(th, offset);

                if (ts == null)
                    return;

                ts.move(offset);
                if (!ts.moveNext())
                    return;

                Token<JavaTokenId> t = ts.token();

                if (JavaTokenId.JAVADOC_COMMENT == t.id()) {
                    // javadoc hyperlinking (references + param names)
                    TokenSequence<JavadocTokenId> jdts = ts.embedded(JavadocTokenId.language());
                    if (false) { //JavadocImports.isInsideReference(jdts, offset) || JavadocImports.isInsideParamName(jdts, offset)) {
                        jdts.move(offset);
                        jdts.moveNext();
                        if (token != null) {
                            token[0] = t;
                        }
                        ret[0] = new int [] {jdts.offset(), jdts.offset() + jdts.token().length()};
                    }
                    return;
                } else { //if ( true /*!USABLE_TOKEN_IDS.contains(t.id())*/ ) {
                    ts.move(offset - 1);
                    if (!ts.moveNext())
                        return;
                    t = ts.token();
                    if (!USABLE_TOKEN_IDS.contains(t.id()))
                        return;
                }

                if (token != null)
                    token[0] = t;

                ret[0] = new int [] {ts.offset(), ts.offset() + t.length()};
            }
        });
        return ret[0];
    }
    
    private static Element handlePossibleAnonymousInnerClass(CompilationInfo info, final Element el) {
        Element encl = el.getEnclosingElement();
        Element doubleEncl = encl != null ? encl.getEnclosingElement() : null;
        
        if (   doubleEncl != null
            && !doubleEncl.getKind().isClass()
            && !doubleEncl.getKind().isInterface()
            && doubleEncl.getKind() != ElementKind.PACKAGE
            && encl.getKind() == ElementKind.CLASS) {
            TreePath enclTreePath = info.getTrees().getPath(encl);
            Tree enclTree = enclTreePath != null ? enclTreePath.getLeaf() : null;
            
            if (enclTree != null && TreeUtilities.CLASS_TREE_KINDS.contains(enclTree.getKind()) && enclTreePath.getParentPath().getLeaf().getKind() == Tree.Kind.NEW_CLASS) {
                NewClassTree nct = (NewClassTree) enclTreePath.getParentPath().getLeaf();
                
                if (nct.getClassBody() != null) {
                    Element parentElement = info.getTrees().getElement(new TreePath(enclTreePath, nct.getIdentifier()));
                    
                    if (parentElement == null || parentElement.getKind().isInterface()) {
                        return parentElement;
                    } else {
                        //annonymous innerclass extending a class. Find out which constructor is used:
                        TreePath superConstructorCall = new FindSuperConstructorCall().scan(enclTreePath, null);
                        
                        if (superConstructorCall != null) {
                            return info.getTrees().getElement(superConstructorCall);
                        }
                    }
                }
            }
            
            return null;//prevent jumps to incorrect positions
        } else {
            if (encl != null)
                return encl;
            else
                return el;
        }
    }
    
    /**
     * Tries to guess element referenced by static import. It may not be deterministic
     * as in <code>import static java.awt.Color.getColor</code>.
     */
    private static Element handleStaticImport(CompilationInfo javac, ImportTree impt) {
        Tree impIdent = impt.getQualifiedIdentifier();
        if (!impt.isStatic() || impIdent == null || impIdent.getKind() != Kind.MEMBER_SELECT) {
            return null;
        }
        
        // resolve type element containing imported element
        Trees trees = javac.getTrees();
        MemberSelectTree select = (MemberSelectTree) impIdent;
        Name mName = select.getIdentifier();
        TreePath cutPath = new TreePath(javac.getCompilationUnit());
        TreePath selectPath = new TreePath(new TreePath(cutPath, impt), select.getExpression());
        Element selectElm = trees.getElement(selectPath);
        if (isError(selectElm)) {
            return null;
        }
        
        // resolve class to determine scope
        TypeMirror clazzMir = null;
        TreePath clazzPath = null;
        List<? extends Tree> decls = javac.getCompilationUnit().getTypeDecls();
        if (!decls.isEmpty()) {
            Tree clazz = decls.get(0);
            if (TreeUtilities.CLASS_TREE_KINDS.contains(clazz.getKind())) {
                clazzPath = new TreePath(cutPath, clazz);
                Element clazzElm = trees.getElement(clazzPath);
                if (isError(clazzElm)) {
                    return null;
                }
                clazzMir = clazzElm.asType();
            }
        }
        if (clazzMir == null) {
            return null;
        }
        
        Scope clazzScope = trees.getScope(clazzPath);
        
        // choose the first acceptable member
        for (Element member : selectElm.getEnclosedElements()) {
            if (member.getModifiers().contains(Modifier.STATIC)
                    && mName.contentEquals(member.getSimpleName())
                    && trees.isAccessible(clazzScope, member, (DeclaredType)clazzMir)) {
                return member;
            }
        }
        return null;
    }
    
    private static boolean isCaretInsideDeclarationName(CompilationInfo info, Tree t, TreePath path, int caret) {
//        try {
            switch (t.getKind()) {
                case ANNOTATION_TYPE:
                case CLASS:
                case ENUM:
                case INTERFACE:
                case METHOD:
                case VARIABLE:
                    int[] span = new int[] { 0}; //org.netbeans.modules.java.editor.semantic.Utilities.findIdentifierSpan(path, info, info.getDocument());

                    if (span == null || span[0] == (-1) || span[1] == (-1)) {
                        return false;
                    }

                    return span[0] <= caret && caret <= span[1];
                default:
                    return false;
            }

//        } catch (IOException iOException) {
//            Exceptions.printStackTrace(iOException);
//            return false;
//        }
    }

    private static int overridableKind(Element el) {
        if (   el.getModifiers().contains(Modifier.FINAL)
            || el.getModifiers().contains(Modifier.PRIVATE)) {
            return -1;
        }

        if (el.getKind().isClass() || el.getKind().isInterface()) {
            return el.getModifiers().contains(Modifier.ABSTRACT) ? 0 : 1;
        }

        if (   el.getKind() == ElementKind.METHOD
            && !el.getModifiers().contains(Modifier.STATIC)
            && !el.getEnclosingElement().getModifiers().contains(Modifier.FINAL)) {
            return el.getModifiers().contains(Modifier.ABSTRACT) ? 2 : 3;
        }

        return -1;
    }

    private static TreePath getPath(final CompilationInfo info, Element el) {
        final Element toFind = info.getElementUtilities().isSynthetic(el) ? el.getEnclosingElement() : el;

        class S extends TreePathScanner<Void, Void> {
            private TreePath found;
            @Override
            public Void scan(Tree tree, Void p) {
                if (found != null) return null;
                return super.scan(tree, p);
            }
            private boolean process() {
                Element resolved = info.getTrees().getElement(getCurrentPath());
                if (toFind.equals(resolved)) {
                    found = getCurrentPath();
                    return true;
                }
                return false;
            }
            @Override
            public Void visitClass(ClassTree node, Void p) {
                if (!process()) super.visitClass(node, p);
                return null;
            }
            @Override
            public Void visitMethod(MethodTree node, Void p) {
                if (!process()) return super.visitMethod(node, p);
                return null;
            }
            @Override
            public Void visitVariable(VariableTree node, Void p) {
                if (!process()) return super.visitVariable(node, p);
                return null;
            }
            @Override
            public Void visitTypeParameter(TypeParameterTree node, Void p) {
                if (!process()) return super.visitTypeParameter(node, p);
                return null;
            }
        }

        S search = new S();

        search.scan(info.getCompilationUnit(), null);

        return search.found;
    }

    private static final class FindSuperConstructorCall extends TreePathScanner<TreePath, Void> {
        
        @Override
        public TreePath visitMethodInvocation(MethodInvocationTree tree, Void v) {
            if (tree.getMethodSelect().getKind() == Kind.IDENTIFIER && "super".equals(((IdentifierTree) tree.getMethodSelect()).getName().toString())) {
                return getCurrentPath();
            }
            
            return null;
        }
        
        @Override
        public TreePath reduce(TreePath first, TreePath second) {
            if (first == null) {
                return second;
            } else {
                return first;
            }
        }
        
    }
    
    private static final class DisplayNameElementVisitor extends AbstractElementVisitor6<Void, Boolean> {

        private final CompilationInfo info;

        public DisplayNameElementVisitor(CompilationInfo info) {
            this.info = info;
        }
        
        private StringBuffer result        = new StringBuffer();
        
        private void boldStartCheck(boolean highlightName) {
            if (highlightName) {
                result.append("<b>");
            }
        }
        
        private void boldStopCheck(boolean highlightName) {
            if (highlightName) {
                result.append("</b>");
            }
        }
        
        public Void visitPackage(PackageElement e, Boolean highlightName) {
            boldStartCheck(highlightName);
            
            result.append(e.getQualifiedName());
            
            boldStopCheck(highlightName);
            
            return null;
        }

        public Void visitType(TypeElement e, Boolean highlightName) {
            return printType(e, null, highlightName);
        }
        
        Void printType(TypeElement e, DeclaredType dt, Boolean highlightName) {
            modifier(e.getModifiers());
            switch (e.getKind()) {
                case CLASS:
                    result.append("class ");
                    break;
                case INTERFACE:
                    result.append("interface ");
                    break;
                case ENUM:
                    result.append("enum ");
                    break;
                case ANNOTATION_TYPE:
                    result.append("@interface ");
                    break;
            }
            Element enclosing = e.getEnclosingElement();
            
            if (enclosing == SourceUtils.getEnclosingTypeElement(e)) {
                result.append(((TypeElement) enclosing).getQualifiedName());
                result.append('.');
                boldStartCheck(highlightName);
                result.append(e.getSimpleName());
                boldStopCheck(highlightName);
            } else {
                result.append(e.getQualifiedName());
            }
            
            if (dt != null)
                dumpRealTypeArguments(dt.getTypeArguments());

            return null;
        }

        public Void visitVariable(VariableElement e, Boolean highlightName) {
            modifier(e.getModifiers());
            
            result.append(getTypeName(info, e.asType(), true));
            
            result.append(' ');
            
            boldStartCheck(highlightName);

            result.append(e.getSimpleName());
            
            boldStopCheck(highlightName);
            
            if (highlightName) {
                if (e.getConstantValue() != null) {
                    result.append(" = ");
                    result.append(StringEscapeUtils.escapeHtml(e.getConstantValue().toString()));
                }
                
                Element enclosing = e.getEnclosingElement();
                
                if (e.getKind() != ElementKind.PARAMETER && e.getKind() != ElementKind.LOCAL_VARIABLE
                        && e.getKind() != ElementKind.RESOURCE_VARIABLE && e.getKind() != ElementKind.EXCEPTION_PARAMETER) {
                    result.append(" in ");

                    //short typename:
                    result.append(getTypeName(info, enclosing.asType(), true));
                }
            }
            
            return null;
        }

        public Void visitExecutable(ExecutableElement e, Boolean highlightName) {
            return printExecutable(e, null, highlightName);
        }

        Void printExecutable(ExecutableElement e, DeclaredType dt, Boolean highlightName) {
            switch (e.getKind()) {
                case CONSTRUCTOR:
                    modifier(e.getModifiers());
                    dumpTypeArguments(e.getTypeParameters());
                    result.append(' ');
                    boldStartCheck(highlightName);
                    result.append(e.getEnclosingElement().getSimpleName());
                    boldStopCheck(highlightName);
                    if (dt != null) {
                        dumpRealTypeArguments(dt.getTypeArguments());
                        dumpArguments(e.getParameters(), ((ExecutableType) info.getTypes().asMemberOf(dt, e)).getParameterTypes());
                    } else {
                        dumpArguments(e.getParameters(), null);
                    }
                    dumpThrows(e.getThrownTypes());
                    break;
                case METHOD:
                    modifier(e.getModifiers());
                    dumpTypeArguments(e.getTypeParameters());
                    result.append(getTypeName(info, e.getReturnType(), true));
                    result.append(' ');
                    boldStartCheck(highlightName);
                    result.append(e.getSimpleName());
                    boldStopCheck(highlightName);
                    dumpArguments(e.getParameters(), null);
                    dumpThrows(e.getThrownTypes());
                    break;
                case INSTANCE_INIT:
                case STATIC_INIT:
                    //these two cannot be referenced anyway...
            }
            return null;
        }

        public Void visitTypeParameter(TypeParameterElement e, Boolean highlightName) {
            return null;
        }
        
        private void modifier(Set<Modifier> modifiers) {
            boolean addSpace = false;
            
            for (Modifier m : modifiers) {
                if (addSpace) {
                    result.append(' ');
                }
                addSpace = true;
                result.append(m.toString());
            }
            
            if (addSpace) {
                result.append(' ');
            }
        }
        
//        private void throwsDump()

        private void dumpTypeArguments(List<? extends TypeParameterElement> list) {
            if (list.isEmpty())
                return ;
            
            boolean addSpace = false;
            
            result.append("&lt;");
            
            for (TypeParameterElement e : list) {
                if (addSpace) {
                    result.append(", ");
                }
                
                result.append(getTypeName(info, e.asType(), true));
                
                addSpace = true;
            }
                
            result.append("&gt;");
        }

        private void dumpRealTypeArguments(List<? extends TypeMirror> list) {
            if (list.isEmpty())
                return ;

            boolean addSpace = false;

            result.append("&lt;");

            for (TypeMirror t : list) {
                if (addSpace) {
                    result.append(", ");
                }

                result.append(getTypeName(info, t, true));

                addSpace = true;
            }

            result.append("&gt;");
        }

        private void dumpArguments(List<? extends VariableElement> list, List<? extends TypeMirror> types) {
            boolean addSpace = false;
            
            result.append('(');

            Iterator<? extends VariableElement> listIt = list.iterator();
            Iterator<? extends TypeMirror> typesIt = types != null ? types.iterator() : null;

            while (listIt.hasNext()) {
                if (addSpace) {
                    result.append(", ");
                }
                
                VariableElement ve = listIt.next();
                TypeMirror      type = typesIt != null ? typesIt.next() : ve.asType();

                result.append(getTypeName(info, type, true));
                result.append(" ");
                result.append(ve.getSimpleName());

                addSpace = true;
            }
                
            result.append(')');
        }

        private void dumpThrows(List<? extends TypeMirror> list) {
            if (list.isEmpty())
                return ;
            
            boolean addSpace = false;
            
            result.append(" throws ");
            
            for (TypeMirror t : list) {
                if (addSpace) {
                    result.append(", ");
                }
                
                result.append(getTypeName(info, t, true));
                
                addSpace = true;
            }
        }
            
    }
    
    private static String getTypeName(CompilationInfo info, TypeMirror t, boolean fqn) {
        return null; //translate(Utilities.getTypeName(info, t, fqn).toString());
    }
    
    private static String[] c = new String[] {"&", "<", ">", "\n", "\""}; // NOI18N
    private static String[] tags = new String[] {"&amp;", "&lt;", "&gt;", "<br>", "&quot;"}; // NOI18N
    
    private static String translate(String input) {
        for (int cntr = 0; cntr < c.length; cntr++) {
            input = input.replaceAll(c[cntr], tags[cntr]);
        }
        
        return input;
    }
    
    static UiUtilsCaller CALLER = new UiUtilsCaller() {
        public boolean open(FileObject fo, int pos) {
            return UiUtils.open(fo, pos);
        }
        public void beep(boolean goToSource, boolean goToJavadoc) {
            Toolkit.getDefaultToolkit().beep();
            int value = goToSource ? 1 : goToJavadoc ? 2 : 0;
            StatusDisplayer.getDefault().setStatusText(NbBundle.getMessage(GoToSupport.class, "WARN_CannotGoToGeneric", value));
        }
        public boolean open(ClasspathInfo info, ElementHandle<?> el) {
            return ElementOpen.open(info, el);
        }
        public void warnCannotOpen(String displayName) {
            Toolkit.getDefaultToolkit().beep();
            StatusDisplayer.getDefault().setStatusText(NbBundle.getMessage(GoToSupport.class, "WARN_CannotGoTo", displayName));
        }
    };
    
    interface UiUtilsCaller {
        public boolean open(FileObject fo, int pos);
        public void beep(boolean goToSource, boolean goToJavadoc);
        public boolean open(ClasspathInfo info, ElementHandle<?> el);
        public void warnCannotOpen(String displayName);
    }

    public static final class Context {
        public final TypeMirror classType;
        public final Element resolved;
        public Context(TypeMirror classType, Element resolved) {
            this.classType = classType;
            this.resolved = resolved;
        }
    }
}
