package org.rosuda.JGR.toolkit;

//
//  FontTracker.java
//  JGR
//
//  Created by Markus Helbig on Fri Mar 05 2004.
//  Copyright (c) 2004 __MyCompanyName__. All rights reserved.
//

import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.text.*;

public class FontTracker {

    public static FontTracker current = null;

    Vector components;


    /** FontTracker, every added component will change its font
      * currently there is a bug in the apple jre so we need to make some hacks*/

    public FontTracker() {
        components = new Vector();
    }

    /** add a JComponent
      * @param comp component to add */
    public void add(JComponent comp) {
        //System.out.println(comp);
        comp.setFont(iPreferences.DefaultFont);
        components.add(comp);
    }

    public void setFontBigger() {
        Enumeration e = components.elements();
        iPreferences.FontSize +=2;
        iPreferences.refresh();
        applyFont();
    }

    public void setFontSmaller() {
        Enumeration e = components.elements();
        iPreferences.FontSize -=2;
        iPreferences.refresh();
        applyFont();
    }

    public void applyFont() {
        Enumeration e = components.elements();
        while (e.hasMoreElements()) {
            JComponent comp = (JComponent) e.nextElement();
            comp.setFont(iPreferences.DefaultFont);
            try {
                //System.out.println(comp.getClass().getName());
                if (comp.getClass().getName().equals("javax.swing.JTable") || comp.getClass().getName().equals("org.rosuda.JGR.RObjectManager$1")) {
                    ((JTable) comp).setRowHeight((int) (iPreferences.FontSize *1.3));
                }
            } catch (Exception ex) {}
        }
    }

}