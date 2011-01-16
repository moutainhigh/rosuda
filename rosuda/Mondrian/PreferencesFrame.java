//
//  PreferencesFrame.java
//  Mandarin
//
//  Created by Simon Urbanek on Wed Jun 11 2003.
//  Copyright (c) 2003 __MyCompanyName__. All rights reserved.
//

import java.awt.*;
import java.awt.event.*;
import javax.swing.JColorChooser;
import javax.swing.JFrame;
import java.util.prefs.*;		 // for preferences

public class PreferencesFrame extends Frame implements WindowListener, MouseListener, ActionListener, ItemListener {
  PrefCanvas pc;
  Join frame;
  Choice cs;
  Preferences prefs;
  static String[] schemes = {
    "RoSuDa classic","#ffff99","#c0c0c0","#000000","#00ff00",  
    "Terra di Siena","#dfb860","#c0c0c0", "#000000","#b46087", 
    "Xtra red","#ffffB3","#c0c0c0","#000000","#ff0000", 
    "DataDesk","#000000","#000000","#ffffff","#ff0000", 
    "Daltonian","#009999","#c0c0c0","#000000","#ff7400", 
    null
  };

  static PreferencesFrame last=null;

  public static PreferencesFrame showPrefsDialog(Join frame) {
    if (last==null)
      last=new PreferencesFrame();
    last.frame = frame;
    last.setVisible(true);
    return last;
  }

  public PreferencesFrame() {
    super("Preferences");
    setLayout(new BorderLayout());
    add(pc=new PrefCanvas());
    pc.addMouseListener(this);
    Panel p=new Panel();
    Panel pp=new Panel();
    pp.setLayout(new BorderLayout());
    Panel ppp=new Panel();
    pp.add(p,BorderLayout.SOUTH);
    pp.add(ppp);
    ppp.setLayout(new FlowLayout());
    ppp.add(new Label("Color scheme:"));
    ppp.add(cs=new Choice());
    cs.add("Custom ...");
    int i=0;
    while (schemes[i]!=null) {
      cs.add(schemes[i]);
      if (schemes[i+1].compareTo(Util.color2hrgb(pc.c[0]))==0 &&
          schemes[i+2].compareTo(Util.color2hrgb(pc.c[1]))==0 &&
          schemes[i+3].compareTo(Util.color2hrgb(pc.c[2]))==0 &&
          schemes[i+4].compareTo(Util.color2hrgb(pc.c[3]))==0)
        cs.select(schemes[i]);
      i+=5;
    }
    cs.addItemListener(this);
    p.setLayout(new FlowLayout());
    Button b=null;
    p.add(b=new Button("Save")); b.addActionListener(this);
    p.add(b=new Button("Apply")); b.addActionListener(this);
    p.add(b=new Button("Close")); b.addActionListener(this);
    add(pp,BorderLayout.SOUTH);
    pack();
    addWindowListener(this);
  }

  class PrefCanvas extends Canvas {
    Color c[];
    PrefCanvas() {
      setSize(250,160);
      c=new Color[4];
      c[0]=MFrame.backgroundColor;
      c[1]=MFrame.objectColor;
      c[2]=MFrame.lineColor;
      c[3]=DragBox.hiliteColor;
    }

    public void paint(Graphics g) {
      g.setFont(new Font("SansSerif",0,11));
      g.drawString("background color:",30,35);
//      g.setColor(Color.gray);
      g.drawString("objects color:",30,65);
      g.drawString("text/lines color:",30,95);
      g.setColor(Color.black);
      g.drawString("highlighting color:",30,125);
      g.setColor(c[0]);
      g.fillRect(170,20,30,20);
      g.setColor(c[1]);
      g.fillRect(170,50,30,20);
      g.setColor(c[2]);
      g.fillRect(170,80,30,20);
      g.setColor(c[3]);
      g.fillRect(170,110,30,20);
      g.setColor(Color.black);
      g.drawRect(170,20,30,20);
      g.drawRect(170,50,30,20);
      g.drawRect(170,80,30,20);
      g.drawRect(170,110,30,20);
    }
  }

  public void windowClosing(WindowEvent e) {
    setVisible(false);
  }
  public void windowClosed(WindowEvent e) {}
  public void windowOpened(WindowEvent e) {}
  public void windowIconified(WindowEvent e) {}
  public void windowDeiconified(WindowEvent e) {}
  public void windowActivated(WindowEvent e) {}
  public void windowDeactivated(WindowEvent e) {}

  public void itemStateChanged(ItemEvent e) {
    String s=cs.getSelectedItem();
    int i=0;
    while (schemes[i]!=null) {
      if (schemes[i]==s) {
        Color cl=Util.hrgb2color(schemes[++i]);
        if (cl!=null) pc.c[0]=cl;
        cl=Util.hrgb2color(schemes[++i]);
        if (cl!=null) pc.c[1]=cl;
        cl=Util.hrgb2color(schemes[++i]);
        if (cl!=null) pc.c[2]=cl;
        cl=Util.hrgb2color(schemes[++i]);
        if (cl!=null) pc.c[3]=cl;
        pc.repaint();
        return;
      }
      i+=5;
    }
  }

  static void setScheme(int dragan) {
    int i=dragan*5;
    Color cl=Util.hrgb2color(schemes[++i]);
    if (cl!=null)
      MFrame.backgroundColor=cl;
    cl=Util.hrgb2color(schemes[++i]);
    if (cl!=null)
      MFrame.objectColor=cl;
    cl=Util.hrgb2color(schemes[++i]);
    if (cl!=null)
      MFrame.lineColor=cl;
    cl=Util.hrgb2color(schemes[++i]);
    if (cl!=null)
      DragBox.hiliteColor=cl;
  }	
  
  
  public void mouseClicked(MouseEvent ev) {
    int x=ev.getX(), y=ev.getY();
    if (x>170 && x<200 && y>20 && y<130) {
      int a=(y-15)/30;
      Color cl=null;
      cl=JColorChooser.showDialog(this,"Choose color",pc.c[a]);
      if (cl!=null) {
        cs.select("Custom ...");
        pc.c[a]=cl;
        pc.repaint();
      }
    }
  }
  public void mousePressed(MouseEvent ev) {
  }
  public void mouseReleased(MouseEvent e) {
  }
  public void mouseDragged(MouseEvent e) {}
  public void mouseMoved(MouseEvent ev) {}
  public void mouseEntered(MouseEvent e) {}
  public void mouseExited(MouseEvent e) {}

  public void actionPerformed(ActionEvent e) {
    String cmd=e.getActionCommand();
    if (cmd=="Close") {
      setVisible(false);
    }
    if (cmd=="Apply" || cmd=="Save") {
      MFrame.backgroundColor=pc.c[0];
      MFrame.objectColor=pc.c[1];
      MFrame.lineColor=pc.c[2];
      DragBox.hiliteColor=pc.c[3];
      frame.updateSelection();
    }
    if (cmd=="Save") {
      prefs = Preferences.userNodeForPackage(this.getClass());
      prefs.put("color.background",Util.color2hrgb(MFrame.backgroundColor));
      prefs.put("color.objects",Util.color2hrgb(MFrame.objectColor));
      prefs.put("color.line",Util.color2hrgb(MFrame.lineColor));
      prefs.put("color.select",Util.color2hrgb(DragBox.hiliteColor));
      setVisible(false);
    }
  }
}
