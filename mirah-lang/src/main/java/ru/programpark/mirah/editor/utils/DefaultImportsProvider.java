package ru.programpark.mirah.editor.utils;

import java.util.Set;

/**
 * Provides additional default imports.
 * 
 * @author Martin Janicek
 */
public interface DefaultImportsProvider {

    Set<String> getDefaultImportPackages();
    
    Set<String> getDefaultImportClasses();

}
