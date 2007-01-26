package thaw.plugins.index;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import thaw.core.Logger;
import thaw.core.FreenetURIHelper;
import thaw.plugins.Hsqldb;

public class Link extends java.util.Observable {
	private int id;
	private final Hsqldb db;

	private String publicKey;

	private int parentId;
	private Index parent = null;

	public Link(final Hsqldb hsqldb, final int id) {
		this.id = id;
		this.db = hsqldb;

		reloadDataFromDb(id);
	}


	public Link(final Hsqldb hsqldb, final int id, String publicKey,
		    int parentId) {
		this.id = id;
		this.db = hsqldb;
		this.publicKey = publicKey;
		this.parentId = parentId;
	}


	public Link(final Hsqldb hsqldb, final int id, String publicKey,
		    Index parent) {
		this.id = id;
		this.db = hsqldb;
		this.publicKey = publicKey;
		this.parentId = parent.getId();
		this.parent = parent;
	}

	public void reloadDataFromDb(int id) {
		this.id = id;

		try {
			PreparedStatement st;

			st = db.getConnection().prepareStatement("SELECT publicKey, indexParent FROM links "+
								 "WHERE id = ? LIMIT 1");

			st.setInt(1, id);

			ResultSet rs = st.executeQuery();

			if (rs.next()) {
				publicKey = rs.getString("publicKey");
				parentId = rs.getInt("indexParent");
			} else {
				Logger.error(this, "Link '"+Integer.toString(id)+"' not found.");
			}
		} catch(SQLException e) {
			Logger.error(this, "Error while loading data for link '"+Integer.toString(id)+"': "+e.toString());
		}
	}

	public String getPublicKey() {
		if (publicKey == null)
			reloadDataFromDb(id);

		return publicKey;
	}

	public boolean compare(final Link l) {
		String key_a;
		String key_b;

		key_a = getPublicKey();
		key_b = l.getPublicKey();

		if ((l == null)
		    || (key_a == null)
		    || (key_b == null)
		    || (key_a.length() < 40)
		    || (key_b.length() < 40))
			return false;

		key_a = FreenetURIHelper.getComparablePart(key_a);
		key_b = FreenetURIHelper.getComparablePart(key_b);

		return (key_a.equals(key_b));
	}

	public boolean compare(final Index l) {
		String key_a;
		String key_b;

		key_a = getPublicKey();
		key_b = l.getPublicKey();

		if ((l == null)
		    || (getPublicKey() == null)
		    || (l.getPublicKey() == null)
		    || (getPublicKey().length() < 40)
		    || (l.getPublicKey().length() < 40))
			return false;

		key_a = FreenetURIHelper.getComparablePart(key_a);
		key_b = FreenetURIHelper.getComparablePart(key_b);

		return (key_a.equals(key_b));

	}

	public void setParent(final Index index) {
		try {
			PreparedStatement st;

			st = db.getConnection().prepareStatement("UPDATE links SET indexParent = ? "+
								 "WHERE id = ?");
			st.setInt(1, index.getId());
			st.setInt(2, id);
			st.execute();
		} catch(SQLException e) {
			Logger.error(this, "Unable to set parent because: "+e.toString());
		}
	}

	/**
	 * @return the parent index in the tree if known, else null
	 */
	public Index getTreeParent() {
		if (parent != null)
			return parent;

		return null;
	}

	/**
	 * @return the parent index ; build a new one with its id if needed
	 */
	public Index getParent() {
		if (parent != null)
			return parent;

		int parentId;

		parentId = getParentId();

		if (parentId < 0)
			return null;

		return new Index(db, parentId);
	}

	public int getParentId() {
		return parentId;
	}

	public String getIndexName() {
		String name;

		if (publicKey == null)
			reloadDataFromDb(id);

		name = Index.getNameFromKey(publicKey);

		return name;
	}

	/**
	 * return the index name
	 */
	public String toString() {
		return getIndexName();
	}

	public void setPublicKey(final String key) {
		this.publicKey = key;

		try {
			PreparedStatement st;

			st = db.getConnection().prepareStatement("UPDATE links SET publicKey = ? "+
								 "WHERE id = ?");
			st.setString(1, key);
			st.setInt(2, id);
			st.execute();
		} catch(SQLException e) {
			Logger.error(this, "Error while changing publicKey: "+e.toString());
		}
	}

	/**
	 * database related
	 */
	public void delete() {
		try {
			PreparedStatement st;

			st = db.getConnection().prepareStatement("DELETE FROM links WHERE id = ?");
			st.setInt(1, id);

			st.execute();

		} catch(final SQLException e) {
			Logger.error(this, "Unable to remove link because: "+e.toString());
		}
	}



	public Element getXML(final Document xmlDoc) {
		final Element link = xmlDoc.createElement("index");

		link.setAttribute("key", getPublicKey());

		return link;
	}


	/**
	 * do a sql queries !
	 */
	public boolean isModifiable() {
		Index index = getParent();

		if (index == null)
			return false;

		return index.isModifiable();
	}
}
