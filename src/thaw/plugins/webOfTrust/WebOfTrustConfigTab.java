package thaw.plugins.webOfTrust;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JComboBox;

import thaw.core.Config;
import thaw.core.I18n;
import thaw.plugins.Hsqldb;
import thaw.plugins.signatures.Identity;

public class WebOfTrustConfigTab implements Observer, ActionListener {
	private thaw.core.ConfigWindow configWindow;
	private Config config;
	private Hsqldb db;
	
	private JPanel panel;
	private JCheckBox activated;
	private JComboBox identityUsed;
	private JComboBox numberOfRefresh;
	
	public WebOfTrustConfigTab(thaw.core.ConfigWindow configWindow,
							Config config, Hsqldb db) {
		this.configWindow = configWindow;
		this.config = config;
		this.db = db;
		
		panel = new JPanel(new java.awt.GridLayout(15, 1));
		
		activated = new JCheckBox(I18n.getMessage("thaw.plugin.wot.activated"));
		panel.add(activated);
		
		panel.add(new JLabel(""));
		panel.add(new JLabel(I18n.getMessage("thaw.plugin.wot.usedIdentity")));
		
		identityUsed = new JComboBox();
		panel.add(identityUsed);
		
		panel.add(new JLabel(""));
		panel.add(new JLabel(I18n.getMessage("thaw.plugin.wot.numberOfRefresh")));
		
		numberOfRefresh = new JComboBox();
		for (int i = 0 ; i <= 100 ; i++)
			numberOfRefresh.addItem(new Integer(i));
		panel.add(numberOfRefresh);
		
		resetContentOfIdentitySelector();
		reloadSettings();
		readActivated();
		
		activated.addActionListener(this);
		configWindow.addObserver(this);
	}
	
	private void resetContentOfIdentitySelector() {
		Vector identities = Identity.getYourIdentities(db);
		
		identityUsed.removeAllItems();
		identityUsed.addItem(I18n.getMessage("thaw.plugin.wot.usedIdentity.none"));
		
		for (Iterator it = identities.iterator();
			it.hasNext();) {
			identityUsed.addItem(it.next().toString());
		}
	}
	
	protected void reloadSettings() {
		/* default values */
		activated.setSelected(true);
		numberOfRefresh.setSelectedItem(new Integer(10));
		identityUsed.setSelectedItem(I18n.getMessage("thaw.plugin.wot.usedIdentity.none"));
		
		/* loading values */
		if (config.getValue("wotActivated") != null)
			activated.setSelected(Boolean.valueOf(config.getValue("wotActivated")).booleanValue());
		if (config.getValue("wotIdentityUsed") != null)
			identityUsed.setSelectedItem(config.getValue("wotIdentityUsed"));
		if (config.getValue("wotNumberOfRefresh") != null)
			numberOfRefresh.setSelectedItem(new Integer(config.getValue("wotNumberOfRefresh")));
	}
	
	protected void saveSettings() {
		config.setValue("wotActivated", Boolean.valueOf(activated.isSelected()).toString());

		if (identityUsed.getSelectedIndex() <= 0)
			config.setValue("wotIdentityUsed", null);
		else
			config.setValue("wotIdentityUsed", identityUsed.getSelectedItem().toString());

		config.setValue("wotNumberOfRefresh", numberOfRefresh.getSelectedItem().toString());
	}
	
	private void readActivated() {
		boolean s = activated.isSelected();
		
		identityUsed.setEnabled(s);
		numberOfRefresh.setEnabled(s);
	}
	
	public JPanel getPanel() {
		return panel;
	}

	public void update(Observable o, Object param) {
		if (param == configWindow.getOkButton())
			saveSettings();

		resetContentOfIdentitySelector();
		reloadSettings();
		readActivated();
	}

	public void actionPerformed(ActionEvent e) {
		readActivated();
	}
}
