package thaw.core;

import javax.swing.ImageIcon;
import java.net.URL;

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

	public static ImageIcon indexEditor;
	public static ImageIcon minIndexEditor;

	public static ImageIcon indexBrowser;
	public static ImageIcon minIndexBrowser;

	public static ImageIcon addToIndexAction;

	public static ImageIcon insertAndAddToIndexAction;

	public static ImageIcon makeALinkAction;

	public static ImageIcon minIndex;

	public static ImageIcon indexNew;
	public static ImageIcon indexReuse;

	public static ImageIcon refreshAction;

	public static ImageIcon clearAction;

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

	protected static ImageIcon loadIcon(String fileName) {
		ImageIcon imageIcon;
		URL url;
		Class daClass;
		ClassLoader classLoader;

		daClass = (new IconBox()).getClass();

		if (daClass == null) {
			Logger.error(new IconBox(), "Icon '"+fileName+"' not found ! (Class)");
			return null;
		}

		classLoader = daClass.getClassLoader();

		if (classLoader == null) {
			Logger.error(new IconBox(), "Icon '"+fileName+"' not found ! (Class)");
			return null;
		}

		url = classLoader.getResource(fileName);

		if (url == null) {
			Logger.error(new IconBox(), "Icon '"+fileName+"' not found ! (Class)");
			return null;
		}

		return new ImageIcon(url);
	}

	public static void loadIcons() {
		blueBunny = loadIcon("blueBunny.png");
		connectAction = loadIcon("go-jump.png");
		minConnectAction = loadIcon("min-go-jump.png");
		disconnectAction = loadIcon("process-stop.png");
		queue = loadIcon("system-search.png");
		minQueue = loadIcon("min-system-search.png");
		insertions = loadIcon("go-next.png");
		minInsertions = loadIcon("min-go-next.png");
		minIndex = loadIcon("index.png");
		indexNew = loadIcon("index-new.png");
		indexReuse = loadIcon("index-existing.png");
		downloads = loadIcon("go-first.png");
		minDownloads = loadIcon("min-go-first.png");
		clearAction = loadIcon("edit-clear.png");
		settings = loadIcon("preferences-system.png");
		minSettings = loadIcon("min-preferences-system.png");
		indexEditor = loadIcon("edit-find-replace.png");
		minIndexEditor = loadIcon("min-edit-find-replace.png");
		indexBrowser = loadIcon("edit-find.png");
		minIndexBrowser = loadIcon("min-edit-find.png");
		addToIndexAction = loadIcon("folder.png");
		insertAndAddToIndexAction = loadIcon("folder-new.png");
		makeALinkAction = loadIcon("application-internet.png");
		reconnectAction = loadIcon("view-refresh.png");
		minReconnectAction = loadIcon("min-view-refresh.png");
		refreshAction = loadIcon("view-refresh.png");
		quitAction = loadIcon("system-log-out.png");
		minQuitAction = loadIcon("min-system-log-out.png");
		search = loadIcon("system-search.png");
	}

}
