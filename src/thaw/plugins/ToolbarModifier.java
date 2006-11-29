package thaw.plugins;

import java.util.Vector;
import javax.swing.JButton;

import thaw.core.Logger;
import thaw.core.MainWindow;

/**
 * Not a plugin ! Just an helper for the plugins !
 */
public class ToolbarModifier {
	private MainWindow mainWindow = null;

	private Vector buttons; /* JButtons */

	private boolean areDisplayed = false;

	public ToolbarModifier() {
		buttons = new Vector();
		areDisplayed = false;
	}

	public ToolbarModifier(MainWindow toolbarTarget) {
		this();
		setMainWindow(toolbarTarget);
	}


	public void setMainWindow(MainWindow target) {
		this.mainWindow = target;
		this.mainWindow.resetLastKnowToolBarModifier();
	}

	public void addButtonToTheToolbar(JButton button) {
		buttons.add(button);

		if (areDisplayed)
			displayButtonsInTheToolbar();
	}

	public void removeButtonFromTheToolbar(JButton button) {
		buttons.remove(button);

		if (areDisplayed)
			displayButtonsInTheToolbar();
	}

	public void displayButtonsInTheToolbar() {
		if (mainWindow != null) {
			if (buttons.size() == 0) {
				Logger.notice(this, "No button to display ?");
			}

			mainWindow.changeButtonsInTheToolbar(this, buttons);
			areDisplayed = true;
		} else
			Logger.error(this, "MainWindow not SET !");
	}

	public void hideButtonsInTheToolbar() {
		if (mainWindow != null) {
			if (areDisplayed) {
				mainWindow.changeButtonsInTheToolbar(this, null);
				areDisplayed = false;
			}
		} else
			Logger.error(this, "MainWindow not SET !");
	}
}
