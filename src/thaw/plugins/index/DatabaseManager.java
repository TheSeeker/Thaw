package thaw.plugins.index;

import java.sql.SQLException;

import thaw.core.Logger;
import thaw.plugins.Hsqldb;

/**
 * Create all the tables used to save the indexes.
 * <br/>
 * "Comprenne qui pourra" :P
 *
 * <pre>
 * indexCategories (name, positionInTree)
 *  |-- indexCategories (name, positionInTree)
 *  | |-- [...]
 *  |
 *  |-- indexes (name, publicKey, [privateKey], positionInTree)
 *    |-- links (indexName, indexPublicKey)
 *    |-- files (filename, publicKey, mime, size)
 *      |-- metadatas (name, value)
 * </pre>
 *
 * positionInTree == position in its JTree branch.
 */
public class DatabaseManager {

	public DatabaseManager() {

	}

	/**
	 * Can be safely called, even if the tables already exist.
	 */
	public static void createTables(final Hsqldb db) {
		//sendQuery(db,
		//	  "SET IGNORECASE TRUE");
		DatabaseManager.sendQuery(db,
			  "CREATE CACHED TABLE indexCategories ("
			  + "id INTEGER IDENTITY NOT NULL,"
			  + "name VARCHAR(255) NOT NULL,"
			  + "positionInTree INTEGER NOT NULL,"
			  + "modifiableIndexes BOOLEAN NOT NULL," /* Obsolete */
			  + "parent INTEGER NULL,"
			  + "PRIMARY KEY (id),"
			  + "FOREIGN KEY (parent) REFERENCES indexCategories (id))");

		DatabaseManager.sendQuery(db,
			  "CREATE CACHED TABLE indexes ("
			  + "id INTEGER IDENTITY NOT NULL, "
			  + "originalName VARCHAR(255) NOT NULL, "
			  + "displayName VARCHAR(255) NULL, "
			  + "publicKey VARCHAR(255) NOT NULL, "
			  + "privateKey VARCHAR(255) NULL, "
			  + "author VARCHAR(255) NULL, "
			  + "positionInTree INTEGER NOT NULL, "
			  + "revision INTEGER NOT NULL, "
			  + "parent INTEGER NULL, "
			  + "PRIMARY KEY (id), "
			  + "FOREIGN KEY (parent) REFERENCES indexCategories (id))");

		DatabaseManager.sendQuery(db,
			  "CREATE CACHED TABLE categories ("
			  + "id INTEGER IDENTITY NOT NULL,"
			  + "name VARCHAR(255) NOT NULL)");

		DatabaseManager.sendQuery(db,
			  "CREATE CACHED TABLE files ("
			  + "id INTEGER IDENTITY NOT NULL,"
			  + "filename VARCHAR(255) NOT NULL,"
			  + "publicKey VARCHAR(350) NOT NULL," // key ~= 100 + filename == 255 max => 350
			  + "localPath VARCHAR(500) NULL,"
			  + "mime VARCHAR(50) NULL,"
			  + "size BIGINT NOT NULL,"
			  + "category INTEGER NULL,"
			  + "indexParent INTEGER NOT NULL,"
			  + "PRIMARY KEY (id),"
			  + "FOREIGN KEY (indexParent) REFERENCES indexes (id),"
			  + "FOREIGN KEY (category) REFERENCES categories (id))");

		DatabaseManager.sendQuery(db,
			  "CREATE CACHED TABLE links ("
			  + "id INTEGER IDENTITY NOT NULL,"
			  + "publicKey VARCHAR(350) NOT NULL," // key ~= 100 + filename == 255 max
			  + "mark INTEGER NOT NULL,"
			  + "comment VARCHAR(512) NOT NULL,"
			  + "indexParent INTEGER NOT NULL,"
			  + "indexTarget INTEGER NULL,"
			  + "PRIMARY KEY (id),"
			  + "FOREIGN KEY (indexParent) REFERENCES indexes (id),"
			  + "FOREIGN KEY (indexTarget) REFERENCES indexes (id))");

		DatabaseManager.sendQuery(db,
			  "CREATE CACHED TABLE metadataNames ("
			  + "id INTEGER IDENTITY NOT NULL,"
			  + "name VARCHAR(255) NOT NULL,"
			  + "PRIMARY KEY (id))");

		DatabaseManager.sendQuery(db,
			  "CREATE CACHED TABLE metadatas ("
			  + "id INTEGER IDENTITY NOT NULL,"
			  + "nameId INTEGER NOT NULL,"
			  + "value VARCHAR(255) NOT NULL,"
			  + "fileId INTEGER NOT NULL,"
			  + "PRIMARY KEY (id),"
			  + "FOREIGN KEY (fileId) REFERENCES files (id),"
			  + "FOREIGN KEY (nameId) REFERENCES metadataNames (id))");

	}

	public static void dropTables(final Hsqldb db) {
		DatabaseManager.sendQuery(db, "DROP TABLE metadatas");
		DatabaseManager.sendQuery(db, "DROP TABLE metadataNames");

		DatabaseManager.sendQuery(db, "DROP TABLE files");
		DatabaseManager.sendQuery(db, "DROP TABLE links");

		DatabaseManager.sendQuery(db, "DROP TABLE indexes");
		DatabaseManager.sendQuery(db, "DROP TABLE indexCategories");
	}


	/**
	 * Returns no error / Throws no exception.
	 */
	protected static void sendQuery(final Hsqldb db, final String query) {
		try {
			db.executeQuery(query);
		} catch(final SQLException e) {
			Logger.notice(new DatabaseManager(), "While (re)creating sql tables: "+e.toString());
		}
	}


	public static void exportDatabase(IndexTree indexTree) {

	}

	public static void importDatabase(IndexTree indexTree) {

	}
}
