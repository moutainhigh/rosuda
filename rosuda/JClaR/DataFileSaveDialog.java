/*
 * DataFileSaveDialog.java
 *
 * Created on 22. September 2005, 14:11
 *
 */

package org.rosuda.JClaR;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.rosuda.JRclient.RSrvException;

/**
 *  Implementation of a file-dialog which allows saving datasets by choosing several options.
 *  Originally called JGRDataFileSaveDialog
 *  @author Markus Helbig
 *      RoSuDa 2003 - 2005
 *
 *  @author tobias
 */

public class DataFileSaveDialog extends JFileChooser implements ActionListener, ItemListener {
    
    private JCheckBox append = new JCheckBox("append",false);
    private JCheckBox quote = new JCheckBox("quote",false);
    private JCheckBox rownames = new JCheckBox("row.names",false);
    
    private JButton ok = new JButton("Save");
    private JButton cancel = new JButton("Cancel");
    
    private JComboBoxExt sepsBox = new JComboBoxExt(new String[] {"\\t","blank",",",";","|","Others..."});
    private String[] seps = new String[] {"\\t"," ",",",";","|"};
    
    private String data;
    
    //private Dimension screenSize = Common.getScreenRes();
    
    /**
     * Create a new Save-filedialog.
     * @param f parent frame
     * @param data name of dataset which should be saved
     * @param directory current directory
     */
    public DataFileSaveDialog(Frame f, String data, String directory) {
        this.setDialogTitle("Save DatFile - "+data);
        if (directory != null && new File(directory).exists()) this.setCurrentDirectory(new File(directory));
        this.data = data;
        this.addActionListener(this);
        
        sepsBox.setMinimumSize(new Dimension(90,22));
        sepsBox.setPreferredSize(new Dimension(90,22));
        sepsBox.setMaximumSize(new Dimension(90,22));
        
        sepsBox.addItemListener(this);
        
        if (System.getProperty("os.name").startsWith("Window")) {
            JPanel fileview = (JPanel)((JComponent)((JComponent)this.getComponent(2)).getComponent(2)).getComponent(2);
            JPanel command = new JPanel(new FlowLayout(FlowLayout.LEFT));
            command.add(append);
            command.add(new JLabel("seps="));
            command.add(sepsBox);
            command.add(rownames);
            command.add(quote);
            
            fileview.add(command);
            JPanel pp = (JPanel) ((JComponent)((JComponent)this.getComponent(2)).getComponent(2)).getComponent(0);
            pp.add(new JPanel());
            this.setPreferredSize(new Dimension(655,450));
        } else {
            JPanel command = new JPanel(new FlowLayout(FlowLayout.LEFT));
            command.add(append);
            command.add(new JLabel("seps="));
            command.add(sepsBox);
            command.add(rownames);
            command.add(quote);
            
            JPanel filename = (JPanel) this.getComponent(this.getComponentCount()-1);
            filename.add(command,filename.getComponentCount()-1);
            this.setPreferredSize(new Dimension(550,450));
        }
        this.showSaveDialog(f);
    }
    
    
    /**
     * Save dataset to choosen file, with specified options.
     */
    public void saveFile() {
        if (this.getSelectedFile() != null) {
            //JGR.directory = this.getCurrentDirectory().getAbsolutePath()+File.separator;
            String file = this.getSelectedFile().toString();
            
            String useSep;
            if (sepsBox.getSelectedIndex() >= seps.length) useSep = sepsBox.getSelectedItem().toString();
            else useSep = seps[sepsBox.getSelectedIndex()];
            
            String cmd = "write.table("+data+",\""+file.replace('\\','/')+"\",append="+(append.isSelected()?"T":"F")+",quote="+(quote.isSelected()?"T":"F")+",sep=\""+useSep+"\""+",row.names="+(rownames.isSelected()?"T":"F")+")";
            try{
                RserveConnection.getRconnection().voidEval(cmd);
            } catch (RSrvException rse){
                ErrorDialog.show(this,rse,"DataFileSaveDialog.saveFile()");
            }
            
            Main.setLast_directory(this.getSelectedFile().getParent());
        }
    }
    
    /**
     * actionPerformed: handle action event: menus.
     */
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if (cmd == "ApproveSelection") saveFile();
    }
    
    /**
     * itemStateChanged: handle item state changed, set separator box editable if "Others..." is choosen.
     */
    public void itemStateChanged(ItemEvent e) {
        if (e.getSource() == sepsBox)
            sepsBox.setEditable((sepsBox.getSelectedIndex() == sepsBox.getItemCount()-1?true:false));
    }
}
