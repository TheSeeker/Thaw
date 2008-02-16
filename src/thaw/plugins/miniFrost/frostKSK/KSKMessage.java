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
import thaw.plugins.miniFrost.interfaces.Message;

/**
 * only notify when the message has been fully parsed
 */
public class KSKMessage
	extends Observable
	implements Message, Observer {

	public final static int FCP_PRIORITY    = 2; /* below 2, the node doesn't react ?! */

	public final static int FCP_MAX_RETRIES = 1; /* we don't have time to loose, in the worst case,
						      * we will come back later
						      */
	public final static int FCP_MAX_SIZE    = 32*1024;


	/* content is not kept in memory (at least not here) */
	private int            id;
	private String         idStr;
	private String         inReplyToStr;
	private String         subject;
	private KSKAuthor      author;
	private java.util.Date date;
	private int            rev;
	private boolean        read;
	private boolean        archived;
	private Identity       encryptedFor;
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
	private FCPQueueManager queueManager = null;

	private String key = null;

	public void download(FCPQueueManager queueManager, Hsqldb db) {
		this.db = db;
		this.queueManager = queueManager;
		
		downloading = true;

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
			
			if (code == 21 /* Too big */
				|| code == 28 /* All data not found */) {

				Logger.warning(this, "MiniFrost: Invalid key: "+key);
				successfullyDownloaded = true;
				
				board.addInvalidSlot(date, rev);
				
			} else if (get.getProtocolErrorCode() == 4 /* URI parse error */
				|| get.getProtocolErrorCode() == 20 /* URL parse error */
			    || code == 20 /* Invalid URI */) {
				Logger.warning(this, "MiniFrost: Invalid key: "+key);
				successfullyDownloaded = true;
			} else if (get.getProtocolErrorCode() >= 0) {
				Logger.warning(this,
					       "MiniFrost: Unknown protocol error (code="+
					       Integer.toString(get.getProtocolErrorCode())+"). Please report.");
				successfullyDownloaded = true;
			} else if (code == 13 /* dnd */
			    || code == 14 /* route not found */
			    || code == 20 /* jflesch is stupid */) {
				Logger.info(this, key+" not found");
				successfullyDownloaded = false;
			} else {
				Logger.notice(this, "Problem with "+key + " ; code : "+Integer.toString(code));
				successfullyDownloaded = true;
			}

			downloading = false;
			successfullyParsed = false;
		} else {

			Logger.info(this, key+" found => parsing");

			KSKMessageParser parser = new KSKMessageParser();
			
			if (!parser.loadFile(new File(get.getPath()), db)) {
				/* invalid slot */
				Logger.notice(this, " message: '"+board.getName()+"'"
							+" - "+date.toString()
							+" - "+Integer.toString(rev));
					
				board.addInvalidSlot(date, rev);
				
				new File(get.getPath()).delete();
				
				successfullyDownloaded = true;
				downloading            = false;
				successfullyParsed     = false;
				read = false;
				
			} else if (parser.checkSignature(db)
			    && parser.filter(board.getFactory().getPlugin().getRegexpBlacklist())
			    && parser.insert(db, board.getId(),
					     date, rev, board.getName())) {
				
				if (parser.getTrustListPublicKey() != null) {
					board.getFactory().getWoT().addTrustList(parser.getIdentity(),
															parser.getTrustListPublicKey(),
															parser.getDate());
				}

				new File(get.getPath()).delete();

				Logger.info(this, "Parsing ok");
				successfullyDownloaded = true;
				downloading            = false;
				successfullyParsed     = true;
				read = parser.mustBeDisplayedAsRead();

			} else {

				new File(get.getPath()).delete();

				Logger.notice(this, "Unable to parse.");
				successfullyDownloaded = true;
				downloading            = false;
				successfullyParsed     = false;
				read = true;
			}
		}
		
		get.stop(queueManager);
		queueManager.remove(get);

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


	public String getMsgId() {
		return idStr;
	}

	public String getInReplyToId() {
		return inReplyToStr;
	}


	public KSKMessage(int id, String idStr,
			  String inReplyToStr,
			  String subject, String nick,
			  int sigId, Identity identity,
			  java.util.Date date, int rev,
			  boolean read, boolean archived,
			  Identity encryptedFor,
			  KSKBoard board) {
		this.id           = id;
		this.idStr        = idStr;
		this.inReplyToStr = inReplyToStr;
		this.subject      = subject;

		this.author       = new KSKAuthor(nick, identity);

		this.date         = date;
		this.rev          = rev;
		this.read         = read;
		this.archived     = archived;
		this.board        = board;
		this.encryptedFor = encryptedFor;
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

	public int compareTo(Object o) {
		if (getDate() == null && ((Message)o).getDate() != null)
			return -1;

		if (getDate() != null && ((Message)o).getDate() == null)
			return 1;

		if (getDate() == null && ((Message)o).getDate() == null)
			return 0;

		int c = getDate().compareTo( ((Message)o).getDate());

		return -1 * c;
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

	public Identity encryptedFor() {
		return encryptedFor;
	}

	public thaw.plugins.miniFrost.interfaces.Board getBoard() {
		return board;
	}

	public void setRead(boolean read) {
		if (read == this.read)
			return;

		this.read = read;

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
				st.close();
			}
		} catch(SQLException e) {
			Logger.error(this, "Can't update read status because : "+e.toString());
		}
	}

	public void setArchived(boolean archived) {
		if (archived == this.archived)
			return;

		this.archived = archived;

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
				st.close();
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

				if (!set.next()) {
					st.close();
					return null;
				}

				String s = set.getString("content");
				st.close();
				return s;
			}
		} catch(SQLException e) {
			Logger.error(this, "Error while getting the messages : "+e.toString());
			return null;
		}
	}


	private void setAuthorAndDate(Vector subMsgs, KSKAuthor author,
				      java.util.Date date) {

		// we browse the vector by starting with the last element
		// so we can't use an iterator
		// the goal is to find the corresponding KSKAuthor
		// and replace it by ours (ours has its identity set)
		for (int i = subMsgs.size()-1; i >= 0 ; i--) {
			KSKSubMessage sub = (KSKSubMessage)subMsgs.get(i);

			if (author.toString().equals(sub.getAuthor().toString())) {
				sub.setAuthor(author);
				sub.setDate(date);
				return;
			}
		}

		// we didn't find it, so we force it on the last message
		Logger.notice(this, "KSKAuthor forced on the last sub-messages");
		KSKSubMessage lastMsg = (KSKSubMessage)subMsgs.get(subMsgs.size()-1);
		lastMsg.setAuthor(author);
		lastMsg.setDate(date);
	}


	/** no caching */
	public Vector getSubMessages() {

		String content;

		try {
			if (board == null)
				Logger.error(this, "No ref to the corresponding board ?!");
			else if (board.getFactory() == null)
				Logger.error(this, "Can't access the board factory ?!");
			
			Hsqldb db = board.getFactory().getDb();

			synchronized(db.dbLock) {

				PreparedStatement st;

				st = db.getConnection().prepareStatement("SELECT content "+
									 "FROM frostKSKMessages "+
									 "WHERE id = ? "+
									 "LIMIT 1");
				st.setInt(1, id);

				ResultSet set = st.executeQuery();

				if (!set.next()) {
					st.close();
					return null;
				}

				content = set.getString("content");
				
				st.close();
			}
		} catch(SQLException e) {
			Logger.error(this, "Error while getting the messages : "+e.toString());
			return null;
		}

		boolean parsed = true;
		Vector v = null;

		try {
			v = parseMessage(content);

			if (v == null || (v.size()) <= 0) {
				parsed = false;
			} else {
				setAuthorAndDate(v, ((KSKAuthor)getSender()), getDate());
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


	public static boolean destroyAll(KSKBoard board, Hsqldb db) {
		if (!KSKFileAttachment.destroyAll(board, db)
		    || !KSKBoardAttachment.destroyAll(board, db))
			return false;

		try {
			synchronized(db.dbLock) {
				PreparedStatement st;

				/* to avoid the problems with the constraints */
				st = db.getConnection().prepareStatement("UPDATE frostKSKMessages SET "+
									 "inReplyTo = NULL");
				st.execute();
				st.close();

				st = db.getConnection().prepareStatement("DELETE FROM frostKSKMessages "+
									 "WHERE boardId = ?");
				st.setInt(1, board.getId());
				st.execute();
				st.close();
			}
		} catch(SQLException e) {
			Logger.error(null, "Can't destroy the board messages because : "+e.toString());
			return false;
		}

		return true;
	}


	public boolean destroy(Hsqldb db) {

		if (!KSKFileAttachment.destroy(this, db)
		    || !KSKBoardAttachment.destroy(this, db))
			return false;

		try {
			synchronized(db.dbLock) {
				PreparedStatement st;

				/* to avoid the integrity constraint violations */
				st = db.getConnection().prepareStatement("UPDATE frostKSKMessages SET "+
									 "inReplyTo = NULL WHERE inReplyTo = ?");
				st.setInt(1, id);
				st.execute();
				st.close();

				st = db.getConnection().prepareStatement("DELETE FROM frostKSKMessages "+
									 "WHERE id = ?");
				st.setInt(1, id);
				st.execute();
				st.close();
			}
		} catch(SQLException e) {
			Logger.error(null, "Can't destroy the message "+Integer.toString(id)+" because : "+e.toString());
			return false;
		}

		return true;
	}
}
