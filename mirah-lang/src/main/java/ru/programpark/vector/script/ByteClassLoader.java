/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.programpark.vector.script;

import java.io.PrintStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

public class ByteClassLoader extends URLClassLoader {

    public ByteClassLoader(ClassLoader parent) {
        super(new URL[0], parent);
        class_map = new HashMap(16);
    }

    public void add(String name, byte bytes[]) {
        name = name.replaceAll("/", ".");
        class_map.put(name, bytes);
    }

    public boolean contains(String name) {
        return class_map.containsKey(name);
    }

    public Class findClass(String name) throws ClassNotFoundException {
        System.out.println((new StringBuilder()).append("find class: ").append(name).toString());
        byte bytes[] = (byte[]) class_map.get(name);
        return bytes == null ? super.findClass(name) : defineClass(name, bytes, 0, bytes.length);
    }

    private Map class_map;
}
