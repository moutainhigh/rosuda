package org.rosuda.JGR;

//
//  RConsole.java
//  JGR
//
//  Created by Markus Helbig on Fri Mar 05 2004.
//  Copyright (c) 2004 __MyCompanyName__. All rights reserved.
//

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.undo.*;

import org.rosuda.JRI.*;
import org.rosuda.JGR.toolkit.*;
import org.rosuda.ibase.*;
import org.rosuda.ibase.toolkit.*;

public class RConsole extends iFrame implements ActionListener, KeyListener,
    FocusListener, RMainLoopCallbacks, MouseListener {

    private IconButton newButton;
    private IconButton openButton;
    private IconButton saveButton;
    private IconButton undoButton;
    private IconButton redoButton;
    private IconButton cutButton;
    private IconButton copyButton;
    private IconButton pasteButton;
    private IconButton stopButton;
    private IconButton helpButton;

    private GridBagLayout layout = new GridBagLayout();
    private JSplitPane back = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    private JScrollPane scrollAreaTop = new JScrollPane();
    private JScrollPane scrollAreaBottom = new JScrollPane();
    public ResultOutput output = new ResultOutput();
    public CmdInput input = new CmdInput();
    private Document inputDoc = input.getDocument();
    private Document outputDoc = output.getDocument();

    private final InsertRemoveUndoManager undoMgr = new InsertRemoveUndoManager(this);

    private String wspace = null;
    public static String directory = System.getProperty("user.home");

    private int currentHistPosition = 0;

    private StringBuffer console = new StringBuffer();

    private boolean wasHistEvent = false;

    public int end = 0;
    private Integer clearpoint = null;

    public RConsole() {
        this(null);
    }

    public RConsole(File workSpace) {
        super("Console", iFrame.clsMain);
        String[] Menu = {
            "+", "File","Load Datafile", "loaddata","-","@NNew Workspace", "newwspace", "@OLoad Workspace",
            "loadwspace",
            "@SSave Workspace", "savewspace", "!SSave Workspace as",
            "savewspaceas","~File.Quit",
            "~Edit",
            "+", "Tools", "@EEditor", "editor", "@BObject Browser", "objectmgr",
            "DataTable", "table", "-", "Increase Fontsize", "fontBigger",
            "Decrease Fontsize", "fontSmaller",
            "+", "Packages", "Package Manager", "packagemgr",
            "~Window",
            "~Help", "R Help", "rhelp", "~About", "0"};
        iMenu.getMenu(this, this, Menu);

        if (JGR.RHISTORY == null) {
            JGR.RHISTORY = new Vector();
        }
        currentHistPosition = JGR.RHISTORY.size();

        input.addKeyListener(this);
        input.setWordWrap(false);
        input.setToolTipText("");

        output.addKeyListener(this);

        inputDoc.addUndoableEditListener(undoMgr);

        output.setEditable(false);

        input.addFocusListener(this);
        output.addFocusListener(this);

        scrollAreaTop.getViewport().add(output, null);
        scrollAreaBottom.getViewport().add(input, null);

        back.setTopComponent(scrollAreaTop);
        back.setBottomComponent(scrollAreaBottom);
        this.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                super.componentResized(evt);
                back.setDividerLocation( ( (int) ( (double) getHeight() * 0.65)));
                if (JGR.R != null && iPreferences.isMac) JGR.R.eval("options(width="+((int) (output.getWidth() / output.getFontMetrics(output.getFont()).getMaxAdvance())*1.9)+")");
                else if (JGR.R != null) JGR.R.eval("options(width="+((int) (output.getWidth() / iToolkit.schnitt(output.getFontMetrics(output.getFont()).getWidths())))+")");
            }
        });
        this.addKeyListener(this);
        this.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                dispose();
            }
        });

        this.getContentPane().setLayout(layout);
        this.getContentPane().add(initIconBar(),
                                  new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            , GridBagConstraints.WEST, GridBagConstraints.NONE,
            new Insets(5, 5, 2, 5), 0, 0));

        this.getContentPane().add(back,
                                  new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0
            , GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(2, 5, 5, 5), 0, 0));
        this.setMinimumSize(new Dimension(555,650));
        this.setSize(new Dimension(600,
                                   Common.screenRes.height < 800 ?
                                   Common.screenRes.height - 50 : 700));
        back.setDividerLocation( ( (int) ( (double)this.getHeight() * 0.65)));
        this.show();
    }


    public void exit() {
        dispose();
    }

    public void dispose() {
        Enumeration e = WinTracker.current.elements();
        while (e.hasMoreElements()) {
            WTentry we = (WTentry) e.nextElement();
            if (we.wclass == iFrame.clsEditor) {
                if (!((REditor) we.w).exit()) return;
            }
        }
        execute("q()");
    }

    public void execute(String cmd) {
         if (cmd.trim().length() > 0) JGR.RHISTORY.add(cmd);
        currentHistPosition = JGR.RHISTORY.size();

        String[] cmdArray = cmd.split("\n");

        String c = null;
        for (int i = 0; i < cmdArray.length; i++) {
            c = cmdArray[i];
            if (!isHelpCMD(c))
                JGR.rSync.triggerNotification(c);
            else
                try { outputDoc.insertString(outputDoc.getLength()," "+c+"\n> ",iPreferences.CMD); } catch (Exception e) {}
        }
    }

    public boolean isHelpCMD(String cmd) {
        if (cmd.startsWith("help") || cmd.startsWith("?") ) {
            help(cmd);
            return true;
        }
        return false;
    }



    public void help(String help) {
        boolean exact = false;
        //System.out.println(help);
        if (help != null) {
            help = help.replaceAll("[\"|(|)]", "");
            if (help.startsWith("help.search")) {
                help = help.replaceFirst("help.search", "");
            }
            else if (help.startsWith("help.start")) help=null;
            else {
                help = help.replaceFirst("help", "");
                help = help.replaceFirst("\\?", "");
                exact = true;
            }
        }
        final boolean e = exact;
        if (RHelp.current == null) {
            final String h;
            if (help!=null) h = help.trim();
            else h = null;
            Thread t = new Thread() {
                public void run() {
                    progress.start("Working");
                    setWorking(true);
                    try {
                        new RHelp();
                        if (h!=null) RHelp.current.search(h,e);
                    } catch (Exception e1) {
                        e1.printStackTrace();
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
                        progress.start("Working");
                        setWorking(true);
                        RHelp.current.show();
                        try {
                            RHelp.current.search(h,e);
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                        setWorking(false);
                    }
                };
                t.start();
            }
        }
    }


    public void clearconsole() {
        try {
            if (clearpoint==null) clearpoint = new Integer(output.getLineEndOffset(output.getLineOfOffset(end)-1)+2);
            output.removeAllFrom(clearpoint.intValue());
        } catch (Exception e) { new iError(e);/*e.printStackTrace();*/ }
    }


    public void loadWorkSpace() {
        FileSelector fopen = new FileSelector(this, "Open Workspace",
                                              FileSelector.OPEN, directory);
        if (fopen.getFile() != null) {
            wspace = (directory = fopen.getDirectory()) + fopen.getFile();
            execute("load(\""+wspace.replace('\\','/')+"\")");
        }
    }

    public void newWorkSpace() {
        int neww = JOptionPane.showConfirmDialog(null, "Save current workspace?",
                                                  "New Workspace",
                                                  JOptionPane.
                                                  YES_NO_CANCEL_OPTION,
                                                  JOptionPane.QUESTION_MESSAGE);

         if (neww == 0) {
             if (saveWorkSpaceAs()) execute("rm(list=ls())");
         }
         else if (neww == 1) execute("rm(list=ls())");
    }

    public void saveWorkSpace(final String file) {
        if (file==null) execute("save.image()");
        else execute("save.image(\""+(file == null ? "" : file.replace('\\','/'))+"\")");
        JGR.writeHistory();
    }

    public boolean saveWorkSpaceAs() {
        FileSelector fsave = new FileSelector(this, "Save Workspace as...",
                                              FileSelector.SAVE, directory);
        if (fsave.getFile() != null) {
            String file = (directory = fsave.getDirectory()) + fsave.getFile();
            saveWorkSpace(file);
            JGR.writeHistory();
            return true;
        }
        else return false;
    }

    public JPanel initIconBar() {
        JPanel buttons = new JPanel(new GridBagLayout());
        buttons.add(newButton = new IconButton("/icons/new.png",
            "New", this, "newwspace"),
                                  new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
            GridBagConstraints.WEST, GridBagConstraints.NONE,
            new Insets(0, 0, 0, 5), 0, 0));
        buttons.add(openButton = new IconButton("/icons/open.png",
            "Open", this, "loadwspace"),
                                  new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
            GridBagConstraints.WEST, GridBagConstraints.NONE,
            new Insets(0, 5, 0, 5), 0, 0));
        buttons.add(saveButton = new IconButton("/icons/save.png",
            "Save", this, "savewspace"),
                                  new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
            GridBagConstraints.WEST, GridBagConstraints.NONE,
            new Insets(0, 5, 0, 5), 0, 0));
        buttons.add(undoButton = undoMgr.undoButton,
                                  new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0,
            GridBagConstraints.WEST, GridBagConstraints.NONE,
            new Insets(0, 20, 0, 5), 0, 0));
        buttons.add(redoButton = undoMgr.redoButton,
                                  new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0,
            GridBagConstraints.WEST, GridBagConstraints.NONE,
            new Insets(0, 5, 0, 5), 0, 0));
        buttons.add(cutButton = new IconButton("/icons/cut.png",
            "Cut", this, "cut"),
                                  new GridBagConstraints(5, 0, 1, 1, 0.0, 0.0,
            GridBagConstraints.WEST, GridBagConstraints.NONE,
            new Insets(0, 5, 0, 5), 0, 0));
        buttons.add(copyButton = new IconButton("/icons/copy.png",
            "Copy", this, "copy"),
                                  new GridBagConstraints(6, 0, 1, 1, 0.0, 0.0,
            GridBagConstraints.WEST, GridBagConstraints.NONE,
            new Insets(0, 5, 0, 5), 0, 0));
        buttons.add(pasteButton = new IconButton(
            "/icons/paste.png", "Paste", this, "paste"),
                                  new GridBagConstraints(7, 0, 1, 1, 0.0, 0.0,
            GridBagConstraints.WEST, GridBagConstraints.NONE,
            new Insets(0, 5, 0, 5), 0, 0));
        buttons.add(stopButton = new IconButton(
            "/icons/stop.png", "Stop", this, "stop"),
                                  new GridBagConstraints(8, 0, 1, 1, 0.0, 0.0,
            GridBagConstraints.WEST, GridBagConstraints.NONE,
            new Insets(0, 20, 0, 5), 0, 0));

        stopButton.setEnabled(false);

        buttons.add(helpButton = new IconButton("/icons/help.png",
            "R Help", this, "rhelp"),
                                  new GridBagConstraints(9, 0, 1, 1, 0.0, 0.0,
            GridBagConstraints.WEST, GridBagConstraints.NONE,
            new Insets(0, 20, 0, 5), 0, 0));
        buttons.add(progress,
                                  new GridBagConstraints(10, 0, 1, 1, 0.0, 0.0
            , GridBagConstraints.EAST, GridBagConstraints.NONE,
            new Insets(0, 50, 0, 5), 0, 0));
        return buttons;
    }


    public void   rWriteConsole(Rengine re, String text) {
        console.append(text);
        if (console.length() > 100) {
            output.append(console.toString(),iPreferences.RESULT);
            console.delete(0,console.length());
            output.setCaretPosition(outputDoc.getLength());
        }
        //try { Thread.sleep(5);} catch (Exception e) {}
    }

    public void   rBusy(Rengine re, int which) {
        if (which==0) {
            if (console != null) {
                output.append(console.toString(), iPreferences.RESULT);
                console.delete(0, console.length());
            }
            output.setCaretPosition(outputDoc.getLength());
            setWorking(false);
        }
        else {
            stopButton.setEnabled(true);
            progress.start("Working");
            setWorking(true);
        }
    }

    public String rReadConsole(Rengine re, String prompt, int addToHistory) {
        setWorking(false);
        stopButton.setEnabled(false);
        if (prompt.indexOf("Save workspace") > -1) return JGR.exit();
        else {
            output.append(prompt,iPreferences.CMD);
            output.setCaretPosition(outputDoc.getLength());
            String s = JGR.rSync.waitForNotification();
            try { outputDoc.insertString(outputDoc.getLength()," "+s+"\n",iPreferences.CMD); } catch (Exception e) {}
            System.out.println("read console "+s);
            return (s==null||s.length()==0)?"\n":s+"\n";
        }
    }

    public void   rShowMessage(Rengine re, String message) {
        JOptionPane.showMessageDialog(this,message,"R Message",JOptionPane.INFORMATION_MESSAGE);
    }

    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if (cmd == "about") new AboutDialog(this);
        else if (cmd == "cut") input.cut();
        else if (cmd == "copy") {
            input.copy();
            output.copy();
        } else if (cmd == "clearconsole") clearconsole();
        else if (cmd == "delete") {
            try {
                int i = 0;
                inputDoc.remove( (i = input.getSelectionStart()),
                                input.getSelectionEnd() - i);
            } catch (BadLocationException ex) {}
        } else if (cmd == "editor") new REditor();
        else if (cmd == "exit") dispose();
        else if (cmd == "fontBigger") FontTracker.current.setFontBigger();
        else if (cmd == "fontSmaller") FontTracker.current.setFontSmaller();
        else if (cmd == "loaddata") new RDataFileDialog(this, directory);
        else if (cmd == "loadwspace") loadWorkSpace();
        else if (cmd == "newwspace") newWorkSpace();
        else if (cmd == "objectmgr") execute("object.manager()");
        else if (cmd == "packagemgr") execute("package.manager()");
        else if (cmd == "paste") input.paste();
        else if (cmd == "prefs") new PrefsDialog(this);
        else if (cmd == "redo") {
            try {
                if (undoMgr.canRedo()) {
                    undoMgr.redo();
                }
            } catch (CannotUndoException ex) {}
        } else if (cmd == "rhelp")  execute("help.start()");
        else if (cmd == "table") new DataTable(null);
        else if (cmd == "savewspace") saveWorkSpace(wspace);
        else if (cmd == "savewspaceas") saveWorkSpaceAs();
        else if (cmd == "stop") JGR.R.rniStop(1);
        else if (cmd == "selAll") {
            if (input.isFocusOwner()) {
                input.selectAll();
            } else if (output.isFocusOwner()) {
                output.selectAll();
            }
        } else if (cmd == "undo") {
            try {
                if (undoMgr.canUndo()) {
                    undoMgr.undo();
                }
            } catch (Exception ex) {}
        }
    }

    public void keyTyped(KeyEvent ke) {
    }

    public void keyPressed(KeyEvent ke) {
        if (iPreferences.useEmacsKeyBindings) {
            if (ke.getKeyCode() == KeyEvent.VK_E && ke.isControlDown()) {
                input.setCaretPosition(input.getText().length());
            }
            if (ke.getKeyCode() == KeyEvent.VK_A && ke.isControlDown()) {
                input.setCaretPosition(0);
            }
        }
        if (ke.getKeyCode() == KeyEvent.VK_UP && currentHistPosition > 0) {
            if (input.getCaretPosition()==0 || input.getCaretPosition()==input.getText().length()) {
                if (input.getText().trim().length() > 0) JGR.RHISTORY.insertElementAt(input.getText().trim(),currentHistPosition);
                System.out.println(JGR.RHISTORY.get(currentHistPosition-1).toString());
                input.setText(JGR.RHISTORY.get(--currentHistPosition).toString());
                input.setCaretPosition(input.getText().length());
                wasHistEvent = true;
            }
        }
        else if (ke.getKeyCode() == KeyEvent.VK_DOWN) {
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

    public void keyReleased(KeyEvent ke) {
        if (ke.getKeyCode() == KeyEvent.VK_TAB) {
            String text = null;
            int pos = input.getCaretPosition();
            if (pos == 0)
                return;
            try {
                text = input.getText(input.getLineStartOffset(input.
                    getLineOfOffset(pos)), pos);
            }
            catch (Exception e) {}
            if (text == null)
                return;
            int tl = text.length(), tp = 0, quotes = 0, dquotes = 0,
                lastQuote = -1;
            while (tp < tl) {
                char c = text.charAt(tp);
                if (c == '\\')
                    tp++;
                else {
                    if (dquotes == 0 && c == '\'') {
                        quotes ^= 1;
                        if (quotes == 0)
                            lastQuote = tp;
                    }
                    if (quotes == 0 && c == '"') {
                        dquotes ^= 1;
                        if (dquotes == 0)
                            lastQuote = tp;
                    }
                }
                tp++;
            }
            String last = input.getLastPart();
            if (last != null) {
                String result = null;
                if ( (quotes + dquotes) > 0)
                  result = RTalk.completeFile(last.substring(last.
                      lastIndexOf("\"", pos - 1) + 1));
                else
                    result = RTalk.completeCode(last);
                if (result != null && !result.equals(last))
                    input.insertAt(pos, result);
                else
                    Toolkit.getDefaultToolkit().beep();
            }
            else
                Toolkit.getDefaultToolkit().beep();
        }
        else if (ke.getKeyCode() == KeyEvent.VK_ENTER) {
            if (ke.isControlDown() || ke.isMetaDown()) {
                try { inputDoc.insertString(input.getCaretPosition(), "\n", null); } catch (Exception e) {}
            }
            else {
                String cmd = input.getText().trim();
                input.setText("");
                input.setCaretPosition(0);
                input.requestFocus();
                execute(cmd);
            }
        }
        if (ke.getSource().equals(output) && ke.getKeyCode() == KeyEvent.VK_V && (ke.isControlDown() || ke.isMetaDown())) {
            input.paste();
            input.requestFocus();
            input.setCaretPosition(input.getText().length());
        }
        else if ((ke.getKeyCode() == KeyEvent.VK_UP || ke.getKeyCode() == KeyEvent.VK_DOWN) && wasHistEvent) {
            wasHistEvent = false;
            input.setCaretPosition(input.getText().length());
        }
    }

    public void focusGained(FocusEvent e) {
        if (e.getSource().equals(output)) {
            cutButton.setEnabled(false);
            iMenu.getItem(this, "cut").setEnabled(false);
        } else if (e.getSource().equals(input)) {
            cutButton.setEnabled(true);
            iMenu.getItem(this, "cut").setEnabled(true);
        }
    }

    public void focusLost(FocusEvent e) {
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    class CmdInput extends SyntaxArea {

        public CmdInput() {
        }

        public String getToolTipText(MouseEvent e) {
            String s = getCurrentWord();
            if (s!=null && JGR.STARTED) return RTalk.getArgs(s);
            return null;
        }

        protected boolean processKeyBinding(KeyStroke ks, KeyEvent e,
                                                int condition, boolean pressed) {

            if (e.getKeyCode() == KeyEvent.VK_ENTER) return true;

            InputMap map = getInputMap(condition);
            ActionMap am = getActionMap();

            if(map != null && am != null && isEnabled()) {
                Object binding = map.get(ks);
                Action action = (binding == null) ? null : am.get(binding);
                if (action != null) {
                    return SwingUtilities.notifyAction(action, ks, e, this,
                                                       e.getModifiers());
                    }
                }
            return false;
        }
    }

    class ResultOutput extends JTextPane {
        public ResultOutput() {
            if (FontTracker.current == null) FontTracker.current = new FontTracker();
            FontTracker.current.add(this);
            setDocument(new RStyledDocument());
        }

        public void append(String str, AttributeSet a) {
            Document doc = getDocument();
                if (doc != null) {
                    try {
                        doc.insertString(doc.getLength(), str, a);
                    } catch (BadLocationException e) {
                    }
                }
        }


        public int getLineCount() {
            Element map = getDocument().getDefaultRootElement();
            return map.getElementCount();
        }


        public int getLineStartOffset(int line) throws BadLocationException {
            int lineCount = getLineCount();
            if (line < 0) {
                throw new BadLocationException("Negative line", -1);
            } else if (line >= lineCount) {
                throw new BadLocationException("No such line", getDocument().getLength()+1);
            } else {
                Element map = getDocument().getDefaultRootElement();
                Element lineElem = map.getElement(line);
                return lineElem.getStartOffset();
            }
        }

        public int getLineEndOffset(int line) throws BadLocationException {
            int lineCount = getLineCount();
            if (line < 0) {
                throw new BadLocationException("Negative line", -1);
            } else if (line >= lineCount) {
                throw new BadLocationException("No such line", getDocument().getLength()+1);
            } else {
                Element map = getDocument().getDefaultRootElement();
                Element lineElem = map.getElement(line);
                int endOffset = lineElem.getEndOffset();
                // hide the implicit break at the end of the document
                return ((line == lineCount - 1) ? (endOffset - 1) : endOffset);
            }
        }


        public int getLineOfOffset(int offset) throws BadLocationException {
            Document doc = getDocument();
            if (offset < 0) {
                throw new BadLocationException("Can't translate offset to line", -1);
            } else if (offset > doc.getLength()) {
                throw new BadLocationException("Can't translate offset to line",
                                               doc.getLength() + 1);
            } else {
                Element map = getDocument().getDefaultRootElement();
                return map.getElementIndex(offset);
            }
        }

        public void removeAllFrom(int index) throws BadLocationException {
            this.getDocument().remove(index,this.getDocument().getLength()-index);
        }


        public void setFont(Font f) {
            super.setFont(f);
            try {
                ((StyledDocument) this.getDocument()).setCharacterAttributes(0, this.getText().length(),iPreferences.SIZE, false);
            } catch (Exception e) {}
        }
    }
}
