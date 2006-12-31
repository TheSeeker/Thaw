package thaw.plugins.index;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import thaw.core.Logger;
import thaw.plugins.Hsqldb;

public class Link extends java.util.Observable {
	private int id;

	private String indexName;
	private String key;

	private Index parent = null;
	private int parentId;

	private final Hsqldb db;


	public Link(final Hsqldb hsqldb, final String indexName, final String key, final Index parent) {
		this.indexName = indexName;
		if (key != null)
			this.key = key.trim();
		else
			this.key = null;
		this.parent = parent;
		db = hsqldb;
	}

	public Link(final Hsqldb hsqldb, final String key, final Index parent) {
		this(hsqldb, Index.getNameFromKey(key), key, parent);
	}

	public Link(final Hsqldb hsqldb, final ResultSet resultSet, final Index parent) throws SQLException {
		db = hsqldb;
		id = resultSet.getInt("id");
		key = resultSet.getString("publicKey").trim();

		indexName = Index.getNameFromKey(key);

		this.parent = parent;
		if (parent != null)
			parentId = parent.getId();
		else
			parentId = resultSet.getInt("indexParent");
	}

	public Link(final Hsqldb hsqldb, final Element linkElement, final Index parent) {
		db = hsqldb;
		key = linkElement.getAttribute("key");

		if (key != null)
			key = key.trim();

		indexName = Index.getNameFromKey(key);

		this.parent = parent;

		if (parent != null)
			parentId = parent.getId();
		else
			parentId = -1;
	}

	public String getPublicKey() {
		return key;
	}

	public boolean compare(final Link l) {
		if ((l == null)
		    || (getPublicKey() == null)
		    || (l.getPublicKey() == null)
		    || (getPublicKey().length() < 40)
		    || (l.getPublicKey().length() < 40))
			return false;

		return (l.getPublicKey().substring(4, 40).equals(getPublicKey().substring(4, 40)));
	}

	public boolean compare(final Index l) {
		if ((l == null)
		    || (getPublicKey() == null)
		    || (l.getPublicKey() == null)
		    || (getPublicKey().length() < 40)
		    || (l.getPublicKey().length() < 40))
			return false;

		return (l.getPublicKey().substring(4, 40).equals(getPublicKey().substring(4, 40)));
	}

	public void setParent(final Index index) {
		parent = index;
	}

	public Index getParent() {
		return parent;
	}

	public int getParentId() {
		if (parent != null)
			return parent.getId();

		return parentId;
	}

	public String getIndexName() {
		return indexName;
	}

	public String toString() {
		return getIndexName();
	}

	public void setIndexKey(final String key) {
		this.key = key;

		if (this.key != null)
			this.key = this.key.trim();

		setChanged();
		this.notifyObservers();
	}

	public void insert() {
		try {
			PreparedStatement st;

			synchronized (db.dbLock) {
				st = db.getConnection().prepareStatement("SELECT id FROM links ORDER BY id DESC LIMIT 1");

				try {
					if(st.execute()) {
						final ResultSet result = st.getResultSet();
						result.next();
						id = result.getInt("id")+1;
					} else
						id = 1;
				} catch(final SQLException e) {
					id = 1;
				}

				st = db.getConnection().prepareStatement("INSERT INTO links (id, publicKey, "+
						"mark, comment, indexParent, indexTarget) "+
				"VALUES (?, ?, ?, ?, ?, ?)");
				st.setInt(1, id);

				if(key != null)
					st.setString(2, key);
				else
					st.setString(2, indexName);

				st.setInt(3, 0);
				st.setString(4, "No comment");
				st.setInt(5, parent.getId());
				st.setNull(6, Types.INTEGER);

				st.execute();
			}
		} catch(final SQLException e) {
			Logger.error(this, "Unable to insert link to '"+indexName+"' because: "+e.toString());
		}
	}

	public boolean isInTheDatabase() {
		if (parent == null) {
			Logger.notice(this, "isInTheDatabase(): No parent !");
			return false;
		}

		try {
			PreparedStatement st;

			st = db.getConnection().prepareStatement("SELECT publicKey from links WHERE publicKey = ? AND indexParent = ?");

			st.setString(1, key);

			st.setInt(2, getParent().getId());

			if(st.execute()) {
				final ResultSet result = st.getResultSet();
				if ((result != null) && result.next())
					return true;
			}

		} catch(final SQLException e) {
			Logger.error(this, "Unable to check if link '"+key+"' exists because: "+e.toString());
		}

		return false;
	}


	public void delete() {
		try {
			PreparedStatement st;

			st = db.getConnection().prepareStatement("DELETE FROM links WHERE id = ?");
			st.setInt(1, id);

			st.execute();

		} catch(final SQLException e) {
			Logger.error(this, "Unable to remove link to '"+indexName+"' because: "+e.toString());
		}
	}

	public void update() {
		try {
			PreparedStatement st;

			st = db.getConnection().prepareStatement("UPDATE links SET publicKey = ?, indexParent = ? WHERE id = ?");

			if(key != null)
				st.setString(1, key);
			else
				st.setString(1, indexName);

			st.setInt(2, getParent().getId());

			st.setInt(3, id);

			st.execute();
		} catch(final SQLException e) {
			Logger.error(this, "Unable to update link to '"+indexName+"' because: "+e.toString());
		}
	}


	public Element getXML(final Document xmlDoc) {
		final Element link = xmlDoc.createElement("index");

		link.setAttribute("key", key);

		return link;
	}


	public boolean isModifiable() {
		if (getParent() == null)
			return false;
		return getParent().isModifiable();
	}
}
