import java.awt.*;
import java.awt.event.*;

/** Simple help frame that displays list of available shortcuts and their descriptions.
    @version $Id$
*/
public class HelpFrame extends TFrame implements ActionListener {
    
    public HelpFrame() {
	super("Help");
	setLayout(new BorderLayout());
	TextArea t=new TextArea();
	t.setText("Shortcuts for commands in tree window:\n\nTool modes:\n"+
		 "<e>        - sElect cases\n"+
		 "<z>        - Zoom\n"+
		 "<v>        - moVe (or hold <space> key for temporary pan mode)\n"+
		 "<n>        - Node select\n\nOther commands:\n"+
		 "<c>        - toggle type of Connecting lines\n"+
		 "<d>        - toggle Deviance display\n"+
		 "<f>        - toggle Final node alignment\n"+
		 "<h>        - Help\n"+
		 "<l>        - toggle Labels\n"+
		 "<m>        - tree Map\n"+
		 "<shift><n> - new tree\n"+
		 "<o>        - Open file\n"+
		 "<p>        - Prune\n"+
		 "<shift><p> - Print\n"+
		 "<q>        - Quit\n"+
		 "<r>        - Re-arrange nodes\n"+
		 "<shift><r> - Rotate\n"+
		 "<s>        - toggle node Size (fixed/porportional)\n"+
		 "<+>/<->    - change deviance zoom\n");
		 
	add(t);
	t.setEditable(false);
	t.setBackground(Color.white);
	Panel p=new Panel();	
	add(p,BorderLayout.SOUTH);
	p.setLayout(new FlowLayout());
	Button b=new Button("Close");
	p.add(b);
	b.addActionListener(this);	
	addWindowListener(Common.defaultWindowListener);
    };

    public void actionPerformed(ActionEvent e) {	
	dispose();
	removeAll();
	WinTracker.current.rm(this);
    };
};
