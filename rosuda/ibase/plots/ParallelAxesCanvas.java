package org.rosuda.ibase.plots;
import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Label;
import java.awt.MenuItem;
import java.awt.Panel;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.TextField;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Vector;

import org.rosuda.ibase.*;
import org.rosuda.ibase.toolkit.*;
import org.rosuda.ibase.toolkit.PGSCanvas.*;
import org.rosuda.pograss.PoGraSS;
import org.rosuda.util.RespDialog;
import org.rosuda.util.SpacingPanel;
import org.rosuda.util.Tools;

public class ParallelAxesCanvas extends BaseCanvas {
    
    protected int mouseX;
    protected int mouseY;
    
    boolean valid=true;
    
    public static final int TYPE_PCP=0;
    public static final int TYPE_BOX=1;
    protected int type;
    
    /**
     * width, height and margins fields
     */
    private int MINWIDTH=60;
    private int MINHEIGHT=50;
    
    int bigMLeft=30;
    int bigMTop=20;
    int bigMBottom=20;
    int bigMRight=30;
    int smallMLeft=10;
    int smallMTop=10;
    int smallMBottom=10;
    int smallMRight=10;
    
    int defaultMLeft;
    int defaultMRight;
    int defaultMTop;
    int defaultMBottom;
    
    int TW, TH;
    
    
    /**
     * axes and labels fields
     */
    boolean useX3=false;
    boolean drawAxes=false;
    boolean commonScale=false;
    protected boolean useRegularPositioning=false;
    int leftGap=7;
    int rightGap=7;
    
    /**
     * variables fields
     */
    
    /** y variables */
    SVar v[];
    /** x variable */
    SVar xv;
    /** categorical variable */
    SVar cv;
    private double totMin;
    private double totMax;
    
    /**
     * menu and command fields
     */
    private static final String M_PLUS = "+";
    private static final String M_MINUS = "-";
    private static final String M_SET1 = "set1";
    private static final String M_SET64 = "set64";
    private static final String M_RESET = "reset";
    private static final String M_LABELS = "labels";
    private static final String M_TRIGRAPH = "trigraph";
    private static final String M_SHOWDOTS = "Show dots";
    private static final String M_TOGGLEPTS = "togglePts";
    private static final String M_NODESIZEUP = "nodeSizeUp";
    private static final String M_NODESIZEDOWN = "nodeSizeDown";
    private static final String M_SHOWAXES = "Show axes";
    private static final String M_TOGGLEAXES = "toggleAxes";
    private static final String M_HIDELINES = "Hide lines";
    private static final String M_TOGGLELINES = "toggleLines";
    private static final String M_HIDENALINES = "hideNAlines";
    private static final String M_COMMON = "common";
    private static final String M_YRANGEDLG = "YrangeDlg";
    private static final String M_SCALEDLG = "scaleDlg";
    private static final String M_ALPHADOWN = "alphaDown";
    private static final String M_ALPHAUP = "alphaUp";
    private static final String M_TRANSHIGHL = "transparentHighlighting";
    
    private MenuItem MIlabels=null;
    private MenuItem MIdots=null;
    private MenuItem MIaxes=null;
    private MenuItem MIlines=null;
    private MenuItem MItrigraph=null;
    private MenuItem MInodeSizeUp=null;
    private MenuItem MInodeSizeDown=null;
    private MenuItem MIhideNAlines=null;
    private MenuItem MItransHighl=null;
    
    /**
     * Box plot specific fields
     */
    int boxwidth=20;
    final int MAX_BOXWIDTH=32;
    final int MIN_BOXWIDTH=4;
    /** if <code>true</code> then side-by-side boxplots grouped by {@link #cv} are drawn,
     * otherwise draw just a single boxpolot */
    boolean vsCat=false;
    boolean dragMode=false;
    boolean vertical=true;
    
    // for vsCat version
    int rk[][];
    int rs[];
    int cs;
    Object cats[];
    OrdStats oss[];
    
    // for plain version
    OrdStats OSdata;
    
    // Array mapping each PPrimBox to the OrdStats object which contains its selections
    OrdStats markStats[];
    
    /**
     * PCP specific fields
     */
    boolean drawPoints=false;
    boolean drawLines=true;
    boolean drawNAlines=true;
    boolean drawHidden=true;
    
    int nodeSize=2;
    
    ParallelAxesCanvas(final PlotComponent ppc, final Frame f, final SVar var, final SVar cvar, final SMarker mark) {
        super(ppc,f,mark);
        
        initFlagsAndFields();
        
        allowDragMove=true;
        objectClipping=true;
        commonScale=false;
        
        mBottom=smallMBottom;
        mTop=smallMTop;
        mLeft=smallMLeft;
        mRight=smallMRight;
        
        v = new SVar[]{var};
        cv = cvar;
        
        xv=new SVarObj(getShortClassName() + ".index",true);
        for(int i=0; i<cv.getNumCats(); i++){
            xv.add(cv.getCatAt(i).toString());
        }
        ax=new Axis(xv,Axis.O_X,xv.isCat()?Axis.T_EqCat:Axis.T_Num); ax.addDepend(this);
        ay=new Axis(v[0],Axis.O_Y,Axis.T_Num); ay.addDepend(this);
        // get some space around (this comes from the scatterplots)
        ay.setValueRange(v[0].getMin()-(v[0].getMax()-v[0].getMin())/20,(v[0].getMax()-v[0].getMin())*1.1);
        
        createMenu(f);
        setCommonScale(commonScale);
        EzMenu.getItem(getFrame(),M_COMMON).setEnabled(false);
    }
    
    /** basic constructor. Every subclass must call this constructor
     * @param f frame owning this canvas. since BaseCanvas itself doesn't modify any attribute of the frame except for title it is possible to put more canvases into one frame. This doesn't have to hold for subclasses, especially those providing their own menus.
     * @param mark marker which will be used for selection/linked highlighting
     */
    ParallelAxesCanvas(final PlotComponent ppc, final Frame f, final SVar[] yvs, final SMarker mark) {
        super(ppc, f, mark);
        
        initFlagsAndFields();
        
        allowDragMove=true;
        objectClipping=true;
        if(yvs.length==1) commonScale=true;
        
        mBottom=smallMBottom;
        mTop=smallMTop;
        mLeft=smallMLeft;
        mRight=smallMRight;
        
        v=new SVar[yvs.length];
        opAy=new Axis[yvs.length-1];
        
        xv=new SVarObj(getShortClassName() + ".index",true);
        int i = 0;
        while(i<yvs.length) {
            if (yvs[i].isNum()) {
                if (i==0) {
                    totMin=yvs[i].getMin(); totMax=yvs[i].getMax();
                } else {
                    if (yvs[i].getMin()<totMin) totMin=yvs[i].getMin();
                    if (yvs[i].getMax()>totMax) totMax=yvs[i].getMax();
                }
            }
            xv.add(yvs[i].getName());
            v[i]=yvs[i]; i++;
        }
        ax=new Axis(xv,Axis.O_X,xv.isCat()?Axis.T_EqCat:Axis.T_Num); ax.addDepend(this);
        ay=new Axis(yvs[0],Axis.O_Y,Axis.T_Num); ay.addDepend(this);
        ay.setValueRange(totMin-(totMax-totMin)/20,(totMax-totMin)*1.1);
        
        createMenu(f);
        setCommonScale(commonScale);
    }
    
    private void createMenu(Frame f){
        createMenu(f,true,true,new String[]{
            "@LHide labels",M_LABELS,
            "Shorten lables",M_TRIGRAPH,
            M_SHOWDOTS,M_TOGGLEPTS,
            "Increase dot size (up)",M_NODESIZEUP,
            "Decrease dot size (down)",M_NODESIZEDOWN,
            M_SHOWAXES,M_TOGGLEAXES,
            M_HIDELINES,M_TOGGLELINES,
            "@NHide NA lines",M_HIDENALINES,
            M_MINUS,
            "@TCommon scale",M_COMMON,
            M_MINUS,
            "Set Y Range ...",M_YRANGEDLG,
            "!SShow scale dialog",M_SCALEDLG,
            M_MINUS,
            "More transparent (left)",M_ALPHADOWN,
            "More opaque (right)",M_ALPHAUP,
            "Transparent highlighting",M_TRANSHIGHL,
            "Set Colors (CB)",M_SET1,
            "Set Colors (rainbow)",M_SET64,
            "Clear Colors",M_RESET
        });
        
        MIlabels=EzMenu.getItem(f,M_LABELS);
        MIdots=EzMenu.getItem(f,M_TOGGLEPTS);
        MIaxes=EzMenu.getItem(f,M_TOGGLEAXES);
        MIlines=EzMenu.getItem(f,M_TOGGLELINES);
        MIlines.setEnabled(false);
        MItrigraph=EzMenu.getItem(f, M_TRIGRAPH);
        MInodeSizeUp=EzMenu.getItem(f, M_NODESIZEUP);
        MInodeSizeUp.setEnabled(false);
        MInodeSizeDown=EzMenu.getItem(f, M_NODESIZEDOWN);
        MInodeSizeUp.setEnabled(false);
        MIhideNAlines=EzMenu.getItem(f,M_HIDENALINES);
        MItransHighl=EzMenu.getItem(f,M_TRANSHIGHL);
    }
    
    public void keyPressed(final KeyEvent e) {
        if (e.getKeyCode()==KeyEvent.VK_RIGHT) run(this, M_ALPHAUP);
        if (e.getKeyCode()==KeyEvent.VK_LEFT) run(this, M_ALPHADOWN);
        if (e.getKeyCode()==KeyEvent.VK_UP) run(this, M_NODESIZEUP);
        if (e.getKeyCode()==KeyEvent.VK_DOWN) run(this,M_NODESIZEDOWN);
    }
    
    public Object run(final Object o, final String cmd) {
        if ("print".equals(cmd)) { drawHidden=false; run(o,"exportPS"); drawHidden=true; return this; }
        super.run(o,cmd);
        if (M_LABELS.equals(cmd)) {
            setShowLabels(!isShowLabels());
            MIlabels.setLabel((isShowLabels())?"Hide labels":"Show labels");
            setUpdateRoot(0);
            repaint();
        }
        if (M_ALPHADOWN.equals(cmd)) {
            ppAlpha-=(ppAlpha>0.2)?0.10:0.02; if (ppAlpha<0.05f) ppAlpha=0.05f;
            setUpdateRoot(0); repaint();
        }
        if (M_ALPHAUP.equals(cmd)) {
            ppAlpha+=(ppAlpha>0.2)?0.10:0.02; if (ppAlpha>1f) ppAlpha=1f;
            setUpdateRoot(0); repaint();
        }
        if ("exit".equals(cmd)) WinTracker.current.Exit();
        if (M_COMMON.equals(cmd)) { setCommonScale(!commonScale); updateObjects(); setUpdateRoot(0); repaint();}
        if (M_TRIGRAPH.equals(cmd)) {
            useX3=!useX3;
            MItrigraph.setLabel(useX3?"Extend labels":"Shorten labels");
            setUpdateRoot(0); repaint();
        }
        if (M_TOGGLEPTS.equals(cmd)) {
            drawPoints=!drawPoints;
            MIdots.setLabel((drawPoints)?"Hide dots":M_SHOWDOTS);
            for(int i=0; i<pp.length; i++){
                ((PPrimPolygon)pp[i]).drawCorners=drawPoints;
            }
            MIdots.setEnabled(!drawPoints||drawLines);
            MIlines.setEnabled(drawPoints||!drawLines);
            MInodeSizeDown.setEnabled(drawPoints);
            MInodeSizeUp.setEnabled(drawPoints);
            setUpdateRoot(0);
            repaint();
        }
        if (M_TOGGLELINES.equals(cmd)) {
            drawLines=!drawLines;
            MIlines.setLabel((drawLines)?M_HIDELINES:"Show lines");
            for(int i=0; i<pp.length; i++){
                ((PPrimPolygon)pp[i]).drawBorder=drawLines;
                ((PPrimPolygon)pp[i]).selectByCorners=!drawLines;
            }
            MIdots.setEnabled(!drawPoints||drawLines);
            MIlines.setEnabled(drawPoints||!drawLines);
            setUpdateRoot(0);
            repaint();
        }
        if (M_TOGGLEAXES.equals(cmd)) {
            drawAxes=!drawAxes;
            MIaxes.setLabel((drawAxes)?"Hide axes":M_SHOWAXES);
            setUpdateRoot(0); repaint();
        }
        if (M_YRANGEDLG.equals(cmd) || "XrangeDlg".equals(cmd)) {
            final Axis rt=(M_YRANGEDLG.equals(cmd))?ay:ax;
            final Dialog d=intDlg=new Dialog(myFrame,(rt==ay)?"Y range":"X range",true);
            
            d.setBackground(Color.white);
            d.setLayout(new BorderLayout());
            d.add(new SpacingPanel(),BorderLayout.WEST);
            d.add(new SpacingPanel(),BorderLayout.EAST);
            final Panel bp=new Panel(); bp.setLayout(new FlowLayout());
            final Button b;
            bp.add(b=new Button("OK"));final Button b2;
            bp.add(b2=new Button("Cancel"));
            d.add(bp,BorderLayout.SOUTH);
            d.add(new Label(" "),BorderLayout.NORTH);
            final Panel cp=new Panel(); cp.setLayout(new FlowLayout());
            d.add(cp);
            cp.add(new Label("start: "));
            final TextField tw=new TextField(""+rt.vBegin,6);
            final TextField th=new TextField(""+(rt.vBegin+rt.vLen),6);
            cp.add(tw);
            cp.add(new Label(", end: "));
            cp.add(th);
            d.pack();
            final IDlgCL ic = new IDlgCL(this);
            b.addActionListener(ic);b2.addActionListener(ic);
            d.setVisible(true);
            if (!cancel) {
                final double w=Tools.parseDouble(tw.getText());
                final double h=Tools.parseDouble(th.getText());
                rt.setValueRange(w,h-w);
                setUpdateRoot(0);
                repaint();
            }
            d.dispose();
            updateGeometry=true;
        }
        if (M_SCALEDLG.equals(cmd) && commonScale) {
            final RespDialog d=new RespDialog(myFrame,"Set y scale",true,RespDialog.okCancel);
            final Panel cp=d.getContentPanel();
            cp.add(new Label("begin: "));
            final TextField tw=new TextField(""+ay.vBegin,6);
            final TextField th=new TextField(""+(ay.vBegin+ay.vLen),6);
            cp.add(tw);
            cp.add(new Label(", end: "));
            cp.add(th);
            d.pack();
            d.setVisible(true);
            if (!cancel) {
                final double vb=Tools.parseDouble(tw.getText());
                final double ve=Tools.parseDouble(th.getText());
                if (ve-vb>0) ay.setValueRange(vb,ve-vb);
                if (myFrame!=null) myFrame.pack();
            }
            d.dispose();
            updateGeometry=true;
        }
        if (M_SET1.equals(cmd)) {
            if (pp!=null && pp.length>0) {
                int i=0;
                while (i<pp.length) {
                    final int cs[] = ((PPrimBase)pp[i]).getCaseIDs();
                    int j=0;
                    if (cs!=null)
                        while (j<cs.length)
                            m.setSec(cs[j++],i+16);
                    i++;
                }
                m.NotifyAll(new NotifyMsg(this,Common.NM_MarkerChange));
            }
        }
        if (M_SET64.equals(cmd)) {
            if (pp!=null && pp.length>0) {
                int i=0;
                while (i<pp.length) {
                    //System.out.println("set64: "+i+" (of "+pp.length+") mapped to "+ax.getCatAtSeqIndex(i)+", pp="+pp[i]);
                    final int cs[] = ((PPrimBase)pp[i]).getCaseIDs();
                    int j=0;
                    if (cs!=null)
                        while (j<cs.length)
                            m.setSec(cs[j++],64+(64*i/pp.length));
                    i++;
                }
                m.NotifyAll(new NotifyMsg(this,Common.NM_MarkerChange));
            }
        }
        if (M_RESET.equals(cmd)) {
            if (m.getSecCount()>0) {
                m.resetSec();
                m.NotifyAll(new NotifyMsg(this,Common.NM_MarkerChange));
            }
        }
        if (M_NODESIZEUP.equals(cmd)) {
            if(pp[0]!=null){
                nodeSize++;
                for(int i=0; i<pp.length; i++)
                    if(pp[i]!=null)
                        ((PPrimPolygon)pp[i]).setNodeSize(nodeSize);
                setUpdateRoot(0); repaint();
            }
        }
        if (M_NODESIZEDOWN.equals(cmd)) {
            if(pp[0]!=null){
                nodeSize--;
                for(int i=0; i<pp.length; i++)
                    if(pp[i]!=null)
                        ((PPrimPolygon)pp[i]).setNodeSize(nodeSize);
                setUpdateRoot(0); repaint();
            }
        }
        if (M_HIDENALINES.equals(cmd)){
            drawNAlines=!drawNAlines;
            for(int i=0; i<pp.length; i++){
                if(pp[i]!=null) ((PPrimPolygon)pp[i]).showInvisibleLines = drawNAlines;
            }
            MIhideNAlines.setLabel(drawNAlines?"Hide NA lines":"Show NA lines");
            setUpdateRoot(0); repaint();
        }
        if(M_TRANSHIGHL.equals(cmd)) {
            alphaHighlighting=!alphaHighlighting;
            MItransHighl.setLabel(alphaHighlighting?"Opaque highlighting":"Transparent highlighting");
            setUpdateRoot(1); repaint();
        }
        
        return null;
    }
    
    public SVar getData(final int id) { return (id>=0 && id<v.length)?v[id]:null; }
    
    public void performZoomIn(final int x1, final int y1, final int x2, final int y2) {
        if(commonScale) super.performZoomIn(x1, y1, x2, y2, null,ay);
        else{
            int minZoomAxis=0;
            int maxZoomAxis=v.length-1;
            
            while(getAxCatPos(ax.getCatAtSeqIndex(minZoomAxis)) < x1) minZoomAxis++;
            while(getAxCatPos(ax.getCatAtSeqIndex(maxZoomAxis)) > x2) maxZoomAxis--;
            
            dontPaint=true;
            for(int i=minZoomAxis; i<=maxZoomAxis; i++){
                final int csi=ax.getCatAtSeqIndex(i);
                super.performZoomIn(x1, y1, x2, y2, null, (csi==0)?ay:opAy[csi-1]);
            }
            dontPaint=false;
        }
    }
    
    public void resetZoom() {
        if(commonScale) super.resetZoom();
        else{
            // this regenerates the y axes instead of resetting the ranges... quick and dirty...
            int i=0;
            while (i<opAy.length) {
                opAy[i]=new Axis(v[i+1],Axis.O_Y,v[i+1].isCat()?Axis.T_EqCat:Axis.T_Num);
                opAy[i].addDepend(this);
                i++;
            }
            
            updateGeometry=true;
            ay.setDefaultRange();
            updateObjects();
            setUpdateRoot(0); repaint();
        }
    }
    
    public String queryObject(int i) {
        return queryObject(pp[i]);
    }
    
    private void setCommonScale(final boolean cs) {
        //if(cs==commonScale) return;
        commonScale=cs;
        updateGeometry=true;
        updateMargins();
        EzMenu.getItem(getFrame(),M_COMMON).setLabel(cs?"Individual scales":"Common scale");
        EzMenu.getItem(getFrame(),M_YRANGEDLG).setEnabled(cs);
        if (cs) {
            ay.setValueRange(totMin-(totMax-totMin)/20,(totMax-totMin)*1.1);
        } else{
            if (opAy!=null && opAy.length>0 && opAy[0]==null) {
                
                int i=0;
                while (i<opAy.length) {
                    opAy[i]=new Axis(v[i+1],Axis.O_Y,v[i+1].isCat()?Axis.T_EqCat:Axis.T_Num);
                    opAy[i].addDepend(this);
                    opAy[i].setValueRange(v[i+1].getMin()-(v[i+1].getMax()-v[i+1].getMin())/20,(v[i+1].getMax()-v[i+1].getMin())*1.1);
                    i++;
                }
                
                
            }
            ay.setValueRange(v[0].getMin()-(v[0].getMax()-v[0].getMin())/20,(v[0].getMax()-v[0].getMin())*1.1);
        }
        
    }
    
    protected static String getShortClassName(){
        return "PA";
    }
    
    public void paintBack(final PoGraSS g) {
        if (drawAxes) {
            g.setColor(C_WHITE);
            g.setLineWidth(1.5f);
            int xx=0;
            while (xx<xv.getNumCats()) {
                final int t=getAxCatPos(xx++);
                if(orientation==0)
                    g.drawLine(t,mTop,t,pc.getSize().height-mBottom);
                else
                    g.drawLine(mLeft,t,pc.getSize().width-mRight,t);
            }
            g.setLineWidth(1.0f);
        }
        
        final Rectangle r=pc.getBounds();
        g.setBounds(r.width,r.height);
        
        TH=r.height;
        TW=r.width;
        
        final int innerH;
        innerH=TH-mBottom-mTop;
        
        if(!getValid()){
            g.setColor("red");
            g.drawLine(0,0,TW,TH);
            g.drawLine(0,TH,TW,0);
            return;
        }
        
        g.setColor(C_BLACK);
        labels.clear();
        addLabelsAndTicks(g);
        labels.finishAdd();
    }
    
    boolean getValid() {
        return valid && pc.getWidth()>=MINWIDTH && pc.getHeight()>=MINHEIGHT;
    }
    
    void addLabelsAndTicks(PoGraSS g) {}
    
    public boolean adjustMargin(final PoGraSS g){
        switch (type){
            case TYPE_PCP:
                if((orientation!=0 || !commonScale) && orientation==0) return false;
                break;
            case TYPE_BOX:
                if(orientation==1 || (!vsCat && v.length<=1)) return false;
                break;
        }
                
        if(orientation==0){
            final double f=ay.getSensibleTickDistance(30,18);
            double fi=ay.getSensibleTickStart(f);
            int maxLabelLength=0;
            while (fi<ay.vBegin+ay.vLen) {
                String s = ay.getDisplayableValue(fi);
                int wi=g.getWidthEstimate(s);
                if(wi>maxLabelLength) maxLabelLength=wi;
                fi+=f;
            }
            return adjustMargin(maxLabelLength);
        } else{
            int maxWidth=0;
            for(int i=0; i<xv.getNumCats(); i++){
                final String s=(String)ax.getVariable().getCatAt(i);
                int wi=g.getWidthEstimate(useX3?Common.getTriGraph(s):s);
                if(wi>maxWidth) maxWidth=wi;
            }
            return adjustMargin(maxWidth);
        }
    }
    
    boolean adjustMargin(int maxWidth){
        final int omLeft=mLeft;
        maxWidth+=6;
        if(maxWidth>defaultMLeft){
            mLeft = maxWidth;
        } else mLeft=defaultMLeft;
        return (mLeft!=omLeft);
    }
    
    void updateMargins() {}
    
    public void mouseMoved(final MouseEvent ev) {
        super.mouseMoved(ev);
        if (Common.isQueryTrigger(ev)) {
            mouseX=ev.getX();
            mouseY=ev.getY();
        }
    }
    
    public void paintInit(final PoGraSS g) {
        super.paintInit(g);
        if(this instanceof BoxCanvas && ax!=null && v.length>1){
            int oBoxwidth = boxwidth;
            final int newBoxwidth = Math.max(((getAxCatPos(ax.getCatAtSeqIndex(1))-getAxCatPos(ax.getCatAtSeqIndex(0)))*8)/10,MIN_BOXWIDTH);
            if(MAX_BOXWIDTH>0) boxwidth = Math.min(newBoxwidth,MAX_BOXWIDTH);
            else boxwidth = newBoxwidth;
            if(boxwidth!=oBoxwidth) updateObjects();
        }
    }
    
    protected int getAxCasePos(int i) {
        return useRegularPositioning?
            getAxCatPos(ax.getCatByPos(ax.getCasePos(i))):
            ax.getCasePos(i);
    }
    
    protected int getAxCatPos(int i) {
        return useRegularPositioning?
            ax.getRegularCatPos(i,leftGap,rightGap):
            ax.getCatCenter(i);
    }
    
    protected void initFlagsAndFields(){}
    
    public void mouseReleased(final MouseEvent e) {
        if (baseDrag && moveDrag) {
            final int pos = (orientation==0)?e.getX():e.getY();
            final int dragNew = ax.getCatByPos(pos);
            final int dragAxis = ax.getCatByPos((orientation==0)?baseDragX1:baseDragY1);
            final int difference;
            final int myX1=ax.getCatLow(dragNew);
            final int myX2=ax.getCatUp(dragNew);
            if(Math.abs(difference=pos-getAxCatPos(dragNew)) > (myX2-myX1)/4){
                int newPos=ax.getCatSeqIndex(dragNew);
                if(difference>0) newPos += 1;
                if(dragAxis<newPos) newPos -=1;
                ax.moveCat(dragAxis, newPos);
            } else{
                ax.swapCats(dragNew, dragAxis);
            }
            
            baseDrag=false;
            updateObjects();
            setUpdateRoot(0);
            repaint();
        } else super.mouseReleased(e);
    }
    
    public Dimension getMinimumSize() { return new Dimension(MINWIDTH,MINHEIGHT); };
    
    protected PPrimBox createBox(final OrdStats os, final int x, final int w, final int rank){
        final Axis axis = (commonScale || rank==0)?ay:opAy[rank-1];
        final PPrimBox box = new PPrimBox();
        box.x=x;
        box.w=w;
        box.med = axis.getValuePos(os.med);
        box.lh = axis.getValuePos(os.lh);
        box.uh = axis.getValuePos(os.uh);
        box.lh15 = axis.getValuePos(os.lh15);
        box.uh15 = axis.getValuePos(os.uh15);
        box.medValue = os.med;
        box.lhValue = os.lh;
        box.uhValue = os.uh;
        box.lh15Value = os.lh15;
        box.uh15Value = os.uh15;
        box.lh3 = os.lh3;
        box.uh3 = os.uh3;
        box.lowEdge = os.lowEdge;
        box.lastR = new double[os.lastR.length];
        box.valPos = new int[os.lastR.length];
        for(int i=0; i< box.lastR.length; i++){
            box.lastR[i] = v[rank].atF(os.lastR[i]);
            box.valPos[i] = axis.getValuePos(box.lastR[i]);
        }
        box.lastTop = os.lastTop;
        box.highEdge = os.highEdge;
        
        //System.out.println("x: " + x + ", w: " + w + ", med: " + ay.getValuePos(os.med) + ", lh: " + ay.getValuePos(os.lh) + ", uh: " + ay.getValuePos(os.uh)
        //+  ", lh15: " + ay.getValuePos(os.lh15) + ", uh15: " + ay.getValuePos(os.uh15) + ", lh3:" +  os.lh3 + ", uh3: " + os.uh3 + ", lowedge: " + os.lowEdge);
        return box;
    }
    
    protected void updateObjectsPCP(){
        if (pp==null || pp.length!=v[0].size()) {
            pp=new PlotPrimitive[v[0].size()];
        }
        
        TW = pc.getSize().width;
        TH = pc.getSize().height;
        
        
        final int[][] xs = new int[v[0].size()][v.length];
        final int[][] ys = new int[v[0].size()][v.length];
        //boolean[] na = new boolean[v[0].size()];
        final int[][] na = new int[v[0].size()][];
        final int[] naIndices = new int[v.length+1];
        for (int i=0;i<v[0].size();i++){
            int numNAs=0;
            for (int j=0;j<v.length;j++){
                if ((drawHidden || !m.at(i)) && (v[j].at(i)!=null)) {
                    xs[i][ax.getCatSeqIndex(j)] = getAxCatPos(j);
                    ys[i][ax.getCatSeqIndex(j)] = ((commonScale||j==0)?ay:opAy[j-1]).getValuePos(v[j].atD(i));
                } else{
                    xs[i][ax.getCatSeqIndex(j)] = getAxCatPos(j);
                    ys[i][ax.getCatSeqIndex(j)] = ((commonScale||j==0)?ay:opAy[j-1]).getValuePos(v[j].atD(i));
                    naIndices[numNAs++] = j;
                }
            }
            if(numNAs>0){
                na[i] = new int[numNAs];
                System.arraycopy(naIndices, 0, na[i], 0, numNAs);
            }
        }
        
        for(int j=0; j<xs.length; j++){
            pp[j] = new PPrimPolygon();
            if(orientation==0) ((PPrimPolygon)pp[j]).pg = new Polygon(xs[j], ys[j], xs[j].length);
            else               ((PPrimPolygon)pp[j]).pg = new Polygon(ys[j], xs[j], xs[j].length);
            ((PPrimPolygon)pp[j]).closed=false;
            ((PPrimPolygon)pp[j]).fill=false;
            ((PPrimPolygon)pp[j]).selectByCorners=!drawLines;
            ((PPrimPolygon)pp[j]).drawCorners = drawPoints;
            ((PPrimPolygon)pp[j]).ref = new int[] {j};
            ((PPrimPolygon)pp[j]).setNodeSize(nodeSize);
            ((PPrimPolygon)pp[j]).drawBorder=drawLines;
            ((PPrimPolygon)pp[j]).showInvisibleLines=drawNAlines;
            final boolean[] nas = new boolean[xs[j].length];
            final boolean[] gap = new boolean[xs[j].length];
            
            if(na[j]!=null){
                final boolean[] nod = new boolean[xs[j].length];
                for(int i=0; i<na[j].length; i++) {
                    nas[na[j][i]]=true;
                    if(na[j][i]>0) nas[na[j][i]-1]=true;
                    nod[na[j][i]]=true;
                }
                ((PPrimPolygon)pp[j]).noDotsAt = nod;
                for(int i=0; i<na[j].length-1; i++){
                    if(na[j][i+1]-na[j][i]==2) gap[na[j][i]+1]=true;
                }
                if(na[j][0]==1) gap[0]=true;
                if(na[j][na[j].length-1]==gap.length-2) gap[gap.length-1]=true;
            }
            ((PPrimPolygon)pp[j]).invisibleLines=nas;
            ((PPrimPolygon)pp[j]).gapDots=gap;
        }
    }
    
    protected void updateObjectsBox() {
        if (!valid) return;
        
        if (!vsCat) {
            pp = new PlotPrimitive[v.length];
            markStats = new OrdStats[v.length];
            for(int i=0; i<pp.length; i++){
                pp[i] = createBox((pp.length==1)?OSdata:oss[i], getAxCasePos(i)-boxwidth/2,boxwidth,i);
                ((PPrimBox)pp[i]).ref = v[i].getRanked();
                markStats[i] = new OrdStats();
            }
        } else {
            final Vector boxes = new Vector();
            for(int i=0; i<cs; i++){
                final PPrimBox box = createBox(oss[i],getAxCasePos(i)-boxwidth/2,boxwidth,0);
                box.ref = rk[i];
                boxes.add(box);
            }
            pp = new PlotPrimitive[boxes.size()];
            boxes.toArray(pp);
            markStats = new OrdStats[boxes.size()];
            System.arraycopy(oss, cs+1, markStats, 0, cs);
        }
        for(int i=0; i<pp.length; i++) ((PPrimBox)pp[i]).slastR=null;
    };
    
    public void updateObjects() {
        if(type == TYPE_PCP) updateObjectsPCP();
        else if (type == TYPE_BOX) updateObjectsBox();
    }
    
    public void paintPost(final PoGraSS g) {
        if(type==TYPE_PCP){
            if(baseDrag && moveDrag){
                final int basey=pc.getBounds().height-mBottom;
                final int pos = (orientation==0)?baseDragX2:baseDragY2;
                final int dragNew = ax.getCatByPos(pos);
                final int myX1=ax.getCatLow(dragNew);
                final int myX2=ax.getCatUp(dragNew);
                final int difference;
                if(Math.abs(difference=pos-getAxCatPos(dragNew)) > (myX2-myX1)/4){
                    final int x;
                    final int w;
                    if(difference>0){
                        x=ax.getCatCenter(dragNew);
                        w=2*(myX2-x);
                    } else{
                        w=2*(ax.getCatCenter(dragNew)-myX1);
                        x=ax.getCatCenter(dragNew)-w;
                        
                    }
                    if(orientation==0) g.fillRect(x,basey,w,4);
                    else g.fillRect(mLeft,x,4,w);
                } else{
                    if(orientation==0) g.fillRect(myX1,basey,myX2-myX1,4);
                    else g.fillRect(mLeft,myX1,4,myX2-myX1);
                }
            }
        }
        super.paintPost(g);
    }
    
    public String queryObject(final PlotPrimitive p) {
        switch(type){
            case TYPE_BOX:
                PPrimBox box = (PPrimBox)p;
                if(box.queriedOutlier!=null)
                    return "Outlier: " + Tools.getDisplayableValue(box.queriedOutlier.getValue());
                else
                    return "lower hinge: " + Tools.getDisplayableValue(box.lhValue) + "\n" +
                            "median: " + Tools.getDisplayableValue(box.medValue) + "\n" +
                            "upper hinge: " + Tools.getDisplayableValue(box.uhValue) + "\n" +
                            "cases: " + box.cases();
            case TYPE_PCP:
                String retValue="";
                final int[] pts = (orientation==0)?(((PPrimPolygon)p).pg.ypoints):(((PPrimPolygon)p).pg.xpoints);
                
                if(isExtQuery){
                    for(int i=0; i<v.length; i++){
                        retValue += v[i].getName() + ": ";
                        if(v[i].isCat()){
                            retValue += v[i].getCatAt((int)((commonScale||i==0)?ay:opAy[i-1]).getValueForPos(pts[i])) + "\n";
                        } else{
                            retValue += Tools.getDisplayableValue(
                                    ((commonScale||i==0)?ay:opAy[i-1]).getValueForPos(pts[i])) + "\n";
                        }
                        
                    }
                } else{
                    int c = ax.getCatByPos((orientation==0)?mouseX:mouseY);
                    int i = ax.getCatSeqIndex(c);
                    retValue += v[c].getName() + ": ";
                    if(v[c].isCat()){
                        retValue += v[c].getCatAt((int)((commonScale||i==0)?ay:opAy[i-1]).getValueForPos(pts[i])) + "\n";
                    } else{
                        retValue += Tools.getDisplayableValue(
                                ((commonScale||c==0)?ay:opAy[c-1]).getValueForPos(pts[i]));
                    }
                }
                return retValue;
        }
        return super.queryObject(p);
    }
}




/** OrdStats - ordinal statistics of a variable, used internally by {@link BoxCanvas}
 * to get necessary information to plot bopxplots */
class OrdStats { // get ordinal statistics to be used in boxplot
    double med, uh, lh, uh15, lh15, uh3, lh3;
    int[] lastR;
    int lastTop;
    /** indexes of points just above/below the 1.5 hinge
     * beware, this is relative to the used r[] so
     * use with care and only with the corresponding r[] */
    int lowEdge, highEdge;
    
    OrdStats() { med=uh=lh=uh3=lh3=0; };
    
    double medFrom(final SVar v,final int[] r,final int min,final int max) {
        return (((max-min)&1)==0)
        ?v.atF(r[min+(max-min)/2])
        :((v.atF(r[min+(max-min)/2])+v.atF(r[min+(max-min)/2+1]))/2);
    };
    
    void update(final SVar v, final int[] r) {
        update(v,r,r.length);
    };
    
    /* v=variable, r=ranked index as returned by getRanked, n=# of el to use */
    void update(final SVar v, final int[] r, final int n) {
        lastTop=n;
        if (n<1) return;
        med=medFrom(v,r,0,n-1);
        uh=medFrom(v,r,n/2,n-1);
        if (n>1 && (n&1)==1)
            lh=medFrom(v,r,0,n/2-1);
        else
            lh=medFrom(v,r,0,n/2);
        lh15=lh-1.5*(uh-lh);
        lh3=lh-3*(uh-lh);
        double x=lh;
        int i=n/4; // find lh15 as extreme between lh and lh15
        while (i>=0) {
            final double d=v.atF(r[i]);
            if (d<lh15) break;
            if (d<x) x=d;
            i--;
        }
        lowEdge=i;
        lh15=x;
        uh15=uh+1.5*(uh-lh);
        uh3=uh+3*(uh-lh);
        x=uh;
        i=n*3/4-1; if (i<0) i=0; // find uh15
        while (i<n) {
            final double d=v.atF(r[i]);
            if (d>uh15) break;
            if (d>x) x=d;
            i++;
        }
        uh15=x;
        highEdge=i;
        lastR=r;
    };
};