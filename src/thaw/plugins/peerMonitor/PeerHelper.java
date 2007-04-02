package thaw.plugins.peerMonitor;

import javax.swing.JDialog;
import javax.swing.JPanel;

import javax.swing.JLabel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.JTextArea;
import javax.swing.JButton;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.AbstractButton;


import thaw.fcp.*; /* I'm lazy */
import thaw.core.MainWindow;
import thaw.core.I18n;

public class PeerHelper {

	public interface PeerAction extends ActionListener {
		public void setTarget(Peer peer);
	}


	public static class PeerAdder implements PeerAction {
		private FCPQueueManager queueManager;
		private AbstractButton src;
		private MainWindow mainWindow;

		private JDialog dialog;
		private JButton okButton;
		private JButton cancelButton;
		private JTextArea refArea;

		public PeerAdder(FCPQueueManager queueManager, MainWindow mainWindow, AbstractButton actionSource) {
			this.queueManager = queueManager;
			this.src = actionSource;
			this.mainWindow = mainWindow;

			if (actionSource != null)
				actionSource.addActionListener(this);
		}

		public void setTarget(Peer peer) {
			/* we ignore */
		}

		public void actionPerformed(ActionEvent e) {
			if (e.getSource() == src) {
				dialog = new JDialog(mainWindow.getMainFrame(),
						     I18n.getMessage("thaw.plugin.peerMonitor.addPeer"));

				dialog.setLayout(new BorderLayout());

				JPanel buttonPanel = new JPanel(new GridLayout(1, 2));

				okButton = new JButton(I18n.getMessage("thaw.common.ok"));
				okButton.addActionListener(this);
				buttonPanel.add(okButton);

				cancelButton = new JButton(I18n.getMessage("thaw.common.cancel"));
				cancelButton.addActionListener(this);
				buttonPanel.add(cancelButton);

				refArea = new JTextArea("");

				JLabel label = new JLabel(I18n.getMessage("thaw.plugin.peerMonitor.enterRef"));

				dialog.getContentPane().add(label, BorderLayout.NORTH);
				dialog.getContentPane().add(refArea, BorderLayout.CENTER);
				dialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);

				dialog.setSize(700, 300);
				dialog.setVisible(true);

				return;
			}


			if (e.getSource() == okButton) {
				/* TODO */
				dialog.setVisible(false);
			}

			if (e.getSource() == cancelButton) {
				dialog.setVisible(false);
			}
		}
	}


	public static class PeerRemover implements PeerAction {
		private FCPQueueManager queueManager;
		private AbstractButton src;

		public PeerRemover(FCPQueueManager queueManager, AbstractButton actionSource) {
			this.queueManager = queueManager;
			this.src = actionSource;

			if (actionSource != null)
				actionSource.addActionListener(this);
		}

		public void setTarget(Peer peer) {
			if (src != null) {
				src.setEnabled(peer != null);
			}
		}

		public void actionPerformed(ActionEvent e) {
			/* TODO */
		}
	}

}

