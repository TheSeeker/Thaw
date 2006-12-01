package thaw.plugins.index;

import java.sql.*;

import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.DefaultMutableTreeNode;

import java.util.Enumeration;
import java.util.Vector;
import java.util.Iterator;

import thaw.fcp.*;
import thaw.plugins.Hsqldb;

import thaw.core.*;

public class IndexCategory extends DefaultMutableTreeNode implements IndexTreeNode {

	private static final long serialVersionUID = 1L;
	private int id;
	private IndexCategory parent;
	private String name;

	private Hsqldb db;
	private FCPQueueManager queueManager;

	public IndexCategory(Hsqldb db, FCPQueueManager queueManager,
			     int id, IndexCategory parent,
			     String name) {
		super(name, true);

		this.id = id;
		this.name = name;
		this.parent = parent;

		this.db = db;
		this.queueManager = queueManager;

		this.setUserObject(this);

	}

	public void setParent(IndexCategory parent) {
		this.parent = parent;
	}

	public DefaultMutableTreeNode getTreeNode() {
		return this;
	}

	/**
	 * Insert the category into the database.
	 */
	public boolean create() {
		try {
			/* Rahh ! Hsqldb doesn't support getGeneratedKeys() ! 8/ */

			Connection c = this.db.getConnection();
			PreparedStatement st;

			st = c.prepareStatement("SELECT id FROM indexCategories ORDER BY id DESC LIMIT 1");
			st.execute();

			try {
				ResultSet key = st.getResultSet();
				key.next();
				this.id = key.getInt(1) + 1;
			} catch(SQLException e) {
				this.id = 0;
			}

			st = c.prepareStatement("INSERT INTO indexCategories (id, name, positionInTree, parent, modifiableIndexes) "+
								  "VALUES (?, ?,?,?, true)");

			st.setInt(1, this.id);
			st.setString(2, this.name);
			st.setInt(3, 0);


			if(this.parent.getId() >= 0)
				st.setInt(4, this.parent.getId());
			else
				st.setNull(4, Types.INTEGER);

			st.execute();

			return true;
		} catch(SQLException e) {
			Logger.error(this, "Unable to insert the new index category in the db, because: "+e.toString());

			return false;
		}
	}

	public void delete() {
		if (this.id < 0) /* Operation not allowed */
			return;

		if(this.children == null)
			this.children = this.loadChildren();

		for(Iterator it = this.children.iterator();
		    it.hasNext();) {
			IndexTreeNode child = (IndexTreeNode)((DefaultMutableTreeNode)it.next()).getUserObject();
			child.delete();
		}

		this.children = null;

		try {
			Connection c = this.db.getConnection();
			PreparedStatement st = c.prepareStatement("DELETE FROM indexCategories WHERE id = ?");
			st.setInt(1, this.id);
			st.execute();
		} catch(SQLException e) {
			Logger.error(this, "Unable to delete the index category '"+this.name+"', because: "+e.toString());
		}
	}

	public void rename(String name) {

		try {
			Connection c = this.db.getConnection();
			PreparedStatement st = c.prepareStatement("UPDATE indexCategories SET name = ? WHERE id = ?");

			st.setString(1, name);
			st.setInt(2, this.id);

			st.execute();

			this.name = name;
		} catch(SQLException e) {
			Logger.error(this, "Unable to rename the index category '"+this.name+"' in '"+name+"', because: "+e.toString());
		}
	}

	public String getPublicKey() {
		String result = "";

		for(Iterator it = this.children.iterator();
		    it.hasNext();) {
			IndexTreeNode node = (IndexTreeNode)((DefaultMutableTreeNode)it.next()).getUserObject();
			result = result + node.getPublicKey();

			if (it.hasNext())
				result = result + "\n";
		}

		return result;
	}

	public String getPrivateKey() {
		String result = "";

		for(Iterator it = this.children.iterator();
		    it.hasNext();) {
			IndexTreeNode node = (IndexTreeNode)((DefaultMutableTreeNode)it.next()).getUserObject();
			result = result + node.getPrivateKey();

			if (it.hasNext())
				result = result + "\n";
		}

		return result;
	}

	public void addObserver(java.util.Observer o) {
		if(this.children == null)
			this.children = this.loadChildren();

		for(Iterator it = this.children.iterator();
		    it.hasNext();) {
			IndexTreeNode node = (IndexTreeNode)((DefaultMutableTreeNode)it.next()).getUserObject();
			node.addObserver(o);
		}
	}

	public void update() {
		for (Enumeration e = this.children() ; e.hasMoreElements() ;) {
			((IndexTreeNode)((DefaultMutableTreeNode)e.nextElement()).getUserObject()).update();
		}
	}

	public void updateFromFreenet(int rev) {
		for (Enumeration e = this.children() ; e.hasMoreElements() ;) {
			((IndexTreeNode)((DefaultMutableTreeNode)e.nextElement()).getUserObject()).updateFromFreenet(rev);
		}
	}

	public boolean isUpdating() {
		for (Enumeration e = this.children() ; e.hasMoreElements() ;) {
			if(((IndexTreeNode)((DefaultMutableTreeNode)e.nextElement())).isUpdating())
				return true;
		}

		return false;
	}

	public void save() {
		this.saveThis();

		for (Enumeration e = this.children() ; e.hasMoreElements() ;) {
			((IndexTreeNode)((DefaultMutableTreeNode)e.nextElement()).getUserObject()).save();
		}
	}


	public void saveThis() {
		if(this.getId() < 0)
			return;

		try {
			Connection c = this.db.getConnection();
			PreparedStatement st = c.prepareStatement("UPDATE indexCategories SET name = ?, positionInTree = ?, parent = ? WHERE id = ?");

			st.setString(1, this.name);
			st.setInt(2, this.getParent().getIndex(this));

			if( ((IndexTreeNode)this.getParent()).getId() < 0)
				st.setNull(3, Types.INTEGER);
			else
				st.setInt(3, ((IndexTreeNode)this.getParent()).getId());

			st.setInt(4, this.getId());

			st.execute();
		} catch(SQLException e) {
			Logger.error(this, "Unable to save index category state '"+this.toString()+"' because : "+e.toString());
		}
	}

	public int getId() {
		return this.id;
	}

	public int getChildNumber() {
		if (children == null)
			children = loadChildren();

		if (children == null)
			return 0;

		return children.size();
	}

	public Vector loadChildren() {
		Vector children = new Vector();

		Logger.debug(this, "loadChildren()");

		this.loadChildCategories(children);
		this.loadChildIndexes(children);

		this.cleanChildList(children);

		Logger.debug(this, "Children: "+children.size());

		for(Iterator it = children.iterator();
		    it.hasNext(); ) {
			this.add((MutableTreeNode)it.next());
		}

		return children;
	}


	public void loadChildIndexes(Vector children) {

		ResultSet result;

		String query;

		query = "SELECT id, originalName, displayName, publicKey, privateKey, author, positionInTree, revision FROM indexes";

		if(this.id < 0)
			query = query + " WHERE parent IS NULL";
		else
			query = query + " WHERE parent = "+Integer.toString(this.id);

		query = query + " ORDER BY positionInTree DESC";

		try {
			result = this.db.executeQuery(query);

			if(result == null) {
				Logger.warning(this, "Unable to get child indexes for '"+this.toString()+"'");
				return;
			}

			while(result.next()) {
				int id = result.getInt("id");
				int position = result.getInt("positionInTree");

				String realName = result.getString("originalName");
				String displayName = result.getString("displayName");

				String publicKey = result.getString("publicKey");
				String privateKey = result.getString("privateKey");

				int revision = result.getInt("revision");

				String author = result.getString("author");

				Index index = new Index(db, queueManager, id, this,
							realName, displayName,
							publicKey, privateKey,
							revision,
							author);

				this.set(children, position, index.getTreeNode());
			}
		} catch (java.sql.SQLException e) {
			Logger.warning(this, "SQLException while getting child of index category '"+this.name+"': "+e.toString());
		}

	}

	public void loadChildCategories(Vector children) {

		ResultSet result;

		String query;

		query = "SELECT id, name, positionInTree FROM indexCategories";

		if(this.id < 0)
			query = query + " WHERE parent IS NULL";
		else
			query = query + " WHERE parent = "+Integer.toString(this.id);

		query = query + " ORDER BY positionInTree DESC";

		try {
			result = this.db.executeQuery(query);

			if(result == null) {
				Logger.error(this, "Unable to get child categories for '"+this.toString()+"'");
				return;
			}

			while(result.next()) {
				int id = result.getInt("id");
				int position = result.getInt("positionInTree");
				String name = result.getString("name");

				IndexCategory cat = new IndexCategory(this.db, this.queueManager, id, this, name);
				cat.loadChildren();
				this.set(children, position, cat);
			}
		} catch (java.sql.SQLException e) {
			Logger.error(this, "SQLException while getting child of index category '"+this.name+"': "+e.toString());
		}

	}

	protected void cleanChildList(Vector children) {
		while(children.remove(null)) {
			Logger.warning(this, "cleanChildList() : null removed !");
		}
	}

	public void set(Vector children, int position, TreeNode node) {
		if(node == this || node == null)
			return;

		if(children.size() <= position) {
			children.setSize(position+1);
		} else {

			if(children.get(position) != null) {
				Logger.warning(this,
					       "Collision in tree position: Moving "+Integer.toString(position)+
					       " to "+Integer.toString(children.size()));
				children.add(node);
				return;
			}
		}

		children.set(position, node);
	}

	public String toString() {
		return this.name;
	}

	public Vector getIndexIds()
	{
		if(this.children == null)
			this.children = this.loadChildren();

		Vector result = new Vector();

		for(Iterator it = this.children.iterator();
		    it.hasNext();) {
			IndexTreeNode node = (IndexTreeNode)((DefaultMutableTreeNode)it.next()).getUserObject();
			result.addAll(node.getIndexIds());
		}

		return result;
	}


	public Index getIndex(int id)
	{
		if(this.children == null)
			this.children = this.loadChildren();

		for(Iterator it = this.children.iterator();
		    it.hasNext();) {
			IndexTreeNode node = (IndexTreeNode)((DefaultMutableTreeNode)it.next()).getUserObject();
			Index result = node.getIndex(id);
			if (result != null)
				return result;
		}

		return null;
	}

	/**
	 * Returns true only if all its child are modifiable
	 */
	public boolean isModifiable()
	{
		if(children == null)
			children = loadChildren();

		for(Iterator it = children.iterator();
		    it.hasNext();) {
			IndexTreeNode node = (IndexTreeNode)((DefaultMutableTreeNode)it.next()).getUserObject();
			if (!node.isModifiable())
				return false;
		}

		return true;
	}


	public boolean isLeaf() {
		return false;
	}

}
