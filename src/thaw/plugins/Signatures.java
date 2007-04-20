package thaw.plugins;


import thaw.core.I18n;
import thaw.core.Core;
import thaw.core.PluginManager;

import thaw.gui.IconBox;


public class Signatures implements thaw.core.Plugin {
	public Signatures() {

	}


	public boolean run(Core core) {
		return true;
	}

	public boolean stop() {
		return true;
	}

	public String getNameForUser() {
		return I18n.getMessage("thaw.plugin.signature.pluginName");
	}

	public javax.swing.ImageIcon getIcon() {
		return IconBox.identity;
	}

}
