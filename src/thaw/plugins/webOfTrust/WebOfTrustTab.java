package thaw.plugins.webOfTrust;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import thaw.core.Config;
import thaw.core.I18n;
import thaw.plugins.Hsqldb;
import thaw.plugins.Signatures;
import thaw.plugins.signatures.Identity;


public class WebOfTrustTab implements Signatures.SignaturesObserver, Observer {
	
	private Config config;
	
	private JSplitPane mainSplit;
	private JPanel rightPanel;
	private JPanel itsTrustListPanel;

	private WotIdentityList idList;
	private TrustListTable trustListTable;
	
	public WebOfTrustTab(Hsqldb db, Config config) {
		this.config = config;
		
		JPanel leftPanel;
				
		leftPanel = new JPanel(new BorderLayout(5,5));
		rightPanel = new JPanel(new GridLayout(1, 1));
		itsTrustListPanel = new JPanel(new BorderLayout(5,5));
		
		idList = new WotIdentityList(db, config);
		trustListTable = new TrustListTable(db, config);
		
		leftPanel.add(new JLabel(I18n.getMessage("thaw.plugin.wot.yourTrustList")), BorderLayout.NORTH);
		leftPanel.add(new JScrollPane(idList.getList()), BorderLayout.CENTER);
		itsTrustListPanel.add(new JLabel(I18n.getMessage("thaw.plugin.wot.itsTrustList")), BorderLayout.NORTH);
		itsTrustListPanel.add(new JScrollPane(trustListTable.getTable()), BorderLayout.CENTER);
		
		rightPanel.add(itsTrustListPanel);
		
		mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
									leftPanel, rightPanel);
		
		idList.addObserver(this);
	}
	
	public void loadState() {
		Signatures.addObserver(this);
		
		mainSplit.setResizeWeight(0.5);
		
		String val;

		if ((val = config.getValue("wotMainSplitPosition")) != null)
			mainSplit.setDividerLocation(Integer.parseInt(val));
		else
			mainSplit.setDividerLocation(350);
	}
	
	public JSplitPane getPanel() {
		return mainSplit;
	}
	
	public void saveState() {
		Signatures.deleteObserver(this);
		
		config.setValue("wotMainSplitPosition",
						Integer.toString(mainSplit.getDividerLocation()));
	}
	
	public void refresh() {
		idList.refresh();
	}

	public void identityUpdated(Identity i) { refresh(); }
	public void privateIdentityAdded(Identity i) { refresh(); }
	public void publicIdentityAdded(Identity i) { refresh(); }

	public void update(Observable o, Object param) {
		if (o == idList) {
			trustListTable.refresh((Identity)param);
		}
	}
}
