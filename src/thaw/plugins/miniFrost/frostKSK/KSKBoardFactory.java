package thaw.plugins.miniFrost.frostKSK;

import java.util.Vector;
import javax.swing.JOptionPane;

import java.sql.*;

import java.util.HashMap;
import java.util.Iterator;

import thaw.core.Core;
import thaw.core.Logger;
import thaw.core.I18n;

import thaw.plugins.Hsqldb;
import thaw.plugins.MiniFrost;



public class KSKBoardFactory
	implements thaw.plugins.miniFrost.interfaces.BoardFactory {

	private Hsqldb db;
	private Core core;
	private MiniFrost plugin;


	private HashMap boards;


	public KSKBoardFactory() {

	}


	public boolean init(Hsqldb db, Core core, MiniFrost plugin) {
		this.db = db;
		this.core = core;
		this.plugin = plugin;

		createTables();
		boards = new HashMap();

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


	protected void createTables() {
		core.getConfig().setValue("frostKSKDatabaseVersion", "0");

		sendQuery("CREATE CACHED TABLE frostKSKBoards ("
			  + "id INTEGER IDENTITY NOT NULL, "
			  + "name VARCHAR(128) NOT NULL, "
			  + "lastUpdate DATE DEFAULT NULL NULL)");

		sendQuery("CREATE CACHED TABLE frostKSKMessages ("
			  + "id INTEGER IDENTITY NOT NULL, "
			  + "subject VARCHAR(512) NULL, "
			  + "nick VARCHAR(128) NOT NULL, "
			  + "sigId INTEGER NULL, "
			  + "content VARCHAR(32768) NOT NULL, "
			  + "date TIMESTAMP NOT NULL, "
			  + "msgId VARCHAR(128) NOT NULL, "
			  + "inReplyToId VARCHAR(128) NULL, "
			  + "inReplyTo INTEGER NULL, "
			  + "rev INTEGER NOT NULL, "
			  + "read BOOLEAN DEFAULT FALSE NOT NULL, "
			  + "archived BOOLEAN DEFAULT FALSE NOT NULL, "
			  + "boardId INTEGER NOT NULL, "
			  + "FOREIGN KEY (boardId) REFERENCES frostKSKBoards (id), "
			  + "FOREIGN KEY (inReplyTo) REFERENCES frostKSKMessages (id), "
			  + "FOREIGN KEY (sigId) REFERENCES signatures (id))");
	}



	public Vector getBoards() {
		Vector v = new Vector();

		try {
			synchronized(db.dbLock) {
				PreparedStatement st
					= db.getConnection().prepareStatement("SELECT id, name, lastUpdate "+
									      "FROM frostKSKBoards "+
				                                              "ORDER BY LOWER(name)");
				ResultSet set = st.executeQuery();

				while(set.next()) {
					int id = set.getInt("id");
					String name = set.getString("name");

					if (boards.get(name) != null)
						v.add(boards.get(name));
					else {

						int count = 0;
						java.util.Date lastUpdate = set.getDate("lastUpdate");

						PreparedStatement subSt
							= st.getConnection().prepareStatement("SELECT count(id)"+
											      "FROM frostKSKMessages "+
											      "WHERE boardId = ? "+
											      "AND read = FALSE AND archived = FALSE");
						subSt.setInt(1, id);

						ResultSet subRes = subSt.executeQuery();

						if (subRes.next())
							count = subRes.getInt(1);

						KSKBoard board = new KSKBoard(this,
									      id, name, lastUpdate,
									      count);

						v.add(board);
						boards.put(name, board);
					}
				}
			}
		} catch(SQLException e) {
			Logger.error(this, "Can't get the board list because : "+e.toString());
		}

		return v;
	}


	protected KSKBoard getBoard(String name) {
		return (KSKBoard)boards.get(name);
	}


	protected KSKBoard getBoard(int id) {
		for (Iterator it = boards.values().iterator();
		     it.hasNext();) {
			KSKBoard board = (KSKBoard)it.next();

			if (board.getId() == id)
				return board;
		}

		return null;
	}


	public Vector getAllMessages(String[] keywords, int orderBy,
				     boolean desc, boolean archived) {
		return KSKBoard.getMessages(-1, this, null, keywords,
					    orderBy, desc, archived, true);
	}



	public void createBoard(thaw.core.MainWindow mainWindow) {
		String name = JOptionPane.showInputDialog(mainWindow.getMainFrame(),
							  I18n.getMessage("thaw.plugin.miniFrost.boardName"),
							  I18n.getMessage("thaw.plugin.miniFrost.boardName"),
							  JOptionPane.QUESTION_MESSAGE);
		if (name == null)
			return;

		try {
			synchronized(db.dbLock) {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("SELECT id FROM frostKSKBoards "+
									 "WHERE LOWER(name) = ?");
				st.setString(1, name.toLowerCase());

				ResultSet set = st.executeQuery();

				if (set.next()) {
					Logger.notice(this, "Board already added");
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

	public String toString() {
		return I18n.getMessage("thaw.plugin.miniFrost.FrostKSK");
	}
}
