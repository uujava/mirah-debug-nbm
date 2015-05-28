package ru.programpark.mirah.editor.ast;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.lang.ref.SoftReference;
import java.lang.reflect.Modifier;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import mirah.lang.ast.Block;
import mirah.lang.ast.ClassDefinition;
import mirah.lang.ast.MethodDefinition;
import mirah.lang.ast.Script;
import mirah.objectweb.asm.Opcodes;

/**
 * This class is a Helper for ClassDefinition and classes handling ClassDefinitions.
 * It does contain a set of predefined ClassDefinitions for the most used 
 * types and some code for cached ClassDefinition creation and basic 
 * ClassDefinition handling 
 * 
 * @author Jochen Theodorou
 */
public class ClassHelper {

    private static final Class[] classes = new Class[] {
        Object.class, Boolean.TYPE, Character.TYPE, Byte.TYPE, Short.TYPE,
        Integer.TYPE, Long.TYPE, Double.TYPE, Float.TYPE, Void.TYPE,
//        Closure.class, GString.class, List.class, Map.class, Range.class,
//        Pattern.class, Script.class, String.class,  Boolean.class, 
        Character.class, Byte.class, Short.class, Integer.class, Long.class,
        Double.class, Float.class, BigDecimal.class, BigInteger.class, 
//        Number.class, Void.class, Reference.class, Class.class, MetaClass.class, 
//        Iterator.class, GeneratedClosure.class, GroovyObjectSupport.class
    };

    private static final String[] primitiveClassNames = new String[] {
        "", "boolean", "char", "byte", "short", 
        "int", "long", "double", "float", "void"
    };
    
    public static final ClassDefinition 
        DYNAMIC_TYPE = makeCached(Object.class),  OBJECT_TYPE = DYNAMIC_TYPE,
        VOID_TYPE = makeCached(Void.TYPE),        
           CLOSURE_TYPE = makeCached(Block.class),
//        GSTRING_TYPE = makeCached(GString.class), 
        LIST_TYPE = makeWithoutCaching(List.class),
        MAP_TYPE = makeWithoutCaching(Map.class),
 //           RANGE_TYPE = makeCached(Range.class),
        PATTERN_TYPE = makeCached(Pattern.class), STRING_TYPE = makeCached(String.class),
        SCRIPT_TYPE = makeCached(Script.class),
//            REFERENCE_TYPE = makeWithoutCaching(Reference.class),
//        BINDING_TYPE = makeCached(Binding.class),

        boolean_TYPE = makeCached(boolean.class),     char_TYPE = makeCached(char.class),
        byte_TYPE = makeCached(byte.class),           int_TYPE = makeCached(int.class),
        long_TYPE = makeCached(long.class),           short_TYPE = makeCached(short.class),
        double_TYPE = makeCached(double.class),       float_TYPE = makeCached(float.class),
        Byte_TYPE = makeCached(Byte.class),           Short_TYPE = makeCached(Short.class),
        Integer_TYPE = makeCached(Integer.class),     Long_TYPE = makeCached(Long.class),
        Character_TYPE = makeCached(Character.class), Float_TYPE = makeCached(Float.class),
        Double_TYPE = makeCached(Double.class),       Boolean_TYPE = makeCached(Boolean.class),
        BigInteger_TYPE =  makeCached(java.math.BigInteger.class),
        BigDecimal_TYPE = makeCached(java.math.BigDecimal.class),
        Number_TYPE = makeCached(Number.class),
        
//        void_WRAPPER_TYPE = makeCached(Void.class),   METACLASS_TYPE = makeCached(MetaClass.class),
        Iterator_TYPE = makeCached(Iterator.class),

        Enum_Type = makeWithoutCaching(Enum.class),
        Annotation_TYPE = makeCached(Annotation.class),
        ELEMENT_TYPE_TYPE = makeCached(ElementType.class),

        // uncached constants.
        CLASS_Type = makeWithoutCaching(Class.class)
//            COMPARABLE_TYPE = makeWithoutCaching(Comparable.class),        
//        GENERATED_CLOSURE_Type = makeWithoutCaching(GeneratedClosure.class),
//        GROOVY_OBJECT_SUPPORT_TYPE = makeWithoutCaching(GroovyObjectSupport.class),
//        GROOVY_OBJECT_TYPE = makeWithoutCaching(GroovyObject.class),
//        GROOVY_INTERCEPTABLE_TYPE = makeWithoutCaching(GroovyInterceptable.class)
        ;
    
    private static final ClassDefinition[] types = new ClassDefinition[] {
        OBJECT_TYPE,
        boolean_TYPE, char_TYPE, byte_TYPE, short_TYPE,
        int_TYPE, long_TYPE, double_TYPE, float_TYPE,
        VOID_TYPE, CLOSURE_TYPE, 
//        GSTRING_TYPE,
        LIST_TYPE, MAP_TYPE, 
//        RANGE_TYPE, 
        PATTERN_TYPE,
        SCRIPT_TYPE, STRING_TYPE, Boolean_TYPE, Character_TYPE,
        Byte_TYPE, Short_TYPE, Integer_TYPE, Long_TYPE,
        Double_TYPE, Float_TYPE, BigDecimal_TYPE, BigInteger_TYPE,
        Number_TYPE,
//        void_WRAPPER_TYPE, REFERENCE_TYPE, 
        CLASS_Type, 
//        METACLASS_TYPE,
        Iterator_TYPE, 
//        GENERATED_CLOSURE_Type, GROOVY_OBJECT_SUPPORT_TYPE, 
//        sGROOVY_OBJECT_TYPE, GROOVY_INTERCEPTABLE_TYPE, 
        Enum_Type, Annotation_TYPE
    };

    private static final int ABSTRACT_STATIC_PRIVATE = 
            Modifier.ABSTRACT|Modifier.PRIVATE|Modifier.STATIC;
    private static final int VISIBILITY = 5; // public|protected
    
    protected static final ClassDefinition[] EMPTY_TYPE_ARRAY = {};
    
    public static final String OBJECT = "java.lang.Object";

    public static ClassDefinition makeCached(Class c){
        final SoftReference<ClassDefinition> classNodeSoftReference = ClassHelperCache.classCache.get(c);
        ClassDefinition classNode = null;
/*            
        if (classNodeSoftReference == null || (classNode = classNodeSoftReference.get()) == null) {
            classNode = new ClassDefinition(c);
            ClassHelperCache.classCache.put(c, new SoftReference<ClassDefinition>(classNode));
//            VMPluginFactory.getPlugin().setAdditionalClassInformation(classNode);
        }
*/
        return classNode;
    }
    
    /**
     * Creates an array of ClassDefinitions using an array of classes.
     * For each of the given classes a new ClassDefinition will be 
     * created
     * @see #make(Class)
     * @param classes an array of classes used to create the ClassDefinitions
     * @return an array of ClassDefinitions
     */
    public static ClassDefinition[] make(Class[] classes) {
        ClassDefinition[] cns = new ClassDefinition[classes.length];
        for (int i=0; i<cns.length; i++) {
            cns[i] = make(classes[i]);
        }
        
        return cns;
    }
    
    /**
     * Creates a ClassDefinition using a given class.
     * A new ClassDefinition object is only created if the class
     * is not one of the predefined ones
     * 
     * @param c class used to created the ClassDefinition
     * @return ClassDefinition instance created from the given class
     */
    public static ClassDefinition make(Class c) {
        return make(c,true);
    }
    
    public static ClassDefinition make(Class c, boolean includeGenerics) {
        for (int i=0; i<classes.length; i++) {
            if (c==classes[i]) return types[i];
        }
        if (c.isArray()) {
            ClassDefinition cn = make(c.getComponentType(),includeGenerics);
            return null; //cn.makeArray();
        }
        return makeWithoutCaching(c,includeGenerics);
    }
    
    public static ClassDefinition makeWithoutCaching(Class c){
        return makeWithoutCaching(c,true);
    }
    
    public static ClassDefinition makeWithoutCaching(Class c, boolean includeGenerics){
        if (c.isArray()) {
            ClassDefinition cn = makeWithoutCaching(c.getComponentType(),includeGenerics);
            return null; //cn.makeArray();
        }

        final ClassDefinition cached = makeCached(c);
        if (includeGenerics) {
            return cached;
        }
        else {
            ClassDefinition t = makeWithoutCaching(c.getName());
//            t.setRedirect(cached);
            return t;
        }
    }
    
    
    /**
     * Creates a ClassDefinition using a given class.
     * Unlike make(String) this method will not use the cache
     * to create the ClassDefinition. This means the ClassDefinition created
     * from this method using the same name will have a different
     * reference
     * 
     * @see #make(String)
     * @param name of the class the ClassDefinition is representing
     */
    public static ClassDefinition makeWithoutCaching(String name) { 
//        ClassDefinition cn = new ClassDefinition(name,Opcodes.ACC_PUBLIC,OBJECT_TYPE);
//        cn.isPrimaryNode = false;
        return null; //cn;
    }
    
    /**     * Creates a ClassDefinition using a given class.
     * If the name is one of the predefined ClassDefinitions then the 
     * corresponding ClassDefinition instance will be returned. If the
     * name is null or of length 0 the dynamic type is returned
     * 
     * @param name of the class the ClassDefinition is representing
     */
    public static ClassDefinition make(String name) {
        if (name == null || name.length() == 0) return DYNAMIC_TYPE;
        
        for (int i=0; i<primitiveClassNames.length; i++) {
            if (primitiveClassNames[i].equals(name)) return types[i];
        }
        
        for (int i=0; i<classes.length; i++) {
            String cname = classes[i].getName();
            if (name.equals(cname)) return types[i];
        }        
        return makeWithoutCaching(name);
    }
    
    /**
     * Creates a ClassDefinition containing the wrapper of a ClassDefinition 
     * of primitive type. Any ClassDefinition representing a primitive
     * type should be created using the predefined types used in
     * class. The method will check the parameter for known 
     * references of ClassDefinition representing a primitive type. If
     * Reference is found, then a ClassDefinition will be contained that
     * represents the wrapper class. For example for boolean, the
     * wrapper class is java.lang.Boolean.
     * 
     * If the parameter is no primitive type, the redirected 
     * ClassDefinition will be returned 
     *   
     * @see #make(Class)
     * @see #make(String)
     * @param cn the ClassDefinition containing a possible primitive type
     */
    public static ClassDefinition getWrapper(ClassDefinition cn) {
//        cn = cn.redirect();
        if (!isPrimitiveType(cn)) return cn;
        if (cn==boolean_TYPE) {
            return Boolean_TYPE;
        } else if (cn==byte_TYPE) {
            return Byte_TYPE;
        } else if (cn==char_TYPE) {
            return Character_TYPE;
        } else if (cn==short_TYPE) {
            return Short_TYPE;
        } else if (cn==int_TYPE) {
            return Integer_TYPE;
        } else if (cn==long_TYPE) {
            return Long_TYPE;
        } else if (cn==float_TYPE) {
            return Float_TYPE;
        } else if (cn==double_TYPE) {
            return Double_TYPE;
        } else if (cn==VOID_TYPE) {
            return null; //void_WRAPPER_TYPE;
        }
        else {
            return cn;
        }
    }

    public static ClassDefinition getUnwrapper(ClassDefinition cn) {
//        cn = cn.redirect();
        if (isPrimitiveType(cn)) return cn;
        if (cn==Boolean_TYPE) {
            return boolean_TYPE;
        } else if (cn==Byte_TYPE) {
            return byte_TYPE;
        } else if (cn==Character_TYPE) {
            return char_TYPE;
        } else if (cn==Short_TYPE) {
            return short_TYPE;
        } else if (cn==Integer_TYPE) {
            return int_TYPE;
        } else if (cn==Long_TYPE) {
            return long_TYPE;
        } else if (cn==Float_TYPE) {
            return float_TYPE;
        } else if (cn==Double_TYPE) {
            return double_TYPE;
        }
        else {
            return cn;
        }
    }


    /**
     * Test to determine if a ClassDefinition is a primitive type.
     * Note: this only works for ClassDefinitions created using a
     * predefined ClassDefinition
     * 
     * @see #make(Class)
     * @see #make(String)
     * @param cn the ClassDefinition containing a possible primitive type
     * @return true if the ClassDefinition is a primitive type
     */
    public static boolean isPrimitiveType(ClassDefinition cn) {
        return  cn == boolean_TYPE ||
                cn == char_TYPE ||
                cn == byte_TYPE ||
                cn == short_TYPE ||
                cn == int_TYPE ||
                cn == long_TYPE ||
                cn == float_TYPE ||
                cn == double_TYPE ||
                cn == VOID_TYPE;
    }

    /**
     * Test to determine if a ClassDefinition is a type belongs to the list of types which
     * are allowed to initialize constants directly in bytecode instead of using &lt;cinit&gt;
     *
     * Note: this only works for ClassDefinitions created using a
     * predefined ClassDefinition
     *
     * @see #make(Class)
     * @see #make(String)
     * @param cn the ClassDefinition to be tested
     * @return true if the ClassDefinition is of int, float, long, double or String type
     */
    public static boolean isStaticConstantInitializerType(ClassDefinition cn) {
        return  cn == int_TYPE ||
                cn == float_TYPE ||
                cn == long_TYPE ||
                cn == double_TYPE ||
                cn == STRING_TYPE ||
                // the next items require conversion to int when initializing
                cn == byte_TYPE ||
                cn == char_TYPE ||
                cn == short_TYPE;
    }

    public static boolean isNumberType(ClassDefinition cn) {
        return  cn == Byte_TYPE ||
                cn == Short_TYPE ||
                cn == Integer_TYPE ||
                cn == Long_TYPE ||
                cn == Float_TYPE ||
                cn == Double_TYPE ||
                cn == byte_TYPE ||
                cn == short_TYPE ||
                cn == int_TYPE ||
                cn == long_TYPE ||
                cn == float_TYPE ||
                cn == double_TYPE;
    }

    public static ClassDefinition makeReference() {
        return null; //REFERENCE_TYPE.getPlainNodeReference();
    }

    public static boolean isCachedType(ClassDefinition type) {
        for (ClassDefinition cachedType : types) {
            if (cachedType == type) return true;
        }
        return false;
    }

    static class ClassHelperCache {
        static ConcurrentMap<Class, SoftReference<ClassDefinition>> classCache = new ConcurrentHashMap<Class, SoftReference<ClassDefinition>>(); //ReferenceBundle.getWeakBundle());
    }
    
    public static boolean isSAMType(ClassDefinition type) {
        return findSAM(type) != null;
    }

    /**
     * Returns the single abstract method of a class node, if it is a SAM type, or null otherwise.
     * @param type a type for which to search for a single abstract method
     * @return the method node if type is a SAM type, null otherwise
     */
    public static MethodDefinition findSAM(ClassDefinition type) {
        /*
        if (!Modifier.isAbstract(type.getModifiers())) return null;
        if (type.isInterface()) {
            List<MethodDefinition> methods = type.getMethods();
            MethodDefinition found=null;
            for (MethodDefinition mi : methods) {
                // ignore methods, that are not abstract and from Object
                if (!Modifier.isAbstract(mi.getModifiers())) continue;
                // ignore trait methods which have a default implementation
//                if (Traits.hasDefaultImplementation(mi)) continue;
                if (mi.getDeclaringClass().equals(OBJECT_TYPE)) continue;
                if (OBJECT_TYPE.getDeclaredMethod(mi.getName(), mi.getParameters())!=null) continue;

                // we have two methods, so no SAM
                if (found!=null) return null;
                found = mi;
            }
            return found;

        } else {

            List<MethodDefinition> methods = type.getAbstractMethods();
            MethodDefinition found = null;
            if (methods!=null) {
                for (MethodDefinition mi : methods) {
                    if (!hasUsableImplementation(type, mi)) {
                        if (found!=null) return null;
                        found = mi;
                    }
                }
            }
            return found;
        }
        */
        return null;
    }

    private static boolean hasUsableImplementation(ClassDefinition c, MethodDefinition m) {
        /*
        if (c==m.getDeclaringClass()) return false;
        MethodDefinition found = c.getDeclaredMethod(m.getName(), m.getParameters());
        if (found==null) return false;
        int asp = found.getModifiers() & ABSTRACT_STATIC_PRIVATE;
        int visible = found.getModifiers() & VISIBILITY;
        if (visible !=0 && asp == 0) return true;
        if (c.equals(OBJECT_TYPE)) return false;
        */
        return false; //hasUsableImplementation(c.getSuperClass(), m);
        
    }

    /**
     * Returns a super class or interface for a given class depending on a given target.
     * If the target is no super class or interface, then null will be returned.
     * @param clazz the start class
     * @param goalClazz the goal class
     * @return the next super class or interface
     */
    public static ClassDefinition getNextSuperClass(ClassDefinition clazz, ClassDefinition goalClazz) {
/*        
        if (clazz.isArray()) {
            ClassDefinition cn = getNextSuperClass(clazz.getComponentType(),goalClazz.getComponentType());
            if (cn!=null) cn = cn.makeArray();
            return cn;
        }

        if (!goalClazz.isInterface()) {
            if (clazz.isInterface()) {
                if (OBJECT_TYPE.equals(clazz)) return null;
                return OBJECT_TYPE;
            } else {
                return clazz.getUnresolvedSuperClass();
            }
        }

        ClassDefinition[] interfaces = clazz.getUnresolvedInterfaces();
        for (int i=0; i<interfaces.length; i++) {
//            if (StaticTypeCheckingSupport.implementsInterfaceOrIsSubclassOf(interfaces[i],goalClazz)) {
//                return interfaces[i];
//            }
        }
        //none of the interfaces here match, so continue with super class
*/
        return null; //clazz.getUnresolvedSuperClass();
        
    }
}
