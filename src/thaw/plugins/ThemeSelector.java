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
import thaw.core.Core;
import thaw.core.ThawThread;
import thaw.core.ThawRunnable;


public class ThemeSelector implements thaw.core.Plugin, Observer, ListSelectionListener {

	private Core core;

	private JPanel panel = null;
	private JList themeList = null;
	private Vector themes = null;

	public final static String[] buggyLnf = new String[] {
		"com.sun.java.swing.plaf.gtk.GTKLookAndFeel"
	};

	public ThemeSelector() {

	}

	public static void addToVector(Vector v, String s) {
		for (int i = 0 ; i < buggyLnf.length ; i++) {
			if (buggyLnf[i].equals(s))
				s += " ("+I18n.getMessage("thaw.common.buggy")+")";
		}

		if (v.indexOf(s) < 0)
			v.add(s);
	}

	public static Vector getPossibleThemes() {
		Vector list = new Vector();

		final UIManager.LookAndFeelInfo[] feels =
			UIManager.getInstalledLookAndFeels();

		for (int i = 0; i < feels.length; i++) {
		        addToVector(list, feels[i].getClassName());
		}

		addToVector(list, "net.infonode.gui.laf.InfoNodeLookAndFeel");
		addToVector(list, "com.birosoft.liquid.LiquidLookAndFeel");

		return list;
	}

	public boolean run(Core core) {
		this.core = core;

		panel = new JPanel(new BorderLayout(5, 5));
		themeList = new JList(themes = getPossibleThemes());
		JLabel label = new JLabel(I18n.getMessage("thaw.plugin.themeSelector.selectATheme"));
		label.setIcon(thaw.gui.IconBox.lookAndFeel);

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


	public void stop() {
		core.getConfigWindow().deleteObserver(this);
		core.getConfigWindow().removeTab(panel);
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

		for (int i = 0 ; i < buggyLnf.length ; i++) {
			if (buggyLnf[i].equals(theme))
				theme += " ("+I18n.getMessage("thaw.common.buggy")+")";
		}

		themeList.setSelectedValue(theme, true);
	}

	public void resetTheme() {
		String theme = core.getConfig().getValue("lookAndFeel");

		if (theme == null)
			theme = UIManager.getSystemLookAndFeelClassName();

		Thread th = new ThawThread(new ThemeSetter(theme), "Theme setter", this);
		th.start();
	}


	public void update(Observable o, Object arg) {
		if (o == core.getConfigWindow()) {
			if (themeList.getSelectedValue() != null) {
				String[] str = ((String)themeList.getSelectedValue()).split(" ");
				core.getConfig().setValue("lookAndFeel",
							  str[0]);
				resetSelection();
			}

		}
	}


	private class ThemeSetter implements ThawRunnable {
		private String theme;

		public ThemeSetter(String t) {
			theme = t;
		}

		public void run() {
			core.setTheme(theme);
		}

		public void stop() { /* \_o< */ }
	}


	public void valueChanged(ListSelectionEvent e) {
		if (e.getFirstIndex() >= 0
		    && themes.get(e.getFirstIndex()) != null) {
			String[] str = ((String)themeList.getSelectedValue()).split(" ");
			Thread th = new ThawThread(new ThemeSetter(str[0]), "Theme setter", this);
			th.start();
		}
	}
}

