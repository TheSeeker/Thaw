package thaw.gui;

import javax.swing.JOptionPane;

import thaw.core.Core;
import thaw.core.I18n;

/**
 * Use to create a warning popup.
 * Currently this popup is simple as possible, but
 * in the future, it may become more complex, showing,
 * for example, last log messages.
 */
public class WarningWindow {


	public WarningWindow(final Core core,
			     final String warning)
	{
		if((core != null) && (core.getMainWindow() != null)) {
			JOptionPane.showMessageDialog(core.getMainWindow().getMainFrame(),
						      warning,
						      "Thaw - "+I18n.getMessage("thaw.warning.title"),
						      JOptionPane.WARNING_MESSAGE);
		} else {
			JOptionPane.showMessageDialog(null,
						      warning,
						      "Thaw - "+I18n.getMessage("thaw.warning.title"),
						      JOptionPane.WARNING_MESSAGE);
		}
	}
}
