package org.rosuda.deducer.plots;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import org.rosuda.JGR.layout.AnchorConstraint;
import org.rosuda.JGR.layout.AnchorLayout;
import org.rosuda.REngine.REXP;
import org.rosuda.deducer.Deducer;
import org.rosuda.deducer.WindowTracker;
import org.rosuda.deducer.toolkit.HelpButton;
import org.rosuda.deducer.toolkit.IconButton;
import org.rosuda.deducer.toolkit.OkayCancelPanel;
import org.rosuda.ibase.toolkit.EzMenuSwing;
import org.rosuda.ibase.toolkit.TJFrame;
import org.rosuda.javaGD.JGDBufferedPanel;

import java.awt.BorderLayout;
import java.io.File;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JList;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;

import javax.swing.WindowConstants;
import javax.swing.border.BevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;


public class PlotBuilder extends TJFrame implements ActionListener, WindowListener{
	private JLayeredPane pane;
	private JPanel bottomPanel;
	private JPanel rightPanel;
	private JPanel topPanel;
	private JPanel defaultPlotPanel;
	private OkayCancelPanel okayCancel;
	private JTabbedPane addTabs;
	private JTabbedPane templateTabs;
	private JButton removeButton;
	private JButton disableButton;
	private JButton editButton;
	private JPanel addPanel;
	private JList elementsList;
	private JScrollPane elementsScroller;
	private JPanel elementsPanel;
	private HelpButton helpButton;
	private PlotBuilderSubFrame layerSheet;
	private JPanel shadow;
	private JPanel background;
	

	private PlotPanel device;
	private JPanel plotHolder;
	
	private MenuListener menuListener = new MenuListener();
	
	private Vector addElementTabNames;
	private HashMap addElementListModels;
	
	private PlotBuilderModel model;
	private PlotBuilderModel initialModel;
	private static PlotBuilderModel lastModel;
	
	
	private static Vector elementPopupMenuItems;

	
	public PlotBuilder() {
		this(lastModel==null ? new PlotBuilderModel() : (PlotBuilderModel)lastModel.clone());
	}
	
	public PlotBuilder(PlotBuilderModel pbm) {
		super();
		try{
			PlotController.init();
			init();
			initGUI();
			setModel(pbm);
			initialModel = (PlotBuilderModel) pbm.clone();
			updatePlot();
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	
	private void init(){
		addElementTabNames = new Vector();
		addElementListModels = new HashMap();
		String[] names = PlotController.getNames();
		for(int i=0;i<names.length;i++)
			addElementTabNames.add(names[i]);
		
		DefaultListModel lm;
		Object[] items;
		lm = new DefaultListModel();
		items = PlotController.getGeoms().values().toArray();
		for(int i=0;i<items.length;i++)
			lm.addElement( items[i]);
		addElementListModels.put(names[1], lm);
		
		lm = new DefaultListModel();
		items = PlotController.getStats().values().toArray();
		for(int i=0;i<items.length;i++)
			lm.addElement( items[i]);
		addElementListModels.put(names[2], lm);
		
		lm = new DefaultListModel();
		items = PlotController.getScales().values().toArray();
		for(int i=0;i<items.length;i++)
			lm.addElement( items[i]);
		addElementListModels.put(names[3], lm);
		
		lm = new DefaultListModel();
		items = PlotController.getFacets().values().toArray();
		for(int i=0;i<items.length;i++)
			lm.addElement( items[i]);
		addElementListModels.put(names[4], lm);
		
		lm = new DefaultListModel();
		items = PlotController.getCoords().values().toArray();
		for(int i=0;i<items.length;i++)
			lm.addElement( items[i]);
		addElementListModels.put(names[5], lm);
		
		
		lm = new DefaultListModel();
		items = PlotController.getThemes().values().toArray();
		for(int i=0;i<items.length;i++)
			lm.addElement( items[i]);
		addElementListModels.put(names[6], lm);
		
	}

	
	private void initGUI() {
		try {
			pane = new JLayeredPane();
			Toolkit.getDefaultToolkit().setDynamicLayout(true);
			AnchorLayout thisLayout = new AnchorLayout();
			pane.setLayout(thisLayout);
			setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			{
				MouseListener ml = new TopPanelMouseListener();
				topPanel = new JPanel();
				AnchorLayout topPanelLayout = new AnchorLayout();
				topPanel.setLayout(topPanelLayout);
				pane.add(topPanel, new AnchorConstraint(1, 0, 164, 0, 
						AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_ABS, 
						AnchorConstraint.ANCHOR_NONE, AnchorConstraint.ANCHOR_REL));
				pane.setLayer(topPanel, 10);
				topPanel.setPreferredSize(new java.awt.Dimension(683, 137));
				topPanel.addMouseListener(ml);
				shadow = new JPanel();
				
				pane.add(shadow, new AnchorConstraint(1, 1, 164, 1, 
						AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_ABS, 
						AnchorConstraint.ANCHOR_NONE, AnchorConstraint.ANCHOR_REL));
				pane.setLayer(shadow, 9);
				shadow.setPreferredSize(new java.awt.Dimension(683, 283));
				shadow.setBackground(new Color(105,105,105));
				shadow.setVisible(false);
				
				background = new JPanel();
				pane.add(background, new AnchorConstraint(0, 0, 0, 0, 
						AnchorConstraint.ANCHOR_ABS, AnchorConstraint.ANCHOR_ABS, 
						AnchorConstraint.ANCHOR_ABS, AnchorConstraint.ANCHOR_ABS));	
				pane.setLayer(background, -1000);
				
				
				{
					addPanel = new JPanel();
					BorderLayout addPanelLayout = new BorderLayout();
					addPanel.setLayout(addPanelLayout);
					topPanel.add(addPanel, new AnchorConstraint(11, 5, 996, 5, 
							AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_ABS, 
							AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_ABS));
					addPanel.setPreferredSize(new java.awt.Dimension(659, 124));
					addPanel.addMouseListener(ml);
					{
						addTabs = new JTabbedPane();
						addPanel.add(addTabs, BorderLayout.CENTER);
						addTabs.setPreferredSize(new java.awt.Dimension(659, 130));
						addTabs.addMouseListener(ml);
						addTabs.addChangeListener(new ChangeListener(){

							public void stateChanged(ChangeEvent arg0) {
								int ind = addTabs.getSelectedIndex();
								if(ind>=4)
									retractTopPanel();
							}
							
						});
						{
							templateTabs = new JTabbedPane();
							templateTabs.setTabPlacement(JTabbedPane.LEFT);
							templateTabs.addMouseListener(ml);
							addTabs.add("   Templates   ", templateTabs);
							String[] titles = {"All","1-D","2-D","Other"};
							for(int i=0;i<3;i++){
								JList list = new JList();
								DefaultListModel mod = new DefaultListModel();
								if(i==0 |i==2)
									mod.addElement(PlottingElement.createElement("template","Scatter"));
								list.addMouseListener(ml);
								list.setCellRenderer(new ElementListRenderer());
								list.setVisibleRowCount(1);
								list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
								list.setModel(mod);
								list.setDragEnabled(true);
								list.setTransferHandler(new AddElementTransferHandler());
								list.addMouseListener(new AddMouseListener());								
								templateTabs.add(titles[i], list);
							}
							
							for(int i=1;i<addElementTabNames.size();i++){
								String name = addElementTabNames.get(i).toString();
								DefaultListModel mod =(DefaultListModel) addElementListModels.get(name);
								
								JPanel panel = new JPanel();
								panel.addMouseListener(ml);
								panel.setLayout(new BorderLayout());
								addTabs.add(name, panel);
								JScrollPane scroller = new JScrollPane();
								scroller.getHorizontalScrollBar().addMouseListener(ml);
								scroller.getVerticalScrollBar().addMouseListener(ml);
								scroller.setHorizontalScrollBarPolicy(
										ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
								panel.add(scroller);
								scroller.addMouseListener(ml);
								JList list = new JList();
								list.addMouseListener(ml);
								list.setCellRenderer(new ElementListRenderer());
								list.setVisibleRowCount(mod.getSize()/7);
								list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
								list.setModel(mod);
								list.setDragEnabled(true);
								list.setTransferHandler(new AddElementTransferHandler());
								list.addMouseListener(new AddMouseListener());
								
								scroller.setViewportView(list);
								
							}
						}
					}
				}
			}
			{
				plotHolder = new JPanel();
				plotHolder.setLayout(new BorderLayout());
				plotHolder.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
				defaultPlotPanel = new DefaultPlotPanel();
				plotHolder.add(defaultPlotPanel);
				pane.add(plotHolder, new AnchorConstraint(137, 158, 52, 22, 
						AnchorConstraint.ANCHOR_ABS, AnchorConstraint.ANCHOR_ABS, 
						AnchorConstraint.ANCHOR_ABS, AnchorConstraint.ANCHOR_REL),2);
				plotHolder.setPreferredSize(new java.awt.Dimension(515, 391));
				plotHolder.setTransferHandler(new PanelTransferHandler());
			}

			{
				rightPanel = new JPanel();
				AnchorLayout rightPanelLayout = new AnchorLayout();
				rightPanel.setLayout(rightPanelLayout);
				pane.add(rightPanel, new AnchorConstraint(135, 1000, 52, 731, 
						AnchorConstraint.ANCHOR_ABS, AnchorConstraint.ANCHOR_REL, 
						AnchorConstraint.ANCHOR_ABS, AnchorConstraint.ANCHOR_NONE),4);
				rightPanel.setPreferredSize(new java.awt.Dimension(160, 389));
				{
					removeButton = new IconButton("/icons/stop_16.png","Remove component",this,"remove");
					rightPanel.add(removeButton, new AnchorConstraint(872, 921, 925, 540, 
							AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, 
							AnchorConstraint.ANCHOR_NONE, AnchorConstraint.ANCHOR_NONE));
					removeButton.setPreferredSize(new java.awt.Dimension(21, 21));
				}
				{
					disableButton = new IconButton("/icons/reload_16.png","Toggle active",this,"active");
					rightPanel.add(disableButton, new AnchorConstraint(872, 578, 913, 240, 
							AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_NONE, 
							AnchorConstraint.ANCHOR_NONE, AnchorConstraint.ANCHOR_REL));
					disableButton.setPreferredSize(new java.awt.Dimension(21, 21));
				}
				{
					editButton = new IconButton("/icons/edit_16.png","Edit component",this,"edit");
					rightPanel.add(editButton, new AnchorConstraint(872, 190, 904, 100, 
							AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_NONE, 
							AnchorConstraint.ANCHOR_NONE, AnchorConstraint.ANCHOR_REL));
					editButton.setPreferredSize(new java.awt.Dimension(21, 21));
				}
				{
					elementsPanel = new JPanel();
					BorderLayout elementsPanelLayout = new BorderLayout();
					elementsPanel.setLayout(elementsPanelLayout);
					rightPanel.add(elementsPanel, new AnchorConstraint(0, 928, 859, 90, 
							AnchorConstraint.ANCHOR_ABS, AnchorConstraint.ANCHOR_REL, 
							AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
					elementsPanel.setPreferredSize(new java.awt.Dimension(134, 334));
					elementsPanel.setBorder(BorderFactory.createTitledBorder("Components"));
					{
						elementsScroller = new JScrollPane();
						elementsPanel.add(elementsScroller, BorderLayout.CENTER);
						{
							elementsList = new JList();
							elementsScroller.setViewportView(elementsList);
							elementsList.setCellRenderer(new ElementListRenderer());
							elementsList.setDragEnabled(true);
							elementsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
							elementsList.addListSelectionListener(new ElementListListener());
							elementsList.setTransferHandler(new ElementTransferHandler());
							elementsList.addMouseListener(new MouseListener(){
								public void mouseClicked(MouseEvent e) {
									if(e.getClickCount()==2){
										PlottingElement o = (PlottingElement) elementsList.getSelectedValue();
										openLayerSheet(o);
									}
								}
								public void mouseEntered(MouseEvent e) {}
								public void mouseExited(MouseEvent e) {}
								public void mousePressed(MouseEvent e) {
									maybePopup(e);
								}
								public void mouseReleased(MouseEvent e) {
									maybePopup(e);
								}
								
								public void maybePopup(MouseEvent e){
									if(e.isPopupTrigger()){
										int i = elementsList.locationToIndex(e.getPoint());
										if(i<0)
											return;
										ElementPopupMenu.element = (PlottingElement) 
																	elementsList.getModel().getElementAt(i);
										ElementPopupMenu.elList=elementsList;
										ElementPopupMenu.plot=PlotBuilder.this;
										ElementPopupMenu.getPopup().show(e.getComponent(),
												e.getX(),e.getY());
									}								
								}
								
							});

						}
					}
				}
			}
			{
				
				bottomPanel = new JPanel();
				AnchorLayout bottomPanelLayout = new AnchorLayout();
				bottomPanel.setLayout(bottomPanelLayout);
				pane.add(bottomPanel, new AnchorConstraint(870, 1000, 1000, 0, 
						AnchorConstraint.ANCHOR_NONE, AnchorConstraint.ANCHOR_REL, 
						AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
				bottomPanel.setPreferredSize(new java.awt.Dimension(688, 59));
				{
					helpButton = new HelpButton("pmwiki.php?n=Main.PlotBuilder");
					bottomPanel.add(helpButton, new AnchorConstraint(364, 51, 872, 19, 
							AnchorConstraint.ANCHOR_NONE, AnchorConstraint.ANCHOR_NONE, 
							AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
					helpButton.setPreferredSize(new java.awt.Dimension(36, 36));
				}
				{
					okayCancel = new OkayCancelPanel(true,true,this);
					bottomPanel.add(okayCancel, new AnchorConstraint(127, 965, 872, 592, 
							AnchorConstraint.ANCHOR_NONE, AnchorConstraint.ANCHOR_REL, 
							AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_NONE));
					okayCancel.setPreferredSize(new java.awt.Dimension(267, 39));
				}

			}
			
			String[] Menu = { "+", "File", "@O New plot","new","@O Load plot","open", "@S Save plot","save",
					"+","Tools",
					"~Window","0" };
			JMenuBar mb = EzMenuSwing.getEzMenu(this, menuListener, Menu);
			
			setContentPane(pane);
			pack();
			this.addWindowListener(this);
			this.setSize(705, 620);
			this.setTitle("Plot Builder");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void setModel(PlotBuilderModel m){
		model = m;
		elementsList.setModel(m.getListModel());
	}
	
	public PlotBuilderModel getModel(){return model;}

	public void openLayerSheet(PlottingElement element){
		closeLayerSheet();
		if(layerSheet == null){
			layerSheet = new PlotBuilderSubFrame(this);
			layerSheet.setSize(500, layerSheet.getHeight());
		}
		layerSheet.setElement(element);
		layerSheet.setVisible(true);
	}
	
	public void closeLayerSheet(){
		if(layerSheet!=null && layerSheet.isVisible()){
			layerSheet.setToInitialModel();
			layerSheet.setVisible(false);
		}
	}
	
	public void expandTopPanel(){
		topPanel.setPreferredSize(new java.awt.Dimension(683, 280));
		shadow.setVisible(true);
		addTabs.revalidate();
		topPanel.repaint();
		topPanel.validate();
		this.validate();
		this.repaint();
	}
	
	public void retractTopPanel(){
		shadow.setVisible(false);
		topPanel.setPreferredSize(new java.awt.Dimension(683, 137));
		addTabs.revalidate();
		topPanel.repaint();
		topPanel.validate();
		rightPanel.repaint();
		rightPanel.validate();
		background.validate();
		background.repaint();
	}
	
	public void plot(String cmd){
		if(cmd ==null || cmd == "")
			return;
		final boolean wasNull = device==null;
		if(wasNull){
			plotHolder.removeAll();
			device = new PlotPanel(plotHolder.getWidth(), plotHolder.getHeight());
			device.setTransferHandler(new PanelTransferHandler());
			plotHolder.add(device);
		}	
		
		okayCancel.getApproveButton().setEnabled(false);
		final JLabel lab = new JLabel("plotting...");
		lab.setHorizontalAlignment(JLabel.CENTER);
		lab.setFont(Font.decode("Arial-BOLD-30"));
		lab.setForeground(Color.green);
		final String command = cmd;
		final PlotBuilder pb = this;
		pane.add(lab, new AnchorConstraint(137, 158, 52, 22, 
				AnchorConstraint.ANCHOR_ABS, AnchorConstraint.ANCHOR_ABS, 
				AnchorConstraint.ANCHOR_ABS, AnchorConstraint.ANCHOR_REL),100);
		pane.setLayer(lab, 100);
		pane.validate();
		pane.repaint();
		new Thread(new Runnable(){
			public void run() {
				try{
					DeviceInterface.plot(command,device);
				}catch(Exception e){e.printStackTrace();}
				SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                    	pane.remove(lab);
        				okayCancel.getApproveButton().setEnabled(true);
        				device.repaint();
        				pane.validate();
                    }
                });
				
			}
			
		}).start();

	}
	
	public void updatePlot(){
		String c = model.getCall();
		if( c!=null && c!="ggplot()")
			plot(c);
	}


	
	class TopPanelMouseListener implements MouseListener{
		private boolean overComponent = false;
		public void mouseClicked(MouseEvent arg0) {}

		public void mouseEntered(MouseEvent arg0) {
			overComponent=true;
			final int ind = addTabs.getSelectedIndex();
			(new Thread(new Runnable(){

				public void run() {
					try {
						Thread.sleep(500);
						if(overComponent==true && ind<4)
							expandTopPanel();						
					} catch (Exception e) {
						e.printStackTrace();
					}

				}
				
			})).start();

		}

		public void mouseExited(MouseEvent arg0) {
			overComponent = false;
			(new Thread(new Runnable(){

				public void run() {
					try {
						Thread.sleep(500);
						if(overComponent==false)
							retractTopPanel();						
					} catch (Exception e) {
						e.printStackTrace();
					}

				}
				
			})).start();
			
		}

		public void mousePressed(MouseEvent arg0) {}

		public void mouseReleased(MouseEvent arg0) {}

	}

	
	class AddElementTransferHandler extends TransferHandler{
		
		public boolean canImport(JComponent comp,DataFlavor[] d) {
			return true;
		}

		public boolean importData(JComponent comp, Transferable t) {
			return false;
		}
        
		public int getSourceActions(JComponent c) {
			return COPY;
		}
        
		protected Transferable createTransferable(JComponent c) {
			try{
	            JList list = (JList)c;
	            PlottingElement value = (PlottingElement) list.getSelectedValue();
	            if(value==null)
	            	System.out.println("failed");
				retractTopPanel(); //probably not the best place for this
	            return (PlottingElement)value.clone();
			}catch(Exception e){
				e.printStackTrace();
				return null;
			}
        }
    }
	
	class PanelTransferHandler extends TransferHandler{
		
		public boolean canImport(JComponent comp,DataFlavor[] d) {
			return true;
		}
		
		public int getSourceActions(JComponent c) {
			return COPY;
		}
		
		public boolean importData(JComponent comp, Transferable t) {
			JList l = elementsList;
			DefaultListModel mod = (DefaultListModel) l.getModel();
			PlottingElement p;
			try {
				p = (PlottingElement) t.getTransferData(
						new DataFlavor(PlottingElement.class,"Plotting element"));
				if(p.getModel() instanceof Layer){
					Layer layer = (Layer) p.getModel();
					model.tryToFillRequiredAess(layer);
					String s = layer.checkValid();
					if(s!=null){
						PlottingElementDialog d = 
							new PlottingElementDialog(PlotBuilder.this,p);
						d.setModal(true);
						d.setLocationRelativeTo(PlotBuilder.this);
						d.setVisible(true);
						s = layer.checkValid();
						if(s!=null)
							return false;
					}
				}
				mod.addElement(p);
				updatePlot();
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			} 			
			return true;
		}
	}
	
	class ElementTransferHandler extends TransferHandler{
		public int lastIndex = -1;
		public boolean selfTarget=false;
		public boolean canImport(JComponent comp,DataFlavor[] d) {
			if( d.length==1 && d[0].equals(PlottingElement.DATAFLAVOR))
				return true;
			return false;
		}

		public boolean importData(JComponent comp, Transferable t) {
			try {		
			JList l = (JList) comp;
			
			DefaultListModel mod = (DefaultListModel) l.getModel();
			PlottingElement p;
	
				p = (PlottingElement) t.getTransferData(
						new DataFlavor(PlottingElement.class,"Plotting element"));
	
				if(p.getModel() instanceof Layer){
					Layer layer = (Layer) p.getModel();
					model.tryToFillRequiredAess(layer);
					String s = layer.checkValid();
					if(s!=null){
						PlottingElementDialog d = 
							new PlottingElementDialog(PlotBuilder.this,p);
						d.setModal(true);
						d.setLocationRelativeTo(PlotBuilder.this);
						d.setVisible(true);
						s = layer.checkValid();
						if(s!=null)
							return false;
					}
				}
				
				int ind = l.getSelectedIndex();
				if(ind+1 < lastIndex)
					lastIndex++;
				if(ind<0)
					mod.insertElementAt(p, 0);
				else
					mod.insertElementAt(p, ind+1);
				updatePlot();
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			} 			
			return true;
		}
        
		public int getSourceActions(JComponent c) {
			return MOVE;
		}
        
		protected Transferable createTransferable(JComponent c) {
            JList list = (JList)c;
            PlottingElement value = (PlottingElement) list.getSelectedValue();
            lastIndex = list.getSelectedIndex();
            return value;
        }

		   
		public void exportDone(JComponent c, Transferable t, int action){
			try {			
				if(action != TransferHandler.MOVE)
					return;
				PlottingElement p;

				p = (PlottingElement) t.getTransferData(
						new DataFlavor(PlottingElement.class,"Plotting element"));
	
				JList list = (JList)c;
				((DefaultListModel)list.getModel()).remove(lastIndex);
			} catch (Exception e) {
				e.printStackTrace();
				return;
			} 					
		}
		
		public Icon getVisualRepresentation(Transferable t){
			PlottingElement p;
			try {
				p = (PlottingElement) t.getTransferData(
						new DataFlavor(PlottingElement.class,"Plotting element"));
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			} 			
			return new ImageIcon(p.getImage());
		}
    }
	
	class ElementDropListener implements DragSourceListener{

		public void dragDropEnd(DragSourceDropEvent arg0) {
			if(arg0.getDropSuccess()){
				ElementTransferHandler th = (ElementTransferHandler) elementsList.getTransferHandler();
				if(th.lastIndex>=0)
					((DefaultListModel)elementsList.getModel()).remove(th.lastIndex);
			}
		}

		public void dragEnter(DragSourceDragEvent arg0) {}

		public void dragExit(DragSourceEvent arg0) {}

		public void dragOver(DragSourceDragEvent dsde) {}

		public void dropActionChanged(DragSourceDragEvent dsde) {}
		
	}
	
	class ElementListListener implements ListSelectionListener{

		public void valueChanged(ListSelectionEvent arg0) {
			if(layerSheet!=null && layerSheet.isVisible())
				closeLayerSheet();
		}
		
	}
	
	static class ElementPopupMenu{
		private static JPopupMenu popup;
		private static PlottingElement element;
		private static JList elList;
		private static PlotBuilder plot;
		
		private static JPopupMenu getPopup(){
			if(popup==null){
				popup = new JPopupMenu();
				elementPopupMenuItems=new Vector();
				JMenuItem menuItem = new JMenuItem("Edit");
				menuItem.addActionListener(new ActionListener(){
					
					public void actionPerformed(ActionEvent e) {
						plot.openLayerSheet(element);
					}
					
				});
				popup.add(menuItem);
				menuItem = new JMenuItem("Toggle active");
				menuItem.addActionListener(new ActionListener(){
					
					public void actionPerformed(ActionEvent e) {
						element.setActive(!element.isActive());
						elList.validate();
						elList.repaint();
						plot.updatePlot();
					}
					
				});
				popup.add(menuItem);
				menuItem = new JMenuItem("Break out");
				menuItem.addActionListener(new ActionListener(){
					
					public void actionPerformed(ActionEvent e) {
						JOptionPane.showMessageDialog(null, "Break out:" + element.getName());
					}
					
				});
				if(element.isCompound())
					popup.add(menuItem);
			    
			    menuItem = new JMenuItem("Remove");
			    menuItem.addActionListener(new ActionListener(){

					public void actionPerformed(ActionEvent e) {
						((DefaultListModel)elList.getModel()).removeElement(element);
						plot.updatePlot();
					}
			    	
			    });
			    popup.add(menuItem);
			    
			    
			}
			return popup;
		}
	}
	
	class AddMouseListener implements MouseListener{
		public void mouseClicked(MouseEvent e) {}
		public void mouseEntered(MouseEvent e) {}
		public void mouseExited(MouseEvent e) {}
		public void mousePressed(MouseEvent e) {
			maybePopup(e,true);
		}
		public void mouseReleased(MouseEvent e) {
			maybePopup(e,false);
		}
		
		public void maybePopup(MouseEvent e,boolean pressed){
			if(e.isPopupTrigger()){
				JList list = (JList) e.getSource();
				int i = list.locationToIndex(e.getPoint());
				if(i<0)
					return;
				AddElementPopupMenu.element = (PlottingElement) 
											list.getModel().getElementAt(i);
				AddElementPopupMenu.pBuilder = PlotBuilder.this;
				AddElementPopupMenu.getPopup().show(e.getComponent(),
						e.getX(),e.getY());
			}else if(e.getClickCount() == 2 && pressed){
				JList list = (JList) e.getSource();
				int i = list.locationToIndex(e.getPoint());
				PlottingElement pe =(PlottingElement) list.getModel().getElementAt(i);
				PlottingElement p = (PlottingElement) pe.clone();
				if(p.getModel() instanceof Layer){
					Layer layer = (Layer) p.getModel();
					model.tryToFillRequiredAess(layer);
					String s = layer.checkValid();
					if(s!=null){
						PlottingElementDialog d = 
							new PlottingElementDialog(PlotBuilder.this,p);
						d.setModal(true);
						d.setLocationRelativeTo(PlotBuilder.this);
						d.setVisible(true);
						s = layer.checkValid();
						if(s!=null)
							return;
					}
				}
				model.getListModel().addElement(p.clone());
				updatePlot();
			}
		}
		
	}
	
	static class AddElementPopupMenu{
		private static JPopupMenu popup;
		private static PlottingElement element;
		private static PlotBuilder pBuilder;
		private static JPopupMenu getPopup(){
			if(popup==null){
				popup = new JPopupMenu();
				elementPopupMenuItems=new Vector();
				JMenuItem menuItem = new JMenuItem("Add");
				menuItem.addActionListener(new ActionListener(){
					
					public void actionPerformed(ActionEvent e) {
						PlottingElement p = (PlottingElement) element.clone();
						if(p.getModel() instanceof Layer){
							Layer layer = (Layer) p.getModel();
							pBuilder.model.tryToFillRequiredAess(layer);
							String s = layer.checkValid();
							if(s!=null){
								PlottingElementDialog d = 
									new PlottingElementDialog(pBuilder,p);
								d.setModal(true);
								d.setLocationRelativeTo(pBuilder);
								d.setVisible(true);
								s = layer.checkValid();
								if(s!=null)
									return;
							}
						}
						pBuilder.model.getListModel().addElement(p);
						pBuilder.updatePlot();
					}
					
				});
				popup.add(menuItem);
				menuItem = new JMenuItem("Get info");
				menuItem.addActionListener(new ActionListener(){
					
					public void actionPerformed(ActionEvent e) {
						String url = element.getUrl();
						if(url!=null && url.length()>0)
							HelpButton.showInBrowser(url);
					}
					
				});
			    popup.add(menuItem);
			}
			return popup;
		}
	}

	public void actionPerformed(ActionEvent arg0) {
		String cmd = arg0.getActionCommand();
		if(cmd == "Run"){
			String call = model.getCall();
			if(call==null || call == "ggplot()"){
				JOptionPane.showMessageDialog(this, "Plot contains no components.");
				return;
			}
			lastModel = (PlotBuilderModel) model.clone();
			this.dispose();			
			Deducer.execute("dev.new()\n"+call);
		}else if(cmd == "Reset"){
			this.setModel(new PlotBuilderModel());
		}else if(cmd == "Cancel"){
			this.dispose();
		}else if(cmd == "remove"){
			PlottingElement element = (PlottingElement) elementsList.getSelectedValue();
			if(element!=null){
				((DefaultListModel)elementsList.getModel()).removeElement(element);
				updatePlot();			
			}
		}else if(cmd == "edit"){
			PlottingElement element = (PlottingElement) elementsList.getSelectedValue();
			if(element!=null)
				this.openLayerSheet(element);
		}else if(cmd == "active"){
			PlottingElement element = (PlottingElement) elementsList.getSelectedValue();
			if(element!=null){
				element.setActive(!element.isActive());
				elementsList.validate();
				elementsList.repaint();
				updatePlot();
			}
		}
	}
	
	public class PlotPanel extends JGDBufferedPanel{
		Dimension lastSize;
		public PlotPanel(double w, double h) {
			super(w, h);
			lastSize=getSize();
		}
		public PlotPanel(int w, int h) {
			super(w, h);
			lastSize = getSize();
		}
		
		public void devOff(){
			Deducer.eval("dev.off("+(this.devNr+1)+")");
			//System.out.println("turning off: "+(this.devNr+1));
		}
		
		public void initRefresh() {
			Deducer.idleEval("try(.C(\"javaGDresize\",as.integer("+devNr+")),silent=TRUE)");
		}
		
		public synchronized void paintComponent(Graphics g) {
			Dimension d=getSize();
	        if (!d.equals(lastSize)) {
	            REXP exp = Deducer.idleEval("try(.C(\"javaGDresize\",as.integer("+devNr+")),silent=TRUE)");
	            if(exp!=null)
	            	lastSize=d;
	            return;
	        }
			super.paintComponent(g);
		}
	       
	}

	public void windowActivated(WindowEvent arg0) {}
	public void windowClosed(WindowEvent arg0) {
		if(device!=null)
			device.devOff();
	}
	public void windowClosing(WindowEvent arg0) {}
	public void windowDeactivated(WindowEvent arg0) {}
	public void windowDeiconified(WindowEvent arg0) {}
	public void windowIconified(WindowEvent arg0) {}
	public void windowOpened(WindowEvent arg0) {}
	
	public class MenuListener implements ActionListener{

		public void actionPerformed(ActionEvent ae) {
			String cmd = ae.getActionCommand();
			if(cmd.equals("save")){
				JFileChooser c = new JFileChooser("Save plot");
				int ret = c.showDialog(null, "Save");
				if(ret == JFileChooser.APPROVE_OPTION){
					File f = c.getSelectedFile();
					if(!f.getName().endsWith(".ggp"))
						f = new File(f.getPath() + ".ggp");
					if(f.exists()){
						int o =JOptionPane.showConfirmDialog(null, "File exists: Overwrite" + f.getName() + "?");
						if(o != JOptionPane.OK_OPTION)
							return;
					}
					model.saveToFile(f);
				}
			}else if(cmd.equals("open")){
				JFileChooser c = new JFileChooser("Open plot");
				int ret = c.showOpenDialog(null);
				if(ret == JFileChooser.APPROVE_OPTION){
					File f = c.getSelectedFile();
					if(!f.getName().endsWith(".ggp")){
						JOptionPane.showMessageDialog(null, "This does not appear to be a"
								+" ggplot2 PlotBuilder file (extension .ggp)");
						return;
					}
					if(f!=null && f.exists()){
						PlotBuilderModel newModel = new PlotBuilderModel();
						newModel.setFromFile(f);
						setModel(newModel);
						updatePlot();
					}
				}
			}else if(cmd.equals("new")){
				PlotBuilder pb = new PlotBuilder(new PlotBuilderModel());
				pb.setVisible(true);
				WindowTracker.addWindow(pb);
			}
		}
		
	}
	
}



















