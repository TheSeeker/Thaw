/* This code is part of Freenet. It is distributed under the GNU General
 * Public License, version 2 (or at your option any later version). See
 * http://www.gnu.org/ for further details of the GPL. */

package thaw.gui;

import java.io.IOException;
import java.util.HashMap;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import javax.swing.DefaultListModel;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import thaw.core.Config;
import thaw.core.Logger;
import thaw.fcp.FCPConnection;

/**
 * This panel implements Zeroconf (called Bonjour/RendezVous by apple) discovery for Thaw
 *
 * WARNING: for it to work, you must have a running freenet node on the same network subnet, using the MDNSDiscovery panel
 *
 * @author Florent Daigni&egrave;re &lt;nextgens@freenetproject.org&gt;
 *
 * @see http://wiki.freenetproject.org/MDNSDiscoverypanel
 *
 * @see http://www.dns-sd.org/ServiceTypes.html
 * @see http://www.multicastdns.org/
 * @see http://jmdns.sourceforge.net/
 *
 * TODO: implement the "Manual" mode
 * TODO: maybe we should have a small progressbar shown in a new popup to introduce a "delay" at startup
 */
public class MDNSDiscoveryPanel extends JFrame implements ListSelectionListener {
	private static final long serialVersionUID = 1L;

	private static final String FCP_SERVICE_TYPE = "_fcp._tcp.local.";

	private boolean goon = true;
	private ServiceInfo selectedValue;

	private final JScrollPane panel;
	private final JProgressBar progressBar;
	private final JList list;
	private final DefaultListModel listModel;
	private final JLabel label;

	private final Config config;
	private final HashMap foundNodes;
	private final JmDNS jmdns;

	public MDNSDiscoveryPanel(Config conf) {
		this.config = conf;
		this.foundNodes = new HashMap();
		try {
			// Spawn the mdns listener
			this.jmdns = new JmDNS();

			// Start listening for new nodes
			jmdns.addServiceListener(MDNSDiscoveryPanel.FCP_SERVICE_TYPE, new FCPMDNSListener(this));

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Error loading MDNSDiscoveryPanel : " + e.getMessage());
		}

		// The UI
		panel = new JScrollPane();
		progressBar = new JProgressBar(0, 30);
		list = new JList();
		listModel = new DefaultListModel();
		label = new JLabel();

		listModel.addElement("Manual configuration (not recommended) : NotYetImplemented ;)");

		list.setModel(listModel);
		list.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
		list.addListSelectionListener(this);

		label.setText("Select the freenet node you want to connect to from the list below :");

		panel.setLayout(new BorderLayout());
		panel.add(label, BorderLayout.NORTH);
		panel.add(list, BorderLayout.CENTER);
		panel.add(progressBar, BorderLayout.SOUTH);

		setContentPane(panel);

		pack();
		super.setAlwaysOnTop(true);
		super.setLocationRelativeTo(this.getParent());
		this.setTitle("ZeroConf discovery plugin... Please hold on");
		this.setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
		this.setVisible(true);

		Logger.notice(this, "The configuration file doesn't seems to be valid... MDNSDiscovery is starting!");
	}


	private class FCPMDNSListener implements ServiceListener {
		private final MDNSDiscoveryPanel panel;

		public FCPMDNSListener(MDNSDiscoveryPanel panel) {
			this.panel = panel;
		}

		public void serviceAdded(ServiceEvent event) {
			Logger.notice(this, "Service added   : " + event.getName()+"."+event.getType());
			// Force the gathering of informations
			jmdns.getServiceInfo(MDNSDiscoveryPanel.FCP_SERVICE_TYPE, event.getName());
		}

		public synchronized void serviceRemoved(ServiceEvent event) {
			Logger.notice(this, "Service removed : " + event.getName()+"."+event.getType());
			ServiceInfo service = event.getInfo();

			synchronized (panel) {
				panel.foundNodes.remove(service.getName());
				panel.listModel.removeElement(service.getName());
				notify();
			}
		}

		public synchronized void serviceResolved(ServiceEvent event) {
			Logger.debug(this, "Service resolved: " + event.getInfo());
			ServiceInfo service = event.getInfo();

			synchronized (panel) {
				panel.foundNodes.put(service.getName(), service);
				panel.listModel.addElement(service.getName());
				notify();
			}
		}
	}

	/**
	 * The user has selected something: notify the main loop and process the data.
	 */
	public void valueChanged(ListSelectionEvent e) {
		if (e.getValueIsAdjusting() == false) {
			if(list.getSelectedValue() == null) return; // Ignore the user if he clicks nowhere

			synchronized (this) {
				selectedValue = (ServiceInfo) foundNodes.get(list.getSelectedValue());
				goon = false;
				notify();
			}
			Logger.debug(this, "User has selected : "+foundNodes.get(list.getSelectedValue()));
		}
	}

	/**
	 * The main loop : TheRealMeat(TM)
	 *
	 */
	public void run() {
		boolean isConfigValid = false;
		FCPConnection fcp = null;

		do {
			// Loop until a selection is done
			while(goon) {
				try {
					synchronized (this) {
						wait(Long.MAX_VALUE);
					}
				} catch (InterruptedException e) {}

				list.repaint();
			}

			if(selectedValue == null) continue; // TODO: implement the "manual" popup there

			Logger.debug(this, "We got something from the UI : let's try to connect");

			// We try to connect to the server
			fcp = new FCPConnection(selectedValue.getHostAddress(), selectedValue.getPort(), -1, true, true);
			isConfigValid = fcp.connect();

			Logger.debug(this, "isConfigValid ="+isConfigValid);

			// Reload, just in  case it failed...
			goon = true;
			list.removeSelectionInterval(0, foundNodes.size());
		} while(!isConfigValid);

		Logger.debug(this, "We got something that looks valid from the UI : let's propagate changes to  the config");

		// Save the config. now that we know it's valid
		config.setValue("nodeAddress", selectedValue.getHostAddress());
		config.setValue("nodePort", new Integer(selectedValue.getPort()).toString());
		try {
			config.setValue("sameComputer", (jmdns.getInterface().equals(selectedValue.getAddress()) ? "true" : "false"));
		} catch (IOException e ) {} // What can we do except assuming default is fine ?


		Logger.info(this, "We are done : configuration has been saved sucessfully.");

		// Close the fcp socket we have openned, cleanup
		fcp.disconnect();
		jmdns.close();
		this.setVisible(false);
	}

	/**
	 * Convenient testing function function.
	 */
	public static void main(String[] args) {
		new MDNSDiscoveryPanel(new Config("/tmp/conf.ini")).run();
		System.exit(0);
	}
}
