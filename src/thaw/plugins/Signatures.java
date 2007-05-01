package thaw.plugins;


import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;


import thaw.core.I18n;
import thaw.core.Core;
import thaw.core.Logger;
import thaw.core.PluginManager;
import thaw.core.LibraryPlugin;

import thaw.gui.IconBox;

import thaw.plugins.signatures.*;


public class Signatures extends LibraryPlugin implements ActionListener {
	private Core core;
	private Hsqldb db;
	private SigConfigTab configTab;

	public final static int DEFAULT_MIN_TRUST_LEVEL = -1; /* see Identity.trustLevelInt */

	/**
	 * because we must be sure that we won't be used anymore when we will
	 * unregister from the db
	 */
	private int used;

	public Signatures() {
		used = 0;
	}



	public boolean run(Core core) {
		this.core = core;

		used++;

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

		DatabaseManager.init(db, core.getConfig(),
				     core.getSplashScreen());

		if (core.getConfig().getValue("minTrustLevel") == null)
			core.getConfig().setValue("minTrustLevel", Integer.toString(DEFAULT_MIN_TRUST_LEVEL));

		configTab = new SigConfigTab(core.getConfig(), core.getConfigWindow(), db);

		core.getConfigWindow().addTab(I18n.getMessage("thaw.plugin.signature.signatures"),
					      thaw.gui.IconBox.minPeers,
					      configTab.getPanel());

		core.getConfigWindow().getOkButton().addActionListener(this);
		core.getConfigWindow().getCancelButton().addActionListener(this);

		return true;
	}


	public void realStart() {
		used++;
	}


	public boolean stop() {
		core.getConfigWindow().removeTab(configTab.getPanel());

		used--;

		if (used == 0)
			db.unregisterChild(this);

		return true;
	}


	public void realStop() {
		used--;

		if (used == 0)
			db.unregisterChild(this);
	}

	public String getNameForUser() {
		return I18n.getMessage("thaw.plugin.signature.pluginName");
	}

	public javax.swing.ImageIcon getIcon() {
		return IconBox.identity;
	}


	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == core.getConfigWindow().getOkButton()) {
			configTab.apply();
			return;
		}

		if (e.getSource() == core.getConfigWindow().getCancelButton()) {
			configTab.reset();
			return;
		}
	}
}
