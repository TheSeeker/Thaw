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

	private long maxLogSize = 5120;

	public Console() {

	}

	public boolean run(Core core) {
		this.core = core;
		
		this.consolePanel = new JPanel();
		this.consolePanel.setLayout(new BorderLayout());
		
		this.logArea = new JTextArea();
		this.logArea.setEditable(false);
		this.saveToFile = new JButton(I18n.getMessage("thaw.plugin.console.saveToFile"));
		
		this.saveToFile.addActionListener(this);

		this.consolePanel.add(new JScrollPane(this.logArea), BorderLayout.CENTER);
		this.consolePanel.add(this.saveToFile, BorderLayout.SOUTH);

		core.getMainWindow().addTab(I18n.getMessage("thaw.plugin.console.console"), this.consolePanel);

		if(core.getConfig().getValue("consoleMaxLogSize") == null)
			core.getConfig().setValue("consoleMaxLogSize", ((new Long(this.maxLogSize)).toString()) );
		else {
			try {
				this.maxLogSize = (new Long(core.getConfig().getValue("consoleMaxLogSize"))).longValue();
			} catch(Exception e) {
				Logger.notice(this, "Invalide size given in configuration ! Using default one.");
				core.getConfig().setValue("consoleMaxLogSize", (new Long(this.maxLogSize)).toString());
			}
		}

		this.configPanel = new JPanel();
		this.configPanel.setLayout(new GridLayout(15, 1));

		this.sizeLabel = new JLabel(I18n.getMessage("thaw.plugin.console.maxSize"));
		this.sizeField = new JTextField(core.getConfig().getValue("consoleMaxLogSize"));

		this.configPanel.add(this.sizeLabel);
		this.configPanel.add(this.sizeField);

		core.getConfigWindow().addTab(I18n.getMessage("thaw.plugin.console.console"), this.configPanel);
		
		Logger.addLogListener(this);

		return true;

	}


	public boolean stop() {
		this.core.getConfig().setValue("consoleMaxLogSize", this.sizeField.getText() );

		Logger.removeLogListener(this);

		this.core.getConfigWindow().removeTab(this.configPanel);
		this.core.getMainWindow().removeTab(this.consolePanel);

		return true;
	}


	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == this.saveToFile) {
			FileChooser fileChooser = new FileChooser();

			fileChooser.setTitle(I18n.getMessage("thaw.plugin.console.console"));
			fileChooser.setDirectoryOnly(false);
			fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
			
			File file = fileChooser.askOneFile();

			if(file != null) {
				Logger.info(this, "Saving logs ...");
				this.writeLogsToFile(file);
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
			output.write(this.logArea.getText().getBytes("UTF-8"));
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
		String text = this.logArea.getText() + "\n" + line;

		if(text.length() > this.maxLogSize) {
			text = text.substring((int)(text.length() - this.maxLogSize));
		}

		this.logArea.setText(text);
	}


	public String getNameForUser() {
		return I18n.getMessage("thaw.plugin.console.console");
	}


}
