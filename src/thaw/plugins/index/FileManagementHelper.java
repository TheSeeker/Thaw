package thaw.plugins.index;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.AbstractButton;
import javax.swing.JFileChooser;

import thaw.core.Config;
import thaw.core.FileChooser;
import thaw.core.I18n;
import thaw.core.Logger;
import thaw.fcp.FCPQueueManager;


public class FileManagementHelper {
	/**
	 * Class implementing IndexAction will automatically do an addActionListener if necessary
	 */
	public interface FileAction extends ActionListener {

		/**
		 * Can disable the abstract button if required
		 * @param files can be null
		 */
		public void setTarget(Vector files);
	}




	public static class FileDownloader implements FileAction {
		private FCPQueueManager queueManager;
		private AbstractButton actionSource;
		private Vector target;
		private Config config;

		public FileDownloader(final Config config, final FCPQueueManager queueManager, final AbstractButton actionSource) {
			this.queueManager = queueManager;
			this.actionSource = actionSource;
			this.config = config;
			if (actionSource != null)
				actionSource.addActionListener(this);
		}

		public void setTarget(final Vector target) {
			this.target = target;
			actionSource.setEnabled((target != null) && (target.size() != 0));
		}

		public void actionPerformed(final ActionEvent e) {
			FileChooser fileChooser;

			if (config.getValue("lastDestinationDirectory") == null)
				fileChooser = new FileChooser();
			else
				fileChooser = new FileChooser(config.getValue("lastDestinationDirectory"));

			fileChooser.setDirectoryOnly(true);
			fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
			fileChooser.setTitle(I18n.getMessage("thaw.plugin.fetch.destinationDirectory"));

			final java.io.File destination = fileChooser.askOneFile();

			if (destination == null)
				return;

			config.setValue("lastDestinationDirectory", destination.getPath());

			FileManagementHelper.downloadFiles(queueManager, target, destination.getPath());
		}
	}


	/**
	 * @param files See thaw.plugins.index.File
	 */
	public static void downloadFiles(final FCPQueueManager queueManager,
					 final Vector files, final String destinationPath) {
		for (final Iterator it = files.iterator();
		     it.hasNext();) {
			final thaw.plugins.index.File file = (thaw.plugins.index.File)it.next();
			file.download(destinationPath, queueManager);
		}
	}


	public static class FileInserter implements FileAction {
		private FCPQueueManager queueManager;
		private AbstractButton actionSource;
		private Vector target;

		public FileInserter(final FCPQueueManager queueManager, final AbstractButton actionSource) {
			this.queueManager = queueManager;
			this.actionSource = actionSource;
			if (actionSource != null)
				actionSource.addActionListener(this);
		}

		public void setTarget(final Vector target) {
			boolean isOk;

			isOk = true;

			this.target = target;

			if (target != null) {
				for (final Iterator it = target.iterator();
				     it.hasNext(); ) {
					final thaw.plugins.index.File file = (thaw.plugins.index.File)it.next();

					if ((file.getLocalPath() == null)
					    || !file.isModifiable()
					    || file.getTransfer() != null) {
						isOk = false;
						break;
					}
				}
			}

			actionSource.setEnabled((target != null) && (target.size() != 0) && isOk);
		}

		public void actionPerformed(final ActionEvent e) {
			FileManagementHelper.insertFiles(queueManager, target);
		}
	}

	/**
	 * @param files See thaw.plugins.index.File
	 */
	public static void insertFiles(final FCPQueueManager queueManager,
				       final Vector files) {
		for (final Iterator it = files.iterator();
		     it.hasNext();) {
			final thaw.plugins.index.File file = (thaw.plugins.index.File)it.next();
			file.insertOnFreenet(queueManager);
		}
	}


	public static class FileKeyComputer implements FileAction {
		private FCPQueueManager queueManager;
		private AbstractButton actionSource;
		private Vector target;

		public FileKeyComputer(final FCPQueueManager queueManager, final AbstractButton actionSource) {
			this.queueManager = queueManager;
			this.actionSource = actionSource;
			if (actionSource != null)
				actionSource.addActionListener(this);
		}

		public void setTarget(final Vector target) {
			boolean isOk;

			isOk = true;
			this.target = target;

			if (target != null) {
				for (final Iterator it = target.iterator();
				     it.hasNext(); ) {
					final thaw.plugins.index.File file = (thaw.plugins.index.File)it.next();

					if ((file.getLocalPath() == null)
					    || !file.isModifiable()
					    || file.getTransfer() != null) {
						isOk = false;
						break;
					}
				}
			}

			actionSource.setEnabled((target != null) && (target.size() != 0) && isOk);
		}

		public void actionPerformed(final ActionEvent e) {
			Logger.notice(this, "COMPUTING");
			FileManagementHelper.computeFileKeys(queueManager, target);
		}
	}

	/**
	 * @param files See thaw.plugins.index.File
	 */
	public static void computeFileKeys(final FCPQueueManager queueManager,
					   final Vector files) {
		for (final Iterator it = files.iterator();
		     it.hasNext();) {
			final thaw.plugins.index.File file = (thaw.plugins.index.File)it.next();
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
		public FileRemover(final FCPQueueManager queueManager, final AbstractButton actionSource) {
			this.queueManager = queueManager;
			this.actionSource = actionSource;
			if (actionSource != null)
				actionSource.addActionListener(this);
		}

		public void setTarget(final Vector target) {
			boolean isOk;

			isOk = true;

			this.target = target;

			if (target != null) {
				for (final Iterator it = target.iterator();
				     it.hasNext();) {
					final thaw.plugins.index.File file = (thaw.plugins.index.File)it.next();

					if (!file.isModifiable()) {
						isOk = false;
						break;
					}
				}
			}


			actionSource.setEnabled((target != null) && (target.size() != 0) && isOk);
		}

		public void actionPerformed(final ActionEvent e) {
			FileManagementHelper.removeFiles(queueManager, target);
		}
	}

	/**
	 * @param files See thaw.plugins.index.File / files must have their parent correctly set
	 * @param queueManager Used to stop the transfer if needed
	 */
	public static void removeFiles(final FCPQueueManager queueManager,
				       final Vector files) {
		for (final Iterator it = files.iterator();
		     it.hasNext();) {
			final thaw.plugins.index.File file = (thaw.plugins.index.File)it.next();
			final Index parent = file.getParent();

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

		public PublicKeyCopier(final AbstractButton actionSource) {
			src = actionSource;
			if (src != null)
				src.addActionListener(this);
		}

		public void setTarget(final Vector targets) {
			t = targets;
			src.setEnabled((targets != null) && (targets.size() > 0));
		}

		public void actionPerformed(final ActionEvent e) {
			FileManagementHelper.copyPublicKeyFrom(t);
		}
	}


	public static void copyPublicKeyFrom(final Vector targets) {
		String keys = "";

		if (targets == null)
			return;

		final Toolkit tk = Toolkit.getDefaultToolkit();

		for(final Iterator it = targets.iterator();
		    it.hasNext();) {
			final thaw.plugins.index.File file = (thaw.plugins.index.File)it.next();
			keys = keys + file.getPublicKey() + "\n";
		}

		final StringSelection st = new StringSelection(keys);
		final Clipboard cp = tk.getSystemClipboard();
		cp.setContents(st, null);
	}
}
