package thaw.plugins.signatures;

import java.awt.Color;

import java.sql.*;

import java.util.Vector;
import java.math.BigInteger;

import freenet.crypt.SHA256;
import freenet.support.Base64;

import freenet.crypt.DSA;
import freenet.crypt.DSAPrivateKey;
import freenet.crypt.DSAPublicKey;
import freenet.crypt.DSASignature;
import freenet.crypt.Global;
import freenet.crypt.RandomSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import thaw.core.Core;
import thaw.core.Logger;
import thaw.core.I18n;
import thaw.plugins.Hsqldb;
import thaw.core.Config;



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
		new java.awt.Color(0, 175, 0), /* green */
		new java.awt.Color(0, 128, 0), /* light green */
		Color.BLACK,
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
	 * @param nick part *before* the @
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


	public int getId() {
		return id;
	}

	public String getNick() {
		return nick;
	}

	public byte[] getY() {
		return y;
	}

	public byte[] getX() {
		return x;
	}

	public String getTrustLevelStr() {
		int i;

		if (x != null) {
			return "thaw.plugin.signature.trustLevel.me";
		}

		for (i = 0 ; i < trustLevelInt.length ; i++) {
			if (trustLevelInt[i] == trustLevel)
				break;
		}

		if (i < trustLevelInt.length) {
			return trustLevelStr[i];
		}


		return "[?]";
	}

	public boolean isDup() {
		return isDup;
	}


	public Color getTrustLevelColor() {
		int i;

		if (x != null)
			return new java.awt.Color(0, 175, 0);

		for (i = 0 ; i < trustLevelInt.length ; i++) {
			if (trustLevelInt[i] == trustLevel)
				break;
		}

		if (i < trustLevelInt.length) {
			return trustLevelColor[i];
		}

		return Color.BLACK;
	}

	public void setTrustLevel(String str) {
		int i;

		for (i = 0 ; i < Identity.trustLevelStr.length ; i++) {
			if (I18n.getMessage(Identity.trustLevelStr[i]).equals(str))
				break;
		}

		if (i >= Identity.trustLevelStr.length) {
			Logger.error(this, "Unknown trust level: "+str);
			return;
		}

		setTrustLevel(trustLevelInt[i]);
	}


	public void setTrustLevel(int i) {
		try {
			synchronized(db.dbLock) {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("UPDATE signatures SET trustLevel = ? WHERE id = ?");
				st.setInt(1, i);
				st.setInt(2, id);

				st.execute();
			}
		} catch(SQLException e) {
			Logger.error(this, "Unable to change trust level because: "+e.toString());
		}
	}


	/**
	 * will put all the other identities with the same nickname as duplicata,
	 * and will put this identity as non duplicate
	 */
	public void setOriginal() {
		try {
			synchronized(db.dbLock) {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("UPDATE signatures SET isDup = TRUE "
									 + "WHERE LOWER(nickName) = ?");
				st.setString(1, nick.toLowerCase());

				st.execute();

				st = db.getConnection().prepareStatement("UPDATE signatures SET isDup = FALSE "
									 + "WHERE id = ?");
				st.setInt(1, id);

				st.execute();
			}
		} catch(SQLException e) {
			Logger.error(this,
				     "SQLException while setting the identity as original : "
				     +e.toString());
		}
	}


	public boolean mustBeIgnored(Config config) {
		if (x != null)
			return false;

		int min = Integer.parseInt(config.getValue("minTrustLevel"));

		return (trustLevel < min);
	}


	/**
	 * if the identity doesn't exists, it will be created
	 */
	public static Identity getIdentity(Hsqldb db,
					   String nick,
					   byte[] y) {
		try {
			synchronized(db.dbLock) {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("SELECT id, nickName, y, x, isDup, trustLevel "+
									 "FROM signatures "+
									 "WHERE y = ? LIMIT 1");
				st.setBytes(1, y);

				ResultSet set = st.executeQuery();

				if (set.next()) {
					Identity i = new Identity(db, set.getInt("id"), set.getString("nickName"),
								  set.getBytes("y"), set.getBytes("x"),
								  set.getBoolean("isDup"), set.getInt("trustLevel"));
					Logger.info(i, "Identity found");
					return i;
				}

				/* else we must add it, but first we need to know if it's a dup */

				st = db.getConnection().prepareStatement("SELECT id FROM signatures "+
									 "WHERE lower(nickName) = ? LIMIT 1");
				st.setString(1, nick.toLowerCase());

				set = st.executeQuery();

				boolean isDup = set.next();

				/* and we add */

				st = db.getConnection().prepareStatement("INSERT INTO signatures "+
									 "(nickName, y, x, isDup, trustLevel) "+
									 "VALUES (?, ?, ?, ?, 0)");

				st.setString(1, nick);
				st.setBytes(2, y);
				st.setNull(3, Types.VARBINARY);
				st.setBoolean(4, isDup);

				st.execute();

				Identity i = new Identity(db, -1, nick, y, null, isDup, 0);
				Logger.info(i, "New identity found");
				return i;

			}
		} catch(SQLException e) {
			Logger.error(new Identity(), "Error while getting identity (2) : "+e.toString());
		}

		return null;
	}


	/**
	 * won't create
	 */
	public static Identity getIdentity(Hsqldb db,
					   int id) {
		Identity i = null;

		try {
			synchronized(db.dbLock) {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("SELECT id, nickName, y, x, isDup, trustLevel "+
									 "FROM signatures "+
									 "WHERE id = ? LIMIT 1");
				st.setInt(1, id);

				ResultSet set = st.executeQuery();

				if (!set.next())
					return null;

				i = new Identity(db, id, set.getString("nickName"),
						 set.getBytes("y"), set.getBytes("x"),
						 set.getBoolean("isDup"), set.getInt("trustLevel"));
			}
		} catch(SQLException e) {
			Logger.error(new Identity(), "Error while getting identity (1) : "+e.toString());
		}

		return i;
	}


	/**
	 * Generate a new identity
	 * you have to insert() it
	 * @param db just here to fill in the class
	 */
	public static Identity generate(Hsqldb db, String nick) {
		Logger.info(nick, "thaw.plugins.signatures.Identity : Generating new identity ...");

		DSAPrivateKey privateKey = new DSAPrivateKey(Global.DSAgroupBigA, Core.getRandom());
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
			Logger.warning(this, "Exception while deleting the identity from the bdd: "+e.toString());
			new thaw.gui.WarningWindow((thaw.core.MainWindow)null,
						   I18n.getMessage("thaw.plugin.signature.delete.cant"));
		}

	}


	public DSASignature sign(String text) {
		return sign(text, x);
	}


	public static DSASignature sign(String text, byte[] x) {
		BigInteger m;

		byte[] bytes;

		try {
			bytes = text.getBytes("UTF-8");
		} catch(java.io.UnsupportedEncodingException e) {
			Logger.warning(new Identity(), "sign() : UnsupportedEncodingException ? => Falling back on default charset");
			bytes = text.getBytes();
		}

		m = new BigInteger(1, SHA256.digest(bytes));


		DSASignature sign = DSA.sign(Global.DSAgroupBigA,
					     new DSAPrivateKey(new BigInteger(1, x)),
					     m,
					     (RandomSource)Core.getRandom());


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

		byte[] bytes;

		try {
			bytes = text.getBytes("UTF-8");
		} catch(java.io.UnsupportedEncodingException e) {
			/* no logging because if it happens once, it will happen often */
			bytes = text.getBytes();
		}

		m = new BigInteger(1, SHA256.digest(bytes));

		boolean ret = DSA.verify(new DSAPublicKey(Global.DSAgroupBigA, new BigInteger(1, y)),
					 new DSASignature(new BigInteger(1, r), new BigInteger(1, s)),
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


	public boolean exportIdentity(File file) {

		try {
			FileOutputStream out = new FileOutputStream(file);

			out.write((nick+"\n").getBytes("UTF-8"));

			if (getX() != null)
				out.write( (Base64.encode(getX())+"\n").getBytes("UTF-8") );
			else
				out.write( "\n".getBytes("UTF-8"));

			if (getY() != null)
				out.write( (Base64.encode(getY())+"\n").getBytes("UTF-8") );
			else
				out.write( "\n".getBytes("UTF-8"));

			out.close();
		} catch(java.io.FileNotFoundException e) {
			Logger.error(this, "(1) Can't export identity because: "+e.toString());
			return false;
		} catch(java.io.UnsupportedEncodingException e) {
			Logger.error(this, "(2) Can't export identity because: "+e.toString());
			return false;
		} catch (java.io.IOException e) {
			Logger.error(this, "(2) Can't export identity because: "+e.toString());
			return false;
		}

		return true;
	}

	public static Identity importIdentity(Hsqldb db, File file) {
		try {
			byte[] lapin = new byte[5120];

			FileInputStream in = new FileInputStream(file);

			for (int i = 0 ; i < 5120 ; i++)
				lapin[i] = 0;

			in.read(lapin);
			in.close();

			String[] elements = new String(lapin).split("\n");


			if (elements.length < 3) {
				Logger.error(new Identity(), "not enought inforation in the file");
				return null;
			}

			Identity i = new Identity(db, -1, elements[0],
						  Base64.decode(elements[2]),
						  Base64.decode(elements[1]),
						  false, 10);
			i.insert();

			return i;
		} catch(java.io.FileNotFoundException e) {
			Logger.error(new Identity(), "(1) Unable to import identity because : "+e.toString());
		} catch(java.io.IOException e) {
			Logger.error(new Identity(), "(2) Unable to import identity because : "+e.toString());
		} catch(freenet.support.IllegalBase64Exception e) {
			Logger.error(new Identity(), "(2) Unable to import identity because : "+e.toString());
		}

		return null;
	}
}

