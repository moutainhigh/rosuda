import java.awt.*;               // ScrollPane, PopupMenu, MenuShortcut, etc.
import java.awt.image.*;         
import java.awt.datatransfer.*;  // Clipboard, Transferable, DataFlavor, etc.
import java.awt.event.*;         // New event model.
import java.io.*;                // Object serialization streams.
import java.util.*;              // For StingTokenizer.
import java.util.zip.*;          // Data compression/decompression streams.
import java.util.Vector;         // To store the scribble in.
import java.util.Properties;     // To store printing preferences in.
import java.lang.*;              // 
import javax.swing.*;
import javax.swing.event.*;

public class Histogram extends DragBox implements ActionListener {
  private Vector rects = new Vector(256,0);            	// Store the tiles.
  private Vector labels = new Vector(256,0);           	// Store the labels.
  private int width, height, realHeight, startX;       	// The preferred size.
  protected int oldWidth, oldHeight;                   	// The last size for constructing the bars.
  private double xMin , xMax, yMin, yMax, range;       	// Scaling
  private int outside = 5;
  private int tick    = 5;
  private double bStart, bWidth;			// Anker and Width of the Bins
  private Table tablep;                                	// The datatable to deal with.
  private Image bi;
  private Graphics bg;
  private int k;
  public String displayMode = "Histogram";
  private dataSet data;
  private int dvar;
  private int round;					// percision for labels ...

  public Histogram(JFrame frame, int width, int height, Table tablep, double bStart, double bWidth) {
    super(frame);
    this.tablep = tablep;
    this.name = tablep.name;
    this.levels = tablep.levels;
    this.names = tablep.names;
    this.lnames = tablep.lnames;
    this.bStart = bStart;
    this.bWidth= bWidth;
    this.width = width;
    this.height = height;

    frame.getContentPane().add(this);

    border = 20;
    yShift = -10;

    data = tablep.data;
    dvar = tablep.initialVars[0];
    this.k = levels[0];
    round = (int)Math.max(0, 3 - Math.round((Math.log(data.getMax(dvar)-data.getMin(dvar))/Math.log(10))));

    Font SF = new Font("SansSerif", Font.BOLD, 12);
    frame.setFont(SF);

    String titletext;
    if( tablep.count == -1 )
      titletext = "Histogram("+names[0]+")";
    else    
      titletext = "Histogram("+names[0]+"|"+tablep.data.getName(tablep.count)+")";

    frame.setTitle(titletext);

    //this.setBackground(new Color(255, 255, 152));

    xMin=tablep.data.getMin(tablep.initialVars[0]); 
    xMax=tablep.data.getMax(tablep.initialVars[0]);
    range = xMax - xMin;
    yMin=0; 
    yMax=1/range*4.25; 

    if( rects.size() == 0 ) {
      setCoordinates(xMin-range*0.05, yMin, xMax+range*0.05, yMax, -1);
//      System.out.println("Vor:  xMin: "+(xMin-range*0.05)+" yMin: "+ yMin +" xMax: "+(xMax+range*0.05)+" yMax: "+ yMax +" "+ -1);
      home();
//      System.out.println("Nach: xMin: "+this.getLlx()+" yMin: "+ yMin +" xMax: "+this.getUrx()+" yMax: "+ yMax +" "+ -1);
//      setCoordinates(this.getLlx(), yMin, this.getUrx(), yMax, -1);
    }

    // We use low-level events, so we must specify
    // which events we are interested in.
    this.enableEvents(AWTEvent.WINDOW_EVENT_MASK);
    this.enableEvents(AWTEvent.KEY_EVENT_MASK);
    this.enableEvents(AWTEvent.MOUSE_EVENT_MASK);
    this.requestFocus();
  }

  public void maintainSelection(Selection S) {

    Rectangle sr = S.r;
    int mode = S.mode;

    S.o = new floatRect(worldToUserX(S.r.x),
                        worldToUserY(S.r.y),
                        worldToUserX(S.r.x + S.r.width),
                        worldToUserY(S.r.y + S.r.height));

    S.condition = new Query();
    for( int i = 0;i < rects.size(); i++) {
      StringTokenizer interval = new StringTokenizer(tablep.lnames[0][i].substring(1, tablep.lnames[0][i].length()-1), ",");
      MyRect r = (MyRect)rects.elementAt(i);
      if ( r.intersects( sr )) {
        S.condition.addCondition("OR",tablep.names[0]+" >= "+interval.nextToken()+" AND "+tablep.names[0]+" < "+interval.nextToken());
        if( tablep.data.isDB )
          tablep.getSelection();
        else {
          double sum=0, sumh=0;
          for( int j=0; j<r.tileIds.size(); j++ ) {
            int id = ((Integer)(r.tileIds.elementAt(j))).intValue();
            tablep.setSelection(id,1,mode);
            sumh += tablep.getSelected(id)*tablep.table[id];
            sum  += tablep.table[id];
          }
          r.setHilite( sumh/sum );
        }
      }
      else
        if( !tablep.data.isDB )
          for( int j=0; j<r.tileIds.size(); j++ ) {
            int id = ((Integer)(r.tileIds.elementAt(j))).intValue();
            tablep.setSelection(id,0,mode);
          }
    }
  }

    public void updateSelection() {
      paint(this.getGraphics());
    }

    public void paint(Graphics g) {

      frame.setBackground(MFrame.backgroundColor);

      tablep.getSelection();

      Dimension size = this.getViewportSize();

      if( oldWidth != size.width || oldHeight != size.height ) {
        this.width = size.width;
        this.height = size.height;
        create(border, border, size.width-border, size.height-border, "");
        this.setSize( size.width, size.height);
        size = this.getSize();
        oldWidth = size.width;
        oldHeight = size.height;
      }

      if( scaleChanged )
      {
        updateScale();
        create(border, border, size.width-border, size.height-border, "");
      }

      if( g instanceof PrintGraphics || g instanceof PSGr ) {
        size = pj.getPageDimension();
        Font SF = new Font("SansSerif", Font.BOLD, 12);
        g.setFont(SF);
        bg = g;
      }
      else {
        if( bi != null ) {
          if( bi.getWidth(null) != size.width || bi.getHeight(null) != size.height ) {
            bg.dispose();
            bi = null;
            System.gc();
            bi = createImage(size.width, size.height);	// double buffering from CORE JAVA p212
          }
        }
        else {
          bi = createImage(size.width, size.height);	// double buffering from CORE JAVA p212
        }
        bg = bi.getGraphics();
        bg.clearRect(0, 0, size.width, size.height);
      }
      FontMetrics fm = bg.getFontMetrics();

      bg.drawLine( (int)userToWorldX( xMin ), (int)userToWorldY( 0 ) + outside, 
                   (int)userToWorldX( xMax ), (int)userToWorldY( 0 ) + outside );  
      // x-ticks  
      bg.drawLine( (int)userToWorldX( xMin ), (int)userToWorldY( 0 ) + outside, 
                   (int)userToWorldX( xMin ), (int)userToWorldY( 0 ) + outside + tick );  

      bg.drawLine( (int)userToWorldX( xMax ), (int)userToWorldY( 0 ) + outside, 
                   (int)userToWorldX( xMax ), (int)userToWorldY( 0 ) + outside + tick );  

      bg.drawString(Stat.roundToString(xMin, round), 
                    (int)userToWorldX( xMin ), 
                    (int)userToWorldY( 0 ) + outside + tick + fm.getMaxAscent() + fm.getMaxDescent() );

      bg.drawString(Stat.roundToString(xMax, round), 
                    (int)userToWorldX( xMax ) - fm.stringWidth(Stat.roundToString(xMax, round)), 
                    (int)userToWorldY( 0 ) + outside + tick + fm.getMaxAscent() + fm.getMaxDescent() );


      for( int i = 0;i < levels[0]; i++) {
        MyRect r = (MyRect)rects.elementAt(i);
        double sum=0, sumh=0;
        for( int j=0; j<r.tileIds.size(); j++ ) {
          int id = ((Integer)(r.tileIds.elementAt(j))).intValue();
          sumh += tablep.getSelected(id)*tablep.table[id];
          sum  += tablep.table[id];
        }
        r.setHilite( sumh/sum );
        r.draw(bg);
      }

      if( !(g instanceof PrintGraphics || g instanceof PSGr ) ) {
        drawSelections(bg);
        g.drawImage(bi, 0, 0, null);
        bg.dispose();
      }  
    }

    public void drawSelections(Graphics bg) {

      for( int i=0; i<Selections.size(); i++) {
        Selection S = (Selection)Selections.elementAt(i);
        drawBoldDragBox(bg, S);
      }
    }

    public void home() {
      yMax = 0;
      for( int i=0; i<k; i++ ) {
        yMax = Math.max(tablep.table[i]/tablep.data.n/bWidth, yMax);
//        System.out.println("yMax   "+yMax);
      }
      yMax *= 1.1;

      setCoordinates(this.getLlx(), yMin, this.getUrx(), yMax, -1);
    }

    public void processKeyEvent(KeyEvent e) {

      if (e.getID() == KeyEvent.KEY_PRESSED && (e.getKeyCode() == KeyEvent.VK_UP
                                                ||  e.getKeyCode() == KeyEvent.VK_DOWN
                                                ||  e.getKeyCode() == KeyEvent.VK_LEFT
                                                ||  e.getKeyCode() == KeyEvent.VK_RIGHT
                                                || (e.getModifiers() == Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()
                                                    && ( e.getKeyCode() == KeyEvent.VK_0 || e.getKeyCode() == KeyEvent.VK_NUMPAD0)))) {
        if( e.getKeyCode() == KeyEvent.VK_DOWN ) {
          if( bWidth > 0 ) {
            tablep.updateBins(bStart, bWidth -= bWidth*0.1);
          }
        }
        if( e.getKeyCode() == KeyEvent.VK_UP ) {
          if( bWidth < (data.getMax(dvar)-data.getMin(dvar))*1.1 ) {
            tablep.updateBins(bStart, bWidth += bWidth*0.1);
          }
        }
        if( e.getKeyCode() == KeyEvent.VK_LEFT ) {
          if( bStart > data.getMin(dvar)-bWidth ) {
            tablep.updateBins(bStart = Math.max(data.getMin(dvar)-bWidth,  bStart - bWidth*0.1), bWidth);
          }
        }
        if( e.getKeyCode() == KeyEvent.VK_RIGHT ) {
          if( bStart < data.getMin(dvar) ) {	
            tablep.updateBins(bStart = Math.min(data.getMin(dvar), bStart + bWidth*0.1), bWidth);
          }
        }

        if( (e.getKeyCode() == KeyEvent.VK_0 || e.getKeyCode() == KeyEvent.VK_NUMPAD0  )
            && e.getModifiers() == Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() ) {
          home();
        }

        create(border, border, this.width-border, this.height-border, "");
        for( int i=0; i<Selections.size(); i++) {
          Selection S = (Selection)Selections.elementAt(i);
          maintainSelection(S);
        }
        paint(this.getGraphics());
      }
      else
        super.processKeyEvent(e);  // Pass other event types on.
    }

    public void processMouseMotionEvent(MouseEvent e) {

      boolean info = false;

      super.processMouseMotionEvent(e);  // Pass other event types on.
    }	

    public void processMouseEvent(MouseEvent e) {

      if( e.isPopupTrigger() )
        super.processMouseEvent(e);  // Pass other event types on.
      if( changePop ) {
        changePop = false;
        return;
      }
      
      boolean info = false;
      if (e.getID() == MouseEvent.MOUSE_PRESSED ||
          e.getID() == MouseEvent.MOUSE_RELEASED ) {
        if (e.isPopupTrigger() || e.isPopupTrigger() && e.isShiftDown() ) {
          for( int i = 0;i < rects.size(); i++) {
            MyRect r = (MyRect)rects.elementAt(i);
            if ( r.contains( e.getX(), e.getY()+sb.getValue() )) {
              info = true;
              r.pop(this, e.getX(), e.getY());
            }
          }
          if( !info ) {
            JPopupMenu mode = new JPopupMenu();
            if( displayMode.equals("Histogram") ) {
              JMenuItem Spineplot = new JMenuItem("Spinogram");
              mode.add(Spineplot);

              Spineplot.setActionCommand("Spinogram");
              Spineplot.addActionListener(this);
              
              final Axis axisW = new Axis(xMin, xMax);

              JMenu menuWidth = new JMenu("Width");

              mode.add(menuWidth);
              JCheckBoxMenuItem[][] wdt = new JCheckBoxMenuItem[3][4];
              for(int i=2; i>=0; i--) {
                if( bWidth == axisW.tickM*Math.pow(10,(double)i) )
                  wdt[i][0] = new JCheckBoxMenuItem(""+axisW.tickM*Math.pow(10,(double)i), true);
                else
                  wdt[i][0] = new JCheckBoxMenuItem(""+axisW.tickM*Math.pow(10,(double)i), false);
                if( bWidth == axisW.tickMM*Math.pow(10,(double)i) )
                  wdt[i][1] = new JCheckBoxMenuItem(""+axisW.tickMM*Math.pow(10,(double)i), true);
                else
                  wdt[i][1] = new JCheckBoxMenuItem(""+axisW.tickMM*Math.pow(10,(double)i), false);
                if( bWidth == axisW.tickMMM*Math.pow(10,(double)i) )
                  wdt[i][2] = new JCheckBoxMenuItem(""+axisW.tickMMM*Math.pow(10,(double)i), true);
                else
                  wdt[i][2] = new JCheckBoxMenuItem(""+axisW.tickMMM*Math.pow(10,(double)i), false);
                if( bWidth == axisW.tickMMMM*Math.pow(10,(double)i) )
                  wdt[i][3] = new JCheckBoxMenuItem(""+axisW.tickMMMM*Math.pow(10,(double)i), true);
                else
                  wdt[i][3] = new JCheckBoxMenuItem(""+axisW.tickMMMM*Math.pow(10,(double)i), false);

                for(int j=0; j<4; j++) {
                  if( xMax-xMin > Util.atod(wdt[i][j].getText())) {
                    menuWidth.add(wdt[i][j]);
                    wdt[i][j].addItemListener(new ItemListener() {
                      public void itemStateChanged(ItemEvent e) {
                        bWidth = Util.atod(((JCheckBoxMenuItem)e.getItem()).getText());
                        tablep.updateBins(bStart, bWidth);
                        Update();
                      }
                    });
                  }
                }
              }
                
              JMenu menuStart = new JMenu("Start");
              
              mode.add(menuStart);
              JCheckBoxMenuItem[][] fst = new JCheckBoxMenuItem[3][4];
              double lastEntry=-3.1415926;
              double starter=0;
              for(int i=2; i>=0; i--) {                
                for(int j=0; j<4; j++) {
                  int k=0;
                  int insert = 0;
                  double ticker = Util.atod(wdt[i][j].getText());
                  if( xMax-xMin > ticker ) {
                    starter = (Math.floor(xMin/ticker)) * ticker;
                    while( k < menuStart.getItemCount() ) {
                      if( starter > Util.atod(((JCheckBoxMenuItem)menuStart.getItem(k)).getText()) )
                        insert = k+1;
                      k++;
                    }

                    if( insert == menuStart.getItemCount() && insert > 0 &&
                        starter == Util.atod(((JCheckBoxMenuItem)menuStart.getItem(insert-1)).getText()) )
                      insert = -1;
                    else if( insert < menuStart.getItemCount() &&
                             starter == Util.atod(((JCheckBoxMenuItem)menuStart.getItem(insert)).getText()) )
                      insert = -1;
                    
                    if( insert != -1 ) {
                      if( bStart == starter )
                        fst[i][j] = new JCheckBoxMenuItem(""+ starter, true);	
                      else
                        fst[i][j] = new JCheckBoxMenuItem(""+ starter, false);
                      lastEntry = starter;
                      if( insert < menuStart.getItemCount() )
                        menuStart.insert(fst[i][j], insert);
                      else
                        menuStart.add(fst[i][j]);
                      fst[i][j].addItemListener(new ItemListener() {
                        public void itemStateChanged(ItemEvent e) {
                          bStart = Util.atod(((JCheckBoxMenuItem)e.getItem()).getText());
                          tablep.updateBins(bStart, bWidth);
                          Update();
                        }
                      });
                    }
                  }
                }
              }
              if( xMin != lastEntry ) {
                  JCheckBoxMenuItem tmp;
                if( bStart == xMin )
                  tmp = new JCheckBoxMenuItem(""+ xMin, true);
                else
                  tmp = new JCheckBoxMenuItem(""+ xMin, false);
                menuStart.add(tmp);
                tmp.addItemListener(new ItemListener() {
                  public void itemStateChanged(ItemEvent e) {
                    bStart = Util.atod(((JCheckBoxMenuItem)e.getItem()).getText());
                    tablep.updateBins(bStart, bWidth);
                    Update();
                  }
                });
              }
            }
            else {
              JMenuItem Barchart  = new JMenuItem("Histogram");
              mode.add(Barchart);
              Barchart.setActionCommand("Histogram");
              Barchart.addActionListener(this);
            }
            mode.add(new JMenuItem("Dismiss"));
            mode.show(this, e.getX(), e.getY());
          }	
        }
        else
          super.processMouseEvent(e);  // Pass other event types on.
      }
      else 
        super.processMouseEvent(e);  // Pass other event types on.
    }

    public void actionPerformed(ActionEvent e) {
      String command = e.getActionCommand();
      if( command.equals("Histogram") || command.equals("Spinogram")) {
        displayMode = command;
        Update();
      } else
        super.actionPerformed(e);
    }


    public void Update() {
      rects.removeAllElements();
      create(border, border, width-border, height-border, "");
      Graphics g = this.getGraphics();
      paint(g);
      g.dispose();
    }

    public void create(int x1, int y1, int x2, int y2, String info) {

      //setCoordinates(this.getLlx(), yMin, this.getUrx(), yMax, -1);

      rects.removeAllElements();
      labels.removeAllElements();

      this.name = tablep.name;
      this.levels = tablep.levels;
      this.names = tablep.names;
      this.lnames = tablep.lnames;

      this.k = levels[0];
      double sum = 0;
      double max = 0;
      Vector[] tileIds = new Vector[k];

      for(int i=0; i<k; i++ ) {
        sum += tablep.table[i];
        max = Math.max( max, tablep.table[i] );
        tileIds[i] = new Vector(1,0);
        tileIds[i].addElement(new Integer(i));
      }

      Graphics g = this.getGraphics();
      FontMetrics FM = g.getFontMetrics();
      int fh = FM.getHeight();
      g.dispose();

      int n = tablep.data.n;

      //    System.out.println("n   "+n);

      if( displayMode == "Histogram" ) {
        for(int i=0; i<k; i++ ) {
          rects.addElement(new MyRect( true, 'y', "Observed",
                                       (int)userToWorldX(bStart + i*bWidth),
                                       (int)userToWorldY(tablep.table[i]/n/bWidth),
                                       (int)userToWorldX(bStart + (i+1)*bWidth)-(int)userToWorldX(bStart + i*bWidth),
                                       (int)userToWorldY(0)-(int)userToWorldY(tablep.table[i]/n/bWidth),
                                       tablep.table[i], tablep.table[i], 1, 0, lnames[0][i]+'\n', tileIds[i]));
        }
      }
      else {				// Spinogramm
        int lastX = (int)userToWorldX(bStart);
        int fullRange = (int)userToWorldX(xMax) - (int)userToWorldX(bStart);
        for(int i=0; i<k; i++ ) {
          int currX = lastX + (int)Math.round(tablep.table[i]/n * fullRange); 
          rects.addElement(new MyRect( true, 'y', "Observed",
                                       lastX,
                                       (int)userToWorldY(yMax),
                                       currX-lastX,
                                       (int)userToWorldY(yMin)-(int)userToWorldY(yMax),
                                       tablep.table[i], tablep.table[i], 1, 0, lnames[0][i]+'\n', tileIds[i]));
          lastX = currX; 
        }
      }

      for( int i=0; i<Selections.size(); i++) {
        Selection S = (Selection)Selections.elementAt(i);
        S.r.x      = (int)userToWorldX( ((floatRect)S.o).x1 );
        S.r.y      = (int)userToWorldY( ((floatRect)S.o).y1 );
        S.r.width  = (int)userToWorldX( ((floatRect)S.o).x2 ) - (int)userToWorldX( ((floatRect)S.o).x1 );
        S.r.height = (int)userToWorldY( ((floatRect)S.o).y2 ) - (int)userToWorldY( ((floatRect)S.o).y1 );
      }
    }

    public void dataChanged(int var) {
      if( var == tablep.initialVars[0] ) {
        paint(this.getGraphics());
      }
    }

    public void scrollTo(int id) {
    }

    public void adjustmentValueChanged(AdjustmentEvent e) {
    }

    class floatRect {
      double x1, y1, x2, y2;
      public floatRect(double x1, double y1, double x2, double y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
      }
    }
    private String name;          // the name of the table;
    private double table[];	// data in classical generalized binary order
    private int[] levels;    	// number of levels for each variable
    private int[] plevels;        // reverse cummulative product of levels

    private String[] names;	// variable names
    private String[][] lnames;	// names of levels
    private DataListener listener;
    private static EventQueue evtq;
}