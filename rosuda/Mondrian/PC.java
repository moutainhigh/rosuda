import java.awt.*;               // ScrollPane, PopupMenu, MenuShortcut, etc.
import java.awt.geom.*;         
import java.awt.image.*;         
import java.awt.event.*;         // 
import java.util.*;              // 
import java.util.Vector;         // 
import java.lang.*;              // 
import java.io.*;              // 
import javax.swing.*;
import javax.swing.event.*;

public class PC extends DragBox implements ActionListener {
  protected int width, height;                   // The preferred size.
  protected int oldWidth, oldHeight;             // The last size for constructing the polygons.
  private Image bi, tbi, ttbi;
  private Graphics2D bg;
  protected int[] vars;
  protected int xVar;
  protected int yVar;
  protected int k;
  protected double slotWidth;
  protected int slotMax = 40;
  protected int addBorder = 0;
  private int outside = 5;
  private int tick    = 5;
  private int roundY;
  protected int selID = -1;
  protected int scaleFactor = 3;
  protected double centerAt = 0;
  protected dataSet data;
  protected double[] dMins, dIQRs, dMedians, dMeans, dSDevs, dMaxs;
  protected double[] Mins, Maxs;
  protected double[][] dataCopy;
  protected String[] lNames;
  protected int[] permA;
  protected double[] sortA;
  protected boolean[] selected;
  protected boolean[] inverted;
  protected boolean hotSelection = false;
  protected boolean zoomToSel = false;
  protected boolean[] onlyHi;
  Polygon[] poly;
  String Scale = "Common";
  String paintMode = "Box";
  String sortMode = "ini";
  String alignMode = "center";
  float alpha = 1F;
  int movingID;
  boolean moving;
  MyText movingName;
  private int lastX;
  Vector names = new Vector(10,10);
  Vector bPlots = new Vector(10,10);
  Vector rects = new Vector(30,10);
  Vector tabs = new Vector(10,10);

  public PC(JFrame frame, dataSet data, int[] vars, String mode) {
    super(frame);
    Dimension size = frame.getSize();
    this.width = size.width;
    this.height = size.height;
    oldWidth = size.width;
    oldHeight = size.height;
    this.vars = vars;
    this.data = data;
    this.k = vars.length;
    this.paintMode = mode;

    border = 22;

    onlyHi = new boolean[data.n];
    for( int i=0; i<data.n; i++ )
      onlyHi[i] = true;
    
    if( k == 2 ) {
      if( data.categorical(vars[0]) && !data.categorical(vars[1]) ) {
        paintMode = "XbyY";
        xVar = vars[1];
        yVar = vars[0];
      }
      if( data.categorical(vars[1]) && !data.categorical(vars[0])) {
        paintMode = "XbyY";
        xVar = vars[0];
        yVar = vars[1];
      }
    }

    if( paintMode.equals("XbyY") ) {
      k = data.getNumLevels(yVar);
      frame.setSize(50 * (1 + k), 400);
    }

    selected = new boolean[k];
    inverted = new boolean[k];

    for( int j=0; j<k; j++ ) {
      selected[j] = false;
      inverted[j] = false;
    }      

    permA = new int[k];
    for(int j=0; j<k; j++)
      permA[j] = j;

    frame.getContentPane().add(this);

    Font SF = new Font("SansSerif", Font.BOLD, 12);
    frame.setFont(SF);

    getData();

    this.setBackground(frame.getBackground());

    setCoordinates(0,1,0,1,-1);

    if( paintMode.equals("XbyY") )
      frame.setTitle("PC("+data.getName(xVar)+"|"+data.getName(yVar)+")");
    else if( paintMode.equals("Poly") )
      frame.setTitle("PC("+data.setName+")");
    else
      frame.setTitle("PB("+data.setName+")");

    // which events we are interested in.
    this.enableEvents(AWTEvent.MOUSE_EVENT_MASK);
    this.enableEvents(AWTEvent.KEY_EVENT_MASK);
    this.requestFocus();
  }


  public String getToolTipText(MouseEvent e) {
    if( e.isControlDown() ) {
      int minXDist = Integer.MAX_VALUE;
      int minYDist = Integer.MAX_VALUE;
      int popXId = 0;
      int popYId = 0;
      Polygon p = poly[0];
      for( int j=0; j<k; j++ ) {
        if( Math.abs(p.xpoints[j]-e.getX()) < minXDist ) {
          popXId = j;
          minXDist =  Math.abs(p.xpoints[j]-e.getX());
        }
      }
      for( int i=0; i<data.n; i++ ) {
        p = poly[i];
        if( Math.abs(p.ypoints[popXId]-e.getY()) < minYDist ) {
          popYId = i;
          minYDist =  Math.abs(p.ypoints[popXId]-e.getY());
        }
      }
      if( minXDist < slotWidth/4 ) {
        String x="";
        if(  data.phoneNumber(vars[permA[popXId]]) ) {
          x = " " + data.getName(vars[permA[popXId]])+'\n'+" Number: "+ Util.toPhoneNumber(dataCopy[permA[popXId]][popYId]);
        }
        else if( data.categorical(vars[permA[popXId]]) )
          if( paintMode.equals("Poly") ) {
            x = " " + data.getName(vars[permA[popXId]]);
            x = " Level: "+data.getLevelName(vars[permA[popXId]], dataCopy[permA[popXId]][popYId]);
          }
            else
              for( int i = 0;i < rects.size(); i++) {
                MyRect r = (MyRect)rects.elementAt(i);
                if ( r.contains( e.getX(), e.getY()+sb.getValue() )) {
                  return Util.info2Html(r.getLabel());
                }
              }
                else {
                  x = data.getName(vars[permA[popXId]]);
                  x = " Value: "+dataCopy[permA[popXId]][popYId];
                }
          return Util.info2Html(x);
        }
      return null;
    } else
      return null;
  }
  

  public void processMouseEvent(MouseEvent e) {

    if (e.getID() == MouseEvent.MOUSE_PRESSED && !paintMode.equals("XbyY")) {
      boolean hit = false;
      for( int j=0; j<k; j++ ) {
        MyText mt = (MyText)names.elementAt(j);
        //System.out.println("Testing Slot: "+j);
        if( mt.contains( e.getX(), e.getY(), (Graphics2D)(this.getGraphics()) ) ) {
          hit = true;
          if( e.getModifiers() == BUTTON1_DOWN + SHIFT_DOWN )
            selected[permA[j]] = !selected[permA[j]];
          else
            selected[permA[j]] = true;
        }
        else if( e.getModifiers() != BUTTON1_DOWN + SHIFT_DOWN && !e.isPopupTrigger() && !(e.getModifiers() ==  BUTTON3_DOWN && SYSTEM == WIN) ) {
          selected[permA[j]] = false;
        }
      }
      if( hit ) {
        update(this.getGraphics());
        return;
      }
    }

    if( e.isPopupTrigger() && !e.isShiftDown() )
      super.processMouseEvent(e);  // Pass other event types on.
    if( changePop ) {
      changePop = false;
      return;
    }

    if ((e.getID() == MouseEvent.MOUSE_PRESSED ||
         e.getID() == MouseEvent.MOUSE_RELEASED) && !e.isShiftDown() ) {
      if ( e.isPopupTrigger() && e.getModifiers() != BUTTON1_DOWN + ALT_DOWN ) {
        super.processMouseEvent(e);
        int minXDist = 5000;
        int minYDist = 5000;
        int popXId = 0;
        int popYId = 0;
        Polygon p = poly[0];
        for( int j=0; j<k; j++ ) {
          if( Math.abs(p.xpoints[j]-e.getX()) < minXDist ) {
            popXId = j;
            minXDist =  Math.abs(p.xpoints[j]-e.getX());
          }
        }
        for( int i=0; i<data.n; i++ ) {
          p = poly[i];
          if( Math.abs(p.ypoints[popXId]-e.getY()) < minYDist ) {
            popYId = i;
            minYDist =  Math.abs(p.ypoints[popXId]-e.getY());
          }
        }
        if( minXDist < slotWidth/4 ) {
          JPopupMenu name = new JPopupMenu();
          JMenuItem colName = null;
          JMenuItem colVal  = null;
          if(  data.phoneNumber(vars[permA[popXId]]) ) {
            colName = new JMenuItem(data.getName(vars[permA[popXId]])+'\n'+"Number: "+ Util.toPhoneNumber(dataCopy[permA[popXId]][popYId]));
          }
          else if( data.categorical(vars[permA[popXId]]) )
            if( paintMode.equals("Poly") ) {
              colName = new JMenuItem(data.getName(vars[permA[popXId]]));
              colVal  = new JMenuItem("   Level: "+data.getLevelName(vars[permA[popXId]], dataCopy[permA[popXId]][popYId]));
            }
              else
                for( int i = 0;i < rects.size(); i++) {
                  MyRect r = (MyRect)rects.elementAt(i);
                  if ( r.contains( e.getX(), e.getY()+sb.getValue() )) {
                    r.pop(this, e.getX(), e.getY());
                  }
                }
                  else {
                    colName = new JMenuItem(data.getName(vars[permA[popXId]]));
                    colVal  = new JMenuItem("   Value: "+dataCopy[permA[popXId]][popYId]);
                  }
                  if( colName != null ) {
                    name.add(colName);
                    name.add(colVal);

                    name.show(e.getComponent(), e.getX(), e.getY());
                  }
        }
          else {
            JPopupMenu scaleType = new JPopupMenu("Title");

            if( Scale.equals("Common") ) {
              String extra = "";
              if( anySelected() )
                extra = "Selected ";
              JMenuItem Com = new JMenuItem("Scale "+extra+"Common");
              scaleType.add(Com);
              Com.setActionCommand("Common");
              Com.addActionListener(this);
            }
            else {
              JMenuItem Ind = new JMenuItem("Scale Individual");
              scaleType.add(Ind);
              Ind.setActionCommand("Individual");
              Ind.addActionListener(this);
            }

            JMenu alignM = new JMenu("Align at");

            JCheckBoxMenuItem centerM = new JCheckBoxMenuItem("Center", false);
            JCheckBoxMenuItem cmeanM = new JCheckBoxMenuItem("Mean", false);
            JCheckBoxMenuItem cmedianM = new JCheckBoxMenuItem("Median", false);
            JCheckBoxMenuItem ccaseM = new JCheckBoxMenuItem("Case", false);
            JCheckBoxMenuItem cvalueM = new JCheckBoxMenuItem("Value ...", false);

            if( alignMode.equals("center") )
              centerM.setState(true);
            if( alignMode.equals("cmean") )
              cmeanM.setState(true);
            if( alignMode.equals("cmedian") )
              cmedianM.setState(true);
            if( data.countSelection() == 1 )
              ccaseM.setEnabled(true);
            else
              ccaseM.setEnabled(false);
            if( alignMode.equals("ccase") )
              ccaseM.setState(true);
            if( alignMode.equals("cvalue") )
              cvalueM.setState(true);

            alignM.add(centerM);
            alignM.add(cmeanM);
            alignM.add(cmedianM);
            alignM.add(ccaseM);
            alignM.add(cvalueM);
            centerM.setActionCommand("center");
            cmeanM.setActionCommand("cmean");
            cmedianM.setActionCommand("cmedian");
            ccaseM.setActionCommand("ccase");
            cvalueM.setActionCommand("cvalue");
            centerM.addActionListener(this);
            cmeanM.addActionListener(this);
            cmedianM.addActionListener(this);
            ccaseM.addActionListener(this);
            cvalueM.addActionListener(this);

            scaleType.add(alignM);

            JMenu sortM = new JMenu("Sort Axes by");
            JCheckBoxMenuItem minM = new JCheckBoxMenuItem("Minimum");
            JCheckBoxMenuItem quarM  = new JCheckBoxMenuItem("IQ-Range");
            JCheckBoxMenuItem medianM  = new JCheckBoxMenuItem("Median");
            JCheckBoxMenuItem meanM = new JCheckBoxMenuItem("Mean");
            JCheckBoxMenuItem sdevM  = new JCheckBoxMenuItem("Std. Dev.");
            JCheckBoxMenuItem maxM  = new JCheckBoxMenuItem("Maximum");
            JCheckBoxMenuItem iniM  = new JCheckBoxMenuItem("Initial");
            JCheckBoxMenuItem revM  = new JCheckBoxMenuItem("Reverse");

            if( sortMode.equals("ini") )
              iniM.setState(true);
            else if( sortMode.equals("rev") )
              revM.setState(true);
            else if( sortMode.equals("min") )
              minM.setState(true);
            else if( sortMode.equals("quar") )
              quarM.setState(true);
            else if( sortMode.equals("median") )
              medianM.setState(true);
            else if( sortMode.equals("mean") )
              meanM.setState(true);
            else if( sortMode.equals("sdev") )
              sdevM.setState(true);
            else if( sortMode.equals("max") )
              maxM.setState(true);
            sortM.add(minM);
            sortM.add(quarM);
            sortM.add(medianM);
            sortM.add(meanM);
            sortM.add(sdevM);
            sortM.add(maxM);
            sortM.addSeparator();
            sortM.add(iniM);
            sortM.add(revM);
            minM.setActionCommand("min");
            quarM.setActionCommand("quar");
            medianM.setActionCommand("median");
            meanM.setActionCommand("mean");
            sdevM.setActionCommand("sdev");
            maxM.setActionCommand("max");
            iniM.setActionCommand("ini");
            revM.setActionCommand("rev");
            minM.addActionListener(this);
            quarM.addActionListener(this);
            medianM.addActionListener(this);	  
            meanM.addActionListener(this);	  
            sdevM.addActionListener(this);	  
            maxM.addActionListener(this);	  
            iniM.addActionListener(this);	  
            revM.addActionListener(this);	  
            scaleType.add(sortM);

            JMenu alphaM = new JMenu("Alpha");
            JCheckBoxMenuItem alpha1 = new JCheckBoxMenuItem("1.0");
            JCheckBoxMenuItem alpha05 = new JCheckBoxMenuItem("0.5");
            JCheckBoxMenuItem alpha01 = new JCheckBoxMenuItem("0.1");
            JCheckBoxMenuItem alpha005  = new JCheckBoxMenuItem("0.05");
            JCheckBoxMenuItem alpha001 = new JCheckBoxMenuItem("0.01");
            JCheckBoxMenuItem alpha0005 = new JCheckBoxMenuItem("0.005");
            if( alpha >= 0.99 )
              alpha1.setState(true);
            else if( alpha >= 0.49 )
              alpha05.setState(true);
            else if( alpha >= 0.099 )
              alpha01.setState(true);
            else if( alpha >= 0.049 )
              alpha005.setState(true);
            else if( alpha >= 0.0099 )
              alpha001.setState(true);
            else if( alpha >= 0.0049 )
              alpha0005.setState(true);

            alphaM.add(alpha1);
            alphaM.add(alpha05);
            alphaM.add(alpha01);
            alphaM.add(alpha005);
            alphaM.add(alpha001);
            alphaM.add(alpha0005);
            alpha1.setActionCommand("1.0");

            alpha05.setActionCommand("0.5");
            alpha01.setActionCommand("0.1");
            alpha005.setActionCommand("0.05");
            alpha001.setActionCommand("0.01");
            alpha0005.setActionCommand("0.005");
            alpha1.addActionListener(this);
            alpha05.addActionListener(this);
            alpha01.addActionListener(this);
            alpha005.addActionListener(this);
            alpha001.addActionListener(this);	  
            alpha0005.addActionListener(this);	  
            scaleType.add(alphaM);


            JMenu plotM = new JMenu("Type");
            JCheckBoxMenuItem polyM = new JCheckBoxMenuItem("Polygons", false);
            JCheckBoxMenuItem boxM = new JCheckBoxMenuItem("Box Plots", false);
            JCheckBoxMenuItem bothM = new JCheckBoxMenuItem("Both", false);
            if( paintMode.equals("Poly") )
              polyM.setState(true);
            else if( paintMode.equals("Box") )
              boxM.setState(true);
            else if( paintMode.equals("Both") )
              bothM.setState(true);

            plotM.add(polyM);
            plotM.add(boxM);	
            plotM.add(bothM);
            if( zoomToSel ) {
              boxM.setEnabled(false);
              bothM.setEnabled(false);
            }
              
            polyM.setActionCommand("Poly");
            boxM.setActionCommand("Box");
            bothM.setActionCommand("Both");
            polyM.addActionListener(this);
            boxM.addActionListener(this);
            bothM.addActionListener(this);

            scaleType.add(plotM);

            JCheckBoxMenuItem hotSelM = new JCheckBoxMenuItem("HotSelector", hotSelection);
            hotSelM.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, Event.SHIFT_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            hotSelM.setActionCommand("Hot");
            hotSelM.addActionListener(this);

            scaleType.add(hotSelM);

            JMenuItem zoomM = new JMenuItem("Crop Selection");
            zoomM.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            zoomM.setActionCommand("Zeiser");
            zoomM.addActionListener(this);

            scaleType.add(zoomM);
            if( !paintMode.equals("Poly") ) {
              zoomM.setEnabled(false);
            }
            
            JMenuItem homeM = new JMenuItem("Home View");
            homeM.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_0, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            homeM.setActionCommand("Home");
            homeM.addActionListener(this);

            scaleType.add(homeM);

            scaleType.add(new JMenuItem("Dismiss"));

            if( k > 1 && !paintMode.equals("XbyY") ) {
              scaleType.show(e.getComponent(), e.getX(), e.getY());
            }
          }
      }
        else
          if( (e.getID() == MouseEvent.MOUSE_PRESSED ) && !paintMode.equals("XbyY") &&
              ((e.getModifiers() == ALT_DOWN+4) && (System.getProperty("os.name").equals("Irix")) ||
               (e.getModifiers() == ALT_DOWN+4) && (System.getProperty("os.name").equals("Linux")) ||
               (e.getModifiers() == ALT_DOWN) && (System.getProperty("os.name").equals("Mac OS")) ||
               (e.getModifiers() == BUTTON1_DOWN + ALT_DOWN)) ) {
            //
            //  System.out.println("Moving Start");
            //
            moving = true;
            frame.setCursor(Frame.HAND_CURSOR);

            int minXDist = 5000;
            int popXId = 0;
            Polygon p = poly[0];
            for( int j=0; j<k; j++ ) {
              if( Math.abs(p.xpoints[j]-e.getX()) < Math.abs(minXDist) ) {
                popXId = j;
                minXDist =  p.xpoints[j]-e.getX();
              }
            }

            movingID = popXId;

            movingName = (MyText)names.elementAt(movingID);

            lastX = e.getX();
          }
        else if( (e.getID() == MouseEvent.MOUSE_RELEASED) && moving ) {
          //
          //   System.out.println("Moving Stop");
          //
          moving = false;
          frame.setCursor(Frame.DEFAULT_CURSOR);

          int minXDist = 5000;
          int popXId = 0;
          int diff = 0;
          Polygon p = poly[0];
          for( int j=0; j<k; j++ ) {
            diff = p.xpoints[j]-e.getX();
            if( Math.abs(diff) < Math.abs(minXDist) ) {
              popXId = j;
              minXDist = diff;
            }
          }
          int insertBefore = popXId;

          if( minXDist < 0 )
            insertBefore = Math.min(insertBefore + 1, k-1);

          int save = permA[movingID];  
          if(movingID < insertBefore) {
            for( int j=movingID; j<insertBefore; j++ )
              permA[j] = permA[j+1];
            permA[insertBefore-1] = save;
          }
          else {
            for( int j=movingID; j>insertBefore; j-- )
              permA[j] = permA[j-1];
            permA[insertBefore] = save;
          }
          if( insertBefore < movingID || insertBefore > movingID +1 ) {   // Check whether the current order has been changed or not
            sortMode = "foo";
          }
          create(width, height);
          update(this.getGraphics());
        }                                    // Moving Axis handling end	  
        else {                               // Zooming into individual axes
          if( ((e.getModifiers() == CTRL_DOWN+16) && (System.getProperty("os.name").equals("Irix"))) ||
              ((e.getModifiers() == CTRL_DOWN+16) && (System.getProperty("os.name").equals("Linux"))) ||
              ((e.getModifiers() == 20) && (System.getProperty("os.name").equals("Mac OS X"))) ||
              (e.getModifiers() == META_DOWN) ) {
//System.out.println("Return: "+e.getModifiers()+"  Test: "+(BUTTON1_DOWN + CTRL_DOWN));
            if (e.getID() == MouseEvent.MOUSE_PRESSED ) {
              lastX = e.getX();
//System.out.println("lastX: "+lastX);
            }
            else {
              int popXId = 0;
              Polygon p = poly[0];
              if( Math.abs(e.getX() - lastX) > 4 ) {
                int testX = lastX + (int)(0.5+slotWidth)/2 * ( e.getX() - lastX ) / Math.abs( e.getX() - lastX );
                for( int j=0; j<k; j++ ) {
                  if( (p.xpoints[j] < testX) && (lastX < p.xpoints[j]) ||
                      (p.xpoints[j] > testX) && (lastX > p.xpoints[j]) ){
                    popXId = j;
//System.out.println("popXId: "+popXId+" e.getX: "+e.getX());
                  }
                }
                setCoordinates(0, Mins[permA[popXId]], 1, Maxs[permA[popXId]], -1);
                super.processMouseEvent(e);                           // This performs the zoom !!
                Mins[permA[popXId]] = getLly();
                Maxs[permA[popXId]] = getUry();
              }
              else {
                int minXDist = 5000;
                for( int j=0; j<k; j++ ) {
                  if( Math.abs(p.xpoints[j]-e.getX()) < Math.abs(minXDist) ) {
                    popXId = j;
                    minXDist =  p.xpoints[j]-e.getX();
                  }
                }
                Mins[permA[popXId]] = dMins[permA[popXId]];
                Maxs[permA[popXId]] = dMaxs[permA[popXId]];
              }
//System.out.println("Id: "+popXId);
//create(width, height);
              update(this.getGraphics());
            }
          }
          super.processMouseEvent(e);
        }
    }
      else                                     // if not pressed or released
        super.processMouseEvent(e);
  }
    
    public void processMouseMotionEvent(MouseEvent e) {
      if( moving ) {
        Graphics g = this.getGraphics();
        g.drawImage(ttbi, 0, 0, Color.black, null);
        g.fillRect(lastX-1, border, 3, height - 2 * border);
        movingName.moveXTo(lastX);
        movingName.draw(g, 2);
        lastX = e.getX();
        lastX = Math.max(lastX, border);
        lastX = Math.min(lastX, width-border);
      }
      else
        super.processMouseMotionEvent(e);  // Pass other event types on.
    }

    public void processKeyEvent(KeyEvent e) {

      boolean hit = false;

      if (     e.getModifiers() == Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()
               && ( e.getKeyCode() == KeyEvent.VK_0 || e.getKeyCode() == KeyEvent.VK_NUMPAD0)) {
        if( (e.getKeyCode() == KeyEvent.VK_0 || e.getKeyCode() == KeyEvent.VK_NUMPAD0  )
            && e.getModifiers() == Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() &&
            frame.getWidth() > Toolkit.getDefaultToolkit().getScreenSize().width) {
          frame.setSize((Toolkit.getDefaultToolkit().getScreenSize()).width, frame.getHeight());
          this.setSize((Toolkit.getDefaultToolkit().getScreenSize()).width, this.getHeight());
        }
      }
      if ((e.getID() == KeyEvent.KEY_PRESSED) &&  (e.getKeyCode() == Event.BACK_SPACE))  {
        if( k>2 && paintMode.equals("Poly") || k>1 && !paintMode.equals("Poly") ) {
          for( int j=0; j<k; j++ ) {
            if( selected[permA[j]] ) {
              hit = true;
              // First take care of eventually deleted selections
              for( int i=0; i<Selections.size(); i++) {
                Selection S = (Selection)Selections.elementAt(i);
                if( !paintMode.equals("XbyY") && vars[permA[j]] == ((floatRect)S.o).var ) {
                  Selections.removeElementAt(i);
                  S.status = Selection.KILLED;
//System.out.println("Deleted Selection for variable: "+((floatRect)S.o).var+" on Slot: "+j+" mapping to: "+permA[j]+" is: "+vars[permA[j]]+" it: "+j);
                  SelectionEvent se = new SelectionEvent(this);
                  EventQueue evtq = Toolkit.getDefaultToolkit().getSystemEventQueue();
                  evtq.postEvent(se);
                }
//                if( ((floatRect)S.o).var > vars[permA[j]] )
//                  ((floatRect)S.o).var -= 1;
              }
              // 
              for( int i=permA[j]; i<k-1; i++) {
                vars[i] = vars[i+1];
                selected[i] = selected[i+1];
                inverted[i] = inverted[i+1];
              }
              for( int i=0; i<k; i++)
                if( permA[i] > permA[j] )
                  permA[i] -= 1;
              for( int i=j; i<k-1; i++) {
                permA[i] = permA[i+1];
              }
              k -= 1;
              j -= 1;
            }
          }
          if( hit ) {
            if( zoomToSel ) {
              for(int i=0; i<data.n; i++)
                data.setSelection(i, onlyHi[i]?1:0, Selection.MODE_STANDARD);
              hotSelection = true;									// we need to set an initial hotSelector to rescale (is switched off in first paint)
            }
            this.dataChanged(0);
            return;
          }
        }
      }
      if (e.getModifiers() == Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() &&
          e.getKeyCode() == KeyEvent.VK_I && !paintMode.equals("XbyY")) {
        for( int j=0; j<k; j++ ) {
          if( selected[permA[j]] )
            inverted[permA[j]] = !inverted[permA[j]];
        }
        create(width, height);
        update(this.getGraphics());
      } else if(e.getID() == KeyEvent.KEY_PRESSED && ( e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_DOWN )) {
        if( e.getKeyCode() == KeyEvent.VK_UP )
          scaleFactor += 1;
        else
          if( scaleFactor >= 2 )
            scaleFactor -= 1;
        this.dataChanged(0);
      } else if(e.getID() == KeyEvent.KEY_PRESSED && ( e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_RIGHT )) {
        if( e.getKeyCode() == KeyEvent.VK_RIGHT ) {
          if( alpha >= 0.99 )
            return;
          else if( alpha >= 0.49 )
            alpha = 1.0F;
          else if( alpha >= 0.099 )
            alpha = 0.5F;
          else if( alpha >= 0.049 )
            alpha = 0.1F;
          else if( alpha >= 0.0099 )
            alpha = 0.05F;
          else if( alpha >= 0.0049 )
            alpha = 0.01F;
        }
        else {
          if( alpha >= 0.99 )
            alpha = 0.5F;
          else if( alpha >= 0.49 )
            alpha = 0.1F;
          else if( alpha >= 0.099 )
            alpha = 0.05F;
          else if( alpha >= 0.049 )
            alpha = 0.01F;
          else if( alpha >= 0.0099 )
            alpha = 0.005F;
          else if( alpha >= 0.0049 )
            return;
        }
        this.dataChanged(0);
      } else if ( e.getKeyCode() == KeyEvent.VK_PAGE_UP || e.getKeyCode() == KeyEvent.VK_PAGE_DOWN ) {
        int[] tPerm = new int[k];
        if ( e.getKeyCode() == KeyEvent.VK_PAGE_UP ) {
          tPerm[0] = permA[2];
          tPerm[1] = permA[0];
          for( int j=2; j<k-2; j++ ) { 
            if( (j % 2) == 0 )
              tPerm[j] = permA[j+2];
            else
              tPerm[j] = permA[j-2];
          }
          if( (k % 2) == 0 ) {
            tPerm[k-1] = permA[k-3];
            tPerm[k-2] = permA[k-1];
          } else {
            tPerm[k-1] = permA[k-2];
            tPerm[k-2] = permA[k-4];
          }
        } else {
          tPerm[0] = permA[1];
          tPerm[1] = permA[3];
          for( int j=2; j<k-2; j++ ) { 
            if( (j % 2) == 0 )
              tPerm[j] = permA[j-2];
            else
              tPerm[j] = permA[j+2];
          }
          if( (k % 2) == 0 ) {
            tPerm[k-1] = permA[k-2];
            tPerm[k-2] = permA[k-4];
          } else {
            tPerm[k-1] = permA[k-3];
            tPerm[k-2] = permA[k-1];
          }
        }
        for( int j=0; j<k; j++ )
          permA[j] = tPerm[j];
        this.dataChanged(0);
      } else if ( e.getModifiers() == Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() && e.getKeyCode() == KeyEvent.VK_R
                  && paintMode.equals("Poly") ) {
        actionPerformed(new ActionEvent(this, 325145, "Zeiser"));
      } else if ( e.getModifiers() == (Event.SHIFT_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()) && e.getKeyCode() == KeyEvent.VK_H ) {
        actionPerformed(new ActionEvent(this, 325141, "Hot"));
      } else if ( e.getModifiers() == Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() && e.getKeyCode() == KeyEvent.VK_NUMPAD0
                  || e.getModifiers() == Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() && e.getKeyCode() == KeyEvent.VK_0) {
        actionPerformed(new ActionEvent(this, 325143, "Home"));
      }      
      else
        super.processKeyEvent(e);
    }

    public void actionPerformed(ActionEvent e) {
      String command = e.getActionCommand();
System.out.println("Command: "+command);
      if( command.equals("Common") || command.equals("Individual") ) {
        if( command.equals("Common") ) 
          Scale = "Individual";
        else
          Scale = "Common";
        create(width, height);
        update(this.getGraphics());
      }
      else if( command.equals("Box") || command.equals("Poly") || command.equals("Both") ) {
        paintMode = command;
        if( !command.equals("Poly") )
          zoomToSel = false;
        create(width, height);
        update(this.getGraphics());
      }
      else if( command.equals("1.0") || command.equals("0.5") || command.equals("0.1") || command.equals("0.05") || command.equals("0.01") || command.equals("0.005") ) {
        alpha = (float)Util.atod(command);
//System.out.println("alpha: "+alpha);

        create(width, height);
        update(this.getGraphics());
      } else if( command.equals("min") || command.equals("quar") || command.equals("sdev") || command.equals("mean") || command.equals("median") || command.equals("max") || command.equals("ini") || command.equals("rev") ) {
        sortMode = command;
        if( command.equals("ini") )
          for( int i=0; i<sortA.length; i++ )
            permA[i] =i;
        else if( command.equals("rev") ) {
          int left  = 0;              // index of leftmost element
          int right = permA.length-1; // index of rightmost element
          while (left < right) {   // exchange the left and right elements
            int temp = permA[left];
            permA[left]  = permA[right];
            permA[right] = temp;
            left++;                // move the bounds toward the center
            right--;
          }
        } else {
          if( command.equals("min") )
            for( int i=0; i<sortA.length; i++ ) 
              sortA[i] = dMins[i];
          if( command.equals("quar") ) 
            for( int i=0; i<sortA.length; i++ ) 
              sortA[i] = dIQRs[i];
          if( command.equals("median") ) 
            for( int i=0; i<sortA.length; i++ ) 
              sortA[i] = dMedians[i];
          if( command.equals("mean") ) 
            for( int i=0; i<sortA.length; i++ ) 
              sortA[i] = dMeans[i];
          if( command.equals("sdev") ) 
            for( int i=0; i<sortA.length; i++ ) 
              sortA[i] = dSDevs[i];
          if( command.equals("max") )
            for( int i=0; i<sortA.length; i++ ) 
              sortA[i] = dMaxs[i];
          int[] perm = Qsort.qsort(sortA, 0, sortA.length-1);
          for( int i=0; i<sortA.length; i++ )
            permA[i] = perm[i];
        }
        create(width, height);
        update(this.getGraphics());
      } else if ( command.equals("center") || command.equals("cmean") || command.equals("cmedian") || command.equals("ccase") || command.equals("cvalue") ) {
        alignMode = command;
        if( alignMode.equals("cvalue") )
          centerAt = Util.atod(JOptionPane.showInputDialog(this, "Align values at:"));

        create(width, height);
        update(this.getGraphics());
      } else if ( command.equals("Hot") ) {
        hotSelection = !hotSelection;
        zoomToSel = false;
        for( int i=0; i<data.n; i++ )
          onlyHi[i] = true;
        this.dataChanged(0);
      } else if ( command.equals("Zeiser") ) {
        bg = null;
        double[] selection = data.getSelection();
        for(int i=0; i<data.n; i++)
          data.setSelection(i, 1, Selection.MODE_XOR);
        for(int i=0; i<data.n; i++)
          if( selection[i] > 0 )
            onlyHi[i] = onlyHi[i] && true;
          else
            onlyHi[i] = onlyHi[i] && false;
        for(int i=0; i<data.n; i++)
          data.setSelection(i, onlyHi[i]?1:0, Selection.MODE_STANDARD);
        hotSelection = true;									// we need to set an initial hotSelector to rescale (is switched off in first paint)
        zoomToSel = true;
        this.dataChanged(0);
      } else if ( command.equals("Home") ) {
        for( int i=0; i<data.n; i++ )
          onlyHi[i] = true;

        zoomToSel = false;
        hotSelection = false;
        alignMode = "center";
        if( k > 1 )
          slotWidth = (width-2.0*border)/(k-1.0);
        else
          slotWidth = 100;        
        this.dataChanged(0);
      }
      else
        super.actionPerformed(e);
    }
    
    public void paint(Graphics2D g2d) {

      Graphics2D g = (Graphics2D)g2d;

      int pF = 1;
      double[] selection;
      Dimension size = this.getSize();

      if( printing )
        slotMax = 1000000;
      else
        slotMax = 40;
      
      if( oldWidth != size.width || oldHeight != size.height || hotSelection || frame.getBackground() != MFrame.backgroundColor) {
        frame.setBackground(MFrame.backgroundColor);

        this.width = size.width;
        this.height = size.height;
        if( hotSelection && !zoomToSel ) 
          getData();
        if( zoomToSel && hotSelection ) {
          hotSelection = false;
          for(int i=0; i<data.n; i++)
            data.setSelection(i, 1, Selection.MODE_XOR);
        }
        create( width, height );
        oldWidth = size.width;
        oldHeight = size.height;
      }
      if( bg == null || printing ) {
        if( !printing )  {
          bi = createImage(size.width, size.height);	// double buffering from CORE JAVA p212
          tbi = createImage(size.width, size.height);
          bg = (Graphics2D)bi.getGraphics();
        }
        else 
          bg = g;

        if( alignMode.equals("cvalue") ) {          // draw reference axis if values are aligned at a certain value
          bg.setColor(Color.gray);
          bg.drawLine(border-6,
                      (int)(-border + height - (height-2*border) * ((centerAt - Mins[1])/(Maxs[1]-Mins[1]))),
                      width-border+6,
                      (int)(-border + height - (height-2*border) * ((centerAt - Mins[1])/(Maxs[1]-Mins[1]))));
          bg.setColor(Color.lightGray);
          bg.drawLine(border-5,
                      (int)(-border + height - (height-2*border) * ((centerAt - Mins[1])/(Maxs[1]-Mins[1])))+1,
                      width-border+5,
                      (int)(-border + height - (height-2*border) * ((centerAt - Mins[1])/(Maxs[1]-Mins[1])))+1);
          bg.setColor(Color.lightGray);
          bg.drawLine(border-5,
                      (int)(-border + height - (height-2*border) * ((centerAt - Mins[1])/(Maxs[1]-Mins[1])))-1,
                      width-border+5,
                      (int)(-border + height - (height-2*border) * ((centerAt - Mins[1])/(Maxs[1]-Mins[1])))-1);
        }
                        
        if( ( alignMode.equals("center") && Scale.equals("Individual") ) || paintMode.equals("XbyY") ) { // draw y-axis if the scaling options allow
          //
          FontMetrics fm = bg.getFontMetrics();
          roundY = (int)Math.max(0, 2 - Math.round((Math.log(Mins[1]-Mins[1])/Math.log(10))));
          // y-axis  
          bg.drawLine( 3 + border - outside*pF, height -  border, 
                       3 + border - outside*pF, border );  
          // y-ticks  
          bg.drawLine( 3 + border - outside*pF,           border, 
                       3 + border - outside*pF - tick*pF, border );  
          
          bg.drawLine( 3 + border - outside*pF,           height -  border, 
                       3 + border - outside*pF - tick*pF, height -  border );  
          
          bg.rotate(-Math.PI/2);
          bg.drawString(Stat.roundToString(Mins[1], roundY), 
                        -height + border, 
                        border + 3 - fm.getMaxAscent() - tick*pF +1*pF);
          bg.drawString(Stat.roundToString(Maxs[1], roundY), 
                        -border - fm.stringWidth(Stat.roundToString(Maxs[1], roundY) ),
                        border + 3 - fm.getMaxAscent() - tick*pF +1*pF);          
          bg.rotate(Math.PI/2);
        }

        bg.setColor(new Color(0, 0, 0, alpha));
        if( paintMode.equals("Poly") && !hotSelection && !zoomToSel) {
          for( int i=0; i<data.n; i++ )
            bg.drawPolyline(poly[i].xpoints, poly[i].ypoints, k); 
        }
        if( paintMode.equals("Poly") && zoomToSel ) {
          for( int i=0; i<data.n; i++ )
            if( onlyHi[i] )
              bg.drawPolyline(poly[i].xpoints, poly[i].ypoints, k); 
        }
        for( int j=0; j<k; j++ ) {	                    	                      	// Draw Axes
          if( !printing )
            bg.setColor(new Color(255, 255, 255, 75));
          else
            bg.setColor(new Color(0.5F, 0.5F, 0.5F, 0.35F));
          bg.drawLine( poly[1].xpoints[j]-1, border-2, (poly[1].xpoints)[j]-1, size.height-border-1);
          bg.drawLine( poly[1].xpoints[j]+1, border-2, (poly[1].xpoints)[j]+1, size.height-border-1);
          if( !printing )
            bg.setColor(new Color(255, 255, 255, 140));
          else
            bg.setColor(new Color(0.5F, 0.5F, 0.5F, 0.5F));
          bg.drawLine( poly[1].xpoints[j],   border-3, (poly[1].xpoints)[j],   size.height-border);
        }	
        for( int j=0; j<k; j++ ) {													// Arrows at Axes
          if( !inverted[permA[j]] || !paintMode.equals("Poly")) {
            if( !printing )
              bg.setColor(new Color(255, 255, 255, 140));
            else
              bg.setColor(new Color(0.5F, 0.5F, 0.5F, 0.5F));
            bg.drawLine( poly[1].xpoints[j]-3,   border,   (poly[1].xpoints)[j]+3,   border);
            bg.drawLine( poly[1].xpoints[j]-2,   border-1, (poly[1].xpoints)[j]+2,   border-1);
            bg.drawLine( poly[1].xpoints[j]-1,   border-2, (poly[1].xpoints)[j]+1,   border-2);
            bg.drawLine( poly[1].xpoints[j],     border-3, (poly[1].xpoints)[j],     border-3);
          } else {
            bg.setColor(Color.red);
            bg.drawLine( poly[1].xpoints[j]-3,   size.height-border-2, (poly[1].xpoints)[j]+3,   size.height-border-2);
            bg.drawLine( poly[1].xpoints[j]-2,   size.height-border-1, (poly[1].xpoints)[j]+2,   size.height-border-1);
            bg.drawLine( poly[1].xpoints[j]-1,   size.height-border,   (poly[1].xpoints)[j]+1,   size.height-border);
            bg.drawLine( poly[1].xpoints[j],     size.height-border+1, (poly[1].xpoints)[j],     size.height-border+1);
          }
        }
        bg.setColor(Color.black);
        if( paintMode.equals("Box") || paintMode.equals("Both") || paintMode.equals("XbyY")) {
          if( !hotSelection )
            for( int i=0; i<bPlots.size(); i++ )
              ((boxPlot)(bPlots.elementAt(i))).draw(bg);
          for( int i=0; i<rects.size(); i++ )
            ((MyRect)(rects.elementAt(i))).draw(bg);
        }
      }

      long start = new Date().getTime();
      Graphics tbg, ttbg;
      if( !printing )
        tbg = (Graphics2D)tbi.getGraphics();
      else
        tbg = g;

      if( !printing  )    
        tbg.drawImage(bi, 0, 0, null);
      //tbg.setColor(Color.red);
      tbg.setColor(DragBox.hiliteColor);
      if( data.countSelection()>0 ) {
        selection = data.getSelection();
        if( paintMode.equals("Poly") || paintMode.equals("Both")) {
          if( ((MFrame)frame).getAlphaHi() )
            ((Graphics2D)tbg).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)alpha));
          else
            ((Graphics2D)tbg).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0F));
          for( int i=0; i<data.n; i++ ) {
            if( selection[i] > 0 && onlyHi[i] )
              tbg.drawPolyline(poly[i].xpoints, poly[i].ypoints, k);
          }
        }
        if( paintMode.equals("Box") || paintMode.equals("Both") || paintMode.equals("XbyY") ) {
          for( int i=0; i<bPlots.size(); i++ ) {
            if( paintMode.equals("XbyY") )
              data.setFilter( yVar, lNames[i]);
            ((boxPlot)(bPlots.elementAt(i))).drawHighlight(tbg);
            data.resetFilter();
          }

          // for build in barcharts
          //
          int tabcount = 0;
          if( tabs.size() > 0 ) {
            Table tablep = (Table)(tabs.firstElement());
            for( int i = 0;i < rects.size(); i++) {
              MyRect r = (MyRect)rects.elementAt(i);
              double sum=0, sumh=0;
              int id = ((Integer)(r.tileIds.elementAt(0))).intValue();
              if( id == 0 && i != 0 )
                tablep = (Table)(tabs.elementAt(++tabcount));
              sumh = tablep.getSelected(id)*tablep.table[id];
              sum  = tablep.table[id];
              r.setHilite( sumh/sum );
              r.draw(tbg);
            }
          }
        }
      }
      else if( hotSelection ) {
        MyText warning = new MyText("No Data Selected!", size.width/2, size.height/2, 2);
        warning.draw(tbg);
      }

      // Plot the labels ...
      if( !printing )  {
        ttbi = createImage(size.width, size.height);
        ttbg = ttbi.getGraphics();
        ttbg.drawImage(tbi, 0, 0, Color.black, null);
      }
      else
        ttbg = g;

      for( int j=0; j<k; j++ ) {
        int x = border+addBorder+(int)(0.5+(j*slotWidth));
        MyText mt = (MyText)names.elementAt(j);
        if( k == 1 )
          mt.setAlign( 2 );
        else {
          if( j == 0 && !paintMode.equals("XbyY") )
            mt.setAlign( 0 );
          else if( j== k-1 )
            mt.setAlign( 1 );
          else
            mt.setAlign( 2 );
        }
        if( !printing ) {
          Font SF = new Font("SansSerif", Font.PLAIN, 11);
          ttbg.setFont(SF);
        }
        if( selected[permA[j]] && !printing ) {
          Font SF = new Font("SansSerif", Font.BOLD, 12);
          ttbg.setFont(SF);
          ttbg.setColor(DragBox.hiliteColor);
        } else
          ttbg.setColor(Color.black);
        // Set Y position of Text AFTER we set the font size
        if( (j % 2) == 1 )
          mt.moveYTo(border - 6);
        else
          mt.moveYTo(height - border + 3 + (ttbg.getFont()).getSize());
        mt.moveXTo(x);

        mt.draw(ttbg);
      }
      
      if( !(printing) ) {
        if( hotSelection || zoomToSel ) {
          ttbg.setColor(Color.red);
          ttbg.drawRect(0, 0, size.width-1, size.height-1);
          ((Graphics2D)ttbg).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6F));
          ttbg.drawRect(1, 1, size.width-3, size.height-3);
          ((Graphics2D)ttbg).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25F));
          ttbg.drawRect(2, 2, size.width-5, size.height-5);
          ((Graphics2D)ttbg).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0F));
        }

        ttbg.setColor(Color.black);
        drawSelections(ttbg);
        g.drawImage(ttbi, 0, 0, Color.black, null);
        tbg.dispose();
        ttbg.dispose();
      }

      long stop = new Date().getTime();
      //System.out.println("Time for polys: "+(stop-start)+"ms");
    }

    public void drawSelections(Graphics g) {

      int plotID = 0;
      int slotID = 0;
      
      for( int i=0; i<Selections.size(); i++) {
        Selection S = (Selection)Selections.elementAt(i);
        for( int j=0; j<k; j++)
          if( (  paintMode.equals("XbyY") && j == ((floatRect)S.o).var ) ||
              ( !paintMode.equals("XbyY") && vars[permA[j]] == ((floatRect)S.o).var ) ) {
            plotID = permA[j];
            slotID = j;
          }

        S.r.width  = (int)(((floatRect)S.o).w * Math.round(slotWidth));
        S.r.x      = (int)(border + addBorder + slotWidth * slotID - (1.0-((floatRect)S.o).x) * (double)slotWidth);
        S.r.height = (int)((height-2*border) * ((floatRect)S.o).h / (Maxs[plotID]-Mins[plotID]));
        if( inverted[plotID] && paintMode.equals("Poly") )
          S.r.y      = (int)(-border + height - (height-2*border) * ((((floatRect)S.o).y - Maxs[plotID])/(Mins[plotID]-Maxs[plotID]))) - S.r.height;
        else
          S.r.y      = (int)(-border + height - (height-2*border) * ((((floatRect)S.o).y - Mins[plotID])/(Maxs[plotID]-Mins[plotID])));

        //System.out.println("S.r.x: "+S.r.x+" S.r.y: "+S.r.y+" S.r.width: "+S.r.width+" S.r.height: "+S.r.height);

        drawBoldDragBox(g, S);
      }
    }

    public void maintainSelection(Selection S) {

      Graphics g = this.getGraphics();

      Rectangle sr = S.r;
      int mode = S.mode;

      int selectCol=0;
      int checkCol=0;

      Polygon p = poly[0];
      for( int j=0; j<p.npoints-1; j++ ) {
        if( p.xpoints[j] <= sr.x && p.xpoints[j+1] > sr.x )
          checkCol = j+1;
      }
      //		System.out.println(" **** select col: "+selectCol);
      int passID = vars[permA[checkCol]];
      if( paintMode.equals("XbyY") )
        passID = checkCol;

      selectCol = permA[checkCol];
      double denom=0;
      if( k == 1 )
        denom = slotWidth;
      else
        denom = p.xpoints[1] - p.xpoints[0];

      if( inverted[selectCol] && paintMode.equals("Poly") )
        S.o = new floatRect((double)((S.r.x - border - addBorder) % slotWidth)/slotWidth,
                            ((double)(-border+S.r.y+S.r.height))/(height-2*border)*(Maxs[selectCol]-Mins[selectCol])+Mins[selectCol],
                            (double)(S.r.width)/denom,
                            (double)(S.r.height)/(height-2*border)*(Maxs[selectCol]-Mins[selectCol]),
                            passID);
      else
        S.o = new floatRect((double)((S.r.x - border - addBorder) % slotWidth)/slotWidth,
                            ((double)(height-border-S.r.y))/(height-2*border)*(Maxs[selectCol]-Mins[selectCol])+Mins[selectCol],
                            (double)(S.r.width)/denom,
                            (double)(S.r.height)/(height-2*border)*(Maxs[selectCol]-Mins[selectCol]),
                            passID);

      /*      else
        S.o = new floatRect((double)((S.r.x - border) % slotWidth)/slotWidth,
                            ((double)(height-border-S.r.y))/(height-40)*(Maxs[selectCol]-Mins[selectCol])+Mins[selectCol],
                            (double)(S.r.width)/(p.xpoints[1] - p.xpoints[0]),
                            (double)(S.r.height)/(height-2*border)*(Maxs[selectCol]-Mins[selectCol]),
                            passID);*/

      if( ((floatRect)S.o).x < 0 )
        ((floatRect)S.o).x = 1-Math.abs(((floatRect)S.o).x);

      int tabcount = 0;
      int thistable = 0;
      boolean intersect = false;
      for( int i = 0;i < rects.size(); i++) {
        MyRect r = (MyRect)rects.elementAt(i);
        int id = ((Integer)(r.tileIds.elementAt(0))).intValue();
        if( id == 0 && i != 0 )
          tabcount++;
        if ( r.intersects( sr )) {
          thistable = tabcount;
          intersect = true;
        }
      }

      if( intersect && !paintMode.equals("Poly") ) {
        Table tablep=(Table)(tabs.firstElement());
        tabcount = 0;
        for( int i = 0;i < rects.size(); i++) {
          MyRect r = (MyRect)rects.elementAt(i);
          double sum=0, sumh=0;
          int id = ((Integer)(r.tileIds.elementAt(0))).intValue();
          if( id == 0 && i != 0 )
            tablep = (Table)(tabs.elementAt(++tabcount));
          if( tabcount == thistable ) {
            if ( r.intersects( sr ))
              tablep.setSelection(id,1,mode);
            else
              tablep.setSelection(id,0,mode);
            sumh = tablep.getSelected(id)*tablep.table[id];
            sum  = tablep.table[id];
            r.setHilite( sumh/sum );	
          }
        }
      }
      else {
        if( paintMode.equals("XbyY") ) {
          for( int i=0; i<data.n; i++ ) {
            p = poly[i];
            if( !sr.contains(p.xpoints[selectCol], p.ypoints[selectCol] ) )
              data.setSelection(i,0,mode);
          }
          data.setFilter( yVar, lNames[selectCol]);
        }
        for( int i=0; i<data.n; i++ ) {
          p = poly[i];
          if( sr.contains(p.xpoints[checkCol], p.ypoints[checkCol] ) ) {
            data.setSelection(i,1,mode);
          }
          else
            data.setSelection(i,0,mode); 
        }
        data.resetFilter();
      }		
    }

    void getData() {

      if( paintMode.equals("XbyY") ) {
        k = data.getNumLevels(yVar);
        vars = new int[k];
        for( int j=0; j<k; j++ ) {
          vars[j] = xVar;
        }
      }
      
      sortA = new double[k];

      if( hotSelection == zoomToSel ) {
        dMins = new double[k];
        dIQRs = new double[k];
        dMedians = new double[k];
        dMeans = new double[k];
        dSDevs = new double[k];
        dMaxs = new double[k];
        Mins = new double[k];
        Maxs = new double[k];

        dataCopy = new double[k][data.n];

        if( !hotSelection ) {   // we need an XOR here ;-)
          System.out.println(" *** RESET *** ");
          for( int j=0; j<k; j++ ) {
            dMins[j] = data.getMin(vars[j]);
            dIQRs[j] = data.getQuantile(vars[j], 0.75) - data.getQuantile(vars[j], 0.25);
            dMedians[j] = data.getQuantile(vars[j], 0.5);
            dMeans[j] = data.getMean(vars[j]);
            dSDevs[j] = data.getSDev(vars[j]);
            dMaxs[j] = data.getMax(vars[j]);
          }
        }
        else if( data.countSelection() > 0 ) {
          for( int j=0; j<k; j++ ) {
            dMins[j] = data.getSelQuantile(vars[j], 0);
            dIQRs[j] = data.getSelQuantile(vars[j], 0.75) - data.getSelQuantile(vars[j], 0.25);
            dMedians[j] = data.getSelQuantile(vars[j], 0.5);
            dMeans[j] = data.getSelMean(vars[j]);
            dSDevs[j] = data.getSelSDev(vars[j]);
            dMaxs[j] = data.getSelQuantile(vars[j], 1);
          }
        }
      }
      for( int j=0; j<k; j++ ) {
        if( data.categorical(vars[j]) && !data.alpha(vars[j]) )
          dataCopy[j] = data.getRawNumbers(vars[j]);
        else
          dataCopy[j] = data.getNumbers(vars[j]);
      }
    }	

    void create(int width, int height) {

//      System.out.println("- - - - - - - - - - - ");
//      for(int j=0; j<k; j++)
//        System.out.print(" "+dSDevs[j]);

      if( bg != null ) {
        bg.dispose();
        bg = null;
      }

      if( k == 1 )
        addBorder = 30;
      
      if( ( alignMode.equals("center") && Scale.equals("Individual") ) )
        addBorder = 3;
      if( paintMode.equals("XbyY") )
        if( k== 2 )
          addBorder = 25;
        else
          addBorder = 17;
      
      if( alignMode.equals("ccase") )
        if( data.countSelection() == 1 )
          for( int i=0; i<data.n; i++ )
            if( data.getSelected(i) > 0 )
              selID = i;

      for( int j=0; j<k; j++ ) {
        if( alignMode.equals("center") ) {
          Mins[j] = dMins[j];// - (dMaxs[j]-dMins[j])/height*4;
          Maxs[j] = dMaxs[j];// + (dMaxs[j]-dMins[j])/height*4;
        } else if( alignMode.equals("cmean") ) {
          Mins[j] = dMeans[j] - scaleFactor * dSDevs[j];
          Maxs[j] = dMeans[j] + scaleFactor * dSDevs[j];
        } else if( alignMode.equals("cmedian") ) {
          Mins[j] = dMedians[j] - scaleFactor * ((dIQRs[j] != 0) ? dIQRs[j] : (dMaxs[j]-dMins[j])/2);
          Maxs[j] = dMedians[j] + scaleFactor * ((dIQRs[j] != 0) ? dIQRs[j] : (dMaxs[j]-dMins[j])/2);
        } else if( alignMode.equals("ccase") ) {
          Mins[j] = dataCopy[j][selID] - scaleFactor * dSDevs[j];
          Maxs[j] = dataCopy[j][selID] + scaleFactor * dSDevs[j];
        } else if( alignMode.equals("cvalue") ) {
          Mins[j] = centerAt - scaleFactor * dSDevs[j];
          Maxs[j] = centerAt + scaleFactor * dSDevs[j];
        }
      }
      if( Scale.equals("Individual") )
        scaleCommon();

      if( k > 1 )
        if( paintMode.equals("XbyY") )
          if( k==2 )
            slotWidth = 70;
          else
            slotWidth = (width-2.75*border)/(k-1.0);
        else
          slotWidth = (width-2.0*border)/(k-1.0);
      else
        slotWidth = 100;

// System.out.println("Slot: "+slotWidth+"Slot Max: "+slotMax);

      names.removeAllElements();
      if( paintMode.equals("XbyY") )
        lNames = data.getLevels(yVar);

      for( int j=0; j<k; j++ ) {
        if( !paintMode.equals("XbyY") )
          names.addElement(new MyText(data.getName(vars[permA[j]]), 1, 1, 0));
        else {
          names.addElement(new MyText(lNames[j], 1, 1, 0));
        }
      }

      poly = new Polygon[data.n];
      for( int i=0; i<data.n; i++ ) {
        poly[i] = new Polygon();
        for( int j=0; j<k; j++ ) {	
          int x = border+addBorder+(int)(0.5+slotWidth*j);
          int y;
          if( !inverted[permA[j]] || !paintMode.equals("Poly") )
            y = (int)(-border + height - (height-2*border) * ((dataCopy[permA[j]][i] - Mins[permA[j]])/(Maxs[permA[j]]-Mins[permA[j]])));
          else
            y = (int)(-border + height - (height-2*border) * ((dataCopy[permA[j]][i] - Maxs[permA[j]])/(Mins[permA[j]]-Maxs[permA[j]])));
          poly[i].addPoint(x, y);
        }
      }
      if( paintMode.equals("Box") || paintMode.equals("Both") || paintMode.equals("XbyY") ) {
        bPlots.removeAllElements();
        tabs.removeAllElements();
        rects.removeAllElements();
        for( int j=0; j<k; j++ ) {
          //        System.out.println("Name: "+lNames[j]);
          int x = border+(int)(0.5+slotWidth*j);
          if( !data.categorical(vars[permA[j]]) ) {
            if( paintMode.equals("XbyY") )
              data.setFilter( yVar, lNames[j]);
            bPlots.addElement(new boxPlot( j, vars[permA[j]] , x + addBorder, (int)(0.5+Math.min(slotWidth/2, slotMax)), border, height-border));
            data.resetFilter();
          }
          else {
            int[] dummy = new int[1];
            dummy[0] = vars[permA[j]];
            Table breakdown = data.breakDown("BlaBla", dummy, -1);
            tabs.addElement(breakdown);
            int lev = breakdown.levels[0];
            int sum = 0;

            Vector[] tileIds = new Vector[lev];
            for(int i=0; i<lev; i++ ) {
              sum += breakdown.table[i];
              tileIds[i] = new Vector(1,0);
              tileIds[i].addElement(new Integer(i));
            }
            int y = (int)(-border + height);
            for( int i=0; i<lev; i++ ) {
              int w = (int)(0.5+slotWidth/2);
              int h = (int)Math.round((height-2*border) * breakdown.table[i]/data.n);
              if( i<lev-1 )
                y -= h;
              else {
                h = y - border;
                y = border;
              }
              rects.addElement(new MyRect( true, 'x', "Observed", 
                                           x-(int)(0.5+slotWidth/4), y, w, h, 
                                           breakdown.table[i], breakdown.table[i], 1, 0, 
                                           breakdown.lnames[0][i]+'\n', tileIds[i]));	  
            }
          }
        }
      }

      /*
        if( paintMode.equals("XbyY") ) {
          bPlots.removeAllElements();
          for( int j=0; j<k; j++ ) {
            int x = border+slotWidth*j;
            bPlots.addElement(new boxPlot( j, xVar, yVar, ((dataSet.Variable)data.data.elementAt(yVar)).isLevel(lNames[j]) ,
                                           x, slotWidth/2, border, height-border));
          }
        }
       */
      setDragBoxConstraints(0, 0, width, height, (int)(0.5+slotWidth)-2, height);
    }

    void scaleCommon() {
      if( alignMode.equals("center") || paintMode.equals("XbyY") ) {
        double totMin = 1000000, totMax=-10000000;
        for( int j=0; j<k; j++ ) {
          if( selected[permA[j]] || !anySelected() ) {
            if( totMin >= Mins[j] )
              totMin = Mins[j];
            if( totMax <= Maxs[j] )
              totMax = Maxs[j];
          }
        }
        for( int j=0; j<k; j++ ) {
          if( selected[permA[j]] || !anySelected() ) {
            Mins[j] = totMin;
            Maxs[j] = totMax;
          }
        }        
      } else {
        double maxRange = 0, range;
        for( int j=0; j<k; j++ )
          if( (range = dMaxs[j] - dMins[j]) > maxRange )
            maxRange = range;
        for( int j=0; j<k; j++ ) {
          if( alignMode.equals("cmean") ) {
            Mins[j] = dMeans[j] - maxRange / 6 * scaleFactor;
            Maxs[j] = dMeans[j] + maxRange / 6 * scaleFactor;
          } else if( alignMode.equals("cmedian") ) {
            Mins[j] = dMedians[j] - maxRange / 6 * scaleFactor;
            Maxs[j] = dMedians[j] + maxRange / 6 * scaleFactor;
          } else if( alignMode.equals("ccase") ) {
            Mins[j] = dataCopy[j][selID] - maxRange / 6 * scaleFactor;
            Maxs[j] = dataCopy[j][selID] + maxRange / 6 * scaleFactor;
          } else if( alignMode.equals("cvalue") ) {
            Mins[j] = centerAt - maxRange / 6 * scaleFactor;
            Maxs[j] = centerAt + maxRange / 6 * scaleFactor;
          }
        }
      }
    }
    
    public void updateSelection() {
      paint(this.getGraphics());
    }

    public void dataChanged(int var) {
      getData();
      create(width, height);
      update(this.getGraphics());
    }
    
    public boolean anySelected() {
      int sel = 0;
      for( int j=0; j<k; j++ )
        if( selected[j] )
          sel++;
      return (sel > 1);
    }

    // dummy for scrolling  
    public void adjustmentValueChanged(AdjustmentEvent e) {
    }
    public void scrollTo(int id) {
    }

    class floatRect {

      double x, y, w, h;
      int var;

      public floatRect(double x, double y, double w, double h, int var) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.var = var;
      }
    }



    class boxPlot {

      double min, lHinge, median, uHinge, max;
      double sMin, lSHinge, sMedian, uSHinge, sMax;
      double lWhisker, uWhisker;
      double lSWhisker, uSWhisker;
      double[] lOutlier, uOutlier;
      double[] lsOutlier, usOutlier;
      int var, id;
      int mid, width, low, high;

      public boxPlot( int id, int var, int mid, int width, int low, int high) {
        this.id = permA[id];
        this.var = var;
        this.mid = mid;
        this.width = width;
        this.low = low;
        this.high = high;
        init();
      }

      public void init() {
        min    = data.getQuantile(var, 0);
        lHinge = data.getQuantile(var, 0.25);
        median = data.getQuantile(var, 0.5);
        uHinge = data.getQuantile(var, 0.75);
        max    = data.getQuantile(var, 1);
        lWhisker = data.getFirstGreater(var, lHinge-(uHinge-lHinge)*1.5);
        uWhisker = data.getFirstSmaller(var, uHinge+(uHinge-lHinge)*1.5);
        lOutlier = data.getAllSmaller(var, lWhisker);
        uOutlier = data.getAllGreater(var, uWhisker);
      }

      void draw( Graphics g ) {

        if( (data.filterVar!=-1 && data.filterGrpSize[data.filterGrp] > 3) || (data.filterVar==-1 && data.n > 3) ) {

          int  lWP  = low+(int)((Maxs[id]-lWhisker)/(Maxs[id]-Mins[id])*(high-low));
          int  lHP  = low+(int)((Maxs[id]-lHinge)/(Maxs[id]-Mins[id])*(high-low));
          int  medP = low+(int)((Maxs[id]-median)/(Maxs[id]-Mins[id])*(high-low));
          int  uHP  = low+(int)((Maxs[id]-uHinge)/(Maxs[id]-Mins[id])*(high-low));
          int  uWP  = low+(int)((Maxs[id]-uWhisker)/(Maxs[id]-Mins[id])*(high-low));

          /*System.out.println("        Min:"+min);
          System.out.println("Lower Whisk:"+lWhisker);
          System.out.println("Lower Hinge:"+lHinge);
          System.out.println("     Median:"+median);
          System.out.println("Upper Hinge:"+uHinge);
          System.out.println("Upper Whisk:"+uWhisker);
          System.out.println("        Max:"+max);

          System.out.println(" Number of uppers:"+ uOutlier.length);
          System.out.println(" Number of lowers:"+ lOutlier.length);
          System.out.println("============");*/

          // Base Boxes
          g.setColor(Color.lightGray);
          g.fillRect(mid-width/2, uWP, width, uHP-uWP);
          g.setColor(Color.black);
          g.drawRect(mid-width/2, uWP, width, uHP-uWP);
          g.setColor(Color.gray);
          g.fillRect(mid-width/2, uHP, width, medP-uHP);
          g.setColor(Color.black);
          g.drawRect(mid-width/2, uHP, width, medP-uHP);
          g.setColor(Color.gray);
          g.fillRect(mid-width/2, medP, width, lHP-medP);
          g.setColor(Color.black);
          g.drawRect(mid-width/2, medP, width, lHP-medP);
          g.setColor(Color.lightGray);
          g.fillRect(mid-width/2, lHP, width, lWP-lHP);
          g.setColor(Color.black);
          g.drawRect(mid-width/2, lHP, width, lWP-lHP);

          int dia = 3;
          if( printing )
            dia *= printFactor;
            
          for( int i=0; i<lOutlier.length; i++ ) {
            if( lOutlier[i] < lHinge-(uHinge-lHinge)*3 )
              g.fillOval(mid-dia/2, low+(int)((Maxs[id]-lOutlier[i])/(Maxs[id]-Mins[id])*(high-low))-dia/2, dia, dia);
            g.drawOval(mid-dia/2, low+(int)((Maxs[id]-lOutlier[i])/(Maxs[id]-Mins[id])*(high-low))-dia/2, dia, dia);
          }
          for( int i=0; i<uOutlier.length; i++ ) {
            if( uOutlier[i] > uHinge+(uHinge-lHinge)*3 )
              g.fillOval(mid-dia/2, low+(int)((Maxs[id]-uOutlier[i])/(Maxs[id]-Mins[id])*(high-low))-dia/2, dia, dia);
            g.drawOval(mid-dia/2, low+(int)((Maxs[id]-uOutlier[i])/(Maxs[id]-Mins[id])*(high-low))-dia/2, dia, dia);
          }
        } else {
          int  MinP = low+(int)((Maxs[id]-min)/(Maxs[id]-Mins[id])*(high-low));
          int  MedP = low+(int)((Maxs[id]-median)/(Maxs[id]-Mins[id])*(high-low));
          int  MaxP = low+(int)((Maxs[id]-max)/(Maxs[id]-Mins[id])*(high-low));
          g.drawRect(mid-width/2+4, MinP, width-8, 1);
          g.drawRect(mid-width/2+4, MedP, width-8, 1);
          g.drawRect(mid-width/2+4, MaxP, width-8, 1);
        }
      }

      void drawHighlight( Graphics g ) {

        if( data.filterVar != -1 ) {
          //        System.out.println("***** Group Size: "+data.filterSelGrpSize[data.filterGrp]);
          if( data.filterSelGrpSize[data.filterGrp] == 0) {
            //          System.out.println("Skipping: "+data.filterGrp);
            return;
          }
        }

        int count = data.countSelection();

        sMin    = data.getSelQuantile(var, 0);
        lSHinge = data.getSelQuantile(var, 0.25);
        sMedian = data.getSelQuantile(var, 0.5);
        uSHinge = data.getSelQuantile(var, 0.75);
        sMax    = data.getSelQuantile(var, 1);

        if( count > 3 ) {
          lsOutlier = data.getAllSelSmaller(var, lSHinge-(uSHinge-lSHinge)*1.5);
          usOutlier = data.getAllSelGreater(var, uSHinge+(uSHinge-lSHinge)*1.5);
          /*        if( lOutlier.length == 0 )
            lSWhisker = sMin;
          else
            lSWhisker = lOutlier[lOutlier.length-1];
          if( uOutlier.length == 0 )
            uSWhisker = sMax;
          else
            uSWhisker = uOutlier[uOutlier.length-1]; */

          lSWhisker = data.getFirstSelGreater(var, lSHinge-(uSHinge-lSHinge)*1.5);
          uSWhisker = data.getFirstSelSmaller(var, uSHinge+(uSHinge-lSHinge)*1.5);

          int  lSWP  = low+(int)((Maxs[id]-lSWhisker)/(Maxs[id]-Mins[id])*(high-low));
          int  lSHP  = low+(int)((Maxs[id]-lSHinge)/(Maxs[id]-Mins[id])*(high-low));
          int  sMedP = low+(int)((Maxs[id]-sMedian)/(Maxs[id]-Mins[id])*(high-low));
          int  uSHP  = low+(int)((Maxs[id]-uSHinge)/(Maxs[id]-Mins[id])*(high-low));
          int  uSWP  = low+(int)((Maxs[id]-uSWhisker)/(Maxs[id]-Mins[id])*(high-low));
          // Highlight Boxes
          int smaller = width/8;
          g.setColor(getHiliteColor());
          g.drawRect(mid-width/2+smaller, uSWP, width-2*smaller, 1);
          g.drawLine(mid, uSWP, mid, uSHP);
          g.fillRect(mid-width/2+smaller, uSHP, width-2*smaller, sMedP-uSHP);
          g.setColor(Color.black);
          g.drawRect(mid-width/2+smaller, uSHP, width-2*smaller, sMedP-uSHP);
          g.setColor(getHiliteColor());
          g.fillRect(mid-width/2+smaller, sMedP, width-2*smaller, lSHP-sMedP);
          g.setColor(Color.black);
          g.drawRect(mid-width/2+smaller, sMedP, width-2*smaller, lSHP-sMedP);
          g.setColor(getHiliteColor());
          g.drawRect(mid-width/2+smaller, lSWP, width-2*smaller, 1);
          g.drawLine(mid, lSWP, mid, lSHP);

          int dia = 3;
          if( printing )
            dia *= printFactor;
          for( int i=0; i<lsOutlier.length; i++ ) {
//            g.setColor(getBackground());
//            g.fillOval(mid-2, low+(int)((Maxs[id]-lOutlier[i])/(Maxs[id]-Mins[id])*(high-low))-2, 5, 5);
//            g.drawOval(mid-2, low+(int)((Maxs[id]-lOutlier[i])/(Maxs[id]-Mins[id])*(high-low))-2, 5, 5);
            g.setColor(getHiliteColor());
            if( lsOutlier[i] < lSHinge-(uSHinge-lSHinge)*3 )
              g.fillOval(mid-dia/2, low+(int)((Maxs[id]-lsOutlier[i])/(Maxs[id]-Mins[id])*(high-low))-dia/2, dia, dia);
            g.drawOval(mid-dia/2, low+(int)((Maxs[id]-lsOutlier[i])/(Maxs[id]-Mins[id])*(high-low))-dia/2, dia, dia);
          }
          for( int i=0; i<usOutlier.length; i++ ) {
//            g.setColor(getBackground());
//            g.fillOval(mid-2, low+(int)((Maxs[id]-uOutlier[i])/(Maxs[id]-Mins[id])*(high-low))-2, 5, 5);
//            g.drawOval(mid-2, low+(int)((Maxs[id]-uOutlier[i])/(Maxs[id]-Mins[id])*(high-low))-2, 5, 5);
            g.setColor(getHiliteColor());
            if( usOutlier[i] > uSHinge+(uSHinge-lSHinge)*3 )
              g.fillOval(mid-dia/2, low+(int)((Maxs[id]-usOutlier[i])/(Maxs[id]-Mins[id])*(high-low))-dia/2, dia, dia);
            g.drawOval(mid-dia/2, low+(int)((Maxs[id]-usOutlier[i])/(Maxs[id]-Mins[id])*(high-low))-dia/2, dia, dia);
          } 
        }
        else {
          sMin    = data.getSelQuantile(var, 0);
          sMedian = data.getSelQuantile(var, 0.5);
          sMax    = data.getSelQuantile(var, 1);

          int  sMinP = low+(int)((Maxs[id]-sMin)/(Maxs[id]-Mins[id])*(high-low));
          int  sMedP = low+(int)((Maxs[id]-sMedian)/(Maxs[id]-Mins[id])*(high-low));
          int  sMaxP = low+(int)((Maxs[id]-sMax)/(Maxs[id]-Mins[id])*(high-low));
          g.setColor(getHiliteColor());
          g.drawRect(mid-width/2+4, sMinP, width-8, 1);
          g.drawRect(mid-width/2+4, sMedP, width-8, 1);
          g.drawRect(mid-width/2+4, sMaxP, width-8, 1);
        }
      }
    }
}




