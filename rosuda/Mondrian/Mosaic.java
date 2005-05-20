import java.awt.*;               // ScrollPane, PopupMenu, MenuShortcut, etc.
import java.awt.image.*;         
import java.awt.event.*;         // New event model.
import java.util.*;              // For StingTokenizer.
import java.util.Vector;         // To store the scribble in.
import java.lang.*;              // 
import javax.swing.*;
import javax.swing.event.*;

public class Mosaic extends DragBox implements ActionListener {
  private Vector rects = new Vector(256,256);  // Store the tiles.
  private Vector Labels = new Vector(16,16);   // Store the labels.
  private int width, height;                   // The preferred size.
  private Table tablep;                        // The datatable to deal with.
  public String displayMode = "Observed";
  private double residSum;
  private int censor = 0;
  private int border = 20;
  private Image bi;
  private Graphics2D bg;

  /** This constructor requires a Frame and a desired size */
  public Mosaic(JFrame frame, int width, int height, Table tablep) {
    super(frame);
    this.tablep = tablep;
    this.width = width;
    this.height = height;
    this.k = tablep.k;
    this.names = tablep.names;

    maxLevel = tablep.k;

    Dirs = new char[tablep.k];
    for (int i=0; i<tablep.k; i++ ) {
      if( (i % 2) == 0 ) 
        Dirs[i] = 'x';
      else
        Dirs[i] = 'y';
    }

    String titletext = "Mosaic(";

    for (int i=0; i<k ; i++) {
      if( i < k-1 && i != maxLevel-1 )
        titletext += names[i]+", ";
      else
        titletext += names[i];
      if( i+1 == maxLevel && maxLevel < k )
        titletext += ")[";
    }
    if ( maxLevel < k )
      titletext += "]";
    else
      titletext += ")";

    frame.setTitle(titletext);

    frame.getContentPane().add(this);

    Font SF = new Font("SansSerif", Font.PLAIN, 11);
    frame.setFont(SF);
    
    evtq = Toolkit.getDefaultToolkit().getSystemEventQueue();

    // We use low-level events, so we must specify
    // which events we are interested in.
    //    this.enableEvents(AWTEvent.MOUSE_EVENT_MASK);
    //		      AWTEvent.MOUSE_MOTION_EVENT_MASK | 
    this.enableEvents(AWTEvent.WINDOW_EVENT_MASK);
    this.enableEvents(AWTEvent.KEY_EVENT_MASK);
  }

  public void addModelListener(ModelListener l) {
    listener = l;
  }

  public void processEvent(AWTEvent evt) {
    if( evt instanceof ModelEvent ) {
      if( listener != null )
        listener.updateModel(tablep, tablep.names, maxLevel);
    }
    else super.processEvent(evt);
  }

  public void maintainSelection(Selection S) {

    Rectangle sr = S.r;
    int mode = S.mode;

    S.condition = new Query();
    Query tmpQ = new Query();
    for( int i = 0;i < rects.size(); i++) {
      MyRect r = (MyRect)rects.elementAt(i);
      if ( r.intersects( sr )) {
        //System.out.println("");
        //System.out.print("Tile "+i+" includes table-line:");

        if( tablep.data.isDB ) {
          StringTokenizer info = new StringTokenizer(r.getLabel(), "\n");
          for( int j=0; j<tablep.k; j++) {
            StringTokenizer line = new StringTokenizer(info.nextToken(), ":");
            tmpQ.addCondition("AND", line.nextToken().trim()+" = '"+line.nextToken().trim()+"'");
          }
          S.condition.addCondition("OR", tmpQ.getConditions());
          tmpQ.clearConditions();
          tablep.getSelection();
        } else {
          double sum=0, sumh=0;
          for( int j=0; j<r.tileIds.size(); j++ ) {
            int id = ((Integer)(r.tileIds.elementAt(j))).intValue();
            tablep.setSelection(id,1,mode);
            sumh += tablep.getSelected(id)*tablep.table[id];
            sum  += tablep.table[id];
          }
          r.setHilite( sumh/sum );
        }
      } else
        if( !tablep.data.isDB )
          for( int j=0; j<r.tileIds.size(); j++ ) {
            int id = ((Integer)(r.tileIds.elementAt(j))).intValue();
            tablep.setSelection(id,0,mode);
          }
    }
  }

    public void updateSelection() {
      tablep.getSelection();
      paint(this.getGraphics());
    }

    public void dataChanged(int var) {
      //System.out.println("Checking Mosaic caused by: "+var);
      for( int i=0; i<tablep.initialVars.length; i++ ) {
        //System.out.println("Variable "+tablep.initialVars[i]);
        if( var == tablep.initialVars[i] ) {
          tablep.rebreak();
          paint(this.getGraphics());
        }
      }
    }

    public void paint(Graphics2D g) {
      
      frame.setBackground(MFrame.backgroundColor);

      tablep.getSelection();

      Dimension size;
      size = this.getSize();

      if( !printing ) {
        bi = createImage(size.width, size.height);	// double buffering from CORE JAVA p212
        bg = (Graphics2D)bi.getGraphics();
      } else
        bg = g;

      create(border, border, size.width-border, size.height-border, "");

      String titletext = "Mosaic(";

      for (int i=0; i<k ; i++) {
        if( i < k-1 && i != maxLevel-1 )
          titletext += names[i]+", ";
        else
          titletext += names[i];
        if( i+1 == maxLevel && maxLevel < k )
          titletext += ")[";
      }
      if ( maxLevel < k )
        titletext += "]";
      else
        titletext += ")";

      frame.setTitle(titletext);

      if( displayMode.equals("Fluctuation") || displayMode.equals("Multiple Barcharts") ) {
        if( !printing )
          bg.setColor(new Color(1.0F, 1.0F, 1.0F, 0.2F));
        else
          bg.setColor(new Color(0.95F, 0.95F, 0.95F, 1.0F));
        for( int i = 0;i < rects.size(); i++) {
          MyRect r = (MyRect)rects.elementAt(i);
          bg.fillRect(r.x, r.y-r.height+r.h, r.width, r.height);
          bg.drawRect(r.x, r.y-r.height+r.h, r.width, r.height);
        }
      }
      
      for( int i = 0;i < rects.size(); i++) {
        MyRect r = (MyRect)rects.elementAt(i);
        double sum=0, sumh=0;
        for( int j=0; j<r.tileIds.size(); j++ ) {
          int id = ((Integer)(r.tileIds.elementAt(j))).intValue();
          sumh += tablep.getSelected(id)*tablep.table[id];
          sum  += tablep.table[id];
        }
        r.setHilite( sumh/sum );
        if( !((displayMode.equals("Fluctuation") || displayMode.equals("Multiple Barcharts")) && sum == 0) )
          r.draw(bg);

        bg.setColor(Color.black);
      }
      if( displayMode.equals("Same Bin Size") || displayMode.equals("Multiple Barcharts") || displayMode.equals("Fluctuation") || printing )
        for( int i = 0;i < Labels.size(); i++) {
          MyText t = (MyText)Labels.elementAt(i);
          t.draw(bg);
        }
      
      if( !printing ) {
        drawSelections(bg);
        g.drawImage(bi, 0, 0, null);
        bg.dispose();
      }
    }

    public void drawSelections(Graphics g) {

      for( int i=0; i<Selections.size(); i++) {
        Selection S = (Selection)Selections.elementAt(i);
        drawBoldDragBox(g, S);
      }
    }

    public void processWindowEvent(WindowEvent e) {
      if (e.getID() == WindowEvent.WINDOW_ACTIVATED) {
        ModelEvent me = new ModelEvent(this);
        evtq.postEvent(me);
      }
    }

      public String getToolTipText(MouseEvent e) {

        if( e.isControlDown() ) {

          for( int i = 0;i < rects.size(); i++) {
            MyRect r = (MyRect)rects.elementAt(i);
            if ( r.contains( e.getX(), e.getY() )) {
              return Util.info2Html(r.getLabel());
            }
          }
          // end FOR
          return null;
        } else
          return null;
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
            if ( r.contains( e.getX(), e.getY() )) {
              info = true;
              r.pop(this, e.getX(), e.getY());
            }
          }
          if( !info ) {
            PopupMenu mode = new PopupMenu();
            MenuItem Observed = new MenuItem("Observed");
            MenuItem Expected = new MenuItem("Expected");
            MenuItem SameBinSize = new MenuItem("Same Bin Size");
            MenuItem MultiBar = new MenuItem("Multiple Barcharts");
            MenuItem Fluctuation = new MenuItem("Fluctuation");
            // infoText.addActionListener(this);
            mode.add(Observed);
            Observed.setActionCommand("Observed");
            Observed.addActionListener(this);
            mode.add(Expected);
            Expected.setActionCommand("Expected");
            Expected.addActionListener(this);
            mode.add(SameBinSize);
            SameBinSize.setActionCommand("Same Bin Size");
            SameBinSize.addActionListener(this);
            mode.add(Fluctuation);
            MultiBar.setActionCommand("Multiple Barcharts");
            MultiBar.addActionListener(this);
            mode.add(MultiBar);
            Fluctuation.setActionCommand("Fluctuation");
            Fluctuation.addActionListener(this);
            
            frame.add(mode);
            mode.show(e.getComponent(), e.getX(), e.getY());
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
      if( command.equals("Observed") || command.equals("Expected") ||
          command.equals("Same Bin Size") || command.equals("Multiple Barcharts") || command.equals("Fluctuation") ) {
        displayMode = command;
        create(border, border, width-border, height-border, "");
        Graphics g = this.getGraphics();
        paint(g);
      }
      else
        super.actionPerformed(e);
    }

    public void processKeyEvent(KeyEvent e) {

//System.out.println("Key pressed: "+e.getKeyCode());
      if (e.getID() == KeyEvent.KEY_PRESSED && (    e.getKeyCode() == KeyEvent.VK_UP
                                                ||  e.getKeyCode() == KeyEvent.VK_DOWN
                                                ||  e.getKeyCode() == KeyEvent.VK_LEFT
                                                ||  e.getKeyCode() == KeyEvent.VK_RIGHT
                                                ||  e.getKeyCode() == KeyEvent.VK_UP && e.isShiftDown()
                                                ||  e.getKeyCode() == KeyEvent.VK_DOWN && e.isShiftDown()    
                                                ||  e.getModifiers() == Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() && e.getKeyCode() == KeyEvent.VK_R
                                                ||  e.getModifiers() == Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() && e.getKeyCode() == KeyEvent.VK_ADD
                                                ||  e.getModifiers() == Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() && e.getKeyCode() == KeyEvent.VK_SUBTRACT)) {
        if( e.getKeyCode() == KeyEvent.VK_DOWN ) {
          if( e.isShiftDown() )
            if( censor > 0 )
              censor--;
            else
              return;	
          else
            if( maxLevel < k ) {
              maxLevel += 1;
            }
        }
        if( e.getKeyCode() == KeyEvent.VK_UP ) {
          if( e.isShiftDown() )
            censor++;
          else
            if( maxLevel > 1 ) {
            maxLevel -= 1;
          }
        }
        if( e.getKeyCode() == KeyEvent.VK_LEFT ) {
          if( maxLevel != k ) {
            int[] rotation = new int[k];
            for (int i=0; i<maxLevel-1; i++)
              rotation[i] = i;
            for (int i=maxLevel-1; i<k ; i++)
              rotation[i] = i+1;
            rotation[k-1] = maxLevel-1;
            tablep.permute(rotation);
          }
        }
        if( e.getKeyCode() == KeyEvent.VK_RIGHT ) {
          if( maxLevel != k ) {
            int[] rotation = new int[k];
            for (int i=0; i<maxLevel-1; i++)
              rotation[i] = i;
            for (int i=maxLevel; i<k ; i++)
              rotation[i] = i-1;
            rotation[maxLevel-1] = k-1;
            tablep.permute(rotation);
          }
        }
        if(e.getModifiers() == Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() && e.getKeyCode() == KeyEvent.VK_R) {
          for( int i=maxLevel-1; i<tablep.k; i++ )
            if( Dirs[i] == 'x')
              Dirs[i] = 'y';
            else
              Dirs[i] = 'x';
        }
        if( (e.getModifiers() == Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() && (e.getKeyCode() == KeyEvent.VK_ADD) || (e.getKeyCode() == KeyEvent.VK_SUBTRACT)) ) {	
          frame.setCursor(Frame.WAIT_CURSOR);
          int[] interact = new int[maxLevel];
          for( int i=0; i<maxLevel; i++ )
            interact[i] = i;
          if( e.getKeyCode() == KeyEvent.VK_ADD )
            if( !tablep.addInteraction( interact, true ) )
              Toolkit.getDefaultToolkit().beep();
          if( e.getKeyCode() == KeyEvent.VK_SUBTRACT )
            if( !tablep.deleteInteraction( interact ) )
              Toolkit.getDefaultToolkit().beep();
          frame.setCursor(Frame.DEFAULT_CURSOR);
        }
        create(border, border, width-border, height-border, "");
        Graphics g = this.getGraphics();
        paint(g);
        ModelEvent me = new ModelEvent(this);
        evtq.postEvent(me);
      }
      else
        super.processKeyEvent(e);  // Pass other event types on.
    }

    public void createMosaic(int start, int levelid, double[] Mtable, int x1, int y1, int x2, int y2, String infop) {

      double[] counts = new double[levels[levelid]+1];
      double[] oCounts = new double[levels[levelid]+1];
      double[]   exps = new double[levels[levelid]];
      double[]    obs = new double[levels[levelid]];
      double total = 0;

      String info;
      MyRect tile;
      int index;
      Vector[] tileIds = new Vector[levels[levelid]];
      for (int j=0; j < levels[levelid]; j++) {
        tileIds[j] = new Vector(8,8);
      }

      // Calculate the absolute counts for each level first
      if ( levelid < k-1 ) {	        // if we did not reach the lowest level

        for (int j=0; j < levels[levelid]; j++) {
          for (int i=0; i < plevels[levelid]; i++) {
            index = start+ j*plevels[levelid] + i;
            total += Mtable[index];
            counts[j+1] += Mtable[index];
            oCounts[j+1] += tablep.table[index];
            exps[j] += tablep.exp[index];
            obs[j] += tablep.table[index];
            if( levelid == maxLevel-1 )
              tileIds[j].addElement(new Integer(index));
          }
          counts[j+1] += counts[j];
          oCounts[j+1] += oCounts[j];
        }
      } else {
        for (int j=0; j < levels[levelid]; j++) {
          total += Mtable[start + j];
          counts[j+1] += Mtable[start + j];
          counts[j+1] += counts[j];
          oCounts[j+1] += tablep.table[start + j];
          oCounts[j+1] += oCounts[j];
          exps[j] += tablep.exp[start + j];
          obs[j] += tablep.table[start + j];
          tileIds[j].addElement(new Integer(start + j));
        }
      }

//      int thisGap = 0;
//      if( !displayMode.equals("Fluctuation") )
      int thisGap = aGap[levelid];

      int emptyBin = 0;
      int emptyWidth = 0;
      if( levelid > 0 ) {
        if( levelid == maxLevel-1 )
          emptyBin = 0;
        else if( levelid == maxLevel-2 )
          emptyBin = 1;
        else
          emptyBin = aGap[levelid] - Gaps[levelid];
        emptyWidth = aGap[levelid-1] - Gaps[levelid-1];
      }      

      int sizeX = x2-x1;
      int sizeY = y2-y1;

      if( total > 0 )
        for (int j=0; j < levels[levelid]; j++) {                                     // for each level in this variable

          info = infop.toString() + names[levelid] + ": " + lnames[levelid][j] + '\n';// Add the popup information

          boolean empty = false;
          boolean stop  = false;
          int addGapX = 0;
          int addGapY = 0;
          
          if( counts[j+1] - counts[j] == 0 )
            empty = true;
          if( displayMode.equals("Same Bin Size") && oCounts[j+1]-oCounts[j] == 0 || levelid == maxLevel-1 ) {
            stop = true;
            for( int i=levelid+1; i<maxLevel; i++ )
              if( Dirs[i] == 'x' )
                addGapX += aGap[i];
              else
                addGapY += aGap[i];
          }
          
          if( stop || empty ) {	            // Now the rectangles are generated
            if( Dirs[levelid] == 'x' ) 
              if( empty )                                        // empty bin 
                tile = new MyRect(false, 'y', displayMode,
                                  x1 + (int)(counts[j] / total * sizeX) + j * thisGap, 
                                  y1,
                                  emptyBin, 
                                  sizeY+emptyWidth,
                                  0,  exps[j], 4 / residSum, tablep.p,
                                  info, tileIds[j]);
              else
                tile = new MyRect(true, 'y', displayMode,
                                  x1 + (int)(counts[j] / total * sizeX) + j * thisGap, 
                                  y1,
                                  Math.max(1, (int)((counts[j+1] - counts[j]) / total * sizeX)) + addGapX, 
                                  y2-y1 + addGapY,
                                  obs[j], exps[j], 4 / residSum, tablep.p,
                                  info, tileIds[j]);
            else {
              if( empty )
                tile = new MyRect(false, 'x', displayMode,
                                  x1,
                                  y1 + (int)(counts[j] / total * sizeY) + j * thisGap, 
                                  sizeX+emptyWidth,
                                  emptyBin,
                                  0, exps[j], 4 / residSum, tablep.p,
                                  info, tileIds[j]);
              else
                tile = new MyRect(true, 'x', displayMode,
                                  x1,
                                  y1 + (int)(counts[j] / total * sizeY) + j * thisGap, 
                                  x2-x1 + addGapX,
                                  Math.max(1, (int)((counts[j+1] - counts[j]) / total * sizeY)) + addGapY,
                                  obs[j], exps[j], 4 / residSum, tablep.p,
                                  info, tileIds[j]);
            }
            rects.addElement(tile);
          }
          else {						// Still to go in the recursion
            if( Dirs[levelid] == 'x' ) {
              createMosaic(start + j*plevels[levelid], 
                           levelid + 1,
                           Mtable,
                           x1 + j * thisGap + (int)(counts[j] / total * sizeX),
                           y1,
                           x1 + j * thisGap + Math.max((int)(counts[j] / total * sizeX +1),
                                                       (int)(counts[j+1] / total * sizeX)),
                           y2, 
                           info);
            }
            else {
              createMosaic(start + j*plevels[levelid], 
                           levelid + 1,
                           Mtable,
                           x1, 
                           y1 + j * thisGap + (int)(counts[j] / total * sizeY),  
                           x2, 
                           y1 + j * thisGap + Math.max((int)(counts[j] / total * sizeY +1),
                                                       (int)(counts[j+1] / total * sizeY)),
                           info);
            }
          }
        }
    }

      public void create(int x1, int y1, int x2, int y2, String info) {

        this.name = tablep.name;
        this.levels = tablep.levels;
        this.lnames = tablep.lnames;
        this.k = tablep.k;
        this.names = tablep.names;
        MyText label;

        rects.removeAllElements();
        Labels.removeAllElements();

        plevels = new int[k];               // reverse cumulative product of levels
        plevels[k-1] = 0;		
        if( k>1 )
          plevels[k-2] = levels[k-1];	// calculate the number of cells covered by a
                                      // category in level k
        for (int i=k-3; i>=0; i--) {
          plevels[i] = plevels[i+1] * levels[i+1];
        }

        int thisGap;
        int subX=0, subY=0;
        int mulX=1, mulY=1;
        Gaps = new int[maxLevel+2];
        aGap = new int[maxLevel+2];

        for( int j=0; j<maxLevel; j++) {
          if( !printing )
            thisGap = (maxLevel - j) * 3;
          else
            thisGap = (maxLevel - j) * 3 * printFactor;
          if( Dirs[j] == 'x' ) {
            subX += thisGap * (levels[j]-1) * mulX;
            mulX *= levels[j];
          }
          else {
            subY += thisGap * (levels[j]-1) * mulY;
            mulY *= levels[j];
          }
          Gaps[j] = thisGap;
        }
        /*    for( int j=0; j<maxLevel; j++) {
          int sum = Gaps[j];
        for( int k=j+2; k<maxLevel; k+=2 ) {
          int prod = 1;
          for( int l=j+2; l<k; l+=2 ) {
            prod *= levels[l];
          }
          prod *= (levels[k] - 1);
          sum += Gaps[k] * prod;
        }
        aGap[j] = sum;
        //      System.out.println("j: "+j+"  aGap[j]: "+aGap[j]+"  Gaps[j]: "+Gaps[j]);
        } "old version" */
//        if( displayMode.equals("Fluctuation") ) {
//          subX = 0;
//          subY = 0;
//        }
        for( int j=0; j<maxLevel; j++) {
          char dir = Dirs[j];
          int sum = Gaps[j];
          int k=j+1;
          while(k<maxLevel) {
            int prod = 1;
            if( Dirs[k] == dir ) {
              int l=j+1;
              while(l<k) {
                if( Dirs[l] == dir ) {
                  prod *= levels[l];
                }
                l++;
              }
              prod *= (levels[k] - 1);
              sum += Gaps[k] * prod;
            }
            k++;
          }
          aGap[j] = sum;
          //      System.out.println("j: "+j+"  aGap[j]: "+aGap[j]+"  Gaps[j]: "+Gaps[j]);
        }
        residSum = 0;
        for( int i=0; i<tablep.table.length; i++ )
//          residSum += Math.abs( tablep.table[i] - tablep.exp[i] );
          residSum += Math.abs( tablep.table[i] - tablep.exp[i] ) / Math.sqrt(tablep.exp[i]);
        if( Math.abs(residSum) < 0.0000001 ) {
          residSum =  1;
          for( int i=0; i<tablep.table.length; i++ )
            tablep.exp[i] = tablep.table[i];
        }
        double[] startTable = {1};  
        if( displayMode.equals("Observed") )
          startTable = tablep.table;
        else if( displayMode.equals("Expected") )
          startTable = tablep.exp;
        else if( displayMode.equals("Same Bin Size") || displayMode.equals("Multiple Barcharts") || displayMode.equals("Fluctuation") ) {
          startTable = new double[tablep.table.length];
          for( int i=0; i< startTable.length; i++ )
            startTable[i] = 1;
        }
        // start the recursion ////////////
        createMosaic(0, 0, startTable, x1, y1, Math.max(x2-subX,1), Math.max(y2-subY,1), info); 

        // Create labels for the first 2 dimensions 
        int pF = 1;
        if( printing )
          pF = printFactor;
        if( Dirs[0] == 'x' && Dirs[1] == 'y' || Dirs[0] == 'y' && Dirs[1] == 'x') {
          for(int j=0; j<Math.min(2, maxLevel); j++)
            for( int i=0; i<levels[j]; i++) {
//System.out.println("Levels("+i+"): "+lnames[j][i]);
              if( Dirs[j] == 'x' )
                label = new MyText(lnames[j][i], (int)((x1+(double)(x2-x1)/(double)levels[j]*(i+0.5))), border-5*pF, 0, (x2-x1)/levels[j]-2);
              else
                label = new MyText(lnames[j][i], -(int)((y1+(double)(y2-y1)/(double)levels[j]*(i+0.5))), border-6*pF,  -Math.PI/2.0, (y2-y1)/levels[j]-2);
              Labels.addElement(label);
            }
        }
        
        if( displayMode.equals("Multiple Barcharts") ||  displayMode.equals("Fluctuation") ) {
          double maxCount=0;
          for (int i=0; i<rects.size(); i++) 
            maxCount = Math.max(maxCount, ((MyRect)(rects.elementAt(i))).obs);
          for (int i=0; i<rects.size(); i++) {
            MyRect r = (MyRect)(rects.elementAt(i));
            int newH = 0;
            if( displayMode.equals("Multiple Barcharts") ) {
              r.dir = 'y';
              r.h = (int)((double)r.height * (1.0+(double)censor/5.0) * r.obs/maxCount);
            } else {
              r.w = (int)((double)r.width * ((1.0+(double)censor/5.0) * Math.sqrt(r.obs/maxCount)));
              r.h = (int)((double)r.height * ((1.0+(double)censor/5.0) * Math.sqrt(r.obs/maxCount)));
            }
//            newH = Math.min(newH, r.height);
//            r.w = Math.min(r.w, r.width);
            if( (r.h >= r.height && r.w >= r.width) && censor > 0)
              r.censored = true;
            r.y += r.height-r.h;
//            r.h = newH;
          }
        }
      }
      
      private String name;          // the name of the table;
      private double table[];	// data in classical generalized binary order
      private int k;		// number of variables
      private int[] levels;    	// number of levels for each variable
      private int[] plevels;        // reverse cummulative product of levels
      private int aGap[];
      private int Gaps[];
      private char Dirs[];

      private String[] names;	// variable names
      private String[][] lnames;	// names of levels

      private int maxLevel;	        // How many variables should be drawn

      private ModelListener listener;
      private static EventQueue evtq;

      public void adjustmentValueChanged(AdjustmentEvent e) {
      }
      public void scrollTo(int id) {
      }
}

class ModelEvent extends AWTEvent {
  public ModelEvent(Mosaic m) {
    super( m, MODEL_EVENT );
  }
  public static final int MODEL_EVENT = AWTEvent.RESERVED_ID_MAX + 2;
}
