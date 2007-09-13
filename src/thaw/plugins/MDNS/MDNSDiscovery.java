/* This code is part of Freenet. It is distributed under the GNU General
 * Public License, version 2 (or at your option any later version). See
 * http://www.gnu.org/ for further details of the GPL. */

package thaw.core;


import java.util.LinkedList;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;


public class MDNSDiscovery {
	// SYNC IT!!!
	private final LinkedList foundNodes;
	private final JmDNS jmdns;
	private Core core;

	public MDNSDiscovery(Core core) {
		this.core = core;
		this.foundNodes = new LinkedList();

		try {
			// Spawn the mdns listener
			Logger.info(this, "Starting JMDNS ...");
			this.jmdns = new JmDNS();

			// Start listening for new nodes
			jmdns.addServiceListener(MDNSDiscoveryPanel.FCP_SERVICE_TYPE, new FCPMDNSListener());

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Error loading MDNSDiscoveryPanel : " + e.getMessage());
		}
	}


	private class FCPMDNSListener implements ServiceListener {
		public void serviceAdded(ServiceEvent event) {
			Logger.notice(this, "Service added   : " + event.getName()+"."+event.getType());
			// Force the gathering of informations
			jmdns.getServiceInfo(MDNSDiscoveryPanel.FCP_SERVICE_TYPE, event.getName());
		}

		public void serviceRemoved(ServiceEvent event) {
			Logger.notice(this, "Service removed : " + event.getName()+"."+event.getType());
			ServiceInfo service = event.getInfo();

			synchronized (foundNodes) {
				foundNodes.remove(service);
				synchronized (core.getConfigWindow().getNodeConfigPanel().getMdnsPanel()) {
					core.getConfigWindow().getNodeConfigPanel().getMdnsPanel().notifyAll();
				}
			}
		}

		public void serviceResolved(ServiceEvent event) {
			Logger.debug(this, "Service resolved: " + event.getInfo());
			ServiceInfo service = event.getInfo();

			synchronized (foundNodes) {
				foundNodes.add(service);
				synchronized (core.getConfigWindow().getNodeConfigPanel().getMdnsPanel()) {
					core.getConfigWindow().getNodeConfigPanel().getMdnsPanel().notifyAll();
				}
			}
		}
	}


	public boolean isHasTheSameIPAddress(ServiceInfo host) {
		try{
			return (jmdns.getInterface().equals(host.getAddress()) ? true : false);
		} catch (java.io.IOException e) {
			return false;
		}
	}

	public LinkedList getFoundNodes() {
		return foundNodes;
	}

	public void stop() {
		Logger.info(this, "Stopping JMDNS ...");
		jmdns.close();
	}
}
