package thaw.plugins.index;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.FileOutputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.JScrollPane;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import java.text.DateFormat;

import thaw.gui.IconBox;

import thaw.core.Config;
import thaw.core.PleaseWaitDialog;
import thaw.core.ThawThread;
import thaw.core.ThawRunnable;
import thaw.gui.FileChooser;
import thaw.core.MainWindow;
import thaw.fcp.FreenetURIHelper;
import thaw.core.I18n;
import thaw.core.Logger;
import thaw.fcp.FCPGenerateSSK;
import thaw.fcp.FCPQueueManager;
import thaw.plugins.Hsqldb;
import thaw.plugins.signatures.Identity;
import thaw.gui.WarningWindow;

/**
 * Index.java, IndexFolder.java and IndexTree.java must NEVER use this helper (to avoid loops).
 */
public class IndexManagementHelper {

	private static String askAName(Component parent, final String prompt, final String defVal) {
		return JOptionPane.showInputDialog(parent, prompt, defVal);
	}


	/**
	 * Class implementing IndexAction will automatically do an addActionListener if necessary
	 */
	public interface IndexAction extends ActionListener {

		/**
		 * Can disable the abstract button if required
		 * @param node can be null
		 */
		public void setTargets(Vector nodes);
	}


	public static abstract class BasicIndexAction
		implements IndexAction, ThawRunnable {

		private FCPQueueManager queueManager;
		private AbstractButton actionSource;
		private Vector targets;

		private IndexBrowserPanel indexBrowser;

		public BasicIndexAction(final FCPQueueManager queueManager,
					final IndexBrowserPanel indexBrowser,
					final AbstractButton actionSource) {
			this.indexBrowser = indexBrowser;
			this.actionSource = actionSource;
			targets = null;
			this.queueManager = queueManager;

			if (actionSource != null) {
				actionSource.addActionListener(this);
			}
		}


		public AbstractButton getActionSource() {
			return actionSource;
		}

		public void setTargets(final Vector nodes) {
			targets = nodes;
		}

		public FCPQueueManager getQueueManager() {
			return queueManager;
		}

		public Vector getTargets() {
			return targets;
		}

		public IndexBrowserPanel getIndexBrowserPanel() {
			return indexBrowser;
		}


		public void actionPerformed(ActionEvent e) {
			if (e.getSource() == actionSource) {
				startThread();
			}
		}


		protected void startThread() {
			Thread th = new ThawThread(this, "Action replier", this);
			th.start();
		}


		public void run() {
			if (targets == null)
				return;
			
			for (Iterator it = targets.iterator(); it.hasNext() ; ) {
				IndexTreeNode node = (IndexTreeNode)it.next();
				apply(node);
			}
		}

		public void stop() {
			/* \_o< */
		}

		public abstract void apply(IndexTreeNode target);
	}



	public static class IndexCreator extends BasicIndexAction implements Observer {
		private IndexTreeNode target;
		
		public IndexCreator(final FCPQueueManager queueManager, final IndexBrowserPanel indexBrowser, final AbstractButton actionSource) {
			super(queueManager, indexBrowser, actionSource);
		}
		
		public void setTargets(Vector nodes) {
			super.setTargets(nodes);
			
			if (nodes == null || nodes.size() != 1)
				getActionSource().setEnabled(false);
			else
				setTarget((IndexTreeNode)nodes.get(0));
		}

		public void setTarget(final IndexTreeNode node) {
			this.target = node;
			
			if (getActionSource() != null)
				getActionSource().setEnabled((node == null) || (node instanceof IndexFolder));
		}

		public void apply(IndexTreeNode target) {
			final String name = IndexManagementHelper.askAName(getIndexBrowserPanel().getMainWindow().getMainFrame(),
									   I18n.getMessage("thaw.plugin.index.indexName"),
									   I18n.getMessage("thaw.plugin.index.newIndex"));

			if (name == null)
				return;

			/* will create a dedicated IndexCreator */
			IndexManagementHelper.createIndex(getQueueManager(), getIndexBrowserPanel(), (IndexFolder)target, name);
		}


		private String name;

		private void createIndex(IndexTreeNode target, String name) {
			if (target == null)
				setTarget(getIndexBrowserPanel().getIndexTree().getRoot());

			if ((name == null) || (name.indexOf("/") >= 0) || name.indexOf("\\") >= 0) {
				Logger.error(new IndexManagementHelper(), "invalid name");
				return;
			}

			this.name = name;

			FCPGenerateSSK sskGenerator;

			sskGenerator = new FCPGenerateSSK();
			sskGenerator.addObserver(this);
			sskGenerator.start(getQueueManager());
		}


		public void update(Observable o, Object param) {
			FCPGenerateSSK sskGenerator = (FCPGenerateSSK)o;
			Hsqldb db = getIndexBrowserPanel().getDb();

			Index index;

			synchronized(db.dbLock) {
				try {
					PreparedStatement st;

					int id = DatabaseManager.getNextId(db, "indexes");

					if (id == -1)
						return;

					st = db.getConnection().prepareStatement("INSERT INTO indexes "+
										 "(id, originalName, displayName, "+
										 " publicKey, privateKey, author, "+
										 " positionInTree, revision, "+
										 " newRev, newComment, parent) "+
										 "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

					/* TODO : Author */

					String publicKey;

					publicKey =
						FreenetURIHelper.convertSSKtoUSK(sskGenerator.getPublicKey())
						+"/"+name+"/0/"+name+".frdx";

					st.setInt(1, id);
					st.setString(2, name);
					st.setString(3, name);
					st.setString(4, publicKey);
					st.setString(5, sskGenerator.getPrivateKey());
					st.setNull(6, Types.VARCHAR);
					st.setInt(7, 0 /* positionInTree */);
					st.setInt(8, 0 /* revision */);
					st.setBoolean(9, false);
					st.setBoolean(10, false);

					if (target.getId() >= 0)
						st.setInt(11, target.getId());
					else
						st.setNull(11, Types.INTEGER);

					st.execute();
					st.close();

					index = new Index(db, getIndexBrowserPanel().getConfig(),
							  id, (TreeNode)target,
							  sskGenerator.getPublicKey(), 0,
							  sskGenerator.getPrivateKey(), false,
							  name, null,
							  false, false);

				} catch(SQLException e) {
					Logger.error(new IndexManagementHelper(),
						     "Error while creating index: "+ e.toString());
					return;
				}
			}

			((MutableTreeNode)target).insert((index), 0);

			getIndexBrowserPanel().getIndexTree().refresh(target);

			IndexConfigDialog dialog = new IndexConfigDialog(getIndexBrowserPanel(),
									 getQueueManager(),
									 index);
			dialog.promptUser();
		}
	}


	public static void createIndex(final FCPQueueManager queueManager,
				       final IndexBrowserPanel indexBrowser,
				       IndexFolder target, final String name) {

		IndexCreator creator = new IndexCreator(queueManager, indexBrowser, null);
		creator.setTarget(target);
		creator.createIndex(target, name);

	}



	public static class IndexModifier extends BasicIndexAction implements ThawRunnable {
		public IndexModifier(final FCPQueueManager queueManager, final IndexBrowserPanel indexBrowser, final AbstractButton actionSource) {
			super(queueManager, indexBrowser, actionSource);
		}
		
		public void setTargets(Vector nodes) {
			super.setTargets(nodes);
			
			if (nodes == null || nodes.size() != 1)
				getActionSource().setEnabled(false);
			else
				setTarget((IndexTreeNode)nodes.get(0));
		}

		public void setTarget(final IndexTreeNode node) {
			if (getActionSource() != null)
				getActionSource().setEnabled((node != null) && (node instanceof Index));
		}

		public void apply(IndexTreeNode target) {
			final Index index = ((Index)target);

			IndexConfigDialog dialog = new IndexConfigDialog(getIndexBrowserPanel(),
									 getQueueManager(),
									 index);

			if (!dialog.promptUser()) {
				Logger.info(this, "Change cancelled");
				return;
			}

			getIndexBrowserPanel().getIndexTree().refresh(index);

			if (index.getPrivateKey() != null)
				new WarningWindow(getIndexBrowserPanel().getMainWindow(),
						  I18n.getMessage("thaw.plugin.index.mustReinsert"));
		}
	}


	public static class IndexReuser extends BasicIndexAction implements ThawRunnable {
		public IndexReuser(final FCPQueueManager queueManager, final IndexBrowserPanel indexBrowser, final AbstractButton actionSource) {
			super(queueManager, indexBrowser, actionSource);
		}
		
		public void setTargets(Vector nodes) {
			super.setTargets(nodes);
			
			if (nodes == null || nodes.size() != 1)
				getActionSource().setEnabled(false);
			else
				setTarget((IndexTreeNode)nodes.get(0));
		}

		public void setTarget(final IndexTreeNode node) {
			if (getActionSource() != null)
				getActionSource().setEnabled((node == null) || (node instanceof IndexFolder));
		}

		public void apply(IndexTreeNode target) {
			String publicKey = null;
			String privateKey = null;

			IndexConfigDialog dialog = new IndexConfigDialog(getIndexBrowserPanel(),
															getQueueManager());

			if (!dialog.promptUser()) /* cancelled */
				return;

			publicKey = dialog.getPublicKey();
			privateKey = dialog.getPrivateKey();

			/* https://bugs.freenetproject.org/view.php?id=1625:
			 *  --- 0001625: Added index not in the correct category
			 *  If you add an index from the link list instead of the unknown index list,
			 *  it's added in the same folder than the index instead of "recently added".
			 *  ==> Target == null
			 */
			IndexManagementHelper.reuseIndex(getQueueManager(), getIndexBrowserPanel(),
							 null, publicKey, privateKey,
							 false /* autosort */);
		}
	}


	public static Index addIndex(final FCPQueueManager queueManager,
				     final IndexBrowserPanel indexBrowser,
				     final IndexFolder target,
				     final String publicKey,
				     boolean autoSort) {

		return IndexManagementHelper.reuseIndex(queueManager, indexBrowser,
							target, publicKey, null,
							autoSort);

	}

	public static Index reuseIndex(final FCPQueueManager queueManager,
				       final IndexBrowserPanel indexBrowser,
				       final IndexFolder target, String publicKey,
				       String privateKey,
				       boolean autoSort) {

		return reuseIndex(queueManager, indexBrowser, target,
				  publicKey, privateKey, true,
				  autoSort);

	}

	/**
	 * @param privateKey Can be null
	 * @param queueManager only needed if load == true
	 * @param target is obsolete
	 */
	public static Index reuseIndex(final FCPQueueManager queueManager,
				       final IndexBrowserPanel indexBrowser,
				       final IndexFolder target,
				       String publicKey, String privateKey,
				       boolean load,
				       boolean autoSort) {

		publicKey = FreenetURIHelper.cleanURI(publicKey);
		privateKey = FreenetURIHelper.cleanURI(privateKey);

		if (publicKey == null)
			return null;

		if (privateKey != null && privateKey.equals(""))
			privateKey = null;

		if (Index.isAlreadyKnown(indexBrowser.getDb(), publicKey, true) >= 0) {
			String name = FreenetURIHelper.getFilenameFromKey(publicKey);

			if (name != null) {
				name = name.replaceAll(".frdx", "");
				Logger.warning(new IndexManagementHelper(), "Index '"+name+"' already added");
			}

			return null;
		}

		final String name = Index.getNameFromKey(publicKey);

		if (name == null || name.indexOf("/") >= 0 || name.indexOf("\\") >= 0) {
			Logger.error(new IndexManagementHelper(), "Invalid index name !\n");
			return null;
		}

		IndexFolder parent;

		if (target != null)
			parent = target;
		else
			parent = indexBrowser.getIndexTree().getRoot().getRecentlyAddedFolder();

		int revision = FreenetURIHelper.getUSKRevision(publicKey);

		Hsqldb db = indexBrowser.getDb();

		Index index = null;
		int pos = parent.getChildCount();

		synchronized(db.dbLock) {
			try {
				PreparedStatement st;

				int id = DatabaseManager.getNextId(db, "indexes");

				if (id == -1)
					return null;

				st = db.getConnection().prepareStatement("INSERT INTO indexes "+
									 "(id, originalName, displayName, "+
									 " publicKey, privateKey, author, "+
									 " positionInTree, revision, "+
									 " newRev, parent) "+
									 "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

				/* TODO : Author */

				st.setInt(1, id);
				st.setString(2, name);
				st.setString(3, name);
				st.setString(4, publicKey);
				st.setString(5, privateKey);
				st.setNull(6, Types.VARCHAR);
				st.setInt(7, pos /* positionInTree */);
				st.setInt(8, revision);
				st.setBoolean(9, false);

				if (parent.getId() > 0)
					st.setInt(10, parent.getId());
				else
					st.setNull(10, Types.INTEGER);

				st.execute();
				st.close();

				index = new Index(db, indexBrowser.getConfig(),
						  id, parent,
						  publicKey, revision,
						  privateKey, false,
						  name, null, false, false);

			} catch(SQLException e) {
				Logger.error(new IndexManagementHelper(), "Error while adding index: "+e.toString());
				return null;
			}

		}

		index.setIsNewFlag();

		((MutableTreeNode)parent).insert((index), pos);

		indexBrowser.getIndexTree().refresh(parent);

		indexBrowser.getUnknownIndexList().removeLink(index);

		if (load) {
			download(queueManager, indexBrowser, index, autoSort);
		}

		return index;
	}




	public static class IndexFolderAdder extends BasicIndexAction {
		public IndexFolderAdder(final IndexBrowserPanel indexBrowser, final AbstractButton actionSource) {
			super(null, indexBrowser, actionSource);
		}
		
		public void setTargets(Vector nodes) {
			super.setTargets(nodes);
			
			if (nodes == null || nodes.size() != 1)
				getActionSource().setEnabled(false);
			else
				setTarget((IndexTreeNode)nodes.get(0));
		}

		public void setTarget(final IndexTreeNode node) {
			if (getActionSource() != null)
				getActionSource().setEnabled((node == null) || (node instanceof IndexFolder));
		}

		public void apply(IndexTreeNode target) {
			final String name = IndexManagementHelper.askAName(getIndexBrowserPanel().getMainWindow().getMainFrame(),
									   I18n.getMessage("thaw.plugin.index.categoryName"),
									   I18n.getMessage("thaw.plugin.index.newCategory"));

			if (name != null)
				IndexManagementHelper.addIndexFolder(getIndexBrowserPanel(), (IndexFolder)target, name);
		}
	}


	public static IndexFolder addIndexFolder(final IndexBrowserPanel indexBrowser, IndexFolder target, final String name) {
		if (target == null)
			target = indexBrowser.getIndexTree().getRoot();

		IndexFolder folder = null;

		synchronized(indexBrowser.getDb().dbLock) {

			try {
				PreparedStatement st;

				int nextId = DatabaseManager.getNextId(indexBrowser.getDb(), "indexFolders");

				if (nextId < -1)
					return null;

				st = indexBrowser.getDb().getConnection().prepareStatement("INSERT INTO indexFolders "+
											   "(id, name, positionInTree, modifiableIndexes, parent) "+
											   "VALUES (?, ?, ?, ?, ?)");

				st.setInt(1, nextId);
				st.setString(2, name);
				st.setInt(3, 0 /* position */);
				st.setBoolean(4, true /* modifiable : obsolete */);

				if (target.getId() > 0)
					st.setInt(5, target.getId());
				else
					st.setNull(5, Types.INTEGER);

				st.execute();
				st.close();

				folder = new IndexFolder(indexBrowser.getDb(), indexBrowser.getConfig(),
							 nextId, target, name, false);

			} catch(SQLException e) {
				Logger.error(new IndexManagementHelper(), "Error while creating folder: "+e.toString());

				return null;
			}
		}

		((MutableTreeNode)target).insert((folder), 0);
		indexBrowser.getIndexTree().refresh(target);

		return folder;
	}


	public static class IndexHasChangedFlagReseter extends BasicIndexAction implements ThawRunnable {
		public IndexHasChangedFlagReseter(IndexBrowserPanel indexBrowser, final AbstractButton actionSource) {
			super(null, indexBrowser, actionSource);
		}
		
		public void setTargets(Vector nodes) {
			super.setTargets(nodes);
		}

		public void apply(IndexTreeNode target) {
			target.setHasChangedFlag(false);
			target.setNewCommentFlag(false);
			getIndexBrowserPanel().getIndexTree().redraw(target);
		}
	}


	public static class IndexDownloader extends BasicIndexAction implements ThawRunnable, Observer {
		private boolean autoSort = false;

		public IndexDownloader(FCPQueueManager queueManager,
				       IndexBrowserPanel indexBrowser,
				       final AbstractButton actionSource) {
			super(queueManager, indexBrowser, actionSource);
		}

		public IndexDownloader(FCPQueueManager queueManager,
				       IndexBrowserPanel indexBrowser,
				       final AbstractButton actionSource,
				       boolean autoSort) {
			this(queueManager, indexBrowser, actionSource);
			this.autoSort = autoSort;
		}
		
		public void setTargets(Vector paths) {
			super.setTargets(paths);
		}

		public void apply(IndexTreeNode target) {
			target.downloadFromFreenet(this,
						getIndexBrowserPanel().getIndexTree(),
						getQueueManager());
			getIndexBrowserPanel().getIndexTree().redraw(target);
		}

		public void update(Observable o, Object param) {
			getIndexBrowserPanel().getIndexTree().redraw(((Index)o), true);


			if (o.equals(getIndexBrowserPanel().getTables().getFileTable().getFileList())) {
				getIndexBrowserPanel().getTables().getFileTable().refresh();
			}

			if (o.equals(getIndexBrowserPanel().getTables().getLinkTable().getLinkList())) {
				getIndexBrowserPanel().getTables().getLinkTable().refresh();
			}

			getIndexBrowserPanel().getUnknownIndexList().addLinks((LinkList)o);

			if (((Index)o).hasChanged() && autoSort) {
				Index index = (Index)o;

				String cat;

				if ( (cat = index.getCategory()) == null) {
					Logger.notice(this, "No category defined ; Can't autosort "+
										"the index '"+index.toString(false)+"'");
					return;
				}

				autoSortIndex(getIndexBrowserPanel(), index, cat);
			}
		}
	}



	public static boolean download(FCPQueueManager queueManager,
				       IndexBrowserPanel indexBrowser,
				       IndexTreeNode target,
				       boolean autoSort) {

		IndexDownloader downloader = new IndexDownloader(queueManager,
								 indexBrowser,
								 null,
								 autoSort);
		Vector v = new Vector();
		v.add(target);
		downloader.setTargets(v);

		Thread th = new ThawThread(downloader, "Index downloader");
		th.start();

		return true;
	}


	/**
	 * @param cat Example: "freenet/thaw" (only folders !)
	 * @return the path in the tree
	 */
	public static TreePath makeMyPath(IndexBrowserPanel indexBrowser, String cat, int maxDepth) {
		String[] split = cat.split("/");

		if (split == null) return null;

		IndexFolder root = indexBrowser.getIndexTree().getRoot();
		IndexFolder currentFolder = indexBrowser.getIndexTree().getRoot().getAutoSortedFolder();
		TreePath path = new TreePath(root);
		path = path.pathByAddingChild(currentFolder);

		for (int i = 0 ; i < split.length && i < maxDepth; i++) {
			if (split[i] == null || "".equals(split[i].trim()))
				continue;
			String folder = split[i].trim().toLowerCase();

			IndexFolder nextFolder = currentFolder.getFolder(folder);

			if (nextFolder == null) {
				nextFolder = addIndexFolder(indexBrowser,
							    currentFolder,
							    folder);
			}

			path = path.pathByAddingChild(nextFolder);

			currentFolder = nextFolder;
		}

		return path;
	}


	public static boolean moveIndexTo(IndexBrowserPanel indexBrowser,
					  Index index,
					  IndexFolder dst) {
		IndexFolder oldParent = (IndexFolder)index.getParent();

		if (oldParent == dst) {
			Logger.notice(new IndexManagementHelper(), "Index '"+index.toString()+"'already sorted.");
			return false;
		}

		index.removeFromParent();
		dst.insert(index, 0);

		if (oldParent != null) {
			indexBrowser.getIndexTree().refresh(oldParent);
		} else {
			indexBrowser.getIndexTree().refresh();
		}

		indexBrowser.getIndexTree().refresh(dst);

		return true;
	}

	public static boolean autoSortIndexes(IndexBrowserPanel indexBrowser,
										IndexTreeNode node,
										MainWindow mainWindow) {
		return autoSortIndexes(indexBrowser, node, mainWindow, true);
	}
	

	public static boolean autoSortIndexes(IndexBrowserPanel indexBrowser,
					      				IndexTreeNode node, MainWindow mainWindow,
					      				boolean showDialog) {
		if (node instanceof Index) {
			String cat = ((Index)node).getCategory();

			if (cat != null)
				return autoSortIndex(indexBrowser, (Index)node, cat);
			else
				Logger.notice(indexBrowser, "No category for '"+((Index)node).toString()+"'; can't sort");

		} else if (node instanceof IndexFolder) {

			IndexFolder folder = ((IndexFolder)node);

			if (folder == null || "".equals(folder.toString())) {
				return false;
			}
			
			PleaseWaitDialog dialog = null;
			
			if (showDialog)
				dialog = new PleaseWaitDialog(mainWindow);

			for (java.util.Enumeration children = folder.children();
			     children.hasMoreElements();) {

				/* dirty recursivity */
				IndexTreeNode subNode = (IndexTreeNode)children.nextElement();
				autoSortIndexes(indexBrowser, subNode, null, false);

			}
			
			if (showDialog && dialog != null)
				dialog.dispose();
		}

		return true;
	}


	public static boolean autoSortIndex(IndexBrowserPanel indexBrowser,
										Index index, String cat) {
		if (cat == null) {
			Logger.info(new IndexManagementHelper(), "No category ; Can't sort the index");
			return false;
		}

		TreePath path = makeMyPath(indexBrowser, cat,
					   IndexFolder.MAX_AUTOSORTING_DEPTH+1);

		if (path == null) {
			return false;
		}

		IndexFolder dst = (IndexFolder)path.getLastPathComponent();

		return moveIndexTo(indexBrowser, index, dst);
	}



	public static class IndexSorter extends BasicIndexAction implements ThawRunnable {
		public IndexSorter(IndexBrowserPanel indexBrowser,
				   final AbstractButton actionSource) {

			super(null, indexBrowser, actionSource);
		}
		
		public void setTargets(Vector nodes) {
			super.setTargets(nodes);

			if (getActionSource() != null)
				getActionSource().setEnabled(nodes != null && nodes.size() > 0);
		}

		public void apply(IndexTreeNode target) {
			if (target instanceof Index) {
				if (((Index)target).getCategory() == null) {
					Logger.warning(this, "No category => can't sort !");
					return;
				}
			}

			autoSortIndexes(getIndexBrowserPanel(),
							(IndexTreeNode)target,
							getIndexBrowserPanel().getMainWindow());
		}
	}


	public static class IndexUploader extends BasicIndexAction implements ThawRunnable, Observer {
		public IndexUploader(FCPQueueManager queueManager,
				     IndexBrowserPanel indexBrowser,
				     final AbstractButton actionSource) {

			super(queueManager, indexBrowser, actionSource);
		}
		
		public void setTargets(Vector nodes) {
			super.setTargets(nodes);

			if (getActionSource() != null)
				getActionSource().setEnabled(nodes != null && nodes.size() > 0);
			
			if (getActionSource().isEnabled()) {
				for (Iterator it = nodes.iterator() ; it.hasNext() && getActionSource().isEnabled() ; ) {
					IndexTreeNode node = (IndexTreeNode)it.next();
					
					if (!node.isModifiable())
						getActionSource().setEnabled(false);
				}
			}
		}

		public void apply(IndexTreeNode target) {
			target.insertOnFreenet(this, getIndexBrowserPanel(), getQueueManager());

			getIndexBrowserPanel().getIndexTree().redraw(target);
		}

		public void update(Observable o, Object param) {
			getIndexBrowserPanel().getIndexTree().redraw(((Index)o));
		}

	}


	public static boolean insert(FCPQueueManager queueManager, IndexBrowserPanel indexBrowser, IndexTreeNode target) {
		IndexUploader uploader = new IndexUploader(queueManager, indexBrowser, null);
		Vector v = new Vector();
		v.add(target);
		uploader.setTargets(v);

		Thread th = new ThawThread(uploader, "Index inserter");
		th.start();

		return true;
	}


	public static class PublicKeyCopier extends BasicIndexAction {
		public PublicKeyCopier(final AbstractButton actionSource) {
			super(null, null, actionSource);
		}

		public void setTargets(final Vector nodes) {
			super.setTargets(nodes);
		}

		public void apply(IndexTreeNode target) {
			IndexManagementHelper.copyPublicKeyFrom(target);
		}
	}


	public static void copyPublicKeyFrom(final IndexTreeNode node) {
		if (node == null)
			return;

		if (node instanceof Index) {
			if (((Index)node).getRevision() <= 0) {
				new WarningWindow((MainWindow)null, I18n.getMessage("thaw.plugin.index.stillRev0"));
			}
		}

		thaw.gui.GUIHelper.copyToClipboard(node.getPublicKey());
	}


	public static class PrivateKeyCopier extends BasicIndexAction {
		public PrivateKeyCopier(final AbstractButton actionSource) {
			super(null, null, actionSource);
		}

		public void setTargets(final Vector nodes) {
			super.setTargets(nodes);
			
			boolean e = true;
			
			if (nodes == null)
				e = false;
			else {
				for (Iterator it = nodes.iterator(); it.hasNext() && e; ) {
					IndexTreeNode node = (IndexTreeNode)it.next();
					if (!(node instanceof Index) || !node.isModifiable())
						e = false;
				}
			}
			
			getActionSource().setEnabled(e);
		}

		public void apply(IndexTreeNode target) {
			IndexManagementHelper.copyPrivateKeyFrom(target);
		}
	}


	public static void copyPrivateKeyFrom(final IndexTreeNode node) {
		if (node == null)
			return;

		final Toolkit tk = Toolkit.getDefaultToolkit();
		final StringSelection st = new StringSelection(node.getPrivateKey());
		final Clipboard cp = tk.getSystemClipboard();
		cp.setContents(st, null);
	}

	/**
	 * Can rename indexes or index categories.
	 */
	public static class IndexRenamer extends BasicIndexAction {
		public IndexRenamer(final IndexBrowserPanel indexBrowser, final AbstractButton actionSource) {
			super(null, indexBrowser, actionSource);
		}
		
		public void setTargets(Vector nodes) {
			super.setTargets(nodes);
			
			if (nodes == null || nodes.size() != 1)
				getActionSource().setEnabled(false);
			else
				setTarget((IndexTreeNode)nodes.get(0));
		}

		public void setTarget(final IndexTreeNode node) {
			if (getActionSource() != null)
				getActionSource().setEnabled(node != null);
		}

		public void apply(IndexTreeNode target) {
			String newName;

			if (target instanceof Index) {
				newName = IndexManagementHelper.askAName(getIndexBrowserPanel().getMainWindow().getMainFrame(),
									 I18n.getMessage("thaw.plugin.index.indexName"),
									 ((Index)target).toString(false));
			} else {
				newName = IndexManagementHelper.askAName(getIndexBrowserPanel().getMainWindow().getMainFrame(),
									 I18n.getMessage("thaw.plugin.index.categoryName"),
									 target.toString());
			}

			if (newName == null)
				return;

			IndexManagementHelper.renameNode(getIndexBrowserPanel(), target, newName);
		}
	}


	public static void renameNode(final IndexBrowserPanel indexBrowser, final IndexTreeNode node, final String newName) {
		if ((node == null) || (newName == null))
			return;

		node.rename(newName);

		indexBrowser.getIndexTree().refresh(node);
	}



	public static class IndexExporter extends BasicIndexAction {
		public IndexExporter(final AbstractButton actionSource) {
			super(null, null, actionSource);
		}
		
		public void setTargets(Vector nodes) {
			super.setTargets(nodes);
			
			if (nodes == null || nodes.size() != 1)
				getActionSource().setEnabled(false);
			else
				setTarget((IndexTreeNode)nodes.get(0));
		}

		public void setTarget(final IndexTreeNode node) {
			if (getActionSource() != null)
				getActionSource().setEnabled((node != null) && (node instanceof Index));
		}

		public void apply(IndexTreeNode target) {
			java.io.File newFile;

			final FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle(I18n.getMessage("thaw.plugin.index.exportIndex"));
			fileChooser.setDirectoryOnly(false);
			fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
			newFile = fileChooser.askOneFile();

			if (newFile == null)
				return;

			FileOutputStream out;

			try {
				out = new FileOutputStream(newFile);
			} catch(final java.io.FileNotFoundException excep) {
				Logger.warning(this, "Unable to create file '"+newFile.toString()+"' ! not generated  because : "+excep.toString());
				return;
			}

			new IndexParser(((Index)target)).generateXML(out);
			
			try {
				out.close();
			} catch(java.io.IOException e) {
				Logger.warning(this, "Can't close the export file cleanly");	
			}
		}
	}


	public static class IndexImporter extends BasicIndexAction {
		public IndexImporter(final IndexBrowserPanel indexBrowser, final AbstractButton actionSource) {
			super(null, indexBrowser, actionSource);
		}
		
		public void setTargets(Vector nodes) {
			super.setTargets(nodes);
			
			if (nodes == null || nodes.size() != 1)
				getActionSource().setEnabled(false);
			else
				setTarget((IndexTreeNode)nodes.get(0));
		}

		public void setTarget(final IndexTreeNode node) {
			if (getActionSource() != null)
				getActionSource().setEnabled((node != null) && (node instanceof Index));
		}

		public void apply(IndexTreeNode target) {
			java.io.File newFile;

			final FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle(I18n.getMessage("thaw.plugin.index.importIndex"));
			fileChooser.setDirectoryOnly(false);
			fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
			newFile = fileChooser.askOneFile();

			if (newFile == null)
				return;

			new IndexParser(((Index)target)).loadXML(newFile.getPath(), false);

			getIndexBrowserPanel().getTables().getFileTable().refresh();
			getIndexBrowserPanel().getTables().getLinkTable().refresh();
		}
	}



	/**
	 * Can be used on indexes or index categories.
	 * Can be also used as a keylistener : will only react with the key 'delete'
	 */
	public static class IndexDeleter extends BasicIndexAction implements KeyListener {
		public IndexDeleter(final IndexBrowserPanel indexBrowser, final AbstractButton actionSource) {
			super(null, indexBrowser, actionSource);
		}

		public void setTargets(final Vector nodes) {
			super.setTargets(nodes);

			if (getActionSource() != null)
				getActionSource().setEnabled(getIndexBrowserPanel().getIndexTree() != null);
		}

		public void  keyPressed(KeyEvent e) {

		}

		public void  keyReleased(KeyEvent e) {
			if (e.getKeyCode() == KeyEvent.VK_DELETE) {
				startThread();
			}
		}

		public void  keyTyped(KeyEvent e) {

		}

		public void apply(IndexTreeNode target) {
			IndexManagementHelper.deleteNode(getIndexBrowserPanel(), target);
		}
	}


	public static void deleteNode(final IndexBrowserPanel indexBrowser, final IndexTreeNode node) {
		if (node == null)
			return;

		IndexFolder folder = (IndexFolder)(node.getTreeNode().getParent());
		node.delete();

		if (folder != null) {
			indexBrowser.getIndexTree().refresh(folder);
		} else {
			indexBrowser.getIndexTree().refresh();
		}

		indexBrowser.getIndexTree().updateMenuState(null);
	}


	/**
	 * Can be used on indexes only
	 */
	public static class IndexBlackLister extends IndexDeleter {
		private IndexBrowserPanel indexBrowser;

		public IndexBlackLister(final IndexBrowserPanel indexBrowser, final AbstractButton actionSource) {
			super(indexBrowser, actionSource);

			this.indexBrowser = indexBrowser;
		}

		public void setTargets(final Vector nodes) {
			super.setTargets(nodes);
		}
		
		private void blackListRecursivly(Hsqldb db, IndexFolder target) {
			/* A nicer way would be to ask directly to the database the index who
			 * are children of this folder, but I'm a lazy bastard.
			 */
			java.util.Enumeration targetChildren = target.children();
			
			while (targetChildren.hasMoreElements()) {
				Object o = targetChildren.nextElement();
				
				if (o instanceof Index) {
					BlackList.addToBlackList(db, ((Index)o).getPublicKey());
				} else if (o instanceof IndexFolder) {
					blackListRecursivly(db, (IndexFolder)o);
				}
			}
		}

		public void apply(IndexTreeNode target) {
			if (target instanceof IndexFolder) {
				blackListRecursivly(indexBrowser.getDb(), (IndexFolder)target);
			} else if (target instanceof Index) {
				BlackList.addToBlackList(indexBrowser.getDb(), target.getPublicKey());
			}
			
			super.apply(target);
			indexBrowser.getBlackList().updateList();
		}
	}




	public static class FileInserterAndAdder extends BasicIndexAction {
		private Config config;

		public FileInserterAndAdder(final Config config, final FCPQueueManager queueManager, final IndexBrowserPanel indexBrowser, final AbstractButton actionSource) {
			super(queueManager, indexBrowser, actionSource);
			this.config = config;
		}
		
		public void setTargets(Vector nodes) {
			super.setTargets(nodes);
			
			if (nodes == null || nodes.size() != 1)
				getActionSource().setEnabled(false);
			else
				setTarget((IndexTreeNode)nodes.get(0));
		}

		public void setTarget(final IndexTreeNode node) {

			if (getActionSource() != null)
				getActionSource().setEnabled((node != null) && (node instanceof Index) && ((Index)node).isModifiable());
		}

		public void apply(IndexTreeNode target) {
			final FileChooser fileChooser;

			String lastDir = null;

			if (config.getValue("lastSourceDirectory") != null)
				lastDir = config.getValue("lastSourceDirectory");

			if (lastDir == null)
				fileChooser = new FileChooser();
			else
				fileChooser = new FileChooser(lastDir);

			fileChooser.setDirectoryOnly(false);
			fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
			fileChooser.setTitle(I18n.getMessage("thaw.plugin.index.addFilesWithInserting"));

			final Vector files = fileChooser.askManyFiles();

			if(files == null)
				return;

			if (files.size() > 0) {
				config.setValue("lastSourceDirectory", fileChooser.getFinalDirectory());
			}

			final String category = FileCategory.promptForACategory();

			IndexManagementHelper.addFiles(getQueueManager(), getIndexBrowserPanel(), (Index)target, files, category, true);
		}
	}


	public static class FileAdder extends BasicIndexAction {
		private Config config;

		public FileAdder(final Config config, final FCPQueueManager queueManager, final IndexBrowserPanel indexBrowser, final AbstractButton actionSource) {
			super(queueManager, indexBrowser, actionSource);
			this.config = config;
		}
		
		public void setTargets(Vector nodes) {
			super.setTargets(nodes);
			
			if (nodes == null || nodes.size() != 1)
				getActionSource().setEnabled(false);
			else
				setTarget((IndexTreeNode)nodes.get(0));
		}

		public void setTarget(final IndexTreeNode node) {

			if (getActionSource() != null)
				getActionSource().setEnabled((node != null) && (node instanceof Index) && ((Index)node).isModifiable());
		}

		public void apply(IndexTreeNode target) {
			final FileChooser fileChooser;
			String lastDir = null;

			if (config.getValue("lastSourceDirectory") != null)
				lastDir = config.getValue("lastSourceDirectory");

			if (lastDir == null)
				fileChooser = new FileChooser();
			else
				fileChooser = new FileChooser(lastDir);

			fileChooser.setDirectoryOnly(false);
			fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
			fileChooser.setTitle(I18n.getMessage("thaw.plugin.index.addFilesWithoutInserting"));

			final Vector files = fileChooser.askManyFiles();

			if(files == null)
				return;

			if (files.size() > 0) {
				config.setValue("lastSourceDirectory", fileChooser.getFinalDirectory());
			}

			final String category = FileCategory.promptForACategory();

			IndexManagementHelper.addFiles(getQueueManager(), getIndexBrowserPanel(), (Index)target, files, category, false);
		}
	}

	/**
	 * @param files See java.io.File
	 */
	public static void addFiles(final FCPQueueManager queueManager, final IndexBrowserPanel indexBrowser,
				    final Index target, final Vector files, final String category, final boolean insert) {
		if ((target == null) || (files == null))
			return;

		Hsqldb db;
		PreparedStatement selectSt;
		PreparedStatement st;
		int nextId;

		db = indexBrowser.getDb();
		
		Vector filesToManage = new Vector();

		synchronized(db.dbLock) {
			try {
				selectSt = db.getConnection().prepareStatement("SELECT id from files "+
						"WHERE indexParent = ? "+
						" AND LOWER(filename) LIKE ? "+
				"LIMIT 1");
				st = db.getConnection().prepareStatement("INSERT INTO files "+
						"(id, filename, publicKey, "+
						" localPath, mime, size, "+
						" category, indexParent, dontDelete) "+
						"VALUES (?, ?, ?, "+
						" ?, ?, ?, "+
				" ?, ?, TRUE)");

				nextId = DatabaseManager.getNextId(db, "files");

				if (nextId < 0) {
					selectSt.close();
					st.close();
					return;
				}
			} catch(SQLException e) {
				Logger.error(new IndexManagementHelper(), "Exception while trying to add file: "+e.toString());
				return;
			}


			for(final Iterator it = files.iterator();
			it.hasNext();) {

				final java.io.File ioFile = (java.io.File)it.next();

				try {
					selectSt.setInt(1, target.getId());
					selectSt.setString(2, ioFile.getName());

					ResultSet set = selectSt.executeQuery();

					if (set.next()) {
						/* this file is already in the index */
						continue;
					}


					st.setInt(1, nextId);
					st.setString(2, ioFile.getName());
					st.setString(3, ioFile.getName() /* stand as public key for the moment */);
					st.setString(4, ioFile.getAbsolutePath());
					st.setString(5, thaw.plugins.insertPlugin.DefaultMIMETypes.guessMIMEType(ioFile.getName()));
					st.setLong(6, ioFile.length());
					st.setNull(7 /* category */, Types.INTEGER /* not used at the moment */);
					st.setInt(8, target.getId());

					st.execute();

					File file = new File(db, nextId);
					
					filesToManage.add(file);

					nextId++;
				} catch(SQLException e) {
					Logger.error(new IndexManagementHelper(), "Error while adding file: "+e.toString());
				}
			}
			
			try {
				selectSt.close();
				st.close();
			} catch(SQLException e) {
				/* \_o< */
			}
		}
		
		for (Iterator it = filesToManage.iterator(); it.hasNext(); ){
			File f = (File)it.next();
			
			if (insert) {
				f.insertOnFreenet(queueManager);
			} else {
				f.recalculateCHK(queueManager);
			}			
		}

		indexBrowser.getTables().getFileTable().refresh();
	} /* addFiles() */



	public static class KeyAdder extends BasicIndexAction implements ThawRunnable, MouseListener {
		private JButton cancelButton = null;
		private JButton okButton = null;
		private JTextArea textArea = null;
		private JDialog frame = null;

		private JPopupMenu popupMenu = null;
		
		private IndexTreeNode target = null;

		public KeyAdder(final IndexBrowserPanel indexBrowser, final AbstractButton actionSource) {
			super(null, indexBrowser, actionSource);
		}
		
		public void setTargets(Vector nodes) {
			super.setTargets(nodes);
			
			if (nodes == null || nodes.size() != 1)
				getActionSource().setEnabled(false);
			else
				setTarget((IndexTreeNode)nodes.get(0));
		}

		public void setTarget(final IndexTreeNode node) {
			if (getActionSource() != null)
				getActionSource().setEnabled((node != null) && (node instanceof Index) && ((Index)node).isModifiable());
		}

		public void apply(IndexTreeNode target) {
			this.target = target;
			JLabel header = null;
			JPanel buttonPanel = null;

			frame = new JDialog(getIndexBrowserPanel().getMainWindow().getMainFrame(), I18n.getMessage("thaw.plugin.index.addKeys"));
			frame.setVisible(false);

			header = new JLabel(I18n.getMessage("thaw.plugin.fetch.keyList"));
			textArea = new JTextArea();
			buttonPanel = new JPanel();
			cancelButton = new JButton(I18n.getMessage("thaw.common.cancel"));
			okButton = new JButton(I18n.getMessage("thaw.common.ok"));

			popupMenu = new JPopupMenu();
			final JMenuItem item = new JMenuItem(I18n.getMessage("thaw.common.paste"));
			popupMenu.add(item);
			textArea.addMouseListener(this);
			new thaw.gui.GUIHelper.PasteHelper(item, textArea);

			cancelButton.addActionListener(this);
			okButton.addActionListener(this);

			frame.getContentPane().setLayout(new BorderLayout());
			frame.getContentPane().add(header, BorderLayout.NORTH);
			frame.getContentPane().add(new JScrollPane(textArea), BorderLayout.CENTER);

			buttonPanel.setLayout(new GridLayout(1, 2));
			buttonPanel.add(okButton);
			buttonPanel.add(cancelButton);

			frame.getContentPane().add(buttonPanel, BorderLayout.SOUTH);

			frame.setSize(500, 300);

			frame.setVisible(true);
		}

		public void actionPerformed(final ActionEvent e) {
			super.actionPerformed(e);

			if (e.getSource() == okButton) {
				final Vector keyVec = new Vector();

				frame.setVisible(false);
				frame.dispose();

				final String category = FileCategory.promptForACategory();

				final String[] keys = textArea.getText().split("\n");

				for (int i = 0 ; i < keys.length ; i++) {
					final String key = FreenetURIHelper.cleanURI(keys[i]);

					if (key != null) {
						keyVec.add(key);
					}
				}

				IndexManagementHelper.addKeys(getIndexBrowserPanel(), (Index)target, keyVec, category);
			}

			if (e.getSource() == cancelButton) {
				frame.setVisible(false);
				frame.dispose();
			}
		}

		public void mouseClicked(final MouseEvent e) { }
		public void mouseEntered(final MouseEvent e) { }
		public void mouseExited(final MouseEvent e) { }

		public void mousePressed(final MouseEvent e) {
			showPopupMenu(e);
		}

		public void mouseReleased(final MouseEvent e) {
			showPopupMenu(e);
		}

		protected void showPopupMenu(final MouseEvent e) {
			if(e.isPopupTrigger()) {
				popupMenu.show(e.getComponent(), e.getX(), e.getY());
			}
		}

	}


	/**
	 * @param keys => String
	 */
	public static void addKeys(final IndexBrowserPanel indexBrowser, final Index target, final Vector keys, final String category) {
		if ((target == null) || (keys == null))
			return;

		Hsqldb db;
		PreparedStatement st;
		PreparedStatement preSt;
		int nextId;

		db = indexBrowser.getDb();

		synchronized(db.dbLock) {
			try {
				preSt = db.getConnection().prepareStatement("SELECT id, publicKey FROM files "+
									    "WHERE indexParent = ? AND "+
									    "LOWER(publicKey) LIKE ? LIMIT 1");

				st = db.getConnection().prepareStatement("INSERT INTO files "+
									 "(id, filename, publicKey, localPath, "+
									 " mime, size, category, indexParent, dontDelete) "+
									 "VALUES (?, ?, ?, ?, ?, ?, ?, ?, TRUE)");
				nextId = DatabaseManager.getNextId(db, "files");

				if (nextId < 0) {
					preSt.close();
					st.close();
					return;
				}
			} catch(SQLException e) {
				Logger.error(new IndexManagementHelper(), "Exception while trying to add file: "+e.toString());
				return;
			}

			for(final Iterator it = keys.iterator();
			    it.hasNext();) {

				final String key = (String)it.next();

				try {
					preSt.setInt(1, target.getId());
					preSt.setString(2, FreenetURIHelper.getComparablePart(key) +"%");

					ResultSet res = preSt.executeQuery();

					boolean alreadyThere = false;

					if (res.next()) {
						String pubKey = res.getString("publicKey");

						if (FreenetURIHelper.compareKeys(key, pubKey))
							alreadyThere = true;
					}

					if (!alreadyThere) {

						st.setInt(1, nextId);
						st.setString(2, FreenetURIHelper.getFilenameFromKey(key));
						st.setString(3, key);
						st.setNull(4, Types.VARCHAR /* localPath */);
						st.setString(5, thaw.plugins.insertPlugin.DefaultMIMETypes.guessMIMEType(FreenetURIHelper.getFilenameFromKey(key)));
						st.setLong(6, 0L);
						st.setNull(7 /* category */, Types.INTEGER /* not used at the moment */);
						st.setInt(8, target.getId());

						st.execute();

						nextId++;
					} else {
						Logger.notice(target, "Key already in the specified index, not added");
					}
				} catch(SQLException e) {
					Logger.error(new IndexManagementHelper(), "Error while adding file: "+e.toString());
				}
			}

			try {
				preSt.close();
				st.close();
			} catch(SQLException e) {
				/* \_o< */
			}
		}

		indexBrowser.getTables().getFileTable().refresh();
	}



	public static class LinkAdder extends BasicIndexAction implements ThawRunnable {
		public LinkAdder(IndexBrowserPanel indexBrowser, final AbstractButton actionSource) {
			super(null, indexBrowser, actionSource);
		}
		
		public void setTargets(Vector nodes) {
			super.setTargets(nodes);
			
			if (nodes == null || nodes.size() != 1)
				getActionSource().setEnabled(false);
			else
				setTarget((IndexTreeNode)nodes.get(0));
		}

		public void setTarget(final IndexTreeNode node) {
			if (getActionSource() != null)
				getActionSource().setEnabled((node != null) && (node instanceof Index) && ((Index)node).isModifiable());
		}

		public void apply(IndexTreeNode target) {
			final IndexSelecter indexSelecter = new IndexSelecter(getIndexBrowserPanel());
			final String[] indexKeys = indexSelecter.askForIndexURIs(getIndexBrowserPanel().getDb());

			if (indexKeys != null) {
				for (int i = 0 ; i < indexKeys.length ; i++)
					IndexManagementHelper.addLink(getIndexBrowserPanel(), (Index)target, indexKeys[i]);
			}
		}
	}

	public static void addLink(final IndexBrowserPanel indexBrowser, final Index target, final String linkKey) {
		if ((target == null) || (linkKey == null))
			return;

		Hsqldb db = indexBrowser.getDb();

		synchronized(db.dbLock) {
			try {
				PreparedStatement st;

				int nextId = DatabaseManager.getNextId(db, "links");
				int catId = target.getCategoryId();

				st = db.getConnection().prepareStatement("INSERT INTO links (id, publicKey, mark, comment, indexParent, indexTarget, blackListed, category) "+
									 "VALUES (?, ?, ?, ?, ?, ?, FALSE, ?)");

				st.setInt(1, nextId);
				st.setString(2, linkKey);
				st.setInt(3, 0 /* mark : not used at the moment */);
				st.setString(4, "" /* comment : not used at the moment */);
				st.setInt(5, target.getId());
				st.setNull(6, Types.INTEGER /* indexTarget : not used at the moment */);
				
				if (catId >= 0)
					st.setInt(7, catId);
				else
					st.setNull(7, Types.INTEGER);

				st.execute();
				st.close();
			} catch(SQLException e) {
				Logger.error(new IndexManagementHelper(), "Error while adding link: "+e.toString());
			}
		}

		indexBrowser.getTables().getLinkTable().refresh();
	}



	public static class IndexFolderReorderer extends BasicIndexAction implements ThawRunnable {
		public IndexFolderReorderer(IndexBrowserPanel indexBrowser, final AbstractButton actionSource) {
			super(null, indexBrowser, actionSource);
		}

		public void setTargets(final Vector nodes) {
			super.setTargets(nodes);

			boolean e = false;
			
			if (nodes != null) {
				for (Iterator it = nodes.iterator() ; it.hasNext() ; ) {
					if (it.next() instanceof IndexFolder)
						e = true;
				}
			}
			
			e = e && nodes.size() > 0;
			
			getActionSource().setEnabled(e);
		}

		public void apply(IndexTreeNode target) {
			if (!(target instanceof IndexFolder))
				return;
			
			((IndexFolder)target).reorder();
			((IndexFolder)target).forceReload();
			getIndexBrowserPanel().getIndexTree().refresh(target);
		}
	}




	public static class IndexCommentAdder extends BasicIndexAction implements ThawRunnable, ActionListener {
		private JDialog dialog;

		private JComboBox author;
		private JTextArea textArea;
		private JButton okButton;
		private JButton cancelButton;

		private IndexTreeNode target;

		public IndexCommentAdder(FCPQueueManager queueManager,
					 IndexBrowserPanel indexBrowser,
					 final AbstractButton actionSource) {
			super(queueManager, indexBrowser, actionSource);

			if (actionSource != null)
				actionSource.setEnabled(false);
		}
		
		public void setTargets(Vector nodes) {
			super.setTargets(nodes);
			
			if (nodes == null || nodes.size() != 1)
				getActionSource().setEnabled(false);
			else
				setTarget((IndexTreeNode)nodes.get(0));
		}

		public void setTarget(final IndexTreeNode node) {
			if (getActionSource() != null) {
				getActionSource().setEnabled(node != null
							     && node instanceof Index
							     && ((Index)node).canHaveComments());
			}
		}


		private void showDialog() {
			if (dialog != null)
				return;


			dialog = new JDialog(getIndexBrowserPanel().getMainWindow().getMainFrame(),
					     I18n.getMessage("thaw.plugin.index.comment.add"));

			/*
			JLabel headerLabel = new JLabel(I18n.getMessage("thaw.plugin.index.comment.comment"),
							IconBox.addComment,
							JLabel.CENTER);
			*/

			JPanel authorPanel = new JPanel(new BorderLayout(5, 5));
			authorPanel.add(new JLabel(I18n.getMessage("thaw.plugin.index.comment.author"),
						   IconBox.addComment,
						   JLabel.LEFT),
					BorderLayout.WEST);

			author = new JComboBox(Identity.getYourIdentities(getIndexBrowserPanel().getDb()));
			authorPanel.add(author, BorderLayout.CENTER);

			JPanel header = new JPanel(new GridLayout(1, 1));

			//header.add(headerLabel);
			header.add(authorPanel);


			textArea = new JTextArea("");

			okButton = new JButton(I18n.getMessage("thaw.common.ok"));
			cancelButton = new JButton(I18n.getMessage("thaw.common.cancel"));

			okButton.addActionListener(this);
			cancelButton.addActionListener(this);

			JPanel buttonPanel = new JPanel(new GridLayout(1, 2));

			buttonPanel.add(okButton);
			buttonPanel.add(cancelButton);

			dialog.getContentPane().setLayout(new BorderLayout(5, 5));

			dialog.getContentPane().add(header, BorderLayout.NORTH);
			dialog.getContentPane().add(new JScrollPane(textArea), BorderLayout.CENTER);
			dialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);

			dialog.setSize(700, 300);
			dialog.setVisible(true);
		}


		public void actionPerformed(ActionEvent e) {
			boolean closeDialog = false;

			if (e.getSource() == getActionSource()) {
				super.actionPerformed(e);
			}

			if (e.getSource() == okButton) {
				if (target instanceof Index) {
					Identity i = ((Identity)author.getSelectedItem());

					if (i == null) {
						new WarningWindow(getIndexBrowserPanel().getMainWindow(),
								  I18n.getMessage("thaw.plugin.index.comment.mustSelectIdentity"));
						return;
					}

					((Index)target).postComment(getQueueManager(),
									 getIndexBrowserPanel().getMainWindow(),
									 i,
									 textArea.getText().trim());

					//if (getActionSource() != null)
					//	getActionSource().setEnabled(false);
				}

				closeDialog = true;
			}

			if (e.getSource() == cancelButton) {
				closeDialog = true;
			}

			if (closeDialog) {
				dialog.setVisible(false);
				dialog.dispose();
				dialog = null;
			}
		}


		public void apply(IndexTreeNode target) {
			this.target = target;
			showDialog();
		}
	}


	public static class IndexCommentViewer extends BasicIndexAction implements ThawRunnable {
		public IndexCommentViewer(IndexBrowserPanel indexBrowser, final AbstractButton actionSource) {
			super(null, indexBrowser, actionSource);

			if (actionSource != null)
				actionSource.setEnabled(false);
		}

		public void setTargets(Vector nodes) {
			super.setTargets(nodes);
			
			if (nodes == null || nodes.size() != 1)
				getActionSource().setEnabled(false);
			else
				setTarget((IndexTreeNode)nodes.get(0));
		}

		public void setTarget(final IndexTreeNode node) {
			if (getActionSource() != null)
				getActionSource().setEnabled(node != null
							     && node instanceof Index
							     && ((Index)node).canHaveComments());
		}

		public void apply(IndexTreeNode target) {
			getIndexBrowserPanel().getCommentTab().showTab();
		}
	}


	public static class IndexDetailsViewer extends BasicIndexAction implements ThawRunnable, ActionListener {
		private DateFormat dateFormat;

		public IndexDetailsViewer(IndexBrowserPanel indexBrowser, final AbstractButton actionSource) {
			super(null, indexBrowser, actionSource);

			dateFormat = DateFormat.getDateInstance();

			if (actionSource != null)
				actionSource.setEnabled(false);
		}
		
		public void setTargets(Vector nodes) {
			super.setTargets(nodes);
			
			if (nodes == null || nodes.size() != 1)
				getActionSource().setEnabled(false);
			else
				setTarget((IndexTreeNode)nodes.get(0));
		}


		public void setTarget(final IndexTreeNode node) {
			getActionSource().setEnabled(node != null);
		}


		private JDialog dialog;
		private JButton closeButton;

		private void displayDialog(MainWindow mainWindow,
					   int nmbFiles,
					   int nmbLinks,
					   java.sql.Date dateSql,
					   long totalSize) {

			String dateStr = null;

			if (dateSql != null)
				dateStr = dateFormat.format(dateSql);

			if (dateStr == null && dateSql != null)
				Logger.warning(this, "There is a date in the db, but I'm unable to print it");

			if (dateStr == null)
				dateStr = I18n.getMessage("thaw.common.unknown");;


			dialog = new JDialog(mainWindow.getMainFrame(),
					     I18n.getMessage("thaw.plugin.index.details"));

			dialog.getContentPane().setLayout(new BorderLayout(5, 5));

			JPanel statPanel = new JPanel(new GridLayout(4, 2));

			statPanel.add(new JLabel(I18n.getMessage("thaw.plugin.index.numberOfFiles")));
			statPanel.add(new JLabel(Integer.toString(nmbFiles), JLabel.RIGHT));

			statPanel.add(new JLabel(I18n.getMessage("thaw.plugin.index.numberOfLinks")));
			statPanel.add(new JLabel(Integer.toString(nmbLinks), JLabel.RIGHT));

			statPanel.add(new JLabel(I18n.getMessage("thaw.plugin.index.insertionDate")));
			statPanel.add(new JLabel(dateStr, JLabel.RIGHT));

			statPanel.add(new JLabel(I18n.getMessage("thaw.plugin.index.totalSize")));
			statPanel.add(new JLabel(thaw.gui.GUIHelper.getPrintableSize(totalSize), JLabel.RIGHT));

			dialog.getContentPane().add(statPanel, BorderLayout.CENTER);

			closeButton = new JButton(I18n.getMessage("thaw.common.ok"));
			closeButton.addActionListener(this);
			dialog.getContentPane().add(closeButton, BorderLayout.SOUTH);

			dialog.pack();

			dialog.setVisible(true);

		}

		public void actionPerformed(ActionEvent e) {
			if (e.getSource() == closeButton) {
				if (dialog != null) {
					dialog.setVisible(false);
					dialog.dispose();
					dialog = null;
				}
			} else {
				super.actionPerformed(e);
			}
		}


		public void apply(IndexTreeNode target) {
			IndexTreeNode node = target;

			Hsqldb db = getIndexBrowserPanel().getDb();
			PreparedStatement st;
			ResultSet rs;

			int nmbFilesInt = 0;
			int nmbLinksInt = 0;
			long totalSize = 0;
			java.sql.Date insertionDate = null;

			synchronized(db.dbLock) {
				try {
					if (node instanceof IndexFolder) {
						if (node instanceof IndexRoot) {
							st = db.getConnection().prepareStatement("SELECT count(id) from files");
							rs = st.executeQuery();
							rs.next();
							nmbFilesInt = rs.getInt(1);
							st.close();

							st = db.getConnection().prepareStatement("SELECT count(id) from links");
							rs = st.executeQuery();
							rs.next();
							nmbLinksInt = rs.getInt(1);
							st.close();

							st = db.getConnection().prepareStatement("SELECT sum(size) from files");
							rs = st.executeQuery();
							rs.next();
							totalSize = rs.getLong(1);
							st.close();
						} else {
							st = db.getConnection().prepareStatement("SELECT count(id) "+
												 "FROM files WHERE files.indexParent IN "+
												 "(SELECT indexParents.indexId "+
												 " FROM indexParents "+
												 " WHERE indexParents.folderId = ?)");

							st.setInt(1, node.getId());
							rs = st.executeQuery();
							rs.next();
							nmbFilesInt = rs.getInt(1);
							st.close();

							st = db.getConnection().prepareStatement("SELECT count(id) "+
												 "FROM links WHERE links.indexParent IN "+
												 "(SELECT indexParents.indexId "+
												 " FROM indexParents "+
												 " WHERE indexParents.folderId = ?)");
							st.setInt(1, node.getId());
							rs = st.executeQuery();
							rs.next();
							nmbLinksInt = rs.getInt(1);
							st.close();

							st = db.getConnection().prepareStatement("SELECT sum(files.size) "+
												 "FROM files WHERE files.indexParent IN "+
												 "(SELECT indexParents.indexId "+
												 " FROM indexParents "+
												 " WHERE indexParents.folderId = ?)");

							st.setInt(1, node.getId());
							rs = st.executeQuery();
							rs.next();
							totalSize = rs.getLong(1);
							st.close();
						}

						insertionDate = null;


					} else if (node instanceof Index) {
						/* mode lazy bastard => on */
						thaw.plugins.index.File[] files = ((Index)node).getFileList(null, true);

						nmbFilesInt = files.length;
						nmbLinksInt = ((Index)node).getLinkList(null, true).length;
						insertionDate = ((Index)node).getDate();

						totalSize = 0;

						for (int i = 0 ; i < files.length ; i++) {
							totalSize += ((thaw.plugins.index.File)files[i]).getSize();
						}
					}

				} catch(SQLException e) {
					Logger.error(this, "Exception while counting files/links : "+e.toString());
					return;
				}
			}

			displayDialog(getIndexBrowserPanel().getMainWindow(),
				      nmbFilesInt, nmbLinksInt, insertionDate, totalSize);
		}
	}


	public static class NodeNameDisplayer implements IndexAction {
		private AbstractButton button;

		public NodeNameDisplayer(AbstractButton source) {
			button = source;

			button.setEnabled(false);
		}

		public void setTargets(Vector nodes) {
			if (nodes == null || nodes.size() != 1) {
				button.setText("N/A");
				return;
			}

			IndexTreeNode node = (IndexTreeNode)nodes.get(0);

			if (node instanceof Index)
				button.setText(((Index)node).toString(false));
			else
				button.setText(node.toString());
		}

		public void actionPerformed(ActionEvent e) {

		}
	}
}
