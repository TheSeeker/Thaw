package thaw.core;

import java.util.Iterator;
import java.util.Vector;

/**
 * Manage all log message.
 * @author Jflesch
 */
public class Logger {


	/* 0 = Errors only
	 * 1 = Errors + warnings
	 * 2 = Errors + warnings + notices
	 * 3 = Errors + warnings + notices + infos
	 * 4 = Errors + warnings + notices + infos + debug
	 * 5 = [...] + horrible things that only God could understand easily.
	 *             (or maybe someone having the FCPv2 specs :)
	 *
	 * 2 or more is recommanded.
	 */
	public final static int LOG_LEVEL = 2;

	private static Vector logListeners = null;


	protected static void displayErr(final String msg) {
		System.err.println(msg);
		Logger.notifyLogListeners(msg);
	}

	protected static void display(final String msg) {
		System.out.println(msg);
		Logger.notifyLogListeners(msg);
	}

	/**
	 * Errors.
	 */
	public static void error(final Object o, final String message) {
		Logger.displayErr("[ ERROR ] "+o.getClass().getName()+": "+message);
	}

	/**
	 * Warnings.
	 */
	public static void warning(final Object o, final String message) {
		if(Logger.LOG_LEVEL >= 1)
			Logger.displayErr("[WARNING] "+o.getClass().getName()+": "+message);
	}

	/**
	 * Notices.
	 */
	public static void notice(final Object o, final String msg) {
		if(Logger.LOG_LEVEL >= 2)
			Logger.display("[NOTICE ] " +o.getClass().getName()+": "+msg);
	}


	public static void info(final Object o, final String msg) {
		Logger.info(o, msg, false);
	}

	/**
	 * Infos.
	 */
	public static void info(final Object o, final String msg, final boolean manda) {
		if((Logger.LOG_LEVEL >= 3) || manda)
			Logger.display("[ INFO  ] "+o.getClass().getName()+": "+msg);
	}

	/**
	 * Debug.
	 */
	public static void debug(final Object o, final String msg) {
		if(Logger.LOG_LEVEL >= 4)
			Logger.display("[ DEBUG ] "+o.getClass().getName()+": "+msg);
	}


	/**
	 * Verbose. Too Verbose.
	 */
	public static void verbose(final Object o, final String msg) {
		if(Logger.LOG_LEVEL >= 5) {
			System.out.println("[VERBOSE] "+ o.getClass().getName()+": "+msg);
			Logger.notifyLogListeners(msg);
		}
	}

	/**
	 * As it. Similar to verbose()
	 */
	public static void asIt(final Object o, final String msg) {
		if(Logger.LOG_LEVEL >= 5) {
			System.out.println(msg);
			Logger.notifyLogListeners(msg);
		}
	}





	public static void addLogListener(final LogListener logListener) {
		if(Logger.logListeners == null)
			Logger.logListeners = new Vector();

		Logger.logListeners.add(logListener);
	}

	public static void removeLogListener(final LogListener logListener) {
		if(Logger.logListeners == null)
			return;

		Logger.logListeners.remove(logListener);
	}


	private static void notifyLogListeners(final String line) {
		if(Logger.logListeners == null)
			return;

		for(final Iterator it = Logger.logListeners.iterator();
		    it.hasNext(); ) {
			final LogListener logListener = (LogListener)it.next();

			logListener.newLogLine(line);
		}
	}

}
