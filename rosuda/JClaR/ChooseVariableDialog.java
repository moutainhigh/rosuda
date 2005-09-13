/*
 * ChooseVariableDialog.java
 *
 * Created on 23. Juli 2005, 12:26
 *
 */

package org.rosuda.JClaR;
import java.awt.Color;
import java.util.Vector;
import javax.swing.JRadioButton;


/**
 * Dialog to select the variable that contains the classes.
 * @author tobias
 */
public final class ChooseVariableDialog extends TableDialog implements SelectionModIF {
    
    private TableModel tm;
    private int variable=-1;
    
    /**
     * Creates a new instance of ChooseVariableDialog
     * @param parent The parent frame. This dialog will be set modal to parent.
     * @param variables Vector that contains the variable names as Strings.
     */
    ChooseVariableDialog(final java.awt.Frame parent, final Vector variables) {
        super(parent, true);
        setUpdateButton(false);
        
        tm = new TableModel();
        // create data vector
        final Vector dataVector = new Vector(variables.size());
        // each element is <variable name>|false|true
        for(int i=0; i<variables.size(); i++){
            final Vector row = new Vector(3);
            row.add(variables.elementAt(i));
            row.add(new Boolean(false));
            row.add(new Boolean(true));
            dataVector.add(row);
        }
        final Object columnIdentifiers[]={"Variable", "Classes", "Used"};
        tm.setDataVector(dataVector, columnIdentifiers);
        setTableModel(tm);
        
        // set RadioButton as editor and renderer
        final JRadioButton jrb = new JRadioButton();
        jrb.setBackground(Color.WHITE);
        getColumn(1).setCellRenderer(new RadioButtonRenderer());
        getColumn(1).setCellEditor(new RadioButtonEditor(jrb));
        
        
        SelectionPanel selp = new SelectionPanel(this);
        addComponent(selp);
        
        // fit frame size to actual data
        resetSize();
    }
    
    /**
     * Called when OK button is clicked.
     */
    protected void ok() {
        if ((variable=tm.getVariablePos())>-1) {
            // count how many variables are marked as used (not counting class variable)
            int count=0;
            for(int i=0; i<tm.getRowCount(); i++){
                if(i!=variable && tm.getUsed(i)) count++;
            } 
            // at least 2 variables must be used
            //TODO: accept count of 2
            if (count>=2){
                // class variable must be used
                tm.setValueAt(Boolean.TRUE, variable, COL_USED);
                dispose();
            } else{
                ErrorDialog.show(this,"Not enough variables marked as used.");
            }
        }
        
        else  {
            ErrorDialog.show(this,"Please select a variable.");
        }
        
    }
    
    /**
     * Sets an instruction text.
     * @param text Instruction text.
     */
    void setText(final String text){
        //TODO: mplement
    }
    
    /**
     * Get variables that should be excluded from classification.
     * @return int array with indices of the unused variables
     */
    int[] getUnusedVariables(){
        final int unused[] = new int[tm.getRowCount()];
        int numberUnused=0;
        for(int i=0; i<unused.length; i++){
            if (!tm.getUsed(i)) {
                unused[numberUnused++]=i;
            }
            
        }
        
        // shrink array to minimal size
        final int ret[] = new int[numberUnused];
        System.arraycopy(unused, 0, ret, 0,  numberUnused);
        return ret;
    }
    
    private static final int COL_CLASSES = 1;
    private static final int COL_USED = 2;
    
    private final class TableModel extends TableDialog.TableModel {
        
        private Vector variables;
        private int variablePos=-1;
        
        public TableModel(){
            final Class[] colClasses = {String.class, Boolean.class, Boolean.class};
            final boolean[] colEditable = {false, true, true};
            
            setColumnClasses(colClasses);
            setColumnEditable(colEditable);
        }
        
        /**
         * Check whether a particular variable should be used.
         * @param row The questioned row
         * @return whether variable in row is marked as used
         */
        public boolean getUsed(final int row){
            return ((Boolean)getValueAt(row, COL_USED)).booleanValue();
        }
        
        /**
         * Get row of selected variable.
         * @return selected row
         */
        public int getVariablePos(){
            for (int i=0; i<getRowCount(); i++){
                if (((Boolean)getValueAt(i, COL_CLASSES)).booleanValue()){
                    return i;
                }
            }
            return -1;
        }
        
        public void setValueAt(final Object aValue, final int row, final int column) {
            if (column==COL_CLASSES){
                // let only one radio button be selected
                if(!((Boolean)getValueAt(row, column)).booleanValue()){
                    for (int i=0; i<getRowCount(); i++) {
                        super.setValueAt(new Boolean(i==row), i, column);}
                }
            } else {
                super.setValueAt(aValue, row, column);
            }
            
        }
    }
    
    /**
     * Get index of class variable.
     * @return index of the selected variable, considering unused variables
     */
    int getVariable() {
        int shift=0;
        for(int i=0; i<variable; i++) {
            if(!tm.getUsed(i)) {
                shift++;
            }
            
        }
        
        return this.variable - shift;
    }
    
    public void invertSelection(){
        tm.invertSelection(COL_USED);
    }
    
    public void selectAll(){
        tm.selectAll(COL_USED);
    }
    
    public void selectNothing(){
        tm.selectNothing(COL_USED);
    }
}
