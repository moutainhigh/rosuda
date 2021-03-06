/*
 * FixVariablesSliderPanel.java
 *
 * Created on 7. Juli 2005, 12:29
 */

package org.rosuda.JClaR;

import javax.swing.event.ChangeListener;

/**
 *
 * @author  tobias
 */
public final class FixVariablesSliderPanel extends javax.swing.JPanel {
    /**
     * FIXME: serialVersionUID field auto-generated by RefactorIT
     */
    static final long serialVersionUID = 200602271225L;
    
    /** Creates new form FixVariablesSliderPanel */
    FixVariablesSliderPanel() {
        initComponents();
    }
    
    void addSliderListener(final ChangeListener l){
        sldValue.addChangeListener(l);
        sldDeviation.addChangeListener(l);
    }
    
    int getDeviation(){
        return sldDeviation.getValue();
    }
    
    int getValue(){
        return sldValue.getValue();
    }
    
    void setDeviation(final int newDev){
        sldDeviation.setValue(newDev);
    }
    
    void setValue(final int newVal){
        sldValue.setValue(newVal);
    }
    
    int whichSlider(final Object obj){
        if (obj.equals(sldDeviation))  {
            return 0;
        }
        
        else  {
            return 1;
        }
        
    }
    
    boolean getAutoUpdate(){
        return jCheckBox1.isSelected();
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents() {//GEN-BEGIN:initComponents
        jPanel1 = new javax.swing.JPanel();
        lblValue = new javax.swing.JLabel();
        sldValue = new javax.swing.JSlider();
        lblDeviation = new javax.swing.JLabel();
        sldDeviation = new javax.swing.JSlider();
        jCheckBox1 = new javax.swing.JCheckBox();

        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));

        lblValue.setText("Value:");
        jPanel1.add(lblValue);

        jPanel1.add(sldValue);

        lblDeviation.setText("Deviation:");
        jPanel1.add(lblDeviation);

        jPanel1.add(sldDeviation);

        add(jPanel1);

        jCheckBox1.setSelected(true);
        jCheckBox1.setText("Update automatically");
        add(jCheckBox1);

    }//GEN-END:initComponents
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JLabel lblDeviation;
    private javax.swing.JLabel lblValue;
    private javax.swing.JSlider sldDeviation;
    private javax.swing.JSlider sldValue;
    // End of variables declaration//GEN-END:variables
    
}
