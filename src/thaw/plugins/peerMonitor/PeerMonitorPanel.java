package thaw.plugins.peerMonitor;

import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.JList;
import javax.swing.JLabel;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import thaw.core.Config;


public class PeerMonitorPanel
{
	private JPanel panel;

	private JPanel mainPanel;

	private JList peerList;


	public PeerMonitorPanel(Config config) {
		panel = new JPanel(new BorderLayout());

		peerList = new JList();

		mainPanel = new JPanel(new GridLayout(2, 1));

		mainPanel.add(new JLabel("*Put the node ref here*"));
		mainPanel.add(new JLabel("*Put details about the peer here*"));

		panel.add(mainPanel, BorderLayout.CENTER);
		panel.add(peerList, BorderLayout.EAST);
	}


	public JPanel getPanel() {
		return panel;
	}

}
