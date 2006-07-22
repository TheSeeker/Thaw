package thaw.core;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.ActionListener;
import javax.swing.JCheckBox;

import java.util.Observer;
import java.util.Observable;

import thaw.i18n.I18n;

/**
 * Creates and manages the panel containing all the things to configure related to Thaw and only Thaw.
 */
public class ThawConfigPanel implements Observer {
	private Core core;
	private JPanel thawConfigPanel = null;

	private JCheckBox advancedModeBox = null;

	private boolean advancedMode;

	public ThawConfigPanel(ConfigWindow configWindow, Core core) {
		this.core = core;

		if(core.getConfig().getValue("advancedMode") == null)
			core.getConfig().setValue("advancedMode", "false");

		advancedMode = Boolean.valueOf(core.getConfig().getValue("advancedMode")).booleanValue();
		

		thawConfigPanel = new JPanel();
		thawConfigPanel.setLayout(new GridLayout(15, 1));

		advancedModeBox = new JCheckBox(I18n.getMessage("thaw.config.advancedMode"), advancedMode);

		thawConfigPanel.add(advancedModeBox);

		configWindow.addObserver(this);
	}


	public JPanel getPanel() {
		return thawConfigPanel;
	}


	public void update(Observable o, Object arg) {
		if(arg == core.getConfigWindow().getOkButton()) {
			advancedMode = advancedModeBox.isSelected();
			core.getConfig().setValue("advancedMode", Boolean.toString(advancedMode));
		}

		if(arg == core.getConfigWindow().getCancelButton()) {
			advancedModeBox.setSelected(advancedMode);
		}
	}

}

