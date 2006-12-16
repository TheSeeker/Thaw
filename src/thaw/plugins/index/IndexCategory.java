package thaw.plugins.index;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

import thaw.core.Logger;
import thaw.fcp.FCPQueueManager;
import thaw.plugins.Hsqldb;

public class IndexCategory extends DefaultMutableTreeNode implements IndexTreeNode {

	private static final long serialVersionUID = 1L;
	private int id;
	private IndexCategory parent;
	private String name;

	private Hsqldb db;
	private FCPQueueManager queueManager;
	private IndexBrowserPanel indexBrowser;

	public IndexCategory(final FCPQueueManager queueManager, final IndexBrowserPanel indexBrowser,
			     final int id, final IndexCategory parent,
			     final String name) {
		super(name, true);

		this.indexBrowser = indexBrowser;
		this.id = id;
		this.name = name;
		this.parent = parent;

		this.db = indexBrowser.getDb();

		this.queueManager = queueManager;

		setUserObject(this);

	}

	public void setParent(final IndexCategory parent) {
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

			final Connection c = db.getConnection();
			PreparedStatement st;

			st = c.prepareStatement("SELECT id FROM indexCategories ORDER BY id DESC LIMIT 1");
			st.execute();

			try {
				final ResultSet key = st.getResultSet();
				key.next();
				id = key.getInt(1) + 1;
			} catch(final SQLException e) {
				id = 0;
			}

			st = c.prepareStatement("INSERT INTO indexCategories (id, name, positionInTree, parent, modifiableIndexes) "+
								  "VALUES (?, ?,?,?, true)");

			st.setInt(1, id);
			st.setString(2, name);
			st.setInt(3, 0);


			if(parent.getId() >= 0)
				st.setInt(4, parent.getId());
			else
				st.setNull(4, Types.INTEGER);

			st.execute();

			return true;
		} catch(final SQLException e) {
			Logger.error(this, "Unable to insert the new index category in the db, because: "+e.toString());

			return false;
		}
	}

	public void delete() {
		if (id < 0) /* Operation not allowed */
			return;

		if(children == null)
			children = loadChildren();

		for(final Iterator it = children.iterator();
		    it.hasNext();) {
			final IndexTreeNode child = (IndexTreeNode)((DefaultMutableTreeNode)it.next()).getUserObject();
			child.delete();
		}

		children = null;

		try {
			final Connection c = db.getConnection();
			final PreparedStatement st = c.prepareStatement("DELETE FROM indexCategories WHERE id = ?");
			st.setInt(1, id);
			st.execute();
		} catch(final SQLException e) {
			Logger.error(this, "Unable to delete the index category '"+name+"', because: "+e.toString());
		}
	}

	public void rename(final String name) {

		try {
			final Connection c = db.getConnection();
			final PreparedStatement st = c.prepareStatement("UPDATE indexCategories SET name = ? WHERE id = ?");

			st.setString(1, name);
			st.setInt(2, id);

			st.execute();

			this.name = name;
		} catch(final SQLException e) {
			Logger.error(this, "Unable to rename the index category '"+this.name+"' in '"+name+"', because: "+e.toString());
		}
	}

	public String getPublicKey() {
		String result = "";

		for(final Iterator it = children.iterator();
		    it.hasNext();) {
			final IndexTreeNode node = (IndexTreeNode)((DefaultMutableTreeNode)it.next()).getUserObject();
			result = result + node.getPublicKey();

			if (it.hasNext())
				result = result + "\n";
		}

		return result;
	}

	public String getPrivateKey() {
		String result = "";

		for(final Iterator it = children.iterator();
		    it.hasNext();) {
			final IndexTreeNode node = (IndexTreeNode)((DefaultMutableTreeNode)it.next()).getUserObject();
			result = result + node.getPrivateKey();

			if (it.hasNext())
				result = result + "\n";
		}

		return result;
	}

	public void addObserver(final java.util.Observer o) {
		if(children == null)
			children = loadChildren();

		for(final Iterator it = children.iterator();
		    it.hasNext();) {
			final IndexTreeNode node = (IndexTreeNode)((DefaultMutableTreeNode)it.next()).getUserObject();
			node.addObserver(o);
		}
	}

	public void update() {
		for (final Enumeration e = children() ; e.hasMoreElements() ;) {
			((IndexTreeNode)((DefaultMutableTreeNode)e.nextElement()).getUserObject()).update();
		}
	}

	public void updateFromFreenet(final int rev) {
		for (final Enumeration e = children() ; e.hasMoreElements() ;) {
			((IndexTreeNode)((DefaultMutableTreeNode)e.nextElement()).getUserObject()).updateFromFreenet(rev);
		}
	}

	public boolean isUpdating() {
		for (final Enumeration e = children() ; e.hasMoreElements() ;) {
			if(((IndexTreeNode)((DefaultMutableTreeNode)e.nextElement())).isUpdating())
				return true;
		}

		return false;
	}

	public void save() {
		saveThis();

		for (final Enumeration e = children() ; e.hasMoreElements() ;) {
			((IndexTreeNode)((DefaultMutableTreeNode)e.nextElement()).getUserObject()).save();
		}
	}


	public void saveThis() {
		if(getId() < 0)
			return;

		try {
			final Connection c = db.getConnection();
			final PreparedStatement st = c.prepareStatement("UPDATE indexCategories SET name = ?, positionInTree = ?, parent = ? WHERE id = ?");

			st.setString(1, name);
			st.setInt(2, getParent().getIndex(this)); /* Index = position here */

			if( ((IndexTreeNode)getParent()).getId() < 0)
				st.setNull(3, Types.INTEGER);
			else
				st.setInt(3, ((IndexTreeNode)getParent()).getId());

			st.setInt(4, getId());

			st.execute();
		} catch(final SQLException e) {
			Logger.error(this, "Unable to save index category state '"+toString()+"' because : "+e.toString());
		}
	}

	public int getId() {
		return id;
	}

	public int getChildNumber() {
		if (children == null)
			children = loadChildren();

		if (children == null)
			return 0;

		return children.size();
	}

	public Vector loadChildren() {
		final Vector children = new Vector();

		Logger.debug(this, "loadChildren()");

		loadChildCategories(children);
		loadChildIndexes(children);

		cleanChildList(children);

		Logger.debug(this, "Children: "+children.size());

		for(final Iterator it = children.iterator();
		    it.hasNext(); ) {
			add((MutableTreeNode)it.next());
		}

		return children;
	}


	public void loadChildIndexes(final Vector children) {

		ResultSet result;

		String query;

		query = "SELECT id, originalName, displayName, publicKey, privateKey, author, positionInTree, revision FROM indexes";

		if(id < 0)
			query = query + " WHERE parent IS NULL";
		else
			query = query + " WHERE parent = "+Integer.toString(id);

		query = query + " ORDER BY positionInTree DESC";

		try {
			result = db.executeQuery(query);

			if(result == null) {
				Logger.warning(this, "Unable to get child indexes for '"+toString()+"'");
				return;
			}

			while(result.next()) {
				final int id = result.getInt("id");
				final int position = result.getInt("positionInTree");

				final String realName = result.getString("originalName");
				final String displayName = result.getString("displayName");

				final String publicKey = result.getString("publicKey");
				final String privateKey = result.getString("privateKey");

				final int revision = result.getInt("revision");

				final String author = result.getString("author");

				final Index index = new Index(queueManager, indexBrowser,
							      id, this,
							      realName, displayName,
							      publicKey, privateKey,
							      revision,
							      author);

				set(children, position, index.getTreeNode());
			}
		} catch (final java.sql.SQLException e) {
			Logger.warning(this, "SQLException while getting child of index category '"+name+"': "+e.toString());
		}

	}

	public void loadChildCategories(final Vector children) {

		ResultSet result;

		String query;

		query = "SELECT id, name, positionInTree FROM indexCategories";

		if(id < 0)
			query = query + " WHERE parent IS NULL";
		else
			query = query + " WHERE parent = "+Integer.toString(id);

		query = query + " ORDER BY positionInTree DESC";

		try {
			result = db.executeQuery(query);

			if(result == null) {
				Logger.error(this, "Unable to get child categories for '"+toString()+"'");
				return;
			}

			while(result.next()) {
				final int id = result.getInt("id");
				final int position = result.getInt("positionInTree");
				final String name = result.getString("name");

				final IndexCategory cat = new IndexCategory(queueManager, indexBrowser, id, this, name);
				cat.loadChildren();
				set(children, position, cat);
			}
		} catch (final java.sql.SQLException e) {
			Logger.error(this, "SQLException while getting child of index category '"+name+"': "+e.toString());
		}

	}

	protected void cleanChildList(final Vector children) {
		while(children.remove(null)) {
			Logger.warning(this, "cleanChildList() : null removed !");
		}
	}

	public void set(final Vector children, final int position, final TreeNode node) {
		if((node == this) || (node == null))
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
		return name;
	}

	public Vector getIndexIds()
	{
		if(children == null)
			children = loadChildren();

		final Vector result = new Vector();

		for(final Iterator it = children.iterator();
		    it.hasNext();) {
			final IndexTreeNode node = (IndexTreeNode)((DefaultMutableTreeNode)it.next()).getUserObject();
			result.addAll(node.getIndexIds());
		}

		return result;
	}

	public Vector getIndexes()
	{
		if(children == null)
			children = loadChildren();

		final Vector result = new Vector();

		for(final Iterator it = children.iterator();
		    it.hasNext();) {
			final IndexTreeNode node = (IndexTreeNode)((DefaultMutableTreeNode)it.next()).getUserObject();
			result.addAll(node.getIndexes());
		}

		return result;
	}

	public Vector getAllIndexCategories()
	{
		if(children == null)
			children = loadChildren();

		final Vector result = new Vector();

		for(final Iterator it = children.iterator();
		    it.hasNext();) {
			final Object node = ((DefaultMutableTreeNode)it.next()).getUserObject();
			if (node instanceof IndexCategory) {
				result.add(node);
				result.addAll(((IndexCategory)node).getAllIndexCategories());
			}
		}

		return result;
	}


	public Index getIndex(final int id)
	{
		if(children == null)
			children = loadChildren();

		for(final Iterator it = children.iterator();
		    it.hasNext();) {
			final IndexTreeNode node = (IndexTreeNode)((DefaultMutableTreeNode)it.next()).getUserObject();
			final Index result = node.getIndex(id);
			if (result != null)
				return result;
		}

		return null;
	}

	/**
	 * Returns true only if all its child are modifiable
	 */
	public boolean isModifiable() {
		if(children == null)
			children = loadChildren();

		if (children.size() <= 0)
			return false;

		for(final Iterator it = children.iterator();
		    it.hasNext();) {
			final IndexTreeNode node = (IndexTreeNode)((DefaultMutableTreeNode)it.next()).getUserObject();
			if (!node.isModifiable())
				return false;
		}

		return true;
	}


	public boolean hasChanged() {
		if(children == null)
			children = loadChildren();

		for(final Iterator it = children.iterator();
		    it.hasNext();) {
			final IndexTreeNode node = (IndexTreeNode)((DefaultMutableTreeNode)it.next()).getUserObject();
			if (node.hasChanged())
				return true;
		}

		return false;
	}


	public boolean isLeaf() {
		return false;
	}

}
