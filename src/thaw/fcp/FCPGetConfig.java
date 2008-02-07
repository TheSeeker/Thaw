package thaw.fcp;

import java.util.Observer;
import java.util.Observable;
import java.util.Hashtable;
import java.util.Enumeration;

public class FCPGetConfig extends Observable implements FCPQuery, Observer {
	private FCPQueueManager queueManager;
	
	private final boolean withCurrent;
	private final boolean withShortDescription;
	private final boolean withLongDescription;
	private final boolean withDefaults;
	private final boolean withSortOrder;
	private final boolean withExpertFlag;
	private final boolean withForceWriteFlag;

	public FCPGetConfig(boolean withCurrent, boolean withShortDescription,
						boolean withLongDescription, boolean withDefaults,
						boolean withSortOrder, boolean withExpertFlag,
						boolean withForceWriteFlag) {
		this.withCurrent = withCurrent;
		this.withShortDescription = withShortDescription;
		this.withLongDescription = withLongDescription;
		this.withDefaults = withDefaults;
		this.withSortOrder = withSortOrder;
		this.withExpertFlag = withExpertFlag;
		this.withForceWriteFlag = withForceWriteFlag;
	}

	public int getQueryType() {
		return 0;
	}

	public boolean start(FCPQueueManager queueManager) {
		this.queueManager = queueManager;
		
		queueManager.getQueryManager().addObserver(this);
		
		FCPMessage msg = new FCPMessage();
		msg.setMessageName("GetConfig");
		
		msg.setValue("WithCurrent", Boolean.toString(withCurrent));
		msg.setValue("WithShortDescription", Boolean.toString(withShortDescription));
		msg.setValue("WithLongDescription", Boolean.toString(withLongDescription));
		msg.setValue("WithDefaults", Boolean.toString(withDefaults));
		msg.setValue("WithSortOrder", Boolean.toString(withSortOrder));
		msg.setValue("WithExpertFlag", Boolean.toString(withExpertFlag));
		msg.setValue("WithForceWriteFlag", Boolean.toString(withForceWriteFlag));
		
		queueManager.getQueryManager().writeMessage(msg);
		
		return true;
	}

	public boolean stop(FCPQueueManager queueManager) {
		queueManager.getQueryManager().deleteObserver(this);
		
		return true;
	}
	
	public class ConfigSetting implements Comparable {
		private final String name;
		private final String shortName;
		private String current;
		private String shortDesc;
		private String longDesc;
		private String defaultValue;
		private int sortOrder;
		private boolean expertFlag;
		private boolean forceWriteFlag;
		
		public ConfigSetting(String name) {
			this.name = name;
			
			int dotPos = name.indexOf('.');
			
			shortName = name.substring(dotPos+1);			
		}
		
		public void setElement(String element, String value) {
			element = element.toLowerCase();
			
			if ("current".equals(element))
				current = value;
			else if ("shortdescription".equals(element))
				shortDesc = value;
			else if ("longdescription".equals(element))
				longDesc = value;
			else if ("default".equals(element))
				defaultValue = value;
			else if ("sortorder".equals(element))
				sortOrder = Integer.parseInt(value);
			else if ("expertflag".equals(element))
				expertFlag = Boolean.valueOf(value).booleanValue();
			else if ("forcewriteflag".equals(element))
				forceWriteFlag = Boolean.valueOf(value).booleanValue();
			else
				thaw.core.Logger.warning(this, "Unknow element '"+element+"' : '"+value+"' !");
		}
		
		public String getName() { return name; }
		public String getCurrent() { return current; }
		public String getShortDesc() { return shortDesc; }
		public String getLongDesc() { return longDesc; }
		public String getDefault() { return defaultValue; }
		public int getSortOrder() { return sortOrder; }
		public boolean getExpertFlag() { return expertFlag; }
		public boolean getForceWriteFlag() { return forceWriteFlag; }

		public int compareTo(Object o) {
			return new Integer(sortOrder).compareTo( new Integer(((ConfigSetting)o).getSortOrder()) );
		}
		
		public String toString() { return shortName; }
	}

	public void update(Observable o, Object param) {
		if (param == null || !(param instanceof FCPMessage))
			return;
		
		FCPMessage msg = (FCPMessage)param;
		
		if (!"ConfigData".equals(msg.getMessageName()))
			return;
		
		Hashtable fields = msg.getValues();
		Hashtable configSettings = new Hashtable();
		
		for (Enumeration keysEnum = fields.keys();
			keysEnum.hasMoreElements(); ) {

			String key = (String)keysEnum.nextElement(); 
			String value = (String)fields.get(key);
			
			int firstPointPos = key.indexOf('.');
			
			String element = key.substring(0, firstPointPos);
			String name = key.substring(firstPointPos+1);
			
			ConfigSetting setting = null;
			
			if ( (setting = (ConfigSetting)configSettings.get(name)) == null) {
				setting = new ConfigSetting(name);
				configSettings.put(name, setting);
			}
			
			setting.setElement(element, value);
		}
		
		stop(queueManager);

		setChanged();
		notifyObservers(configSettings);
	}
}
