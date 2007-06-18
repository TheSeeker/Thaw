package thaw.gui;

import javax.swing.JTabbedPane;
import javax.swing.Icon;
import java.awt.Component;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


import java.util.Vector;

import thaw.core.Logger;


public class TabbedPane extends JTabbedPane implements ChangeListener {
	private Vector tabNames;

	public TabbedPane() {
		super();
		tabNames = new Vector();
		super.addChangeListener(this);
	}


	public void addTab(final String tabName, final Icon icon,
			   final java.awt.Component panel) {
		tabNames.add(tabName);

		if (tabNames.size() > 1)
			super.addTab("", icon, panel);
		else
			super.addTab(tabName, icon, panel);

		int x = super.indexOfComponent(panel);

		super.setToolTipTextAt(x, tabName);
	}


	public void remove(Component panel) {
		int x = super.indexOfComponent(panel);

		if (x >= 0)
			tabNames.remove(x);
		else
			Logger.error(this, "remove(): Component not found ?");

		super.remove(panel);
	}

	public int indexOfTab(String tabName) {
		return tabNames.indexOf(tabName);
	}


	public void stateChanged(final ChangeEvent e) {
		int x = super.getSelectedIndex();
		int tabCount = super.getTabCount();

		for (int i = 0 ; i < tabCount ; i++) {
			if (i == x)
				super.setTitleAt(i, (String)tabNames.get(i));
			else
				super.setTitleAt(i, "");
		}
	}
}
