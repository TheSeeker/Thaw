package thaw.plugins.miniFrost.frostKSK;

import java.util.Vector;

import java.util.Observer;
import java.util.Observable;

import java.sql.*;

import java.util.Date;
import java.util.Calendar;

import thaw.core.Logger;
import thaw.core.I18n;
import thaw.plugins.Hsqldb;

import thaw.plugins.signatures.Identity;

import thaw.plugins.miniFrost.interfaces.Board;
import thaw.plugins.miniFrost.interfaces.Message;
import thaw.plugins.miniFrost.interfaces.Draft;


public class KSKBoard
	extends Observable
	implements Board, Runnable, Observer {

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

	private int newMsgs;

	private KSKBoard() {

	}


	public KSKBoard(KSKBoardFactory factory,
			int id, String name,
			Date lastUpdate,
			int newMessages) {
		this.id = id;
		this.name = name;
		this.factory = factory;
		this.lastUpdate = lastUpdate;

		newMsgs = newMessages;

		refreshing = false;
	}


	public Vector getMessages(String[] keywords,
				  int orderBy,
				  boolean desc,
				  boolean archived,
				  boolean unsigned,
				  int minTrustLevel) {
		return getMessages(id, factory, this, keywords,
				   orderBy, desc, archived, unsigned,
				   minTrustLevel, false);
	}


	protected static Vector getMessages(int id,
					    KSKBoardFactory factory,
					    KSKBoard board,
					    String[] keywords,
					    int orderBy,
					    boolean desc,
					    boolean archived,
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
					"       frostKSKMessages.subject, "+
					"       frostKSKMessages.nick, "+
					"       frostKSKMessages.sigId, "+
					"       frostKSKMessages.date, "+
					"       frostKSKMessages.rev, "+
					"       frostKSKMessages.read, "+
					"       frostKSKMessages.archived, "+
					"       frostKSKMessages.boardId, "+
					"       signatures.nickName, "+
					"       signatures.publicKey, "+
					"       signatures.privateKey, "+
					"       signatures.isDup, "+
					"       signatures.trustLevel "+
					"FROM frostKSKMessages LEFT OUTER JOIN signatures "+
					" ON frostKSKMessages.sigId = signatures.id "+
					whereBase+
					archivedStr+
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
					KSKBoard daBoard = ((board != null) ?
							    board :
							    factory.getBoard(set.getInt("boardId")));

					int sigId = set.getInt("sigId");
					String nick = set.getString("nickname");

					v.add(new KSKMessage(set.getInt("id"),
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
							     daBoard));
				}
			}

		} catch(SQLException e) {
			Logger.error(new KSKBoard(), "Can't get message list because : "+e.toString());
		}

		return v;
	}


	public Message getNextUnreadMessage(boolean unsigned,
					    int minTrustLevel) {

		String trustLvlStr;

		if (unsigned)
			trustLvlStr = " AND (signatures.trustLevel IS NULL "+
				"  OR signatures.trustLevel >= "+Integer.toString(minTrustLevel)+") ";
		else
			trustLvlStr = " AND signatures.trustLevel >= "+Integer.toString(minTrustLevel)+" ";


		try {
			Hsqldb db = factory.getDb();

			synchronized(db.dbLock) {
				PreparedStatement st;

				String query = "SELECT frostKSKMessages.id AS id, "+
					"       frostKSKMessages.subject AS subject, "+
					"       frostKSKMessages.nick AS nick, "+
					"       frostKSKMessages.sigId AS sigId, "+
					"       frostKSKMessages.date AS date, "+
					"       frostKSKMessages.rev AS rev, "+
					"       signatures.nickName AS sigNick, "+
					"       signatures.publicKey AS sigPublicKey, "+
					"       signatures.privateKey AS sigPrivateKey, "+
					"       signatures.isDup AS sigIsDup, "+
					"       signatures.trustLevel AS sigTrustLevel "+
					"FROM frostKSKMessages LEFT OUTER JOIN signatures "+
					" ON frostKSKMessages.sigId = signatures.id "+
					"WHERE frostKSKMessages.boardId = ? AND "+
					"frostKSKMessages.archived = FALSE AND frostKSKMessages.read = FALSE "+
					trustLvlStr+
					"ORDER BY frostKSKMessages.date LIMIT 1";

				st = db.getConnection().prepareStatement(query);
				st.setInt(1, id);

				ResultSet set = st.executeQuery();

				if (set.next()) {
					int sigId = set.getInt("sigId");

					return new KSKMessage(set.getInt("id"),
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
							      this);
				}
			}

		} catch(SQLException e) {
			Logger.error(this, "Can't get message list because : "+e.toString());
		}

		return null;
	}



	/* last started */
	private int lastRev;
	private Date lastDate;

	private int lastSuccessfulRev;
	private int failed;

	private int maxDaysInThePast;


	/* we keep the failed one in this queue as long as no other succeed */
	/* sync() on it ! */
	private KSKMessage runningDownloads[] = new KSKMessage[MAX_DOWNLOADS_AT_THE_SAME_TIME];


	protected Date getCurrentlyRefreshedDate() {
		return lastDate;
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

		java.sql.Timestamp date = new java.sql.Timestamp(daDate.getTime());


		try {
			Hsqldb db = factory.getDb();

			synchronized(db.dbLock) {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("SELECT rev FROM frostKSKMessages "+
									 "WHERE date >= ? AND date < ? "+
									 "AND rev > ? AND boardId = ? ORDER by rev");
				st.setTimestamp(1, date);
				st.setTimestamp(2, new java.sql.Timestamp(date.getTime() + 24*60*60*1000));
				st.setInt( 3, rev);
				st.setInt(4, id);

				ResultSet set = st.executeQuery();

				int lastRev = rev;

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


	/* synchronize your self on runningDownloads */
	protected void startNewMessageDownload() {
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

		int rev = getNextNonDownloadedRev(lastDate, lastRev);

		Logger.debug(this, "Rev : "+Integer.toString(lastRev)+
			     " ; "+Integer.toString(rev)+" ; Date : "+lastDate.toString());

		runningDownloads[slot] = new KSKMessage(this, lastDate, rev);
		runningDownloads[slot].addObserver(this);
		runningDownloads[slot].download(factory.getCore().getQueueManager(),
						factory.getDb());
		if (lastRev < rev)
			lastRev = rev;

	}

	protected void notifyChange() {
		factory.getPlugin().getPanel().notifyChange(this);
		setChanged();
		notifyObservers();
	}

	protected void endOfRefresh() {
		Logger.info(this, "End of refresh");

		try {
			Hsqldb db = factory.getDb();

			synchronized(db.dbLock) {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("UPDATE frostKSKBoards "+
									 "SET lastUpdate = ? "+
									 "WHERE id = ?");
				st.setTimestamp(1, new java.sql.Timestamp(new java.util.Date().getTime()));
				st.setInt(2, id);
				st.execute();
			}
		} catch(SQLException e) {
			Logger.error(this, "Unable to update the lastUpdate date :"+e.toString());
		}

		if (newMsgs > 0) {
			String announce = I18n.getMessage("thaw.plugin.miniFrost.newMsgAnnounce");
			announce = announce.replaceAll("X", Integer.toString(newMsgs));
			announce = announce.replaceAll("Y", toString());

			thaw.plugins.TrayIcon.popMessage(factory.getCore().getPluginManager(),
							 "MiniFrost",
							 announce);
		}

		refreshing = false;
		notifyChange();
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
				if (msg.isParsable())
					newMsgs++;

				notifyChange();
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
					startNewMessageDownload();

				return;
			} else {
				/* we notify a change anyway because of KSKDraft */
				notifyChange();

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
					startNewMessageDownload();
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

					lastDate = new Date(lastDate.getTime() - 24*60*60*1000);
					lastRev = -1;
					lastSuccessfulRev = 0;

					Date maxInPast = new Date(new Date().getTime() - ((maxDaysInThePast+1) * 24*60*60*1000));
					Date lastUpdatePast = ((lastUpdate == null) ? null :
							       new Date(lastUpdate.getTime() - (DAYS_BEFORE_THE_LAST_REFRESH * 24*60*60*1000)));

					if (lastDate.getTime() >= maxInPast.getTime()
					    && (lastUpdatePast == null || lastDate.getTime() >= lastUpdatePast.getTime())) {
						/* the date is in the limits */

						/* we start again */

						for (int i = 0;
						     i < MAX_DOWNLOADS_AT_THE_SAME_TIME;
						     i++) {
							startNewMessageDownload();
						}

					} else {
						/* the date is out of limits */
						endOfRefresh();
					}
				}

			}

		}

	}

	public void refresh() {
		refresh(MAX_DAYS_IN_THE_PAST);
	}

	public void refresh(int maxDaysInThePast) {
		if (refreshing) {
			Logger.warning(this, "Already refreshing");
			return;
		}

		this.maxDaysInThePast = maxDaysInThePast;

		refreshing = true;

		notifyChange();

		Thread th = new Thread(this);
		th.start();
	}

	public void run() {

		lastRev = -1;
		lastSuccessfulRev = 0;

		lastDate = new Date((new Date()).getTime()
				    + (MIN_DAYS_IN_THE_FUTURE * (24 * 60 * 60 * 1000 /* 1 day */)));

		synchronized(runningDownloads) {
			for (int i = 0 ; i < MAX_DOWNLOADS_AT_THE_SAME_TIME ; i++) {
				runningDownloads[i] = null;
			}

			for (int i = 0 ; i < MAX_DOWNLOADS_AT_THE_SAME_TIME ; i++) {
				startNewMessageDownload();
			}
		}

	}


	public boolean isRefreshing() {
		return refreshing;
	}

	public int getNewMessageNumber() {
		return newMsgs;
	}

	protected void setNewMessageNumber(int nmb) {
		this.newMsgs = nmb;
	}


	public void destroy() {
		Hsqldb db = factory.getDb();

		try {
			synchronized(db.dbLock) {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("DELETE FROM frostKSKBoards "+
									 "WHERE id = ?");
				st.setInt(1, id);
				st.execute();
			}
		} catch(SQLException e) {
			Logger.error(this, "Can't destroy the board because : "+e.toString());
		}
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	/**
	 * Always return the board name,
	 * without anything more
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
}
