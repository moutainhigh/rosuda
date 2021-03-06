package org.rosuda.JGR.toolkit;

//JGR - Java Gui for R, see http://www.rosuda.org/JGR/
//Copyright (C) 2003 - 2005 Markus Helbig
//--- for licensing information see LICENSE file in the original JGR distribution ---

import java.awt.Component;
import java.awt.Font;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.JComponent;

import org.rosuda.JGR.JGR;
import org.rosuda.JGR.util.ErrorMsg;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;

/**
 * FontTracker - collect all componentes and apply prefs-font to them
 * 
 * @author Markus Helbig
 * 
 * RoSuDa 2003 - 2005
 */

public class FontTracker {

	public static FontTracker current = null;

	Vector components;

	public FontTracker() {
		components = new Vector();
	}

	/**
	 * Add a component to the list.
	 * 
	 * @param comp
	 *            component to add
	 */
	public void add(Component comp) {
		comp.setFont(JGRPrefs.DefaultFont);
		components.add(comp);
	}

	/**
	 * Add a component to the list.
	 * 
	 * @param comp
	 *            component to add
	 */
	public void add(JComponent comp) {
		add((Component) comp);
	}

	/**
	 * Increase fontsize, current step is 2.
	 */
	public void setFontBigger() {
		JGRPrefs.FontSize += 2;
		JGRPrefs.refresh();
		applyFont();
	}

	/**
	 * Decrease fontsize, current step is 2.
	 */
	public void setFontSmaller() {
		JGRPrefs.FontSize -= 2;
		if (JGRPrefs.FontSize <= 6)
			JGRPrefs.FontSize = 6;
		JGRPrefs.refresh();
		applyFont();
	}

	/**
	 * Apply font, fontsize from JGRPrefs to components contained in the
	 * tracker.
	 */
	public void applyFont() {
		Enumeration e = components.elements();
		Font f = JGRPrefs.DefaultFont;
		while (e.hasMoreElements()) {
			Component comp = (Component) e.nextElement();
			try {
				Class sc = comp.getClass().getSuperclass();

				while (!sc.getName().startsWith("java"))
					sc = sc.getSuperclass();
				if (sc.getName().equals("javax.swing.JTable")) {
					if (f.getSize() > JGRPrefs.MINFONTSIZE)
						f = new Font(f.getName(), f.getStyle(),
								JGRPrefs.MINFONTSIZE);
					((javax.swing.JTable) comp)
							.setRowHeight((int) (f.getSize() * 1.6));
				} else if (sc.getName().equals("javax.swing.JTextComponent")
						|| sc.getName().equals("javax.swing.JTextPane")
						|| comp.getClass().getName().equals("jedit.syntax.JEditTextArea")) {
					f = JGRPrefs.DefaultFont;
				} else if (f.getSize() > 18)
					f = new Font(f.getName(), f.getStyle(),
							JGRPrefs.MINFONTSIZE);
				comp.setFont(f);
			} catch (Exception ex) {
			}
		}
		if (JGR.getREngine() != null && JGR.STARTED) {
			try {
				JGR.eval("options(width=" + JGR.MAINRCONSOLE.getFontWidth() + ")");
			} catch (REngineException e1) {
				new ErrorMsg(e1);
			} catch (REXPMismatchException e1) {
				new ErrorMsg(e1);
			}
		}
	}

}