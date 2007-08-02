package thaw.plugins.miniFrost;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.JButton;

import java.util.Observer;
import java.util.Observable;

import java.awt.GridLayout;
import java.awt.BorderLayout;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import thaw.core.Config;
import thaw.core.ConfigWindow;
import thaw.core.I18n;
import thaw.core.Logger;
import thaw.gui.IconBox;

import thaw.plugins.MiniFrost;


public class MiniFrostConfigTab implements Observer, ActionListener {

	private Config config;
	private ConfigWindow configWindow;
	private RegexpBlacklist regexpBlacklist;

	private JPanel panel;

	public final static int MIN_BOARDS = 0;
	public final static int MAX_BOARDS = 30;

	private JComboBox maxBoards;

	public final static int MIN_DAYS = 0;
	public final static int MAX_DAYS = 365;

	private JComboBox archiveAfter;
	private JComboBox deleteAfter;


	private JButton regexpButton;


	public MiniFrostConfigTab(Config config,
				  ConfigWindow configWindow,
				  RegexpBlacklist regexpBlacklist) {
		this.config = config;
		this.configWindow = configWindow;
		this.regexpBlacklist = regexpBlacklist;

		panel = new JPanel(new GridLayout(16, 1));

		maxBoards = new JComboBox();

		for (int i = MIN_BOARDS ; i <= MAX_BOARDS ; i++)
			maxBoards.addItem(Integer.toString(i));

		archiveAfter = new JComboBox();
		deleteAfter = new JComboBox();

		for (int i = MIN_DAYS ; i <= MAX_DAYS ; i++) {
			archiveAfter.addItem(Integer.toString(i)+ " "+I18n.getMessage("thaw.plugin.miniFrost.days"));
			deleteAfter.addItem( Integer.toString(i)+ " "+I18n.getMessage("thaw.plugin.miniFrost.days"));
		}

		selectValue();

		panel.add(new JLabel(I18n.getMessage("thaw.plugin.miniFrost.maxBoardsRefreshed")));
		panel.add(maxBoards);

		panel.add(new JLabel(I18n.getMessage("thaw.plugin.miniFrost.archiveAfter")));
		panel.add(archiveAfter);

		panel.add(new JLabel(I18n.getMessage("thaw.plugin.miniFrost.deleteAfter")));
		panel.add(deleteAfter);

		JPanel regexpPanel = new JPanel(new BorderLayout());
		regexpPanel.add(new JLabel(""), BorderLayout.CENTER);
		regexpButton = new JButton(I18n.getMessage("thaw.plugin.miniFrost.modifyRegexp"));
		regexpButton.addActionListener(this);
		regexpPanel.add(regexpButton, BorderLayout.EAST);

		panel.add(new JLabel(""));
		panel.add(regexpPanel);

		configWindow.addObserver(this);
	}


	public void display() {
		configWindow.addTab(I18n.getMessage("thaw.plugin.miniFrost"),
				    thaw.gui.IconBox.minReadComments,
				    panel);
	}


	public void hide() {
		configWindow.removeTab(panel);
	}

	private void selectValue() {

		int max;

		if (config.getValue("miniFrostAutoRefreshMaxBoards") != null) {
			max = Integer.parseInt(config.getValue("miniFrostAutoRefreshMaxBoards"));
			Logger.info(this, "Max boards: "+Integer.toString(max));
		} else {
			max = AutoRefresh.DEFAULT_MAX_BOARDS_REFRESHING;
		}

		maxBoards.setSelectedIndex(max-MIN_BOARDS);


		if (config.getValue("miniFrostArchiveAfter") != null) {
			max = Integer.parseInt(config.getValue("miniFrostArchiveAfter"));
			Logger.info(this, "Archive after: "+Integer.toString(max));
		} else {
			max = MiniFrost.DEFAULT_ARCHIVE_AFTER;
		}

		archiveAfter.setSelectedIndex(max-MIN_DAYS);


		if (config.getValue("miniFrostDeleteAfter") != null) {
			max = Integer.parseInt(config.getValue("miniFrostDeleteAfter"));
			Logger.info(this, "Delete after: "+Integer.toString(max));
		} else {
			max = MiniFrost.DEFAULT_DELETE_AFTER;
		}

		deleteAfter.setSelectedIndex(max-MIN_DAYS);
	}


	private String extractNumber(JComboBox box) {
		String[] split = ((String)box.getSelectedItem()).split(" ");
		return split[0];
	}


	public void update(Observable o, Object param) {
		if (param == configWindow.getOkButton()) {

			config.setValue("miniFrostAutoRefreshMaxBoards",
					(String)maxBoards.getSelectedItem());

			config.setValue("miniFrostArchiveAfter",
					extractNumber(archiveAfter));

			config.setValue("miniFrostDeleteAfter",
					extractNumber(deleteAfter));

		} else if (param == configWindow.getCancelButton()) {

			selectValue();

		}
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == regexpButton) {
			regexpBlacklist.displayTab(configWindow);
		}
	}
}
