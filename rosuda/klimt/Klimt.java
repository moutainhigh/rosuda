/**
 * InTr.java
 * Klimt - Interactive Trees 
 *
 * Created: Tue May  1 16:25:54 2001
 *
 * @author <a href="mailto:su@b-q-c.com">Simon Urbanek</a>
 * @version 0.94a $Id$
 */

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
/*SWING*/
import javax.swing.*;
/*ENDSWING*/

//---------------------------------------------------------------------------
// InTr
//---------------------------------------------------------------------------

/** Main Interactive Trees class. For historical reasons the main class of the KLIMT application
    is not Klimt (which is still provided as a wrapper) but this InTr (which stands for Interactive Trees, the
    original project name until it was renamed to Klimt) */
public class InTr
{
    /** file name of the most recently loaded tree. Because of more recent support of multiple trees the use of the variable is deprecated for external packages. */
    public static String lastTreeFileName;

    /** creates a new tree display
	@param t root node
	@param tf frame of the tree
	@param x,y,w,h initial geometry of the canvas
	@return the newly created canvas */
    public static TreeCanvas newTreeDisplay(SNode t, TFrame tf,int x, int y, int w, int h) {
	if (t==null) return null;
	adjustDevGain(t);

	TFrame f=tf;
	if (f==null)
	    f=new TFrame("KLIMT "+Common.Version);
	
	TreeCanvas tc=new TreeCanvas(t,f);
	f.add(tc);
	if (Common.defaultWindowListener==null)
	    Common.defaultWindowListener=new DefWinL();
	f.addWindowListener(Common.defaultWindowListener);
	tc.setBounds(x,y,w,h);
	f.setBounds(x,y,w,h);
	f.pack();
	f.show();
	tc.redesignNodes();
	//t.printTree(" ");	
	return tc;
    };

    /** creates a new tree display with default geometry (0,0,800,500)
	@param t root node
	@param tf frame of the tree 
	@return the newly created canvas */
    public static TreeCanvas newTreeDisplay(SNode t, TFrame tf) {
	return newTreeDisplay(t,tf,0,0,800,500);
    };

    /** creates a new variables display
	@param vs the underlying dataset
	@param x,y,w,h initial geomery. Note: VarFrame itself modifies the height if necessary
	@return the newly created variables canvas */
    public static VarFrame newVarDisplay(SVarSet vs,int x, int y, int w, int h) {
	VarFrame VarF=new VarFrame(vs,x,y,w,h);
	return VarF;
    };

    /** creates a new variables display with default geometry (0,0,140,200)
	@param vs the underlying dataset
	@return the newly created variables canvas */
    public static VarFrame newVarDisplay(SVarSet vs) {
	return newVarDisplay(vs,0,0,140,200);
    };

    /** creates a pruned copy of a tree
	@param t root of the source tree
	@return copy of the tree without pruned nodes */
    public static SNode makePrunedCopy(SNode t) {
        return makePrunedCopy(t,false,null,true,null);
    }
    
    public static SNode makePrunedCopy(SNode t, boolean deepCopy, SNode cutpoint, boolean imTheRoot, Vector cps) 
    {
	SNode n=new SNode();
        if (imTheRoot) {
            SNode root=(SNode)t.getRoot();
            n.name=root.name+"*";
            n.prediction=root.prediction;
            n.response=root.response;
        }
	n.Cases=t.Cases; n.Cond=t.Cond;
        if (deepCopy)
            n.data=new Vector(t.data);
        else
            n.data=t.data;
        n.F1=t.F1; n.Name=t.Name; n.sel=0; n.id=t.id;
        n.sampleDev=t.sampleDev; n.sampleDevGain=t.sampleDevGain;
	n.splitComp=t.splitComp; n.splitIndex=t.splitIndex;
	n.splitVal=t.splitVal; n.splitValF=t.splitValF;
	n.V=t.V; n.vset=t.vset;
        if (cutpoint!=null && t==cutpoint && cps!=null)
            cps.addElement(n);
        if (!t.isLeaf() && (
                     (cutpoint==null && !t.isPruned()) ||
                     (cutpoint!=null && t!=cutpoint)))
	    for (Enumeration e=t.children(); e.hasMoreElements();) {
                SNode nc=makePrunedCopy((SNode)e.nextElement(),deepCopy,cutpoint,false,cps);
		n.add(nc);		
	    };
        return n;
    };

    public static SNode openTreeFile(Frame f,String fn,SVarSet tvs) {
        return openTreeFile(f,fn,tvs,false,true);
    }

    public static String lastUsedDir=null;
    
    /** loads a dataset and a tree from a file.
	@param f frame to be used for FileDialog if necessary
        @param fn filename of the source. If <code>null</code> {@link FileDialog} is used to let the user select the file
	@param tvs {@link SVarSet} object to be used for storage of the dataset.
        @param readOnlyDataset if set to <code>true</code> then tvs is not modified except for classifier
	@return root node of the tree or <code>null</code> if no tree was present. This methods returns <code>null</code> even if the dataset was loaded correcly and no tree was present. Total failure to process the file can be determined only by using clean dataset and check for size of the dataset after the call. */	
    public static SNode openTreeFile(Frame f,String fn,SVarSet tvs,boolean readOnlyDataset,boolean createFrames)
    {
	SNode t=null;	
	String fnam=fn;
	try {
	    lastTreeFileName=fnam;
	    if (fnam==null) {
/*SWING*/
                if (Common.useSwing && tvs!=null && tvs.count()>0) {
                    JFileChooser chooser=null;
                    if (lastUsedDir!=null)
                        chooser=new JFileChooser(new File(lastUsedDir));
                    else
                        chooser = new JFileChooser();
                    chooser.setDialogTitle((tvs==null||tvs.count()==0)?"Select dataset file":"Select tree file(s)");
                    chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                    chooser.setMultiSelectionEnabled(true);
                    int returnVal = chooser.showOpenDialog(f);
                    if(returnVal == JFileChooser.APPROVE_OPTION) {
                        File fs[]=chooser.getSelectedFiles();
                        if (fs!=null && fs.length>0) {
                            int fi=0;
                            while (fi<fs.length) {
                                BufferedReader r=new BufferedReader(new InputStreamReader(new FileInputStream(fs[fi])));
                                long fsz=0;
                                try {
                                    fsz=fs[fi].length();
                                } catch(Exception e) {};
                                t=RTree.Load(r,fs[fi].getName(),tvs,fsz,null,null,readOnlyDataset,true);
                                if (t!=null && tvs!=null) {
                                    TFrame ff=null;
                                    t.name=fs[fi].getName();
                                    if (createFrames) {
                                        t.frame=ff=new TFrame(fs[fi].getName());
                                        TreeCanvas tc=InTr.newTreeDisplay(t,ff);
                                        tc.repaint(); tc.redesignNodes();                                        
                                    }
                                    tvs.registerTree(t,fs[fi].getName());
                                };
                                fi++;
                            }
                        }
                    }
                    String wars=Common.getWarnings();
                    if (wars!=null) {
                        HelpFrame hf=new HelpFrame();
                        hf.t.setText("Following warnings were produced during dataset import:\n\n"+wars);
                        hf.setTitle("Load warnings");
                        //hf.setModal(true);
                        hf.show();
                    };
                    return t;
                };
/*ENDSWING*/
                FileDialog fd=new FileDialog(f,"Select data and tree file");
		fd.setModal(true);
		fd.show();
		fnam=fd.getDirectory()+fd.getFile();
                lastUsedDir=fd.getDirectory();
		if (fd.getFile()!=null)
		    tvs.setName(lastTreeFileName=fd.getFile());
		else
		    return null;
	    } else tvs.setName(fnam);
	    
	    BufferedReader r=new BufferedReader(new InputStreamReader(new FileInputStream(fnam)));
            Common.flushWarnings();
            long fsz=0;
            String fnn=fnam;
            try {
                File fil=new File(fnam);
                fnn=fil.getName();
                fsz=fil.length();
            } catch(Exception e) {};
            t=RTree.Load(r,fnn,tvs,fsz,null,null,readOnlyDataset,true);
            if (t!=null) t.name=fnn;
	    if (Common.DEBUG>0) SVarSet.Debug(tvs);
	    if (tvs.getMarker()==null && (tvs.at(0)!=null)&&(tvs.at(0).size()>0))
		tvs.setMarker(new SMarker(tvs.at(0).size()));
            String wars=Common.getWarnings();
            if (wars!=null) {
                HelpFrame hf=new HelpFrame();
                hf.t.setText("Following warnings were produced during dataset import:\n\n"+wars);
                hf.setTitle("Load warnings");
                //hf.setModal(true);
                hf.show();
            };            
	} catch (Exception E) {
	    E.printStackTrace();
	    t=null;
	};
        if (t!=null && tvs!=null) tvs.registerTree(t,fnam);
	return t;
    };

    /**
     * Main InTrees method, entry for KLIMT as stand-alone application.
     *
     * @param <code>argv</code> Run-time parameters. The syntax is as follows:<pre>
     * [--debug] source [source2 [source3 [...]]]
     *
     * source must contain a dataset and may contain a tree
     * source2... must contain a tree only
     * the --debug parameter switches debugging output on</pre>
     */
    public static void main(String[] argv)
    {
        boolean silentTreeLoad=false;
        int firstNonOption=-1;
        try {
	    int argc=argv.length;
	    int carg=0;

            while (carg<argv.length) {
                if (argv[carg].compareTo("--debug")==0)
                    Common.DEBUG=1;
                if (argv[carg].compareTo("--profile")==0)
                    Common.PROFILE=1;
                if (argv[carg].compareTo("--nodebug")==0)
                    Common.DEBUG=0;
                if (argv[carg].compareTo("--with-loader")==0) {
                    Common.informLoader=true;
                    System.out.println("InfoForLoader:Initializing...");
                }
                if (argv[carg].compareTo("--with-aqua")==0)
                    Common.useAquaBg=true;
                if (argv[carg].compareTo("--without-aqua")==0)
                    Common.useAquaBg=false;
                if (firstNonOption==-1 && argv[carg].length()>0 && argv[carg].charAt(0)!='-')
                    firstNonOption=carg;
                carg++;
	    };
            if (Common.DEBUG>0)
                System.out.println("KLIMT v"+Common.Version+" (Release 0x"+Common.Release+")  "+
                                   ((Common.PROFILE>0)?"PROF ":"")+
                                   (Common.informLoader?"LOADER ":"")+
                                   (Common.useAquaBg?"AQUA ":"")+
                                   (silentTreeLoad?"SILENT ":""));
	    
 	    TFrame f=new TFrame("KLIMT "+Common.Version);
	    Common.mainFrame=f;
	    
	    SVarSet tvs=new SVarSet();
            String fname=(firstNonOption>-1)?argv[firstNonOption]:null;
            if (fname==null || fname.length()<1 || fname.charAt(0)=='-') fname=null;

            if (Common.informLoader) {
                if (fname==null) System.out.println("InfoForLoader:Select file to load");
                else System.out.println("InfoForLoader:Loading data...");
            }
                                   
	    SNode t=openTreeFile(f,fname,tvs);
	    if (t==null && tvs.count()<1) {
                new MsgDialog(f,"Load Error","I'm sorry, but I was unable to load the file you selected"+((fname!=null)?" ("+fname+")":"")+".");
		System.exit(1);
	    };
            if (Common.informLoader)
                System.out.println("InfoForLoader:Setting up windows...");

	    if (Common.DEBUG>0) {
		for(Enumeration e=tvs.elements();e.hasMoreElements();) {
		    SVar vv=(SVar)e.nextElement();
		    System.out.println("==> "+vv.getName()+", CAT="+vv.isCat()+", NUM="+vv.isNum());
		    if (vv.isCat()) {
			System.out.println("    categories: "+vv.getNumCats());
		    };
		};
	    }; 
	    
	    f.setTitle("KLIMT "+Common.Version+", "+tvs.getName()+" - tree");

	    Dimension sres=Toolkit.getDefaultToolkit().getScreenSize();
            Common.screenRes=sres;
	    if (t!=null)
		newTreeDisplay(t,f,0,0,sres.width-160,(sres.height>600)?600:sres.height-20);
	    VarFrame vf=newVarDisplay(tvs,sres.width-150,0,140,(sres.height>600)?600:sres.height);
	    Common.mainFrame=vf;		
	    
	    carg=firstNonOption+1;
	    while (carg<argv.length) {
                if (argv[carg].compareTo("--silent")==0)
                    silentTreeLoad=true;                
                if (argv[carg].length()<2 || argv[carg].substring(0,2).compareTo("--")!=0) {
                    SNode ttt=InTr.openTreeFile(Common.mainFrame,argv[carg],tvs);
                    if (ttt!=null && !silentTreeLoad) {
                        TFrame fff=new TFrame(InTr.lastTreeFileName);
                        TreeCanvas tc=InTr.newTreeDisplay(ttt,fff);
                        tc.repaint(); tc.redesignNodes();
                    };
                };
		carg++;
	    };
            if (Common.informLoader)
                System.out.println("InfoForLoader:Done.");		
	} catch (Exception E) {
	    System.out.println("Something went wrong.");
	    System.out.println("LM: "+E.getLocalizedMessage());
	    System.out.println("MSG: "+E.getMessage());
	    E.printStackTrace();
	};
    };

    /** adjusts cached deviance gain for an entire subtree
	@param t root of the subtree */
    static void adjustDevGain(SNode t) {
	if (t==null) return;
	double myDev=t.F1;
	if (t.isLeaf()) {
	    t.devGain=0;
	} else {
	    for (Enumeration e=t.children(); e.hasMoreElements();) {
		SNode c=(SNode)e.nextElement();
		if (c!=null) {
		    myDev-=c.F1;
		    adjustDevGain(c);
		};
	    };
	    t.devGain=myDev;
	};
    };
    };
    