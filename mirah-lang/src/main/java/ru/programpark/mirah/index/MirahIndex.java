package ru.programpark.mirah.index;

import ca.weblite.netbeans.mirah.LOG;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.modules.csl.api.Modifier;
import org.netbeans.modules.parsing.spi.indexing.support.IndexResult;
import org.netbeans.modules.parsing.spi.indexing.support.QuerySupport;
import org.openide.filesystems.FileObject;
import org.openide.modules.InstalledFileLocator;
import org.openide.util.Exceptions;
import ru.programpark.mirah.index.elements.IndexedClass;
import ru.programpark.mirah.index.elements.IndexedElement;
import ru.programpark.mirah.index.elements.IndexedField;
import ru.programpark.mirah.index.elements.IndexedMethod;
import ru.programpark.mirah.index.elements.MethodElement;

public final class MirahIndex {

//    private static final Logger LOG = Logger.getLogger(MirahIndex.class.getName());
    private static final MirahIndex EMPTY = new MirahIndex(null);
    private static final String CLUSTER_URL = "cluster:"; // NOI18N
    private static String clusterUrl = null;
    private final QuerySupport querySupport;


    private MirahIndex(QuerySupport querySupport) {
        this.querySupport = querySupport;
    }

    /**
     * Get the index. Use wisely multiple calls are expensive.
     */
    public static MirahIndex get(Collection<FileObject> roots) {
        try {
            return new MirahIndex(QuerySupport.forRoots(
                    MirahIndexer.Factory.NAME,
                    MirahIndexer.Factory.VERSION,
                    roots.toArray(new FileObject[roots.size()])));
        } catch (IOException ioe) {
//            LOG.log(Level.WARNING, null, ioe);
//            LOG.exception(MirahIndex.class, ioe);
            return EMPTY;
        }
    }

    public static MirahIndex get(FileObject fo) {
        // Sources - ClassPath.SOURCE and translated ClassPath.COMPILE & ClassPath.BOOT
        Collection<FileObject> srcRoots = QuerySupport.findRoots(
                        (Project)null,
                        Collections.singleton(ClassPath.SOURCE),
                        Collections.<String>emptySet(),
                        Collections.<String>emptySet());

        Collection<FileObject> coll = QuerySupport.findRoots(fo, Collections.singleton(ClassPath.SOURCE), null, null);
        Project project = FileOwnerQuery.getOwner(fo);
        if ( project != null ) coll.add(project.getProjectDirectory());

        // FIXME index is broken when invoked on start
//            this.index = MirahIndex.get(QuerySupport.findRoots(fo, Collections.singleton(ClassPath.SOURCE), null, null));
        return MirahIndex.get(coll);
    }


    /**
     * Returns all {@link IndexedClass}es that are located in the given package.
     *
     * @param packageName package name for which we want to get {@link IndexedClass}es
     * @return all {@link IndexedClass}es that are located in the given package
     */
    public Set<IndexedClass> getClassesFromPackage(String packageName) {
        Set<IndexedClass> result = new HashSet<>();

        for (IndexedClass indexedClass : getAllClasses()) {
            String pkgName = MirahUtils.getPackageName(indexedClass.getFqn());
            if (packageName.equals(pkgName)) {
                result.add(indexedClass);
            }
        }
        return result;
    }

    /**
     * Returns all available {@link IndexedClass}es.
     *
     * @return all available {@link IndexedClass}es
     */
    public Set<IndexedClass> getAllClasses() {
        return getClasses(".*", QuerySupport.Kind.REGEXP);
    }

    /**
     * Return the full set of classes that match the given name.
     *
     * @param name The name of the class - possibly a fqn like file.Stat, or just a class
     *   name like Stat, or just a prefix like St.
     * @param kind Whether we want the exact name, or whether we're searching by a prefix.
     * @param includeAll If true, return multiple IndexedClasses for the same logical
     *   class, one for each declaration point.
     */
    
     //todo - добавить список полей в результатах поиска!!!
    
    public Set<IndexedClass> getClasses(String name, QuerySupport.Kind kind) {
        String classFqn = null;

        if (name != null) {
            // CamelCase check is here because of issue #212878
            if (name.endsWith(".") && (QuerySupport.Kind.CAMEL_CASE != kind)) {
                // User has typed something like "Test." and wants completion on
                // for something like Test.Unit
                classFqn = name.substring(0, name.length() - 1);
                name = "";
            }
        }

        final Set<IndexResult> result = new HashSet<>();
        QuerySupport.Kind searchKind = kind;
        
        String field;
        switch (kind) {
        case EXACT:
//            field = MirahIndexer.FQN_NAME;
            field = MirahIndexer.CLASS_NAME;
            break;
        case PREFIX:
        case CAMEL_CASE:
        case REGEXP:
            field = MirahIndexer.CLASS_NAME;
            break;
        case CASE_INSENSITIVE_PREFIX:
        case CASE_INSENSITIVE_REGEXP:
            field = MirahIndexer.CASE_INSENSITIVE_CLASS_NAME;
//          field = MirahIndexer.CLASS_NAME;
            searchKind = QuerySupport.Kind.PREFIX;
            name = name.toLowerCase();
//            name = "*";
            break;
        default:
            throw new UnsupportedOperationException(kind.toString());
        }

        search(field, name, searchKind, result);

        final Set<IndexedClass> classes = new HashSet<>();

        for (IndexResult map : result) {
            String simpleName = map.getValue(MirahIndexer.CLASS_NAME);

            if (simpleName == null) {
                // It's probably a module
                // XXX I need to handle this... for now punt
                continue;
            }

            // Lucene returns some inexact matches, TODO investigate why this is necessary
            if ((kind == QuerySupport.Kind.PREFIX) && !simpleName.startsWith(name)) {
                continue;
            } else if (kind == QuerySupport.Kind.CASE_INSENSITIVE_PREFIX ) {
                String lowerName = map.getValue(MirahIndexer.CASE_INSENSITIVE_CLASS_NAME);
                if ( !lowerName.regionMatches(true, 0, name, 0, name.length()))
                continue;
            }

            if (classFqn != null) {
                if (kind == QuerySupport.Kind.CASE_INSENSITIVE_PREFIX ||
                        kind == QuerySupport.Kind.CASE_INSENSITIVE_REGEXP) {
                    if (!classFqn.equalsIgnoreCase(map.getValue(MirahIndexer.IN))) {
                        continue;
                    }
                } else if (kind == QuerySupport.Kind.CAMEL_CASE) {
                    String in = map.getValue(MirahIndexer.IN);
                    if (in != null) {
                        // Superslow, make faster
                        StringBuilder sb = new StringBuilder();
                        int lastIndex = 0;
                        int idx;
                        do {

                            int nextUpper = -1;
                            for( int i = lastIndex+1; i < classFqn.length(); i++ ) {
                                if ( Character.isUpperCase(classFqn.charAt(i)) ) {
                                    nextUpper = i;
                                    break;
                                }
                            }
                            idx = nextUpper;
                            String token = classFqn.substring(lastIndex, idx == -1 ? classFqn.length(): idx);
                            sb.append(token);
                            sb.append( idx != -1 ?  "[\\p{javaLowerCase}\\p{Digit}_\\$]*" : ".*"); // NOI18N
                            lastIndex = idx;
                        }
                        while(idx != -1);

                        final Pattern pattern = Pattern.compile(sb.toString());
                        if (!pattern.matcher(in).matches()) {
                            continue;
                        }
                    } else {
                        continue;
                    }
                } else {
                    if (!classFqn.equals(map.getValue(MirahIndexer.IN))) {
                        continue;
                    }
                }
            }

            String attrs = map.getValue(MirahIndexer.CLASS_ATTRS);
            boolean isClass = true;
            if (attrs != null) {
                int flags = IndexedElement.stringToFlag(attrs, 0);
                isClass = (flags & IndexedClass.MODULE) == 0;
            }

            String fqn = map.getValue(MirahIndexer.FQN_NAME);
            
            IndexedClass newClass = createClass(fqn, simpleName, map);
//            newClass.setUrl(map.getValue(MirahIndexer.URL));
            newClass.setSuperClass(map.getValue(MirahIndexer.SUPER_CLASS));
            classes.add(newClass);
        }

        return classes;
    }

    /**
     * For the given class name finds explicitely declared constructors.
     *
     * @param className name of the class
     * @return explicitely declared constructors
     */
    public Set<IndexedMethod> getConstructors(final String className) {
        final Set<IndexResult> indexResult = new HashSet<>();
        final Set<IndexedMethod> result = new HashSet<>();

//        search(MirahIndexer.CONSTRUCTOR, className, QuerySupport.Kind.PREFIX, indexResult);
        search(MirahIndexer.CONSTRUCTOR, "", QuerySupport.Kind.PREFIX, indexResult);
        for (IndexResult map : indexResult) {
            if (className != null) {
                String fqn = map.getValue(MirahIndexer.FQN_NAME);
                if (!(className.equals(fqn))) {
                    continue;
                }
            }
            String[] constructors = map.getValues(MirahIndexer.CONSTRUCTOR);

            for (String constructor : constructors) {
//                String paramList = constructor.substring(constructor.indexOf(";") + 1, constructor.length()); // NOI18N
//                String[] params = paramList.split(",");
//
//                List<MethodElement.MethodParameter> methodParams = new ArrayList<>();
//                for (String param : params) {
//                    if (!"".equals(param.trim())) { // NOI18N
//                        methodParams.add(new MethodElement.MethodParameter(param, MirahUtils.stripPackage(param)));
//                    }
//                }
                String[] tokens = constructor.split(";");
                if ( tokens.length == 0 ) continue;
                String constructorSignature = tokens[0];
                String modifiers = tokens.length > 1 ? tokens[1] : "";
                int offset = tokens.length > 2 ? Integer.parseInt(tokens[2]) : 0;
                List<MethodElement.MethodParameter> constructorParams = getMethodParameter(constructorSignature);
                result.add(new IndexedMethod(map, className, "new", "void", constructorParams, "", 0, offset));
            }
        }

        return result;
    }

    /**
     * Return a set of methods that match the given name prefix, and are in the given
     * class and module. If no class is specified, match methods across all classes.
     * Note that inherited methods are not checked. If you want to match inherited methods
     * you must call this method on each superclass as well as the mixin modules.
     */
    @SuppressWarnings("fallthrough")
    public Set<IndexedMethod> getMethods(final String name, final String clz, QuerySupport.Kind kind) {
        final Set<IndexResult> result = new HashSet<>();

        String field = MirahIndexer.METHOD_NAME;
        QuerySupport.Kind originalKind = kind;
        if (kind == QuerySupport.Kind.EXACT) {
            // I can't do exact searches on methods because the method
            // entries include signatures etc. So turn this into a prefix
            // search and then compare chopped off signatures with the name
            kind = QuerySupport.Kind.PREFIX;
        }

        search(field, name==null ? "" : name, kind, result);

        // TODO Prune methods to fit my scheme - later make lucene index smarter about how to prune its index search
        final Set<IndexedMethod> methods = new HashSet<>();

        for (IndexResult map : result) {
            if (clz != null) {
                String fqn = map.getValue(MirahIndexer.FQN_NAME);

                if (!(clz.equals(fqn))) {
                    continue;
                }
            }

            String[] signatures = map.getValues(MirahIndexer.METHOD_NAME);

            if (signatures != null) {
                for (String signature : signatures) {
                    // Skip weird methods... Think harder about this
                    if (((name == null) || (name.length() == 0)) &&
                            !Character.isLowerCase(signature.charAt(0))) {
                        continue;
                    }

                    // Lucene returns some inexact matches, TODO investigate why this is necessary
                    if ( name != null )
                    if ((kind == QuerySupport.Kind.PREFIX) && !signature.startsWith(name)) {
                        continue;
                    } else if (kind == QuerySupport.Kind.CASE_INSENSITIVE_PREFIX && !signature.regionMatches(true, 0, name, 0, name.length())) {
                        continue;
                    } else if (kind == QuerySupport.Kind.CASE_INSENSITIVE_REGEXP) {
                        int len = signature.length();
                        int end = signature.indexOf('(');
                        if (end == -1) {
                            end = signature.indexOf(';');
                            if (end == -1) {
                                end = len;
                            }
                        }
                        String n = end != len ? signature.substring(0, end) : signature;
                        try {
                            if (!n.matches(name)) {
                                continue;
                            }
                        } catch (PatternSyntaxException e) {
                            // Silently ignore regexp failures in the search expression
                        }
                    } else if (originalKind == QuerySupport.Kind.EXACT) {
                        // Make sure the name matches exactly
                        // We know that the prefix is correct from the first part of
                        // this if clause, by the signature may have more
                        if (((signature.length() > name.length()) &&
                                (signature.charAt(name.length()) != '(')) &&
                                (signature.charAt(name.length()) != ';')) {
                            continue;
                        }
                    }

                    // XXX THIS DOES NOT WORK WHEN THERE ARE IDENTICAL SIGNATURES!!!
                    assert map != null;
                    methods.add(createMethod(signature, map));
                }
            }
        }

        return methods;
    }

    /**
     * Gets all fields for the given fully qualified name.
     *
     * @param fqName fully qualified name
     * @return all fields for the given type
     */
    public Set<IndexedField> getAllFields(final String fqName) {
        return getFields(".*", fqName, QuerySupport.Kind.REGEXP); // NOI18N
    }
    
    public Set<IndexedClass> findClassesByFqn( String fqName )
    {
        Set<IndexedClass> classes = getAllClasses();
        if ( classes == null ) return null;
        
        Set<IndexedClass> result = new HashSet<>();
        for( IndexedClass indexedClass : classes )
        {
            String n = indexedClass.getFqn();
            if ( indexedClass.getFqn().equals(fqName) ) 
                result.add(indexedClass);
        }
        return result;
    }

    public String findSuperClassByFqn( String fqName )
    {
        Set<IndexedClass> classes = findClassesByFqn(fqName);
        return classes.isEmpty() ? null : classes.iterator().next().getSuperClass();
    }

    /**
     * Gets all static fields for the given fully qualified name.
     *
     * @param fqName fully qualified name
     * @return all static fields for the given type
     */
    public Set<IndexedField> getStaticFields(final String fqName) {
        Set<IndexedField> fields = getFields(".*", fqName, QuerySupport.Kind.REGEXP); // NOI18N
        Set<IndexedField> staticFields = new HashSet<>();

        for (IndexedField field : fields) {
            if (field.getModifiers().contains(Modifier.STATIC)) {
                staticFields.add(field);
            }
        }
        return staticFields;   
    }

    public Set<IndexedField> getFields(final String name, final String clz, QuerySupport.Kind kind) {
        boolean inherited = clz == null;
        final Set<IndexResult> result = new HashSet<>();

        String field = MirahIndexer.FIELD_NAME;
        QuerySupport.Kind originalKind = kind;
        if (kind == QuerySupport.Kind.EXACT) {
            // I can't do exact searches on methods because the method
            // entries include signatures etc. So turn this into a prefix
            // search and then compare chopped off signatures with the name
            kind = QuerySupport.Kind.PREFIX;
        }

        search(field, name, kind, result);

        // TODO Prune methods to fit my scheme - later make lucene index smarter about how to prune its index search
        final Set<IndexedField> fields = new HashSet<>();

        for (IndexResult map : result) {
            if (clz != null) {
                String fqn = map.getValue(MirahIndexer.FQN_NAME);

                if (!(clz.equals(fqn))) {
                    continue;
                }
            }

            String[] signatures = map.getValues(MirahIndexer.FIELD_NAME);

            if (signatures != null) {
                for (String signature : signatures) {
                    // Skip weird methods... Think harder about this
                    if (((name == null) || (name.length() == 0)) &&
                            !Character.isLowerCase(signature.charAt(0))) {
                        continue;
                    }

                    // Lucene returns some inexact matches, TODO investigate why this is necessary
                    if ((kind == QuerySupport.Kind.PREFIX) && !signature.startsWith(name)) {
                        continue;
                    } else if (kind == QuerySupport.Kind.CASE_INSENSITIVE_PREFIX && !signature.regionMatches(true, 0, name, 0, name.length())) {
                        continue;
                    } else if (kind == QuerySupport.Kind.CASE_INSENSITIVE_REGEXP) {
                        int len = signature.length();
                        int end = signature.indexOf(';');
                        if (end == -1) {
                            end = len;
                        }

                        String n = end != len ? signature.substring(0, end) : signature;
                        try {
                            if (!n.matches(name)) {
                                continue;
                            }
                        } catch (PatternSyntaxException e) {
                            // Silently ignore regexp failures in the search expression
                        }
                    } else if (originalKind == QuerySupport.Kind.EXACT) {
                        // Make sure the name matches exactly
                        // We know that the prefix is correct from the first part of
                        // this if clause, by the signature may have more
                        if ((signature.length() > name.length()) &&
                                (signature.charAt(name.length()) != ';')) {
                            continue;
                        }
                    }

                    // XXX THIS DOES NOT WORK WHEN THERE ARE IDENTICAL SIGNATURES!!!
                    assert map != null;
                    fields.add(createField(signature, map, inherited));
                }
            }
        }

        return fields;
    }

    /**
     * Get the set of inherited (through super classes and mixins) for the given fully qualified class name.
     * @param classFqn FQN: module1.module2.moduleN.class
     * @param prefix If kind is NameKind.PREFIX/CASE_INSENSITIVE_PREFIX, a prefix to filter methods by. Else,
     *    if kind is NameKind.EXACT_NAME filter methods by the exact name.
     * @param kind Whether the prefix field should be taken as a prefix or a whole name
     */
    public Set<IndexedMethod> getInheritedMethods(String classFqn, String prefix, QuerySupport.Kind kind) {
        Set<IndexedMethod> methods = new HashSet<>();
        Set<String> scannedClasses = new HashSet<>();
        Set<String> seenSignatures = new HashSet<>();

        if (prefix == null) {
            prefix = "";
        }

        addMethodsFromClass(prefix, kind, classFqn, methods, seenSignatures, scannedClasses);

        return methods;
    }

    /** Return whether the specific class referenced (classFqn) was found or not. This is
     * not the same as returning whether any classes were added since it may add
     * additional methods from parents (Object/Class).
     */
    private boolean addMethodsFromClass(String prefix, QuerySupport.Kind kind, String classFqn,
        Set<IndexedMethod> methods, Set<String> seenSignatures, Set<String> scannedClasses) {
        // Prevent problems with circular includes or redundant includes
        if (scannedClasses.contains(classFqn)) {
            return false;
        }

        scannedClasses.add(classFqn);

        String searchField = MirahIndexer.FQN_NAME;

        Set<IndexResult> result = new HashSet<>();

        search(searchField, classFqn, QuerySupport.Kind.EXACT, result);

        boolean foundIt = result.size() > 0;

        // If this is a bogus class entry (no search rsults) don't continue
        if (!foundIt) {
            return foundIt;
        }

        for (IndexResult map : result) {
            assert map != null;

            String[] signatures = map.getValues(MirahIndexer.METHOD_NAME);

            if (signatures != null) {
                for (String signature : signatures) {
                    // Skip weird methods like "[]" etc. in completion lists... TODO Think harder about this
                    if ((prefix.length() == 0) && !Character.isLowerCase(signature.charAt(0))) {
                        continue;
                    }

                    // Prevent duplicates when method is redefined
                    if (!seenSignatures.contains(signature)) {
                        if (signature.startsWith(prefix)) {
                            if (kind == QuerySupport.Kind.EXACT) {
                                // Ensure that the method is not longer than the prefix
                                if ((signature.length() > prefix.length()) &&
                                        (signature.charAt(prefix.length()) != '(') &&
                                        (signature.charAt(prefix.length()) != ';')) {
                                    continue;
                                }
                            } else {
                                // REGEXP, CAMELCASE filtering etc. not supported here
                                assert (kind == QuerySupport.Kind.PREFIX) ||
                                (kind == QuerySupport.Kind.CASE_INSENSITIVE_PREFIX);
                            }

                            seenSignatures.add(signature);

                            IndexedMethod method = createMethod(signature, map);
                            methods.add(method);
                        }
                    }
                }
            }
        }

//        if (extendsClass == null) {
            // XXX GroovyObject, GroovyScript
        addMethodsFromClass(prefix, kind, "java.lang.Object", methods, seenSignatures, scannedClasses); // NOI18N

        return foundIt;
    }

    private IndexedClass createClass(String fqn, String simpleName, IndexResult map) {

        // TODO - how do I determine -which- file to associate with the file?
        // Perhaps the one that defines initialize() ?

        if (simpleName == null) {
            simpleName = map.getValue(MirahIndexer.CLASS_NAME);
        }

        String attrs = map.getValue(MirahIndexer.CLASS_ATTRS);

        int flags = 0;
        if (attrs != null) {
            flags = IndexedElement.stringToFlag(attrs, 0);
        }

        String lineStr = map.getValue(MirahIndexer.CLASS_OFFSET);
        int line = lineStr != null ? Integer.parseInt(lineStr) : 0;
        
        IndexedClass c = IndexedClass.create(simpleName, fqn, map, attrs, flags, line);

        return c;
    }

    private IndexedMethod createMethod(String signature, IndexResult map) {
        String clz = map.getValue(MirahIndexer.CLASS_NAME);
        String module = map.getValue(MirahIndexer.IN);

        if (clz == null) {
            // Module method?
            clz = module;
        } else if ((module != null) && (module.length() > 0)) {
            clz = module + "." + clz; // NOI18N
        }

        //String fqn = map.getValue(MirahIndexer.FQN_NAME);

        String[] tokens = signature.split(";");

        String methodSignature = tokens[0];
        String type = tokens[1];
        if ( type.isEmpty() ) type = "void";
        
        String attributes = tokens[2];
        int offset = -1;
        
        offset = Integer.parseInt(tokens[3]);

        int flags = 0;

        String method_name = getMethodName(methodSignature);
        List<MethodElement.MethodParameter> parameters = getMethodParameter(methodSignature);
        IndexedMethod method = new IndexedMethod(map, clz, method_name, type, parameters, attributes, flags, offset);
        
        return method;
/*        
        int typeIndex = signature.indexOf(';');
        String methodSignature = signature;
        String type = "void";
        if (typeIndex != -1) {
            int endIndex = signature.indexOf(';', typeIndex + 1);
            if (endIndex == -1) {
                endIndex = signature.length();
            }
            type = signature.substring(typeIndex + 1, endIndex);
            methodSignature = signature.substring(0, typeIndex);
        }

        
        // Extract attributes
        int attributeIndex = signature.indexOf(';', typeIndex + 1);
        String attributes = null;
        int flags = 0;

        if (attributeIndex != -1) {
            flags = IndexedElement.stringToFlag(signature, attributeIndex+1);

            if (signature.length() > attributeIndex+1) {
                attributes = signature.substring(attributeIndex+1, signature.length());
            }
        }
*/
//        return new IndexedMethod(map, clz, getMethodName(methodSignature), type, getMethodParameter(methodSignature), attributes, flags);
    }

    private String getMethodName(String methodSignature) {
        int parenIndex = methodSignature.indexOf('(');
        if (parenIndex == -1) {
            return methodSignature;
        } else {
            return methodSignature.substring(0, parenIndex);
        }
    }

    private List<MethodElement.MethodParameter> getMethodParameter(String methodSignature) {
        int parenIndex = methodSignature.indexOf('('); // NOI18N
        if (parenIndex == -1) {
            return Collections.emptyList();
        }

        String argsPortion = methodSignature.substring(parenIndex + 1, methodSignature.length() - 1);
        String[] args = argsPortion.split(","); // NOI18N

        if (args == null || args.length <= 0) {
            return Collections.emptyList();
        }

        List<MethodElement.MethodParameter> parameters = new ArrayList<>();
        for (String paramType : args) {
			if ( paramType.length() == 0 ) continue;
            int index = paramType.indexOf(':');
            String type = index == -1 ? paramType : paramType.substring(index+1);
            parameters.add(new MethodElement.MethodParameter(type, MirahUtils.stripPackage(type)));
        }
        return parameters;
    }

    private IndexedField createField(String signature, IndexResult map, boolean inherited) {
        String clz = map.getValue(MirahIndexer.CLASS_NAME);
        String module = map.getValue(MirahIndexer.IN);

        if (clz == null) {
            // Module method?
            clz = module;
        } else if ((module != null) && (module.length() > 0)) {
            clz = module + "." + clz; // NOI18N
        }

        //String fqn = map.getValue(MirahIndexer.FQN_NAME);

        int typeIndex = signature.indexOf(';');
        String name = signature;
        String type = "java.lang.Object";
        if (typeIndex != -1) {
            int endIndex = signature.indexOf(';', typeIndex + 1);
            if (endIndex == -1) {
                endIndex = signature.length();
            }
            type = signature.substring(typeIndex + 1, endIndex);
            name = signature.substring(0, typeIndex);
        }

        int attributeIndex = signature.indexOf(';', typeIndex + 1);
        String attributes = null;
        int flags = 0;

        if (attributeIndex != -1) {
            flags = IndexedElement.stringToFlag(signature, attributeIndex + 1);

            if (signature.length() > attributeIndex + 1) {
                attributes = signature.substring(attributeIndex + 1, signature.length());
            }

            //signature = signature.substring(0, attributeIndex);
        }

        IndexedField m = IndexedField.create(type, name, clz, map, attributes, flags);
        m.setInherited(inherited);

        return m;
    }

    private boolean search(String key, String name, QuerySupport.Kind kind, Set<IndexResult> result) {
        try {
            result.addAll(querySupport.query(key, name, kind));
            return true;
        } catch (IOException ioe) {
            Exceptions.printStackTrace(ioe);
            return false;
        }
    }

    public static void setClusterUrl(String url) {
        clusterUrl = url;
    }

    static String getPreindexUrl(String url) {
        String s = getClusterUrl();

        if (url.startsWith(s)) {
            return CLUSTER_URL + url.substring(s.length());
        }

        return url;
    }

    static String getClusterUrl() {
        if (clusterUrl == null) {
            File f =
                InstalledFileLocator.getDefault()
                                    .locate("modules/org-netbeans-modules-groovy-editor.jar", null, false); // NOI18N

            if (f == null) {
                throw new RuntimeException("Can't find cluster");
            }

            f = new File(f.getParentFile().getParentFile().getAbsolutePath());

            try {
                f = f.getCanonicalFile();
                clusterUrl = f.toURI().toURL().toExternalForm();
            } catch (IOException ioe) {
                Exceptions.printStackTrace(ioe);
            }
        }

        return clusterUrl;
    }
}
