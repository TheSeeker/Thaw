package thaw.core;

import java.awt.GridLayout;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

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


	public ThawConfigPanel(final ConfigWindow configWindow, final Core core) {
		this.core = core;

		if(core.getConfig().getValue("advancedMode") == null)
			core.getConfig().setValue("advancedMode", "false");

		advancedMode = Boolean.valueOf(core.getConfig().getValue("advancedMode")).booleanValue();


		thawConfigPanel = new JPanel();
		thawConfigPanel.setLayout(new GridLayout(15, 1));

		advancedModeBox = new JCheckBox(I18n.getMessage("thaw.config.advancedMode"), advancedMode);

		nicknameLabel = new JLabel(I18n.getMessage("thaw.config.nickname"));

		if (core.getConfig().getValue("userNickname") == null)
			nicknameField = new JTextField("Another anonymous");
		else
			nicknameField = new JTextField(core.getConfig().getValue("userNickname"));

		thawConfigPanel.add(advancedModeBox);
		thawConfigPanel.add(new JLabel(" "));
		thawConfigPanel.add(nicknameLabel);
		thawConfigPanel.add(nicknameField);

		configWindow.addObserver(this);
	}


	public JPanel getPanel() {
		return thawConfigPanel;
	}


	public void update(final Observable o, final Object arg) {
		if(arg == core.getConfigWindow().getOkButton()) {
			advancedMode = advancedModeBox.isSelected();
			core.getConfig().setValue("advancedMode", Boolean.toString(advancedMode));
			core.getConfig().setValue("userNickname", nicknameField.getText());
		}

		if(arg == core.getConfigWindow().getCancelButton()) {
			advancedModeBox.setSelected(advancedMode);
			nicknameField.setText(core.getConfig().getValue("userNickname"));
		}
	}

}

