package thaw.core;

import java.net.URL;

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

	public static ImageIcon indexDelete;

	public static ImageIcon error;
	public static ImageIcon ok;

	public static ImageIcon refreshAction;

	public static ImageIcon clearAction;

	public static ImageIcon settings;
	public static ImageIcon minSettings;

	public static ImageIcon reconnectAction;
	public static ImageIcon minReconnectAction;

	public static ImageIcon quitAction;
	public static ImageIcon minQuitAction;

	public static ImageIcon search;

	public static ImageIcon key;


	/**
	 * Not really used
	 */
	public IconBox() {

	}

	protected static ImageIcon loadIcon(final String fileName) {
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
		IconBox.blueBunny = IconBox.loadIcon("blueBunny.png");
		IconBox.connectAction = IconBox.loadIcon("go-jump.png");
		IconBox.minConnectAction = IconBox.loadIcon("min-go-jump.png");
		IconBox.disconnectAction = IconBox.loadIcon("process-stop.png");
		IconBox.queue = IconBox.loadIcon("system-search.png");
		IconBox.minQueue = IconBox.loadIcon("min-system-search.png");
		IconBox.insertions = IconBox.loadIcon("go-next.png");
		IconBox.minInsertions = IconBox.loadIcon("min-go-next.png");
		IconBox.minIndex = IconBox.loadIcon("index.png");
		IconBox.indexNew = IconBox.loadIcon("index-new.png");
		IconBox.indexReuse = IconBox.loadIcon("index-existing.png");
		IconBox.downloads = IconBox.loadIcon("go-first.png");
		IconBox.minDownloads = IconBox.loadIcon("min-go-first.png");
		IconBox.clearAction = IconBox.loadIcon("edit-clear.png");
		IconBox.settings = IconBox.loadIcon("preferences-system.png");
		IconBox.minSettings = IconBox.loadIcon("min-preferences-system.png");
		IconBox.indexEditor = IconBox.loadIcon("edit-find-replace.png");
		IconBox.minIndexEditor = IconBox.loadIcon("min-edit-find-replace.png");
		IconBox.indexBrowser = IconBox.loadIcon("edit-find.png");
		IconBox.minIndexBrowser = IconBox.loadIcon("min-edit-find.png");
		IconBox.addToIndexAction = IconBox.loadIcon("folder.png");
		IconBox.insertAndAddToIndexAction = IconBox.loadIcon("folder-new.png");
		IconBox.makeALinkAction = IconBox.loadIcon("application-internet.png");
		IconBox.reconnectAction = IconBox.loadIcon("view-refresh.png");
		IconBox.minReconnectAction = IconBox.loadIcon("min-view-refresh.png");
		IconBox.refreshAction = IconBox.loadIcon("view-refresh.png");
		IconBox.quitAction = IconBox.loadIcon("system-log-out.png");
		IconBox.minQuitAction = IconBox.loadIcon("min-system-log-out.png");
		IconBox.search = IconBox.loadIcon("system-search.png");
		IconBox.key = IconBox.loadIcon("key.png");
		IconBox.indexDelete = IconBox.loadIcon("x_red.png");
		IconBox.error = IconBox.loadIcon("min-x_red.png");
		IconBox.ok = IconBox.loadIcon("min-v_green.png");
	}

}
