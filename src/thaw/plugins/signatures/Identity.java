package thaw.plugins.signatures;

import java.awt.Color;

import java.sql.*;

import java.util.Vector;
import java.util.Iterator;
import java.util.List;

import frost.crypt.FrostCrypt;
import frost.util.XMLTools;
import org.w3c.dom.*;
import java.io.File;
import thaw.core.Logger;
import thaw.core.I18n;
import thaw.plugins.Hsqldb;
import thaw.plugins.Signatures;
import thaw.core.Config;



public class Identity {

	public final static int[] trustLevelInt = {
		100,
		10,
		5,
		1,
		0,
		-1,
		-5,
		-10
	};

	public final static String[] trustLevelStr = {
		I18n.getMessage("thaw.plugin.signature.trustLevel.dev"),
		I18n.getMessage("thaw.plugin.signature.trustLevel.trustworthy"),
		I18n.getMessage("thaw.plugin.signature.trustLevel.good"),
		I18n.getMessage("thaw.plugin.signature.trustLevel.observe"),
		I18n.getMessage("thaw.plugin.signature.trustLevel.check"),
		I18n.getMessage("thaw.plugin.signature.trustLevel.bad"),
		I18n.getMessage("thaw.plugin.signature.trustLevel.evil"),
		I18n.getMessage("thaw.plugin.signature.trustLevel.asshole")
	};

	public final static String[] trustLevelUserStr= {
		I18n.getMessage("thaw.plugin.signature.trustLevel.trustworthy"),
		I18n.getMessage("thaw.plugin.signature.trustLevel.good"),
		I18n.getMessage("thaw.plugin.signature.trustLevel.observe"),
		I18n.getMessage("thaw.plugin.signature.trustLevel.check"),
		I18n.getMessage("thaw.plugin.signature.trustLevel.bad"),
		I18n.getMessage("thaw.plugin.signature.trustLevel.evil"),
		I18n.getMessage("thaw.plugin.signature.trustLevel.asshole")
	};

	public final static Color[] trustLevelColor = {
		Color.BLUE,
		new Color(0, 200, 0), /* light green */
		new Color(0, 150, 0), 
		new Color(0, 80, 0), /* green */
		Color.BLACK,
		new Color(125, 0, 0), /* moderatly red */
		new Color(200, 0, 0),
		new Color(255, 0, 0)
	};

	public final static Color trustLevelColorMe = new Color(127, 127, 255) /* weird color */;


	private Hsqldb db;

	private int id = -1;

	private String nick;


	/* public key (aka Y) */
	private String publicKey;

	/* private key (aka X) */
	private String privateKey;

	private boolean isDup;
	private int trustLevel;


	private String hash;

	private static FrostCrypt frostCrypt;


	protected Identity() { }


	/**
	 * If you don't have a value, let it to null and pray it won't be used :P
	 * @param nick part *before* the @
	 */
	public Identity(Hsqldb db, int id, String nick,
			String publicKey, String privateKey,
			boolean isDup,
			int trustLevel) {

		if (nick == null || publicKey == null) {
			Logger.error(this, "missing value ?!");

			if (nick == null)
				Logger.error(this, "nick missing");
			if (publicKey == null)
				Logger.error(this, "publicKey missing");
		}

		this.db = db;
		this.id = id;
		this.nick = nick;
		this.publicKey = publicKey;
		this.privateKey = privateKey;
		this.isDup = isDup;
		this.trustLevel = trustLevel;

		//hash = Base64.encode(SHA256.digest(publicKey.getBytes("UTF-8")));
		initFrostCrypt();

		hash = frostCrypt.digest(publicKey);
	}
	
	protected void setDb(Hsqldb db) {
		this.db = db;
	}
	
	public Hsqldb getDb() {
		return db;
	}

	protected void setId(int id) {
		this.id = id;
	}

	private static void initFrostCrypt() {
		if (frostCrypt == null)
			frostCrypt = new FrostCrypt();
	}

	public int getId() {
		return id;
	}

	public String getNick() {
		return nick;
	}

	public String getPublicKey() {
		return publicKey;
	}

	public String getPrivateKey() {
		return privateKey;
	}

	public int getTrustLevel() {
		return trustLevel;
	}

	public static int getTrustLevel(String str) {
		int i;

		for (i = 0 ; i < trustLevelStr.length ; i++) {
			if (trustLevelStr[i].equals(str))
				return trustLevelInt[i];
		}

		return 0;

	}

	public String getTrustLevelStr() {
		if (privateKey != null) {
			return I18n.getMessage("thaw.plugin.signature.trustLevel.me");
		}

		return getTrustLevelStr(getTrustLevel());
	}

	public static String getTrustLevelStr(int trustLevel) {
		int i;

		for (i = 0 ; i < trustLevelInt.length ; i++) {
			if (trustLevelInt[i] == trustLevel)
				return trustLevelStr[i];
		}

		return "[?]";
	}

	public boolean isDup() {
		if (privateKey != null)
			return false;

		return isDup;
	}


	public Color getTrustLevelColor() {
		int i;

		if (privateKey != null)
			return trustLevelColorMe;

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
			if (Identity.trustLevelStr[i].equals(str))
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

			trustLevel = i;
			
			Signatures.notifyIdentityUpdated(this);

		} catch(SQLException e) {
			Logger.error(this, "Unable to change trust level because: "+e.toString());
			e.printStackTrace();
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
			e.printStackTrace();
		}
	}


	public boolean mustBeIgnored(Config config) {
		if (privateKey != null)
			return false;

		int min = Integer.parseInt(config.getValue("minTrustLevel"));

		return (trustLevel < min);
	}


	/**
	 * if the identity doesn't exists, it will be created
	 */
	public static Identity getIdentity(Hsqldb db,
					   String nick,
					   String publicKey) {
		return getIdentity(db, nick, publicKey, true);
	}

	public static Identity getIdentity(Hsqldb db,
					   String nick,
					   String publicKey,
					   boolean create) {
		if (nick == null || publicKey == null)
			return null;
		
		try {
			synchronized(db.dbLock) {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("SELECT id, nickName, publicKey, "+
									 "privateKey, isDup, trustLevel "+
									 "FROM signatures "+
									 "WHERE publicKey = ? LIMIT 1");
				st.setString(1, publicKey);

				ResultSet set = st.executeQuery();

				if (set.next()) {
					Identity i = new Identity(db, set.getInt("id"), set.getString("nickName"),
								  set.getString("publicKey"), set.getString("privateKey"),
								  set.getBoolean("isDup"), set.getInt("trustLevel"));
					Logger.debug(i, "Identity found");
					return i;
				}

				if (!create)
					return null;

				/* else we must add it, but first we need to know if it's a dup */

				st = db.getConnection().prepareStatement("SELECT id FROM signatures "+
									 "WHERE lower(nickName) = ? LIMIT 1");
				st.setString(1, nick.toLowerCase());

				set = st.executeQuery();

				boolean isDup = set.next();

				/* and we add */

				st = db.getConnection().prepareStatement("INSERT INTO signatures "+
									 "(nickName, publicKey, privateKey, isDup, trustLevel) "+
									 "VALUES (?, ?, ?, ?, 0)");

				st.setString(1, nick);
				st.setString(2, publicKey);
				st.setNull(3, Types.VARCHAR);
				st.setBoolean(4, isDup);

				st.execute();


				/* and next we find back the id */

				st = db.getConnection().prepareStatement("SELECT id "+
									 "FROM signatures "+
									 "WHERE publicKey = ? LIMIT 1");
				st.setString(1, publicKey);

				set = st.executeQuery();

				set.next();

				int id = set.getInt("id");

				Identity i = new Identity(db, id, nick, publicKey, null, isDup, 0);
				Logger.info(i, "New identity found");
				
				Signatures.notifyPublicIdentityAdded(i);
				
				return i;

			}
		} catch(SQLException e) {
			Logger.error(new Identity(), "Error while getting identity (2) : "+e.toString());
			e.printStackTrace();
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

				st = db.getConnection().prepareStatement("SELECT id, nickName, publicKey, "+
									 "privateKey, isDup, trustLevel "+
									 "FROM signatures "+
									 "WHERE id = ? LIMIT 1");
				st.setInt(1, id);

				ResultSet set = st.executeQuery();

				if (!set.next())
					return null;

				i = new Identity(db, id, set.getString("nickName"),
						 set.getString("publicKey"), set.getString("privateKey"),
						 set.getBoolean("isDup"), set.getInt("trustLevel"));
			}
		} catch(SQLException e) {
			Logger.error(new Identity(), "Error while getting identity (1) : "+e.toString());
			e.printStackTrace();
		}

		return i;
	}


	/**
	 * Generate a new identity
	 * you have to insert() it after
	 * @param db just here to fill in the class
	 */
	public static Identity generate(Hsqldb db, String nick) {
		Logger.info(null, "thaw.plugins.signatures.Identity : Generating new identity ...");

		initFrostCrypt();

		String[] keys = frostCrypt.generateKeys();

		Identity identity = new Identity(db, -1, nick,
						 keys[1], /* public */
						 keys[0], /* private */
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

				st = db.getConnection().prepareStatement("SELECT id FROM signatures "+
									 "WHERE publicKey = ? LIMIT 1");
				st.setString(1, publicKey);
				st.execute();

				ResultSet set = st.executeQuery();

				if (set.next()) {
					int id = set.getInt("id");

					st = db.getConnection().prepareStatement("UPDATE signatures SET "+
										 "privateKey = ?, trustLevel = ? "+
										 "WHERE id = ?");
					st.setString(1, privateKey);
					st.setInt(2, trustLevel);
					st.setInt(3, id);

					st.execute();
					
					Signatures.notifyIdentityUpdated(this);
				} else {

					st = db.getConnection().prepareStatement("INSERT INTO signatures "+
										 "(nickName, publicKey, privateKey, "+
										 "isDup, trustLevel) "+
										 "VALUES (?, ?, ?, ?, ?)");
					st.setString(1, nick);
					st.setString(2, publicKey);
					st.setString(3, privateKey);
					st.setBoolean(4, isDup);
					st.setInt(5, trustLevel);

					st.execute();
					
					if (privateKey == null)
						Signatures.notifyPublicIdentityAdded(this);
					else
						Signatures.notifyPrivateIdentityAdded(this);
				}
			}
		} catch(SQLException e) {
			Logger.error(this, "Exception while adding the identity to the bdd: "+e.toString());
			e.printStackTrace();
		}
	}
	
	
	public static boolean hasAtLeastATrustDefined(Hsqldb db) {
		try {
			synchronized(db.dbLock) {
				PreparedStatement st = db.getConnection().prepareStatement("SELECT id FROM signatures WHERE trustLevel != ? and trustLevel < ?");
				st.setInt(1, 0);
				st.setInt(2, trustLevelInt[0]);
				
				ResultSet set = st.executeQuery();

				return set.next();				
			}			
		} catch(SQLException e) {
			Logger.error(new Identity(), "Exception while accessing the signature table : "+e.toString());
			e.printStackTrace();
		}
		
		return false;
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
			e.printStackTrace();
			new thaw.gui.WarningWindow((thaw.core.MainWindow)null,
						   I18n.getMessage("thaw.plugin.signature.delete.cant"));
		}

	}


	public String sign(String text) {
		initFrostCrypt();

		return frostCrypt.detachedSign(text, privateKey);
	}


	public static String sign(String text, String privateKey) {
		initFrostCrypt();

		return frostCrypt.detachedSign(text, privateKey);
	}


	public boolean check(String text, String sig) {
		try {
			initFrostCrypt();
			return frostCrypt.detachedVerify(text, publicKey, sig);
		} catch(Exception e) {
			Logger.info(this, "Exception while checking signature: "+e.toString());
			//e.printStackTrace();
			return false;
		}
	}


	public static boolean check(String text, /* signed text */
				    String sig,
				    String publicKey) /* y */ {
		initFrostCrypt();
		return frostCrypt.detachedVerify(text, publicKey, sig);
	}


	public String toString() {
		String n = nick;

		if (n.indexOf('@') >= 0)
			n.replaceAll("@", "_");

		return n+"@"+hash;
	}


	public static Vector getIdentities(Hsqldb db, String cond) {
		try {
			synchronized(db.dbLock) {
				Vector v = new Vector();

				PreparedStatement st;

				if (cond != null)
					st = db.getConnection().prepareStatement("SELECT id, nickName, publicKey, "+
										 "privateKey, isDup, trustLevel "+
										 "FROM signatures "+
										 "WHERE "+cond + " "+
										 "ORDER BY LOWER(nickName)");
				else
					st = db.getConnection().prepareStatement("SELECT id, nickName, publicKey, "+
										 "privateKey, isDup, trustLevel "+
										 "FROM signatures ORDER BY LOWER(nickName)");

				ResultSet set = st.executeQuery();

				while(set.next()) {
					v.add(new Identity(db,
							   set.getInt("id"),
							   set.getString("nickName"),
							   set.getString("publicKey"),
							   set.getString("privateKey"),
							   set.getBoolean("isDup"),
							   set.getInt("trustLevel")));
				}

				return v;
			}
		} catch(SQLException e) {
			Logger.error(new Identity(), "Error while getting identities (1): "+e.toString());
			e.printStackTrace();
		}

		return null;
	}


	public static Vector getYourIdentities(Hsqldb db) {
		return getIdentities(db, "privateKey IS NOT NULL");
	}


	public static Vector getOtherIdentities(Hsqldb db) {
		return getIdentities(db, "privateKey IS NULL");
	}


	public Element makeCDATA(Document doc, String tagName, String content) {
		if (content == null || tagName == null)
			return null;

		CDATASection cdata;
		Element current;

		current = doc.createElement(tagName);
		cdata = doc.createCDATASection(content);
		current.appendChild(cdata);

		return current;
	}

	/**
	 * Frost format
	 * @param file
	 * @return
	 */
	public boolean exportIdentity(File file) {

		Document doc = XMLTools.createDomDocument();

		Element root = doc.createElement("FrostLocalIdentities");
		Element identityEl = doc.createElement("MyIdentity");

		identityEl.appendChild(makeCDATA(doc, "name", toString()));
		identityEl.appendChild(makeCDATA(doc, "key", publicKey));
		
		if (privateKey != null)
			identityEl.appendChild(makeCDATA(doc, "privKey", privateKey));

		root.appendChild(identityEl);
		doc.appendChild(root);

		return XMLTools.writeXmlFile(doc, file.getPath());
	}


	public byte[] decode(byte[] input) {
		initFrostCrypt();

		try {
			return frostCrypt.decrypt(input, privateKey);
		} catch(Exception e) {
			Logger.info(this, "hm, '"+e.toString()+"' => probably not for us ("+toString()+")");
			e.printStackTrace();
		}

		return null;
	}
	
	public byte[] encode(byte[] input) {
		initFrostCrypt();
		
		try {
			return frostCrypt.encrypt(input, publicKey);
		} catch(Exception e) {
			Logger.error(this, "Can't crypt message because : '"+e.toString()+"'");
			e.printStackTrace();
		}
		
		return null;
	}

	/**
	 * Frost format
	 * @param db
	 * @param file
	 * @return Vector<Identity>
	 */
	public static Vector importIdentity(Hsqldb db, File file) {
		Vector ids = new Vector();
		
		try {
			Document doc = null;
			try {
				doc = XMLTools.parseXmlFile(file, false);
			} catch(Exception ex) {  // xml format error
				Logger.error(ex, "Invalid Xml");
				return null;
			}

			if( doc == null ) {
				Logger.error(null,
					       "Error: couldn't parse XML Document - " +
					       "File name: '" + file.getName() + "'");
				return null;
			}

			Element rootEl = doc.getDocumentElement();

			List l = XMLTools.getChildElementsByTagName(rootEl, "MyIdentity");

			if (l == null) {
				Logger.error(null, "No identity to import");
				return ids;
			}

			for (Iterator it = l.iterator();
			     it.hasNext();) {
				Element identityEl = (Element)it.next();

				String[] split = XMLTools.getChildElementsCDATAValue(identityEl, "name").split("@");
				String nick = split[0];
				String publicKey = XMLTools.getChildElementsCDATAValue(identityEl, "key");
				String privateKey = XMLTools.getChildElementsCDATAValue(identityEl, "privKey");


				Identity identity = new Identity(db, -1, nick,
								 publicKey, privateKey, false,
								 10);
				identity.insert();
				
				ids.add(identity);
			}

		} catch(Exception e) {
			/* XMLTools throws runtime exception sometimes ... */
			Logger.error(e, "Unable to parse XML message because : "+e.toString());
			e.printStackTrace();
			return null;
		}

		return ids;
	}
	
	public boolean equals(Object o) {
		if (o == null)
			return false;
		
		if (!(o instanceof Identity))
			return false;
		
		if (getId() < 0)
			return false;
		
		return (getId() == ((Identity)o).getId());
	}
}

