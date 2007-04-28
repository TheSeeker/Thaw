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
import java.util.Enumeration;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

/* DOM */

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



/* SAX */

import org.xml.sax.*;
import org.xml.sax.helpers.LocatorImpl;

import java.io.IOException;

import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;


import thaw.core.I18n;
import thaw.core.Logger;

import thaw.fcp.FreenetURIHelper;
import thaw.fcp.FCPClientGet;
import thaw.fcp.FCPClientPut;
import thaw.fcp.FCPQueueManager;
import thaw.fcp.FCPTransferQuery;
import thaw.fcp.FCPGenerateSSK;

import thaw.plugins.Hsqldb;
import thaw.plugins.insertPlugin.DefaultMIMETypes;
import thaw.plugins.signatures.Identity;


public class Index extends Observable implements MutableTreeNode, FileAndLinkList, IndexTreeNode, Observer {
	private final static long MAX_SIZE = 5242880; /* 5MB */


	private final Hsqldb db;
	private int id;
	private TreeNode parentNode;


	private String publicKey = null;
	/* needed for display: */
	private String privateKey = null;
	private int rev = -1;
	private String displayName = null;
	private boolean hasChanged = false;
	private boolean newComment = false;


	/* loaded only if asked explictly */
	private String realName = null;

	/* when all the comment fetching will failed,
	   loading will stop */
	public final static int COMMENT_FETCHING_RUNNING_AT_THE_SAME_TIME = 5;
	private int lastCommentRev = 0;
	private int nmbFailedCommentFetching = 0;


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
	public Index(Hsqldb db, int id, TreeNode parentNode, String publicKey, int rev, String privateKey, String displayName, boolean hasChanged, boolean newComment) {
		this(db, id);
		this.parentNode = parentNode;
		this.privateKey = privateKey;
		this.publicKey = publicKey;
		this.rev = rev;
		this.displayName = displayName;
		this.hasChanged = hasChanged;
		this.newComment = newComment;
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
				purgeCommentKeys();

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
		Logger.debug(this, "loadData()");
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
			Logger.debug(this, "getPublicKey() => loadData()");
			loadData();
		}

		return publicKey;
	}

	public int getRevision() {
		if (rev < 0) {
			Logger.debug(this, "getRevision() => loadData()");
			loadData();
		}

		return rev;
	}

	public String getPrivateKey() {
		if (publicKey == null) { /* we rely on the publicKey because the privateKey is not often availabe */
			Logger.debug(this, "getPrivateKey() => loadData()");
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


				/* we update also all the links in the index with the private key */

				st = db.getConnection().prepareStatement("SELECT links.id, links.publicKey "+
									 "FROM LINKS JOIN INDEXES ON links.indexParent = indexes.id "+
									 "WHERE indexes.privateKey IS NOT NULL AND LOWER(publicKey) LIKE ?");

				st.setString(1, FreenetURIHelper.getComparablePart(publicKey)+"%");
				ResultSet res = st.executeQuery();


				PreparedStatement updateLinkSt;

				updateLinkSt = db.getConnection().prepareStatement("UPDATE links SET publicKey = ? WHERE id = ?");

				while(res.next()) {
					String pubKey = res.getString("publicKey").replaceAll(".xml", ".frdx");

					if (FreenetURIHelper.compareKeys(pubKey, publicKey)) {
						updateLinkSt.setString(1, publicKey);
						updateLinkSt.setInt(2, res.getInt("id"));
						updateLinkSt.execute();
					}
				}

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
			Logger.debug(this, "toString() => loadData()");
			loadData();
		}

		if (withRev) {
			if (rev > 0 || (rev == 0 && privateKey == null))
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


	private IndexTree indexTree = null;


	public int insertOnFreenet(Observer o, IndexBrowserPanel indexBrowser, FCPQueueManager queueManager) {
		String privateKey = getPrivateKey();
		String publicKey = getPublicKey();
		int rev = getRevision();

		if (indexBrowser != null && indexBrowser.getMainWindow() != null) {
			indexTree = indexBrowser.getIndexTree();

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


			try {
				PreparedStatement st;

				String query = "UPDATE # SET dontDelete = FALSE WHERE indexParent = ?";

				st = db.getConnection().prepareStatement(query.replaceFirst("#", "files"));
				st.setInt(1, id);
				st.execute();

				st = db.getConnection().prepareStatement(query.replaceFirst("#", "links"));
				st.setInt(1, id);
				st.execute();

			} catch(SQLException e) {
				Logger.error(this, "Error while reseting dontDelete flags: "+e.toString());
			}

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
	private FCPQueueManager queueManager;


	public int downloadFromFreenet(Observer o, IndexTree tree, FCPQueueManager queueManager, int specificRev) {
		FCPClientGet clientGet;
		String publicKey;

		this.queueManager = queueManager;

		int rev = getRevision();

		indexTree = tree;

		rewriteKey = true;

		publicKey = getPublicKey();
		String privateKey = getPrivateKey();

		if (rev <= 0 && privateKey != null) {
			Logger.error(this, "Can't update an non-inserted index !");
			return 0;
		}

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

		if (specificRev >= 0) {
			key = FreenetURIHelper.convertUSKtoSSK(publicKey);
			key = FreenetURIHelper.changeSSKRevision(key, specificRev, 0);
			rewriteKey = false;
		} else {
			key = publicKey;
			rewriteKey = true;
		}

		if (rev < 0)
			rewriteKey = false;

		Logger.info(this, "Updating index ...");


		if (key.startsWith("USK")) {
			int negRev = 0;

			if ((negRev = FreenetURIHelper.getUSKRevision(key)) > 0) {
				negRev = -1 * negRev;
				key = FreenetURIHelper.changeUSKRevision(key, negRev, 0);
			}
		}



		Logger.debug(this, "Key asked: "+key);


		clientGet = new FCPClientGet(key, 2, 2, false, 5,
					     System.getProperty("java.io.tmpdir"),
					     MAX_SIZE, true /* <= noDDA */);

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

			if (get.isFinished() && get.isSuccessful()) {

				String key = get.getFileKey();

				int oldRev = rev;
				int newRev = FreenetURIHelper.getUSKRevision(key);

				if (rewriteKey) {
					setPublicKey(key, newRev);
				}

				if (oldRev < newRev)
					setHasChangedFlag(true);

				String path = get.getPath();

				if (path != null) {
					loadXML(path);

					if (getCommentPublicKey() != null)
						loadComments(queueManager);
					else if (indexTree != null)
						indexTree.removeUpdatingIndex(this);
				} else
					Logger.error(this, "No path specified in transfer ?!");
			}
		}

		if (o instanceof FCPClientPut) {
			/* TODO : check if it's successful, else merge if it's due to a collision */
			if (((FCPClientPut)o).isFinished()) {
				if (indexTree != null)
					indexTree.removeUpdatingIndex(this);
			}
		}

		if (o instanceof Comment) {

			Comment c = (Comment)o;

			if (c.exists()) {
				nmbFailedCommentFetching = 0;

				if (c.isNew()) {
					Logger.info(this, "New comment !");

					setNewCommentFlag(true);

					setChanged();
					notifyObservers();
				}

			} else {
				nmbFailedCommentFetching++;
			}

			if (nmbFailedCommentFetching > COMMENT_FETCHING_RUNNING_AT_THE_SAME_TIME +1) {
				if (indexTree != null) {
					Logger.info(this, "All the comments should be fetched");
					indexTree.removeUpdatingIndex(this);
				}
			}
			else {
				lastCommentRev++;
				Comment comment = new Comment(db, this, lastCommentRev, null, null);
				comment.addObserver(this);
				comment.fetchComment(queueManager);
			}

		}

		if (o instanceof FCPTransferQuery) {
			FCPTransferQuery transfer = (FCPTransferQuery)o;

			if (transfer.isFinished()) {
				String path = transfer.getPath();

				final java.io.File fl = new java.io.File(path);
				fl.delete();

				((Observable)transfer).deleteObserver(this);

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
									    localPath, mime, size, id, this);
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

				st = db.getConnection().prepareStatement("SELECT id, publicKey, blackListed FROM links WHERE indexParent = ?");

				st.setInt(1, id);

				ResultSet res = st.executeQuery();

				while(res.next()) {
					Link l = new Link(db, res.getInt("id"), res.getString("publicKey"),
							  res.getBoolean("blackListed"),
							  this);
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

		if (canHaveComments())
			rootEl.appendChild(getXMLCommentInfos(xmlDoc));

		return true;
	}

	public Element getXMLHeader(final Document xmlDoc) {
		final Element header = xmlDoc.createElement("header");

		final Element title = xmlDoc.createElement("title");
		final Text titleText = xmlDoc.createTextNode(toString(false));
		title.appendChild(titleText);

		header.appendChild(title);


		final Element thawVersion = xmlDoc.createElement("client");
		final Text versionText = xmlDoc.createTextNode("Thaw "+thaw.core.Main.VERSION);
		thawVersion.appendChild(versionText);

		header.appendChild(thawVersion);

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

					//xmlFile.setAttribute("id", set.getString("id"));
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



	public Element getXMLCommentInfos(final Document xmlDoc) {
		final Element infos = xmlDoc.createElement("comments");

		infos.setAttribute("publicKey", getCommentPublicKey());
		infos.setAttribute("privateKey", getCommentPrivateKey());

		try {
			synchronized(db.dbLock) {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("SELECT rev FROM indexCommentBlackList WHERE indexId = ?");
				st.setInt(1, id);

				ResultSet set = st.executeQuery();

				while (set.next()) {
					Element bl = xmlDoc.createElement("blackListed");
					bl.setAttribute("rev", set.getString("rev"));

					infos.appendChild(bl);
				}
			}
		} catch(SQLException e) {
			Logger.error(this, "Unable to get comment black list  because: "+e.toString());
		}

		return infos;
	}




	/*********** INDEX LOADING **************/


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


	public class IndexHandler extends DefaultHandler {
		private Locator locator = null;

		public IndexHandler() {

		}

		/**
		 * @see org.xml.sax.ContentHandler#setDocumentLocator(org.xml.sax.Locator)
		 */
		public void setDocumentLocator(Locator value) {
			locator =  value;
		}

		/**
		 * Called when parsing is started
		 * @see org.xml.sax.ContentHandler#startDocument()
		 */
		public void startDocument() throws SAXException {
			try {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("DELETE FROM files WHERE indexParent = ? AND dontDelete = FALSE");
				st.setInt(1, id);
				st.execute();

				st = db.getConnection().prepareStatement("DELETE FROM links WHERE indexParent = ? AND dontDelete = FALSE");
				st.setInt(1, id);
				st.execute();

				st = db.getConnection().prepareStatement("DELETE FROM indexCommentBlackList WHERE indexId = ?");
				st.setInt(1, id);
				st.execute();

			} catch(SQLException e) {
				Logger.error(this, "Hm, failure while starting to parse the index: "+e.toString());
				throw new SAXException("SQLException ; have a nice day.");
			}
		}

		/**
		 * Called when starting to parse in a specific name space
		 * @param prefix name space prefix
		 * @param URI name space URI
		 * @see org.xml.sax.ContentHandler#startPrefixMapping(java.lang.String, java.lang.String)
		 */
		public void startPrefixMapping(String prefix, String URI) throws SAXException {
			/* \_o< */
		}

		/**
		 * @param prefix name space prefix
		 * @see org.xml.sax.ContentHandler#endPrefixMapping(java.lang.String)
		 */
		public void endPrefixMapping(String prefix) throws SAXException {
			/* \_o< */
		}



		private boolean ownerTag = false;
		private boolean privateKeyTag = false;
		private boolean commentsTag = false;

		private boolean hasCommentTag = false;

		private PreparedStatement insertFileSt = null;
		private PreparedStatement insertLinkSt = null;

		/**
		 * Called when the parsed find an opening tag
		 * @param localName local tag name
		 * @param rawName rawName (the one used here)
		 * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
		 */
		public void startElement(String nameSpaceURI, String localName,
					 String rawName, Attributes attrs) throws SAXException {
			if (rawName == null) {
				rawName = localName;
			}

			if (rawName == null)
				return;

			ownerTag = false;
			privateKeyTag = false;

			/* TODO : <title></title> */

			if ("owner".equals(rawName)) {
				ownerTag = true;
				return;
			}

			if ("privateKey".equals(rawName)) {
				privateKeyTag = true;
				return;
			}

			if ("index".equals(rawName)) { /* links */

				int nextId;

				try {
					String key = attrs.getValue("key");

					if (key == null) /* it was the beginning of the index */
						return;

					key = key.trim();


					boolean blackListed = (BlackList.isBlackListed(db, key) >= 0);


					if (insertLinkSt == null)
						insertLinkSt = db.getConnection().prepareStatement("INSERT INTO links "
												   + "(publicKey, mark, comment, indexParent, indexTarget, blackListed) "
												   + "VALUES (?, 0, ?, ?, NULL, ?)");

					insertLinkSt.setString(1, key);
					insertLinkSt.setString(2, "No comment"); /* comment not used at the moment */
					insertLinkSt.setInt(3, id);
					insertLinkSt.setBoolean(4, blackListed);

					insertLinkSt.execute();
				} catch(SQLException e) {
					Logger.error(this, "Error while adding link : "+e.toString());
				}

				return;
			}

			if ("file".equals(rawName)) {
				int nextId;

				try {
					if (insertFileSt == null)
						insertFileSt =
							db.getConnection().prepareStatement("INSERT INTO files "
											    + "(filename, publicKey, localPath, mime, size, category, indexParent) "
											    + "VALUES (?, ?, NULL, ?, ?, NULL, ?)");

					String key = attrs.getValue("key");
					String filename = FreenetURIHelper.getFilenameFromKey(key);
					String mime = attrs.getValue("mime");
					long size = Long.parseLong(attrs.getValue("size"));

					insertFileSt.setString(1, filename);
					insertFileSt.setString(2, key);
					insertFileSt.setString(3, mime);
					insertFileSt.setLong(4, size);
					insertFileSt.setInt(5, id);

					synchronized(db.dbLock) {
						insertFileSt.execute();
					}
				} catch(SQLException e) {
					Logger.error(this, "Error while adding file: "+e.toString());
				}

				return;

			}


			if ("comments".equals(rawName)) {
				String pub = attrs.getValue("publicKey");
				String priv = attrs.getValue("privateKey");

				if (pub != null && priv != null) {
					hasCommentTag = true;
					commentsTag = true;
					Logger.debug(this, "Comment allowed in this index");
					setCommentKeys(pub, priv);
				}
			}


			if ("blackListed".equals(rawName)) {
				int blRev;

				try {
					blRev = Integer.parseInt(attrs.getValue("rev"));
				} catch(Exception e) {
					/* quick and dirty */
					return;
				}

				Logger.notice(this, "BlackListing rev '"+Integer.toString(rev)+"'");

				try {
					synchronized(db.dbLock) {
						PreparedStatement st;

						st = db.getConnection().prepareStatement("INSERT into indexCommentBlackList (rev, indexId) VALUES (?, ?)");
						st.setInt(1, blRev);
						st.setInt(2, id);

						st.execute();

					}
				} catch(SQLException e) {
					Logger.error(this, "Error while adding element to the blackList: "+e.toString());
				}
			}



			/* ignore unknown tags */

			/* et paf ! Ca fait des Chocapics(tm)(r)(c)(m)(dtc) ! */
		}

		/**
		 * Called when a closing tag is met
		 * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
		 */
		public void endElement(String nameSpaceURI, String localName,
				       String rawName) throws SAXException {
			if (rawName == null) {
				rawName = localName;
			}

			if (rawName == null)
				return;

			if ("owner".equals(rawName)) {
				ownerTag = false;
				return;
			}

			if ("privateKey".equals(rawName)) {
				privateKeyTag = false;
				return;
			}

			if ("comments".equals(rawName)) {
				commentsTag = false;
				return;
			}
		}


		/**
		 * Called when a text between two tag is met
		 * @param ch text
		 * @param start position
		 * @param end position
		 * @see org.xml.sax.ContentHandler#characters(char[], int, int)
		 */
		public void characters(char[] ch, int start, int end) throws SAXException {
			String txt = new String(ch, start, end);

			if (ownerTag) {
				/* \_o< ==> TODO */

				return;
			}

			if (privateKeyTag) {
				if (privateKey == null || privateKey.trim().equals(txt.trim())) {
					/**
					 * the private key was published, we will have to do the same later
					 */
					setPublishPrivateKey(true);
				} else {
					/**
					 * the provided key doesn't match with the one we have,
					 * we won't publish it anymore
					 */
					Logger.notice(this, "A private key was provided, but didn't match with the one we have ; ignored.");

				}

				if (privateKey == null)
					setPrivateKey(txt.trim());
				return;
			}




			/* ignore unkwown stuffs */

		}

		public void ignorableWhitespace(char[] ch, int start, int end) throws SAXException {

		}

		public void processingInstruction(String target, String data) throws SAXException {

		}

		/**
		 * @see org.xml.sax.ContentHandler#skippedEntity(java.lang.String)
		 */
		public void skippedEntity(String arg0) throws SAXException {

		}


		/**
		 * Called when parsing is finished
		 * @see org.xml.sax.ContentHandler#endDocument()
		 */
		public void endDocument() throws SAXException {
			if (!hasCommentTag) {
				Logger.debug(this, "No comment allowed in this index");
				purgeCommentKeys();
			}
		}
	}


	/**
	 * see import functionnality
	 */
	public IndexHandler getIndexHandler() {
		return new IndexHandler();
	}


	public synchronized void loadXML(final java.io.InputStream input, boolean clean) {
		IndexHandler handler = new IndexHandler();

		synchronized(db.dbLock) {
			try {
				// Use the default (non-validating) parser
				SAXParserFactory factory = SAXParserFactory.newInstance();

				// Parse the input
				SAXParser saxParser = factory.newSAXParser();
				saxParser.parse(input, handler );
			} catch(javax.xml.parsers.ParserConfigurationException e) {
				Logger.error(this, "Error (1) while parsing index: "+e.toString());
			} catch(org.xml.sax.SAXException e) {
				Logger.error(this, "Error (2) while parsing index: "+e.toString());
			} catch(java.io.IOException e) {
				Logger.error(this, "Error (3) while parsing index: "+e.toString());
			}
		}
	}



	public static String getNameFromKey(final String key) {
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


	public static int isAlreadyKnown(Hsqldb db, String key) {
		return isAlreadyKnown(db, key, false);
	}

	/**
	 * @return the index id if found ; -1 else
	 */
	public static int isAlreadyKnown(Hsqldb db, String key, boolean strict) {
		if (key.length() < 40) {
			Logger.error(new Index(), "isAlreadyKnown(): Invalid key: "+key);
			return -1;
		}

		key = key.replaceAll(".xml", ".frdx");

		synchronized(db.dbLock) {
			try {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("SELECT id, publicKey from indexes WHERE LOWER(publicKey) LIKE ?"
									 + (strict ? "" : " LIMIT 1"));

				st.setString(1, FreenetURIHelper.getComparablePart(key) +"%");

				ResultSet res = st.executeQuery();

				if (strict) {
					while(res.next()) {
						String pubKey = res.getString("publicKey").replaceAll(".xml", ".frdx");

						if (FreenetURIHelper.compareKeys(pubKey, key)) {
							return res.getInt("id");
						}
					}

					return -1;
				} else {
					if (!res.next())
						return -1;

					return res.getInt("id");
				}

			} catch(final SQLException e) {
				Logger.error(new Index(), "isAlreadyKnown: Unable to check if link '"+key+"' point to a know index because: "+e.toString());
			}
		}

		return -1;
	}


	public void forceHasChangedReload() {
		Logger.debug(this, "forceHasChangedReload() => loadData()");
		loadData();
	}


	public boolean hasChanged() {
		if (publicKey == null) {
			Logger.debug(this, "hasChanged() => loadData()");
			loadData();
		}

		return hasChanged;
	}

	public boolean hasNewComment() {
		if (publicKey == null) {
			Logger.debug(this, "hasNewComment() => loadData()");
			loadData();
		}

		return newComment;
	}


	public boolean setHasChangedFlagInMem(boolean flag) {
		hasChanged = flag;
		return true;
	}

	public boolean setNewCommentFlagInMem(boolean flag) {
		newComment = flag;
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


	/**
	 * @return true if a change was done
	 */
	public boolean setNewCommentFlag(boolean flag) {
		setNewCommentFlagInMem(flag);

		synchronized(db.dbLock) {
			try {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("UPDATE indexes SET newComment = ? "+
									 "WHERE id = ?");

				st.setBoolean(1, flag);
				st.setInt(2, id);
				if (st.executeUpdate() > 0)
					return true;
				return false;
			} catch(SQLException e) {
				Logger.error(this, "Unable to change 'newComment' flag because: "+e.toString());
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


	public boolean equals(Object o) {
		if (o == null || !(o instanceof Index))
			return false;

		if (((Index)o).getId() == getId())
			return true;
		return false;
	}



	/**
	 * @return an SSK@
	 */
	public String getCommentPublicKey() {
		try {
			synchronized(db.dbLock) {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("SELECT publicKey FROM indexCommentKeys WHERE indexId = ? LIMIT 1");
				st.setInt(1, id);

				ResultSet set = st.executeQuery();

				if (set != null && set.next())
					return set.getString("publicKey");

			}
		} catch(SQLException e) {
			Logger.error(this, "Unable to get comment public key because : "+e.toString());
		}

		return null;
	}


	/**
	 * @return an SSK@
	 */
	public String getCommentPrivateKey() {
		try {
			synchronized(db.dbLock) {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("SELECT privateKey FROM indexCommentKeys WHERE indexId = ? LIMIT 1");
				st.setInt(1, id);

				ResultSet set = st.executeQuery();

				if (set != null && set.next())
					return set.getString("privateKey");

			}
		} catch(SQLException e) {
			Logger.error(this, "Unable to get comment public key because : "+e.toString());
		}


		return null;
	}

	/**
	 * Will also purge comments !
	 */
	public void purgeCommentKeys() {
		try {
			synchronized(db.dbLock) {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("DELETE FROM indexCommentBlackList WHERE indexId = ?");
				st.setInt(1, id);
				st.execute();

				st = db.getConnection().prepareStatement("DELETE FROM indexComments WHERE indexId = ?");
				st.setInt(1, id);
				st.execute();

				st = db.getConnection().prepareStatement("DELETE FROM indexCommentKeys WHERE indexId = ?");
				st.setInt(1, id);
				st.execute();

			}
		} catch(SQLException e) {
			Logger.error(this, "Unable to purge comment keys, because : "+e.toString());
		}
	}


	/**
	 * will reset the comments !
	 */
	public void setCommentKeys(String publicKey, String privateKey) {
		String oldPubKey = getCommentPublicKey();
		String oldPrivKey = getCommentPrivateKey();

		if ( ((publicKey == null && oldPubKey == null)
		      || (publicKey != null && publicKey.equals(oldPubKey)))
		     &&
		     ((privateKey == null && oldPrivKey == null)
		      || (privateKey != null && privateKey.equals(oldPrivKey))) )
			return; /* same keys => no change */


		purgeCommentKeys();

		try {
			synchronized(db.dbLock) {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("INSERT INTO indexCommentKeys (publicKey, privateKey, indexId) VALUES (?, ?, ?)");
				st.setString(1, publicKey);
				st.setString(2, privateKey);
				st.setInt(3, id);

				st.execute();
			}
		} catch(SQLException e) {
			Logger.error(this, "Unable to set comment keys, because : "+e.toString());
		}
	}


	protected class CommentKeyRegenerator implements Observer {
		private FCPGenerateSSK sskGenerator;

		public CommentKeyRegenerator(FCPQueueManager queueManager) {
			sskGenerator = new FCPGenerateSSK();
			sskGenerator.addObserver(this);

			sskGenerator.start(queueManager);
		}


		public void update(Observable o, Object param) {
			if (o instanceof FCPGenerateSSK) {
				setCommentKeys(((FCPGenerateSSK)o).getPublicKey(),
					       ((FCPGenerateSSK)o).getPrivateKey());
			}
		}

	}

	public void regenerateCommentKeys(FCPQueueManager queueManager) {
		new CommentKeyRegenerator(queueManager);
	}


	public boolean canHaveComments() {
		return (getCommentPublicKey() != null);
	}


	public boolean postComment(FCPQueueManager queueManager, Identity author, String msg) {
		String privKey;

		if ((privKey = getCommentPrivateKey()) == null) {
			return false;
		}

		Comment comment = new Comment(db, this, -1, author, msg);

		return comment.insertComment(queueManager);
	}


	public void loadComments(FCPQueueManager queueManager) {
		String pubKey;

		if ((pubKey = getCommentPublicKey()) == null)
			return;

		if (queueManager == null) {
			Logger.warning(this, "Can't load comments ! QueueManager is not set for this index !");
			return;
		}

		for (lastCommentRev = 0 ;
		     lastCommentRev < COMMENT_FETCHING_RUNNING_AT_THE_SAME_TIME;
		     lastCommentRev++) {
			Comment comment = new Comment(db, this, lastCommentRev, null, null);
			comment.addObserver(this);
			comment.fetchComment(queueManager);
		}
	}


	public Vector getComments() {
		return getComments(true);
	}

	public Vector getComments(boolean asc) {

		try {
			synchronized(db.dbLock) {
				Vector comments = new Vector();

				PreparedStatement st;

				st = db.getConnection().prepareStatement("SELECT authorId, text, rev "+
									 "FROM indexComments WHERE indexId = ? ORDER BY rev" +
									 (asc ? "" : " DESC"));

				st.setInt(1, id);

				ResultSet set = st.executeQuery();

				while(set.next())
					comments.add(new Comment(db, this,
								 set.getInt("rev"),
								 Identity.getIdentity(db, set.getInt("authorId")),
								 set.getString("text")));

				if (comments.size() == 0)
					Logger.notice(this, "No comment for this index");
				else
					Logger.info(this, Integer.toString(comments.size())+ " comments for this index");

				return comments;
			}
		} catch(SQLException e) {
			Logger.error(this, "Error while fetching comment list : "+e.toString());
		}

		return null;
	}


	public int getNmbComments() {

		try {
			synchronized(db.dbLock) {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("SELECT count(id) FROM indexComments WHERE indexId = ?");

				st.setInt(1, id);

				ResultSet set = st.executeQuery();

				if (set.next())
					return set.getInt(1);

				return 0;
			}
		} catch(SQLException e) {
			Logger.error(this, "Error while fetching comment list : "+e.toString());
		}

		return 0;
	}
}
