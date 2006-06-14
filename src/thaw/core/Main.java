package thaw.core;

/**
 * Main class. Only used to display some informations and init the core.
 *
 * @author <a href="mailto:jflesch@nerim.net">Jerome Flesch</a>
 */
public class Main {

	public final static String VERSION="0.1 WIP";
	

	/**
	 * Used to start the program
	 *
	 * @param args Arguments given to the program.
	 */
	public static void main(String[] args) {
		Core core;

		core = new Core();
		core.initAll();
	}
}

