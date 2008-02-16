package thaw.plugins;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import thaw.core.Core;
import thaw.core.I18n;
import thaw.core.LibraryPlugin;
import thaw.core.Logger;

public class Hsqldb extends LibraryPlugin {
	private Core core;

	public volatile Object dbLock;
	private Connection connection;


	public boolean run(final Core core) {
		this.core = core;

		dbLock = new Object();

		try {
			Class.forName("org.hsqldb.jdbcDriver");
		} catch (final Exception e) {
			Logger.error(this, "ERROR: failed to load HSQLDB JDBC driver.");
			e.printStackTrace();
			System.exit(1);
			return false;
		}

		return true;
	}



	public void realStart() {
		Logger.info(this, "Connecting to the database ...");

		if(core.getConfig().getValue("hsqldb.url") == null)
			core.getConfig().setValue("hsqldb.url", "jdbc:hsqldb:file:thaw.db;shutdown=true");

		try {
			connect();
		} catch (final java.sql.SQLException e) {
			Logger.error(this, "SQLException while connecting to the database '"+core.getConfig().getValue("hsqldb.url")+"'");
			e.printStackTrace();
		}
	}


	public void connect() throws java.sql.SQLException {
		if(core.getConfig().getValue("hsqldb.url") == null)
			core.getConfig().setValue("hsqldb.url", "jdbc:hsqldb:file:thaw.db");

		if(connection != null)
			disconnect();
		
		connection = DriverManager.getConnection(core.getConfig().getValue("hsqldb.url"),
							 "sa", "");
		
		executeQuery("SET LOGSIZE 50;");
		executeQuery("SET CHECKPOINT DEFRAG 50;");
		executeQuery("SET PROPERTY \"hsqldb.nio_data_file\" FALSE");
	}

	public void disconnect() throws java.sql.SQLException {
		synchronized(dbLock) {
			connection.commit();
			executeQuery("SHUTDOWN");
			connection.close();
		}
	}


	public void stop() {
		/* \_o< */
	}

	public void realStop() {
		Logger.info(this, "Disconnecting from the database ...");

		try {
			disconnect();
		} catch(final java.sql.SQLException e) {
			Logger.error(this, "SQLException while closing connection !");
			e.printStackTrace();
		}

		Logger.info(this, "Done.");
	}

	public String getNameForUser() {
		return I18n.getMessage("thaw.plugin.hsqldb.database");
	}



	public Connection getConnection() {
		return connection;
	}


	public void executeQuery(final String query) throws java.sql.SQLException {
		final Statement stmt = connection.createStatement();

		stmt.execute(query);
		
		stmt.close();
	}

	public javax.swing.ImageIcon getIcon() {
		return thaw.gui.IconBox.database;
	}
}
