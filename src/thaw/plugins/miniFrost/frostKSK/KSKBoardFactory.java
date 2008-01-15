package thaw.plugins.miniFrost.frostKSK;

import java.util.Vector;
import javax.swing.JOptionPane;

import java.sql.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Date;

import thaw.core.Core;
import thaw.core.Logger;
import thaw.core.I18n;

import thaw.plugins.Hsqldb;
import thaw.plugins.MiniFrost;
import thaw.plugins.signatures.Identity;


public class KSKBoardFactory
	implements thaw.plugins.miniFrost.interfaces.BoardFactory {

	/* must correspond at the position in the array of boardfactory in miniFrost */
	public final static int BOARD_FACTORY_ID = 0;

	public final static String[] DEFAULT_BOARDS = new String[] {
		"freenet",
		"freenet.0.7.bugs",
		"freenet-refs",
		"thaw",
		"frost",
		"jsite",
		"successful",
		"unsuccessful",
		"Thaw-indexes",
		"de.freenet",
		"fr.accueil",
		"fr.boards",
		"fr.discussion",
		"fr.freenet",
		"fr.freenet.freesites",
		"boards",
		"public",
		"sites",
		"test",
		"privacy"
	};


	private Hsqldb db;
	private Core core;
	private MiniFrost plugin;

	private HashMap boardsHashMap;
	private Vector boards;

	public KSKBoardFactory() {

	}


	public boolean init(Hsqldb db, Core core, MiniFrost plugin) {
		return init(db, core, plugin, "frostKSKDatabaseVersion");
	}

	public boolean init(Hsqldb db, Core core, MiniFrost plugin, String configOption) {
		this.db = db;
		this.core = core;
		this.plugin = plugin;

		boolean firstStart = (core.getConfig().getValue(configOption) == null);

		convertExistingTables();

		createTables();

		if (firstStart) {
			addDefaultBoards();

			if (core.getConfig().getValue(configOption) == null)
				core.getConfig().setValue(configOption, "true");
		}

		boardsHashMap = new HashMap();

		return true;
	}


	public boolean cleanUp(int archiveAfter, int deleteAfter) {
		try {
			synchronized(db.dbLock) {
				PreparedStatement st;

				java.sql.Timestamp timestamp = new java.sql.Timestamp(new Date().getTime()
										      - ( ((long)deleteAfter) * 24 * 60*60*1000));

				Logger.info(this, "Cleaning:");
				Logger.info(this, "Now: "+new Date().toString());
				Logger.info(this, "Delete older than: "+timestamp.toString()
					    + " ("+Integer.toString(deleteAfter)+")");

				st = db.getConnection().prepareStatement("SELECT "+
									 " id, msgId, inReplyToId, subject, "+
									 " nick, sigId, date, rev, read, "+
									 " archived "+
									 "FROM frostKSKMessages WHERE date < ?");
				st.setTimestamp(1, timestamp);
				ResultSet set = st.executeQuery();

				while(set.next()) {
					KSKMessage msg = new KSKMessage(set.getInt("id"),
									set.getString("msgId"),
									set.getString("inReplyToId"),
									set.getString("subject"),
									set.getString("nick"),
									set.getInt("sigId"),
									null, /* author Identity */
									set.getTimestamp("date"),
									set.getInt("rev"),
									set.getBoolean("read"),
									set.getBoolean("archived"),
									null, /* encryptedFor */
									null /* board */);
					Logger.info(this, "Destroying a message from "+
						    set.getTimestamp("date"));

					msg.destroy(db);
				}
				
				st = db.getConnection().prepareStatement("DELETE FROM frostKSKInvalidSlots WHERE date < ?");
				st.setTimestamp(1, timestamp);
				st.execute();


				timestamp = new java.sql.Timestamp(new Date().getTime()
								   - ( ((long)archiveAfter) * 24 * 60*60*1000));
				Logger.info(this, "Archive older than: "+timestamp.toString()+
					    " ("+Integer.toString(archiveAfter)+")");

				st = db.getConnection().prepareStatement("UPDATE frostKSKMessages SET archived = TRUE WHERE date < ?");
				st.setTimestamp(1, timestamp);
				st.execute();
			}
		} catch(SQLException e) {
			Logger.error(this, "Can't cleanup the db because : "+e.toString());
		}

		return true;
	}


	public MiniFrost getPlugin() {
		return plugin;
	}

	public Core getCore() {
		return core;
	}

	public Hsqldb getDb() {
		return db;
	}


	protected boolean sendQuery(String query) {
		return sendQuery(db, query);
	}

	protected static boolean sendQuery(final Hsqldb db, final String query) {
		try {
			db.executeQuery(query);
			return true;
		} catch(final SQLException e) {
			Logger.notice(e, "While (re)creating sql tables: "+e.toString());
			return false;
		}
	}


	protected void convertExistingTables() {
		if (core.getConfig().getValue("frostKSKDatabaseVersion") == null)
			return;

		if ("0".equals(core.getConfig().getValue("frostKSKDatabaseVersion"))) {
			if (convertDatabase_0_to_1())
				core.getConfig().setValue("frostKSKDatabaseVersion", "2");
		}

		/* due to a stupid mistake, the rev 1 will never really exist */

		if ("1".equals(core.getConfig().getValue("frostKSKDatabaseVersion"))
		    || "2".equals(core.getConfig().getValue("frostKSKDatabaseVersion"))) {
			if (convertDatabase_2_to_3())
				core.getConfig().setValue("frostKSKDatabaseVersion", "3");
		}
	}

	protected boolean convertDatabase_0_to_1() {
		boolean b = sendQuery("ALTER TABLE frostKSKMessages ADD COLUMN encryptedFor INTEGER DEFAULT NULL NULL");

		boolean c = sendQuery("ALTER TABLE frostKSKMessages ADD FOREIGN KEY (encryptedFor) REFERENCES signatures (id)");
		b = b & c;

		if (!b) {

			Logger.error(this, "Error while converting the board database from version 0 to 1");
			return false;

		}

		return true;
	}

	protected boolean convertDatabase_2_to_3() {
		if (!sendQuery("ALTER TABLE frostKSKMessages ADD COLUMN keyDate DATE DEFAULT NULL NULL")) {
			Logger.error(this, "Error while converting the board database from version 2 to 3");
			return false;
		}

		try {
			synchronized(db.dbLock) {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("SELECT id, date FROM frostKSKMessages");

				ResultSet set = st.executeQuery();

				st = db.getConnection().prepareStatement("UPDATE frostKSKMessages SET keyDate = ? WHERE id = ?");

				while (set.next()) {
					int id = set.getInt("id");
					java.sql.Timestamp timestamp = set.getTimestamp("date");
					java.sql.Date date = new java.sql.Date(timestamp.getTime());

					st.setDate(1, date);
					st.setInt(2, id);
					st.execute();
				}
			}
		} catch(SQLException e) {
			Logger.error(this, "Error while converting the board database from version 2 to 3: "+e.toString());
		}

		return true;
	}


	protected void createTables() {
		sendQuery("CREATE CACHED TABLE frostKSKBoards ("
			  + "id INTEGER IDENTITY NOT NULL, "
			  + "name VARCHAR(128) NOT NULL, "
			  + "lastUpdate DATE DEFAULT NULL NULL)");

		sendQuery("CREATE CACHED TABLE frostSSKBoards ("
			  + "id INTEGER IDENTITY NOT NULL, "
			  + "publicKey VARCHAR(256) NOT NULL, "
			  + "privateKey VARCHAR(256) NULL, "
			  + "kskBoardId INTEGER NOT NULL, "
			  + "FOREIGN KEY (kskBoardId) REFERENCES frostKSKBoards (id))");
		
		sendQuery("CREATE CACHED TABLE frostKSKInvalidSlots ("
				+ "id INTEGER IDENTITY NOT NULL, "
				+ "boardId INTEGER NOT NULL, "
				+ "date DATE NOT NULL, "
				+ "minRev INTEGER NOT NULL, "
				+ "maxRev INTEGER NOT NULL, "
				+ "FOREIGN KEY (boardId) REFERENCES frostKSKBoards (id))");

		sendQuery("CREATE CACHED TABLE frostKSKMessages ("
			  + "id INTEGER IDENTITY NOT NULL, "
			  + "subject VARCHAR(512) NULL, "
			  + "nick VARCHAR(128) NOT NULL, "
			  + "sigId INTEGER NULL, "
			  + "content VARCHAR(32768) NOT NULL, "
			  + "keyDate DATE NOT NULL, "
			  + "date TIMESTAMP NOT NULL, "
			  + "msgId VARCHAR(128) NOT NULL, "
			  + "inReplyToId VARCHAR(128) NULL, "
			  + "inReplyTo INTEGER NULL, "
			  + "rev INTEGER NOT NULL, "
			  + "read BOOLEAN DEFAULT FALSE NOT NULL, "
			  + "archived BOOLEAN DEFAULT FALSE NOT NULL, "
			  + "encryptedFor INTEGER DEFAULT NULL NULL, "
			  + "boardId INTEGER NOT NULL, "
			  + "FOREIGN KEY (boardId) REFERENCES frostKSKBoards (id), "
			  + "FOREIGN KEY (inReplyTo) REFERENCES frostKSKMessages (id), "
			  + "FOREIGN KEY (sigId) REFERENCES signatures (id), "
			  + "FOREIGN KEY (encryptedFor) REFERENCES signatures (id))");

		sendQuery("CREATE CACHED TABLE frostKSKAttachmentFiles ("
			  + "id INTEGER IDENTITY NOT NULL, "
			  + "filename VARCHAR(256) NOT NULL, "
			  + "size BIGINT NOT NULL, "
			  + "key VARCHAR(512) NOT NULL, "
			  + "messageId INTEGER NOT NULL, "
			  + "FOREIGN KEY (messageId) REFERENCES frostKSKMessages (id))");

		sendQuery("CREATE CACHED TABLE frostKSKAttachmentBoards ("
			  + "id INTEGER IDENTITY NOT NULL, "
			  + "name VARCHAR(128) NOT NULL, "
			  + "publicKey VARCHAR(256) NULL, "
			  + "privateKey VARCHAR(256) NULL, "
			  + "description VARCHAR(512) NULL, "
			  + "messageId INTEGER NOT NULL, "
			  + "FOREIGN KEY (messageId) REFERENCES frostKSKMessages (id))");

		if (core.getConfig().getValue("frostKSKDatabaseVersion") == null)
			core.getConfig().setValue("frostKSKDatabaseVersion", "3");
	}


	protected void addDefaultBoards() {
		for (int i = 0 ; i < DEFAULT_BOARDS.length ; i++) {
			createBoard(DEFAULT_BOARDS[i], false);
		}
	}


	public Vector getBoards() {
		Vector v = new Vector();

		try {
			synchronized(db.dbLock) {
				PreparedStatement st
					= db.getConnection().prepareStatement("SELECT frostKSKBoards.id, "+
									      "       frostKSKBoards.name, "+
									      "       frostKSKBoards.lastUpdate "+
									      "FROM frostKSKBoards LEFT OUTER JOIN frostSSKBoards "+
									      "  ON frostKSKBoards.id = frostSSKBoards.kskBoardId "+
									      "WHERE frostSSKBoards.id IS NULL "+
				                                              "ORDER BY LOWER(name)");
				ResultSet set = st.executeQuery();

				while(set.next()) {
					int id = set.getInt("id");
					String name = set.getString("name");
					Date lastUpdate = set.getDate("lastUpdate");

					if (boardsHashMap.get(name) != null)
						v.add(boardsHashMap.get(name));
					else {
						KSKBoard board = new KSKBoard(this,
									      id, name, lastUpdate);

						v.add(board);
						boardsHashMap.put(name, board);
					}
				}
			}
		} catch(SQLException e) {
			Logger.error(this, "Can't get the board list because : "+e.toString());
		}

		boards = v;

		return v;
	}



	/**
	 * A little bit inefficient function ...
	 */
	protected KSKBoard getBoard(int id) {
		for (Iterator it = boards.iterator();
		     it.hasNext();) {
			KSKBoard board = (KSKBoard)it.next();

			if (board.getId() == id)
				return board;
		}

		return null;
	}



	public Vector getAllMessages(String[] keywords, int orderBy,
				     boolean desc, boolean archived, boolean read,
				     boolean unsigned, int minTrustLevel) {
		return KSKBoard.getMessages(-1, this, null, keywords,
					    orderBy, desc, archived, read,
					    unsigned, minTrustLevel, true);
	}


	public Vector getSentMessages() {
		Vector v = new Vector();

		try {
			synchronized(db.dbLock) {
				Vector identities = Identity.getYourIdentities(db);

				PreparedStatement st;

				st = db.getConnection().prepareStatement("SELECT "+
									 " id, "+
									 " subject, "+
									 " nick, "+
									 " keyDate, "+
									 " date, "+
									 " msgId, "+
									 " rev, "+
									 " read, "+
									 " archived, "+
									 " encryptedFor, "+
									 " boardId "+
									 "FROM frostKSKMessages "+
									 "WHERE sigId = ? ORDER by DATE DESC");

				for (Iterator it = identities.iterator();
				     it.hasNext() ; ) {
					Identity identity = (Identity)it.next();

					st.setInt(1, identity.getId());

					ResultSet set = st.executeQuery();

					while (set.next()) {
						KSKBoard board = getBoard(set.getInt("boardId"));
						v.add(new KSKMessage(set.getInt("id"),
								     set.getString("msgId"),
								     null, /* in reply to => We don't want a tree to be built */
								     set.getString("subject"),
								     identity.toString(),
								     identity.getId(),
								     identity,
								     set.getTimestamp("date"),
								     set.getInt("rev"),
								     set.getBoolean("read"),
								     set.getBoolean("archived"),
								     null, /* TODO : encryptedFor */
								     board));
					}

				}

			}
		} catch(SQLException e) {
			Logger.error(this, "Can't get the sent messages because : "+
				     e.toString());
		}


		return v;
	}


	public void createBoard(thaw.core.MainWindow mainWindow) {
		String name = JOptionPane.showInputDialog(mainWindow.getMainFrame(),
							  I18n.getMessage("thaw.plugin.miniFrost.boardName"),
							  I18n.getMessage("thaw.plugin.miniFrost.boardName"),
							  JOptionPane.QUESTION_MESSAGE);
		if (name == null)
			return;

		try {
			/* ugly workaround to avoid a crash due to a Sun bug:
			 * If you call JOptionPanel.showInputDialog() and just after
			 * TrayIcon.displayMessage(), Swing will crash.
			 * (Note: remember, TrayIcon.displayMessage() is called by Logger.warning())
			 */
			Thread.sleep(1500);
		} catch(InterruptedException e) {
			/* \_o< */
		}

		createBoard(name);
	}

	protected void createBoard(String name) {
		createBoard(name, true);
	}

	protected void createBoard(String name, boolean warningIfExisting) {
		try {
			synchronized(db.dbLock) {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("SELECT frostKSKBoards.id "+
									 "FROM frostKSKBoards LEFT OUTER JOIN frostSSKBoards "+
									 "  ON frostKSKBoards.id = frostSSKBoards.kskBoardId "+
									 "WHERE frostSSKBoards.id IS NULL "+
									 "AND LOWER(frostKSKBoards.name) = ? "+
									 "LIMIT 1");

				st.setString(1, name.toLowerCase());

				ResultSet set = st.executeQuery();

				if (set.next()) {
					if (warningIfExisting)
						Logger.warning(this, "Board already added");
					return;
				}

				st = db.getConnection().prepareStatement("INSERT INTO frostKSKBoards (name) "+
									 "VALUES (?)");
				st.setString(1, name.toLowerCase());
				st.execute();
			}
		} catch(SQLException e) {
			Logger.error(this, "Can't add board because: "+e.toString());
		}

	}


	protected void createBoard(String name, String publicKey, String privateKey) {
		createBoard(name, publicKey, privateKey, true);
	}

	/**
	 * Put here to make my life simpler with the KSKBoardAttachment.
	 */
	protected void createBoard(String name, String publicKey, String privateKey,
				   				boolean warningIfExisting) {

		if (!thaw.fcp.FreenetURIHelper.isAKey(publicKey)) {
			Logger.error(this, "Invalid publicKey");
			return;
		}
		
		if (thaw.fcp.FreenetURIHelper.isObsolete(publicKey)) {
			new thaw.gui.WarningWindow(core, I18n.getMessage("thaw.error.obsolete"));
			return;
		}

		if (privateKey != null && "".equals(privateKey))
			privateKey = null;


		try {
			synchronized(db.dbLock) {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("SELECT id "+
									 "FROM frostSSKBoards "+
									 "WHERE publicKey = ?");
				st.setString(1, publicKey);
				ResultSet set = st.executeQuery();

				if (set.next()) {
					if (warningIfExisting)
						Logger.warning(this, "Board already added");
					return;
				}

				/* we must get the id first, else we will mix up things */

				int id = 0;

				st = db.getConnection().prepareStatement("SELECT id FROM frostKSKBoards "+
									 "ORDER by id DESC LIMIT 1");
				set = st.executeQuery();

				if (set.next())
					id = set.getInt("id") + 1;


				name = name.toLowerCase();

				st = db.getConnection().prepareStatement("INSERT INTO frostKSKBoards "+
									 "(id, name) VALUES (?, ?)");

				st.setInt(1, id);
				st.setString(2, name);

				st.execute();


				st = db.getConnection().prepareStatement("INSERT INTO frostSSKBoards "+
									 "(publicKey, privateKey, kskBoardId) "+
									 "VALUES (?, ?, ?)");
				st.setString(1, publicKey);
				if (privateKey != null)
					st.setString(2, privateKey);
				else
					st.setNull(2, Types.VARCHAR);
				st.setInt(3, id);

				st.execute();

			}
		} catch(SQLException e) {
			Logger.error(this, "Can't add the board because : "+e.toString());
		}
	}

	public Vector getAllKnownBoards() {
		Vector v = new Vector();

		try {
			synchronized(db.dbLock) {
				PreparedStatement st;
				
				st = db.getConnection().prepareStatement("select distinct name, publickey, privatekey from frostKSKAttachmentBoards");
				
				ResultSet set = st.executeQuery();
				
				while(set.next()) {
					v.add(new KSKBoardAttachment(this,
												set.getString("name"),
												set.getString("publicKey"),
												set.getString("privateKey"),
												null));
				}
			}
		} catch(SQLException e) {
			Logger.error(this, "Can't get the list of know boards because: "+e.toString());
		}

		return v;
	}

	public String toString() {
		return I18n.getMessage("thaw.plugin.miniFrost.FrostKSK");
	}
}
