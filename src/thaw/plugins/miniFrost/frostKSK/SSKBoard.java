package thaw.plugins.miniFrost.frostKSK;

import java.sql.*;

import java.util.Date;
import thaw.core.Logger;
import thaw.plugins.Hsqldb;


public class SSKBoard extends KSKBoard {

	private String publicKey;
	private String privateKey;

	public SSKBoard(SSKBoardFactory factory,
			int id, String name, Date lastUpdate,
			String publicKey, String privateKey,
			int newMessages) {

		super(factory, id, name, lastUpdate, newMessages);

		if (!publicKey.endsWith("/"))
			publicKey += "/";
		if (privateKey != null && !privateKey.endsWith("/"))
			privateKey += "/";

		this.publicKey = publicKey;
		this.privateKey = privateKey;
	}


	/**
	 * called by KSKMessage.download();
	 */
	protected String getDownloadKey(Date date, int rev) {
		java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyy.M.d");

		StringBuffer keyBuf = new StringBuffer(publicKey);

		keyBuf.append(getName()+"|");
		keyBuf = formatter.format(date, keyBuf, new java.text.FieldPosition(0));
		keyBuf.append("-");
		keyBuf.append(Integer.toString(rev));
		keyBuf.append(".xml");

		return keyBuf.toString();
	}

	/**
	 * called by KSKDraft
	 */
	protected String getPrivateKey() {
		return privateKey;
	}

	/**
	 * called by KSKDraft
	 */
	protected String getNameForInsertion(Date date, int rev) {
		java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyy.M.d");

		StringBuffer keyBuf = new StringBuffer(getName()+"|");

		keyBuf = formatter.format(date, keyBuf, new java.text.FieldPosition(0));
		keyBuf.append("-");
		keyBuf.append(Integer.toString(rev));
		keyBuf.append(".xml");

		return keyBuf.toString();
	}

	public boolean destroy() {
		try {
			Hsqldb db = getFactory().getDb();

			synchronized(db.dbLock) {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("DELETE FROM frostSSKBoards "+
									 "WHERE kskBoardId = ?");
				st.setInt(1, getId());
				st.execute();
			}
		} catch(SQLException e) {
			Logger.error(this, "Can't destroy the board because : "+e.toString());
			return false;
		}

		if (!super.destroy()) {
			/* unable to destroy the board
			 * => we put back the public and private keys
			 * to avoid some desynchronisations
			 */

			try {
				Hsqldb db = getFactory().getDb();

				synchronized(db.dbLock) {
					PreparedStatement st;

					st = db.getConnection().prepareStatement("INSERT INTO frostSSKBoards "+
										 "(publicKey, privateKey, kskBoardId) "+
										 "VALUES (?, ?, ?)");
					st.setString(1, publicKey);
					if (privateKey != null)
						st.setString(2, privateKey);
					else
						st.setNull(2, Types.VARCHAR);
					st.setInt(3, getId());

					st.execute();
				}
			} catch(SQLException e) {
				Logger.error(this, "Oops ! Unable to delete the board, and I think that I've broken something :(");
				Logger.error(this, "Reason : "+e.toString());
			}

			return false;
		}

		return true;
	}


	public thaw.plugins.miniFrost.interfaces.Draft getDraft(thaw.plugins.miniFrost.interfaces.Message inReplyTo) {
		if (privateKey == null) {
			Logger.warning(this, "Sorry, you need the private key to post on this board");
			return null;
		}

		return super.getDraft(inReplyTo);
	}
}

