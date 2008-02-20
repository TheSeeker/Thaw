package thaw.plugins.webOfTrust;

import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import thaw.core.Config;
import thaw.core.Logger;
import thaw.gui.Table;
import thaw.plugins.Hsqldb;
import thaw.plugins.signatures.Identity;
import thaw.plugins.signatures.IdentityTable;

public class WotIdentityList extends Observable implements Observer {
	private Hsqldb db;
	
	private IdentityTable table;
	private int usedIdentityId = -1;
	
	public WotIdentityList(Hsqldb db, Config config) {
		this.db = db;

		table = new IdentityTable(config, "wotIdList_", false);
		table.addObserver(this);
		
		try {
			if (config.getValue("wotIdentityUsed") != null)
				usedIdentityId = Integer.parseInt(config.getValue("wotIdentityUsed"));
		} catch(Exception e) {
			Logger.error(this, "Error in the config : can't find the identity to use to upload the trust list (or its keys) => won't insert the trust list ; Exception throwed: "+e.toString());
		}

		refresh();
	}
	
	public void refresh() {
		Vector yourIds = WotIdentity.getYourIdentities(db);
		
		Vector ids = new Vector();
		
		for (Iterator it = yourIds.iterator(); it.hasNext() ; ) {
			Identity i = (Identity)it.next();
			if (i.getId() == usedIdentityId)
				ids.add(i);
		}
		
		ids.addAll(WotIdentity.getOtherWotIdentities(db));
		table.setIdentities(ids);
	}
	
	public Table getList() {
		return table.getTable();
	}

	public void update(Observable arg0, Object arg1) {
		setChanged();
		notifyObservers(arg1);
	}
}
