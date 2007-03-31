package thaw.plugins.index;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Observer;
import java.util.Vector;

import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import thaw.core.Logger;
import thaw.fcp.FCPQueueManager;
import thaw.plugins.Hsqldb;


public class IndexFolder implements IndexTreeNode, MutableTreeNode {

	private static final long serialVersionUID = 2L;

	private Hsqldb db;
	private int id;

	private TreeNode parentNode = null;
	private String name = null;

	private Vector children = null;

	private boolean loadOnTheFly = true;



	public IndexFolder(final Hsqldb db, final int id, boolean loadOnTheFly) {
		this.id = id;
		this.db = db;
		this.loadOnTheFly = loadOnTheFly;
	}

	/**
	 * @param parentNode only required if in a tree
	 */
	public IndexFolder(final Hsqldb db, final int id, TreeNode parentNode, String name, boolean loadOnTheFly) {
		this(db, id, loadOnTheFly);

		this.parentNode = parentNode;
		this.name = name;
	}


	protected Hsqldb getDb() {
		return db;
	}


	public boolean isInTree() {
		return (parentNode != null);
	}

	protected void addToVector(Vector v, ResultSet set, boolean folder) throws SQLException {
		if (set == null)
			return;

		while(set.next()) {
			IndexTreeNode n;

			if (folder) {
				n = new IndexFolder(db, set.getInt("id"), this, set.getString("name"), loadOnTheFly);
				if (!loadOnTheFly) /* => load immediatly */
					((IndexFolder)n).loadChildren();
			}
			else
				n = new Index(db, set.getInt("id"), this, set.getString("publicKey"),
					      set.getInt("revision"), set.getString("privateKey"),
					      set.getString("displayName"), set.getBoolean("newRev"));

			int pos = set.getInt("positionInTree");

			if (v.size() <= pos) {
				v.setSize(pos+1);
			}

			if (pos >= 0 && v.get(pos) == null)
				v.set(pos, n);
			else
				v.add(n);
		}

	}


	/** TREENODE **/

	public Enumeration children() {
		if (children == null)
			loadChildren();

		synchronized(children) {
			return children.elements();
		}
	}


	public boolean loadChildren() {
		Logger.info(this, "Loading child for folder "+Integer.toString(id)+"...");

		Vector v = new Vector();

		synchronized(db.dbLock) {

			try {
				/* category first */
				PreparedStatement st;

				if (id >= 0) {
					st = db.getConnection().prepareStatement("SELECT id, name, positionInTree FROM indexFolders "
										 + "WHERE parent = ? "
										 +"ORDER BY positionInTree");
					st.setInt(1, id);
				} else {
					st = db.getConnection().prepareStatement("SELECT id, positionInTree, name FROM indexFolders "
										 + "WHERE parent IS NULL "
										 +"ORDER BY positionInTree");
				}

				addToVector(v, st.executeQuery(), true);


				if (id >= 0) {
					st = db.getConnection().prepareStatement("SELECT id, positionInTree, displayName, publicKey, privateKey, revision, newRev FROM indexes "
										 + "WHERE parent = ? ORDER BY positionInTree");
					st.setInt(1, id);
				} else {
					st = db.getConnection().prepareStatement("SELECT id, positionInTree, displayName, publicKey, privateKey, revision, newRev FROM indexes "
										 + "WHERE parent IS NULL ORDER BY positionInTree");
				}

				addToVector(v, st.executeQuery(), false);

			} catch (SQLException e) {
				Logger.error(this, "Unable to load child list because: "+e.toString());
			}
		}

		while(v.remove(null)) { }

		children = v;

		return true;
	}


	protected Vector getChildren() {
		return children;
	}


	public void unloadChildren() {
		children = null;
	}


	public boolean forceReload() {
		Logger.info(this, "forceReload()");
		return loadChildren();
	}


	public boolean getAllowsChildren() {
		return true;
	}



	public TreeNode getChildAt(int childIndex) {
		if (children == null)
			loadChildren();

		synchronized(children) {
			try {
				return (TreeNode)children.get(childIndex);
			} catch(ArrayIndexOutOfBoundsException e) {
				Logger.error(this, "Huho : ArrayIndexOutOfBoundsException ... :/");
				Logger.error(this, e.toString());
				return null;
			}
		}
	}


	public int getChildCount() {
		/* we use systematically this solution because else we can have problems with
		 * gap / other-not-funny-things-to-manage
		 */

		if (children == null)
			loadChildren();

		synchronized(children) {
			return children.size();
		}
	}


	/**
	 * position
	 */
	public int getIndex(TreeNode node) {
		Logger.info(this, "getIndex()");

		synchronized(db.dbLock) {
			try {
				IndexTreeNode n = (IndexTreeNode)node;

				PreparedStatement st =
					db.getConnection().prepareStatement("SELECT positionInTree FROM "+
									    ((n instanceof Index) ? "indexes" : "indexFolders")
									    + " WHERE id = ? LIMIT 1");

				st.setInt(1, n.getId());

				ResultSet set = st.executeQuery();

				if (set.next())
					return set.getInt("positionInTree");

				return -1;

			} catch(SQLException e) {
				Logger.error(this, "Unable to find position because: "+e.toString());
			}
		}

		return -1;
	}

	public TreeNode getParent() {
		return parentNode;
	}

	public boolean isLeaf() {
		return false;
	}


	/**
	 * entry point
	 * the target child must be in the database
	 */
	public void insert(MutableTreeNode child, int index) {
		Logger.info(this, "Inserting node at "+Integer.toString(index)+" in node "+Integer.toString(id)+" ("+toString()+")");

		if (children != null) {

			synchronized(children) {
				if (index < children.size())
					children.add(index, child);
				else
					children.add(child);

				while(children.remove(null)) {};
			}
		}

		child.setParent(this);

		synchronized(db.dbLock) {

			Logger.info(this, "moving other nodes ...");

			try {
				PreparedStatement st;

				if (id >= 0) {
					st = db.getConnection().prepareStatement("UPDATE indexes "+
										 "SET positionInTree = positionInTree + 1 "+
										 "WHERE parent = ? AND positionInTree >= ?");
					st.setInt(1, id);
					st.setInt(2, index);
					st.execute();

					st = db.getConnection().prepareStatement("UPDATE indexFolders "+
										 "SET positionInTree = positionInTree + 1 "+
										 "WHERE parent = ? AND positionInTree >= ?");
					st.setInt(1, id);
					st.setInt(2, index);
					st.execute();

					st = db.getConnection().prepareStatement("UPDATE "+
										 ((child instanceof IndexFolder) ? "indexFolders" : "indexes")+
										 " SET parent = ?, positionInTree = ?"+
										 "WHERE id = ?");
					st.setInt(1, id);
					st.setInt(2, index);
					st.setInt(3, ((IndexTreeNode)child).getId());
					st.execute();
				} else {
					st = db.getConnection().prepareStatement("UPDATE indexes "+
										 "SET positionInTree = positionInTree + 1 "+
										 "WHERE parent IS NULL AND positionInTree >= ?");
					st.setInt(1, index);
					st.execute();

					st = db.getConnection().prepareStatement("UPDATE indexFolders "+
										 "SET positionInTree = positionInTree + 1 "+
										 "WHERE parent IS NULL AND positionInTree >= ?");
					st.setInt(1, index);
					st.execute();

					st = db.getConnection().prepareStatement("UPDATE "+
										 ((child instanceof IndexFolder) ? "indexFolders" : "indexes")+
										 " SET parent = ?, positionInTree = ?"+
										 "WHERE id = ?");
					st.setNull(1, Types.INTEGER);
					st.setInt(2, index);
					st.setInt(3, ((IndexTreeNode)child).getId());
					st.execute();

				}


			} catch(SQLException e) {
				Logger.error(this, "Error while inserting node: "+e.toString());
			}
		}
	}



	public void remove(int target_id, int pos, boolean index) {
		if (children != null) {
			synchronized(children) {
				IndexTreeNode node;

				if (index)
					node = new Index(db, target_id);
				else
					node = new IndexFolder(db, target_id, false);

				children.remove(node);
			}
		}

		if (pos < 0) {
			Logger.error(this, "invalid position in tree ?!!!");
			pos = 0;
		}


		Logger.info(this, "Removing obj pos "+Integer.toString(pos));

		synchronized(db.dbLock) {
			try {
				PreparedStatement st;

				if (id >= 0) {
					st = db.getConnection().prepareStatement("UPDATE indexes "+
										 "SET positionInTree = positionInTree - 1 "+
										 "WHERE parent = ? AND positionInTree > ?");
					st.setInt(1, id);
					st.setInt(2, pos);
					st.execute();

					st = db.getConnection().prepareStatement("UPDATE indexFolders "+
										 "SET positionInTree = positionInTree - 1 "+
										 "WHERE parent = ? AND positionInTree > ?");
					st.setInt(1, id);
					st.setInt(2, pos);
					st.execute();

				} else {
					st = db.getConnection().prepareStatement("UPDATE indexes "+
										 "SET positionInTree = positionInTree - 1 "+
										 "WHERE parent IS NULL AND positionInTree > ?");
					st.setInt(1, pos);
					st.execute();

					st = db.getConnection().prepareStatement("UPDATE indexFolders "+
										 "SET positionInTree = positionInTree - 1 "+
										 "WHERE parent IS NULL AND positionInTree > ?");
					st.setInt(1, pos);
					st.execute();
				}

			} catch(SQLException e) {
				Logger.error(this,
					     "Error while removing node at the position "+Integer.toString(pos)+
					     " : "+e.toString());
			}
		}
	}


	public void remove(MutableTreeNode n) {
		IndexTreeNode node = (IndexTreeNode)n;

		int t_id, pos;
		boolean index;

		synchronized(db.dbLock) {
			try {
				PreparedStatement st;
				int id;

				st = db.getConnection().prepareStatement("SELECT positionInTree FROM "+
									 ((n instanceof Index) ? "indexes" : "indexFolders")+
									 " WHERE id = ?");

				st.setInt(1, node.getId());

				ResultSet set = st.executeQuery();

				if (set.next()) {
					t_id = node.getId();
					pos = set.getInt("positionInTree");
					index = n instanceof Index;
				} else {
					Logger.error(this, "Node not found !");
					return;
				}

			} catch(SQLException e) {
				Logger.error(this, "Unable to remove node "+Integer.toString(node.getId())+" because: "+
					     e.toString());
				return;
			}
		}

		remove(t_id, pos, index);
	}

	public void remove(int pos) {
		remove((MutableTreeNode)getChildAt(pos));
	}


	/**
	 * entry point
	 */
	public void removeFromParent() {
		if (id < 0) {
			Logger.error(this, "removeFromParent() : We are root ?!");
			return;
		}

		synchronized(db.dbLock) {
			try {
				PreparedStatement st;


				st = db.getConnection().prepareStatement("DELETE FROM indexParents "+
									 "WHERE (indexParents.indexId IN "+
									 " (SELECT indexParents.indexId FROM indexParents "+
									 "  WHERE indexParents.folderId = ?)) "+
									 "AND ((indexParents.folderId IN "+
									 "  (SELECT folderParents.parentId FROM folderParents "+
									 "   WHERE folderParents.folderId = ?)) "+
									 " OR (indexParents.folderId IS NULL))");

				st.setInt(1, id);
				st.setInt(2, id);
				st.execute();

				st = db.getConnection().prepareStatement("DELETE FROM folderParents "+
									 "WHERE ( ((folderId IN "+
									 " (SELECT folderId FROM folderParents "+
									 "  WHERE parentId = ?)) OR "+
									 "  (folderId = ?)) "+
									 "AND ((parentId IN "+
									 " (SELECT parentId FROM folderParents "+
									 "  WHERE folderId = ?)) "+
									 "  OR (parentId IS NULL) ) )");

				st.setInt(1, id);
				st.setInt(2, id);
				st.setInt(3, id);

				st.execute();

			} catch(SQLException e) {
				Logger.error(this, "Error while removing from parent: "+e.toString());
			}
		}

		((IndexFolder)parentNode).remove(this);
	}


	public void setParent(MutableTreeNode newParent) {
		this.parentNode = newParent;
		setParent(((IndexTreeNode)newParent).getId());
	}


	public void setUserObject(Object object) {
		rename(object.toString());
	}


	/** /TREENODE **/


	public MutableTreeNode getTreeNode() {
		return this;
	}


	public void setParent(int parentId) {
		Logger.info(this, "setParent(id)");

		if (id < 0) {
			Logger.error(this, "setParent(): Hu ? we are root, we can't have a new parent ?!");
			return;
		}

		synchronized(db.dbLock) {

			try {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("UPDATE indexFolders "+
									 "SET parent = ? "+
									 "WHERE id = ?");

				if (parentId < 0)
					st.setNull(1, Types.INTEGER);
				else
					st.setInt(1, parentId);

				st.setInt(2, id);

				st.execute();


				/* put all its child folders into its new parents */

				/* all its children:
				 * SELECT folderId FROM folderParents WHERE parentId = IT
				 */
				/* all the parent of its parent:
				 * SELECT parentId FROM folderParents WHERE folderId = <parentId>
				 */

				/* we put all its children in the parents of the parent folder */
				if (parentId >= 0) {
					st = db.getConnection().prepareStatement("INSERT INTO folderParents (folderId, parentId) "+
										 "SELECT a.folderId, b.parentId FROM "+
										 "  folderParents AS a JOIN folderParents AS b ON (a.parentId = ?) WHERE b.folderId = ?");
					st.setInt(1, id);
					st.setInt(2, parentId);
					st.execute();
				} /* else no parent of the parent */


				/* we put all its children in its parent */
				st = db.getConnection().prepareStatement("INSERT INTO folderParents (folderId, parentId) "+
									 " SELECT folderId, ? FROM folderParents "+
									 "  WHERE parentId = ?");
				if (parentId >= 0)
					st.setInt(1, parentId);
				else
					st.setNull(1, Types.INTEGER);
				st.setInt(2, id);
				st.execute();

				/* we put itself in the parents of its parent */
				if (parentId >= 0) {
					st = db.getConnection().prepareStatement("INSERT INTO folderParents (folderId, parentId) "+
										 " SELECT ?, parentId FROM folderParents "+
										 " WHERE folderId = ?");
					st.setInt(1, id);
					st.setInt(2, parentId);
					st.execute();
				}

				/* and then in its parent */
				st = db.getConnection().prepareStatement("INSERT INTO folderParents (folderId, parentId) "+
									 " VALUES (?, ?)");
				st.setInt(1, id);
				if (parentId >= 0)
					st.setInt(2, parentId);
				else
					st.setNull(2, Types.INTEGER);
				st.execute();


				st = db.getConnection().prepareStatement("INSERT INTO indexParents (indexId, folderId) "+
									 "SELECT indexParents.indexId, folderParents.parentId "+
									 "FROM indexParents JOIN folderParents ON "+
									 " indexParents.folderId = folderParents.folderId "+
									 " WHERE folderParents.folderId = ?");
				st.setInt(1, id);
				st.execute();
			} catch(SQLException e) {
				Logger.error(this, "Unable to change parent because: "+e.toString());
			}

		}
	}

	/**
	 * get Id of this node in the database.
	 */
	public int getId() {
		return id;
	}


	/**
	 * Change the name of the node.
	 */
	public void rename(String name) {
		if (id < 0) {
			Logger.error(this, "Can't rename the root node !");
			return;
		}

		this.name = name;

		synchronized(db.dbLock) {
			try {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("UPDATE indexFolders "+
									 "SET name = ? "+
									 "WHERE id = ?");

				st.setString(1, name);
				st.setInt(2, id);

				st.execute();
			} catch(SQLException e) {
				Logger.error(this, "Error while renaming the folder: "+e.toString());
			}
		}
	}


	/**
	 * no choice :/
	 * else we have troubles with the Foreign keys :/
	 */
	private void deleteChildFoldersRecursivly(int id) throws SQLException {
		Vector children = new Vector();

		PreparedStatement st =
			db.getConnection().prepareStatement("SELECT indexFolders.id FROM indexFolders where indexfolders.parent = ?");
		st.setInt(1, id);

		ResultSet set = st.executeQuery();

		while(set.next()) {
			children.add(new Integer(set.getInt("id")));
		}

		st = db.getConnection().prepareStatement("DELETE from indexfolders where id = ?");

		for (Iterator it = children.iterator();
		     it.hasNext();) {
			Integer nextId = (Integer)it.next();
			deleteChildFoldersRecursivly(nextId.intValue());
			st.setInt(1, nextId.intValue());
			st.execute();
		}
	}



	/**
	 * Entry point
	 * Remove the node from the database.<br/>
	 * Remark: Parent node must be set !
	 */
	public void delete() {
		if (id < 0) {
			Logger.error(this, "Can't remove the root node !");
			return;
		}

		Logger.notice(this, "DELETING FOLDER");

		((IndexFolder)parentNode).remove(this);

		synchronized(db.dbLock) {
			try {
				/* we remove all the files */
				PreparedStatement st;

				st = db.getConnection().prepareStatement("DELETE FROM files WHERE files.indexParent IN "+
									 "(SELECT indexParents.indexId FROM indexParents WHERE indexParents.folderId = ?)");

				st.setInt(1, id);

				st.execute();

				/* we remove all the links */


				st = db.getConnection().prepareStatement("DELETE FROM links WHERE links.indexParent IN "+
									 "(SELECT indexParents.indexId FROM indexParents WHERE indexParents.folderId = ?)");

				st.setInt(1, id);

				st.execute();

				/* we remove all the indexes */

				st = db.getConnection().prepareStatement("DELETE FROM indexes WHERE indexes.id IN "+
									 "(SELECT indexParents.indexId FROM indexParents WHERE indexParents.folderId = ?)");

				st.setInt(1, id);

				st.execute();


				/* we remove all the child folders */
				/* we must to it this way, else we have problems with foreign key */

				deleteChildFoldersRecursivly(id);

				st = db.getConnection().prepareStatement("DELETE FROM indexFolders WHERE indexFolders.id = ?");

				st.setInt(1, id);

				st.execute();

				/* we clean the joint tables */

				st = db.getConnection().prepareStatement("DELETE FROM indexParents "+
									 "WHERE indexId IN "+
									 "(SELECT indexParents.indexId FROM indexParents WHERE indexParents.folderId = ?)");
				st.setInt(1, id);
				st.execute();


				st = db.getConnection().prepareStatement("DELETE FROM folderParents "+
									 "WHERE (folderId IN "+
									 "(SELECT folderParents.folderId FROM folderParents WHERE folderParents.parentId = ?))");
				st.setInt(1, id);

				st.execute();


				st = db.getConnection().prepareStatement("DELETE FROM folderParents "+
									 "WHERE folderId = ?");
				st.setInt(1, id);

				st.execute();

			} catch(SQLException e) {
				Logger.error(this, "Error while removing the folder: "+e.toString());
			}
		}
	}

	/**
	 * Update from freenet / Update the freenet version, depending of the index kind (recursive)
	 */
	public int insertOnFreenet(Observer observer, IndexBrowserPanel indexBrowser, FCPQueueManager queueManager) {
		int i = 0;

		synchronized(db.dbLock) {
			try {
				PreparedStatement st;

				if (id >= 0) {
					st = db.getConnection().prepareStatement("SELECT indexId from indexParents WHERE folderId = ?");
					st.setInt(1, id);
				} else {
					st = db.getConnection().prepareStatement("SELECT indexId from indexParents WHERE folderId IS NULL");
				}

				ResultSet set = st.executeQuery();


				while(set.next()) {
					(new Index(db, set.getInt("indexId"))).insertOnFreenet(observer, indexBrowser, queueManager);
					i++;
				}

			} catch(SQLException e) {
				Logger.error(this, "Unable to start insertions because: "+e.toString());
			}
		}
		return i;
	}

	/**
	 * Update from freenet using the given revision
	 */
	public int downloadFromFreenet(Observer observer, IndexTree indexTree, FCPQueueManager queueManager) {
		return downloadFromFreenet(observer, indexTree, queueManager, -1);
	}

	public int downloadFromFreenet(Observer observer, IndexTree indexTree, FCPQueueManager queueManager, int rev) {
		int i = 0;

		synchronized(db.dbLock) {
			try {
				PreparedStatement st;

				if (id >= 0) {
					st = db.getConnection().prepareStatement("SELECT indexParents.indexId "+
										 "FROM indexParents WHERE folderId = ?");
					st.setInt(1, id);
				} else {
					st = db.getConnection().prepareStatement("SELECT id FROM indexes");
				}

				ResultSet set = st.executeQuery();

				while(set.next()) {
					int indexId;

					if (id >= 0)
						indexId = set.getInt("indexId");
					else
						indexId = set.getInt("id");

					/* TODO : give publickey too immediatly */
					if (rev < 0)
						(new Index(db, indexId)).downloadFromFreenet(observer, indexTree, queueManager);
					else
						(new Index(db, indexId)).downloadFromFreenet(observer, indexTree, queueManager, rev);
					i++;
				}

			} catch(SQLException e) {
				Logger.error(this, "Unable to start insertions because: "+e.toString());
			}
		}

		return i;
	}


	/**
	 * Get key(s)
	 */
	public String getPublicKey() {
		String keys = "";

		Logger.info(this, "getPublicKey()");

		synchronized(db.dbLock) {
			try {
				PreparedStatement st;

				if (id >= 0) {
					st = db.getConnection().prepareStatement("SELECT indexes.publicKey FROM indexes "+
										 "WHERE indexes.id = "+
										 "(SELECT indexParents.id FROM indexParents "+
										 " WHERE indexParents.folderId = ?)");
					st.setInt(1, id);
				} else {
					st = db.getConnection().prepareStatement("SELECT indexes.publicKey FROM indexes "+
										 "WHERE indexes.id = "+
										 "(SELECT indexParents.id FROM indexParents "+
										 " WHERE indexParents.folderId IS NULL)");
				}

				ResultSet set = st.executeQuery();

				while(set.next()) {
					keys += set.getString("publicKey") + "\n";
				}

			} catch(SQLException e) {
				Logger.error(this, "Unable to get public keys because: "+e.toString());
			}
		}

		return keys;
	}


	public String getPrivateKey() {
		String keys = "";

		Logger.info(this, "getPrivateKey()");

		synchronized(db.dbLock) {
			try {
				PreparedStatement st;

				if (id >= 0) {
					st = db.getConnection().prepareStatement("SELECT indexes.privateKey FROM indexes "+
										 "WHERE indexes.id = "+
										 "(SELECT indexParents.id FROM indexParents "+
										 " WHERE indexParents.folderId = ?)");
					st.setInt(1, id);
				} else {
					st = db.getConnection().prepareStatement("SELECT indexes.privateKey FROM indexes "+
										 "WHERE indexes.id = "+
										 "(SELECT indexParents.id FROM indexParents "+
										 " WHERE indexParents.folderId IS NULL)");
				}

				ResultSet set = st.executeQuery();

				while(set.next()) {
					keys += set.getString("privateKey") + "\n";
				}

			} catch(SQLException e) {
				Logger.error(this, "Unable to get private keys because: "+e.toString());
			}
		}

		if ("".equals(keys))
			return null;

		return keys;
	}



	public void reorder() {
		int position;

		synchronized(db.dbLock) {
			try {
				PreparedStatement selectSt;
				PreparedStatement updateSt;

				position = 0;


				/* We first sort the index folders */

				if (id >= 0) {
					selectSt =
						db.getConnection().prepareStatement("SELECT id FROM indexFolders WHERE parent = ? ORDER BY LOWER(name)");
				} else {
					selectSt =
						db.getConnection().prepareStatement("SELECT id FROM indexFolders WHERE parent IS NULL ORDER BY LOWER(name)");
				}

				updateSt =
					db.getConnection().prepareStatement("UPDATE indexFolders SET positionInTree = ? WHERE id = ?");


				if (id >= 0)
					selectSt.setInt(1, id);


				ResultSet set = selectSt.executeQuery();


				while(set.next()) {
					updateSt.setInt(1, position);
					updateSt.setInt(2, set.getInt("id"));
					updateSt.execute();
					position++;
				}



				/* next we sort the indexes */



				if (id >= 0) {
					selectSt =
						db.getConnection().prepareStatement("SELECT id FROM indexes WHERE parent = ? ORDER BY LOWER(displayName)");
				} else {
					selectSt =
						db.getConnection().prepareStatement("SELECT id FROM indexes WHERE parent IS NULL ORDER BY LOWER(displayName)");
				}

				updateSt =
					db.getConnection().prepareStatement("UPDATE indexes SET positionInTree = ? WHERE id = ?");


				if (id >= 0)
					selectSt.setInt(1, id);


				set = selectSt.executeQuery();


				while(set.next()) {
					updateSt.setInt(1, position);
					updateSt.setInt(2, set.getInt("id"));
					updateSt.execute();
					position++;
				}


			} catch(SQLException e) {
				Logger.error(this, "Error while reordering: "+e.toString());
			}
		}
	}


	public String toString() {
		if (id < 0)
			return thaw.core.I18n.getMessage("thaw.plugin.index.yourIndexes");

		if (name != null)
			return name;

		Logger.info(this, "toString()");

		synchronized(db.dbLock) {
			try {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("SELECT name from indexfolders WHERE id = ? LIMIT 1");
				st.setInt(1, id);
				ResultSet set = st.executeQuery();
				if (set.next()) {
					name = set.getString("name");
					return name;
				}

				Logger.error(this, "toString(): not found in the db ?!");
				return null;
			} catch(SQLException e) {
				Logger.error(this, "Unable to get name because: "+e.toString());
			}
		}

		return null;
	}


	public boolean equals(Object o) {
		if (o == null || !(o instanceof IndexFolder))
			return false;

		if (((IndexTreeNode)o).getId() == getId())
			return true;
		return false;
	}


	public boolean isModifiable() {
		/* disable for performance reasons */
		return false;
	}


	public boolean realIsModifiable() {

		if (children != null) {
			synchronized(children) {

				for (Iterator it = children.iterator();
				     it.hasNext();) {
					IndexTreeNode child = (IndexTreeNode)it.next();

					if (!child.isModifiable()) {
						return false;
					}
				}
			}

			return true;
		}

		synchronized(db.dbLock) {
			try {
				PreparedStatement st;

				if (id >= 0) {
					st = db.getConnection().prepareStatement("SELECT count(indexes.id) FROM "+
										 "indexes JOIN indexParents ON indexes.id = indexParents.indexId "+
										 "WHERE indexParents.folderId = ?");

					st.setInt(1, id);
				} else {
					st = db.getConnection().prepareStatement("SELECT count(indexes.id) FROM "+
										 "indexes JOIN indexParents ON indexes.id = indexParents.indexId "+
										 "WHERE indexParents.folderId IS NULL");
				}

				ResultSet set = st.executeQuery();

				if(set.next()) {
					int res;

					res = set.getInt(1);

					if (res > 0)
						return false;
					else
						return true;
				}
			} catch(SQLException e) {
				Logger.error(this, "unable to know if the folder contains only modifiable indexes because: "+e.toString());
			}
		}
		return false;
	}


	public boolean setHasChangedFlag(boolean flag) {
		setHasChangedFlagInMem(flag);

		synchronized(db.dbLock) {
			try {
				PreparedStatement st;
				if (id > 0) {
					st = db.getConnection().prepareStatement("UPDATE indexes "+
										 "SET newRev = ? "+
										 "WHERE id IN "+
										 "(SELECT indexParents.indexId FROM indexParents WHERE indexParents.folderId = ?)");
					st.setInt(2, id);
				} else {
					st = db.getConnection().prepareStatement("UPDATE indexes "+
										 "SET newRev = ?");
				}
				st.setBoolean(1, flag);

				st.execute();
			} catch(SQLException e) {
				Logger.error(this, "Error while changing 'hasChanged' flag: "+e.toString());
				return false;
			}
		}

		return true;
	}

	public boolean setHasChangedFlagInMem(boolean flag) {
		if (children != null) {

			synchronized(children) {
				for (Iterator it = children.iterator();
				     it.hasNext();) {
					IndexTreeNode child = (IndexTreeNode)it.next();

					child.setHasChangedFlagInMem(flag);
				}
			}
		}

		return true;
	}


	private boolean lastHasChangedValue = false;
	private boolean hasLastHasChangedValueBeenSet = false;

	public void forceHasChangedReload() {
		if (children != null) {
			synchronized(children) {
				for (Iterator it = children.iterator();
				     it.hasNext();) {
					IndexTreeNode child = (IndexTreeNode)it.next();
					child.forceHasChangedReload();
				}
			}
		}

		hasLastHasChangedValueBeenSet = false;
		hasChanged();
	}

	public boolean hasChanged() {
		if (children != null) {

			synchronized(children) {
				for (Iterator it = children.iterator();
				     it.hasNext();) {
					IndexTreeNode child = (IndexTreeNode)it.next();

					if (child.hasChanged())
						return true;
				}

				return false;
			}
		}

		/* It's dirty and will probably cause graphical bug :/ */
		if (hasLastHasChangedValueBeenSet)
			return lastHasChangedValue;

		synchronized(db.dbLock) {
			try {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("SELECT indexes.id "+
									 "FROM indexes JOIN indexParents ON indexes.id = indexParents.indexId "+
									 "WHERE indexParents.folderId = ? AND indexes.newRev = TRUE LIMIT 1");
				st.setInt(1, id);

				ResultSet set = st.executeQuery();

				boolean ret;

				ret = set.next();

				lastHasChangedValue = ret;
				hasLastHasChangedValueBeenSet = true;

				return ret;

			} catch(SQLException e) {
				Logger.error(this, "Error while trying to see if there is any change: "+e.toString());
			}
		}

		return false;
	}

	/**
	 * Will export private keys too !<br/>
	 * TODO: Improve perfs
	 */
	public Element do_export(Document xmlDoc, boolean withContent) {
		Element e = xmlDoc.createElement("indexCategory");

		if (id != -1)
			e.setAttribute("name", name);

		if(children == null)
			loadChildren();

		for(final Iterator it = children.iterator();
		    it.hasNext();) {
			final IndexTreeNode node = (IndexTreeNode)(it.next());
			e.appendChild(node.do_export(xmlDoc, withContent));
		}

		unloadChildren();

		return e;
	}


	public IndexFolder getChildFolder(int id) {
		if (id < 0) {
			Logger.notice(this, "getChildFolder() : Asked me to have the root ?!");
			return null;
		}

		if (children == null)
			loadChildren();

		for (Iterator it = children.iterator() ;
		     it.hasNext(); ) {
		        Object child = it.next();

			if (child instanceof IndexFolder) {
				if (((IndexFolder)child).getId() == id) {
					return ((IndexFolder)child);
				}
			}
		}

		return null;
	}


	public Index getChildIndex(int id) {
		if (id < 0) {
			Logger.error(this, "getChildIndex() : Invalid parameter !");
			return null;
		}

		if (children == null)
			loadChildren();

		for (Iterator it = children.iterator() ;
		     it.hasNext(); ) {
		        Object child = it.next();

			if (child instanceof Index) {
				if (((Index)child).getId() == id) {
					return ((Index)child);
				}
			}
		}

		return null;
	}

}
