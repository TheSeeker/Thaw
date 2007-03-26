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

import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.BorderLayout;
import java.awt.GridLayout;

import java.awt.Color;

import java.util.Vector;
import java.util.Iterator;
import java.util.Enumeration;
import java.util.Hashtable;

import java.util.Observable;
import java.util.Observer;


import thaw.core.Config;
import thaw.core.I18n;
import thaw.core.Logger;

import thaw.gui.IconBox;

/**
 * In fact, here is two panels : A panel with the peer list
 * and a panel with various details (ref, etc)
 */
public class PeerMonitorPanel extends Observable implements ActionListener, ListSelectionListener
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

	public final static Color JLIST_NODE_STAT_BACKGROUND = Color.WHITE;
	public final static Color JLIST_PEER_BACKGROUND = new Color(240,240,240);

	public final static int STR_INFO_MAX_LNG = 55;
	public final static int STR_NODENAME_MAX_LNG = 15;

	private JPanel refPanel;

	private JPanel peerPanel;

	private JPanel tabPanel;
	private JPanel mainPanel;

	private JLabel refLabel;
	private JTextArea refArea;
	private JButton refCopyButton;

	private JList peerList;
	private JProgressBar thawMemBar;
	private JProgressBar nodeMemBar;

	private JLabel detailsLabel;
	private JPanel detailsPanel;

	private boolean advanced;


	public PeerMonitorPanel(Config config) {

		advanced = Boolean.valueOf(config.getValue("advancedMode")).booleanValue();

		tabPanel = new JPanel(new BorderLayout(10, 10));

		peerPanel = new JPanel(new BorderLayout());

		peerList = new JList();

		peerList.setCellRenderer(new PeerCellRenderer());
		peerList.addListSelectionListener(this);

		Vector v = new Vector();

		if (advanced)
			v.add(I18n.getMessage("thaw.plugin.peerMonitor.nodeStats"));

		peerList.setListData(v);


		JLabel peerListLabel = new JLabel(I18n.getMessage("thaw.plugin.peerMonitor.peerList"));
		peerListLabel.setIcon(IconBox.peers);


		nodeMemBar = new JProgressBar(0, 100);
		thawMemBar = new JProgressBar(0, 100);
		nodeMemBar.setStringPainted(true);
		thawMemBar.setStringPainted(true);

		setMemBar(0, 134217728);


		peerPanel.add(peerListLabel, BorderLayout.NORTH);
		peerPanel.add(new JScrollPane(peerList), BorderLayout.CENTER);

		JPanel memPanel = new JPanel(new GridLayout(2, 1));
		memPanel.add(nodeMemBar);
		memPanel.add(thawMemBar);

		peerPanel.add(memPanel, BorderLayout.SOUTH);


		mainPanel = new JPanel(new GridLayout(2, 1, 10, 10));


		JPanel refPanel = new JPanel(new BorderLayout());
		JPanel refTitle = new JPanel(new BorderLayout());

		refLabel = new JLabel(I18n.getMessage("thaw.plugin.peerMonitor.yourReference"));
		refLabel.setIcon(IconBox.identity);

		refCopyButton = new JButton(I18n.getMessage("thaw.plugin.peerMonitor.copyReference"));
		refCopyButton.addActionListener(this);

		refTitle.add(refLabel, BorderLayout.CENTER);
		refTitle.add(refCopyButton, BorderLayout.EAST);

		refArea = new JTextArea("*Put the node ref here*");
		refArea.setEditable(false);
		refArea.setBackground(new Color(240,240,240));

		refPanel.add(refTitle, BorderLayout.NORTH);
		refPanel.add(new JScrollPane(refArea), BorderLayout.CENTER);

		mainPanel.add(refPanel);

		detailsLabel = new JLabel();
		detailsPanel = new JPanel();

		JPanel globalDetailsPanel = new JPanel(new BorderLayout(5, 5));
		globalDetailsPanel.add(detailsLabel, BorderLayout.NORTH);
		globalDetailsPanel.add(new JScrollPane(detailsPanel), BorderLayout.CENTER);

		mainPanel.add(globalDetailsPanel);

		tabPanel.add(mainPanel, BorderLayout.CENTER);
	}


	public void setMemBar(long used, long max) {
		int pourcent;

		/* node mem bar */

		pourcent = (int)((used * 100) / max);

		nodeMemBar.setString(I18n.getMessage("thaw.plugin.peerMonitor.infos.nodeMemory")+ ": "
				     + thaw.gui.GUIHelper.getPrintableSize(used)
				     + " / "
				     + thaw.gui.GUIHelper.getPrintableSize(max)
				     + " ("+Integer.toString(pourcent)+"%)");

		nodeMemBar.setValue(pourcent);


		/* thaw mem bar */

		max = Runtime.getRuntime().maxMemory();

		if (max == Long.MAX_VALUE) {
			max = Runtime.getRuntime().totalMemory();
		}

		used = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

		pourcent = (int)((used * 100) / max);

		thawMemBar.setString(I18n.getMessage("thaw.plugin.peerMonitor.infos.thawMemory")+ ": "
				     + thaw.gui.GUIHelper.getPrintableSize(used)
				     + " / "
				     + thaw.gui.GUIHelper.getPrintableSize(max)
				     + " ("+Integer.toString(pourcent)+"%)");

		thawMemBar.setValue(pourcent);
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

		public Hashtable getParameters() {
			return parameters;
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
				setBackground(JLIST_NODE_STAT_BACKGROUND);
			}

			if (value instanceof Peer) {
				String txt = ((Peer)value).toString();

				if (txt.length() > 35) {
					txt = txt.substring(0, 35) + "(...)";
				}

				setText(txt);
				setForeground(((Peer)value).getTextColor());
				setBackground(JLIST_PEER_BACKGROUND);
			}

			// Set a border if the
			//list item is selected
			if (iss)
				setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
			else
				setBorder(BorderFactory.createLineBorder(list.getBackground(), 1));

			return this;
		}
	}



	private Vector peers;
	private Hashtable nodeInfos = null;

	/**
	 * \param peers : Vectors containing Hashmap containing the parameter list
	 */
	public synchronized void setPeerList(Hashtable pL)
	{
		peers = new Vector();

		if (advanced)
			peers.add(I18n.getMessage("thaw.plugin.peerMonitor.nodeStats"));

		/* TODO : dirty : should use comparator, etc */
		for (int i = 0 ; i < STR_STATUS.length ; i++) {
			for (Enumeration e = pL.elements();
			     e.hasMoreElements();) {
				Hashtable ht = (Hashtable)e.nextElement();

				if (STR_STATUS[i].equals(ht.get("volatile.status"))) {
					peers.add(new Peer(ht));
				}
			}
		}

		peerList.setListData(peers);
	}

	public synchronized void setNodeInfos(Hashtable infos) {
		if (nodeInfos == null) { /* first time */
			displayInfos(I18n.getMessage("thaw.plugin.peerMonitor.nodeInfos"), infos);
		}

		nodeInfos = infos;
	}


	public JPanel getTabPanel() {
		return tabPanel;
	}

	public JPanel getPeerListPanel() {
		return peerPanel;
	}


	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == refCopyButton) {
			thaw.gui.GUIHelper.copyToClipboard(refArea.getText());
		}
	}



	/**
	 * @return null if it must not be displayed ; else an array with two elements (key translated + value translated)
	 */
	public String[] getTranslation(String key, String value) {
		String [] result = null;

		/* PEERS */

		if (result == null && "volatile.lastRoutingBackoffReason".equals(key))
			result = new String[] {
				I18n.getMessage("thaw.plugin.peerMonitor.infos.peer.lastRoutingBackoffReason"),
				value
			};

		if (result == null && "volatile.routingBackoffPercent".equals(key))
			result = new String[] {
				I18n.getMessage("thaw.plugin.peerMonitor.infos.peer.routingBackoffPercent"),
				value + "%"
			};

		if (result == null && "version".equals(key))
			result = new String[] {
				I18n.getMessage("thaw.plugin.peerMonitor.infos.peer.version"),
				value
			};

		if (result == null && "volatile.status".equals(key))
			result = new String[] {
				I18n.getMessage("thaw.plugin.peerMonitor.infos.peer.status"),
				value
			};

		if (result == null && "myName".equals(key))
			result = new String[] {
				I18n.getMessage("thaw.plugin.peerMonitor.infos.peer.myName"),
				value
			};


		if (result == null && "physical.udp".equals(key))
			result = new String[] {
				I18n.getMessage("thaw.plugin.peerMonitor.infos.peer.physical.udp"),
				value
			};

		if (result == null && "volatile.averagePingTime".equals(key))
			result = new String[] {
				I18n.getMessage("thaw.plugin.peerMonitor.infos.peer.averagePingTime"),
				Integer.toString(new Float(value).intValue()) + " ms"
			};

		if (result == null && "volatile.idle".equals(key))
			result = new String[] {
				I18n.getMessage("thaw.plugin.peerMonitor.infos.peer.idle"),
				"~" + thaw.gui.GUIHelper.getPrintableTime(Long.parseLong(value) / 1000)
			};

		/* NODE */

		if (result == null && "volatile.overallSize".equals(key))
			result = new String[] {
				I18n.getMessage("thaw.plugin.peerMonitor.infos.node.overallSize"),
				"~" + thaw.gui.GUIHelper.getPrintableSize(Long.parseLong(value))
			};

		if (result == null && "volatile.uptimeSeconds".equals(key)) {
			result = new String[] {
				I18n.getMessage("thaw.plugin.peerMonitor.infos.node.uptimeSeconds"),
				"~" + thaw.gui.GUIHelper.getPrintableTime(Long.parseLong(value))
			};
		}

		if (result == null && "volatile.networkSizeEstimateSession".equals(key)) {
			result = new String[] {
				I18n.getMessage("thaw.plugin.peerMonitor.infos.node.networkSizeEstimateSession"),
				value
			};
		}

		if (result == null && "myName".equals(key)) {
			result = new String[] {
				I18n.getMessage("thaw.plugin.peerMonitor.infos.node.myName"),
				value
			};
		}

		if (advanced) {
			if (result == null)
				result = new String[] { key, value };
			else
				result[0] = result[0] + " ("+key+")";
		}

		return result;
	}


	private static JLabel makeInfoLabel(String txt) {
		if (txt.length() > STR_INFO_MAX_LNG)
			txt = txt.substring(0, STR_INFO_MAX_LNG) + "(...)";

		JLabel lb = new JLabel(txt);
		return lb;
	}


	public void displayInfos(String title, Hashtable ht) {
		if (ht == null)
			return;

		Vector v = new Vector();

		for (Enumeration e = ht.keys();
		     e.hasMoreElements(); ) {
			String key = (String)e.nextElement();

			String[] val = getTranslation(key, (String)ht.get(key));

			if (val != null)
				v.add(val);
		}

		detailsPanel.removeAll();

		detailsPanel.setLayout(new GridLayout(v.size()+1, 2));

		detailsLabel.setText(title);
		detailsLabel.setIcon(IconBox.computer);

		for (Iterator i = v.iterator();
		     i.hasNext();) {
			String[] val = (String[])i.next();

			detailsPanel.add(makeInfoLabel(val[0] + ":"));
			detailsPanel.add(makeInfoLabel(val[1]));
		}

		mainPanel.validate();
		detailsPanel.validate();
	}


	public void valueChanged(ListSelectionEvent e) {
		if (e.getFirstIndex() == 0) {
			displayInfos(I18n.getMessage("thaw.plugin.peerMonitor.nodeInfos"), nodeInfos);
		} else {
			String peerName = peers.get(e.getFirstIndex()).toString();

			displayInfos(I18n.getMessage("thaw.plugin.peerMonitor.peerInfos") + " '" + peerName + "':",
				     ((Peer)peers.get(e.getFirstIndex())).getParameters());
		}

		setChanged();
		notifyObservers();
	}
}
