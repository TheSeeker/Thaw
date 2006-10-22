package thaw.fcp;

import java.util.Hashtable;
import java.util.Enumeration;

import thaw.core.Logger;

/**
 * This class is a generic class, able to handle all kind of FCPMessage.
 * Raw data are NOT stored inside. You *have* to handle them by yourself
 * (FCPConnection.read() / FCPConnection.write())
 * after reading / writing a message with this class.
 */
public class FCPMessage {

	private String messageName = null;
	private Hashtable fields = null; /* String (field) -> String (value) ; See http://wiki.freenetproject.org/FreenetFCPSpec2Point0 */
	private long dataWaiting = 0;


	public FCPMessage() {
		this.fields = new Hashtable();
	}

	/**
	 * As you can't fetch the value returns by loadFromRawMessage(), this constructor is not recommanded.
	 */
	public FCPMessage(String rawMessage) {
		this();
		this.loadFromRawMessage(rawMessage);
	}


	/**
	 * Raw message does not need to finish by "EndMessage" / "Data".
	 */
	public boolean loadFromRawMessage(String rawMessage) {
		int i;

		String[] lines = rawMessage.split("\n");

		for(i = 0 ; "".equals( lines[i] );) {
			i++;
		}
			

		this.setMessageName(lines[i]);
		Logger.info(this, "Message (Node >> Thaw): "+lines[i]);

		
		for(i++; i < lines.length ; i++) {
			/* Empty lines are ignored. */
			/* Line not containing '=' (like "Data" or "EndMessage") are ignored */
			if("".equals( lines[i] ) || !(lines[i].indexOf("=") >= 0))
				continue;

			String[] affectation = lines[i].split("=");
			
			if(affectation.length < 2) {
				Logger.warning(this, "Malformed message");
				continue;
			}

			this.setValue(affectation[0], affectation[1]);
		}

		if("ProtocolError".equals( this.getMessageName() )) {
			Logger.notice(this, "PROTOCOL ERROR:"+this.toString());
		}

		return true;
	}


	public String getMessageName() {
		return this.messageName;
	}

	public void setMessageName(String name) {	
		if(name == null || "".equals( name )) {
			Logger.notice(this, "Setting name to empty ? weird");
		}
		
		if(name.indexOf("\n")>=0) {
			Logger.notice(this, "Name shouldn't contain '\n'");
		}

		this.messageName = name;
	}

	public String getValue(String field) {
		return ((String)this.fields.get(field));
	}

	public void setValue(String field, String value) {
		if("DataLength".equals( field )) {
			this.setAmountOfDataWaiting((new Long(value)).longValue());
		}

		if(value == null) {
			this.fields.remove(field);
			return;
		}

		this.fields.put(field, value);
	}

	
	/**
	 * Returns the amount of data waiting on socket (in octets).
	 * @return if > 0 : Data are still waiting (except if the message name is "PersistentPut" !), if == 0 : No data waiting, if < 0 : These data are now unavailable.
	 */
	public long getAmountOfDataWaiting() {
		return this.dataWaiting;
	}

	
	public void setAmountOfDataWaiting(long amount) {
		if(amount == 0) {
			Logger.warning(this, "Setting amount of data waiting to 0 ?! Abnormal !");
		}

		this.dataWaiting = amount;
	}


	/**
	 * Generate FCP String to send.
	 * If amount of data waiting is set to > 0, then, a field "DataLength" is added,
	 * and resulting string finish by "Data", else resulting string simply finish by "EndMessage".
	 */
	public String toString() {
		String result = "";

		Logger.info(this, "Message (Node << Thaw): "+this.getMessageName());

		result = result + this.getMessageName() + "\n";

		for(Enumeration fieldNames = this.fields.keys() ; fieldNames.hasMoreElements();) {
			String fieldName = ((String)fieldNames.nextElement());

			result = result + fieldName + "=" + this.getValue(fieldName) + "\n";
		}

		if(this.getAmountOfDataWaiting() == 0)
			result = result + "EndMessage\n";
		else {
			result = result + "DataLength="+ (new Long(this.getAmountOfDataWaiting())).toString() + "\n";
			result = result + "Data\n";
		}

		return result;
	}
}
