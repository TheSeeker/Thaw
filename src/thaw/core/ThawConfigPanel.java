package thaw.core;

import javax.swing.JPanel;
import java.awt.GridLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

import java.util.Observer;
import java.util.Observable;

/**
 * Creates and manages the panel containing all the things to configure related to Thaw and only Thaw.
 */
public class ThawConfigPanel implements Observer {
	private Core core;
	private JPanel thawConfigPanel = null;

	private JCheckBox advancedModeBox = null;

	private JLabel nicknameLabel = null;
	private JTextField nicknameField = null;

	private boolean advancedMode;


	public ThawConfigPanel(ConfigWindow configWindow, Core core) {
		this.core = core;

		if(core.getConfig().getValue("advancedMode") == null)
			core.getConfig().setValue("advancedMode", "false");

		this.advancedMode = Boolean.valueOf(core.getConfig().getValue("advancedMode")).booleanValue();


		this.thawConfigPanel = new JPanel();
		this.thawConfigPanel.setLayout(new GridLayout(15, 1));

		this.advancedModeBox = new JCheckBox(I18n.getMessage("thaw.config.advancedMode"), this.advancedMode);

		this.nicknameLabel = new JLabel(I18n.getMessage("thaw.config.nickname"));

		if (core.getConfig().getValue("userNickname") == null)
			this.nicknameField = new JTextField("Another anonymous");
		else
			this.nicknameField = new JTextField(core.getConfig().getValue("userNickname"));

		this.thawConfigPanel.add(this.advancedModeBox);
		this.thawConfigPanel.add(new JLabel(" "));
		this.thawConfigPanel.add(this.nicknameLabel);
		this.thawConfigPanel.add(this.nicknameField);

		configWindow.addObserver(this);
	}


	public JPanel getPanel() {
		return this.thawConfigPanel;
	}


	public void update(Observable o, Object arg) {
		if(arg == this.core.getConfigWindow().getOkButton()) {
			this.advancedMode = this.advancedModeBox.isSelected();
			this.core.getConfig().setValue("advancedMode", Boolean.toString(this.advancedMode));
			this.core.getConfig().setValue("userNickname", this.nicknameField.getText());
		}

		if(arg == this.core.getConfigWindow().getCancelButton()) {
			this.advancedModeBox.setSelected(this.advancedMode);
			this.nicknameField.setText(this.core.getConfig().getValue("userNickname"));
		}
	}

}

