package thaw.plugins.miniFrost.frostKSK;

import java.util.Vector;
import java.util.Observer;
import java.util.Observable;

import java.io.File;

import java.text.SimpleDateFormat;

import java.sql.*;


import thaw.fcp.*;

import thaw.core.Logger;
import thaw.core.I18n;

import thaw.plugins.Hsqldb;
import thaw.plugins.signatures.Identity;


/**
 * only notify when the message has been fully parsed
 */
public class KSKMessage
	extends Observable
	implements thaw.plugins.miniFrost.interfaces.Message,
		   Observer {

	public final static int FCP_PRIORITY    = 2; /* below 2, the node doesn't react ?! */

	public final static int FCP_MAX_RETRIES = 1; /* we don't have time to loose, in the worst case,
						      * we will come back later
						      */
	public final static int FCP_MAX_SIZE    = 32*1024;


	/* content is not kept in memory (at least not here) */
	private int            id;
	private String         subject;
	private KSKAuthor      author;
	private int            sigId;
	private java.util.Date date;
	private int            rev;
	private boolean        read;
	private boolean        archived;
	private KSKBoard       board;


	public KSKMessage(KSKBoard board,
			  java.util.Date date, int rev) {
		this.board     = board;
		this.date      = date;
		this.rev       = rev;
	}

	private Hsqldb db;

	private boolean downloading = false; /* put back to false only after parsing */
	private boolean successfullyDownloaded = false;
	private boolean successfullyParsed = false;

	private String key = null;

	public void download(FCPQueueManager queueManager, Hsqldb db) {
		this.db = db;
		downloading = true;

		java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyy.M.d");
		//formatter.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));

		key = board.getDownloadKey(date, rev);

		Logger.info(this, "Fetching : "+key.toString());

		FCPClientGet get = new FCPClientGet(key, FCP_PRIORITY,
						    FCPClientGet.PERSISTENCE_UNTIL_DISCONNECT,
						    false /* globalQueue */,
						    FCP_MAX_RETRIES,
						    System.getProperty("java.io.tmpdir"),
						    FCP_MAX_SIZE,
						    true /* noDDA */);
		get.setNoRedirectionFlag(true);
		get.addObserver(this);

		/* we override the queueManager */
		get.start(queueManager);
	}

	public void update(Observable o, Object param) {
		FCPClientGet get = (FCPClientGet)o;

		if (!get.isFinished())
			return;

		if (!get.isSuccessful()) {
			int code = get.getGetFailedCode();

			if (code == 13 /* dnd */
			    || code == 14 /* route not found */
			    || code == 20 /* jflesch is stupid */) {
				Logger.info(this, key+" not found");
				successfullyDownloaded = false;
			} else {
				Logger.notice(this, "Problem with "+key);
				successfullyDownloaded = true;
			}

			downloading = false;
			successfullyParsed = false;
		} else {

			Logger.info(this, key+" found => parsing");

			KSKMessageParser parser = new KSKMessageParser();

			if (parser.loadFile(new File(get.getPath()))
			    && parser.checkSignature(db)
			    && parser.insert(db, board.getId(),
					     date, rev, board.getName())) {

				new File(get.getPath()).delete();

				Logger.info(this, "Parsing ok");
				successfullyDownloaded = true;
				downloading            = false;
				successfullyParsed     = true;

			} else {
				Logger.notice(this, "Unable to parse. File not deleted");
				successfullyDownloaded = true;
				downloading            = false;
				successfullyParsed     = false;
			}
		}

		setChanged();
		notifyObservers();
	}


	public boolean isDownloading() {
		return downloading;
	}

	/**
	 * @return true if the FCPClientGet didn't end on a 'Data not found',
	 *              a 'Route not found', etc
	 */
	public boolean isSuccessful() {
		return successfullyDownloaded;
	}

	/**
	 * @return true if the message was correctly parsed
	 * Note: it returns false if the signature was invalid.
	 */
	public boolean isParsable() {
		return successfullyParsed;
	}


	public KSKMessage(int id,
			  String subject, String nick,
			  int sigId, Identity identity,
			  java.util.Date date, int rev,
			  boolean read, boolean archived,
			  KSKBoard board) {
		this.id        = id;
		this.subject   = subject;

		this.author    = new KSKAuthor(nick, identity);

		this.sigId     = sigId;
		this.date      = date;
		this.rev       = rev;
		this.read      = read;
		this.archived  = archived;
		this.board     = board;
	}

	public String getSubject() {
		return subject;
	}

	public thaw.plugins.miniFrost.interfaces.Author getSender() {
		return author;
	}

	public java.util.Date getDate() {
		return date;
	}


	public int getRev() {
		return rev;
	}

	public boolean isArchived() {
		return archived;
	}

	public boolean isRead() {
		return read;
	}

	public thaw.plugins.miniFrost.interfaces.Board getBoard() {
		return board;
	}

	public void setRead(boolean read) {
		if (read == this.read)
			return;

		this.read = read;

		if (read)
			board.setNewMessageNumber(board.getNewMessageNumber() -1);
		else
			board.setNewMessageNumber(board.getNewMessageNumber() +1);

		try {
			Hsqldb db = board.getFactory().getDb();

			synchronized(db.dbLock) {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("UPDATE frostKSKMessages "+
									 "SET read = ? "+
									 "WHERE id = ?");
				st.setBoolean(1, read);
				st.setInt(2, id);

				st.execute();
			}
		} catch(SQLException e) {
			Logger.error(this, "Can't update read status because : "+e.toString());
		}
	}

	public void setArchived(boolean archived) {
		if (archived == this.archived)
			return;

		this.archived = archived;

		if (!read) {
			if (archived)
				board.setNewMessageNumber(board.getNewMessageNumber() -1);
			else
				board.setNewMessageNumber(board.getNewMessageNumber() +1);
		}

		try {
			Hsqldb db = board.getFactory().getDb();

			synchronized(db.dbLock) {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("UPDATE frostKSKMessages "+
									 "SET archived = ? "+
									 "WHERE id = ?");
				st.setBoolean(1, archived);
				st.setInt(2, id);

				st.execute();
			}
		} catch(SQLException e) {
			Logger.error(this, "Can't update archived status because : "+e.toString());
		}
	}


	protected Vector parseMessage(final String fullMsg) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd - HH:mm:ss");
		//sdf.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));

		Vector v = new Vector();

		String[] split = fullMsg.split("(\\A|[^-])-----[^-]");

		if (split.length < 4) {
			Logger.notice(this, "Not enought elements in the messages");
			return null;
		}

		/* the first element of split is expected to be empty */

		if (split[0] == null || !"".equals(split[0])) {
			Logger.notice(this, "Does not begin as expected ?");
			return null;
		}

		for (int i = 0 ; i < split.length ; i++) {
			split[i] = split[i].trim();
		}

		for (int i = 1 ; i < split.length ; i += 3) {
			/* expect 3 elements */

			if ((i + 2) >= split.length) {
				Logger.notice(this, "Not enought elements in the last part");
				return null;
			}

			String author  = split[i];
			String dateStr = split[i+1].replaceAll("GMT", "");
			String msg     = split[i+2];

			java.util.Date date = sdf.parse(dateStr, new java.text.ParsePosition(0));

			if (date == null) {
				Logger.notice(this, "Unable to parse the date : "+dateStr);
				return null;
			}

			v.add(new KSKSubMessage(new KSKAuthor(author, null),
						date, msg));

		}


		return v;
	}


	protected String getMsgId() {
		try {
			Hsqldb db = board.getFactory().getDb();

			synchronized(db.dbLock) {

				PreparedStatement st;

				st = db.getConnection().prepareStatement("SELECT msgId "+
									 "FROM frostKSKMessages "+
									 "WHERE id = ? "+
									 "LIMIT 1");
				st.setInt(1, id);

				ResultSet set = st.executeQuery();

				if (!set.next())
					return null;

				return set.getString("msgId");
			}
		} catch(SQLException e) {
			Logger.error(this, "Error while getting the messages : "+e.toString());
			return null;
		}
	}


	public String getRawMessage() {
		try {
			Hsqldb db = board.getFactory().getDb();

			synchronized(db.dbLock) {

				PreparedStatement st;

				st = db.getConnection().prepareStatement("SELECT content "+
									 "FROM frostKSKMessages "+
									 "WHERE id = ? "+
									 "LIMIT 1");
				st.setInt(1, id);

				ResultSet set = st.executeQuery();

				if (!set.next())
					return null;

				return set.getString("content");
			}
		} catch(SQLException e) {
			Logger.error(this, "Error while getting the messages : "+e.toString());
			return null;
		}
	}


	/** no caching */
	public Vector getSubMessages() {

		String content, nick;

		try {
			Hsqldb db = board.getFactory().getDb();

			synchronized(db.dbLock) {

				PreparedStatement st;

				st = db.getConnection().prepareStatement("SELECT content, nick "+
									 "FROM frostKSKMessages "+
									 "WHERE id = ? "+
									 "LIMIT 1");
				st.setInt(1, id);

				ResultSet set = st.executeQuery();

				if (!set.next())
					return null;

				content = set.getString("content");
				nick = set.getString("nick");
			}
		} catch(SQLException e) {
			Logger.error(this, "Error while getting the messages : "+e.toString());
			return null;
		}

		boolean parsed = true;
		Vector v = null;

		try {
			v = parseMessage(content);

			int size;

			if (v == null || (size = v.size()) <= 0) {
				parsed = false;
			} else {
				KSKSubMessage lastMsg = (KSKSubMessage)v.get(size-1);
				lastMsg.setAuthor((KSKAuthor)getSender());
				lastMsg.setDate(getDate());
			}

		} catch(Exception e) { /* dirty, but should work */
			Logger.error(this, "Error while parsing : "+e.toString());
			parsed = false;
		}

		if (!parsed) {
			Logger.warning(this, "Unable to parse the message ?! returning raw content");
			v = new Vector();
			v.add(new KSKSubMessage((KSKAuthor)getSender(),
						getDate(),
						"==== "+I18n.getMessage("thaw.plugin.miniFrost.rawMessage")+" ===="
						+"\n\n"+content));
		}


		return v;
	}

	protected int getId() {
		return id;
	}


	public Vector getAttachments() {
		return KSKAttachmentFactory.getAttachments(this,
							   board.getFactory(),
							   board.getFactory().getDb());

	}


	public boolean equals(Object o) {
		if (!(o instanceof KSKMessage))
			return false;

		return (((KSKMessage)o).getId() == id);
	}


	public static boolean destroy(KSKBoard board, Hsqldb db) {
		if (!KSKFileAttachment.destroy(board, db)
		    || !KSKBoardAttachment.destroy(board, db))
			return false;

		try {
			synchronized(db.dbLock) {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("DELETE FROM frostKSKMessages "+
									 "WHERE boardId = ?");
				st.setInt(1, board.getId());
				st.execute();
			}
		} catch(SQLException e) {
			Logger.error(null, "Can't destroy the board messages because : "+e.toString());
			return false;
		}

		return true;
	}
}
