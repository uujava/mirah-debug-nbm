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
@ActionID(category = "File",id = "ru.programpark.mirah.tests.ParseMirahAction")
@ActionRegistration(displayName = "Parse VRB File")
//@ActionReference(path = "Menu/File",position = 1210)
@ActionReference(path = "Editors/text/x-vruby/Popup", position = 5210,separatorBefore = 5200)
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