package thaw.gui;

import javax.swing.JOptionPane;
import java.awt.Component;

import thaw.core.Core;
import thaw.core.MainWindow;
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
		this(core != null ?
		     (core.getSplashScreen() != null && core.getSplashScreen().getDialog() != null ?
		      (Component)core.getSplashScreen().getDialog()
		      : (Component)core.getMainWindow().getMainFrame())
		     : (Component)null,
		     warning);
	}

	public WarningWindow(final MainWindow mainWindow, String warning) {
		this(mainWindow != null ? mainWindow.getMainFrame() : null, warning);
	}


	public WarningWindow(Component parent, String warning) {

		JOptionPane.showMessageDialog(parent,
					      warning,
					      "Thaw - "+I18n.getMessage("thaw.warning.title"),
					      JOptionPane.WARNING_MESSAGE);
	}
}
