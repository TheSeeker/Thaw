package thaw.core;

import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

/**
 * Main class. Only used to display some informations and init the core.
 *
 * @author <a href="mailto:jflesch@nerim.net">Jerome Flesch</a>
 */
public class Main {

	public final static String VERSION="0.1 Beta";

	/**
	 * Look & feel use by GUI front end
	 */
	private static String lookAndFeel = null;


	/**
	 * Used to start the program
	 *
 	 * @param args "-?", "-help", "--help", "/?", "/help", "-lf lookandfeel"
	 */
	public static void main(String[] args) {
		Core core;

		parseCommandLine(args);

		core = new Core();
		Core.setLookAndFeel(lookAndFeel);
		core.initAll();
	}




	/**
	 * This method parses the command line arguments
	 * 
	 * @param args the arguments
	 */
	private static void parseCommandLine(String[] args) {

		int count = 0;

		try {
			while (args.length > count) {
				if (args[count].equals("-?") || args[count].equals("-help")
						|| args[count].equals("--help")
						|| args[count].equals("/?")
						|| args[count].equals("/help")) {
					showHelp();
					count++;
				} else if (args[count].equals("-lf")) {
					lookAndFeel = args[count + 1];
					count = count + 2;
				} else {
					showHelp();
				}
			}
		} catch (ArrayIndexOutOfBoundsException exception) {
			showHelp();
		}

	}

	/**
	 * This method shows a help message on the standard output and exits the
	 * program.
	 */
	private static void showHelp() {

		System.out.println("java -jar thaw.jar [-lf lookAndFeel]\n");
		System.out.println("-lf     Sets the 'Look and Feel' will use.");
		System.out.println("        (overriden by the skins preferences)\n");
		System.out.println("        These ones are currently available:");
		LookAndFeelInfo[] feels = UIManager.getInstalledLookAndFeels();
		for (int i = 0; i < feels.length; i++) {
			System.out.println("           " + feels[i].getClassName());
		}
		System.out.println("\n         And this one is used by default:");
		System.out.println("           " + UIManager.getSystemLookAndFeelClassName() + "\n");

		System.exit(0);

	}






}

