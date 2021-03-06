package org.rosuda.klimt;

import org.rosuda.ibase.*;
import org.rosuda.ibase.toolkit.*;
import org.rosuda.ibase.plots.*;
import org.rosuda.util.*;
import org.rosuda.klimt.plots.*;
import org.rosuda.plugins.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;

/** Variables window - central place for operations on a dataset
    @version $Id$
*/

public class VarFrame extends TFrame {
    VarCanvas vc;
    VarCmdCanvas vcc;
    Scrollbar sb=null;
    DataRoot dr;
    
    static int newRootId=1;

    public static final int cmdHeight=182;
    
    Menu recentMenu;
    
    public VarFrame(DataRoot dr, int x, int y, int w, int h) {
	super(dr.getDataSet().getName()+" (Variables)",TFrame.clsVars);
        SVarSet vs=dr.getDataSet();
        this.dr=dr;
        setBackground(Common.backgroundColor);
        if (h>550) h=550; // temporary fix until we know how to get really available space sub. menus etc.
	int rh=h;
	if (rh>vs.count()*17+6+cmdHeight+40)
	    rh=vs.count()*17+6+cmdHeight+40;
	setLayout(new BorderLayout());
	int minus=0;
	if (rh==h) {
	    add(sb=new Scrollbar(Scrollbar.VERTICAL,0,17,0,vs.count()*17+23+cmdHeight-h),"East");
	    pack();
	    Dimension sbd=sb.getSize();
	    minus=sbd.width;
            sb.setBlockIncrement(17*4);
	};
	add(vc=new VarCanvas(this,vs,sb));
	if (rh!=h)
	    vc.minDim=new Dimension(w,rh-cmdHeight);
	else
	    sb.addAdjustmentListener(vc);
	
	add(vcc=new VarCmdCanvas(this,vs),"South");
	addWindowListener(Common.getDefaultWindowListener());
	setBounds(x-minus,y,w,rh);
	vc.setBounds(x-minus,y,w,rh-cmdHeight);
	vcc.setBounds(x-minus,y+rh-cmdHeight,w,cmdHeight);
	pack();
        //if (System.getProperty("").indexOf("")>-1) {
            String myMenu[]={"+","File","@OOpen Dataset ...","openData","!OOpen Tree ...","openTree","Open Models ...","openModel","#Open Recent","","Save Selected ...","export","-",
                "New Derived Variable ...","deriveVar","Show Data Table","datatab","-","New Tree Root","newRoot","Grow Tree ...","growTree","-",
                "Export Forest ...","exportForest","@FDisplay Forest","displayForest","~File.Quit",
                "+","Edit","@ASelect All","selAll","@DClear","selNone","-",
                "Select Residuals","selRes","Select Predictions","selPred","Select Node Variables","selNode",
                "+","Plot","Barchart","barchart","Histogram","histogram",
                "Boxplot","boxplot","-","Scatterplot","scatterplot",
                "Fluctuation Diagram","fluct","-","Speckle Plot","speckle",
                "Parallel coord. plot","PCP","Hammock Plot","hammock","Misclassification Plot","xMCP",
                "Series Plot","lineplot","Series Plot with Index","lineplot2","Kaplan-Meier Plot","kmplot","-","Map","map",
                "-","Trace Plot","tfplot",
                "~Window","0"};
            EzMenu.getEzMenu(this,vc,myMenu);
            Menu rm=recentMenu=(Menu) EzMenu.getItemByLabel(this,"Open Recent");
            if (rm!=null) {
                if (SplashScreen.recentOpen==null)
                    SplashScreen.recentOpen=new RecentList(Common.appName,"RecentOpenFiles",8);
                String[] shortNames=SplashScreen.recentOpen.getShortEntries();
                String[] longNames =SplashScreen.recentOpen.getAllEntries();
                int i=0;
                while (i<shortNames.length) {
                    MenuItem mi=new MenuItem(shortNames[i]);
                    mi.setActionCommand("recent:"+longNames[i]);
                    mi.addActionListener(vc);
                    rm.add(mi);
                    i++;
                }
                if (i>0) rm.addSeparator();
                MenuItem ca=new MenuItem("Clear list");
                ca.setActionCommand("recent-clear");
                ca.addActionListener(vc);
                rm.add(ca);
                if (i==0) ca.setEnabled(false);
            }
            
            MenuItem mi=EzMenu.getItem(this,"datatab");
            EzMenu.getItem(this,"export").setEnabled(false);
            if (!PluginManager.pluginExists("PluginTable")) mi.setEnabled(false);
            if (!PluginManager.pluginExists("PluginModelLoader")) EzMenu.getItem(this,"openModel").setEnabled(false);
        //};
	setVisible(true);
    };

    /** VarCanvas is canvas for the variables list */
    class VarCanvas extends DBCanvas implements PlotComponent, MouseListener, AdjustmentListener, Commander, ActionListener
    {
	/** associated window */
	VarFrame win;
	/** selection mask of the variables */
	boolean[] selMask;
	/** data source */
	SVarSet vs;
        /** # of variables (cached from data source) - do NOT use directly, access via {@link #getVars} */
	int c_vars;
	/** scrollbar if too many vars are present */
	Scrollbar sb;
	Dimension minDim;
	Dimension lastSize;

	QueryPopup qp;

	int offset=0;
        int genCount=0;
        int firstSel=-1;

	/** constructs a new variable canvas (list of variables) for associated tree canvas
	    @param w window in which this canvsa is displayed
	    @param p associated tree canvas
	*/
	VarCanvas(VarFrame w, SVarSet dataset,Scrollbar s) {
	    setBackground(Common.backgroundColor);
	    win=w; vs=dataset;
	    c_vars=vs.count();
	    selMask=new boolean[c_vars+4];
	    addMouseListener(this);
	    sb=s;
	    minDim=new Dimension(140,100);
	    qp=PlotComponentFactory.createQueryPopup(this,w, null,"variables");
	}

        public int getVars() {
            if (vs.count()!=c_vars) rebuildVars();
            return c_vars;
        };
        
        public void rebuildVars() {
            if (Global.DEBUG>0)
                System.out.println("VarFrame.VarCanvas:rebuilding variables ("+c_vars+"/"+vs.count()+")");
            c_vars=vs.count();
            selMask=new boolean[c_vars+4]; lastSize=null; // force rebuild of scrollbar etc.
            repaint();
        };
        
	public void adjustmentValueChanged(AdjustmentEvent e) {
	    offset=e.getValue();
	    repaint();
	};
	public Dimension getMinimumSize() { return minDim; };

	public void initializeLayerCanvas(LayerCanvas l) {};
	public void initializeGraphics(Window w) {};
	
	public Window getParentWindow() { return win; };
	public int getGraphicsEngine()  { return PlotComponent.AWT; }
	public Component getComponent() { return this; };

	public void setPreferredSize(Dimension d) {};
	public void setMinimumSize(Dimension d) {};
	public void setMaximumSize(Dimension d) {};
	public void setToolTipText(String s) {};

	/** implementation of the {@link DBCanvas#paintBuffer} method
	    @param g graphic context to paint on */
	public void paintBuffer(Graphics g) {
	    int totsel=0;	    
	    Dimension cd=getSize();

            if (Global.useAquaBg) {
                g.setColor(Color.white);
                g.fillRect(0, 0, cd.width, cd.height);
                int y=0;
                g.setColor(Common.aquaBgColor);
                while (y<cd.height-2) {
                    g.fillRect(0,y,cd.width,2); y+=4;
                }
            }
            
            if (c_vars!=vs.count()) // make sure the # of vars didint grow
                rebuildVars();

	    if (lastSize==null || cd.width!=lastSize.width || cd.height!=lastSize.height) {
		int minh=getVars()*17+6;
		if (minh>200) minh=200;
		if (cd.width<140 || cd.height<minh) {
		    setSize((cd.width<140)?140:cd.width,(cd.height<minh)?minh:cd.height);
		    win.pack();
                    cd=getSize();
		}; 
		minh=getVars()*17+6;
                if (sb!=null) {
                    if (minh-cd.height+17<=0) {
                        sb.setValue(offset=0); vc.repaint();
                        sb.setMaximum(0);
                    } else {
                        sb.setMaximum(minh-cd.height+17);
                    };
                };
		lastSize=cd;
	    };

	    Font fo=getFont();
	    Font f2=new Font(fo.getName(),Font.BOLD,fo.getSize());
	    Color C_varNam=new Color(0,0,128);
	    Color C_info=new Color(128,0,0);
	    Color C_bg=new Color(255,255,255);
	    Color C_sel=new Color(128,255,128);
	    Color C_frame=new Color(128,128,128);
            Color C_intVar=new Color(216,216,255);

	    int i=0;
	    for (Enumeration e=vs.elements(); e.hasMoreElements();) {
		SVar v=(SVar)e.nextElement();	    
		if (selMask[i]) totsel++;
                g.setColor(selMask[i]?C_sel:((v.getInternalType()>0)?C_intVar:C_bg));
		g.fillRect(5,5+i*17-offset,130,15);
		g.setColor(C_frame);
		g.drawRect(5,5+i*17-offset,130,15);
		g.setFont(fo); g.setColor(C_info);
                String st="?";
                if (v.getContentsType()==SVar.CT_String) st="S";
                if (v.getContentsType()==SVar.CT_Number) st="N";
                if (v.getContentsType()==SVar.CT_Map) st="Map";
                if (v.getContentsType()==SVar.CT_Tree) st="Tree";
		g.drawString(st+(v.isCat()?"C":"")+(v.hasMissing()?"*":""),10,17+i*17-offset);
		g.setFont(f2); g.setColor(C_varNam);	
		g.drawString(v.getName(),35,17+i*17-offset);
		i++;
	    };

	    /*
	    if (17+i*17>cd.height) {
		setSize(cd.width,17+i*17);
		win.pack();
		win.repaint();
		}; 
	    */
	};
    
	/* mouse actions */

	public void mouseClicked(MouseEvent ev) 
	{
	    if (vs==null) return;

	    int x=ev.getX(), y=ev.getY()+offset;
	    int svar=-1;
	    if ((x>5)&&(x<115)) svar=(y-3)/17;
	
	    if (Common.isQueryTrigger(ev)) {
		if (svar<vs.count() && svar>=0) {
		    SVar v=vs.at(svar);
		    qp.setContent("Name: "+v.getName()+
				  "\nType: "+(v.isNum()?"numeric":"text")+
				  (v.isCat()?", discrete":", continuous")+
				  "\nHas missings: "+(v.hasMissing()?"yes ("+v.getMissingCount()+")":"no")+
				  (v.isCat()?("\nLevels: "+v.getNumCats()):"")+
				  (v.isNum()?("\nRange: "+Tools.getDisplayableValue(v.getMin(),v.getMax()-v.getMin())+" .. "+Tools.getDisplayableValue(v.getMax(),v.getMax()-v.getMin())):""));
		    Point cl=win.getLocation();
		    qp.setLocation(cl.x+x,cl.y+y);
		    qp.show();
		}
		return;
	    };
	    qp.hide();
	    if (svar<vs.count()) {
		if (ev.isMetaDown() || ev.isControlDown()) {
                    if (svar>=0) {
                        selMask[svar]=!selMask[svar];
                        if (vs!=null && vs.at(svar)!=null) vs.at(svar).setSelected(selMask[svar]);
                    }
		} else {
                    if (firstSel==-1 || !ev.isShiftDown()) {
                        for(int i=0;i<selMask.length;i++) {
                            selMask[i]=false;
                            if (vs!=null && vs.at(i)!=null) vs.at(i).setSelected(false);
                        }                            
                        firstSel=-1;
                        if (svar>=0) {
                            selMask[svar]=true;
                            if (vs!=null && vs.at(svar)!=null) vs.at(svar).setSelected(true);
                            firstSel=svar;
                        }
                    } else {
                        if (svar>=0) {
                            for(int i=0;i<selMask.length;i++) {
                                selMask[i]=false;
                                if (vs!=null && vs.at(i)!=null) vs.at(i).setSelected(false);
                            }
                            int j=firstSel, k=firstSel;
                            if (svar>firstSel) k=svar; else j=svar;
                            while(j<=k) {
                                selMask[j]=true;
                                if (vs!=null && vs.at(j)!=null) vs.at(j).setSelected(true);
                                j++;
                            }
                        }
                    }
		};
                repaint();
		if (ev.getClickCount()==2) {
                    if (vs.at(svar).isNum())
                        vs.at(svar).setCategorical(!vs.at(svar).isCat());
		    repaint();
		};
	    };

	    win.getVarCmdCanvas().repaint();
	};
        
	public void mousePressed(MouseEvent ev) {};
	public void mouseReleased(MouseEvent e) {};
	public void mouseEntered(MouseEvent e) {};
	public void mouseExited(MouseEvent e) {};
        
        public Object run(Object o, String cmd) {
            if (cmd=="exit") WinTracker.current.Exit();
            if (cmd.startsWith("recent")) return KlimtSplash.main.run(o, cmd);
            if (cmd=="exportForest") {
                try {
                    PrintStream p=Tools.getNewOutputStreamDlg(win,"Export forest data to ...","forest.txt");
                    dr.getTreeRegistry().exportForest(p);
                } catch(Exception ee) {};
            };
            if (cmd=="displayForest") {
                SVarSet fs=dr.getTreeRegistry().getForestVarSet();
                DataRoot dr=Klimt.addData(fs);
                dr.setDataType(DataRoot.DT_Forest);
                Dimension sres=Toolkit.getDefaultToolkit().getScreenSize();
                Common.screenRes=sres;
                VarFrame vf=Klimt.newVarDisplay(dr,sres.width-150,0,140,(sres.height>600)?600:sres.height-20);
            }
            if (cmd=="selNone" || cmd=="selAll" || cmd=="selPred" || cmd=="selRes" || cmd=="selNode") {
                int target=-998;
                if (cmd=="selPred") target=SVar.IVT_Prediction;
                if (cmd=="selNode") target=SVar.IVT_LeafID;
                if (cmd=="selRes") target=SVar.IVT_Resid;
                if (cmd=="selAll") target=-997;
                int i=0, l=selMask.length;
                while (i<l) {
                    selMask[i]=false;
                    int vit=-999;
                    if (vs.at(i)!=null) vit=vs.at(i).getInternalType();
                    if (target==-997 || vit==target || (cmd=="selRes" && vit==SVar.IVT_RCC)) selMask[i]=true;
                    if (vs.at(i)!=null) vs.at(i).setSelected(selMask[i]); 
                    i++;
                }
                vc.getVars();
                vc.repaint();
                repaint();
            }
            if (cmd=="openData") {
                KlimtSplash.main.run(o, cmd);
                vc.getVars();
                vc.repaint();
            }
            if (cmd=="map") {
                int i=0;
                SVar map=null, vLat=null, vLon=null;
                while (i<vc.getVars()) {
                    if (vc.selMask[i]) {
                        if (vs.at(i).getContentsType()==SVar.CT_Map) map=vs.at(i);
						if (vs.at(i).isNum()) {
							if (vLat==null) vLat=vs.at(i); else
								if (vLon==null) vLon=vs.at(i);
						}
					}
                    i++;
                }
                if (map!=null) {
                    TFrame f=new TFrame("Map ("+map.getName()+")",TFrame.clsMap);
                    f.addWindowListener(Common.getDefaultWindowListener());
					if (vLon!=null) { // if we have lat/lon, got for mapped SP
						MapScatterCanvas bc=new MapScatterCanvas(null,f,vLat,vLon,map,vs.getMarker());
						if (vs.getMarker()!=null) vs.getMarker().addDepend(bc);
						bc.pc.setSize(new Dimension(400,300));
						f.add(bc.getComponent());
					} else {
						MapCanvas bc=new MapCanvas(null,f,map,vs.getMarker());
						if (vs.getMarker()!=null) vs.getMarker().addDepend(bc);
						bc.pc.setSize(new Dimension(400,300));
						f.add(bc.getComponent());
					}
                    f.pack(); f.show();
                }
            }
            if (cmd=="openTree" || cmd=="openTreeSilently") { // Open tree
                SNode t=Klimt.openTreeFile(Common.mainFrame,null,dr,true,(cmd=="openTree"));
                if (t!=null) {
                    vc.getVars();
                    vc.repaint();
                };
            };
            if (cmd=="newRoot") { // create new tree root
                if (vc.vs!=null && vc.vs.count()>0 && vc.vs.at(0)!=null) {
                    int j=0; SVar resp=null;
                    while(j<vs.count()) {
                        if(vc.selMask[j]) { resp=vc.vs.at(j); break; }
                        j++;
                    }
                    if (resp==null) {
                        Frame f=new Frame("dummy");
                        MsgDialog md=new MsgDialog(f, "Missing Response",
                                                   "You didn't select any response variable. Please select a variable in the variable window.");
                        md.dispose();
                        f.dispose();
                        return null;
                    }
                    String tn="new.tree."+(VarFrame.newRootId++);
                    SNode t=new SNode();
                    int i=0;
                    int cn=vc.vs.at(0).size();
                    t.data=new int[cn];
                    while (i<cn) { t.data[i]=i; i++; }
                    t.vset=vc.vs;
                    t.Cases=cn;
                    t.Name="root";
                    RootInfo ri=t.getRootInfo();
                    ri.response=resp;
                    ri.name=tn;
                    dr.getTreeRegistry().registerTree(t,tn);
                    ri.frame=new TFrame(tn,TFrame.clsTree);
                    t.calculateSampleDeviances();
                    TreeCanvas tc=Klimt.newTreeDisplay(t,ri.frame);
                    tc.repaint(); tc.redesignNodes();
                }
            }
            if (cmd=="export") { // Export ...
                System.out.println("export...");
                try {
                    PrintStream p=Tools.getNewOutputStreamDlg(win,"Export selected variables to ...","selected.txt");
                    if (p!=null) {
                        int j=0,tcnt=0,fvar=0;
                        j=0;
                        while(j<vs.count()) {
                            if(vc.selMask[j]) {
                                p.print(((tcnt==0)?"":"\t")+vs.at(j).getName());
                                if (tcnt==0) fvar=j;
                                tcnt++;
                            }
                            j++;
                        }
                        p.println("");
                        int i=0;
                        SMarker m=vs.getMarker();
                        boolean exportAll=(m==null || m.marked()==0);
                        while (i<vs.at(fvar).size()) {
                            if (exportAll || m.at(i)) {
                                j=fvar;
                                while(j<vs.count()) {
                                    if (vc.selMask[j]) {
                                        Object oo=vs.at(j).at(i);
                                        p.print(((j==fvar)?"":"\t")+((oo==null)?"NA":oo.toString()));
                                    };
                                    j++;
                                };
                                p.println("");
                            }
                            i++;
                        };
                        p.close();
                    };
                } catch (Exception eee) {
                    if (Global.DEBUG>0) {
                        System.out.println("* VarFrame.Export...: something went wrong during the export: "+eee.getMessage()); eee.printStackTrace();
                    };
                };
            }
            if (cmd=="barchart" || cmd=="histogram") { //  Histogram/barchart
                          // we got one special case here - one cat and one num(non-cat) are used to plot weighted barchart
                int i=0;
                int selC=0, selN=0;
                SVar theCat=null, theNum=null;
                while (i<vc.getVars()) {
                    if (vc.selMask[i]) {
                        if (vs.at(i).isCat()) { selC++; theCat=vs.at(i); }
                        else if(vs.at(i).isNum()) { selN++; theNum=vs.at(i); };
                    };
                    i++;
                };
                if (selC==1 && selN==1) { // ok, go for weighter barchart instead
                    TFrame f=new TFrame("w.Barchart ("+theCat.getName()+"*"+theNum.getName()+")",TFrame.clsBar);
                    f.addWindowListener(Common.getDefaultWindowListener());
                    BarCanvas bc=new BarCanvas(null,f,theCat,vs.getMarker(),theNum);
                    if (vs.getMarker()!=null) vs.getMarker().addDepend(bc);
                    int iw=100+40*theCat.getNumCats(); if (iw>800) iw=800;
                    bc.pc.setSize(new Dimension(iw,200));
                    f.add(bc.getComponent()); f.pack(); f.show();
                    f.initPlacement();
                } else {
                    for(i=0;i<vc.getVars();i++)
                        if (vc.selMask[i]) {
                            TFrame f=new TFrame((vs.at(i).isCat()?"Barchart":"Histogram")+" ("+vs.at(i).getName()+")",
                                                vs.at(i).isCat()?TFrame.clsBar:TFrame.clsHist);
                            f.addWindowListener(Common.getDefaultWindowListener());
                            PGSCanvas cvs=null;
                            int xdim=400, ydim=300;
                            if (vs.at(i).isCat()) {
                                BarCanvas bc=new BarCanvas(null,f,vs.at(i),vs.getMarker()); cvs=bc;
                                xdim=100+40*vs.at(i).getNumCats(); ydim=200;
                                if (xdim>800) xdim=800;
                                if (vs.getMarker()!=null) vs.getMarker().addDepend(bc);
                            } else {
                                HistCanvasEx hc=new HistCanvasEx(null,f,vs.at(i),vs.getMarker(),dr.getNodeMarker()); cvs=hc;
                                if (vs.getMarker()!=null) vs.getMarker().addDepend(hc);
                            };
                            cvs.pc.setSize(new Dimension(xdim,ydim));
                            f.add(cvs.getComponent()); f.pack(); f.show();
                            f.initPlacement();
                        };
                };
            };

            if (cmd=="kmplot") {
                int i=0;
                int selC=0, selN=0;
                SVar theCat=null, theNum=null;
                while (i<vc.getVars()) {
                    if (vc.selMask[i]) {
                        if (vs.at(i).isCat()) { selC++; theCat=vs.at(i); }
                        else if(vs.at(i).isNum()) { selN++; theNum=vs.at(i); }
                    }
                    i++;
                }
                if (selC==1 && selN==1) {
                    TFrame f=new TFrame("Kaplan-Meier ("+theCat.getName()+"*"+theNum.getName()+")",TFrame.clsUser);
                    f.addWindowListener(Common.getDefaultWindowListener());
                    KapMeNodeCanvas bc=new KapMeNodeCanvas(f,theNum,theCat,vs.getMarker(),dr.getNodeMarker());
                    if (vs.getMarker()!=null) vs.getMarker().addDepend(bc);
                    bc.pc.setSize(new Dimension(300,200));
                    f.add(bc.getComponent()); f.pack(); f.show();
                    f.initPlacement();
                }
            }
                    
            if (cmd=="lineplot") {
                int i=0;
                int selC=0, selN=0;
                while (i<vc.getVars()) {
                    if (vc.selMask[i]) {
                        if (vs.at(i).isCat()) selC++;
                        if (vs.at(i).isNum()) selN++;
                    };
                    i++;
                };
                if (selN>0) {
                    SVar[] vars=new SVar[selN];
                    i=0; int vsc=0;
                    while (i<vc.getVars()) {
                        if (vc.selMask[i] &&vs.at(i).isNum()) vars[vsc++]=vs.at(i);
                        i++;
                    };
                    TFrame f=new TFrame("Line plot",TFrame.clsLine);
                    f.addWindowListener(Common.getDefaultWindowListener());
                    LineCanvas lc=new LineCanvas(f,null,vars,vs.getMarker());
                    lc.pc.setSize(400,300);
                    f.add(lc.getComponent()); f.pack(); f.show();
                }
            }
            if (cmd=="tfplot") {
                TFrame f=new TFrame("Trace Plot",TFrame.clsUser);
                f.addWindowListener(Common.getDefaultWindowListener());
                TreeFlowCanvas lc=new TreeFlowCanvas(f,dr.getTreeRegistry().getRoots());
                lc.pc.setSize(400,300);
                f.add(lc.getComponent()); f.pack(); f.show();
            }
            if (cmd=="lineplot2") {
                int i=0;
                int selC=0, selN=0;
                while (i<vc.getVars()) {
                    if (vc.selMask[i]) {
                        if (vs.at(i).isCat()) selC++;
                        if (vs.at(i).isNum()) selN++;
                    };
                    i++;
                };
                if (selN>1) {
                    SVar[] vars=new SVar[selN-1];
                    SVar idx=null;
                    i=0; int vsc=0, rc=0;
                    while (i<vc.getVars()) {
                        if (vc.selMask[i] &&vs.at(i).isNum()) {
                            if (rc==0) { idx=vs.at(i); rc++; } else vars[vsc++]=vs.at(i);
                        }
                        i++;
                    };
                    TFrame f=new TFrame("Line plot",TFrame.clsLine);
                    f.addWindowListener(Common.getDefaultWindowListener());
                    LineCanvas lc=new LineCanvas(f,idx,vars,vs.getMarker());
                    lc.pc.setSize(400,300);
                    f.add(lc.getComponent()); f.pack(); f.show();
                    f.initPlacement();
                }
            }
            if (cmd=="scatterplot") { // Scatterplot
                int vnr[]=new int[2];
                int i,j=0,tsel=0;
                for(i=0;i<vc.getVars();i++) if (vc.selMask[i]) { vnr[j]=i; j++; tsel++; };
                if (tsel==2) {
                    TFrame f=new TFrame("Scatterplot ("+
                                        vs.at(vnr[1]).getName()+" vs "+
                                        vs.at(vnr[0]).getName()+")",TFrame.clsScatter);
                    f.addWindowListener(Common.getDefaultWindowListener());
                    SectScatterCanvas sc=new SectScatterCanvas(null,win.dr,f,vs.at(vnr[0]),vs.at(vnr[1]),vs.getMarker(),dr.getNodeMarker());
                    if (vs.getMarker()!=null) vs.getMarker().addDepend(sc);
                    sc.pc.setSize(new Dimension(400,300));
                    f.add(sc.getComponent()); f.pack(); f.show();
                    f.initPlacement();
                };
            };
            if (cmd=="boxplot") { // Boxplot
                int bI=0; int bJ=0;
                SVar catVar=null;
                while(bI<vc.getVars()) {
                    if (vc.selMask[bI] && vs.at(bI).isCat()) {
                        catVar=vs.at(bI); break;
                    };
                    bI++;
                };
                if (catVar==null) {
                    while(bJ<vc.getVars()) {
                        if (vc.selMask[bJ]) {
                            TFrame f=new TFrame("Boxplot ("+vs.at(bJ).getName()+")",TFrame.clsBox);
                            f.addWindowListener(Common.getDefaultWindowListener());
                            ParallelAxesCanvas sc=new ParallelAxesCanvas(null,f,vs.at(bJ),vs.getMarker(),ParallelAxesCanvas.TYPE_BOX);
                            if (vs.getMarker()!=null) vs.getMarker().addDepend(sc);
                            sc.pc.setSize(new Dimension(80,300));
                            f.add(sc.getComponent()); f.pack(); f.show();
                            f.initPlacement();
                        };
                        bJ++;
                    };
                } else {
                    int lx=0, ly=0;
                    while(bJ<vc.getVars()) {
                        if (vc.selMask[bJ] && bJ!=bI) {
                            TFrame f=new TFrame("Boxplot ("+vs.at(bJ).getName()+" grouped by "+catVar.getName()+")",TFrame.clsBox);
                            f.addWindowListener(Common.getDefaultWindowListener());
                            ParallelAxesCanvas sc=new ParallelAxesCanvas(null,f,vs.at(bJ),catVar,vs.getMarker(),ParallelAxesCanvas.TYPE_BOX);
                            if (vs.getMarker()!=null) vs.getMarker().addDepend(sc);
                            sc.pc.setSize(new Dimension(40+catVar.getNumCats()*40,300));
                            f.add(sc.getComponent()); f.pack(); f.show();
                            f.initPlacement();
                        };
                        bJ++;
                    };
                };
            };
            if (cmd=="fluct" || cmd=="speckle") { // fluctuation diagram
                int vnr[]=new int[2];
                SVar weight=null;
                int i,j=0,tsel=0;
                for(i=0;i<vc.getVars();i++) if (vc.selMask[i]) {
                    if (cmd=="speckle" && j==2 && weight==null && vs.at(i).isCat()) weight=vs.at(i);
                    if(vs.at(i).isCat() && j<2) { vnr[j]=i; j++; tsel++; };
                    if(!vs.at(i).isCat() && vs.at(i).isNum() && weight==null) weight=vs.at(i);
                }
                    if (tsel==2) {
                        TFrame f=new TFrame(((weight==null)?"":"W")+"FD ("+
                                            vs.at(vnr[1]).getName()+" vs "+
                                            vs.at(vnr[0]).getName()+")"+((weight==null)?"":"*"+weight.getName()),TFrame.clsFD);
                        f.addWindowListener(Common.getDefaultWindowListener());
                        FluctCanvas sc;
                        if (cmd=="speckle" && weight!=null && weight.isCat())
                            sc=new FCCCanvas(null,f,vs.at(vnr[0]),vs.at(vnr[1]),vs.getMarker(),weight);
                        else
                            sc=new FluctCanvas(null,f,vs.at(vnr[0]),vs.at(vnr[1]),vs.getMarker(),weight);
                        if (vs.getMarker()!=null) vs.getMarker().addDepend(sc);
                        sc.pc.setSize(new Dimension(400,300));
                        f.add(sc.getComponent()); f.pack(); f.show();
                        f.initPlacement();
                    };
            };
            if (cmd=="PCP") { //PCP
                int i,j=0,tsel=0;
                for(i=0;i<vc.getVars();i++) if (vc.selMask[i] && vs.at(i).isNum()) tsel++;
                if (tsel>0) {
                    SVar[] vl=new SVar[tsel];
                    for(i=0;i<vc.getVars();i++) if (vc.selMask[i] && vs.at(i).isNum()) {
                        vl[j]=vs.at(i); j++;
                    };
                    TFrame f=new TFrame("Parallel coord. plot",TFrame.clsPCP);
                    f.addWindowListener(Common.getDefaultWindowListener());
                    ParallelAxesCanvas sc=new ParallelAxesCanvas(null,f,vl,vs.getMarker(),ParallelAxesCanvas.TYPE_PCP);
                    if (vs.getMarker()!=null) vs.getMarker().addDepend(sc);
                    sc.pc.setSize(new Dimension(400,300));
                    f.add(sc.getComponent()); f.pack(); f.show();
                    f.initPlacement();
                }
            }
            if (cmd=="hammock") { // Hammock plot
                int i,j=0,tsel=0;
                for(i=0;i<vc.getVars();i++) if (vc.selMask[i] && vs.at(i).isCat()) tsel++;
                if (tsel>0) {
                    SVar[] vl=new SVar[tsel];
                    for(i=0;i<vc.getVars();i++) if (vc.selMask[i] && vs.at(i).isCat()) {
                        vl[j]=vs.at(i); j++;
                    };
                    TFrame f=new TFrame("Hammock plot",TFrame.clsPCP);
                    f.addWindowListener(Common.getDefaultWindowListener());
                    HamCanvas sc=new HamCanvas(null,f,vl,vs.getMarker());
                    if (vs.getMarker()!=null) vs.getMarker().addDepend(sc);
                    sc.pc.setSize(new Dimension(400,300));
                    f.add(sc.getComponent()); f.pack(); f.show();
                    f.initPlacement();
                }
            }
            if (cmd=="xMCP") { // eXperimental MCP
                int i,j=0,tsel=0;
                TreeRegistry tr = new TreeRegistry();
                for(i=0;i<vc.getVars();i++) if (vc.selMask[i] && vs.at(i).isCat()) tsel++;
                if (tsel>1) {
                    SVar[] vl=new SVar[tsel];
                    for(i=0;i<vc.getVars();i++) if (vc.selMask[i] && vs.at(i).isCat()) {
                        vl[j]=vs.at(i); j++;
                    };
                    i=0;
                    while (i<tsel-1) {
                        SNode n = new SNode();
                        RootInfo ri = n.getRootInfo();
                        ri.name = vl[i].getName();
                        ri.prediction = vl[i];
                        ri.response = vl[tsel-1];
                        tr.registerTree(n, ri.name);
                        i++;
                    };
                    
                    TFrame f=new TFrame("Misclassification plot",TFrame.clsMCP);
                    MCPCanvas dc=new MCPCanvas(f,tr,vs.getMarker());
                    f.add(dc.getComponent()); f.addWindowListener(Common.getDefaultWindowListener());
                    dc.pc.setSize(400,300);
                    f.pack(); f.setVisible(true);
                    f.initPlacement();
                } else {
                    TFrame mcpf=new TFrame("Misclassification plot",TFrame.clsMCP);
                    MCPCanvas dc=new MCPCanvas(mcpf,dr.getTreeRegistry(),vs.getMarker());
                    mcpf.add(dc.getComponent()); mcpf.addWindowListener(Common.getDefaultWindowListener());
                    dc.pc.setSize(400,300);
                    mcpf.pack(); mcpf.setVisible(true);
                    mcpf.initPlacement();
                }
            }
            if (cmd=="openModel") {
                Plugin gt=PluginManager.loadPlugin("PluginModelLoader");
                if (gt==null || !gt.initPlugin()) {
                    new MsgDialog(win,"Plugin init failed","Cannot initialize plugin.\n"+((gt==null)?"Model loader plugin not found":gt.getLastError()));
                    return null;
                }

                FileDialog fd=new FileDialog(win,"Select model image file");
                fd.setModal(true);
                fd.show();
                if (fd.getFile()==null) return null;
                    
                String fnam=fd.getDirectory()+fd.getFile();
                gt.setParameter("image",fnam);
                gt.setParameter("dataroot",dr);
                gt.checkParameters();
                if (!gt.pluginDlg(win)) {
                    if (gt.cancel) {
                        gt.donePlugin();
                        return null;
                    };
                    new MsgDialog(win,"Parameter check failed","Some of your selections are invalid.\n"+gt.getLastError());
                    return null;
                }
                gt.execPlugin();
            }
            if (cmd=="growTree") { // grow tree
                ProgressDlg pd=new ProgressDlg("Running tree generation plugin ...");
                pd.setText("Initializing plugin, loading R ...");
                pd.show();
                Plugin gt=PluginManager.loadPlugin("PluginGetTreeR");
                if (gt==null || !gt.initPlugin()) {
                    pd.dispose();
                    new MsgDialog(win,"Plugin init failed","Cannot initialize plugin.\n"+((gt==null)?"Tree generation plugin not found":gt.getLastError()));
                    return null;
                }
                gt.setParameter("dataset",vs);
                gt.checkParameters();
                pd.setVisible(false);
                if (!gt.pluginDlg(win)) {
                    pd.dispose();
                    if (gt.cancel) {
                        gt.donePlugin();
                        return null;
                    };
                    new MsgDialog(win,"Model load failed","Could not load the model image.\n"+gt.getLastError());
                    return null;
                }
                pd.setProgress(40);
                pd.setVisible(true);
                if (!gt.execPlugin()) {
                    pd.dispose();
                    HelpFrame hf=new HelpFrame();
                    hf.t.setText("Tree generation failed.\n"+gt.getLastError()+"\n\nDump of R output (if any):\n"+gt.getParameter("lastdump"));
                    hf.setTitle("Plugin execution failed");
                    //hf.setModal(true);
                    hf.show();
                    return null;
                }
                pd.setProgress(100);
                SNode nr=(SNode)gt.getParameter("root");
                gt.donePlugin();
                if (nr!=null) {
                    genCount++;
                    TFrame fff=new TFrame("Generated_"+genCount,TFrame.clsTree);
                    TreeCanvas tc=Klimt.newTreeDisplay(nr,fff);
                }
                pd.dispose();
            }

            if (cmd=="deriveVar") { // derive variable
                ProgressDlg pd=new ProgressDlg("Running var generation plugin ...");
                pd.setText("Initializing plugin, loading R ...");
                pd.show();
                Plugin gt=PluginManager.loadPlugin("PluginDeriveVar");
                if (gt==null || !gt.initPlugin()) {
                    pd.dispose();
                    new MsgDialog(win,"Plugin init failed","Cannot initialize plugin.\n"+((gt==null)?"Tree generation plugin not found":gt.getLastError()));
                    return null;
                }
                gt.setParameter("dataset",vs);
                gt.checkParameters();
                pd.setVisible(false);
                if (!gt.pluginDlg(win)) {
                    pd.dispose();
                    if (gt.cancel) {
                        gt.donePlugin();
                        return null;
                    };
                    new MsgDialog(win,"Parameter check failed","Some of your selections are invalid.\n"+gt.getLastError());
                    return null;
                }
                pd.setProgress(40);
                pd.setVisible(true);
                if (!gt.execPlugin()) {
                    pd.dispose();
                    HelpFrame hf=new HelpFrame();
                    hf.t.setText("Variable generation failed.\n"+gt.getLastError()+"\n\nDump of R output (if any):\n"+gt.getParameter("lastdump"));
                    hf.setTitle("Plugin execution failed");
                    //hf.setModal(true);
                    hf.show();
                    return null;
                }
                pd.setProgress(100);
                gt.donePlugin();
                pd.dispose();
            }
            if (cmd=="datatab") {
                Plugin p=PluginManager.loadPlugin("PluginTable");
                p.setParameter("dataset",vs);
                p.execPlugin();
            }

            if (cmd=="exportCases") {
                /*
                try {
                    PrintStream p=Tools.getNewOutputStreamDlg(win,"Export selected cases to ...","selected.txt");
                    if (p!=null) {
                        p.println(v[0].getName()+"\t"+v[1].getName());
                        int i=0;
                        for (Enumeration e=v[0].elements(); e.hasMoreElements();) {
                            Object oo=e.nextElement();
                            if (m.at(i))
                                p.println(((oo==null)?"NA":oo.toString())+"\t"+((v[1].at(i)==null)?"NA":v[1].at(i).toString()));
                            i++;
                        };
                        p.close();
                    };
                } catch (Exception eee) {};
                 */
            };

            return null;
        };

        public void actionPerformed(ActionEvent e) {
            if (e==null) return;
            run(e.getSource(),e.getActionCommand());
        };
    };

    /** VarCmdCanvas is the canvas of commands for variables */
    class VarCmdCanvas extends DBCanvas implements MouseListener, Dependent
    {
	/** associated window */
        VarFrame win;
	/** data source */
	SVarSet vs;
	VarCanvas vc;
	Dimension minDim;
        SMarker sm;

	/** constructs a new variable commands canvas for associated tree canvas
	    @param w window in which this canvas is displayed
	    @param p associated tree canvas
	*/
	VarCmdCanvas(VarFrame w, SVarSet dataset) {
	    setBackground(Common.backgroundColor);
	    win=w; vs=dataset;
	    addMouseListener(this);
            vc=w.vc; sm=vs.getMarker();
            if (sm!=null) sm.addDepend(this);
	    minDim=new Dimension(140,132);            
	};

        public void Notifying(NotifyMsg msg, Object o, Vector path) {
            repaint();
        };
        
	public Dimension getMinimumSize() { return minDim; };

	/** implementation of the {@link DBCanvas#paintBuffer} method
	    @param g graphic context to paint on */
	public void paintBuffer(Graphics g) {
	    int totsel=0;
            int selCat=0;
            int selNum=0;
            boolean selMap=false;
	    int i=0;
	    while (i<vc.getVars()) {
                if (vc.selMask[i]) {
                    totsel++;
                    if (vs.at(i).isCat()) selCat++;
                    else if(vs.at(i).isNum()) selNum++;
                    if (vs.at(i).getContentsType()==SVar.CT_Map) selMap=true;
                };
		i++;
	    };

            MenuItem mi=null;
            mi=EzMenu.getItem(win,"histogram");
            if(mi!=null) mi.setEnabled(selNum>0);
            mi=EzMenu.getItem(win,"barchart");
            if(mi!=null) mi.setEnabled(selCat>0);
            mi=EzMenu.getItem(win,"boxplot");
            if(mi!=null) mi.setEnabled(selCat<2 && selNum>0);
            mi=EzMenu.getItem(win,"scatterplot");
            if(mi!=null) mi.setEnabled(selCat+selNum>1);
            mi=EzMenu.getItem(win,"fluct");
            if(mi!=null) mi.setEnabled(selCat==2);
            mi=EzMenu.getItem(win,"speckle");
            if(mi!=null) mi.setEnabled(selCat==3);
            mi=EzMenu.getItem(win,"PCP");
            if(mi!=null) mi.setEnabled(/* selCat+ */ selNum>1);
            mi=EzMenu.getItem(win,"map");
            if(mi!=null) mi.setEnabled(selMap);
            mi=EzMenu.getItem(win,"lineplot");
            if(mi!=null) mi.setEnabled(selNum>0);
            mi=EzMenu.getItem(win,"lineplot2");
            if(mi!=null) mi.setEnabled(selNum>1);
            mi=EzMenu.getItem(win,"kmplot");
            if(mi!=null) mi.setEnabled(selCat==1 && selNum==1);
            mi=EzMenu.getItem(win,"hammock");
            if(mi!=null) mi.setEnabled(selCat>1);
            mi=EzMenu.getItem(win,"export");
            if(mi!=null) mi.setEnabled(selCat+selNum>0);
            
	    Dimension cd=getSize();

            if (Global.useAquaBg) {
                g.setColor(Color.white);
                g.fillRect(0, 0, cd.width, cd.height);
                int y=0;
                g.setColor(Common.aquaBgColor);
                while (y<cd.height-2) {
                    g.fillRect(0,y,cd.width,2); y+=4;
                }
            }
            
	    Font fo=getFont();
	    Font f2=new Font(fo.getName(),Font.BOLD,fo.getSize());
	    Color C_varNam=new Color(0,0,128);
	    Color C_info=new Color(128,0,0);
	    Color C_bg=new Color(255,255,255);
	    Color C_sel=new Color(128,255,128);
	    Color C_frame=new Color(128,128,128);

            g.setColor(Color.black);
            sm = vs.getMarker();
            if (sm!=null)
                g.drawString("Selected "+sm.marked()+" of "+vs.at(0).size()+" cases",10,16);
            else
                g.drawString("Total "+vs.at(0).size()+" cases",10,16);
            
	    i=1;
	    String menu[]={"Exit","Open tree...","Hist/Barchar","Scatterplot","Boxplot","Fluct.Diag.","PCP","Grow tree...","Export..."};
            int j=0;
	    while (j<menu.length) {
		boolean boxValid=false;
		if (j==4 && totsel>0) { /* boxplot */
		    int bI=0, bJ=0, bK=0;
		    boolean crap=false;
		    while(bI<vc.getVars() && bJ<2) {
			if (vc.selMask[bI]) {
			    if (vs.at(bI).isCat()) bJ++;
			    else {
				if (!vs.at(bI).isNum()) { crap=true; break; };			
				bK++;
			    };
			};
			bI++;
		    };
		    if (!crap && bJ<2 && bK>0) boxValid=true;
		};
                if ( j<2 || j==7 ||
                    (j==2 && totsel>0)||boxValid||
                    (j==3 && totsel==2)||
                    (j==5 && (
			      (totsel==2 && selCat==2)||
			      (totsel==3 && selCat==2 && selNum==1)
			      //|| totsel==3 // HACK! just to allow FCC
			      ))||
                    (j==6 && totsel>0)|| 
                    (j==8 && totsel>0)) {
                    g.setColor(C_bg);
                    g.fillRect(5,5+i*17,130,15);
		};
		g.setColor(C_frame);
		g.drawRect(5,5+i*17,130,15);
		g.setFont(f2); g.setColor(Color.black);	
		g.drawString(menu[j],20,17+i*17);
		i++; j++;
	    };
	};
    
	/* mouse actions */

	public void mouseClicked(MouseEvent ev) 
	{
	    if (vs==null) return;

	    int x=ev.getX(), y=ev.getY();
	    int svar=-1;
	    if ((x>5)&&(x<115)) svar=(y-3)/17;
	    int cmd=svar-1;
	    if (cmd==0) {
		if (WinTracker.current!=null)
		    WinTracker.current.disposeAll();
		System.exit(0);
	    };
            if (cmd==1) vc.run(this,ev.isShiftDown()?"openTreeSilently":"openTree");
            if (cmd==2) vc.run(this,"barchart");
            if (cmd==3) vc.run(this,"scatterplot");
            if (cmd==4) vc.run(this,"boxplot");
            if (cmd==5) vc.run(this,"fluct");
            if (cmd==6) vc.run(this,"PCP");
            if (cmd==7) vc.run(this,"growTree");
            if (cmd==8) vc.run(this,"export");
	};
	public void mousePressed(MouseEvent ev) {};
	public void mouseReleased(MouseEvent e) {};
	public void mouseEntered(MouseEvent e) {};
	public void mouseExited(MouseEvent e) {};
    };

    public VarCanvas getVarCanvas() { return vc; };
    public VarCmdCanvas getVarCmdCanvas() { return vcc; };

};
