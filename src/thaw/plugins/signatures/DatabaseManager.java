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
			config.setValue("signaturesDatabaseVersion", "2");
		} else {

			/* CONVERTIONS */

			if ("1".equals(config.getValue("signaturesDatabaseVersion"))) {
				if (splashScreen != null)
					splashScreen.setStatus("Converting database ...");
				if (convertDatabase_1_to_2(db))
					config.setValue("signaturesDatabaseVersion", "2");
			}


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
			  + "publicKey VARCHAR(400) NOT NULL, " /* publicKey */
			  + "privateKey VARCHAR(400) DEFAULT NULL NULL, " /* privateKey */
			  + "isDup BOOLEAN DEFAULT FALSE NOT NULL, "
			  + "trustLevel TINYINT DEFAULT 0 NOT NULL, " /* See Identity.java */
			  + "PRIMARY KEY(id))");
	}

	/* dropTables is not implements because signatures may be VERY important */
	/* (anyway, because of the foreign key, it would probably fail */

	protected static boolean convertDatabase_1_to_2(Hsqldb db) {
		if (!sendQuery(db, "DELETE FROM indexComments")
		    || !sendQuery(db, "DELETE FROM signatures")
		    || !sendQuery(db, "ALTER TABLE signatures DROP y")
		    || !sendQuery(db, "ALTER TABLE signatures DROP x")
		    || !sendQuery(db, "ALTER TABLE signatures ADD publicKey VARCHAR(400) NOT NULL")
		    || !sendQuery(db, "ALTER TABLE signatures ADD privateKey VARCHAR(400) NULL"))
			return false;

		return true;
	}
}
