package thaw.core;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;


/**
 * PluginConfigPanel. Creates and manages the panel containing all the things to configure
 *  the list of plugins.
 */
public class PluginConfigPanel implements Observer, ActionListener {
	private Core core;
	private JPanel pluginConfigPanel;

	private JLabel pluginsLoaded;
	private JList pluginList;

	private JPanel buttonPanel;
	private JButton removeButton;

	private JPanel subButtonPanel;
	private JTextField pluginToAdd;
	private JButton addButton;

	public PluginConfigPanel(final ConfigWindow configWindow, final Core core) {
		this.core = core;

		pluginConfigPanel = new JPanel();

		pluginConfigPanel.setLayout(new BorderLayout());

		pluginsLoaded = new JLabel(I18n.getMessage("thaw.config.pluginsLoaded"));

		pluginList = new JList();
		// List is leave empty until windows is displayed (see update())

		pluginToAdd = new JTextField("", 30);
		pluginToAdd.addActionListener(this);

		buttonPanel = new JPanel();

		final GridLayout layout = new GridLayout(2, 1);
		layout.setVgap(10);
		buttonPanel.setLayout(layout);

		subButtonPanel = new JPanel();

		subButtonPanel.setLayout(new GridLayout(1, 2));

		addButton = new JButton(I18n.getMessage("thaw.common.add"));
		removeButton = new JButton(I18n.getMessage("thaw.common.remove"));

		addButton.addActionListener(this);
		removeButton.addActionListener(this);

		buttonPanel.add(removeButton);

		subButtonPanel.add(pluginToAdd);
		subButtonPanel.add(addButton);
		buttonPanel.add(subButtonPanel);

		pluginConfigPanel.add(pluginsLoaded, BorderLayout.NORTH);
		pluginConfigPanel.add(pluginList, BorderLayout.CENTER);
		pluginConfigPanel.add(buttonPanel, BorderLayout.SOUTH);

		configWindow.addObserver(this);
	}


	public JPanel getPanel() {
		return pluginConfigPanel;
	}

	/**
	 * In fact, it's not used here, because config is immediatly updated when
	 * user change something.
	 */
	public void update(final Observable o, final Object arg) {
		if(arg == null) // Warns us window is now visible
			refreshList();
	}

	public void refreshList() {
		//pluginList.setListData(core.getConfig().getPluginNames());

		final Iterator pluginNames = core.getConfig().getPluginNames().iterator();

		final Vector toPutInTheList = new Vector();

		while(pluginNames.hasNext()) {
			final String name = (String)pluginNames.next();
			final Plugin plugin = core.getPluginManager().getPlugin(name);

			if (plugin != null) {
				toPutInTheList.add(name +
						   " ("+plugin.getNameForUser()+")");
			} else {
				toPutInTheList.add(name +
						   " (Plugin missing)");
			}
		}

		pluginList.setListData(toPutInTheList);
	}


	/**
	 * Return the class name contained in an option name from the list.
	 */
	public String getClassName(final String optionName) {
		final String[] part = optionName.split(" ");
		return part[0];
	}

	public void actionPerformed(final ActionEvent e) {
		if((e.getSource() == addButton) || (e.getSource() == pluginToAdd)) {
			if(core.getPluginManager().loadPlugin(pluginToAdd.getText())
			   && core.getPluginManager().runPlugin(pluginToAdd.getText())) {

				core.getConfig().addPlugin(pluginToAdd.getText());
				refreshList();

			} else {
				Logger.error(this, "Unable to load '"+pluginToAdd.getText()+"'");
				JOptionPane.showMessageDialog(core.getConfigWindow().getFrame(),
							      "Unable to load plugin '"+pluginToAdd.getText()+"'",
							      "Unable to load plugin",
							      JOptionPane.ERROR_MESSAGE);
			}
		}


		if(e.getSource() == removeButton) {
			final String pluginName = getClassName((String)pluginList.getSelectedValue());

			if (((String)pluginList.getSelectedValue()).indexOf("Plugin missing") > 0) {
				core.getConfig().removePlugin(pluginName);
				refreshList();
				return;
			}

			if(core.getPluginManager().stopPlugin(pluginName)
			   && core.getPluginManager().unloadPlugin(pluginName)) {
				core.getConfig().removePlugin(pluginName);
				refreshList();
			} else {
				Logger.error(this, "Error while unloading '"+pluginName+"'");
				JOptionPane.showMessageDialog(core.getConfigWindow().getFrame(),
							      "Unable to unload plugin '"+pluginName+"'",
							      "Unable to unload plugin",
							      JOptionPane.ERROR_MESSAGE);
			}
		}
	}
}
