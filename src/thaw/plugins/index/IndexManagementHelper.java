package thaw.plugins.index;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.AbstractButton;

import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.DefaultMutableTreeNode;

import javax.swing.JFrame;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JDialog;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JLabel;

import java.util.Vector;
import java.util.Iterator;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

import thaw.core.FileChooser;
import thaw.core.Config;
import thaw.core.I18n;
import thaw.core.FreenetURIHelper;
import thaw.plugins.Hsqldb;

import thaw.fcp.*;

/**
 * Index.java, IndexCategory.java and IndexTree.java must NEVER use this helper (to avoid loops).
 */
public class IndexManagementHelper {

	private static String askAName(String prompt, String defVal) {
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
		private AbstractButton actionSource;
		private IndexTreeNode target;

		public BasicIndexAction(Hsqldb db, FCPQueueManager queueManager, IndexTree tree, AbstractButton actionSource) {
			this.db = db;
			this.tree = tree;
			this.actionSource = actionSource;
			this.target = null;
			this.queueManager = queueManager;
			actionSource.addActionListener(this);
		}

		public AbstractButton getActionSource() {
			return actionSource;
		}

		public void setTarget(IndexTreeNode node) {
			target = node;

		}

		public Hsqldb getDb() {
			return db;
		}

		public FCPQueueManager getQueueManager() {
			return queueManager;
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
		public IndexCreator(Hsqldb db, FCPQueueManager queueManager, IndexTree tree, AbstractButton actionSource) {
			super(db, queueManager, tree, actionSource);
		}

		public void setTarget(IndexTreeNode node) {
			super.setTarget(node);
			getActionSource().setEnabled(node == null || node instanceof IndexCategory);
		}

		public void actionPerformed(ActionEvent e) {
			String name = askAName(I18n.getMessage("thaw.plugin.index.indexName"),
					       I18n.getMessage("thaw.plugin.index.newIndex"));

			if (name == null)
				return;

			createIndex(getDb(), getQueueManager(), getTree(), (IndexCategory)getTarget(), name);
		}
	}

	public static void createIndex(Hsqldb db, FCPQueueManager queueManager, IndexTree tree, IndexCategory target, String name) {
		if (target == null)
			target = tree.getRoot();

		Index index = new Index(db, queueManager, -1, target, name, name, null, null, 0, null);

		index.generateKeys();

		index.create(); /* TODO : To Check: can it be create without publicKey ? */

		tree.addToIndexCategory(target, index);
	}


	public static class IndexAdder extends BasicIndexAction {
		public IndexAdder(Hsqldb db, FCPQueueManager queueManager, IndexTree tree, AbstractButton actionSource) {
			super(db, queueManager, tree, actionSource);
		}

		public void setTarget(IndexTreeNode node) {
			super.setTarget(node);
			getActionSource().setEnabled(node == null || node instanceof IndexCategory);
		}

		public void actionPerformed(ActionEvent e) {
			String key = askAName(I18n.getMessage("thaw.plugin.index.indexKey"), "USK@");

			addIndex(getDb(), getQueueManager(), getTree(), (IndexCategory)getTarget(), key);
		}
	}


	/**
	 * Can be use directly
	 */
	public static void addIndex(Hsqldb db, FCPQueueManager queueManager, IndexTree tree, IndexCategory target, String publicKey) {
		publicKey = FreenetURIHelper.cleanURI(publicKey);

		if (publicKey == null)
			return;

		String name = Index.getNameFromKey(publicKey);

		if (name == null)
			return;

		if (target == null)
			target = tree.getRoot();

		Index index = new Index(db, queueManager, -2, target, name, name, publicKey, null, 0, null);

		if (tree.addToIndexCategory(target, index)) {
			index.create();
			index.updateFromFreenet(-1);
		}
	}



	public static class IndexReuser extends BasicIndexAction implements Runnable {
		private JButton okButton;
		private JButton cancelButton;
		private int formState;

		public IndexReuser(Hsqldb db, FCPQueueManager queueManager, IndexTree tree, AbstractButton actionSource) {
			super(db, queueManager, tree, actionSource);
		}

		public void setTarget(IndexTreeNode node) {
			super.setTarget(node);
			getActionSource().setEnabled(node == null || node instanceof IndexCategory);
		}

		protected String[] askKeys(boolean askPrivateKey) {
			formState = 0;

			JFrame frame = new JFrame(I18n.getMessage("thaw.plugin.index.indexKey"));

			frame.getContentPane().setLayout(new BorderLayout());

			JTextField publicKeyField = new JTextField("USK@");
			JTextField privateKeyField = new JTextField("SSK@");

			JPanel subPanelA = new JPanel(); /* left => labels */
			JPanel subPanelB = new JPanel(); /* right => textfield */

			subPanelA.setLayout(new GridLayout(askPrivateKey ? 2 : 1, 1));
			subPanelB.setLayout(new GridLayout(askPrivateKey ? 2 : 1, 1));

			subPanelA.add(new JLabel(I18n.getMessage("thaw.plugin.index.indexKey")+ " "), BorderLayout.WEST);
			subPanelB.add(publicKeyField, BorderLayout.CENTER);

			if (askPrivateKey) {
				subPanelA.add(new JLabel(I18n.getMessage("thaw.plugin.index.indexPrivateKey")+" "), BorderLayout.WEST);
				subPanelB.add(privateKeyField, BorderLayout.CENTER);
			}

			frame.getContentPane().add(subPanelA, BorderLayout.WEST);
			frame.getContentPane().add(subPanelB, BorderLayout.CENTER);

			JPanel subPanelC = new JPanel();
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

			while(formState == 0) {
				try {
					Thread.sleep(500);
				} catch(InterruptedException e) {
					/* \_o< */
				}
			}

			frame.setVisible(false);

			if (formState == 2)
				return null;

			String[] keys = new String[2];

			keys[0] = publicKeyField.getText();
			if (askPrivateKey)
				keys[1] = privateKeyField.getText();
			else
				keys[1] = null;

			if (keys[0].equals("USK@"))
				return null;

			if (keys[1].equals("SSK@"))
				keys[1] = null;

			return keys;
		}

		public void run() {
			String keys[];
			String publicKey = null;
			String privateKey = null;

			keys = askKeys(true);

			if (keys == null)
				return;

			publicKey = keys[0];
			privateKey = keys[1];

			reuseIndex(getDb(), getQueueManager(), getTree(), (IndexCategory)getTarget(), publicKey, privateKey);
		}

		public void actionPerformed(ActionEvent e) {
			if (e.getSource() == getActionSource()) {
				Thread newThread = new Thread(this);
				newThread.start();
			}

			if (e.getSource() == okButton) {
				formState = 1;
			}

			if (e.getSource() == cancelButton) {
				formState = 2;
			}
		}
	}


	/**
	 * Can be use directly
	 * @param privateKey Can be null
	 */
	public static void reuseIndex(Hsqldb db, FCPQueueManager queueManager, IndexTree tree, IndexCategory target,
				      String publicKey, String privateKey) {

		publicKey = FreenetURIHelper.cleanURI(publicKey);
		privateKey = FreenetURIHelper.cleanURI(privateKey);

		if (publicKey == null)
			return;

		String name = Index.getNameFromKey(publicKey);

		IndexCategory parent;

		if (target != null && target instanceof IndexCategory)
			parent = (IndexCategory)target;
		else
			parent = tree.getRoot();

		Index index = new Index(db, queueManager, -2, parent, name, name, publicKey, privateKey, 0, null);

		index.create();

		index.updateFromFreenet(-1);

		parent.insert(index.getTreeNode(), 0);

		tree.reloadModel(parent);
	}




	public static class IndexCategoryAdder extends BasicIndexAction {
		public IndexCategoryAdder(Hsqldb db, FCPQueueManager queueManager, IndexTree tree, AbstractButton actionSource) {
			super(db, queueManager, tree, actionSource);
		}

		public void setTarget(IndexTreeNode node) {
			super.setTarget(node);
			getActionSource().setEnabled(node == null || node instanceof IndexCategory);
		}

		public void actionPerformed(ActionEvent e) {
			String name = askAName(I18n.getMessage("thaw.plugin.index.categoryName"),
					       I18n.getMessage("thaw.plugin.index.newCategory"));

			addIndexCategory(getDb(), getQueueManager(), getTree(), (IndexCategory)getTarget(), name);
		}
	}

	public static void addIndexCategory(Hsqldb db, FCPQueueManager queueManager, IndexTree tree, IndexCategory target, String name) {
		if (target == null)
			target = tree.getRoot();

		IndexCategory newCat = new IndexCategory(db, queueManager, -2, target, name);

		newCat.create();

		tree.addToIndexCategory(target, newCat);
	}


	public static class IndexDownloader extends BasicIndexAction implements Runnable {
		public IndexDownloader(AbstractButton actionSource) {
			super(null, null, null, actionSource);
		}

		public void setTarget(IndexTreeNode node) {
			super.setTarget(node);
		}

		public void actionPerformed(ActionEvent e) {
			Thread newTh = new Thread(this);
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
		public IndexUploader(AbstractButton actionSource) {
			super(null, null, null, actionSource);
		}

		public void setTarget(IndexTreeNode node) {
			super.setTarget(node);
			getActionSource().setEnabled(node != null && node.isModifiable());
		}

		public void actionPerformed(ActionEvent e) {
			Thread newTh = new Thread(this);
			newTh.run();
		}

		public void run() {
			if (getTarget() != null)
				getTarget().update();
		}
	}


	public static class PublicKeyCopier extends BasicIndexAction {
		public PublicKeyCopier(AbstractButton actionSource) {
			super(null, null, null, actionSource);
		}

		public void setTarget(IndexTreeNode node) {
			super.setTarget(node);
			getActionSource().setEnabled(node != null);
		}

		public void actionPerformed(ActionEvent e) {
			copyPublicKeyFrom(getTarget());
		}
	}


	public static void copyPublicKeyFrom(IndexTreeNode node) {
		if (node == null)
			return;

		Toolkit tk = Toolkit.getDefaultToolkit();
		StringSelection st = new StringSelection(node.getPublicKey());
		Clipboard cp = tk.getSystemClipboard();
		cp.setContents(st, null);
	}


	public static class PrivateKeyCopier extends BasicIndexAction {
		public PrivateKeyCopier(AbstractButton actionSource) {
			super(null, null, null, actionSource);
		}

		public void setTarget(IndexTreeNode node) {
			super.setTarget(node);
			getActionSource().setEnabled(node != null && node instanceof Index && node.isModifiable());
		}

		public void actionPerformed(ActionEvent e) {
			copyPrivateKeyFrom(getTarget());
		}
	}


	public static void copyPrivateKeyFrom(IndexTreeNode node) {
		if (node == null)
			return;

		Toolkit tk = Toolkit.getDefaultToolkit();
		StringSelection st = new StringSelection(node.getPrivateKey());
		Clipboard cp = tk.getSystemClipboard();
		cp.setContents(st, null);
	}

	/**
	 * Can rename indexes or index categories.
	 */
	public static class IndexRenamer extends BasicIndexAction {
		public IndexRenamer(IndexTree tree, AbstractButton actionSource) {
			super(null, null, tree, actionSource);
		}

		public void setTarget(IndexTreeNode node) {
			super.setTarget(node);
			getActionSource().setEnabled(node != null);
		}

		public void actionPerformed(ActionEvent e) {
			String newName;

			if (getTarget() instanceof Index) {
				newName = askAName(I18n.getMessage("thaw.plugin.index.indexName"),
						   getTarget().toString());
			} else {
				newName = askAName(I18n.getMessage("thaw.plugin.index.categoryName"),
						   getTarget().toString());
			}

			if (newName == null)
				return;

			renameNode(getTree(), getTarget(), newName);
		}
	}


	public static void renameNode(IndexTree tree, IndexTreeNode node, String newName) {
		if (node == null || newName == null)
			return;

		node.rename(newName);

		tree.reloadModel(node.getTreeNode());
	}



	/**
	 * Can be used on indexes or index categories.
	 */
	public static class IndexDeleter extends BasicIndexAction {
		public IndexDeleter(IndexTree tree, AbstractButton actionSource) {
			super(null, null, tree, actionSource);
		}

		public void setTarget(IndexTreeNode node) {
			super.setTarget(node);
			getActionSource().setEnabled(node != null);
		}

		public void actionPerformed(ActionEvent e) {
			deleteNode(getTree(), getTarget());
		}
	}


	public static void deleteNode(IndexTree tree, IndexTreeNode node) {
		if (node == null)
			return;

		DefaultMutableTreeNode parent = (DefaultMutableTreeNode)node.getTreeNode().getParent();

		if (parent != null)
			parent.remove(node.getTreeNode());

		node.delete();

		if (parent != null)
			tree.reloadModel(parent);
		else
			tree.reloadModel();
	}



	public static class FileInserterAndAdder extends BasicIndexAction {
		public FileInserterAndAdder(Hsqldb db, FCPQueueManager queueManager, AbstractButton actionSource) {
			super(db, queueManager, null, actionSource);
		}

		public void setTarget(IndexTreeNode node) {
			super.setTarget(node);
			getActionSource().setEnabled(node != null && node instanceof Index && ((Index)node).isModifiable());
		}

		public void actionPerformed(ActionEvent e) {
			FileChooser fileChooser = new FileChooser();

			fileChooser.setDirectoryOnly(false);
			fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
			fileChooser.setTitle(I18n.getMessage("thaw.plugin.index.addFilesWithInserting"));

			Vector files = fileChooser.askManyFiles();

			if(files == null)
				return;

			String category = FileCategory.promptForACategory();

			addFiles(getDb(), getQueueManager(), (Index)getTarget(), files, category, true);
		}
	}


	public static class FileAdder extends BasicIndexAction {
		public FileAdder(Hsqldb db, FCPQueueManager queueManager, AbstractButton actionSource) {
			super(db, queueManager, null, actionSource);
		}

		public void setTarget(IndexTreeNode node) {
			super.setTarget(node);
			getActionSource().setEnabled(node != null && node instanceof Index && ((Index)node).isModifiable());
		}

		public void actionPerformed(ActionEvent e) {
			FileChooser fileChooser = new FileChooser();

			fileChooser.setDirectoryOnly(false);
			fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
			fileChooser.setTitle(I18n.getMessage("thaw.plugin.index.addFilesWithoutInserting"));

			Vector files = fileChooser.askManyFiles();

			if(files == null)
				return;

			String category = FileCategory.promptForACategory();

			addFiles(getDb(), getQueueManager(), (Index)getTarget(), files, category, false);
		}
	}

	/**
	 * @param files See thaw.plugins.index.File
	 */
	public static void addFiles(Hsqldb db, FCPQueueManager queueManager,
				    Index target, Vector files, String category, boolean insert) {
		if (target == null || files == null)
			return;

		for(Iterator it = files.iterator();
		    it.hasNext();) {

			java.io.File ioFile = (java.io.File)it.next();

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

			thaw.plugins.index.File file = new thaw.plugins.index.File(db, ioFile.getPath(),
										   category, target,
										   insertion);

			((FCPClientPut)insertion).addObserver(file);

			target.addFile(file);

		}
	}



	public static class KeyAdder extends BasicIndexAction implements Runnable {
		private JButton cancelButton = null;
		private JButton okButton = null;
		private JTextArea textArea = null;
		private JFrame frame = null;

		public KeyAdder(Hsqldb db, AbstractButton actionSource) {
			super(db, null, null, actionSource);
		}

		public void setTarget(IndexTreeNode node) {
			super.setTarget(node);
			getActionSource().setEnabled(node != null && node instanceof Index && ((Index)node).isModifiable());
		}

		public void run() {
			JLabel header = null;
			JPanel buttonPanel = null;

			frame = new JFrame(I18n.getMessage("thaw.plugins.index.addKeys"));
			frame.setVisible(false);

			header = new JLabel(I18n.getMessage("thaw.plugin.fetch.keyList"));
			textArea = new JTextArea();
			buttonPanel = new JPanel();
			cancelButton = new JButton(I18n.getMessage("thaw.common.cancel"));
			okButton = new JButton(I18n.getMessage("thaw.common.ok"));

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

		public void actionPerformed(ActionEvent e) {
			if (e.getSource() == getActionSource()) {
				Thread newThread = new Thread(this);
				newThread.start();
			}

			if (e.getSource() == okButton) {
				Vector keyVec = new Vector();

				frame.setVisible(false);

				String category = FileCategory.promptForACategory();

				String[] keys = textArea.getText().split("\n");

				for (int i = 0 ; i < keys.length ; i++) {
					String key = FreenetURIHelper.cleanURI(keys[i]);

					if (key != null) {
						keyVec.add(key);
					}
				}

				addKeys(getDb(), (Index)getTarget(), keyVec, category);
			}

			if (e.getSource() == cancelButton) {
				frame.setVisible(false);
			}
		}
	}

	/**
	 * @param keys => String
	 */
	public static void addKeys(Hsqldb db, Index target, Vector keys, String category) {
		if (target == null || keys == null)
			return;

		for(Iterator it = keys.iterator();
		    it.hasNext();) {

			String key = (String)it.next();

			thaw.plugins.index.File file = new thaw.plugins.index.File(db, key, target);
			target.addFile(file);
		}
	}



	public static class LinkAdder extends BasicIndexAction implements Runnable {
		private JButton cancelButton = null;
		private JButton okButton = null;
		private JTextArea textArea = null;
		private JFrame frame = null;

		public LinkAdder(Hsqldb db, AbstractButton actionSource) {
			super(db, null, null, actionSource);
		}

		public void setTarget(IndexTreeNode node) {
			super.setTarget(node);
			getActionSource().setEnabled(node != null && node instanceof Index && ((Index)node).isModifiable());
		}

		public void run() {
			IndexSelecter indexSelecter = new IndexSelecter();
			String indexKey = indexSelecter.askForAnIndexURI(getDb());

			if (indexKey != null) {
				addLink(getDb(), (Index)getTarget(), indexKey);
			}
		}

		public void actionPerformed(ActionEvent e) {
			Thread newThread = new Thread(this);
			newThread.start();
		}
	}

	/**
	 * @param keys => String
	 */
	public static void addLink(Hsqldb db, Index target, String linkKey) {
		if (target == null || linkKey == null)
			return;

		Link newLink = new Link(db, linkKey, target);
		target.addLink(newLink);
	}

}
