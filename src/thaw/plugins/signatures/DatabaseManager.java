package thaw.plugins.signatures;


import java.sql.*;


import thaw.core.Config;
import thaw.core.SplashScreen;
import thaw.core.Logger;

import thaw.plugins.Hsqldb;


public class DatabaseManager {


	private DatabaseManager() {

	}


	public static boolean init(Hsqldb db, Config config, SplashScreen splashScreen) {
		boolean newDb;

		newDb = false;

		if (config.getValue("signaturesDatabaseVersion") == null) {
			newDb = true;
			config.setValue("signaturesDatabaseVersion", "1");
		} else {

			/* CONVERTIONS */
			/*
			if ("1".equals(config.getValue("indexDatabaseVersion"))) {
				if (splashScreen != null)
					splashScreen.setStatus("Converting database ...");
				if (convertDatabase_1_to_2(db))
					config.setValue("indexDatabaseVersion", "2");
			}
			*/

		}


		createTables(db);

		return newDb;
	}


	protected static boolean sendQuery(final Hsqldb db, final String query) {
		try {
			db.executeQuery(query);
			return true;
		} catch(final SQLException e) {
			Logger.notice(new DatabaseManager(), "While (re)creating sql tables: "+e.toString());
			return false;
		}
	}


	public static void createTables(Hsqldb db) {
		sendQuery(db, "CREATE CACHED TABLE signatures ("
			  + "id INTEGER IDENTITY NOT NULL, "
			  + "nickName VARCHAR(255) NOT NULL, "
			  + "y VARBINARY(400) NOT NULL, " /* publicKey */
			  + "x VARBINARY(400) DEFAULT NULL NULL, " /* privateKey */
			  + "isDup BOOLEAN DEFAULT FALSE NOT NULL, "
			  + "trustLevel TINYINT DEFAULT 0 NOT NULL, " /* See Identity.java */
			  + "PRIMARY KEY(id))");
	}

	/* dropTables is not implements because signatures may be VERY important */
}
