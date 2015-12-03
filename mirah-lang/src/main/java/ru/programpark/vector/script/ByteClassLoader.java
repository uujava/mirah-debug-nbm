/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.programpark.vector.script;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;

public class ByteClassLoader extends URLClassLoader {

//    public ByteClassLoader(ClassLoader parent,String loadPath) {
    public ByteClassLoader(ClassLoader parent) {
        super(new URL[0], parent);
        class_map = new HashMap(16);
//        this.loadPath = loadPath;
    }

    public void add(String name, byte bytes[]) {
        name = name.replaceAll("/", ".");
        class_map.put(name, bytes);
    }

    public boolean contains(String name) {
        return class_map.containsKey(name);
    }

    public void close() {
//        loadPath = null;
        class_map.clear();
        class_map = null;
    }
    
    public Class findClass(String name) throws ClassNotFoundException {
        System.out.println((new StringBuilder()).append("find class: ").append(name).toString());
        byte bytes[] = (byte[]) class_map.get(name);
//        if ( bytes == null ) {
//            String s = name.replace('.', '/');
//            String fileName = loadPath + "/" + name.replace('.', '/') + ".class";
//            File f = new File(fileName);
//            if ( f.exists() ) {
//                try {
//                    bytes = Files.readAllBytes(Paths.get(fileName));
//                } catch (IOException ex) {
//                    Exceptions.printStackTrace(ex);
//                }
//                if ( bytes != null ) add(name,bytes);
//            }
//        }
        return bytes == null ? super.findClass(name) : defineClass(name, bytes, 0, bytes.length);
    }

    // загрузить все скомпилированные классы
    public void loadAllClasses(String loadPath) throws IOException {
        FileObject loadDir = FileUtil.toFileObject(new File(loadPath));
        for( FileObject fo : loadDir.getChildren() )
        {
            if ( "class".equalsIgnoreCase(fo.getExt() )) {
                String name = fo.getPath().substring(loadPath.length());
                if ( name.startsWith("/")) name = name.substring(1);
                name = name.substring(0,name.length()-6);
                add(name, fo.asBytes());
            }
        }
    }
    
    private Map class_map;
//    private String loadPath;
}
