package thaw.plugins.index;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.AbstractButton;

import javax.swing.JFileChooser;

import java.util.Vector;
import java.util.Iterator;

import thaw.core.FileChooser;
import thaw.core.Config;
import thaw.core.I18n;
import thaw.plugins.Hsqldb;
import thaw.core.Logger;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

import thaw.fcp.*;

public class FileManagementHelper {
	/**
	 * Class implementing IndexAction will automatically do an addActionListener if necessary
	 */
	public interface FileAction extends ActionListener {

		/**
		 * Can disable the abstract button if required
		 * @param node can be null
		 */
		public void setTarget(Vector files);
	}




	public static class FileDownloader implements FileAction {
		private FCPQueueManager queueManager;
		private AbstractButton actionSource;
		private Vector target;
		private Config config;

		public FileDownloader(Config config, FCPQueueManager queueManager, AbstractButton actionSource) {
			this.queueManager = queueManager;
			this.actionSource = actionSource;
			this.config = config;
			if (actionSource != null)
				actionSource.addActionListener(this);
		}

		public void setTarget(Vector target) {
			this.target = target;
			actionSource.setEnabled(target != null && target.size() != 0);
		}

		public void actionPerformed(ActionEvent e) {
			FileChooser fileChooser;

			if (this.config.getValue("lastDestinationDirectory") == null)
				fileChooser = new FileChooser();
			else
				fileChooser = new FileChooser(config.getValue("lastDestinationDirectory"));

			fileChooser.setDirectoryOnly(true);
			fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
			fileChooser.setTitle(I18n.getMessage("thaw.plugin.fetch.destinationDirectory"));

			java.io.File destination = fileChooser.askOneFile();

			if (destination == null)
				return;

			config.setValue("lastDestinationDirectory", destination.getPath());

			downloadFiles(queueManager, target, destination.getPath());
		}
	}


	/**
	 * @param files See thaw.plugins.index.File
	 */
	public static void downloadFiles(FCPQueueManager queueManager,
					 Vector files, String destinationPath) {
		for (Iterator it = files.iterator();
		     it.hasNext();) {
			thaw.plugins.index.File file = (thaw.plugins.index.File)it.next();
			file.download(destinationPath, queueManager);
		}
	}


	public static class FileInserter implements FileAction {
		private FCPQueueManager queueManager;
		private AbstractButton actionSource;
		private Vector target;

		public FileInserter(FCPQueueManager queueManager, AbstractButton actionSource) {
			this.queueManager = queueManager;
			this.actionSource = actionSource;
			if (actionSource != null)
				actionSource.addActionListener(this);
		}

		public void setTarget(Vector target) {
			boolean isOk;

			isOk = true;

			this.target = target;

			if (target != null) {
				for (Iterator it = target.iterator();
				     it.hasNext(); ) {
					thaw.plugins.index.File file = (thaw.plugins.index.File)it.next();

					if (file.getLocalPath() == null
					    || !file.isModifiable()) {
						isOk = false;
						break;
					}
				}
			}

			actionSource.setEnabled(target != null && target.size() != 0 && isOk);
		}

		public void actionPerformed(ActionEvent e) {
			insertFiles(queueManager, target);
		}
	}

	/**
	 * @param files See thaw.plugins.index.File
	 */
	public static void insertFiles(FCPQueueManager queueManager,
				       Vector files) {
		for (Iterator it = files.iterator();
		     it.hasNext();) {
			thaw.plugins.index.File file = (thaw.plugins.index.File)it.next();
			file.insertOnFreenet(queueManager);
		}
	}


	public static class FileKeyComputer implements FileAction {
		private FCPQueueManager queueManager;
		private AbstractButton actionSource;
		private Vector target;

		public FileKeyComputer(FCPQueueManager queueManager, AbstractButton actionSource) {
			this.queueManager = queueManager;
			this.actionSource = actionSource;
			if (actionSource != null)
				actionSource.addActionListener(this);
		}

		public void setTarget(Vector target) {
			boolean isOk;

			isOk = true;
			this.target = target;

			if (target != null) {
				for (Iterator it = target.iterator();
				     it.hasNext(); ) {
					thaw.plugins.index.File file = (thaw.plugins.index.File)it.next();

					if (file.getLocalPath() == null
					    || !file.isModifiable()) {
						isOk = false;
						break;
					}
				}
			}

			actionSource.setEnabled(target != null && target.size() != 0 && isOk);
		}

		public void actionPerformed(ActionEvent e) {
			Logger.notice(this, "COMPUTING");
			computeFileKeys(queueManager, target);
		}
	}

	/**
	 * @param files See thaw.plugins.index.File
	 */
	public static void computeFileKeys(FCPQueueManager queueManager,
					   Vector files) {
		for (Iterator it = files.iterator();
		     it.hasNext();) {
			thaw.plugins.index.File file = (thaw.plugins.index.File)it.next();
			file.recalculateCHK(queueManager);
		}
	}


	public static class FileRemover implements FileAction {
		private FCPQueueManager queueManager;
		private AbstractButton actionSource;
		private Vector target;

		/**
		 * @param queueManager is used to stop transfers if needed
		 */
		public FileRemover(FCPQueueManager queueManager, AbstractButton actionSource) {
			this.queueManager = queueManager;
			this.actionSource = actionSource;
			if (actionSource != null)
				actionSource.addActionListener(this);
		}

		public void setTarget(Vector target) {
			boolean isOk;

			isOk = true;

			this.target = target;

			if (target != null) {
				for (Iterator it = target.iterator();
				     it.hasNext();) {
					thaw.plugins.index.File file = (thaw.plugins.index.File)it.next();

					if (!file.isModifiable()) {
						isOk = false;
						break;
					}
				}
			}


			actionSource.setEnabled(target != null && target.size() != 0 && isOk);
		}

		public void actionPerformed(ActionEvent e) {
			removeFiles(queueManager, target);
		}
	}

	/**
	 * @param files See thaw.plugins.index.File / files must have their parent correctly set
	 * @param queueManager Used to stop the transfer if needed
	 */
	public static void removeFiles(FCPQueueManager queueManager,
				       Vector files) {
		for (Iterator it = files.iterator();
		     it.hasNext();) {
			thaw.plugins.index.File file = (thaw.plugins.index.File)it.next();
			Index parent = file.getParent();

			if (parent == null) {
				Logger.warning(new FileManagementHelper(), "File '"+file.getFilename()+"' has no parent ?!");
				continue;
			}

			if (file.getTransfer() != null)
				file.getTransfer().stop(queueManager);

			parent.removeFile(file);
		}
	}


	public static class PublicKeyCopier implements FileAction {
		private AbstractButton src;
		private Vector t;

		public PublicKeyCopier(AbstractButton actionSource) {
			src = actionSource;
		}

		public void setTarget(Vector targets) {
			t = targets;
			src.setEnabled(targets != null && targets.size() > 0);
		}

		public void actionPerformed(ActionEvent e) {
			copyPublicKeyFrom(t);
		}
	}


	public static void copyPublicKeyFrom(Vector targets) {
		String keys = "";

		if (targets == null)
			return;

		Toolkit tk = Toolkit.getDefaultToolkit();

		for(Iterator it = targets.iterator();
		    it.hasNext();) {
			thaw.plugins.index.File file = (thaw.plugins.index.File)it.next();
			keys = keys + file.getPublicKey() + "\n";
		}

		StringSelection st = new StringSelection(keys);
		Clipboard cp = tk.getSystemClipboard();
		cp.setContents(st, null);
	}
}
