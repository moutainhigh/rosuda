import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;

/** BarCanvas - implementation of the barcharts
    @version $Id$
*/
class BarCanvas extends PGSCanvas implements Dependent, MouseListener, MouseMotionListener, KeyListener, Commander, ActionListener
{
    /** corresponding variable */
    SVar v;
    /** weight variable for weighted barcharts */
    SVar weight;

    /** corresponding marker */
    SMarker m;
    /** axes */
    Axis ax,ay;

    QueryPopup qi;
    
    Rectangle[] Bars;
    int[] cat_seq;
    String[] cat_nam;
    int cats;

    int botSpace=20;
    int topSpace=10;
    int sideSpace=10;

    int countMax=0;
    int count[];
    int marked[];
    
    // for weighted barcharts
    double c_max;
    double cumulated[];
    double c_marked[];

    Object[] cols; // colors 0=regular sel, 1... sec marks
    boolean hasSec; // has sec marks
    

    int ow=0,oh=0;

    int bars=20;
    boolean dragMode=false;
    boolean isSpine=false;
    int dragBar, dragX, dragY, dragW, dragH, dragNew;

    MenuItem MIspine=null;

    /** creates a (weighted) barchart
	@param f associated frame (or <code>null</code> if common default frame is to be used)
	@param var associated variable
	@param mark associated marker
	@param wvar weight variable for weoghted barcharts or null for normal ones */
    public BarCanvas(Frame f, SVar var, SMarker mark, SVar wvar) {
        super(2); // 2 layers; 0=bars, 1=drag
	v=var; weight=wvar; m=mark; setFrame(f);
	setTitle(((wvar==null)?"":"w.")+"Barchart ("+v.getName()+((wvar==null)?"":("*"+wvar.getName()))+")");
	ax=new Axis(v,Axis.O_X,Axis.T_EqCat);
	ax.addDepend(this);
	if (weight==null)
	    ay=new Axis(v,Axis.O_Y,Axis.T_EqSize);
	else
	    ay=new Axis(weight,Axis.O_Y,Axis.T_Num);
	ay.addDepend(this);
	setBackground(Common.backgroundColor);
	addMouseListener(this);
	addMouseMotionListener(this);
        addKeyListener(this); f.addKeyListener(this);
	cats=v.getNumCats();
	bars=cats;
	if (v.hasMissing()) bars++;
	
	Bars=new Rectangle[bars];
	updateBars();
	MenuBar mb=null;
	String myMenu[]={"+","File","~File.Graph","~Edit","+","View","Spineplot","spine","~Window","0"};
	EzMenu.getEzMenu(f,this,myMenu);
	MIspine=EzMenu.getItem(f,"spine");
	if (weight!=null) MIspine.setEnabled(false);
        qi=new QueryPopup(f,mark==null?null:mark.masterSet,"BarCanvas");
    };
    
    public BarCanvas(Frame f, SVar var, SMarker mark) { this(f,var,mark,null); };

    /** notification handler - rebuilds bars and repaints */
    public void Notifying(NotifyMsg msg, Object o, Vector path) {
	updateBars();
        setUpdateRoot(0);
	repaint();
    };

    /** rebuilds bars */
    public void updateBars() {
	countMax=0; c_max=0;
	Object[] cts=v.getCategories();
	cat_nam=new String[cts.length+1];
        if (bars!=v.getNumCats()) {
            cats=v.getNumCats();
            bars=cats;
            if (v.hasMissing()) bars++;

            Bars=new Rectangle[bars];
        }
	cols=new Object[cats+1]; hasSec=false;
	int j=0;
	while (j<cats) {
	    cat_nam[j]=cts[j].toString();
	    if (m!=null)
		cols[j]=new int[m.maxMark+1];
	    j++;
        };
        cat_nam[j]="n/a"; // if you see this category, then somehting's wrong as getCatIndex returns -1
	if (weight==null) {
	    count=new int[bars];
	    marked=new int[bars];
	    j=0;
	    while (j<v.size()) {
		int i=v.getCatIndex(j);
		if (i==-1) i=cats;
		if (m!=null) {
		    int[] cl=(int[])cols[i];
		    int k=m.get(j);
		    if (k==-1) cl[0]++;
		    else if (k!=0 && k<cl.length) { cl[k]++; hasSec=true; }
		}
		count[i]++;
		if ((m!=null)&&(m.at(j))) marked[i]++;
		if (count[i]>countMax) countMax=count[i];
		j++;
	    };
	    ay.setValueRange(countMax);
	} else {
	    cumulated=new double[bars];
	    c_marked=new double[bars];
	    j=0;
	    while (j<v.size()) {
		int i=v.getCatIndex(j);
		if (i==-1) i=cats;
		double wval=weight.atD(j);
		if (wval<0) wval=-wval; // weight are always treated as positive
		cumulated[i]+=wval;
		if ((m!=null)&&(m.at(j))) c_marked[i]+=wval;
		if (cumulated[i]>c_max) c_max=cumulated[i];
		j++;
	    };
	    ay.setValueRange(0,c_max);	    
	};
    };

    public Dimension getMinimumSize() { return new Dimension(sideSpace*2+30,topSpace+botSpace+30); };

    /** actual paint method */
    public void paintPoGraSS(PoGraSS g) {
	Rectangle r=getBounds();
	int w=r.width, h=r.height;
	if (oh!=h) 
	    ay.setGeometry(Axis.O_Y,botSpace,h-topSpace-botSpace);
	if (ow!=w)
	    ax.setGeometry(Axis.O_X,sideSpace,w-2*sideSpace);
	//System.out.println("BarCanvas.paint:\n ax="+ax.toString()+"\n ay="+ay.toString());
	ow=w; oh=h;
	int basey=h-botSpace;
	g.setBounds(w,h);
	g.begin();
	g.defineColor("axes",0,0,0);
	g.defineColor("outline",0,0,0);
	g.defineColor("fill",255,255,255);
	g.defineColor("sel",128,255,128);
	g.defineColor("drag",255,0,0);

	if (bars==0) return;
	
	g.drawLine(sideSpace,basey,w-2*sideSpace,basey); 

	int i=0;
	int lh=(weight==null)?ay.getCasePos(0):ay.getValuePos(0);
	while(i<bars) {
	    g.setColor("fill");
	    int cl=ax.getCatLow(i);
	    int cu=ax.getCatUp(i);
	    int cd=cu-cl;
	    cu-=cd/10;
	    cl+=cd/10;
	    
	    int ch=0;
	    if (weight==null)
		ch=ay.getCasePos(count[i]);
	    else
		ch=ay.getValuePos(cumulated[i]);
	    if (isSpine) ch=lh+ay.gLen;
	    //System.out.println(">>Bar["+i+"] cl="+cl+", cu="+cu+", ch="+ch+" (w="+w+",h="+h+")");
	    g.fillRect(cl,h-ch,cu-cl,ch-lh);
	    Bars[i]=new Rectangle(cl,h-ch,cu-cl,ch-lh);
	    
	    if ((weight==null && (marked[i]>0 || hasSec))||(weight!=null && c_marked[i]>0)) {
		if (weight==null && hasSec) {
		    int agg=0, lyp=0;
		    int j=0;
		    int[] col=(int[])cols[i];
		    lyp=ay.getCasePos(0);
		    while (j<col.length) {
			if (col[j]>0) {
			    agg+=col[j];
			    int ly=ay.getCasePos(agg);
			    if (ly!=lyp) {
				if (j==0)
				    g.setColor("sel");
				else
				    g.setColor(ColorBridge.main.getColor(j));
				g.fillRect(cl,h-ly,cu-cl,ly-lyp);
				lyp=ly;
			    }
			}
			j++;
		    }
		} else {
		    int mh=0;
		    if (isSpine)
			mh=lh+((weight==null)?(ch-lh)*marked[i]/count[i]:(int)(((double)(ch-lh))*c_marked[i]/cumulated[i]));
		    else
			mh=(weight==null)?ay.getCasePos(marked[i]):ay.getValuePos(c_marked[i]);
		    g.setColor("sel");
		    g.fillRect(cl,h-mh,cu-cl,mh-lh);
		}
	    };
	    
	    g.setColor("outline");
	    g.drawRect(cl,h-ch,cu-cl,ch-lh);
	    if (cu-cl<cat_nam[i].length()*8)
		g.drawString(Common.getTriGraph(cat_nam[i]),cl+5,h-botSpace/2);
	    else
		g.drawString(cat_nam[i],cl+5,h-botSpace/2);
	    i++;
	};

	if(dragMode) {
            nextLayer(g);
	    int myX=ax.getCatCenter(ax.getCatByPos(dragX));
	    g.setColor(192,192,192);
	    g.fillRect(dragX-dragW/2,basey-dragH,dragW,dragH);	
	    g.setColor("outline");
	    g.drawRect(dragX-dragW/2,basey-dragH,dragW,dragH);	
	    g.setColor("drag");
	    g.fillRect(myX-dragW/2,basey,dragW,4);
	};
	
	g.end();
        setUpdateRoot(2);
    };
    
    public void mouseClicked(MouseEvent ev) 
    {
	int x=ev.getX(), y=ev.getY();
	Point cl=getFrame().getLocation();
	Point tl=getLocation(); cl.x+=tl.x; cl.y+=tl.y;
        int i=0, bars=cats;
        boolean setTo=false;

        if (Common.DEBUG>0) {
            String mods="";
            if (ev.isShiftDown()) mods+=" SHIFT";
            if (ev.isAltDown()) mods+=" ALT";
            if (ev.isControlDown()) mods+=" CTRL";
            if (ev.isMetaDown()) mods+=" META";
            if (ev.isAltGraphDown()) mods+=" ALT.GR";
            if ((ev.getModifiers()&MouseEvent.BUTTON1_MASK)==MouseEvent.BUTTON1_MASK) mods+=" M1";
            if ((ev.getModifiers()&MouseEvent.BUTTON2_MASK)==MouseEvent.BUTTON2_MASK) mods+=" M2";
            if ((ev.getModifiers()&MouseEvent.BUTTON3_MASK)==MouseEvent.BUTTON3_MASK) mods+=" M3";
            if (ev.isPopupTrigger()) mods+=" POPUP";
            System.out.println("Event:"+ev+mods);
        }
        
        boolean effect=false, hideQI=true;
        boolean actionSelect=Common.isSelectTrigger(ev);
        boolean actionQuery=Common.isQueryTrigger(ev);
        boolean actionExtQuery=Common.isExtQuery(ev);
        if (Common.DEBUG>0)
            System.out.println("select="+actionSelect+", query="+actionQuery+", isMac="+Common.isMac);
        
        //System.out.println("BarCanvas.mouseClicked; Alt="+ev.isAltDown()+", Ctrl="+ev.isControlDown()+
	//		   ", Shift="+ev.isShiftDown()+", popup="+ev.isPopupTrigger());
        if (actionQuery || actionSelect) {
            int selMode=Common.getSelectMode(ev);
            if (selMode>1) setTo=true;
            while (i<bars) {
                if (Bars[i]!=null && Bars[i].contains(x,y)) {
                    if (actionQuery) {
                        String qs="Name: "+cat_nam[i]+"\n";
                        if (weight==null) {
                            if (actionExtQuery) {
                                qs+="consists of "+count[i]+" cases ("+
                                Tools.getDisplayableValue(100.0*((double)count[i])/((double)v.size()),2)+
                                "% of total)\nSelected "+marked[i]+" cases ("+
                                Tools.getDisplayableValue(100.0*((double)marked[i])/((double)count[i]),2)+
                                "% of this cat., " +
                                Tools.getDisplayableValue(100.0*((double)marked[i])/((double)v.size()),2)+"% of total)";
                            } else {
                                qs+="Selected "+marked[i]+" of "+count[i];
                            };
                        } else {
                        };
                        qi.setContent(qs);
                        qi.setLocation(cl.x+x,cl.y+y);
                        qi.show(); hideQI=false;
                    } else {
                        effect=true;
                        if (selMode==0) m.selectNone();
                        int j=0, pts=v.size();
                        while (j<pts) {
                            if (v.getCatIndex(j)==i)
                                m.set(j,m.at(j)?setTo:true);
                            j++;
                        };
                        break; // one can be inside one bar only
                    };
                };
                i++;
            };
        }
        if (effect) m.NotifyAll(new NotifyMsg(m,Common.NM_MarkerChange));
	if (hideQI) qi.hide();
    };

    boolean inQuery=false;
    
    public void mousePressed(MouseEvent ev) {
	int x=ev.getX(), y=ev.getY();
	int i=0, bars=cats, setTo=0;
	while (i<bars) {
	    if (Bars[i]!=null && Bars[i].contains(x,y)) {
		dragMode=true;
		dragBar=i; dragNew=i; dragW=Bars[i].width; dragH=Bars[i].height;
		if (!inQuery) setCursor(Common.cur_hand);		
		break;
	    };
	    i++;
	};	
    };
    
    public void mouseReleased(MouseEvent e) {
	if (dragMode) {
	    dragMode=false;
	    if (!inQuery) setCursor(Common.cur_arrow);
            setUpdateRoot(1);
	    if (dragNew!=dragBar) {
		ax.moveCat(dragBar,ax.getCatSeqIndex(dragNew));
		updateBars();
                setUpdateRoot(0);
	    };
	    repaint();
	};
    };

    public void mouseDragged(MouseEvent e) 
    {
	if (dragMode) {
	    dragX=e.getX(); dragY=e.getY();
	    dragNew=ax.getCatByPos(dragX);
	    //System.out.println("dragX="+dragX+" dragY="+dragY+" dragNew="+dragNew);
            setUpdateRoot(1);
	    repaint();
	};
    };

    public void mouseMoved(MouseEvent ev) {};

    public void mouseEntered(MouseEvent e) {};
    public void mouseExited(MouseEvent e) {};

    public void keyTyped(KeyEvent e) 
    {
	if (e.getKeyChar()=='R') run(this,"rotate");
	if (e.getKeyChar()=='s') run(this,"spine");
	//if (e.getKeyChar()=='l') run(this,"labels");
	if (e.getKeyChar()=='P') run(this,"print");
	if (e.getKeyChar()=='X') run(this,"exportPGS");
	if (e.getKeyChar()=='C') run(this,"exportCases");

	if (e.getKeyChar()>='0' && e.getKeyChar()<='9') {
	    m.setSelected(e.getKeyChar()-'0');
	    m.NotifyAll(new NotifyMsg(this,Common.NM_SecMarkerChange));
	    setUpdateRoot(0);
	    repaint();
	}
	    
    };

    public void keyPressed(KeyEvent e) {
        int kc=e.getKeyCode();
        if (kc==KeyEvent.VK_ALT) {
            setCursor(Common.cur_query);
            inQuery=true;
        }
    };

    public void keyReleased(KeyEvent e) {
        int kc=e.getKeyCode();
        if (kc==KeyEvent.VK_ALT) {
            setCursor(Common.cur_arrow);
            inQuery=false;
        }
    };

    public Object run(Object o, String cmd) {
	super.run(o,cmd);
        if (m!=null) m.run(o,cmd);
	if (cmd=="rotate") {
	    //    rotate();
	};
	if (cmd=="labels") {
	    //    showLabels=!showLabels;
            setUpdateRoot(0);
	    repaint();
	};
	if (cmd=="print") run(o,"exportPS");
	if (cmd=="spine") {
	    if (isSpine) {
		ax.setType(Axis.T_EqCat);
		MIspine.setLabel("Spineplot");
		isSpine=false;
	    } else {
		ax.setType(Axis.T_PropCat);
		MIspine.setLabel("Barchart");
		isSpine=true;
	    };
	};
        if (cmd=="exportCases") {
	    try {
		PrintStream p=Tools.getNewOutputStreamDlg(myFrame,"Export selected cases to ...","selected.txt");
		if (p!=null) {
		    p.println(v.getName());
		    int i=0;
		    for (Enumeration e=v.elements(); e.hasMoreElements();) {
			Object oo=e.nextElement();
			if (m.at(i)) {
			    if (oo!=null)
				p.println(oo.toString());
			    else 
				p.println("NA");
			};
			i++;
		    };
		    p.close();
		};
	    } catch (Exception eee) {};
	};
	if (cmd=="exit") WinTracker.current.Exit();
	return null;
    };

    public void actionPerformed(ActionEvent e) {
	if (e==null) return;
	run(e.getSource(),e.getActionCommand());
    };
}
