package thaw.core;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JOptionPane;

import java.util.Observer;
import java.util.Observable;
import java.util.Vector;
import java.util.Iterator;


/**
 * PluginConfigPanel. Creates and manages the panel containing all the things to configure
 *  the list of plugins.
 */
public class PluginConfigPanel implements Observer, ActionListener {
	private Core core;
	private JPanel pluginConfigPanel = null;

	private JLabel pluginsLoaded = null;
	private JList pluginList = null;
	
	private JPanel buttonPanel = null;
	private JButton removeButton = null;

	private JPanel subButtonPanel = null;
	private JTextField pluginToAdd = null;
	private JButton addButton = null;


	public PluginConfigPanel(ConfigWindow configWindow, Core core) {
		Vector pluginNames;

		this.core = core;

		this.pluginConfigPanel = new JPanel();

		this.pluginConfigPanel.setLayout(new BorderLayout());

		this.pluginsLoaded = new JLabel(I18n.getMessage("thaw.config.pluginsLoaded"));
		
		pluginNames = core.getConfig().getPluginNames();
		this.pluginList = new JList();
		// List is leave empty until windows is displayed (see update())
		
		this.pluginToAdd = new JTextField("", 30);
		this.pluginToAdd.addActionListener(this);
		
		this.buttonPanel = new JPanel();
		
		GridLayout layout = new GridLayout(2, 1);
		layout.setVgap(10);
		this.buttonPanel.setLayout(layout);
		
		this.subButtonPanel = new JPanel();
		
		this.subButtonPanel.setLayout(new GridLayout(1, 2));

		this.addButton = new JButton(I18n.getMessage("thaw.common.add"));
		this.removeButton = new JButton(I18n.getMessage("thaw.common.remove"));

		this.addButton.addActionListener(this);
		this.removeButton.addActionListener(this);
		
		this.buttonPanel.add(this.removeButton);

		this.subButtonPanel.add(this.pluginToAdd);
		this.subButtonPanel.add(this.addButton);
		this.buttonPanel.add(this.subButtonPanel);
		
		this.pluginConfigPanel.add(this.pluginsLoaded, BorderLayout.NORTH);
		this.pluginConfigPanel.add(this.pluginList, BorderLayout.CENTER);
		this.pluginConfigPanel.add(this.buttonPanel, BorderLayout.SOUTH);

		configWindow.addObserver(this);
	}


	public JPanel getPanel() {
		return this.pluginConfigPanel;
	}

	/**
	 * In fact, it's not used here, because config is immediatly updated when
	 * user change something.
	 */
	public void update(Observable o, Object arg) {
		if(arg == null) // Warns us window is now visible
			this.refreshList();
	}

	public void refreshList() {
		//pluginList.setListData(core.getConfig().getPluginNames());
		
		Iterator pluginNames = this.core.getConfig().getPluginNames().iterator();

		Vector toPutInTheList = new Vector();

		while(pluginNames.hasNext()) {
			String name = (String)pluginNames.next();
			toPutInTheList.add(name +
					   " ("+this.core.getPluginManager().getPlugin(name).getNameForUser()+")");
		}

		this.pluginList.setListData(toPutInTheList);
	}
	

	/**
	 * Return the class name contained in an option name from the list.
	 */
	public String getClassName(String optionName) {
		String[] part = optionName.split(" ");
		return part[0];
	}

	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == this.addButton || e.getSource() == this.pluginToAdd) {
			if(this.core.getPluginManager().loadPlugin(this.pluginToAdd.getText())
			   && this.core.getPluginManager().runPlugin(this.pluginToAdd.getText())) {

				this.core.getConfig().addPlugin(this.pluginToAdd.getText());
				this.refreshList();

			} else {
				Logger.error(this, "Unable to load '"+this.pluginToAdd.getText()+"'");
				JOptionPane.showMessageDialog(this.core.getConfigWindow().getFrame(),
							      "Unable to load plugin '"+this.pluginToAdd.getText()+"'",
							      "Unable to load plugin",
							      JOptionPane.ERROR_MESSAGE);
			}
		}


		if(e.getSource() == this.removeButton) {
			String pluginName = this.getClassName((String)this.pluginList.getSelectedValue());

			if(this.core.getPluginManager().stopPlugin(pluginName)
			   && this.core.getPluginManager().unloadPlugin(pluginName)) {
				
				this.core.getConfig().removePlugin(pluginName);
				this.refreshList();
			} else {
				Logger.error(this, "Unable to unload '"+pluginName+"'");
				JOptionPane.showMessageDialog(this.core.getConfigWindow().getFrame(),
							      "Unable to unload plugin '"+pluginName+"'",
							      "Unable to unload plugin",
							      JOptionPane.ERROR_MESSAGE);
			}
		}
	}
}
