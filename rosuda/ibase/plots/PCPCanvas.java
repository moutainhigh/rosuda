package org.rosuda.ibase.plots;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.PrintStream;

import org.rosuda.ibase.*;
import org.rosuda.ibase.toolkit.*;
import org.rosuda.pograss.*;
import org.rosuda.util.*;

/** implementation of line plot
    @version $Id$
*/
public class PCPCanvas extends PGSCanvas implements Dependent, MouseListener, MouseMotionListener, KeyListener, ActionListener, Commander
{
    /** variables; 0=x, 1,2,3...=Y */
    SVar v[];
    /** associated marker */
    SMarker m;

    /** flag whether axis labels should be shown */
    boolean showLabels=true;

    /** flag whether alternative selection style should be used */
    boolean selRed=false;

    /** use trigraph for X axis in case X is categorical */
    boolean useX3=false; 
    
    /** is NA represented as 0? (false=NA's are not drawn at all) */
    boolean na0=true;

    boolean drawPoints=false;
    boolean drawAxes=false;
    
    /** array of axes */
    Axis A[];

    boolean commonScale=true;
    boolean drawHidden=true; // if true then hidden lines are drawn (default because if set to false, one more layer has to be updated all the time; export functions may want to set it to false for output omtimization)
    
    int x1, y1, x2, y2;
    boolean drag;

    MenuItem MIlabels=null;

    int X,Y,W,H, TW,TH;
    double totMin, totMax;
    /** create a new PCP
	@param f associated frame (or <code>null</code> if none)
	@param yvs list of variables
	@param mark associated marker */
    public PCPCanvas(Frame f, SVar[] yvs, SMarker mark) {
        super(3); // 3 layers; 0=base+points, 1=selected, 2=drag
	setFrame(f); setTitle("PCP");
	v=new SVar[yvs.length+1];
	A=new Axis[yvs.length+1];
	m=mark;
	int i=0;
        SVar xv=new SVarObj("PCP.index",true);
	while(i<yvs.length) {
	    if (yvs[i].isNum()) {
		if (i==0) {
		    totMin=yvs[i].getMin(); totMax=yvs[i].getMax();
		} else {
		    if (yvs[i].getMin()<totMin) totMin=yvs[i].getMin();
		    if (yvs[i].getMax()>totMax) totMax=yvs[i].getMax();
		};
	    }
            xv.add(yvs[i].getName());
	    v[i+1]=yvs[i]; i++;
	};
	A[1]=new Axis(yvs[0],Axis.O_Y,Axis.T_Num); A[1].addDepend(this);
	A[1].setValueRange(totMin,totMax-totMin);
	v[0]=xv; A[0]=new Axis(v[0],Axis.O_X,v[0].isCat()?Axis.T_EqCat:Axis.T_Num); A[0].addDepend(this);
	ax=A[0];
	ay=A[1];
	setBackground(Common.backgroundColor);
	drag=false;
	addMouseListener(this);
	addMouseMotionListener(this);
	addKeyListener(this); f.addKeyListener(this);
	MenuBar mb=null;
	String myMenu[]={"+","File","~File.Graph","~Edit","+","View","Hide labels","labels","Toggle hilight. style","selRed","Toggle nodes","togglePts","Toggle axes","toggleAxes","-","Common scale on/off","common","~Window","0"};
	EzMenu.getEzMenu(f,this,myMenu);
	MIlabels=EzMenu.getItem(f,"labels");	
    };

    public void setCommonScale(boolean cs) {
	if (cs==commonScale) return;
	commonScale=cs;
	if (cs) {
	    A[1].setValueRange(totMin,totMax-totMin);
	    //TODO: notify!
	    setUpdateRoot(0); repaint();
	    return;
	}
	if (A[A.length-1]==null) {
	    Dimension Dsize=getSize();
	    int w=Dsize.width, h=Dsize.height;
	    int innerL=30, innerB=30, lshift=0;
	    int innerW=w-innerL-10, innerH=h-innerB-10;

	    int i=2;
	    while (i<A.length) {
		A[i]=new Axis(v[i],Axis.O_Y,v[i].isCat()?Axis.T_EqCat:Axis.T_Num);
		A[i].setGeometry(Axis.O_Y,innerB,innerH);
		A[i].addDepend(this);
		i++;
	    }
	};
	A[1].setDefaultRange();
	setUpdateRoot(0); repaint();
    }

    public Dimension getMinimumSize() { return new Dimension(60,50); };

    public void Notifying(NotifyMsg msg, Object o, Vector path) {
        setUpdateRoot((msg.getMessageID()==Common.NM_MarkerChange)?1:0);
        repaint();
    };

    public void paintPoGraSS(PoGraSS g) {
	Rectangle r=getBounds();
	g.setBounds(r.width,r.height);
	g.begin();
	g.defineColor("white",255,255,255);
	if (selRed)
	    g.defineColor("marked",255,0,0);
	else
	    g.defineColor("marked",Common.selectColor.getRed(),Common.selectColor.getGreen(),Common.selectColor.getBlue());
        g.defineColor("axis",192,192,192);
	g.defineColor("black",0,0,0);
	g.defineColor("outline",0,0,0);
	g.defineColor("point",0,0,128);	
	g.defineColor("red",255,0,0);
	g.defineColor("line",128,128,192); // color of line plot
	g.defineColor("lines",96,96,255);	
	g.defineColor("selText",255,0,0);
	g.defineColor("selBg",255,255,192);
	g.defineColor("splitRects",128,128,255);
        float[] scc=Common.selectColor.getRGBComponents(null);
        g.defineColor("aSelBg",scc[0],scc[1],scc[2],0.3f);

	Dimension Dsize=getSize();
	if (Dsize.width!=TW || Dsize.height!=TH) {
	    int w=Dsize.width, h=Dsize.height;
	    TW=w; TH=h;
	    int innerL=30, innerB=30, lshift=0;
	    int innerW=w-innerL-10, innerH=h-innerB-10;
	
	    A[0].setGeometry(Axis.O_X,X=innerL,W=innerW);
	    int i=1;
	    while (i<A.length) {
		if (A[i]!=null)
		    A[i].setGeometry(Axis.O_Y,innerB,H=innerH);
		i++;
	    }
	    Y=TH-innerB-innerH;
	};

	if (TW<50||TH<50) {
	    g.setColor("red");
	    g.drawLine(0,0,TW,TH); 
	    g.drawLine(0,TH,TW,0); 
	    return;
	};

	g.setColor("white");
	g.fillRect(X,Y,W,H);
        g.setColor("black");
        g.drawLine(X,Y,X,Y+H);
        g.drawLine(X,Y+H,X+W,Y+H);

	/* draw ticks and labels for X axis */
        {
            double f=A[0].getSensibleTickDistance(50,26);
            double fi=A[0].getSensibleTickStart(f);
            //if (Common.DEBUG>0)
            //System.out.println("SP.A[0]:"+A[0].toString()+", distance="+f+", start="+fi);
            while (fi<A[0].vBegin+A[0].vLen) {
                int t=A[0].getValuePos(fi);
                g.drawLine(t,Y+H,t,Y+H+5);
                if (showLabels)
                    g.drawString(v[0].isCat()?((useX3)?Common.getTriGraph(v[0].getCatAt((int)fi).toString()):v[0].getCatAt((int)fi).toString()):
                                 A[0].getDisplayableValue(fi),t-5,Y+H+20);
                fi+=f;
            };
        }

	/* draw ticks and labels for Y axis */
        if (commonScale) {
            double f=A[1].getSensibleTickDistance(50,18);
            double fi=A[1].getSensibleTickStart(f);
            //if (Common.DEBUG>0)
            //System.out.println("SP.A[1]:"+A[1].toString()+", distance="+f+", start="+fi);
            while (fi<A[1].vBegin+A[1].vLen) {
                int t=TH-A[1].getValuePos(fi);
                g.drawLine(X-5,t,X,t);
                if(showLabels)
                    g.drawString(v[1].isCat()?Common.getTriGraph(v[1].getCatAt((int)fi).toString()):A[1].getDisplayableValue(fi),X-25,t+5);
                fi+=f;
            };
        }

        if (drawAxes) {
            g.setColor("axis");
            int xx=0;
            while (xx<v[0].getNumCats()) {
                int t=A[0].getCatCenter(xx++);
                g.drawLine(t,Y,t,Y+H);
            }
        }
        
        g.setColor("line");
	for (int j=2;j<v.length;j++) {
	    for (int i=0;i<v[1].size();i++)
                if ((drawHidden || !m.at(i)) && (na0 || (v[j-1].at(i)!=null && v[j].at(i)!=null))) {
                    g.drawLine(A[0].getCatCenter(j-2),TH-A[commonScale?1:j-1].getValuePos(v[j-1].atD(i)),
                               A[0].getCatCenter(j-1),TH-A[commonScale?1:j].getValuePos(v[j].atD(i)));
                    if (drawPoints) {
                        int x=A[0].getCatCenter(j-2); int y=TH-A[commonScale?1:j-1].getValuePos(v[j-1].atD(i));
                        g.fillOval(x-1,y-1,3,3);
                    }
                }                    
	}
	
        g.nextLayer();
        
        if (m.marked()>0) {
            g.setColor("marked");
            for (int j=2;j<v.length;j++) {
                for (int i=0;i<v[1].size();i++)
                    if (m.at(i) && (na0 || (v[j-1].at(i)!=null && v[j].at(i)!=null))) {
			g.drawLine(A[0].getCatCenter(j-2),TH-A[commonScale?1:j-1].getValuePos(v[j-1].atD(i)),
				   A[0].getCatCenter(j-1),TH-A[commonScale?1:j].getValuePos(v[j].atD(i)));
                        if (drawPoints) {
                            int x=A[0].getCatCenter(j-2); int y=TH-A[commonScale?1:j-1].getValuePos(v[j-1].atD(i));
                            g.fillOval(x-1,y-1,3,3);
                        }
                    }
            }
        }

        if (drag) {
            g.nextLayer();
            int dx1=A[0].clip(x1),dy1=TH-A[1].clip(TH-y1),
		dx2=A[0].clip(x2),dy2=TH-A[1].clip(TH-y2);
	    if (dx1>dx2) { int h=dx1; dx1=dx2; dx2=h; };
	    if (dy1>dy2) { int h=dy1; dy1=dy2; dy2=h; };
	    g.setColor("aSelBg");
	    g.fillRect(dx1,dy1,dx2-dx1,dy2-dy1);
	    g.setColor("black");
	    g.drawRect(dx1,dy1,dx2-dx1,dy2-dy1);
	};

	g.end();
        setUpdateRoot(3); // by default no repaint is necessary unless resize occurs
    };

    public void mouseClicked(MouseEvent ev) 
    {
	int x=ev.getX(), y=ev.getY();
	x1=x-2; y1=y-2; x2=x+3; y2=y+3; drag=true; mouseReleased(ev);
    };

    public void mousePressed(MouseEvent ev) 
    {	
	x1=ev.getX(); y1=ev.getY();
	drag=true;
    };
    public void mouseReleased(MouseEvent e)
    {
	int X1=x1, Y1=y1, X2=x2, Y2=y2;
	if (x1>x2) { X2=x1; X1=x2; };
	if (y1>y2) { Y2=y1; Y1=y2; };
	Rectangle sel=new Rectangle(X1,Y1,X2-X1,Y2-Y1);

	boolean setTo=false;
	if (e.isControlDown()) setTo=true;
	if (!e.isShiftDown()) m.selectNone();
	
	drag=false; 
	for (int j=1;j<v.length;j++)
	    for (int i=0;i<v[1].size();i++) {
		int x=A[0].getCatCenter(j-1);
		int y=TH-A[commonScale?1:j].getValuePos(v[j].atD(i));
		if (x>=X1 && x<=X2 && y>=Y1 && y<=Y2)
		    m.set(i,m.at(i)?setTo:true);
	    }

	m.NotifyAll(new NotifyMsg(m,Common.NM_MarkerChange));
        setUpdateRoot(1);
	repaint();	
    };
    public void mouseEntered(MouseEvent e) {};
    public void mouseExited(MouseEvent e) {};
    public void mouseDragged(MouseEvent e) 
    {
	if (drag) {
	    int x=e.getX(), y=e.getY();
	    if (x!=x2 || y!=y2) {
		x2=x; y2=y;
                setUpdateRoot(2);
		repaint();
	    };
	};
    };
    public void mouseMoved(MouseEvent ev) {};

    public void keyTyped(KeyEvent e) 
    {
	if (e.getKeyChar()=='l') run(this,"labels");
	if (e.getKeyChar()=='P') run(this,"print");
	if (e.getKeyChar()=='X') run(this,"exportPGS");
	if (e.getKeyChar()=='C') run(this,"exportCases");
	if (e.getKeyChar()=='e') run(this,"selRed");
	if (e.getKeyChar()=='t') run(this,"trigraph");
	if (e.getKeyChar()=='c') run(this,"common");
	if (e.getKeyChar()=='n') run(this,"toggleNA");
	if (e.getKeyChar()=='S') run(this,"scaleDlg");
    };
    public void keyPressed(KeyEvent e) {};
    public void keyReleased(KeyEvent e) {};

    public Object run(Object o, String cmd) {
	if (cmd=="print") { drawHidden=false; run(o,"exportPS"); drawHidden=true; return this; }
	super.run(o,cmd);
        if (m!=null) m.run(o,cmd);
	if (cmd=="labels") {
	    showLabels=!showLabels;
	    MIlabels.setLabel((showLabels)?"Hide labels":"Show labels");
            setUpdateRoot(0);
            repaint();
	};
	if (cmd=="exit") WinTracker.current.Exit();
        if (cmd=="selRed") { selRed=!selRed; setUpdateRoot(1); repaint(); };
	if (cmd=="common") { setCommonScale(!commonScale); }
        if (cmd=="trigraph") { useX3=!useX3; setUpdateRoot(0); repaint(); }
        if (cmd=="toggleNA") { na0=!na0; setUpdateRoot(0); repaint(); }
        if (cmd=="togglePts") { drawPoints=!drawPoints; setUpdateRoot(0); repaint(); }
        if (cmd=="toggleAxes") { drawAxes=!drawAxes; setUpdateRoot(0); repaint(); }
        if (cmd=="exportCases") {
	    try {
		PrintStream p=Tools.getNewOutputStreamDlg(myFrame,"Export selected cases to ...","selected.txt");
		if (p!=null) {
		    p.println(v[0].getName()+"\t"+v[1].getName());
                    int i=0, sz=v[0].size();
                    while(i<sz) {
                        if (m.at(i)) {
                            Object oo=v[0].at(i);
			    p.println(((oo==null)?"NA":oo.toString())+"\t"+((v[1].at(i)==null)?"NA":v[1].at(i).toString()));
                        }
			i++;
		    }
		    p.close();
		}
	    } catch (Exception eee) {}
	}
        if (cmd=="scaleDlg" && commonScale) {
            RespDialog d=new RespDialog(myFrame,"Set y scale",true,RespDialog.okCancel);
            Panel cp=d.getContentPanel();
            cp.add(new Label("begin: "));
            TextField tw=new TextField(""+A[1].vBegin,6);
            TextField th=new TextField(""+(A[1].vBegin+A[1].vLen),6);
            cp.add(tw);
            cp.add(new Label(", end: "));
            cp.add(th);
            d.pack();
            d.setVisible(true);
            if (!cancel) {
                double vb=Tools.parseDouble(tw.getText());
                double ve=Tools.parseDouble(th.getText());
                if (ve-vb>0) A[1].setValueRange(vb,ve-vb);
                if (myFrame!=null) myFrame.pack();
            }
            d.dispose();
        }
	
	return null;
    }

    public void actionPerformed(ActionEvent e) {
	if (e==null) return;
	run(e.getSource(),e.getActionCommand());
    };
};
