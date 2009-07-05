package org.rosuda.deducer.models;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JPanel;

import org.rosuda.JGR.toolkit.JavaGD;
import org.rosuda.JGR.util.ErrorMsg;
import org.rosuda.deducer.Deducer;
import org.rosuda.javaGD.JGDBufferedPanel;

public class GLMExplorer extends ModelExplorer implements WindowListener{
	
	protected GLMModel model = new GLMModel();
	protected RModel pre;
	protected ModelPlotPanel diagnosticTab;
	protected ModelPlotPanel termTab;
	protected ModelPlotPanel addedTab;
	
	GLMExplorer(GLMModel mod){
		super();
		setModel(mod);
		initTabs();
		this.addWindowListener(this);
	}
	
	public void initTabs(){
		try{
			String call="par(mfrow = c(2, 3),mar=c(5,4,2,2))\n"+
				"hist(resid("+pre.modelName+"),main=\"Residual\",xlab=\"Residuals\")\n"+
				"plot("+pre.modelName+",2,sub.caption=\"\")\n"+
				"plot("+pre.modelName+", c(1,4,3,5),sub.caption=\"\")";
			diagnosticTab = new ModelPlotPanel(call);
			tabs.addTab("Diagnostics", diagnosticTab);
			
			call="par(mar=c(5,4,2,2))\n"+
				"try(cr.plots("+pre.modelName+",one.page=T,ask=F,identify.points=F,col=1),silent=TRUE)";
			termTab = new ModelPlotPanel(call);
			if(Deducer.rniEval("length(grep(\":\",c(attr(terms("+pre.modelName+"),\"term.labels\"))))==0").asBool().isTRUE())
				tabs.addTab("Terms", termTab);
			
			call="par(mar=c(5,4,2,2))\n"+
			"try(av.plots("+pre.modelName+",one.page=T,ask=F,identify.points=F,col=1),silent=TRUE)";
			addedTab = new ModelPlotPanel(call);
			tabs.addTab("Added Variable", addedTab);
		}catch(Exception e){
			new ErrorMsg(e);
		}
		
	}
	
	public void setModel(GLMModel mod){
		model = mod;
		pre =model.run(true,pre);
		modelFormula.setText(pre.formula);
		preview.setText(pre.preview);
		preview.setCaretPosition(0);
	}
	
	
	public void run(){
		model.run(false,null);
		this.dispose();
		GLMDialog.setLastModel(model);
		Deducer.rniEval("rm('"+pre.data.split("$")[1]+"','"+pre.modelName.split("$")[1]+"',envir="+Deducer.guiEnv+")");
	}
	
	public void updateClicked(){
		GLMBuilder bld = new GLMBuilder(model);
		bld.setLocationRelativeTo(this);
		this.dispose();
		bld.setVisible(true);
	}
	
	public void optionsClicked(){
		GLMExplorerOptions opt = new GLMExplorerOptions(this,model);
		opt.setLocationRelativeTo(this);
		opt.setVisible(true);
		setModel(model);
	}
	
	public void postHocClicked(){
		GLMExplorerPostHoc post = new GLMExplorerPostHoc(this,model,pre);
		post.setLocationRelativeTo(this);
		post.setVisible(true);
		setModel(model);
	}
	public void exportClicked(){
		GLMExplorerExport exp = new GLMExplorerExport(this,model);
		exp.setLocationRelativeTo(this);
		exp.setVisible(true);
	}

	public void windowActivated(WindowEvent arg0) {}

	public void windowClosed(WindowEvent arg0) {
		if(diagnosticTab!=null)
			diagnosticTab.executeDevOff();
		if(termTab!=null)
			termTab.executeDevOff();
		if(addedTab!=null)
			addedTab.executeDevOff();
	}

	public void windowClosing(WindowEvent arg0) {}

	public void windowDeactivated(WindowEvent arg0) {}

	public void windowDeiconified(WindowEvent arg0) {}

	public void windowIconified(WindowEvent arg0) {}

	public void windowOpened(WindowEvent arg0) {}
}