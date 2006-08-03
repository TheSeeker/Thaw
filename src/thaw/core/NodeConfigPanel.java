package thaw.core;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.ActionListener;
import javax.swing.JTextField;
import javax.swing.JLabel;

import java.util.Observer;
import java.util.Observable;

/**
 * NodeConfigPanel. Creates and manages the panel containing all the things to configure
 *  the settings to access the node.
 */
public class NodeConfigPanel implements Observer {
	private Core core;
	private JPanel nodeConfigPanel = null;


	private final static String[] paramNames = { 
		I18n.getMessage("thaw.config.nodeAddress"),
		I18n.getMessage("thaw.config.nodePort"),
		I18n.getMessage("thaw.config.maxSimultaneousDownloads"),
		I18n.getMessage("thaw.config.maxSimultaneousInsertions"),
		I18n.getMessage("thaw.config.maxUploadSpeed"),
		I18n.getMessage("thaw.config.thawId")
	};

	private final static String[] configNames = {
		"nodeAddress",
		"nodePort",
		"maxSimultaneousDownloads",
		"maxSimultaneousInsertions",
		"maxUploadSpeed",
		"thawId"
	};

	private JLabel[] paramLabels = new JLabel[paramNames.length];
	private JTextField[] paramFields = new JTextField[configNames.length];

	

	public NodeConfigPanel(ConfigWindow configWindow, Core core) {
		this.core = core;

		nodeConfigPanel = new JPanel();
		nodeConfigPanel.setLayout(new GridLayout(15, 1));
		
		for(int i=0; i < paramNames.length ; i++) {
			String value;
			
			if( (value = core.getConfig().getValue(configNames[i])) == null)
				value = "";

			paramLabels[i] = new JLabel(paramNames[i]);
			paramFields[i] = new JTextField(value);
			
			nodeConfigPanel.add(paramLabels[i]);
			nodeConfigPanel.add(paramFields[i]);
		}

		setVisibility(Boolean.valueOf(core.getConfig().getValue("advancedMode")).booleanValue());

		configWindow.addObserver(this);
	}

	public JPanel getPanel() {
		return nodeConfigPanel;
	}

	private void setVisibility(boolean advancedMode) {
		for(int i= 2; i < paramNames.length;i++) {
			paramLabels[i].setVisible(advancedMode);
			paramFields[i].setVisible(advancedMode);
		}

	}

	
	public void update(Observable o, Object arg) {
		if(arg == core.getConfigWindow().getOkButton()) {
			for(int i=0;i < paramNames.length;i++) {
				core.getConfig().setValue(configNames[i], paramFields[i].getText());
			}

			setVisibility(Boolean.valueOf(core.getConfig().getValue("advancedMode")).booleanValue());
		}


		if(arg == core.getConfigWindow().getCancelButton()) {
			for(int i=0;i < paramNames.length;i++) {
				String value;

				if( (value = core.getConfig().getValue(configNames[i])) == null)
					value = "";

				paramFields[i].setText(value);
			}
		}
	}

}
