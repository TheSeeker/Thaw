package thaw.plugins.miniFrost;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;

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

	private JPanel globalPanel;

	public final static int MIN_BOARDS = 0;
	public final static int MAX_BOARDS = 30;

	private JComboBox maxBoards;

	public final static int MIN_DAYS = 0;
	public final static int MAX_DAYS = 365;

	private JComboBox archiveAfter;
	private JComboBox deleteAfter;


	private JButton regexpButton;

	private JRadioButton gmailView;
	private JRadioButton outlookView;

	private JCheckBox treeCheckBox;

	public MiniFrostConfigTab(Config config,
			ConfigWindow configWindow,
			RegexpBlacklist regexpBlacklist) {
		this.config = config;
		this.configWindow = configWindow;
		this.regexpBlacklist = regexpBlacklist;

		globalPanel = new JPanel(new BorderLayout(10, 10));

		JPanel panel = new JPanel(new GridLayout(10, 1));

		maxBoards = new JComboBox();

		for (int i = MIN_BOARDS ; i <= MAX_BOARDS ; i++)
			maxBoards.addItem(Integer.toString(i));

		archiveAfter = new JComboBox();
		deleteAfter = new JComboBox();

		for (int i = MIN_DAYS ; i <= MAX_DAYS ; i++) {
			archiveAfter.addItem(Integer.toString(i)+ " "+I18n.getMessage("thaw.plugin.miniFrost.days"));
			deleteAfter.addItem( Integer.toString(i)+ " "+I18n.getMessage("thaw.plugin.miniFrost.days"));
		}
		
		treeCheckBox = new JCheckBox(I18n.getMessage("thaw.plugin.miniFrost.seeTree"));

		panel.add(new JLabel(I18n.getMessage("thaw.plugin.miniFrost.maxBoardsRefreshed")));
		panel.add(maxBoards);

		panel.add(new JLabel(I18n.getMessage("thaw.plugin.miniFrost.archiveAfter")));
		panel.add(archiveAfter);

		panel.add(new JLabel(I18n.getMessage("thaw.plugin.miniFrost.deleteAfter")));
		panel.add(deleteAfter);
		
		panel.add(new JLabel(""));
		panel.add(treeCheckBox);
		panel.add(new JLabel(""));

		JPanel regexpPanel = new JPanel(new BorderLayout());
		regexpPanel.add(new JLabel(""), BorderLayout.CENTER);
		regexpButton = new JButton(I18n.getMessage("thaw.plugin.miniFrost.modifyRegexp"));
		regexpButton.addActionListener(this);
		regexpPanel.add(regexpButton, BorderLayout.EAST);

		panel.add(new JLabel(""));
		panel.add(regexpPanel);


		globalPanel.add(panel, BorderLayout.CENTER);

		JPanel southPanel = new JPanel(new BorderLayout());

		southPanel.add(new JLabel(I18n.getMessage("thaw.plugin.miniFrost.views")),
				BorderLayout.NORTH);

		ButtonGroup buttonGroup = new ButtonGroup();

		JPanel viewPanel = new JPanel(new GridLayout(1, 2));

		JPanel gmailPanel = new JPanel(new BorderLayout(3, 3));
		JPanel outlookPanel = new JPanel(new BorderLayout(3, 3));

		gmailView   = new JRadioButton(I18n.getMessage("thaw.plugin.miniFrost.views.gmail"));
		outlookView = new JRadioButton(I18n.getMessage("thaw.plugin.miniFrost.views.outlook"));

		gmailPanel.add(gmailView, BorderLayout.NORTH);
		gmailPanel.add(new JLabel(IconBox.miniFrostGmailView,
				JLabel.LEFT),
				BorderLayout.CENTER);

		outlookPanel.add(outlookView, BorderLayout.NORTH);
		outlookPanel.add(new JLabel(IconBox.miniFrostOutlookView,
				JLabel.LEFT),
				BorderLayout.CENTER);


		buttonGroup.add(gmailView);
		buttonGroup.add(outlookView);


		viewPanel.add(gmailPanel);
		viewPanel.add(outlookPanel);

		southPanel.add(viewPanel, BorderLayout.CENTER);

		globalPanel.add(southPanel, BorderLayout.SOUTH);

		selectValues();
	}


	public void display() {
		configWindow.addObserver(this);
		configWindow.addTab(I18n.getMessage("thaw.plugin.miniFrost"),
				thaw.gui.IconBox.minReadComments,
				globalPanel);
	}


	public void hide() {
		configWindow.deleteObserver(this);
		configWindow.removeTab(globalPanel);
	}

	private void selectValues() {

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


		int view = MiniFrostPanel.DEFAULT_VIEW;

		if (config.getValue("miniFrostView") != null) {
			view = Integer.parseInt(config.getValue("miniFrostView"));
			Logger.info(this, "View : "+Integer.toString(view));
		}

		if (view == 0) {
			outlookView.setSelected(false);
			gmailView.setSelected(true);
		} else {
			gmailView.setSelected(false);
			outlookView.setSelected(true);
		}
		
		boolean s = MiniFrost.DISPLAY_AS_TREE;
		
		if (config.getValue("checkbox_miniFrost_seeTree") != null) {
			s = Boolean.valueOf(config.getValue("checkbox_miniFrost_seeTree")).booleanValue();
		}
		
		treeCheckBox.setSelected(s);
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

			config.setValue("miniFrostView",
					(gmailView.isSelected() ? "0" : "1"));
			
			config.setValue("checkbox_miniFrost_seeTree",
					Boolean.toString(treeCheckBox.isSelected()));

		} else if (param == configWindow.getCancelButton()) {

			selectValues();

		}
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == regexpButton) {
			regexpBlacklist.displayTab(configWindow);
		}
	}
}
