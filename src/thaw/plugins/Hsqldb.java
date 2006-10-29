package thaw.plugins;

import java.sql.Connection;
import java.sql.DriverManager;

import java.sql.ResultSet;
import java.sql.Statement;

import thaw.core.*;

public class Hsqldb extends LibraryPlugin {
	private Core core;

	private Connection connection;

	private int writeLock;

	public Hsqldb() {

	}

	public boolean run(Core core) {
		this.core = core;

		try {
			Class.forName("org.hsqldb.jdbcDriver");
		} catch (Exception e) {
			Logger.error(this, "ERROR: failed to load HSQLDB JDBC driver.");
			e.printStackTrace();
			System.exit(1);
			return false;
		}

		return true;
	}


	public void lockWriting() {
		while(writeLock > 0) {
			try {
				Thread.sleep(100);
			} catch(java.lang.InterruptedException e) {
				/* \_o< */
			}
		}
		writeLock++;
	}

	public synchronized void unlockWriting() {
		writeLock = 0;
	}

	public void realStart() {
		Logger.info(this, "Connecting to the database ...");

		if(this.core.getConfig().getValue("hsqldb.url") == null)
			this.core.getConfig().setValue("hsqldb.url", "jdbc:hsqldb:file:thaw.db");

		try {
			this.connect();
		} catch (java.sql.SQLException e) {
			Logger.error(this, "SQLException while connecting to the database '"+this.core.getConfig().getValue("hsqldb.url")+"'");
			e.printStackTrace();
		}
	}


	public void connect() throws java.sql.SQLException {
		if(this.core.getConfig().getValue("hsqldb.url") == null)
			this.core.getConfig().setValue("hsqldb.url", "jdbc:hsqldb:file:thaw.db");

		if(this.connection != null)
			this.disconnect();

		this.connection = DriverManager.getConnection(this.core.getConfig().getValue("hsqldb.url"),
							 "sa", "");
	}

	public void disconnect() throws java.sql.SQLException {
		this.connection.close();
	}


	public boolean stop() {

		return true;
	}

	public void realStop() {
		Logger.info(this, "Disconnecting from the database ...");

		try {
			this.connection.commit();
			this.executeQuery("SHUTDOWN");

			this.connection.close();
		} catch(java.sql.SQLException e) {
			Logger.error(this, "SQLException while closing connection !");
			e.printStackTrace();
		}
	}

	public String getNameForUser() {
		return I18n.getMessage("thaw.plugin.hsqldb.database");
	}



	public Connection getConnection() {
		return this.connection;
	}


	public ResultSet executeQuery(String query) throws java.sql.SQLException {
		ResultSet results;

		Statement stmt = this.connection.createStatement();

		results = stmt.executeQuery(query);

		return results;
	}

}
