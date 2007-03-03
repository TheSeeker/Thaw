package thaw.plugins;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import thaw.core.Core;
import thaw.core.FileChooser;
import thaw.core.I18n;
import thaw.core.LogListener;
import thaw.core.Logger;
import thaw.core.Plugin;


/**
 * Quick and dirty console showing Thaw logs, and allowing to save them.
 */
public class LogConsole implements Plugin, LogListener, ActionListener, Runnable {
	public final static int MAX_LINE = 512;

	private Core core;

	private String[] buffer;
	private int readOffset;
	private int writeOffset;

	private JPanel consolePanel;
	private JTextArea logArea;
	private JScrollPane logAreaScrollPane;
	private JButton saveToFile;

	private boolean threadRunning;
	private boolean hasChanged;

	public LogConsole() {

	}

	public boolean run(final Core core) {
		this.core = core;
		threadRunning = true;
		hasChanged = false;

		buffer = new String[MAX_LINE+1];
		readOffset = 0;
		writeOffset = 0;

		consolePanel = new JPanel();
		consolePanel.setLayout(new BorderLayout());

		logArea = new JTextArea();
		logArea.setEditable(false);
		saveToFile = new JButton(I18n.getMessage("thaw.plugin.console.saveToFile"));

		saveToFile.addActionListener(this);

		logAreaScrollPane = new JScrollPane(logArea);

		consolePanel.add(logAreaScrollPane, BorderLayout.CENTER);
		consolePanel.add(saveToFile, BorderLayout.SOUTH);

		core.getMainWindow().addTab(I18n.getMessage("thaw.plugin.console.console"), thaw.core.IconBox.minTerminal, consolePanel);

		Logger.addLogListener(this);

		Thread dispThread = new Thread(this);
		dispThread.start();

		return true;

	}


	public boolean stop() {
		threadRunning = false;

		Logger.removeLogListener(this);

		core.getMainWindow().removeTab(consolePanel);

		return true;
	}


	public void actionPerformed(final ActionEvent e) {
		if(e.getSource() == saveToFile) {
			final FileChooser fileChooser = new FileChooser();

			fileChooser.setTitle(I18n.getMessage("thaw.plugin.console.console"));
			fileChooser.setDirectoryOnly(false);
			fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);

			final File file = fileChooser.askOneFile();

			if(file != null) {
				Logger.info(this, "Saving logs ...");
				writeLogsToFile(file);
				Logger.info(this, "Saving done.");
			}

		}

	}

	public void writeLogsToFile(final File file) {
		/* A la bourrin */

		FileOutputStream output;

		try {
			output = new FileOutputStream(file);
		} catch(final java.io.FileNotFoundException e) {
			Logger.error(this, "FileNotFoundException ? wtf ?");
			return;
		}

		try {
			output.write(logArea.getText().getBytes("UTF-8"));
		} catch(final java.io.IOException e) {
			Logger.error(this, "IOException while writing logs ... out of space ?");
			return;
		}

		try {
			output.close();
		} catch(final java.io.IOException e) {
			Logger.error(this, "IOException while closing log file ?!");
			return;
		}
	}

	public void newLogLine(final String line) {
		addLine(line + "\n");
	}


	public void addLine(String line) {
		buffer[writeOffset] = line;

		writeOffset++;

		if (writeOffset == MAX_LINE)
			writeOffset = 0;

		if (writeOffset == readOffset) {
			readOffset++;

			if (readOffset == MAX_LINE)
				readOffset = 0;
		}

		hasChanged = true;
	}

	public void refreshDisplay() {
		String res = "";
		int i;

		for (i = readOffset ; ; i++) {
			if (i == MAX_LINE+1)
				i = 0;

			if (buffer[i] != null)
				res += buffer[i];

			if ( (readOffset > 0 && i == readOffset-1)
			     || (readOffset <= 0 && i == MAX_LINE))
				break;
		}

		logArea.setText(res);
		logAreaScrollPane.getVerticalScrollBar().setValue(logAreaScrollPane.getVerticalScrollBar().getMaximum());
	}


	public void run() {
		while(threadRunning) {
			try {
				Thread.sleep(1000);
			} catch(java.lang.InterruptedException e) {
				/* \_o< */
			}

			if (threadRunning && hasChanged) {
				hasChanged = false;
				refreshDisplay();
			}
		}
	}


	public String getNameForUser() {
		return I18n.getMessage("thaw.plugin.console.console");
	}


	public javax.swing.ImageIcon getIcon() {
		return thaw.core.IconBox.terminal;
	}
}
