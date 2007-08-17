package thaw.plugins.miniFrost.frostKSK;

import java.util.Vector;
import org.w3c.dom.*;


import thaw.plugins.Hsqldb;


public abstract class KSKAttachment
	implements thaw.plugins.miniFrost.interfaces.Attachment {

	public abstract String getType();
	public abstract String getPrintableType();

	public abstract String[] getProperties();

	/**
	 * @return an array with the same size than the one of getProperties()
	 */
	public abstract String[] getValues();

	public abstract String getValue(String property);
	public abstract void   setValue(String property, String value);

	/**
	 * Provides the XML tag name containing the properties
	 * @return null if none
	 */
	public abstract String getContainer();

	/**
	 * Name to display
	 * (the exact display will be: "[getPrintableType()]: [toString()]")
	 */
	public abstract String toString();

	public abstract String[] getActions();
	public abstract void apply(Hsqldb db, thaw.fcp.FCPQueueManager queueManager,
				   String action);

	public abstract void insert(Hsqldb db, int messageId);
	public abstract StringBuffer getSignedStr();

	/**
	 * if isReady() return false, the draft will wait() on this attachment.
	 * so don't forget to notify it !
	 */
	public abstract boolean isReady();

	/* why ? nobody knows. */
	public final static String[] CDATA_EXCEPTIONS = new String[] {
		"key", "size"
	};


	private boolean isCDATAException(String name) {
		for (int j = 0; j < CDATA_EXCEPTIONS.length ; j++)
			if (CDATA_EXCEPTIONS[j].equals(name))
				return true;

		return false;
	}


	public Element getXML(org.w3c.dom.Document doc) {
		Element root = doc.createElement("Attachment");
		root.setAttribute("type", getType());

		Element subRoot;

		if (getContainer() != null)
			/* why ? nobody knows. */
			subRoot = doc.createElement(getContainer());
		else
			subRoot = root;

		String[] properties = getProperties();
		String[] values = getValues();

		for (int i = 0 ; i < properties.length ; i++) {
			Element el = doc.createElement(properties[i]);

			boolean inCdata = isCDATAException(properties[i]);

			if (inCdata) {
				CDATASection cdata = doc.createCDATASection(values[i]);
				el.appendChild(cdata);
			} else {
				Text txt = doc.createTextNode(values[i]);
				el.appendChild(txt);
			}

			subRoot.appendChild(el);
		}


		if (subRoot != root)
			root.appendChild(subRoot);

		return root;
	}
}
