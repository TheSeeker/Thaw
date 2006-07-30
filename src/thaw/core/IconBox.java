package thaw.core;

import javax.swing.ImageIcon;

/**
 * This class is simply an helper to find and load quickly some common icons.
 */
public class IconBox {

	/**
	 * Freenet logo :)
	 */
	public static ImageIcon blueBunny;

	public static ImageIcon connectAction; 
	public static ImageIcon minConnectAction; 

	public static ImageIcon disconnectAction;

	public static ImageIcon queue;
	public static ImageIcon minQueue;

	public static ImageIcon insertions;
	public static ImageIcon minInsertions;

	public static ImageIcon downloads;
	public static ImageIcon minDownloads;

	public static ImageIcon settings;
	public static ImageIcon minSettings;

	public static ImageIcon reconnectAction;
	public static ImageIcon minReconnectAction;

	public static ImageIcon quitAction;
	public static ImageIcon minQuitAction;

	public static ImageIcon search;

	/**
	 * Not really used
	 */
	public IconBox() {

	}

	/**
	 * no warranty
	 */
	public static void loadIcons() {
		try {

			blueBunny = 
				new ImageIcon((new IconBox()).getClass().getClassLoader().getResource("blueBunny.png"));


			connectAction = 
				new ImageIcon((new IconBox()).getClass().getClassLoader().getResource("go-jump.png"));
			minConnectAction =
				new ImageIcon((new IconBox()).getClass().getClassLoader().getResource("min-go-jump.png"));


			disconnectAction = 
				new ImageIcon((new IconBox()).getClass().getClassLoader().getResource("process-stop.png"));

			queue =
				new ImageIcon((new IconBox()).getClass().getClassLoader().getResource("system-search.png"));
			minQueue = 
				new ImageIcon((new IconBox()).getClass().getClassLoader().getResource("min-system-search.png"));


			insertions = 
				new ImageIcon((new IconBox()).getClass().getClassLoader().getResource("go-next.png"));
			minInsertions =
				new ImageIcon((new IconBox()).getClass().getClassLoader().getResource("min-go-next.png"));


			downloads =
				new ImageIcon((new IconBox()).getClass().getClassLoader().getResource("go-first.png"));
			minDownloads =
				new ImageIcon((new IconBox()).getClass().getClassLoader().getResource("min-go-first.png"));

			settings =
				new ImageIcon((new IconBox()).getClass().getClassLoader().getResource("preferences-system.png"));
			minSettings =
				new ImageIcon((new IconBox()).getClass().getClassLoader().getResource("min-preferences-system.png"));


			reconnectAction =
				new ImageIcon((new IconBox()).getClass().getClassLoader().getResource("view-refresh.png"));
			minReconnectAction =
				new ImageIcon((new IconBox()).getClass().getClassLoader().getResource("min-view-refresh.png"));

			quitAction =
				new ImageIcon((new IconBox()).getClass().getClassLoader().getResource("system-log-out.png"));
			minQuitAction =
				new ImageIcon((new IconBox()).getClass().getClassLoader().getResource("min-system-log-out.png"));

			search =
				new ImageIcon((new IconBox()).getClass().getClassLoader().getResource("system-search.png"));
		} catch(Exception e) {
			Logger.notice(new IconBox(), "Exception while loading icons: "+e);
		}
	}

}
