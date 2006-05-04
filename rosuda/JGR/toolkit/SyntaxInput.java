package org.rosuda.JGR.toolkit;

//JGR - Java Gui for R, see http://www.rosuda.org/JGR/
//Copyright (C) 2003 - 2005 Markus Helbig
//--- for licensing information see LICENSE file in the original JGR distribution ---

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.BadLocationException;

import org.rosuda.JGR.RController;

/**
 *  SyntaxInput - is responsible for codecompletion and helpagent
 *
 *	@author Markus Helbig
 *
 * 	RoSuDa 2003 - 2004
 */

public class SyntaxInput extends SyntaxArea implements KeyListener {
	
    private boolean disableEnter = false;
    private String fun = null;
    private String funHelp = null;
    Popup funHelpTip = null;
    private Popup cmdHelp = null;
    private JToolTip Tip = new JToolTip();
    /** {@see CodeCompleteMultiple} */
    public  CodeCompleteMultiple mComplete;
    private Point pc;
	private Point pco;
	private Point ph;
	private iFrame parent;
    
    private String comp;
    
    public SyntaxInput(String parent, boolean disableEnter){
        this.disableEnter = disableEnter;
        this.addKeyListener(this);
        this.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (funHelpTip != null) funHelpTip.hide();
                if (mComplete != null && mComplete.isVisible()) {
                    if (cmdHelp != null) cmdHelp.hide();
                    mComplete.setVisible(false);
                }
            }
        });
        this.setDocument(new SyntaxInputDocument());
        mComplete = new CodeCompleteMultiple(this);
        this.comp = parent;
		this.setFocusTraversalKeysEnabled(false);
    }
	
    private String getLastCommand() {
		if (funHelpTip != null) funHelpTip.hide();
        String word = null;
        String text = this.getText();
        int pos = this.getCaretPosition();
        int lastb = this.getText(0,pos+1).lastIndexOf('(');
        int lasteb = this.getText(0,pos).lastIndexOf(')');
        if (lasteb > lastb) return null;
        //if (lastb < 0) lastb = text.indexOf('(',pos);
        if (lastb < 0) return null;
		if (pos < 0) return null;
        int line, loffset, lend;
		try {
        	line = this.getLineOfOffset(pos);
        	loffset = this.getLineStartOffset(line);
        	lend = this.getLineEndOffset(line);
			if (lastb > lend) return null;
        }
        catch (Exception e) { return null; }
        if (text.substring(loffset,pos).indexOf("#") >= 0) return null; //comment line
        int offset = lastb--, end = lastb; pos=lastb;
        if (text==null) return null;
        while (offset > -1 && pos > -1) {
            char c = text.charAt(pos);
            if ((c>='0' && c <='9')||((c>='a')&&(c<='z'))||((c>='A')&&(c<='Z'))||c=='.'||c=='_') offset--;
            else break;
            pos--;
        }
        offset = offset==-1?0:offset;
		try {
        	line = this.getLineOfOffset(this.getCaretPosition());
        	loffset = this.getLineStartOffset(line);
        	lend = this.getLineEndOffset(line);
			if (offset < loffset || end > lend) return null;
        }
        catch (Exception e) { return null; }
		if (this.getCaretPosition() < offset) return null;
        end = ++lastb;
        return (offset!=end)?text.substring(offset,end).trim():null;
    }
	
    private String getLastPart() {
        String word = null;
        String text = this.getText();
        int pos = this.getCaretPosition();
        if (pos > 0 && text.length() > 0 && text.charAt(pos-1)=='(') pos--;
        if (pos < 0) return null;
        int offset = pos-1, end = pos; pos--;
        if (text==null) return null;
        int l = text.length();
        while (offset > -1 && pos > -1) {
            char c = text.charAt(pos);
            if (((c>='0')&&(c<='9')) || ((c>='a')&&(c<='z'))||((c>='A')&&(c<='Z'))||c=='.'||c=='_'||c=='\\'||c=='/'||c=='~' || c=='$') offset--;
            else break;
            pos--;
        }
        offset = offset==-1?0:++offset;
        return (offset!=end)?text.substring(offset,end).trim():null;
    }
    
    /**
     * Comment/ Uncomment selected Code
     * @param comment remove comment string or add
     * @throws BadLocationException
     */
    public void commentSelection(boolean comment) throws BadLocationException {
    	int a = this.getLineOfOffset(this.getSelectionStart());
    	int b = this.getLineOfOffset(this.getSelectionEnd());
    	while ( a <= b) {
    		int ls = this.getLineStartOffset(a);
    		ls = this.getSelectionStart() > ls?this.getSelectionStart():ls;
			int le = this.getLineEndOffset(a);
			if (comment && !this.getText(ls,le-ls).trim().startsWith("#") && !this.getText(ls-1,1).equals("#")) this.insertAt(ls,"#");
    		if (!comment) {
    			ls = this.getLineStartOffset(a);
    			int i = this.getText(ls,le-ls).indexOf("#");
    			if (i >= 0) this.getDocument().remove(ls+i,1);
    		}
    		a++;
    	}
	}
    
    /**
     * Indent/ Reindent selected lines
     * @param direction -1 for left and 1 for right indentation
     * @throws BadLocationException
     */
    public void shiftSelection(int direction) throws BadLocationException {
    	int a = this.getLineOfOffset(this.getSelectionStart());
    	int b = this.getLineOfOffset(this.getSelectionEnd());
    	while ( a <= b) {
    		int ls = this.getLineStartOffset(a);
    		int le = this.getLineEndOffset(a);
    		if (direction == -1 && this.getText(ls,le-ls).startsWith("\t")) {
    			this.getDocument().remove(ls,1);
    		}
    		else if (direction == 1) {
    			this.insertAt(ls,"\t");
    		}
    		a++;
    	}
    	
    }
	
    /**
     * Show possibilities for current part of command.
     * @param result possibilities for completion.
     */
    public void showCompletions(String[] result) {
        try {
			this.requestFocus(true);
            if (cmdHelp != null) cmdHelp.hide();
        	pc = getCaret().getMagicCaretPosition();
			if (pc == null) {
				processKeyEvent(new KeyEvent(this,KeyEvent.KEY_TYPED,0,KeyEvent.VK_TAB,KeyEvent.VK_TAB,'\t'));
				pc = getCaret().getMagicCaretPosition();
			}
			if (pco != null && pco.equals(pc)) pc = pco;
			else SwingUtilities.convertPointToScreen(pc,this);
        	mComplete.refresh(result);
        	mComplete.setVisible(true);
        	cmdHelp = PopupFactory.getSharedInstance().getPopup(this,mComplete,pc.x,pc.y+15);
        	cmdHelp.show();
        } catch (Exception e) {}
		finally {
			this.requestFocus(true);
		}
    }
	
    /**
     * keyTyped: handle key event.
     */
    public void keyTyped(KeyEvent ke) {
    }
	
    /**
     * keyPressed: handle key event: take care about emacs-keybindings.
     */
    public void keyPressed(KeyEvent ke) {
		this.requestFocus(true);
        if (JGRPrefs.useEmacsKeyBindings) {
            if (ke.getKeyCode() == KeyEvent.VK_E && ke.isControlDown()) {
				try {
					int line = this.getLineOfOffset(this.getCaretPosition());
					int lend = this.getLineEndOffset(line);
					this.setCaretPosition((this.getLineCount()==1?lend:lend-1));
				}
				catch (Exception e) { this.setCaretPosition(this.getText().length()); }
            }
            if (ke.getKeyCode() == KeyEvent.VK_A && ke.isControlDown()) {
				try {
					int line = this.getLineOfOffset(this.getCaretPosition());
					int loffset = this.getLineStartOffset(line);
					this.setCaretPosition(loffset);
				}
				catch (Exception e) { this.setCaretPosition(0); }
            }
        }
    }
	
    /**
     * keyReleased: handle key event: ESC hide tooltips, TAB show completions and/ or complete.
     */
    public void keyReleased(KeyEvent ke) {
        if (ke.getKeyCode() == KeyEvent.VK_TAB) {
			if (funHelpTip != null) {
                funHelpTip.hide();
                funHelpTip = null;
            }
            String text = null;
            int pos = getCaretPosition();
            if (pos==0) return;
            try {
                int i = getLineStartOffset(getLineOfOffset(pos));
                text = getText(i,pos-i);
            } catch (Exception e) {
            }
            if (text == null) return;
            int tl = text.length(), tp=0, quotes=0, dquotes=0, lastQuote=-1;
            while (tp<tl) {
                char c=text.charAt(tp);
                if (c=='\\') tp++;
                else {
                    if (dquotes==0 && c=='\'') {
                        quotes^=1;
                        if (quotes==0) lastQuote=tp;
                    }
                    if (quotes==0 && c=='"') {
                        dquotes^=1;
                        if (dquotes==0) lastQuote=tp;
                    }
                }
                tp++;
            }
            fun = getLastPart();
			String[] result = null; // = new String[1];
			if ((quotes+dquotes)>0) result = RController.completeFile(fun==null?"":fun);
			else if (fun != null) result = RController.completeCommand(fun.replaceAll("\\.","\\."));
			if (result != null && result.length > 1) {
				if (funHelpTip != null) funHelpTip.hide();
				if (pc == null || !pc.equals(getCaret().getMagicCaretPosition()))
					showCompletions(result);
				//if (JGRPrefs.isMac && cmdHelp != null) cmdHelp.show();
			}
			else {
				if (result != null && result.length > 0 && result[0] != null && !result[0].equals(fun) ) {
					insertAt(pos,result[0].replaceFirst(fun,""));
					if (cmdHelp != null) cmdHelp.hide();
					if (mComplete != null) mComplete.setVisible(false);
				}
				else if (fun != null && fun.length() > 0) Toolkit.getDefaultToolkit().beep();
			}
        }
        else if (mComplete != null && mComplete.isVisible()) {
            int k = ke.getKeyCode();
            if (k != KeyEvent.VK_ESCAPE && k != KeyEvent.VK_ENTER && k != KeyEvent.VK_DOWN && k != KeyEvent.VK_UP && k != KeyEvent.VK_LEFT && k != KeyEvent.VK_RIGHT && k != KeyEvent.VK_TAB && !ke.isShiftDown() && !ke.isMetaDown() && !ke.isControlDown() && !ke.isAltDown() && !ke.isAltGraphDown()) {
                fun = getLastPart();
                if (fun != null) {
                    String[] result = new String[1];
                    result = RController.completeCommand(fun);
                    if (result != null && result.length > 0){
                        if (funHelpTip != null) {
                            funHelpTip.hide();
                            funHelpTip = null;
                        }
                        showCompletions(result);
                    }
                    else {
                        if (cmdHelp != null) cmdHelp.hide();
                        mComplete.setVisible(false);
                    }
                }
                else {
                    if (cmdHelp != null) cmdHelp.hide();
                    mComplete.setVisible(false);
                }
            }
        }
        else if (ke.getKeyCode() == KeyEvent.VK_ENTER) {
            if (cmdHelp != null) cmdHelp.hide();
            mComplete.setVisible(false);
            if (funHelpTip != null) {
                funHelpTip.hide();
                funHelpTip = null;
            }
        }
        if (ke.getKeyCode() != KeyEvent.VK_TAB && ke.getKeyCode() != KeyEvent.VK_UP && ke.getKeyCode() != KeyEvent.VK_DOWN && ke.getKeyCode() != KeyEvent.VK_ESCAPE && ke.getKeyCode() != KeyEvent.VK_ENTER && !ke.isMetaDown() && !ke.isControlDown() && !ke.isAltDown() && !ke.isShiftDown() && JGRPrefs.useHelpAgent && isHelpAgentWanted() && !ke.isShiftDown()) {
            if (funHelpTip != null) {
                funHelpTip.hide();
                funHelpTip = null;
            }
            showFunHelp(getLastCommand());
        }
		if (ke.getKeyCode() == KeyEvent.VK_ESCAPE) {
            if (cmdHelp != null) { cmdHelp.hide(); pco = pc;}
            mComplete.setVisible(false);
            if (funHelpTip != null) funHelpTip.hide();
			pc = null;
        }
		if (mComplete.isVisible() || ke.getKeyCode() == KeyEvent.VK_UP || ke.getKeyCode() == KeyEvent.VK_DOWN || ke.getKeyCode() == KeyEvent.VK_TAB) {
			if (funHelpTip != null) funHelpTip.hide();
		}
    }
    
    private boolean isHelpAgentWanted() {
    	if (comp.equals("console")) return JGRPrefs.useHelpAgentConsole;
    	if (comp.equals("editor")) return JGRPrefs.useHelpAgentEditor;
    	return JGRPrefs.useHelpAgent;
    }
	
    
    private void showFunHelp(String fun) {
        try {
			this.requestFocus(true);
            funHelp = RController.getFunHelpTip(fun);
            if (fun != null && funHelp != null) {
                Tip = new JToolTip();
                Tip.setTipText(funHelp);
                Tip.addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) {
                        if (funHelpTip != null) {
                            funHelpTip.hide();
                            funHelpTip = null;
                        }
                    }
                });
                ph = getCaret().getMagicCaretPosition();
                SwingUtilities.convertPointToScreen(ph,this);
                funHelpTip = PopupFactory.getSharedInstance().getPopup(this,Tip,ph.x,ph.y+20);
                funHelpTip.show();
                //commands.add(funHelp);
                //commands.add(p);
            }
        } catch (Exception e) {}
		finally {
			this.requestFocus(true);
		}
    }
	
    protected boolean processKeyBinding(KeyStroke ks, KeyEvent e,int condition, boolean pressed) {
        if (disableEnter && e.getKeyCode() == KeyEvent.VK_ENTER) return true;
		if (mComplete.isVisible() && (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_DOWN)) return true;
        InputMap map = getInputMap(condition);
        ActionMap am = getActionMap();

        if(map != null && am != null && isEnabled()) {
            Object binding = map.get(ks);
            Action action = (binding == null) ? null : am.get(binding);
            if (action != null) {
                return SwingUtilities.notifyAction(action, ks, e, this, e.getModifiers());
            }
        }
        return false;
    }
	
    class SyntaxInputDocument extends SyntaxDocument {
    }
	
    /**
     * 
     * CodeCompleteMultiple - implementation of a panel showing a list of completion-possibilities.
     * 
     * @author Markus Helbig
     * 
     * RoSuDA 2003 - 2005
     *
     */
    public class CodeCompleteMultiple extends JPanel {
		
        public JList cmds = new JList();
		private JScrollPane sp = new JScrollPane();
        private SyntaxInput parent = null;
		
        public CodeCompleteMultiple(SyntaxInput tcomp) {
            parent = tcomp;
            cmds.addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER)
                        completeCommand();
                }
            });
            cmds.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2)
                        completeCommand();
                }
            });
			sp = new JScrollPane(cmds);
			sp.setMinimumSize(new Dimension(200,120));
			sp.setPreferredSize(new Dimension(200,120));
			sp.setMaximumSize(new Dimension(200,120));
			sp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
			sp.setAutoscrolls(true);
            this.setLayout(new GridLayout(1,1));
            this.add(sp);
            this.setVisible(false);
        }
		
        /**
         * Refresh content.
         * @param commands new commands
         */
        public void refresh(String[] commands) {
			cmds.setListData(commands);
			cmds.setSelectedIndex(0);
			cmds.ensureIndexIsVisible(0);
        }
		
        /**
         * Complete current part.
         */
        public void completeCommand() {
            parent.insertAt(parent.getCaretPosition(),cmds.getSelectedValue().toString().replaceFirst(fun==null?"":fun,""));
            this.setVisible(false);
            if (cmdHelp != null) cmdHelp.hide();
        }
		
        /**
         * Select previous possibility.
         */
        public void selectPrevious() {
            int i = cmds.getSelectedIndex();
            if (--i >= 0)
				cmds.setSelectedIndex(i);
			cmds.ensureIndexIsVisible(cmds.getSelectedIndex());			
        }
		
        /**
         * Select next possibility.
         */
        public void selectNext() {
            int i = cmds.getSelectedIndex();
            if (++i < cmds.getModel().getSize())
				cmds.setSelectedIndex(i);
			cmds.ensureIndexIsVisible(cmds.getSelectedIndex());
        }
		
        /**
         * Show CodeCompletionMultiple.
         */
        public void setVisible(boolean b) {
            if (!b && cmdHelp != null) cmdHelp.hide();
            super.setVisible(b);
        }
    }
}

