package thaw.fcp;

import java.util.Enumeration;
import java.util.Hashtable;

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
		fields = new Hashtable();
	}

	/**
	 * As you can't fetch the value returns by loadFromRawMessage(), this constructor is not recommanded.
	 */
	public FCPMessage(final String rawMessage) {
		this();
		loadFromRawMessage(rawMessage);
	}


	/**
	 * Raw message does not need to finish by "EndMessage" / "Data".
	 */
	public boolean loadFromRawMessage(final String rawMessage) {
		int i;

		final String[] lines = rawMessage.split("\n");

		for(i = 0 ; "".equals( lines[i] );) {
			i++;
		}

		setMessageName(lines[i]);

		for(i++; i < lines.length ; i++) {
			/* Empty lines are ignored. */
			/* Line not containing '=' (like "Data" or "EndMessage") are ignored */
			if("".equals( lines[i] ) || !(lines[i].indexOf("=") >= 0))
				continue;
			
			int equalPos = lines[i].indexOf('=');
			
			String name = lines[i].substring(0, equalPos);
			String value = lines[i].substring(equalPos+1);

			setValue(name, value);
		}


		if("ProtocolError".equals( getMessageName() )
			&& !"25".equals(getValue("Code")) ) /* code 25 == need to test DDA */
			Logger.warning(this, "PROTOCOL ERROR:\n"+toString());
		else if (Logger.getLogLevel() <= 3)
			Logger.info(this, "Message (Node >> Thaw): "+getMessageName());
		else
			Logger.debug(this, "Message (Node >> Thaw): "+toString());


		return true;
	}


	public String getMessageName() {
		return messageName;
	}

	public void setMessageName(final String name) {
		if ((name == null) || "".equals( name )) {
			Logger.notice(this, "Setting name to empty ? weird");
		}

		if (name.indexOf("\n") >= 0) {
			Logger.notice(this, "Name shouldn't contain '\n'");
		}

		messageName = name;
	}

	public String getValue(final String field) {
		return ((String)fields.get(field));
	}

	public Hashtable getValues() {
		return fields;
	}

	public void setValue(final String field, final String value) {
		if("DataLength".equals( field )) {
			setAmountOfDataWaiting((new Long(value)).longValue());
		}

		if(value == null) {
			fields.remove(field);
			return;
		}

		fields.put(field, value);
	}


	/**
	 * Returns the amount of data waiting on socket (in octets).
	 * @return if > 0 : Data are still waiting (except if the message name is "PersistentPut" !), if == 0 : No data waiting, if < 0 : These data are now unavailable.
	 */
	public long getAmountOfDataWaiting() {
		return dataWaiting;
	}


	public void setAmountOfDataWaiting(final long amount) {
		if(amount == 0) {
			Logger.warning(this, "Setting amount of data waiting to 0 ?! Abnormal !");
		}

		dataWaiting = amount;
	}


	/**
	 * Generate FCP String to send.
	 * If amount of data waiting is set to > 0, then, a field "DataLength" is added,
	 * and resulting string finish by "Data", else resulting string simply finish by "EndMessage".
	 */
	public String toString() {
		String result = "";

		Logger.info(this, "Message (Node << Thaw): "+getMessageName());

		result = result + getMessageName() + "\n";

		for(final Enumeration fieldNames = fields.keys() ; fieldNames.hasMoreElements();) {
			final String fieldName = ((String)fieldNames.nextElement());

			result = result + fieldName + "=" + getValue(fieldName) + "\n";
		}

		if(getAmountOfDataWaiting() == 0)
			result = result + "EndMessage\n";
		else {
			result = result + "DataLength="+ (new Long(getAmountOfDataWaiting())).toString() + "\n";
			result = result + "Data\n";
		}

		return result;
	}
}
