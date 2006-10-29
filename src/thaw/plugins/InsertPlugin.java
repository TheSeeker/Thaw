package thaw.plugins;

import javax.swing.JScrollPane;
import java.io.File;

import thaw.core.*;
import thaw.plugins.insertPlugin.*;
import thaw.fcp.*;

public class InsertPlugin implements thaw.core.Plugin {
	private Core core;

	private InsertPanel insertPanel;
	private JScrollPane scrollPane;

	public InsertPlugin() {

	}


	public boolean run(Core core) {
		this.core = core;

		Logger.info(this, "Starting plugin \"InsertPlugin\" ...");

		this.insertPanel = new InsertPanel(this,
					      Boolean.valueOf(core.getConfig().getValue("advancedMode")).booleanValue());

		this.scrollPane = new JScrollPane(this.insertPanel.getPanel());

		core.getMainWindow().addTab(I18n.getMessage("thaw.common.insertion"),
					    IconBox.minInsertions,
					    this.scrollPane);

		return true;
	}


	public boolean stop() {
		Logger.info(this, "Stopping plugin \"InsertPlugin\" ...");

		this.core.getMainWindow().removeTab(this.scrollPane);

		return true;
	}


	public String getNameForUser() {
		return I18n.getMessage("thaw.common.insertion");
	}


	/**
	 * Note: public key is found from private one.
	 * @param fileList File list, separated by ';'
	 * @param keyType 0 = CHK ; 1 = KSK ; 2 = SSK
	 * @param rev  ignored if key == CHK
	 * @param name ignored if key == CHK
	 * @param privateKey ignored if key == CHK/KSK ; can be null if it has to be generated
	 * @param persistence 0 = Forever ; 1 = Until node reboot ; 2 = Until the app disconnect
	 * @param mimeType null = autodetect
	 */
	public boolean insertFile(String fileList, int keyType,
				  int rev, String name,
				  String privateKey,
				  int priority, boolean global,
				  int persistence, String mimeType) {

		FCPClientPut clientPut = null;
		String[] files = fileList.split(";");

		if(keyType > 0 && files.length > 1) {
			new WarningWindow(this.core, "Can't insert multiple SSH@ / KSK@ files at the same time. Use jSite.");
			return false;
		}

		for(int i = 0 ; i < files.length ; i++) {

			if(privateKey != null && !"".equals( privateKey )) {
				clientPut = new FCPClientPut(new File(files[i]), keyType, rev, name,
							     "USK@"+privateKey+"/", priority,
							     global, persistence);
			} else {
				clientPut = new FCPClientPut(new File(files[i]), keyType, rev, name,
							     null, priority,
							     global, persistence);
			}

			if(mimeType != null) {
				Logger.notice(this, "Mime type forced to "+mimeType);
				clientPut.setMetadata("ContentType", mimeType);
			}

			this.insertPanel.setLastInserted(clientPut);
			clientPut.addObserver(this.insertPanel);

			this.core.getQueueManager().addQueryToThePendingQueue(clientPut);

		}

		return true;
	}

}
