package thaw.plugins.index;

import javax.swing.tree.DefaultMutableTreeNode;

import java.sql.*;

import java.util.Vector;
import java.util.Iterator;

import org.w3c.dom.Document;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.DOMImplementation;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.OutputKeys;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.OutputStream;
import java.io.FileOutputStream;

import thaw.fcp.*;
import thaw.plugins.Hsqldb;
import thaw.core.*;

public class Index extends java.util.Observable implements FileAndLinkList, IndexTreeNode, java.util.Observer {

	private Hsqldb db;
	private IndexTree tree;

	private int id;
	private IndexCategory parent;
	private String realName;
	private String displayName;
	private boolean modifiable;
	
	private String publicKey; /* without the filename ! */
	private String privateKey;

	private int revision = 0;

	private Vector fileList;
	private Vector linkList;

	private DefaultMutableTreeNode treeNode;

	private FCPGenerateSSK sskGenerator;

	private FCPQueueManager queueManager;

	private FCPTransferQuery transfer = null;
	private java.io.File targetFile = null;

	private String author = null;

	private boolean rewriteKey = true;
	

	private FCPClientPut publicKeyRecalculation = null;


	/**
	 * The bigest constructor of the world ...
	 */
	public Index(Hsqldb db, FCPQueueManager queueManager,
		     int id, IndexCategory parent,
		     String realName, String displayName,
		     String publicKey, String privateKey,
		     int revision, String author, 
		     boolean modifiable) {
		this.queueManager = queueManager;

		this.treeNode = new DefaultMutableTreeNode(displayName, false);

		this.db = db;
		this.tree = this.tree;

		this.id = id;
		this.parent = parent;
		this.realName = realName.trim();
		this.displayName = displayName.trim();
		this.modifiable = (privateKey == null ? false : true);

		if (privateKey != null)
			this.privateKey = privateKey.trim();
		else
			this.privateKey = null;

		if (publicKey != null)
			this.publicKey = publicKey.trim();
		else
			this.publicKey = null;


		if (modifiable == true && publicKey != null && publicKey.startsWith("USK@")) {
			String[] split = FreenetURIHelper.convertUSKtoSSK(publicKey).split("/");
			publicKey = split[0];
		}

		this.revision = revision;

		this.author = author;
		
		this.treeNode.setUserObject(this);

		this.setTransfer();
	}

	public void setParent(IndexCategory parent) {
		this.parent = parent;
	}

	public IndexCategory getParent() {
		return this.parent;
	}

	public DefaultMutableTreeNode getTreeNode() {
		return this.treeNode;
	}

	public Index getIndex(int id) {
		return (id == this.getId()) ? this : null;
	}

	public void generateKeys(FCPQueueManager queueManager) {
		this.publicKey = "N/A";
		this.privateKey = "N/A";

		this.sskGenerator = new FCPGenerateSSK();
		this.sskGenerator.addObserver(this);
		this.sskGenerator.start(queueManager);
	}

	public boolean create() {
		try {
			/* Rahh ! Hsqldb doesn't support getGeneratedKeys() ! 8/ */
			
			Connection c = this.db.getConnection();
			PreparedStatement st;

			st = c.prepareStatement("SELECT id FROM indexes ORDER BY id DESC LIMIT 1");

			st.execute();

			try {
				ResultSet key = st.getResultSet();		
				key.next();
				this.id = key.getInt(1) + 1;
			} catch(SQLException e) {
				this.id = 1;
			}

			st = c.prepareStatement("INSERT INTO indexes (id, originalName, displayName, publicKey, privateKey, author, positionInTree, revision, parent) VALUES (?, ?,?,?,?,?, ?,?, ?)");
			st.setInt(1, this.id);
			st.setString(2, this.realName);
			st.setString(3, this.displayName);
			st.setString(4, this.publicKey);
			st.setString(5, this.privateKey);
			st.setString(6, this.author);
			st.setInt(7, 0);

			st.setInt(8, this.revision);

			if(this.parent != null && this.parent.getId() >= 0)
				st.setInt(9, this.parent.getId());
			else
				st.setNull(9, Types.INTEGER);
			
			st.execute();
						
			return true;
		} catch(SQLException e) {
			Logger.error(this, "Unable to insert the new index in the db, because: "+e.toString());

			return false;
		}

	}

	public void rename(String name) {
		try {
			Connection c = this.db.getConnection();
			PreparedStatement st;

			if(!this.modifiable) {
				st = c.prepareStatement("UPDATE indexes SET displayName = ? WHERE id = ?");
				st.setString(1, name);
				st.setInt(2, this.id);
			} else {
				st = c.prepareStatement("UPDATE indexes SET displayName = ?, originalName = ? WHERE id = ?");
				st.setString(1, name);
				st.setString(2, name);
				st.setInt(3, this.id);
			}
			
			st.execute();

			this.displayName = name;
			
			if(this.modifiable)
				this.realName = name;

		} catch(SQLException e) {
			Logger.error(this, "Unable to rename the index '"+this.displayName+"' in '"+name+"', because: "+e.toString());
		}

	}

	public void delete() {
		try {
			this.loadFiles(null, true);
			this.loadLinks(null, true);

			for(Iterator fileIt = this.fileList.iterator(); fileIt.hasNext(); ) {
				thaw.plugins.index.File file = (thaw.plugins.index.File)fileIt.next();
				file.delete();
			}
			    
			for (Iterator linkIt = this.linkList.iterator(); linkIt.hasNext() ;) {
				Link link = (Link)linkIt.next();
				link.delete();
			}

			Connection c = this.db.getConnection();
			PreparedStatement st = c.prepareStatement("DELETE FROM indexes WHERE id = ?");
			st.setInt(1, this.id);
			st.execute();
		} catch(SQLException e) {
			Logger.error(this, "Unable to delete the index '"+this.displayName+"', because: "+e.toString());
		}
	}


	public void update() {
		this.targetFile = new java.io.File(this.toString()+".xml");

		if (this.transfer != null) {
			Logger.notice(this, "A transfer is already running");
			return;
		}

		if(this.modifiable) {
			FCPClientPut clientPut;

			Logger.info(this, "Generating index ...");
			this.generateXML(this.targetFile);

			if(this.targetFile.exists()) {
				Logger.info(this, "Inserting new version");
				
				this.revision++;

				clientPut = new FCPClientPut(this.targetFile, 2, this.revision, this.toString(), this.privateKey, 2, false, 0);
				this.transfer = clientPut;
				clientPut.addObserver(this);

				this.queueManager.addQueryToThePendingQueue(clientPut);

				this.save();

			} else {
				Logger.warning(this, "Index not generated !");
			}

			this.setChanged();
			this.notifyObservers();
		} else {
			this.updateFromFreenet(-1);
		}
		
	}

	public void updateFromFreenet(int rev) {
		FCPClientGet clientGet;
		
		Logger.info(this, "Getting lastest version ...");
		
		String key;
		
		/* We will trust the node for the incrementation
		   execept if a rev is specified */

		
		if (rev >= 0) {
			key = FreenetURIHelper.convertUSKtoSSK(this.publicKey);
			key = FreenetURIHelper.changeSSKRevision(key, rev, 0);
			this.rewriteKey = false;
		} else {
			if (!this.modifiable) {
				key = this.publicKey;
				this.rewriteKey = true;
			} else {
				key = this.getPublicKey();
				this.rewriteKey = false;
			}
		}
		
		Logger.info(this, "Key asked: "+key);
		
		clientGet = new FCPClientGet(key, 2, 2, false, -1, System.getProperty("java.io.tmpdir"));
		this.transfer = clientGet;
		clientGet.addObserver(this);
		
		this.queueManager.addQueryToThePendingQueue(clientGet);

		this.setChanged();
		this.notifyObservers();
	}

	public boolean isUpdating() {
		return (this.transfer != null && (!this.transfer.isFinished()));
	}


	protected void setTransfer(FCPTransferQuery query) {
		this.transfer = query;

		if (this.transfer != null) {
			if (this.transfer instanceof FCPClientGet)
				((FCPClientGet)this.transfer).addObserver(this);
			if (this.transfer instanceof FCPClientPut)
				((FCPClientPut)this.transfer).addObserver(this);
			this.update(((java.util.Observable)this.transfer), null);
		}
	}

	protected void setTransfer() {
		if (this.queueManager == null)
			return;

		if (this.getPublicKey() != null) {
			if(!this.queueManager.isQueueCompletlyLoaded()) {
				Thread th = new Thread(new WaitCompleteQueue());
				th.start();
				return;
			}
			
			String key;

			if (this.modifiable)
				key = FreenetURIHelper.getPublicInsertionSSK(this.getPublicKey());
			else
				key = this.getPublicKey();

			this.setTransfer(this.queueManager.getTransfer(key));
		}

		this.setChanged();
		this.notifyObservers();
	}

	private class WaitCompleteQueue implements Runnable {
		public WaitCompleteQueue() { }

		public void run() {
			int tryNumber = 0;

			while(!Index.this.queueManager.isQueueCompletlyLoaded() && tryNumber < 120 /* 1min */) {
				try {
					Thread.sleep(500);
				} catch(java.lang.InterruptedException e) {
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
		try {
			Connection c = this.db.getConnection();
			PreparedStatement st = c.prepareStatement("DELETE FROM links WHERE indexParent = ?");
			st.setInt(1, this.getId());
			st.execute();
			this.linkList = new Vector();
		} catch(SQLException e) {
			Logger.warning(this, "Unable to purge da list ! Exception: "+e.toString());
		}
	}
	
	public void purgeFileList() {
		try {
			Connection c = this.db.getConnection();
			PreparedStatement st = c.prepareStatement("DELETE FROM files WHERE indexParent = ?");
			st.setInt(1, this.getId());
			st.execute();
			this.fileList = new Vector();
		} catch(SQLException e) {
			Logger.warning(this, "Unable to purge da list ! Exception: "+e.toString());
		}
	}

	public void save() {
		try {
			Connection c = this.db.getConnection();
			PreparedStatement st = c.prepareStatement("UPDATE indexes SET originalName = ?, displayName = ?, publicKey = ?, privateKey = ?, positionInTree = ?, revision = ?, parent = ? , author = ? WHERE id = ?");

			st.setString(1, this.realName);
			st.setString(2, this.displayName);
			st.setString(3, this.publicKey);
			if(this.privateKey != null)
				st.setString(4, this.privateKey);
			else
				st.setNull(4, Types.VARCHAR);
			st.setInt(5, this.treeNode.getParent().getIndex(this.treeNode));
			
			st.setInt(6, this.revision);

			if( ((IndexTreeNode)this.treeNode.getParent()).getId() < 0)
				st.setNull(7, Types.INTEGER);
			else
				st.setInt(7, ((IndexTreeNode)this.treeNode.getParent()).getId());

			st.setString(8, this.author);
			
			st.setInt(9, this.getId());

			st.execute();
		} catch(SQLException e) {
			Logger.error(this, "Unable to save index state '"+this.toString()+"' because : "+e.toString());
		}
	}

	public int getId() {
		return this.id;
	}

	public String getPublicKey() {
		if (this.publicKey == null)
			return this.publicKey;

		if(this.publicKey.startsWith("SSK@")) { /* as it should when modifiable == true */
			return this.publicKey.replaceFirst("SSK@", "USK@")+this.realName+"/"+this.revision+"/"+this.realName+".xml";
		} else
			return this.publicKey;
	}

	public String getPrivateKey() {
		if (!this.modifiable)
			return null;

		return this.privateKey;
	}

	public String toString() {
		if(this.displayName != null)
			return this.displayName;
		return this.realName;
	}

	public boolean isLeaf() {
		return true;
	}


	public void update(java.util.Observable o, Object p) {

		if(o == this.sskGenerator) {
			this.sskGenerator.deleteObserver(this);
			this.publicKey = this.sskGenerator.getPublicKey();
			this.privateKey = this.sskGenerator.getPrivateKey();

			Logger.debug(this, "Index public key: " +this.publicKey);
			Logger.debug(this, "Index private key: "+this.privateKey);

		}

		if(o == this.transfer) {
			if(this.transfer.isFinished() && this.transfer.isSuccessful()) {

				this.queueManager.remove(this.transfer);

				if(this.transfer instanceof FCPClientPut) {
					if (this.targetFile != null)
						this.targetFile.delete();
					else {
						String path = ((FCPClientPut)this.transfer).getPath();
						if (path != null) {
							java.io.File fl = new java.io.File(path);
							fl.delete();
						}
					} 
					
					this.transfer = null;
					
					this.setChanged();
					this.notifyObservers();
					
					return;
				}
				
				if(this.transfer instanceof FCPClientGet) {
					java.io.File file = new java.io.File(this.transfer.getPath());

					Logger.info(this, "Updating index ...");
				
					if (this.rewriteKey)
						this.publicKey = this.transfer.getFileKey();
					else
						this.revision = FreenetURIHelper.getUSKRevision(this.transfer.getFileKey());

					Logger.info(this, "Most up-to-date key found: " + this.publicKey);
					
					this.loadXML(file);
					this.save();

					Logger.info(this, "Update done.");

					file.delete();
					
					this.transfer = null;
					
					this.setChanged();
					this.notifyObservers();
					
					return;
				}

			}

			if (this.transfer.isFinished() && !this.transfer.isSuccessful()) {
				Logger.info(this, "Unable to get new version of the index");
				this.transfer = null;
				this.setChanged();
				this.notifyObservers();
				return;
			}

		}

		if (o instanceof thaw.plugins.index.File
		    || o instanceof Link) {
			this.setChanged();
			this.notifyObservers(o);
		}
	}
	

	public boolean isEmpty() {
		if (this.fileList == null)
			this.loadFiles(null, true);
		if (this.linkList == null)
			this.loadLinks(null, true);

		if (this.fileList.size() == 0 && this.linkList.size() == 0)
			return true;

		return false;
	}
	

	////// FILE LIST ////////

	public void loadFiles(String columnToSort, boolean asc) {
		if(this.fileList != null) {
			Logger.notice(this, "Files already loaded, won't reload them");
			return;
		}

		this.fileList = new Vector();

		try {
			String query = "SELECT id, filename, publicKey, mime, size, localPath, category, indexParent FROM files WHERE indexParent = ?";

			if(columnToSort != null) {
				query = query + "ORDER BY " + columnToSort;

				if(!asc)
					query = query + " DESC";
			}

			PreparedStatement st = this.db.getConnection().prepareStatement(query);

			st.setInt(1, this.getId());

			if(st.execute()) {
				ResultSet results = st.getResultSet();

				while(results.next()) {
					thaw.plugins.index.File file = new thaw.plugins.index.File(this.db, results, this);
					file.setTransfer(this.queueManager);
					this.addFileToList(file);
				}
			}


		} catch(java.sql.SQLException e) {
			Logger.warning(this, "Unable to get the file list for index: '"+this.toString()+"' because: "+e.toString());
		}

		this.setChanged();
		this.notifyObservers();
	}

	/**
	 * Returns a *copy* of da vector.
	 */
	public Vector getFileList() {
		Vector newList = new Vector();

		for(Iterator it = this.fileList.iterator();
		    it.hasNext();) {
			newList.add(it.next());
		}

		return newList;
	}

	public thaw.plugins.index.File getFile(int index) {
		return (thaw.plugins.index.File)this.fileList.get(index);
	}

	public int getFilePosition(thaw.plugins.index.File fileA) {
		int pos = this.fileList.indexOf(fileA);

		if (fileA.getPublicKey() == null)
			return -1;

		if (pos < 0) {
			/* Manual research */
			pos = 0;
			for(Iterator it = this.fileList.iterator();
			    it.hasNext();) {
				thaw.plugins.index.File fileB = (thaw.plugins.index.File)it.next();

				if (fileB.getPublicKey() != null
				    && fileB.getPublicKey().equals(fileA.getPublicKey()))
					return pos;
				pos++;

			}
		}

		return -1;
	}


	public void unloadFiles() {
		/*
		for(Iterator it = fileList.iterator();
		    it.hasNext(); ) {
			thaw.plugins.index.File file = (thaw.plugins.index.File)it.next();
			if(file.getTransfer() != null && !file.getTransfer().isFinished()) {
				Logger.info(this, "Transfer still runinng. No unloading");
				return;
			}
		}
		*/

		if (this.fileList != null) {
			for (Iterator it  = this.fileList.iterator();
			     it.hasNext();)
				{
					thaw.plugins.index.File file = (thaw.plugins.index.File)it.next();
					file.deleteObserver(this);
				}
		}

		this.fileList = null;
	}


	/**
	 * Note for myself: For external use only ! (file will be inserted in the database etc)
	 */
	public void addFile(thaw.plugins.index.File file) {
		file.setParent(this);

		if (!file.isInTheDatabase()) {
			file.insert();

			this.addFileToList(file);
		
			this.setChanged();
			this.notifyObservers(file);
		}
		else
			Logger.notice(this, "File already in the database for this index");
	}


	/**
	 * Won't notify
	 */
	protected void addFileToList(thaw.plugins.index.File file) {
		if (this.fileList == null)
			this.loadFiles(null, true);
		file.addObserver(this);
		this.fileList.add(file);
	}


	public void removeFile(thaw.plugins.index.File file) {
		file.delete();

		if(this.fileList != null) {
			this.fileList.remove(file);

			this.setChanged();
			this.notifyObservers(file);
		}
	}

	/**
	 * Do the update all the files in the database.
	 */
	public void updateFileList() {		
		for(Iterator it = this.fileList.iterator(); it.hasNext();) {
			thaw.plugins.index.File file = (thaw.plugins.index.File)it.next();
			file.update();
		}
	}


	//// LINKS ////

	public void addLink(Link link) {
		link.setParent(this);

		if (!link.isInTheDatabase()) {
			link.insert();

			this.addLinkToList(link);
			this.setChanged();
			this.notifyObservers(link);
		}
		else
			Logger.notice(this, "Link already in the database for this index");

	}

	protected void addLinkToList(Link link) {
		if (this.linkList == null)
			this.loadLinks(null, true);

		this.linkList.add(link);
	}

	public void removeLink(Link link) {
		link.delete();

		if (this.linkList != null) {
			this.linkList.remove(link);
			this.setChanged();
			this.notifyObservers(link);
		}

	}

	/**
	 * Update all the links in the database.
	 */
	public void updateLinkList() {
		for(Iterator it = this.linkList.iterator(); it.hasNext();) {
			Link link = (Link)it.next();
			link.update();
		}
	}
	
	public void loadLinks(String columnToSort, boolean asc)
	{
		if(this.linkList != null) {
			Logger.notice(this, "Links aleady loaded, won't reload ...");
			return;
		}

		this.linkList = new Vector();

		try {
			String query = "SELECT id, publicKey, mark, comment, indexTarget FROM links WHERE indexParent = ?";

			if(columnToSort != null) {
				query = query + "ORDER BY " + columnToSort;

				if(!asc)
					query = query + " DESC";
			}

			PreparedStatement st = this.db.getConnection().prepareStatement(query);

			st.setInt(1, this.getId());

			if(st.execute()) {
				ResultSet results = st.getResultSet();

				while(results.next()) {
					try {
						Link link = new Link(this.db, results, this);
						this.addLinkToList(link);
					} catch(Exception e) {
						Logger.warning(this, "Unable to add index '"+this.publicKey+"' to the list because: "+e.toString());
					}
				}
			}


		} catch(java.sql.SQLException e) {
			Logger.warning(this, "Unable to get the link list for index: '"+this.toString()+"' because: "+e.toString());
		}

	}

	/* Returns a copy ! */
	public Vector getLinkList()
	{
		Vector newList = new Vector();

		for(Iterator it = this.linkList.iterator();
		    it.hasNext();) {
			newList.add(it.next());
		}

		return newList;
	}

	public Link getLink(int index)
	{
		return (Link)this.linkList.get(index);
	}

	public void unloadLinks()
	{
		this.linkList = null;
	}


	//// XML ////

	public void generateXML(java.io.File file) {
		
		FileOutputStream outputStream;

		try {
			outputStream = new FileOutputStream(file);
		} catch(java.io.FileNotFoundException e) {
			Logger.warning(this, "Unable to create file '"+file+"' ! not generated !");
			return;
		}

		this.generateXML(outputStream);

		try {
			outputStream.close();
		} catch(java.io.IOException e) {
			Logger.error(this, "Unable to close stream because: "+e.toString());
		}
	}

	public void generateXML(OutputStream out) {
		StreamResult streamResult = new StreamResult(out);

		Document xmlDoc;
		
		DocumentBuilderFactory xmlFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder xmlBuilder;

		try {
			xmlBuilder = xmlFactory.newDocumentBuilder();
		} catch(javax.xml.parsers.ParserConfigurationException e) {
			Logger.error(this, "Unable to generate the index because : "+e.toString());
			return;
		}

		DOMImplementation impl = xmlBuilder.getDOMImplementation();

		xmlDoc = impl.createDocument(null, "index", null);

		Element rootEl = xmlDoc.getDocumentElement();

		rootEl.appendChild(this.getXMLHeader(xmlDoc));
		rootEl.appendChild(this.getXMLLinks(xmlDoc));
		rootEl.appendChild(this.getXMLFileList(xmlDoc));
		
		/* Serialization */
		DOMSource domSource = new DOMSource(xmlDoc);
		TransformerFactory transformFactory = TransformerFactory.newInstance();

		Transformer serializer;

		try {
			serializer = transformFactory.newTransformer();
		} catch(javax.xml.transform.TransformerConfigurationException e) {
			Logger.error(this, "Unable to save index because: "+e.toString());
			return;
		}

		serializer.setOutputProperty(OutputKeys.ENCODING,"UTF-8");
		serializer.setOutputProperty(OutputKeys.INDENT,"yes");
		
		/* final step */
		try {
			serializer.transform(domSource, streamResult);
		} catch(javax.xml.transform.TransformerException e) {
			Logger.error(this, "Unable to save index because: "+e.toString());
			return;
		}
	}

	public Element getXMLHeader(Document xmlDoc) {
		Element header = xmlDoc.createElement("header");
		
		Element title = xmlDoc.createElement("title");
		Text titleText = xmlDoc.createTextNode(this.toString());		
		title.appendChild(titleText);

		Element owner = xmlDoc.createElement("owner");
		Text ownerText;

		if (this.author == null)
			ownerText = xmlDoc.createTextNode("Another anonymous");
		else
			ownerText = xmlDoc.createTextNode(this.author);

		owner.appendChild(ownerText);
		
		header.appendChild(title);
		header.appendChild(owner);

		return header;
	}

	public Element getXMLLinks(Document xmlDoc) {
		Element links = xmlDoc.createElement("indexes");

		if (this.linkList == null) {
			this.loadLinks(null, true);
		}

		for (Iterator it = this.getLinkList().iterator();
		     it.hasNext();) {
			Link link = (Link)it.next();

			Element xmlLink = link.getXML(xmlDoc);

			if (xmlLink != null)
				links.appendChild(xmlLink);
			else
				Logger.warning(this, "Unable to get XML for the link '"+link.getIndexName()+"' => Gruick da link");
		}

		return links;
	}

	public Element getXMLFileList(Document xmlDoc) {
		Element files = xmlDoc.createElement("files");

		if(this.fileList == null) {
			this.loadFiles(null, true);
		}

		for(Iterator it = this.getFileList().iterator();
		    it.hasNext();) {
			thaw.plugins.index.File file = (thaw.plugins.index.File)it.next();

			Element xmlFile = file.getXML(xmlDoc);

			if(xmlFile != null)
				files.appendChild(xmlFile);
			else
				Logger.warning(this, "Public key wasn't generated ! Not added to the index !");
		}

		return files;
	}

	public void loadXML(java.io.File file) {
		DocumentBuilderFactory xmlFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder xmlBuilder;

		try {
			xmlBuilder = xmlFactory.newDocumentBuilder();
		} catch(javax.xml.parsers.ParserConfigurationException e) {
			Logger.error(this, "Unable to load index because: "+e.toString());
			return;
		}
		
		Document xmlDoc;

		try {
			xmlDoc = xmlBuilder.parse(file);
		} catch(org.xml.sax.SAXException e) {
			Logger.error(this, "Unable to load index because: "+e.toString());
			return;
		} catch(java.io.IOException e) {
			Logger.error(this, "Unable to load index because: "+e.toString());
			return;
		}

		Element rootEl = xmlDoc.getDocumentElement();

		this.loadHeader(rootEl);
		this.loadLinks(rootEl);
		this.loadFileList(rootEl);
	}


	public void loadHeader(Element rootEl) {
		Element header = (Element)rootEl.getElementsByTagName("header").item(0);

		this.realName = this.getHeaderElement(header, "title");
		this.author = this.getHeaderElement(header, "owner");

		if (this.author == null)
			this.author = "Another anonymous";
	}

	public String getHeaderElement(Element header, String name) {
		try {
			if (header == null)
				return null;

			NodeList nl = header.getElementsByTagName(name);

			if (nl == null)
				return null;

			Element sub = (Element)nl.item(0);

			if (sub == null)
				return null;

			Text txt = (Text)sub.getFirstChild();

			if (txt == null)
				return null;

			return txt.getData();
		} catch(Exception e) {
			Logger.notice(this, "Unable to get header element '"+name+"', because: "+e.toString());
			return null;
		}
	}

	public void loadLinks(Element rootEl) {
		this.purgeLinkList();

		Element links = (Element)rootEl.getElementsByTagName("indexes").item(0);
		NodeList list = links.getChildNodes();

		for(int i = 0; i < list.getLength() ; i++) {
			if(list.item(i).getNodeType() == Node.ELEMENT_NODE) {
				Element e = (Element)list.item(i);
				
				Link link = new Link(this.db, e, this);
				this.addLink(link);
			}
		}
		
	}

	public void loadFileList(Element rootEl) {
		this.purgeFileList();

		Element filesEl = (Element)rootEl.getElementsByTagName("files").item(0);
		NodeList list = filesEl.getChildNodes();

		for(int i = 0; i < list.getLength() ; i++) {
			if(list.item(i).getNodeType() == Node.ELEMENT_NODE) {
				Element e = (Element)list.item(i);
				
				thaw.plugins.index.File file = new thaw.plugins.index.File(this.db, e, this);	
				this.addFile(file);
			}
		}

		this.setChanged();
		this.notifyObservers();
	}


	static String getNameFromKey(String key) {
		String name = null;

		name = FreenetURIHelper.getFilenameFromKey(key);

		if (name == null)
			return null;

		name = name.replaceAll(".xml", "");

		return name;
	}

	
	public Vector getIndexIds() {
		Vector ids = new Vector();
		ids.add(new Integer(this.getId()));
		return ids;
	}


	public boolean isInIndex(thaw.plugins.index.File file) {
		if (this.fileList == null)
			this.loadFiles(null, true);
		return this.fileList.contains(file);
	}

}
