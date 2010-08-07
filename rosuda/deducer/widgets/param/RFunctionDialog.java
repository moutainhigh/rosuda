package org.rosuda.deducer.widgets.param;

import org.rosuda.JGR.layout.AnchorConstraint;
import org.rosuda.JGR.layout.AnchorLayout;
import org.rosuda.deducer.Deducer;
import org.rosuda.deducer.toolkit.HelpButton;
import org.rosuda.deducer.toolkit.OkayCancelPanel;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;


public class RFunctionDialog extends javax.swing.JDialog implements ActionListener {
	private JPanel panel;
	private OkayCancelPanel okayCancel;
	private HelpButton help;
	private ParamWidget view;
	private RFunction initialModel;
	private RFunction model;
	
	private boolean isRun = true;
	
	public RFunctionDialog(JFrame frame,RFunction el) {
		super(frame);
		initGUI();
		setModel(el);
		setRun(true);
	}
	
	public RFunctionDialog(RFunction el) {
		super();
		initGUI();
		setModel(el);
		setRun(true);
	}
	
	private void initGUI() {
		try {
			AnchorLayout thisLayout = new AnchorLayout();
			getContentPane().setLayout(thisLayout);
			{
				help = new HelpButton("");
				getContentPane().add(help, new AnchorConstraint(923, 92, 12, 12, 
						AnchorConstraint.ANCHOR_NONE, AnchorConstraint.ANCHOR_NONE, 
						AnchorConstraint.ANCHOR_ABS, AnchorConstraint.ANCHOR_ABS));
				help.setPreferredSize(new java.awt.Dimension(29, 26));
				help.setVisible(false);
			}
			{
				okayCancel = new OkayCancelPanel(false,false,this);
				getContentPane().add(okayCancel, new AnchorConstraint(923, 21, 0, 521, 
						AnchorConstraint.ANCHOR_NONE, AnchorConstraint.ANCHOR_ABS, 
						AnchorConstraint.ANCHOR_ABS, AnchorConstraint.ANCHOR_NONE));
				okayCancel.setPreferredSize(new java.awt.Dimension(195, 38));
			}
			{
				panel = new JPanel();
				BorderLayout panelLayout = new BorderLayout();
				panel.setLayout(panelLayout);
				getContentPane().add(panel, new AnchorConstraint(1, 994, 44, 1, 
						AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, 
						AnchorConstraint.ANCHOR_ABS, AnchorConstraint.ANCHOR_REL));
				panel.setPreferredSize(new java.awt.Dimension(447, 449));
			}
			this.setSize(450, 515);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void setModel(RFunction el){
		panel.removeAll();
		view = el.getView();
		panel.add(view);
		initialModel = (RFunction) el.clone();
		model = el;
		this.setTitle(el.getName());
	}
	public void setToInitialModel(){
		RFunction newModel = (RFunction) initialModel.clone();
		setModel(newModel);
	}
	
	public void actionPerformed(ActionEvent arg0) {
		String cmd = arg0.getActionCommand();
		if(cmd == "OK"){
			view.updateModel();
			String s = ((RFunction)view.getModel()).checkValid();
			if(s!=null){
				JOptionPane.showMessageDialog(this, s);
			}else{
				initialModel = (RFunction) model.clone();
				this.dispose();
			}
		}else if(cmd == "Cancel"){
			setToInitialModel();
			this.dispose();
		}else if(cmd == "Run"){
			view.updateModel();
			String s = ((RFunction)view.getModel()).checkValid();
			if(s!=null){
				JOptionPane.showMessageDialog(this, s);
			}else{
				initialModel = (RFunction) model.clone();
				this.dispose();
				Deducer.execute(model.getCall());
			}
		}
	}

	private HelpButton getHelpButton() {
		return help;
	}

	public void setRun(boolean isRun) {
		if(isRun)
			okayCancel.getApproveButton().setText("Run");
		else
			okayCancel.getApproveButton().setText("OK");
		this.isRun = isRun;
	}

	public boolean isRun() {
		return isRun;
	}
	
}
