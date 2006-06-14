package thaw.core;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JDialog;
import javax.swing.JTextField;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JOptionPane;

import java.util.Observer;
import java.util.Observable;
import java.util.Vector;

import thaw.core.i18n.I18n;


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

		pluginConfigPanel = new JPanel();

		pluginConfigPanel.setLayout(new BorderLayout());

		pluginsLoaded = new JLabel(I18n.getMessage("thaw.config.pluginsLoaded"));
		
		pluginNames = core.getConfig().getPluginNames();
		pluginList = new JList();
		pluginList.setListData(core.getConfig().getPluginNames());
		
		
		pluginToAdd = new JTextField("", 30);
		
		buttonPanel = new JPanel();
		buttonPanel.setLayout(new GridLayout(2, 1));
		
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
	public void update(Observable o, Object arg) {

	}


	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == addButton) {
			if(core.getPluginManager().loadPlugin(pluginToAdd.getText())
			   && core.getPluginManager().runPlugin(pluginToAdd.getText())) {

				core.getConfig().addPlugin(pluginToAdd.getText());
				pluginList.setListData(core.getConfig().getPluginNames());

			} else {
				Logger.error(this, "Unable to load '"+pluginToAdd.getText()+"'");
				JOptionPane.showMessageDialog(core.getConfigWindow().getFrame(),
							      "Unable to load plugin '"+pluginToAdd.getText()+"'",
							      "Unable to load plugin",
							      JOptionPane.ERROR_MESSAGE);
			}
		}


		if(e.getSource() == removeButton) {
			if(core.getPluginManager().stopPlugin((String)pluginList.getSelectedValue())
			   && core.getPluginManager().unloadPlugin((String)pluginList.getSelectedValue())) {
				
				core.getConfig().removePlugin((String)pluginList.getSelectedValue());
				pluginList.setListData(core.getConfig().getPluginNames());
			} else {
				Logger.error(this, "Unable to unload '"+pluginToAdd.getText()+"'");
				JOptionPane.showMessageDialog(core.getConfigWindow().getFrame(),
							      "Unable to unload plugin '"+pluginToAdd.getText()+"'",
							      "Unable to unload plugin",
							      JOptionPane.ERROR_MESSAGE);
			}
		}
	}
}
