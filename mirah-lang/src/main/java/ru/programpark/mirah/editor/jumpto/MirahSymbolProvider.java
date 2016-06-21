package ru.programpark.mirah.editor.jumpto;

import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.annotations.common.NullAllowed;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.source.ClassIndex;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.SourceUtils;
import org.netbeans.api.project.Project;
import org.netbeans.modules.java.source.usages.ClassIndexManager;
import org.netbeans.modules.parsing.lucene.support.IndexManager;
import org.netbeans.modules.parsing.spi.indexing.support.QuerySupport;
import org.netbeans.spi.jumpto.support.NameMatcher;
import org.netbeans.spi.jumpto.support.NameMatcherFactory;
import org.netbeans.spi.jumpto.symbol.SymbolProvider;
import org.netbeans.spi.jumpto.type.SearchType;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.Pair;
import org.openide.util.lookup.ServiceProvider;
import ru.programpark.mirah.index.MirahIndex;
import ru.programpark.mirah.index.elements.IndexedClass;
import ru.programpark.mirah.index.elements.IndexedMethod;

import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.SimpleTypeVisitor6;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * @author Tomas Zezula
 */
@ServiceProvider(service = SymbolProvider.class)
public class MirahSymbolProvider implements SymbolProvider {

    private static final Logger LOGGER = Logger.getLogger(MirahSymbolProvider.class.getName());

    private static final String CAPTURED_WILDCARD = "<captured wildcard>"; //NOI18N
    private static final String UNKNOWN = "<unknown>"; //NOI18N
    private static final String INIT = "<init>"; //NOI18N

    private volatile boolean canceled;

    public String name() {
        return "mirah symbols";  //NOI18N
    }

    public String getDisplayName() {
        return "MirahSymbols"; //NbBundle.getMessage(MirahTypeProvider.class, "MSG_JavaSymbols");
    }

    //todo - убрать ссылки на замыкания
    public void computeSymbolNames(final Context context, final Result result) {

        try {
            final SearchType st = context.getSearchType();
            String textToSearch = context.getText();
            String prefix = null;
            final int dotIndex = textToSearch.lastIndexOf('.'); //NOI18N
            if (dotIndex > 0 && dotIndex != textToSearch.length() - 1) {
                prefix = textToSearch.substring(0, dotIndex);
                textToSearch = textToSearch.substring(dotIndex + 1);
            }
            String[] _ident = new String[]{textToSearch};
            ClassIndex.NameKind _kind;
            boolean _caseSensitive;
            switch (st) {
                case PREFIX:
                    _kind = ClassIndex.NameKind.PREFIX;
                    _caseSensitive = true;
                    break;
                case REGEXP:
                    _kind = ClassIndex.NameKind.REGEXP;
                    _ident[0] = removeNonJavaChars(_ident[0]);
                    _ident[0] = NameMatcherFactory.wildcardsToRegexp(_ident[0], true);
                    _caseSensitive = true;
                    break;
                case CAMEL_CASE:
                    _ident = createCamelCase(_ident);
                    _kind = ClassIndex.NameKind.CAMEL_CASE;
                    _caseSensitive = true;
                    break;
                case EXACT_NAME:
                    _kind = ClassIndex.NameKind.SIMPLE_NAME;
                    _caseSensitive = true;
                    break;
                case CASE_INSENSITIVE_PREFIX:
                    _kind = ClassIndex.NameKind.CASE_INSENSITIVE_PREFIX;
                    _caseSensitive = false;
                    break;
                case CASE_INSENSITIVE_EXACT_NAME:
                    _kind = ClassIndex.NameKind.CASE_INSENSITIVE_REGEXP;
                    _caseSensitive = false;
                    break;
                case CASE_INSENSITIVE_REGEXP:
                    _kind = ClassIndex.NameKind.CASE_INSENSITIVE_REGEXP;
                    _ident[0] = removeNonJavaChars(_ident[0]);
                    _ident[0] = NameMatcherFactory.wildcardsToRegexp(_ident[0], true);
                    _caseSensitive = false;
                    break;
                default:
                    throw new IllegalArgumentException();
            }
            final String[] ident = _ident;
            final ClassIndex.NameKind kind = _kind;
            final boolean caseSensitive = _caseSensitive;
            final Pair<NameMatcher, Boolean> restriction;
            if (prefix != null) {
                restriction = compileName(prefix, caseSensitive);
                result.setHighlightText(textToSearch);
            } else {
                restriction = null;
            }
            try {
                final ClassIndexManager manager = ClassIndexManager.getDefault();

                Collection<FileObject> roots = QuerySupport.findRoots(
                        (Project) null,
                        Collections.singleton(ClassPath.SOURCE),
                        Collections.<String>emptySet(),
                        Collections.<String>emptySet());

                final MirahIndex index = MirahIndex.get(roots);
                /*
                final Set<URL> rootUrls = new HashSet<URL>();
                for(FileObject root : roots) {
                    if (canceled) {
                        return;
                    }          
                    LOG.info(this,"ROOT: "+root.toURL());
                    rootUrls.add(root.toURL());
                }

                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Querying following roots:"); //NOI18N
                    for (URL url : rootUrls) {
                        LOGGER.log(Level.FINE, "  {0}", url); //NOI18N
                    }
                    LOGGER.log(Level.FINE, "-------------------------"); //NOI18N
                }
                */

                final String text = textToSearch;


                //Perform all queries in single op
                IndexManager.priorityAccess(new IndexManager.Action<Void>() {
                    @Override
                    public Void run() throws IOException, InterruptedException {

//                        Set<IndexedClass> classes = index.getClasses("Base", QuerySupport.Kind.PREFIX);
                        Set<IndexedClass> classes = index.getClasses(text, QuerySupport.Kind.CASE_INSENSITIVE_PREFIX);
//                        Set<IndexedClass> classes = index.getAllClasses();
                        for (IndexedClass indexedClass : classes) {
                            String className = indexedClass.getName();
                            if (className.indexOf("$Closure") != -1 || className.indexOf("$ZBinding") != -1) {
                                int t = 0;
                                continue;
                            }
                            String[] signatures = new String[]{};
                            ElementHandle eh = ElementHandle.createTypeElementHandle(ElementKind.CLASS, "");
                            /*
                            result.addResult(new JavaSymbolDescriptor(
                                    iclass.getName(),
                                    ElementKind.CLASS,
                                    new HashSet<Modifier>(),
                                    eh,
                                    eh,
                                    FileOwnerQuery.getOwner(iclass.getFileObject()),
                                    iclass.getFileObject(),
                                    null));
                            */
                            String fqn = indexedClass.getFqn();
                            if (fqn.length() > className.length() + 1)
                                fqn = fqn.substring(0, fqn.length() - className.length() - 1);
                            /*
                            result.addResult(new MirahSymbolDescriptor(
                                    indexedClass.getName(),
                                    ElementKind.CLASS,
                                    new HashSet<Modifier>(),
                                    eh,
                                    eh,
                                    FileOwnerQuery.getOwner(indexedClass.getFileObject()),
                                    indexedClass.getFileObject(),
                                    fqn) );
                            */
                            result.addResult(new MirahSymbolDescriptor(
                                    indexedClass.getName(),
                                    ElementKind.CLASS,
                                    indexedClass.getFileObject(),
                                    indexedClass.getOffset(),
                                    fqn));
                        }
                        Set<IndexedMethod> methods = index.getMethods(text, null, QuerySupport.Kind.CASE_INSENSITIVE_PREFIX);
                        for (IndexedMethod indexedMethod : methods) {
                            String[] signatures = new String[]{};
//                            ElementHandle eh = ElementHandle.createTypeElementHandle(ElementKind.METHOD, "");
//                            ElementHandle eh = ElementHandleSupport.createHandle(null, indexedMethod.getName(), org.netbeans.modules.csl.api.ElementKind.METHOD, new HashSet<Modifier>());
                            String fqn = indexedMethod.getSignature();
                            /*
                            result.addResult(new MirahSymbolDescriptor(
                                    indexedMethod.getName(),
                                    ElementKind.METHOD,
                                    new HashSet<Modifier>(),
                                    eh,
                                    eh,
                                    FileOwnerQuery.getOwner(indexedMethod.getFileObject()),
                                    indexedMethod.getFileObject(),
                                    fqn));
                            */
                            String displayName = fqn;
                            if (displayName.indexOf(':') != -1)
                                displayName = displayName.substring(0, displayName.indexOf(':'));

                            result.addResult(new MirahSymbolDescriptor(
                                    displayName,
                                    ElementKind.METHOD,
                                    indexedMethod.getFileObject(),
                                    indexedMethod.getOffset(),
                                    indexedMethod.getIn()));

                        }
                        /*
                        for (URL url : rootUrls) {
                            if (canceled) {
                                return null;
                            }
                            final FileObject root = URLMapper.findFileObject(url);
                            if (root == null) {
                                continue;
                            }

                            final Project project = FileOwnerQuery.getOwner(root);
                            final ClassIndexImpl impl = manager.getUsagesQuery(root.toURL(), true);
                            if (impl != null) {
                                final Map<ElementHandle<TypeElement>,Set<String>> r = new HashMap<ElementHandle<TypeElement>,Set<String>>();
                                for (String currentIdent : ident) {
                                    impl.getDeclaredElements(currentIdent, kind, DocumentUtil.elementHandleConvertor(),r);
                                }
                                if (!r.isEmpty()) {
                                    //Needs FileManagerTransaction as it creates CPI with backgroundCompilation == true
                                    TransactionContext.
                                            beginTrans().
                                            register(FileManagerTransaction.class, FileManagerTransaction.read()).
                                            register(ProcessorGenerated.class, ProcessorGenerated.nullWrite());
                                    try {
                                        final ClasspathInfo cpInfo = null; //ClasspathInfoAccessor.getINSTANCE().create(root,null,true,true,false,false);
                                        final JavaSource js = JavaSource.create(cpInfo);
                                        js.runUserActionTask(new Task<CompilationController>() {
                                            @Override
                                            public void run (final CompilationController controller) {
                                                for (final Map.Entry<ElementHandle<TypeElement>,Set<String>> p : r.entrySet()) {
                                                    final ElementHandle<TypeElement> owner = p.getKey();
                                                    final TypeElement te = owner.resolve(controller);
                                                    final Set<String> idents = p.getValue();
                                                    if (te != null) {
                                                        if (idents.contains(getSimpleName(te, null)) && matchesRestrictions(te, restriction)) {
                                                            result.addResult(new JavaSymbolDescriptor(
                                                                    te.getSimpleName().toString(),
                                                                    te.getKind(),
                                                                    te.getModifiers(),
                                                                    owner,
                                                                    ElementHandle.create(te),
                                                                    project,
                                                                    root,
                                                                    impl));
                                                        }
                                                        for (Element ne : te.getEnclosedElements()) {
                                                            if (idents.contains(getSimpleName(ne, te)) && matchesRestrictions(ne, restriction)) {
                                                                result.addResult(new JavaSymbolDescriptor(
                                                                    getDisplayName(ne, te),
                                                                    ne.getKind(),
                                                                    ne.getModifiers(),
                                                                    owner,
                                                                    ElementHandle.create(ne),
                                                                    project,
                                                                    root,
                                                                    impl));
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            private String getSimpleName (
                                                    @NonNull final Element element,
                                                    @NullAllowed final Element enclosingElement) {
                                                String result = element.getSimpleName().toString();
                                                if (enclosingElement != null && INIT.equals(result)) {
                                                    result = enclosingElement.getSimpleName().toString();
                                                }
                                                if (!caseSensitive) {
                                                    result = result.toLowerCase();
                                                }
                                                return result;
                                            }
                                        },true);
                                    } finally {
                                        TransactionContext.get().commit();
                                    }
                                }

                            }
                        }
                        */
                        return null;
                    }
                });
            } catch (IOException ioe) {
                Exceptions.printStackTrace(ioe);
            } catch (InterruptedException ie) {
                return;
            }
        } finally {
            cleanup();
        }
    }

    private boolean matchesRestrictions(
            @NonNull final Element e,
            @NullAllowed Pair<NameMatcher, Boolean> restriction) {
        if (restriction == null) {
            return true;
        }
        final Element owner = e.getEnclosingElement();
        if (owner == null) {
            return false;
        }
        final Name n;
        if (restriction.second() && (owner instanceof QualifiedNameable)) {
            n = ((QualifiedNameable) owner).getQualifiedName();
        } else {
            n = owner.getSimpleName();
        }
        return restriction.first().accept(n.toString());
    }

    private static Pair<NameMatcher, Boolean> compileName(
            @NonNull final String prefix,
            final boolean caseSensitive) {
        final boolean fqn = prefix.indexOf('.') > 0;    //NOI18N
        final SearchType searchType = containsWildCard(prefix) ?
                (caseSensitive ? SearchType.REGEXP : SearchType.CASE_INSENSITIVE_REGEXP) :
                (caseSensitive ? SearchType.PREFIX : SearchType.CASE_INSENSITIVE_PREFIX);
        return Pair.<NameMatcher, Boolean>of(
                NameMatcherFactory.createNameMatcher(prefix, searchType),
                fqn);
    }

    private static boolean containsWildCard(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '?' || text.charAt(i) == '*') { // NOI18N
                return true;
            }
        }
        return false;
    }

    private static String getDisplayName(
            @NonNull final Element e,
            @NonNull final Element enclosingElement) {
        assert e != null;
        if (e.getKind() == ElementKind.METHOD || e.getKind() == ElementKind.CONSTRUCTOR) {
            StringBuilder sb = new StringBuilder();
            if (e.getKind() == ElementKind.CONSTRUCTOR) {
                sb.append(enclosingElement.getSimpleName());
            } else {
                sb.append(e.getSimpleName());
            }
            sb.append('('); //NOI18N
            ExecutableElement ee = (ExecutableElement) e;
            final List<? extends VariableElement> vl = ee.getParameters();
            for (Iterator<? extends VariableElement> it = vl.iterator(); it.hasNext(); ) {
                final VariableElement v = it.next();
                final TypeMirror tm = v.asType();
                sb.append(getTypeName(tm, false, true));
                if (it.hasNext()) {
                    sb.append(", ");    //NOI18N
                }
            }
            sb.append(')');
            return sb.toString();
        }
        return e.getSimpleName().toString();
    }

    private static String[] createCamelCase(final String[] text) {
        if (text[0].length() == 0) {
            return text;
        } else {
            return new String[]{
                    text[0],
                    Character.toLowerCase(text[0].charAt(0)) + text[0].substring(1)
            };
        }
    }

    private static CharSequence getTypeName(TypeMirror type, boolean fqn, boolean varArg) {
        if (type == null)
            return ""; //NOI18N
        return new TypeNameVisitor(varArg).visit(type, fqn);
    }

    private static class TypeNameVisitor extends SimpleTypeVisitor6<StringBuilder, Boolean> {

        private boolean varArg;
        private boolean insideCapturedWildcard = false;

        private TypeNameVisitor(boolean varArg) {
            super(new StringBuilder());
            this.varArg = varArg;
        }

        @Override
        public StringBuilder defaultAction(TypeMirror t, Boolean p) {
            return DEFAULT_VALUE.append(t);
        }

        @Override
        public StringBuilder visitDeclared(DeclaredType t, Boolean p) {
            Element e = t.asElement();
            if (e instanceof TypeElement) {
                TypeElement te = (TypeElement) e;
                DEFAULT_VALUE.append((p ? te.getQualifiedName() : te.getSimpleName()).toString());
                Iterator<? extends TypeMirror> it = t.getTypeArguments().iterator();
                if (it.hasNext()) {
                    DEFAULT_VALUE.append("<"); //NOI18N
                    while (it.hasNext()) {
                        visit(it.next(), p);
                        if (it.hasNext())
                            DEFAULT_VALUE.append(", "); //NOI18N
                    }
                    DEFAULT_VALUE.append(">"); //NOI18N
                }
                return DEFAULT_VALUE;
            } else {
                return DEFAULT_VALUE.append(UNKNOWN); //NOI18N
            }
        }

        @Override
        public StringBuilder visitArray(ArrayType t, Boolean p) {
            boolean isVarArg = varArg;
            varArg = false;
            visit(t.getComponentType(), p);
            return DEFAULT_VALUE.append(isVarArg ? "..." : "[]"); //NOI18N
        }

        @Override
        public StringBuilder visitTypeVariable(TypeVariable t, Boolean p) {
            Element e = t.asElement();
            if (e != null) {
                String name = e.getSimpleName().toString();
                if (!CAPTURED_WILDCARD.equals(name))
                    return DEFAULT_VALUE.append(name);
            }
            DEFAULT_VALUE.append("?"); //NOI18N
            if (!insideCapturedWildcard) {
                insideCapturedWildcard = true;
                TypeMirror bound = t.getLowerBound();
                if (bound != null && bound.getKind() != TypeKind.NULL) {
                    DEFAULT_VALUE.append(" super "); //NOI18N
                    visit(bound, p);
                } else {
                    bound = t.getUpperBound();
                    if (bound != null && bound.getKind() != TypeKind.NULL) {
                        DEFAULT_VALUE.append(" extends "); //NOI18N
                        if (bound.getKind() == TypeKind.TYPEVAR)
                            bound = ((TypeVariable) bound).getLowerBound();
                        visit(bound, p);
                    }
                }
                insideCapturedWildcard = false;
            }
            return DEFAULT_VALUE;
        }

        @Override
        public StringBuilder visitWildcard(WildcardType t, Boolean p) {
            int len = DEFAULT_VALUE.length();
            DEFAULT_VALUE.append("?"); //NOI18N
            TypeMirror bound = t.getSuperBound();
            if (bound == null) {
                bound = t.getExtendsBound();
                if (bound != null) {
                    DEFAULT_VALUE.append(" extends "); //NOI18N
                    if (bound.getKind() == TypeKind.WILDCARD)
                        bound = ((WildcardType) bound).getSuperBound();
                    visit(bound, p);
                } else if (len == 0) {
                    bound = SourceUtils.getBound(t);
                    if (bound != null && (bound.getKind() != TypeKind.DECLARED || !((TypeElement) ((DeclaredType) bound).asElement()).getQualifiedName().contentEquals("java.lang.Object"))) { //NOI18N
                        DEFAULT_VALUE.append(" extends "); //NOI18N
                        visit(bound, p);
                    }
                }
            } else {
                DEFAULT_VALUE.append(" super "); //NOI18N
                visit(bound, p);
            }
            return DEFAULT_VALUE;
        }

        @Override
        public StringBuilder visitError(ErrorType t, Boolean p) {
            Element e = t.asElement();
            if (e instanceof TypeElement) {
                TypeElement te = (TypeElement) e;
                return DEFAULT_VALUE.append((p ? te.getQualifiedName() : te.getSimpleName()).toString());
            }
            return DEFAULT_VALUE;
        }
    }

    private static String removeNonJavaChars(String text) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isJavaIdentifierPart(c) || c == '*' || c == '?') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public void cancel() {
        canceled = true;
    }

    public void cleanup() {
        canceled = false;
    }

}
