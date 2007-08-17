package thaw.plugins.miniFrost.frostKSK;

import java.sql.*;

import java.util.Vector;

import thaw.core.I18n;
import thaw.plugins.Hsqldb;
import thaw.core.Logger;
import thaw.fcp.FCPQueueManager;

import thaw.plugins.miniFrost.interfaces.Board;


public class KSKBoardAttachment
	extends KSKAttachment {

	private String boardName;
	private String publicKey;
	private String privateKey;
	private String description;

	private KSKMessage msg;
	private KSKBoardFactory boardFactory;


	public KSKBoardAttachment() {

	}

	public KSKBoardAttachment(Board board) {
		if (board instanceof KSKBoard) {
			boardName = ((KSKBoard)board).getName();
		}

		if (board instanceof SSKBoard) {
			publicKey = ((SSKBoard)board).getPublicKey();
			privateKey = ((SSKBoard)board).getPrivateKey();
		}
	}

	public KSKBoardAttachment(String boardName,
				  String publicKey,
				  String privateKey,
				  String description) {
		this.boardName = boardName;

		if (publicKey != null && publicKey.endsWith("/"))
			publicKey.replaceAll("/", "");

		if (privateKey != null && privateKey.endsWith("/"))
			privateKey.replaceAll("/", "");

		this.publicKey = publicKey;
		this.privateKey = privateKey;
		this.description = description;

		if (this.description == null)
			this.description = "";
	}


	public KSKBoardAttachment(String boardName,
				  String publicKey,
				  String privateKey,
				  String description,
				  KSKMessage msg,
				  KSKBoardFactory boardFactory) {
		this(boardName, publicKey, privateKey, description);
		this.msg = msg;
		this.boardFactory = boardFactory;
	}


	public String getType() {
		return "board";
	}

	public String getPrintableType() {
		return I18n.getMessage("thaw.plugin.miniFrost.board");
	}

	public String[] getProperties() {
		/* DIRTY :p */
		if (publicKey == null && boardName != null) {
			return new String[] {
				"Name",
				"description",
			};
		} else if (privateKey == null && boardName != null) {
			return new String[] {
				"Name",
				"pubKey",
				"description"
			};
		} else { /* if this attachment is empty or if we have all the values */
			return new String[] {
				"Name",
				"pubKey",
				"privKey",
				"description"
			};
		}
	}


	public String[] getValues() {
		/* DIRTY :p */
		if (publicKey == null) {
			return new String[] {
				boardName,
				description,
			};
		} else if (privateKey == null) {
			return new String[] {
				boardName,
				publicKey,
				description
			};
		} else {
			return new String[] {
				boardName,
				publicKey,
				privateKey,
				description
			};
		}
	}

	public String getValue(String property) {
		if ("Name".equals(property)) {
			return boardName;
		} else if ("pubKey".equals(property)) {
			return publicKey;
		} else if ("privKey".equals(property)) {
			return privateKey;
		} else if ("description".equals(property)) {
			return description;
		}

		return null;
	}

	public static final char[] INVALID_CHARS = { '/', '\\', '?', '*', '<', '>',
						     '\"', ':', '|', '#', '&' };

	private void setBoardName(String name) {
		if (name.startsWith("."))
			name = "_" + name;

		for (int i = 0 ; i < INVALID_CHARS.length ; i++) {
			name = name.replace(INVALID_CHARS[i], '_');
		}

		boardName = name;
	}

	public void setValue(String property, String value) {
		if ("Name".equals(property)) {
			setBoardName(value);
		} else if ("pubKey".equals(property)) {
			publicKey = value;
		} else if ("privKey".equals(property)) {
			privateKey = value;
		} else if ("description".equals(property)) {
			description = value;
		} else {
			Logger.error(this, "Unknown field : "+property);
		}
	}

	public String getContainer() {
		return null;
	}

	public String toString() {
		if (publicKey == null)
			return boardName;
		else if (privateKey == null)
			return boardName + " (R)";
		else
			return boardName + " (R/W)";
	}

	public String[] getActions() {
		return new String[] {
			I18n.getMessage("thaw.common.add")
		};
	}


	public void apply(Hsqldb db, FCPQueueManager queueManager, String action) {
		if (action.equals(I18n.getMessage("thaw.common.add"))) {
			if (publicKey != null) {
				boardFactory.createBoard(boardName, publicKey, privateKey);
				boardFactory.getPlugin().getPanel().notifyChange();

				return;
			}
			boardFactory.createBoard(boardName);
			boardFactory.getPlugin().getPanel().notifyChange();
		}
	}


	public void insert(Hsqldb db, int messageId) {
		if (boardName == null) {
			Logger.warning(this, "Missing field");
			return;
		}

		if (description == null) {
			Logger.notice(this, "no description");
		}

		try {
			synchronized(db.dbLock) {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("INSERT INTO frostKSKAttachmentBoards "
									 +"(name, publicKey, privateKey, description, messageId) "
									 +"VALUES (?, ?, ?, ?, ?)");
				st.setString(1, boardName);
				st.setString(2, publicKey);
				st.setString(3, privateKey);
				st.setString(4, description);
				st.setInt(5, messageId);

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

				st = db.getConnection().prepareStatement("SELECT name, publicKey, "+
									 "privateKey, description "+
									 "FROM frostKSKAttachmentBoards "+
									 "WHERE messageId = ?");
				st.setInt(1, msg.getId());

				ResultSet set = st.executeQuery();

				while(set.next()) {
					v.add(new KSKBoardAttachment(set.getString("name"),
								     set.getString("publicKey"),
								     set.getString("privateKey"),
								     set.getString("description"),
								     msg,
								     boardFactory));
				}
			}
		} catch(SQLException e) {
			Logger.error(e, "Can't select file attachments because: "+e.toString());
		}

		return (v.size() > 0 ? v : null);
	}


	public StringBuffer getSignedStr() {
		StringBuffer buf = new StringBuffer();

		Logger.info(this, "Board : "+boardName);
		buf.append(boardName.toLowerCase()).append(KSKMessageParser.SIGNATURE_ELEMENTS_SEPARATOR);

		if (publicKey != null) {
			Logger.info(this, "public key for : "+boardName+" : '"+publicKey+"'");
			buf.append(publicKey.trim()).append(KSKMessageParser.SIGNATURE_ELEMENTS_SEPARATOR);
		}
		if (privateKey != null) {
			Logger.info(this, "private key for : "+boardName+" : '"+privateKey+"'");
			buf.append(privateKey.trim()).append(KSKMessageParser.SIGNATURE_ELEMENTS_SEPARATOR);
		}

		return buf;
	}


	public static boolean destroy(KSKMessage msg, Hsqldb db) {
		try {
			synchronized(db.dbLock) {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("DELETE FROM frostKSKAttachmentBoards "+
									 "WHERE messageId = ?");
				st.setInt(1, msg.getId());
				st.execute();
			}
		} catch(SQLException e) {
			Logger.error(null, "Can't destroy the message attachments of the board because : "+e.toString());
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
					st = db.getConnection().prepareStatement("DELETE FROM frostKSKAttachmentBoards "+
										 "WHERE messageId = ?");
					st.setInt(1, id);
					st.execute();
				}
			}
		} catch(SQLException e) {
			Logger.error(null, "Can't destroy the board attachments of the board because : "+e.toString());
			return false;
		}

		return true;
	}


	public boolean isReady() {
		return true;
	}
}
