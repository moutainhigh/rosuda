package org.rosuda.JGR.toolkit;

//JGR - Java Gui for R, see http://www.rosuda.org/JGR/
//Copyright (C) 2003 - 2005 Markus Helbig
//--- for licensing information see LICENSE file in the original JGR distribution ---


import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import org.rosuda.ibase.*;

/**
 *  AboutDialog - show splashscreen with authors and copyright information
 * 
 *	@author Markus Helbig
 *  
 * 	RoSuDa 2003 - 2005 
 */

public class AboutDialog extends SplashScreen implements MouseListener {



    private Dimension screenSize = Common.getScreenRes();
    private Image splash;

    public AboutDialog() {
        this(null);
    }

    /**
     * Create new aboutdialog (splashscreen).
     * @param f parent frame
     */
    public AboutDialog(JFrame f) {
        this.addMouseListener(this);
        this.setVisible(true);
    }

    /**
     * mouseClicked: handle mouse event: dispose when mouse clicks on splash
     */
    public void mouseClicked(MouseEvent e) {
        this.dispose();
    }

    /**
     * mouseEntered: handle mouse event.
     */
    public void mouseEntered(MouseEvent e) {
    }

    /**
     * mousePressed: handle mouse event.
     */
    public void mousePressed(MouseEvent e) {
    }

    /**
     * mouseReleased: handle mouse event.
     */
    public void mouseReleased(MouseEvent e) {
    }

    /**
     * mouseExited: handle mouse event.
     */
    public void mouseExited(MouseEvent e) {
    }
}