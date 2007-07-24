package thaw.plugins.miniFrost;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JComboBox;

import java.util.Observer;
import java.util.Observable;

import java.awt.GridLayout;

import thaw.core.Config;
import thaw.core.ConfigWindow;
import thaw.core.I18n;
import thaw.core.Logger;
import thaw.gui.IconBox;


public class MiniFrostConfigTab implements Observer {

	private Config config;
	private ConfigWindow configWindow;

	private JPanel panel;

	public final static int MIN_BOARDS = 0;
	public final static int MAX_BOARDS = 30;

	private JComboBox maxBoards;

	public MiniFrostConfigTab(Config config,
				  ConfigWindow configWindow) {
		this.config = config;
		this.configWindow = configWindow;

		panel = new JPanel(new GridLayout(16, 1));

		maxBoards = new JComboBox();

		for (int i = MIN_BOARDS ; i <= MAX_BOARDS ; i++)
			maxBoards.addItem(Integer.toString(i));

		selectValue();

		panel.add(new JLabel(I18n.getMessage("thaw.plugin.miniFrost.maxBoardsRefreshed")));
		panel.add(maxBoards);

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
			Logger.info(this, "Max: "+Integer.toString(max));
		} else {
			max = AutoRefresh.DEFAULT_MAX_BOARDS_REFRESHING;
		}

		maxBoards.setSelectedIndex(max-MIN_BOARDS);
	}


	public void update(Observable o, Object param) {
		if (param == configWindow.getOkButton()) {

			config.setValue("miniFrostAutoRefreshMaxBoards",
					(String)maxBoards.getSelectedItem());

		} else if (param == configWindow.getCancelButton()) {

			selectValue();

		}
	}
}
