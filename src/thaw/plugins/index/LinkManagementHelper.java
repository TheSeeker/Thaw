package thaw.plugins.index;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.AbstractButton;

import thaw.fcp.FCPQueueManager;
import thaw.plugins.Hsqldb;

public class LinkManagementHelper {
	public interface LinkAction extends ActionListener {

		/**
		 * Can disable the abstract button if required
		 * @param links can be null
		 */
		public void setTarget(Vector links);
	}


	public static class LinkRemover implements LinkAction {
		private AbstractButton actionSource;
		private Vector target;

		public LinkRemover(final AbstractButton actionSource) {
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
					final Link link = (Link)it.next();

					if (!link.isModifiable()) {
						isOk = false;
						break;
					}
				}
			}


			actionSource.setEnabled((target != null) && (target.size() != 0) && isOk);
		}

		public void actionPerformed(final ActionEvent e) {
			LinkManagementHelper.removeLinks(target);
		}
	}


	public static void removeLinks(final Vector links) {
		if (links == null)
			return;

		for (final Iterator it = links.iterator();
		     it.hasNext() ; ) {
			final Link link = (Link)it.next();
			link.getParent().removeLink(link);
		}
	}


	public static class IndexAdder implements LinkAction {
		private FCPQueueManager queueManager;
		private IndexBrowserPanel indexBrowser;

		private AbstractButton src;
		private Vector t;

		public IndexAdder(final AbstractButton actionSource,
				  final FCPQueueManager queueManager,
				  final IndexBrowserPanel indexBrowser) {
			src = actionSource;
			if (actionSource != null)
				actionSource.addActionListener(this);
			this.queueManager = queueManager;
			this.indexBrowser = indexBrowser;
		}

		public void setTarget(final Vector targets) {
			t = targets;
			src.setEnabled((targets != null) && (targets.size() > 0));
		}

		public void actionPerformed(final ActionEvent e) {
			for (final Iterator it = t.iterator();
			     it.hasNext(); ) {
				final Link link = (Link)it.next();
				IndexManagementHelper.addIndex(queueManager, indexBrowser, null,
							       link.getPublicKey());
			}
		}
	}


	public static class PublicKeyCopier implements LinkAction {
		private AbstractButton src;
		private Vector t;

		public PublicKeyCopier(final AbstractButton actionSource) {
			src = actionSource;
			if (actionSource != null)
				actionSource.addActionListener(this);
		}

		public void setTarget(final Vector targets) {
			t = targets;
			src.setEnabled((targets != null) && (targets.size() > 0));
		}

		public void actionPerformed(final ActionEvent e) {
			LinkManagementHelper.copyPublicKeyFrom(t);
		}
	}


	public static void copyPublicKeyFrom(final Vector targets) {
		String keys = "";

		if (targets == null)
			return;

		final Toolkit tk = Toolkit.getDefaultToolkit();

		for(final Iterator it = targets.iterator();
		    it.hasNext();) {
			final Link link = (Link)it.next();
			keys = keys + link.getPublicKey() + "\n";
		}

		final StringSelection st = new StringSelection(keys);
		final Clipboard cp = tk.getSystemClipboard();
		cp.setContents(st, null);
	}


}
