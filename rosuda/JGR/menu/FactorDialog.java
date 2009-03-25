package org.rosuda.JGR.menu;


import org.rosuda.JGR.JGR;
import org.rosuda.JGR.RController;
import org.rosuda.JGR.layout.AnchorConstraint;
import org.rosuda.JGR.layout.AnchorLayout;
import org.rosuda.JGR.toolkit.DJList;
import org.rosuda.JGR.toolkit.IconButton;
import org.rosuda.JGR.util.ErrorMsg;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;

public class FactorDialog extends JDialog implements ActionListener {
	private JPanel listPanel;
	private JScrollPane levelScroller;
	private DJList levelList;
	private JCheckBox ordered;
	private JButton cancel;
	private JButton okay;
	private JButton contrast;
	private IconButton add;
	private IconButton remove;
	private IconButton down;
	private IconButton up;
	private ContrastDialog cntr;
	
	private String variable;

	
	public FactorDialog(JFrame frame, String var) {
		super(frame);
		variable = var;
		initGUI();
		
	}
	
	private void initGUI() {
		try {
			AnchorLayout thisLayout = new AnchorLayout();
			getContentPane().setLayout(thisLayout);
			this.setPreferredSize(new java.awt.Dimension(280, 331));
			this.setTitle("Edit Factor");
			{
				cancel = new JButton();
				AnchorLayout cancelLayout = new AnchorLayout();
				cancel.setLayout(cancelLayout);
				getContentPane().add(cancel, new AnchorConstraint(836, 630, 920, 373, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
				cancel.setText("Cancel");
				cancel.setPreferredSize(new java.awt.Dimension(72, 26));
				cancel.addActionListener(this);
			}
			{
				okay = new JButton();
				getContentPane().add(okay, new AnchorConstraint(810, 937, 946, 694, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
				okay.setText("OK");
				okay.setPreferredSize(new java.awt.Dimension(68, 42));
				okay.addActionListener(this);
			}
			{
				ordered = new JCheckBox();
				getContentPane().add(ordered, new AnchorConstraint(690, 769, 752, 123, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_NONE, AnchorConstraint.ANCHOR_NONE, AnchorConstraint.ANCHOR_ABS));
				ordered.setText("Ordered");
				ordered.setPreferredSize(new java.awt.Dimension(92, 19));
				ordered.addActionListener(this);
				ordered.setSelected(JGR.R.eval("is.ordered("+variable+")").asBool().isTRUE());
			}
			{
				contrast = new JButton();
				getContentPane().add(contrast, new AnchorConstraint(674, 373, 771, 12, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_NONE, AnchorConstraint.ANCHOR_NONE, AnchorConstraint.ANCHOR_ABS));
				contrast.setText("Contrasts");
				contrast.setPreferredSize(new java.awt.Dimension(92, 30));
				contrast.addActionListener(this);
			}
			{
				add = new IconButton("/icons/edit_add_32.png","Delete",this,"Add");
				getContentPane().add(add, new AnchorConstraint(110, 937, 412, 769, AnchorConstraint.ANCHOR_ABS, AnchorConstraint.ANCHOR_NONE, AnchorConstraint.ANCHOR_NONE, AnchorConstraint.ANCHOR_REL));
				add.setPreferredSize(new java.awt.Dimension(40, 41));
			}
			{
				remove = new IconButton("/icons/edit_remove_32.png","Delete",this,"Delete");
				getContentPane().add(remove, new AnchorConstraint(152, 937, 655, 769, AnchorConstraint.ANCHOR_ABS, AnchorConstraint.ANCHOR_NONE, AnchorConstraint.ANCHOR_NONE, AnchorConstraint.ANCHOR_REL));
				remove.setPreferredSize(new java.awt.Dimension(40, 41));
			}
			{
				down = new IconButton("/icons/1downarrow_32.png","Down",this,"Down");
				getContentPane().add(down, new AnchorConstraint(70, 937, 412, 769, AnchorConstraint.ANCHOR_ABS, AnchorConstraint.ANCHOR_NONE, AnchorConstraint.ANCHOR_NONE, AnchorConstraint.ANCHOR_REL));
				down.setPreferredSize(new java.awt.Dimension(40, 35));

			}
			{
				up = new IconButton("/icons/1uparrow_32.png","Up",this,"Up");
				getContentPane().add(up, new AnchorConstraint(35, 937, 260, 769, AnchorConstraint.ANCHOR_ABS, AnchorConstraint.ANCHOR_NONE, AnchorConstraint.ANCHOR_NONE, AnchorConstraint.ANCHOR_REL));
				up.setPreferredSize(new java.awt.Dimension(40, 35));
			}
			{
				listPanel = new JPanel();
				getContentPane().add(listPanel, new AnchorConstraint(40, 748, 655, 12, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_ABS));
				BorderLayout listPanelLayout = new BorderLayout();
				listPanel.setLayout(listPanelLayout);
				listPanel.setBorder(BorderFactory.createTitledBorder("Levels"));
				listPanel.setPreferredSize(new java.awt.Dimension(197, 190));
				{
					levelScroller = new JScrollPane();
					listPanel.add(levelScroller, BorderLayout.CENTER);
					{
						DefaultListModel levelListModel = new DefaultListModel();
						String[] levels = JGR.R.eval("levels("+variable+")").asStringArray();
						for(int i=0;i<levels.length;i++)
							levelListModel.addElement(levels[i]);
						levelList = new DJList();
						levelScroller.setViewportView(levelList);
						levelList.setModel(levelListModel);
					}
				}
			}
			this.setSize(280, 331);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void actionPerformed(ActionEvent arg0) {
		String cmd = arg0.getActionCommand();
		if(cmd=="Up"){
			int[] ind= levelList.getSelectedIndices();
			if(ind.length>1){
				levelList.setSelectedIndex(ind[0]);
			}else if(ind.length == 1 && ind[0]>0){
				DefaultListModel model = (DefaultListModel)levelList.getModel();
				Object obj =model.remove(ind[0]);
				model.add(ind[0]-1, obj);
				levelList.setSelectedIndex(ind[0]-1);
			}
		}else if(cmd=="Down"){
			int[] ind= levelList.getSelectedIndices();
			if(ind.length>1){
				levelList.setSelectedIndex(ind[0]);
			}else if(ind.length == 1 && ind[0]<(levelList.getModel().getSize()-1)){
				DefaultListModel model = (DefaultListModel)levelList.getModel();
				Object obj =model.remove(ind[0]);
				model.add(ind[0]+1, obj);
				levelList.setSelectedIndex(ind[0]+1);
			}
		}else if(cmd=="Add"){
			String result =JOptionPane.showInputDialog(this, "Please Enter the new factor Level", "New Factor Level", JOptionPane.INFORMATION_MESSAGE);
			if(result!=null && result.length()>0)
				((DefaultListModel)levelList.getModel()).add(0,result);
		}else if(cmd == "Delete"){
			int[] ind= levelList.getSelectedIndices();
			if(ind.length>0){
				DefaultListModel model = (DefaultListModel)levelList.getModel();
				for(int i=ind.length-1;i>=0;i--){
					model.remove(ind[i]);
				}
			}
		}else if(cmd == "Cancel"){
			this.dispose();
		}else if(cmd == "OK"){
			ArrayList newLevels = new ArrayList();
			DefaultListModel model = (DefaultListModel)levelList.getModel();
			for(int i=0;i<model.getSize();i++){
				newLevels.add(model.get(i));
			}
			String rLevels = RController.makeRStringVector(newLevels);
			String order;
			if(ordered.isSelected())
				order="TRUE";	
			else
				order="FALSE";	
			JGR.MAINRCONSOLE.executeLater(variable+"<-factor("+variable+
					",levels="+rLevels+",ordered="+order+")");
			if(cntr!=null)
				cntr.executeSelection(variable);
			this.dispose();
		}else if(cmd =="Contrasts"){
			cntr = new ContrastDialog(null);
			cntr.setLocation(contrast.getLocationOnScreen());
			cntr.setTitle("Set Contrast Codes");
			cntr.setVisible(true);
			
		}
		
	}

	
	
	
	public class ContrastDialog extends javax.swing.JDialog implements ActionListener {
		private ButtonGroup buttonGroup1;
		private JPanel groupPanel;
		private JCheckBox helmert;
		private JButton okay;
		private JButton cancel;
		private JCheckBox custom;
		private JCheckBox polynomial;
		private JCheckBox sum;
		private JCheckBox treatment;
		private String contrast=null;


		
		public ContrastDialog(JFrame frame) {
			super(frame,true);
			initGUI();
		}
		
		private void initGUI() {
			try {
				AnchorLayout thisLayout = new AnchorLayout();
				getContentPane().setLayout(thisLayout);
				{
					okay = new JButton();
					getContentPane().add(okay, new AnchorConstraint(833, 944, 957, 566, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
					okay.setText("OK");
					okay.setPreferredSize(new java.awt.Dimension(79, 31));
				}
				{
					cancel = new JButton();
					getContentPane().add(cancel, new AnchorConstraint(853, 471, 937, 59, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
					cancel.setText("Cancel");
					cancel.setPreferredSize(new java.awt.Dimension(86, 21));
				}
				{
					groupPanel = new JPanel();
					AnchorLayout groupPanelLayout = new AnchorLayout();
					getContentPane().add(groupPanel, new AnchorConstraint(50, 944, 785, 59, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
					groupPanel.setPreferredSize(new java.awt.Dimension(185, 183));
					groupPanel.setLayout(groupPanelLayout);
					groupPanel.setBorder(BorderFactory.createTitledBorder("Contrast Type"));
					{
						custom = new JCheckBox();
						groupPanel.add(custom, new AnchorConstraint(751, 916, 849, 218, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
						custom.setText("Custom");
						custom.setPreferredSize(new java.awt.Dimension(129, 18));
					}
					{
						polynomial = new JCheckBox();
						groupPanel.add(polynomial, new AnchorConstraint(614, 916, 713, 218, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
						polynomial.setText("Polynomial");
						polynomial.setPreferredSize(new java.awt.Dimension(129, 18));
					}
					{
						helmert = new JCheckBox();
						groupPanel.add(helmert, new AnchorConstraint(483, 916, 576, 218, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
						helmert.setText("Helment");
						helmert.setPreferredSize(new java.awt.Dimension(129, 17));
					}
					{
						sum = new JCheckBox();
						groupPanel.add(sum, new AnchorConstraint(346, 916, 445, 218, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
						sum.setText("Sum (Deviation)");
						sum.setPreferredSize(new java.awt.Dimension(129, 18));
					}
					{
						treatment = new JCheckBox();
						groupPanel.add(treatment, new AnchorConstraint(210, 916, 308, 218, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
						treatment.setText("Treatment");
						treatment.setPreferredSize(new java.awt.Dimension(129, 18));
					}
				}
				buttonGroup1 = new ButtonGroup();
				buttonGroup1.add(treatment);
				buttonGroup1.add(helmert);
				buttonGroup1.add(sum);
				buttonGroup1.add(polynomial);
				buttonGroup1.add(custom);
				treatment.addActionListener(this);
				helmert.addActionListener(this);
				sum.addActionListener(this);
				polynomial.addActionListener(this);
				custom.addActionListener(this);
				okay.addActionListener(this);
				cancel.addActionListener(this);
				//TODO: add custom logic
				custom.setVisible(false);
				this.setSize(217, 283);
			} catch (Exception e) {
				new ErrorMsg(e);
			}
		}
		
		private ButtonGroup getButtonGroup1() {
			if(buttonGroup1 == null) {
				buttonGroup1 = new ButtonGroup();
			}
			return buttonGroup1;
		}


		public void actionPerformed(ActionEvent act) {
			String cmd = act.getActionCommand();
			System.out.println(cmd);
			if(cmd=="Treatment"){
				contrast = "\"contr.treatment\"";
			}else if(cmd=="Sum (Deviation)"){
				contrast = "\"contr.sum\"";
			}else if(cmd=="Helmert"){
				contrast = "\"contr.helmert\"";
			}else if(cmd=="Polynomial"){
				contrast = "\"contr.poly\"";
			}else if(cmd=="custom")
				contrast = "Error: undefined contrast";
			else if(cmd=="Cancel"){
				contrast=null;
				this.dispose();
			}else if(cmd=="OK")
				this.dispose();
		}
		
		public void executeSelection(String variable){
			if(contrast!=null)
				JGR.MAINRCONSOLE.executeLater("contrasts("+variable+") <-"+contrast);
		}

	}
}
