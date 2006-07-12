package thaw.core;

import javax.swing.JOptionPane;


/**
 * Use to create a warning popup.
 * Currently this popup is simple as possible, but
 * in the future, it may become more complex, showing,
 * for example, last log messages.
 */
public class WarningWindow {


	public WarningWindow(Core core,
			     String warning)
	{
		if(core != null && core.getMainWindow() != null) {
			JOptionPane.showMessageDialog(core.getMainWindow().getMainFrame(),
						      warning,
						      "Warning",
						      JOptionPane.WARNING_MESSAGE);
		} else {
			JOptionPane.showMessageDialog(null,
						      warning,
						      "Warning",
						      JOptionPane.WARNING_MESSAGE);
		}
	}
}
