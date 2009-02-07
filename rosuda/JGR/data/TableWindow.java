/**
 * ExTable is a Graphical table that extends JTable and provides
 * superior ease of data entry and manipulation. The goal is to mirror
 * Excel's behavior
 * Copyright (C) 2009  Ian Fellows
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */


package org.rosuda.JGR.data;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;

import javax.swing.WindowConstants;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import java.util.*;

/**
 * Table window implements a scrollable pane in which to house ExTable.
 * It also implements the row headers.
 * 
 * 
 * @author Ian Fellows
 *
 */
public class TableWindow extends javax.swing.JFrame {

	private ExScrollableTable jScrollPane1;
	private ExTable table;
	private TableModel tableModel;
	



	/**
	 * Sets up a Window
	 * @param t
	 * @param tm
	 */
	public TableWindow(ExTable t,TableModel tm) {
		super();
		table = t;
		tableModel = tm;
		initGUI();
	}
	
	
	/**
	 * Starts up the GUI components
	 * 
	 */
	private void initGUI() {
		try {
			setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			{
				jScrollPane1 = new ExScrollableTable(table);
				getContentPane().add(jScrollPane1, BorderLayout.CENTER);
				jScrollPane1.setPreferredSize(new java.awt.Dimension(537, 334));
			}
			pack();
			this.setSize(758, 441);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	

	
}