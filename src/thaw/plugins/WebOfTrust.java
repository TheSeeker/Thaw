package thaw.plugins;

import javax.swing.ImageIcon;

import thaw.core.Core;
import thaw.core.Logger;
import thaw.core.I18n;
import thaw.plugins.Hsqldb;
import thaw.plugins.Signatures;
import thaw.plugins.webOfTrust.WebOfTrustConfigTab;

public class WebOfTrust extends thaw.core.LibraryPlugin {
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

	public boolean run(Core core) {
		core.getConfig().addListener("wotActivated",        this);
		core.getConfig().addListener("wotIdentityUsed",     this);
		core.getConfig().addListener("wotNumberOfRefresh",  this);

		used++;
		
		this.core = core;
		
		if (!loadDeps(core))
			return false;
		
		configTab = new WebOfTrustConfigTab(core.getConfigWindow(),
											core.getConfig(), db);

		core.getConfigWindow().addTab(I18n.getMessage("thaw.plugin.wot"),
			      thaw.gui.IconBox.minTrust,
			      configTab.getPanel());

		return true;
	}

	public void stop() {
		used--;
		
		if (configTab != null) {
			core.getConfigWindow().removeTab(configTab.getPanel());
			configTab = null;
		}
		
		if (used == 0)
			unloadDeps(core);
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
