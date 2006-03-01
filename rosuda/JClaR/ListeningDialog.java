/*
 * TableDialog.java
 *
 * Created on 23. Juni 2005, 13:51
 */

package org.rosuda.JClaR;

import java.awt.Component;
import java.awt.Dimension;
import java.util.EventListener;
import java.util.Vector;
import javax.swing.ListSelectionModel;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

/**
 *
 * @author  tobias
 */
public abstract class ListeningDialog extends javax.swing.JDialog {
    
    boolean success=false;
    private EventListenerList listeners=new EventListenerList();
    
    /** Creates new form TableDialog */
    ListeningDialog(final java.awt.Frame parent) {
        this(parent, false);
    }
    
    ListeningDialog(final java.awt.Frame parent, final boolean modal){
        super(parent,modal);
        initComponents();
    }
    
    private final boolean getSuccess(){
        return success;
    }
    
    final void addSimpleChangeListener(final SimpleChangeListener l){
        listeners.add(SimpleChangeListener.class, l);
    }
    
    private final void removeSimpleChangeListener(final SimpleChangeListener l){
        listeners.remove(SimpleChangeListener.class, l);
    }
    
    final void setUpdateButton(final boolean hasUpdateButton){
        butUpdate.setVisible(hasUpdateButton);
    }
    
    final void setOkButtonText(String text, char mnemonic){
        butOK.setText(text);
        butOK.setMnemonic(mnemonic);
    }
    
    abstract void ok();

    void update(){        /* CAUTION: empty block! */
    }
    
    private final void cancel(){
        success=false;
        dispose();
    }
    
    final void addComponent(final Component component){
        panSouth.add(component,0);
    }
    
    final void addCenterComponent(final Component component){
        getContentPane().add(component, java.awt.BorderLayout.CENTER);
    }
    
    final void fireSimpleChange(final int message){
        final EventListener[] els = listeners.getListeners(SimpleChangeListener.class);
        for(int i=0; i<els.length; i++){
            ((SimpleChangeListener)els[i]).stateChanged(new SimpleChangeEvent(this,message));
        }
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        panSouth = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        butOK = new javax.swing.JButton();
        butUpdate = new javax.swing.JButton();
        butCancel = new javax.swing.JButton();

        panSouth.setLayout(new javax.swing.BoxLayout(panSouth, javax.swing.BoxLayout.Y_AXIS));

        butOK.setMnemonic('o');
        butOK.setText("OK");
        butOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butOKActionPerformed(evt);
            }
        });

        jPanel2.add(butOK);

        butUpdate.setMnemonic('u');
        butUpdate.setText("Update");
        butUpdate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butUpdateActionPerformed(evt);
            }
        });

        jPanel2.add(butUpdate);

        butCancel.setMnemonic('c');
        butCancel.setText("Cancel");
        butCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butCancelActionPerformed(evt);
            }
        });

        jPanel2.add(butCancel);

        panSouth.add(jPanel2);

        getContentPane().add(panSouth, java.awt.BorderLayout.SOUTH);

        pack();
    }// </editor-fold>//GEN-END:initComponents
    
    private final void butUpdateActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butUpdateActionPerformed
        update();
    }//GEN-LAST:event_butUpdateActionPerformed
    
    private final void butCancelActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butCancelActionPerformed
        cancel();
    }//GEN-LAST:event_butCancelActionPerformed
    
    private final void butOKActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butOKActionPerformed
        ok();
    }//GEN-LAST:event_butOKActionPerformed
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton butCancel;
    private javax.swing.JButton butOK;
    private javax.swing.JButton butUpdate;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel panSouth;
    // End of variables declaration//GEN-END:variables
    
}
