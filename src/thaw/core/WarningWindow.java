package thaw.core;

import javax.swing.JOptionPane;

import thaw.i18n.I18n;

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
						      I18n.getMessage("thaw.warning.title"),
						      JOptionPane.WARNING_MESSAGE);
		} else {
			JOptionPane.showMessageDialog(null,
						      warning,
						      I18n.getMessage("thaw.warning.title"),
						      JOptionPane.WARNING_MESSAGE);
		}
	}
}
