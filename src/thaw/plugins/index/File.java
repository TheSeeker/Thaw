package thaw.plugins.index;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Iterator;
import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import thaw.core.FreenetURIHelper;
import thaw.core.Logger;
import thaw.fcp.FCPClientGet;
import thaw.fcp.FCPClientPut;
import thaw.fcp.FCPQueueManager;
import thaw.fcp.FCPTransferQuery;
import thaw.plugins.Hsqldb;
import thaw.plugins.insertPlugin.DefaultMIMETypes;

public class File extends java.util.Observable implements java.util.Observer {
	private int id = -1; /* -1 = undefined */

	private String fileName;
	private String publicKey;
	private String mime;
	private long size = -1;
	private String category;

	private String localPath = null;
	private FCPTransferQuery transfer = null; /* can be null */

	private Index parent;
	private int parentId;

	private Hsqldb db;

	private FCPQueueManager queueManager = null;


	public File(final Hsqldb db, final String publicKey, final Index parent) {
		if (db == null)
			Logger.error(this, "No ref. to the database ?!");

		this.db = db;
		id = -1;
		this.publicKey = publicKey;
		deduceFilenameFromKey();

		size = 0;
	}

	/**
	 * @param path Local path
	 * @param transfer Corresponding tranfer (can be null).
	 */
	public File(final Hsqldb db, final String path, final String category, final Index parent, final FCPTransferQuery transfer) {
		this.db = db;

		if (db == null)
			Logger.error(this, "No ref. to the database ?! (2)");

		id = -1;
		localPath = path;
		this.transfer = transfer;

		final String[] pathElements = localPath.split(java.io.File.separator.replaceAll("\\\\", "\\\\\\\\"));
		fileName = pathElements[pathElements.length-1];

		size = (new java.io.File(path)).length();

		this.category = category;
		setParent(parent);

		if(transfer != null)
			((java.util.Observable)transfer).addObserver(this);
	}


	public File(final Hsqldb db, final ResultSet resultSet, final Index parent) throws SQLException {
		this.db = db;

		if (db == null)
			Logger.error(this, "No ref. to the database ?! (3)");

		id = resultSet.getInt("id");
		fileName = resultSet.getString("filename").trim();
		publicKey = resultSet.getString("publicKey").trim();
		localPath = resultSet.getString("localPath");
		size = resultSet.getLong("size");
		parentId = resultSet.getInt("indexParent");
		//category = resultSet.getString("category");

		deduceFilenameFromKey();

		this.parent = parent;
	}

	public File(final Hsqldb db, final Element fileElement, final Index parent) {
		this.db = db;

		if (db == null)
			Logger.error(this, "No ref. to the database ?! (4)");

		id = Integer.parseInt(fileElement.getAttribute("id")); /* will be changed when inserted in the database */
		publicKey = fileElement.getAttribute("key");

		if (publicKey != null)
			publicKey = publicKey.trim();

		localPath = null;

		size = Long.parseLong(fileElement.getAttribute("size"));

		setOptions(fileElement.getChildNodes());

		deduceFilenameFromKey();

		this.parent = parent;
	}

	public void deduceFilenameFromKey() {
		fileName = FreenetURIHelper.getFilenameFromKey(publicKey);
	}

	public void setOptions(final NodeList list) {
		for(int i = 0 ; i < list.getLength() ; i++) {
			if(list.item(i).getNodeType() == Node.ELEMENT_NODE) {
				final Element option = (Element)list.item(i);

				if("category".equals( option.getAttribute("name") ))
					category = option.getAttribute("value");
				else {
					/* TODO */
				}
			}
		}
	}

	public void setParent(final Index parent) {
		this.parent = parent;
		if (parent != null)
			parentId = parent.getId();
		else
			parentId = -1;
	}

	public Index getParent() {
		return parent;
	}

	public int getParentId() {
		return parentId;
	}

	public String getFilename() {
		return fileName;
	}

	public long getSize() {
		return size;
	}

	public String getLocalPath() {
		return localPath;
	}

	public String getCategory() {
		return category;
	}

	public String getPublicKey() {
		return publicKey;
	}

	public void setPublicKey(final String publicKey) {
		this.publicKey = publicKey;

		if (publicKey != null && !publicKey.equals(this.publicKey)) {
			this.publicKey = publicKey;
			update();
		}
		else
			this.publicKey = publicKey;

		setChanged();
		this.notifyObservers();
	}

	public FCPTransferQuery getTransfer() {
		return transfer;
	}

	public void setTransfer(final FCPTransferQuery query) {
		if (transfer != null) {
			Logger.notice(this, "A transfer is already running for this file");
			return;
		}

		transfer = query;

		if (transfer != null) {
			if(transfer instanceof FCPClientPut)
				((FCPClientPut)transfer).addObserver(this);
			if(transfer instanceof FCPClientGet)
				((FCPClientGet)transfer).addObserver(this);
		}

		if (transfer != null)
			update(((java.util.Observable)transfer), null);

		setChanged();
		this.notifyObservers(query);
	}


	public void recalculateCHK(final FCPQueueManager queueManager) {
		this.queueManager = queueManager;

		if (getLocalPath() == null) {
			Logger.notice(this, "Trying to recalculate key from a file where we don't have the local path");
			return;
		}

		final FCPClientPut insertion = new FCPClientPut(new java.io.File(getLocalPath()), 0, 0, null,
							  null, 4,
							  true, 2, true); /* getCHKOnly */
		queueManager.addQueryToThePendingQueue(insertion);

		this.setTransfer(insertion);
	}

	public void download(final String targetPath, final FCPQueueManager queueManager) {
		if (!FreenetURIHelper.isAKey(getPublicKey())) {
			Logger.warning(this, "Can't start download: file key is unknown");
			return;
		}

		if (getTransfer() != null && getTransfer().isRunning()) {
			Logger.warning(this, "Can't download: a transfer is already running");
			return;
		}

		final FCPClientGet clientGet = new FCPClientGet(getPublicKey(), 4, 0, true, -1, targetPath);

		queueManager.addQueryToThePendingQueue(clientGet);

		setTransfer(clientGet);
	}


	public void insertOnFreenet(final FCPQueueManager queueManager) {
		if (getTransfer() != null && getTransfer().isRunning()) {
			Logger.warning(this, "Another transfer is already running : can't insert");
			return;
		}

		final FCPClientPut clientPut = new FCPClientPut(new java.io.File(getLocalPath()),
							  0, 0, null, null, 4, true, 0);
		queueManager.addQueryToThePendingQueue(clientPut);

		setTransfer(clientPut);
	}


	/* Try to find its download automagically */
	public void setTransfer(final FCPQueueManager queueManager) {
		if ((publicKey != null) || (fileName != null)) {
			FCPTransferQuery trans;

			if (getPublicKey() == null)
				trans = queueManager.getTransfer(getFilename());
			else
				trans = queueManager.getTransfer(getPublicKey());

			if (trans == null) {
				trans = queueManager.getTransferByFilename(fileName);
			}

			setTransfer(trans);
		}

		setChanged();
		notifyObservers();
	}

	public void insert() {
		if (parent == null) {
			Logger.notice(this, "insert(): No parent !");
			return;
		}

		synchronized (db.dbLock) {
			try {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("SELECT id FROM files ORDER BY id DESC LIMIT 1");

				try {
					if(st.execute()) {
						final ResultSet result = st.getResultSet();
						result.next();
						id = result.getInt("id")+1;
					} else
						id = 1;
				} catch(final SQLException e) {
					id = 1;
				}


				st = db.getConnection().prepareStatement("INSERT INTO files (id, filename, publicKey, "+
						"localPath, mime, size, category, indexParent) "+
				"VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
				st.setInt(1, id);

				st.setString(2, fileName);

				if(publicKey != null)
					st.setString(3, publicKey);
				else
					st.setString(3, fileName);

				if(localPath != null)
					st.setString(4, localPath);
				else
					st.setNull(4, Types.VARCHAR);

				if(mime != null)
					st.setString(5, mime);
				else
					st.setNull(5, Types.VARCHAR);

				st.setLong(6, size);

				if(category != null)
					st.setString(7, category);
				else
					st.setNull(7, Types.VARCHAR);

				st.setInt(8, parent.getId());

				st.execute();
			} catch(final SQLException e) {
				Logger.error(this, "Unable to insert file '"+fileName+"' because: "+e.toString());
			}
		}
	}

	public void delete() {

		try {
			PreparedStatement st;

			st = db.getConnection().prepareStatement("DELETE FROM files WHERE id = ?");
			st.setInt(1, id);

			st.execute();

		} catch(final SQLException e) {
			Logger.error(this, "Unable to remove file '"+fileName+"' because: "+e.toString());
		}
	}

	public void update() {
		if (parent == null) {
			Logger.notice(this, "update(): No parent !");
			return;
		}

		try {
			PreparedStatement st;

			st = db.getConnection().prepareStatement("UPDATE files SET filename = ?, publicKey = ?, localPath = ?, mime= ?, size = ?, category = ?, indexParent = ? WHERE id = ?");

			st.setString(1, fileName);

			if(publicKey != null)
				st.setString(2, publicKey);
			else
				st.setString(2, fileName);

			if(localPath != null)
				st.setString(3, localPath);
			else
				st.setNull(3, Types.VARCHAR);

			if(mime != null)
				st.setString(4, mime);
			else
				st.setNull(4, Types.VARCHAR);

			st.setLong(5, size);

			if(category != null)
				st.setString(6, category);
			else
				st.setNull(6, Types.VARCHAR);

			st.setInt(7, getParent().getId());

			st.setInt(8, id);

			st.execute();
		} catch(final SQLException e) {
			Logger.error(this, "Unable to update file '"+fileName+"' because: "+e.toString());
		}
	}


	public boolean isInTheDatabase() {
		if (parent == null) {
			Logger.notice(this, "isInTheDatabase(): No parent !");
			return false;
		}

		try {
			PreparedStatement st;

			st = db.getConnection().prepareStatement("SELECT publicKey from files WHERE publicKey = ? AND indexParent = ?");

			if(publicKey != null)
				st.setString(1, publicKey);
			else
				st.setString(1, fileName);

			st.setInt(2, getParent().getId());

			if(st.execute()) {
				final ResultSet result = st.getResultSet();
				if ((result != null) && result.next())
					return true;
			}

		} catch(final SQLException e) {

		}

		return false;
	}


	public void update(final java.util.Observable o, final Object param) {
		if(o == transfer) {
			if (transfer.getFileKey() != null)
				setPublicKey(transfer.getFileKey());

			if (transfer.isFinished() && (transfer instanceof FCPClientPut)) {
				if (queueManager != null) {
					queueManager.remove(transfer);
					queueManager = null;
				}

				((FCPClientPut)transfer).deleteObserver(this);
				setPublicKey(transfer.getFileKey());
				update();
			}

			if(transfer.isFinished() && (transfer instanceof FCPClientGet)) {
				((FCPClientGet)transfer).deleteObserver(this);
				size = (new java.io.File(transfer.getPath())).length();
			}

			if(transfer.isFinished() && transfer.isSuccessful()) {
				transfer = null;
			}

			setChanged();
			this.notifyObservers();
		}
	}

	public int getId() {
		return id;
	}


	public Element getXML(final Document xmlDoc) {
		if(getPublicKey() == null) {
			Logger.notice(this, "No public key for file '"+fileName+"' => not added to the index");
			return null;
		}

		final Element file = xmlDoc.createElement("file");

		file.setAttribute("id", Integer.toString(getId()));
		file.setAttribute("key", getPublicKey());
		file.setAttribute("size", Long.toString(getSize()));
		file.setAttribute("mime", DefaultMIMETypes.guessMIMEType(fileName));

		for(final Iterator it = getOptionElements(xmlDoc).iterator();
		    it.hasNext(); ) {
			final Element e = (Element)it.next();
			file.appendChild(e);
		}

		return file;
	}

	/**
	 * @return Element Vector
	 */
	public Vector getOptionElements(final Document xmlDoc) {
		final Vector options = new Vector();

		if(category != null) {
			final Element categoryEl = xmlDoc.createElement("option");
			categoryEl.setAttribute("name", "category");
			categoryEl.setAttribute("value", category);

			options.add(categoryEl);
		}

		return options;
	}

	/**
	 * Modifiable in the database
	 */
	public boolean isModifiable() {
		if (getParent() == null) {
			Logger.warning(this, "No parent ?!");
			return false;
		}

		return getParent().isModifiable();
	}
}
