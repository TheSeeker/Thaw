package thaw.plugins.webOfTrust;

import thaw.core.Config;
import thaw.core.SplashScreen;
import thaw.core.Logger;
import thaw.plugins.Hsqldb;

import java.sql.*;

public class DatabaseManager {
	/**
	 * @splashScreen can be null
	 * @return true if database is a new one
	 */
	public static boolean init(Hsqldb db, Config config, SplashScreen splashScreen) {
		boolean newDb;

		newDb = false;

		if (config.getValue("wotDatabaseVersion") == null) {
			newDb = true;
			config.setValue("wotDatabaseVersion", "0");
		} else {
			/* CONVERTIONS */
			
			/* ... */			
		}

		createTables(db);

		return newDb;
	}
	
	public static void createTables(Hsqldb db) {
		sendQuery(db, "CREATE CACHED TABLE wotKeys ("
				+ "id INTEGER IDENTITY NOT NULL, "
				+ "publicKey VARCHAR(400) NOT NULL, "
				+ "keyDate TIMESTAMP NOT NULL, "
				+ "score SMALLINT NOT NULL, "
				+ "sigId INTEGER NOT NULL, "
				+ "lastDownload TIMESTAMP DEFAULT NULL NULL, "
				+ "lastUpdate TIMESTAMP DEFAULT NULL NULL, "
				+ "PRIMARY KEY(id), "
				+ "FOREIGN KEY(sigId) REFERENCES signatures (id))");
		
		sendQuery(db, "CREATE CACHED TABLE wotTrustLists ("
				+ "id INTEGER IDENTITY NOT NULL, "
				+ "source INTEGER NOT NULL, "
				+ "destination INTEGER NOT NULL, "
				+ "trustLevel SMALLINT NOT NULL, "
				+ "PRIMARY KEY(id), "
				+ "FOREIGN KEY(source) REFERENCES signatures (id), "
				+ "FOREIGN KEY(destination) REFERENCES signatures(id))");
	}
	
	/**
	 * Returns no error / Throws no exception.
	 * @return false if an exception happened
	 */
	protected static boolean sendQuery(final Hsqldb db, final String query) {
		try {
			db.executeQuery(query);
			return true;
		} catch(final SQLException e) {
			Logger.notice(new DatabaseManager(), "While (re)creating sql tables: "+e.toString());
			return false;
		}
	}
}
