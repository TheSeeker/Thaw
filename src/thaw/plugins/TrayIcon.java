package thaw.plugins;

import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowListener;
import java.awt.event.WindowEvent;

import thaw.core.Core;
import thaw.core.Logger;
import thaw.core.I18n;

import thaw.gui.SysTrayIcon;


public class TrayIcon implements thaw.core.Plugin, MouseListener, WindowListener {
	private Core core;
	private SysTrayIcon icon;


	public TrayIcon() {

	}


	public boolean run(Core core) {
		this.core = core;

		icon = new SysTrayIcon(thaw.gui.IconBox.blueBunny);
		icon.setToolTip("Thaw "+thaw.core.Main.VERSION);
		icon.addMouseListener(this);

		core.getMainWindow().addWindowListener(this);

		icon.setVisible(true);

		return true;
	}


	public boolean stop() {
		core.getMainWindow().addWindowListener(this);
		icon.removeMouseListener(this);

		icon.setVisible(false);

		return true;
	}

	public String getNameForUser() {
		return I18n.getMessage("thaw.plugin.trayIcon.pluginName");
	}

	public javax.swing.ImageIcon getIcon() {
		return thaw.gui.IconBox.blueBunny;
	}

	public void switchMainWindowVisibility() {
		boolean v = !core.getMainWindow().isVisible();

		core.getMainWindow().setNonIconified();

		core.getMainWindow().setVisible(v);

		core.getMainWindow().setNonIconified();
	}

	public void windowActivated(WindowEvent e) { }
	public void windowClosed(WindowEvent e) { }
	public void windowClosing(WindowEvent e) { }
	public void windowDeactivated(WindowEvent e) { }
	public void windowDeiconified(WindowEvent e) { }

        public void windowIconified(WindowEvent e) {
		switchMainWindowVisibility();
	}

	public void windowOpened(WindowEvent e) { }


	public void mouseClicked(MouseEvent e) {
		switchMainWindowVisibility();
	}

	public void mouseEntered(MouseEvent e) { }
	public void mouseExited(MouseEvent e) { }
	public void mousePressed(MouseEvent e) { }
	public void mouseReleased(MouseEvent e) { }
}
