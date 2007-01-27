package thaw.plugins.index;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Iterator;
import java.util.Vector;
import java.util.Observer;
import java.util.Observable;

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

public class File implements Observer {
	private Hsqldb db = null;
	private int id = -1; /* -1 = undefined */

	private String filename = null;
	private String publicKey = null;
	private java.io.File localPath = null;
	private String mime = null;
	private long size = 0;

	private int parentId;

	/* if not null, the transfer will be removed when finished */
	private FCPQueueManager queueManager = null;


	public File(final Hsqldb db, final int id) {
		this.db = db;
		this.id = id;
		reloadDataFromDb(id);
	}

	public File(final Hsqldb db, final int id, final String filename,
		    String publicKey, java.io.File localPath,
		    String mime, long size, int parentId) {
		this.db = db;
		this.id = id;
		this.filename = filename;
		this.publicKey = publicKey;
		this.localPath = localPath;
		this.mime = mime;
		this.size = size;
		this.parentId = parentId;
	}


	public void update(Observable o, Object param) {
		if (o instanceof FCPClientPut) {
			FCPClientPut put = (FCPClientPut)o;

			String key = put.getFileKey();

			if (FreenetURIHelper.isAKey(key)) {
				setPublicKey(key);
				o.deleteObserver(this);
			}

			if (queueManager != null) {
				queueManager.remove(put);
				queueManager = null;
			}

			return;
		}

		Logger.error(this, "Unknow object: "+o.toString());
	}


	public void reloadDataFromDb(int id) {
		this.id = id;

		try {
			PreparedStatement st;

			st = db.getConnection().prepareStatement("SELECT filename, publicKey, localPath, mime, size, indexParent "+
								 " FROM files WHERE id = ? LIMIT 1");

			st.setInt(1, id);

			ResultSet rs = st.executeQuery();

			if (rs.next()) {
				String lp;

				filename = rs.getString("filename");
				publicKey = rs.getString("publicKey");
				lp = rs.getString("localPath");
				localPath = (lp == null ? null : new java.io.File(lp));
				mime = rs.getString("mime");
				size = rs.getLong("size");
				parentId = rs.getInt("indexParent");
			} else {
				Logger.error(this, "File '"+Integer.toString(id)+"' not found");
			}

		} catch(SQLException e) {
			Logger.error(this, "Unable to get info for file '"+Integer.toString(id)+"'");
		}
	}

	public void forceReload() {
		reloadDataFromDb(id);
	}

	public void setParent(int parent_id) {
		try {
			PreparedStatement st;

			st = db.getConnection().prepareStatement("UPDATE files SET indexParent = ? "+
								 "WHERE id = ?");
			st.setInt(1, parent_id);
			st.setInt(2, id);
			st.execute();
		} catch(SQLException e) {
			Logger.error(this, "Unable to set parent: "+e.toString());
		}
	}

	public String getFilename() {
		if (filename == null)
			reloadDataFromDb(id);

		return filename;
	}

	public long getSize() {
		return size;
	}

	public String getLocalPath() {
		if (localPath == null)
			reloadDataFromDb(id);

		if (localPath == null)
			return null;

		return localPath.getAbsolutePath();
	}

	public String getPublicKey() {
		if (publicKey == null)
			reloadDataFromDb(id);

		return publicKey;
	}


	public FCPTransferQuery getTransfer(FCPQueueManager q) {
		FCPTransferQuery tr;

		tr = q.getTransfer(getPublicKey());

		return tr;
	}

	public void setPublicKey(final String publicKey) {
		this.publicKey = publicKey;

		try {
			PreparedStatement st;

			st = db.getConnection().prepareStatement("UPDATE files SET publicKey = ? "+
								 "WHERE id = ?");

			st.setString(1, publicKey);
			st.setInt(2, id);
			st.execute();
		} catch(SQLException e) {
			Logger.error(this, "Unable to set publicKey: "+e.toString());
		}
	}


	public void setSize(final long size) {
		this.publicKey = publicKey;

		try {
			PreparedStatement st;

			st = db.getConnection().prepareStatement("UPDATE files SET size = ? "+
								 "WHERE id = ?");

			st.setLong(1, size);
			st.setInt(2, id);
			st.execute();
		} catch(SQLException e) {
			Logger.error(this, "Unable to set publicKey: "+e.toString());
		}
	}


	public FCPClientPut recalculateCHK(final FCPQueueManager queueManager) {
		if (localPath == null) {
			Logger.notice(this, "Trying to recalculate key from a file where we don't have the local path");
			return null;
		}

		if (getTransfer(queueManager) != null) {
			Logger.notice(this, "Another transfer is already running for this file");
			return null;
		}

		final FCPClientPut insertion = new FCPClientPut(localPath, 0, 0, null,
								null, 4,
								true, 2, true); /* getCHKOnly */

		this.queueManager = queueManager; /* so the transfer will be removed when finished */
		queueManager.addQueryToTheRunningQueue(insertion);

		insertion.addObserver(this);

		return insertion;
	}


	public FCPClientGet download(final String targetPath, final FCPQueueManager queueManager) {
		FCPTransferQuery q;
		String publicKey = getPublicKey();

		if (publicKey == null) {
			Logger.notice(this, "No key !");
			return null;
		}

		if (!FreenetURIHelper.isAKey(publicKey)) {
			Logger.warning(this, "Can't start download: file key is unknown");
			return null;
		}

		if ( (q = getTransfer(queueManager)) != null && q.isRunning()) {
			Logger.warning(this, "Can't download: a transfer is already running");
			return null;
		}

		final FCPClientGet clientGet = new FCPClientGet(publicKey, 4, 0, true, -1, targetPath);

		queueManager.addQueryToThePendingQueue(clientGet);

		return clientGet;
	}


	public FCPClientPut insertOnFreenet(final FCPQueueManager queueManager) {
		FCPTransferQuery q;
		String localPath;

		if ( (q = getTransfer(queueManager)) != null && q.isRunning()) {
			Logger.warning(this, "Another transfer is already running : can't insert");
			return null;
		}

		localPath = getLocalPath();

		if (localPath == null) {
			Logger.warning(this, "No local path => can't insert");
			return null;
		}

		final FCPClientPut clientPut = new FCPClientPut(new java.io.File(localPath),
								0, 0, null, null, 4, true, 0);
		queueManager.addQueryToThePendingQueue(clientPut);

		clientPut.addObserver(this);

		return clientPut;
	}


	public int getId() {
		return id;
	}

	public int getParentId() {
		try {
			PreparedStatement st;

			st = db.getConnection().prepareStatement("SELECT indexParent FROM files "+
								 "WHERE id = ? LIMIT 1");
			st.setInt(1, id);

			ResultSet rs = st.executeQuery();

			if (rs.next()) {
				return rs.getInt("indexParent");
			} else {
				Logger.error(this, "File id not found: "+Integer.toString(id));
			}

		} catch(SQLException e) {
			Logger.error(this, "Unable to get parent id because: "+e.toString());
		}

		return -1;
	}


	public Element getXML(final Document xmlDoc) {
		/* TODO */
		return null;
	}


	public void delete() {
		try {
			PreparedStatement st;

			st = db.getConnection().prepareStatement("DELETE FROM files WHERE id = ?");
			st.setInt(1, id);
			st.execute();
		} catch(SQLException e) {
			Logger.error(this, "Unable to remove file because: "+e.toString());
		}
	}


	/**
	 * Modifiable in the database<br/>
	 * Note: Do a SQL requests each time
	 */
	public boolean isModifiable() {
		return (new Index(db, parentId)).isModifiable();
	}
}
