package thaw.core;

import java.util.Vector;
import java.util.Iterator;


public class ThawThread extends Thread {
	private static ThreadGroup threadGroup = new ThreadGroup("Thaw");
	private static Vector threads = new Vector();
	private static boolean allowFullStop = false;

	private Object parent;
	private ThawRunnable target;
	private String name;


	public ThawThread(ThawRunnable target, String name) {
		this(target, name, null);
	}

	public ThawThread(ThawRunnable target, String name, Object parent) {
		super(threadGroup, name);

		this.target = target;
		this.name = name;
		this.parent = parent;
	}

	public void run() {
		Logger.info(this, "Starting thread '"+name+"' ...");

		synchronized(threads) {
			threads.add(this);
		}

		target.run();

		synchronized(threads) {
			threads.remove(this);
		}

		Logger.info(this, "Thread '"+name+"' finished");

		if (threads.size() == 0) {
			Logger.notice(this, "All threads are stopped");

			if (allowFullStop) {
				Logger.notice(this, "Halting Thaw");
				System.exit(0);
			}
		}
	}

	public ThawRunnable getTarget() {
		return target;
	}

	public Object getParent() {
		return parent;
	}


	public static void setAllowFullStop(boolean a) {
		allowFullStop = a;

		synchronized(threads) {
			if (allowFullStop) {
				if (threads.size() == 0) {
					Logger.notice(null, "All threads are stopped => Halting Thaw");
					System.exit(0);
				}
			}
		}
	}


	public static void listThreads() {
		synchronized(threads) {
			Logger.info(null,
				    Integer.toString(threadGroup.activeCount())+" threads "+
				    "("+Integer.toString(threads.size())+" known)");

			for (Iterator it = threads.iterator();
			     it.hasNext();) {
				ThawThread th = (ThawThread)it.next();

				if (th != null) {
					Logger.info(null,
						    "'"+th.getName()+"' "+
						    "(parent: '"+th.getParent().getClass().getName()+"')");
				}
			}
		}
	}

	public static void stopAll() {
		synchronized(threads) {
			for (Iterator it = threads.iterator();
			     it.hasNext();) {
				ThawThread th = (ThawThread)it.next();
				th.getTarget().stop();
			}
		}
	}
}
