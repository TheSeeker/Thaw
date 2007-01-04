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

import thaw.core.FreenetURIHelper;
import thaw.core.Logger;
import thaw.fcp.FCPClientGet;
import thaw.fcp.FCPClientPut;
import thaw.fcp.FCPGenerateSSK;
import thaw.fcp.FCPQueueManager;
import thaw.fcp.FCPTransferQuery;
import thaw.plugins.Hsqldb;

public class Index extends java.util.Observable implements FileAndLinkList, IndexTreeNode, java.util.Observer {

	private final Hsqldb db;

	private int id;
	private IndexCategory parent;
	private String realName;
	private String displayName;

	private String publicKey; /* without the filename ! */
	private String privateKey;

	private int revision = 0;

	private Vector fileList;
	private Vector linkList;

	private DefaultMutableTreeNode treeNode;

	private FCPGenerateSSK sskGenerator;

	private final FCPQueueManager queueManager;
	private final IndexBrowserPanel indexBrowser;

	private FCPTransferQuery transfer = null;
	private File targetFile = null;

	private String author;

	private boolean rewriteKey = true;
	private boolean changed = false;

	private boolean updateWhenKeyAreAvailable = false;

	/**
	 * @deprecated Just don't use it !
	 */
	public Index() {
		db = null;
		queueManager = null;
		indexBrowser = null;
	}


	/**
	 * The bigest constructor of the world ...
	 * @param revision Ignored if the index is not modifiable (=> deduced from the publicKey)
	 */
	public Index(final FCPQueueManager queueManager,
		     final IndexBrowserPanel indexBrowser,
		     final int id, final IndexCategory parent,
		     String realName, String displayName,
		     final String publicKey, final String privateKey,
		     final int revision,
		     final String author) {
		this.indexBrowser = indexBrowser;
		this.queueManager = queueManager;
		treeNode = new DefaultMutableTreeNode(displayName, false);
		this.db = indexBrowser.getDb();

		this.id = id;
		this.parent = parent;
		this.realName = realName.trim();
		this.displayName = displayName.trim();

		if (realName == null)
			realName = displayName;

		if (displayName == null)
			displayName = realName;

		this.revision = revision;

		this.author = author;

		setPrivateKey(privateKey);
		setPublicKey(publicKey);

		treeNode.setUserObject(this);

		this.setTransfer();
	}


	/**
	 * Index registration allows to have a flat view of the loaded / displayed indexes
	 */
	public void register() {
		indexBrowser.getIndexTree().registerIndex(this);
	}

	public void unregister() {
		indexBrowser.getIndexTree().unregisterIndex(this);
	}


	public static boolean isDumbKey(final String key) {
		return ((key == null) || key.equals("") || (key.length() < 20));
	}

	public void setParent(final IndexCategory parent) {
		this.parent = parent;
	}

	public IndexCategory getParent() {
		return parent;
	}

	public DefaultMutableTreeNode getTreeNode() {
		return treeNode;
	}

	public Index getIndex(final int id) {
		return (id == getId()) ? this : null;
	}

	public void generateKeys() {
		publicKey = null;
		privateKey = null;

		sskGenerator = new FCPGenerateSSK();
		sskGenerator.addObserver(this);
		sskGenerator.start(queueManager);
	}

	public boolean create() {
		try {
			/* Rahh ! Hsqldb doesn't support getGeneratedKeys() ! 8/ */
			synchronized (db.dbLock) {
				final Connection c = db.getConnection();
				PreparedStatement st;

				st = c.prepareStatement("SELECT id FROM indexes ORDER BY id DESC LIMIT 1");

				st.execute();

				try {
					final ResultSet key = st.getResultSet();
					key.next();
					id = key.getInt(1) + 1;
				} catch(final SQLException e) {
					id = 1;
				}

				st = c.prepareStatement("INSERT INTO indexes (id, originalName, displayName, publicKey, privateKey, author, positionInTree, revision, parent) VALUES (?, ?,?,?,?,?, ?,?, ?)");
				st.setInt(1, id);
				st.setString(2, realName);
				st.setString(3, displayName);
				st.setString(4, publicKey != null ? publicKey : "");
				st.setString(5, privateKey);
				st.setString(6, author);
				st.setInt(7, 0);

				st.setInt(8, revision);

				if((parent != null) && (parent.getId() >= 0))
					st.setInt(9, parent.getId());
				else
					st.setNull(9, Types.INTEGER);

				st.execute();
			}

			return true;
		} catch(final SQLException e) {
			Logger.error(this, "Unable to insert the new index in the db, because: "+e.toString());
			return false;
		}

	}

	public void rename(final String name) {
		try {
			final Connection c = db.getConnection();
			PreparedStatement st;

			st = c.prepareStatement("UPDATE indexes SET displayName = ? WHERE id = ?");
			st.setString(1, name);
			st.setInt(2, id);
			st.execute();

			displayName = name;

		} catch(final SQLException e) {
			Logger.error(this, "Unable to rename the index '"+displayName+"' in '"+name+"', because: "+e.toString());
		}

	}

	public void delete() {
		try {
			loadFiles(null, true);
			this.loadLinks(null, true);

			for(final Iterator fileIt = fileList.iterator(); fileIt.hasNext(); ) {
				final thaw.plugins.index.File file = (thaw.plugins.index.File)fileIt.next();
				file.delete();
			}

			for (final Iterator linkIt = linkList.iterator(); linkIt.hasNext() ;) {
				final Link link = (Link)linkIt.next();
				link.delete();
			}

			final Connection c = db.getConnection();
			final PreparedStatement st = c.prepareStatement("DELETE FROM indexes WHERE id = ?");
			st.setInt(1, id);
			st.execute();
		} catch(final SQLException e) {
			Logger.error(this, "Unable to delete the index '"+displayName+"', because: "+e.toString());
		}

		if (transfer != null)
			transfer.stop(queueManager);
	}


	public void update() {
		if (!Index.isDumbKey(publicKey) && Index.isDumbKey(privateKey)) /* non modifiable */
			return;

		if (Index.isDumbKey(publicKey) && Index.isDumbKey(privateKey)) {
			generateKeys();
			updateWhenKeyAreAvailable = true;
			return;
		}

		String tmpdir = System.getProperty("java.io.tmpdir");

		if (tmpdir == null)
			tmpdir = "";
		else
			tmpdir = tmpdir + java.io.File.separator;

		targetFile = new java.io.File(tmpdir + realName +".frdx");

		if (transfer != null) {
			Logger.notice(this, "A transfer is already running");
			return;
		}

		if(privateKey != null) {
			FCPClientPut clientPut;

			Logger.info(this, "Generating index ...");

			FileOutputStream outputStream;

			try {
				outputStream = new FileOutputStream(targetFile);
			} catch(final java.io.FileNotFoundException e) {
				Logger.warning(this, "Unable to create file '"+targetFile.toString()+"' ! not generated !");
				return;
			}

			generateXML(outputStream);

			if(targetFile.exists()) {
				Logger.info(this, "Inserting new version");

				revision++;

				clientPut = new FCPClientPut(targetFile, 2, revision, realName, privateKey, 2, true, 0);
				clientPut.setMetadata("ContentType", "application/x-freenet-index");
				setTransfer(clientPut);

				queueManager.addQueryToThePendingQueue(clientPut);

				save();

			} else {
				Logger.warning(this, "Index not generated !");
			}

			setChanged();
			notifyObservers();
		} else {
			updateFromFreenet(-1);
		}

	}

	public void updateFromFreenet(final int rev) {
		FCPClientGet clientGet;

		if (transfer != null) {
			Logger.notice(this, "A transfer is already running !");
			return;
		}

		if (publicKey == null) {
			Logger.error(this, "No public key !! Can't get the index !");
			return;
		}

		Logger.info(this, "Getting lastest version ...");

		String key;

		/* We will trust the node for the incrementation
		   execept if a rev is specified */


		if (rev >= 0) {
			key = FreenetURIHelper.convertUSKtoSSK(publicKey);
			key = FreenetURIHelper.changeSSKRevision(key, rev, 0);
			rewriteKey = false;
		} else {
			if (privateKey == null) {
				key = publicKey;
				rewriteKey = true;
			} else {
				key = getPublicKey();
				rewriteKey = false;
			}
		}

		Logger.info(this, "Key asked: "+key);


		clientGet = new FCPClientGet(key, 2, 2, false, -1, null);
		setTransfer(clientGet);

		/*
		 * These requests are usually quite fast, and don't consume a lot
		 * of bandwith / CPU. So we can skip the queue and start immediatly
		 * (and like this, they won't appear in the queue)
		 */
		//this.queueManager.addQueryToThePendingQueue(clientGet);
		clientGet.start(queueManager);

		this.setChanged();
		this.notifyObservers();
	}

	public boolean isUpdating() {
		return ((sskGenerator != null) || ((transfer != null) && (!transfer.isFinished())));
	}


	protected void setTransfer(final FCPTransferQuery query) {
		if (transfer != null && indexBrowser != null) {
			indexBrowser.getIndexProgressBar().removeTransfer(query);
		}

		transfer = query;

		if (transfer != null) {
			if (transfer instanceof FCPClientGet)
				((FCPClientGet)transfer).addObserver(this);
			if (transfer instanceof FCPClientPut)
				((FCPClientPut)transfer).addObserver(this);
			this.update(((java.util.Observable)transfer), null);
			if (indexBrowser != null)
				indexBrowser.getIndexProgressBar().addTransfer(query);
		}
	}

	protected void setTransfer() {
		if (queueManager == null || transfer != null)
			return;

		if (getPublicKey() != null) {
			if(!queueManager.isQueueCompletlyLoaded()) {
				final Thread th = new Thread(new WaitCompleteQueue());
				th.start();
				return;
			}

			String key;

			if (privateKey != null)
				key = FreenetURIHelper.getPublicInsertionSSK(getPublicKey());
			else
				key = getPublicKey();

			this.setTransfer(queueManager.getTransfer(key));
		}

		this.setChanged();
		this.notifyObservers();
	}

	private class WaitCompleteQueue implements Runnable {
		public WaitCompleteQueue() { }

		public void run() {
			int tryNumber = 0;

			while(!queueManager.isQueueCompletlyLoaded() && (tryNumber < 120 /* 1min */)) {
				try {
					Thread.sleep(500);
				} catch(final java.lang.InterruptedException e) {
					/* \_o< */
				}

				tryNumber++;
			}

			if (tryNumber == 120)
				return;

			Index.this.setTransfer();
		}
	}


	public void purgeLinkList() {
		unloadLinks();
		try {
			final Connection c = db.getConnection();
			final PreparedStatement st = c.prepareStatement("DELETE FROM links WHERE indexParent = ?");
			st.setInt(1, getId());
			st.execute();
			linkList = new Vector();
		} catch(final SQLException e) {
			Logger.error(this, "Unable to purge da list ! Exception: "+e.toString());
		}
	}

	public void purgeFileList() {
		unloadFiles();
		try {
			final Connection c = db.getConnection();
			final PreparedStatement st = c.prepareStatement("DELETE FROM files WHERE indexParent = ?");
			st.setInt(1, getId());
			st.execute();
			fileList = new Vector();
		} catch(final SQLException e) {
			Logger.error(this, "Unable to purge da list ! Exception: "+e.toString());
		}
	}

	public synchronized void save() {
		try {
			final Connection c = db.getConnection();
			final PreparedStatement st = c.prepareStatement("UPDATE indexes SET originalName = ?, displayName = ?, publicKey = ?, privateKey = ?, positionInTree = ?, revision = ?, parent = ? , author = ? WHERE id = ?");

			st.setString(1, realName);
			st.setString(2, displayName);
			st.setString(3, publicKey != null ? publicKey : "");
			if(privateKey != null)
				st.setString(4, privateKey);
			else
				st.setNull(4, Types.VARCHAR);

			if ((treeNode != null) && (treeNode.getParent() != null))
				st.setInt(5, treeNode.getParent().getIndex(treeNode));
			else
				st.setInt(5, 0);

			st.setInt(6, revision);

			if( treeNode.getParent() == null || ((IndexTreeNode)treeNode.getParent()).getId() < 0)
				st.setNull(7, Types.INTEGER);
			else
				st.setInt(7, ((IndexTreeNode)treeNode.getParent()).getId());

			st.setString(8, author);

			st.setInt(9, getId());

			st.execute();
		} catch(final SQLException e) {
			Logger.error(this, "Unable to save index state '"+toString()+"' because : "+e.toString());
		}
	}

	public int getId() {
		return id;
	}

	public String getPublicKey() {
		if ((publicKey == null) || Index.isDumbKey(publicKey))
			return null;

		if (publicKey.startsWith("SSK@")) { /* as it should when privateKey is known */
			if (publicKey.endsWith("/"))
				return publicKey.replaceFirst("SSK@", "USK@")+realName+"/"+revision+"/"+realName+".frdx";
			else
				return publicKey.replaceFirst("SSK@", "USK@")+"/"+realName+"/"+revision+"/"+realName+".frdx";
		} else
			return publicKey;
	}

	/**
	 * Always set the privateKey first
	 */
	public void setPublicKey(final String key) {
		if ((key != null) && !Index.isDumbKey(key))
			publicKey = key.trim();
		else
			publicKey = null;


		if ((privateKey != null) && (publicKey != null) && publicKey.startsWith("USK@")) {
			final String[] split = FreenetURIHelper.convertUSKtoSSK(publicKey).split("/");
			publicKey = split[0];
		}

		if ((publicKey != null) && publicKey.startsWith("USK@"))
			revision = FreenetURIHelper.getUSKRevision(publicKey);
	}

	public String getPrivateKey() {
		if ((privateKey == null) || Index.isDumbKey(privateKey))
			return null;
		return privateKey;
	}

	public void setPrivateKey(final String key) {
		if ((key != null) && !Index.isDumbKey(key))
			privateKey = key.trim();
		else
			privateKey = null;
	}

	public String toString() {
		return toString(true);
	}

	public String toString(boolean withRev) {
		String toDisp;

		if(displayName != null)
			toDisp = displayName;
		else
			toDisp = realName;

		if (withRev && revision > 0)
			toDisp += " (r"+Integer.toString(revision)+")";

		return toDisp;
	}

	public boolean isLeaf() {
		return true;
	}


	public void update(final java.util.Observable o, final Object p) {

		if(o == sskGenerator) {
			sskGenerator.deleteObserver(this);
			publicKey = sskGenerator.getPublicKey();
			privateKey = sskGenerator.getPrivateKey();
			sskGenerator = null;

			Logger.debug(this, "Index public key: " +publicKey);
			Logger.debug(this, "Index private key: "+privateKey);

			revision = 0;

			if (updateWhenKeyAreAvailable) {
				updateWhenKeyAreAvailable = false;
				update();
			}

			setChanged();
			notifyObservers();
			return;
		}

		if(o == transfer) {
			if(transfer.isFinished() && transfer.isSuccessful()) {
				if(transfer instanceof FCPClientPut) {

					((FCPClientPut)transfer).deleteObserver(this);

					if (transfer.stop(queueManager))
						queueManager.remove(transfer);

					this.setChanged();
					this.notifyObservers();

					return;
				}

				if (transfer instanceof FCPClientGet) {
					((FCPClientGet)transfer).deleteObserver(this);

					final int oldRevision = revision;

					if (rewriteKey)
						publicKey = transfer.getFileKey();

					revision = FreenetURIHelper.getUSKRevision(transfer.getFileKey());

					Logger.info(this, "Most up-to-date key found: " + getPublicKey());

					if (oldRevision < revision)
						changed = true;

					/* Reminder: These requests are non-peristent */
					//if (this.transfer.stop(this.queueManager))
					//	this.queueManager.remove(this.transfer);

					queueManager.remove(transfer);

					if (transfer.getPath() == null) {
						Logger.error(this, "No path ?!");
						return;
					}

					if (changed)
						loadXML(transfer.getPath());

					save();

					indexBrowser.getUnknownIndexList().addLinks(this);

					setChanged();
					notifyObservers();
				}

			}

			if ((transfer != null) && transfer.isFinished() && !transfer.isSuccessful()) {
				Logger.info(this, "Unable to get new version of the index");

				this.setChanged();
				this.notifyObservers();
			}

			if (transfer != null && transfer.isFinished()) {
				Logger.info(this, "Removing file '"+transfer.getPath()+"'");
				final String path = transfer.getPath();
				if (path != null) {
					final java.io.File fl = new java.io.File(path);
					fl.delete();
				}
				else
					Logger.error(this, "Unable to remove file !");
				setTransfer(null);
			}

		}

		if ((o instanceof thaw.plugins.index.File)
		    || (o instanceof Link)) {
			this.setChanged();
			this.notifyObservers(o);
		}
	}


	public boolean hasChanged() {
		return changed;
	}


	public void setChanged(final boolean val) {
		changed = val;
	}


	public boolean isEmpty() {
		if (fileList == null)
			loadFiles(null, true);
		if (linkList == null)
			this.loadLinks(null, true);

		if ((fileList.size() == 0) && (linkList.size() == 0))
			return true;

		return false;
	}


	////// FILE LIST ////////

	public void loadFiles(final String columnToSort, boolean asc) {

		fileList = new Vector();

		try {
			String query = "SELECT id, filename, publicKey, mime, size, localPath, category, indexParent FROM files WHERE indexParent = ?";

			if(columnToSort != null) {
				query = query + "ORDER BY " + columnToSort;

				if(!asc)
					query = query + " DESC";
			}

			final PreparedStatement st = db.getConnection().prepareStatement(query);

			st.setInt(1, getId());

			if(st.execute()) {
				final ResultSet results = st.getResultSet();

				while(results.next()) {
					final thaw.plugins.index.File file = new thaw.plugins.index.File(db, results, this);
					file.setTransfer(queueManager);
					addFileToList(file);
				}
			}


		} catch(final java.sql.SQLException e) {
			Logger.error(this, "Unable to get the file list for index: '"+toString()+"' because: "+e.toString());
		}

		setChanged(false); /* java.util.Index */
		setChanged(); /* java.util.Observer */
		notifyObservers();
	}

	/**
	 * Returns a *copy* of da vector.
	 */
	public Vector getFileList() {
		final Vector newList = new Vector();

		if (fileList == null)
			return newList;

		for(final Iterator it = fileList.iterator();
		    it.hasNext();) {
			newList.add(it.next());
		}

		return newList;
	}

	public thaw.plugins.index.File getFile(final int index) {
		return (thaw.plugins.index.File)fileList.get(index);
	}

	public int getFilePosition(final thaw.plugins.index.File fileA) {
		int pos = fileList.indexOf(fileA);

		if (fileA.getPublicKey() == null)
			return -1;

		if (pos < 0) {
			/* Manual research */
			pos = 0;
			for(final Iterator it = fileList.iterator();
			    it.hasNext();) {
				final thaw.plugins.index.File fileB = (thaw.plugins.index.File)it.next();

				if ((fileB.getPublicKey() != null)
				    && fileB.getPublicKey().equals(fileA.getPublicKey()))
					return pos;
				pos++;

			}
		}

		return -1;
	}


	public void unloadFiles() {
		if (fileList != null) {
			for (final Iterator it  = fileList.iterator();
			     it.hasNext();)
				{
					final thaw.plugins.index.File file = (thaw.plugins.index.File)it.next();
					file.deleteObserver(this);
				}
		}

		fileList = null;
	}


	/**
	 * Note for myself: For external use only ! (file will be inserted in the database etc)
	 */
	public void addFile(final thaw.plugins.index.File file) {
		file.setParent(this);

		if (!file.isInTheDatabase()) {
			file.insert();

			addFileToList(file);

			this.setChanged();
			this.notifyObservers(file);
		}
		else
			Logger.notice(this, "File already in the database for this index");
	}


	/**
	 * Won't notify
	 */
	protected void addFileToList(final thaw.plugins.index.File file) {
		if (fileList == null)
			loadFiles(null, true);
		file.addObserver(this);
		fileList.add(file);
	}


	public void removeFile(final thaw.plugins.index.File file) {
		file.delete();

		if(fileList != null) {
			fileList.remove(file);

			this.setChanged();
			this.notifyObservers(file);
		}
	}

	/**
	 * Do the update all the files in the database.
	 */
	public void updateFileList() {
		for(final Iterator it = fileList.iterator(); it.hasNext();) {
			final thaw.plugins.index.File file = (thaw.plugins.index.File)it.next();
			file.update();
		}
	}


	//// LINKS ////

	public void addLink(final Link link) {
		link.setParent(this);

		if (!link.isInTheDatabase()) {
			link.insert();

			addLinkToList(link);

			setChanged();
			notifyObservers(link);
		}
		else
			Logger.notice(this, "Link already in the database for this index");

	}

	protected void addLinkToList(final Link link) {
		if (linkList == null)
			this.loadLinks(null, true);

		linkList.add(link);
	}

	public void removeLink(final Link link) {
		link.delete();

		if (linkList != null) {
			linkList.remove(link);
			this.setChanged();
			this.notifyObservers(link);
		}

	}

	/**
	 * Update all the links in the database.
	 */
	public void updateLinkList() {
		for(final Iterator it = linkList.iterator(); it.hasNext();) {
			final Link link = (Link)it.next();
			link.update();
		}
	}

	public void loadLinks(final String columnToSort, boolean asc)
	{
		linkList = new Vector();

		try {
			String query = "SELECT id, publicKey, mark, comment, indexTarget FROM links WHERE indexParent = ?";

			if(columnToSort != null) {
				query = query + "ORDER BY " + columnToSort;

				if(!asc)
					query = query + " DESC";
			}

			final PreparedStatement st = db.getConnection().prepareStatement(query);

			st.setInt(1, getId());

			if(st.execute()) {
				final ResultSet results = st.getResultSet();

				while(results.next()) {
					try {
						final Link link = new Link(db, results, this);
						addLinkToList(link);
					} catch(final Exception e) {
						Logger.warning(this, "Unable to add index '"+publicKey+"' to the list because: "+e.toString());
					}
				}
			}


		} catch(final java.sql.SQLException e) {
			Logger.error(this, "Unable to get the link list for index: '"+toString()+"' because: "+e.toString());
		}

		setChanged(false); /* Index */
		setChanged();      /* java.util.Observable */
		notifyObservers();

	}

	/* Returns a copy ! */
	public Vector getLinkList()
	{
		final Vector newList = new Vector();

		if (linkList == null)
			return newList;

		for(final Iterator it = linkList.iterator();
		    it.hasNext();) {
			newList.add(it.next());
		}

		return newList;
	}

	public Link getLink(final int index)
	{
		return (Link)linkList.get(index);
	}

	public void unloadLinks()
	{
		linkList = null;
	}


	//// XML ////

	public void generateXML(final OutputStream out) {
		final StreamResult streamResult = new StreamResult(out);

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

		rootEl.appendChild(getXMLHeader(xmlDoc));
		rootEl.appendChild(getXMLLinks(xmlDoc));
		rootEl.appendChild(getXMLFileList(xmlDoc));

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

	public Element getXMLHeader(final Document xmlDoc) {
		final Element header = xmlDoc.createElement("header");

		final Element title = xmlDoc.createElement("title");
		final Text titleText = xmlDoc.createTextNode(displayName);
		title.appendChild(titleText);

		final Element owner = xmlDoc.createElement("owner");
		Text ownerText;

		if (author == null)
			ownerText = xmlDoc.createTextNode("Another anonymous");
		else
			ownerText = xmlDoc.createTextNode(author);

		owner.appendChild(ownerText);

		header.appendChild(title);
		header.appendChild(owner);

		return header;
	}

	public Element getXMLLinks(final Document xmlDoc) {
		final Element links = xmlDoc.createElement("indexes");

		if (linkList == null) {
			this.loadLinks(null, true);
		}

		for (final Iterator it = getLinkList().iterator();
		     it.hasNext();) {
			final Link link = (Link)it.next();

			final Element xmlLink = link.getXML(xmlDoc);

			if (xmlLink != null)
				links.appendChild(xmlLink);
			else
				Logger.warning(this, "Unable to get XML for the link '"+link.getIndexName()+"' => Gruick da link");
		}

		return links;
	}

	public Element getXMLFileList(final Document xmlDoc) {
		final Element files = xmlDoc.createElement("files");

		if(fileList == null) {
			loadFiles(null, true);
		}

		for(final Iterator it = getFileList().iterator();
		    it.hasNext();) {
			final thaw.plugins.index.File file = (thaw.plugins.index.File)it.next();

			final Element xmlFile = file.getXML(xmlDoc);

			if(xmlFile != null)
				files.appendChild(xmlFile);
			else
				Logger.warning(this, "Public key wasn't generated ! Not added to the index !");
		}

		return files;
	}

	public void loadXML(final String filePath) {
		try {
			loadXML(new FileInputStream(filePath));
		} catch(final java.io.FileNotFoundException e) {
			Logger.error(this, "Unable to load XML: FileNotFoundException ('"+filePath+"') ! : "+e.toString());
		}
	}

	public synchronized void loadXML(final java.io.InputStream input) {
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

		loadXML(rootEl);
	}

	public void loadXML(Element rootEl) {
		loadHeader(rootEl);
		loadLinks(rootEl);
		loadFileList(rootEl);
	}


	public void loadHeader(final Element rootEl) {
		final Element header = (Element)rootEl.getElementsByTagName("header").item(0);

		if (header == null)
			return;

		realName = getHeaderElement(header, "title");
		author = getHeaderElement(header, "owner");

		if (author == null)
			author = "Another anonymous";
	}

	public String getHeaderElement(final Element header, final String name) {
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

	public void loadLinks(final Element rootEl) {
		purgeLinkList();

		final Element links = (Element)rootEl.getElementsByTagName("indexes").item(0);

		if (links == null)
			return;

		final NodeList list = links.getChildNodes();

		for(int i = 0; i < list.getLength() ; i++) {
			if(list.item(i).getNodeType() == Node.ELEMENT_NODE) {
				final Element e = (Element)list.item(i);

				final Link link = new Link(db, e, this);
				addLink(link);
			}
		}

		setChanged();
		notifyObservers();
	}

	public void loadFileList(final Element rootEl) {
		purgeFileList();

		final Element filesEl = (Element)rootEl.getElementsByTagName("files").item(0);

		if (filesEl == null)
			return;

		final NodeList list = filesEl.getChildNodes();

		for(int i = 0; i < list.getLength() ; i++) {
			if(list.item(i).getNodeType() == Node.ELEMENT_NODE) {
				final Element e = (Element)list.item(i);

				final thaw.plugins.index.File file = new thaw.plugins.index.File(db, e, this);
				addFile(file);

			}
		}

		this.setChanged();
		this.notifyObservers();
	}


	static String getNameFromKey(final String key) {
		String name = null;

		name = FreenetURIHelper.getFilenameFromKey(key);

		if (name == null)
			return null;

		name = name.replaceAll(".xml", "");
		name = name.replaceAll(".frdx", "");

		return name;
	}


	public Vector getIndexIds() {
		final Vector ids = new Vector();
		ids.add(new Integer(getId()));
		return ids;
	}

	public Vector getIndexes() {
		final Vector idx = new Vector();
		idx.add(this);
		return idx;
	}

	public boolean isModifiable() {
		return (privateKey != null);
	}

	public boolean isInIndex(final thaw.plugins.index.File file) {
		if (fileList == null)
			loadFiles(null, true);
		return fileList.contains(file);
	}


	public static boolean isAlreadyKnown(Hsqldb db, String key) {
		if (key.length() < 40) {
			Logger.error(new Index(), "isAlreadyKnown: Invalid key: "+key);
			return false;
		}

		try {
			PreparedStatement st;

			st = db.getConnection().prepareStatement("SELECT publicKey from indexes WHERE publicKey LIKE ?");

			st.setString(1, "%"+key.substring(3, 40).replaceAll("%","\\%") +"%");

			if(st.execute()) {
				final ResultSet result = st.getResultSet();
				if ((result != null) && result.next())
					return true;
			}

		} catch(final SQLException e) {
			Logger.error(new Index(), "isAlreadyKnown: Unable to check if link '"+key+"' point to a know index because: "+e.toString());
		}

		return false;
	}

}
