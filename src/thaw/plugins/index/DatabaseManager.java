package thaw.plugins.index;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Iterator;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import thaw.core.Config;
import thaw.core.Logger;
import thaw.core.SplashScreen;
import thaw.fcp.FCPQueueManager;
import thaw.plugins.Hsqldb;

/**
 * Creates all the tables used to save the indexes,
 * manages structure changes if needed, etc.
 *
 * <br/> TAKE CARE: because of the conversion mechanisms, the field order is NOT ALWAYS the same !
 *       So DON'T DO "SELECT * [...]" !
 *
 * <br/>
 * "Comprenne qui pourra" :P
 *
 * <pre>
 * indexFolders (id, name, positionInTree)
 *  |-- indexFolders (id, name, positionInTree)
 *  | |-- [...]
 *  |
 *  |-- indexes (id, realName, displayName, publicKey, [privateKey], positionInTree, newRev, publishPrivateKey (0/1))
 *    |-- links (id, indexName, indexPublicKey)
 *    |-- files (id, filename, publicKey, mime, size)
 *      |-- metadatas (id, name, value)
 *
 * indexParents(indexId, folderId) # table de jointure
 * folderParents(folderId, parentId) # table de jointure
 *
 * </pre>
 *
 * positionInTree == position in its JTree branch.
 */
public class DatabaseManager {

	public DatabaseManager() {

	}


	/**
	 * @splashScreen can be null
	 * @return true if database is a new one
	 */
	public static boolean init(Hsqldb db, Config config, SplashScreen splashScreen) {
		boolean newDb;

		newDb = false;

		if (config.getValue("indexDatabaseVersion") == null) {
			newDb = true;
			config.setValue("indexDatabaseVersion", "3");
		} else {

			/* CONVERTIONS */

			if ("1".equals(config.getValue("indexDatabaseVersion"))) {
				if (splashScreen != null)
					splashScreen.setStatus("Converting database ...");
				if (convertDatabase_1_to_2(db))
					config.setValue("indexDatabaseVersion", "2");
				/* else
				 * TODO : Put a warning here and stop the plugin loading
				 */
			}

			if ("2".equals(config.getValue("indexDatabaseVersion"))) {
				if (splashScreen != null)
					splashScreen.setStatus("Converting database ...");
				if (convertDatabase_2_to_3(db))
					config.setValue("indexDatabaseVersion", "3");
			}

			/* ... */
		}


		createTables(db);

		return newDb;
	}


	/**
	 * Can be safely called, even if the tables already exist.
	 */
	public static void createTables(final Hsqldb db) {
		//sendQuery(db,
		//	  "SET IGNORECASE TRUE");

		sendQuery(db,
			  "CREATE CACHED TABLE indexFolders ("
			  + "id INTEGER IDENTITY NOT NULL,"
			  + "name VARCHAR(255) NOT NULL,"
			  + "positionInTree INTEGER NOT NULL,"
			  + "modifiableIndexes BOOLEAN NOT NULL," /* Obsolete */
			  + "parent INTEGER NULL,"
			  + "PRIMARY KEY (id),"
			  + "FOREIGN KEY (parent) REFERENCES indexFolders (id))");

		sendQuery(db,
			  "CREATE CACHED TABLE indexes ("
			  + "id INTEGER IDENTITY NOT NULL, "
			  + "originalName VARCHAR(255) NOT NULL, "
			  + "displayName VARCHAR(255) NULL, "
			  + "publicKey VARCHAR(255) NOT NULL, "
			  + "privateKey VARCHAR(255) NULL, "
			  + "publishPrivateKey BOOLEAN DEFAULT FALSE NOT NULL, "
			  + "author VARCHAR(255) NULL, "
			  + "positionInTree INTEGER NOT NULL, "
			  + "revision INTEGER NOT NULL, "
			  + "newRev BOOLEAN DEFAULT FALSE NOT NULL, "
			  + "parent INTEGER NULL, " /* direct parent */
			  + "PRIMARY KEY (id), "
			  + "FOREIGN KEY (parent) REFERENCES indexFolders (id))");

		/* direct AND indirect parents */
		sendQuery(db, /* this table avoid some horrible recursivities */
			  "CREATE CACHED TABLE indexParents ("
			  + "indexId INTEGER NOT NULL,"
			  + "folderId INTEGER NULL)");
		//+ "FOREIGN KEY (indexId) REFERENCES indexes (id)"
		//+ "FOREIGN KEY (folderId) REFERENCES indexFolders (id))");


		/* direct AND indirect parents */
		sendQuery(db, /* this table avoid some horrible recursivities */
			  "CREATE CACHED TABLE folderParents ("
			  + "folderId INTEGER NOT NULL,"
			  + "parentId INTEGER NULL)");
		//+ "FOREIGN KEY (folderId) REFERENCES indexFolders (id)"
		//+ "FOREIGN KEY (parentId) REFERENCES indexFolders (id))");


		sendQuery(db,
			  "CREATE CACHED TABLE categories ("
			  + "id INTEGER IDENTITY NOT NULL,"
			  + "name VARCHAR(255) NOT NULL)");

		sendQuery(db,
			  "CREATE CACHED TABLE files ("
			  + "id INTEGER IDENTITY NOT NULL,"
			  + "filename VARCHAR(255) NOT NULL,"
			  + "publicKey VARCHAR(350) NOT NULL," // key ~= 100 + filename == 255 max => 350
			  + "localPath VARCHAR(500) NULL,"
			  + "mime VARCHAR(50) NULL,"
			  + "size BIGINT NOT NULL,"
			  + "category INTEGER NULL,"
			  + "indexParent INTEGER NOT NULL,"
			  + "toDelete BOOLEAN DEFAULT FALSE NOT NULL,"
			  + "dontDelete BOOLEAN DEFAULT FALSE NOT NULL,"
			  + "PRIMARY KEY (id),"
			  + "FOREIGN KEY (indexParent) REFERENCES indexes (id),"
			  + "FOREIGN KEY (category) REFERENCES categories (id))");

		sendQuery(db,
			  "CREATE CACHED TABLE links ("
			  + "id INTEGER IDENTITY NOT NULL,"
			  + "publicKey VARCHAR(350) NOT NULL," // key ~= 100 + filename == 255 max
			  + "mark INTEGER NOT NULL,"
			  + "comment VARCHAR(512) NOT NULL,"
			  + "indexParent INTEGER NOT NULL,"
			  + "indexTarget INTEGER NULL,"
			  + "toDelete BOOLEAN DEFAULT false NOT NULL,"
			  + "dontDelete BOOLEAN DEFAULT false NOT NULL,"
			  + "PRIMARY KEY (id),"
			  + "FOREIGN KEY (indexParent) REFERENCES indexes (id),"
			  + "FOREIGN KEY (indexTarget) REFERENCES indexes (id))");

		sendQuery(db,
			  "CREATE CACHED TABLE metadataNames ("
			  + "id INTEGER IDENTITY NOT NULL,"
			  + "name VARCHAR(255) NOT NULL,"
			  + "PRIMARY KEY (id))");

		sendQuery(db,
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
		/* TODO : Add a warning here */

		sendQuery(db, "DROP TABLE metadatas");
		sendQuery(db, "DROP TABLE metadataNames");

		sendQuery(db, "DROP TABLE files");
		sendQuery(db, "DROP TABLE links");

		sendQuery(db, "DROP TABLE indexes");
		sendQuery(db, "DROP TABLE indexCategories");
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

	public static int getNextId(Hsqldb db, String table) {
			try {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("SELECT IDENTITY()+1 FROM "+
									 table);
				ResultSet res = st.executeQuery();

				if (res.next())
					return res.getInt(1);
				else
					return 1;

			} catch(SQLException e) {
				Logger.error(new DatabaseManager(), "Unable to get next id because: "+e.toString());
			}

			return -1;
	}


	public static int getNmbIndexes(Hsqldb db) {
		int nmb;

		try {
			final Connection c = db.getConnection();
			PreparedStatement st;

			st = c.prepareStatement("SELECT count(id) FROM indexes");
			st.execute();

			try {
				final ResultSet answer = st.getResultSet();
				answer.next();
				nmb = answer.getInt(1);
			} catch(final SQLException e) {
				nmb = 0;
			}

		} catch(final SQLException e) {
			Logger.error(new DatabaseManager(), "Unable to insert the new index category in the db, because: "+e.toString());

			return 0;
		}

		return nmb;
	}


	public static void exportDatabase(java.io.File dest, Hsqldb db, IndexTree indexTree, boolean withContent) {
		int nmbIndexes = getNmbIndexes(db);


		Logger.info(new DatabaseManager(), "Generating export ...");

		FileOutputStream outputStream;

		try {
			outputStream = new FileOutputStream(dest);
		} catch(final java.io.FileNotFoundException e) {
			Logger.warning(new DatabaseManager(), "Unable to create file '"+dest.toString()+"' ! not generated !");
			return;
		}

		final StreamResult streamResult = new StreamResult(outputStream);
		Document xmlDoc;

		final DocumentBuilderFactory xmlFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder xmlBuilder;

		try {
			xmlBuilder = xmlFactory.newDocumentBuilder();
		} catch(final javax.xml.parsers.ParserConfigurationException e) {
			Logger.error(new DatabaseManager(), "Unable to generate the index because : "+e.toString());
			return;
		}

		final DOMImplementation impl = xmlBuilder.getDOMImplementation();

		xmlDoc = impl.createDocument(null, "indexDatabase", null);

		final Element rootEl = xmlDoc.getDocumentElement();


		rootEl.appendChild(indexTree.getRoot().do_export(xmlDoc, withContent));


		/* Serialization */
		final DOMSource domSource = new DOMSource(xmlDoc);
		final TransformerFactory transformFactory = TransformerFactory.newInstance();

		Transformer serializer;

		try {
			serializer = transformFactory.newTransformer();
		} catch(final javax.xml.transform.TransformerConfigurationException e) {
			Logger.error(new DatabaseManager(), "Unable to save index because: "+e.toString());
			return;
		}

		serializer.setOutputProperty(OutputKeys.ENCODING,"UTF-8");
		serializer.setOutputProperty(OutputKeys.INDENT,"yes");

		/* final step */
		try {
			serializer.transform(domSource, streamResult);
		} catch(final javax.xml.transform.TransformerException e) {
			Logger.error(new DatabaseManager(), "Unable to save index because: "+e.toString());
			return;
		}

		Logger.info(new DatabaseManager(), "Export done");
	}


	public static void importDatabase(java.io.File source, IndexBrowserPanel indexBrowser, FCPQueueManager queueManager) {
		java.io.InputStream input;

		Logger.info(new DatabaseManager(), "Importing ...");

		try {
			input = new FileInputStream(source);
		} catch(final java.io.FileNotFoundException e) {
			Logger.error(new DatabaseManager(), "Unable to load XML: FileNotFoundException ('"+source.getPath()+"') ! : "+e.toString());
			return;
		}

		final DocumentBuilderFactory xmlFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder xmlBuilder;

		try {
			xmlBuilder = xmlFactory.newDocumentBuilder();
		} catch(final javax.xml.parsers.ParserConfigurationException e) {
			Logger.error(new DatabaseManager(), "Unable to load index because: "+e.toString());
			return;
		}

		Document xmlDoc;

		try {
			xmlDoc = xmlBuilder.parse(input);
		} catch(final org.xml.sax.SAXException e) {
			Logger.error(new DatabaseManager(), "Unable to load XML file because: "+e.toString());
			return;
		} catch(final java.io.IOException e) {
			Logger.error(new DatabaseManager(), "Unable to load index because: "+e.toString());
			return;
		}

		final Element rootEl = xmlDoc.getDocumentElement();

		Element e = (Element)rootEl.getElementsByTagName("indexCategory").item(0);

		IndexFolder importCategory = indexBrowser.getIndexTree().getRoot().getNewImportFolder(indexBrowser.getDb());

		importCategory.do_import(indexBrowser, e);

		indexBrowser.getIndexTree().getRoot().forceReload();
		indexBrowser.getIndexTree().refresh();

		Logger.info(new DatabaseManager(), "Import done");

	}



	/**
	 * used by convertDatabase_1_to_2()
	 */
	public static boolean insertChildIn(Hsqldb db, int folderId) throws SQLException {
		java.util.Vector results;
		int i = 0, j;

		Logger.notice(new DatabaseManager(), "Expanding folder "+Integer.toString(folderId));

		PreparedStatement st;

		st = db.getConnection().prepareStatement("SELECT id FROM indexFolders WHERE "+
							 ((folderId >= 0) ? "parent = ?" : "parent IS NULL"));

		if (folderId >= 0)
			st.setInt(1, folderId);

		ResultSet set = st.executeQuery();
		results = new java.util.Vector();

		while(set.next()) {
			results.add(new Integer(set.getInt("id")));
		}

		for (java.util.Iterator it = results.iterator();
		     it.hasNext();) {
			int nextId = ((Integer)it.next()).intValue();

			if (!insertChildIn(db, nextId)) {
				Logger.error(new DatabaseManager(), "halted");
				return false;
			}

			i++;

			st = db.getConnection().prepareStatement("SELECT folderId FROM folderParents WHERE parentId = ?");
			st.setInt(1, nextId);

			Vector childFolders = new Vector();

			j = 0;

			ResultSet rs = st.executeQuery();

			while(rs.next()) {
				j++;
				childFolders.add(new Integer(rs.getInt("folderId")));
			}

			for (Iterator ite = childFolders.iterator();
			     ite.hasNext();) {
				Integer a = (Integer)ite.next();

				st = db.getConnection().prepareStatement("INSERT INTO folderParents (folderId, parentId) "+
									 "VALUES (?, ?)");
				st.setInt(1, a.intValue());
				if (folderId < 0)
					st.setNull(2, Types.INTEGER);
				else
					st.setInt(2, folderId);

				st.execute();
			}



			st = db.getConnection().prepareStatement("SELECT indexId FROM indexParents WHERE folderId = ?");
			st.setInt(1, nextId);

			Vector childIndexes = new Vector();

			rs = st.executeQuery();

			while(rs.next()) {
				j++;
				childIndexes.add(new Integer(rs.getInt("indexId")));
			}

			if (j == 0) {
				Logger.warning(new DatabaseManager(), "empty folder (id = "+Integer.toString(nextId)+") ?");
			}

			for (Iterator ite = childIndexes.iterator();
			     ite.hasNext();) {
				Integer a = (Integer)ite.next();

				st = db.getConnection().prepareStatement("INSERT INTO indexParents (indexId, folderId) "+
									 "VALUES (?, ?)");
				st.setInt(1, a.intValue());
				if (folderId < 0)
					st.setNull(2, Types.INTEGER);
				else
					st.setInt(2, folderId);

				st.execute();
			}

		}

		Logger.notice(new DatabaseManager(), Integer.toString(i) + " child folder found for folder "+Integer.toString(folderId));

		return true;
	}



	public static boolean convertDatabase_1_to_2(Hsqldb db) {
		if (!sendQuery(db, "ALTER TABLE indexCategories RENAME TO indexFolders")) {
			Logger.error(new DatabaseManager(), "Error while converting the database (1 to 2) (renaming table indexCategories to indexfolders)");
			return false;
		}


		if (!sendQuery(db, "ALTER TABLE indexes ADD COLUMN newRev BOOLEAN DEFAULT false")) {
			Logger.error(new DatabaseManager(), "Error while converting the database (1 to 2) ! (adding column 'newRev' to the index table)");
			return false;
		}

		if (!sendQuery(db, "ALTER TABLE indexes ADD COLUMN publishPrivateKey BOOLEAN DEFAULT false")) {
			Logger.error(new DatabaseManager(), "Error while converting the database (1 to 2) ! (adding column 'publishPrivateKey' to the index table)");
			return false;
		}

		if (!sendQuery(db, "ALTER TABLE links ADD COLUMN toDelete BOOLEAN DEFAULT false")) {
			Logger.error(new DatabaseManager(), "Error while converting the database (1 to 2) ! (adding column to link table)");
			return false;
		}

		if (!sendQuery(db, "ALTER TABLE files ADD COLUMN toDelete BOOLEAN DEFAULT false")) {
			Logger.error(new DatabaseManager(), "Error while converting the database (1 to 2) ! (adding column to file table)");
			return false;
		}

		/* direct AND indirect parents */
		if (!sendQuery(db, /* this table avoid some horrible recusirvities */
			       "CREATE CACHED TABLE indexParents ("
			       + "indexId INTEGER NOT NULL,"
			       + "folderId INTEGER NULL)")) {
			Logger.error(new DatabaseManager(), "Unable to create table because");
			return false;
		}

		/* direct AND indirect parents */
		if (!sendQuery(db, /* this table avoid some horrible recursivities */
			       "CREATE CACHED TABLE folderParents ("
			       + "folderId INTEGER NOT NULL,"
			       + "parentId INTEGER NULL)")) {
			Logger.error(new DatabaseManager(), "Unable to create table because");
			return false;
		}


		if (!sendQuery(db,
			       "INSERT INTO folderParents (folderId, parentId) "+
			       "SELECT id, parent FROM indexFolders")) {
			Logger.error(new DatabaseManager(), "Error while converting (1_2_1)");
			return false;
		}

		if (!sendQuery(db,
			       "INSERT INTO indexParents (indexId, folderId) "+
			       "SELECT id, parent FROM indexes")) {
			Logger.error(new DatabaseManager(), "Error while converting (1_2_2)");
			return false;
		}


		try {
			insertChildIn(db, -1);
		} catch(SQLException e) {
			Logger.error(new DatabaseManager(), "Error while converting : "+e.toString());
			return false;
		}


		/* convert SSK into USK */

		try {
			PreparedStatement st;

			st = db.getConnection().prepareStatement("SELECT id, publicKey, originalName, revision FROM indexes");

			PreparedStatement subSt;
			subSt = db.getConnection().prepareStatement("UPDATE indexes "+
								    "SET publicKey = ? "+
								    "WHERE id = ?");

			ResultSet set = st.executeQuery();

			while(set.next()) {
				String key = set.getString("publicKey");

				if (key != null && key.startsWith("SSK@")) {
					int id = set.getInt("id");
					String name = set.getString("originalName");
					int rev = set.getInt("revision");

					String newKey;

					if (key.endsWith("/"))
						newKey = key.replaceFirst("SSK@", "USK@")+name+"/"+rev+"/"+name+".frdx";
					else
						newKey = key.replaceFirst("SSK@", "USK@")+"/"+name+"/"+rev+"/"+name+".frdx";

					Logger.notice(new DatabaseManager(), "Replacing "+key+" with "+newKey);

					subSt.setString(1, newKey);
					subSt.setInt(2, id);

					subSt.execute();
				}
			}

		} catch(SQLException e) {
			Logger.error(new DatabaseManager(), "Error while converting SSK into USK : "+e.toString());
			return false;
		}

		return true;
	}

	public static boolean convertDatabase_2_to_3(Hsqldb db) {
		if (!sendQuery(db, "ALTER TABLE links ADD COLUMN dontDelete BOOLEAN DEFAULT false")) {
			Logger.error(new DatabaseManager(), "Error while converting the database (2 to 3) ! (adding column to link table)");
			return false;
		}

		if (!sendQuery(db, "ALTER TABLE files ADD COLUMN dontDelete BOOLEAN DEFAULT false")) {
			Logger.error(new DatabaseManager(), "Error while converting the database (2 to 3) ! (adding column to file table)");
			return false;
		}

		return true;
	}
}
