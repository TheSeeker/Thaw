package thaw.plugins;

import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.JFileChooser;

import java.io.File;
import java.io.FileOutputStream;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import thaw.i18n.I18n;
import thaw.core.*;


/**
 * Quick and dirty console showing Thaw logs, and allowing to save them.
 */
public class Console implements Plugin, LogListener, ActionListener {
	private Core core;
	
	private JPanel consolePanel;
	private JTextArea logArea;
	private JButton saveToFile;

	private JPanel configPanel;
	private JLabel sizeLabel;
	private JTextField sizeField;

	private long maxLogSize = 25600;

	public boolean run(Core core) {
		this.core = core;
		
		consolePanel = new JPanel();
		consolePanel.setLayout(new BorderLayout());
		
		logArea = new JTextArea();
		logArea.setEditable(false);
		saveToFile = new JButton(I18n.getMessage("thaw.plugin.console.saveToFile"));
		
		saveToFile.addActionListener(this);

		consolePanel.add(new JScrollPane(logArea), BorderLayout.CENTER);
		consolePanel.add(saveToFile, BorderLayout.SOUTH);

		core.getMainWindow().addTab(I18n.getMessage("thaw.plugin.console.console"), consolePanel);

		if(core.getConfig().getValue("consoleMaxLogSize") == null)
			core.getConfig().setValue("consoleMaxLogSize", ((new Long(maxLogSize)).toString()) );
		else {
			try {
				maxLogSize = (new Long(core.getConfig().getValue("consoleMaxLogSize"))).longValue();
			} catch(Exception e) {
				Logger.notice(this, "Invalide size given in configuration ! Using default one.");
				core.getConfig().setValue("consoleMaxLogSize", (new Long(maxLogSize)).toString());
			}
		}

		configPanel = new JPanel();
		configPanel.setLayout(new GridLayout(15, 1));

		sizeLabel = new JLabel(I18n.getMessage("thaw.plugin.console.maxSize"));
		sizeField = new JTextField(core.getConfig().getValue("consoleMaxLogSize"));

		configPanel.add(sizeLabel);
		configPanel.add(sizeField);

		core.getConfigWindow().addTab(I18n.getMessage("thaw.plugin.console.console"), configPanel);
		
		Logger.addLogListener(this);

		return true;

	}


	public boolean stop() {
		core.getConfig().setValue("consoleMaxLogSize", sizeField.getText() );

		Logger.removeLogListener(this);

		core.getConfigWindow().removeTab(configPanel);
		core.getMainWindow().removeTab(consolePanel);

		return true;
	}


	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == saveToFile) {
			FileChooser fileChooser = new FileChooser();

			fileChooser.setTitle(I18n.getMessage("thaw.plugin.console.console"));
			fileChooser.setDirectoryOnly(false);
			fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
			
			File file = fileChooser.askOneFile();

			if(file != null) {
				Logger.info(this, "Saving logs ...");
				writeLogsToFile(file);
				Logger.info(this, "Saving done.");
			}

		}

	}

	public void writeLogsToFile(File file) {
		/* A la bourrin */
		
		FileOutputStream output;

		try {
			output = new FileOutputStream(file);
		} catch(java.io.FileNotFoundException e) {
			Logger.error(this, "FileNotFoundException ? wtf ?");
			return;
		}

		try {
			output.write(logArea.getText().getBytes());
		} catch(java.io.IOException e) {
			Logger.error(this, "IOException while writing logs ... out of space ?");
			return;
		}

		try {
			output.close();
		} catch(java.io.IOException e) {
			Logger.error(this, "IOException while closing log file ?!");
			return;
		}
	}

	public void newLogLine(String line) {
		String text = logArea.getText() + "\n" + line;

		if(text.length() > maxLogSize) {
			text = text.substring((int)(text.length() - maxLogSize));
		}

		logArea.setText(text);
	}


	public String getNameForUser() {
		return I18n.getMessage("thaw.plugin.console.console");
	}


}
