package thaw.plugins.webOfTrust;

import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import thaw.core.Config;
import thaw.gui.Table;
import thaw.plugins.Hsqldb;
import thaw.plugins.signatures.IdentityTable;

public class WotIdentityList extends Observable implements Observer {
	private Hsqldb db;
	
	private IdentityTable table;
	
	public WotIdentityList(Hsqldb db, Config config) {
		this.db = db;

		table = new IdentityTable(config, "wotIdList_", false);
		table.addObserver(this);

		refresh();
	}
	
	public void refresh() {
		Vector ids = WotIdentity.getOtherIdentities(db);
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
