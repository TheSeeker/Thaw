package thaw.core;

import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import java.util.Locale;


/**
 * Main class. Only used to display some informations and init the core.
 *
 * @author <a href="mailto:jflesch@nerim.net">Jerome Flesch</a>
 */
public class Main {

	public final static String VERSION;

	static{
		char	_major = 0,
			_minor = 6;
		String	_svnBuildNumber = "@custom@";
		VERSION = _major + '.' + _minor + " WIP r"+_svnBuildNumber;
	}

	/**
	 * Look &amp; feel use by GUI front end
	 */
	private static String lookAndFeel = null;


	/**
	 * Locale (null = default)
	 */
	private static String locale = null;


	/**
	 * Used to start the program
	 *
 	 * @param args "-?", "-help", "--help", "/?", "/help", "-lf lookandfeel"
	 */
	public static void main(String[] args) {
		Core core;

		parseCommandLine(args);

		if(locale != null)
			I18n.setLocale(new Locale(locale));

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
				if ("-?".equals( args[count] ) || "-help".equals( args[count] )
						|| "--help".equals( args[count] )
						|| "/?".equals( args[count] )
						|| "/help".equals( args[count] )) {
					showHelp();
					count++;
				} else if ("-lf".equals( args[count] )) {
					lookAndFeel = args[count + 1];
					count = count + 2;
				} else if ("-lc".equals( args[count] )) {
					locale = args[count + 1];
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

		System.out.println("\n-lc     Sets the locale to use: 'en' for english,");
		System.out.println("        'fr' for french, etc.");
		System.out.println("        see http://ftp.ics.uci.edu/pub/ietf/http/related/iso639.txt");
		System.out.println("        for the complete list");

		System.exit(0);

	}






}

