package thaw.plugins;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import thaw.plugins.mDns.MDNSDiscovery;
import thaw.plugins.mDns.MDNSDiscoveryPanel;
import thaw.plugins.mDns.MDNSDiscoveryPanel.MDNSDiscoveryPanelCallback;

import thaw.core.I18n;
import thaw.core.Core;
import thaw.core.ThawThread;

public class MDns implements thaw.core.Plugin, ActionListener, MDNSDiscoveryPanelCallback {
	private Core core;

	private MDNSDiscovery discovery;
	private MDNSDiscoveryPanel mdnsPanel;
	private boolean isMDNSPanerShown = false;


	public MDns() {

	}

	public boolean run(Core core) {
		this.core = core;

		if (discovery == null) { /* first call */
			discovery = new MDNSDiscovery(this);

			isMDNSPanerShown = false;
			mdnsPanel = new MDNSDiscoveryPanel(core.getConfigWindow().getFrame(), this, this);
		} else { /* not first call */
			discovery.start();
		}

		core.getConfigWindow().getNodeConfigPanel().getAutodetectButton().addActionListener(this);
		core.getConfigWindow().getNodeConfigPanel().getAutodetectButton().setEnabled(true);

		return false;
	}

	public MDNSDiscovery getMDNSDiscovery() {
		return discovery;
	}

	public MDNSDiscoveryPanel getMdnsPanel() {
		return mdnsPanel;
	}

	public thaw.core.Config getConfig() {
		return core.getConfig();
	}

	public boolean stop() {
		discovery.stop();

		core.getConfigWindow().getNodeConfigPanel().getAutodetectButton().removeActionListener(this);
		core.getConfigWindow().getNodeConfigPanel().getAutodetectButton().setEnabled(false);

		return false;
	}



	public void actionPerformed(ActionEvent e) {
		if (e.getSource() ==
		    core.getConfigWindow().getNodeConfigPanel().getAutodetectButton()) {

			synchronized (this) {
				if(isMDNSPanerShown) return;
				isMDNSPanerShown = true;
			}
			core.getConfigWindow().getNodeConfigPanel().getAutodetectButton().setEnabled(false);
			new ThawThread(mdnsPanel, "MDns host list refresher", this).start();

		}
	}


	public void onMDNSDiscoverPanelClosure(boolean hasBeenCancelled) {
		// We got back !
		synchronized (this) {
			isMDNSPanerShown = false;
		}
		core.getConfigWindow().getNodeConfigPanel().getAutodetectButton().setEnabled(true);
	}


	public String getNameForUser() {
		return I18n.getMessage("thaw.plugin.MDNS");
	}

	public javax.swing.ImageIcon getIcon() {
		return thaw.gui.IconBox.mDns;
	}
}
