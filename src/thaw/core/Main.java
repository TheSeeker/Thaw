package thaw.core;

import java.util.Locale;

import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import java.util.Vector;
import java.util.Iterator;

/**
 * Main class. Only used to display some informations and init the core.
 *
 * @author <a href="mailto:jflesch@nerim.net">Jerome Flesch</a>
 */
public class Main {

	public final static int
	_major  = 0,
	_minor  = 7,
	_update = 0;
	public final static String
	_svnBuildNumber = "@custom@";

	public final static String
	VERSION = Main._major + "." + Main._minor + "." + Main._update + " r"+Main._svnBuildNumber;


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
	public static void main(final String[] args) {
		Core core;

		Main.parseCommandLine(args);

		if(Main.locale != null)
			I18n.setLocale(new Locale(Main.locale));

		core = new Core();

		/* we specify to the core what lnf to use */
		core.setLookAndFeel(Main.lookAndFeel);

		/* and we force it to refresh change it right now */
		if (Main.lookAndFeel != null)
			core.setTheme(Main.lookAndFeel);

		core.initAll();
	}




	/**
	 * This method parses the command line arguments
	 *
	 * @param args the arguments
	 */
	private static void parseCommandLine(final String[] args) {

		int count = 0;

		try {
			while (args.length > count) {
				if ("-?".equals( args[count] ) || "-help".equals( args[count] )
						|| "--help".equals( args[count] )
						|| "/?".equals( args[count] )
						|| "/help".equals( args[count] )) {
					Main.showHelp();
					count++;
				} else if ("-lf".equals( args[count] )) {
					Main.lookAndFeel = args[count + 1];
					count = count + 2;
				} else if ("-lc".equals( args[count] )) {
					Main.locale = args[count + 1];
					count = count + 2;
				} else {
					Main.showHelp();
				}
			}
		} catch (final ArrayIndexOutOfBoundsException exception) {
			Main.showHelp();
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
		Vector feels = thaw.plugins.ThemeSelector.getPossibleThemes();

		for (Iterator it = feels.iterator() ; it.hasNext(); ) {
			String str = (String)it.next();

			System.out.println("           " + str);
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

