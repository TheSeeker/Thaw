package thaw.plugins.transferLogs;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;


import javax.swing.AbstractButton;

import java.util.Vector;
import java.util.Iterator;


public class TransferManagementHelper {

	public interface TransferAction extends ActionListener {

		/**
		 * Can disable the abstract button if required
		 * @param files can be null
		 */
		public void setTarget(Vector transfers);
	}


	/*
	 * As a key listener, it reacts with the key 'suppr'
	 */
	public static class TransferRemover implements TransferAction, KeyListener {
		private AbstractButton b;
		private Vector targets;
		private TransferTable tt;

		public TransferRemover(AbstractButton b, TransferTable t) {
			this.b = b;
			this.tt = t;

			if (b != null) {
				b.addActionListener(this);
			}
		}


		public void setTarget(Vector transfers) {
			targets = transfers;
		}

		public void keyPressed(final KeyEvent e) { }

		public void keyReleased(final KeyEvent e) {
			if (e.getKeyCode() == KeyEvent.VK_DELETE) {
				actionPerformed(null);
			}
		}

		public void keyTyped(final KeyEvent e) { }

		public void actionPerformed(ActionEvent e) {
			if (targets == null)
				return;

			if (e == null || b == null || e.getSource() == b) {

				for (Iterator it = targets.iterator();
				     it.hasNext(); ) {
					Transfer t = (Transfer)it.next();
					t.delete();
				}

			}

			if (tt != null)
				tt.refresh();
		}
	}


	public static class TransferKeyCopier implements TransferAction {
		private AbstractButton b;
		private Vector targets;


		public TransferKeyCopier(AbstractButton b) {
			this.b = b;

			if (b != null) {
				b.addActionListener(this);
			}
		}


		public void setTarget(Vector transfers) {
			targets = transfers;
		}


		public void actionPerformed(ActionEvent e) {
			if (targets == null)
				return;

			if (b == null || e.getSource() == b) {
				String keys = "";

				for (Iterator it = targets.iterator();
				     it.hasNext(); ) {
					Transfer t = (Transfer)it.next();

					if (t.getKey() != null)
						keys += t.getKey() + "\n";
				}

				thaw.gui.GUIHelper.copyToClipboard(keys);
			}
		}
	}
}
