package org.rosuda.JGR.browser;

import java.awt.Component;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Enumeration;

import javax.swing.Icon;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeSelectionModel;



public class BrowserTree extends JTree {

	HeadNode head;
	DefaultTreeModel mod;
	Refresher ref;
	public BrowserTree(){
		super();
		setModel(mod=new DefaultTreeModel(head=new HeadNode()));
		//this.setEditable(true);
		this.getSelectionModel().setSelectionMode
        (TreeSelectionModel.SINGLE_TREE_SELECTION);
		this.putClientProperty("JTree.lineStyle", "None");
		this.setCellRenderer(new BrowserCellRenderer());
		new Thread(new Runnable(){
			public void run() {
				head.update(mod);
			}
			
		}).start();
		
		this.repaint();
		ExpandListener lis = new ExpandListener();
		this.addTreeWillExpandListener(lis);
		this.addTreeExpansionListener(lis);
		TreeMouseListener mlis = new TreeMouseListener();
		this.addMouseListener(mlis);
		//this.setRootVisible(false);
		this.setToggleClickCount(1000);
	}
	
	public void startRefresher(){
		if(ref!=null)
			return;
		ref = new Refresher(mod);
		new Thread(ref).start();
	}
	
	public void stopRefresher(){
		if(ref!=null){
			ref.stopRunning();
			ref=null;
		}
	}
	
	
	class BrowserCellRenderer implements TreeCellRenderer{

		public Component getTreeCellRendererComponent(JTree tree, Object value,
				boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
			BrowserNode node = (BrowserNode) value;
			return node.getRenderer().getTreeCellRendererComponent(
					tree,value,selected,expanded,leaf,row,hasFocus);
		}
		
	}

	
	class ExpandListener implements TreeWillExpandListener, TreeExpansionListener{

		public void treeWillCollapse(TreeExpansionEvent event)
				throws ExpandVetoException {
		}

		public void treeWillExpand(TreeExpansionEvent event)
				throws ExpandVetoException {
			final BrowserNode node = (BrowserNode) event.getPath().getLastPathComponent();
			new Thread(new Runnable(){
				public void run() {
					node.setExpanded(true);
					node.update(mod);
					SwingUtilities.invokeLater(new Runnable(){
						public void run() {
							mod.reload(node);
						}
						
					});
					
				}
				
			}).start();

		}

		public void treeCollapsed(TreeExpansionEvent event) {
			//System.out.println("collapsing");
			final BrowserNode node = (BrowserNode) event.getPath().getLastPathComponent();
			new Thread(new Runnable(){
				public void run() {
					node.setExpanded(false);
					node.update(mod);
					SwingUtilities.invokeLater(new Runnable(){
						public void run() {
							mod.reload(node);
						}
						
					});
					
				}
				
			}).start();
		}

		public void treeExpanded(TreeExpansionEvent event) {}
		
	}
	
	class TreeMouseListener implements MouseListener{

		public void mouseClicked(MouseEvent e) {
		    if(e.getClickCount() == 2){
		        int row = BrowserTree.this.getClosestRowForLocation(e.getX(), e.getY());
		        BrowserTree.this.setSelectionRow(row);
		        final BrowserNode node = ((BrowserNode)BrowserTree.this.getSelectionPath().getLastPathComponent());
		        new Thread(new Runnable(){
					public void run() {
						node.editObject();
					}
		        }).start();
		        
		    }
		}

		public void mouseEntered(MouseEvent arg0) {}

		public void mouseExited(MouseEvent arg0) {}

		public void mousePressed(MouseEvent e) {
			if (e.isPopupTrigger()) {
		    	pop(e);
		    }
		}

		public void mouseReleased(MouseEvent e) {
			if (e.isPopupTrigger()) {
		    	pop(e);
		    }
		}
		
		private void pop(MouseEvent e){
			 int row = BrowserTree.this.getClosestRowForLocation(e.getX(), e.getY());
		       BrowserTree.this.setSelectionRow(row);
		       JPopupMenu popupMenu= ((BrowserNode)BrowserTree.this.getSelectionPath().getLastPathComponent()).getPopupMenu();
  		        if(popupMenu!=null)
		        	popupMenu.show(e.getComponent(), e.getX(), e.getY());			
		}
		
	}
	
}


class Refresher implements Runnable{
	public boolean keepRunning = true;
	public volatile boolean isUpdating = false;
	DefaultTreeModel model;
	public Refresher(DefaultTreeModel mod){
		model=mod;
	}
	
	public void stopRunning(){
		keepRunning = false;
	}
	
	public void run(){
		while(keepRunning){
			//System.out.println("refresh");
			try {	
				Thread.sleep(4000);
			} catch (InterruptedException e) {
				keepRunning=false;
			}
			if(!isUpdating){
				isUpdating = true;
				((BrowserNode)model.getRoot()).update(model);
				isUpdating=false;
				/*SwingUtilities.invokeLater(new Runnable(){
	
					public void run() {
						((BrowserNode)model.getRoot()).update(model);
						isUpdating=false;
					}
				});*/
			}
		}
	}
	

}


