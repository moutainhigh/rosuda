/*
 * TableDialog.java
 *
 * Created on 23. Juni 2005, 13:51
 */

package org.rosuda.JClaR;

import java.awt.Dimension;
import java.awt.Frame;
import java.util.Vector;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

/**
 *
 * @author  tobias
 */
public abstract class TableDialog extends ListeningDialog {
    
    boolean success=false;
    private EventListenerList listeners=new EventListenerList();
    
    private JTable table;
    
    TableDialog(final Frame parent){
        this(parent,false);
    }
        
    TableDialog(final Frame parent, final boolean modal){
        super(parent,modal);
        
        // create table component
        JScrollPane jScrollPane1 = new javax.swing.JScrollPane();
        table = new javax.swing.JTable();
        jScrollPane1.setMaximumSize(new java.awt.Dimension(300, 150));
        table.setMaximumSize(new java.awt.Dimension(3000, 3000));
        table.setMinimumSize(new java.awt.Dimension(400, 50));
        jScrollPane1.setViewportView(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        //add table
        addCenterComponent(jScrollPane1);
    }
    
    //TODO: resetSize necessary?
    final void resetSize(){
        final Dimension dim = table.getPreferredSize();
        double w = dim.getWidth();
        double h = dim.getHeight();
        if (h>200)  {
            h=200;
        }
        
        if (w>400)  {
            w=400;
        }
        
        dim.setSize(w,h);
        table.setPreferredScrollableViewportSize(dim);
        pack();
    }
    
    final void addListSelectionListener(final ListSelectionListener lSelListener){
        table.getSelectionModel().addListSelectionListener(lSelListener);
    }
    
    private final boolean getSuccess(){
        return success;
    }
    
    final void setTableModel(final AbstractTableModel tm){
        table.setModel(tm);
    }
    
    final int getSelectedRow(){
        return table.getSelectedRow();
    }
    
    final TableColumn getColumn(final int col){
        return table.getColumnModel().getColumn(col);
    }
    
    class TableModel extends javax.swing.table.DefaultTableModel {
        
        private Class[] columnClasses;
        private boolean[] columnEditable;
        
        protected final void setColumnClasses(final Class[] columnClasses){
            this.columnClasses = columnClasses;
        }
        
        protected final void setColumnEditable(final boolean[] columnEditable){
            this.columnEditable = columnEditable;
        }
        
        
        public final Class getColumnClass(final int columnIndex){
            if (columnClasses!=null){
                try{
                    return columnClasses[columnIndex];
                } catch(ArrayIndexOutOfBoundsException e){                    /* CAUTION: empty block! */
                    
                }
            }
            return super.getColumnClass(columnIndex);
        }
        
        public final boolean isCellEditable(final int rowIndex, final int columnIndex) {
            if (columnEditable!=null){
                try{
                    return columnEditable[columnIndex];
                } catch(ArrayIndexOutOfBoundsException e){                    /* CAUTION: empty block! */
                    
                }
            }
            return super.isCellEditable(rowIndex, columnIndex);
        }
        
        public final void setDataVector(final Vector newDataVector, final Object[] newColumnIdentifiers){
            setDataVector(newDataVector, convertToVector(newColumnIdentifiers));
        }
        
        public final Object[] getColumnData(final int column){
            final Vector data = getDataVector();
            final Object ret[] = new Object[getRowCount()];
            for(int i=0; i< data.size(); i++){
                ret[i] = ((Vector)data.elementAt(i)).elementAt(column);
            }
            return ret;
        }
        
        public final void setColumnData(final int column, final Object[] data){
            for(int i=0; i<data.length; i++)  {
                ((Vector)dataVector.elementAt(i)).setElementAt(data[i], column);
            }
            
            fireTableDataChanged();
        }
        
        /**
         * Inverts the boolean values in the given column.
         * @param column Column to be inverted.
         */
        public final void invertSelection(final int column){
            if(!getColumnClass(column).equals(Boolean.class)) return; // can only invert booleans
            Object[] colData = getColumnData(column);
            for(int i=0; i<colData.length; i++)
                colData[i] = ((Boolean)colData[i]).booleanValue()?Boolean.FALSE:Boolean.TRUE;
            setColumnData(column, colData);
        }
        
        /**
         * Sets all boolean values to true.
         * @param column Column to be selected.
         */
        public final void selectAll(final int column){
            if(!getColumnClass(column).equals(Boolean.class)) return; // can only be applied to booleans
            Object[] colData = getColumnData(column);
            for(int i=0; i<colData.length; i++)
                colData[i] = Boolean.TRUE;
            setColumnData(column, colData);
        }
        
        /**
         * Sets all boolean values to false.
         * @param column Column to be unselected.
         */
        public final void selectNothing(final int column){
            if(!getColumnClass(column).equals(Boolean.class)) return; // can only be applied to booleans
            Object[] colData = getColumnData(column);
            for(int i=0; i<colData.length; i++)
                colData[i] = Boolean.FALSE;
            setColumnData(column, colData);
        }
    }
    
}
