/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.programpark.mirah.tests;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;

/**
 *
 * @author sdv
 */
@ActionID(category = "File",id = "ru.programpark.mirah.tests.PrintIndexAction")
@ActionRegistration(displayName = "Print Index")
@ActionReference(path = "Menu/File",position = 1230)
public final class PrintIndexAction implements ActionListener {
    
    @Override
    public void actionPerformed(ActionEvent e) {
        final Runnable action = new Runnable() {
            @Override
            public void run() {
                PrintIndex.printIndex();
            }
        };
        action.run();
    }
    
}