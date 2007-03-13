package thaw.plugins.peerMonitor;

import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JButton;
import javax.swing.ListCellRenderer;
import javax.swing.BorderFactory;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.BorderLayout;
import java.awt.GridLayout;

import java.awt.Color;

import java.util.Vector;
import java.util.Iterator;
import java.util.Enumeration;
import java.util.Hashtable;

import thaw.core.Config;
import thaw.core.I18n;

public class PeerMonitorPanel implements ActionListener
{
	/* must match with color list */
	public final static String[] STR_STATUS = {
		"CONNECTED",
		"BACKED OFF",
		"TOO OLD",
		"TOO NEW",
		"DISCONNECTED",
		"NEVER CONNECTED"
	};


	public final static Color[] COLOR_STATUS = {
		new Color(0, 164, 0),  /* CONNECTED */
		Color.ORANGE,          /* BACKED OFF */
		Color.RED,             /* TOO OLD */
		Color.BLUE,            /* TOO NEW */
		Color.GRAY,            /* DISCONNECTED */
		Color.PINK             /* NEVER CONNECTED */
	};

	private JPanel panel;
	private JPanel mainPanel;

	private JLabel refLabel;
	private JTextArea refArea;
	private JButton refCopyButton;

	private JList peerList;
	private JProgressBar memBar;


	public PeerMonitorPanel(Config config) {

		panel = new JPanel(new BorderLayout(10, 10));

		JPanel peerPanel = new JPanel(new BorderLayout());

		peerList = new JList();

		peerList.setCellRenderer(new PeerCellRenderer());

		Vector v = new Vector();
		v.add(I18n.getMessage("thaw.plugin.peerMonitor.nodeStats"));
		peerList.setListData(v);

		peerPanel.add(new JLabel(I18n.getMessage("thaw.plugin.peerMonitor.peerList")), BorderLayout.NORTH);
		peerPanel.add(new JScrollPane(peerList), BorderLayout.CENTER);


		mainPanel = new JPanel(new GridLayout(2, 1, 10, 10));


		JPanel refPanel = new JPanel(new BorderLayout());
		JPanel refTitle = new JPanel(new BorderLayout());

		refLabel = new JLabel(I18n.getMessage("thaw.plugin.peerMonitor.yourReference"));
		refCopyButton = new JButton(I18n.getMessage("thaw.plugin.peerMonitor.copyReference"));
		refCopyButton.addActionListener(this);

		refTitle.add(refLabel, BorderLayout.CENTER);
		refTitle.add(refCopyButton, BorderLayout.EAST);

		refArea = new JTextArea("*Put the node ref here*");
		refArea.setEditable(false);
		refArea.setBackground(new Color(250,250,250));

		refPanel.add(refTitle, BorderLayout.NORTH);
		refPanel.add(refArea, BorderLayout.CENTER);

		mainPanel.add(refPanel);
		mainPanel.add(new JLabel("*Put details about the peer here*"));

		memBar = new JProgressBar(0, 100);
		setMemBar(0, 134217728);
		memBar.setStringPainted(true);

		panel.add(mainPanel, BorderLayout.CENTER);
		panel.add(peerPanel, BorderLayout.EAST);
		panel.add(memBar, BorderLayout.SOUTH);
	}


	public void setMemBar(long used, long max) {
		int pourcent;

		pourcent = (int)((used * 100) / max);

		memBar.setString("Used memory : "
				 + thaw.gui.GUIHelper.getPrintableSize(used)
				 + " / "
				 + thaw.gui.GUIHelper.getPrintableSize(max)
				 + " ("+Integer.toString(pourcent)+"%)");

		memBar.setValue(pourcent);
	}

	public void setRef(String ref) {
		refArea.setText(ref);
	}



	protected class Peer {
		private String displayName = null;
		private Color textColor = Color.BLACK;
		private Hashtable parameters = null;

		public Peer(Hashtable parameters) {
			this.parameters = parameters;
			displayName = (String)parameters.get("myName");

			String status = (String)parameters.get("volatile.status");

			for (int i = 0 ; i < STR_STATUS.length ; i++) {
				if (STR_STATUS[i].equals(status))
					setTextColor(COLOR_STATUS[i]);
			}
		}

		public void setTextColor(Color c) {
			textColor = c;
		}

		public Color getTextColor() {
			return textColor;
		}

		public String toString() {
			return displayName;
		}
	}


	protected class PeerCellRenderer extends JLabel implements ListCellRenderer {

		public PeerCellRenderer() {
			setOpaque(true);
		}

		public java.awt.Component
			getListCellRendererComponent(JList list,
						     Object value, // value to display
						     int index,    // cell index
						     boolean iss,  // is selected
						     boolean chf)  // cell has focus?
		{
			if (value instanceof String) {
				setText((String)value);
				setForeground(Color.BLACK);
			}

			if (value instanceof Peer) {
				setText(((Peer)value).toString());
				setForeground(((Peer)value).getTextColor());
			}

			// Set a border if the
			//list item is selected
			if (iss)
				setBorder(BorderFactory.createLineBorder(Color.BLUE, 2));
			else
				setBorder(BorderFactory.createLineBorder(list.getBackground(), 2));

			return this;
		}
	}



	/**
	 * \param peers : Vectors containing Hashmap containing the parameter list
	 */
	public synchronized void setPeerList(Hashtable peers)
	{
		Vector results = new Vector();

		/* TODO : dirty : should use comparator, etc */
		for (int i = 0 ; i < STR_STATUS.length ; i++) {
			for (Enumeration e = peers.elements();
			     e.hasMoreElements();) {
				Hashtable ht = (Hashtable)e.nextElement();

				if (STR_STATUS[i].equals(ht.get("volatile.status"))) {
					results.add(new Peer(ht));
				}
			}
		}

		peerList.setListData(results);
	}



	public JPanel getPanel() {
		return panel;
	}


	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == refCopyButton) {
			thaw.gui.GUIHelper.copyToClipboard(refArea.getText());
		}
	}
}
