package org.rosuda.JGR.data;

import org.rosuda.JGR.layout.AnchorConstraint;
import org.rosuda.JGR.layout.AnchorLayout;
import org.rosuda.JGR.toolkit.IconButton;
import org.rosuda.JGR.JGR;
import org.rosuda.JGR.RController;
import org.rosuda.JGR.robjects.RObject;
import org.rosuda.ibase.Common;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

import javax.swing.table.DefaultTableModel;
import javax.swing.WindowConstants;
import javax.swing.SwingUtilities;


/**
* This code was edited or generated using CloudGarden's Jigloo
* SWT/Swing GUI Builder, which is free for non-commercial
* use. If Jigloo is being used commercially (ie, by a corporation,
* company or business for any purpose whatever) then you
* should purchase a license for each developer using Jigloo.
* Please visit www.cloudgarden.com for details.
* Use of Jigloo implies acceptance of these licensing terms.
* A COMMERCIAL LICENSE HAS NOT BEEN PURCHASED FOR
* THIS MACHINE, SO JIGLOO OR THIS CODE CANNOT BE USED
* LEGALLY FOR ANY CORPORATE OR COMMERCIAL PURPOSE.
*/
public class DataFrameWindow extends JFrame implements ActionListener {
	private JMenuBar dataFrameMenuBar;
	private JMenu dataMenu;
	private ExScrollableTable dataScrollPane;
	private JMenuItem CopyItem;
	private JMenuItem openData;
	private IconButton jButton2;
	private JMenu helpMenu;
	private JMenu windowMenu;
	private JMenu environmentMenu;
	private JTabbedPane jTabbedPane1;
	private IconButton jButton1;
	private IconButton button1;
	private JSeparator separator2;
	private JSeparator separator1;
	private JLabel dataSelectorLabel;
	private JComboBox dataSelector;
	private JPanel dataSelectorPanel;
	private JMenu editMenu;
	private JMenu fileMenu;
	
	private ExTable table;
	
	


	
	public DataFrameWindow() {
		super();
		initGUI(null);
	}
	
	public DataFrameWindow(ExTable t) {
		super();
		initGUI(t);
	}
	
	private void initGUI(ExTable t) {
		try {
			
			RController.refreshObjects();
			
			
			BorderLayout thisLayout = new BorderLayout();
			getContentPane().setLayout(thisLayout);
			setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			this.setMinimumSize(new java.awt.Dimension(400, 400));
			{
				dataSelectorPanel = new JPanel();
				getContentPane().add(dataSelectorPanel, BorderLayout.NORTH);
				AnchorLayout dataSelectorPanelLayout = new AnchorLayout();
				dataSelectorPanel.setLayout(dataSelectorPanelLayout);
				dataSelectorPanel.setPreferredSize(new java.awt.Dimension(839, 52));
				dataSelectorPanel.setSize(10, 10);
				dataSelectorPanel.setMinimumSize(new java.awt.Dimension(100, 100));
				{
					button1 = new IconButton("/icons/kfloppy.png","Save Data",this,"Save Data");
					dataSelectorPanel.add(button1, new AnchorConstraint(12, 60, 805, 62, 
										AnchorConstraint.ANCHOR_ABS, AnchorConstraint.ANCHOR_NONE, 
										AnchorConstraint.ANCHOR_NONE, AnchorConstraint.ANCHOR_ABS));
					button1.setFont(new java.awt.Font("Dialog",0,8));
					button1.setPreferredSize(new java.awt.Dimension(32,32));
				
				}
				{
					separator2 = new JSeparator();
					dataSelectorPanel.add(separator2, new AnchorConstraint(-76, 620, 1008, 608, 
										AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, 
										AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
					separator2.setPreferredSize(new java.awt.Dimension(10, 64));
					separator2.setOrientation(SwingConstants.VERTICAL);
				}
				{
					separator1 = new JSeparator();
					dataSelectorPanel.add(separator1, new AnchorConstraint(8, 415, 1076, 402, 
										AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, 
										AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
					separator1.setPreferredSize(new java.awt.Dimension(11, 63));
					separator1.setOrientation(SwingConstants.VERTICAL);
				}
				{
					dataSelectorLabel = new JLabel();
					dataSelectorPanel.add(dataSelectorLabel, new AnchorConstraint(48, 595, 432, 415, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
					dataSelectorLabel.setText("Data Set");
					dataSelectorLabel.setPreferredSize(new java.awt.Dimension(151, 20));
					dataSelectorLabel.setFont(new java.awt.Font("Dialog",0,14));
					dataSelectorLabel.setHorizontalAlignment(SwingConstants.CENTER);
					dataSelectorLabel.setHorizontalTextPosition(SwingConstants.CENTER);
				}
				{
					DefaultComboBoxModel dataSelectorModel = //new DataFrameComboBoxModel();
						new DefaultComboBoxModel(JGR.DATA);
					dataSelector = new JComboBox();
					dataSelectorPanel.add(dataSelector, new AnchorConstraint(432, 594, 906, 415, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
					dataSelector.setModel(dataSelectorModel);
					dataSelector.setPreferredSize(new java.awt.Dimension(149, 28));
					dataSelector.addActionListener(this);
				}
				{
					jButton1 = new IconButton("/icons/opendata_24.png","Open Data",this,"Open Data");
					dataSelectorPanel.add(jButton1, new AnchorConstraint(12, 60, 805, 12, AnchorConstraint.ANCHOR_ABS, 
											AnchorConstraint.ANCHOR_NONE, AnchorConstraint.ANCHOR_NONE, 
											AnchorConstraint.ANCHOR_ABS));
					jButton1.setPreferredSize(new java.awt.Dimension(32,32));
				}
				{
					jButton2 = new IconButton("/icons/trashcan_remove_32.png","Clear",this,"Clear Data");
					dataSelectorPanel.add(jButton2, new AnchorConstraint(144, 12, 971, 863, AnchorConstraint.ANCHOR_NONE, AnchorConstraint.ANCHOR_ABS, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_NONE));
					jButton2.setPreferredSize(new java.awt.Dimension(40,40));
				}
			}
			{
				jTabbedPane1 = new JTabbedPane();
				getContentPane().add(jTabbedPane1, BorderLayout.CENTER);
				jTabbedPane1.setPreferredSize(new java.awt.Dimension(839, 395));
				jTabbedPane1.setTabPlacement(JTabbedPane.LEFT);

				{
					if(t==null){
						if(JGR.DATA.size()==0)
							dataScrollPane = null;
						else
							dataScrollPane = new ExScrollableTable(table =new ExTable(new RDataFrameModel(((RObject)JGR.DATA.elementAt(0)).getName())));
					}else
						dataScrollPane = new ExScrollableTable(table = t);
				}
				jTabbedPane1.addTab("Data View", null, dataScrollPane, null);
				jTabbedPane1.addTab("Variable View", null, null, null);
			}
			{
				dataFrameMenuBar = new JMenuBar();
				setJMenuBar(dataFrameMenuBar);
				{
					fileMenu = new JMenu();
					dataFrameMenuBar.add(fileMenu);
					fileMenu.setText("  File");
					fileMenu.setPreferredSize(new java.awt.Dimension(44, 23));
					{
						openData = new JMenuItem();
						fileMenu.add(openData);
						openData.setText("Open Data");
					}
				}
				{
					editMenu = new JMenu();
					dataFrameMenuBar.add(editMenu);
					editMenu.setText("Edit");
					editMenu.setPreferredSize(new java.awt.Dimension(40, 23));
					{
						CopyItem = new JMenuItem();
						editMenu.add(CopyItem);
						CopyItem.setText("Copy");
					}
				}
				{
					environmentMenu = new JMenu();
					dataFrameMenuBar.add(environmentMenu);
					environmentMenu.setText("Environment");
					environmentMenu.setPreferredSize(new java.awt.Dimension(81, 23));
					if(Common.isMac())
						environmentMenu.setPreferredSize(new java.awt.Dimension(101, 23));
				}				
				{
					dataMenu = new JMenu();
					dataFrameMenuBar.add(dataMenu);
					dataMenu.setText("Data");
					dataMenu.setPreferredSize(new java.awt.Dimension(44, 23));
				}
				{
					windowMenu = new JMenu();
					dataFrameMenuBar.add(windowMenu);
					windowMenu.setText("Window");
					windowMenu.setPreferredSize(new java.awt.Dimension(62, 23));
				}
				{
					helpMenu = new JMenu();
					dataFrameMenuBar.add(helpMenu);
					helpMenu.setText("Help");
				}

			}
			pack();
			this.setSize(839, 839);
			final JFrame theFrame = this;
			theFrame.addComponentListener(new java.awt.event.ComponentAdapter() {
				  public void componentResized(ComponentEvent event) {
					  theFrame.setSize(
				      Math.max(300, theFrame.getWidth()),
				      theFrame.getHeight());
				  }
				});

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void actionPerformed(ActionEvent e) {
		if(dataSelector==((JComboBox)e.getSource())){
			JGR.R.eval("print('"+e.getActionCommand()+"')");
			if(e.getActionCommand()=="comboBoxChanged"){

				dataScrollPane = new ExScrollableTable(new ExTable(new RDataFrameModel(
						((RObject)dataSelector.getSelectedItem()).getName())));
				jTabbedPane1.setComponentAt(0, dataScrollPane);
			}

		}
	}
	
	class DataFrameComboBoxModel extends DefaultComboBoxModel{
		
		
		
		public DataFrameComboBoxModel(){
			super();
			
		}

		public int getSize(){
			//Keep current with environment
			//TODO: This is not pretty
			RController.refreshObjects();
			dataSelector.setModel(this);
			
			return JGR.DATA.size();
		}
		public Object getElementAt(int index){
			return ((RObject)JGR.DATA.elementAt(index)).getName();
		}
		public int getIndexOf(Object item){
			return JGR.DATA.indexOf(item);
		}
	}
}