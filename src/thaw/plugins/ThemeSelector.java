package thaw.plugins;

import javax.swing.JPanel;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

import javax.swing.UIManager;

import java.awt.BorderLayout;

import java.util.Observer;
import java.util.Observable;

import java.util.Vector;

import thaw.core.I18n;
import thaw.core.Logger;
import thaw.core.Core;


public class ThemeSelector implements thaw.core.Plugin, Observer, ListSelectionListener {

	private Core core;

	private JPanel panel = null;
	private JList themeList = null;
	private Vector themes = null;

	public ThemeSelector() {

	}

	public Vector getPossibleThemes() {
		Vector list = new Vector();

		final UIManager.LookAndFeelInfo[] feels =
			UIManager.getInstalledLookAndFeels();

		for (int i = 0; i < feels.length; i++) {
		        list.add(feels[i].getClassName());
		}

		return list;
	}

	public boolean run(Core core) {
		this.core = core;

		panel = new JPanel(new BorderLayout(5, 5));
		themeList = new JList(themes = getPossibleThemes());
		JLabel label = new JLabel(I18n.getMessage("thaw.plugin.themeSelector.selectATheme"));

		themeList.addListSelectionListener(this);

		panel.add(label, BorderLayout.NORTH);
		panel.add(new JScrollPane(themeList), BorderLayout.CENTER);

		resetSelection();

		core.getConfigWindow().addObserver(this);

		core.getConfigWindow().addTab(I18n.getMessage("thaw.plugin.themeSelector.theme"),
					      thaw.gui.IconBox.minLookAndFeel,
					      panel);

		return true;
	}


	public boolean stop() {
		core.getConfigWindow().removeTab(panel);

		return false;
	}

	public String getNameForUser() {
		return I18n.getMessage("thaw.plugin.themeSelector.themeSelector");
	}


	public javax.swing.ImageIcon getIcon() {
		return thaw.gui.IconBox.lookAndFeel;
	}


	public void resetSelection() {
		String theme = core.getConfig().getValue("lookAndFeel");

		if (theme == null)
			theme = UIManager.getSystemLookAndFeelClassName();

		themeList.setSelectedValue(theme, true);
	}

	public void resetTheme() {
		String theme = core.getConfig().getValue("lookAndFeel");

		if (theme == null)
			theme = UIManager.getSystemLookAndFeelClassName();

		setTheme(core, theme);
	}


	public void update(Observable o, Object arg) {
		if (o == core.getConfigWindow()) {

			if (arg == core.getConfigWindow().getOkButton()) {
				if (themeList.getSelectedValue() != null) {
					core.getConfig().setValue("lookAndFeel",
								  ((String)themeList.getSelectedValue()));

					resetSelection();
				}

				return;
			}

			if (arg == core.getConfigWindow().getCancelButton()) {
				resetSelection();
				resetTheme();
				return;
			}
		}
	}


	public static void setTheme(Core core, String theme) {
		try {
			Logger.notice(core, "Setting theme : "+ theme);
			UIManager.setLookAndFeel(theme);
			javax.swing.SwingUtilities.updateComponentTreeUI(core.getMainWindow().getMainFrame());
			javax.swing.SwingUtilities.updateComponentTreeUI(core.getConfigWindow().getFrame());
		} catch(Exception exc) {
			Logger.error(new ThemeSelector(), "Error while changing theme : "+exc.toString());
		}
	}

	public void valueChanged(ListSelectionEvent e) {
		if (e.getFirstIndex() >= 0
		    && themes.get(e.getFirstIndex()) != null) {
			setTheme(core, ((String)themeList.getSelectedValue()));
		}
	}
}

