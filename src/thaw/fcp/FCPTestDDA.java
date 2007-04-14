package thaw.fcp;

import java.util.Observable;
import java.util.Observer;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;



import thaw.core.Logger;


/**
 * FCPClientGet do it automagically when using DDA, so you shouldn't have to bother about it
 */
public class FCPTestDDA extends Observable implements FCPQuery, Observer {
	private String dir;
	private boolean wantRead;
	private boolean wantWrite;

	private String testFile;

	private boolean nodeCanRead;
	private boolean nodeCanWrite;

	private FCPQueueManager queueManager;


	public FCPTestDDA(String directory,
			  boolean wantTheNodeToRead,
			  boolean wantTheNodeToWrite) {
		this.dir       = directory;
		this.wantRead  = wantTheNodeToRead;
		this.wantWrite = wantTheNodeToWrite;
	}


	public boolean start(FCPQueueManager queueManager) {
		this.queueManager = queueManager;

		FCPMessage msg = new FCPMessage();
		msg.setMessageName("TestDDARequest");
		msg.setValue("Directory", dir);
		msg.setValue("WantReadDirectory", Boolean.toString(wantRead));
		msg.setValue("WantWriteDirectory", Boolean.toString(wantWrite));

		queueManager.getQueryManager().addObserver(this);

		return queueManager.getQueryManager().writeMessage(msg);
	}

	public boolean stop(FCPQueueManager queueManager) {
		/* Red Hot Chili Peppers - Can't stop */
		return false;
	}


	protected boolean writeFile(String filename, String content) {
		try {
			FileOutputStream stream = new FileOutputStream(filename, false);

			stream.write(content.getBytes());
			stream.close();

			return true;

		} catch(java.io.FileNotFoundException e) {

			Logger.warning(this, "Unable to write file: "+e.toString());
			return false;

		} catch(java.io.IOException e) {

			Logger.warning(this, "Unable to write file: "+e.toString());
			return false;
		}
	}



	protected String readFile(String filename) {
		String data = null;
		try {
			FileInputStream stream = new FileInputStream(filename);
			DataInputStream dis = new DataInputStream(stream);
			
			data = dis.readUTF();
			
			dis.close();
			stream.close();

		} catch(java.io.FileNotFoundException e) {
			Logger.warning(this, "Unable to read file : "+e.toString());
			return null;
		} catch(java.io.IOException e) {
			Logger.warning(this, "Unable to read file : "+e.toString());
			return null;
		}

		return data;
	}


	protected boolean deleteFile(String filename) {
		return (new File(filename)).delete();
	}


	public void update(Observable o, Object param) {
		if (param == null || !(param instanceof FCPMessage))
			return;

		FCPMessage msg = (FCPMessage)param;

		/* TOREMOVE when all the node will be up-to-date */
		if ("ProtocolError".equals(msg.getMessageName())) {
			if ("7".equals(msg.getValue("Code"))) {
				Logger.warning(this, "Node doesn't support TestDDA (-> ProtocolError) => DDA desactivated");

				queueManager.getQueryManager().getConnection().setLocalSocket(false);

				nodeCanRead = false;
				nodeCanWrite = false;
				setChanged();
				notifyObservers();

				return;
			}
		}


		if (!dir.equals(msg.getValue("Directory"))) {
			/* not for us */
			return;
		}


		if ("TestDDAReply".equals(msg.getMessageName())) {
			FCPMessage answer = new FCPMessage();
			answer.setMessageName("TestDDAResponse");

			answer.setValue("Directory", dir);

			if (wantWrite) {
				testFile = msg.getValue("WriteFilename");
				writeFile(testFile, msg.getValue("ContentToWrite"));
			}

			if (wantRead) {
				String data = readFile(msg.getValue("ReadFilename"));

				if (data == null) {
					Logger.error(this, "Thaw can't read the file written by the node !");
				}

				answer.setValue("ReadContent", data != null ? data : "bleh");
			}

			queueManager.getQueryManager().writeMessage(answer);
		}




		if ("TestDDAComplete".equals(msg.getMessageName())) {
			nodeCanRead = false;
			nodeCanWrite = false;

			if (wantRead)
				nodeCanRead = Boolean.valueOf(msg.getValue("ReadDirectoryAllowed")).booleanValue();
			if (wantWrite)
				nodeCanWrite = Boolean.valueOf(msg.getValue("WriteDirectoryAllowed")).booleanValue();

			Logger.info(this,
				    "TestDDA : R : " +Boolean.toString(wantRead)
				    + " ; W : "+Boolean.toString(wantWrite));

			if (wantWrite)
				deleteFile(testFile);

			queueManager.getQueryManager().deleteObserver(this);

			setChanged();
			notifyObservers();
		}
	}


	public boolean mayTheNodeRead() {
		return nodeCanRead;
	}

	public boolean mayTheNodeWrite() {
		return nodeCanWrite;
	}


	public int getQueryType() {
		return 0;
	}
}
