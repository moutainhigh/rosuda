
package org.rosuda.deducer.toolkit;

import org.rosuda.JGR.layout.AnchorConstraint;
import org.rosuda.JGR.layout.AnchorLayout;

import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.ListModel;

import org.rosuda.JGR.*;
import org.rosuda.JGR.robjects.*;
import org.rosuda.JGR.util.*;
import org.rosuda.JRI.REXP;


public class VariableSelector extends JPanel implements ActionListener, KeyListener {
	private JComboBox dataComboBox;
	private DefaultComboBoxModel dataComboBoxModel;
	private DJList variableList;
	private JTextField filter;
	private JLabel filterText;
	private String rFilter = "";
	private String splitStr = null;
	
	
	public VariableSelector() {
		super();
		initGUI();
		RController.refreshObjects();
		dataComboBoxModel.removeAllElements();
		for(int i=0;i<JGR.DATA.size();i++){
			dataComboBoxModel.addElement(((RObject) JGR.DATA.elementAt(i)).getName());
		}

		String dataName = (String)dataComboBox.getSelectedItem();
		if(dataName!=null)
			variableList.setModel(new FilteringModel(JGR.R.eval("names("+dataName+")").asStringArray()));

	}
	
	private void initGUI() {
		try {
			this.setName("Variable Selector");
			AnchorLayout thisLayout = new AnchorLayout();
			this.setLayout(thisLayout);
			this.setPreferredSize(new java.awt.Dimension(194, 294));
			this.setBorder(BorderFactory.createEtchedBorder(BevelBorder.LOWERED));
			dataComboBoxModel = 
				new DefaultComboBoxModel();
			dataComboBox = new JComboBox();
			dataComboBox.setModel(dataComboBoxModel);
			dataComboBox.addActionListener(this);
			
			filterText = new JLabel();
			filterText.setText("  Filter:");
			
			
			filter = new JTextField();
			this.add(filter, new AnchorConstraint(25, 970, 147, 62, 
					AnchorConstraint.ANCHOR_ABS, AnchorConstraint.ANCHOR_REL, 
					AnchorConstraint.ANCHOR_NONE, AnchorConstraint.ANCHOR_ABS));
			this.add(filterText, new AnchorConstraint(30, 260, 144, 30, 
					AnchorConstraint.ANCHOR_ABS, AnchorConstraint.ANCHOR_NONE, 
					AnchorConstraint.ANCHOR_NONE, AnchorConstraint.ANCHOR_REL));
			this.add(dataComboBox, new AnchorConstraint(1, 999, 79, 2,  
					AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, 
					AnchorConstraint.ANCHOR_NONE, AnchorConstraint.ANCHOR_REL));
			dataComboBox.setPreferredSize(new java.awt.Dimension(164, 22));
			filterText.setPreferredSize(new java.awt.Dimension(55, 15));
			filter.setPreferredSize(new java.awt.Dimension(103, 21));
			filter.addKeyListener(this);

			ListModel variableListModel = new FilteringModel(new String[] {"Oh Snaps!","No Data." });
			variableList = new VarDJList();
			variableList.setModel(variableListModel);
			JScrollPane listScroller = new JScrollPane(variableList,
                    ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			this.add(listScroller, new AnchorConstraint(50, 999, 1001, 3, 
					AnchorConstraint.ANCHOR_ABS, AnchorConstraint.ANCHOR_REL, 
					AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
			listScroller.setPreferredSize(new java.awt.Dimension(164, 224));

		} catch (Exception e) {
			new ErrorMsg(e);
		}
	}
	


	public void add(Object variable){
		String var = (String)variable;
		if(splitStr!=null && var.indexOf(splitStr)>0)
			var = var.substring(0,var.indexOf(splitStr));
		((FilteringModel) variableList.getModel()).addElement(var);
	}
	
	public boolean remove(Object variable){
		String var = (String)variable;
		if(splitStr!=null && var.indexOf(splitStr)>0)
			var = var.substring(0,var.indexOf(splitStr));
		return ((FilteringModel) variableList.getModel()).removeElement(var);
	}
	
	public boolean removeAll(DefaultListModel model){
		if(model==null)
			return false;
		String temp;
		if(model.getSize()==0)
			return true;
		boolean exists=false;
		for(int i=0;i<model.getSize();i++){
			temp = (String) model.get(i);
			exists=this.remove(temp);
			if(!exists){
				this.getJList().setModel(new FilteringModel(
						JGR.R.eval("names("+this.getJComboBox().getSelectedItem()
								+")").asStringArray()));
				break;
			}
		}
		return exists;
	}
	
	public JList getJList(){
		return variableList;
	}
	public JComboBox getJComboBox(){
		return dataComboBox;
	}
	public void setSelectedData(String dataName){
		dataComboBox.setSelectedItem(dataName);
	}
	public String getSelectedData(){
		return (String)dataComboBox.getSelectedItem();
	}
	public ArrayList getSelectedVariables(){
		Object[] vars = variableList.getSelectedValues();
		ArrayList lis = new ArrayList();
		for(int i=0;i<vars.length;i++)
			lis.add(vars[i]);
		return lis;
	}
	/**
	 * Filter the variables using an R function
	 * 
	 * @param function
	 * 			the name of the R function to use in filtering.
	 * 			The function should take one argument, and return true if
	 * 			the variable should be included in the list. 
	 * 			ex: "is.factor" will list only factor variables.
	 * 			Set function = "" to remove the filter
	 * 
	 */
	public void setRFilter(String function){
		rFilter = function;
		((FilteringModel)variableList.getModel()).filter(filter.getText());
	}
	
	public void reset(){
		String dataName = (String)dataComboBox.getSelectedItem();
		variableList.setModel(new FilteringModel(JGR.R.eval("names("+dataName+")").asStringArray()));
		filter.setText("");
		((FilteringModel)variableList.getModel()).filter(filter.getText());		
	}
	
	/**
	 * 
	 * Any string dropped to A VariableSelector will truncate all 
	 * elements up to the string str
	 * 
	 * @param str
	 */
	public void setDropStringSplitter(String str){
		splitStr=str;
	}
	
	public void actionPerformed(ActionEvent arg0) {
		String cmd = arg0.getActionCommand();
		//JGR.R.eval("print('"+arg0.toString()+"')");
		if(cmd=="comboBoxChanged"){
			reset();
		}
		
	}

	public void keyTyped(KeyEvent arg0) {
		Runnable doWorkRunnable = new Runnable() {
		    public void run() { ((FilteringModel) variableList.getModel()).filter(filter.getText()); }
		};
		SwingUtilities.invokeLater(doWorkRunnable);
	}
	public class FilteringModel extends DefaultListModel{
		List list;
		List filteredList;
		String lastFilter = "";

		public FilteringModel() {
			list = new ArrayList();
			filteredList = new ArrayList();
		}
		public FilteringModel(String[] items){
			list = new ArrayList();
			filteredList = new ArrayList();
			for(int i=0;i<items.length;i++){
				list.add(items[i]);
				filteredList.add(items[i]);
			}
		}
		public void add(int index,Object element) {
			int ind;
			if(index>filteredList.size()-1)
				ind=list.size();
			else
				ind =list.indexOf(filteredList.get(index));
			if(ind<0)
				ind=0;
			list.add(ind,element);
			filter(lastFilter);
		}
		public void addElement(Object element) {
			list.add(element);
			filter(lastFilter);
		}
		public boolean removeElement(Object element) {
			boolean is =list.remove(element);
			filter(lastFilter);
			return is;
		}
		public Object remove(int index) {
			int objectCount=0;
			for(int i=0;i<index;i++)
				if(filteredList.get(index).equals(filteredList.get(i)))
					objectCount++;
			Object obj =filteredList.remove(index);			
			if(objectCount==0){
				list.remove(obj);
				filter(lastFilter);
				return obj;
			}
			int listCount = 0;
			for(int i=0;i<list.size();i++)
				if(list.get(i).equals(obj)){
					if(listCount==objectCount)
						list.remove(i);
					listCount++;					
				}
			filter(lastFilter);
			return obj;
		}

		public int getSize() {
			return filteredList.size();
		}
		public int getUnfilteredSize() {
			return list.size();
		}

		public Object getElementAt(int index) {
			Object returnValue;
			if (index < filteredList.size()) {
				returnValue = filteredList.get(index);
			} else {
				returnValue = null;
			}
			return returnValue;
		}
		public Object getUnfilteredElementAt(int index) {
			Object returnValue = list.get(index);
			filter(lastFilter);
			return returnValue;
		}
		public Object removeUnfilteredElementAt(int index) {
			Object returnValue = list.remove(index);
			filter(lastFilter);
			return returnValue;
		}
		public void addUnfilteredElementAt(int index,Object obj) {
			list.add(index, obj);
			filter(lastFilter);
		}

		void filter(String search) {
			filteredList.clear();
			Object element;
			for (int i=0;i<list.size();i++) {
				element = list.get(i);
				if (element.toString().toLowerCase().indexOf(search.toLowerCase(), 0) != -1) {
					if(rFilter!=""){
						if(JGR.R.eval(rFilter+"("+(String)dataComboBox.getSelectedItem()+
								"$"+(String)element+")").asBool().isTRUE()){
							filteredList.add(element);
						}
					}else
						filteredList.add(element);
					
				}
			}
			lastFilter=search;
			fireContentsChanged(this, 0, getSize());
		}
	}
	
	public void keyPressed(KeyEvent e) {}
	public void keyReleased(KeyEvent e) {}
	
	private class VarDJList extends DJList{
		public void drop(DropTargetDropEvent dtde) {
			String dataName = (String)dataComboBox.getSelectedItem();
			REXP rNames = JGR.R.eval("names("+dataName+")");
			if(rNames==null){
				dtde.rejectDrop();
				return;
			}
			String[] names = rNames.asStringArray();
			super.drop(dtde);
			FilteringModel model =(FilteringModel) this.getModel();
			int len = model.getUnfilteredSize();
			String temporary;
			String nameInData=null;
			boolean exists;
			for(int i=0;i<len;i++){
				temporary =(String) model.getUnfilteredElementAt(i);
				if(splitStr!=null && temporary.indexOf(splitStr)>0)
					temporary = temporary.substring(0,temporary.indexOf(splitStr));
				exists=false;
				for(int j=0;j<names.length;j++){
					if(temporary.equals(names[j])){
							exists=true;
							nameInData=names[j];
					}	
				}
				if(exists){
					model.removeUnfilteredElementAt(i);
					model.addUnfilteredElementAt(i, nameInData);
					nameInData=null;
				}else{
					((FilteringModel)this.getModel()).removeUnfilteredElementAt(i);
				}

			}
		}
		
	}
}
