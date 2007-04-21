package thaw.plugins.signatures;

import java.awt.Color;

import java.security.MessageDigest;

import java.sql.*;

import java.util.Vector;
import java.util.Iterator;

import freenet.crypt.SHA256;
import freenet.support.Base64;

import freenet.crypt.DSAPrivateKey;
import freenet.crypt.DSAGroup;
import freenet.crypt.DSAPublicKey;
import freenet.crypt.DSASignature;
import freenet.crypt.Yarrow;
import freenet.crypt.Global;

import thaw.core.Logger;
import thaw.plugins.Hsqldb;


public class Identity {

	public final static int[] trustLevelInt = {
		100,
		10,
		1,
		0,
		-1,
		-10
	};

	public final static String[] trustLevelStr = {
		"thaw.plugin.signature.trustLevel.dev",
		"thaw.plugin.signature.trustLevel.good",
		"thaw.plugin.signature.trustLevel.observe",
		"thaw.plugin.signature.trustLevel.check",
		"thaw.plugin.signature.trustLevel.bad",
		"thaw.plugin.signature.trustLevel.evil"
	};

	public final static Color[] trustLevelColor = {
		Color.BLUE,
		Color.GREEN,
		new java.awt.Color(0, 128, 0), /* light green */
		Color.ORANGE,
		new java.awt.Color(175, 0, 0), /* moderatly red */
		Color.RED
	};


	private Hsqldb db;

	private int id;

	private String nick;


	/* public key */
	private byte[] y;

	/* private key */
	private byte[] x;

	private boolean isDup;
	private int trustLevel;


	private String hash;


	private Identity() {
	}

	public Identity(Hsqldb db, int id, String nick,
			byte[] y, byte[] x,
			boolean isDup,
			int trustLevel) {
		this.db = db;
		this.id = id;
		this.nick = nick;
		this.y = y;
		this.x = x;
		this.isDup = isDup;
		this.trustLevel = trustLevel;

		MessageDigest md = SHA256.getMessageDigest();
		md.reset();
		md.update(y);

		hash = Base64.encode(md.digest());
	}


	/**
	 * Generate a new identity
	 * you have to insert() it
	 * @param db just here to fill in the class
	 */
	public static Identity generate(Hsqldb db, String nick) {
		Logger.info(nick, "thaw.plugins.signatures.Identity : Generating new identity ...");

		Yarrow randomSource = new Yarrow();

		DSAPrivateKey privateKey = new DSAPrivateKey(Global.DSAgroupBigA, randomSource);
		DSAPublicKey publicKey = new DSAPublicKey(Global.DSAgroupBigA, privateKey);

		Identity identity = new Identity(db, -1, nick,
						 publicKey.getY().toByteArray(),
						 privateKey.getX().toByteArray(),
						 false,
						 10);


		Logger.info(identity, "done");

		return identity;
	}


	/**
	 * id won't be set
	 */
	public void insert() {
		try {
			synchronized(db.dbLock) {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("INSERT INTO signatures (nickName, y, x, isDup, trustLevel) "+
									 "VALUES (?, ?, ?, ?, ?)");
				st.setString(1, nick);
				st.setBytes(2, y);
				st.setBytes(3, x);
				st.setBoolean(4, isDup);
				st.setInt(5, trustLevel);

				st.execute();
			}
		} catch(SQLException e) {
			Logger.error(this, "Exception while adding the identity to the bdd: "+e.toString());
		}
	}


	public void delete() {
		try {
			synchronized(db.dbLock) {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("DELETE FROM signatures "+
									 "WHERE id = ?");
				st.setInt(1, id);

				st.execute();
			}
		} catch(SQLException e) {
			Logger.error(this, "Exception while deleting the identity from the bdd: "+e.toString());
		}

	}


	/**
	 * All the parameters are Base64 encoded, except text.
	 */
	public static boolean isValid(String text, /* signed text */
				      String r, /* sig */
				      String s, /* sig */
				      String y) /* publicKey */ {
		return true;
	}


	/**
	 * we use q as a reference
	 */
	public static boolean isDuplicata(Hsqldb db, String nickName, String q) {
		return false;
	}



	public String toString() {
		return nick+"@"+hash;
	}


	public static Vector getIdentities(Hsqldb db, String cond) {
		try {
			synchronized(db.dbLock) {
				Vector v = new Vector();

				PreparedStatement st;

				st = db.getConnection().prepareStatement("SELECT id, nickName, y, x, isDup, trustLevel FROM signatures WHERE "+cond + " ORDER BY nickName");
				ResultSet set = st.executeQuery();

				while(set.next()) {
					v.add(new Identity(db,
							   set.getInt("id"),
							   set.getString("nickName"),
							   set.getBytes("y"),
							   set.getBytes("x"),
							   set.getBoolean("isDup"),
							   set.getInt("trustLevel")));
				}

				return v;
			}
		} catch(SQLException e) {
			Logger.error(new Identity(), "Error while getting identities (1): "+e.toString());
		}

		return null;
	}


	public static Vector getYourIdentities(Hsqldb db) {
		return getIdentities(db, "x IS NOT NULL");
	}


	public static Vector getOtherIdentities(Hsqldb db) {
		return getIdentities(db, "x IS NULL");
	}
}

