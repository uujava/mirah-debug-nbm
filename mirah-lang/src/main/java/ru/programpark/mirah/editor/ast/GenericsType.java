package ru.programpark.mirah.editor.ast;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import mirah.lang.ast.ClassDefinition;
import mirah.lang.ast.CloneListener;
import mirah.lang.ast.Node;
import mirah.lang.ast.NodeFilter;
import mirah.lang.ast.NodeVisitor;
import mirah.lang.ast.Position;
import ru.programpark.mirah.editor.utils.GenericsUtils;

/**
 * This class is used to describe generic type signatures for ClassDefinitions.
 *
 * @author Jochen Theodorou
 * @see ClassDefinition
 */
public class GenericsType implements Node {
    public static final GenericsType[] EMPTY_ARRAY = new GenericsType[0];
    
    private final ClassDefinition[] upperBounds;
    private final ClassDefinition lowerBound;
    private ClassDefinition type;
    private String name;
    private boolean placeholder;
    private boolean resolved;
    private boolean wildcard;

    public GenericsType(ClassDefinition type, ClassDefinition[] upperBounds, ClassDefinition lowerBound) {
        this.type = type;
        this.name = type.name().identifier(); //type.isGenericsPlaceHolder() ? type.getUnresolvedName() : type.getName();
        this.upperBounds = upperBounds;
        this.lowerBound = lowerBound;
        placeholder = false; //type.isGenericsPlaceHolder();
        resolved = false;
    }

    public GenericsType(ClassDefinition basicType) {
        this(basicType, null, null);
    }

    public ClassDefinition getType() {
        return type;
    }

    public void setType(ClassDefinition type) {
        this.type = type;
    }

    public String toString() {
        Set<String> visited = new HashSet<String>();
        return toString(visited);
    }

    private String toString(Set<String> visited) {
        if (placeholder) visited.add(name);
        String ret = null; //wildcard?"?":((type == null || placeholder) ? name : genericsBounds(type, visited));
        if (upperBounds != null) {
            if (placeholder && upperBounds.length==1 ) { //&& !upperBounds[0].isGenericsPlaceHolder() && upperBounds[0].getName().equals("java.lang.Object")) {
                // T extends Object should just be printed as T
            } else {
                ret += " extends ";
                for (int i = 0; i < upperBounds.length; i++) {
                    ret += genericsBounds(upperBounds[i], visited);
                    if (i + 1 < upperBounds.length) ret += " & ";
                }
            }
        } else if (lowerBound != null) {
            ret += " super " + genericsBounds(lowerBound, visited);
        }
        return ret;
    }

    private String nameOf(ClassDefinition theType) {
        StringBuilder ret = new StringBuilder();
//        if (theType.isArray()) {
//            ret.append(nameOf(theType.getComponentType()));
//            ret.append("[]");
//        } else {
//            ret.append(theType.getName());
//        }
        return ret.toString();
    }

    private String genericsBounds(ClassDefinition theType, Set<String> visited) {

        StringBuilder ret = new StringBuilder();
/*
        if (theType.isArray()) {
            ret.append(nameOf(theType));
        } else if (theType.redirect() instanceof InnerClassDefinition) {
            InnerClassDefinition innerClassDefinition = (InnerClassDefinition) theType.redirect();
            String parentClassDefinitionName = innerClassDefinition.getOuterClass().getName();
            if (Modifier.isStatic(innerClassDefinition.getModifiers()) || innerClassDefinition.isInterface()) {
                ret.append(innerClassDefinition.getOuterClass().getName());
            } else {
                ret.append(genericsBounds(innerClassDefinition.getOuterClass(), new HashSet<String>()));
            }
            ret.append(".");
            String typeName = theType.getName();
            ret.append(typeName.substring(parentClassDefinitionName.length() + 1));
        } else {
            ret.append(theType.getName());
        }
*/
        GenericsType[] genericsTypes = null; //theType.getGenericsTypes();
        if (genericsTypes == null || genericsTypes.length == 0)
            return ret.toString();

        // TODO instead of catching Object<T> here stop it from being placed into type in first place
        if (genericsTypes.length == 1 && genericsTypes[0].isPlaceholder() && theType.name().identifier().equals("java.lang.Object")) {
            return genericsTypes[0].getName();
        }

        ret.append("<");
        for (int i = 0; i < genericsTypes.length; i++) {
            if (i != 0) ret.append(", ");

            GenericsType type = genericsTypes[i];
            if (type.isPlaceholder() && visited.contains(type.getName())) {
                ret.append(type.getName());
            }
            else {
                ret.append(type.toString(visited));
            }
        }
        ret.append(">");

        return ret.toString();
    }

    public ClassDefinition[] getUpperBounds() {
        return upperBounds;
    }

    public String getName() {
        return name;
    }

    public boolean isPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(boolean placeholder) {
        this.placeholder = placeholder;
//        type.setGenericsPlaceHolder(placeholder);
    }

    public boolean isResolved() {
        return resolved || placeholder;
    }

    public void setResolved(boolean res) {
        resolved = res;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isWildcard() {
        return wildcard;
    }

    public void setWildcard(boolean wildcard) {
        this.wildcard = wildcard;
    }

    public ClassDefinition getLowerBound() {
        return lowerBound;
    }

    /**
     * Tells if the provided class node is compatible with this generic type definition
     * @param classNode the class node to be checked
     * @return true if the class node is compatible with this generics type definition
     */
    public boolean isCompatibleWith(ClassDefinition classNode) {
        return new GenericsTypeMatcher().matches(classNode);
    }

    @Override
    public Position position() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Node parent() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setParent(Node node) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Node originalNode() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setOriginalNode(Node node) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Node replaceChild(Node node, Node node1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removeChild(Node node) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object accept(NodeVisitor nv, Object o) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void whenCloned(CloneListener cl) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Node findAncestor(Class type) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Node findAncestor(NodeFilter nf) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List findChild(NodeFilter nf) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List findChildren(NodeFilter nf) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List findChildren(NodeFilter nf, List list) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Node findDescendant(NodeFilter nf) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List findDescendants(NodeFilter nf) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List findDescendants(NodeFilter nf, List list) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Implements generics type comparison.
     */
    private class GenericsTypeMatcher {

        public boolean implementsInterfaceOrIsSubclassOf(ClassDefinition type, ClassDefinition superOrInterface) {
            boolean result = true;
//                    type.equals(superOrInterface)
//                    || type.isDerivedFrom(superOrInterface)
//                    || type.implementsInterface(superOrInterface);
            if (result) {
                return true;
            }
/*            
            if (GROOVY_OBJECT_TYPE.equals(superOrInterface) && type.getCompileUnit()!=null) {
                // type is being compiled so it will implement GroovyObject later
                return true;
            }
            if (superOrInterface instanceof WideningCategories.LowestUpperBoundClassDefinition) {
                WideningCategories.LowestUpperBoundClassDefinition cn = (WideningCategories.LowestUpperBoundClassDefinition) superOrInterface;
                result = implementsInterfaceOrIsSubclassOf(type, cn.getSuperClass());
                if (result) {
                    for (ClassDefinition interfaceNode : cn.getInterfaces()) {
                        result = implementsInterfaceOrIsSubclassOf(type,interfaceNode);
                        if (!result) break;
                    }
                }
                if (result) return true;
            }
*/            
//            if (type.isArray() && superOrInterface.isArray()) {
//                return implementsInterfaceOrIsSubclassOf(type.getComponentType(), superOrInterface.getComponentType());
//            }
            return false;
        }

        /**
         * Compares this generics type with the one represented by the provided class node. If the provided
         * classnode is compatible with the generics specification, returns true. Otherwise, returns false.
         * The check is complete, meaning that we also check "nested" generics.
         * @param classNode the classnode to be checked
         * @return true iff the classnode is compatible with this generics specification
         */
        public boolean matches(ClassDefinition classNode) {
            GenericsType[] genericsTypes = null; //classNode.getGenericsTypes();
            // diamond always matches
            if (genericsTypes!=null && genericsTypes.length==0) return true;
            /*
            if (classNode.isGenericsPlaceHolder()) {
                // if the classnode we compare to is a generics placeholder (like <E>) then we
                // only need to check that the names are equal
                if (genericsTypes==null) return true;
                if (isWildcard()) {
                    if (lowerBound!=null) return genericsTypes[0].getName().equals(lowerBound.getUnresolvedName());
                    if (upperBounds!=null) {
                        for (ClassDefinition upperBound : upperBounds) {
                            String name = upperBound.getGenericsTypes()[0].getName();
                            if (genericsTypes[0].getName().equals(name)) return true;
                        }
                        return false;
                    }
                }
                return genericsTypes[0].getName().equals(name);
            }
            */
            if (wildcard || placeholder) {
                // if the current generics spec is a wildcard spec or a placeholder spec
                // then we must check upper and lower bounds
                if (upperBounds != null) {
                    // check that the provided classnode is a subclass of all provided upper bounds
                    boolean upIsOk = true;
                    for (int i = 0, upperBoundsLength = upperBounds.length; i < upperBoundsLength && upIsOk; i++) {
                        final ClassDefinition upperBound = upperBounds[i];
                        upIsOk = implementsInterfaceOrIsSubclassOf(classNode, upperBound);
                    }
                    // if the provided classnode is a subclass of the upper bound
                    // then check that the generic types supplied by the class node are compatible with
                    // this generics specification
                    // for example, we could have the spec saying List<String> but provided classnode
                    // saying List<Integer>
                    upIsOk = upIsOk && checkGenerics(classNode);
                    return upIsOk;
                }
                if (lowerBound != null) {
                    // if a lower bound is declared, then we must perform the same checks that for an upper bound
                    // but with reversed arguments
                    return implementsInterfaceOrIsSubclassOf(lowerBound, classNode) && checkGenerics(classNode);
                }
            }
            // if this is not a generics placeholder, first compare that types represent the same type
            if ((type!=null && !type.equals(classNode))) {
                return false;
            }
            // last, we could have the spec saying List<String> and a classnode saying List<Integer> so
            // we must check that generics are compatible.
            // The null check is normally not required but done to prevent from NPEs
            return type == null || compareGenericsWithBound(classNode, type);
        }

        /**
         * Iterates over each generics bound of this generics specification, and checks
         * that the generics defined by the bound are compatible with the generics specified
         * by the type.
         * @param classNode the classnode the bounds should be compared with
         * @return true if generics from bounds are compatible
         */
        private boolean checkGenerics(final ClassDefinition classNode) {
            if (upperBounds!=null) {
                for (ClassDefinition upperBound : upperBounds) {
                    if (!compareGenericsWithBound(classNode, upperBound)) return false;
                }
            }
            if (lowerBound!=null) {
//                if (!lowerBound.redirect().isUsingGenerics()) {
//                    if (!compareGenericsWithBound(classNode, lowerBound)) return false;
//                }
            }
            return true;
        }

        /**
         * Given a parameterized type (List&lt;String&gt; for example), checks that its
         * generic types are compatible with those from a bound.
         * @param classNode the classnode from which we will compare generics types
         * @param bound the bound to which the types will be compared
         * @return true if generics are compatible
         */
        private boolean compareGenericsWithBound(final ClassDefinition classNode, final ClassDefinition bound) {
            if (classNode==null) return false;
            /*
            if (!bound.isUsingGenerics() || (classNode.getGenericsTypes()==null && classNode.redirect().getGenericsTypes()!=null)) {
                // if the bound is not using generics, there's nothing to compare with
                return true;
            }
            */
            if (!classNode.equals(bound)) {
                 // the class nodes are on different types
                // in this situation, we must choose the correct execution path : either the bound
                // is an interface and we must find the implementing interface from the classnode
                // to compare their parameterized generics, or the bound is a regular class and we
                // must compare the bound with a superclass
                /*
                if (bound.isInterface()) {
                    Set<ClassDefinition> interfaces = classNode.getAllInterfaces();
                    // iterate over all interfaces to check if any corresponds to the bound we are
                    // comparing to
                    for (ClassDefinition anInterface : interfaces) {
                        if (anInterface.equals(bound)) {
                            // when we obtain an interface, the types represented by the interface
                            // class node are not parameterized. This means that we must create a
                            // new class node with the parameterized types that the current class node
                            // has defined.
                            ClassDefinition node = GenericsUtils.parameterizeType(classNode, anInterface);
                            return compareGenericsWithBound(node, bound);
                        }
                    }
                }
                */
/*                
                if (bound instanceof WideningCategories.LowestUpperBoundClassDefinition) {
                    // another special case here, where the bound is a "virtual" type
                    // we must then check the superclass and the interfaces
                    boolean success = compareGenericsWithBound(classNode, bound.getSuperClass());
                    if (success) {
                        ClassDefinition[] interfaces = bound.getInterfaces();
                        for (ClassDefinition anInterface : interfaces) {
                            success &= compareGenericsWithBound(classNode, anInterface);
                            if (!success) break;
                        }
                        if (success) return true;
                    }
                }
*/                
                return false; //compareGenericsWithBound(getParameterizedSuperClass(classNode), bound);
            }
            GenericsType[] cnTypes = null; //classNode.getGenericsTypes();
//            if (cnTypes==null && classNode.isRedirectNode()) cnTypes=classNode.redirect().getGenericsTypes();
            if (cnTypes==null) {
                // may happen if generic type is Foo<T extends Foo> and classnode is Foo -> Foo
                return true;
            }
            GenericsType[] redirectBoundGenericTypes = null; //bound.redirect().getGenericsTypes();
            Map<String, GenericsType> classNodePlaceholders = GenericsUtils.extractPlaceholders(classNode);
            Map<String, GenericsType> boundPlaceHolders = GenericsUtils.extractPlaceholders(bound);
            boolean match = true;
            for (int i = 0; redirectBoundGenericTypes!=null && i < redirectBoundGenericTypes.length && match; i++) {
                GenericsType redirectBoundType = redirectBoundGenericTypes[i];
                GenericsType classNodeType = cnTypes[i];
                // The following code has been commented out because it causes GROOVY-5415
                // However, commenting doesn't make any test fail, which is curious...
                if (classNodeType.isPlaceholder()) {
                    String name = classNodeType.getName();
                    if (redirectBoundType.isPlaceholder()) {
                        match = name.equals(redirectBoundType.getName());
                        if (!match) {
                            GenericsType genericsType = boundPlaceHolders.get(redirectBoundType.getName());
                            match = false;
                            if (genericsType!=null) {
                                if (genericsType.isPlaceholder()) {
                                    match = true;
                                } else if (genericsType.isWildcard()) {
                                    if (genericsType.getUpperBounds()!=null) {
                                        for (ClassDefinition up : genericsType.getUpperBounds()) {
                                            match |= redirectBoundType.isCompatibleWith(up);
                                        }
                                        if (genericsType.getLowerBound()!=null) {
                                            match |= redirectBoundType.isCompatibleWith(genericsType.getLowerBound());
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        if (classNodePlaceholders.containsKey(name)) classNodeType=classNodePlaceholders.get(name);
                        match = classNodeType.isCompatibleWith(redirectBoundType.getType());
                    }
                } else {
                    if (redirectBoundType.isPlaceholder()) {
                        if (classNodeType.isPlaceholder()) {
                            match = classNodeType.getName().equals(redirectBoundType.getName());
                        } else {
                            String name = redirectBoundType.getName();
                            if (boundPlaceHolders.containsKey(name)) {
                                redirectBoundType = boundPlaceHolders.get(name);
                                boolean wildcard = redirectBoundType.isWildcard();
                                boolean placeholder = redirectBoundType.isPlaceholder();
                                if (placeholder || wildcard) {
                                    // placeholder aliases, like Map<U,V> -> Map<K,V>
//                                    redirectBoundType = classNodePlaceholders.get(name);
                                    if (wildcard) {
                                        // ex: Comparable<Integer> <=> Comparable<? super T>
                                        if (redirectBoundType.lowerBound!=null) {
                                            GenericsType gt = new GenericsType(redirectBoundType.lowerBound);
                                            if (gt.isPlaceholder()) {
                                                // check for recursive generic typedef, like in
                                                // <T extends Comparable<? super T>>
                                                if (classNodePlaceholders.containsKey(gt.getName())) {
                                                    gt = classNodePlaceholders.get(gt.getName());
                                                }
                                            }
                                            match = implementsInterfaceOrIsSubclassOf(gt.getType(), classNodeType.getType());
                                        }
                                        if (match && redirectBoundType.upperBounds!=null) {
                                            for (ClassDefinition upperBound : redirectBoundType.upperBounds) {
                                                GenericsType gt = new GenericsType(upperBound);
                                                if (gt.isPlaceholder()) {
                                                    // check for recursive generic typedef, like in
                                                    // <T extends Comparable<? super T>>
                                                    if (classNodePlaceholders.containsKey(gt.getName())) {
                                                        gt = classNodePlaceholders.get(gt.getName());
                                                    }
                                                }
                                                match = match &&
                                                        (implementsInterfaceOrIsSubclassOf(classNodeType.getType(), gt.getType())
                                                         || classNodeType.isCompatibleWith(gt.getType())); // workaround for GROOVY-6095
                                                if (!match) break;
                                            }
                                        }
                                        return match;
                                    } else {
                                        redirectBoundType = classNodePlaceholders.get(name);
                                    }

                                }
                            }
                            match = redirectBoundType.isCompatibleWith(classNodeType.getType());
                        }
                    } else {
                        // todo: the check for isWildcard should be replaced with a more complete check
                        match = redirectBoundType.isWildcard() || classNodeType.isCompatibleWith(redirectBoundType.getType());
                    }
                }
            }
            if (!match) return false;
            return true;
        }
    }

    /**
     * If you have a class which extends a class using generics, returns the superclass with parameterized types. For
     * example, if you have:
     * <code>class MyList&lt;T&gt; extends LinkedList&lt;T&gt;
     * def list = new MyList&lt;String&gt;
     * </code>
     * then the parameterized superclass for MyList&lt;String&gt; is LinkedList&lt;String&gt;
     * @param classNode the class for which we want to return the parameterized superclass
     * @return the parameterized superclass
     */
    private static ClassDefinition getParameterizedSuperClass(ClassDefinition classNode) {
        if (ClassHelper.OBJECT_TYPE.equals(classNode)) return null;
        ClassDefinition superClass = null; //classNode.getUnresolvedSuperClass();
        if (superClass==null) {
            return ClassHelper.OBJECT_TYPE;
        }
        /*
        if (!classNode.isUsingGenerics() || !superClass.isUsingGenerics()) return superClass;
        GenericsType[] genericsTypes = classNode.getGenericsTypes();
        GenericsType[] redirectGenericTypes = classNode.redirect().getGenericsTypes();
        superClass = superClass.getPlainNodeReference();
        if (genericsTypes==null || redirectGenericTypes==null || superClass.getGenericsTypes()==null) return superClass;
        for (int i = 0, genericsTypesLength = genericsTypes.length; i < genericsTypesLength; i++) {
            if (redirectGenericTypes[i].isPlaceholder()) {
                final GenericsType genericsType = genericsTypes[i];
                GenericsType[] superGenericTypes = superClass.getGenericsTypes();
                for (int j = 0, superGenericTypesLength = superGenericTypes.length; j < superGenericTypesLength; j++) {
                    final GenericsType superGenericType = superGenericTypes[j];
                    if (superGenericType.isPlaceholder() && superGenericType.getName().equals(redirectGenericTypes[i].getName())) {
                        superGenericTypes[j] = genericsType;
                    }
                }
            }
        }
                */
        return superClass;
    }
     public Object clone()
     {
         return this;
     }
}
