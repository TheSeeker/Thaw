package thaw.plugins;

import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import java.io.File;

import thaw.core.*;
import thaw.i18n.I18n;
import thaw.plugins.insertPlugin.*;
import thaw.fcp.*;

public class InsertPlugin implements thaw.core.Plugin {
	private Core core;

	private InsertPanel insertPanel;

	public InsertPlugin() {

	}


	public boolean run(Core core) {
		this.core = core;
		
		Logger.info(this, "Starting plugin \"InsertPlugin\" ...");

		insertPanel = new InsertPanel(this,
					      Boolean.valueOf(core.getConfig().getValue("advancedMode")).booleanValue());

		core.getMainWindow().addTab(I18n.getMessage("thaw.common.insertion"), insertPanel.getPanel());

		return true;
	}


	public boolean stop() {
		Logger.info(this, "Stopping plugin \"InsertPlugin\" ...");

		core.getMainWindow().removeTab(insertPanel.getPanel());

		return true;
	}


	public String getNameForUser() {
		return I18n.getMessage("thaw.common.insertion");
	}


	/**
	 * Note: public key is found from private one.
	 * @param keyType : 0 = CHK ; 1 = KSK ; 2 = SSK
	 * @param rev  : ignored if key == CHK
	 * @param name : ignored if key == CHK
	 * @param privateKey : ignored if key == CHK/KSK ; can be null if it has to be generated
	 * @param persistence 0 = Forever ; 1 = Until node reboot ; 2 = Until the app disconnect
	 */
	public boolean insertFile(String fileList, int keyType,
				       int rev, String name,
				       String privateKey,
				       int priority, boolean global,
				       int persistence) {

		FCPClientPut clientPut = null;
		String[] files = fileList.split(";");

		if(keyType > 0 && files.length > 1) {
			new WarningWindow(core, "Can't insert multiple SSH@ / KSK@ files at the same time. Use jSite.");
			return false;
		}

		for(int i = 0 ; i < files.length ; i++) {

			if(privateKey != null && !privateKey.equals("")) {
				clientPut = new FCPClientPut(new File(files[i]), keyType, rev, name,
							     "USK@"+privateKey+"/", priority,
							     global, persistence);
			} else {
				clientPut = new FCPClientPut(new File(files[i]), keyType, rev, name,
							     null, priority,
							     global, persistence);
			}
			
			insertPanel.setLastInserted(clientPut);
			clientPut.addObserver(insertPanel);
			
			core.getQueueManager().addQueryToThePendingQueue(clientPut);

		}
		
		return true;
	}
				       
				       
				       
}
