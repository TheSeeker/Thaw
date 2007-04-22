package thaw.plugins.signatures;

import java.awt.Color;

import java.security.MessageDigest;

import java.sql.*;

import java.util.Vector;
import java.util.Iterator;
import java.math.BigInteger;

import freenet.crypt.SHA256;
import freenet.support.Base64;

import freenet.crypt.DSA;
import freenet.crypt.DSAPrivateKey;
import freenet.crypt.DSAGroup;
import freenet.crypt.DSAPublicKey;
import freenet.crypt.DSASignature;
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

	/**
	 * If you don't have a value, let it to null and pray it won't be used :P
	 */
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

		hash = Base64.encode(SHA256.digest(y));
	}



	/**
	 * Generate a new identity
	 * you have to insert() it
	 * @param db just here to fill in the class
	 */
	public static Identity generate(Hsqldb db, String nick) {
		Logger.info(nick, "thaw.plugins.signatures.Identity : Generating new identity ...");

		freenet.crypt.RandomSource randomSource = thaw.plugins.signatures.RandomSource.getRandomSource();

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


	public DSASignature sign(String text) {
		return sign(text, x);
	}


	public static DSASignature sign(String text, byte[] x) {
		freenet.crypt.RandomSource randomSource = thaw.plugins.signatures.RandomSource.getRandomSource();

		BigInteger m;


		try {
			m = new BigInteger(SHA256.digest(text.getBytes("UTF-8")));
		} catch(java.io.UnsupportedEncodingException e) {
			Logger.warning(new Identity(), "sign() : UnsupportedEncodingException ? => Falling back on default charset");
			m = new BigInteger(SHA256.digest(text.getBytes()));
		}


		DSASignature sign = DSA.sign(Global.DSAgroupBigA,
					     new DSAPrivateKey(new BigInteger(x)),
					     m,
					     randomSource);


		return sign;
	}


	public boolean check(String text, byte[] r, byte[] s) {
		return check(text, r, s, y);
	}


	public static boolean check(String text, /* signed text */
				    byte[] r, /* sig */
				    byte[] s, /* sig */
				    byte[] y) /* publicKey */ {

		BigInteger m;

		try {
			m = new BigInteger(SHA256.digest(text.getBytes("UTF-8")));
		} catch(java.io.UnsupportedEncodingException e) {
			/* no logging because if it happens once, it will happen often */
			m = new BigInteger(SHA256.digest(text.getBytes()));
		}

		boolean ret = DSA.verify(new DSAPublicKey(Global.DSAgroupBigA, new BigInteger(y)),
					 new DSASignature(new BigInteger(r), new BigInteger(s)),
					 m, false);

		return ret;
	}


	public String toString() {
		return nick+"@"+hash;
	}


	public static Vector getIdentities(Hsqldb db, String cond) {
		try {
			synchronized(db.dbLock) {
				Vector v = new Vector();

				PreparedStatement st;

				if (cond != null)
					st = db.getConnection().prepareStatement("SELECT id, nickName, y, x, isDup, trustLevel FROM signatures WHERE "+cond + " ORDER BY nickName");
				else
					st = db.getConnection().prepareStatement("SELECT id, nickName, y, x, isDup, trustLevel FROM signatures ORDER BY nickName");

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

