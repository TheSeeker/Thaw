package thaw.plugins.index;

import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;

import java.sql.*;

import java.util.Enumeration;
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

		treeNode = new DefaultMutableTreeNode(displayName, false);

		this.db = db;
		this.tree = tree;

		this.id = id;
		this.parent = parent;
		this.realName = realName;
		this.displayName = displayName;
		this.modifiable = modifiable;

		this.publicKey = publicKey;

		this.privateKey = privateKey;

		this.revision = revision;

		this.author = author;
		
		treeNode.setUserObject(this);
	}

	public void setParent(IndexCategory parent) {
		this.parent = parent;
	}

	public DefaultMutableTreeNode getTreeNode() {
		return treeNode;
	}

	public void generateKeys(FCPQueueManager queueManager) {
		publicKey = "N/A";
		privateKey = "N/A";

		sskGenerator = new FCPGenerateSSK();
		sskGenerator.addObserver(this);
		sskGenerator.start(queueManager);
	}

	public boolean create() {
		try {
			/* Rahh ! Hsqldb doesn't support getGeneratedKeys() ! 8/ */
			
			Connection c = db.getConnection();
			PreparedStatement st;

			st = c.prepareStatement("SELECT id FROM indexes ORDER BY id DESC LIMIT 1");

			st.execute();

			try {
				ResultSet key = st.getResultSet();		
				key.next();
				id = key.getInt(1) + 1;
			} catch(SQLException e) {
				id = 1;
			}

			st = c.prepareStatement("INSERT INTO indexes (id, originalName, displayName, publicKey, privateKey, author, positionInTree, revision, parent) VALUES (?, ?,?,?,?,?, ?,?, ?)");
			st.setInt(1, id);
			st.setString(2, realName);
			st.setString(3, displayName);
			st.setString(4, publicKey);
			st.setString(5, privateKey);
			st.setString(6, author);
			st.setInt(7, 0);

			st.setInt(8, revision);

			if(parent != null && parent.getId() >= 0)
				st.setInt(9, parent.getId());
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
			Connection c = db.getConnection();
			PreparedStatement st;

			if(!modifiable) {
				st = c.prepareStatement("UPDATE indexes SET displayName = ? WHERE id = ?");
				st.setString(1, name);
				st.setInt(2, id);
			} else {
				st = c.prepareStatement("UPDATE indexes SET displayName = ?, originalName = ? WHERE id = ?");
				st.setString(1, name);
				st.setString(2, name);
				st.setInt(3, id);
			}
			
			st.execute();

			this.displayName = name;
			
			if(modifiable)
				this.realName = name;

		} catch(SQLException e) {
			Logger.error(this, "Unable to rename the index '"+this.displayName+"' in '"+name+"', because: "+e.toString());
		}

	}

	public void delete() {
		try {
			loadFiles(null, true);
			loadLinks(null, true);

			for(Iterator fileIt = fileList.iterator(); fileIt.hasNext(); ) {
				thaw.plugins.index.File file = (thaw.plugins.index.File)fileIt.next();
				file.delete();
			}
			    
			for (Iterator linkIt = linkList.iterator(); linkIt.hasNext() ;) {
				Link link = (Link)linkIt.next();
				link.delete();
			}

			Connection c = db.getConnection();
			PreparedStatement st = c.prepareStatement("DELETE FROM indexes WHERE id = ?");
			st.setInt(1, id);
			st.execute();
		} catch(SQLException e) {
			Logger.error(this, "Unable to delete the index '"+displayName+"', because: "+e.toString());
		}
	}


	protected String changeRevision(String key, int move) {
		try {
			String newKey;
			String[] split = key.split("/");

			newKey = split[0] + "/" + split[1] + "/"
				+ Integer.toString(Integer.parseInt(split[2])+move) + "/"
				+ split[3];
			
			return newKey;
		} catch (Exception e) {
			Logger.warning(this, "Unable to add a revision to the key '"+key+"' because : "+e.toString());
		}

		return key;
	}


	public void update() {
		targetFile = new java.io.File(toString()+".xml");

		if(modifiable) {
			FCPClientPut clientPut;

			Logger.info(this, "Generating index ...");
			generateXML(targetFile);

			if(targetFile.exists()) {
				Logger.info(this, "Inserting new version");
				
				revision++;

				clientPut = new FCPClientPut(targetFile, 2, revision, toString(), privateKey, 4, false, 0);
				transfer = clientPut;
				clientPut.addObserver(this);

				queueManager.addQueryToThePendingQueue(clientPut);

				save();

			} else {
				Logger.warning(this, "Index not generated !");
			}
		} else {
			FCPClientGet clientGet;
			
			Logger.info(this, "Getting lastest version ...");

			String key;

			if (!isEmpty())
				key = changeRevision(publicKey, 1);
			else
				key = publicKey;

			//key = key.replaceFirst("USK@", "SSK@");

			Logger.info(this, "Key asked: "+key);

			clientGet = new FCPClientGet(key, 4, 2, false, 1, System.getProperty("java.io.tmpdir"));
			transfer = clientGet;
			clientGet.addObserver(this);

			queueManager.addQueryToThePendingQueue(clientGet);
		}

		setChanged();
		notifyObservers();
		
	}

	public boolean isUpdating() {
		return (transfer != null && (!transfer.isFinished()));
	}

	public void purgeLinkList() {
		try {
			Connection c = db.getConnection();
			PreparedStatement st = c.prepareStatement("DELETE FROM links WHERE indexParent = ?");
			st.setInt(1, getId());
			st.execute();
			linkList = new Vector();
		} catch(SQLException e) {
			Logger.warning(this, "Unable to purge da list ! Exception: "+e.toString());
		}
	}
	
	public void purgeFileList() {
		try {
			Connection c = db.getConnection();
			PreparedStatement st = c.prepareStatement("DELETE FROM files WHERE indexParent = ?");
			st.setInt(1, getId());
			st.execute();
			fileList = new Vector();
		} catch(SQLException e) {
			Logger.warning(this, "Unable to purge da list ! Exception: "+e.toString());
		}
	}

	public void save() {
		try {
			Connection c = db.getConnection();
			PreparedStatement st = c.prepareStatement("UPDATE indexes SET originalName = ?, displayName = ?, publicKey = ?, privateKey = ?, positionInTree = ?, revision = ?, parent = ? , author = ? WHERE id = ?");

			st.setString(1, realName);
			st.setString(2, displayName);
			st.setString(3, publicKey);
			if(privateKey != null)
				st.setString(4, privateKey);
			else
				st.setNull(4, Types.VARCHAR);
			st.setInt(5, treeNode.getParent().getIndex(treeNode));
			
			st.setInt(6, revision);

			if( ((IndexTreeNode)treeNode.getParent()).getId() < 0)
				st.setNull(7, Types.INTEGER);
			else
				st.setInt(7, ((IndexTreeNode)treeNode.getParent()).getId());

			st.setString(8, author);
			
			st.setInt(9, getId());

			st.execute();
		} catch(SQLException e) {
			Logger.error(this, "Unable to save index state '"+toString()+"' because : "+e.toString());
		}
	}

	public int getId() {
		return id;
	}

	public String getKey() {
		if(modifiable)
			return publicKey.replaceFirst("SSK@", "USK@")+realName+"/"+revision+"/"+realName+".xml";
		else
			return publicKey;
	}

	public String toString() {
		if(displayName != null)
			return displayName;
		return realName;
	}

	public boolean isLeaf() {
		return true;
	}

	public void update(java.util.Observable o, Object p) {
		if(o == sskGenerator) {
			sskGenerator.deleteObserver(this);
			publicKey = sskGenerator.getPublicKey();
			privateKey = sskGenerator.getPrivateKey();

			Logger.debug(this, "Index public key: " +publicKey);
			Logger.debug(this, "Index private key: "+privateKey);

		}

		if(o == transfer) {
			if(transfer.isFinished() && transfer.isSuccessful()) {

				queueManager.remove(transfer);

				if(transfer instanceof FCPClientPut) {
					targetFile.delete();
					
					transfer = null;
					
					setChanged();
					notifyObservers();
					
					return;
				}
				
				if(transfer instanceof FCPClientGet) {
					java.io.File file = new java.io.File(transfer.getPath());

					Logger.info(this, "Updating index ...");
				
					publicKey = transfer.getFileKey();

					Logger.info(this, "Most up-to-date key found: " + publicKey);
					
					loadXML(file);
					save();

					Logger.info(this, "Update done.");

					file.delete();
					
					transfer = null;
					
					setChanged();
					notifyObservers();
					
					return;
				}

			}

			if (transfer.isFinished() && !transfer.isSuccessful()) {
				Logger.info(this, "Unable to get new version of the index");
				transfer = null;
				setChanged();
				notifyObservers();
				return;
			}

		}

		if (o instanceof thaw.plugins.index.File
		    || o instanceof Link) {
			setChanged();
			notifyObservers(o);
		}
	}
	

	public boolean isEmpty() {
		if (fileList == null)
			loadFiles(null, true);
		if (linkList == null)
			loadLinks(null, true);

		if (fileList.size() == 0 && linkList.size() == 0)
			return true;

		return false;
	}
	

	////// FILE LIST ////////

	public void loadFiles(String columnToSort, boolean asc) {
		if(fileList != null) {
			Logger.notice(this, "Files already loaded, won't reload them");
			return;
		}

		fileList = new Vector();

		try {
			String query = "SELECT id, publicKey, mime, size, localPath, category FROM files WHERE indexParent = ?";

			if(columnToSort != null) {
				query = query + "ORDER BY " + columnToSort;

				if(!asc)
					query = query + " DESC";
			}

			PreparedStatement st = db.getConnection().prepareStatement(query);

			st.setInt(1, getId());

			if(st.execute()) {
				ResultSet results = st.getResultSet();

				while(results.next()) {
					thaw.plugins.index.File file = new thaw.plugins.index.File(db, results, this);
					addFileToList(file);
				}
			}


		} catch(java.sql.SQLException e) {
			Logger.warning(this, "Unable to get the file list for index: '"+toString()+"' because: "+e.toString());
		}

		setChanged();
		notifyObservers();
	}

	/**
	 * Returns a *copy* of da vector.
	 */
	public Vector getFileList() {
		Vector newList = new Vector();

		for(Iterator it = fileList.iterator();
		    it.hasNext();) {
			newList.add(it.next());
		}

		return newList;
	}

	public thaw.plugins.index.File getFile(int index) {
		return (thaw.plugins.index.File)fileList.get(index);
	}


	public void unloadFiles() {
		for(Iterator it = fileList.iterator();
		    it.hasNext(); ) {
			thaw.plugins.index.File file = (thaw.plugins.index.File)it.next();
			if(file.getTransfer() != null && !file.getTransfer().isFinished()) {
				Logger.info(this, "Transfer still runinng. No unloading");
				return;
			}
		}

		if (fileList != null) {
			for (Iterator it  = fileList.iterator();
			     it.hasNext();)
				{
					thaw.plugins.index.File file = (thaw.plugins.index.File)it.next();
					file.deleteObserver(this);
				}
		}

		fileList = null;
	}


	/**
	 * Note for myself: For external use only ! (file will be inserted in the database etc)
	 */
	public void addFile(thaw.plugins.index.File file) {
		file.setParent(this);

		if (!file.isInTheDatabase()) {
			file.insert();

			addFileToList(file);
		
			setChanged();
			notifyObservers(file);
		}
		else
			Logger.notice(this, "File already in the database for this index");
	}


	/**
	 * Won't notify
	 */
	protected void addFileToList(thaw.plugins.index.File file) {
		if (fileList == null)
			loadFiles(null, true);
		file.addObserver(this);
		fileList.add(file);
	}


	public void removeFile(thaw.plugins.index.File file) {
		file.delete();

		if(fileList != null) {
			fileList.remove(file);

			setChanged();
			notifyObservers(file);
		}
	}

	/**
	 * Do the update all the files in the database.
	 */
	public void updateFileList() {		
		for(Iterator it = fileList.iterator(); it.hasNext();) {
			thaw.plugins.index.File file = (thaw.plugins.index.File)it.next();
			file.update();
		}
	}


	//// LINKS ////

	public void addLink(Link link) {
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

	protected void addLinkToList(Link link) {
		if (linkList == null)
			loadLinks(null, true);

		linkList.add(link);
	}

	public void removeLink(Link link) {
		link.delete();

		if (linkList != null) {
			linkList.remove(link);
			setChanged();
			notifyObservers(link);
		}

	}

	/**
	 * Update all the links in the database.
	 */
	public void updateLinkList() {
		for(Iterator it = linkList.iterator(); it.hasNext();) {
			Link link = (Link)it.next();
			link.update();
		}
	}
	
	public void loadLinks(String columnToSort, boolean asc)
	{
		if(linkList != null) {
			Logger.notice(this, "Links aleady loaded, won't reload ...");
			return;
		}

		linkList = new Vector();

		try {
			String query = "SELECT id, publicKey, mark, comment, indexTarget FROM links WHERE indexParent = ?";

			if(columnToSort != null) {
				query = query + "ORDER BY " + columnToSort;

				if(!asc)
					query = query + " DESC";
			}

			PreparedStatement st = db.getConnection().prepareStatement(query);

			st.setInt(1, getId());

			if(st.execute()) {
				ResultSet results = st.getResultSet();

				while(results.next()) {
					try {
						Link link = new Link(db, results, this);
						addLinkToList(link);
					} catch(Exception e) {
						Logger.warning(this, "Unable to add index '"+publicKey+"' to the list because: "+e.toString());
					}
				}
			}


		} catch(java.sql.SQLException e) {
			Logger.warning(this, "Unable to get the link list for index: '"+toString()+"' because: "+e.toString());
		}

	}

	/* Returns a copy ! */
	public Vector getLinkList()
	{
		Vector newList = new Vector();

		for(Iterator it = linkList.iterator();
		    it.hasNext();) {
			newList.add(it.next());
		}

		return newList;
	}

	public Link getLink(int index)
	{
		return (Link)linkList.get(index);
	}

	public void unloadLinks()
	{
		linkList = null;
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

		generateXML(outputStream);

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

		rootEl.appendChild(getXMLHeader(xmlDoc));
		rootEl.appendChild(getXMLLinks(xmlDoc));
		rootEl.appendChild(getXMLFileList(xmlDoc));
		
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
		Text titleText = xmlDoc.createTextNode(toString());		
		title.appendChild(titleText);

		Element owner = xmlDoc.createElement("owner");
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

	public Element getXMLLinks(Document xmlDoc) {
		Element links = xmlDoc.createElement("indexes");

		if (linkList == null) {
			loadLinks(null, true);
		}

		for (Iterator it = getLinkList().iterator();
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

		if(fileList == null) {
			loadFiles(null, true);
		}

		for(Iterator it = getFileList().iterator();
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

		loadHeader(rootEl);
		loadLinks(rootEl);
		loadFileList(rootEl);
	}


	public void loadHeader(Element rootEl) {
		Element header = (Element)rootEl.getElementsByTagName("header").item(0);

		realName = getHeaderElement(header, "title");
		author = getHeaderElement(header, "author");

		if (author == null)
			author = "Another anonymous";
	}

	public String getHeaderElement(Element header, String name) {
		try {
			Element sub = (Element)header.getElementsByTagName(name).item(0);
			return ((Text)sub.getFirstChild()).getData();
		} catch(Exception e) {
			Logger.notice(this, "Unable to get header element '"+name+"', because: "+e.toString());
			return null;
		}
	}

	public void loadLinks(Element rootEl) {
		purgeLinkList();

		Element links = (Element)rootEl.getElementsByTagName("indexes").item(0);
		NodeList list = links.getChildNodes();

		for(int i = 0; i < list.getLength() ; i++) {
			if(list.item(i).getNodeType() == Node.ELEMENT_NODE) {
				Element e = (Element)list.item(i);
				
				Link link = new Link(db, e, this);
				addLink(link);
			}
		}
		
	}

	public void loadFileList(Element rootEl) {
		purgeFileList();

		Element filesEl = (Element)rootEl.getElementsByTagName("files").item(0);
		NodeList list = filesEl.getChildNodes();

		for(int i = 0; i < list.getLength() ; i++) {
			if(list.item(i).getNodeType() == Node.ELEMENT_NODE) {
				Element e = (Element)list.item(i);
				
				thaw.plugins.index.File file = new thaw.plugins.index.File(db, e, this);	
				addFile(file);
			}
		}

		setChanged();
		notifyObservers();
	}


	static String getNameFromKey(String key) {
		String name = null;
		try {
			String[] cutcut = key.split("/");
			name = cutcut[cutcut.length-1];
			name = name.replaceAll(".xml", "");
		} catch (Exception e) {
			Logger.warning(e, "thaw.plugins.index.Index: Error while parsing index key: "+key+" because: "+e.toString() );
			name = key;
		}
		return name;
	}

	
	public Vector getIndexIds() {
		Vector ids = new Vector();
		ids.add(new Integer(getId()));
		return ids;
	}


	public boolean isInIndex(thaw.plugins.index.File file) {
		if (fileList == null)
			loadFiles(null, true);
		return fileList.contains(file);
	}

}
