package org.rosuda.deducer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


import org.rosuda.JGR.JGR;
import org.rosuda.deducer.data.DataFrameSelector;
import org.rosuda.deducer.data.DataFrameWindow;
import org.rosuda.deducer.menu.*;
import org.rosuda.JGR.robjects.RObject;
import org.rosuda.JGR.util.ErrorMsg;
import org.rosuda.deducer.toolkit.VariableSelectionDialog;
import org.rosuda.deducer.menu.RecodeDialog;
import org.rosuda.ibase.toolkit.EzMenuSwing;

public class Deducer {
	ConsoleListener cListener =  new ConsoleListener();

	public Deducer(){
		String dataMenu = "Data";
		String analysisMenu = "Analysis";
		try{
			EzMenuSwing.insertMenu(JGR.MAINRCONSOLE,dataMenu,3);
			EzMenuSwing.addJMenuItem(JGR.MAINRCONSOLE, dataMenu, "Edit Factor", "factor", cListener);
			EzMenuSwing.addJMenuItem(JGR.MAINRCONSOLE, dataMenu, "Recode Variables", "recode", cListener);
			EzMenuSwing.addJMenuItem(JGR.MAINRCONSOLE, dataMenu, "Reset Row Names", "reset rows", cListener);
			EzMenuSwing.addMenuSeparator(JGR.MAINRCONSOLE, dataMenu);
			EzMenuSwing.addJMenuItem(JGR.MAINRCONSOLE, dataMenu, "Sort", "sort", cListener);
			EzMenuSwing.addJMenuItem(JGR.MAINRCONSOLE, dataMenu, "Merge Data", "merge", cListener);
			EzMenuSwing.addJMenuItem(JGR.MAINRCONSOLE, dataMenu, "Transpose", "trans", cListener);
			EzMenuSwing.addJMenuItem(JGR.MAINRCONSOLE, dataMenu, "Subset", "subset", cListener);
			
			EzMenuSwing.insertMenu(JGR.MAINRCONSOLE,analysisMenu,4);
			EzMenuSwing.addJMenuItem(JGR.MAINRCONSOLE, analysisMenu, "Frequencies", "frequency", cListener);
			EzMenuSwing.addJMenuItem(JGR.MAINRCONSOLE, analysisMenu, "Descriptives", "descriptives", cListener);
			EzMenuSwing.addJMenuItem(JGR.MAINRCONSOLE, analysisMenu, "Contingency Tables", "contingency", cListener);

			EzMenuSwing.insertJMenuItem(JGR.MAINRCONSOLE, "Environment", "Data Viewer", "table", cListener, 2);
		}catch(Exception e){JGR.MAINRCONSOLE.execute("'"+e.getMessage()+"'");}
	}

	class ConsoleListener implements ActionListener{
		public void actionPerformed(ActionEvent arg0) {
			String cmd = arg0.getActionCommand();
		
			if(cmd == "recode"){
				RecodeDialog recode =new RecodeDialog(JGR.MAINRCONSOLE); 
				recode.setLocationRelativeTo(null);
				recode.setVisible(true);
			}else if(cmd=="factor"){
				VariableSelectionDialog inst =new VariableSelectionDialog(JGR.MAINRCONSOLE);
				inst.SetSingleSelection(true);
				inst.setLocationRelativeTo(null);
				inst.setRFilter("is.factor");
				inst.setTitle("Select Factor to Edit");
				inst.setVisible(true);
				String variable = inst.getSelecteditem();
				if(variable==null)
					return;
				FactorDialog fact = new FactorDialog(JGR.MAINRCONSOLE,variable);
				fact.setLocationRelativeTo(null);
				fact.setVisible(true);
			}else if(cmd == "reset rows"){
				String name = null;
				RObject data = null;
				DataFrameSelector sel = new DataFrameSelector(JGR.MAINRCONSOLE);
				data = sel.getSelection();
				if(data!=null){
					name = data.getName();
					JGR.MAINRCONSOLE.executeLater("rownames("+name+") <-1:dim("+name+")[1]");
					DataFrameWindow.setTopDataWindow(name);
				}
				JGR.MAINRCONSOLE.toFront();
			}else if(cmd=="sort"){
				SortDialog sort = new SortDialog(JGR.MAINRCONSOLE);
				sort.setLocationRelativeTo(null);
				sort.setVisible(true);
			}else if(cmd == "merge"){
				MergeDialog merge =new MergeDialog(JGR.MAINRCONSOLE); 
				merge.setLocationRelativeTo(null);
				merge.setVisible(true);
			}else if (cmd == "trans"){
				String name = null;
				RObject data = null;
				DataFrameSelector sel = new DataFrameSelector(JGR.MAINRCONSOLE);
				data = sel.getSelection();
				if(data!=null){
					name = data.getName();
					JGR.MAINRCONSOLE.executeLater(name+"<-as.data.frame(t("+name+"))");
					DataFrameWindow.setTopDataWindow(name);
					JGR.MAINRCONSOLE.toFront();
				}
			}else if(cmd == "subset"){
				SubsetDialog sub = new SubsetDialog(JGR.MAINRCONSOLE);
				sub.setLocationRelativeTo(null);
				sub.setVisible(true);
				JGR.MAINRCONSOLE.toFront();
			}else if(cmd =="frequency"){
				FrequencyDialog freq = new FrequencyDialog(JGR.MAINRCONSOLE);
				freq.setLocationRelativeTo(null);
				freq.setVisible(true);
			}else if(cmd =="descriptives"){
				DescriptivesDialog desc = new DescriptivesDialog(JGR.MAINRCONSOLE);
				desc.setLocationRelativeTo(null);
				desc.setVisible(true);
			}else if(cmd =="contingency"){
				ContingencyDialog cont = new ContingencyDialog(JGR.MAINRCONSOLE);
				cont.setLocationRelativeTo(null);
				cont.setVisible(true);
			}else if (cmd == "table"){
				DataFrameWindow inst = new DataFrameWindow();
				inst.setLocationRelativeTo(null);
				inst.setVisible(true);
			}
		}
	}
}