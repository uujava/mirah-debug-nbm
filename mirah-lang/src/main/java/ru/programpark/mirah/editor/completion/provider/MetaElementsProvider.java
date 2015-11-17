package ru.programpark.mirah.editor.completion.provider;

import ru.programpark.mirah.editor.completion.CompletionItem;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import mirah.lang.ast.MethodDefinition;
import ru.programpark.mirah.editor.completion.FieldSignature;
import ru.programpark.mirah.editor.completion.MethodSignature;
import ru.programpark.mirah.editor.completion.context.CompletionContext;
import org.openide.util.lookup.ServiceProvider;
import ru.programpark.mirah.editor.utils.Utilities;

/**
 * FIXME: this should somehow use compilation classpath.
 * 
 * @author Petr Hejl
 * @author Martin Janicek
 */
/*
@ServiceProvider(
    service = CompletionProvider.class,
    position = 500
)
*/
public final class MetaElementsProvider implements CompletionProvider {

    
    @Override
    public Map<MethodSignature, CompletionItem> getMethods(CompletionContext context) {
        final Map<MethodSignature, CompletionItem> result = new HashMap<MethodSignature, CompletionItem>();
        final Class clz = loadClass(context.getTypeName());
        /*
        if (clz != null) {
            final MetaClass metaClz = GroovySystem.getMetaClassRegistry().getMetaClass(clz);

            if (metaClz != null) {
                for (MethodDefinition method : metaClz.getMethodDefinitions()) {
                    populateProposal(clz, method, context.getPrefix(), context.getAnchor(), result, context.isNameOnly());
                }
            }
        }
                */
        return result;
    }

    @Override
    public Map<MethodSignature, CompletionItem> getStaticMethods(CompletionContext context) {
        return Collections.emptyMap();
    }

    @Override
    public Map<FieldSignature, CompletionItem> getFields(CompletionContext context) {
        final Map<FieldSignature, CompletionItem> result = new HashMap<FieldSignature, CompletionItem>();
        final Class clazz = loadClass(context.getTypeName());
        
        if (clazz != null) {
            /*
            final MetaClass metaClass = GroovySystem.getMetaClassRegistry().getMetaClass(clazz);

            if (metaClass != null) {
                
                for (Object field : metaClass.getProperties()) {
                    MetaProperty prop = (MetaProperty) field;
                    if (prop.getName().startsWith(context.getPrefix())) {
                        result.put(new FieldSignature(prop.getName()), new CompletionItem.FieldItem(
                                prop.getType().getSimpleName(),
                                prop.getName(),
                                prop.getModifiers(),
                                context.getAnchor()));
                    }
                }
            }
                    */
        }
        
        return result;
    }

    @Override
    public Map<FieldSignature, CompletionItem> getStaticFields(CompletionContext context) {
        return Collections.emptyMap();
    }
    
    private Class loadClass(String className) {
        try {
            // FIXME should be loaded by classpath classloader
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        } catch (NoClassDefFoundError err) {
            return null;
        }
    }

    private void populateProposal(Class clz, MethodDefinition method, String prefix, int anchor,
            Map<MethodSignature, CompletionItem> methodList, boolean nameOnly) {

        if (method.name().identifier().startsWith(prefix)) {
            addOrReplaceItem(methodList, new CompletionItem.MethodDefinitionItem(clz, method, anchor, true, nameOnly));
        }
    }

    private void addOrReplaceItem(Map<MethodSignature, CompletionItem> methodItemList, CompletionItem.MethodDefinitionItem itemToStore) {

        // if we have a method in-store which has the same name and same signature
        // then replace it if we have a method with a higher distance to the super-class.
        // For example: toString() is defined in java.lang.Object and java.lang.String
        // therefore take the one from String.
/*
        MethodDefinition methodToStore = itemToStore.getMethod();

        for (CompletionItem methodItem : methodItemList.values()) {
            if (methodItem instanceof MethodDefinitionItem) {
                MethodDefinition currentMethod = ((MethodDefinitionItem) methodItem).getMethod();

                if (isSameMethod(currentMethod, methodToStore)) {
                    if (isBetterDistance(currentMethod, methodToStore)) {
                        methodItemList.remove(getSignature(currentMethod));
                        methodItemList.put(getSignature(methodToStore), itemToStore);
                    }
                    return;
                }
            }
        }

        // We don't have method with the same signature yet
        methodItemList.put(getSignature(methodToStore), itemToStore);
*/        
    }

    private boolean isSameMethod(MethodDefinition currentMethod, MethodDefinition methodToStore) {
//        if (!currentMethod.getName().equals(methodToStore.getName())) {
//            return false;
//        }
        
        int mask = java.lang.reflect.Modifier.PRIVATE |
                   java.lang.reflect.Modifier.PROTECTED |
                   java.lang.reflect.Modifier.PUBLIC |
                   java.lang.reflect.Modifier.STATIC;
//        if ((currentMethod.getModifiers() & mask) != (methodToStore.getModifiers() & mask)) {
//            return false;
//        }
        
//        if (!isSameParams(currentMethod.getParameterTypes(), methodToStore.getParameterTypes())) {
//            return false;
//        }
        
        return true;
    }
/*
    private boolean isSameParams(CachedClass[] parameters1, CachedClass[] parameters2) {
        if (parameters1.length == parameters2.length) {
            for (int i = 0, size = parameters1.length; i < size; i++) {
                if (parameters1[i] != parameters2[i]) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
*/
    private MethodSignature getSignature(MethodDefinition method) {
        String[] parameters = new String[4]; //method.getParameterTypes().length];
        for (int i = 0; i < parameters.length; i++) {
//            parameters[i] = Utilities.translateClassLoaderTypeName(method.getParameterTypes()[i].getName());
        }

        return new MethodSignature(method.name().identifier(), parameters);
    }
    
    private boolean isBetterDistance(MethodDefinition currentMethod, MethodDefinition methodToStore) {
        String currentClassName = ""; //currentMethod.getDeclaringClass().getName();
        String toStoreClassName = ""; //methodToStore.getDeclaringClass().getName();

        // In some cases (e.g. #206610) there is the same distance between java.lang.Object and some
        // other interface java.util.Map and in such cases we always want to prefer the interface over
        // the java.lang.Object
        if ("java.lang.Object".equals(currentClassName)) {
            return true;
        }
        if ("java.lang.Object".equals(toStoreClassName)) {
            return false;
        }

        int currentSuperClassDistance = -1; //currentMethod.getDeclaringClass().getSuperClassDistance();
        int toStoreSuperClassDistance = -1; //methodToStore.getDeclaringClass().getSuperClassDistance();
        if (currentSuperClassDistance < toStoreSuperClassDistance) {
            return true;
        }

        if (currentSuperClassDistance == toStoreSuperClassDistance) {
            // Always prefer Set methods over the Collection methods
            if ("java.util.Collection".equals(currentClassName) && "java.util.Set".equals(toStoreClassName)) {
                return true;
            }
            if ("java.util.Collection".equals(toStoreClassName) && "java.util.Set".equals(currentClassName)) {
                return false;
            }

            // Always prefer List methods over the Collection methods
            if ("java.util.Collection".equals(currentClassName) && "java.util.List".equals(toStoreClassName)) {
                return true;
            }
            if ("java.util.Collection".equals(toStoreClassName) && "java.util.List".equals(currentClassName)) {
                return false;
            }
        }
        return false;
    }
}
