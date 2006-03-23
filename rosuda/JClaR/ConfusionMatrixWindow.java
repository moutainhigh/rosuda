/*
 * ConfusionMatrixWindow.java
 *
 * Created on 21. April 2005, 14:24
 */

package org.rosuda.JClaR;

import java.util.Vector;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author  tobias
 */
public final class ConfusionMatrixWindow extends javax.swing.JFrame {
    /**
     * FIXME: serialVersionUID field auto-generated by RefactorIT
     */
    protected static final long serialVersionUID = 200602231353L;
    
    //TODO: write SVM-Change-listener
    
    private final AccuracyTableModel atm;
    
    /**
     * Creates new form ConfusionMatrixWindow
     */
    ConfusionMatrixWindow(final int[] data, final Vector variables) {
        initComponents();
        
        //TODO: Have column and row headers
        jSP.setColumnHeaderView(null);
        atm = new AccuracyTableModel();
        atm.setData(data);
        atm.setVariables(variables);
        table.setModel(atm);
        
        table.setPreferredScrollableViewportSize(table.getPreferredSize());
        
        setTitle("Confusion matrix");
        pack();
    }
    
    void setData(final int[] data){
        atm.setData(data);
        atm.fireTableDataChanged();
        pack();
    }
    
    void setAccuracy(final double accuracy){
        atm.setAccuracy(accuracy);
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        jSP = new javax.swing.JScrollPane();
        table = new javax.swing.JTable();

        jSP.setViewportView(table);

        getContentPane().add(jSP, java.awt.BorderLayout.CENTER);

        pack();
    }
    // </editor-fold>//GEN-END:initComponents
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jSP;
    private javax.swing.JTable table;
    // End of variables declaration//GEN-END:variables
    
    private final class AccuracyTableModel extends AbstractTableModel {
        /**
         * FIXME: serialVersionUID field auto-generated by RefactorIT
         */
        protected static final long serialVersionUID = 200602231353L;
        
        private Vector variables;
        private int data[];
        private int numVariables=0;
        private double accuracy;
        
        public void setVariables(final Vector variables){
            this.variables = variables;
            numVariables = variables.size();
        }
        
        public void setData(final int[] data){
            this.data=data;
        }
        
        public void setAccuracy(final double accuracy){
            this.accuracy = accuracy;
        }
        
        public int getRowCount(){
            return numVariables+2;
        };
        public int getColumnCount(){
            return numVariables+1;
        };
        public Object getValueAt(final int row, final int column){
            if (row==numVariables+1){
                if (column==0)  {
                    return "Group size:";
                }
                
                int groupSize=0;
                for (int i=0; i<numVariables; i++)  {
                    groupSize += data[i + numVariables*(column-1)];
                }
                
                return new Integer(groupSize);
            } else if(row>0 && column>0){
                return new Integer(data[row-1 + numVariables*(column-1)]);
            } else if(row+column>0){
                return variables.elementAt(row+column-1);
            } else  {
                return new Double(accuracy);
            }
            
        };
    }
}
