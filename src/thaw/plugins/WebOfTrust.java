package thaw.plugins;

import javax.swing.ImageIcon;

import thaw.core.Core;
import thaw.core.Logger;
import thaw.core.I18n;
import thaw.core.ThawThread;
import thaw.core.ThawRunnable;
import thaw.fcp.FreenetURIHelper;
import thaw.plugins.Hsqldb;
import thaw.plugins.Signatures;
import thaw.plugins.signatures.Identity;
import thaw.plugins.webOfTrust.*;

public class WebOfTrust extends thaw.core.LibraryPlugin {
	public final static long UPLOAD_AFTER_MS = 30*60*1000; /* 30 min */ 
	
	private Core core;
	private Hsqldb db;
	private Signatures sigs;
	
	private WebOfTrustConfigTab configTab = null;
	
	private int used = 0;
	
	public WebOfTrust() { used = 0; }
	
	private boolean loadDeps(Core core) {
		/* Hsqldb */
		if(core.getPluginManager().getPlugin("thaw.plugins.Hsqldb") == null) {
			Logger.info(this, "Loading Hsqldb plugin");

			if(core.getPluginManager().loadPlugin("thaw.plugins.Hsqldb") == null
			   || !core.getPluginManager().runPlugin("thaw.plugins.Hsqldb")) {
				Logger.error(this, "Unable to load thaw.plugins.Hsqldb !");
				return false;
			}
		}

		db = (Hsqldb)core.getPluginManager().getPlugin("thaw.plugins.Hsqldb");
		db.registerChild(this);

		/* Signatures */
		if(core.getPluginManager().getPlugin("thaw.plugins.Signatures") == null) {
			Logger.info(this, "Loading Signatures plugin");

			if(core.getPluginManager().loadPlugin("thaw.plugins.Signatures") == null
			   || !core.getPluginManager().runPlugin("thaw.plugins.Signatures")) {
				Logger.error(this, "Unable to load thaw.plugins.Signatures !");
				return false;
			}
		}

		sigs = (Signatures)core.getPluginManager().getPlugin("thaw.plugins.Signatures");
		sigs.registerChild(this);
		
		return true;
	}
	
	private boolean unloadDeps(Core core) {
		
		if (sigs != null)
			sigs.unregisterChild(this);
		if (db != null)
			db.unregisterChild(this);
		
		sigs = null;
		db = null;
		
		return true;
	}

	public ImageIcon getIcon() {
		return thaw.gui.IconBox.trust;
	}

	public String getNameForUser() {
		return I18n.getMessage("thaw.plugin.wot");
	}
	
	private class TrucMucheThread implements ThawRunnable {
		private boolean running = false;

		public TrucMucheThread() {
			running = false;
		}
		
		public void run() {
			running = true;
			
			while(running) {
				try {
					Thread.sleep(1000);
				} catch(InterruptedException e) {
					/* \_o< */
				}
				
				try {
					if (running)
						process();
				} catch(Exception e) {
					Logger.error(this, "Exception in the web of trust plugin : "+e.toString());
					e.printStackTrace();
				}
			}
		}
		
		public void stop() {
			running = false;
		}
	}
	
	private TrucMucheThread thread;
	private TrustListUploader trustListUploader = null;
	
	private void initThread() {
		trustListUploader = new TrustListUploader(db, core.getQueueManager(), core.getConfig());
		trustListUploader.init();

		thread = new TrucMucheThread();
		new ThawThread(thread, "WoT refresher", this).start();
	}
	
	private void process() {
		if (trustListUploader != null)
			trustListUploader.process();
	}
	
	private void stopThread() {
		if (thread != null) {
			thread.stop();
			thread = null;
		}
		
		if (trustListUploader != null) {
			trustListUploader.stop();
			trustListUploader = null;
		}
	}

	public boolean run(Core core) {
		core.getConfig().addListener("wotActivated",        this);
		core.getConfig().addListener("wotIdentityUsed",     this);
		core.getConfig().addListener("wotNumberOfRefresh",  this);
		core.getConfig().addListener("wotPrivateKey",       this);
		core.getConfig().addListener("wotPublicKey",        this);

		used++;
		
		this.core = core;
		
		if (!loadDeps(core))
			return false;
		
		configTab = new WebOfTrustConfigTab(core.getConfigWindow(),
											core.getConfig(), db);

		core.getConfigWindow().addTab(I18n.getMessage("thaw.plugin.wot"),
			      thaw.gui.IconBox.minTrust,
			      configTab.getPanel());
		
		initThread();

		return true;
	}
	
	private Identity getUsedIdentity() {
		return trustListUploader.getIdentityUsed();
	}
	
	private String getTrustListPublicKey() {
		String key;
		
		if ( (key = core.getConfig().getValue("wotPublicKey")) == null)
			return null;
		
		return FreenetURIHelper.convertSSKtoUSK(key)+"/trustList/0/trustList.xml";
	}
	
	public String getTrustListPublicKeyFor(Identity id) {
		if (id.equals(getUsedIdentity()))
			return getTrustListPublicKey();
		return null;
	}
	
	public void addTrustList(Identity id, String publicKey) {
		/* TODO */
	}

	public void stop() {
		used--;
		
		if (configTab != null) {
			core.getConfigWindow().removeTab(configTab.getPanel());
			configTab = null;
		}
		
		if (used == 0) {
			unloadDeps(core);
		}
		
		stopThread();
	}

	public void realStart() {
		used++;
	}

	public void realStop() {
		used--;
		
		if (used == 0)
			unloadDeps(core);
	}

}
