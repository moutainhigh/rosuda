package org.rosuda.JGR;

//
//  JGR.java
//  JGR
//
//  Created by Markus Helbig on Fri Mar 05 2004.
//  Copyright (c) 2004 __MyCompanyName__. All rights reserved.
//


import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;


import org.rosuda.JRI.*;
import org.rosuda.JGR.rhelp.*;
import org.rosuda.JGR.toolkit.*;
import org.rosuda.ibase.*;
import org.rosuda.util.*;

public class JGR {


    public static Vector RHISTORY = null;
    public static RConsole MAINRCONSOLE = null;
    public static String RHOME = "";
    public static String[] RLIBS;
    public static Rengine R = null;
    public static ConsoleSync rSync = new ConsoleSync();
    public static boolean STARTED = false;
    public static boolean READY = false;

    public static Vector DATA = new Vector();
    public static Vector MODELS = new Vector();
    public static Vector OTHERS = new Vector();

    public static int SLEEPTIME = 50;
    public static int STRINGBUFFERSIZE = 80;

    private static JGRListener jgrlistener  = null;

    public static SplashScreen splash;

    public JGR() {
        SVar.int_NA=-2147483648;
        Platform.initPlatform("org.rosuda.JGR.toolkit.");
        iPreferences.initialize();
        splash = new SplashScreen();
        splash.start();
        readHistory();
        MAINRCONSOLE = new RConsole();
        splash.toFront();
        if (System.getProperty("os.name").startsWith("Window")) splash.stop();
        MAINRCONSOLE.progress.start("Starting R");
        MAINRCONSOLE.setWorking(true);
        String[] args={"--save"};
        R=new Rengine(args,true,MAINRCONSOLE);
        System.out.println("Rengine created, waiting for R");
        if (!R.waitForR()) {
            System.out.println("Cannot load R");
            System.exit(1);
        }
        RHOME = RTalk.getRHome();
        RLIBS = RTalk.getRLIBS();
        for (int i = 0; i< RLIBS.length; i++) {
            if(RLIBS[i].startsWith("~")) RLIBS[i] = RLIBS[i].replaceFirst("~",System.getProperty("user.home"));
        }
        iPreferences.refreshKeyWords();
        MAINRCONSOLE.setWorking(false);
        MAINRCONSOLE.input.requestFocus();
        STARTED = true;
        if (!System.getProperty("os.name").startsWith("Window")) splash.stop();
        rSync.triggerNotification("library(JGR, warn.conflicts=FALSE)");
    }

    public static String exit() {
        int exit = JOptionPane.showConfirmDialog(null, "Save workspace?",
                                                 "Close JGR",
                                                 JOptionPane.
                                                 YES_NO_CANCEL_OPTION,
                                                 JOptionPane.QUESTION_MESSAGE);

        if (exit == 0) {
            writeHistory();
            return "y\n";
        }
        else if (exit == 1) return "n\n";
        else return "c\n";
    }


    public static void help(String keyword, String file, String location) {
        if (file.trim().equals("null")) file = null;
        if (keyword.trim().equals("null")) keyword = null;
        if (location.trim().equals("null")) location = null;
        if (RHelp.current == null) new RHelp(location);
        else {
            RHelp.current.show();
            RHelp.current.refresh();
        }
        if (keyword!=null && file !=null) RHelp.current.goTo(keyword, file);
    }

    public static void addMenu(String name) {
        iMenu.addMenu(MAINRCONSOLE,name);
    }

    public static void addMenuItem(String menu, String name, String cmd) {
        if (jgrlistener == null) jgrlistener = new JGRListener();
        iMenu.addMenuItem(MAINRCONSOLE,menu,name,cmd,jgrlistener);
    }

    public static void fix(String data, String type) {
        System.out.println(type);
        if (type.equals("data.frame")) new DataTable(RTalk.getVarSet(RTalk.createDataFrame(data)));
        else if (type.equals("matrix")) new DataTable(RTalk.getVarSet(RTalk.createMatrix(data)));
    }



    public static void readHistory() {
        File hist = null;
        try {
            if ((hist = new File(System.getProperty("user.home") +
                                  File.separator + ".Rhistory")).exists()) {

                 BufferedReader reader = new BufferedReader(new FileReader(hist));
                 RHISTORY = new Vector();
                 while (reader.ready()) RHISTORY.add(reader.readLine());
                 reader.close();
            }
        }
        catch (Exception e) {
            new iError(e);
        }
    }

    public static void writeHistory() {
        File hist = null;
        try {
            hist = new File(System.getProperty("user.home") +
                                  File.separator + ".Rhistory");
            BufferedWriter writer = new BufferedWriter(new FileWriter(hist));
            Enumeration e = RHISTORY.elements(); int i = 0;
            while(e.hasMoreElements()) writer.write(e.nextElement().toString()+"\n");
            writer.flush();
            writer.close();
        }
        catch (Exception e) {
            new iError(e);
        }


    }

    public static void main(String[] args) {
        try {
            JGR JGR1 = new JGR();
        }
        catch (Exception e) {
            e.printStackTrace();
            new iError(e);
        }
    }
}