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

	public ToolbarModifier(final MainWindow toolbarTarget) {
		this();
		setMainWindow(toolbarTarget);
	}

	public void setMainWindow(final MainWindow target) {
		mainWindow = target;
		mainWindow.resetLastKnowToolBarModifier();
	}

	public void addButtonToTheToolbar(final JButton button) {
		addButtonToTheToolbar(button, -1);
	}

	/**
	 * @param position if == -1, then the button is put at the end
	 */
	public void addButtonToTheToolbar(final JButton button, int position) {
		if (position < 0)
			buttons.add(button);
		else {
			if (position > buttons.size())
				buttons.setSize(position);
			buttons.add(position, button);
		}

		if (areDisplayed) {
			areDisplayed = false;
			displayButtonsInTheToolbar();
		}
	}

	public void removeButtonFromTheToolbar(final JButton button) {
		buttons.remove(button);

		if (areDisplayed) {
			areDisplayed = false;
			displayButtonsInTheToolbar();
		}
	}

	public void displayButtonsInTheToolbar() {
		if (mainWindow != null) {
			if (areDisplayed && mainWindow.getLastToolbarModifier() == this) {
				Logger.warning(this, "Already displayed !");
				return;
			}

			if (buttons == null || buttons.size() == 0) {
				Logger.notice(this, "No button to display ?");
				mainWindow.changeButtonsInTheToolbar(this, null);
			} else
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

	/**
	 * Don't forget to call it when the plugin is stopped !
	 */
	public void purgeButtonList() {
		hideButtonsInTheToolbar();
		buttons = new Vector();
	}
}
