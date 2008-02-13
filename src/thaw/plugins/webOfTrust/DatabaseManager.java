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
			/* ... */			
		}

		createTables(db);

		return newDb;
	}
	
	public static void createTables(Hsqldb db) {
		/*sendQuery(db,
				  "CREATE CACHED TABLE indexFolders ("
				  + "id INTEGER IDENTITY NOT NULL,"
				  + "name VARCHAR(255) NOT NULL,"
				  + "positionInTree INTEGER NOT NULL,"
				  + "modifiableIndexes BOOLEAN NOT NULL,"
				  + "parent INTEGER NULL,"
				  + "PRIMARY KEY (id),"
				  + "FOREIGN KEY (parent) REFERENCES indexFolders (id))");
		 */

		sendQuery(db, "CREATE CACHED TABLE wot ("
				+ "id INTEGER IDENTITY NOT NULL, "
				+ "publicKey VARCHAR(400) NOT NULL, "
				+ "score SMALLINT NOT NULL, "
				+ "sigId INTEGER NOT NULL, "
				+ "PRIMARY KEY(id), "
				+ "FOREIGN KEY(sigId) REFERENCES signatures (id))");
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
