package thaw.plugins.index;

import java.sql.*;

import java.io.FileOutputStream;
import java.io.FileInputStream;
import javax.swing.tree.DefaultMutableTreeNode;
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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;


import thaw.core.Logger;
import thaw.plugins.Hsqldb;
import thaw.fcp.FCPQueueManager;

/**
 * Creates all the tables used to save the indexes,
 * manages structure changes if needed, etc.
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

		IndexCategory importCategory = indexBrowser.getIndexTree().getRoot().getNewImportFolder();

		importCategory.do_import(e);

		Logger.info(new DatabaseManager(), "Import done");

	}
}
