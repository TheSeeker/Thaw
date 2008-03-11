package thaw.core;

import java.util.Locale;

import javax.swing.UIManager;
import java.util.Vector;
import java.util.Iterator;

import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;



/**
 * Main class. Only used to display some informations and init the core.
 *
 * @author <a href="mailto:jflesch@gmail.com">Jerome Flesch</a>
 */
public class Main {

	public final static int
	_major  = 0,
	_minor  = 8,
	_update = 4;
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


	private Main() {

	}


	/**
	 * Used to start the program
	 *
	 * @param args "-?", "-help", "--help", "/?", "/help", "-lf lookandfeel"
	 */
	public static void main(final String[] args) {
		Core core;

		Main.extractDeps();

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


	/**
	 * need a non-static context
	 */
	public void extractFileFromJar(String src, String dst) {
		try {
			String realHome = this.getClass().getProtectionDomain().
				getCodeSource().getLocation().toString();

			String home = java.net.URLDecoder.decode(realHome.substring(5), "UTF-8");

			Logger.info(this, "Extracting : "+realHome+" ; "+src+" ; "+dst);

			ZipFile jar = new ZipFile(home);
			ZipEntry entry = jar.getEntry(src);

			File jarFile = new File(dst);


			InputStream in = new BufferedInputStream(jar.getInputStream(entry));
			OutputStream out = new BufferedOutputStream(new FileOutputStream(jarFile));

			byte[] buffer = new byte[2048];

			int nBytes;

			while( (nBytes = in.read(buffer)) > 0) {
				out.write(buffer, 0, nBytes);
			}

			out.flush();
			out.close();
			in.close();

			return;
		} catch(java.io.IOException e) {
			Logger.warning(this, "Can't extract '"+src+"' because : "+e.toString());
			if (e.getCause() != null)
				Logger.warning(this, "Cause : "+e.getCause().toString());
			e.printStackTrace();
		}

		Logger.warning(this, "Will try to continue anyway");
		//System.exit(1);
	}


	public final static String[] DEPS = new String[] {
		"jmdns.jar",
		"hsqldb.jar",
		"BouncyCastle.jar"
	};

	public static void extractDeps() {
		Main main = new Main();

		/* we erase each time the files to be sure that they are always up to date */
		for (int i = 0 ; i < DEPS.length ; i++) {
			main.extractFileFromJar("lib/"+DEPS[i], DEPS[i]);
		}
	}

}

