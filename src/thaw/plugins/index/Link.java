package thaw.plugins.index;

import java.sql.*;

import org.w3c.dom.Element;
import org.w3c.dom.Document;
import thaw.core.Logger;
import thaw.plugins.Hsqldb;

public class Link extends java.util.Observable {
	private int id;

	private String indexName = null;
	private String key = null;

	private Index parent = null;
	private int parentId;

	private Hsqldb db;


	public Link(Hsqldb hsqldb, String indexName, String key, Index parent) {
		this.indexName = indexName;
		if (key != null)
			this.key = key.trim();
		else
			this.key = null;
		this.parent = parent;
		this.db = hsqldb;
	}

	public Link(Hsqldb hsqldb, String key, Index parent) {
		this(hsqldb, Index.getNameFromKey(key), key, parent);
	}

	public Link(Hsqldb hsqldb, ResultSet resultSet, Index parent) throws SQLException {
		this.db = hsqldb;
		this.id = resultSet.getInt("id");
		this.key = resultSet.getString("publicKey").trim();

		this.indexName = Index.getNameFromKey(this.key);

		this.parent = parent;
		if (parent != null)
			this.parentId = parent.getId();
		else
			this.parentId = resultSet.getInt("indexParent");
	}

	public Link(Hsqldb hsqldb, Element linkElement, Index parent) {
		this.db = hsqldb;
		this.key = linkElement.getAttribute("key");

		if (this.key != null)
			this.key = this.key.trim();

		this.indexName = Index.getNameFromKey(this.key);

		this.parent = parent;

		if (parent != null)
			parentId = parent.getId();
		else
			parentId = -1;
	}

	public String getPublicKey() {
		return key;
	}

	public boolean compare(Link l) {
		if (l == null
		    || getPublicKey() == null
		    || l.getPublicKey() == null
		    || getPublicKey().length() < 40
		    || l.getPublicKey().length() < 40)
			return false;

		return (l.getPublicKey().substring(4, 40).equals(getPublicKey().substring(4, 40)));
	}

	public boolean compare(Index l) {
		if (l == null
		    || getPublicKey() == null
		    || l.getPublicKey() == null
		    || getPublicKey().length() < 40
		    || l.getPublicKey().length() < 40)
			return false;

		return (l.getPublicKey().substring(4, 40).equals(getPublicKey().substring(4, 40)));
	}

	public void setParent(Index index) {
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

	public void setIndexKey(String key) {
		this.key = key;

		if (this.key != null)
			this.key = this.key.trim();

		this.setChanged();
		this.notifyObservers();
	}

	public void insert() {
		try {
			PreparedStatement st;

			db.lockWriting();

			st = this.db.getConnection().prepareStatement("SELECT id FROM links ORDER BY id DESC LIMIT 1");

			try {
				if(st.execute()) {
					ResultSet result = st.getResultSet();
					result.next();
					this.id = result.getInt("id")+1;
				} else
					this.id = 1;
			} catch(SQLException e) {
				this.id = 1;
			}

			st = this.db.getConnection().prepareStatement("INSERT INTO links (id, publicKey, "+
								 "mark, comment, indexParent, indexTarget) "+
								 "VALUES (?, ?, ?, ?, ?, ?)");
			st.setInt(1, this.id);

			if(this.key != null)
				st.setString(2, this.key);
			else
				st.setString(2, this.indexName);

			st.setInt(3, 0);
			st.setString(4, "No comment");
			st.setInt(5, this.parent.getId());
			st.setNull(6, Types.INTEGER);

			st.execute();
		} catch(SQLException e) {
			Logger.error(this, "Unable to insert link to '"+this.indexName+"' because: "+e.toString());
		}

		db.unlockWriting();

	}

	public boolean isIndexAlreadyKnown() {
		if (key.length() < 40) {
			Logger.error(this, "Invalid key: "+key);
			return false;
		}

		try {
			PreparedStatement st;

			st = db.getConnection().prepareStatement("SELECT publicKey from indexes WHERE publicKey LIKE ?");

			st.setString(1, "%"+key.substring(3, 40)+"%");

			if(st.execute()) {
				ResultSet result = st.getResultSet();
				if (result != null && result.next()) {
					return true;
				}
			}

		} catch(SQLException e) {
			Logger.error(this, "Unable to check if link '"+key+"' point to a know index because: "+e.toString());
		}

		return false;
	}

	public boolean isInTheDatabase() {
		if (this.parent == null) {
			Logger.notice(this, "isInTheDatabase(): No parent !");
			return false;
		}

		try {
			PreparedStatement st;

			st = this.db.getConnection().prepareStatement("SELECT publicKey from links WHERE publicKey = ? AND indexParent = ?");

			st.setString(1, this.key);

			st.setInt(2, this.getParent().getId());

			if(st.execute()) {
				ResultSet result = st.getResultSet();
				if (result != null && result.next()) {
					return true;
				}
			}

		} catch(SQLException e) {
			Logger.error(this, "Unable to check if link '"+this.key+"' exists because: "+e.toString());
		}

		return false;
	}


	public void delete() {
		try {
			PreparedStatement st;

			st = this.db.getConnection().prepareStatement("DELETE FROM links WHERE id = ?");
			st.setInt(1, this.id);

			st.execute();

		} catch(SQLException e) {
			Logger.error(this, "Unable to remove link to '"+this.indexName+"' because: "+e.toString());
		}
	}

	public void update() {
		try {
			PreparedStatement st;

			st = this.db.getConnection().prepareStatement("UPDATE links SET publicKey = ?, indexParent = ? WHERE id = ?");

			if(this.key != null)
				st.setString(1, this.key);
			else
				st.setString(1, this.indexName);

			st.setInt(2, this.getParent().getId());

			st.setInt(3, this.id);

			st.execute();
		} catch(SQLException e) {
			Logger.error(this, "Unable to update link to '"+this.indexName+"' because: "+e.toString());
		}
	}


	public Element getXML(Document xmlDoc) {
		Element link = xmlDoc.createElement("index");

		link.setAttribute("key", this.key);

		return link;
	}


	public boolean isModifiable() {
		if (getParent() == null)
			return false;
		return getParent().isModifiable();
	}
}
