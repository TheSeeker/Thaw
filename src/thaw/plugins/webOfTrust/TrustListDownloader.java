package thaw.plugins.webOfTrust;


import thaw.core.Config;

import thaw.fcp.FCPQueueManager;
import thaw.plugins.Hsqldb;


public class TrustListDownloader {
	private final FCPQueueManager queueManager;
	private final Config config;
	private final Hsqldb db;
	
	public TrustListDownloader(Hsqldb db, FCPQueueManager queueManager, Config config) {
		this.queueManager = queueManager;
		this.config = config;
		this.db = db;
	}
	
	public void init() {
		
	}
	
	public synchronized void process() {
		
	}
	
	public void stop() {
		
	}
}
