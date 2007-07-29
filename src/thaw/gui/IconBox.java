package thaw.gui;

import java.net.URL;

import javax.swing.ImageIcon;

import thaw.core.Logger;


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
	public static ImageIcon minDisconnectAction;

	public static ImageIcon stop;
	public static ImageIcon minStop;

	public static ImageIcon copy;
	public static ImageIcon minCopy;

	public static ImageIcon queue;
	public static ImageIcon minQueue;

	public static ImageIcon insertions;
	public static ImageIcon minInsertions;

	public static ImageIcon downloads;
	public static ImageIcon minDownloads;

	public static ImageIcon indexBrowser;
	public static ImageIcon minIndexBrowser;

	public static ImageIcon addToIndexAction;

	public static ImageIcon insertAndAddToIndexAction;

	public static ImageIcon makeALinkAction;
	public static ImageIcon minMakeALinkAction;

	public static ImageIcon minIndex;
	public static ImageIcon minIndexReadOnly;

	public static ImageIcon indexNew;
	public static ImageIcon minIndexNew;

	public static ImageIcon indexReuse;

	public static ImageIcon delete;
	public static ImageIcon minDelete;

	public static ImageIcon refreshAction;
	public static ImageIcon minRefreshAction;

	public static ImageIcon settings;
	public static ImageIcon minSettings;

	public static ImageIcon reconnectAction;
	public static ImageIcon minReconnectAction;

	public static ImageIcon quitAction;
	public static ImageIcon minQuitAction;

	public static ImageIcon key;
	public static ImageIcon minKey;

	public static ImageIcon help;
	public static ImageIcon minHelp;

	public static ImageIcon folderNew;
	public static ImageIcon minFolderNew;

	public static ImageIcon mainWindow;

	public static ImageIcon add;
	public static ImageIcon minAdd;

	public static ImageIcon remove;
	public static ImageIcon minRemove;

	public static ImageIcon terminal;
	public static ImageIcon minTerminal;
	public static ImageIcon queueWatcher;
	public static ImageIcon importExport;


	public static ImageIcon minPeerMonitor;

	public static ImageIcon minImportAction;
	public static ImageIcon minExportAction;

	public static ImageIcon database;

	public static ImageIcon computer;
	public static ImageIcon identity;

	public static ImageIcon peers;
	public static ImageIcon minPeers;

	public static ImageIcon lookAndFeel;
	public static ImageIcon minLookAndFeel;

	public static ImageIcon close;
	public static ImageIcon minClose;

	public static ImageIcon link;
	public static ImageIcon minLink;

	public static ImageIcon file;
	public static ImageIcon minFile;

	public static ImageIcon indexSettings;
	public static ImageIcon minIndexSettings;

	public static ImageIcon addComment;
	public static ImageIcon minAddComment;

	public static ImageIcon readComments;
	public static ImageIcon minReadComments;

	public static ImageIcon minRed;
	public static ImageIcon minOrange;
	public static ImageIcon minGreen;

	public static ImageIcon minDetails;

	public static ImageIcon mDns;
	public static ImageIcon minMDns;

	public static ImageIcon msgReply;
	public static ImageIcon msgNew;

	public static ImageIcon minMsgReply;
	public static ImageIcon minMsgNew;

	public static ImageIcon search;
	public static ImageIcon minSearch;

	public static ImageIcon nextUnread;
	public static ImageIcon minNextUnread;

	public static ImageIcon up;
	public static ImageIcon down;
	public static ImageIcon left;
	public static ImageIcon right;

	public static ImageIcon minUp;
	public static ImageIcon minDown;
	public static ImageIcon minLeft;
	public static ImageIcon minRight;

	public static ImageIcon attachment;
	public static ImageIcon minAttachment;

	public static ImageIcon windowNew;
	public static ImageIcon minWindowNew;


	/**
	 * Not really used
	 */
	public IconBox() {

	}

	protected static ImageIcon loadIcon(final String fileName) {
		URL url;
		Class daClass;
		ClassLoader classLoader;

		daClass = IconBox.class;

		if (daClass == null) {
			Logger.error(IconBox.class, "Icon '"+fileName+"' not found ! (Class)");
			return null;
		}

		classLoader = daClass.getClassLoader();

		if (classLoader == null) {
			Logger.error(IconBox.class, "Icon '"+fileName+"' not found ! (ClassLoader)");
			return null;
		}

		url = classLoader.getResource(fileName);

		if (url == null) {
			Logger.error(IconBox.class, "Icon '"+fileName+"' not found ! (Resource)");
			return null;
		}

		return new ImageIcon(url);
	}


	public static void loadIcons() {
		IconBox.blueBunny           = IconBox.loadIcon("blueBunny.png");
		IconBox.connectAction       = IconBox.loadIcon("connect.png");
		IconBox.minConnectAction    = IconBox.loadIcon("min-connect.png");
		IconBox.disconnectAction    = IconBox.loadIcon("disconnect.png");
		IconBox.minStop             = IconBox.loadIcon("min-stop.png");
		IconBox.stop                = IconBox.loadIcon("stop.png");
		IconBox.minDisconnectAction = IconBox.loadIcon("min-disconnect.png");
		IconBox.queue               = IconBox.loadIcon("connect.png");
		IconBox.minQueue            = IconBox.loadIcon("min-connect.png");
		IconBox.insertions          = IconBox.loadIcon("insertion.png");
		IconBox.minInsertions       = IconBox.loadIcon("min-insertion.png");
		IconBox.minIndex            = IconBox.loadIcon("min-index.png");
		IconBox.minIndexReadOnly    = IconBox.loadIcon("min-indexReadOnly.png");
		IconBox.indexNew            = IconBox.loadIcon("index-new.png");
		IconBox.minIndexNew         = IconBox.loadIcon("min-index-new.png");
		IconBox.indexReuse          = IconBox.loadIcon("indexReadOnly.png");
		IconBox.downloads           = IconBox.loadIcon("download.png");
		IconBox.minDownloads        = IconBox.loadIcon("min-download.png");
		IconBox.settings            = IconBox.loadIcon("settings.png");
		IconBox.minSettings         = IconBox.loadIcon("min-settings.png");
		IconBox.indexBrowser        = IconBox.loadIcon("index.png");
		IconBox.minIndexBrowser     = IconBox.loadIcon("min-index.png");
		IconBox.addToIndexAction    = IconBox.loadIcon("add.png");
		IconBox.add                 = IconBox.loadIcon("add.png");
		IconBox.minAdd              = IconBox.loadIcon("min-add.png");
		IconBox.insertAndAddToIndexAction = IconBox.loadIcon("index.png");
		IconBox.makeALinkAction     = IconBox.loadIcon("makeLink.png");
		IconBox.minMakeALinkAction  = IconBox.loadIcon("min-makeLink.png");
		IconBox.reconnectAction     = IconBox.loadIcon("refresh.png");
		IconBox.minReconnectAction  = IconBox.loadIcon("min-refresh.png");
		IconBox.refreshAction       = IconBox.loadIcon("refresh.png");
		IconBox.minRefreshAction    = IconBox.loadIcon("min-refresh.png");
		IconBox.quitAction          = IconBox.loadIcon("quit.png");
		IconBox.minQuitAction       = IconBox.loadIcon("min-quit.png");
		IconBox.key                 = IconBox.loadIcon("key.png");
		IconBox.minKey              = IconBox.loadIcon("min-key.png");
		IconBox.delete              = IconBox.loadIcon("delete.png");
		IconBox.minDelete           = IconBox.loadIcon("min-delete.png");
		IconBox.folderNew           = IconBox.loadIcon("folder-new.png");
		IconBox.minFolderNew        = IconBox.loadIcon("min-folder-new.png");
		IconBox.help                = IconBox.loadIcon("help.png");
		IconBox.minHelp             = IconBox.loadIcon("min-help.png");
		IconBox.mainWindow          = IconBox.loadIcon("mainWindow.png");
		IconBox.terminal            = IconBox.loadIcon("terminal.png");
		IconBox.minTerminal         = IconBox.loadIcon("min-terminal.png");
		IconBox.remove              = IconBox.loadIcon("remove.png");
		IconBox.minRemove           = IconBox.loadIcon("min-remove.png");
		IconBox.queueWatcher        = IconBox.loadIcon("queueWatcher.png");
		IconBox.importExport        = IconBox.loadIcon("refresh.png");
		IconBox.minImportAction     = IconBox.loadIcon("min-import.png");
		IconBox.minExportAction     = IconBox.loadIcon("min-export.png");
		IconBox.database            = IconBox.loadIcon("database.png");
		IconBox.minPeerMonitor      = IconBox.loadIcon("min-peerMonitor.png");
		IconBox.computer            = IconBox.loadIcon("computer.png");
		IconBox.identity            = IconBox.loadIcon("identity.png");
		IconBox.peers               = IconBox.loadIcon("peers.png");
		IconBox.minPeers            = IconBox.loadIcon("min-peers.png");
		IconBox.lookAndFeel         = IconBox.loadIcon("lookAndFeel.png");
		IconBox.minLookAndFeel      = IconBox.loadIcon("min-lookAndFeel.png");
		IconBox.close               = IconBox.loadIcon("emblem-unreadable.png");
		IconBox.minClose            = IconBox.loadIcon("min-emblem-unreadable.png");
		IconBox.copy                = IconBox.loadIcon("copy.png");
		IconBox.minCopy             = IconBox.loadIcon("min-copy.png");
		IconBox.file                = IconBox.loadIcon("file.png");
		IconBox.minFile             = IconBox.loadIcon("min-file.png");
		IconBox.link                = IconBox.loadIcon("indexBrowser.png");
		IconBox.minLink             = IconBox.loadIcon("min-indexBrowser.png");
		IconBox.minIndexSettings    = IconBox.loadIcon("min-indexSettings.png");
		IconBox.indexSettings       = IconBox.loadIcon("indexSettings.png");
		IconBox.addComment          = IconBox.loadIcon("mail-message-new.png");
		IconBox.minAddComment       = IconBox.loadIcon("min-mail-message-new.png");
		IconBox.readComments        = IconBox.loadIcon("readComments.png");
		IconBox.minReadComments     = IconBox.loadIcon("min-readComments.png");
		IconBox.minRed              = IconBox.loadIcon("min-red.png");
		IconBox.minOrange           = IconBox.loadIcon("min-orange.png");
		IconBox.minGreen            = IconBox.loadIcon("min-green.png");
		IconBox.minDetails          = IconBox.loadIcon("min-details.png");
		IconBox.mDns                = IconBox.loadIcon("mDns.png");
		IconBox.minMDns             = IconBox.loadIcon("min-mDns.png");
		IconBox.msgReply            = IconBox.loadIcon("mail-reply-sender.png");
		IconBox.msgNew              = IconBox.loadIcon("mail-message-new.png");
		IconBox.minMsgReply         = IconBox.loadIcon("min-mail-reply-sender.png");
		IconBox.minMsgNew           = IconBox.loadIcon("min-mail-message-new.png");
		IconBox.search              = IconBox.loadIcon("mDns.png");
		IconBox.minSearch           = IconBox.loadIcon("min-mDns.png");
		IconBox.nextUnread          = IconBox.loadIcon("mail-forward.png");
		IconBox.minNextUnread       = IconBox.loadIcon("min-mail-forward.png");
		IconBox.up                  = IconBox.loadIcon("go-up.png");
		IconBox.down                = IconBox.loadIcon("go-down.png");
		IconBox.left                = IconBox.loadIcon("go-previous.png");
		IconBox.right               = IconBox.loadIcon("go-next.png");
		IconBox.minUp               = IconBox.loadIcon("min-go-up.png");
		IconBox.minDown             = IconBox.loadIcon("min-go-down.png");
		IconBox.minLeft             = IconBox.loadIcon("min-go-previous.png");
		IconBox.minRight            = IconBox.loadIcon("min-go-next.png");
		IconBox.attachment          = IconBox.loadIcon("mail-attachment.png");
		IconBox.minAttachment       = IconBox.loadIcon("min-mail-attachment.png");
		IconBox.windowNew           = IconBox.loadIcon("window-new.png");
		IconBox.minWindowNew        = IconBox.loadIcon("min-window-new.png");
	}

}
