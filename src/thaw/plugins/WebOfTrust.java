package thaw.plugins;

import javax.swing.ImageIcon;

import thaw.core.Core;
import thaw.core.Plugin;
import thaw.core.I18n;

public class WebOfTrust implements Plugin {
	
	public WebOfTrust() {

	}

	public ImageIcon getIcon() {
		return thaw.gui.IconBox.trust;
	}

	public String getNameForUser() {
		return I18n.getMessage("thaw.plugin.wot");
	}

	public boolean run(Core core) {

		return false;
	}

	public void stop() {

	}

}
