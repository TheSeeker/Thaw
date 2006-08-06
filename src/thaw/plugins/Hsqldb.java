package thaw.plugins;

import java.sql.Connection;
import java.sql.DriverManager;

import java.sql.ResultSet;
import java.sql.Statement;

import thaw.core.*;
import thaw.fcp.*;

public class Hsqldb extends LibraryPlugin {
	private Core core;

	private Connection connection;

	public Hsqldb() {

	}

	public boolean run(Core core) {
		this.core = core;

		try {
			Class.forName("org.hsqldb.jdbcDriver");
		} catch (Exception e) {
			Logger.error(this, "ERROR: failed to load HSQLDB JDBC driver.");
			e.printStackTrace();
			return false;
		}

		return true;
	}

	public void realStart() {
		Logger.info(this, "Connecting to the database ...");

		if(core.getConfig().getValue("hsqldb.url") == null)
			core.getConfig().setValue("hsqldb.url", "jdbc:hsqldb:file:thaw.db");

		try {
			connect();
		} catch (java.sql.SQLException e) {
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
	}

	public void disconnect() throws java.sql.SQLException {
		connection.close();
	}


	public boolean stop() {

		return true;
	}

	public void realStop() {
		Logger.info(this, "Disconnecting from the database ...");
		
		try {
			connection.commit();
			connection.close();
		} catch(java.sql.SQLException e) {
			Logger.error(this, "SQLException while closing connection !");
			e.printStackTrace();
		}
	}

	public String getNameForUser() {
		return I18n.getMessage("thaw.plugin.hsqldb.database");
	}



	public Connection getConnection() {
		return connection;
	}


	public ResultSet executeQuery(String query) throws java.sql.SQLException {

		ResultSet results;
		
		Statement stmt = connection.createStatement();
			
		results = stmt.executeQuery(query);

		return results;
	}


	public boolean execute(String query) throws java.sql.SQLException {
		boolean result;
		
		Statement stmt = connection.createStatement();
		
		result = stmt.execute(query);

		return result;
	}
}
