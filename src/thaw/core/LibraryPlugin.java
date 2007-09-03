package thaw.core;

import thaw.core.Logger;

import java.util.Vector;
import java.util.Iterator;


/**
 * Plugins adding functionality for other plugins should extends this class.
 * Then plugins using these library plugins will be able to register them one by one.
 * realStart() is called when the first plugin has registered itself.
 * realStop() is called when the last plugin has unregistered itself.
 */
public abstract class LibraryPlugin implements Plugin {
	private Vector registered = null;

	public abstract boolean run(Core core);
	public abstract boolean stop();


	public void registerChild(final Plugin child) {
		if (registered == null)
			registered = new Vector();

		if (registered.size() == 0)
			realStart();

		while (registered.remove(child)) {
			Logger.warning(this, "Plugin '"+
				       child.getClass().getName()+
				       "' was already registered to '"+
				       this.getClass().getName()+
				       "'");
		}

		registered.add(child);
	}


	public void unregisterChild(final Plugin child) {
		if (registered == null) {
			Logger.warning(this, "Abnormal : '"+this.getClass().getName()+
				       "' is unregistering child plugin '"+
				       child.getClass().getName()+
				       "' but "+
				       "no plugin was registered ?!");
			registered = new Vector();
		}

		while (registered.remove(child)) { }

		if(registered.size() == 0) {
			realStop();
		} else {
			Logger.debug(this, "unregisterChild() : Children still registered to "+
				     "'"+this.getClass().getName()+"' : ");
			for (Iterator it = registered.iterator();
			     it.hasNext();) {
				Logger.debug(this, it.next().getClass().getName());
			}
		}
	}

	public abstract void realStart();

	public abstract void realStop();

}
