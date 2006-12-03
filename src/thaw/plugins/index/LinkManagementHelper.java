package thaw.plugins.index;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.AbstractButton;

import java.util.Vector;
import java.util.Iterator;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

import thaw.core.Logger;
import thaw.fcp.*;

import thaw.plugins.Hsqldb;

public class LinkManagementHelper {
	public interface LinkAction extends ActionListener {

		/**
		 * Can disable the abstract button if required
		 * @param node can be null
		 */
		public void setTarget(Vector links);
	}


	public static class LinkRemover implements LinkAction {
		private AbstractButton actionSource;
		private Vector target;

		/**
		 * @param queueManager is used to stop transfers if needed
		 */
		public LinkRemover(AbstractButton actionSource) {
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
					Link link = (Link)it.next();

					if (!link.isModifiable()) {
						isOk = false;
						break;
					}
				}
			}


			actionSource.setEnabled(target != null && target.size() != 0 && isOk);
		}

		public void actionPerformed(ActionEvent e) {
			removeLinks(target);
		}
	}


	public static void removeLinks(Vector links) {
		if (links == null)
			return;

		for (Iterator it = links.iterator();
		     it.hasNext() ; ) {
			Link link = (Link)it.next();
			link.getParent().removeLink(link);
		}
	}


	public static class IndexAdder implements LinkAction {
		private Hsqldb db;
		private FCPQueueManager queueManager;
		private IndexTree tree;
		private UnknownIndexList uil;

		private AbstractButton src;
		private Vector t;

		public IndexAdder(AbstractButton actionSource,
				  Hsqldb db, FCPQueueManager queueManager,
				  UnknownIndexList uil,
				  IndexTree tree) {
			src = actionSource;
			if (actionSource != null)
				actionSource.addActionListener(this);
			this.db = db;
			this.queueManager = queueManager;
			this.tree = tree;
			this.uil = uil;
		}

		public void setTarget(Vector targets) {
			t = targets;
			src.setEnabled(targets != null && targets.size() > 0);
		}

		public void actionPerformed(ActionEvent e) {
			for (Iterator it = t.iterator();
			     it.hasNext(); ) {
				Link link = (Link)it.next();
				IndexManagementHelper.addIndex(db, queueManager, uil, tree, null,
							       link.getPublicKey());
			}
		}
	}


	public static class PublicKeyCopier implements LinkAction {
		private AbstractButton src;
		private Vector t;

		public PublicKeyCopier(AbstractButton actionSource) {
			src = actionSource;
			if (actionSource != null)
				actionSource.addActionListener(this);
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
			Link link = (Link)it.next();
			keys = keys + link.getPublicKey() + "\n";
		}

		StringSelection st = new StringSelection(keys);
		Clipboard cp = tk.getSystemClipboard();
		cp.setContents(st, null);
	}


}
