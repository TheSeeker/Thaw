package thaw.gui;

import javax.swing.JCheckBox;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import thaw.core.Config;


public class CheckBox extends JCheckBox implements ActionListener {
	public final static String PREFIX = "checkbox_";

	private Config config;
	private String name;

	public CheckBox(Config config, String name,
			String txt) {
		super(txt);

		this.config = config;
		this.name = name;

		loadState();

		super.addActionListener(this);
	}

	public CheckBox(Config config, String name,
			String txt, boolean selected) {
		super(txt, selected);

		this.config = config;
		this.name = name;

		loadState();

		super.addActionListener(this);
	}

	public void loadState() {
		if (config.getValue(PREFIX + name) != null)
			super.setSelected( (new Boolean(config.getValue(PREFIX + name))).booleanValue() );
	}

	public void saveState() {
		config.setValue(PREFIX+name,
				Boolean.toString(super.isSelected()));
	}

	public void actionPerformed(ActionEvent e) {
		saveState();
	}
}
