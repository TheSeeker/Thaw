package thaw.plugins.peerMonitor;


import java.awt.Color;

import java.util.Hashtable;


public class Peer {

	private String displayName = null;
	private Color textColor = Color.BLACK;
	private Hashtable parameters = null;

	public Peer(Hashtable parameters) {
		this.parameters = parameters;
		displayName = (String)parameters.get("myName");

		String status = (String)parameters.get("volatile.status");

		for (int i = 0 ; i < PeerMonitorPanel.STR_STATUS.length ; i++) {
			if (PeerMonitorPanel.STR_STATUS[i].equals(status))
				setTextColor(PeerMonitorPanel.COLOR_STATUS[i]);
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
