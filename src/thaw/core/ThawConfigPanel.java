package thaw.core;

import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.util.Observable;
import java.util.Observer;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JTextField;

import thaw.gui.FileChooser;

/**
 * Creates and manages the panel containing all the things to configure related to Thaw and only Thaw.
 */
public class ThawConfigPanel implements Observer, ActionListener {
	private Core core;
	private JPanel thawConfigPanel = null;

	private JCheckBox advancedModeBox = null;

	private boolean advancedMode;

	private JLabel tmpDirLabel;
	private JTextField tmpDirField;
	private JButton tmpDirButton;


	public ThawConfigPanel(final ConfigWindow configWindow, final Core core) {
		this.core = core;

		if(core.getConfig().getValue("advancedMode") == null)
			core.getConfig().setValue("advancedMode", "false");

		advancedMode = Boolean.valueOf(core.getConfig().getValue("advancedMode")).booleanValue();


		thawConfigPanel = new JPanel();
		thawConfigPanel.setLayout(new GridLayout(15, 1));

		advancedModeBox = new JCheckBox(I18n.getMessage("thaw.config.advancedMode"), advancedMode);

		thawConfigPanel.add(advancedModeBox);
		thawConfigPanel.add(new JLabel(" "));

		tmpDirField = new JTextField(System.getProperty("java.io.tmpdir"));
		tmpDirButton = new JButton(I18n.getMessage("thaw.common.browse"));
		tmpDirButton.addActionListener(this);

		tmpDirLabel = new JLabel(I18n.getMessage("thaw.common.tempDir"));
		thawConfigPanel.add(tmpDirLabel);

		JPanel tempDirPanel = new JPanel(new BorderLayout());

		tempDirPanel.add(tmpDirField,
				 BorderLayout.CENTER);
		tempDirPanel.add(tmpDirButton,
				 BorderLayout.EAST);
		thawConfigPanel.add(tempDirPanel);

		setAdvancedOptionsVisibility(advancedMode);

		configWindow.addObserver(this);
	}


	public JPanel getPanel() {
		return thawConfigPanel;
	}


	private void setAdvancedOptionsVisibility(boolean v) {
		tmpDirField.setVisible(v);
		tmpDirButton.setVisible(v);
		tmpDirLabel.setVisible(v);
	}

	public void actionPerformed(ActionEvent e) {
		FileChooser chooser = new FileChooser(System.getProperty("java.io.tmpdir"));
		chooser.setTitle(I18n.getMessage("thaw.common.tempDir"));
		chooser.setDirectoryOnly(true);
		chooser.setDialogType(javax.swing.JFileChooser.OPEN_DIALOG);

		java.io.File file = chooser.askOneFile();
		tmpDirField.setText(file.getPath());
	}


	public void update(final Observable o, final Object arg) {
		if(arg == core.getConfigWindow().getOkButton()) {
			advancedMode = advancedModeBox.isSelected();
			core.getConfig().setValue("advancedMode", Boolean.toString(advancedMode));

			core.getConfig().setValue("tmpDir", tmpDirField.getText());
			System.setProperty("java.io.tmpdir", tmpDirField.getText());
			tmpDirField.setText(System.getProperty("java.io.tmpdir"));

			setAdvancedOptionsVisibility(advancedMode);
		}

		if(arg == core.getConfigWindow().getCancelButton()) {
			advancedModeBox.setSelected(advancedMode);

			tmpDirField.setText(System.getProperty("java.io.tmpdir"));
		}
	}

}

