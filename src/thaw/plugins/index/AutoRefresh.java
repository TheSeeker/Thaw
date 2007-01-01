package thaw.plugins.index;

import java.sql.*;

import thaw.core.Config;
import thaw.core.Logger;
import thaw.plugins.Hsqldb;

import thaw.fcp.FCPQueueManager;

public class AutoRefresh implements Runnable {

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

			st = c.prepareStatement("SELECT id, originalName, displayName, publicKey, privateKey, author, positionInTree, revision "+
						"FROM indexes WHERE id > ? ORDER by id LIMIT 1");

			if (lastIdx != -1) {
				st.setInt(1, lastIdx);
				if (st.execute())
					results = st.getResultSet();
				else
					return -1;
			} else {
				results = db.executeQuery("SELECT id, originalName, displayName, publicKey, privateKey, author, positionInTree, revision "+
							  "FROM indexes ORDER by Id LIMIT 1");

				if (results == null)
					return -1;
			}

			if (!results.next())
				return -1;

			ret = results.getInt("id");

			Index index;

			index = browserPanel.getIndexTree().getRegisteredIndex(results.getString("publicKey"));

			if (index == null)
				index = new Index(queueManager, browserPanel,
						  results.getInt("id"), null,
						  results.getString("originalName"),
						  results.getString("displayName"),
						  results.getString("publicKey"),
						  results.getString("privateKey"),
						  results.getInt("revision"),
						  results.getString("author"));

			if (index.getPrivateKey() != null) {
				Logger.debug(this, "Private key found ! index ignored");
				return ret;
			}

			index.updateFromFreenet(-1);

			return ret;

		} catch(java.sql.SQLException e) {
			Logger.error(this, "SQLEXCEPTION while autorefreshing: "+e.toString());
			return -2;
		}
	}


	public void run() {
		int lastIdx = -1;

		while(threadRunning) {
			try {
				Thread.sleep(1000 * subInterval);
			} catch(java.lang.InterruptedException e) {
				/* \_o< */
			}

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
