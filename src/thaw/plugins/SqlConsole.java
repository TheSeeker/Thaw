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

import thaw.plugins.index.TableCreator;

import thaw.core.*;

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

		this.hsqldb = (Hsqldb)core.getPluginManager().getPlugin("thaw.plugins.Hsqldb");

		this.hsqldb.registerChild(this);

		this.panel = this.getPanel();

		core.getMainWindow().addTab(I18n.getMessage("thaw.plugin.hsqldb.console"),
					    this.panel);
					    

		return true;
	}


	public boolean stop() {
		this.core.getMainWindow().removeTab(this.panel);

		this.hsqldb.unregisterChild(this);

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

		this.sqlArea = new JTextArea("");
		this.sqlArea.setEditable(false);
		this.sqlArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

		this.commandField = new JTextField("");
		this.commandField.addActionListener(this);

		this.sendButton = new JButton(" Ok ");
		this.sendButton.addActionListener(this);
		
		subPanel.add(this.commandField, BorderLayout.CENTER);
		subPanel.add(this.sendButton, BorderLayout.EAST);

		panel.add(new JScrollPane(this.sqlArea), BorderLayout.CENTER);
		panel.add(subPanel, BorderLayout.SOUTH);

		return panel;
	}

	public void addToConsole(String txt) {
		String text = this.sqlArea.getText() + txt;

		if(text.length() > BUFFER_SIZE) {
			text = text.substring((text.length() - BUFFER_SIZE));
		}

		this.sqlArea.setText(text);
	}

	public void actionPerformed(java.awt.event.ActionEvent e) {

		this.sendCommand(this.commandField.getText());

		this.commandField.setText("");

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

		this.addToConsole(fTxt);
	}

	public void sendCommand(String cmd) {

		/* A simple reminder :) */
		if("list_tables".equals( cmd.toLowerCase() ))
			cmd = "SELECT * FROM INFORMATION_SCHEMA.SYSTEM_TABLES";

		this.addToConsole("\n> "+cmd+"\n\n");

		try {

			if("reconnect".equals( cmd.toLowerCase() )) {
				this.hsqldb.connect();
				this.addToConsole("Ok\n");
				return;
			}
			
			java.sql.Statement st = this.hsqldb.getConnection().createStatement();

			ResultSet result;

			if(!"drop_tables".equals( cmd.toLowerCase() )) {
				if(st.execute(cmd))
					result = st.getResultSet();
				else {
					this.addToConsole("Ok\n");
					return;
				}
			} else {
				TableCreator.dropTables(this.hsqldb);
				this.addToConsole("Ok\n");
				return;
			}
				
			
			if(result == null) {
				this.addToConsole("(null)\n");
				return;
			}
			
			if(result.getFetchSize() == 0) {
				this.addToConsole("(done)\n");
				return;
			}

			java.sql.SQLWarning warning = result.getWarnings();

			while(warning != null) {
				this.addToConsole("Warning: "+warning.toString());
				warning = warning.getNextWarning();
			}



			ResultSetMetaData metadatas = result.getMetaData();
			
			int nmbCol = metadatas.getColumnCount();
			
			this.addToConsole("      ");
			
			for(int i = 1; i <= nmbCol ; i++) {
				this.display(metadatas.getColumnLabel(i), metadatas.getColumnDisplaySize(i));
				this.addToConsole("  ");
			}
			this.addToConsole("\n");

			this.addToConsole("      ");
			for(int i = 1; i <= nmbCol ; i++) {
			        this.display(metadatas.getColumnTypeName(i), metadatas.getColumnDisplaySize(i));
				this.addToConsole("  ");
			}
			this.addToConsole("\n");

			this.addToConsole("      ");
			for(int i = 1; i <= nmbCol ; i++) {
				this.display("----", metadatas.getColumnDisplaySize(i));
				this.addToConsole("  ");
			}
			this.addToConsole("\n");

			boolean ret = true;

			while(ret) {
				ret = result.next();

				if(!ret)
					break;

				this.display(Integer.toString(result.getRow()), 4);
				this.addToConsole("  ");

				for(int i =1; i <= nmbCol ; i++) {
					this.display(result.getString(i), metadatas.getColumnDisplaySize(i));
					this.addToConsole("  ");
				}
				this.addToConsole("\n");
			}

		} catch(java.sql.SQLException e) {
			this.addToConsole("SQLException : "+e.toString()+" : "+e.getCause()+"\n");
		}
		
	}

}
