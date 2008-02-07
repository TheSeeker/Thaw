package thaw.plugins;

import javax.swing.ImageIcon;

import thaw.core.Core;
import thaw.core.I18n;
import thaw.core.Plugin;

import thaw.plugins.nodeConfigurator.*;

public class NodeConfigurator implements Plugin {
	private Core core;
	private NodeConfiguratorTab configTab;
	
	public NodeConfigurator() { }

	public ImageIcon getIcon() {
		return thaw.gui.IconBox.settings;
	}

	public String getNameForUser() {
		return thaw.core.I18n.getMessage("thaw.plugin.nodeConfig");
	}

	public boolean run(Core core) {
		this.core = core;
		
		core.getConfig().addListener("advancedMode", this);
		
		boolean advanced = Boolean.valueOf(core.getConfig().getValue("advancedMode")).booleanValue();
		
		configTab = new NodeConfiguratorTab(advanced, core.getQueueManager());
		
		core.getConfigWindow().addTab(I18n.getMessage("thaw.plugin.nodeConfig"),
			      thaw.gui.IconBox.minSettings,
			      configTab.getPanel());
		
		configTab.refresh();
		
		return true;
	}

	public void stop() {
		if (configTab != null) {
			core.getConfigWindow().removeTab(configTab.getPanel());
			configTab = null;
		}
	}

}
