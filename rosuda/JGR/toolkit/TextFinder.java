package org.rosuda.JGR.toolkit;

//JGR - Java Gui for R, see http://www.rosuda.org/JGR/
//Copyright (C) 2003 - 2005 Markus Helbig
//--- for licensing information see LICENSE file in the original JGR distribution ---

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;

import org.rosuda.ibase.*;

/**
 *  TextFinder - find specified pattern in attached textcomponent
 * 
 *	@author Markus Helbig
 *  
 * 	RoSuDa 2003 - 2005 
 */

public class TextFinder extends JDialog implements ActionListener {

    private GridBagLayout layout = new GridBagLayout();

    private Dimension screenSize = Common.screenRes;

    private JTextField keyWordField = new JTextField();
    private JTextComponent searchArea = null;
    private JButton searchButton = new JButton("Find");
    private JButton cancelButton = new JButton("Cancel");
    private JLabel status = new JLabel("                       ");

    Highlighter.HighlightPainter highLighter = new FoundHighlighter(JGRPrefs.HighLightColor);

    private String keyWord = null;
    private int position = -1;
    private boolean found = false;

    private TextFinder last = null;

    public TextFinder() {
        this(null);
    }

    public TextFinder(JTextComponent searchArea) {
        this.setTitle("Find");

        this.searchArea = searchArea;

        Dimension d = new Dimension(80,25);
        searchButton.setActionCommand("search");
        searchButton.addActionListener(this);
        searchButton.setMaximumSize(d);
        searchButton.setMinimumSize(d);
        searchButton.setPreferredSize(d);
        cancelButton.setActionCommand("cancel");
        cancelButton.addActionListener(this);
        cancelButton.setMaximumSize(d);
        cancelButton.setPreferredSize(d);
        cancelButton.setMinimumSize(d);

        this.getRootPane().setDefaultButton(searchButton);

        FontTracker.current.add(keyWordField);
        keyWordField.setFont(JGRPrefs.DefaultFont);
        keyWordField.setMaximumSize(new Dimension(300,25));
        keyWordField.setMinimumSize(new Dimension(300,25));
        keyWordField.setPreferredSize(new Dimension(300,25));
        
        JPanel top = new JPanel();
        top.add(keyWordField);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(status);
        bottom.add(searchButton);
        bottom.add(cancelButton);
        
        this.getContentPane().setLayout(new BorderLayout());
        
        this.getContentPane().add(top,BorderLayout.CENTER);
        this.getContentPane().add(bottom,BorderLayout.SOUTH);
        
        this.setSize(new Dimension(320, 95));
        this.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                exit();
            }
        });
        this.setResizable(false);
    }

    private void exit() {
        removeHighlights(searchArea);
        setVisible(false);
    }
    
    /**
     * Attach textcomponent to finder.
     * @param comp textcomponent
     */
    public void setSearchArea(JTextComponent comp) {
        this.searchArea = comp;
    }

    private void find() {
        if (searchArea == null) return;
        searchArea.requestFocus();
        if (keyWord != null && !keyWord.equals(keyWordField.getText().trim())) {
            position = -1;
            found = false;
        }
        keyWord = keyWordField.getText().trim().toLowerCase();
        if (!keyWord.equals("")) {
            position = searchArea.getText().toLowerCase().indexOf(keyWord, position + 1);
            if (position == -1) {
                if (!found) status.setText("No found!              ");
                else  status.setText("No more results!       ");
                found = false;
            }
            else {
            	status.setText("                       ");
                highlight(searchArea,position, position + keyWord.length());
                searchArea.select(position,position);
                found = true;
            }

        }
    }


    private void highlight(JTextComponent textComp, int off, int end) {
        removeHighlights(textComp);
        try {
            Highlighter hilite = textComp.getHighlighter();
            hilite.addHighlight(off, end, highLighter);
        } catch (BadLocationException e) {
        }
    }

    private void removeHighlights(JTextComponent textComp) {
        Highlighter hilite = textComp.getHighlighter();
        Highlighter.Highlight[] hilites = hilite.getHighlights();

        for (int i=0; i<hilites.length; i++) {
            if (hilites[i].getPainter() instanceof FoundHighlighter) {
                hilite.removeHighlight(hilites[i]);
            }
        }
    }

    private void showFinder() {
        keyWordField.requestFocus();
        this.setLocation((screenSize.width - 400 )/ 2, (screenSize.height - 100) / 2);
        super.setVisible(true);
        super.toFront();
    }

    /**
     * Show the textfinder.
     * @param next false if show a new one, true if searching for the same keyword as before.
     * @return TextFinder
     */
    public TextFinder showFind(boolean next) {
        if (!next) {
            keyWordField.setText(null);
            keyWord = null;
            position = -1;
            found = false;
            showFinder();
        }
        else {
            keyWordField.setText(keyWord);
            showFinder();
            find();
        }
        return last;
    }

    /**
     * actionPerformed: handle action event: buttons.
     */
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if (cmd=="cancel") this.exit();
        else if (cmd=="search") this.find();
    }

    class FoundHighlighter extends DefaultHighlighter.DefaultHighlightPainter {
        public FoundHighlighter(Color color) {
            super(color);
        }
    }
}