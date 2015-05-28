package ru.programpark.mirah.editor.completion.provider;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import mirah.lang.ast.ClassDefinition;
import mirah.lang.ast.FieldDeclaration;
import mirah.lang.ast.MethodDefinition;
import ru.programpark.mirah.editor.api.completion.CompletionItem;
import ru.programpark.mirah.editor.api.completion.CompletionItem.FieldItem;
import ru.programpark.mirah.editor.api.completion.FieldSignature;
import ru.programpark.mirah.editor.api.completion.MethodSignature;
import ru.programpark.mirah.editor.utils.Utilities;
import ru.programpark.mirah.index.MirahIndex;

/**
 * Code completion handler focused on Mirah AST transformations. For example using
 * {@code @Singleton} on a certain Mirah class creates field called "instance" and
 * also accessor with name "getInstance".
 * 
 * @author Martin Janicek
 */
public final class TransformationHandler {
 
    private static final String SINGLETON_ANNOTATION = "Singleton"; // NOI18N
    private static final String DELEGATE_ANNOTATION = "Delegate"; // NOI18N
    
    private static final String SINGLETON_FIELD_NAME = "instance"; // NOI18N
    private static final String SINGLETON_METHOD_NAME = "getInstance"; // NOI18N
    
    
    public static Map<FieldSignature, CompletionItem> getFields(
            final MirahIndex index,
            final ClassDefinition typeNode,
            final String prefix,
            final int anchorOffset) {
        
        final Map<FieldSignature, CompletionItem> result = new HashMap<FieldSignature, CompletionItem>();
        /*
        for (AnnotationNode annotation : typeNode.getAnnotations()) {
            final String annotationName = annotation.getClassDefinition().getNameWithoutPackage();
            
            if (SINGLETON_ANNOTATION.equals(annotationName)) {
                final FieldSignature signature = new FieldSignature(SINGLETON_FIELD_NAME);
                final CompletionItem proposal = new FieldItem(
                        typeNode.getNameWithoutPackage(),
                        SINGLETON_FIELD_NAME,
                        Modifier.STATIC,
                        anchorOffset);
                
                if (signature.getName().startsWith(prefix)) { // NOI18N
                    result.put(signature, proposal);
                }
            }
        }
        */
        /*
        for (FieldDeclaration field : typeNode.getFields()) {
            for (AnnotationNode fieldAnnotation : field.getAnnotations()) {
                final String fieldAnnotationName = fieldAnnotation.getClassDefinition().getNameWithoutPackage();

                // If any field on the current typeNode has @Delegate annotation then iterate
                // through all fields of that field and put them into the code completion result
                if (DELEGATE_ANNOTATION.equals(fieldAnnotationName)) {
                    for (FieldDeclaration annotatedField : field.getType().getFields()) {
                        final FieldSignature signature = new FieldSignature(annotatedField.getName());
                        final CompletionItem fieldProposal = new FieldItem(
                                annotatedField.getType().getNameWithoutPackage(),
                                annotatedField.getName(),
                                annotatedField.getModifiers(),
                                anchorOffset);

                        if (signature.getName().startsWith(prefix)) {
                            result.put(signature, fieldProposal);
                        }
                    }
                }
            }
        }
        */
        return result;
    }
    
    public static Map<MethodSignature, CompletionItem> getMethods(
            final MirahIndex index,
            final ClassDefinition typeNode,
            final String prefix,
            final int anchorOffset) {
        
        final Map<MethodSignature, CompletionItem> result = new HashMap<MethodSignature, CompletionItem>();
        final boolean prefixed = "".equals(prefix) ? false : true; // NOI18N
        /*
        for (AnnotationNode annotation : typeNode.getAnnotations()) {
            final String annotationName = annotation.getClassDefinition().getNameWithoutPackage();
            
            if (SINGLETON_ANNOTATION.equals(annotationName)) {
                final MethodSignature signature = new MethodSignature(SINGLETON_METHOD_NAME, new String[0]);
                final CompletionItem proposal = CompletionItem.forJavaMethod(
                        typeNode.getNameWithoutPackage(),
                        SINGLETON_METHOD_NAME,
                        Collections.<String>emptyList(),
                        typeNode.getNameWithoutPackage(),
                        Utilities.reflectionModifiersToModel(Modifier.STATIC),
                        anchorOffset,
                        true,
                        false);
                
                if (signature.getName().startsWith(prefix)) {
                    result.put(signature, proposal);
                }
            }
        }
        */
        /*
        for (FieldDeclaration field : typeNode.getFields()) {
            for (AnnotationNode fieldAnnotation : field.getAnnotations()) {
                final String fieldAnnotationName = fieldAnnotation.getClassDefinition().getNameWithoutPackage();

                if (DELEGATE_ANNOTATION.equals(fieldAnnotationName)) {
                    for (MethodDefinition method : field.getType().getMethods()) {
                        final MethodSignature signature = getSignature(method);
                        final CompletionItem proposal = createMethodProposal(method, prefixed, anchorOffset);
                        
                        if (signature.getName().startsWith(prefix)) { // NOI18N
                            result.put(signature, proposal);
                        }
                    }
                }
            }
        }
        */
        return result;
    }
    
    private static MethodSignature getSignature(MethodDefinition method) {
        String[] parameters = new String[0]; // method.getParameters().length];
        for (int i = 0; i < parameters.length; i++) {
//            parameters[i] = Utilities.translateClassLoaderTypeName(method.getParameters()[i].getName());
        }

        return new MethodSignature(method.name().identifier(), parameters);
    }
    
    private static CompletionItem createMethodProposal(
            final MethodDefinition method, 
            final boolean prefixed,
            final int anchorOffset) {
        
        final String methodName = method.name().identifier();
        final String[] methodParams = getMethodParams(method);
        final String returnType = method.type().typeref().name();

        return CompletionItem.forDynamicMethod(anchorOffset, methodName, methodParams, returnType, prefixed);
    }
    
    private static String[] getMethodParams(MethodDefinition method) {
        String[] parameters = new String[0]; //method.getParameters().length];
        for (int i = 0; i < parameters.length; i++) {
//            parameters[i] = method.getParameters()[i].getName();
        }
        return parameters;
    }
}
