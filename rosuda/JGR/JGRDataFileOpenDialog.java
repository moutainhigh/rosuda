package org.rosuda.JGR;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.io.*;
import javax.swing.*;

import org.rosuda.ibase.*;

import org.rosuda.JGR.toolkit.*;

/**
 *  JGRDataFileOpenDialog - implementation of a file-dialog which allows loading datasets into R by choosing several options.
 *
 *	@author Markus Helbig
 *
 * 	RoSuDA 2003 - 2005
 */

public class JGRDataFileOpenDialog extends JFileChooser implements ActionListener, ItemListener, PropertyChangeListener {
	
    private JTextField dataName = new JTextField();
    private JTextField otherSeps = new JTextField();
    private JCheckBox header = new JCheckBox("Header",true);
    private JCheckBox attach = new JCheckBox("Attach",false);
	
	private JComboBoxExt sepsBox = new JComboBoxExt(new String[] {"Default","\\t",",",";","|","Others..."});
    private String[] seps = new String[] {" ","\\t",",",";","|"};
	
    private JComboBoxExt quoteBox = new JComboBoxExt(new String[] {"Default","\\\"","\\'","Others..."});
	private String[] quotes = new String[] {""};

	
	private Dimension screenSize = Common.getScreenRes();

	/**
	 * Create a new DataFileOpenDialog
	 * @param f parent frame
	 * @param directory current directory
	 */
	public JGRDataFileOpenDialog(Frame f,String directory) {
		
		dataName.setMinimumSize(new Dimension(180,22));
		dataName.setPreferredSize(new Dimension(180,22));
		dataName.setMaximumSize(new Dimension(180,22));
		
		quoteBox.setMinimumSize(new Dimension(90,22));
		quoteBox.setPreferredSize(new Dimension(90,22));
		quoteBox.setMaximumSize(new Dimension(90,22));
		
		sepsBox.setMinimumSize(new Dimension(90,22));
		sepsBox.setPreferredSize(new Dimension(90,22));
		sepsBox.setMaximumSize(new Dimension(90,22));
				
		quoteBox.addItemListener(this);
		sepsBox.addItemListener(this);
		
		this.addActionListener(this);
		this.addPropertyChangeListener(this);
		if (directory != null && new File(directory).exists()) this.setCurrentDirectory(new File(directory));
		
		
		
		JPanel options = new JPanel();
		BoxLayout box = new BoxLayout(options,BoxLayout.Y_AXIS);
		JPanel command = new JPanel(new FlowLayout(FlowLayout.LEFT));
		command.add(new JLabel(" read.table(...) -> "));
		command.add(dataName);
		
		JPanel command2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
		command2.add(header);
		command2.add(new JLabel(", sep="));
		command2.add(sepsBox);
		command2.add(new JLabel(", quote="));
		command2.add(quoteBox);
		
		JPanel att = new JPanel(new FlowLayout(FlowLayout.LEFT));
		att.add(attach);
		
		
		options.add(command); 
		options.add(command2);
		options.add(att); 
		
		if (System.getProperty("os.name").startsWith("Window")) {
			JPanel fileview = (JPanel)((JComponent)((JComponent)this.getComponent(2)).getComponent(2)).getComponent(2);
			fileview.add(command);
			fileview.add(command2);
			fileview.add(att);
			JPanel pp = (JPanel) ((JComponent)((JComponent)this.getComponent(2)).getComponent(2)).getComponent(0);
			pp.add(new JPanel());
		}
		else {
			JPanel filename = (JPanel) this.getComponent(this.getComponentCount()-1);
			JPanel buttons = (JPanel) filename.getComponent(filename.getComponentCount()-1);
			this.setControlButtonsAreShown(false);
			filename.add(command);
			filename.add(command2);
			filename.add(att);
			filename.add(buttons);
		}
		this.showOpenDialog(f);
		this.setSize(this.getSize().width, this.getSize().height+60);
	}
	
	/**
	 * Open selected datafile, with specified options, R-command: read.table(...)
	 */
	public void loadFile() {
		if (this.getSelectedFile() != null) {
			JGR.directory = this.getCurrentDirectory().getAbsolutePath()+File.separator;
			String file = this.getSelectedFile().toString();
			
			String useSep;
			if (sepsBox.getSelectedIndex() >= seps.length) useSep = sepsBox.getSelectedItem().toString();
			else useSep = seps[sepsBox.getSelectedIndex()];
			String useQuote;
			if (quoteBox.getSelectedIndex() >= quotes.length) useQuote = quoteBox.getSelectedItem().toString();
			else useQuote = quotes[quoteBox.getSelectedIndex()];
			
			String cmd = dataName.getText().trim().replaceAll("\\s","")+ "<- read.table(\""+file.replace('\\','/')+"\",header="+(header.isSelected()?"T":"F")+",sep=\""+useSep+"\", quote=\""+useQuote+"\")"+(attach.isSelected()?";attach("+dataName.getText().trim().replaceAll("\\s","")+")":"")+"";
			JGR.MAINRCONSOLE.execute(cmd);
		}
	}

	/**
	 * actionPerformed: handle action event: menus.
	 */
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if (cmd == "ApproveSelection") loadFile();
		//else if (cmd == "CancelSelection") dispose();
	}

	/**
	 * itemStateChanged: handle itemStateChanged event, et separator and quote box enabled if "Others..." is choosen.
	 */
	public void itemStateChanged(ItemEvent e) {
		Object source = e.getItemSelectable();
		boolean edit = false;
		if (source == quoteBox) {
			edit = quoteBox.getSelectedIndex() == quoteBox.getItemCount()-1?true:false;
			quoteBox.setEditable(edit);
		}
		else if (source == sepsBox) {
			edit = sepsBox.getSelectedIndex() == sepsBox.getItemCount()-1?true:false;
			sepsBox.setEditable(edit);
		}
	}

	/**
	 * propertyChange: handle propertyChange, used for setting the name where the file should be assigned to.
	 */
	public void propertyChange(PropertyChangeEvent e) {
		File file = this.getSelectedFile();
		if(file!=null && !file.isDirectory()) {
			String name = file.getName().replaceAll("\\..*", "");
			name = name.replaceAll("^[0-9]+|[^a-zA-Z|^0-9|^_]",".");
			dataName.setText(name);
		}
		else {
			dataName.setText(null);
		}
	}
}
