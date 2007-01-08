package thaw.core;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;
import java.util.LinkedHashMap;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JCheckBox;

/**
 * PluginConfigPanel. Creates and manages the panel containing all the things to configure
 *  the list of plugins.
 */
public class PluginConfigPanel implements Observer, ActionListener {
	private Core core;
	private JPanel pluginConfigPanel;

	private Vector pluginCheckBoxes = null;

	public PluginConfigPanel(final ConfigWindow configWindow, final Core core) {
		this.core = core;

		pluginConfigPanel = new JPanel();
		pluginConfigPanel.setLayout(new GridLayout(16,1));
		configWindow.addObserver(this);
		//refreshList();
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


	/**
	 * We regenerate each time all the checkboxes<br/>
	 * Okay, it's dirty, but it should work
	 */
	public void refreshList() {
		if (pluginCheckBoxes != null) {
			for(Iterator it = pluginCheckBoxes.iterator();
			    it.hasNext();) {
				pluginConfigPanel.remove((JCheckBox)it.next());
			}
		}

		PluginManager pluginManager = core.getPluginManager();

		pluginCheckBoxes = new Vector();

		String[] knownPlugins = pluginManager.getKnownPlugins();

		for (int i = 0 ; i < knownPlugins.length ; i++) {
			JCheckBox c = new JCheckBox(knownPlugins[i]);
			c.addActionListener(this);
			c.setSelected(false);
			pluginCheckBoxes.add(c);
			pluginConfigPanel.add(c);
		}

		LinkedHashMap loadedPlugins = pluginManager.getPlugins();
		Iterator it = (new Vector(loadedPlugins.values())).iterator();

		while(it.hasNext()) {
			Plugin plugin = (Plugin)it.next();

			Iterator checkBoxIt = pluginCheckBoxes.iterator();

			while(checkBoxIt.hasNext()) {
				JCheckBox c = (JCheckBox)checkBoxIt.next();

				if (c.getText().equals(plugin.getClass().getName())) {
					c.setSelected(true);
					c.setText(c.getText()+" ("+plugin.getNameForUser()+")");
				}
			}
		}
	}


	/**
	 * Return the class name contained in an option name from the list.
	 */
	protected String getClassName(final JCheckBox checkBox) {
		final String[] part = checkBox.getText().split(" ");
		return part[0];
	}

	public void actionPerformed(final ActionEvent e) {

		if (e.getSource() instanceof JCheckBox) {
			boolean load;
			JCheckBox c = (JCheckBox)e.getSource();

			load = c.isSelected();

			if (load) {
				if (core.getPluginManager().loadPlugin(getClassName(c)) == null
				    || !core.getPluginManager().runPlugin(getClassName(c)))
					load = false;
				else
					core.getConfig().addPlugin(getClassName(c));
			}

			if (!load) {
				core.getPluginManager().stopPlugin(getClassName(c));
				core.getPluginManager().unloadPlugin(getClassName(c));
				core.getConfig().removePlugin(getClassName(c));
			}

			refreshList();
		}
	}
}
