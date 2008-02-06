package thaw.core;

import java.util.Iterator;
import java.util.Vector;

/**
 * Manage all log message.
 * @author Jflesch
 */
public class Logger {

	public final static int LOG_LEVEL_ERROR   = 0;
	public final static int LOG_LEVEL_WARNING = 1;
	public final static int LOG_LEVEL_NOTICE  = 2;
	public final static int LOG_LEVEL_INFO    = 3;
	public final static int LOG_LEVEL_DEBUG   = 4;
	public final static int LOG_LEVEL_VERBOSE = 5;

	private static int LOG_LEVEL = 2;


	public final static String[] PREFIXES = new String[] {
		"[ ERROR ]",
		"[WARNING]",
		"[NOTICE ]",
		"[ INFO  ]",
		"[ DEBUG ]",
		"[VERBOSE]"
	};


	/**
	 * 0 = Errors only
	 * 1 = Errors + warnings
	 * 2 = Errors + warnings + notices
	 * 3 = Errors + warnings + notices + infos
	 * 4 = Errors + warnings + notices + infos + debug
	 * 5 = [...] + horrible things that only God could understand easily.
	 *             (or maybe someone with the FCPv2 specs :)
	 *
	 * 2 or more is recommended.
	 * 4 or more is unhealthy
	 */
	public static void setLogLevel(int logLevel) {
		Logger.notice(null, "Setting verbosity to "+Integer.toString(logLevel));
		LOG_LEVEL = logLevel;
	}

	public static int getLogLevel() {
		return LOG_LEVEL;
	}

	private static Vector logListeners = null;


	private static void displayErr(final String msg) {
		System.err.println(msg);
	}

	private static void display(final String msg) {
		System.out.println(msg);
	}

	private static void log(final int level, final Object o, final String msg) {
		log(level, o, msg, false);
	}

	private static void log(final int level, final Object o, final String msg,
							final boolean manda) {
		if (Logger.LOG_LEVEL < level && !manda)
			return;

		String str = ((o != null) ? o.getClass().getName()+": " : "");

		if (level <= 1)
			displayErr(PREFIXES[level]+" "+str+msg);
		else
			display(PREFIXES[level]+" "+str+msg);

		notify(level, o, msg);
	}


	/**
	 * Errors.
	 * A process ended because of it.
	 */
	public static void error(final Object o, final String message) {
		log(0, o, message);
	}

	/**
	 * Warnings.
	 * Some informations will probably be / are probably missing.
	 * Or: Can't do something, but it's normal.
	 */
	public static void warning(final Object o, final String message) {
		log(1, o, message);
	}

	/**
	 * Notices.
	 * Strange event, but probably not unusual.
	 * Or: Normal event, but who can create troubles.
	 */
	public static void notice(final Object o, final String msg) {
		log(2, o, msg);
	}


	/**
	 * Infos.
	 * Normal process.
	 */
	public static void info(final Object o, final String msg) {
		log(3, o, msg);
	}

	/**
	 * Infos.
	 * @param manda force the display of these informations
	 */
	public static void info(final Object o, final String msg, final boolean manda) {
		log(3, o, msg, manda);
	}

	/**
	 * Debug.
	 * Details about a normal process.
	 */
	public static void debug(final Object o, final String msg) {
		log(4, o, msg);
	}


	/**
	 * Verbose. Too Verbose.
	 * Details, a LOT of details.
	 */
	public static void verbose(final Object o, final String msg) {
		log(5, o, msg);
	}




	public static void addLogListener(final LogListener logListener) {
		if(Logger.logListeners == null)
			Logger.logListeners = new Vector();

		synchronized(logListeners) {
			Logger.logListeners.add(logListener);
		}
	}

	public static void removeLogListener(final LogListener logListener) {
		if(Logger.logListeners == null)
			return;

		synchronized(logListener) {
			Logger.logListeners.remove(logListener);

			if (logListeners.size() == 0)
				logListeners = null;
		}
	}


	/**
	 * notify the observers if there is.
	 */
	private static void notify(final int level, final Object src, final String line) {
		if(Logger.logListeners == null)
			return;

		synchronized(logListeners) {
			for(final Iterator it = Logger.logListeners.iterator();
			    it.hasNext(); ) {
				final LogListener logListener = (LogListener)it.next();

				logListener.newLogLine(level, src, line);
			}
		}
	}

}
