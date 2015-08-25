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
@ActionID(category = "File",id = "ru.programpark.mirah.tests.ParseMirahAction")
@ActionRegistration(displayName = "Parse Mirah File")
@ActionReference(path = "Menu/File",position = 1210)
public final class ParseMirahAction implements ActionListener {
    
    @Override
    public void actionPerformed(ActionEvent e) {
        final Runnable action = new Runnable() {
            @Override
            public void run() {
                ParseMirah.dumpDocument();
            }
        };
        action.run();
    }
}