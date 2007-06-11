// Please use this software or at least parts of it to
//    - navigate airplanes and supertankers
//    - control nuclear power plants and chemical plants
//    - launch cruise missiles automatically
//
//      thanks
//
//      To Do:
//               
//	       - Sorting of Intervalls in Histos ?? (DB)
//
//         - PC
//           - zoom after permuting the axis ???
//           - zoom back -> common scale ?!


import java.awt.*;               // ScrollPane, PopupMenu, MenuShortcut, etc.
import java.awt.image.*;         
import java.awt.event.*;         // New event model.
import java.io.*;                // Object serialization streams.
import java.io.InputStream;      // Object serialization streams.
import java.util.*;              // For StingTokenizer.
import java.util.Vector;         // 
import java.util.Properties;     // To store printing preferences in.
import java.util.jar.JarFile; 	 // To load logo
import java.util.zip.ZipEntry;
import java.lang.*;              // 
import java.net.URL;
import java.sql.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.border.*;
import org.rosuda.JRclient.*;
//import com.apple.eawt.*;
import com.apple.mrj.*;
import java.text.*;

/**
*/

class Join extends JFrame implements ProgressIndicator, SelectionListener, DataListener, MRJQuitHandler, MRJOpenDocumentHandler {  
  
  /** Remember # of open windows so we can quit when last one is closed */
  protected static int num_windows = 0;
  protected static Vector dataSets;
  protected static Vector Mondrians;
  public Vector Plots = new Vector(10,0);
  public Vector selList = new Vector(10,0);
  public Query sqlConditions;
  public boolean selseq = false;
  public boolean alphaHi = false;
  public boolean hasR = false;
  private Vector polys = new Vector(256,256);
  private JList varNames = null;
  private int numCategorical = 0;
  private int weightIndex = 0;
  private JScrollPane scrollPane;
  private JProgressBar progBar;
  private JPanel progPanel;
  private JLabel progText;
  public JMenuBar menubar;
  public JMenu windows, help, dv, sam, trans;
  private JMenuItem n, nw, c, q, t, m, o, s, ss, sa, ts, p, od, mv, mn, pr, b, bw, pc, pb, byx, sc, sc2, hi, hiw, cc, cs, vm, rc, oh, mds, pca;
  public  JMenuItem ca, fc, fs, transPlus, transMinus, transTimes, transDiv, transNeg, transInv, transLog, transExp;
  private JCheckBoxMenuItem se, ah, ih, os, as;
  private ModelNavigator Mn;
  private PreferencesFrame Pr;
  private int thisDataSet  = -1;
  private int dCol=1, dSel=1;
  private int graphicsPerf;
  static String user;
  public boolean mondrianRunning = false;
  private String justFile = "";
  private boolean load = false;
  private boolean killed = false;
  private int[] selectBuffer;
  
  public Join(Vector Mondrians, Vector dataSets, boolean load, boolean loadDB, File loadFile) {
    
    Mondrians.addElement(this);
    
    MRJApplicationUtils.registerOpenDocumentHandler ( this );

//    System.out.println("........... Creating new Instance of Join .........");
    
    this.load = load;
    
    Toolkit.getDefaultToolkit().setDynamicLayout(false);
    
    MRJApplicationUtils.registerQuitHandler(this);
    
    hasR = Srs.checkLocalRserve();
    
    System.out.println("Starting RServe ... "+hasR);
    
    user = System.getProperty("user.name");
    System.out.println(user+" on "+System.getProperty("os.name"));
    
    if( user.indexOf("dibene") > -1 || user.indexOf("hofmann") > -1) {
      PreferencesFrame.setScheme(1);
      selseq = true;
    } else if( user.indexOf("unwin") > -1 ) {
      PreferencesFrame.setScheme(0);
    } 
    PreferencesFrame.setScheme(2);
    
    Font SF = new Font("SansSerif", Font.BOLD, 12);
    this.setFont(SF);
    this.dataSets = dataSets;
    this.Mondrians = Mondrians;
    this.setTitle("Mondrian");               // Create the window.
    num_windows++;                           // Count it.
    
    menubar = new JMenuBar();         // Create a menubar.
    
    // Create menu items, with menu shortcuts, and add to the menu.
    JMenu file = (JMenu) menubar.add(new JMenu("File"));
    //   JMenu file = new JMenu("File");            // Create a File menu.
    file.add(o = new JMenuItem("Open"));
    o.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    file.add(od = new JMenuItem("Open Database"));
    od.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    if( user.indexOf("theus") > -1)
      od.setEnabled(true);
    else
      od.setEnabled(false);
    file.add(s = new JMenuItem("Save"));
    s.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    s.setEnabled(false);
    file.add(ss = new JMenuItem("Save Selection"));
    ss.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Event.SHIFT_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    ss.setEnabled(false);
    file.add(c = new JMenuItem("Close Dataset"));
    c.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    c.setEnabled(false);
    //    file.add(p = new JMenuItem("Print Window",new JMenuShortcut(KeyEvent.VK_P)));
    q = new JMenuItem("Quit");
    if( ((System.getProperty("os.name")).toLowerCase()).indexOf("mac") == -1 ) {
      file.addSeparator();                     // Put a separator in the menu
      file.add(q);
      q.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

      q.addActionListener(new ActionListener() {     // Quit the program.
        public void actionPerformed(ActionEvent e) {
          try {																				// Shut down RServe if running ...
            Rconnection c=new Rconnection();
            c.shutdown();
          } catch (Exception x) {};
          System.exit(0); 
        }
      });
    }
    menubar.add(file);                         // Add to menubar.
                                               //
    JMenu plot = new JMenu("Plot");            // Create a Plot menu.
    plot.add(mv = new JMenuItem("Missing Value Plot"));
    mv.setEnabled(false);
    plot.addSeparator();        
    plot.add(b = new JMenuItem("Barchart"));
    b.setEnabled(false);
    plot.add(bw = new JMenuItem("Weighted Barchart"));
    bw.setEnabled(false);
    plot.addSeparator();        
    plot.add(hi = new JMenuItem("Histogram"));
    hi.setEnabled(false);
    plot.add(hiw = new JMenuItem("Weighted Histogram"));
    hiw.setEnabled(false);
    plot.addSeparator();        
    plot.add(sc2 = new JMenuItem("Scatterplot"));
    sc2.setEnabled(false);
    plot.addSeparator();        
    plot.add(n = new JMenuItem("Mosaic Plot"));
    n.setEnabled(false);
    plot.add(nw = new JMenuItem("Weighted Mosaic Plot"));
    nw.setEnabled(false);
    plot.addSeparator();        
    plot.add(pc = new JMenuItem("Parallel Coordinates"));
    pc.setEnabled(false);
    plot.add(pb = new JMenuItem("Parallel Boxplot"));
    pb.setEnabled(false);
    plot.add(byx = new JMenuItem("Boxplot y by x"));
    byx.setEnabled(false);
    plot.addSeparator();        
    if( true || user.indexOf("theus") > -1 ) {
      plot.add(t = new JMenuItem("SPLOM"));
      t.setEnabled(false);
      plot.addSeparator();                     // Put a separator in the menu
    }
    plot.add(m = new JMenuItem("Map"));
    m.setEnabled(false);
    menubar.add(plot);                         // Add to menubar.
    //
    JMenu calc = new JMenu("Calc");            // Create a Calc menu.
    calc.add(mds = new JMenuItem("2-dim MDS"));
    mds.setEnabled(false);
                                               //
    calc.add(pca = new JMenuItem("PCA"));
    pca.setEnabled(false);
                                               //
    calc.add(trans = new JMenu("transform"));
    trans.setEnabled(false);
    trans.add(transPlus = new JMenuItem("x + y"));
    trans.add(transMinus = new JMenuItem("x - y"));
    trans.add(transTimes = new JMenuItem("x * y"));
    trans.add(transDiv = new JMenuItem("x / y"));
    trans.addSeparator();                     // Put a separator in the menu
    trans.add(transNeg = new JMenuItem("- x"));
    trans.add(transInv = new JMenuItem("1/x"));
    trans.add(transLog = new JMenuItem("log(x)"));
    trans.add(transExp = new JMenuItem("exp(x)"));

    menubar.add(calc);                         // Add to menubar.

    JMenu options = new JMenu("Options");      // Create an Option menu.
    options.add(sa = new JMenuItem("Select All"));
    sa.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    
    options.add(ts = new JMenuItem("Toggle Selection"));
    ts.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_K, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    
    options.add(sam = new JMenu("<SHIFT><ALT> is"));
    sam.add(os = new JCheckBoxMenuItem("OR Selection"));
    sam.add(as = new JCheckBoxMenuItem("AND Selection"));
    as.setSelected(true);

    options.addSeparator();                     // Put a separator in the menu
    options.add(cc = new JMenuItem("Clear all Colors"));
    cc.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, Event.ALT_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

    options.addSeparator();                     // Put a separator in the menu
    options.add(se = new JCheckBoxMenuItem("Selection Sequences", selseq));
    se.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    
    options.add(cs = new JMenuItem("Clear Sequences"));
    cs.setAccelerator(KeyStroke.getKeyStroke(Event.BACK_SPACE, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    
    options.addSeparator();                     // Put a separator in the menu
    options.add(ah = new JCheckBoxMenuItem("Alpha on Highlight", alphaHi));
    ah.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

    options.addSeparator();                     // Put a separator in the menu
    options.add(vm = new JMenuItem("Switch Variable Mode"));
    vm.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    
    options.add(dv = new JMenu("Derive Variable from"));
    dv.add(fs = new JMenuItem("Selection"));
    fs.setEnabled(false);
    dv.add(fc = new JMenuItem("Colors"));
    fc.setEnabled(false);
    
    options.addSeparator();                     // Put a separator in the menu
    options.add(mn = new JMenuItem("Model Navigator", KeyEvent.VK_J));
    mn.setEnabled(false);

    options.addSeparator();                     // Put a separator in the menu
    options.add(pr = new JMenuItem("Preferences ...", KeyEvent.VK_K));
    pr.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_COMMA, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    
    menubar.add(options);                      // Add to menubar.
    
    windows = (JMenu) menubar.add(new JMenu("Window"));

    windows.add(ca = new JMenuItem("Close All"));
    ca.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, Event.SHIFT_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    ca.setEnabled(false);

    windows.addSeparator();

    help = (JMenu) menubar.add(new JMenu("Help"));

    help.add(rc = new JMenuItem("Reference Card"));
    rc.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_HELP, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    rc.setEnabled(true);

    help.add(ih = new JCheckBoxMenuItem("Interactive Help"));
    ih.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_HELP, Event.SHIFT_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    ih.setEnabled(false);

    help.add(oh = new JMenuItem("Online Help"));
    oh.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_HELP, Event.SHIFT_MASK | Event.ALT_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    oh.setEnabled(true);

    this.setJMenuBar(menubar);                 // Add it to the frame.
    
    Icon MondrianIcon = new ImageIcon(readGif("Logo.gif"));    
    
    JLabel MondrianLabel = new JLabel(MondrianIcon);
    scrollPane = new JScrollPane(MondrianLabel, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    getContentPane().add("Center", scrollPane);

    // Add the status/progress bar
    progPanel = new JPanel();
    progText = new JLabel("   Welcome !    "); 
    progPanel.add("North", progText);
    progBar = new JProgressBar();
    progBar.setMinimum(0);
    progBar.setMaximum(1);
    progBar.setValue(0);
    progBar.addChangeListener(new ChangeListener() {     
      public void stateChanged(ChangeEvent e) { showIt();}
    });
    progPanel.add("South", progBar);
    
    getContentPane().add("South", progPanel);
    
    // Create and register action listener objects for the menu items.
    mv.addActionListener(new ActionListener() {     // Open a new missing value plot window
      public void actionPerformed(ActionEvent e) {
        missPlot();
      }
    });
    n.addActionListener(new ActionListener() {     // Open a new mosaic plot window
      public void actionPerformed(ActionEvent e) {
        mosaicPlot();
      }
    });
    nw.addActionListener(new ActionListener() {     // Open a new weighted mosaic plot window
      public void actionPerformed(ActionEvent e) {
        weightedMosaicPlot();
      }
    });
    b.addActionListener(new ActionListener() {     // Open a new mosaic plot window
      public void actionPerformed(ActionEvent e) {
        barChart();
      }
    });
    bw.addActionListener(new ActionListener() {     // Open a new mosaic plot window
      public void actionPerformed(ActionEvent e) {
        weightedbarChart();
      }
    });
    hi.addActionListener(new ActionListener() {     // Open a histogram window
      public void actionPerformed(ActionEvent e) {
        histogram();
      }
    });
    hiw.addActionListener(new ActionListener() {     // Open a weighted histogram window
      public void actionPerformed(ActionEvent e) {
        weightedHistogram();
      }
    });
    pc.addActionListener(new ActionListener() {     // Open a parallel coordinate plot window
      public void actionPerformed(ActionEvent e) {
        pc("Poly");
      }
    });
    pb.addActionListener(new ActionListener() {     // Open a parallel boxplot plot window
      public void actionPerformed(ActionEvent e) { pc("Box"); }
    });
    byx.addActionListener(new ActionListener() {     // Open a boxplot plot y by x window
      public void actionPerformed(ActionEvent e) { pc("Box"); }
    });
    sc2.addActionListener(new ActionListener() {     // Open a scatterplot window
      public void actionPerformed(ActionEvent e) {
        scatterplot2D();
      }
    });
    if( true || user.indexOf("theus") > -1 )
      t.addActionListener(new ActionListener() {     // Open a new test window
        public void actionPerformed(ActionEvent e) {
          test();
        }
      });
    o.addActionListener(new ActionListener() {     // Load a dataset
      public void actionPerformed(ActionEvent e) {
//        System.out.println(".......... CALL loadDataSet() FROM Open .........");
        loadDataSet(false, null);
      }
    });
    s.addActionListener(new ActionListener() {     // Save the current dataset
      public void actionPerformed(ActionEvent e) {
        Save(false);
      }
    });
    ss.addActionListener(new ActionListener() {     // Save the current selection
      public void actionPerformed(ActionEvent e) {
        Save(true);
      }
    });
    od.addActionListener(new ActionListener() {     // Load a database
      public void actionPerformed(ActionEvent e) {
        loadDataSet(true, null);
      }
    });
    m.addActionListener(new ActionListener() {     // Open a new window to draw an interactive maps
      public void actionPerformed(ActionEvent e) {
        mapPlot();
      }
    });
    mds.addActionListener(new ActionListener() {     // Open a new window for a 2-dim MDS
      public void actionPerformed(ActionEvent e) {
        mds();
      }
    });
    pca.addActionListener(new ActionListener() {     // calculate PCA
      public void actionPerformed(ActionEvent e) {
        pca();
      }
    });
    transPlus.addActionListener(new ActionListener() {     // x + y
      public void actionPerformed(ActionEvent e) {
        transform(1);
      }
    });
    transMinus.addActionListener(new ActionListener() {     // x - y
      public void actionPerformed(ActionEvent e) {
        transform(2);
      }
    });
    transTimes.addActionListener(new ActionListener() {     // x * y
      public void actionPerformed(ActionEvent e) {
        transform(3);
      }
    });
    transDiv.addActionListener(new ActionListener() {     // x / y
      public void actionPerformed(ActionEvent e) {
        transform(4);
      }
    });
    transNeg.addActionListener(new ActionListener() {     // - x
      public void actionPerformed(ActionEvent e) {
        transform(5);
      }
    });
    transInv.addActionListener(new ActionListener() {     // 1/x
      public void actionPerformed(ActionEvent e) {
        transform(6);
      }
    });
    transLog.addActionListener(new ActionListener() {     // log(x)
      public void actionPerformed(ActionEvent e) {
        transform(7);
      }
    });
    transExp.addActionListener(new ActionListener() {     // exp(x)
      public void actionPerformed(ActionEvent e) {
        transform(8);
      }
    });
    se.addActionListener(new ActionListener() {     // Change the selection mode
      public void actionPerformed(ActionEvent e) {
        switchSelection();
      }
    });
    fs.addActionListener(new ActionListener() {     // Derive variable from selection (false) or color (true)
      public void actionPerformed(ActionEvent e) {
        deriveVariable(false);
      }
    });
    fc.addActionListener(new ActionListener() {     // Derive variable from selection (false) or color (true)
      public void actionPerformed(ActionEvent e) {
        deriveVariable(true);
      }
    });
    os.addActionListener(new ActionListener() {     // Set extended selection mode AND (false) or OR (true)
      public void actionPerformed(ActionEvent e) {
        setExtSelMode(true);
      }
    });
    as.addActionListener(new ActionListener() {     // Set extended selection mode AND (false) or OR (true)
      public void actionPerformed(ActionEvent e) {
        setExtSelMode(false);
      }
    });
    sa.addActionListener(new ActionListener() {     // Select All via Menu
      public void actionPerformed(ActionEvent e) { selectAll(); }
    });
    ts.addActionListener(new ActionListener() {     // Toggle Selection via Menu
      public void actionPerformed(ActionEvent e) { toggleSelection(); }
    });
    cc.addActionListener(new ActionListener() {     // Clear all Colors
      public void actionPerformed(ActionEvent e) { clearColors(); }
    });
    ah.addActionListener(new ActionListener() {     // Change the alpha mode for highlighted cases
      public void actionPerformed(ActionEvent e) {
        switchAlpha();
      }
    });
    mn.addActionListener(new ActionListener() {     // Open a new window for the model navigator
      public void actionPerformed(ActionEvent e) {
        modelNavigator();
      }
    });
    pr.addActionListener(new ActionListener() {     // Open the Preference Box
      public void actionPerformed(ActionEvent e) {
        preferenceFrame();
      }
    });
    cs.addActionListener(new ActionListener() {     // Delete the current selection sequence
      public void actionPerformed(ActionEvent e) {
        deleteSelection();
      }
    });
    vm.addActionListener(new ActionListener() {     // Delete the current selection sequence
      public void actionPerformed(ActionEvent e) {
        switchVariableMode();
      }
    });
    ca.addActionListener(new ActionListener() {     // Close all Windows
      public void actionPerformed(ActionEvent e) {
        closeAll();
      }
    });
    c.addActionListener(new ActionListener() {     // Close this window.
      public void actionPerformed(ActionEvent e) { close(); }
    });
    rc.addActionListener(new ActionListener() {     // Show reference card window.
      public void actionPerformed(ActionEvent e) { refCard(); }
    });
    oh.addActionListener(new ActionListener() {     // Show Mondrian Webpage.
      public void actionPerformed(ActionEvent e) { onlineHelp(); }
    });
    
    // Another event listener, this one to handle window close requests.
    setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    this.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) { close(); }
    });
    
    this.addWindowListener(new WindowAdapter() {
      public void windowActivated(WindowEvent e) {
        topWindow();
      }
    });
    
    // Set the window size and pop it up.
    this.setResizable(false);
    this.setSize(295,320);
    this.show();

    if( dataSets.isEmpty() )
      graphicsPerf = setGraphicsPerformance();
    else if (((dataSet)dataSets.firstElement()).graphicsPerf != 0)
      graphicsPerf = ((dataSet)dataSets.firstElement()).graphicsPerf;
    else
      graphicsPerf = 25000;
    
    Graphics g = this.getGraphics();
    g.setFont(new Font("SansSerif",0,11));
    g.drawString("beta 8", 250, 285);

    mondrianRunning = true;

    if( !hasR ) {
//      JOptionPane.showMessageDialog(this, "Connection to R failed:\nSome functions might be missing!\n\nPlease check installation of R and  Rserve\nor try starting Rserve manually ...","Rserve Error",JOptionPane.WARNING_MESSAGE);
      g.setColor(Color.white);
      g.fillRect(9,275,220,14);
      g.setColor(Color.gray);
      g.drawString("Connection to R failed: Please check Rserve", 9, 285);
    }

    if( load )
      if( loadDB )
        loadDataSet(true, null);  
      else {
//        System.out.println(".......... CALL loadDataSet() FROM Join .........");
        loadDataSet(false, loadFile); 
      }
  }

  public void handleQuit()
  {	
    System.exit(0);
  }
  
  void showIt() {
    paintAll(this.getGraphics());
  }

  byte[] readGif(String name) {

    byte[] arrayLogo;
    try {
      JarFile MJF;
      try {
        MJF = new JarFile("Mondrian.app/Contents/Resources/Java/Mondrian.jar");
      } catch (Exception e) {
        MJF = new JarFile(System.getProperty("java.class.path"));
      }
      ZipEntry LE = MJF.getEntry(name);
      InputStream inputLogo = MJF.getInputStream(LE);
      arrayLogo = new byte[(int)LE.getSize()];
      for( int i=0; i<arrayLogo.length; i++ ) {
        arrayLogo[i] = (byte)inputLogo.read();
      }
    } catch (Exception e) {
      System.out.println("Logo Exception: "+e);
      arrayLogo = new byte[1];
    }
    return arrayLogo;
  }
  
  int setGraphicsPerformance() {
    
    int graphicsPerf=0;
    Image testI = createImage(200, 200);	    //
    Graphics2D gI = (Graphics2D)testI.getGraphics();
    gI.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ((float)0.05)));
    long start = new java.util.Date().getTime();
    while( new java.util.Date().getTime() - start < 1000) {
      graphicsPerf++;
      gI.fillOval(10, 10, 3, 3);
    }
    System.out.println("Graphics Performance: "+ graphicsPerf);
    
    return graphicsPerf;
  }
  
  /** Close a window.  If this is the last open window, just quit. */
  void close() {
    // Modal dialog with OK button

    if( thisDataSet == -1 ) {
      this.dispose();
      if( --num_windows == 0 )
        System.exit(0);
      return;
    }
    
    String message = "Close dataset \""+((dataSet)dataSets.elementAt(thisDataSet)).setName+"\" and\n all corresponding plots?";

    int answer = JOptionPane.showConfirmDialog(this, message);
    if (answer == JOptionPane.YES_OPTION) {
      num_windows--;
      for( int i=Plots.size()-1; i>=0; i-- )
        ((MFrame)((DragBox)Plots.elementAt(i)).frame).close();
      dataSets.setElementAt(new dataSet("nullinger"), thisDataSet);
      this.dispose();
      if (num_windows == 0) {
        new Join(Mondrians, dataSets, false , false, null);
//        System.out.println(" -----------------------> disposing Join !!!!!!!!!!!!!!!!");
        this.dispose();
        this.killed = true;
      }
    } 
  }

  public void closeAll() {
    for( int i=Plots.size()-1; i>=0; i-- ) {
      ((MFrame)((DragBox)Plots.elementAt(i)).frame).close();
      Plots.removeElementAt(i);
    }
  }

  public void refCard() {
    final MFrame refCardf = new MFrame(this);

    Icon RefIcon = new ImageIcon(readGif("ReferenceCard.gif"));

    JLabel RefLabel = new JLabel(RefIcon);
    JScrollPane refScrollPane = new JScrollPane(RefLabel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    refCardf.getContentPane().add("Center", refScrollPane);
    refCardf.setTitle("Mondrian - Reference Card");
    refCardf.setResizable(false);
    refCardf.pack();
    refCardf.setSize(refCardf.getWidth(), Math.min(refCardf.getHeight(), (Toolkit.getDefaultToolkit().getScreenSize()).height-34));
    refCardf.setLocation((Toolkit.getDefaultToolkit().getScreenSize()).width - refCardf.getWidth(), 0);
    refCardf.show();

    refCardf.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) { refCardf.dispose(); }
    });
    refCardf.addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent e) { if (e.getModifiers() == Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() && e.getKeyCode() == KeyEvent.VK_W ) refCardf.dispose(); }
    });
  }

  public void onlineHelp() {

    try {
      if( ((System.getProperty("os.name")).toLowerCase()).indexOf("mac") > -1 )
        Runtime.getRuntime().exec("open http://www.rosuda.org/Mondrian");
      else if( ((System.getProperty("os.name")).toLowerCase()).indexOf("win") > -1 )
        Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler http://www.rosuda.org/Mondrian");
      else
        Runtime.getRuntime().exec("firefox http://www.rosuda.org/Mondrian");
    }
    catch (Exception e) {
      System.out.println("Can't start browser!");
    }
  }
    
  public void transform(int mode) {
    checkHistoryBuffer();
    
    System.out.println("Transform: "+mode);
    String name = "";
    dataSet data = ((dataSet)dataSets.elementAt(thisDataSet));
    
    double[]  tData = new double[data.n];
    boolean[] tMiss = new boolean[data.n];
    String name1 = data.getName(selectBuffer[1]);
    String name2 = data.getName(selectBuffer[0]);
    switch(mode) {
      case 1:
        name = name1+" + "+name2;
        break;
      case 2:
        name = name1+" - "+name2;
        break;
      case 3:
        name = name1+" * "+name2;
        break;
      case 4:
        name = name1+" / "+name2;
        break;
      case 5:
        name = "-"+name2;
        break;
      case 6:
        name = "1/"+name2;
        break;
      case 7:
        name = "log("+name2+")";
        break;
      case 8:
        name = "exp("+name2+")";
        break;
    }
    double[] var1 = data.getRawNumbers(selectBuffer[1]);
    double[] var2 = data.getRawNumbers(selectBuffer[0]);
    boolean[] miss1 = data.getMissings(selectBuffer[1]);
    boolean[] miss2 = data.getMissings(selectBuffer[0]);
    for( int i=0; i<data.n; i++ ) {
      if( miss2[i] || ((mode < 5) && (miss1[i] || miss2[i]) ) )
        tMiss[i] = true;
      else
        tMiss[i] = false;
      switch(mode) {
        case 1:
          tData[i] = var1[i] + var2[i];
          break;
        case 2:
          tData[i] = var1[i] - var2[i];
          break;
        case 3:
          tData[i] = var1[i] * var2[i];
          break;
        case 4:
          if( var2[i] != 0 )
            tData[i] = var1[i] / var2[i];
          else
            tMiss[i] = true;
          break;
        case 5:
          tData[i] = -var2[i];
          break;
        case 6:
          if( var2[i] != 0 )
            tData[i] = 1/var2[i];
          else
            tMiss[i] = true;
          break;
        case 7:
          if( var2[i] > 0 )
            tData[i] = Math.log(var2[i]);
          else
            tMiss[i] = true;
          break;
        case 8:
          tData[i] = Math.exp(var2[i]);
          break;
      }
    }
    for( int i=0; i<data.n; i++ )
      if( tMiss[i] )
        tData[i] = Double.MAX_VALUE;
    boolean what;
    if( mode < 5 )
      what = data.categorical(selectBuffer[0]) && data.categorical(selectBuffer[1]);
    else    
      what = data.categorical(selectBuffer[0]);
    data.addVariable(name, false, what, tData, tMiss);
    varNames = null;
    setVarList();
  }

  public void switchSelection() {
    if( thisDataSet>-1 && ((dataSet)dataSets.elementAt(thisDataSet)).isDB )
      selseq = true;
    else {
      selseq = se.isSelected();
//System.out.println("Selection Sequences : "+selseq);
      if( !selseq )
        deleteSelection();
    }
  }

  public void switchAlpha() {
    alphaHi = ah.isSelected();
    updateSelection();
  }
  
  public void selectAll() {
    if(thisDataSet > -1) { 
      ((dataSet)dataSets.elementAt(thisDataSet)).selectAll();
      updateSelection();
    }
  }
  
  public void toggleSelection() {
    if(thisDataSet > -1) { 
      ((dataSet)dataSets.elementAt(thisDataSet)).toggleSelection();
      updateSelection();
    }
  }
  
  public void clearColors() {
    if(thisDataSet > -1) { 
      ((dataSet)dataSets.elementAt(thisDataSet)).colorsOff();
      dataChanged(-1);
    }
  }
    
  public void setExtSelMode(boolean mode) {
    DragBox.extSelMode = mode;
    os.setSelected(mode);
    as.setSelected(!mode);
  }
    
  public void deriveVariable(boolean color) {
    
    String name;
    dataSet data = ((dataSet)dataSets.elementAt(thisDataSet));
    if( color )
      name = "Colors "+dCol++;
    else
      name = "Selection "+dSel++;
    name = JOptionPane.showInputDialog(this, "Please name the new variable:", name);

    double[] dData;
    if( color ) {
      dData = new double[data.n];
      for( int i=0; i<data.n; i++ )
        dData[i] = (double)data.colorArray[i];
    } else {
      dData = data.getSelection();
    }
    data.addVariable(name, false, true, dData, new boolean[data.n]);
    varNames = null;
    setVarList();
  }
  
  
  public void deleteSelection() {
    if( selList.size() > 0 ) {
      for( int i=0; i<Plots.size(); i++ )
        (((DragBox)Plots.elementAt(i)).Selections).removeAllElements();
      for( int i=0; i< selList.size(); i++ ) 
        ((Selection)selList.elementAt(i)).status = Selection.KILLED;
      maintainWindowMenu(false);
      updateSelection();
    }
  }

  public void updateSelection() {
    // Remove Selections from list, which are no longer active
    //
    boolean selectAll = false;
    boolean toggleSelection = false;
    boolean deleteAll = false;
    boolean switchSel = false;
        
    for( int i=0; i<Plots.size(); i++ ) {
      if( ((DragBox)Plots.elementAt(i)).selectAll ) {    // This window has caused the select all event 
        ((DragBox)Plots.elementAt(i)).selectAll = false;
        selectAll = true;
      }
      if( ((DragBox)Plots.elementAt(i)).toggleSelection ) {    // This window has caused the toggle selection event 
        ((DragBox)Plots.elementAt(i)).toggleSelection = false;
        toggleSelection = true;
      }
      if( ((DragBox)Plots.elementAt(i)).deleteAll ) {    // This window has caused the deletion event 
        ((DragBox)Plots.elementAt(i)).deleteAll = false;
        deleteSelection();
        return;
      }
      if( ((DragBox)Plots.elementAt(i)).switchSel ) {    // This window has caused the switch event 
        ((DragBox)Plots.elementAt(i)).switchSel = false;
        se.setSelected(!se.isSelected());                // perform the tick mark change manually ...
        switchSelection();
        return;
      }
      if( ((DragBox)Plots.elementAt(i)).switchAlpha ) {    // This window has caused the switch alpha event
        ((DragBox)Plots.elementAt(i)).switchAlpha = false;
        ah.setSelected(!ah.isSelected());
        switchAlpha();
        ((DragBox)Plots.elementAt(i)).updateSelection();
        return;
      }
    }
    
    if( !(selectAll || toggleSelection) ) {
      
      for( int i=selList.size()-1; i>=0; i-- ) {
        if( (((Selection)selList.elementAt(i)).status == Selection.KILLED) || 
            !((Selection)selList.elementAt(i)).d.frame.isVisible() ) {
          selList.removeElementAt(i);
        }
      }
      
      selList.trimToSize();
      
      Selection oneClick = null;

      // Get the latest selection and add it, if its a new selection
      //
      for( int i=0; i<Plots.size(); i++ ) 
        if( ((DragBox)Plots.elementAt(i)).frame.isVisible() ) {  // Plotwindow still exists
          if( ((DragBox)Plots.elementAt(i)).selectFlag ) {       // This window has caused the selection event 
            ((DragBox)Plots.elementAt(i)).selectFlag = false;    // We need to get the last selection from this plot
            Selection S = (Selection)(((DragBox)Plots.elementAt(i)).Selections.lastElement());
            if( selList.indexOf(S) == -1 )  { // Not in the list yet => new Selection to add !
              if( !(S.r.width < 3 || S.r.height < 3) && selseq) {
                System.out.println("Selection Sequence  !!");
                S.step = selList.size() + 1;
                selList.addElement(S);
              } else {
                oneClick = S;
                System.out.println("Click Selection  !!");
                oneClick.status = Selection.KILLED;
                ((DragBox)Plots.elementAt(i)).Selections.removeElementAt(((DragBox)Plots.elementAt(i)).Selections.size()-1);
              }
            }
          }
        } else 
          Plots.removeElementAt(i--);
      
      if( selList.size() > 1 ) {
        ((Selection)(selList.firstElement())).mode = Selection.MODE_STANDARD;
      }
      // Do the update over all selections
      //
      if( oneClick != null ) {
        //  This is a oneClick selection -> make it visible for Java 1.4 ...
        oneClick.r.width += 1;
        oneClick.r.height += 1;
        (oneClick.d).maintainSelection(oneClick);
      } else {
        maintainWindowMenu(false);

        for( int i=0; i< selList.size(); i++ ) {
          Selection S = ((Selection)selList.elementAt(i));
          S.step = i + 1;
          S.total = selList.size();
          (S.d).maintainSelection(S);
          ((MFrame)((S.d).frame)).maintainMenu(S.step);
        }
      }
      sqlConditions = new Query();				// Replace ???
      if( ((dataSet)dataSets.elementAt(thisDataSet)).isDB )
        for( int i=0; i< selList.size(); i++ ) {
          Selection S = ((Selection)selList.elementAt(i));
          if( S.mode == S.MODE_STANDARD )
            sqlConditions.clearConditions();
          String condStr = S.condition.getConditions();
          if( !condStr.equals("") )
            sqlConditions.addCondition(S.getSQLModeString(S.mode), "("+condStr+")");
        };
      ((dataSet)(dataSets.elementAt(thisDataSet))).sqlConditions = sqlConditions;

//      System.out.println("Main Update: "+sqlConditions.makeQuery());
      
    } else {
      if( toggleSelection ) {
        System.out.println(" TOGGLE SELECTION ... ");
        ((dataSet)(dataSets.elementAt(thisDataSet))).toggleSelection();
      }
      else {
        System.out.println(" SELECT ALL ... ");
        ((dataSet)(dataSets.elementAt(thisDataSet))).selectAll();
      }
      if( ((dataSet)dataSets.elementAt(thisDataSet)).isDB )
        sqlConditions.clearConditions();
    }
				
    // Finally get the plots updated
    //
    for( int i=0; i<Plots.size(); i++ ) {	
      //     progText.setText("Query: "+i);
      progBar.setValue(1);   
      ((DragBox)Plots.elementAt(i)).updateSelection(); 
    }
				
    ((dataSet)dataSets.elementAt(thisDataSet)).selChanged = true;
    int nom   = ((dataSet)dataSets.elementAt(thisDataSet)).countSelection();
    int denom = ((dataSet)dataSets.elementAt(thisDataSet)).n;
    String Display = nom+"/"+denom+" ("+Stat.roundToString(100F*nom/denom,2)+"%)";
    progText.setText(Display);
    progBar.setValue(nom);

    maintainOptionMenu();
    
    if( nom > 0 )
      ss.setEnabled(true);
    else
      ss.setEnabled(false);
  }
  
  public void dataChanged(int id) {
    
    //System.out.println("Join got the event !!!!"+id);

    maintainOptionMenu();
    
    // First check whether a color has been set individually
    for( int i=0; i<Plots.size(); i++ ) {
      int col = ((DragBox)Plots.elementAt(i)).colorSet;
      if( col > -1 ) {
        ((DragBox)Plots.elementAt(i)).colorSet = -1;
        dataSet data = ((dataSet)dataSets.elementAt(thisDataSet));
        id = -1;
        if( col < 999 ) {
          System.out.println("Setting Colors !!!!");
          int retCol = data.addColor( col );
          double selections[] = data.getSelection();
          for( int j=0; j<data.n; j++) 
            if( selections[j] != 0 )
              data.setColor(j, retCol);
        } else
          data.colorsOff();
      }
    }    
    // Then ordinary update loop
    for( int i=0; i<Plots.size(); i++ )
      if( ((DragBox)Plots.elementAt(i)).frame.isVisible() )  // Plotwindow still exists
        if( ((DragBox)Plots.elementAt(i)).dataFlag )         // This window was already updated 
          ((DragBox)Plots.elementAt(i)).dataFlag = false;
        else
          ((DragBox)Plots.elementAt(i)).dataChanged(id);
      else
        Plots.removeElementAt(i);
  }

  public void Save(boolean selection) {
    checkHistoryBuffer();
    
    FileDialog f;
    if( selection )
      f = new FileDialog(this, "Save Selection", FileDialog.SAVE);
    else
      f = new FileDialog(this, "Save Data", FileDialog.SAVE);
    f.show();
    if (f.getFile() != null )
      saveDataSet(f.getDirectory() + f.getFile(), selection);
  }

  public boolean saveDataSet(String file, boolean selection) {
    try {
      int k=((dataSet)dataSets.elementAt(thisDataSet)).k;
      int n=((dataSet)dataSets.elementAt(thisDataSet)).n;
      
      FileWriter fw = new FileWriter( file );

      double[][] dataCopy = new double[k][n];
      boolean[][] missing = new boolean[k][n];
      dataSet data = ((dataSet)dataSets.elementAt(thisDataSet));
      double[] selected = data.getSelection();
      for( int j=0; j<k; j++ ) {
        missing[j] = data.getMissings(j);
        if( data.categorical(j) && !data.alpha(j) )
          dataCopy[j] = data.getRawNumbers(j);
        else
          dataCopy[j] = data.getNumbers(j);
      }

      String line="";
      NumberFormat nf = NumberFormat.getNumberInstance(new Locale("en", "US"));
      DecimalFormat df = (DecimalFormat)nf;
      df.applyPattern("#.#################"); 

      boolean first = true;
      for( int j=0; j<k; j++)
        if( (varNames.getSelectedIndices().length == 0) || varNames.isSelectedIndex(j) ) {
          line += (first?"":"\t") + data.getName(j);
          first = false;
        }
      fw.write(line+"\r");
        
      for( int i=0; i<n; i++) {
        if( !selection || (selection && selected[i]>0 ) ) {
          line="";
          first = true;
          for( int j=0; j<k; j++)
            if( (varNames.getSelectedIndices().length == 0) || varNames.isSelectedIndex(j) ) {
              if( missing[j][i] )
                line += (first?"":"\t") + "NA";
              else if( data.categorical(j) )
                line += (first?"":"\t") + data.getLevelName(j,  dataCopy[j][i]);
              else
                line += (first?"":"\t") + df.format(dataCopy[j][i]);
              first = false;
            }
          fw.write(line+(i==(n-1)?"":"\r"));
        }
      }
      fw.close();
      
    } catch (Exception ex) {
      System.out.println("Error writing to file: "+ex);
      return false;
    }
    return true;
  }
  
  public void loadDataSet(boolean isDB, File file) {

//    System.out.println(".......... IN loadDataSet("+thisDataSet+") IN .........");

    if( isDB ) {
      loadDataBase();
    } else if( thisDataSet == -1 ) {
      if( loadAsciiFile(file) ) {
        setVarList();
        this.setTitle("Mondrian("+((dataSet)dataSets.elementAt(thisDataSet)).setName+")");               // 
        c.setEnabled(true);
        s.setEnabled(true);
        
        int nom   = ((dataSet)dataSets.elementAt(thisDataSet)).countSelection();
        int denom = ((dataSet)dataSets.elementAt(thisDataSet)).n;
        String Display = nom+"/"+denom+" ("+Stat.roundToString(100*nom/denom,2)+"%)";
        progText.setText(Display);
        progBar.setValue(nom);          
        
        load = false;
        maintainOptionMenu();
      }
    }
    else {
      new Join(Mondrians, dataSets, true , isDB, file);
    }
    if( thisDataSet != -1 )
      ((dataSet)dataSets.elementAt(thisDataSet)).graphicsPerf = graphicsPerf;	
  }
  
  public void setVarList() {
    if( varNames != null ) {
      paint(this.getGraphics());
      return;
    }
    if( thisDataSet == -1 )
      thisDataSet = dataSets.size() - 1;
    final dataSet data = (dataSet)dataSets.elementAt(thisDataSet); 
    String listNames[] = new String[data.k];
    for( int j=0; j<data.k; j++) {
      listNames[j] = " "+data.getName(j);
//      System.out.println("Adding:"+listNames[j]);
    }
    
    varNames = new JList(listNames);
    scrollPane.setViewportView(varNames);
    
    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    
    varNames.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
          int index = varNames.locationToIndex(e.getPoint());
          if( !data.alpha(index) ) {
            if( data.categorical(index) )
              data.catToNum(index);
            else
              data.numToCat(index);
            setVarList();
            maintainPlotMenu();
          }
        } else
        {
          int index = varNames.locationToIndex(e.getPoint());
          System.out.println("Shift "+e.isShiftDown());
          System.out.print("Item Selected: "+index);
          int diff=0;
          if( e.isShiftDown() ) {
            diff = selectBuffer[0] - index;
            diff -= (diff<0?-1:1);
            System.out.println(" diff "+diff);
          }
          for(int j=Math.abs(diff); j>=0; j--) { 
            if( varNames.isSelectedIndex( index ) && index != selectBuffer[0] ) {
              for(int i=data.k-1; i>0; i--)
                selectBuffer[i] = selectBuffer[i-1];
              selectBuffer[0] = index + (j*(diff<0?-1:1));
            }
            if( !varNames.isSelectedIndex( index ) ) {              // Deselection, remove elements from Buffer
              for(int i=0; i<data.k; i++)
                if( selectBuffer[i] == index )
                  for(int k=i; k<data.k-1; k++)
                    selectBuffer[k] = selectBuffer[k+1];
            }
            System.out.println(" History: "+selectBuffer[0]+" "+selectBuffer[1]+" "+selectBuffer[2]+" "+selectBuffer[3]+" "+selectBuffer[4]);
          }
          maintainPlotMenu();
        }
      }
    });

    varNames.addListSelectionListener(new ListSelectionListener() {     
      public void valueChanged(ListSelectionEvent e) { maintainPlotMenu(); }
    });
    
    varNames.setCellRenderer(new MCellRenderer());

    RepaintManager currentManager = RepaintManager.currentManager(varNames);
    currentManager.setDoubleBufferingEnabled(true);    
    
    if( polys.size() > 0 )
      m.setEnabled(true);

    this.setResizable(true);

    this.show();
  }
  
  Driver d;
  Connection con;
  
  public boolean DBConnect(String URL,String  Username,String  Passwd) {
    try {
      // Connect to the database at that URL. 
      //	  URL="jdbc:mysql://137.250.124.51:3306/datasets";
      //      System.out.println("Database trying to connect ...: "+URL+"?user="+Username+"&password="+Passwd);
      con = DriverManager.getConnection(URL, Username, Passwd);
      //      con = DriverManager.getConnection(URL+"?user="+Username+"&password="+Passwd);
      System.out.println("Database Connected");
      return true;
    } catch (Exception ex) {
      System.out.println("Connection Exception: "+ex);
      return false;
    }
  }
  
  public boolean LoadDriver(String Driver) {
    try {
      d = (Driver)Class.forName(Driver).newInstance();
      System.out.println("Driver Registered");
      return true;
    } catch (Exception ex) {
      System.out.println("Driver Exception: "+ex);
      return false;
    }
  }
  
  public void loadDataBase() {
    if( thisDataSet == -1 ) {
      final JFrame DBFrame = new JFrame();
      DBFrame.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) { DBFrame.dispose(); }
      });
      
      DBFrame.setTitle("DB Connection");
      GridBagLayout gbl = new GridBagLayout();
      DBFrame.getContentPane().setLayout(gbl);
      
      GridBagConstraints gbc = new GridBagConstraints();
      gbc.weightx = 20;
      gbc.weighty = 100;
      gbc.fill = GridBagConstraints.NONE;
      gbc.anchor = GridBagConstraints.EAST;
      
      Util.add(DBFrame, new JLabel(" Driver: "), gbc, 0,0,1,1);
      Util.add(DBFrame, new JLabel(" URL: "), gbc, 0,1,1,1);
      Util.add(DBFrame, new JLabel(" User: "), gbc, 0,2,1,1);
      Util.add(DBFrame, new JLabel(" Pwd: "), gbc, 2,2,1,1);
      Util.add(DBFrame, new JLabel(" DB: "), gbc, 0,3,1,1);
      Util.add(DBFrame, new JLabel(" Table: "), gbc, 0,4,1,1);
      
      final JTextField DriverName = new JTextField("org.gjt.mm.mysql.Driver",35);
      final JTextField URL = new JTextField("jdbc:mysql://137.250.124.51:3306/datasets",35);
      final JTextField Username = new JTextField("theusm",16);
      final JPasswordField Passwd = new JPasswordField("",16);
      final Choice DBList = new Choice();
      DBList.addItem("Not Connected");
      DBList.setEnabled(false);
      final Choice tableList = new Choice();
      tableList.addItem("Choose DB");
      tableList.setEnabled(false);
      final JButton Select = new JButton("Select");
      Select.setEnabled(false);
      final JButton Cancel = new JButton("Cancel");
      gbc.fill = GridBagConstraints.BOTH;
      gbc.anchor = GridBagConstraints.CENTER;
      Util.add(DBFrame, DriverName, gbc, 1,0,3,1);
      Util.add(DBFrame, URL, gbc, 1,1,3,1);
      Util.add(DBFrame, Username, gbc, 1,2,1,1);
      Util.add(DBFrame, Passwd, gbc, 3,2,1,1);
      Util.add(DBFrame, DBList, gbc, 1,3,3,1);
      Util.add(DBFrame, tableList, gbc, 1,4,3,1);
      gbc.fill = GridBagConstraints.NONE;
      Util.add(DBFrame, Select, gbc, 1,5,1,1);
      Util.add(DBFrame, Cancel, gbc, 3,5,1,1);
      
      final JButton Load = new JButton("Load");
      DBFrame.getRootPane().setDefaultButton(Load);
      final JButton Connect = new JButton("Connect");
      Connect.setEnabled(false);
      gbc.fill = GridBagConstraints.BOTH;
      gbc.anchor = GridBagConstraints.CENTER;
      Util.add(DBFrame, Load, gbc, 4,0,1,1);
      Util.add(DBFrame, Connect, gbc, 4,2,1,1);
      
      DBFrame.pack();
      DBFrame.show();
      Load.addActionListener(new ActionListener() {     // 
        public void actionPerformed(ActionEvent e) {
          if( LoadDriver(DriverName.getText()) ) {
            Connect.setEnabled(true);
            DBFrame.getRootPane().setDefaultButton(Connect);
          }
        }
      });
      
      Cancel.addActionListener(new ActionListener() {     // 
        public void actionPerformed(ActionEvent e) {
          DBFrame.dispose(); 
        }
      });
      
      Connect.addActionListener(new ActionListener() {     // 
        public void actionPerformed(ActionEvent e) {
          if( DBConnect(URL.getText(), Username.getText(), Passwd.getText()) ) {
            try {
              // Create statement
              Statement stmt = con.createStatement();
              
              // Execute query
              String query = "show databases";
              
              // Obtain the result set
              ResultSet rs = stmt.executeQuery(query);
              
              DBList.removeAll();
              while( rs.next() ) {
                DBList.addItem(rs.getString(1));
              }
              DBList.setEnabled(true);
              
              rs.close();
              
              // Close statement
              stmt.close(); 	    
            } catch (Exception ex) {
              System.out.println("Driver Exception: "+ex);
            }
          }
        }
      });
      
      DBList.addItemListener(new ItemListener() {     // 
        public void itemStateChanged(ItemEvent e) {
          try {
            // Create statement
            Statement stmt = con.createStatement();
            
            // Execute query
            String query = "show tables from "+DBList.getSelectedItem();
            
            // Obtain the result set
            ResultSet rs = stmt.executeQuery(query);
            
            tableList.removeAll();
            while( rs.next() ) {
              tableList.addItem(rs.getString(1));
            }
            tableList.setEnabled(true);
            
            rs.close();
            
            // Close statement
            stmt.close(); 	    
            con.close();                                // disconnect from DB and connect to selected DB
            String url = URL.getText();
            DBConnect(url.substring(0, url.lastIndexOf("/")+1)+DBList.getSelectedItem(), Username.getText(), Passwd.getText());
          } catch (Exception ex) {
            System.out.println("Can't get tables out of DB: "+ex);
          }
        }
      });
      
      tableList.addItemListener(new ItemListener() {     // 
        public void itemStateChanged(ItemEvent e) {
          Select.setEnabled(true);	    
          try {
            // Create statement
            Statement stmt = con.createStatement();
            
            // Execute query
            String query = "show fields from "+tableList.getSelectedItem()+" from "+DBList.getSelectedItem();
            
            // Obtain the result set
            ResultSet rs = stmt.executeQuery(query);
            
            while( rs.next() ) {
              System.out.println(rs.getString(1)+" - "+rs.getString(2));
            }
            
            rs.close();
            
            // Close statement
            stmt.close();
            
            DBFrame.getRootPane().setDefaultButton(Select);
          } catch (Exception ex) {
            System.out.println("Can't retreive columns of table >"+tableList.getSelectedItem()+"<: "+ex);
          }
        }
      });
      
      Select.addActionListener(new ActionListener() {     // 
        public void actionPerformed(ActionEvent e) {
          dataSet data = new dataSet(d, con, DBList.getSelectedItem(), tableList.getSelectedItem());
          dataSets.addElement( data );
          setVarList();
          DBFrame.dispose();
          selseq = true;
          se.setSelected(true);
          se.setEnabled(false);
        }
      });
    }
  }

  boolean loadAsciiFile(File file) {
    
    boolean[] alpha;
    dataSet data;
    String filename = "";
    String path = "";
    
    if( file == null ) {
      FileDialog f = new FileDialog(this, "Load Data", FileDialog.LOAD);
      //      JFileChooser f = new JFileChooser(this, "Load Data", FileDialog.LOAD);
      f.setFile("");
      f.show();
      //System.out.println("->"+f.getDirectory()+"<-.->" + f.getFile());
      if (f.getFile() != null ) { 
        justFile = f.getFile();
        path = f.getDirectory();
        filename = f.getDirectory() + justFile;
      } else
        filename = "";
    } else {
      filename = file.getAbsolutePath();
      justFile = file.getName();
    }
    
    if( !filename.equals("") ) {
      
      String line="";
      
      if( true ) {                                          // new reader
        progBar.setMinimum(0);
        progBar.setMaximum(100);
        data = new dataSet( justFile );
        dataSets.addElement(data);
        progText.setText("Loading ...");
        
        String mapFile = data.turboRead(filename, this);
        if( mapFile.indexOf("ERROR") == 0 ) {
          JOptionPane.showMessageDialog(this, mapFile.substring(mapFile.indexOf(":")+2), "Open File Error", JOptionPane.ERROR_MESSAGE); 
          progText.setText("");
          setProgress(0.0);
          return false;
        }
        progText.setText(""); 
        progBar.setValue(0);
        progBar.setMaximum(data.n);
        
        selectBuffer = new int[data.k+15];
        
        if( !mapFile.equals("") ) {                          // more lines detected -> read the polygon
          try {
            BufferedReader br = new BufferedReader( new FileReader(path + mapFile) );
            br.mark(1000000);
            progText.setText("Polygons ..."); 
            
            double xMin =  10e10;
            double xMax = -10e10;
            double yMin =  10e10;
            double yMax = -10e10;
            
            String tLine = br.readLine();
            try {
              StringTokenizer head = new StringTokenizer(tLine, "\t");
              
              try{
                int      Id = Integer.valueOf(head.nextToken()).intValue();
                String name = head.nextToken();
                int npoints = Integer.valueOf(head.nextToken()).intValue();
                double[]  x = new double[npoints];
                double[]  y = new double[npoints];
                
                for( int i=0; i<npoints; i++ ) {
                  tLine = br.readLine();
                  StringTokenizer coord = new StringTokenizer(tLine);
                  x[i] = Float.valueOf(coord.nextToken()).floatValue();
                  xMin = Math.min(xMin, x[i]);
                  xMax = Math.max(xMax, x[i]);
                  y[i] = Float.valueOf(coord.nextToken()).floatValue();
                  yMin = Math.min(yMin, y[i]);
                  yMax = Math.max(yMax, y[i]);
                }
              }	
              catch(NoSuchElementException e) {System.out.println("Poly Read Error: "+line);}
            }
            catch( IOException e ) {
              System.out.println("Error: "+e);
              System.exit(1);
            }
            
            br.reset();
            int count = 0;
            while( line != null ) {
              MyPoly p = new MyPoly();
              p.read(br, xMin, 100000/Math.min(xMax-xMin, yMax-yMin), yMin, 100000/Math.min(xMax-xMin, yMax-yMin));
              if( count++ % (int)(Math.max(data.n/20, 1)) == 0 )
                progBar.setValue(Math.min(count, data.n));
              polys.addElement(p);
              line = br.readLine();                          // Read seperator (single blank line)
            }
          }
          catch( IOException e ) {
            System.out.println("Error: "+e);
            System.exit(1);
          }
        }
        return true;
      } else {                                               // old reader
        try {
          BufferedReader br = new BufferedReader( new FileReader(filename) );
          data = new dataSet( justFile );
          dataSets.addElement(data);
          progText.setText("Peeking ...");
          alpha = data.sniff(br);
          progBar.setMaximum(data.n);
          br = new BufferedReader( new FileReader(filename) );
          progText.setText("Loading ...");
          data.read(br, alpha, progBar);
          
          br.mark(1000000);
          line = br.readLine();
          
          while( line != null && (line.trim()).equals("") ) {       // skip empty lines
            br.mark(1000000);
            line = br.readLine();
          }
          
          if( line != null ) {                          // more lines detected -> read the polygon
            
            progText.setText("Polygons ..."); 
            
            //====================== Check Scaling of the Polygon ===============================//
            String tLine;
            
            double xMin =  10e10;
            double xMax = -10e10;
            double yMin =  10e10;
            double yMax = -10e10;
            
            try {
              tLine = line;
              
              StringTokenizer head = new StringTokenizer(tLine, "\t");
              
              try{
                int      Id = Integer.valueOf(head.nextToken()).intValue();
                String name = head.nextToken();
                int npoints = Integer.valueOf(head.nextToken()).intValue();
                double[] x = new double[npoints];
                double[] y = new double[npoints];
                
                for( int i=0; i<npoints; i++ ) {
                  tLine = br.readLine();
                  StringTokenizer coord = new StringTokenizer(tLine);
                  x[i] = Float.valueOf(coord.nextToken()).floatValue();
                  xMin = Math.min(xMin, x[i]);
                  xMax = Math.max(xMax, x[i]);
                  y[i] = Float.valueOf(coord.nextToken()).floatValue();
                  yMin = Math.min(yMin, y[i]);
                  yMax = Math.max(yMax, y[i]);
                }
                //                  System.out.println("Read: "+npoints+" Points - xMin: "+xMin+"xMax: "+xMax+"yMin: "+yMin+"yMax: "+yMax);
              }	
              catch(NoSuchElementException e) {System.out.println("Poly Read Error: "+line);}
            }
            catch( IOException e ) {
              System.out.println("Error: "+e);
              System.exit(1);
            }
            //==================================================================//
            
            br.reset();
            int count = 0;
            while( line != null ) {
              MyPoly p = new MyPoly();
              p.read(br, xMin, 100000/Math.min(xMax-xMin, yMax-yMin), yMin, 100000/Math.min(xMax-xMin, yMax-yMin));
              if( count++ % (int)(Math.max(data.n/20, 1)) == 0 )
                progBar.setValue(Math.min(count, data.n));
              //MyPoly newP = p.thinHard();
              polys.addElement(p);
              line = br.readLine();                          // Read seperator (single blank line)
            }
          }
        }
        
        catch( IOException e ) {
          System.out.println("Error: "+e);
          System.exit(1);
        }
        progText.setText(""); 
        progBar.setValue(0);
        
        return true;
      }
    } else
      return false;
  }

  public void setProgress(double progress) {
    progBar.setValue((int)(100*progress));
  }
  
/*  public void handleReOpenApplication(ApplicationEvent event) {}
  
  public void handleQuit(ApplicationEvent event) {}
  
  public void handlePrintFile(ApplicationEvent event) {} */

  public int[] getWeightVariable(int[] vars, dataSet data) {

    if( numCategorical == (vars).length - 1 ) {
      int[] returner = new int[vars.length];
      System.arraycopy(vars,0,returner,0,returner.length);
      for( int i=0; i<returner.length-1; i++ ) {
        if( vars[i] == weightIndex ) {
          for(int j=i; j<returner.length-1; j++)
            returner[j] = vars[j+1];
          returner[returner.length-1] = weightIndex;
          i = returner.length;
        } else
          returner[i] = vars[i];
      }
      for( int i=0; i<returner.length; i++ ) {
        System.out.println("ind old = "+vars[i]+" ind new = "+returner[i]);
      }
      return returner ;
    } else {
      final Dialog countDialog = new Dialog(this, " Choose Weight Variable", true);
      Choice getCount = new Choice();
      for( int j=0; j<vars.length; j++ ) {
        if( data.getName(vars[j]).length()>1 && data.getName(vars[j]).substring(0,1).equals("/") )
          getCount.addItem(data.getName(vars[j]).substring(2));
        else
          getCount.addItem(data.getName(vars[j]));
      }
      for( int j=0; j<getCount.getItemCount(); j++ )
        if( getCount.getItem(j).toLowerCase().equals("count")    ||
            getCount.getItem(j).toLowerCase().equals("counts")   ||
            getCount.getItem(j).toLowerCase().equals("n")        ||
            getCount.getItem(j).toLowerCase().equals("weight")   ||
            getCount.getItem(j).toLowerCase().equals("observed") ||
            getCount.getItem(j).toLowerCase().equals("number") )
          getCount.select(j);
      Panel p1 = new Panel();
      p1.add(getCount);
      countDialog.add(p1, "Center");
      Button OK = new Button("OK");
      Panel p2 = new Panel();
      p2.add(OK);
      countDialog.add(p2, "South");
      OK.addActionListener(new ActionListener() {     //
        public void actionPerformed(ActionEvent e) {
          countDialog.dispose();
        }
      });
      countDialog.pack();
      if( countDialog.getWidth() < 240 )
        countDialog.setSize(240, countDialog.getHeight());
      countDialog.setResizable(false);
      countDialog.setModal(true);
      countDialog.setBounds(this.getBounds().x+this.getBounds().width/2-countDialog.getBounds().width/2,
                            this.getBounds().y+this.getBounds().height/2-countDialog.getBounds().height/2,
                            countDialog.getBounds().width,
                            countDialog.getBounds().height);
      countDialog.show();

      String[] selecteds = new String[(varNames.getSelectedValues()).length];
      for( int i=0; i < (varNames.getSelectedValues()).length; i++)
        selecteds[i] = (String)(varNames.getSelectedValues())[i];
      int[] selected = vars;
      int[] returner = new int[selected.length];
      for( int i=0; i<selected.length; i++) {
        if( (selecteds[i].trim()).equals(getCount.getSelectedItem()) ) {
          returner[selected.length-1] = selected[i];
          for( int j=i; j<selected.length-1; j++ )
            returner[j] = selected[j+1];
          i = selected.length;
        } else
          returner[i] = selected[i];
      }
      return returner;
    }
  }
  
  public void modelNavigator() {
    if( Mn == null )
      Mn = new ModelNavigator();
    else
      Mn.show();
  }
  
  public void preferenceFrame() {
    PreferencesFrame.showPrefsDialog(this);
  }
  
  public void test() {
    checkHistoryBuffer();
    
    int p = (varNames.getSelectedIndices()).length;
    dataSet tempData = ((dataSet)dataSets.elementAt(thisDataSet));
    final MFrame scatterMf = new MFrame(this);
    int dims = Math.min(200*p,(Toolkit.getDefaultToolkit().getScreenSize()).height);
    scatterMf.setSize(dims-20,dims);
    scatterMf.getContentPane().setLayout(new GridLayout(p-1,p-1));

    for(int i=0; i<(p-1); i++)
      for(int j=1; j<p; j++) {
        if( i>=j ) {
          JPanel Filler = new JPanel();
          Filler.setBackground(scatterMf.backgroundColor);
          scatterMf.getContentPane().add(Filler);
//          (Filler.getGraphics()).drawString("text",10,10);
        }
        else {
          int[] tmpVars = new int[2];
//          tmpVars[0] = varNames.getSelectedIndices()[j];
//          tmpVars[1] = varNames.getSelectedIndices()[i];
          tmpVars[0] = selectBuffer[p-j-1];
          tmpVars[1] = selectBuffer[p-i-1];
          //
          Scatter2D scat = new Scatter2D(scatterMf, 200, 200, (dataSet)dataSets.elementAt(thisDataSet), tmpVars, varNames, true);
          scat.addSelectionListener(this);
          scat.addDataListener(this);
          Plots.addElement(scat);
        }
    }
    scatterMf.setLocation(300, 0);
    scatterMf.setTitle("Scatterplot Matrix");
    scatterMf.show();
  }
  
  public void pc(String mode) {
    checkHistoryBuffer();
    
    final MFrame pC = new MFrame(this);
    
    int totWidth = (Toolkit.getDefaultToolkit().getScreenSize()).width;
    int tmpWidth = 50 * (1 + (varNames.getSelectedIndices()).length);
    if( tmpWidth > totWidth)
      if( 20 * (1 + (varNames.getSelectedIndices()).length) < totWidth )
        tmpWidth = totWidth;
      else
        tmpWidth = 20 * (1 + (varNames.getSelectedIndices()).length);
      
    pC.setSize(tmpWidth, 400);
    pC.setLocation(300, 0);
    
    int k=(varNames.getSelectedIndices()).length;
    int[] passBuffer = new int[k];
    for(int i=0; i<k; i++) 
      passBuffer[i] = selectBuffer[k-i-1];
    
    PC plotw = new PC(pC, (dataSet)dataSets.elementAt(thisDataSet), passBuffer, mode, varNames);
    plotw.addSelectionListener(this);
    plotw.addDataListener(this);
    Plots.addElement(plotw);
    pC.getContentPane().add(plotw);
    pC.show();
  }
  
  public void missPlot() {
    checkHistoryBuffer();
    
    final MFrame mV = new MFrame(this);
    int k=0;
    for( int i=0;i<(varNames.getSelectedIndices()).length;i++)
      if( ((dataSet)dataSets.elementAt(thisDataSet)).n > ((dataSet)dataSets.elementAt(thisDataSet)).getN( (varNames.getSelectedIndices())[i] ) )
        k++;
    int[] passVars = new int[k];
    int kk=0;
    for( int i=0;i<(varNames.getSelectedIndices()).length;i++)
      if( ((dataSet)dataSets.elementAt(thisDataSet)).n > ((dataSet)dataSets.elementAt(thisDataSet)).getN( selectBuffer[i] ) )
        passVars[k-1-kk++] = selectBuffer[i]; //(varNames.getSelectedIndices())[i];
    
    if( k > 0 ) {
      int totHeight = (Toolkit.getDefaultToolkit().getScreenSize()).height;
      int tmpHeight = 35 * (1 + k)+15;
      if( tmpHeight > totHeight)
        if( 20 * (1 + k) < totHeight )
          tmpHeight = totHeight;
        else
          tmpHeight = 20 * (1 + k);
      
      mV.setSize(300, Math.min(tmpHeight, (Toolkit.getDefaultToolkit().getScreenSize()).height-30));
      mV.setLocation(150, 150);
      
      MissPlot plotw = new MissPlot(mV, (dataSet)dataSets.elementAt(thisDataSet), passVars);
      plotw.setScrollX();
      plotw.addSelectionListener(this);
      plotw.addDataListener(this);
      Plots.addElement(plotw);
      mV.show();
    } else 
      JOptionPane.showMessageDialog(this, "Non of the selected variables\ninclude any missing values"); 
  }
  
  public void weightedMosaicPlot() {
    checkHistoryBuffer();
    
    final MFrame mondrian = new MFrame(this);
    mondrian.setSize(400,400);
    
    dataSet data = (dataSet)dataSets.elementAt(thisDataSet);
    
    int k=(varNames.getSelectedIndices()).length;
    int[] passBuffer = new int[k];
    for(int i=0; i<k; i++) 
      passBuffer[i] = selectBuffer[k-i-1];

//    int[] vars = getWeightVariable(varNames.getSelectedIndices(), data);
    int[] vars = getWeightVariable(passBuffer, data);
    int[] passed = new int[vars.length-1];
    System.arraycopy(vars,0,passed,0,vars.length-1);
    int weight = vars[vars.length-1];
    Table breakdown = data.breakDown(data.setName, passed, weight);
    for( int i=0; i<passed.length-1; i++ )
      breakdown.addInteraction( new int[] { i }, false );
    breakdown.addInteraction( new int[] { passed.length-1 } , true  );
    final Mosaic plotw = new Mosaic(mondrian, 400, 400, breakdown);
    plotw.addSelectionListener(this);
    plotw.addDataListener(this);
    Plots.addElement(plotw);
//    mondrian.getContentPane().add(plotw);                      // Add it
    mondrian.setLocation(300, 0);
    mondrian.show();
    
    if( Mn == null )	
      Mn = new ModelNavigator();
    plotw.addModelListener(Mn);    
    mn.setEnabled(true);
  }
  
  public void mosaicPlot() {
    checkHistoryBuffer();
    
    final MFrame mondrian = new MFrame(this);
    mondrian.setSize(400,400);
    
    dataSet tempData = ((dataSet)dataSets.elementAt(thisDataSet));
    
    int k=(varNames.getSelectedIndices()).length;
    int[] passBuffer = new int[k];
    for(int i=0; i<k; i++) 
      passBuffer[i] = selectBuffer[k-i-1];

    Table breakdown = tempData.breakDown(tempData.setName, passBuffer, -1);
    for( int i=0; i<(varNames.getSelectedIndices()).length-1; i++ ) {
      breakdown.addInteraction( new int[] { i }, false );
    }
    breakdown.addInteraction( new int[] { (varNames.getSelectedIndices()).length-1 } , true  );
    
    final Mosaic plotw = new Mosaic(mondrian, 400, 400, breakdown);
    plotw.addSelectionListener(this);
    plotw.addDataListener(this);
    Plots.addElement(plotw);
//    mondrian.getContentPane().add(plotw);                      // Add it
    mondrian.setLocation(300, 0);
    mondrian.show();
    
    mondrian.addWindowListener(new WindowAdapter() {
      public void windowActivated(WindowEvent e) {  plotw.processWindowEvent(e); }
    });
    
    if( Mn == null )
      Mn = new ModelNavigator();
    plotw.addModelListener(Mn);
    mn.setEnabled(true); 
  }
  
  public void barChart() {
    
    dataSet tempData = ((dataSet)dataSets.elementAt(thisDataSet));
    
    int[] indices = varNames.getSelectedIndices();
    int lastY = 333;
    int col=0;
    for(int i=0; i<indices.length; i++) {
      final MFrame bars = new MFrame(this);
      
      int[] dummy = {0};
      dummy[0] = indices[i];
      
      Table breakdown = tempData.breakDown(tempData.setName, dummy, -1);
      
      int totHeight = (Toolkit.getDefaultToolkit().getScreenSize()).height;
      int tmpHeight = Math.min(totHeight-30, 60 + breakdown.levels[0] * 30);
      
      bars.setSize(300, tmpHeight);
      final Barchart plotw = new Barchart(bars, 300, tmpHeight, breakdown);

      plotw.addSelectionListener(this);
      plotw.addDataListener(this);
      Plots.addElement(plotw);
      if( lastY + bars.getHeight() > (Toolkit.getDefaultToolkit().getScreenSize()).height ) {
        col += 1;
        lastY = 0;
      }
      if( 300*col > (Toolkit.getDefaultToolkit().getScreenSize()).width - 50 ) {
        col = 0;
        lastY = 353;
      }
      bars.setLocation(300*col, lastY);
        
      bars.show();
      if( lastY==0 )
        lastY += bars.getY();
      lastY += bars.getHeight();
    }
  }
  
  public void weightedbarChart() {
    
    dataSet tempData = ((dataSet)dataSets.elementAt(thisDataSet));
    
    int[] vars = getWeightVariable(varNames.getSelectedIndices(), tempData);
    int[] passed = new int[vars.length-1];
    System.arraycopy(vars,0,passed,0,vars.length-1);
    int weight = vars[vars.length-1];
    int lastY = 333;
    int col=0;
    
    for(int i=0; i<passed.length; i++) {
      final MFrame bars = new MFrame(this);
      
      int[] dummy = {0};
      dummy[0] = passed[i];
      Table breakdown = tempData.breakDown(tempData.setName, dummy, weight);
      
      int totHeight = (Toolkit.getDefaultToolkit().getScreenSize()).height;
      int tmpHeight = Math.min(totHeight-20, 60 + breakdown.levels[0] * 30);
      
      bars.setSize(300, tmpHeight);
      final Barchart plotw = new Barchart(bars, 300, tmpHeight, breakdown);

      plotw.addSelectionListener(this);
      plotw.addDataListener(this);
      Plots.addElement(plotw);
      if( lastY + bars.getHeight() > (Toolkit.getDefaultToolkit().getScreenSize()).height ) {
        col += 1;
        lastY = 0;
      }
      if( 300*col > (Toolkit.getDefaultToolkit().getScreenSize()).width - 50 ) {
        col = 0;
        lastY = 333;
      }
      bars.setLocation(300*col, lastY);

      bars.show();
      if( lastY==0 )
        lastY += bars.getY();
      lastY += bars.getHeight();
    }
  }


  public void weightedHistogram() {

    dataSet tempData = ((dataSet)dataSets.elementAt(thisDataSet));

    int[] vars = getWeightVariable(varNames.getSelectedIndices(), tempData);
    if( vars.length > 1 ) {
      int[] passed = new int[vars.length-1];
      System.arraycopy(vars,0,passed,0,vars.length-1);
      int weight = vars[vars.length-1];

//      System.out.println(passed[0]+", "+weight);
      
      histoCore(tempData, passed, weight);
    } else
      histoCore(tempData, vars, vars[0]);
  }


  public void histogram() {
    
    dataSet tempData = ((dataSet)dataSets.elementAt(thisDataSet));
    int[] indices = varNames.getSelectedIndices();

    histoCore(tempData, indices, -1);
  }

  public void histoCore(dataSet tempData, int[] indices, int weight) {
    int lastX = 310, oldX = 0;
    int row=0;
    int menuOffset=0, xOff=0;

    for(int i=0; i<indices.length; i++) {
      final MFrame hists = new MFrame(this);
      
      int dummy = 0;
      dummy = indices[i];
      double start = tempData.getMin(dummy);
      double width = (tempData.getMax(dummy) - tempData.getMin(dummy)) / 8.9;
      Table discrete = tempData.discretize(tempData.setName, dummy, start, width, weight);

      hists.setSize(310, 250);
      final Histogram plotw = new Histogram(hists, 250, 310, discrete, start, width, weight);
      
      plotw.addSelectionListener(this);
      plotw.addDataListener(this);
      Plots.addElement(plotw);
      if( lastX + hists.getWidth() > (Toolkit.getDefaultToolkit().getScreenSize()).width +50 ) {   	// new Row
        row += 1;
        lastX = oldX % 310;
      }
      if( 250*row > (Toolkit.getDefaultToolkit().getScreenSize()).height - 125 ) {									// new Page
        row = 0;
        lastX = 310+xOff;
        xOff += menuOffset;
      }
      hists.setLocation(lastX, xOff+250*row);
      lastX += hists.getWidth();
      oldX = lastX;
      
      hists.show();
      if( i==0 ) {
        menuOffset = hists.getY();
        xOff = menuOffset;
      }
    }
  }
  
  public void mapPlot() {
    final MFrame mapf = new MFrame(this);
    mapf.setSize(400,400);
    mapf.setTitle("Map");

    Map map = new Map(mapf, 400, 400, (dataSet)dataSets.elementAt(thisDataSet), polys, varNames);
    map.addSelectionListener(this);
    map.addDataListener(this);
    Plots.addElement(map);

    if( map.ratio > 1 )
      mapf.setSize((int)(350 * map.ratio), 350 + 56);
    else
      mapf.setSize(350, (int)(350 / map.ratio) + 56);
    mapf.setLocation(0, 333);
      
    mapf.show();
  }
  
  public void scatterplot2D() {
    checkHistoryBuffer();
    
    final MFrame scatterf = new MFrame(this);
    scatterf.setSize(400,400);
    
    int[] passBuffer = new int[2];
    passBuffer[0] = selectBuffer[1];
    passBuffer[1] = selectBuffer[0];
    Scatter2D scat = new Scatter2D(scatterf, 400, 400, (dataSet)dataSets.elementAt(thisDataSet), passBuffer, varNames, false);
    scat.addSelectionListener(this);
    scat.addDataListener(this);
    Plots.addElement(scat);
    scatterf.setLocation(300, 333);
    scatterf.show();
  }
  
  public void mds() {
    
    int[] varsT = varNames.getSelectedIndices();
    dataSet dataT = (dataSet)dataSets.elementAt(thisDataSet);
    try {
      Rconnection c = new Rconnection();
      c.voidEval("library(MASS)");
      for( int i=0; i<varsT.length; i++ ) {
        c.assign("x",dataT.getRawNumbers(varsT[i]));
        if( dataT.n > dataT.getN(varsT[i]) ) {                      // Check for missings in this variable
          boolean[] missy = dataT.getMissings(varsT[i]);
          int[] flag = new int[dataT.n];
          for( int j=0; j<dataT.n; j++ )
            if( missy[j] )
              flag[j] = 1;
            else
              flag[j] = 0;
          c.assign("xM",flag);
          c.voidEval("is.na(x)[xM==1] <- T");
        }
        if( i==0 )
          c.voidEval("tempData <- x");
        else
          c.voidEval("tempData <- cbind(tempData, x)");
      }
      c.voidEval("tempD <- dist(scale(tempData))");
      c.voidEval("is.na(tempD)[tempD==0] <- T");
      RList mdsL = c.eval("sMds <- sammon(tempD, y=cmdscale(dist(scale(tempData)), k=2), k=2)").asList();
      double[] x1 = c.eval("sMds$points[,1]").asDoubleArray();
      double[] x2 = c.eval("sMds$points[,2]").asDoubleArray();      
      
      dataT.addVariable("mds1", false, false, x1, new boolean[dataT.n]);
      dataT.addVariable("mds2", false, false, x2, new boolean[dataT.n]);
      
      final MFrame scatterf = new MFrame(this);
      scatterf.setSize(400,400);
      scatterf.setTitle("Scatterplot 2D");
      
      Scatter2D scat = new Scatter2D(scatterf, 400, 400, dataT, new int[] {dataT.k-2,dataT.k-1}, varNames, false);
      scat.addSelectionListener(this);
      Plots.addElement(scat);
      scatterf.setLocation(300, 333);
      scatterf.show();
    } catch(RSrvException rse) {JOptionPane.showMessageDialog(this, "Calculation of MDS failed");}
  }
  
  public void pca() {
    
    int[] varsT = varNames.getSelectedIndices();
    dataSet dataT = (dataSet)dataSets.elementAt(thisDataSet);
    try {
      Rconnection c = new Rconnection();
      String call=" ~ x1 ";
      for( int i=0; i<varsT.length; i++ ) {
        c.assign("x",dataT.getRawNumbers(varsT[i]));
        if( dataT.n > dataT.getN(varsT[i]) ) {                      // Check for missings in this variable
          boolean[] missy = dataT.getMissings(varsT[i]);
          int[] flag = new int[dataT.n];
          for( int j=0; j<dataT.n; j++ )
            if( missy[j] )
              flag[j] = 1;
            else
              flag[j] = 0;
          c.assign("xM",flag);
          c.voidEval("is.na(x)[xM==1] <- T");
        }
        if( i==0 ) 
          c.voidEval("tempData <- x");
        else {
          c.voidEval("tempData <- cbind(tempData, x)");
          call+=" + x"+(i+1)+"";
        }
      }
      c.voidEval("tempData <- data.frame(tempData)");

      for( int i=0; i<varsT.length; i++ )
        c.voidEval("names(tempData)["+(i+1)+"] <- \"x"+(i+1)+"\"");
 
      String opt = "TRUE";
      int answer = JOptionPane.showConfirmDialog(this, "Calculate PCA for correlation matrix","Standardize Data?",JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE  );
      if (answer == JOptionPane.NO_OPTION)
        opt = "FALSE";
      
      c.voidEval("pca <- predict(princomp("+call+" , data = tempData, cor = "+opt+", na.action = na.exclude))");
      for( int i=0; i<varsT.length; i++ ) {
        double[] x = c.eval("pca[,"+(i+1)+"]").asDoubleArray();
        boolean missy[] = new boolean[dataT.n];
        for(int j=0; j<x.length; j++) {
          if( Double.isNaN(x[j]) ) {
            missy[j] = true;
            x[j] = Double.MAX_VALUE;
          } else
            missy[j] = false;
        }
        dataT.addVariable("pca "+(i+1)+"", false, false, x, missy);
      }
      varNames = null;
      setVarList();
    } catch(RSrvException rse) {JOptionPane.showMessageDialog(this, "Calculation of PCA failed");System.out.println(rse);}
  }
  
  public void switchVariableMode(){
    for(int i=0; i<varNames.getSelectedIndices().length; i++) {
      int index=(varNames.getSelectedIndices())[i];
      dataSet data = (dataSet)dataSets.elementAt(thisDataSet);
      if( !data.alpha(index) ) {
        if( data.categorical(index) )
          data.catToNum(index);
        else
          data.numToCat(index);
      }
    }
    setVarList();
    maintainPlotMenu();
  }
  
  public void getSelectedTypes() {
    numCategorical = 0;
    for( int i=0; i<varNames.getSelectedIndices().length; i++ ) {
      if( ((dataSet)dataSets.elementAt(thisDataSet)).categorical(varNames.getSelectedIndices()[i]) )
        numCategorical++;
      else
        weightIndex = varNames.getSelectedIndices()[i];
    }
  }
  
  public void checkHistoryBuffer() {

    int k = (varNames.getSelectedIndices()).length;
    boolean error = false;
    boolean[] check = new boolean[k];
    for( int i=0; i<k; i++ )
      check[i] = false;
/*    for( int i=0; i<k; i++ )
      System.out.print(selectBuffer[i]+", ");
    System.out.println("");
    for( int i=0; i<k; i++ )
      System.out.print(varNames.getSelectedIndices()[i]+", ");
    System.out.println("");
*/    
    for( int i=0; i<k; i++ ) {
      int match = selectBuffer[i];
      for( int j=0; j<k; j++ )
        if( varNames.getSelectedIndices()[j] == match )
          if( check[j] ) 
            error = true;
          else
            check[j] = true;
    }          
    for( int i=0; i<k; i++ )
      if( !check[i] )
        error = true;
    
    if( error ) {
      System.out.println(" Error in Selection History "+k);          
      for( int i=0; i<k; i++ )
        selectBuffer[k-i-1] = varNames.getSelectedIndices()[i];
    }
  }
  
  public void maintainPlotMenu() {
    
    getSelectedTypes();
    
//    System.out.println("number categorical: "+numCategorical+", weight Index "+weightIndex);
    
    switch( (varNames.getSelectedIndices()).length ) {
      case 0:
        n.setEnabled(false);
        b.setEnabled(false);
        bw.setEnabled(false);
        nw.setEnabled(false);
        hi.setEnabled(false);
        hiw.setEnabled(false);
        pc.setEnabled(false);
        pb.setEnabled(false);
        //              sc.setEnabled(false);
        sc2.setEnabled(false);
        mds.setEnabled(false);
        pca.setEnabled(false);
        mv.setEnabled(false);
        t.setEnabled(false);
        break;
      case 1:
        if( numCategorical == (varNames.getSelectedIndices()).length ) {
          b.setEnabled(true);
          hi.setEnabled(false);
          hiw.setEnabled(false);
          pb.setEnabled(false);
        }
        else {
          b.setEnabled(false);
          hi.setEnabled(true);
          hiw.setEnabled(true);
          pb.setEnabled(true);
        }
        mv.setEnabled(true);
        n.setEnabled(false);
        bw.setEnabled(false);
        nw.setEnabled(false);
        pc.setEnabled(false);
        byx.setEnabled(false);
        //              sc.setEnabled(false);
        sc2.setEnabled(false);
        mds.setEnabled(false);
        pca.setEnabled(false);
        t.setEnabled(false);
        break;
      case 2: 
        pc.setEnabled(true);
        sc2.setEnabled(true);
        t.setEnabled(true);
        mv.setEnabled(true);
        mds.setEnabled(false);
        pb.setEnabled(true);
        byx.setEnabled(false);
        if( numCategorical == (varNames.getSelectedIndices()).length ) {
          b.setEnabled(true);
          n.setEnabled(true);
        } else {
          b.setEnabled(false);
          n.setEnabled(false);
        }
        if( numCategorical == 1 ) {
          bw.setEnabled(true);
          nw.setEnabled(true);
          pb.setEnabled(false);
          byx.setEnabled(true);
        } else {
          bw.setEnabled(false);
          nw.setEnabled(false);
        }
        if( numCategorical == 0 ) {
          hi.setEnabled(true);
          hiw.setEnabled(true);
          pca.setEnabled(true);   
        } else {
          hi.setEnabled(false);
          hiw.setEnabled(false);
        }
        break;
      default:
        if( numCategorical == (varNames.getSelectedIndices()).length ) {
          b.setEnabled(true);
          n.setEnabled(true);
        } else {
          b.setEnabled(false);
          n.setEnabled(false);
        }
        if( numCategorical == (varNames.getSelectedIndices()).length - 1 ) {
          bw.setEnabled(true);
          nw.setEnabled(true);
        } else {
          bw.setEnabled(false);
          nw.setEnabled(false);
        }
        if( numCategorical == 0 ) {
          hi.setEnabled(true);
          hiw.setEnabled(true);
        } else {
          hi.setEnabled(false);
          hiw.setEnabled(false);
        }
        if( (varNames.getSelectedIndices()).length - numCategorical > 1 && hasR )
          pca.setEnabled(true);   
        if( (varNames.getSelectedIndices()).length - numCategorical > 2 && hasR )
          mds.setEnabled(true);   
        pc.setEnabled(true);      
        pb.setEnabled(true);
        mv.setEnabled(true);
        t.setEnabled(true);
        sc2.setEnabled(false);
        //        sc.setEnabled(false);
    }
    if( !((dataSet)dataSets.elementAt(thisDataSet)).hasMissings )
      mv.setEnabled(false);
    
    // Now handle transform Menue
    int alphs = 0;
    dataSet data = ((dataSet)dataSets.elementAt(thisDataSet));
    for( int i=0; i<varNames.getSelectedIndices().length; i++ )
      if( data.alpha(varNames.getSelectedIndices()[i]) )
        alphs++;
    if( alphs == 0 && (varNames.getSelectedIndices().length == 2 || varNames.getSelectedIndices().length == 1) ) {
      trans.setEnabled(true);
      if( alphs == 0 && varNames.getSelectedIndices().length == 2 ) {
       transPlus.setText(data.getName(selectBuffer[1])+" + "+data.getName(selectBuffer[0]));
      transMinus.setText(data.getName(selectBuffer[1])+" - "+data.getName(selectBuffer[0]));
      transTimes.setText(data.getName(selectBuffer[1])+" * "+data.getName(selectBuffer[0]));
        transDiv.setText(data.getName(selectBuffer[1])+" / "+data.getName(selectBuffer[0]));
        transNeg.setText("-x");
        transInv.setText("1/x");
        transLog.setText("log(x)");
        transExp.setText("exp(x)");
        transPlus.setEnabled(true);
        transMinus.setEnabled(true);
        transTimes.setEnabled(true);
        transDiv.setEnabled(true);
        transNeg.setEnabled(false);
        transInv.setEnabled(false);
        transLog.setEnabled(false);
        transExp.setEnabled(false);
      } else { 
        transNeg.setText("-"+data.getName(selectBuffer[0]));
        transInv.setText("1/"+data.getName(selectBuffer[0]));
        transLog.setText("log("+data.getName(selectBuffer[0])+")");
        transExp.setText("exp("+data.getName(selectBuffer[0])+")");
        transPlus.setText("x + y");
        transMinus.setText("x - y");
        transTimes.setText("x * y");
        transDiv.setText("x / y");
        transPlus.setEnabled(false);
        transMinus.setEnabled(false);
        transTimes.setEnabled(false);
        transDiv.setEnabled(false);
        transNeg.setEnabled(true);
        transInv.setEnabled(true);
        transLog.setEnabled(true);
        transExp.setEnabled(true);
      }
    } else 
      trans.setEnabled(false);
  }

  
  public void maintainOptionMenu() {
    dataSet data = ((dataSet)dataSets.elementAt(thisDataSet));

    // Selection
    if( data.countSelection() == 0 )
      fs.setEnabled(false);
    else
      fs.setEnabled(true);
    // Colors
    if( data.colorBrush )
      fc.setEnabled(true);
    else
      fc.setEnabled(false);
    
    boolean mode = DragBox.extSelMode;
    os.setSelected(mode);
    as.setSelected(!mode);
  }
  
  public void maintainWindowMenu(boolean preserve) {
    for( int i=0; i<Plots.size(); i++ ) 
        ((MFrame)(((DragBox)Plots.elementAt(i)).frame)).maintainMenu(preserve);
  }
  
  public void topWindow() {
    if( ((System.getProperty("os.name")).toLowerCase()).indexOf("mac") > -1 )
      this.setJMenuBar(menubar);                 // Add it to the frame.
  }
  
  public void handleOpenFile( File inFile )
  {
    //  handleOpenFile does not get an Event if a file is dropped on a non-running Mondrian.app, so we need to get it here, but only in this singular situation!
    //
//    System.out.println(".......... CALL loadDataSet("+inFile+") FROM handleOpenFile IN Join .........");
    if( mondrianRunning )
      return;
    while( !mondrianRunning ) 
      System.out.println(" wait for Mondrian to initialize ...");   // Wait until Mondrian initialized
    if( !dataSets.isEmpty() )
      return;
    loadDataSet( false, inFile );
  }
    
  class MCellRenderer extends JLabel implements ListCellRenderer {

    final dataSet data = (dataSet)dataSets.elementAt(thisDataSet); 

    final ImageIcon alphaIcon = new ImageIcon(readGif("alpha.gif"));
    final Icon catIcon = new ImageIcon(readGif("cat.gif"));
    final Icon numIcon = new ImageIcon(readGif("num.gif"));
    // This is the only method defined by ListCellRenderer.
    // We just reconfigure the JLabel each time we're called.

    public Component getListCellRendererComponent(JList list,
                                                  Object value,            // value to display
                                                  int index,               // cell index
                                                  boolean isSelected,      // is the cell selected
                                                  boolean cellHasFocus)    // the list and the cell have the focus
    {
      String s = value.toString();
      setText(s);
      if( data.alpha(index) )
        setIcon(alphaIcon);
      else if( data.categorical(index) )
        setIcon(catIcon);
      else
        setIcon(numIcon);
    
      if (isSelected) {
        setBackground(list.getSelectionBackground());
        setForeground(list.getSelectionForeground());
      }
      else {
        setBackground(list.getBackground());
        setForeground(list.getForeground());
      }
      setEnabled(list.isEnabled());
      setFont(list.getFont());
      setOpaque(true);
      return this;
    }
  }
}