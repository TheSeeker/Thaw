package thaw.plugins.index;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Iterator;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Observer;
import java.util.Observable;

import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
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

import thaw.core.FreenetURIHelper;
import thaw.core.Logger;
import thaw.core.I18n;
import thaw.fcp.FCPClientGet;
import thaw.fcp.FCPClientPut;
import thaw.fcp.FCPGenerateSSK;
import thaw.fcp.FCPQueueManager;
import thaw.fcp.FCPTransferQuery;
import thaw.plugins.Hsqldb;

import thaw.plugins.insertPlugin.DefaultMIMETypes;



public class Index extends Observable implements MutableTreeNode, FileAndLinkList, IndexTreeNode, Observer {

	private final Hsqldb db;
	private int id;
	private TreeNode parentNode;


	private String publicKey = null;
	/* needed for display: */
	private String privateKey = null;
	private int rev = -1;
	private String displayName = null;
	private boolean hasChanged = false;


	/* loaded only if asked explictly */
	private String realName = null;


	/**
	 * @deprecated Just don't use it !
	 */
	public Index() {
		db = null;
	}

	public Index(Hsqldb db, int id) {
		this.db = db;
		this.id = id;
	}

	/**
	 * Use it when you can have these infos easily ; else let the index do the job
	 */
	public Index(Hsqldb db, int id, TreeNode parentNode, String publicKey, int rev, String privateKey, String displayName, boolean hasChanged) {
		this(db, id);
		this.parentNode = parentNode;
		this.privateKey = privateKey;
		this.publicKey = publicKey;
		this.rev = rev;
		this.displayName = displayName;
		this.hasChanged = hasChanged;
	}


	/**
	 * Won't apply in the database !
	 */
	public void setId(int id) {
		this.id = id;
	}


	/**
	 * Is this node coming from the tree ?
	 */
	public boolean isInTree() {
		return (getParent() != null);
	}

	public TreeNode getParent() {
		return parentNode;
	}

	public Enumeration children() {
		return null;
	}

	public boolean getAllowsChildren() {
		return false;
	}

	public TreeNode getChildAt(int childIndex) {
		return null;
	}

	public int getChildCount() {
		return 0;
	}

	/**
	 * relative to tree, not indexes :p
	 */
	public int getIndex(TreeNode node) {
		return -1;
	}

	public void setParent(MutableTreeNode newParent) {
		parentNode = newParent;
		setParent(((IndexTreeNode)newParent).getId());
	}

	public void setParent(final int parentId) {
		synchronized(db.dbLock) {

			Logger.info(this, "setParent("+Integer.toString(parentId)+")");
			try {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("UPDATE indexes "+
									 "SET parent = ? "+
									 "WHERE id = ?");
				if (parentId >= 0)
					st.setInt(1, parentId);
				else
					st.setNull(1, Types.INTEGER);

				st.setInt(2, id);

				st.execute();


				if (parentId >= 0) {
					st = db.getConnection().prepareStatement("INSERT INTO indexParents (indexId, folderId) "+
										 " SELECT ?, parentId FROM folderParents "+
										 "   WHERE folderId = ?");
					st.setInt(1, id);
					st.setInt(2, parentId);

					st.execute();
				} /* else this parent has no parent ... :) */

				st = db.getConnection().prepareStatement("INSERT INTO indexParents (indexId, folderId) "+
									 "VALUES (?, ?)");

				st.setInt(1, id);
				if (parentId >= 0)
					st.setInt(2, parentId);
				else
					st.setNull(2, Types.INTEGER);

				st.execute();
			} catch(SQLException e) {
				Logger.error(this, "Error while changing parent : "+e.toString());
			}

		}
	}

	/**
	 * entry point
	 */
	public void removeFromParent() {
		Logger.info(this, "removeFromParent()");

		((IndexFolder)parentNode).remove(this);

		synchronized(db.dbLock) {

			PreparedStatement st;

			try {
				st = db.getConnection().prepareStatement("DELETE FROM indexParents "+
									 "WHERE indexId = ?");
				st.setInt(1, id);
				st.execute();

			} catch(SQLException e) {
				Logger.error(this, "Error while removing the index: "+e.toString());
			}
		}
	}

	public void remove(int index) {
		/* nothing to do */
	}

	public void remove(MutableTreeNode node) {
		/* nothing to do */
	}

	public void insert(MutableTreeNode child, int index) {
		/* nothing to do */
	}

	public boolean isLeaf() {
		return true;
	}


	public void setUserObject(Object o) {
		rename(o.toString());
	}

	public MutableTreeNode getTreeNode() {
		return this;
	}


	public void rename(final String name) {
		synchronized(db.dbLock) {

			try {
				final Connection c = db.getConnection();
				PreparedStatement st;

				st = c.prepareStatement("UPDATE indexes SET displayName = ? WHERE id = ?");
				st.setString(1, name);
				st.setInt(2, id);
				st.execute();

			} catch(final SQLException e) {
				Logger.error(this, "Unable to rename the index in '"+name+"', because: "+e.toString());
			}
		}

	}


	public void delete() {
		removeFromParent();

		synchronized(db.dbLock) {

			try {

				PreparedStatement st;

				purgeFileList();
				purgeLinkList();

				st = db.getConnection().prepareStatement("DELETE FROM indexParents "+
									 "WHERE indexId = ?");
				st.setInt(1, id);
				st.execute();

				Logger.notice(this, "DELETING AN INDEX");

				st = db.getConnection().prepareStatement("DELETE FROM indexes WHERE id = ?");
				st.setInt(1, id);
				st.execute();
			} catch(SQLException e) {
				Logger.error(this, "Unable to delete the index because : "+e.toString());
			}
		}
	}


	public void purgeLinkList() {
		synchronized(db.dbLock) {

			try {
				final Connection c = db.getConnection();
				final PreparedStatement st = c.prepareStatement("DELETE FROM links WHERE indexParent = ?");
				st.setInt(1, getId());
				st.execute();
			} catch(final SQLException e) {
				Logger.error(this, "Unable to purge da list ! Exception: "+e.toString());
			}
		}
	}

	public void purgeFileList() {
		synchronized(db.dbLock) {
			try {
				final Connection c = db.getConnection();
				final PreparedStatement st = c.prepareStatement("DELETE FROM files WHERE indexParent = ?");
				st.setInt(1, getId());
				st.execute();
			} catch(final SQLException e) {
				Logger.error(this, "Unable to purge da list ! Exception: "+e.toString());
			}
		}
	}

	public int getId() {
		return id;
	}

	public boolean loadData() {
		Logger.info(this, "loadData()");
		synchronized(db.dbLock) {
			try {
				PreparedStatement st =
					db.getConnection().prepareStatement("SELECT publicKey, revision, privateKey, displayName FROM indexes WHERE id = ? LIMIT 1");

				st.setInt(1, id);

				ResultSet set = st.executeQuery();

				if (set.next()) {
					publicKey = set.getString("publicKey");
					privateKey = set.getString("privateKey");
					rev = set.getInt("revision");
					displayName = set.getString("displayName");
					return true;
				} else {
					Logger.error(this, "Unable to find index "+Integer.toString(id)+" in the database ?!");
					return false;
				}
			} catch (final SQLException e) {
				Logger.error(this, "Unable to get public key because: "+e.toString());
			}
		}
		return false;
	}


	public String getPublicKey() {
		if (publicKey == null) {
			loadData();
		}

		return publicKey;
	}

	public int getRevision() {
		if (rev < 0) {
			loadData();
		}

		return rev;
	}

	public String getPrivateKey() {
		if (publicKey == null) { /* we rely on the publicKey because the privateKey is not often availabe */
			loadData();
		}

		return privateKey;
	}


	public boolean getPublishPrivateKey() {
		try {

			PreparedStatement st
				= db.getConnection().prepareStatement("SELECT publishPrivateKey FROM indexes WHERE id = ? LIMIT 1");

			st.setInt(1, id);

			ResultSet set = st.executeQuery();

			if (!set.next()) {
				Logger.error(this, "Unable to get publishPrivateKey value => not found !");
			}

			return set.getBoolean("publishPrivateKey");


		} catch(SQLException e){
			Logger.error(this, "Unable to get publishPrivateKey value because: "+e.toString());
		}

		return false;
	}

	public void setPublishPrivateKey(boolean val) {
		try {
			PreparedStatement st =
				db.getConnection().prepareStatement("UPDATE indexes "+
								    "SET publishPrivateKey = ? "+
								    "WHERE id = ?");
			st.setBoolean(1, val);
			st.setInt(2, id);
			st.execute();

		} catch(SQLException e){
			Logger.error(this, "Unable to set publishPrivateKey value because: "+e.toString());
		}
	}


	public void setPublicKey(String publicKey) {
		int rev = FreenetURIHelper.getUSKRevision(publicKey);

		setPublicKey(publicKey, rev);
	}


	/**
	 * Use directly this function only if you're sure that the rev is the same in the key
	 * @param publicKey must be an USK
	 */
	public void setPublicKey(String publicKey, int rev) {
		this.publicKey = publicKey;
		this.rev = rev;

		synchronized(db.dbLock) {

			try {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("UPDATE indexes "+
									 "SET publicKey = ?, revision = ? "+
									 "WHERE id = ?");
				st.setString(1, publicKey);
				st.setInt(2, rev);
				st.setInt(3, id);

				st.execute();
			} catch(SQLException e) {
				Logger.error(this, "Unable to set public Key because: "+e.toString());
			}
		}
	}


	public void setPrivateKey(String privateKey) {
		if (privateKey != null && !FreenetURIHelper.isAKey(privateKey))
			privateKey = null;

		this.privateKey = privateKey;

		synchronized(db.dbLock) {
			try {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("UPDATE indexes "+
									 "SET privateKey = ? "+
									 "WHERE id = ?");
				if (privateKey != null)
					st.setString(1, privateKey);
				else
					st.setNull(1, Types.VARCHAR);
				st.setInt(2, id);

				st.execute();
			} catch(SQLException e) {
				Logger.error(this, "Unable to set private Key because: "+e.toString());
			}
		}
	}


	public String getRealName() {
		if (realName != null)
			return realName;


		synchronized(db.dbLock) {
			try {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("SELECT originalName FROM indexes WHERE id = ?");

				st.setInt(1, id);
				ResultSet set = st.executeQuery();

				if (set.next()) {
					realName = set.getString("originalName");
					return realName;
				} else {
					Logger.error(this, "Unable to get index real name: not found");
					return null;
				}
			} catch(SQLException e) {
				Logger.error(this, "Unable to get real index name: "+e.toString());
			}
		}

		return null;
	}


	public String toString() {
		return toString(true);
	}

	public String toString(boolean withRev) {
		if (displayName == null || rev < 0) {
			loadData();
		}

		if (withRev) {
			if (rev > 0 || privateKey == null)
				return displayName + " (r"+Integer.toString(rev)+")";
			else {
				if (rev > 0)
					return displayName+" ["+I18n.getMessage("thaw.plugin.index.nonInserted")+"]";
				else
					return displayName;
			}
		} else
			return displayName;

	}



	public int insertOnFreenet(Observer o, IndexBrowserPanel indexBrowser, FCPQueueManager queueManager) {
		String privateKey = getPrivateKey();
		String publicKey = getPublicKey();
		int rev = getRevision();


		if (indexBrowser != null && indexBrowser.getMainWindow() != null) {
			synchronized(db.dbLock) {
				try {
					PreparedStatement st;

					st = db.getConnection().prepareStatement("SELECT id FROM links where indexParent = ? LIMIT 1");
					st.setInt(1, id);

					ResultSet set = st.executeQuery();

					if (!set.next()) {
						/* no link ?! we will warn the user */

						int ret =
							JOptionPane.showOptionDialog(indexBrowser.getMainWindow().getMainFrame(),
										     I18n.getMessage("thaw.plugin.index.indexWithNoLink").replaceAll("\\?", toString(false)),
										     I18n.getMessage("thaw.warning.title"),
										     JOptionPane.YES_NO_OPTION,
										     JOptionPane.WARNING_MESSAGE,
										     null,
										     null,
										     null);

						if (ret == JOptionPane.CLOSED_OPTION
						    || ret == JOptionPane.NO_OPTION) {
							return 0;
						}
					}

				} catch(SQLException e) {
					Logger.error(this, "Error while checking the link number before insertion : "+e.toString());
				}
			}
		}



		if (indexBrowser != null && indexBrowser.getIndexTree() != null
		    && indexBrowser.getIndexTree().isIndexUpdating(this)) {
			Logger.notice(this, "A transfer is already running !");
			return 0;
		}

		if (!FreenetURIHelper.isAKey(publicKey)
		    || !FreenetURIHelper.isAKey(privateKey)) { /* non modifiable */
			Logger.notice(this, "Tried to insert an index for which we don't have the private key ...");
			return 0;
		}

		String tmpdir = System.getProperty("java.io.tmpdir");

		if (tmpdir == null)
			tmpdir = "";
		else
			tmpdir = tmpdir + java.io.File.separator;

		File targetFile = new java.io.File(tmpdir + getRealName() +".frdx");


		Logger.info(this, "Generating index ...");

		if (!generateXML(targetFile.getAbsolutePath()))
			return 0;

		FCPClientPut put;

		if(targetFile.exists()) {
			Logger.info(this, "Inserting new version");

			String key = FreenetURIHelper.changeUSKRevision(publicKey, rev, 1);

			rev++;

			put = new FCPClientPut(targetFile, 2, rev, realName, privateKey, 2, true, 0);
			put.setMetadata("ContentType", "application/x-freenet-index");

			if (indexBrowser != null && indexBrowser.getIndexTree() != null)
				indexBrowser.getIndexTree().addUpdatingIndex(this);

			queueManager.addQueryToThePendingQueue(put);

			put.addObserver(this);
			this.addObserver(o);

			setPublicKey(key, rev);

		} else {
			Logger.warning(this, "Index not generated !");
			return 0;
		}


		return 1;
	}

	public int downloadFromFreenet(Observer o, IndexTree tree, FCPQueueManager queueManager) {
		return downloadFromFreenet(o, tree, queueManager, -1);
	}


	/**
	 * if true, when the transfer will finish, the index public key will be updated
	 */
	private boolean rewriteKey = true;

	private IndexTree indexTree = null;


	public int downloadFromFreenet(Observer o, IndexTree tree, FCPQueueManager queueManager, int specificRev) {
		FCPClientGet clientGet;
		String publicKey;

		int rev = getRevision();

		indexTree = tree;

		rewriteKey = true;

		publicKey = getPublicKey();

		if (tree != null && tree.isIndexUpdating(this)) {
			Logger.notice(this, "A transfer is already running !");
			return 0;
		}

		if (publicKey == null) {
			Logger.error(this, "No public key !! Can't get the index !");
			return 0;
		}

		Logger.info(this, "Getting lastest version ...");

		String key;

		/* We will trust the node for the incrementation
		   execept if a rev is specified */


		if (specificRev >= 0) {
			key = FreenetURIHelper.convertUSKtoSSK(publicKey);
			key = FreenetURIHelper.changeSSKRevision(key, specificRev, 0);
			rewriteKey = false;
		} else {
			key = publicKey;
			rewriteKey = true;
		}

		Logger.info(this, "Updating index ...");
		Logger.debug(this, "Key asked: "+key);


		clientGet = new FCPClientGet(key, 2, 2, false, -1,
					     System.getProperty("java.io.tmpdir"));

		/*
		 * These requests are usually quite fast, and don't consume too much
		 * of bandwith / CPU. So we can skip the queue and start immediatly
		 * (and like this, they won't appear in the queue)
		 */
		clientGet.start(queueManager);

		if (tree != null)
			tree.addUpdatingIndex(this);

		clientGet.addObserver(this);

		this.addObserver(o);

		return 1;
	}



	public void update(Observable o, Object param) {
		if (o instanceof FCPClientGet) {
			FCPClientGet get = (FCPClientGet)o;

			String key = get.getFileKey();

			int oldRev = rev;
			int newRev = FreenetURIHelper.getUSKRevision(key);

			if (rewriteKey) {
				setPublicKey(key, newRev);
			}

			if (oldRev < newRev)
				setHasChangedFlag(true);

			if (get.isFinished() && get.isSuccessful()) {
				String path = get.getPath();

				if (path != null) {
					loadXML(path);
				} else
					Logger.error(this, "No path specified in transfer ?!");
			}
		}


		/* nothing special to do if it is an insert */
		/* TODO : check if it's successful, else merge if it's due to a collision */


		if (o instanceof FCPTransferQuery) {
			FCPTransferQuery transfer = (FCPTransferQuery)o;

			if (transfer.isFinished()) {
				String path = transfer.getPath();

				final java.io.File fl = new java.io.File(path);
				fl.delete();

				((Observable)transfer).deleteObserver(this);
				if (indexTree != null)
					indexTree.removeUpdatingIndex(this);

				setChanged();
				notifyObservers();
			}
		}

	}


	////// FILE LIST ////////

	public Vector getFileList(String columnToSort, boolean asc) {
		synchronized(db.dbLock) {

			try {
				Vector files = new Vector();

				PreparedStatement st;

				if (columnToSort == null) {
					st = db.getConnection().prepareStatement("SELECT id, filename, publicKey, localPath, mime, size "+
										 "FROM files WHERE indexParent = ?");
				} else {
					st = db.getConnection().prepareStatement("SELECT id, filename, publicKey, localPath, mime, size "+
										 "FROM files WHERE indexParent = ? ORDER by "+
										 columnToSort + (asc == true ? "" : " DESC"));
				}

				st.setInt(1, id);

				ResultSet rs = st.executeQuery();

				while(rs.next()) {
					int file_id = rs.getInt("id");
					String filename = rs.getString("filename");
					String file_publicKey = rs.getString("publicKey");
					String lp = rs.getString("localPath");
					java.io.File localPath = (lp == null ? null : new java.io.File(lp));
					String mime = rs.getString("mime");
					long size = rs.getLong("size");

					thaw.plugins.index.File file =
						new thaw.plugins.index.File(db, file_id, filename, file_publicKey,
									    localPath, mime, size, id);
					files.add(file);
				}

				return files;

			} catch(SQLException e) {
				Logger.error(this, "SQLException while getting file list: "+e.toString());
			}
		}
		return null;
	}


	//// LINKS ////

	public Vector getLinkList(String columnToSort, boolean asc) {
		synchronized(db.dbLock) {

			try {
				Vector links = new Vector();

				PreparedStatement st;

				st = db.getConnection().prepareStatement("SELECT id, publicKey FROM links WHERE indexParent = ?");

				st.setInt(1, id);

				ResultSet res = st.executeQuery();

				while(res.next()) {
					Link l = new Link(db, res.getInt("id"), res.getString("publicKey"), this);
					links.add(l);
				}

				return links;

			} catch(SQLException e) {
				Logger.error(this, "SQLException while getting link list: "+e.toString());
			}
		}

		return null;
	}

	//// XML ////
	public boolean generateXML(String path) {
		try {
			generateXML(new FileOutputStream(new File(path)));
			return true;
		} catch(java.io.FileNotFoundException e) {
			Logger.error(this, "File not found exception ?!");
		}
		return false;
	}

	public void generateXML(final OutputStream out) {
		StreamResult streamResult;

		streamResult = new StreamResult(out);

		Document xmlDoc;

		final DocumentBuilderFactory xmlFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder xmlBuilder;

		try {
			xmlBuilder = xmlFactory.newDocumentBuilder();
		} catch(final javax.xml.parsers.ParserConfigurationException e) {
			Logger.error(this, "Unable to generate the index because : "+e.toString());
			return;
		}

		final DOMImplementation impl = xmlBuilder.getDOMImplementation();

		xmlDoc = impl.createDocument(null, "index", null);

		final Element rootEl = xmlDoc.getDocumentElement();

		fillInRootElement(rootEl, xmlDoc);

		/* Serialization */
		final DOMSource domSource = new DOMSource(xmlDoc);
		final TransformerFactory transformFactory = TransformerFactory.newInstance();

		Transformer serializer;

		try {
			serializer = transformFactory.newTransformer();
		} catch(final javax.xml.transform.TransformerConfigurationException e) {
			Logger.error(this, "Unable to save index because: "+e.toString());
			return;
		}

		serializer.setOutputProperty(OutputKeys.ENCODING,"UTF-8");
		serializer.setOutputProperty(OutputKeys.INDENT,"yes");

		/* final step */
		try {
			serializer.transform(domSource, streamResult);
		} catch(final javax.xml.transform.TransformerException e) {
			Logger.error(this, "Unable to save index because: "+e.toString());
			return;
		}
	}

	public boolean fillInRootElement(Element rootEl, Document xmlDoc) {
		rootEl.appendChild(getXMLHeader(xmlDoc));
		rootEl.appendChild(getXMLLinks(xmlDoc));
		rootEl.appendChild(getXMLFileList(xmlDoc));

		return true;
	}

	public Element getXMLHeader(final Document xmlDoc) {
		final Element header = xmlDoc.createElement("header");

		final Element title = xmlDoc.createElement("title");
		final Text titleText = xmlDoc.createTextNode(toString(false));
		title.appendChild(titleText);

		header.appendChild(title);

		if (getPublishPrivateKey() && getPrivateKey() != null) {
			final Element privateKeyEl = xmlDoc.createElement("privateKey");
			final Text privateKeyText = xmlDoc.createTextNode(getPrivateKey());
			privateKeyEl.appendChild(privateKeyText);

			header.appendChild(privateKeyEl);
		}


		/* TODO : Author */

		return header;
	}

	public Element getXMLLinks(final Document xmlDoc) {
		final Element links = xmlDoc.createElement("indexes");
		synchronized(db.dbLock) {
			try {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("SELECT publicKey FROM links WHERE indexParent = ?");
				st.setInt(1, id);

				ResultSet set = st.executeQuery();

				while (set.next()) {
					final Element xmlLink = xmlDoc.createElement("index");

					xmlLink.setAttribute("key", set.getString("publicKey"));

					if (xmlLink != null)
						links.appendChild(xmlLink);
					else
						Logger.warning(this, "Unable to get XML for a link => Gruick da link");

				}
			} catch(SQLException e) {
				Logger.error(this, "Error while getting link list for XML : "+e.toString());
			}
		}

		return links;
	}

	public Element getXMLFileList(final Document xmlDoc) {
		final Element files = xmlDoc.createElement("files");

		synchronized(db.dbLock) {
			try {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("SELECT id, filename, publicKey, size, mime "+
									 "FROM files "+
									 "WHERE indexParent = ?");

				st.setInt(1, id);

				ResultSet set = st.executeQuery();

				while(set.next()) {
					String pubKey = set.getString("publicKey");

					if (pubKey == null)
						continue;

					pubKey = pubKey.trim();

					if (!FreenetURIHelper.isAKey(pubKey)) {
						Logger.notice(this, "One of the file key wasn't generated => not added");
						continue;
					}

					final Element xmlFile = xmlDoc.createElement("file");

					xmlFile.setAttribute("id", set.getString("id"));
					xmlFile.setAttribute("key", pubKey);
					xmlFile.setAttribute("size", set.getString("size"));
					if (set.getString("mime") == null)
						xmlFile.setAttribute("mime", DefaultMIMETypes.guessMIMEType(set.getString("filename")));
					else
						xmlFile.setAttribute("mime", set.getString("mime"));

					if(xmlFile != null)
						files.appendChild(xmlFile);
					else
						Logger.warning(this, "Public key wasn't generated ! Not added to the index !");
				}

			} catch(SQLException e) {
				Logger.error(this, "Error while getting file list for XML : "+e.toString());
			}
		}

		return files;
	}



	public void loadXML(final String filePath) {
		loadXML(filePath, true);
	}


	/**
	 * @param clean if set to false, will do a merge
	 */
	public void loadXML(final String filePath, boolean clean) {
		try {
			loadXML(new FileInputStream(filePath), clean);
		} catch(final java.io.FileNotFoundException e) {
			Logger.error(this, "Unable to load XML: FileNotFoundException ('"+filePath+"') ! : "+e.toString());
		}
	}

	public synchronized void loadXML(final java.io.InputStream input, boolean clean) {
		final DocumentBuilderFactory xmlFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder xmlBuilder;

		try {
			xmlBuilder = xmlFactory.newDocumentBuilder();
		} catch(final javax.xml.parsers.ParserConfigurationException e) {
			Logger.error(this, "Unable to load index because: "+e.toString());
			return;
		}

		Document xmlDoc;

		try {
			Logger.debug(this, "XML parser ready");
			xmlDoc = xmlBuilder.parse(input);
			Logger.info(this, "Index parsed");
		} catch(final org.xml.sax.SAXException e) {
			Logger.error(this, "Unable to load index because: "+e.toString());
			return;
		} catch(final java.io.IOException e) {
			Logger.error(this, "Unable to load index because: "+e.toString());
			return;
		}

		final Element rootEl = xmlDoc.getDocumentElement();

		Logger.info(this, "Extracting informations from index ...");
		loadXML(rootEl, clean);
		Logger.info(this, "Extraction done");
	}

	public void loadXML(Element rootEl, boolean clean) {
		loadHeader(rootEl);
		loadLinks(rootEl, clean);
		loadFileList(rootEl, clean);
	}


	public void loadHeader(final Element rootEl) {
		final Element header = (Element)rootEl.getElementsByTagName("header").item(0);

		if (publicKey == null)
			loadData();

		String pKey = getHeaderElement(header, "privateKey");
		if (pKey != null) {
			if (privateKey == null || privateKey.trim().equals(pKey.trim())) {
				/* the public key was published, we will have to do the same later */
				setPublishPrivateKey(true);
			}
			else
				setPublishPrivateKey(false);

			if (privateKey == null)
				setPrivateKey(pKey.trim());
		}
	}

	protected String getHeaderElement(final Element header, final String name) {
		try {
			if (header == null)
				return null;

			final NodeList nl = header.getElementsByTagName(name);

			if (nl == null)
				return null;

			final Element sub = (Element)nl.item(0);

			if (sub == null)
				return null;

			final Text txt = (Text)sub.getFirstChild();

			if (txt == null)
				return null;

			return txt.getData();
		} catch(final Exception e) {
			Logger.notice(this, "Unable to get header element '"+name+"', because: "+e.toString());
			return null;
		}
	}

	public void loadLinks(final Element rootEl, boolean clean) {
		final Element links = (Element)rootEl.getElementsByTagName("indexes").item(0);

		if (links == null) {
			Logger.notice(this, "No links in index !");
			return;
		}

		final NodeList list = links.getChildNodes();

		PreparedStatement updateSt = null;
		PreparedStatement insertSt = null;
		int nextId;

		synchronized(db.dbLock) {
			try {
				updateSt =
					db.getConnection().prepareStatement("UPDATE links "+
									    "SET publicKey = ?, "+
									    "toDelete = FALSE "+
									    "WHERE indexParent = ? "+
									    "AND LOWER(publicKey) LIKE ?");
				insertSt =
					db.getConnection().prepareStatement("INSERT INTO links "
									    + "(id, publicKey, mark, comment, indexParent, indexTarget) "
									    + "VALUES (?, ?, 0, ?, ?, NULL)");
				if ((nextId = DatabaseManager.getNextId(db, "links")) < 0)
					return;

				PreparedStatement st =
					db.getConnection().prepareStatement("UPDATE links "+
									    "SET toDelete = TRUE "+
									    "WHERE indexParent = ?");
				st.setInt(1, id);
				st.execute();
			} catch(SQLException exc) {
				Logger.error(this, "Unable to prepare statement because: "+exc.toString());
				return;
			}

			if (list.getLength() < 0) {
				Logger.notice (this, "No link ?!");
			}

			for(int i = 0; i < list.getLength() ; i++) {
				if(list.item(i).getNodeType() == Node.ELEMENT_NODE) {
					final Element e = (Element)list.item(i);

					String key = e.getAttribute("key");

					if (key == null) {
						Logger.notice(this, "No key in this link ?!");
						continue;
					}

					key = key.trim();

					try {
						updateSt.setString(1, key);
						updateSt.setInt(2, id);
						updateSt.setString(3, key);

						if (updateSt.executeUpdate() <= 0) {

							insertSt.setInt(1, nextId);
							insertSt.setString(2, key);
							insertSt.setString(3, "No comment"); /* comment not used at the moment */
							insertSt.setInt(4, id);

							insertSt.execute();
							nextId++;
						}

					} catch(SQLException exc) {
						Logger.error(this, "Unable to add link because: "+exc.toString());
					}
				}
			} /* for() */

			if (clean) {
				try {
					PreparedStatement st;

					st = db.getConnection().prepareStatement("DELETE FROM links "+
											 "WHERE toDelete = TRUE "+
											 "AND indexParent = ?");
					st.setInt(1, id);
					st.execute();
				} catch(SQLException exc) {
					Logger.error(this, "error while clean index (links) : "+exc.toString());
				}

			}
		}

	}

	public void loadFileList(final Element rootEl, boolean clean) {
		final Element filesEl = (Element)rootEl.getElementsByTagName("files").item(0);

		if (filesEl == null) {
			Logger.notice(this, "No file in the index !");
			return;
		}

		PreparedStatement updateSt = null;
		PreparedStatement insertSt = null;
		int nextId;

		synchronized(db.dbLock) {
			try {
				updateSt =
					db.getConnection().prepareStatement("UPDATE files "+
									    "SET filename = ?, "+
									    "publicKey = ?, " +
									    "mime = ?, "+
									    "size = ?, "+
									    "toDelete = FALSE "+
									    "WHERE indexParent = ? "+
									    "AND LOWER(publicKey) LIKE ?");
				insertSt =
					db.getConnection().prepareStatement("INSERT INTO files "
									    + "(id, filename, publicKey, localPath, mime, size, category, indexParent) "
									    + "VALUES (?, ?, ?, NULL, ?, ?, NULL, ?)");
				if ((nextId = DatabaseManager.getNextId(db, "files")) < 0)
					return;

				PreparedStatement st;

				st = db.getConnection().prepareStatement("UPDATE files "+
									 "SET toDelete = TRUE "+
									 "WHERE indexParent = ?");
				st.setInt(1, id);
				st.execute();
			} catch(SQLException exc) {
				Logger.error(this, "Unable to prepare statement because: "+exc.toString());
				return;
			}

			final NodeList list = filesEl.getChildNodes();

			if (list.getLength() < 0) {
				Logger.notice(this, "No files?!");
			}

			for(int i = 0; i < list.getLength() ; i++) {
				if(list.item(i).getNodeType() == Node.ELEMENT_NODE) {

					final Element e = (Element)list.item(i);

					String key = e.getAttribute("key");

					if (key == null)
						continue;

					key = key.trim();

					long size = 0;

					if (e.getAttribute("size") != null)
						size = Long.parseLong(e.getAttribute("size"));
					String mime = e.getAttribute("mime");
					String filename = FreenetURIHelper.getFilenameFromKey(key);

					try {
						updateSt.setString(1, filename);
						updateSt.setString(2, key);
						updateSt.setString(3, mime);
						updateSt.setLong(4, size);
						updateSt.setInt(5, id);
						updateSt.setString(6, FreenetURIHelper.getComparablePart(key)+"%");

						int rs = updateSt.executeUpdate();

						if (rs <= 0) {
							insertSt.setInt(1, nextId);
							insertSt.setString(2, filename);
							insertSt.setString(3, key);
							insertSt.setString(4, mime);
							insertSt.setLong(5, size);
							insertSt.setInt(6, id);

							nextId++;

							insertSt.execute();
						}
					} catch(SQLException exc) {
						Logger.error(this, "error while adding file: "+exc.toString());
						Logger.error(this, "Next id : "+Integer.toString(nextId));
						exc.printStackTrace();
						return;
					}

				}
			} /* for each file in the XML */


			if (clean) {
				try {
					PreparedStatement st;

					st = db.getConnection().prepareStatement("DELETE FROM files "+
										 "WHERE toDelete = TRUE "+
										 "AND indexParent = ?");
					st.setInt(1, id);
					st.execute();
				} catch(SQLException exc) {
					Logger.error(this, "error while clean index (files) : "+exc.toString());
				}
			}
		}

	} /* loadFileList() */


	static String getNameFromKey(final String key) {
		String name = null;

		name = FreenetURIHelper.getFilenameFromKey(key);

		if (name == null)
			return null;

		/* quick and dirty */
		name = name.replaceAll(".xml", "");
		name = name.replaceAll(".frdx", "");

		return name;
	}


	public boolean isModifiable() {
		if (getPrivateKey() != null)
			return true;

		return false;
	}


	public static boolean isAlreadyKnown(Hsqldb db, String key) {
		if (key.length() < 40) {
			Logger.error(new Index(), "isAlreadyKnown: Invalid key: "+key);
			return false;
		}

		synchronized(db.dbLock) {
			try {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("SELECT publicKey from indexes WHERE LOWER(publicKey) LIKE ? LIMIT 1");

				st.setString(1, FreenetURIHelper.getComparablePart(key) +"%");

				ResultSet res = st.executeQuery();

				if (res.next()) {
					return true;
				}

			} catch(final SQLException e) {
				Logger.error(new Index(), "isAlreadyKnown: Unable to check if link '"+key+"' point to a know index because: "+e.toString());
			}
		}

		return false;
	}


	public void forceHasChangedReload() {
		loadData();
	}


	public boolean hasChanged() {
		if (publicKey == null) {
			loadData();
		}

		return hasChanged;
	}


	public boolean setHasChangedFlagInMem(boolean flag) {
		hasChanged = flag;
		return true;
	}


	/**
	 * @return true if a change was done
	 */
	public boolean setHasChangedFlag(boolean flag) {
		setHasChangedFlagInMem(flag);

		synchronized(db.dbLock) {
			try {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("UPDATE indexes SET newRev = ? "+
									 "WHERE id = ?");

				st.setBoolean(1, flag);
				st.setInt(2, id);
				if (st.executeUpdate() > 0)
					return true;
				return false;
			} catch(SQLException e) {
				Logger.error(this, "Unable to change 'hasChanged' flag because: "+e.toString());
			}
		}

		return false;
	}


	public Element do_export(Document xmlDoc, boolean withContent) {
		Element e = xmlDoc.createElement("fullIndex");

		e.setAttribute("displayName", toString(false));
		e.setAttribute("publicKey", getPublicKey());
		if (getPrivateKey() != null)
			e.setAttribute("privateKey", getPrivateKey());

		if (withContent) {
			fillInRootElement(e, xmlDoc);
		}

		return e;
	}


	public void do_import(IndexBrowserPanel indexBrowser, Element e) {
		loadXML(e, true);
	}


	public boolean equals(Object o) {
		if (o == null || !(o instanceof Index))
			return false;

		if (((Index)o).getId() == getId())
			return true;
		return false;
	}
}
