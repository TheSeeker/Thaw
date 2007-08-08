package thaw.plugins.miniFrost.frostKSK;

import java.sql.*;

import java.util.Vector;
import java.util.Iterator;

import thaw.core.I18n;
import thaw.core.Config;
import thaw.plugins.Hsqldb;
import thaw.core.Logger;
import thaw.gui.FileChooser;
import thaw.fcp.*;


public class KSKFileAttachment
	extends KSKAttachment implements Runnable {

	private String filename;
	private long size;
	private String key;

	private FCPQueueManager queueManager;
	private KSKMessage msg;

	public KSKFileAttachment() {

	}

	public KSKFileAttachment(String filename,
				 long size,
				 String key) {
		this.filename = filename;
		this.size = size;
		this.key = key;
	}

	public KSKFileAttachment(String filename,
				 long size,
				 String key,
				 KSKMessage msg,
				 FCPQueueManager queueManager) {
		this(filename, size, key);
		this.msg = msg;
		this.queueManager = queueManager;
	}


	public String getType() {
		return "file";
	}

	public String getPrintableType() {
		return I18n.getMessage("thaw.common.file");
	}


	public String[] getProperties() {
		return new String[] {
			"name",
			"size",
			"key"
		};
	}

	public String[] getValues() {
		return new String[] {
			filename,
			Long.toString(size),
			key
		};
	}

	public String getValue(String property) {
		if ("name".equals(property)) {
			return filename;
		} else if ("size".equals(property)) {
			return Long.toString(size);
		} else if ("key".equals(property)) {
			return key;
		}

		return null;
	}

	public void setValue(String property, String value) {
		if ("name".equals(property)) {
			if (filename == null)
				filename = value;
		} else if ("size".equals(property)) {
			try {
				size = Long.parseLong(value);
			} catch(NumberFormatException e) {
				Logger.warning(this, "Can't parse size");
				return;
			}
		} else if ("key".equals(property)) {
			key = value;

			String possibleFilename = thaw.fcp.FreenetURIHelper.getFilenameFromKey(key);

			if (possibleFilename != null)
				possibleFilename = possibleFilename.trim();

			if (possibleFilename != null
			    && !("".equals(possibleFilename)))
				filename = possibleFilename;
		} else {
			Logger.error(this, "Unknown field : "+property);
		}
	}

	public String toString() {
		return filename;
	}

	public String getContainer() {
		return "File";
	}


	public String[] getActions() {
		return new String[] {
			I18n.getMessage("thaw.common.copyKeyToClipboard"),
			I18n.getMessage("thaw.plugin.miniFrost.copyAllKeys"),
			I18n.getMessage("thaw.common.action.download"),
			I18n.getMessage("thaw.plugin.miniFrost.downloadAll")
		};
	}

	private Vector keysToDownload;


	public void apply(Hsqldb db, FCPQueueManager queueManager, String action) {
		if (action.equals(I18n.getMessage("thaw.common.copyKeyToClipboard"))) {
			thaw.gui.GUIHelper.copyToClipboard(key);
		} else if (action.equals(I18n.getMessage("thaw.plugin.miniFrost.copyAllKeys"))) {
			String keys = "";

			Vector a = msg.getAttachments();

			for (Iterator it = a.iterator() ;
			     it.hasNext();) {
				Object o = it.next();

				if (o instanceof KSKFileAttachment)
					keys += ((KSKFileAttachment)o).getValue("key");
			}

			thaw.gui.GUIHelper.copyToClipboard(keys);

		} else if (action.equals(I18n.getMessage("thaw.common.action.download"))) {
			keysToDownload = new Vector();
			keysToDownload.add(key);

			Thread th = new Thread(this);
			th.start();
		} else if (action.equals(I18n.getMessage("thaw.plugin.miniFrost.downloadAll"))) {
			keysToDownload = new Vector();

			Vector a = msg.getAttachments();

			for (Iterator it = a.iterator() ;
			     it.hasNext();) {
				Object o = it.next();

				if (o instanceof KSKFileAttachment)
					keysToDownload.add(((KSKFileAttachment)o).getValue("key"));
			}

			Thread th = new Thread(this);
			th.start();
		}
	}


	public void run() {
		/* yeah, I didn't realize that I may need the config here :/ */
		Config config = ((KSKBoard)msg.getBoard()).getFactory().getCore().getConfig();

		FileChooser ch = new FileChooser(config.getValue("lastDestinationDirectory"));
		ch.setTitle(I18n.getMessage("thaw.plugin.fetch.chooseDestination"));
		ch.setDirectoryOnly(true);
		ch.setDialogType(javax.swing.JFileChooser.SAVE_DIALOG);

		java.io.File dir = ch.askOneFile();

		if (dir == null)
			return;

		for (Iterator it = keysToDownload.iterator();
		     it.hasNext();) {
			String key = (String)it.next();

			FCPClientGet get = new FCPClientGet(key, FCPClientGet.DEFAULT_PRIORITY,
							    FCPClientGet.PERSISTENCE_FOREVER,
							    true, /* globale queue */
							    FCPClientGet.DEFAULT_MAX_RETRIES,
							    dir.getPath());
			queueManager.addQueryToThePendingQueue(get);
		}
	}


	public void insert(Hsqldb db, int messageId) {
		if (filename == null
		    || size == 0
		    || key == null) {
			Logger.warning(this, "Missing field");
			return;
		}

		try {
			synchronized(db.dbLock) {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("INSERT INTO frostKSKAttachmentFiles "+
									 "(filename, size, key, messageId) "+
									 "VALUEs (?, ?, ?, ?)");
				st.setString(1, filename);
				st.setLong(2, size);
				st.setString(3, key);
				st.setInt(4, messageId);

				st.execute();
			}
		} catch(SQLException e) {
			Logger.error(this, "Can't insert the file attachment because : "+e.toString());
		}
	}


	public static Vector select(KSKMessage msg, KSKBoardFactory boardFactory,
				    Hsqldb db) {
		Vector v = new Vector();

		try {
			synchronized(db.dbLock) {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("SELECT filename, size, key "+
									 "FROM frostKSKAttachmentFiles "+
									 "WHERE messageId = ?");
				st.setInt(1, msg.getId());

				ResultSet set = st.executeQuery();

				while(set.next()) {
					v.add(new KSKFileAttachment(set.getString("filename"),
								    set.getLong("size"),
								    set.getString("key"),
								    msg,
								    boardFactory.getCore().getQueueManager()));
				}
			}
		} catch(SQLException e) {
			Logger.error(e, "Can't select file attachments because: "+e.toString());
		}

		return (v.size() > 0 ? v : null);
	}

	public StringBuffer getSignedStr() {
		StringBuffer buf = new StringBuffer();

		buf.append(filename).append(KSKMessageParser.SIGNATURE_ELEMENTS_SEPARATOR);
		buf.append(key).append(KSKMessageParser.SIGNATURE_ELEMENTS_SEPARATOR);

		return buf;
	}


	public static boolean destroy(KSKMessage msg, Hsqldb db) {
		try {
			synchronized(db.dbLock) {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("DELETE FROM frostKSKAttachmentFiles "+
									 "WHERE messageId = ?");
				st.setInt(1, msg.getId());
				st.execute();
			}
		} catch(SQLException e) {
			Logger.error(null, "Can't destroy the file attachments of the message because : "+e.toString());
			return false;
		}

		return true;
	}


	public static boolean destroyAll(KSKBoard board, Hsqldb db) {
		try {
			synchronized(db.dbLock) {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("SELECT id FROM frostKSKMessages "+
									 "WHERE boardId = ?");
				st.setInt(1, board.getId());

				ResultSet set = st.executeQuery();

				while(set.next()) {
					int id = set.getInt("id");
					st = db.getConnection().prepareStatement("DELETE FROM frostKSKAttachmentFiles "+
										 "WHERE messageId = ?");
					st.setInt(1, id);
					st.execute();
				}
			}
		} catch(SQLException e) {
			Logger.error(null, "Can't destroy the file attachments of the board because : "+e.toString());
			return false;
		}

		return true;
	}
}
