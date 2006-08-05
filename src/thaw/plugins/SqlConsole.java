package thaw.plugins;

import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JScrollPane;
import javax.swing.JButton;

import java.awt.BorderLayout;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

import java.awt.Font;

import thaw.core.*;
import thaw.fcp.*;

public class SqlConsole implements Plugin, java.awt.event.ActionListener {
	public final static int BUFFER_SIZE = 51200;

	private Core core;
	private Hsqldb hsqldb;

	private JPanel panel;

	private JTextArea sqlArea;
	private JTextField commandField;
	private JButton sendButton;

	public SqlConsole() {

	}

	public boolean run(Core core) {
		this.core = core;

		if(core.getPluginManager().getPlugin("thaw.plugins.Hsqldb") == null) {
			Logger.info(this, "Loading Hsqldb plugin");

			if(!core.getPluginManager().loadPlugin("thaw.plugins.Hsqldb")
			   || !core.getPluginManager().runPlugin("thaw.plugins.Hsqldb")) {
				Logger.error(this, "Unable to load thaw.plugins.Hsqldb !");
				return false;
			}
		}

		hsqldb = (Hsqldb)core.getPluginManager().getPlugin("thaw.plugins.Hsqldb");

		hsqldb.registerChild(this);

		panel = getPanel();

		core.getMainWindow().addTab(I18n.getMessage("thaw.plugin.hsqldb.console"),
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

		panel.add(new JScrollPane(sqlArea), BorderLayout.CENTER);
		panel.add(subPanel, BorderLayout.SOUTH);

		return panel;
	}

	public void addToConsole(String txt) {
		String text = sqlArea.getText() + txt;

		if(text.length() > BUFFER_SIZE) {
			text = text.substring((int)(text.length() - BUFFER_SIZE));
		}

		sqlArea.setText(text);
	}

	public void actionPerformed(java.awt.event.ActionEvent e) {

		sendCommand(commandField.getText());

		commandField.setText("");

	}

	protected void display(String txt, int lng) {
		if(txt == null)
			txt = "(null)";

		int txtLength = txt.length();

		String fTxt = txt;

		if(lng > 30)
			lng = 30;

		for(int i = 0 ; i + txtLength < lng; i++) {
			fTxt = fTxt + " ";
		}

		addToConsole(fTxt);
	}

	public void sendCommand(String cmd) {

		/* A simple reminder :) */
		if(cmd.toLowerCase().equals("list_tables"))
			cmd = "SELECT * FROM INFORMATION_SCHEMA.SYSTEM_TABLES";

		addToConsole("\n> "+cmd+"\n\n");

		try {

			if(cmd.toLowerCase().equals("reconnect")) {
				hsqldb.connect();
				addToConsole("Ok\n");
				return;
			}

			ResultSet result = hsqldb.executeQuery(cmd);
			
			if(result == null) {
				addToConsole("(null)\n");
				return;
			}
			
			java.sql.SQLWarning warning = result.getWarnings();

			while(warning != null) {
				addToConsole("Warning: "+warning.toString());
				warning = warning.getNextWarning();
			}



			ResultSetMetaData metadatas = result.getMetaData();
			
			int nmbCol = metadatas.getColumnCount();
			
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

		} catch(java.sql.SQLException e) {
			addToConsole("SQLException : "+e.toString()+" : "+e.getCause()+"\n");
		}
		
	}

}
