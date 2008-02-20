package thaw.plugins;

import javax.swing.ImageIcon;

import thaw.core.Core;
import thaw.core.I18n;
import thaw.core.Logger;
import thaw.core.Plugin;

import thaw.plugins.webOfTrust.*;

public class WebOfTrustViewer implements Plugin {
	private Core core;
	private Hsqldb db;
	private WebOfTrust wot;

	private WebOfTrustTab wotTab;
	
	public WebOfTrustViewer() {
		
	}

	public ImageIcon getIcon() {
		return thaw.gui.IconBox.trust;
	}

	public String getNameForUser() {
		return I18n.getMessage("thaw.plugin.wot.viewer");
	}

	public boolean run(Core core) {
		this.core = core;
		
		core.getConfig().addListener("wotIdentityUsed", this);

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
		
		/* wot */
		if(core.getPluginManager().getPlugin("thaw.plugins.WebOfTrust") == null) {
			Logger.info(this, "Loading WoT plugin");

			if(core.getPluginManager().loadPlugin("thaw.plugins.WebOfTrust") == null
			   || !core.getPluginManager().runPlugin("thaw.plugins.WebOfTrust")) {
				Logger.error(this, "Unable to load thaw.plugins.WebOfTrust !");
				return false;
			}
		}

		wot = (WebOfTrust)core.getPluginManager().getPlugin("thaw.plugins.WebOfTrust");
		wot.registerChild(this);
		
		/* GUI */
		
		if (core.getConfig().getValue("wotActivated") == null
				|| Boolean.valueOf(core.getConfig().getValue("wotActivated")).booleanValue()) {
			
				wotTab = new WebOfTrustTab(db, core.getConfig());

				core.getMainWindow().addTab(I18n.getMessage("thaw.plugin.wot"),
							    thaw.gui.IconBox.trust,
							    wotTab.getPanel());

				core.getMainWindow().getMainFrame().validate();

				wotTab.loadState();
		}
		
		return true;
	}

	public void stop() {
		if (wotTab != null) {
			core.getMainWindow().removeTab(wotTab.getPanel());
			wotTab = null;
		}
		
		if (wot != null)
			wot.unregisterChild(this);
		if (db != null)
			db.unregisterChild(this);
		}

}
