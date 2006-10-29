package thaw.core;

import javax.swing.JPanel;
import java.awt.GridLayout;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JCheckBox;

import java.util.Observer;
import java.util.Observable;

/**
 * NodeConfigPanel. Creates and manages the panel containing all the things to configure
 *  the settings to access the node.
 */
public class NodeConfigPanel implements Observer {
	private Core core;
	private JPanel nodeConfigPanel = null;


	private final static String[] paramNames = {
		I18n.getMessage("thaw.config.nodeAddress"),
		I18n.getMessage("thaw.config.nodePort"),
		I18n.getMessage("thaw.config.maxSimultaneousDownloads"),
		I18n.getMessage("thaw.config.maxSimultaneousInsertions"),
		I18n.getMessage("thaw.config.maxUploadSpeed"),
		I18n.getMessage("thaw.config.thawId")
	};

	private final static String[] configNames = {
		"nodeAddress",
		"nodePort",
		"maxSimultaneousDownloads",
		"maxSimultaneousInsertions",
		"maxUploadSpeed",
		"thawId"
	};

	private final static String[] currentValues = new String[6];


	private JLabel[] paramLabels = new JLabel[paramNames.length];
	private JTextField[] paramFields = new JTextField[configNames.length];

	private JCheckBox multipleSockets = null;
	private ConfigWindow configWindow = null;


	public NodeConfigPanel(ConfigWindow configWindow, Core core) {
		this.core = core;
		this.configWindow = configWindow;

		this.nodeConfigPanel = new JPanel();
		this.nodeConfigPanel.setLayout(new GridLayout(15, 1));

		for(int i=0; i < paramNames.length ; i++) {
			String value;

			if( (value = core.getConfig().getValue(configNames[i])) == null)
				value = "";

			this.paramLabels[i] = new JLabel(paramNames[i]);
			this.paramFields[i] = new JTextField(value);
			currentValues[i] = value;

			this.nodeConfigPanel.add(this.paramLabels[i]);
			this.nodeConfigPanel.add(this.paramFields[i]);
		}

		this.multipleSockets = new JCheckBox(I18n.getMessage("thaw.config.multipleSockets"),
						Boolean.valueOf(core.getConfig().getValue("multipleSockets")).booleanValue());
		this.nodeConfigPanel.add(new JLabel(" "));
		this.nodeConfigPanel.add(this.multipleSockets);

		this.setVisibility(Boolean.valueOf(core.getConfig().getValue("advancedMode")).booleanValue());

		configWindow.addObserver(this);
	}

	public JPanel getPanel() {
		return this.nodeConfigPanel;
	}

	private void setVisibility(boolean advancedMode) {
		for(int i= 2; i < paramNames.length;i++) {
			this.paramLabels[i].setVisible(advancedMode);
			this.paramFields[i].setVisible(advancedMode);
		}

		this.multipleSockets.setVisible(advancedMode);
	}


	public boolean hasAValueChanged() {
		for(int i=0; i < paramNames.length ; i++) {
			if (!this.paramFields[i].getText().equals(currentValues[i]))
				return true;
		}

		if (this.core.getConfig().getValue("multipleSockets") == null
		    || !this.core.getConfig().getValue("multipleSockets").equals(Boolean.toString(this.multipleSockets.isSelected())))
			return true;

		return false;
	}


	public void update(Observable o, Object arg) {
		if(arg == this.core.getConfigWindow().getOkButton()) {
			if (this.hasAValueChanged())
				this.configWindow.willNeedConnectionReset();

			for(int i=0;i < paramNames.length;i++) {
				this.core.getConfig().setValue(configNames[i], this.paramFields[i].getText());
			}

			this.core.getConfig().setValue("multipleSockets", Boolean.toString(this.multipleSockets.isSelected()));

			this.setVisibility(Boolean.valueOf(this.core.getConfig().getValue("advancedMode")).booleanValue());
		}


		if(arg == this.core.getConfigWindow().getCancelButton()) {
			for(int i=0;i < paramNames.length;i++) {
				String value;

				if( (value = this.core.getConfig().getValue(configNames[i])) == null)
					value = "";

				this.paramFields[i].setText(value);
			}

			this.multipleSockets.setSelected(Boolean.valueOf(this.core.getConfig().getValue("multipleSockets")).booleanValue());
		}
	}

}
