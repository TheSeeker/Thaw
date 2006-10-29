package thaw.plugins.index;

import java.sql.*;

import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Vector;
import java.util.Iterator;

import thaw.core.Logger;
import thaw.core.FreenetURIHelper;

import thaw.fcp.*;
import thaw.plugins.Hsqldb;

import thaw.plugins.insertPlugin.DefaultMIMETypes;

public class File extends java.util.Observable implements java.util.Observer {
	private int id = -1; /* -1 = undefined */

	private String fileName = null;
	private String publicKey = null;
	private String mime = null;
	private long size = -1;
	private String category = null;

	private String localPath = null;
	private FCPTransferQuery transfer = null; /* can be null */

	private Index parent;
	private int parentId;

	private Hsqldb db;

	private FCPQueueManager queueManager = null;

	/**
	 * @param path Local path
	 * @param transfer Corresponding tranfer (can be null).
	 */
	public File(Hsqldb db, String path, String category, Index parent, FCPTransferQuery transfer) {
		this.db = db;

		this.id = -1;
		this.localPath = path;
		this.transfer = transfer;

		String[] pathElements = this.localPath.split(java.io.File.separator.replaceAll("\\\\", "\\\\\\\\"));
		this.fileName = pathElements[pathElements.length-1];

		this.size = (new java.io.File(path)).length();

		this.category = category;
		this.setParent(parent);

		if(transfer != null)
			((java.util.Observable)transfer).addObserver(this);
	}


	public File(Hsqldb db, ResultSet resultSet, Index parent) throws SQLException {
		this.db = db;

		this.id = resultSet.getInt("id");
		this.fileName = resultSet.getString("filename").trim();
		this.publicKey = resultSet.getString("publicKey").trim();
		this.localPath = resultSet.getString("localPath");
		this.size = resultSet.getLong("size");
		this.parentId = resultSet.getInt("indexParent");
		//category = resultSet.getString("category");

		this.deduceFilenameFromKey();

		this.parent = parent;
	}

	public File(Hsqldb db, Element fileElement, Index parent) {
		this.db = db;

		this.id = Integer.parseInt(fileElement.getAttribute("id")); /* will be changed when inserted in the database */
		this.publicKey = fileElement.getAttribute("key");

		if (this.publicKey != null)
			this.publicKey = this.publicKey.trim();

		this.localPath = null;

		this.size = Long.parseLong(fileElement.getAttribute("size"));

		this.setOptions(fileElement.getChildNodes());

		this.deduceFilenameFromKey();

		this.parent = parent;
	}

	public void deduceFilenameFromKey() {
		this.fileName = FreenetURIHelper.getFilenameFromKey(this.publicKey);
	}

	public void setOptions(NodeList list) {
		for(int i = 0 ; i < list.getLength() ; i++) {
			if(list.item(i).getNodeType() == Node.ELEMENT_NODE) {
				Element option = (Element)list.item(i);

				if("category".equals( option.getAttribute("name") ))
					this.category = option.getAttribute("value");
				else {
					/* TODO */
				}
			}
		}
	}

	public void setParent(Index parent) {
		this.parent = parent;
		if (parent != null)
			this.parentId = parent.getId();
		else
			this.parentId = -1;
	}

	public Index getParent() {
		return this.parent;
	}

	public int getParentId() {
		return this.parentId;
	}

	public String getFilename() {
		return this.fileName;
	}

	public long getSize() {
		return this.size;
	}

	public String getLocalPath() {
		return this.localPath;
	}

	public String getCategory() {
		return this.category;
	}

	public String getPublicKey() {
		return this.publicKey;
	}

	public void setPublicKey(String publicKey) {
		this.publicKey = publicKey;

		this.setChanged();
		this.notifyObservers();
	}

	public FCPTransferQuery getTransfer() {
		return this.transfer;
	}

	public void setTransfer(FCPTransferQuery query) {
		if (this.transfer != null) {
			Logger.notice(this, "A transfer is already running for this file");
			return;
		}

		this.transfer = query;

		if (this.transfer != null) {
			if(this.transfer instanceof FCPClientPut)
				((FCPClientPut)this.transfer).addObserver(this);
			if(this.transfer instanceof FCPClientGet)
				((FCPClientGet)this.transfer).addObserver(this);
		}

		this.setChanged();
		this.notifyObservers(query);
	}


	public void recalculateCHK(FCPQueueManager queueManager) {
		this.queueManager = queueManager;

		FCPClientPut insertion = new FCPClientPut(new java.io.File(this.getLocalPath()), 0, 0, null,
							  null, 4,
							  true, 2, true); /* getCHKOnly */
		queueManager.addQueryToThePendingQueue(insertion);

		this.setTransfer(insertion);
	}

	public void download(String targetPath, FCPQueueManager queueManager) {
		FCPClientGet clientGet = new FCPClientGet(this.getPublicKey(), 4, 0, true, -1, targetPath);

		queueManager.addQueryToThePendingQueue(clientGet);

		this.setTransfer(clientGet);
	}


	public void insertOnFreenet(FCPQueueManager queueManager) {
		FCPClientPut clientPut = new FCPClientPut(new java.io.File(this.getLocalPath()),
							  0, 0, null, null, 4, true, 0);
		queueManager.addQueryToThePendingQueue(clientPut);

		this.setTransfer(clientPut);
	}


	/* Try to find its download automagically */
	public void setTransfer(FCPQueueManager queueManager) {
		if (this.publicKey != null || this.fileName != null) {
			FCPTransferQuery trans;

			trans = queueManager.getTransfer(this.publicKey);

			if (trans == null) {
				trans = queueManager.getTransferByFilename(this.fileName);
			}

			this.setTransfer(trans);
		}

		this.setChanged();
		this.notifyObservers();
	}

	public synchronized void insert() {
		if (this.parent == null) {
			Logger.notice(this, "insert(): No parent !");
			return;
		}

		db.lockWriting();

		try {
			PreparedStatement st;

			st = this.db.getConnection().prepareStatement("SELECT id FROM files ORDER BY id DESC LIMIT 1");

			try {
				if(st.execute()) {
					ResultSet result = st.getResultSet();
					result.next();
					this.id = result.getInt("id")+1;
				} else
					this.id = 1;
			} catch(SQLException e) {
				this.id = 1;
			}


			st = this.db.getConnection().prepareStatement("INSERT INTO files (id, filename, publicKey, "+
								       "localPath, mime, size, category, indexParent) "+
								       "VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
			st.setInt(1, this.id);

			st.setString(2, this.fileName);

			if(this.publicKey != null)
				st.setString(3, this.publicKey);
			else
				st.setString(3, this.fileName);

			if(this.localPath != null)
				st.setString(4, this.localPath);
			else
				st.setNull(4, Types.VARCHAR);

			if(this.mime != null)
				st.setString(5, this.mime);
			else
				st.setNull(5, Types.VARCHAR);

			st.setLong(6, this.size);

			if(this.category != null)
				st.setString(7, this.category);
			else
				st.setNull(7, Types.VARCHAR);

			st.setInt(8, this.parent.getId());

			st.execute();
		} catch(SQLException e) {
			Logger.error(this, "Unable to insert file '"+this.fileName+"' because: "+e.toString());
		}

		db.unlockWriting();
	}

	public void delete() {

		try {
			PreparedStatement st;

			st = this.db.getConnection().prepareStatement("DELETE FROM files WHERE id = ?");
			st.setInt(1, this.id);

			st.execute();

		} catch(SQLException e) {
			Logger.error(this, "Unable to remove file '"+this.fileName+"' because: "+e.toString());
		}
	}

	public void update() {
		if (this.parent == null) {
			Logger.notice(this, "update(): No parent !");
			return;
		}

		try {
			PreparedStatement st;

			st = this.db.getConnection().prepareStatement("UPDATE files SET filename = ?, publicKey = ?, localPath = ?, mime= ?, size = ?, category = ?, indexParent = ? WHERE id = ?");

			st.setString(1, this.fileName);

			if(this.publicKey != null)
				st.setString(2, this.publicKey);
			else
				st.setString(2, this.fileName);

			if(this.localPath != null)
				st.setString(3, this.localPath);
			else
				st.setNull(3, Types.VARCHAR);

			if(this.mime != null)
				st.setString(4, this.mime);
			else
				st.setNull(4, Types.VARCHAR);

			st.setLong(5, this.size);

			if(this.category != null)
				st.setString(6, this.category);
			else
				st.setNull(6, Types.VARCHAR);

			st.setInt(7, this.getParent().getId());

			st.setInt(8, this.id);

			st.execute();
		} catch(SQLException e) {
			Logger.error(this, "Unable to update file '"+this.fileName+"' because: "+e.toString());
		}
	}


	public boolean isInTheDatabase() {
		if (this.parent == null) {
			Logger.notice(this, "isInTheDatabase(): No parent !");
			return false;
		}

		try {
			PreparedStatement st;

			st = this.db.getConnection().prepareStatement("SELECT publicKey from files WHERE publicKey = ? AND indexParent = ?");

			if(this.publicKey != null)
				st.setString(1, this.publicKey);
			else
				st.setString(1, this.fileName);

			st.setInt(2, this.getParent().getId());

			if(st.execute()) {
				ResultSet result = st.getResultSet();
				if (result != null && result.next()) {
					return true;
				}
			}

		} catch(SQLException e) {

		}

		return false;
	}


	public void update(java.util.Observable o, Object param) {
		if(o == this.transfer) {
			if(this.transfer.isFinished() && this.transfer instanceof FCPClientPut) {
				if (this.queueManager != null) {
					this.queueManager.remove(this.transfer);
					this.queueManager = null;
				}

				((FCPClientPut)this.transfer).deleteObserver(this);
				this.setPublicKey(this.transfer.getFileKey());
				this.update();
			}

			if(this.transfer.isFinished() && this.transfer instanceof FCPClientGet) {
				((FCPClientGet)this.transfer).deleteObserver(this);
			}

			if(this.transfer.isFinished() && this.transfer.isSuccessful()) {
				this.transfer = null;
			}

			this.setChanged();
			this.notifyObservers();
		}
	}

	public int getId() {
		return this.id;
	}


	public Element getXML(Document xmlDoc) {
		if(this.getPublicKey() == null) {
			Logger.notice(this, "No public key for file '"+this.fileName+"' => not added to the index");
			return null;
		}

		Element file = xmlDoc.createElement("file");

		file.setAttribute("id", Integer.toString(this.getId()));
		file.setAttribute("key", this.getPublicKey());
		file.setAttribute("size", Long.toString(this.getSize()));
		file.setAttribute("mime", DefaultMIMETypes.guessMIMEType(this.fileName));

		for(Iterator it = this.getOptionElements(xmlDoc).iterator();
		    it.hasNext(); ) {
			Element e = (Element)it.next();
			file.appendChild(e);
		}

		return file;
	}

	/**
	 * @return Element Vector
	 */
	public Vector getOptionElements(Document xmlDoc) {
		Vector options = new Vector();

		if(this.category != null) {
			Element categoryEl = xmlDoc.createElement("option");
			categoryEl.setAttribute("name", "category");
			categoryEl.setAttribute("value", this.category);

			options.add(categoryEl);
		}

		return options;
	}

}
