package thaw.plugins.index;

import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JButton;

import thaw.core.Config;
import thaw.core.ConfigWindow;
import thaw.core.I18n;
import thaw.core.Logger;

public class IndexConfigPanel implements ActionListener {
	private ConfigWindow configWindow;
	private Config config;

	private JPanel panel;

	private JCheckBox autorefreshActivated;
	private JTextField refreshInterval;
	private JTextField indexPerRefresh;

	private JCheckBox loadOnTheFly;

	private JButton editBlackList;

	private IndexBrowserPanel indexBrowser;


	public IndexConfigPanel(ConfigWindow configWindow, Config config, IndexBrowserPanel indexBrowser) {
		this.configWindow = configWindow;
		this.config = config;
		this.indexBrowser = indexBrowser;

		panel = new JPanel();
		panel.setLayout(new GridLayout(15, 1));

		autorefreshActivated = new JCheckBox(I18n.getMessage("thaw.plugin.index.useAutoRefresh"));

		JLabel refreshIntervalLabel = new JLabel(I18n.getMessage("thaw.plugin.index.autoRefreshInterval"));
		refreshInterval = new JTextField("");

		JLabel indexPerRefreshLabel = new JLabel(I18n.getMessage("thaw.plugin.index.nmbIndexPerRefresh"));
		indexPerRefresh = new JTextField("");

		loadOnTheFly = new JCheckBox(I18n.getMessage("thaw.plugin.index.loadOnTheFly"));


		resetValues();

		autorefreshActivated.addActionListener(this);
		configWindow.getOkButton().addActionListener(this);
		configWindow.getCancelButton().addActionListener(this);


		editBlackList = new JButton(I18n.getMessage("thaw.plugin.index.editBlackList")+ " ...");
		editBlackList.addActionListener(this);

		JPanel editBlackListPanel = new JPanel(new BorderLayout());
		editBlackListPanel.add(new JLabel(""), BorderLayout.CENTER);
		editBlackListPanel.add(editBlackList, BorderLayout.EAST);


		panel.add(autorefreshActivated);
		panel.add(refreshIntervalLabel);
		panel.add(refreshInterval);
		panel.add(indexPerRefreshLabel);
		panel.add(indexPerRefresh);
		panel.add(editBlackListPanel);

		if (Boolean.valueOf(config.getValue("advancedMode")).booleanValue()) {
			panel.add(new JLabel(" "));
			panel.add(loadOnTheFly);
		}

		updateTextFieldState();
	}


	public void addTab() {
		configWindow.addTab(I18n.getMessage("thaw.plugin.index.indexes"),
				    thaw.gui.IconBox.minIndex,
				    panel);
	}


	public void removeTab() {
		saveValues();
		configWindow.removeTab(panel);
	}


	public void updateTextFieldState() {
		refreshInterval.setEnabled(autorefreshActivated.isSelected());
		indexPerRefresh.setEnabled(autorefreshActivated.isSelected());
	}

	public void resetValues() {
		boolean activated = AutoRefresh.DEFAULT_ACTIVATED;
		int refreshIntervalInt = AutoRefresh.DEFAULT_INTERVAL;
		int nmbIndexInt = AutoRefresh.DEFAULT_INDEX_NUMBER;
		boolean loadOnTheFlyBoolean = false;

		try {
			if (config.getValue("indexAutoRefreshActivated") != null) {
				activated = Boolean.valueOf(config.getValue("indexAutoRefreshActivated")).booleanValue();
			}

			if (config.getValue("indexRefreshInterval") != null) {
				refreshIntervalInt = Integer.parseInt(config.getValue("indexRefreshInterval"));
			}

			if (config.getValue("nmbIndexesPerRefreshInterval") != null) {
				nmbIndexInt = Integer.parseInt(config.getValue("nmbIndexesPerRefreshInterval"));
			}


			if (config.getValue("loadIndexTreeOnTheFly") != null) {
				loadOnTheFlyBoolean = Boolean.valueOf(config.getValue("loadIndexTreeOnTheFly")).booleanValue();
			}

		} catch(NumberFormatException e) {
			Logger.error(this, "Error while parsing config !");
		}


		autorefreshActivated.setSelected(activated);
		refreshInterval.setText(Integer.toString(refreshIntervalInt));
		indexPerRefresh.setText(Integer.toString(nmbIndexInt));
		loadOnTheFly.setSelected(loadOnTheFlyBoolean);
	}


	public void saveValues() {
		config.setValue("indexAutoRefreshActivated",
				Boolean.toString(autorefreshActivated.isSelected()));
		config.setValue("indexRefreshInterval",
				refreshInterval.getText());
		config.setValue("nmbIndexesPerRefreshInterval",
				indexPerRefresh.getText());
		config.setValue("loadIndexTreeOnTheFly",
				Boolean.toString(loadOnTheFly.isSelected()));
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == autorefreshActivated) {
			updateTextFieldState();
		}

		if (e.getSource() == configWindow.getOkButton()) {
			saveValues();
		}

		if (e.getSource() == configWindow.getCancelButton()) {
			resetValues();
		}

		if (e.getSource() == editBlackList) {
			indexBrowser.getBlackList().displayPanel();
			configWindow.close();
		}
	}

}
