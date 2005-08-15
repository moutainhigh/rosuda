package org.rosuda.JGR;

//JGR - Java Gui for R, see http://www.rosuda.org/JGR/
//Copyright (C) 2003 - 2005 Markus Helbig
//--- for licensing information see LICENSE file in the original JGR distribution ---


import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.undo.*;

import org.rosuda.ibase.*;
import org.rosuda.ibase.toolkit.*;

import org.rosuda.JRI.*;
import org.rosuda.JGR.toolkit.*;
import org.rosuda.JGR.util.*;

/**
 *   JGRConsole Console Frame, the main window of JGR.
 *
 *	@author Markus Helbig
 *
 * 	RoSuDa 2003 - 2005
 */

public class JGRConsole extends iFrame implements ActionListener, KeyListener,
FocusListener, RMainLoopCallbacks {

    private JSplitPane consolePanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    
    /** Console output text panel */
    public ConsoleOutput output = new ConsoleOutput();
    /** Console command input area*/
    public SyntaxInput input = new SyntaxInput("console",true);
    
    private Document inputDoc = input.getDocument();
    private Document outputDoc = output.getDocument();

    private TextFinder textFinder = new TextFinder(output);

    private ToolBar toolBar;

    private String wspace = null;
    

    private int currentHistPosition = 0;

    private StringBuffer console = new StringBuffer();

    private boolean wasHistEvent = false;

    /** Position where the R splash ends (need for clearing the console*/
    public int end = 0;
    private Integer clearpoint = null;

    public JGRConsole() {
        this(null);
    }

    /**
     * Create a new Console window.
     * @param workSpace workspace which should be loaded when starting JGR
     */
    public JGRConsole(File workSpace) {
        super("Console", iFrame.clsMain);

        //Initialize JGRConsoleMenu
        String[] Menu = {
            "+", "File","Load Datafile", "loaddata","-","@NNew Document","new","@OOpen Document","open","!OSource File...","source","@SSave","save","-", "@DSet Working Directory", "setwd","~File.Quit", 
            "~EditC",
            "+", "Tools", "Editor", "editor", "@BObject Browser", "objectmgr",
            "DataTable", "table", "-", "Increase Font Size", "fontBigger",
            "Decrease Font Size", "fontSmaller",
            "+", "Packages", "Package Manager", "packagemgr",
            "+","Workspace","Load Workspace","openwsp","Save Workspace", "savewsp", "Save Workspace as", "saveaswsp","Clear Workspace", "clearwsp", 
            "~Window",
            "~Help",/*"R Help","help",/*"Update JGR","update",*/"~About", "0"};
        iMenu.getMenu(this, this, Menu);

        //Add History if we didn't found one in the user's home directory
        if (JGR.RHISTORY == null) {
            JGR.RHISTORY = new Vector();
        }
        currentHistPosition = JGR.RHISTORY.size();

        //Add default toolbar with stop button to interrupt R
        toolBar = new ToolBar(this,true);

        input.addKeyListener(this);
        input.setWordWrap(false);
        input.addFocusListener(this);
        inputDoc.addUndoableEditListener(toolBar.undoMgr);

        output.setEditable(false);
        output.addFocusListener(this);
        output.addKeyListener(this);
        output.setDragEnabled(true);
		output.setCaret(new SelectionPreservingCaret());

        JScrollPane sp1 = new JScrollPane(output);
        sp1.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        consolePanel.setTopComponent(sp1);
        JScrollPane sp2 = new JScrollPane(input);
        sp2.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        consolePanel.setBottomComponent(sp2);
        consolePanel.setDividerLocation( ( (int) ( (double)this.getHeight() * 0.65)));

        this.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent evt) {
                super.componentResized(evt);
                if (JGR.R != null && JGR.STARTED) JGR.R.eval("options(width="+getFontWidth()+")");
                consolePanel.setDividerLocation( ( (int) ( (double) getHeight() * 0.65)));
            }
        });
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                dispose();
            }
        });
        this.addKeyListener(this);

        this.getContentPane().setLayout(new BorderLayout());
        this.getContentPane().add(toolBar,BorderLayout.NORTH);
        this.getContentPane().add(consolePanel,BorderLayout.CENTER);
        this.setMinimumSize(new Dimension(555,650));
        this.setSize(new Dimension(600,
                                   Common.screenRes.height < 800 ?
                                   Common.screenRes.height - 50 : 700));
		this.setVisible(true);
        //progress.setVisible(false);
		input.mComplete.setVisible(false);
    }


    /**
     * Close Console, but not before we asked the user if he wants to save opened Editors.
     */
    public void exit() {
        dispose();
    }

    /**
     * Close Console, but not before we asked the user if he wants to save opened Editors.
     */
    public void dispose() {
        Enumeration e = WinTracker.current.elements();
        while (e.hasMoreElements()) {
            WTentry we = (WTentry) e.nextElement();
            if (we.wclass == iFrame.clsEditor) {
                if (!((Editor) we.w).exit()) return;
            }
        }
        execute("q()",false);
    }

    /**
     * Execute a command and add it into history.
     * @param cmd command for execution
     * @param addToHist indicates wether the command should be added to history or not
     */
    public void execute(String cmd, boolean addToHist) {
        if (!JGR.STARTED) return;
        if (addToHist && JGR.RHISTORY.size()==0)  JGR.RHISTORY.add(cmd);
        else if (addToHist && cmd.trim().length() > 0 && JGR.RHISTORY.size() > 0 && !JGR.RHISTORY.lastElement().equals(cmd.trim())) JGR.RHISTORY.add(cmd);
        currentHistPosition = JGR.RHISTORY.size();

        String[] cmdArray = cmd.split("\n");

        String c = null;
        for (int i = 0; i < cmdArray.length; i++) {
            c = cmdArray[i];
            if (isHelpCMD(c))
            	try { outputDoc.insertString(outputDoc.getLength(),c+"\n"+RController.getRPrompt(),JGRPrefs.CMD); } catch (Exception e) {}
            else if (isSupported(c))
            	JGR.rSync.triggerNotification(c.trim());
        }
    }

    /**
     * Parse command if it is a helpcommand.
     * @param cmd command which should be executed
     * @return true if help should be started, false if not
     */
    // later i hope it will be possible let R do this
    public boolean isHelpCMD(String cmd) {
        if (cmd.startsWith("help") || cmd.startsWith("?") ) {
            help(cmd);
            return true;
        }
        return false;
    }
    
    private boolean isSupported(String cmd) {
    	if (cmd.indexOf("fix(") >= 0 || cmd.indexOf("edit(") >= 0 || cmd.indexOf("edit.data.frame(") >= 0) {
    		try { outputDoc.insertString(outputDoc.getLength(),cmd+"\n",JGRPrefs.CMD); } catch (Exception e) {}
    		try { outputDoc.insertString(outputDoc.getLength(),"Editing is not supported yet!",JGRPrefs.RESULT); } catch (Exception e) {}
    		try { outputDoc.insertString(outputDoc.getLength(),"\n"+RController.getRPrompt(),JGRPrefs.CMD); } catch (Exception e) {}
    		return false;
    	}
    		
    	return true;
    }


    /**
     * Start the help-browser, first parse for the keyword.
     * @param help help-command
     */
    public void help(String help) {
        boolean exact = false;
        if (help != null) {
            help = help.replaceAll("[\"|(|)]", "");
            if (help.startsWith("help.search")) {
                help = help.replaceFirst("help.search", "");
            }
            else if (help.startsWith("help.start")) help=null;
            else {
                if (help.trim().startsWith("?")) 
                    help = help.replaceFirst("\\?", "");
                else 
                    help = help.replaceFirst("help", "");
                exact = true;
            }
        }
        final boolean e = exact;
        if (JGRHelp.current == null) {
            final String h;
            if (help!=null) h = help.trim();
            else h = null;
            Thread t = new Thread() {
                public void run() {
                    setWorking(true);
                    try {
                        new JGRHelp();
                        if (h!=null) JGRHelp.current.search(h,e);
                    } catch (Exception e1) {
                        new ErrorMsg(e1);
                    }
                    setWorking(false);
                }
            };
            t.start();
        }
        else {
            if (help!=null && help.trim().length() > 0) {
                final String h = help.trim();
                Thread t = new Thread() {
                    public void run() {
                        setWorking(true);
                        JGRHelp.current.show();
                        try {
                            JGRHelp.current.search(h,e);
                        } catch (Exception e1) {
                            new ErrorMsg(e1);
                        }
                        setWorking(false);
                    }
                };
                t.start();
            }
        }
    }

    /**
     * Clear the console's content, if it's too full.
     */
    public void clearconsole() {
        try {
            if (clearpoint==null) clearpoint = new Integer(output.getLineEndOffset(output.getLineOfOffset(end)-1)+2);
            output.removeAllFrom(clearpoint.intValue());
        } catch (Exception e) { new ErrorMsg(e);/*e.printStackTrace();*/ }
    }

    /**
     * Load a workspace, R-command: load(...).
     */
    public void loadWorkSpace() {
        FileSelector fopen = new FileSelector(this, "Open Workspace",
                                              FileSelector.LOAD, JGR.directory);
        fopen.setVisible(true);
        if (fopen.getFile() != null) {
            wspace = (JGR.directory = fopen.getDirectory()) + fopen.getFile();
            execute("load(\""+wspace.replace('\\','/')+"\")",false);
        }
    }

    /**
     * Save workspace with specified filename, R-command: save.image(...).
     * @param file filename
     */
    public void saveWorkSpace(String file) {
        if (file==null) execute("save.image()",false);
        else execute("save.image(\""+(file == null ? "" : file.replace('\\','/'))+"\",compress=TRUE)",false);
        JGR.writeHistory();
    }

    /**
     * Save workspace to a different file then .RData.
     */
    public void saveWorkSpaceAs() {
        FileSelector fsave = new FileSelector(this, "Save Workspace as...",
                                              FileSelector.SAVE, JGR.directory);
        fsave.setVisible(true);
        if (fsave.getFile() != null) {
            String file = (JGR.directory = fsave.getDirectory()) + fsave.getFile();
            saveWorkSpace(file);
            JGR.writeHistory();
        }
    }

    /**
     * Get the font's width form current settings using {@see FontMetrics}.
     * @return fontwidth
     */
	public int getFontWidth() {
		int width = output.getFontMetrics(output.getFont()).charWidth('M');
        width = output.getWidth() / width;
		return (int) (width) - (JGRPrefs.isMac?0:1);
    }
		
	//======================================================= R callbacks ===

	/**
	 * Write output from R into console (R callback).
	 * @param re used Rengine
	 * @param text output
	 */
    public void   rWriteConsole(Rengine re, String text) {
    	console.append(text);
        if (console.length() > 100) {
            output.append(console.toString(),JGRPrefs.RESULT);
            console.delete(0,console.length());
            output.setCaretPosition(outputDoc.getLength());
        }
    }

    /**
     * Invoke the busy cursor (R callback).
     * @param re used Rengine
     * @param which busy (1) or not (0)
     */
    public void   rBusy(Rengine re, int which) {
        if (which==0) {
            if (console != null) {
                output.append(console.toString(), JGRPrefs.RESULT);
                console.delete(0, console.length());
            }
            output.setCaretPosition(outputDoc.getLength());
            setWorking(false);
        }
        else {
            toolBar.stopButton.setEnabled(true);
            setWorking(true);
        }
    }

    /**
     * Read the commands from input area (R callback).
     * @param re used Rengine
     * @param prompt prompt from R
     * @param addToHistory is it an command which to add to the history
     */
    public String rReadConsole(Rengine re, String prompt, int addToHistory) {
        toolBar.stopButton.setEnabled(false);
        if (prompt.indexOf("Save workspace") > -1) {
        	String retVal = JGR.exit(); 
        	if (wspace != null && retVal.indexOf('y') >= 0) {
        		JGR.R.eval("save.image(\""+wspace.replace('\\','/')+"\")");
        		return "n\n";
        	}
        	else return retVal;
        }
        else {
            output.append(prompt,JGRPrefs.CMD);
            output.setCaretPosition(outputDoc.getLength());
            String s = JGR.rSync.waitForNotification();
            try { outputDoc.insertString(outputDoc.getLength(),s+"\n",JGRPrefs.CMD); } catch (Exception e) {}
            return (s==null||s.length()==0)?"\n":s+"\n";
        }
    }

    /**
     * Showing a message from the rengine (R callback).
     * @param re used Rengine
     * @param message message from R
     */
    public void   rShowMessage(Rengine re, String message) {
        JOptionPane.showMessageDialog(this,message,"R Message",JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Choose a file invoked be file.choose() (R callback).
     * @param re used Rengine
     * @param newFile if it's a new file
     */
	public String rChooseFile(Rengine re, int newFile) {
		FileSelector fd = new FileSelector(this, (newFile==0)?"Select a file":"Select a new file", (newFile==0)?FileDialog.LOAD:FileDialog.SAVE,JGR.directory);
		fd.setVisible(true);
		String res=null;
		if (fd.getDirectory()!=null && fd.getFile() != null) res=fd.getDirectory();
		if (fd.getFile()!=null) res=(res==null)?fd.getFile():(res+fd.getFile());
		return res;
	}
	
	/**
	 * Flush the console (R callback).
	 * !! not implemented yet !!
	 * @param re used Rengine
	 */
    public void   rFlushConsole (Rengine re) {
	}

    /**
     * Load history from a file (R callback).
     * @param re used Rengine
     * @param filename history file
     */
	public void   rLoadHistory  (Rengine re, String filename) {
		File hist = null;
		try {
			if ((hist = new File(filename)).exists()) {
				BufferedReader reader = new BufferedReader(new FileReader(hist));
				if (JGR.RHISTORY == null) JGR.RHISTORY = new Vector();
				while (reader.ready())
					JGR.RHISTORY.add(reader.readLine()+"\n");
				reader.close();
			}
		} catch (Exception e) {
			new ErrorMsg(e);
		}
	}
	
	/**
	 * Save history to a file (R callback).
	 * @param re used Rengine
	 * @param filename history file
	 */
    public void   rSaveHistory  (Rengine re, String filename) {
        try {
			System.out.println("Save History");
            File hist = new File(filename);
            BufferedWriter writer = new BufferedWriter(new FileWriter(hist));
            Enumeration e = JGR.RHISTORY.elements(); int i = 0;
            while(e.hasMoreElements()) writer.write(e.nextElement().toString()+"#\n");
            writer.flush();
            writer.close();
        } catch (Exception e) {
            new ErrorMsg(e);
        }
	}
	
	//======================================================= other events ===
	
    /**
     * actionPerformed: handle action event: menus.
     */
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if (cmd == "about") new AboutDialog(this);
        else if (cmd == "cut") input.cut();
        else if (cmd == "clearwsp") execute("rm(list=ls())",false);
        else if (cmd == "copy") {
            input.copy();
            output.copy();
        } else if (cmd=="copyoutput") output.copyOutput();
        else if (cmd=="copycmds") output.copyCommands();
        else if (cmd=="copyresult") output.copyResults();
        else if (cmd == "clearconsole") clearconsole();
        else if (cmd == "delete") {
            try {
                int i = 0;
                inputDoc.remove((i = input.getSelectionStart()),input.getSelectionEnd()-i);
            } catch (BadLocationException ex) {}
        } else if (cmd == "editor") new Editor();
        else if (cmd == "exit") dispose();
        else if (cmd == "exportOutput") output.startExport();
        else if (cmd == "fontBigger") FontTracker.current.setFontBigger();
        else if (cmd == "fontSmaller") FontTracker.current.setFontSmaller();
        else if (cmd == "loaddata") new JGRDataFileOpenDialog(this, JGR.directory);
        else if (cmd == "open") new Editor().open();
        else if (cmd == "openwsp") loadWorkSpace();
        else if (cmd == "new") new Editor();
        //else if (cmd == "newwsp") newWorkSpace();
        else if (cmd == "objectmgr") execute("object.browser()",false);
        else if (cmd == "packagemgr") execute("package.manager()",false);
        else if (cmd == "paste") input.paste();
        else if (cmd == "prefs") new PrefsDialog(this);
        else if (cmd == "redo") {
            try {
                if (toolBar.undoMgr.canRedo())
                    toolBar.undoMgr.redo();
            } catch (CannotUndoException ex) {}
        } else if (cmd == "help")  execute("help.start()",false);
        else if (cmd == "table") new DataTable(null,null,true);
        else if (cmd == "save") output.startExport();
        else if (cmd == "savewsp") saveWorkSpace(wspace);
        else if (cmd == "saveaswsp") saveWorkSpaceAs();
        else if (cmd == "search") textFinder.showFind(false);
        else if (cmd == "searchnext") textFinder.showFind(true);
        else if (cmd == "source") execute("source(file.choose())",false);
        else if (cmd == "stop") JGR.R.rniStop(1);
        else if (cmd == "selAll") {
            if (input.isFocusOwner()) {
                input.selectAll();
            } else if (output.isFocusOwner()) {
                output.selectAll();
            }
        } else if (cmd == "undo") {
            try {
                if (toolBar.undoMgr.canUndo())
                    toolBar.undoMgr.undo();
            } catch (Exception ex) {}
        }
        else if (cmd == "setwd") {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setDialogTitle("Choose Working Directory");
            chooser.setApproveButtonText("Choose");
            int r = chooser.showOpenDialog(this);
            if (r == JFileChooser.CANCEL_OPTION) return;
            if (chooser.getSelectedFile()!=null)
                JGR.directory = chooser.getSelectedFile().toString();
                execute("setwd(\""+chooser.getSelectedFile().toString().replace('\\','/')+"\")",true);
        }
		else if (cmd == "update") {
			execute("update.JGR(contriburl=\"http://rosuda.org/R/nightly\")",false);
		}
    }

    /**
     * keyTyped: handle key event.
     */
    public void keyTyped(KeyEvent ke) {
    }

    /**
     * keyPressed: handle key event, like: adding a new line, history ....
     */
    public void keyPressed(KeyEvent ke) {
        if (ke.getSource().equals(output) && !ke.isMetaDown() && !ke.isControlDown() && !ke.isAltDown())
            input.requestFocus();
        if (ke.getKeyCode() == KeyEvent.VK_UP) {
            if (input.mComplete != null && input.mComplete.isVisible()) {
                input.mComplete.selectPrevious();
            }
            else if (currentHistPosition > 0){
                if (input.getCaretPosition()==0 || input.getCaretPosition()==input.getText().length()) {
                    if (input.getText().trim().length() > 0) {
                        if (currentHistPosition==JGR.RHISTORY.size()-1 && !input.getText().trim().equals(JGR.RHISTORY.elementAt(currentHistPosition-1))) {
							if (System.getProperty("user.name").indexOf("markus") > -1)
								JGR.RHISTORY.insertElementAt(input.getText().trim(),currentHistPosition);
							else
								JGR.RHISTORY.add(input.getText().trim());
                        }
                        else if (System.getProperty("user.name").indexOf("markus") > -1)
							JGR.RHISTORY.add(input.getText().trim());
                    }
                    input.setText(JGR.RHISTORY.get(--currentHistPosition).toString());
                    input.setCaretPosition(input.getText().length());
                    wasHistEvent = true;
                }
            }
        }
        else if (ke.getKeyCode() == KeyEvent.VK_DOWN) {
            if (input.mComplete != null && input.mComplete.isVisible()) {
                input.mComplete.selectNext();
            }
            else {
                if (input.getCaretPosition()==0 || input.getCaretPosition()==input.getText().length()) {
                    if (currentHistPosition < JGR.RHISTORY.size() - 1) {
                        input.setText(JGR.RHISTORY.get(++currentHistPosition).toString());
                        input.setCaretPosition(input.getText().length());
                    }
                    else if (JGR.RHISTORY.size() > 0 && currentHistPosition < JGR.RHISTORY.size()) {
                        input.setText("");
                        currentHistPosition++;
                    }
                    wasHistEvent = true;
                }
            }
        }
    }

    /**
     * keyReleased: handle key event, sending the command.
     */
    public void keyReleased(KeyEvent ke) {
        if (ke.getKeyCode() == KeyEvent.VK_ENTER) {
            if (input.mComplete != null && input.mComplete.isVisible() && !(ke.isControlDown() || ke.isMetaDown())) {
                input.mComplete.completeCommand();
            }
            else {
                if (ke.isControlDown() || ke.isMetaDown()) {
                    try {
                        inputDoc.insertString(input.getCaretPosition(), "\n", null);
                        input.mComplete.setVisible(false);
                    } catch (Exception e) {}
                }
                else {
                    String cmd = input.getText().trim();
                    input.setText("");
                    input.setCaretPosition(0);
                    input.requestFocus();
                    execute(cmd,true);
                }
            }
        }
        if (ke.getSource().equals(output) && ke.getKeyCode() == KeyEvent.VK_V && (ke.isControlDown() || ke.isMetaDown())) {
            input.requestFocus();
            input.paste();
            input.setCaretPosition(input.getText().length());
        }
        else if ((ke.getKeyCode() == KeyEvent.VK_UP || ke.getKeyCode() == KeyEvent.VK_DOWN) && wasHistEvent) {
            wasHistEvent = false;
            input.setCaretPosition(input.getText().length());
        }
    }

    /**
     * focusGained: handle focus event, enable and disable cut and paste button.
     */
    public void focusGained(FocusEvent e) {
        if (e.getSource().equals(output)) {
            toolBar.cutButton.setEnabled(false);
            iMenu.getItem(this, "cut").setEnabled(false);
            toolBar.pasteButton.setEnabled(false);
            iMenu.getItem(this, "paste").setEnabled(false);
        } else if (e.getSource().equals(input)) {
            toolBar.cutButton.setEnabled(true);
            iMenu.getItem(this, "cut").setEnabled(true);
            toolBar.pasteButton.setEnabled(true);
            iMenu.getItem(this, "paste").setEnabled(true);
        }
    }

    /**
     * focusLost: handle focus event.
     */
    public void focusLost(FocusEvent e) {
    }
}
