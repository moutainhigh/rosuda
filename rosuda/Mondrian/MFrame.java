
//
// A closeable Frame for Mondrian.
//

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.Vector;
import java.util.Properties;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.*;
import javax.swing.event.*;

public class MFrame extends JFrame implements WindowListener {
  
  private Join J;
  private JMenuItem m;
  public String selString = "";
  private int counter = 0;

  private Timer resizePlotTimer    = new Timer();
  private TimerTask resizePlotTask;
  private boolean firstTime = true;
  
  static Color backgroundColor = new Color(223, 184, 96);
  static Color objectColor     = Color.lightGray;
  static Color lineColor       = Color.black;
  
  public MFrame(Join J) {
    this.J = J;
    this.setBackground(backgroundColor);
    addWindowListener(this);
  }
  
  public final void initComponents(final DragBox DB) {
    resizePlotTask = new ResizePlotTask(DB);
    this.addComponentListener(new java.awt.event.ComponentAdapter() {
      public final void componentResized(final java.awt.event.ComponentEvent evt) {
        lblPlotComponentResized(DB, evt);
      }
    });
  }
  
  public void windowClosing(WindowEvent e) {
    close();
  }

  public boolean getAlphaHi() {
    return J.alphaHi;
  }

  public boolean hasR() {
    return J.hasR;
  }

  public void close() {
    System.out.println("Window Closed!!");

    J.windows.remove(m);
    if( J.windows.getItemCount() < 3 )
      J.ca.setEnabled(false);
    if( !selString.equals("") )
      J.updateSelection();
    this.setVisible(false);
    this.dispose();
  }

  public void maintainMenu(int step) {
    selString += " ["+step+"]";
    m.setText( getTitle() + selString );
  }

  public void maintainMenu(boolean preserve) {
    if( !preserve )
      selString = "";
    m.setText( getTitle() + selString );
  }

  public void show() {
    boolean same = false, added = false;

    m = new JMenuItem(getTitle());
    J.ca.setEnabled(true);

    for(int i=2; i<J.windows.getItemCount(); i++)
      if( ((J.windows.getItem(i)).getText()).substring(0,2).equals((m.getText()).substring(0,2)) )
        same = true;
      else
        if( same ) {
          J.windows.insert(m, i);
          added = true;
          same = false;
        }

    if( !added )
      J.windows.add(m);
    
    m.addActionListener(new ActionListener() {     
      public void actionPerformed(ActionEvent e) {
        toFront();
      }
    });

    super.show();
  }
  
  public void windowClosed(WindowEvent e) {}
  public void windowIconified(WindowEvent e) {}
  public void windowOpened(WindowEvent e) {}
  public void windowDeiconified(WindowEvent e) {}
  public void windowActivated(WindowEvent e) {}
  public void windowDeactivated(WindowEvent e) {}

  private final void lblPlotComponentResized(DragBox DB, final java.awt.event.ComponentEvent evt) {      
    if( resizePlotTask != null && !firstTime ) {
//      System.out.println("+++++++++++ Canceled "+resizePlotTask);
      resizePlotTask.cancel();
      System.out.println(" Cancled timer!: "+(--counter));
    }
    if( !firstTime ) {
      DB.resizeReady = false;
      resizePlotTask = new ResizePlotTask(DB);
    System.out.println("+++++++++++ Created "+(++counter));
      resizePlotTimer.schedule(resizePlotTask, 200);
    }
    firstTime = false;
  }
}
