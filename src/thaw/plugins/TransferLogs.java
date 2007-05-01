package thaw.plugins;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.JScrollPane;

import javax.swing.event.TableModelEvent;

import java.util.Observer;
import java.util.Observable;

import java.util.Vector;
import java.util.Iterator;

import java.text.DateFormat;

import java.sql.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.DataInputStream;
import java.io.BufferedReader;


import thaw.core.Core;
import thaw.core.Logger;
import thaw.gui.IconBox;
import thaw.gui.Table;
import thaw.gui.FileChooser;
import thaw.gui.GUIHelper;
import thaw.core.I18n;
import thaw.core.Plugin;

import thaw.fcp.FCPQueueManager;
import thaw.fcp.FCPTransferQuery;
import thaw.fcp.FreenetURIHelper;


/**
 * Quick and dirty plugin to log the transfers
 */
public class TransferLogs implements Plugin, ActionListener, Observer {
	public final static String MAX_DISPLAYED = "1000";


	private Core core;
	private Hsqldb db;

	private JPanel tab;


	private JButton purgeLogs;
	private JButton copyKey;

	private JButton importKeys;
	private JButton exportKeys;


	private Table table;
	private EventListModel model;

	public TransferLogs() {

	}


	public boolean run(final Core core) {
		this.core = core;

		/* loading hsqldb */

		if(core.getPluginManager().getPlugin("thaw.plugins.Hsqldb") == null) {
			Logger.info(this, "Loading Hsqldb plugin");

			if(core.getPluginManager().loadPlugin("thaw.plugins.Hsqldb") == null
			   || !core.getPluginManager().runPlugin("thaw.plugins.Hsqldb")) {
				Logger.error(this, "Unable to load thaw.plugins.Hsqldb !");
				return false;
			}
		}

		db = (Hsqldb)core.getPluginManager().getPlugin("thaw.plugins.Hsqldb");

		createTables();


		/* making GUI */

		tab = new JPanel(new BorderLayout(5, 5));

		JLabel topLabel = new JLabel(I18n.getMessage("thaw.plugin.transferLogs.transferLogs") + " :");
		topLabel.setIcon(IconBox.file);

		model = new EventListModel();
		model.reloadList();

		table = new Table(core.getConfig(), "transfer_log_table",
					model);

		purgeLogs = new JButton(I18n.getMessage("thaw.plugin.transferLogs.purgeLogs"), IconBox.minDelete);
		copyKey = new JButton(I18n.getMessage("thaw.plugin.transferLogs.copyKey"), IconBox.minCopy );
		importKeys = new JButton(I18n.getMessage("thaw.plugin.transferLogs.importKeys"), IconBox.minImportAction);
		exportKeys = new JButton(I18n.getMessage("thaw.plugin.transferLogs.exportKeys"), IconBox.minExportAction);

		purgeLogs.addActionListener(this);
		copyKey.addActionListener(this);
		importKeys.addActionListener(this);
		exportKeys.addActionListener(this);


		JPanel buttonPanel = new JPanel();
		buttonPanel.add(purgeLogs);
		buttonPanel.add(importKeys);
		buttonPanel.add(exportKeys);
		buttonPanel.add(copyKey);

		JPanel southPanel = new JPanel(new BorderLayout());
		southPanel.add(buttonPanel, BorderLayout.WEST);
		southPanel.add(new JLabel(""), BorderLayout.CENTER);

		tab.add(topLabel, BorderLayout.NORTH);
		tab.add(new JScrollPane(table), BorderLayout.CENTER);
		tab.add(southPanel, BorderLayout.SOUTH);

		setAsObserverEverywhere(true);

		core.getMainWindow().addTab(I18n.getMessage("thaw.plugin.transferLogs.transferLogsShort"),
					    thaw.gui.IconBox.minFile,
					    tab);

		return true;
	}


	public boolean stop() {
		core.getMainWindow().removeTab(tab);
		setAsObserverEverywhere(false);

		db.unregisterChild(this);

		return false;
	}


	public String getNameForUser() {
		return I18n.getMessage("thaw.plugin.transferLogs.transferLogs");
	}


	public javax.swing.ImageIcon getIcon() {
		return thaw.gui.IconBox.file;
	}


	protected void createTables() {
		/*
		 * this db structure is dirty, I know it, and at the moment,
		 * I don't care => I will fix it later
		 */
		sendQuery("CREATE CACHED TABLE transferEvents ("
			  + "id INTEGER IDENTITY NOT NULL,"
			  + "date TIMESTAMP NOT NULL,"
			  + "msg VARCHAR(500) NOT NULL,"
			  + "key VARCHAR(500) NULL,"
			  + "isDup BOOLEAN NOT NULL, "
			  + "isSuccess BOOLEAN NOT NULL, "
			  + "PRIMARY KEY (id))");
	}


	/**
	 * Returns no error / Throws no exception.
	 * @return false if an exception happened
	 */
	protected boolean sendQuery(final String query) {
		try {
			synchronized(db.dbLock) {
				db.executeQuery(query);
			}
			return true;
		} catch(final SQLException e) {
			Logger.notice(this, "While (re)creating sql tables: "+e.toString());
			return false;
		}
	}


	/**
	 * Add the current plugin as an observer on all the running query
	 * @param really if set to false, will deleteObserver() instead of addObserver()
	 */
	public void setAsObserverEverywhere(boolean really) {
		if (really)
			core.getQueueManager().addObserver(this);
		else
			core.getQueueManager().deleteObserver(this);


		Vector runningQueue = core.getQueueManager().getRunningQueue();

		synchronized(runningQueue) {
			for (Iterator it = runningQueue.iterator();
			     it.hasNext();) {
				FCPTransferQuery query = (FCPTransferQuery)it.next();

				if (really) {
					if (query.isFinished() && !isDup(query.getFileKey()))
						notifyEnd(query);

					if (query instanceof Observable)
						((Observable)query).addObserver(this);
				} else {
					if (query instanceof Observable)
						((Observable)query).deleteObserver(this);
				}
			}
		}
	}


	protected boolean isDup(String key) {
		try {
			synchronized(db.dbLock) {

				PreparedStatement st;

				st = db.getConnection().prepareStatement("SELECT id FROM transferEvents "+
									 "WHERE LOWER(key) LIKE ? AND isSuccess = TRUE");
				st.setString(1, FreenetURIHelper.getComparablePart(key)+"%");
				ResultSet set = st.executeQuery();
				return set.next();

			}
		} catch(SQLException e) {
			Logger.error(this, "Unable to know if a key is dup in the event because : "+e.toString());
		}

		return false;
	}


	protected void notifyAddition(FCPTransferQuery query) {
		String str;
		String key;

		if (query.getQueryType() == 0)
			return;

		if (query.getQueryType() == 1) {
			str = I18n.getMessage("thaw.plugin.transferLogs.download.added") + " : "
				+ query.getFilename();
			key = query.getFileKey();
		} else {
			str =  I18n.getMessage("thaw.plugin.transferLogs.insertion.added") + " : "
				+ query.getPath();
			key = null;
		}

		java.sql.Timestamp date = new java.sql.Timestamp((new java.util.Date()).getTime());

		boolean isDup;

		if (key != null)
			isDup = isDup(key);
		else
			isDup = false;

		try {
			synchronized(db.dbLock) {
				PreparedStatement st;


				st = db.getConnection().prepareStatement("INSERT INTO transferEvents "+
									 "(date, msg, key, isDup, isSuccess) "+
									 " VALUES "+
									 "(?, ?, ?, ?, FALSE)");
				st.setTimestamp(1, date);
				st.setString(2, str);
				st.setString(3, key);
				st.setBoolean(4, isDup);

				st.execute();
			}
		} catch(SQLException e) {
			Logger.error(this, "Error while adding an event to the logs: "+e.toString());
		}

		model.reloadList();
	}


	protected void notifyEnd(FCPTransferQuery query) {
		String str;
		String key;

		if (query.isFinished() && query instanceof Observable)
			((Observable)query).deleteObserver(this);

		if (query.getQueryType() == 0 || !query.isFinished())
			return;

		if (query.getQueryType() == 1)
			str = "thaw.plugin.transferLogs.download.";
		else
			str = "thaw.plugin.transferLogs.insertion.";

		if (query.isSuccessful())
			str = I18n.getMessage(str+"successful");
		else
			str = I18n.getMessage(str+"failed");

		key = query.getFileKey();

		str += " : "+query.getFilename();

		str += "\n" + I18n.getMessage("thaw.plugin.transferLogs.finalStatus") + " : "+query.getStatus();

		boolean isDup;
		boolean isSuccess = query.isSuccessful();

		if (key != null)
			isDup = isDup(key);
		else
		        isDup = false;

		java.sql.Timestamp date = new java.sql.Timestamp((new java.util.Date()).getTime());


		try {
			synchronized(db.dbLock) {
				PreparedStatement st;


				st = db.getConnection().prepareStatement("INSERT INTO transferEvents "+
									 "(date, msg, key, isDup, isSuccess) "+
									 " VALUES "+
									 "(?, ?, ?, ?, ?)");
				st.setTimestamp(1, date);
				st.setString(2, str);
				st.setString(3, key);
				st.setBoolean(4, isDup);
				st.setBoolean(5, isSuccess);

				st.execute();
			}
		} catch(SQLException e) {
			Logger.error(this, "Error while adding an event to the logs: "+e.toString());
		}


		model.reloadList();
	}


	public void update(Observable o, Object param) {

		if (o instanceof FCPQueueManager) {
			FCPQueueManager queue = (FCPQueueManager)o;

			if (param == null)
				return;

			FCPTransferQuery query = (FCPTransferQuery)param;

			if(core.getQueueManager().isInTheQueues(query)
			   && query.isRunning()) { // then it's an addition
				if (query instanceof Observable)
					((Observable)query).addObserver(this);
				if (core.getQueueManager().isQueueCompletlyLoaded())
					notifyAddition(query);
				return;
			}
		}


		if (o instanceof FCPTransferQuery) {
			FCPTransferQuery query = (FCPTransferQuery)o;

			if (query.isFinished()) {
				notifyEnd(query);
				return;
			}
		}
	}


	private class Event {
		private java.sql.Timestamp date;
		private String msg;
		private String key;
		private boolean isDup;

		public Event(java.sql.Timestamp date, String message, String key, boolean isDup) {
			this.date = date;
			this.msg = message;
			this.key = key;
			this.isDup = isDup;
		}

		public java.sql.Timestamp getDate() {
			return date;
		}

		public String getMsg() {
			return msg;
		}

		public String getKey() {
			return key;
		}

		public boolean isDup() {
			return isDup;
		}
	}

	private class EventListModel extends javax.swing.table.AbstractTableModel {
		private static final long serialVersionUID = 1L;

		private DateFormat dateFormat;

		public String[] columnNames =
		{
			I18n.getMessage("thaw.plugin.transferLogs.date"),
			I18n.getMessage("thaw.plugin.transferLogs.message"),
			I18n.getMessage("thaw.plugin.transferLogs.key"),
			I18n.getMessage("thaw.plugin.transferLogs.isDup")
		};


		public Vector events = null; /* thaw.plugins.index.File Vector */


		public EventListModel() {
			super();
			dateFormat = DateFormat.getDateTimeInstance();
		}

		public void reloadList() {
			events = null;

			try {
				synchronized(db.dbLock) {
					PreparedStatement st;

					st = db.getConnection().prepareStatement("SELECT date, msg, key, isDup FROM "+
										 "transferEvents ORDER BY date DESC LIMIT "+MAX_DISPLAYED);

					ResultSet set = st.executeQuery();

					events = new Vector();

					while(set.next()) {
						events.add(new Event(set.getTimestamp("date"),
								     set.getString("msg"),
								     set.getString("key"),
								     set.getBoolean("isDup")));
					}
				}
			} catch(SQLException e) {
				Logger.error(this, "Error while getting the list of events from the logs: "+e.toString());
			}

			refresh();
		}


		protected void refresh() {
			final TableModelEvent event = new TableModelEvent(this);
			fireTableChanged(event);
		}

		public int getRowCount() {
			if (events == null)
				return 0;

			return events.size();
		}

		public int getColumnCount() {
			return columnNames.length;
		}

		public String getColumnName(final int column) {
			return columnNames[column];

		}

		public Object getValueAt(final int row, final int column) {
			if (events == null)
				return null;

			if (column == 0)
				return dateFormat.format(((Event)events.get(row)).getDate());

			if (column == 1)
				return ((Event)events.get(row)).getMsg();

			if (column == 2) {
				String key = ((Event)events.get(row)).getKey();
				return key != null ? key : I18n.getMessage("thaw.plugin.transferLogs.none");
			}

			if (column == 3)
				return ((Event)events.get(row)).isDup() ? "X" : "";

			return null;
		}


		public Vector getSelectedRows(Table table) {
			int[] selectedRows = table.getSelectedRows();

			if (selectedRows == null)
				return null;

			Vector r = new Vector();

			for (int i = 0 ; i < selectedRows.length ; i++) {
				r.add(events.get(selectedRows[i]));
			}

			return r;
		}
	}


	private File chooseFile(boolean save) {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(I18n.getMessage("thaw.plugin.transferLogs.chooseFile"));
		fileChooser.setDirectoryOnly(false);
		fileChooser.setDialogType(save ? FileChooser.SAVE_DIALOG
					  : FileChooser.OPEN_DIALOG);
		return fileChooser.askOneFile();
	}


	private class KeyImporter implements Runnable {
		public KeyImporter() { }

		public void run() {
			java.sql.Timestamp date = new java.sql.Timestamp((new java.util.Date()).getTime());


			File file  = chooseFile(false);

			if (file == null)
				return;

			try {
				FileInputStream fstream = new FileInputStream(file);

				DataInputStream in = new DataInputStream(fstream);
				BufferedReader br = new BufferedReader(new InputStreamReader(in));

				String strLine;

				while ((strLine = br.readLine()) != null)   {
					String key = strLine.trim();

					if (!FreenetURIHelper.isAKey(key))
						continue;

					boolean isDup = isDup(key);
					String str = I18n.getMessage("thaw.plugin.transferLogs.importedKey")
						+ " : "+FreenetURIHelper.getFilenameFromKey(key);

					try {
						synchronized(db.dbLock) {
							PreparedStatement st;

							st = db.getConnection().prepareStatement("INSERT INTO transferEvents "+
												 "(date, msg, key, isDup, isSuccess) "+
												 " VALUES "+
												 "(?, ?, ?, ?, FALSE)");
							st.setTimestamp(1, date);
							st.setString(2, str);
							st.setString(3, key);
							st.setBoolean(4, isDup);

							st.execute();
						}
					} catch(SQLException e) {
						Logger.error(this, "Error while adding an event to the logs: "+e.toString());
					}
				}

			in.close();

			} catch(java.io.FileNotFoundException e) {
				Logger.error(this, "(1) Unable to import keys because: "+e.toString());
			} catch(java.io.IOException e) {
				Logger.error(this, "(2) Unable to import keys because: "+e.toString());
			}

			model.reloadList();
		}
	}


	private class KeyExporter implements Runnable {
		public KeyExporter() { }

		public void run() {
			File file  = chooseFile(true);

			if (file == null)
				return;

			try {
				FileOutputStream out = new FileOutputStream(file);

				synchronized(db.dbLock) {
					PreparedStatement st;

					st = db.getConnection().prepareStatement("SELECT DISTINCT key "+
										 "FROM transferEvents "+
										 "WHERE key is NOT NULL");

					ResultSet set = st.executeQuery();

					while(set.next()) {
						out.write((set.getString("key")+"\n").getBytes("UTF-8"));
					}
				}

				out.close();

			} catch(SQLException e) {
				Logger.error(this, "Unable to export keys because: "+e.toString());
			} catch(java.io.FileNotFoundException e) {
				Logger.error(this, "(1) Unable to export keys because : "+e.toString());
			} catch(java.io.IOException e) {
				Logger.error(this, "(2) Unable to export keys because : "+e.toString());
			}

		}
	}



	public void actionPerformed(ActionEvent e) {

		if (e.getSource() == purgeLogs) {
			sendQuery("DROP TABLE transferEvents");
			createTables();

			model.reloadList();

			return;
		}

		if (e.getSource() == copyKey) {
			Vector v = model.getSelectedRows(table);

			if (v == null)
				return;

			String str = "";

			for (Iterator it = v.iterator();
			     it.hasNext();) {
				String key = ((Event)it.next()).getKey();

				if (key != null)
					str += key + "\n";
			}

			GUIHelper.copyToClipboard(str);

			return;
		}


		if (e.getSource() == importKeys) {
			Thread th = new Thread(new KeyImporter());
			th.start();
			return;
		}

		if (e.getSource() == exportKeys) {
			Thread th = new Thread(new KeyExporter());
			th.start();
			return;
		}
	}
}
