package org.rosuda.javaGD;

import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.Method;

public class JavaGD extends GDInterface implements WindowListener {
    public Frame f;
    
    public JavaGD() {
        super();
    }
    
    public void     gdOpen(double w, double h) {
        if (f!=null) gdClose();

        f=new Frame("JavaGD");
        f.addWindowListener(this);
        c=new GDCanvas(w, h);
        f.add((GDCanvas)c);
        f.pack();
        f.setVisible(true);
    }

    public void     gdActivate() {
        super.gdActivate();
        if (f!=null) {
            f.requestFocus();
            f.setTitle("JavaGD "+((devNr>0)?("("+(devNr+1)+")"):"")+" *active*");
        }
    }

    public void     gdClose() {
        super.gdClose();
        if (f!=null) {
            c=null;
            f.removeAll();
            f.dispose();
            f=null;
        }
    }

    public void     gdDeactivate() {
        super.gdDeactivate();
        if (f!=null) f.setTitle("JavaGD "+((devNr>0)?("("+(devNr+1)+")"):""));
    }

    public double[] gdLocator() {
        double[] res=new double[2];
        // FixME
        res[0]=0.0; res[1]=0.0;
        return res;
    }

    public void     gdNewPage(int devNr) { // new API: provides the device Nr.
        super.gdNewPage(devNr);
        if (f!=null) f.setTitle("JavaGD ("+(devNr+1)+")"+(active?" *active*":""));
    }

    public void executeDevOff() {
        if (c==null || c.getDeviceNumber()<0) return;
        try { // for now we use no cache - just pure reflection API for: Rengine.getMainEngine().eval("...")
            Class cl=Class.forName("org.rosuda.JRI.Rengine");
            if (cl==null)
                System.out.println(">> can't find Rengine, close function disabled. [c=null]");
            else {
                Method m=cl.getMethod("getMainEngine",null);
                Object o=m.invoke(null,null);
                if (o!=null) {
                    Class[] par=new Class[1];
                    par[0]=Class.forName("java.lang.String");
                    m=cl.getMethod("eval",par);
                    Object[] pars=new Object[1];
                    pars[0]="try({ dev.set("+(c.getDeviceNumber()+1)+"); dev.off()},silent=TRUE)";
                    m.invoke(o, pars);
                }
            }
        } catch (Exception e) {
            System.out.println(">> can't find Rengine, close function disabled. [x:"+e.getMessage()+"]");
        }
    }

    /*-- WindowListener interface methods */
    
    public void windowClosing(WindowEvent e) {
        if (c!=null) executeDevOff();
    }
    public void windowClosed(WindowEvent e) {}
    public void windowOpened(WindowEvent e) {}
    public void windowIconified(WindowEvent e) {}
    public void windowDeiconified(WindowEvent e) {}
    public void windowActivated(WindowEvent e) {}
    public void windowDeactivated(WindowEvent e) {}
    
}
