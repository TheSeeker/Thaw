package thaw.plugins.index;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import thaw.core.Config;
import thaw.core.Logger;
import thaw.fcp.FCPQueueManager;
import thaw.plugins.Hsqldb;

public class AutoRefresh implements Runnable, java.util.Observer {

	public final static boolean DEFAULT_ACTIVATED = true;
	public final static int DEFAULT_INTERVAL = 300;
	public final static int DEFAULT_INDEX_NUMBER = 20;

	private Hsqldb db;
	private IndexBrowserPanel browserPanel;
	private Config config;

	private boolean threadRunning;

	private int interval;
	private int nmbIndexesPerInterval;

	private int subInterval;
	private int nmbIndexesPerSubInterval;

	private FCPQueueManager queueManager;

	public AutoRefresh(Hsqldb db, IndexBrowserPanel indexBrowser, FCPQueueManager queueManager, Config config) {
		this.browserPanel = indexBrowser;
		this.queueManager = queueManager;
		this.config = config;
		threadRunning = false;

		interval = 0;
		nmbIndexesPerInterval = 0;

		try {
			if (config.getValue("indexRefreshInterval") != null) {
				interval = Integer.parseInt(config.getValue("indexRefreshInterval"));
			}

			if (config.getValue("nmbIndexesPerRefreshInterval") != null) {
				nmbIndexesPerInterval = Integer.parseInt(config.getValue("nmbIndexesPerRefreshInterval"));
			}
		} catch(NumberFormatException e) {
			Logger.error(this, "Error while parsing value in the configuration, using default ones");
			interval = 0;
			nmbIndexesPerInterval = 0;
		}

		if (interval == 0)
			interval = DEFAULT_INTERVAL;
		if (nmbIndexesPerInterval == 0)
			nmbIndexesPerInterval = DEFAULT_INDEX_NUMBER;


		this.db = db;


		if (interval >= nmbIndexesPerInterval) {
			nmbIndexesPerSubInterval = 1;
			subInterval = (interval / nmbIndexesPerInterval);
		} else {
			subInterval = 1;
			nmbIndexesPerSubInterval = (nmbIndexesPerInterval / interval);
		}


	}

	public void start() {
		if (!threadRunning) {
			threadRunning = true;
			Thread th = new Thread(this);
			th.start();
		}
	}


	public int updateNext(int lastIdx) {
		try {
			Connection c = db.getConnection();
			PreparedStatement st;
			ResultSet results;
			int ret;

			if (lastIdx != -1) {
				st = c.prepareStatement("SELECT id, originalName, displayName, publicKey, privateKey, author, positionInTree, revision "+
							"FROM indexes WHERE id > ? ORDER by id LIMIT 1");

				st.setInt(1, lastIdx);

				results = st.executeQuery();
			} else {
				results = db.executeQuery("SELECT id, originalName, displayName, publicKey, privateKey, author, positionInTree, revision "+
							  "FROM indexes ORDER by Id LIMIT 1");
			}

			if (results == null || !results.next())
				return -1;

			ret = results.getInt("id");

			Index index;

			if (results.getString("privateKey") == null
			    || results.getInt("revision") > 0) {

				Logger.debug(this, "Index unavailable on freenet -> not updated");

				index = new Index(browserPanel.getDb(),
						  results.getInt("id"),
						  null, results.getString("publicKey"),
						  results.getInt("revision"),
						  results.getString("privateKey"),
						  results.getString("displayName"),
						  false);

				index.downloadFromFreenet(this, browserPanel.getIndexTree(), queueManager);

				browserPanel.getIndexTree().redraw();

				browserPanel.getIndexProgressBar().addTransfer(1);
			}

			return ret;

		} catch(java.sql.SQLException e) {
			Logger.error(this, "SQLEXCEPTION while autorefreshing: "+e.toString());
			return -2;
		}
	}


	public void update(java.util.Observable o, Object param) {

		if (((Index)o).hasChanged())
			browserPanel.getIndexTree().redraw();
		else
			browserPanel.getIndexTree().redraw();

		if (o.equals(browserPanel.getTables().getFileTable().getFileList())) {
			browserPanel.getTables().getFileTable().refresh();
		}

		if (o.equals(browserPanel.getTables().getLinkTable().getLinkList())) {
			browserPanel.getTables().getLinkTable().refresh();
		}
		browserPanel.getUnknownIndexList().addLinks((LinkList)o);
		browserPanel.getIndexProgressBar().removeTransfer(1);
	}


	public void run() {
		int lastIdx = -1;

		while(threadRunning) {
			try {
				Thread.sleep(1000 * subInterval);
			} catch(java.lang.InterruptedException e) {
				/* \_o< */
			}

			if (!threadRunning)
				break;

			for (int i = 0 ; i < nmbIndexesPerSubInterval ; i++) {
				lastIdx = updateNext(lastIdx);

				if (lastIdx == -2) {
					Logger.error(this, "Disabling auto-refreshing !");
					return;
				}
			}
		}
	}

	public void stop() {
		if (threadRunning)
			threadRunning = false;
	}


}
