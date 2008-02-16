package thaw.core;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;

import javax.swing.JDialog;
import javax.swing.JLabel;

public class PleaseWaitDialog {
	private JDialog dialog;

	public PleaseWaitDialog(MainWindow mainWindow) {
		dialog = new JDialog(mainWindow.getMainFrame(),
							" "+I18n.getMessage("thaw.common.pleaseWait"));

		dialog.getContentPane().setLayout(new GridLayout(1, 1));
		dialog.getContentPane().add(new JLabel(I18n.getMessage("thaw.common.pleaseWait"),
				JLabel.CENTER));
		
//		dialog.setUndecorated(true);
//		dialog.setResizable(false);

		dialog.setSize(200, 75);

		Dimension screenSize =
			Toolkit.getDefaultToolkit().getScreenSize();

		Dimension dialogSize = dialog.getSize();
		dialog.setLocation(screenSize.width/2 - (dialogSize.width/2),
				   screenSize.height/2 - (dialogSize.height/2));

		dialog.setVisible(true);
		
		dialog.setSize(200, 75);

		dialogSize = dialog.getSize();
		dialog.setLocation(screenSize.width/2 - (dialogSize.width/2),
				   screenSize.height/2 - (dialogSize.height/2));
	}
	
	public JDialog getDialog() {
		return dialog;
	}
	
	public void dispose() {
		dialog.setVisible(false);
		dialog.dispose();
	}
}
