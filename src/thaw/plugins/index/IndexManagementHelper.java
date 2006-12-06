package thaw.plugins.index;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.FileOutputStream;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.tree.DefaultMutableTreeNode;

import thaw.core.FileChooser;
import thaw.core.FreenetURIHelper;
import thaw.core.I18n;
import thaw.core.Logger;
import thaw.core.MainWindow;
import thaw.fcp.FCPClientPut;
import thaw.fcp.FCPQueueManager;
import thaw.fcp.FCPTransferQuery;
import thaw.plugins.Hsqldb;

/**
 * Index.java, IndexCategory.java and IndexTree.java must NEVER use this helper (to avoid loops).
 */
public class IndexManagementHelper {

	private static String askAName(final String prompt, final String defVal) {
		return JOptionPane.showInputDialog(prompt, defVal);
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


	public static abstract class BasicIndexAction implements IndexAction {
		private Hsqldb db;
		private IndexTree tree;
		private FCPQueueManager queueManager;
		private UnknownIndexList uIndexList;
		private AbstractButton actionSource;
		private IndexTreeNode target;

		public BasicIndexAction(final Hsqldb db, final FCPQueueManager queueManager, final UnknownIndexList uIndexList, final IndexTree tree, final AbstractButton actionSource) {
			this.db = db;
			this.tree = tree;
			this.actionSource = actionSource;
			target = null;
			this.queueManager = queueManager;
			this.uIndexList = uIndexList;
			actionSource.addActionListener(this);
		}

		public AbstractButton getActionSource() {
			return actionSource;
		}

		public void setTarget(final IndexTreeNode node) {
			target = node;

		}

		public Hsqldb getDb() {
			return db;
		}

		public FCPQueueManager getQueueManager() {
			return queueManager;
		}

		public UnknownIndexList getUnknownIndexList() {
			return uIndexList;
		}

		public IndexTreeNode getTarget() {
			return target;
		}

		public IndexTree getTree() {
			return tree;
		}

		public abstract void actionPerformed(ActionEvent e);
	}



	public static class IndexCreator extends BasicIndexAction {
		public IndexCreator(final Hsqldb db, final FCPQueueManager queueManager, final UnknownIndexList uIndexList, final IndexTree tree, final AbstractButton actionSource) {
			super(db, queueManager, uIndexList, tree, actionSource);
		}

		public void setTarget(final IndexTreeNode node) {
			super.setTarget(node);
			getActionSource().setEnabled((node == null) || (node instanceof IndexCategory));
		}

		public void actionPerformed(final ActionEvent e) {
			final String name = IndexManagementHelper.askAName(I18n.getMessage("thaw.plugin.index.indexName"),
					       I18n.getMessage("thaw.plugin.index.newIndex"));

			if (name == null)
				return;

			IndexManagementHelper.createIndex(getDb(), getQueueManager(), getUnknownIndexList(), getTree(), (IndexCategory)getTarget(), name);
		}
	}

	public static void createIndex(final Hsqldb db, final FCPQueueManager queueManager, final UnknownIndexList uIndexList, final IndexTree tree, IndexCategory target, final String name) {
		if (target == null)
			target = tree.getRoot();

		if ((name == null) || (name.indexOf("/") >= 0)) {
			Logger.error(new IndexManagementHelper(), "invalid name");
			return;
		}

		final Index index = new Index(db, queueManager, uIndexList, -1, target, name, name, null, null, 0, null);

		index.generateKeys();
		index.create();

		tree.addToIndexCategory(target, index);
	}



	public static class KeyAsker implements ActionListener, MouseListener {
		private JButton okButton;
		private JButton cancelButton;
		private int formState;

		private JTextField publicKeyField = null;
		private JTextField privateKeyField = null;

		private JPopupMenu popupMenuA;
		private JPopupMenu popupMenuB;

		public KeyAsker() {
		}


		/**
		 * @return String[0] == publicKey ; String[1] == privateKey ; public or private are null if unchanged
		 */
		public static String[] askKeys(final boolean askPrivateKey,
					       final String defaultPublicKey,
					       final String defaultPrivateKey,
					       final MainWindow mainWindow) {
			return ((new KeyAsker()).askKeysBis(askPrivateKey, defaultPublicKey, defaultPrivateKey, mainWindow));
		}

		public synchronized String[] askKeysBis(final boolean askPrivateKey,
					   String defaultPublicKey,
					   String defaultPrivateKey,
					   final MainWindow mainWindow) {
			formState = 0;

			if (defaultPublicKey == null)
				defaultPublicKey = "USK@";

			if (defaultPrivateKey == null)
				defaultPrivateKey = "SSK@";

			final JDialog frame = new JDialog(mainWindow.getMainFrame(), I18n.getMessage("thaw.plugin.index.indexKey"));

			frame.getContentPane().setLayout(new BorderLayout());

			publicKeyField = new JTextField(defaultPublicKey);
			privateKeyField = new JTextField(defaultPrivateKey);

			final JPanel subPanelA = new JPanel(); /* left => labels */
			final JPanel subPanelB = new JPanel(); /* right => textfield */

			subPanelA.setLayout(new GridLayout(askPrivateKey ? 2 : 1, 1));
			subPanelB.setLayout(new GridLayout(askPrivateKey ? 2 : 1, 1));

			subPanelA.add(new JLabel(I18n.getMessage("thaw.plugin.index.indexKey")+ " "), BorderLayout.WEST);
			subPanelB.add(publicKeyField, BorderLayout.CENTER);

			popupMenuA = new JPopupMenu();
			JMenuItem item = new JMenuItem(I18n.getMessage("thaw.common.paste"));
			popupMenuA.add(item);
			new thaw.core.GUIHelper.PasteHelper(item, publicKeyField);
			publicKeyField.addMouseListener(this);

			if (askPrivateKey) {
				subPanelA.add(new JLabel(I18n.getMessage("thaw.plugin.index.indexPrivateKey")+" "), BorderLayout.WEST);
				subPanelB.add(privateKeyField, BorderLayout.CENTER);
				popupMenuB = new JPopupMenu();
				item = new JMenuItem(I18n.getMessage("thaw.common.paste"));
				popupMenuB.add(item);
				new thaw.core.GUIHelper.PasteHelper(item, privateKeyField);
				privateKeyField.addMouseListener(this);
			}

			frame.getContentPane().add(subPanelA, BorderLayout.WEST);
			frame.getContentPane().add(subPanelB, BorderLayout.CENTER);

			final JPanel subPanelC = new JPanel();
			subPanelC.setLayout(new GridLayout(1, 2));

			cancelButton = new JButton(I18n.getMessage("thaw.common.cancel"));
			okButton = new JButton(I18n.getMessage("thaw.common.ok"));

			cancelButton.addActionListener(this);
			okButton.addActionListener(this);

			subPanelC.add(okButton);
			subPanelC.add(cancelButton);

			frame.getContentPane().add(subPanelC, BorderLayout.SOUTH);

			frame.setSize(700, 100);
			frame.setVisible(true);

			/* TODO: DO IT BETTER YOU ù^{"(*µ */
			/*       VVVVVVVVVVV              */
			try {
				wait();
			} catch(final java.lang.InterruptedException e) {
				/* \_o< */
			}
			/*
			while(formState == 0) {
				try {
					Thread.sleep(500);
				} catch(InterruptedException e) {

				}
			}*/

			frame.setVisible(false);

			if (formState == 2)
				return null;

			final String[] keys = new String[2];

			keys[0] = publicKeyField.getText();
			if (askPrivateKey)
				keys[1] = privateKeyField.getText();
			else
				keys[1] = null;

			frame.dispose();

			if ((keys[0] == null) || (keys[0].length() < 20))
				return null;

			if ((keys[1] == null) || (keys[1].length() < 20))
				keys[1] = null;


			return keys;
		}

		public synchronized void actionPerformed(final ActionEvent e) {
			if (e.getSource() == okButton) {
				formState = 1;
			}

			if (e.getSource() == cancelButton) {
				formState = 2;
			}

			notify();
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
				if (e.getComponent() == publicKeyField)
					popupMenuA.show(e.getComponent(), e.getX(), e.getY());
				if (e.getComponent() == privateKeyField)
					popupMenuB.show(e.getComponent(), e.getX(), e.getY());
			}
		}

	}

	public static class IndexKeyModifier extends BasicIndexAction implements Runnable {
		private MainWindow mainWindow;

		public IndexKeyModifier(final MainWindow mainWindow, final AbstractButton actionSource) {
			super(null, null, null, null, actionSource);
			this.mainWindow = mainWindow;
		}

		public void setTarget(final IndexTreeNode node) {
			super.setTarget(node);
			getActionSource().setEnabled((node != null) || (node instanceof Index));
		}

		public void run() {
			final Index index = ((Index)getTarget());

			final String[] keys = KeyAsker.askKeys(true, index.getPublicKey(), index.getPrivateKey(), mainWindow);

			if (keys == null)
				return;

			index.setPrivateKey(keys[1]);
			index.setPublicKey(keys[0]);
		}

		public void actionPerformed(final ActionEvent e) {
			if (e.getSource() == getActionSource()) {
				final Thread th = new Thread(this);
				th.start();
			}
		}
	}


	public static class IndexReuser extends BasicIndexAction implements Runnable {
		private MainWindow mainWindow;

		public IndexReuser(final Hsqldb db, final FCPQueueManager queueManager, final UnknownIndexList uIndexList, final IndexTree tree, final MainWindow mainWindow, final AbstractButton actionSource) {
			super(db, queueManager, uIndexList, tree, actionSource);
			this.mainWindow = mainWindow;
		}

		public void setTarget(final IndexTreeNode node) {
			super.setTarget(node);
			getActionSource().setEnabled((node == null) || (node instanceof IndexCategory));
		}

		public void run() {
			String keys[];
			String publicKey = null;
			String privateKey = null;

			keys = KeyAsker.askKeys(true, "USK@", "SSK@", mainWindow);

			if (keys == null)
				return;

			publicKey = keys[0];
			privateKey = keys[1];

			IndexManagementHelper.reuseIndex(getDb(), getQueueManager(), getUnknownIndexList(), getTree(), (IndexCategory)getTarget(), publicKey, privateKey);
		}

		public void actionPerformed(final ActionEvent e) {
			if (e.getSource() == getActionSource()) {
				final Thread newThread = new Thread(this);
				newThread.start();
			}
		}
	}


	public static void addIndex(final Hsqldb db, final FCPQueueManager queueManager, final UnknownIndexList uIndexList, final IndexTree tree, final IndexCategory target, final String publicKey) {
		IndexManagementHelper.reuseIndex(db, queueManager, uIndexList, tree, target, publicKey, null);
	}


	/**
	 * Can be use directly
	 * @param privateKey Can be null
	 */
	public static void reuseIndex(final Hsqldb db, final FCPQueueManager queueManager, final UnknownIndexList uIndexList, final IndexTree tree, final IndexCategory target,
				      String publicKey, String privateKey) {

		publicKey = FreenetURIHelper.cleanURI(publicKey);
		privateKey = FreenetURIHelper.cleanURI(privateKey);

		if (publicKey == null)
			return;

		final String name = Index.getNameFromKey(publicKey);

		IndexCategory parent;

		if ((target != null) && (target instanceof IndexCategory))
			parent = target;
		else
			parent = tree.getRoot();

		final Index index = new Index(db, queueManager, uIndexList, -2, parent, name, name, publicKey, privateKey, 0, null);
		uIndexList.removeLink(index);

		index.create();

		index.updateFromFreenet(-1);

		parent.insert(index.getTreeNode(), 0);

		tree.reloadModel(parent);
	}




	public static class IndexCategoryAdder extends BasicIndexAction {
		public IndexCategoryAdder(final Hsqldb db, final FCPQueueManager queueManager, final UnknownIndexList uil, final IndexTree tree, final AbstractButton actionSource) {
			super(db, queueManager, uil, tree, actionSource);
		}

		public void setTarget(final IndexTreeNode node) {
			super.setTarget(node);
			getActionSource().setEnabled((node == null) || (node instanceof IndexCategory));
		}

		public void actionPerformed(final ActionEvent e) {
			final String name = IndexManagementHelper.askAName(I18n.getMessage("thaw.plugin.index.categoryName"),
					       I18n.getMessage("thaw.plugin.index.newCategory"));

			IndexManagementHelper.addIndexCategory(getDb(), getQueueManager(), getUnknownIndexList(), getTree(), (IndexCategory)getTarget(), name);
		}
	}

	public static void addIndexCategory(final Hsqldb db, final FCPQueueManager queueManager, final UnknownIndexList uIndexList, final IndexTree tree, IndexCategory target, final String name) {
		if (target == null)
			target = tree.getRoot();

		final IndexCategory newCat = new IndexCategory(db, queueManager, uIndexList, -2, target, name);

		newCat.create();

		tree.addToIndexCategory(target, newCat);
	}


	public static class IndexDownloader extends BasicIndexAction implements Runnable {
		public IndexDownloader(final AbstractButton actionSource) {
			super(null, null, null, null, actionSource);
		}

		public void setTarget(final IndexTreeNode node) {
			super.setTarget(node);
		}

		public void actionPerformed(final ActionEvent e) {
			final Thread newTh = new Thread(this);
			newTh.run();
		}

		public void run() {
			if (getTarget() != null)
				getTarget().updateFromFreenet(-1);
			else
				getTree().getRoot().updateFromFreenet(-1);
		}
	}

	public static class IndexUploader extends BasicIndexAction implements Runnable {
		public IndexUploader(final AbstractButton actionSource) {
			super(null, null, null, null, actionSource);
		}

		public void setTarget(final IndexTreeNode node) {
			super.setTarget(node);
			getActionSource().setEnabled((node != null) && node.isModifiable());
		}

		public void actionPerformed(final ActionEvent e) {
			final Thread newTh = new Thread(this);
			newTh.run();
		}

		public void run() {
			if (getTarget() != null)
				getTarget().update();
		}
	}


	public static class PublicKeyCopier extends BasicIndexAction {
		public PublicKeyCopier(final AbstractButton actionSource) {
			super(null, null, null, null, actionSource);
		}

		public void setTarget(final IndexTreeNode node) {
			super.setTarget(node);
			getActionSource().setEnabled(node != null);
		}

		public void actionPerformed(final ActionEvent e) {
			IndexManagementHelper.copyPublicKeyFrom(getTarget());
		}
	}


	public static void copyPublicKeyFrom(final IndexTreeNode node) {
		if (node == null)
			return;

		final Toolkit tk = Toolkit.getDefaultToolkit();
		final StringSelection st = new StringSelection(node.getPublicKey());
		final Clipboard cp = tk.getSystemClipboard();
		cp.setContents(st, null);
	}


	public static class PrivateKeyCopier extends BasicIndexAction {
		public PrivateKeyCopier(final AbstractButton actionSource) {
			super(null, null, null, null, actionSource);
		}

		public void setTarget(final IndexTreeNode node) {
			super.setTarget(node);
			getActionSource().setEnabled((node != null) && (node instanceof Index) && node.isModifiable());
		}

		public void actionPerformed(final ActionEvent e) {
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
		public IndexRenamer(final IndexTree tree, final AbstractButton actionSource) {
			super(null, null, null, tree, actionSource);
		}

		public void setTarget(final IndexTreeNode node) {
			super.setTarget(node);
			getActionSource().setEnabled(node != null);
		}

		public void actionPerformed(final ActionEvent e) {
			String newName;

			if (getTarget() instanceof Index) {
				newName = IndexManagementHelper.askAName(I18n.getMessage("thaw.plugin.index.indexName"),
						   getTarget().toString());
			} else {
				newName = IndexManagementHelper.askAName(I18n.getMessage("thaw.plugin.index.categoryName"),
						   getTarget().toString());
			}

			if (newName == null)
				return;

			IndexManagementHelper.renameNode(getTree(), getTarget(), newName);
		}
	}


	public static void renameNode(final IndexTree tree, final IndexTreeNode node, final String newName) {
		if ((node == null) || (newName == null))
			return;

		node.rename(newName);

		tree.reloadModel(node.getTreeNode());
	}



	public static class IndexExporter extends BasicIndexAction {
		public IndexExporter(final AbstractButton actionSource) {
			super(null, null, null, null, actionSource);
		}

		public void setTarget(final IndexTreeNode node) {
			super.setTarget(node);
			getActionSource().setEnabled((node != null) && (node instanceof Index));
		}

		public void actionPerformed(final ActionEvent e) {
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

			((Index)getTarget()).generateXML(out);
		}
	}


	public static class IndexImporter extends BasicIndexAction {
		public IndexImporter(final AbstractButton actionSource) {
			super(null, null, null, null, actionSource);
		}

		public void setTarget(final IndexTreeNode node) {
			super.setTarget(node);
			getActionSource().setEnabled((node != null) && (node instanceof Index));
		}

		public void actionPerformed(final ActionEvent e) {
			java.io.File newFile;

			final FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle(I18n.getMessage("thaw.plugin.index.importIndex"));
			fileChooser.setDirectoryOnly(false);
			fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
			newFile = fileChooser.askOneFile();

			if (newFile == null)
				return;

			((Index)getTarget()).loadXML(newFile.getPath());
		}
	}



	/**
	 * Can be used on indexes or index categories.
	 */
	public static class IndexDeleter extends BasicIndexAction {
		public IndexDeleter(final IndexTree tree, final AbstractButton actionSource) {
			super(null, null, null, tree, actionSource);
		}

		public void setTarget(final IndexTreeNode node) {
			super.setTarget(node);
			getActionSource().setEnabled(node != null);
		}

		public void actionPerformed(final ActionEvent e) {
			IndexManagementHelper.deleteNode(getTree(), getTarget());
		}
	}


	public static void deleteNode(final IndexTree tree, final IndexTreeNode node) {
		if (node == null)
			return;

		final DefaultMutableTreeNode parent = (DefaultMutableTreeNode)node.getTreeNode().getParent();

		if (parent != null)
			parent.remove(node.getTreeNode());

		node.delete();

		if (parent != null)
			tree.reloadModel(parent);
		else
			tree.reloadModel();
	}



	public static class FileInserterAndAdder extends BasicIndexAction {
		public FileInserterAndAdder(final Hsqldb db, final FCPQueueManager queueManager, final AbstractButton actionSource) {
			super(db, queueManager, null, null, actionSource);
		}

		public void setTarget(final IndexTreeNode node) {
			super.setTarget(node);
			getActionSource().setEnabled((node != null) && (node instanceof Index) && ((Index)node).isModifiable());
		}

		public void actionPerformed(final ActionEvent e) {
			final FileChooser fileChooser = new FileChooser();

			fileChooser.setDirectoryOnly(false);
			fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
			fileChooser.setTitle(I18n.getMessage("thaw.plugin.index.addFilesWithInserting"));

			final Vector files = fileChooser.askManyFiles();

			if(files == null)
				return;

			final String category = FileCategory.promptForACategory();

			IndexManagementHelper.addFiles(getDb(), getQueueManager(), (Index)getTarget(), files, category, true);
		}
	}


	public static class FileAdder extends BasicIndexAction {
		public FileAdder(final Hsqldb db, final FCPQueueManager queueManager, final AbstractButton actionSource) {
			super(db, queueManager, null, null, actionSource);
		}

		public void setTarget(final IndexTreeNode node) {
			super.setTarget(node);
			getActionSource().setEnabled((node != null) && (node instanceof Index) && ((Index)node).isModifiable());
		}

		public void actionPerformed(final ActionEvent e) {
			final FileChooser fileChooser = new FileChooser();

			fileChooser.setDirectoryOnly(false);
			fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
			fileChooser.setTitle(I18n.getMessage("thaw.plugin.index.addFilesWithoutInserting"));

			final Vector files = fileChooser.askManyFiles();

			if(files == null)
				return;

			final String category = FileCategory.promptForACategory();

			IndexManagementHelper.addFiles(getDb(), getQueueManager(), (Index)getTarget(), files, category, false);
		}
	}

	/**
	 * @param files See thaw.plugins.index.File
	 */
	public static void addFiles(final Hsqldb db, final FCPQueueManager queueManager,
				    final Index target, final Vector files, final String category, final boolean insert) {
		if ((target == null) || (files == null))
			return;

		for(final Iterator it = files.iterator();
		    it.hasNext();) {

			final java.io.File ioFile = (java.io.File)it.next();

			FCPTransferQuery insertion = null;

			if(insert) {
				insertion = new FCPClientPut(ioFile, 0, 0, null,
							     null, FCPClientPut.DEFAULT_INSERTION_PRIORITY,
							     true, 0, false);
				queueManager.addQueryToThePendingQueue(insertion);
			} else {
				insertion = new FCPClientPut(ioFile, 0, 0, null,
							     null, FCPClientPut.DEFAULT_INSERTION_PRIORITY,
							     true, 2, true); /* getCHKOnly */
				insertion.start(queueManager);
			}

			final thaw.plugins.index.File file = new thaw.plugins.index.File(db, ioFile.getPath(),
										   category, target,
										   insertion);

			((FCPClientPut)insertion).addObserver(file);

			target.addFile(file);

		}
	}



	public static class KeyAdder extends BasicIndexAction implements Runnable, MouseListener {
		private JButton cancelButton = null;
		private JButton okButton = null;
		private JTextArea textArea = null;
		private JDialog frame = null;

		private JPopupMenu popupMenu = null;
		private MainWindow mainWindow;

		public KeyAdder(final Hsqldb db, final MainWindow win, final AbstractButton actionSource) {
			super(db, null, null, null, actionSource);
			mainWindow = win;
		}

		public void setTarget(final IndexTreeNode node) {
			super.setTarget(node);
			getActionSource().setEnabled((node != null) && (node instanceof Index) && ((Index)node).isModifiable());
		}

		public void run() {
			JLabel header = null;
			JPanel buttonPanel = null;

			frame = new JDialog(mainWindow.getMainFrame(), I18n.getMessage("thaw.plugins.index.addKeys"));
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
			new thaw.core.GUIHelper.PasteHelper(item, textArea);

			cancelButton.addActionListener(this);
			okButton.addActionListener(this);

			frame.getContentPane().setLayout(new BorderLayout());
			frame.getContentPane().add(header, BorderLayout.NORTH);
			frame.getContentPane().add(textArea, BorderLayout.CENTER);

			buttonPanel.setLayout(new GridLayout(1, 2));
			buttonPanel.add(okButton);
			buttonPanel.add(cancelButton);

			frame.getContentPane().add(buttonPanel, BorderLayout.SOUTH);

			frame.setSize(500, 300);

			frame.setVisible(true);
		}

		public void actionPerformed(final ActionEvent e) {
			if (e.getSource() == getActionSource()) {
				final Thread newThread = new Thread(this);
				newThread.start();
			}

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

				IndexManagementHelper.addKeys(getDb(), (Index)getTarget(), keyVec, category);
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
	public static void addKeys(final Hsqldb db, final Index target, final Vector keys, final String category) {
		if ((target == null) || (keys == null))
			return;

		for(final Iterator it = keys.iterator();
		    it.hasNext();) {

			final String key = (String)it.next();

			final thaw.plugins.index.File file = new thaw.plugins.index.File(db, key, target);
			target.addFile(file);
		}
	}



	public static class LinkAdder extends BasicIndexAction implements Runnable {
		public LinkAdder(final Hsqldb db, final AbstractButton actionSource) {
			super(db, null, null, null, actionSource);
		}

		public void setTarget(final IndexTreeNode node) {
			super.setTarget(node);
			getActionSource().setEnabled((node != null) && (node instanceof Index) && ((Index)node).isModifiable());
		}

		public void run() {
			final IndexSelecter indexSelecter = new IndexSelecter();
			final String indexKey = indexSelecter.askForAnIndexURI(getDb());

			if (indexKey != null) {
				IndexManagementHelper.addLink(getDb(), (Index)getTarget(), indexKey);
			}
		}

		public void actionPerformed(final ActionEvent e) {
			final Thread newThread = new Thread(this);
			newThread.start();
		}
	}

	/**
	 * @param keys => String
	 */
	public static void addLink(final Hsqldb db, final Index target, final String linkKey) {
		if ((target == null) || (linkKey == null))
			return;

		final Link newLink = new Link(db, linkKey, target);
		target.addLink(newLink);
	}

}
