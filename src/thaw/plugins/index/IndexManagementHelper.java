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
		public void setTarget(IndexTreeNode node);
	}


	public static abstract class BasicIndexAction
		implements IndexAction, ThawRunnable {

		private FCPQueueManager queueManager;
		private AbstractButton actionSource;
		private IndexTreeNode target;

		private IndexBrowserPanel indexBrowser;

		public BasicIndexAction(final FCPQueueManager queueManager,
					final IndexBrowserPanel indexBrowser,
					final AbstractButton actionSource) {
			this.indexBrowser = indexBrowser;
			this.actionSource = actionSource;
			target = null;
			this.queueManager = queueManager;

			if (actionSource != null) {
				actionSource.addActionListener(this);
			}
		}


		public AbstractButton getActionSource() {
			return actionSource;
		}

		public void setTarget(final IndexTreeNode node) {
			target = node;

		}

		public FCPQueueManager getQueueManager() {
			return queueManager;
		}

		public IndexTreeNode getTarget() {
			return target;
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
			apply();
		}

		public void stop() {
			/* \_o< */
		}

		public abstract void apply();
	}



	public static class IndexCreator extends BasicIndexAction implements Observer {
		public IndexCreator(final FCPQueueManager queueManager, final IndexBrowserPanel indexBrowser, final AbstractButton actionSource) {
			super(queueManager, indexBrowser, actionSource);
		}

		public void setTarget(final IndexTreeNode node) {
			super.setTarget(node);

			if (getActionSource() != null)
				getActionSource().setEnabled((node == null) || (node instanceof IndexFolder));
		}

		public void apply() {
			final String name = IndexManagementHelper.askAName(getIndexBrowserPanel().getMainWindow().getMainFrame(),
									   I18n.getMessage("thaw.plugin.index.indexName"),
									   I18n.getMessage("thaw.plugin.index.newIndex"));

			if (name == null)
				return;

			/* will create a dedicated IndexCreator */
			IndexManagementHelper.createIndex(getQueueManager(), getIndexBrowserPanel(), (IndexFolder)getTarget(), name);
		}


		private String name;

		/**
		 * Don't use directly
		 */
		public void createIndex(String name) {
			if (getTarget() == null)
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

					if (getTarget().getId() >= 0)
						st.setInt(11, getTarget().getId());
					else
						st.setNull(11, Types.INTEGER);

					st.execute();

					index = new Index(db, getIndexBrowserPanel().getConfig(),
							  id, (TreeNode)getTarget(),
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

			((MutableTreeNode)getTarget()).insert((index), 0);

			getIndexBrowserPanel().getIndexTree().refresh(getTarget());

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
		creator.createIndex(name);

	}



	public static class IndexModifier extends BasicIndexAction implements ThawRunnable {

		public IndexModifier(final FCPQueueManager queueManager, final IndexBrowserPanel indexBrowser, final AbstractButton actionSource) {
			super(queueManager, indexBrowser, actionSource);
		}

		public void setTarget(final IndexTreeNode node) {
			super.setTarget(node);

			if (getActionSource() != null)
				getActionSource().setEnabled((node != null) && (node instanceof Index));
		}

		public void apply() {
			final Index index = ((Index)getTarget());

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

		public void setTarget(final IndexTreeNode node) {
			super.setTarget(node);

			if (getActionSource() != null)
				getActionSource().setEnabled((node == null) || (node instanceof IndexFolder));
		}

		public void apply() {
			String publicKey = null;
			String privateKey = null;

			IndexConfigDialog dialog = new IndexConfigDialog(getIndexBrowserPanel(),
															getQueueManager());

			if (!dialog.promptUser()) /* cancelled */
				return;

			publicKey = dialog.getPublicKey();
			privateKey = dialog.getPrivateKey();

			IndexManagementHelper.reuseIndex(getQueueManager(), getIndexBrowserPanel(),
							 (IndexFolder)getTarget(), publicKey, privateKey,
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

		/* https://bugs.freenetproject.org/view.php?id=1625:
		 *  --- 0001625: Added index not in the correct category
		 *  If you add an index from the link list instead of the unknown index list,
		 *  it's added in the same folder than the index instead of "recently added".
		 */
		/*
		if (target != null)
			parent = target;
		else
		*/
		parent = indexBrowser.getIndexTree().getRoot().getRecentlyAddedFolder();

		int revision = FreenetURIHelper.getUSKRevision(publicKey);

		Hsqldb db = indexBrowser.getDb();

		Index index = null;

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
				st.setInt(7, 0 /* positionInTree */);
				st.setInt(8, revision);
				st.setBoolean(9, false);

				if (parent.getId() > 0)
					st.setInt(10, parent.getId());
				else
					st.setNull(10, Types.INTEGER);

				st.execute();

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

		((MutableTreeNode)parent).insert((index), 0);

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

		public void setTarget(final IndexTreeNode node) {
			super.setTarget(node);

			if (getActionSource() != null)
				getActionSource().setEnabled((node == null) || (node instanceof IndexFolder));
		}

		public void apply() {
			final String name = IndexManagementHelper.askAName(getIndexBrowserPanel().getMainWindow().getMainFrame(),
									   I18n.getMessage("thaw.plugin.index.categoryName"),
									   I18n.getMessage("thaw.plugin.index.newCategory"));

			if (name != null)
				IndexManagementHelper.addIndexFolder(getIndexBrowserPanel(), (IndexFolder)getTarget(), name);
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

		public void setTarget(final IndexTreeNode node) {
			super.setTarget(node);
		}

		public void apply() {
			getTarget().setHasChangedFlag(false);
			getTarget().setNewCommentFlag(false);
			getIndexBrowserPanel().getIndexTree().redraw(getTarget());
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

		public void setTarget(final IndexTreeNode node) {
			super.setTarget(node);
		}

		public void apply() {
			getTarget().downloadFromFreenet(this,
							getIndexBrowserPanel().getIndexTree(),
							getQueueManager());
			getIndexBrowserPanel().getIndexTree().redraw(getTarget());
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
		downloader.setTarget(target);

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
			Logger.notice(new IndexManagementHelper(), "Index already sorted.");
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
					      IndexTreeNode node) {
		if (node instanceof Index) {
			String cat = ((Index)node).getCategory();

			if (cat != null)
				return autoSortIndex(indexBrowser, (Index)node, cat);
			else
				Logger.notice(indexBrowser, "No category ; can't sort");

		} else if (node instanceof IndexFolder) {

			IndexFolder folder = ((IndexFolder)node);

			if (folder == null || "".equals(folder.toString())) {
				return false;
			}

			for (java.util.Enumeration children = folder.children();
			     children.hasMoreElements();) {

				/* dirty recursivity */
				IndexTreeNode subNode = (IndexTreeNode)children.nextElement();
				autoSortIndexes(indexBrowser, subNode);

			}
		}

		return true;
	}


	public static boolean autoSortIndex(IndexBrowserPanel indexBrowser,
					    Index index,
					    String cat) {
		if (cat == null) {
			Logger.warning(new IndexManagementHelper(), "No category ; Can't sort the index");
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

		public void setTarget(final IndexTreeNode node) {
			super.setTarget(node);

			if (getActionSource() != null)
				getActionSource().setEnabled(node != null);
		}

		public void apply() {
			if (getTarget() instanceof Index) {
				if (((Index)getTarget()).getCategory() == null) {
					Logger.warning(this, "No category => can't sort !");
					return;
				}
			}

			autoSortIndexes(getIndexBrowserPanel(),
					(IndexTreeNode)getTarget());
		}
	}


	public static class IndexUploader extends BasicIndexAction implements ThawRunnable, Observer {
		public IndexUploader(FCPQueueManager queueManager,
				     IndexBrowserPanel indexBrowser,
				     final AbstractButton actionSource) {

			super(queueManager, indexBrowser, actionSource);
		}

		public void setTarget(final IndexTreeNode node) {
			super.setTarget(node);

			if (getActionSource() != null)
				getActionSource().setEnabled((node != null) && node.isModifiable());
		}

		public void apply() {
			getTarget().insertOnFreenet(this, getIndexBrowserPanel(), getQueueManager());

			getIndexBrowserPanel().getIndexTree().redraw(getTarget());
		}

		public void update(Observable o, Object param) {
			getIndexBrowserPanel().getIndexTree().redraw(((Index)o));
		}

	}


	public static boolean insert(FCPQueueManager queueManager, IndexBrowserPanel indexBrowser, IndexTreeNode target) {
		IndexUploader uploader = new IndexUploader(queueManager, indexBrowser, null);
		uploader.setTarget(target);

		Thread th = new ThawThread(uploader, "Index inserter");
		th.start();

		return true;
	}


	public static class PublicKeyCopier extends BasicIndexAction {
		public PublicKeyCopier(final AbstractButton actionSource) {
			super(null, null, actionSource);
		}

		public void setTarget(final IndexTreeNode node) {
			super.setTarget(node);

			if (getActionSource() != null)
				getActionSource().setEnabled(node != null);
		}

		public void apply() {
			IndexManagementHelper.copyPublicKeyFrom(getTarget());
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

		public void setTarget(final IndexTreeNode node) {
			super.setTarget(node);

			if (getActionSource() != null)
				getActionSource().setEnabled((node != null) && (node instanceof Index) && node.isModifiable());
		}

		public void apply() {
			IndexManagementHelper.copyPrivateKeyFrom(getTarget());
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

		public void setTarget(final IndexTreeNode node) {
			super.setTarget(node);

			if (getActionSource() != null)
				getActionSource().setEnabled(node != null);
		}

		public void apply() {
			String newName;

			if (getTarget() instanceof Index) {
				newName = IndexManagementHelper.askAName(getIndexBrowserPanel().getMainWindow().getMainFrame(),
									 I18n.getMessage("thaw.plugin.index.indexName"),
									 ((Index)getTarget()).toString(false));
			} else {
				newName = IndexManagementHelper.askAName(getIndexBrowserPanel().getMainWindow().getMainFrame(),
									 I18n.getMessage("thaw.plugin.index.categoryName"),
									 getTarget().toString());
			}

			if (newName == null)
				return;

			IndexManagementHelper.renameNode(getIndexBrowserPanel(), getTarget(), newName);
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

		public void setTarget(final IndexTreeNode node) {
			super.setTarget(node);

			if (getActionSource() != null)
				getActionSource().setEnabled((node != null) && (node instanceof Index));
		}

		public void apply() {
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

			new IndexParser(((Index)getTarget())).generateXML(out);
			
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

		public void setTarget(final IndexTreeNode node) {
			super.setTarget(node);

			if (getActionSource() != null)
				getActionSource().setEnabled((node != null) && (node instanceof Index));
		}

		public void apply() {
			java.io.File newFile;

			final FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle(I18n.getMessage("thaw.plugin.index.importIndex"));
			fileChooser.setDirectoryOnly(false);
			fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
			newFile = fileChooser.askOneFile();

			if (newFile == null)
				return;

			new IndexParser(((Index)getTarget())).loadXML(newFile.getPath(), false);

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

		public void setTarget(final IndexTreeNode node) {
			super.setTarget(node);

			if (getActionSource() != null)
				getActionSource().setEnabled(node != null
							     && (getIndexBrowserPanel().getIndexTree() != null));
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

		public void apply() {
			IndexManagementHelper.deleteNode(getIndexBrowserPanel(), getTarget());
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

		public void setTarget(final IndexTreeNode node) {
			if (node != null && node instanceof Index)
				super.setTarget(node);
			else
				super.setTarget(null);
		}

		public void apply() {
			BlackList.addToBlackList(indexBrowser.getDb(), getTarget().getPublicKey());
			super.apply();
			indexBrowser.getBlackList().updateList();
		}
	}




	public static class FileInserterAndAdder extends BasicIndexAction {
		private Config config;

		public FileInserterAndAdder(final Config config, final FCPQueueManager queueManager, final IndexBrowserPanel indexBrowser, final AbstractButton actionSource) {
			super(queueManager, indexBrowser, actionSource);
			this.config = config;
		}

		public void setTarget(final IndexTreeNode node) {
			super.setTarget(node);

			if (getActionSource() != null)
				getActionSource().setEnabled((node != null) && (node instanceof Index) && ((Index)node).isModifiable());
		}

		public void apply() {
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

			IndexManagementHelper.addFiles(getQueueManager(), getIndexBrowserPanel(), (Index)getTarget(), files, category, true);
		}
	}


	public static class FileAdder extends BasicIndexAction {
		private Config config;

		public FileAdder(final Config config, final FCPQueueManager queueManager, final IndexBrowserPanel indexBrowser, final AbstractButton actionSource) {
			super(queueManager, indexBrowser, actionSource);
			this.config = config;
		}

		public void setTarget(final IndexTreeNode node) {
			super.setTarget(node);

			if (getActionSource() != null)
				getActionSource().setEnabled((node != null) && (node instanceof Index) && ((Index)node).isModifiable());
		}

		public void apply() {
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

			IndexManagementHelper.addFiles(getQueueManager(), getIndexBrowserPanel(), (Index)getTarget(), files, category, false);
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

				if (nextId < 0)
					return;
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

					if (insert) {
						file.insertOnFreenet(queueManager);
					} else {
						file.recalculateCHK(queueManager);
					}

					nextId++;
				} catch(SQLException e) {
					Logger.error(new IndexManagementHelper(), "Error while adding file: "+e.toString());
				}
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

		public KeyAdder(final IndexBrowserPanel indexBrowser, final AbstractButton actionSource) {
			super(null, indexBrowser, actionSource);
		}

		public void setTarget(final IndexTreeNode node) {
			super.setTarget(node);

			if (getActionSource() != null)
				getActionSource().setEnabled((node != null) && (node instanceof Index) && ((Index)node).isModifiable());
		}

		public void apply() {
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

				final String category = FileCategory.promptForACategory();

				final String[] keys = textArea.getText().split("\n");

				for (int i = 0 ; i < keys.length ; i++) {
					final String key = FreenetURIHelper.cleanURI(keys[i]);

					if (key != null) {
						keyVec.add(key);
					}
				}

				IndexManagementHelper.addKeys(getIndexBrowserPanel(), (Index)getTarget(), keyVec, category);
			}

			if (e.getSource() == cancelButton) {
				frame.setVisible(false);
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

				if (nextId < 0)
					return;
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
		}

		indexBrowser.getTables().getFileTable().refresh();
	}



	public static class LinkAdder extends BasicIndexAction implements ThawRunnable {
		public LinkAdder(IndexBrowserPanel indexBrowser, final AbstractButton actionSource) {
			super(null, indexBrowser, actionSource);
		}

		public void setTarget(final IndexTreeNode node) {
			super.setTarget(node);

			if (getActionSource() != null)
				getActionSource().setEnabled((node != null) && (node instanceof Index) && ((Index)node).isModifiable());
		}

		public void apply() {
			final IndexSelecter indexSelecter = new IndexSelecter(getIndexBrowserPanel());
			final String indexKey = indexSelecter.askForAnIndexURI(getIndexBrowserPanel().getDb());

			if (indexKey != null) {
				IndexManagementHelper.addLink(getIndexBrowserPanel(), (Index)getTarget(), indexKey);
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

				st = db.getConnection().prepareStatement("INSERT INTO links (id, publicKey, mark, comment, indexParent, indexTarget, blackListed) "+
									 "VALUES (?, ?, ?, ?, ?, ?, FALSE)");

				st.setInt(1, nextId);
				st.setString(2, linkKey);
				st.setInt(3, 0 /* mark : not used at the moment */);
				st.setString(4, "" /* comment : not used at the moment */);
				st.setInt(5, target.getId());
				st.setNull(6, Types.INTEGER /* indexTarget : not used at the moment */);

				st.execute();
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

		public void setTarget(final IndexTreeNode node) {
			super.setTarget(node);
			getActionSource().setEnabled(node != null && node instanceof IndexFolder);
		}

		public void apply() {
			if (getTarget() != null && getTarget() instanceof IndexFolder) {
				((IndexFolder)getTarget()).reorder();
				((IndexFolder)getTarget()).forceReload();
				getIndexBrowserPanel().getIndexTree().refresh(getTarget());
			}
			else
				Logger.notice(this, "No target ?!");
		}
	}




	public static class IndexCommentAdder extends BasicIndexAction implements ThawRunnable, ActionListener {
		private JDialog dialog;

		private JComboBox author;
		private JTextArea textArea;
		private JButton okButton;
		private JButton cancelButton;


		public IndexCommentAdder(FCPQueueManager queueManager,
					 IndexBrowserPanel indexBrowser,
					 final AbstractButton actionSource) {
			super(queueManager, indexBrowser, actionSource);

			if (actionSource != null)
				actionSource.setEnabled(false);
		}


		public void setTarget(final IndexTreeNode node) {
			super.setTarget(node);

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
				if (getTarget() instanceof Index) {
					Identity i = ((Identity)author.getSelectedItem());

					if (i == null) {
						new WarningWindow(getIndexBrowserPanel().getMainWindow(),
								  I18n.getMessage("thaw.plugin.index.comment.mustSelectIdentity"));
						return;
					}

					((Index)getTarget()).postComment(getQueueManager(),
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
				dialog = null;
			}
		}


		public void apply() {
			showDialog();
		}
	}


	public static class IndexCommentViewer extends BasicIndexAction implements ThawRunnable {
		public IndexCommentViewer(IndexBrowserPanel indexBrowser, final AbstractButton actionSource) {
			super(null, indexBrowser, actionSource);

			if (actionSource != null)
				actionSource.setEnabled(false);
		}


		public void setTarget(final IndexTreeNode node) {
			super.setTarget(node);

			if (getActionSource() != null)
				getActionSource().setEnabled(node != null
							     && node instanceof Index
							     && ((Index)node).canHaveComments());
		}

		public void apply() {
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


		public void setTarget(final IndexTreeNode node) {
			super.setTarget(node);

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
					dialog = null;
				}
			} else {
				super.actionPerformed(e);
			}
		}


		public void apply() {
			IndexTreeNode node = getTarget();

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

							st = db.getConnection().prepareStatement("SELECT count(id) from links");
							rs = st.executeQuery();
							rs.next();
							nmbLinksInt = rs.getInt(1);

							st = db.getConnection().prepareStatement("SELECT sum(size) from files");
							rs = st.executeQuery();
							rs.next();
							totalSize = rs.getLong(1);
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


							st = db.getConnection().prepareStatement("SELECT count(id) "+
												 "FROM links WHERE links.indexParent IN "+
												 "(SELECT indexParents.indexId "+
												 " FROM indexParents "+
												 " WHERE indexParents.folderId = ?)");
							st.setInt(1, node.getId());
							rs = st.executeQuery();
							rs.next();
							nmbLinksInt = rs.getInt(1);


							st = db.getConnection().prepareStatement("SELECT sum(files.size) "+
												 "FROM files WHERE files.indexParent IN "+
												 "(SELECT indexParents.indexId "+
												 " FROM indexParents "+
												 " WHERE indexParents.folderId = ?)");

							st.setInt(1, node.getId());
							rs = st.executeQuery();
							rs.next();
							totalSize = rs.getLong(1);

						}

						insertionDate = null;


					} else if (node instanceof Index) {
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

		public void setTarget(IndexTreeNode node) {
			if (node == null) {
				button.setText("N/A");
				return;
			}

			if (node instanceof Index)
				button.setText(((Index)node).toString(false));
			else
				button.setText(node.toString());
		}

		public void actionPerformed(ActionEvent e) {

		}
	}
}
