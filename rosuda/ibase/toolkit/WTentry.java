package org.rosuda.ibase.toolkit;

import java.awt.*;
import java.util.*;
import javax.swing.*;

// what we ought to add to WT are among others:
// - associated variables
// - class (other that just using java tools)

/** a {@link WinTracker} entry
    @version $Id$
*/
abstract public class WTentry extends Object
{
    public static int lid=1;
    public static String windowMenuName="Window";
    
    public Window w;
    public String name;
    public int id=0;
    public int wclass=0;

    WinTracker wt;
    
    public WTentry(WinTracker wt, Window win, String nam, int wndclass) {
        this.wt=wt;
        name=nam;
	w=win; id=lid; lid++;
        wclass=wndclass;
        wt.newWindowMenu(this);
        wt.add(this);
    }

    public abstract Object getWindowMenu();
    public abstract void addMenuSeparator();
    public abstract void addMenuItem(String name, String action);
    public abstract void rmMenuItemByAction(String action);
    public abstract Object getMenuItemByAction(String action);
    public abstract void setNameByAction(String action, String name);

    /** adds the entry for this object into a menu of another window specified by its entry.
        @param we the target window entry; if <code>null</code> then uses itself */
    public String addWindowMenuEntry(WTentry we) { // generic way to do this
        if (we==null) we=this;
        we.addMenuItem(((name==null)?"Window":name)+" ["+id+"]", "WTMwindow"+id);
	return "WTMwindow"+id;
    }
    
    public void rmWindowMenuEntry(WTentry we) {
        if (we==null) we=this;
        we.rmMenuItemByAction("WTMwindow"+id);
    }

    public String toString() {
	return "WTentry(id="+id+", class="+wclass+", name="+name+", win="+((w==null)?"<null>":w.toString())+")";
    }
}
