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

	public final static String[][] DEVS = {
		/* { nick, publicKey } */
		{ "toad",
		  "CHwpbtSA+asXiF3s0xGX1hd3nA2scMMjSZbh:"+
		  "AKC/rtW2/X1DQH9VUzpunZgGXIlhFzFeDX4CT5nGjq7Fbkx9ipgekog5T4ElLVZ6nZDaf9t+t"+
		  "XRbqdBBo4SI7+OnHHJ8MVgRm5doU6bWtMbtEQ5E3LQk8174TNoWKGJ2cuvEsmzgE4BQZPLErs"+
		  "1rVfAfAziI0YPWFHKq2OzEOGW5" },
		{ "nextgens",
		  "CHwpbtSA+asXiF3s0xGX1hd3nA2scMMjSZbh:"+
		  "AM8giqR7C4ND7RxQzByb/ZOLEPoNRd818proqkW5rvqpPd5ODZvJYLZgDC+fe01BU0MFnE7LP"+
		  "tolulBNxiz4AaIyL51FnnzZt9Dp60h9lgawxttQ2DHx553hszZ6LQ5iAvzDivjmKGzWa6p17F"+
		  "+qo2OaGRXL3SGk1fsS40qgdVsP" },
		{ "Jflesch",
		  "CHwpbtSA+asXiF3s0xGX1hd3nA2scMMjSZbh:"+
		  "AIf5yXowXT/0Aa5UhwzOg/kZQnHar/dQl1lFqrcB6TKXf42LZecR1tNV9Hsxboa3Eo5JWdK9T"+
		  "AnHCLBu2lkYUOtcqEH2U0DkAfR11AYn2sdazk/LSFicAq7Ic6ZO7An2RJJUtcHY7pB164GZ76"+
		  "jpOn29iPagMq17n4zbZNqe03q1" },
		{ "bback",
		  "CHwpbtSA+asXiF3s0xGX1hd3nA2scMMjSZbh:"+
		  "AKLw/y7/oDmXYYT0KWNeAfJPcVRgns+Nw9rJ0X/2JLt7o73Yt9PMkx5STJhlJekF36sf+l3vk"+
		  "TzPNHa2xmyjHms2biHl3jGy9dHdAduqNKs99PyW0Z19/QbZ1HEeeCXgllKE+z4aHgWK2CzeID"+
		  "6wPkzZLjFInKLb3gZtQLh5cm3n" },
		{ "Zothar",
		  "CHwpbtSA+asXiF3s0xGX1hd3nA2scMMjSZbh:"+
		  "AL81sQtHFj5QZl/6GoG1fP0lmdHQOtvVuuxN60MWx1kLqByk17XlnP/wxGZyffSRdnZSmZoN0"+
		  "dprYrFJo9qj3mhsqmA8Gk50mkfX/gnkWzT2ChwkWIvk/+gPtOLXXx4yrGQlnNIQQ/V7y3YaN9"+
		  "706zYgySMcW1cv8eqhPxNZ7yYB" }

		/* TODO:
		 * Missing:
		 * - bombe
		 */

		/* Can't do:
		 * - sanity : no definitive key
		 */
	};


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
		core.getConfigWindow().getOkButton().removeActionListener(this);
		core.getConfigWindow().getCancelButton().removeActionListener(this);
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
