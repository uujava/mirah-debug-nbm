package ru.programpark.mirah.editor.utils;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import mirah.lang.ast.ClassDefinition;
import mirah.lang.ast.MethodDefinition;
import ru.programpark.mirah.editor.ast.ClassHelper;
import ru.programpark.mirah.editor.ast.GenericsType;

/**
 * Utility methods to deal with generic types.
 *
 * @author Cedric Champeau
 * @author Paul King
 */
public class GenericsUtils {
    public static final GenericsType[] EMPTY_GENERICS_ARRAY = new GenericsType[0];

    /**
     * Given a parameterized type and a generic type information, aligns actual type parameters. For example, if a
     * class uses generic type <pre>&lt;T,U,V&gt;</pre> (redirectGenericTypes), is used with actual type parameters
     * <pre>&lt;java.lang.String, U,V&gt;</pre>, then a class or interface using generic types <pre>&lt;T,V&gt;</pre>
     * will be aligned to <pre>&lt;java.lang.String,V&gt;</pre>
     * @param redirectGenericTypes the type arguments or the redirect class node
     * @param parameterizedTypes the actual type arguments used on this class node
     * @param alignmentTarget the generic type arguments to which we want to align to
     * @return aligned type arguments
     * @deprecated You shouldn't call this method because it is inherently unreliable
     */
    @Deprecated
    public static GenericsType[] alignGenericTypes(final GenericsType[] redirectGenericTypes, final GenericsType[] parameterizedTypes, final GenericsType[] alignmentTarget) {
        if (alignmentTarget==null) return EMPTY_GENERICS_ARRAY;
        if (parameterizedTypes==null || parameterizedTypes.length==0) return alignmentTarget;
        GenericsType[] generics = new GenericsType[alignmentTarget.length];
        for (int i = 0, scgtLength = alignmentTarget.length; i < scgtLength; i++) {
            final GenericsType currentTarget = alignmentTarget[i];
            GenericsType match = null;
            if (redirectGenericTypes!=null) {
                for (int j = 0; j < redirectGenericTypes.length && match == null; j++) {
                    GenericsType redirectGenericType = redirectGenericTypes[j];
                    if (redirectGenericType.isCompatibleWith(currentTarget.getType())) {
                        if (currentTarget.isPlaceholder() && redirectGenericType.isPlaceholder() && !currentTarget.getName().equals(redirectGenericType.getName())) {
                            // check if there's a potential better match
                            boolean skip = false;
                            for (int k=j+1; k<redirectGenericTypes.length && !skip; k++) {
                                GenericsType ogt = redirectGenericTypes[k];
                                if (ogt.isPlaceholder() && ogt.isCompatibleWith(currentTarget.getType()) && ogt.getName().equals(currentTarget.getName())) {
                                    skip = true;
                                }
                            }
                            if (skip) continue;
                        }
                        match = parameterizedTypes[j];
                        if (currentTarget.isWildcard()) {
                            // if alignment target is a wildcard type
                            // then we must make best effort to return a parameterized
                            // wildcard
                            ClassDefinition lower = currentTarget.getLowerBound()!=null?match.getType():null;
                            ClassDefinition[] currentUpper = currentTarget.getUpperBounds();
                            ClassDefinition[] upper = currentUpper !=null?new ClassDefinition[currentUpper.length]:null;
                            if (upper!=null) {
                                for (int k = 0; k < upper.length; k++) {
                                    //upper[k] = currentUpper[k].isGenericsPlaceHolder()?match.getType():currentUpper[k];
                                }
                            }
                            match = new GenericsType(ClassHelper.makeWithoutCaching("?"), upper, lower);
                            match.setWildcard(true);
                        }
                    }
                }
            }
            if (match == null) {
                match = currentTarget;
            }
            generics[i]=match;
        }
        return generics;
    }

    /**
     * Generates a wildcard generic type in order to be used for checks against class nodes.
     * See {@link GenericsType#isCompatibleWith(org.codehaus.groovy.ast.ClassDefinition)}.
     * @param types the type to be used as the wildcard upper bound
     * @return a wildcard generics type
     */
    public static GenericsType buildWildcardType(final ClassDefinition... types) {
        ClassDefinition base = ClassHelper.makeWithoutCaching("?");
        GenericsType gt = new GenericsType(base, types, null);
        gt.setWildcard(true);
        return gt;
    }

    public static Map<String, GenericsType> extractPlaceholders(ClassDefinition cn) {
        Map<String, GenericsType> ret = new HashMap<String, GenericsType>();
        extractPlaceholders(cn, ret);
        return ret;
    }

    /**
     * For a given classnode, fills in the supplied map with the parameterized
     * types it defines.
     * @param node
     * @param map
     */
    public static void extractPlaceholders(ClassDefinition node, Map<String, GenericsType> map) {
        if (node == null) return;

//        if (node.isArray()) {
//            extractPlaceholders(node.getComponentType(), map);
//            return;
//        }

//        if (!node.isUsingGenerics() || !node.isRedirectNode()) return;
        GenericsType[] parameterized = null; //node.getGenericsTypes();
        if (parameterized == null || parameterized.length == 0) return;
        GenericsType[] redirectGenericsTypes = null; //node.redirect().getGenericsTypes();
        if (redirectGenericsTypes==null) redirectGenericsTypes = parameterized;
        for (int i = 0; i < redirectGenericsTypes.length; i++) {
            GenericsType redirectType = redirectGenericsTypes[i];
            if (redirectType.isPlaceholder()) {
                String name = redirectType.getName();
                if (!map.containsKey(name)) {
                    GenericsType value = parameterized[i];
                    map.put(name, value);
                    if (value.isWildcard()) {
                        ClassDefinition lowerBound = value.getLowerBound();
                        if (lowerBound!=null) {
                            extractPlaceholders(lowerBound, map);
                        }
                        ClassDefinition[] upperBounds = value.getUpperBounds();
                        if (upperBounds!=null) {
                            for (ClassDefinition upperBound : upperBounds) {
                                extractPlaceholders(upperBound, map);
                            }
                        }
                    } else if (!value.isPlaceholder()) {
                        extractPlaceholders(value.getType(), map);
                    }
                }
            }
        }
    }

    /**
     * Interface class nodes retrieved from {@link org.codehaus.groovy.ast.ClassDefinition#getInterfaces()}
     * or {@link org.codehaus.groovy.ast.ClassDefinition#getAllInterfaces()} are returned with generic type
     * arguments. This method allows returning a parameterized interface given the parameterized class
     * node which implements this interface.
     * @param hint the class node where generics types are parameterized
     * @param target the interface we want to parameterize generics types
     * @return a parameterized interface class node
     * @deprecated Use #parameterizeType instead
     */
    public static ClassDefinition parameterizeInterfaceGenerics(final ClassDefinition hint, final ClassDefinition target) {
        return parameterizeType(hint, target);
    }

    /**
     * Interface class nodes retrieved from {@link org.codehaus.groovy.ast.ClassDefinition#getInterfaces()}
     * or {@link org.codehaus.groovy.ast.ClassDefinition#getAllInterfaces()} are returned with generic type
     * arguments. This method allows returning a parameterized interface given the parameterized class
     * node which implements this interface.
     * @param hint the class node where generics types are parameterized
     * @param target the interface we want to parameterize generics types
     * @return a parameterized interface class node
     */
    public static ClassDefinition parameterizeType(final ClassDefinition hint, final ClassDefinition target) {
        /*
        if (hint.isArray()) {
            if (target.isArray()) {
                return parameterizeType(hint.getComponentType(), target.getComponentType()).makeArray();
            }
            return target;
        }
        */
/*        
        if (!target.equals(hint) && StaticTypeCheckingSupport.implementsInterfaceOrIsSubclassOf(target, hint)) {
            ClassDefinition nextSuperClass = ClassHelper.getNextSuperClass(target, hint);
            if (!hint.equals(nextSuperClass)) {
                Map<String, ClassDefinition> genericsSpec = createGenericsSpec(hint);
                extractSuperClassGenerics(hint, nextSuperClass, genericsSpec);
                ClassDefinition result = correctToGenericsSpecRecurse(genericsSpec, nextSuperClass);
                return parameterizeType(result, target);
            }
        }
*/        
        Map<String, ClassDefinition> genericsSpec = createGenericsSpec(hint);
        ClassDefinition targetRedirect = null; //target.redirect();
        genericsSpec = createGenericsSpec(targetRedirect, genericsSpec);
        extractSuperClassGenerics(hint, targetRedirect, genericsSpec);
        return correctToGenericsSpecRecurse(genericsSpec, targetRedirect);

    }

    public static ClassDefinition nonGeneric(ClassDefinition type) {
//        if (type.isUsingGenerics()) {
//            final ClassDefinition nonGen = ClassHelper.makeWithoutCaching(type.getName());
//            nonGen.setRedirect(type);
//            nonGen.setGenericsTypes(null);
//            nonGen.setUsingGenerics(false);
//            return nonGen;
//        }
//        if (type.isArray() && type.getComponentType().isUsingGenerics()) {
//            return type.getComponentType().getPlainNodeReference().makeArray();
//        }
        return type;
    }

    public static ClassDefinition newClass(ClassDefinition type) {
        return type; //type.getPlainNodeReference();
    }

    public static ClassDefinition makeClassSafe(Class klass) {
        return makeClassSafeWithGenerics(ClassHelper.make(klass));
    }

    public static ClassDefinition makeClassSafeWithGenerics(Class klass, ClassDefinition genericsType) {
        GenericsType[] genericsTypes = new GenericsType[1];
        genericsTypes[0] = new GenericsType(genericsType);
        return makeClassSafeWithGenerics(ClassHelper.make(klass), genericsTypes);
    }

    public static ClassDefinition makeClassSafe0(ClassDefinition type, GenericsType... genericTypes) {
        ClassDefinition plainNodeReference = newClass(type);
//        if (genericTypes != null && genericTypes.length > 0) {
//            plainNodeReference.setGenericsTypes(genericTypes);
//            if (type.isGenericsPlaceHolder()) plainNodeReference.setGenericsPlaceHolder(true);
//        }
        return plainNodeReference;
    }

    public static ClassDefinition makeClassSafeWithGenerics(ClassDefinition type, GenericsType... genericTypes) {
//        if (type.isArray()) {
//            return makeClassSafeWithGenerics(type.getComponentType(), genericTypes).makeArray();
//        }
        GenericsType[] gtypes = new GenericsType[0];
        if (genericTypes != null) {
            gtypes = new GenericsType[genericTypes.length];
            System.arraycopy(genericTypes, 0, gtypes, 0, gtypes.length);
        }
        return makeClassSafe0(type, gtypes);
    }

    public static MethodDefinition correctToGenericsSpec(Map<String,ClassDefinition> genericsSpec, MethodDefinition mn) {
        ClassDefinition correctedType = null; //correctToGenericsSpecRecurse(genericsSpec, mn.getReturnType());
//        Parameter[] origParameters = mn.getParameters();
//        Parameter[] newParameters = new Parameter[origParameters.length];
//        for (int i = 0; i < origParameters.length; i++) {
//            Parameter origParameter = origParameters[i];
//            newParameters[i] = new Parameter(correctToGenericsSpecRecurse(genericsSpec, origParameter.getType()), origParameter.getName(), origParameter.getInitialExpression());
//        }
//        return new MethodDefinition(mn.getName(), mn.getModifiers(), correctedType, newParameters, mn.getExceptions(), mn.getCode());
        return null;
    }

    public static ClassDefinition correctToGenericsSpecRecurse(Map<String,ClassDefinition> genericsSpec, ClassDefinition type) {
        return correctToGenericsSpecRecurse(genericsSpec, type, new ArrayList<String>());
    }

    /**
     * @since 2.4.1
     */
    public static ClassDefinition[] correctToGenericsSpecRecurse(Map<String,ClassDefinition> genericsSpec, ClassDefinition[] types) {
        if (types==null || types.length==1) return types;
        ClassDefinition[] newTypes = new ClassDefinition[types.length];
        boolean modified = false;
        for (int i=0; i<types.length; i++) {
            newTypes[i] = correctToGenericsSpecRecurse(genericsSpec, types[i], new ArrayList<String>());
            modified = modified || (types[i]!=newTypes[i]);
        }
        if (!modified) return types;
        return newTypes;
    }

    public static ClassDefinition correctToGenericsSpecRecurse(Map<String,ClassDefinition> genericsSpec, ClassDefinition type, List<String> exclusions) {
        /*
        if (type.isArray()) {
            return correctToGenericsSpecRecurse(genericsSpec, type.getComponentType(), exclusions).makeArray();
        
        }
        if (type.isGenericsPlaceHolder() && !exclusions.contains(type.getUnresolvedName())) {
            String name = type.getGenericsTypes()[0].getName();
            type = genericsSpec.get(name);
            if (type != null && type.isGenericsPlaceHolder() && type.getGenericsTypes() == null) {
                ClassDefinition placeholder = ClassHelper.makeWithoutCaching(type.getUnresolvedName());
                placeholder.setGenericsPlaceHolder(true);
                type = makeClassSafeWithGenerics(type, new GenericsType(placeholder));
            }
        }
        */
        if (type == null) type = ClassHelper.OBJECT_TYPE;
        GenericsType[] oldgTypes = null; //type.getGenericsTypes();
        GenericsType[] newgTypes = GenericsType.EMPTY_ARRAY;
        if (oldgTypes != null) {
            newgTypes = new GenericsType[oldgTypes.length];
            for (int i = 0; i < newgTypes.length; i++) {
                GenericsType oldgType = oldgTypes[i];
                if (oldgType.isPlaceholder() ) {
                    if (genericsSpec.get(oldgType.getName())!=null) {
                        newgTypes[i] = new GenericsType(genericsSpec.get(oldgType.getName()));
                    } else {
                        newgTypes[i] = new GenericsType(ClassHelper.OBJECT_TYPE);
                    }
                } else if (oldgType.isWildcard()) {
                    ClassDefinition oldLower = oldgType.getLowerBound();
                    ClassDefinition lower = oldLower!=null?correctToGenericsSpecRecurse(genericsSpec, oldLower, exclusions):null;
                    ClassDefinition[] oldUpper = oldgType.getUpperBounds();
                    ClassDefinition[] upper = null;
                    if (oldUpper!=null) {
                        upper = new ClassDefinition[oldUpper.length];
                        for (int j = 0; j < oldUpper.length; j++) {
                            upper[j] = correctToGenericsSpecRecurse(genericsSpec,oldUpper[j], exclusions);
                        }
                    }
                    GenericsType fixed = new GenericsType(oldgType.getType(), upper, lower);
                    fixed.setName(oldgType.getName());
                    fixed.setWildcard(true);
                    newgTypes[i] = fixed;
                } else {
                    newgTypes[i] = null; //new GenericsType(correctToGenericsSpecRecurse(genericsSpec,correctToGenericsSpec(genericsSpec, oldgType), exclusions));
                }
            }
        }
        return makeClassSafeWithGenerics(type, newgTypes);
    }

    public static ClassDefinition correctToGenericsSpec(Map<String, ClassDefinition> genericsSpec, GenericsType type) {
        ClassDefinition ret = null;
        if (type.isPlaceholder()) {
            String name = type.getName();
            ret = genericsSpec.get(name);
        }
        if (ret == null) ret = type.getType();
        return ret;
    }

    public static ClassDefinition correctToGenericsSpec(Map<String,ClassDefinition> genericsSpec, ClassDefinition type) {
//        if (type.isArray()) {
//            return correctToGenericsSpec(genericsSpec, type.getComponentType()).makeArray();
//        }
//        if (type.isGenericsPlaceHolder()) {
//            String name = type.getGenericsTypes()[0].getName();
//            type = genericsSpec.get(name);
//        }
        if (type == null) type = ClassHelper.OBJECT_TYPE;
        return type;
    }

    @SuppressWarnings("unchecked")
    public static Map<String,ClassDefinition> createGenericsSpec(ClassDefinition current) {
        return createGenericsSpec(current, Collections.EMPTY_MAP);
    }

    public static Map<String,ClassDefinition> createGenericsSpec(ClassDefinition current, Map<String,ClassDefinition> oldSpec) {
        Map<String,ClassDefinition> ret = new HashMap<String,ClassDefinition>(oldSpec);
        // ret contains the type specs, what we now need is the type spec for the
        // current class. To get that we first apply the type parameters to the
        // current class and then use the type names of the current class to reset
        // the map. Example:
        //   class A<V,W,X>{}
        //   class B<T extends Number> extends A<T,Long,String> {}
        // first we have:    T->Number
        // we apply it to A<T,Long,String> -> A<Number,Long,String>
        // resulting in:     V->Number,W->Long,X->String
/*
        GenericsType[] sgts = current.getGenericsTypes();
        if (sgts != null) {
            ClassDefinition[] spec = new ClassDefinition[sgts.length];
            for (int i = 0; i < spec.length; i++) {
                spec[i] = correctToGenericsSpec(ret, sgts[i]);
            }
            GenericsType[] newGts = current.redirect().getGenericsTypes();
            if (newGts == null) return ret;
            ret.clear();
            for (int i = 0; i < spec.length; i++) {
                ret.put(newGts[i].getName(), spec[i]);
            }
        }
*/        
        return ret;
    }

    
    
    public static Map<String,ClassDefinition> addMethodGenerics(MethodDefinition current, Map<String,ClassDefinition> oldSpec) {
        Map<String,ClassDefinition> ret = new HashMap<String,ClassDefinition>(oldSpec);
        // ret starts with the original type specs, now add gts for the current method if any
        GenericsType[] sgts = null; //current.getGenericsTypes();
        if (sgts != null) {
            for (GenericsType sgt : sgts) {
                ret.put(sgt.getName(), sgt.getType());
            }
        }
        return ret;
    }

    public static void extractSuperClassGenerics(ClassDefinition type, ClassDefinition target, Map<String,ClassDefinition> spec) {
        // TODO: this method is very similar to StaticTypesCheckingSupport#extractGenericsConnections,
        // but operates on ClassDefinitions instead of GenericsType
        if (target==null || type==target) return;
        /*
        if (type.isArray() && target.isArray()) {
            extractSuperClassGenerics(type.getComponentType(), target.getComponentType(), spec);
//        } else if (target.isGenericsPlaceHolder() || type.equals(target) || !StaticTypeCheckingSupport.implementsInterfaceOrIsSubclassOf(type, target)) {
//            // structural match route
//            if (target.isGenericsPlaceHolder()) {
//                spec.put(target.getGenericsTypes()[0].getName(),type);
//            } else {
//                extractSuperClassGenerics(type.getGenericsTypes(), target.getGenericsTypes(), spec);
//            }
        } else {
            // have first to find matching super class or interface
            Map <String,ClassDefinition> genSpec = createGenericsSpec(type);
            ClassDefinition superClass = ClassHelper.getNextSuperClass(type,target);
            if (superClass!=null){
                ClassDefinition corrected = GenericsUtils.correctToGenericsSpecRecurse(genSpec, superClass);
                extractSuperClassGenerics(corrected, target, spec);
            } else {
                // if we reach here, we have an unhandled case 
                throw new GroovyBugError("The type "+type+" seems not to normally extend "+target+". Sorry, I cannot handle this.");
            }
        }
        */
    }

    private static void extractSuperClassGenerics(GenericsType[] usage, GenericsType[] declaration, Map<String, ClassDefinition> spec) {
        // if declaration does not provide generics, there is no connection to make 
        if (usage==null || declaration==null || declaration.length==0) return;
        if (usage.length!=declaration.length) return;

        // both have generics
        for (int i=0; i<usage.length; i++) {
            GenericsType ui = usage[i];
            GenericsType di = declaration[i];
            if (di.isPlaceholder()) {
                spec.put(di.getName(), ui.getType());
            } else if (di.isWildcard()){
                if (ui.isWildcard()) {
                    extractSuperClassGenerics(ui.getLowerBound(), di.getLowerBound(), spec);
                    extractSuperClassGenerics(ui.getUpperBounds(), di.getUpperBounds(), spec);
                } else {
                    ClassDefinition cu = ui.getType();
                    extractSuperClassGenerics(cu, di.getLowerBound(), spec);
                    ClassDefinition[] upperBounds = di.getUpperBounds();
                    if (upperBounds!=null) {
                        for (ClassDefinition cn : upperBounds) {
                            extractSuperClassGenerics(cu, cn, spec);
                        }
                    }
                }
            } else {
                extractSuperClassGenerics(ui.getType(), di.getType(), spec);
            }
        }
    }

    private static void extractSuperClassGenerics(ClassDefinition[] usage, ClassDefinition[] declaration, Map<String, ClassDefinition> spec) {
        if (usage==null || declaration==null || declaration.length==0) return;
        // both have generics
        for (int i=0; i<usage.length; i++) {
            ClassDefinition ui = usage[i];
            ClassDefinition di = declaration[i];
//            if (di.isGenericsPlaceHolder()) {
//                spec.put(di.getGenericsTypes()[0].getName(), di);
//            } else if (di.isUsingGenerics()){
//                extractSuperClassGenerics(ui.getGenericsTypes(), di.getGenericsTypes(), spec);
//            }
        }
    }
/*
    public static ClassDefinition[] parseClassDefinitionsFromString(
            final String option,
            final SourceUnit sourceUnit,
            final CompilationUnit compilationUnit,
            final MethodDefinition mn,
            final ASTNode usage) {
        GroovyLexer lexer = new GroovyLexer(new StringReader("DummyNode<" + option + ">"));
        final GroovyRecognizer rn = GroovyRecognizer.make(lexer);
        try {
            rn.classOrInterfaceType(true);
            final AtomicReference<ClassDefinition> ref = new AtomicReference<ClassDefinition>();
            AntlrParserPlugin plugin = new AntlrParserPlugin() {
                @Override
                public ModuleNode buildAST(final SourceUnit sourceUnit, final ClassLoader classLoader, final Reduction cst) throws ParserException {
                    ref.set(makeTypeWithArguments(rn.getAST()));
                    return null;
                }
            };
            plugin.buildAST(null, null, null);
            ClassDefinition parsedNode = ref.get();
            // the returned node is DummyNode<Param1, Param2, Param3, ...)
            GenericsType[] parsedNodeGenericsTypes = parsedNode.getGenericsTypes();
            if (parsedNodeGenericsTypes == null) {
                return null;
            }
            ClassDefinition[] signature = new ClassDefinition[parsedNodeGenericsTypes.length];
            for (int i = 0; i < parsedNodeGenericsTypes.length; i++) {
                final GenericsType genericsType = parsedNodeGenericsTypes[i];
                signature[i] = resolveClassDefinition(sourceUnit, compilationUnit, mn, usage, genericsType.getType());
            }
            return signature;
        } catch (RecognitionException e) {
            sourceUnit.addError(new IncorrectTypeHintException(mn, e, usage.getLineNumber(), usage.getColumnNumber()));
        } catch (TokenStreamException e) {
            sourceUnit.addError(new IncorrectTypeHintException(mn, e, usage.getLineNumber(), usage.getColumnNumber()));
        } catch (ParserException e) {
            sourceUnit.addError(new IncorrectTypeHintException(mn, e, usage.getLineNumber(), usage.getColumnNumber()));
        }
        return null;
    }
*/
/*    
    private static ClassDefinition resolveClassDefinition(final SourceUnit sourceUnit, final CompilationUnit compilationUnit, final MethodDefinition mn, final ASTNode usage, final ClassDefinition parsedNode) {
        ClassDefinition dummyClass = new ClassDefinition("dummy",0, ClassHelper.OBJECT_TYPE);
        dummyClass.setModule(new ModuleNode(sourceUnit));
        dummyClass.setGenericsTypes(mn.getDeclaringClass().getGenericsTypes());
        MethodDefinition dummyMN = new MethodDefinition(
                "dummy",
                0,
                parsedNode,
                Parameter.EMPTY_ARRAY,
                ClassDefinition.EMPTY_ARRAY,
                EmptyStatement.INSTANCE
        );
        dummyMN.setGenericsTypes(mn.getGenericsTypes());
        dummyClass.addMethod(dummyMN);
        ResolveVisitor visitor = new ResolveVisitor(compilationUnit) {
            @Override
            protected void addError(final String msg, final ASTNode expr) {
                sourceUnit.addError(new IncorrectTypeHintException(mn, msg, usage.getLineNumber(), usage.getColumnNumber()));
            }
        };
        visitor.startResolving(dummyClass, sourceUnit);
        return dummyMN.getReturnType();
    }
*/
    /**
     * transforms generics types from an old context to a new context using the given spec. This method assumes
     * all generics types will be placeholders. WARNING: The resulting generics types may or may not be placeholders
     * after the transformation.
     * @param genericsSpec the generics context information spec
     * @param oldPlaceHolders the old placeholders
     * @return the new generics types
     */
    public static GenericsType[] applyGenericsContextToPlaceHolders(Map<String, ClassDefinition> genericsSpec, GenericsType[] oldPlaceHolders) {
        if (oldPlaceHolders==null || oldPlaceHolders.length==0) return oldPlaceHolders;
        if (genericsSpec.isEmpty()) return oldPlaceHolders;
        GenericsType[] newTypes = new GenericsType[oldPlaceHolders.length];
        for (int i=0; i<oldPlaceHolders.length; i++) {
            GenericsType old = oldPlaceHolders[i];
            if (!old.isPlaceholder()) throw new MirahBugError("Given generics type "+old+" must be a placeholder!");
            ClassDefinition fromSpec = genericsSpec.get(old.getName());
            if (fromSpec!=null) {
//                if (fromSpec.isGenericsPlaceHolder()) {
//                    ClassDefinition[] upper = new ClassDefinition[]{fromSpec.redirect()};
//                    newTypes[i] = new GenericsType(fromSpec, upper, null);
//                } else {
//                    newTypes[i] = new GenericsType(fromSpec);
//                }
            } else {
                ClassDefinition[] upper = old.getUpperBounds();
                ClassDefinition[] newUpper = upper;
                if (upper!=null && upper.length>0) {
                    ClassDefinition[] upperCorrected = new ClassDefinition[upper.length];
                    for (int j=0;j<upper.length;j++) {
                        upperCorrected[i] = correctToGenericsSpecRecurse(genericsSpec,upper[j]);
                    }
                    upper = upperCorrected;
                }
                ClassDefinition lower = old.getLowerBound();
                ClassDefinition newLower = correctToGenericsSpecRecurse(genericsSpec,lower);
                if (lower==newLower && upper==newUpper) {
                    newTypes[i] = oldPlaceHolders[i];
                } else {
                    ClassDefinition newPlaceHolder = ClassHelper.make(old.getName());
                    GenericsType gt = new GenericsType(newPlaceHolder, newUpper, newLower);
                    gt.setPlaceholder(true);
                    newTypes[i] = gt;
                }
            }
        }
        return newTypes;
    }
}
