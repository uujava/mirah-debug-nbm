/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.programpark.mirah.tests;

import ca.weblite.netbeans.mirah.LOG;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.Lookup;

/**
 *
 * @author sdv
 */
@ActionID(category = "File",id = "ru.programpark.mirah.tests.PrintAstAction")
@ActionRegistration(displayName = "Print Ast Path")
//@ActionReferences({@ActionReference(path = "Menu/File",position = 1200)
//,@ActionReference(path = "Menu/Window",position = 1200)
//,@ActionReference(path = "Menu/Edit",position = 1200)
//})
@ActionReference(path = "Menu/File",position = 1200)
public final class PrintAstAction implements ActionListener {
    
//    static String project_dir = "c:\\mirah-debug\\mavenproject1";
    
    @Override
    public void actionPerformed(ActionEvent e) {
        final Runnable action = new Runnable() {
            @Override
            public void run() {
                PrintAst.printAst();
            }
        };
        action.run();
    }
}