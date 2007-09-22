package thaw.plugins;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;

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

		PreparedStatement st = connection.prepareStatement("SET LOGSIZE 50");
		st.execute();
	}

	public void disconnect() throws java.sql.SQLException {
		synchronized(dbLock) {
			connection.commit();
			//executeQuery("SHUTDOWN COMPACT");
			executeQuery("SHUTDOWN");
			connection.close();
			//connection = null;
		}
	}


	public boolean stop() {
		return true;
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


	public ResultSet executeQuery(final String query) throws java.sql.SQLException {
		ResultSet results;

		final Statement stmt = connection.createStatement();

		results = stmt.executeQuery(query);

		return results;
	}

	public javax.swing.ImageIcon getIcon() {
		return thaw.gui.IconBox.database;
	}
}
