package thaw.plugins;

import java.util.Iterator;
import java.util.Vector;

import java.awt.Color;


import thaw.core.Core;
import thaw.core.I18n;
import thaw.core.Logger;
import thaw.core.Main;
import thaw.core.Plugin;
import thaw.core.LogListener;

import thaw.gui.IconBox;
import thaw.fcp.FCPTransferQuery;

public class StatusBar implements Runnable, Plugin, LogListener {
	public final static int INTERVAL = 3000; /* in ms */
	public final static String SEPARATOR = "     ";

	private Core core;
	private boolean running = true;
	private Thread refresher;

	private boolean advancedMode = false;

	private boolean dropNextRefresh = false;

	private Object barLock = new Object();

	public final static Color ORANGE = new Color(240, 160, 0);

	public boolean run(final Core core) {
		this.core = core;

		advancedMode = Boolean.valueOf(core.getConfig().getValue("advancedMode")).booleanValue();

		running = true;
		refresher = new Thread(this);

		refresher.start();

		Logger.addLogListener(this);

		return true;
	}

	public void run() {
		while(running) {

			try {
				Thread.sleep(StatusBar.INTERVAL);
			} catch(final java.lang.InterruptedException e) {
				// pfff :P
			}

			if (!dropNextRefresh)
				updateStatusBar();
			else
				dropNextRefresh = false;

		}

	}


	public void newLogLine(int level, Object src, String line) {
		if (level <= 1) { /* error / warnings */
			dropNextRefresh = true;

			String str = Logger.PREFIXES[level]+" "
				+ ((src != null) ? (src.getClass().getName() + ": ") : "")
				+ line;

			core.getMainWindow().setStatus(IconBox.minStop, str,
						       ((level == 0) ? Color.RED : ORANGE));
		}
	}


	public void updateStatusBar() {

		if (core.isReconnecting()) {
			core.getMainWindow().setStatus(IconBox.blueBunny,
						       I18n.getMessage("thaw.statusBar.connecting"), java.awt.Color.RED);
			return;
		}

		if (!core.getConnectionManager().isConnected()) {
			core.getMainWindow().setStatus(IconBox.minDisconnectAction,
						       I18n.getMessage("thaw.statusBar.disconnected"), java.awt.Color.RED);
			return;
		}

		int progressDone = 0;
		int progressTotal = 0;

		int finished = 0;
		int failed = 0;
		int running = 0;
		int pending = 0;
		int total = 0;

		final Vector runningQueue = core.getQueueManager().getRunningQueue();

		synchronized(runningQueue) {
			for(final Iterator it = runningQueue.iterator();
			    it.hasNext(); ) {
				final FCPTransferQuery query = (FCPTransferQuery)it.next();

				if(query.isRunning() && !query.isFinished()) {
					running++;
					progressTotal += 100;
					progressDone += query.getProgression();
				}

				if(query.isFinished() && query.isSuccessful()) {
					finished++;
					progressTotal += 100;
					progressDone += 100;
				}

				if(query.isFinished() && !query.isSuccessful()) {
					failed++;
				}
			}
		}

		final Vector[] pendingQueues = core.getQueueManager().getPendingQueues();

		synchronized(pendingQueues) {
			for(int i =0 ; i < pendingQueues.length; i++) {

				progressTotal += pendingQueues[i].size() * 100;
				pending += pendingQueues[i].size();

			}
		}


		total = finished + failed + running + pending;

		String status = "Thaw "+Main.VERSION;

		if(advancedMode) {
			status = status
				+ StatusBar.SEPARATOR + I18n.getMessage("thaw.plugin.statistics.globalProgression") + " "
				+ Integer.toString(progressDone) + "/" + Integer.toString(progressTotal);
		}


		int nmbThread = Thread.activeCount();


		status = status
			+ StatusBar.SEPARATOR + I18n.getMessage("thaw.plugin.statistics.finished")+ " "
			+ Integer.toString(finished) + "/" + Integer.toString(total)
			+ StatusBar.SEPARATOR + I18n.getMessage("thaw.plugin.statistics.failed") + " "
			+ Integer.toString(failed) + "/" + Integer.toString(total)
			+ StatusBar.SEPARATOR + I18n.getMessage("thaw.plugin.statistics.running") + " "
			+ Integer.toString(running) + "/" + Integer.toString(total)
			+ StatusBar.SEPARATOR + I18n.getMessage("thaw.plugin.statistics.pending") + " "
			+ Integer.toString(pending) + "/" + Integer.toString(total);

		core.getMainWindow().setStatus(IconBox.minConnectAction, status);
	}


	public boolean stop() {
		running = false;

		core.getMainWindow().setStatus(IconBox.blueBunny, "Thaw "+Main.VERSION);

		return true;
	}


	public String getNameForUser() {
		return I18n.getMessage("thaw.plugin.statistics.statistics");
	}

	public javax.swing.ImageIcon getIcon() {
		return IconBox.remove;
	}
}
