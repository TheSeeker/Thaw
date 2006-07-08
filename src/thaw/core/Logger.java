package thaw.core;


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
	 * 3 or more is recommanded.
	 * 5 is never logged in a file, only on stdout.
	 */
	private final static int LOG_LEVEL = 5;


	protected static void displayErr(String msg) {
		System.err.println(msg);
	}

	protected static void display(String msg) {
		System.out.println(msg);
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
		if(LOG_LEVEL >= 5)
			System.out.println("[VERBOSE] "+ o.getClass().getName()+": "+msg);
	}

	/**
	 * As it. Similar to verbose()
	 */
	public static void asIt(Object o, String msg) {
		if(LOG_LEVEL >= 5)
			System.out.println(msg);
	}
}
