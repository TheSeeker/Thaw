package thaw.plugins;

import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import thaw.core.*;
import thaw.i18n.I18n;
import thaw.plugins.insertPlugin.*;

public class InsertPlugin implements thaw.core.Plugin {
	private Core core;

	private InsertPanel insertPanel;

	public InsertPlugin() {

	}


	public boolean run(Core core) {
		this.core = core;
		
		Logger.info(this, "Starting plugin \"InsertPlugin\" ...");

		insertPanel = new InsertPanel();

		core.getMainWindow().addTab(I18n.getMessage("thaw.common.insertion"), insertPanel.getPanel());

		return true;
	}


	public boolean stop() {
		Logger.info(this, "Stopping plugin \"InsertPlugin\" ...");

		core.getMainWindow().removeTab(insertPanel.getPanel());

		return true;
	}


	public String getNameForUser() {
		return I18n.getMessage("thaw.common.insertion");
	}
}
