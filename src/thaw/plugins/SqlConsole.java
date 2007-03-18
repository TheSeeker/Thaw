package thaw.plugins;

import java.awt.BorderLayout;
import java.awt.Font;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import thaw.core.Core;
import thaw.core.I18n;
import thaw.core.Logger;
import thaw.core.Plugin;
import thaw.plugins.index.DatabaseManager;

public class SqlConsole implements Plugin, java.awt.event.ActionListener {
	public final static int MAX_LINE = 512;

	private String[] buffer;
	private String currentLine; /* line in construction (added to buffer when \n is added) */
	private int readOffset;
	private int writeOffset;

	private Core core;
	private Hsqldb hsqldb;

	private JPanel panel;

	private JTextArea sqlArea;
	private JScrollPane sqlAreaScrollPane;
	private JTextField commandField;
	private JButton sendButton;

	public SqlConsole() {

	}

	public boolean run(final Core core) {
		buffer = new String[MAX_LINE+1];
		currentLine = "";
		readOffset = 0;
		writeOffset = 0;

		this.core = core;

		if(core.getPluginManager().getPlugin("thaw.plugins.Hsqldb") == null) {
			Logger.info(this, "Loading Hsqldb plugin");

			if(core.getPluginManager().loadPlugin("thaw.plugins.Hsqldb") == null
			   || !core.getPluginManager().runPlugin("thaw.plugins.Hsqldb")) {
				Logger.error(this, "Unable to load thaw.plugins.Hsqldb !");
				return false;
			}
		}

		hsqldb = (Hsqldb)core.getPluginManager().getPlugin("thaw.plugins.Hsqldb");

		hsqldb.registerChild(this);

		panel = getPanel();

		core.getMainWindow().addTab(I18n.getMessage("thaw.plugin.hsqldb.console"),
					    thaw.gui.IconBox.minTerminal,
					    panel);

		return true;
	}


	public boolean stop() {
		core.getMainWindow().removeTab(panel);

		hsqldb.unregisterChild(this);

		return true;
	}

	public String getNameForUser() {
		return I18n.getMessage("thaw.plugin.hsqldb.console");
	}

	protected JPanel getPanel() {
		JPanel panel;
		JPanel subPanel;

		panel = new JPanel();
		panel.setLayout(new BorderLayout());

		subPanel = new JPanel();
		subPanel.setLayout(new BorderLayout());

		sqlArea = new JTextArea("");
		sqlArea.setEditable(false);
		sqlArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

		commandField = new JTextField("");
		commandField.addActionListener(this);

		sendButton = new JButton(" Ok ");
		sendButton.addActionListener(this);

		subPanel.add(commandField, BorderLayout.CENTER);
		subPanel.add(sendButton, BorderLayout.EAST);

		sqlAreaScrollPane = new JScrollPane(sqlArea);

		panel.add(sqlAreaScrollPane, BorderLayout.CENTER);
		panel.add(subPanel, BorderLayout.SOUTH);

		return panel;
	}

	public void addToConsole(final String txt) {
		currentLine += txt;

		if (txt.endsWith("\n")) {
			buffer[writeOffset] = currentLine;
			currentLine = "";

			writeOffset++;

			if (writeOffset == MAX_LINE)
				writeOffset = 0;

			if (writeOffset == readOffset) {
				readOffset++;

				if (readOffset == MAX_LINE)
					readOffset = 0;
			}
		}
	}


	public synchronized void refreshDisplay() {
		int i;
		String res = "";

		for (i = readOffset ; ; i++) {

			if (buffer[i] != null)
				res += buffer[i];

			if ( (readOffset-1 > 0 && i == readOffset-2)
			     || (readOffset-1 <= 0 && i == MAX_LINE))
				break;

			if (i == MAX_LINE)
				i = 0;
		}

		sqlArea.setText(res);

		sqlAreaScrollPane.getVerticalScrollBar().setValue(sqlAreaScrollPane.getVerticalScrollBar().getMaximum());
	}

	public synchronized void flushBuffer() {
		int i;

		for (i = 0 ; i < MAX_LINE + 1; i++) {
			buffer[i] = null;
		}
	}


	public void actionPerformed(final java.awt.event.ActionEvent e) {

		sendCommand(commandField.getText());

		commandField.setText("");

		refreshDisplay();

	}

	protected void display(String txt, int lng) {
		if(txt == null)
			txt = "(null)";

		final int txtLength = txt.length();

		String fTxt = txt;

		if(lng > 30)
			lng = 30;

		for(int i = 0 ; i + txtLength < lng; i++) {
			fTxt = fTxt + " ";
		}

		addToConsole(fTxt);
	}

	public synchronized void sendCommand(String cmd) {

		if ("clear".equals(cmd.toLowerCase())) {
			flushBuffer();
			return;
		}

		if ("refresh".equals(cmd.toLowerCase())) {
			refreshDisplay();
		}

		/* A simple reminder :) */
		if("list_tables".equals( cmd.toLowerCase() ))
			cmd = "SELECT * FROM INFORMATION_SCHEMA.SYSTEM_TABLES";

		addToConsole("\n");
		addToConsole("> "+cmd+"\n");
		addToConsole("\n");

		try {

			if("reconnect".equals( cmd.toLowerCase() )) {
				hsqldb.connect();
				addToConsole("Ok\n");
				return;
			}

			final java.sql.Statement st = hsqldb.getConnection().createStatement();

			ResultSet result;

			if(!"drop_tables".equals( cmd.toLowerCase() )) {
				if(st.execute(cmd))
					result = st.getResultSet();
				else {
					addToConsole("Ok\n");
					return;
				}
			} else {
				DatabaseManager.dropTables(hsqldb);
				addToConsole("Ok\n");
				return;
			}

			if(result == null) {
				addToConsole("(null)\n");
				return;
			}

			if(result.getFetchSize() == 0) {
				addToConsole("(done)\n");
				return;
			}

			java.sql.SQLWarning warning = result.getWarnings();

			while(warning != null) {
				addToConsole("Warning: "+warning.toString());
				warning = warning.getNextWarning();
			}



			final ResultSetMetaData metadatas = result.getMetaData();

			final int nmbCol = metadatas.getColumnCount();

			addToConsole("      ");

			for(int i = 1; i <= nmbCol ; i++) {
				display(metadatas.getColumnLabel(i), metadatas.getColumnDisplaySize(i));
				addToConsole("  ");
			}
			addToConsole("\n");

			addToConsole("      ");
			for(int i = 1; i <= nmbCol ; i++) {
			        display(metadatas.getColumnTypeName(i), metadatas.getColumnDisplaySize(i));
				addToConsole("  ");
			}
			addToConsole("\n");

			addToConsole("      ");
			for(int i = 1; i <= nmbCol ; i++) {
				display("----", metadatas.getColumnDisplaySize(i));
				addToConsole("  ");
			}
			addToConsole("\n");

			boolean ret = true;

			while(ret) {
				ret = result.next();

				if(!ret)
					break;

				display(Integer.toString(result.getRow()), 4);
				addToConsole("  ");

				for(int i =1; i <= nmbCol ; i++) {
					display(result.getString(i), metadatas.getColumnDisplaySize(i));
					addToConsole("  ");
				}
				addToConsole("\n");
			}

		} catch(final java.sql.SQLException e) {
			addToConsole("SQLException : "+e.toString()+" : "+e.getCause()+"\n");
		}

	}

	public javax.swing.ImageIcon getIcon() {
		return thaw.gui.IconBox.terminal;
	}
}
