import java.awt.*;
import java.awt.event.*;
import java.util.*;

/** SplitEditor */

public class SplitEditor extends TFrame implements ActionListener, ItemListener {
    SNode n,ln;
    SMarker m;
    SVarSet vs;
    SVar cv;
    SNode root;
    TextField st=null;
    Choice vc;
    ScatterCanvas sc;
    LineCanvas lc;
    Label l1;
    Panel cp,sp,pp;
    PlotLine li;
    double spVal;
    
    public SplitEditor(SNode nd) {
        super("Split Editor");
        n=nd; vs=n.getSource();
        m=vs.getMarker();
        root=(SNode)n.getRoot();
        ln=(SNode)n.at(0); if (ln!=null) cv=ln.splitVar;
        addWindowListener(Common.defaultWindowListener);
        if (n!=null && !n.isLeaf()) {
            setLayout(new BorderLayout());
            add(new SpacingPanel(),BorderLayout.WEST);
            add(new SpacingPanel(),BorderLayout.EAST);
            Panel bp=new Panel(); bp.setLayout(new FlowLayout());
            Button b;
            //bp.add(b=new Button("Preview")); b.addActionListener(this);
            bp.add(b=new Button("OK")); b.addActionListener(this);
            bp.add(b=new Button("Cancel")); b.addActionListener(this);
            add(bp,BorderLayout.SOUTH);
            Panel vp=new Panel(); vp.setLayout(new FlowLayout());
            vp.add(new Label("Variable: "));
            vc=new Choice(); vc.addItemListener(this);
            int j=0;
            while(j<vs.count()) {
                vc.add(vs.at(j).getName());
                j++;
            }
            vc.select(cv.getName());
            vp.add(vc);
            pp=new Panel(); pp.setLayout(new GridLayout(2,1));
            add(vp,BorderLayout.NORTH);
            sp=new Panel(); // split panel
            cp=new Panel(); // canvas panel
            cp.setLayout(new BorderLayout());
            cp.add(sp,BorderLayout.NORTH);
            add(cp);
            constructInnerPlots();
            pack();
        };
    }

    void constructInnerPlots() {
        Dimension scd=null;
        if (sc!=null) scd=sc.getSize(); else scd=new Dimension(400,100);
        if (sc!=null) { pp.remove(sc); sc=null; };
        if (lc!=null) { pp.remove(lc); lc=null; };
        if (st!=null) { sp.remove(st); st=null; };
        if (l1!=null) { sp.remove(l1); l1=null; };
        if (cv.isCat()) { /** split variable is categorical */
        } else { /** split variabel is numerical */
            Stopwatch sw=new Stopwatch();
            sp.setLayout(new FlowLayout());
            sp.add(l1=new Label("split at "));
            spVal=ln.splitValF;
            sp.add(st=new TextField((ln==null)?"0":""+spVal,10));
            st.addActionListener(this);
            sc=new ScatterCanvas(this,cv,root.response,m);
            m.addDepend(sc);
            sc.setFilter(n.data);
            sc.setSize(scd); sc.bgTopOnly=true;
            cp.add(pp); pp.add(sc);
            sw.profile("innerPlots.build graphics");
            double maxD=0, maxDP=0, optSP=0;
            
            /** build deviance plot
                known bugs:
                - uses entire data instead of in-node data
                - is not updated on var change
                todo?
                - plot also left/right deviance? */

            SVar sdv=new SVar("SplitDev",false);
            SVar rxv=new SVar("RankedXV",false);
            int []fullrks=cv.getRanked();
            int []rks=new int[n.data.size()];
            int rki=0;
            double D=0;
            SVar rsp=root.response;
            boolean isCat=rsp.isCat();
            double sumL=0, sumR=0; // regr: sum of y[i] left/right
            int trct=0;
            for (int ix=0;ix<fullrks.length;ix++)
                if (n.data.contains(new Integer(fullrks[ix]))) {
                    rks[rki++]=fullrks[ix];
                    if (!isCat && rsp.at(fullrks[ix])!=null) {
                        sumR+=rsp.atD(fullrks[ix]); trct++;
                    }
                };
            double mnL=0.0, mnR=0.0; // regr: mean left/right
            if (!isCat) {
                mnR=sumR/((double)trct);
                for(int ix=0;ix<rks.length;ix++)
                    if(rsp.at(rks[ix])!=null)
                        D+=(rsp.atD(rks[ix])-mnR)*(rsp.atD(rks[ix])-mnR);
            }
            sw.profile("innerPlots.init");
            if (Common.DEBUG>0)
                System.out.println("input consistency check: rks.len="+rks.length+", rki="+rki);
            int q=0;
            int []cls=null;
            int []tcls=null;
            if (isCat) {
                cls=new int[root.response.getNumCats()];
                tcls=new int[root.response.getNumCats()];
                if (Common.DEBUG>0)
                    System.out.println("ranked: "+rks.length+", classes="+tcls.length);
                q=0; while(q<tcls.length) tcls[q++]=0;
                q=0;
                while(q<rks.length) {
                    int ci=root.response.getCatIndex(rks[q]);
                    if (ci>-1) tcls[ci]++;
                    q++;
                };
                D=Tools.nlogn(rks.length);
            }
            int lct=0, eq=0;
            boolean isOpt=false;
            double lv=0, devL=0, devR=D;
            q=0;
            while(q<rks.length) {
                lv=cv.atD(rks[q]);
                if (isOpt)
                    optSP=(lv+maxDP)/2;
                eq=0;
                double deltay=0, deltay2=0;
                while (q<rks.length && lv==cv.atD(rks[q])) {
                    if (isCat) {
                        int ci=rsp.getCatIndex(rks[q]);
                        if (ci>-1) cls[ci]++;
                    } else {
                        if (rsp.at(rks[q])!=null) {
                            deltay+=rsp.atD(rks[q]);
                            deltay2+=rsp.atD(rks[q])*rsp.atD(rks[q]);
                        }
                    };
                    rxv.add(cv.at(rks[q]));
                    q++; eq++; lct++;
                };
                sumL+=deltay;
                sumR-=deltay;
                if (Common.DEBUG>0)
                    System.out.println("q="+q+", lv="+lv+", eq="+eq+", lct="+lct);

                double d=D;
                if (isCat) {
                    d-=Tools.nlogn(lct)+Tools.nlogn(rks.length-lct);
                    int cl=0;
                    while(cl<cls.length) {
                        d+=-Tools.nlogn(tcls[cl])+Tools.nlogn(cls[cl])+Tools.nlogn(tcls[cl]-cls[cl]);
                        cl++;
                    }
                } else {
                    double ctL=(double)lct, ctR=(double)(trct-lct);
                    double pvL=sumL/ctL, delL=mnL-pvL;
                    double pvR=sumR/ctR, delR=mnR-pvR;
                    /*
                    devL+=2*delL*(sumL-deltay)+((double)(lct-eq))*(pvL*pvL-mnL*mnL)+((double)eq)*pvL*pvL+deltay2-2*pvL*deltay;
                    devR-=2*delR*(sumR-deltay)+((double)(trct-lct+eq))*(pvR*pvR-mnR*mnR)+((double)eq)*pvR*pvR+deltay2-2*pvR*deltay;
                    d-=devL+devR;
                    mnL=pvL; mnR=pvR; */
                    devL=0; devR=0;
                    for(int iX=0;iX<rks.length;iX++) {
                        if (iX<q) devL+=(rsp.atD(rks[iX])-pvL)*(rsp.atD(rks[iX])-pvL);
                        else devR+=(rsp.atD(rks[iX])-pvR)*(rsp.atD(rks[iX])-pvR);
                    }
//                    System.out.println("sums=["+sumL+":"+sumR+"], pv=["+pvL+":"+pvR+"], ct=["+lct+":"+(trct-lct)+"], dev=["+devL+":"+devR+":"+D+"]");
                    d-=devL+devR;
                }
                isOpt=(d>maxD);
                if (isOpt) { maxD=d; maxDP=lv; optSP=lv; };
                while(eq>0) {
                    sdv.add(new Double(d));                    
                    eq--;
                };
            };
            sw.profile("innerPlots.calculate deviance");
            if (Common.DEBUG>0) {
                System.out.println("Consistency check:");
                System.out.println("sdv length="+sdv.size()+", rxv length="+rxv.size());
                if (isCat) {
                    q=0; while(q<tcls.length) {
                        System.out.println(" class "+q+", tcls="+tcls[q]+", cls="+cls[q]);
                        q++;
                    };
                }
                System.out.println("max.Dev="+maxD+", at "+maxDP+", ergo opt.split="+optSP);
            }
            spVal=optSP;
            st.setText(Tools.getDisplayableValue(spVal));
            SVar[] svl=new SVar[1]; svl[0]=sdv;
            lc=new LineCanvas(this,rxv,svl,m);
            //pp.add(new ScatterCanvas(this,rxv,sdv,m));
            lc.setLineType(LineCanvas.LT_RECT);
            pp.add(lc);
            lc.getXAxis().setValueRange(cv.getMin(),cv.getMax()-cv.getMin());
            PlotManager pm=sc.getPlotManager();
            if (pm!=null) {
                li=new PlotLine(pm);
                li.setCoordinates(1,2);
                li.setColor(new PlotColor(255,0,0));
                li.set(spVal,-1,spVal,1);
                li.setVisible(true);
            }
            sw.profile("fixup , draw line");
        }
    }
    
    public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange()==ItemEvent.SELECTED) {
            String sv=vc.getSelectedItem();
            SVar v=vs.byName(sv);
            if (v!=null && v!=cv) {
                cv=v;
                setMenuBar(null);
                constructInnerPlots();
                pack(); repaint();
            };
        };
    };
    
    public void actionPerformed(ActionEvent e) {
        if (e==null) return;
        String cmd=e.getActionCommand();
        if (Common.DEBUG>0)
            System.out.println("SplitEditor.actionPerformed(\""+cmd+"\") ["+e.toString()+"]\n source="+e.getSource().toString());
        if (e.getSource()==st) {
            double v=0; boolean ok=false;
            try { v=Double.parseDouble(cmd); ok=true; } catch(Exception ex) {};
            if (ok) {
                spVal=v;
                if (li!=null) {
                    li.set(spVal,-1,spVal,1);
                    sc.repaint();
                }
            }
        } else {
            if (cmd=="Cancel") {
                WinTracker.current.rm(this);
                sc=null; li=null; removeAll();
                dispose();
            }
            if (cmd=="OK") {
                if (!cv.isCat() && cv.isNum()) {
                    if (spVal<cv.getMin()||spVal>=cv.getMax()) {
                        new MsgDialog(this,"Invalid split value","The specified split value would result in a single son. No action will be performed.");
                    } else {
                        Vector cp=new Vector();
                        SNode nt=InTr.makePrunedCopy(root,true,n,true,cp);
                        nt.name=nt.name+"*";
                        
                        
                        /* build the two other chunks here */
                        ProgressDlg pd=new ProgressDlg(null,"Running tree generation plugin ...");
                        pd.setText("Initializing plugin, loading R ...");
                        pd.show();
                        PluginGetTreeR gt=new PluginGetTreeR();
                        if (!gt.initPlugin()) {
                            pd.dispose();
                            new MsgDialog(this,"Plugin init failed","Cannot initialize R-plugin.\n"+gt.getLastError());
                            return;
                        }
                        gt.setParameter("dataset",vs);
                        gt.checkParameters();
                        pd.setVisible(false);
                        if (!gt.pluginDlg(this)) {
                            pd.dispose();
                            if (gt.cancel) {
                                gt.donePlugin();
                                return;
                            };
                            new MsgDialog(this,"Parameter check failed","Some of your selections are invalid.\n"+gt.getLastError());
                            return;
                        }
                        pd.setProgress(40);
                        pd.setVisible(true);
                        gt.setParameter("holdConnection",Boolean.TRUE);
                        gt.setParameter("selectedOnly",Boolean.TRUE);
                        gt.setParameter("registerTree",Boolean.FALSE);
                        SMarker bak=vs.getMarker();
                        SMarker ml=new SMarker(cv.size());
                        SMarker mr=new SMarker(cv.size());
                        int i=0;
                        Vector leftd=new Vector(), rightd=new Vector();
                        while(i<cv.size()) {
                            Object o=cv.at(i);
                            if (o!=null && n.data.contains(new Integer(i))) {
                                try {
                                    double v=((Number)o).doubleValue();
                                    if (v<=spVal) { ml.set(i,1); leftd.addElement(new Integer(i)); };
                                    if (v>spVal) { mr.set(i,1); rightd.addElement(new Integer(i)); };
                                } catch(Exception ex) {};
                            }
                            i++;
                        }
                        if (Common.DEBUG>0)
                            System.out.println("Markers: ml="+ml.marked()+", mr="+mr.marked());
                        vs.setMarker(ml);
                        if (!gt.execPlugin()) {
                            vs.setMarker(bak);
                            pd.dispose();
                            HelpFrame hf=new HelpFrame();
                            hf.t.setText("Left-branch generation failed.\n"+gt.getLastError()+"\n\nDump of R output (if any):\n"+gt.getParameter("lastdump"));
                            hf.setTitle("Plugin execution failed");
                            //hf.setModal(true);
                            hf.show();
                            return;
                        }
                        pd.setProgress(70);
                        vs.setMarker(mr);
                        SNode leftb=(SNode)gt.getParameter("root");
                        if (!gt.execPlugin()) {
                            vs.setMarker(bak);
                            pd.dispose();
                            HelpFrame hf=new HelpFrame();
                            hf.t.setText("Right-branch generation failed.\n"+gt.getLastError()+"\n\nDump of R output (if any):\n"+gt.getParameter("lastdump"));
                            hf.setTitle("Plugin execution failed");
                            //hf.setModal(true);
                            hf.show();
                            return;
                        }
                        SNode rightb=(SNode)gt.getParameter("root");
                        vs.setMarker(bak);

                        pd.setProgress(100);
                        gt.donePlugin();
                        pd.dispose();

                        SNode ntcp=(SNode)cp.elementAt(0);
                        ntcp.add(leftb);
                        ntcp.add(rightb);
                        leftb.splitVar=rightb.splitVar=cv;
                        leftb.splitValF=rightb.splitValF=spVal;
                        leftb.splitIndex=rightb.splitIndex=vs.indexOf(cv.getName());
                        leftb.Cond=cv.getName()+" < "+spVal;
                        rightb.Cond=cv.getName()+" > "+spVal;
                        //leftb.data=leftd; rightb.data=rightd;
                        leftb.splitComp=-1; rightb.splitComp=1;
                        RTree.passDownData(ntcp,leftb); RTree.passDownData(ntcp,rightb);
                        //---
                        SVar vvv;
                        vvv=RTree.getPredictionVar(nt,nt.response);
                        if (vvv!=null) {
                            vs.add(vvv);
                            nt.prediction=vvv;
                            if (vvv.isCat()) {
                                if (vs.globalMisclassVarID!=-1)
                                    RTree.manageMisclassVar(root,vs.at(vs.globalMisclassVarID));
                                else {
                                    SVar vmc=RTree.manageMisclassVar(nt,null);
                                    vs.globalMisclassVarID=vs.add(vmc);
                                }
                            }
                        };
                        TFrame f=new TFrame(nt.name);
                        TreeCanvas tc=InTr.newTreeDisplay(nt,f);
                        tc.repaint(); tc.redesignNodes();
                        RTree.getManager().addTree(nt);

                        WinTracker.current.rm(this);
                        sc=null; li=null; removeAll();
                        dispose();                        
                    }
                };
            };
        }        
    };
}
