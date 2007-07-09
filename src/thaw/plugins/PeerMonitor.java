package thaw.plugins;

import java.util.Observable;
import java.util.Observer;
import java.util.Vector;
import java.util.HashMap;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.JButton;

import thaw.core.I18n;
import thaw.core.Core;
import thaw.core.Logger;

import thaw.plugins.peerMonitor.*;
import thaw.fcp.*;


public class PeerMonitor implements thaw.core.Plugin, Observer, ActionListener
{
	public final static int DEFAULT_REFRESH_RATE = 10; /* in sec */

	private PeerMonitorPanel peerPanel;
	private Core core;

	private boolean running = false;

	private boolean isRefSet = false;

	private boolean advancedMode;

	private JButton unfoldButton;
	private boolean folded = false;

	public PeerMonitor() {

	}


	protected class DisplayRefresher implements Observer, Runnable{
		private FCPGetNode getNode = null;
		private FCPListPeers listPeers = null;

		public DisplayRefresher() {

		}

		public void run() {
			while(running) {
				if (getNode == null) {
					getNode = new FCPGetNode(false /* private */, true /* volatile */);
					getNode.addObserver(this);
				}

				getNode.start(core.getQueueManager());

				if (listPeers == null) {
					listPeers = new FCPListPeers(false /* metadata */, true /* volatile */);
					listPeers.addObserver(this);
				}

				if (listPeers.hasEnded())
					listPeers.start(core.getQueueManager());

				try {
					Thread.sleep(DEFAULT_REFRESH_RATE * 1000);
				} catch(InterruptedException e) {
					/* \_o< \_o< \_o< */
				}

				if (!running)
					return;
			}
		}

		public void update(Observable o, Object param) {
			if (!running)
				return;

			if (o instanceof FCPGetNode) {

				FCPGetNode gN = (FCPGetNode)o;

				peerPanel.setMemBar(gN.getUsedJavaMemory(), gN.getMaxJavaMemory());
				peerPanel.setNmbThreads(gN.getNmbThreads());
				peerPanel.setNodeInfos(gN.getAllParameters());

				if (!isRefSet) {
					peerPanel.setRef(gN.getRef());
					isRefSet = true;
				}
			}

			if (o instanceof FCPListPeers) {

				FCPListPeers lP = (FCPListPeers)o;

				peerPanel.setPeerList(lP.getPeers());
			}
		}
	}


	public boolean run(Core core) {
		this.core = core;

		advancedMode = Boolean.valueOf(core.getConfig().getValue("advancedMode")).booleanValue();

		unfoldButton = new JButton("<");
		unfoldButton.addActionListener(this);

		peerPanel = new PeerMonitorPanel(this, core.getQueueManager(), core.getConfig(), core.getMainWindow());

		peerPanel.addObserver(this);
		peerPanel.getFoldButton().addActionListener(this);

		core.getMainWindow().addComponent(peerPanel.getPeerListPanel(),
						  BorderLayout.EAST);

		running = true;
		isRefSet = false;
		Thread th = new Thread(new DisplayRefresher());
		th.start();

		if (core.getConfig().getValue("peerMonitorFolded") != null) {
			boolean f = (new Boolean(core.getConfig().getValue("peerMonitorFolded"))).booleanValue();
			if (f) foldPanel();
		}

		return true;
	}


	public boolean stop() {
		hideTab();
		if (!folded)
			core.getMainWindow().removeComponent(peerPanel.getPeerListPanel());
		else
			core.getMainWindow().removeComponent(unfoldButton);

		core.getConfig().setValue("peerMonitorFolded", Boolean.toString(folded));

		running = false;
		return false;
	}




	public String getNameForUser() {
		return I18n.getMessage("thaw.plugin.peerMonitor.peerMonitor");
	}

	public javax.swing.ImageIcon getIcon() {
		return thaw.gui.IconBox.peers;
	}


	private boolean tabVisible = false;

	public void update(Observable o, Object param) {
		core.getMainWindow().addTab(I18n.getMessage("thaw.plugin.peerMonitor.peerMonitor"),
					    thaw.gui.IconBox.peers,
					    peerPanel.getTabPanel());
		core.getMainWindow().setSelectedTab(peerPanel.getTabPanel());

		tabVisible = true;

		peerPanel.showToolbarButtons();
	}

	public void hideTab() {
		if (tabVisible) {
			peerPanel.hideToolbarButtons();

			core.getMainWindow().removeTab(peerPanel.getTabPanel());
			tabVisible = false;
		}
	}


	public void foldPanel() {
		Logger.info(this, "Folding peer monitor panel");
		core.getMainWindow().removeComponent(peerPanel.getPeerListPanel());
		core.getMainWindow().getMainFrame().validate();
		core.getMainWindow().addComponent(unfoldButton,
						  BorderLayout.EAST);
		core.getMainWindow().getMainFrame().validate();
		folded = true;
	}

	public void unfoldPanel() {
		Logger.info(this, "Unfolding peer monitor panel");
		core.getMainWindow().removeComponent(unfoldButton);
		core.getMainWindow().getMainFrame().validate();
		core.getMainWindow().addComponent(peerPanel.getPeerListPanel(),
						  BorderLayout.EAST);
		core.getMainWindow().getMainFrame().validate();
		folded = false;
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == peerPanel.getFoldButton())
			foldPanel();
		else if (e.getSource() == unfoldButton)
			unfoldPanel();
	}
}
