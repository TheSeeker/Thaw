package thaw.plugins.miniFrost.interfaces;

import thaw.plugins.Hsqldb;
import thaw.fcp.FCPQueueManager;


public interface Attachment {

	public String getType();
	public String getPrintableType();

	public String[] getProperties();

	/**
	 * @return an array with the same size than the one of getProperties()
	 */
	public String[] getValues();

	public String getValue(String property);
	public void   setValue(String property, String value);

	/**
	 * Provides the XML tag name containing the properties
	 * @return null if none
	 */
	public String getContainer();

	/**
	 * Name to display
	 * (the exact display will be: "[getPrintableType()]: [toString()]")
	 */
	public String toString();

	public String[] getActions();
	public void apply(Hsqldb db, FCPQueueManager queueManager, String action);
}
