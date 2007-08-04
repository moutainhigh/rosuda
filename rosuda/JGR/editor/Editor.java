package org.rosuda.JGR.editor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.util.StringTokenizer;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import javax.swing.text.BadLocationException;

import jedit.syntax.FindReplaceDialog;
import jedit.syntax.JEditTextArea;
import jedit.syntax.RTokenMarker;

import org.rosuda.JGR.JGR;

import org.rosuda.JGR.util.DocumentRenderer;
import org.rosuda.JGR.toolkit.FileSelector;
import org.rosuda.JGR.toolkit.JGRPrefs;
import org.rosuda.JGR.toolkit.PrefsDialog;

import org.rosuda.ibase.toolkit.EzMenu;
import org.rosuda.ibase.toolkit.TJFrame;
import org.rosuda.util.RecentList;

public class Editor extends TJFrame implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2281511772856410211L;

	JEditTextArea textArea = new JEditTextArea();

	JLabel modifiedStatus = new JLabel("   ");
	
	CaretListenerLabel cLabel = new CaretListenerLabel();

	private boolean modified = false;

	/** Recent documents which where opened the last times with the editor. */
	public static RecentList recentOpen;

	/** Menuitem for the recent-list */
	public Menu recentMenu;
	
	private String fileName;

	public Editor()
	{
		this(null);
	}

	public Editor(String file) {
		super("Editor", false, TJFrame.clsEditor);
		this.setLayout(new BorderLayout());

		// Menu
		String[] Menu = { "+", "File", "@NNew", "new", "@OOpen", "open", "#Open Recent","", "@SSave", "save", "!SSave as", "saveas", "-", /*"@PPrint", "print", "-",*/
				"@QQuit", "quit", 
				"+", "Edit", "@ZUndo","undo","!ZRedo","redo","-","@XCut", "cut", "@CCopy", "copy", "@VPaste", "paste", "-", 
				"@/Toggle Comment","commentcode","-","!LShift Left","shiftleft","!RShift Right","shiftright","-","@RRun Selection","runselection","!RRun all","runall","-", "@FFind", "find", "@GFind next",
				"findnext", 
				
				"+", "Tools", "Increase Font Size", "fontBigger", "Decrease Font Size", "fontSmaller", 
				"~Window", "+","Help","R Help","rhelp", "~Preferences", "~About", "0" };
		EzMenu.getEzMenu(this, this, Menu);
		Menu rm = recentMenu = (Menu) EzMenu.getItem(this,"Open Recent");
		//System.out.println(rm);
		if (rm != null) {
			rm.removeAll();
			if (recentOpen == null)
				recentOpen = new RecentList("JGR", "RecentOpenFiles", 8);
			String[] shortNames = recentOpen.getShortEntries();
			String[] longNames = recentOpen.getAllEntries();
			int i = 0;
			while (i < shortNames.length) {
				MenuItem mi = new MenuItem(shortNames[i]);
				mi.setActionCommand("recent:" + longNames[i]);
				mi.addActionListener(this);
				rm.add(mi);
				i++;
			}
			if (i > 0)
				rm.addSeparator();
			MenuItem ca = new MenuItem("Clear list");
			ca.setActionCommand("recent-clear");
			ca.addActionListener(this);
			rm.add(ca);
			if (i == 0)
				ca.setEnabled(false);
		}

		// END Menu

		this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		// TextArea

		textArea.getDocument().setTokenMarker(new RTokenMarker());
		RInputHandler rih = new RInputHandler();
		rih.addKeyBindings();
		textArea.setInputHandler(rih);
		textArea.addCaretListener(cLabel);

		textArea.setPreferredSize(new Dimension(600, 800));

		JPanel status = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		status.add(modifiedStatus);
		status.add(cLabel);
		
		this.add(textArea, BorderLayout.CENTER);
		this.add(status,BorderLayout.SOUTH);
		this.pack();
		textArea.requestFocus();

		// END TextArea

		// Toolbar

		new EditToolbar(this, this);

		// END Toolbar

		if (file != null)
		{
			this.fileName = file;
			openFile(file);
		}

		this.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent evt) {
				super.componentResized(evt);
				setTitle(fileName);
			}
		});
		
		this.setVisible(true);
		
	}
	
	public void dispose()
	{
		exit();
	}

	public boolean exit() {
		if (modified) {
			int i = JOptionPane.showConfirmDialog(this, "Save File?", "Exit", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
			if (i == 1) {
				super.dispose();
				return true;
			} else if (i == 0 && saveFile()) {
				super.dispose();
				return true;
			} else
				return false;
		}
		super.dispose();
		return true;
	}

	private boolean saveFile() {
		if (fileName == null || fileName.equals(""))
			return saveFileAs();
		setWorking(true);
		textArea.saveFile(fileName);
		setWorking(false);
		this.setTitle(fileName == null ? "Editor" : fileName);
		setModified(modified = false);
		return true;
	}

	public void setText(StringBuffer sb) {
		textArea.setText(sb.toString());
	}

	public void open() {
		openFile();
	}
	
	private void openFile()
	{
		openFile(null);
	}

	private void openFile(String file) {
	
	
		if (modified && textArea.getText().trim().length() > 0) {
			int i = JOptionPane.showConfirmDialog(this, "Save current File?", "Open", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
			if (i == 0 && !saveFile()) {
				return;
			}
			if (i == 2)
				return;
		}
		String newFile = file;
		
		if (file == null)
		{

			FileSelector fopen = new FileSelector(this, "Open...", FileSelector.LOAD, JGRPrefs.workingDirectory);
			fopen.setVisible(true);


		
			if (fopen.getFile() != null)
				newFile = (JGRPrefs.workingDirectory = fopen.getDirectory()) + fopen.getFile();
			}
			else {
				 newFile = file;
		}

		if (/*textArea.getText().length() == 0 && */newFile != null && newFile.trim().length() > 0) {
			fileName = newFile;
			setWorking(true);
			textArea.setText("");
			textArea.loadFile(fileName);
			setWorking(false);
		}
	}

	private void newEditor() {
		Editor e = new Editor();
		e.setVisible(true);
	}

	public void setTitle(String title) {
		int length, cc = 1;
		if (System.getProperty("os.name").startsWith("Win")) {
			super.setTitle(title == null ? "Editor" : title);
			return;
		}
		try {
			length = this.getFontMetrics(this.getFont()).stringWidth(title);
		} catch (Exception e) {
			super.setTitle(title == null ? "Editor" : title);
			return;
		}
		boolean next = true;
		while (length > this.getWidth() - 100 && next) {
			StringTokenizer st = new StringTokenizer(title, File.separator);
			int i = st.countTokens();
			if (!JGRPrefs.isMac)
				title = st.nextElement() + "" + File.separator;
			else
				title = File.separator;
			if (cc > i) {
				for (int z = 1; z < i && st.hasMoreTokens(); z++)
					st.nextToken();
				if (st.hasMoreTokens())
					title = st.nextToken();
				next = false;
			} else {
				for (int z = 1; z <= i && st.hasMoreTokens(); z++)
					if (z <= i / 2 - (cc - cc / 2) || z > i / 2 + cc / 2)
						title += st.nextToken() + "" + (st.hasMoreTokens() ? File.separator : "");
					else {
						title += "..." + File.separator;
						st.nextToken();
					}
				next = true;
			}
			length = this.getFontMetrics(this.getFont()).stringWidth(title);
			cc++;
		}
		super.setTitle(title);
	}

	private boolean saveFileAs() {
		FileSelector fsave = new FileSelector(this, "Save as...", FileSelector.SAVE, JGRPrefs.workingDirectory);
		fsave.setVisible(true);
		if (fsave.getFile() != null) {
			fileName = (JGRPrefs.workingDirectory = fsave.getDirectory()) + fsave.getFile();
			return saveFile();
		}
		return false;
	}

	private void setModified(boolean mod) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				modifiedStatus.setText(modified ? "Modified" : "");
			}
		});
	}
	
	public void print() {
		DocumentRenderer docrender = new DocumentRenderer();
		//docrender.print(textArea);
	}

	public static void main(String[] args) {
		Editor e = new Editor();
		e.setVisible(true);
	}

	public void actionPerformed(ActionEvent e) {

		if ("new".equalsIgnoreCase(e.getActionCommand()))
			newEditor();
		if ("open".equalsIgnoreCase(e.getActionCommand()))
			openFile();
		if ("save".equalsIgnoreCase(e.getActionCommand()))
			saveFile();
		if ("saveas".equalsIgnoreCase(e.getActionCommand()))
			saveFileAs();

		if ("copy".equalsIgnoreCase(e.getActionCommand()))
			textArea.copy();
		if ("cut".equalsIgnoreCase(e.getActionCommand()))
			textArea.cut();
		if ("paste".equalsIgnoreCase(e.getActionCommand()))
			textArea.paste();

		if ("undo".equalsIgnoreCase(e.getActionCommand()))
			textArea.undo();
		if ("redo".equalsIgnoreCase(e.getActionCommand()))
			textArea.redo();

		if ("find".equalsIgnoreCase(e.getActionCommand()))
			FindReplaceDialog.findExt(this, textArea);
		if ("findnext".equalsIgnoreCase(e.getActionCommand()))
			FindReplaceDialog.findNextExt(this, textArea);

		if ("fontBigger".equalsIgnoreCase(e.getActionCommand()))
			textArea.increaseFontSize();
		if ("fontSmaller".equalsIgnoreCase(e.getActionCommand()))
			textArea.decreaseFontSize();
		
			
				 if (e.getActionCommand() == "commentcode") {
			if (textArea.getSelectedText().trim().length() > 0)
				try {
					textArea.commentSelection(true);
				} catch (BadLocationException e1) {
				}
		} else if (e.getActionCommand() == "uncommentcode") {
			if (textArea.getSelectedText().trim().length() > 0)
				try {
					textArea.commentSelection(false);
				} catch (BadLocationException e1) {
				}
		}	
			
			if (e.getActionCommand().startsWith("recent:")) {
			//System.out.println(e.getActionCommand().replaceFirst("recent:", ""));
			if (modified)
				new Editor(e.getActionCommand().replaceFirst("recent:", ""));
			else {
				fileName = e.getActionCommand().replaceFirst("recent:", "");
				openFile(fileName);
			}
		} else if (e.getActionCommand() == "preferences")
			new PrefsDialog(this);
		else if (e.getActionCommand() == "print")
			print();
		else if (e.getActionCommand() == "recent-clear") {
			if (recentOpen != null && recentMenu != null) {
				recentMenu.removeAll();
				recentMenu.addSeparator();
				MenuItem ca = new MenuItem("Clear list");
				ca.setActionCommand("recent-clear");
				ca.addActionListener(this);
				ca.setEnabled(false);
				recentMenu.add(ca);
				recentOpen.reset();
			}
			}
			
			if (e.getActionCommand() == "help")
			JGR.MAINRCONSOLE.execute("help.start()", false);

if (e.getActionCommand() == "shiftleft")
			try {
				textArea.shiftSelection(-1);
			} catch (BadLocationException e1) {
			}
		else if (e.getActionCommand() == "shiftright")
			try {
				textArea.shiftSelection(1);
			} catch (BadLocationException e2) {
			}

if (e.getActionCommand() == "runall")
			try {
				String s = textArea.getText();
				if (s.length() > 0)
					JGR.MAINRCONSOLE.execute(s.trim(), true);
			} catch (Exception ex) {
			}
		else if (e.getActionCommand() == "runselection")
			try {
				String s = textArea.getSelectedText().trim();
				if (s.length() > 0)
					JGR.MAINRCONSOLE.execute(s.trim(), true);
			} catch (Exception ex) {
			}

		
		if ("quit".equalsIgnoreCase(e.getActionCommand()))
			exit();
	}
	
	protected class CaretListenerLabel extends JLabel implements CaretListener {

		private static final long serialVersionUID = -4451331086216529945L;

		public CaretListenerLabel() {
		}

		public void caretUpdate(CaretEvent e) {
			modified = true;
			setModified(modified);
			displayInfo(e);
		}

		protected void displayInfo(final CaretEvent e) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					try {
						int currentpos = textArea.getCaretPosition();
						int lastnewline = textArea.getText().lastIndexOf("\n", currentpos - 1);
						int chars = textArea.getText(0, lastnewline < 0 ? 0 : lastnewline).length();
						int currentline = textArea.getLineOfOffset(textArea.getCaretPosition())+1;
						currentpos -= chars;
						setText(currentline + ":" + (currentline == 1 ? currentpos + 1 : currentpos)+"   ");
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
		}
	}
}