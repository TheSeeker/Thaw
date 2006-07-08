package thaw.fcp;

import java.util.Hashtable;
import java.util.Enumeration;

import thaw.core.Logger;

/**
 * This class is a generic class, able to handle all kind of FCPMessage.
 * Raw data are NOT stored inside. You have to handle them by yourself
 * (FCPConnection.getInputStream() / FCPConnection.getOutputStream())
 * after reading / writing a message with this class.
 */
public class FCPMessage {

	private String messageName = null;
	private Hashtable fields = null; /* String (field) -> String (value) ; See http://wiki.freenetproject.org/FreenetFCPSpec2Point0 */
	private long dataWaiting = 0;


	public FCPMessage() {
		fields = new Hashtable();
	}

	/**
	 * As you can't fetch the value returns by loadFromRawMessage(), this constructor is not recommanded.
	 */
	public FCPMessage(String rawMessage) {
		this();
		loadFromRawMessage(rawMessage);
	}


	/**
	 * Raw message does not need to finish by "EndMessage" / "Data".
	 */
	public boolean loadFromRawMessage(String rawMessage) {
		int i;

		String[] lines = rawMessage.split("\n");

		for(i = 0 ; lines[i].equals("");) {
			i++;
		}
			

		setMessageName(lines[i]);

		
		for(i++; i < lines.length ; i++) {
			/* Empty lines are ignored. */
			/* Line not containing '=' (like "Data" or "EndMessage") are ignored */
			if(lines[i].equals("") || !lines[i].contains("="))
				continue;

			String[] affectation = lines[i].split("=");
			
			setValue(affectation[0], affectation[1]);
		}

		return true;
	}


	public String getMessageName() {
		return messageName;
	}

	public void setMessageName(String name) {
		if(name == null || name.equals("")) {
			Logger.notice(this, "Setting name to empty ? weird");
		}
		
		if(name.contains("\n")) {
			Logger.notice(this, "Name shouldn't contain '\n'");
		}

		messageName = name;
	}

	public String getValue(String field) {
		return ((String)fields.get(field));
	}

	public void setValue(String field, String value) {
		fields.put(field, value);
	}

	
	/**
	 * Returns the amount of data waiting on socket (in octets).
	 * @return if > 0 : Data are still waiting, if == 0 : No data waiting, if < 0 : These data are now unavailable.
	 */
	public long getAmountOfDataWaiting() {
		return dataWaiting;
	}

	
	public void setAmountOfDataWaiting(long amount) {
		this.dataWaiting = amount;
	}


	/**
	 * Generate FCP String to send.
	 * If amount of data waiting is set to > 0, then, a field "DataLength" is added,
	 * and resulting string finish by "Data", else resulting string simply finish by "EndMessage".
	 */
	public String toString() {
		String result = "";

		result = result + getMessageName() + "\n";

		for(Enumeration fieldNames = fields.keys() ; fieldNames.hasMoreElements();) {
			String fieldName = ((String)fieldNames.nextElement());

			result = result + fieldName + "=" + getValue(fieldName) + "\n";
		}

		if(getAmountOfDataWaiting() == 0)
			result = result + "EndMessage\n";
		else {
			result = result + "DataLength="+ (new Long(getAmountOfDataWaiting())).toString();
			result = result + "Data\n";
		}

		return result;
	}
}
