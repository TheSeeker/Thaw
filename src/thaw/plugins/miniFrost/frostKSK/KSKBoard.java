package thaw.plugins.miniFrost.frostKSK;

import java.util.Vector;

import java.util.Observer;
import java.util.Observable;

import java.sql.*;

import java.util.Date;
import java.util.Calendar;

import thaw.core.Logger;
import thaw.plugins.Hsqldb;

import thaw.plugins.miniFrost.interfaces.Board;

public class KSKBoard
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
				  boolean archived) {
		return getMessages(id, factory, this, keywords, orderBy, desc, archived, false);
	}


	protected static Vector getMessages(int id,
					    KSKBoardFactory factory,
					    KSKBoard board,
					    String[] keywords,
					    int orderBy,
					    boolean desc,
					    boolean archived,
					    boolean allBoards) {

		String orderColumn;

		if (orderBy == Board.ORDER_SUBJECT)
			orderColumn = "LOWER(subject)";
		else if (orderBy == Board.ORDER_SENDER)
			orderColumn = "LOWER(nick)";
		else
			orderColumn = "date";

		if (desc)
			orderColumn += " DESC";

		String whereBase = "WHERE true AND ";

		if (!allBoards) {
			whereBase = "WHERE boardId = ? AND ";
		}


		String archivedStr = " true ";

		if (!archived)
			archivedStr = "archived = FALSE ";


		String keywordsStr = "";

		if (keywords != null) {
			for (int i = 0 ; i < keywords.length ; i++) {
				keywordsStr += " AND (LOWER(subject) LIKE ? "+
					"  OR LOWER(content) LIKE ? "+
					"  OR LOWER(nick) LIKE ?)";
			}
		}

		Vector v = new Vector();

		try {
			Hsqldb db = factory.getDb();

			synchronized(db.dbLock) {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("SELECT id, "+
									 "       subject, "+
									 "       nick, "+
									 "       sigId, "+
									 "       date, "+
									 "       rev, "+
									 "       read, "+
									 "       archived, "+
									 "       boardId "+
									 "FROM frostKSKMessages "+
									 whereBase+
									 archivedStr+
									 keywordsStr+
									 "ORDER BY "+orderColumn);
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

					v.add(new KSKMessage(set.getInt("id"),
							     set.getString("subject"),
							     set.getString("nick"),
							     set.getInt("sigId"),
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


	public thaw.plugins.miniFrost.interfaces.Message getNextUnreadMessage() {
		try {
			Hsqldb db = factory.getDb();

			synchronized(db.dbLock) {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("SELECT id, subject, nick, "+
									 "       sigId, date, rev "+
									 "FROM frostKSKMessages "+
									 "WHERE boardId = ? AND "+
									 "archived = FALSE AND read = FALSE "+
									 "ORDER BY date LIMIT 1");
				st.setInt(1, id);

				ResultSet set = st.executeQuery();

				if (set.next()) {
					return new KSKMessage(set.getInt("id"),
							      set.getString("subject"),
							      set.getString("nick"),
							      set.getInt("sigId"),
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

	/* we keep the failed one in this queue as long as no other succeed */
	/* sync() on it ! */
	private KSKMessage runningDownloads[] = new KSKMessage[MAX_DOWNLOADS_AT_THE_SAME_TIME];


	protected Date getMidnight(Date date) {
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
				&& msg.isSuccessful()
				&& msg.isParsable();

			if (successful) {
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
			}


			if (!successful) { /* if not successful, we look if all the other failed */
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

					Date maxInPast = new Date(new Date().getTime() - (MAX_DAYS_IN_THE_PAST * 24*60*60*1000));
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
		if (refreshing) {
			Logger.warning(this, "Already refreshing");
			return;
		}

		refreshing = true;

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
}
