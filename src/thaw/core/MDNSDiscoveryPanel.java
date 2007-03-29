/* This code is part of Freenet. It is distributed under the GNU General
 * Public License, version 2 (or at your option any later version). See
 * http://www.gnu.org/ for further details of the GPL. */

package thaw.core;

import javax.jmdns.ServiceInfo;
import javax.swing.DefaultListModel;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.Socket;

import javax.swing.JButton;

import thaw.core.Logger;

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
public class MDNSDiscoveryPanel extends JDialog implements ActionListener, Runnable {
	public interface MDNSDiscoveryPanelCallback {
		public void onMDNSDiscoverPanelClosure(boolean hasBeenCancelled);
	}
	private static final long serialVersionUID = 1L;

	public static final String FCP_SERVICE_TYPE = "_fcp._tcp.local.";

	private boolean goon;
	private boolean cancelledByUser = false;
	private ServiceInfo selectedValue;
	private final MDNSDiscoveryPanelCallback cb;

	private final JList list;
	private final DefaultListModel listModel;
	private final JLabel label;

	private final Core core;

	private JButton okButton;
	private JButton cancelButton;


	public MDNSDiscoveryPanel(java.awt.Dialog owner, Core core, MDNSDiscoveryPanelCallback cb) {
		super(owner, "ZeroConf");
		this.core = core;
		this.cb = cb;

		// The UI
		list = new JList();
		listModel = new DefaultListModel();
		label = new JLabel();

		list.setModel(listModel);
		list.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

		label.setText(I18n.getMessage("thaw.zeroconf.searchingNode")
			      +" ; "+
			      I18n.getMessage("thaw.zeroconf.nodeList"));

		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(label, BorderLayout.NORTH);
		getContentPane().add(new JScrollPane(list), BorderLayout.CENTER);

		okButton = new JButton(I18n.getMessage("thaw.common.ok"));
		cancelButton = new JButton(I18n.getMessage("thaw.common.cancel"));

		okButton.addActionListener(this);
		cancelButton.addActionListener(this);

		JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);

		getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		pack();
	}


	

	/**
	 * The user has selected something: notify the main loop and process the data.
	 */
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == cancelButton)
			cancelledByUser = true;
		
		goon = false;
		
		synchronized (this) {
			selectedValue = core.getServiceInfoFromDiscoveredNodeList(list.getSelectedValue());
			notify();
		}
		Logger.info(this, "User has selected : " + selectedValue);
	}

	/**
	 * The main loop : TheRealMeat(TM)
	 *
	 */
	public void run() {
		super.setLocationRelativeTo(this.getParent());
		this.setVisible(true);
		
		Logger.notice(this, "Show the MDNSDiscoveryPanel");
		Socket testSocket = null;
		boolean isConfigValid = false;
		
		goon = true;
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
			
			if(cancelledByUser) break;
			else if(selectedValue == null) continue;

			Logger.debug(this, "We got something from the UI : let's try to connect");

			try {
				// We try to connect to the server
				// TODO: implement a proper test!
				testSocket = new Socket(selectedValue.getHostAddress(), selectedValue.getPort());
				isConfigValid = testSocket.isConnected();

				// Close the fcp socket we have openned, cleanup
				testSocket.close();
			} catch (IOException e) {
				isConfigValid = false;
			}
			
			Logger.debug(this, "isConfigValid ="+isConfigValid);

			// Reload, just in  case it failed...
			goon = true;
			list.removeSelectionInterval(0, core.getDiscoveredNodeListSize());
		} while(!isConfigValid);


		this.setVisible(false);
		
		if (!cancelledByUser) {
			Logger.debug(this, "We got something that looks valid from the UI : let's propagate changes to  the config");
			// Save the config. now that we know it's valid
			core.config.setValue("nodeAddress", selectedValue.getHostAddress());
			core.config.setValue("nodePort", new Integer(selectedValue.getPort()).toString());
			core.config.setValue("sameComputer", String.valueOf(core.isHasTheSameIPAddress(selectedValue)));


			Logger.info(this, "We are done : configuration has been saved sucessfully.");
			
		}else
			Logger.info(this, "The user has cancelled!");
		cb.onMDNSDiscoverPanelClosure(cancelledByUser);
		Logger.notice(this, "We got back from the MDNSDiscoveryPanel callback");
	}
}
