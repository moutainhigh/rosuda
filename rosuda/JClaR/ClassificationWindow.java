/*
 * ClassificationWindow.java
 *
 * Created on 6. Mai 2005, 19:18
 */

package org.rosuda.JClaR;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.prefs.PreferenceChangeListener;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;



/**
 * Prototype for windows, which allow to change classification parameters
 * and which display plots.
 * @author tobias
 */
public abstract class ClassificationWindow extends JFrame implements SimpleChangeListener, PreferenceChangeListener {
    
    Classifier classifier;
    
    private Plot plot;
    private RserveConnection rcon;
    
    private boolean noRecalc=false; //TODO: is this ever used?
    private SnapshotPanel snapPan = new SnapshotPanel();
    
    //TODO: is there another way to avoid recalculating the plot when still resizing?
    private Timer resizePlotTimer = new Timer();
    private TimerTask resizePlotTask= new ResizePlotTask(this);
    
    Data data;
    private ConfusionMatrixWindow confMxDialog;
    
    private final int originalVariablePos;
    private final Data originalData;
    
    private int[] hiddenVariables;
    
    private SidePanel sidePanel;
    private PreferencesDialog prefd;
    
    /**
     * Contains the subwindows that belong to this ClassificationWindow (e.g. the
     * confusuion matrix window). These windows will get disposed when
     * this.dispose() is called.
     */
    private Vector subWindows = new Vector();
    
    
    static final int CHANGE_TYPE_HARD=0;
    static final int CHANGE_TYPE_SOFT=1;
    static final int CHANGE_TYPE_RESIZE=2;
    
    /**
     * Creates new form ClassificationWindow
     */
    ClassificationWindow(final Classifier classifier) {
        
        this.classifier = classifier;
        
        rcon=RserveConnection.getRconnection();
        initComponents();
        
        originalVariablePos = classifier.getVariablePos();
        originalData = classifier.getData();
        data = originalData;
        
        
        final SpinnerNumberModel snm = (SpinnerNumberModel)spinZoom.getModel();
        snm.setValue(new Integer(100));
        snm.setMinimum(new Integer(0));
        snm.setStepSize(new Integer(20));
        
        plot=classifier.getPlot();
        if (plot!=null)  {
            plot.setShowDataInPlot(m_DisplayDataInPlot.getState());
        }
        
        updatePlot();
        
        scrPlotVertical.addAdjustmentListener(new AdjustmentListener(){
            public final void adjustmentValueChanged(final AdjustmentEvent e){
                setVerticalPanning(e.getValue() + scrPlotVertical.getVisibleAmount()/2);
            }
        });
        
        scrPlotHorizontal.addAdjustmentListener(new AdjustmentListener(){
            public final void adjustmentValueChanged(final AdjustmentEvent e){
                setHorizontalPanning(e.getValue() + scrPlotHorizontal.getVisibleAmount()/2);
            }
        });
        
        snapPan.addRestoreActionListener(new ActionListener(){
            public final void actionPerformed(final java.awt.event.ActionEvent evt){
                restore(snapPan.getSelectedSnapshot());
            }
        });
        
        updateVisibleAmount();
        (new BorderLayout()).minimumLayoutSize(this);
        pack();
        Dimension minSize=getMinimumSize();
        setSize(Math.max(740,minSize.width), Math.max(550,minSize.height));
        
        lockInMinSize(this);
    }
    
    private final void updateVisibleAmount(){
        scrPlotHorizontal.setVisibleAmount((int)Math.floor(
                (double)(scrPlotHorizontal.getMaximum()-scrPlotHorizontal.getMinimum())
                *(double)50
                /((Integer)spinZoom.getValue()).doubleValue()
                +0.5
                ));
        
        scrPlotVertical.setVisibleAmount((int)Math.floor(
                (double)(scrPlotVertical.getMaximum()-scrPlotVertical.getMinimum())
                *(double)50
                /((Integer)spinZoom.getValue()).doubleValue()
                +0.5
                ));
    }
    
    private final void setVerticalPanning(final int vPan){
        if(!scrPlotVertical.getValueIsAdjusting() && plot!=null){
            final double shift = (double)vPan/(scrPlotVertical.getMaximum()-
                    scrPlotVertical.getMinimum()-scrPlotHorizontal.getVisibleAmount())*2;
            plot.setVerticalShift(shift);
            if(sidePanel.getAutoRecalc() && !noRecalc)  {
                updatePlot(false, CHANGE_TYPE_HARD);
            }
            
        }
    }
    
    private final void setHorizontalPanning(final int hPan){
        if(!scrPlotHorizontal.getValueIsAdjusting() && plot!=null){
            final double shift = -(double)hPan/(scrPlotHorizontal.getMaximum()-
                    scrPlotHorizontal.getMinimum()-scrPlotHorizontal.getVisibleAmount())*2;
            plot.setHorizontalShift(shift);
            if(sidePanel.getAutoRecalc() && !noRecalc)  {
                updatePlot(false, CHANGE_TYPE_HARD);
            }
            
        }
    }
    
    final void updatePlot(){
        updatePlot(true, CHANGE_TYPE_HARD);
    }
    
    abstract void updatePlot(boolean doSnapshot, int changeType);
    
    abstract void probablyDoSnapshot();
    
    abstract void restore(SnapshotContainer snapC);
    
    abstract void plotClassifier(boolean hardChange);
    
    
    
    private final void showConfusionMatrix(){
        //TODO: The dialog is recreated if it is not showing. is this necessary?
        if (classifier.isReady()){
            if (confMxDialog == null){
                confMxDialog = new ConfusionMatrixWindow(classifier.getConfusionMatrix(), classifier.getClassNames());
                subWindows.add(confMxDialog);
                confMxDialog.setAccuracy(classifier.getAccuracy());
            } else{
                //TODO: do this with events
                confMxDialog.setAccuracy(classifier.getAccuracy());
                confMxDialog.setData(classifier.getConfusionMatrix());
            }
            confMxDialog.show();
        } else {
            ErrorDialog.show(this,"SVM not trained.");
        }
        
    }
    
    /**
     * Before disposing this method notifies WindowManager.
     */
    public final void dispose() {
        dispose(true);
    }
    
    final void dispose(boolean notify){
        for (final Enumeration en = subWindows.elements(); en.hasMoreElements();)  {
            ((JFrame)en.nextElement()).dispose();
        }
        
        if (notify) {
            WindowManager.closeWindow(this);
        }
        super.dispose();
    }
    
    final void setSidePanel(final SidePanel span){
        if (sidePanel!=null)  {
            getContentPane().remove(sidePanel);
        }
        
        getContentPane().add(span,0);
        sidePanel=span;
        pack();
    }
    
    void addDisplayMenuItem(Component mitem){
        m_Display.add(mitem);
    }
    
    
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        bgrKernels = new javax.swing.ButtonGroup();
        brgTypes = new javax.swing.ButtonGroup();
        panPlots = new javax.swing.JPanel();
        jPanel6 = new javax.swing.JPanel();
        lblPlot = new javax.swing.JLabel();
        scrPlotVertical = new javax.swing.JScrollBar();
        scrPlotHorizontal = new javax.swing.JScrollBar();
        jPanel7 = new javax.swing.JPanel();
        butPlot = new javax.swing.JButton();
        lblZoom = new javax.swing.JLabel();
        spinZoom = new javax.swing.JSpinner();
        butSavePlot = new javax.swing.JButton();
        jMenuBar2 = new javax.swing.JMenuBar();
        m_File = new javax.swing.JMenu();
        m_FileNew = new javax.swing.JMenuItem();
        m_FileOpenDataset = new javax.swing.JMenuItem();
        m_FileOpenedDatasets = new javax.swing.JMenuItem();
        m_FilePreferences = new javax.swing.JMenuItem();
        m_FileClose = new javax.swing.JMenuItem();
        m_FileExit = new javax.swing.JMenuItem();
        m_Display = new javax.swing.JMenu();
        m_DisplayConfusionMatrix = new javax.swing.JCheckBoxMenuItem();
        m_DisplayDataInPlot = new javax.swing.JCheckBoxMenuItem();
        m_DisplaySnapshots = new javax.swing.JCheckBoxMenuItem();
        m_Snapshots = new javax.swing.JMenu();
        m_SnapshotsDoSnapshots = new javax.swing.JCheckBoxMenuItem();

        getContentPane().setLayout(new javax.swing.BoxLayout(getContentPane(), javax.swing.BoxLayout.X_AXIS));

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        panPlots.setLayout(new java.awt.BorderLayout());

        panPlots.setBorder(new javax.swing.border.TitledBorder("Plots"));
        jPanel6.setLayout(new java.awt.BorderLayout());

        lblPlot.setMinimumSize(new java.awt.Dimension(300, 300));
        lblPlot.setPreferredSize(new java.awt.Dimension(400, 400));
        lblPlot.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                lblPlotComponentResized(evt);
            }
        });

        jPanel6.add(lblPlot, java.awt.BorderLayout.CENTER);

        scrPlotVertical.setMinimum(-100);
        scrPlotVertical.setValue(-50);
        scrPlotVertical.setVisibleAmount(100);
        jPanel6.add(scrPlotVertical, java.awt.BorderLayout.EAST);

        scrPlotHorizontal.setMinimum(-100);
        scrPlotHorizontal.setOrientation(javax.swing.JScrollBar.HORIZONTAL);
        scrPlotHorizontal.setValue(-50);
        scrPlotHorizontal.setVisibleAmount(100);
        jPanel6.add(scrPlotHorizontal, java.awt.BorderLayout.SOUTH);

        panPlots.add(jPanel6, java.awt.BorderLayout.CENTER);

        butPlot.setText("Plot");
        butPlot.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butPlotActionPerformed(evt);
            }
        });

        jPanel7.add(butPlot);

        lblZoom.setText("Zoom:");
        jPanel7.add(lblZoom);

        spinZoom.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinZoomStateChanged(evt);
            }
        });

        jPanel7.add(spinZoom);

        butSavePlot.setText("Save plot");
        butSavePlot.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butSavePlotActionPerformed(evt);
            }
        });

        jPanel7.add(butSavePlot);

        panPlots.add(jPanel7, java.awt.BorderLayout.SOUTH);

        getContentPane().add(panPlots);

        m_File.setMnemonic('f');
        m_File.setText("File");
        m_FileNew.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        m_FileNew.setMnemonic('n');
        m_FileNew.setText("New");
        m_FileNew.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_FileNewActionPerformed(evt);
            }
        });

        m_File.add(m_FileNew);

        m_FileOpenDataset.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        m_FileOpenDataset.setMnemonic('o');
        m_FileOpenDataset.setText("Open dataset...");
        m_FileOpenDataset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_FileOpenDatasetActionPerformed(evt);
            }
        });

        m_File.add(m_FileOpenDataset);

        m_FileOpenedDatasets.setMnemonic('s');
        m_FileOpenedDatasets.setText("Show open datasets...");
        m_FileOpenedDatasets.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_FileOpenedDatasetsActionPerformed(evt);
            }
        });

        m_File.add(m_FileOpenedDatasets);

        m_FilePreferences.setMnemonic('p');
        m_FilePreferences.setText("Preferences");
        m_FilePreferences.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_FilePreferencesActionPerformed(evt);
            }
        });

        m_File.add(m_FilePreferences);

        m_FileClose.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        m_FileClose.setMnemonic('c');
        m_FileClose.setText("Close");
        m_FileClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_FileCloseActionPerformed(evt);
            }
        });

        m_File.add(m_FileClose);

        m_FileExit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        m_FileExit.setMnemonic('x');
        m_FileExit.setText("Exit");
        m_FileExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_FileExitActionPerformed(evt);
            }
        });

        m_File.add(m_FileExit);

        jMenuBar2.add(m_File);

        m_Display.setMnemonic('d');
        m_Display.setText("Display");
        m_DisplayConfusionMatrix.setMnemonic('c');
        m_DisplayConfusionMatrix.setText("ConfusionMatrix");
        m_DisplayConfusionMatrix.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_DisplayConfusionMatrixActionPerformed(evt);
            }
        });

        m_Display.add(m_DisplayConfusionMatrix);

        m_DisplayDataInPlot.setMnemonic('d');
        m_DisplayDataInPlot.setSelected(true);
        m_DisplayDataInPlot.setText("Show datapoints in plot");
        m_DisplayDataInPlot.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_DisplayDataInPlotActionPerformed(evt);
            }
        });

        m_Display.add(m_DisplayDataInPlot);

        m_DisplaySnapshots.setMnemonic('s');
        m_DisplaySnapshots.setText("Display Snapshots");
        m_DisplaySnapshots.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_DisplaySnapshotsActionPerformed(evt);
            }
        });

        m_Display.add(m_DisplaySnapshots);

        jMenuBar2.add(m_Display);

        m_Snapshots.setMnemonic('s');
        m_Snapshots.setText("Snapshots");
        m_SnapshotsDoSnapshots.setMnemonic('c');
        m_SnapshotsDoSnapshots.setSelected(true);
        m_SnapshotsDoSnapshots.setText("Create snapshots");
        m_Snapshots.add(m_SnapshotsDoSnapshots);

        jMenuBar2.add(m_Snapshots);

        setJMenuBar(jMenuBar2);

        pack();
    }
    // </editor-fold>//GEN-END:initComponents
    
    private void m_FileOpenedDatasetsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_FileOpenedDatasetsActionPerformed
        ChooseDatasetDialog cdd = new ChooseDatasetDialog(this);
        cdd.show();
        Data dataset = cdd.getSelectedDataset();
        if (dataset!=null){
            WindowManager.newWindow(dataset);
        }
    }//GEN-LAST:event_m_FileOpenedDatasetsActionPerformed
    
    private void m_FilePreferencesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_FilePreferencesActionPerformed
        if(prefd==null){
            ErrorDialog.show(this,"No preferences available.");
        } else {
            prefd.show();
        }
    }//GEN-LAST:event_m_FilePreferencesActionPerformed
    
    private final void lblPlotComponentResized(final java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_lblPlotComponentResized
        resizePlotTask.cancel();
        resizePlotTask = new ResizePlotTask(this);
        resizePlotTimer.schedule(resizePlotTask, 300);
    }//GEN-LAST:event_lblPlotComponentResized
    
    private final void m_FileExitActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_FileExitActionPerformed
        if (WindowManager.getNumberOfWindows()>1){
            if(JOptionPane.showConfirmDialog(
                    this,
                    "This will close all opened windows. Do you really want to exit this application?",
                    "Really close all windows?",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION){
                System.exit(0);
            }
        } else {
            System.exit(0);
        }
    }//GEN-LAST:event_m_FileExitActionPerformed
    
    private final void m_FileCloseActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_FileCloseActionPerformed
        WindowManager.closeWindow(this);
    }//GEN-LAST:event_m_FileCloseActionPerformed
    
    private final void m_FileOpenDatasetActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_FileOpenDatasetActionPerformed
//TODO: shouldn't do the same thing as File-New.
        WindowManager.newWindow();
    }//GEN-LAST:event_m_FileOpenDatasetActionPerformed
    
    private final void m_FileNewActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_FileNewActionPerformed
        WindowManager.newWindow();
    }//GEN-LAST:event_m_FileNewActionPerformed
    
    private final void m_DisplaySnapshotsActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_DisplaySnapshotsActionPerformed
        if (m_DisplaySnapshots.getState()) {
            snapPan.setPreferredSize(new Dimension(120, getContentPane().getHeight()));
            getContentPane().add(snapPan);
        } else  {
            getContentPane().remove(snapPan);
        }
        
        pack();
    }//GEN-LAST:event_m_DisplaySnapshotsActionPerformed
    
    private final void m_DisplayDataInPlotActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_DisplayDataInPlotActionPerformed
        if (plot!=null){
            final boolean showDataInPlot;
            if((showDataInPlot=m_DisplayDataInPlot.getState())!=plot.getShowDataInPlot()){
                plot.setShowDataInPlot(showDataInPlot);
                updatePlot(false, CHANGE_TYPE_SOFT);
            }
        }
    }//GEN-LAST:event_m_DisplayDataInPlotActionPerformed
    
    private final void m_DisplayConfusionMatrixActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_DisplayConfusionMatrixActionPerformed
        if (m_DisplayConfusionMatrix.getState()){
            if (classifier.isReady())  {
                showConfusionMatrix();
            }
            
            else {
                //TODO: check ...
                ErrorDialog.show(this, "SVM not trained.");
                m_DisplayConfusionMatrix.setState(false);
            }
        } else if (confMxDialog!=null)  {
            confMxDialog.hide();
        }
        
    }//GEN-LAST:event_m_DisplayConfusionMatrixActionPerformed
    
    private final void butSavePlotActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butSavePlotActionPerformed
        if(plot!=null){
            final SaveAsDialog sad = new SaveAsDialog();
            if(sad.showSaveAsDialog(SaveAsDialog.EXTENSIONS_PLOT)) {
                plot.saveAs(sad.getSelectedFile());
            }
        } else  {
            ErrorDialog.show(this,"SVM not plotted yet.");
        }
        
    }//GEN-LAST:event_butSavePlotActionPerformed
    
    private final void spinZoomStateChanged(final javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinZoomStateChanged
        if(spinZoom.getValue().equals(new Integer(0)))  {
            spinZoom.getModel().setValue(new Integer(1));
        }
        
        if (plot!=null) {
            plot.setZoom(((Integer)spinZoom.getValue()).doubleValue()/100);
            updatePlot(false, CHANGE_TYPE_HARD);
        }
        updateVisibleAmount();
    }//GEN-LAST:event_spinZoomStateChanged
    
    private final void butPlotActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butPlotActionPerformed
        //TODO: not always true! not always hardChange!
        plotClassifier(true);
    }//GEN-LAST:event_butPlotActionPerformed
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup bgrKernels;
    private javax.swing.ButtonGroup brgTypes;
    private javax.swing.JButton butPlot;
    private javax.swing.JButton butSavePlot;
    private javax.swing.JMenuBar jMenuBar2;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JLabel lblPlot;
    private javax.swing.JLabel lblZoom;
    private javax.swing.JMenu m_Display;
    private javax.swing.JCheckBoxMenuItem m_DisplayConfusionMatrix;
    private javax.swing.JCheckBoxMenuItem m_DisplayDataInPlot;
    private javax.swing.JCheckBoxMenuItem m_DisplaySnapshots;
    private javax.swing.JMenu m_File;
    private javax.swing.JMenuItem m_FileClose;
    private javax.swing.JMenuItem m_FileExit;
    private javax.swing.JMenuItem m_FileNew;
    private javax.swing.JMenuItem m_FileOpenDataset;
    private javax.swing.JMenuItem m_FileOpenedDatasets;
    private javax.swing.JMenuItem m_FilePreferences;
    private javax.swing.JMenu m_Snapshots;
    private javax.swing.JCheckBoxMenuItem m_SnapshotsDoSnapshots;
    private javax.swing.JPanel panPlots;
    private javax.swing.JScrollBar scrPlotHorizontal;
    private javax.swing.JScrollBar scrPlotVertical;
    private javax.swing.JSpinner spinZoom;
    // End of variables declaration//GEN-END:variables
    
    
    final Plot getPlot() {
        return this.plot;
    }
    
    void setPlot(final Plot plot) {
        this.plot = plot;
    }
    
    final Dimension getPreferredPlotSize(){
        return lblPlot.getSize();
    }
    
    final void setPlotGraphic(final ImageIcon plotGraphic){
        lblPlot.setIcon(plotGraphic);
    }
    
    final boolean getDoSnapshots(){
        return m_SnapshotsDoSnapshots.getState();
    }
    
    final void addSnaphot(final SnapshotContainer snapC){
        snapPan.addSnapshot(snapC);
    }
    
    final void updateConfusionMatrix(){
        //TODO: do this with events
        if (confMxDialog!=null){
            confMxDialog.setAccuracy(classifier.getAccuracy());
            confMxDialog.setData(classifier.getConfusionMatrix());
        }
    }
    
    /**
     * Sets the checkBoxMenus' states according to plot.
     */
    void updateCheckBoxMenus(){
        if(plot!=null){
            m_DisplayDataInPlot.setState(plot.getShowDataInPlot());
        }
    }
    
    /**
     * Sets plot fields according to checkBoxMenus' states.
     */
    void adjustPlotToCheckBoxMenus(Plot newPlot){
        newPlot.setShowDataInPlot(m_DisplayDataInPlot.getState());
    }
    
    public PreferencesDialog getPreferencesDialog() {
        return this.prefd;
    }
    
    public void setPreferencesDialog(final PreferencesDialog prefd) {
        this.prefd = prefd;
    }
    
    public void preferenceChange(java.util.prefs.PreferenceChangeEvent evt) {
        if(plot!=null && plot instanceof ContourPlot)
            ((ContourPlot)plot).setGrid(prefd.getGrid());
        updatePlot(false, CHANGE_TYPE_HARD);
    }
    
    
    /**
     * JFrame doesn't enforce its minimum size. This is a workaround.
     * Should be removed as soon as that bug is fixed.
     * See {@link http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4320050}.
     */
    private void lockInMinSize(final JFrame frame) {
        //Ensures user cannot resize frame to be smaller than frame is right now.
        final int origX = frame.getSize().width;
        final int origY = frame.getSize().height;
        frame.addComponentListener(new
                java.awt.event.ComponentAdapter() {
            public void componentResized(ComponentEvent event) {
                frame.setSize(
                        (frame.getWidth() < origX) ? origX :
                            frame.getWidth(),
                        (frame.getHeight() < origY) ? origY :
                            frame.getHeight());
                frame.repaint();
            }
        });
    }
}
