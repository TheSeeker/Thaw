package thaw.plugins.miniFrost.frostKSK;

import java.util.Vector;

import java.util.Observer;
import java.util.Observable;

import java.sql.*;

import java.util.Date;
import java.util.Calendar;

import thaw.core.Logger;
import thaw.core.I18n;
import thaw.core.ThawThread;
import thaw.core.ThawRunnable;
import thaw.plugins.Hsqldb;

import thaw.plugins.signatures.Identity;

import thaw.plugins.miniFrost.interfaces.Board;
import thaw.plugins.miniFrost.interfaces.Message;
import thaw.plugins.miniFrost.interfaces.Draft;


public class KSKBoard
	extends Observable
	implements Board, ThawRunnable, Observer {

	public final static int MAX_DOWNLOADS_AT_THE_SAME_TIME = 5;
	public final static int MAX_FAILURES_IN_A_ROW          = 5;

	public final static int DAYS_BEFORE_THE_LAST_REFRESH   = 1;
	public final static int MIN_DAYS_IN_THE_PAST           = 5;
	public final static int MAX_DAYS_IN_THE_PAST           = 5;

	public final static int MIN_DAYS_IN_THE_FUTURE         = 1;
	public final static int MAX_DAYS_IN_THE_FUTURE         = 1; /* not really used */


	private int id;
	private String name;

	/**
	 * last successful & finished one
	 */
	private Date lastUpdate;

	private KSKBoardFactory factory;

	private boolean refreshing;

	private KSKBoard() {

	}


	public KSKBoard(KSKBoardFactory factory,
			int id, String name,
			Date lastUpdate) {
		this.id = id;
		this.name = name;
		this.factory = factory;
		this.lastUpdate = lastUpdate;

		refreshing = false;
	}



	public Vector getMessages(String[] keywords,
				  int orderBy,
				  boolean desc,
				  boolean archived,
				  boolean read,
				  boolean unsigned,
				  int minTrustLevel) {
		return getMessages(id, factory, this, keywords,
				   orderBy, desc, archived, read, unsigned,
				   minTrustLevel, false);
	}


	protected static Vector getMessages(int id,
					    KSKBoardFactory factory,
					    KSKBoard board,
					    String[] keywords,
					    int orderBy,
					    boolean desc,
					    boolean archived,
					    boolean read,
					    boolean unsigned,
					    int minTrustLevel,
					    boolean allBoards) {

		String orderColumn;

		if (orderBy == Board.ORDER_SUBJECT)
			orderColumn = "LOWER(frostKSKMessages.subject)";
		else if (orderBy == Board.ORDER_SENDER)
			orderColumn = "LOWER(frostKSKMessages.nick)";
		else
			orderColumn = "frostKSKMessages.date";

		if (desc)
			orderColumn += " DESC";

		String whereBase = "WHERE true AND ";

		if (!allBoards) {
			whereBase = "WHERE frostKSKMessages.boardId = ? AND ";
		}


		String archivedStr = " true ";

		if (!archived)
			archivedStr = "frostKSKMessages.archived = FALSE ";

		String readStr = "";

		if (!read)
			readStr = " AND frostKSKMessages.read = FALSE ";

		String keywordsStr = "";

		if (keywords != null) {
			for (int i = 0 ; i < keywords.length ; i++) {
				keywordsStr += " AND (LOWER(frostKSKMessages.subject) LIKE ? "+
					"  OR LOWER(frostKSKMessages.content) LIKE ? "+
					"  OR LOWER(frostKSKMessages.nick) LIKE ?)";
			}
		}


		String trustLvlStr;

		if (unsigned)
			trustLvlStr = " AND (signatures.trustLevel IS NULL "+
				"  OR signatures.trustLevel >= "+Integer.toString(minTrustLevel)+") ";
		else
			trustLvlStr = " AND signatures.trustLevel >= "+Integer.toString(minTrustLevel)+" ";


		Vector v = new Vector();

		try {
			Hsqldb db = factory.getDb();

			synchronized(db.dbLock) {
				PreparedStatement st;

				String query = "SELECT frostKSKMessages.id, "+
					"       frostKSKMessages.msgId, "+
					"       frostKSKMessages.inReplyToId, "+
					"       frostKSKMessages.subject, "+
					"       frostKSKMessages.nick, "+
					"       frostKSKMessages.sigId, "+
					"       frostKSKMessages.date, "+
					"       frostKSKMessages.rev, "+
					"       frostKSKMessages.read, "+
					"       frostKSKMessages.archived, "+
					"       frostKSKMessages.boardId, "+
					"       frostKSKMessages.encryptedFor, "+
					"       signatures.nickName, "+
					"       signatures.publicKey, "+
					"       signatures.privateKey, "+
					"       signatures.isDup, "+
					"       signatures.trustLevel "+
					"FROM frostKSKMessages LEFT OUTER JOIN signatures "+
					" ON frostKSKMessages.sigId = signatures.id "+
					whereBase+
					archivedStr+
					readStr+
					keywordsStr+
					trustLvlStr+
					"ORDER BY "+orderColumn;


				st = db.getConnection().prepareStatement(query);

				int i = 1;

				if (!allBoards)
					st.setInt(i++, id);

				if (keywords != null) {
					for (int j = 0 ; j < keywords.length ; j++) {
						String word = keywords[j].toLowerCase();

						st.setString(i++, "%"+word+"%");
						st.setString(i++, "%"+word+"%");
						st.setString(i++, "%"+word+"%");
					}
				}

				ResultSet set = st.executeQuery();

				while(set.next()) {
					Identity encryptedFor = null;

					if (set.getInt("encryptedFor") > 0) {
						encryptedFor = Identity.getIdentity(db, set.getInt("encryptedFor"));
					}

					KSKBoard daBoard = ((board != null) ?
							    board :
							    factory.getBoard(set.getInt("boardId")));

					int sigId = set.getInt("sigId");
					String nick = set.getString("nickname");

					v.add(new KSKMessage(set.getInt("id"),
							     set.getString("msgId"),
							     set.getString("inReplyToId"),
							     set.getString("subject"),
							     set.getString("nick"),
							     sigId,
							     (nick != null ?
							      new Identity(db, sigId,
									   nick,
									   set.getString("publicKey"),
									   set.getString("privateKey"),
									   set.getBoolean("isDup"),
									   set.getInt("trustLevel"))
							      : null),
							     set.getTimestamp("date"),
							     set.getInt("rev"),
							     set.getBoolean("read"),
							     set.getBoolean("archived"),
							     encryptedFor,
							     daBoard));
				}
			}

		} catch(SQLException e) {
			Logger.error(new KSKBoard(), "Can't get message list because : "+e.toString());
		}

		return v;
	}


	public Message getNextUnreadMessage(boolean unsigned,
					    boolean archived,
					    int minTrustLevel) {

		String trustLvlStr;

		if (unsigned)
			trustLvlStr = " AND (signatures.trustLevel IS NULL "+
				"  OR signatures.trustLevel >= "+Integer.toString(minTrustLevel)+") ";
		else
			trustLvlStr = " AND signatures.trustLevel >= "+Integer.toString(minTrustLevel)+" ";

		String archivedStr = "";

		if (!archived)
			archivedStr = " AND frostKSKMessages.archived = FALSE ";


		try {
			Hsqldb db = factory.getDb();

			synchronized(db.dbLock) {
				PreparedStatement st;

				String query = "SELECT frostKSKMessages.id AS id, "+
					"       frostKSKMessages.msgId AS msgId, "+
					"       frostKSKMessages.inReplyToId AS inReplyToId, "+
					"       frostKSKMessages.subject AS subject, "+
					"       frostKSKMessages.nick AS nick, "+
					"       frostKSKMessages.sigId AS sigId, "+
					"       frostKSKMessages.date AS date, "+
					"       frostKSKMessages.rev AS rev, "+
					"       frostKSKMessages.encryptedFor AS encryptedFor, "+
					"       signatures.nickName AS sigNick, "+
					"       signatures.publicKey AS sigPublicKey, "+
					"       signatures.privateKey AS sigPrivateKey, "+
					"       signatures.isDup AS sigIsDup, "+
					"       signatures.trustLevel AS sigTrustLevel "+
					"FROM frostKSKMessages LEFT OUTER JOIN signatures "+
					" ON frostKSKMessages.sigId = signatures.id "+
					"WHERE frostKSKMessages.boardId = ? "+
					"AND frostKSKMessages.read = FALSE "+
					archivedStr+
					trustLvlStr+
					"ORDER BY frostKSKMessages.date LIMIT 1";

				st = db.getConnection().prepareStatement(query);
				st.setInt(1, id);

				ResultSet set = st.executeQuery();


				if (set.next()) {
					Identity encryptedFor = null;

					if (set.getInt("encryptedFor") > 0) {
						encryptedFor = Identity.getIdentity(db, set.getInt("encryptedFor"));
					}

					int sigId = set.getInt("sigId");

					return new KSKMessage(set.getInt("id"),
							      set.getString("msgId"),
							      set.getString("inReplyToId"),
							      set.getString("subject"),
							      set.getString("nick"),
							      sigId,
							      (sigId > 0 ?
							       new Identity(db, sigId,
									   set.getString("sigNick"),
									   set.getString("sigPublicKey"),
									   set.getString("sigPrivateKey"),
									   set.getBoolean("sigIsDup"),
									   set.getInt("sigTrustLevel"))
							       : null),
							      set.getTimestamp("date"),
							      set.getInt("rev"),
							      false, false,
							      encryptedFor,
							      this);
				}
			}

		} catch(SQLException e) {
			Logger.error(this, "Can't get the next unread message because : "+e.toString());
		}

		return null;
	}



	/* last started */
	private int lastRev;
	private Date lastDate;

	private int lastSuccessfulRev;

	private int maxDaysInThePast;


	/* we keep the failed one in this queue as long as no other succeed */
	/* sync() on it ! */
	private KSKMessage runningDownloads[] = new KSKMessage[MAX_DOWNLOADS_AT_THE_SAME_TIME];


	protected Date getCurrentlyRefreshedDate() {
		return lastDate;
	}

	/* for example KSK@frost|message|news|2007.7.21-boards-47.xml */
	public final static String KEY_HEADER = /* "KSK@" + */"frost|message|news|";

	/**
	 * called by KSKMessage.download();
	 */
	protected String getDownloadKey(Date date, int rev) {
		java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyy.M.d");

		StringBuffer keyBuf = new StringBuffer("KSK@"+KEY_HEADER);

		keyBuf = formatter.format(date, keyBuf, new java.text.FieldPosition(0));
		keyBuf.append("-"+getName()+"-");
		keyBuf.append(Integer.toString(rev));
		keyBuf.append(".xml");

		return keyBuf.toString();
	}

	protected int getKeyType() {
		return thaw.fcp.FCPClientPut.KEY_TYPE_KSK;
	}

	/**
	 * called by KSKDraft
	 */
	protected String getPrivateKey() {
		return null;
	}

	/**
	 * called by KSKDraft
	 */
	protected String getNameForInsertion(Date date, int rev) {
		java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyy.M.d");
		//formatter.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));

		StringBuffer keyBuf = new StringBuffer(KEY_HEADER);

		keyBuf = formatter.format(date, keyBuf, new java.text.FieldPosition(0));
		keyBuf.append("-"+getName()+"-");
		keyBuf.append(Integer.toString(rev));
		keyBuf.append(".xml");

		return keyBuf.toString();
	}


	protected static Date getMidnight(Date date) {
		Calendar cal = new java.util.GregorianCalendar();
		cal.setTime(date);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTime();
	}


	protected int getNextNonDownloadedRev(Date daDate, int rev) {
		daDate = getMidnight(daDate);

		java.sql.Date date = new java.sql.Date(daDate.getTime());


		try {
			Hsqldb db = factory.getDb();

			synchronized(db.dbLock) {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("SELECT rev FROM frostKSKMessages "+
									 "WHERE keyDate = ? "+
									 "AND rev >= ? AND boardId = ? ORDER by rev");
				st.setDate(1, date);
				st.setInt( 2, rev);
				st.setInt(3, id);

				ResultSet set = st.executeQuery();

				int lastRev = rev-1;

				while(set.next()) {
					int newRev = set.getInt("rev");

					if (newRev > lastRev+1) /* there is a hole */
						return lastRev+1;

					lastRev = newRev;
				}

				/* no hole found */
				return lastRev+1;
			}
		} catch(SQLException e) {
			Logger.error(this, "Can't get the next non-downloaded rev in the board because : "+e.toString());
		}

		return -1;
	}
	
	protected int getNextNonDownloadedAndValidRev(Date daDate, int rev) {
		int nextNonDownloaded;
		int nextValid = rev+1;

		do {
			nextNonDownloaded = getNextNonDownloadedRev(daDate, nextValid);
			nextValid = getNextValidSlot(daDate, nextNonDownloaded);
		} while(nextValid != nextNonDownloaded);
		
		return nextValid;
	}


	protected int getLastDownloadedRev(Date daDate) {
		daDate = getMidnight(daDate);

		java.sql.Date date = new java.sql.Date(daDate.getTime());


		try {
			Hsqldb db = factory.getDb();

			synchronized(db.dbLock) {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("SELECT rev FROM frostKSKMessages "+
									 "WHERE keyDate = ? "+
									 "AND boardId = ? "+
									 "ORDER by rev DESC "+
									 "LIMIT 1");
				st.setDate(1, date);
				st.setInt(2, id);

				ResultSet set = st.executeQuery();

				if (set.next())
					return set.getInt("rev");
			}
		} catch(SQLException e) {
			Logger.error(this, "Can't get the next non-downloaded rev in the board because : "+e.toString());
		}

		return 0;
	}


	/* synchronize your self on runningDownloads */
	/**
	 * @param initial true if one of the first downloads of the day
	 */
	protected void startNewMessageDownload(boolean initial) {
		if (!refreshing)
			return;

		int slot;

		/* we search an empty slot */
		for (slot = 0 ;
		     slot < MAX_DOWNLOADS_AT_THE_SAME_TIME;
		     slot++) {

			if (runningDownloads[slot] == null
			    || !runningDownloads[slot].isDownloading())
				break;

		}

		if (slot == MAX_DOWNLOADS_AT_THE_SAME_TIME) {
			Logger.notice(this, "No more empty slots ?!");
			return;
		}

		int rev = getNextNonDownloadedAndValidRev(lastDate, lastRev);

		Logger.debug(this, "Rev : "+Integer.toString(lastRev)+
			     " ; "+Integer.toString(rev)+" ; Date : "+lastDate.toString());

		runningDownloads[slot] = new KSKMessage(this, lastDate, rev);
		runningDownloads[slot].addObserver(this);
		runningDownloads[slot].download(factory.getCore().getQueueManager(),
						factory.getDb());

		if (lastRev < rev) {
			lastRev = rev;
		}

	}




	protected void notifyChange() {
		factory.getPlugin().getPanel().notifyChange(this);
		setChanged();
		notifyObservers();
	}

	protected void endOfRefresh() {
		synchronized(this) {
			Logger.info(this, "End of refresh");

			try {
				Hsqldb db = factory.getDb();

				synchronized(db.dbLock) {
					PreparedStatement st;

					st = db.getConnection().prepareStatement("UPDATE frostKSKBoards "+
										 "SET lastUpdate = ? "+
										 "WHERE id = ?");
					st.setDate(1, new java.sql.Date(new java.util.Date().getTime()));
					st.setInt(2, id);
					st.execute();
				}
			} catch(SQLException e) {
				Logger.error(this, "Unable to update the lastUpdate date :"+e.toString());
			}

			int newMsgs = getNewMessageNumber();

			if (newMsgs > 0) {
				String announce = I18n.getMessage("thaw.plugin.miniFrost.newMsgAnnounce");
				announce = announce.replaceAll("X", Integer.toString(newMsgs));
				announce = announce.replaceAll("Y", toString());

				thaw.plugins.TrayIcon.popMessage(factory.getCore().getPluginManager(),
								 "MiniFrost",
								 announce);
			}

			refreshing = false;
		}

		notifyChange();
	}


	private Date getNextRefreshDate(Date originalDate) {
		Date today = getMidnight(new Date());

		if (originalDate == null)
			return today;

		/* if the last date was in the future */
		if (getMidnight(originalDate).getTime() > today.getTime()) {
			/* TODO : Take into consideration that we could have
			 *        MIN_DAYS_IN_THE_FUTURE > 1
			 */
			/* we stop */
			return null;
		}


		Date newDate = new Date(originalDate.getTime() - 24*60*60*1000);
		Date maxInPast = new Date(new Date().getTime() - ((maxDaysInThePast+1) * 24*60*60*1000));
		Date lastUpdatePast = ((lastUpdate == null) ? null :
				       new Date(lastUpdate.getTime() - (DAYS_BEFORE_THE_LAST_REFRESH * 24*60*60*1000)));

		if (newDate.getTime() >= maxInPast.getTime()
		    && (lastUpdatePast == null || newDate.getTime() >= lastUpdatePast.getTime())) {
			/* date in the limits */
			return getMidnight(newDate);
		} else {
			/* no more in the limits => we do tomorrow and then we stop */
			return getMidnight(new Date( (today.getTime()) + 24*60*60*1000));
		}
	}


	/**
	 * only called when a message has finished its download
	 */
	public void update(Observable o, Object param) {
		synchronized(runningDownloads) {
			KSKMessage msg = (KSKMessage)o;

			boolean successful = !msg.isDownloading()
				&& msg.isSuccessful();

			if (successful) {
				//if (msg.isParsable() && !msg.isRead())
				//	newMsgs++;

				if (msg.getRev() > lastSuccessfulRev)
					lastSuccessfulRev = msg.getRev();

				int toRestart = 0;

				/* we restart all the failed ones */

				for (int i = 0;
				     i < MAX_DOWNLOADS_AT_THE_SAME_TIME;
				     i++) {
					if (runningDownloads[i] == null
					    || !runningDownloads[i].isDownloading()) {

						toRestart++;
					}
				}

				Logger.info(this, "One successful => Restarting "+Integer.toString(toRestart)+" transfers");

				for (int i = 0 ; i < toRestart ; i++)
					startNewMessageDownload(false);

			} else {

				/* if not successful, we look if all the other failed */
				/* we look first if we can restart some of the failed transfers
				 * up to lastSuccessfulRev + MAX_FAILURES_IN_A_ROW */

				boolean moveDay = true;

				int lastLoadedRev = -1;
				int nmbFailed = 0;

				for (int i = 0;
				     i < MAX_DOWNLOADS_AT_THE_SAME_TIME;
				     i++) {
					if (runningDownloads[i] != null
					    && runningDownloads[i].getRev() > lastLoadedRev)
						lastLoadedRev = runningDownloads[i].getRev();
				}

				for (int i = 0;
				     i < MAX_DOWNLOADS_AT_THE_SAME_TIME;
				     i++) {
					if (runningDownloads[i] == null
					    || !runningDownloads[i].isDownloading()) {

						nmbFailed++;

					}
				}

				if (nmbFailed > MAX_FAILURES_IN_A_ROW)
					nmbFailed = MAX_FAILURES_IN_A_ROW;

				Logger.info(this, "One failed");

				/* we can't restart more than the number of failed one */
				/* and we can't go upper than lastSuccessfulRev + MAX_FAILURES
				 * (hm, in fact, startNewMessageDownload() can go upper ... rah fuck) */
				for (int i = 0 ;
				     i < nmbFailed
					     && (lastLoadedRev+1+i) < (lastSuccessfulRev + MAX_FAILURES_IN_A_ROW);
				     i++) {
					Logger.info(this, "Continuing progression ...");
					startNewMessageDownload(false);
					moveDay = false;
				}


				if (!moveDay)
					return;


				/* if every transfer has failed, we move to another day */
				for (int i = 0 ;
				     i < MAX_DOWNLOADS_AT_THE_SAME_TIME;
				     i++) {
					if (runningDownloads[i] != null
					    && (runningDownloads[i].isDownloading()
						|| runningDownloads[i].isSuccessful())) {
						moveDay = false;
					}
				}

				if (moveDay) {
					Logger.info(this, "no more message to fetch for this day => moving to another");

					lastDate = getNextRefreshDate(lastDate);
					lastRev = -1;

					if (lastDate != null) {
						lastSuccessfulRev = getLastDownloadedRev(lastDate);

						/* we start again */

						for (int i = 0;
						     i < MAX_DOWNLOADS_AT_THE_SAME_TIME;
						     i++) {
							startNewMessageDownload(true);
						}

					} else {
						endOfRefresh();
					}
				}

			}

		}

		/* we notify a change anyway because of KSKDraft */
		notifyChange();

	}

	public void refresh() {
		refresh(MAX_DAYS_IN_THE_PAST);
	}

	public void refresh(int maxDaysInThePast) {
		if (refreshing) {
			Logger.notice(this, "Already refreshing");
			return;
		}

		this.maxDaysInThePast = maxDaysInThePast;

		synchronized(this) {
			lastDate = getNextRefreshDate(null);
			lastRev = -1;
			refreshing = true;
		}

		notifyChange();

		Thread th = new ThawThread(this, "Board refreshment", this);
		th.start();
	}

	public void run() {

		//lastDate = new Date((new Date()).getTime()
		//		    + (MIN_DAYS_IN_THE_FUTURE * (24 * 60 * 60 * 1000 /* 1 day */)));

		synchronized(runningDownloads) {
			lastSuccessfulRev = getLastDownloadedRev(lastDate);

			for (int i = 0 ; i < MAX_DOWNLOADS_AT_THE_SAME_TIME ; i++) {
				runningDownloads[i] = null;
			}

			for (int i = 0 ; i < MAX_DOWNLOADS_AT_THE_SAME_TIME ; i++) {
				startNewMessageDownload(true);
			}
		}

	}

	public void stop() {
		/* startNewMessageDownload() won't start any new message download
		 * if this variable is set to false
		 */
		refreshing = false;
	}


	public boolean isRefreshing() {
		return refreshing;
	}


        protected static int countNewMessages(Hsqldb db, int boardId, String boardName,
					      boolean unsigned, boolean archived, int minTrustLevel) {
		int count = -1;

		String archivedStr = "";

		if (!archived)
			archivedStr = " AND frostKSKMessages.archived = FALSE";

		String unsignedStr = " AND (frostKSKMessages.sigId IS NULL OR signatures.trustLevel >= ?)";

		if (!unsigned)
			unsignedStr = " AND frostKSKMessages.sigId IS NOT NULL AND signatures.trustLevel >= ?";

		String query = "SELECT count(frostKSKMessages.id) "+
		    "FROM frostKSKMessages LEFT JOIN signatures "+
		    " ON frostKSKMessages.sigId = signatures.id "+
		    "WHERE frostKSKMessages.boardId = ? "+
		    "AND frostKSKMessages.read = FALSE"+
		    archivedStr+
		    unsignedStr;

		try {
			PreparedStatement subSt;

			subSt = db.getConnection().prepareStatement(query);
			subSt.setInt(1, boardId);
			subSt.setInt(2, minTrustLevel);

			ResultSet subRes = subSt.executeQuery();

			if (subRes.next())
				count = subRes.getInt(1);

		} catch(SQLException e) {
			Logger.error(db, "Can't count the number of new message on the board "+
				     "'"+boardName+"'because : "+e.toString());
			Logger.error(db, "The query was: "+query);
		}

		return count;
	}


	boolean lastUnsignedSetting;
	boolean	lastArchivedSetting;
	int lastMinTrustLevelSetting;

	/**
	 * just for the announce through the trayicon;
	 */
	private int getNewMessageNumber() {
		return getNewMessageNumber(lastUnsignedSetting,
					   lastArchivedSetting,
					   lastMinTrustLevelSetting);
	}


	public int getNewMessageNumber(boolean unsigned, boolean archived, int minTrustLevel) {
		this.lastUnsignedSetting = unsigned;
		this.lastArchivedSetting = archived;
		this.lastMinTrustLevelSetting = minTrustLevel;

		return countNewMessages(factory.getDb(), id, name,
					unsigned, archived, minTrustLevel);
	}


	public boolean destroy() {
		refreshing = false;
		Hsqldb db = factory.getDb();

		if (!KSKMessage.destroyAll(this, db))
			return false;

		try {
			synchronized(db.dbLock) {
				PreparedStatement st;
				
				st = db.getConnection().prepareStatement("DELETE FROM frostKSKInvalidSlots WHERE boardId = ?");
				st.setInt(1, id);
				st.execute();

				st = db.getConnection().prepareStatement("DELETE FROM frostKSKBoards "+
									 "WHERE id = ?");
				st.setInt(1, id);
				st.execute();
			}
		} catch(SQLException e) {
			Logger.error(this, "Can't destroy the board because : "+e.toString());
			return false;
		}

		return true;
	}

	public int getId() {
		return id;
	}


	/**
	 * @return the board name, as it
	 */
	public String getName() {
		return name;
	}

	/**
	 * Return the board name,
	 * with maybe some informations
	 */
	public String toString() {
		return name;
	}


	public KSKBoardFactory getFactory() {
		return factory;
	}


	public Draft getDraft(Message inReplyTo) {
		return new KSKDraft(this, (KSKMessage)inReplyTo);
	}

	public int compareTo(Object o) {
		return toString().compareToIgnoreCase(o.toString());
	}

	public boolean equals(Object o) {
		if (!(o instanceof KSKBoard))
			return false;
		return ( ((KSKBoard)o).getId() == getId() );
	}
	
	
	public void addInvalidSlot(Date date, int rev) {
		date = getMidnight(date);
		Hsqldb db = factory.getDb();

		synchronized(db.dbLock) {
			PreparedStatement st;
			
			try {
				st = db.getConnection().prepareStatement("SELECT id, minRev, maxRev "+
							"FROM frostKSKInvalidSlots "+
							"WHERE boardId = ? "+
							"AND date = ? "+
							"AND (maxRev = ? "+
							"OR minRev = ?) LIMIT 1");
				st.setInt(1, id);
				st.setDate(2, new java.sql.Date(date.getTime()));
				st.setInt(3, rev-1);
				st.setInt(4, rev+1);
				
				ResultSet set = st.executeQuery();
				
				if (!set.next()) {
					/* no existing interval near our rev */
					/* => we create one */
					
					st = db.getConnection().prepareStatement("INSERT INTO frostKSKInvalidSlots (boardId, date, minRev, maxRev) "+
							"VALUES (?, ?, ?, ?)");
					st.setInt(1, id);
					st.setDate(2, new java.sql.Date(date.getTime()));
					st.setInt(3, rev);
					st.setInt(4, rev);
					st.execute();
					
				} else {
					/* an interval near this one already exist */
					/* => we adjust */
					
					int intervalId = set.getInt("id");
					int intervalMinRev = set.getInt("minRev");
					int intervalMaxRev = set.getInt("maxRev");
					
					if (intervalMaxRev == (rev-1)) {
						st = db.getConnection().prepareStatement("UPDATE frostKSKInvalidSlots SET maxRev = ? "+
								"WHERE id = ?");
						st.setInt(1, rev);
						st.setInt(2, intervalId);
						st.execute();
					} else if (intervalMinRev == (rev+1)) {
						st = db.getConnection().prepareStatement("UPDATE frostKSKInvalidSlots SET minRev = ? "+
							"WHERE id = ?");
						st.setInt(1, rev);
						st.setInt(2, intervalId);
						st.execute();
					} else {
						Logger.error(this, "Unmanaged case !");
					}
				}
				
			} catch(SQLException e) {
				Logger.error(this, "Error while adding invalid slot to the database : "+e.toString());
			}
		}
	}
	
	public int getNextValidSlot(Date date, int rev) {
		date = getMidnight(date);
		Hsqldb db = factory.getDb();

		synchronized(db.dbLock) {
			PreparedStatement st;
			
			try {
				st = db.getConnection().prepareStatement("SELECT id, minRev, maxRev "+
						"FROM frostKSKInvalidSlots "+
						"WHERE boardId = ? "+
						"AND date = ? "+
						"AND (maxRev >= ? "+
						"AND minRev <= ?) LIMIT 1");

				while(true) {
					st.setInt(1, id);
					st.setDate(2, new java.sql.Date(date.getTime()));
					st.setInt(3, rev);
					st.setInt(4, rev);
				
					ResultSet set = st.executeQuery();
				
					if (!set.next()) {
						return rev;
					}
					rev = set.getInt("maxRev")+1;
				}

			} catch(SQLException e) {
				Logger.error(this, "getNextValidSlot(): "+e.toString());
			}
		}
		
		return rev;
	}
}
