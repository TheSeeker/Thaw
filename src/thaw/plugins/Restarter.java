package thaw.plugins;

import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JCheckBox;
import java.awt.GridLayout;

import java.util.Vector;
import java.util.Iterator;
import java.util.Observer;
import java.util.Observable;
import java.util.Random;

import thaw.core.*;
import thaw.fcp.*;

/**
 * This plugin, after a given time, restart all/some the failed downloads (if maxDownloads >= 0, downloads to restart are choosen randomly).
 * A not too bad example to show how to make plugins.
 * @deprecated When this plugin was created, MaxRetries was stupidly set to 0 instead of -1 => now this plugin is useless.
 */
public class Restarter implements Observer, Runnable, Plugin {

	private int interval = 180; /* in s */
	private boolean restartFatals = false;

	private int timeElapsed = 0;

	private Core core;
	private boolean running = true;

	private Thread restarter = null;

	private JPanel configPanel;

	private JLabel restartIntervalLabel;
	private JTextField restartIntervalField;

	private JCheckBox restartFatalsBox;


	public boolean run(Core core) {
		this.core = core;

		/* Reloading value from the configuration */
		try {
			if(core.getConfig().getValue("restartInterval") != null
			   && core.getConfig().getValue("restartFatals") != null) {
				this.interval = Integer.parseInt(core.getConfig().getValue("restartInterval"));
				this.restartFatals = Boolean.valueOf(core.getConfig().getValue("restartFatals")).booleanValue();
			}
		} catch(Exception e) { /* probably conversion errors */ /* Yes I know, it's dirty */
			Logger.notice(this, "Unable to read / understand value from the config. Using default values");
		}


		/* Adding restart config tab to the config window */
		this.configPanel = new JPanel();
		this.configPanel.setLayout(new GridLayout(15, 1));

		this.restartIntervalLabel = new JLabel(I18n.getMessage("thaw.plugin.restarter.interval"));
		this.restartIntervalField = new JTextField(Integer.toString(this.interval));

		this.restartFatalsBox = new JCheckBox(I18n.getMessage("thaw.plugin.restarter.restartFatals"), this.restartFatals);

		this.configPanel.add(this.restartIntervalLabel);
		this.configPanel.add(this.restartIntervalField);
		this.configPanel.add(this.restartFatalsBox);

		core.getConfigWindow().addTab(I18n.getMessage("thaw.plugin.restarter.configTabName"), this.configPanel);
		core.getConfigWindow().addObserver(this);

		this.running = true;
		this.restarter = new Thread(this);
		this.restarter.start();

		return true;
	}


	public boolean stop() {
		this.core.getConfigWindow().removeTab(this.configPanel);
		this.core.getConfigWindow().deleteObserver(this);
		this.running = false;

		return true;
	}


	public void run() {
		while(this.running) {
			try {

				for(this.timeElapsed = 0 ; this.timeElapsed < this.interval && this.running; this.timeElapsed++) {
					Thread.sleep(1000);
				}

			} catch(java.lang.InterruptedException e) {
				// We really really really don't care.
			}

			Logger.notice(this, "Restarting [some] failed downloads");

			try {
				if(!this.running)
					break;

				int maxDownloads = this.core.getQueueManager().getMaxDownloads();
				int alreadyRunning = 0;
				int failed = 0;
				Vector runningQueue = this.core.getQueueManager().getRunningQueue();

				if(maxDownloads >= 0) {
					/* We count how many are really running
					   and we write down those which are failed */
					for(Iterator it = runningQueue.iterator();
					    it.hasNext();) {
						FCPTransferQuery query = (FCPTransferQuery)it.next();

						if(query.getQueryType() != 1)
							continue;

						if(query.isRunning() && !query.isFinished()) {
							alreadyRunning++;
						}

						if(query.isFinished() && !query.isSuccessful()
						   && (this.restartFatals || !query.isFatallyFailed()) ) {
							failed++;
						}
					}

					/* We choose randomly the ones to restart */
					while(alreadyRunning < maxDownloads && failed > 0) {
						int toRestart = (new Random()).nextInt(failed);

						Iterator it = runningQueue.iterator();
						int i = 0;

						while(it.hasNext()) {
							FCPTransferQuery query = (FCPTransferQuery)it.next();

							if(query.getQueryType() != 1)
								continue;

							if(query.isFinished() && !query.isSuccessful()
							   && (this.restartFatals || !query.isFatallyFailed())) {
								if(i == toRestart) {
									this.restartQuery(query);
									break;
								}

								i++;
							}
						}

						alreadyRunning++;
						failed--;
					}


				} else { /* => if maxDownloads < 0 */

					/* We restart them all */
					for(Iterator it = runningQueue.iterator();
					    it.hasNext();) {
						FCPTransferQuery query = (FCPTransferQuery)it.next();

						if(query.getQueryType() == 1 && query.isFinished()
						   && !query.isSuccessful()
						   && (this.restartFatals || !query.isFatallyFailed()))
							this.restartQuery(query);
					}
				}

			} catch(java.util.ConcurrentModificationException e) {
				Logger.notice(this, "Collision ! Sorry I will restart failed downloads later ...");
			} catch(Exception e) {
				Logger.error(this, "Exception : "+e);
			}

		}
	}


	public void restartQuery(FCPTransferQuery query) {
		query.stop(this.core.getQueueManager());

		if(query.getMaxAttempt() >= 0)
			query.setAttempt(0);

		query.start(this.core.getQueueManager());
	}

	public void update(Observable o, Object arg) {
		if(o == this.core.getConfigWindow()) {

			if(arg == this.core.getConfigWindow().getOkButton()){
				this.core.getConfig().setValue("restartInterval", this.restartIntervalField.getText());
				this.core.getConfig().setValue("restartFatals",
							  Boolean.toString(this.restartFatalsBox.isSelected()));

				/* Plugin will be stop() and start(), so no need to reload config */

				return;
			}

			if(arg == this.core.getConfigWindow().getCancelButton()) {
				this.restartIntervalField.setText(Integer.toString(this.interval));
				this.restartFatalsBox.setSelected(this.restartFatals);

				return;
			}
		}
	}

	public String getNameForUser() {
		return I18n.getMessage("thaw.plugin.restarter.restarter");
	}

}
