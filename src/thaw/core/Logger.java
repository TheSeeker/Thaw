package thaw.core;

import java.util.Vector;
import java.util.Iterator;

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
	 *             (or maybe someone having the FCPv2 doc :)
	 *
	 * 2 or more is recommanded.
	 */
	public final static int LOG_LEVEL = 2;

	private static Vector logListeners = null;
	

	protected static void displayErr(String msg) {
		System.err.println(msg);
		notifyLogListeners(msg);
	}

	protected static void display(String msg) {
		System.out.println(msg);
		notifyLogListeners(msg);
	}

	/**
	 * Errors.
	 */
	public static void error(Object o, String message) {
		displayErr("[ ERROR ] "+o.getClass().getName()+": "+message);
	}

	/**
	 * Warnings.
	 */
	public static void warning(Object o, String message) {
		if(LOG_LEVEL >= 1)
			displayErr("[WARNING] "+o.getClass().getName()+": "+message);
	}

	/**
	 * Notices.
	 */
	public static void notice(Object o, String msg) {
		if(LOG_LEVEL >= 2)
			display("[NOTICE ] " +o.getClass().getName()+": "+msg);
	}
	

	public static void info(Object o, String msg) {
		info(o, msg, false);
	}

	/**
	 * Infos.
	 */
	public static void info(Object o, String msg, boolean manda) {
		if(LOG_LEVEL >= 3 || manda)
			display("[ INFO  ] "+o.getClass().getName()+": "+msg);
	}

	/**
	 * Debug.
	 */
	public static void debug(Object o, String msg) {
		if(LOG_LEVEL >= 4)
			display("[ DEBUG ] "+o.getClass().getName()+": "+msg);
	}


	/** 
	 * Verbose. Too Verbose.
	 */
	public static void verbose(Object o, String msg) {
		if(LOG_LEVEL >= 5) {
			System.out.println("[VERBOSE] "+ o.getClass().getName()+": "+msg);
			notifyLogListeners(msg);
		}
	}

	/**
	 * As it. Similar to verbose()
	 */
	public static void asIt(Object o, String msg) {
		if(LOG_LEVEL >= 5) {
			System.out.println(msg);
			notifyLogListeners(msg);
		}
	}





	public static void addLogListener(LogListener logListener) {
		if(logListeners == null)
			logListeners = new Vector();

		logListeners.add(logListener);
		
	}
	
	public static void removeLogListener(LogListener logListener) {
		if(logListeners == null)
			return;

		logListeners.remove(logListener);
	}
	

	private static void notifyLogListeners(String line) {
		if(logListeners == null)
			return;

		for(Iterator it = logListeners.iterator();
		    it.hasNext(); ) {
			LogListener logListener = (LogListener)it.next();

			logListener.newLogLine(line);			
		}
	}

}
