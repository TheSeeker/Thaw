package thaw.plugins.index;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import thaw.core.Config;
import thaw.core.Logger;
import thaw.core.ThawThread;
import thaw.core.ThawRunnable;
import thaw.fcp.FCPQueueManager;
import thaw.plugins.Hsqldb;

public class AutoRefresh implements ThawRunnable, java.util.Observer {

	public final static boolean DEFAULT_ACTIVATED = true;
	public final static int DEFAULT_INTERVAL = 150;
	public final static int DEFAULT_INDEX_NUMBER = 10;

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
			Thread th = new ThawThread(this, "Index tree auto-refresher", this);
			th.start();
		}
	}


	public int updateNext(int lastIdx) {
		if (browserPanel.getIndexTree().numberOfUpdatingIndexes() >= nmbIndexesPerInterval) {
			Logger.debug(this, "Too many indexes are updating ; won't auto-update another one");
			return lastIdx;
		}

		try {
			Connection c = db.getConnection();
			PreparedStatement st;
			ResultSet results;
			int ret;

			st = c.prepareStatement("SELECT id, originalName, displayName, "+
						"       publicKey, privateKey, publishPrivateKey, "+
						"       author, positionInTree, revision, "+
						"       insertionDate "+
						"FROM indexes ORDER by RAND() LIMIT 1");

			results = st.executeQuery();


			if (results == null || !results.next()) {
				st.close();
				return -1;
			}

			ret = results.getInt("id");

			Index index;

			if (results.getString("privateKey") == null
			    || results.getInt("revision") > 0) {

				Logger.debug(this, "Index unavailable on freenet -> not updated");

				index = new Index(browserPanel.getDb(),
						  config,
						  results.getInt("id"),
						  null, results.getString("publicKey"),
						  results.getInt("revision"),
						  results.getString("privateKey"),
						  results.getBoolean("publishPrivateKey"),
						  results.getString("displayName"),
						  results.getDate("insertionDate"),
						  false, false);

				index.downloadFromFreenet(this, browserPanel.getIndexTree(), queueManager);

				browserPanel.getIndexTree().redraw();
			}
			
			st.close();

			return ret;

		} catch(java.sql.SQLException e) {
			Logger.error(this, "SQLEXCEPTION while autorefreshing: "+e.toString());
			return -2;
		}
	}


	public void update(java.util.Observable o, Object param) {

		browserPanel.getIndexTree().redraw(((Index)o).getTreePath(browserPanel.getIndexTree()));

		if (o.equals(browserPanel.getTables().getFileTable().getFileList())) {
			browserPanel.getTables().getFileTable().refresh();
		}

		if (o.equals(browserPanel.getTables().getLinkTable().getLinkList())) {
			browserPanel.getTables().getLinkTable().refresh();
		}
		browserPanel.getUnknownIndexList().addLinks((LinkList)o);
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
