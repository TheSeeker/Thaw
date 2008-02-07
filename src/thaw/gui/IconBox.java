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

	public static ImageIcon markAsRead;
	public static ImageIcon minMarkAsRead;

	public static ImageIcon mail;
	public static ImageIcon minMail;

	public static ImageIcon web;

	public static ImageIcon miniFrostGmailView;
	public static ImageIcon miniFrostOutlookView;
	
	public static ImageIcon trust;
	public static ImageIcon minTrust;
	
	public static ImageIcon minPlugins;


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
		IconBox.blueBunny           = IconBox.loadIcon("images/blueBunny.png");
		IconBox.connectAction       = IconBox.loadIcon("images/connect.png");
		IconBox.minConnectAction    = IconBox.loadIcon("images/min-connect.png");
		IconBox.disconnectAction    = IconBox.loadIcon("images/disconnect.png");
		IconBox.minStop             = IconBox.loadIcon("images/min-stop.png");
		IconBox.stop                = IconBox.loadIcon("images/stop.png");
		IconBox.minDisconnectAction = IconBox.loadIcon("images/min-disconnect.png");
		IconBox.queue               = IconBox.loadIcon("images/connect.png");
		IconBox.minQueue            = IconBox.loadIcon("images/min-connect.png");
		IconBox.insertions          = IconBox.loadIcon("images/insertion.png");
		IconBox.minInsertions       = IconBox.loadIcon("images/min-insertion.png");
		IconBox.minIndex            = IconBox.loadIcon("images/min-index.png");
		IconBox.minIndexReadOnly    = IconBox.loadIcon("images/min-indexReadOnly.png");
		IconBox.indexNew            = IconBox.loadIcon("images/index-new.png");
		IconBox.minIndexNew         = IconBox.loadIcon("images/min-index-new.png");
		IconBox.indexReuse          = IconBox.loadIcon("images/indexReadOnly.png");
		IconBox.downloads           = IconBox.loadIcon("images/download.png");
		IconBox.minDownloads        = IconBox.loadIcon("images/min-download.png");
		IconBox.settings            = IconBox.loadIcon("images/settings.png");
		IconBox.minSettings         = IconBox.loadIcon("images/min-settings.png");
		IconBox.indexBrowser        = IconBox.loadIcon("images/index.png");
		IconBox.minIndexBrowser     = IconBox.loadIcon("images/min-index.png");
		IconBox.addToIndexAction    = IconBox.loadIcon("images/add.png");
		IconBox.add                 = IconBox.loadIcon("images/add.png");
		IconBox.minAdd              = IconBox.loadIcon("images/min-add.png");
		IconBox.insertAndAddToIndexAction = IconBox.loadIcon("images/index.png");
		IconBox.makeALinkAction     = IconBox.loadIcon("images/makeLink.png");
		IconBox.minMakeALinkAction  = IconBox.loadIcon("images/min-makeLink.png");
		IconBox.reconnectAction     = IconBox.loadIcon("images/refresh.png");
		IconBox.minReconnectAction  = IconBox.loadIcon("images/min-refresh.png");
		IconBox.refreshAction       = IconBox.loadIcon("images/refresh.png");
		IconBox.minRefreshAction    = IconBox.loadIcon("images/min-refresh.png");
		IconBox.quitAction          = IconBox.loadIcon("images/quit.png");
		IconBox.minQuitAction       = IconBox.loadIcon("images/min-quit.png");
		IconBox.key                 = IconBox.loadIcon("images/key.png");
		IconBox.minKey              = IconBox.loadIcon("images/min-key.png");
		IconBox.delete              = IconBox.loadIcon("images/delete.png");
		IconBox.minDelete           = IconBox.loadIcon("images/min-delete.png");
		IconBox.folderNew           = IconBox.loadIcon("images/folder-new.png");
		IconBox.minFolderNew        = IconBox.loadIcon("images/min-folder-new.png");
		IconBox.help                = IconBox.loadIcon("images/help.png");
		IconBox.minHelp             = IconBox.loadIcon("images/min-help.png");
		IconBox.mainWindow          = IconBox.loadIcon("images/mainWindow.png");
		IconBox.terminal            = IconBox.loadIcon("images/terminal.png");
		IconBox.minTerminal         = IconBox.loadIcon("images/min-terminal.png");
		IconBox.remove              = IconBox.loadIcon("images/remove.png");
		IconBox.minRemove           = IconBox.loadIcon("images/min-remove.png");
		IconBox.queueWatcher        = IconBox.loadIcon("images/queueWatcher.png");
		IconBox.importExport        = IconBox.loadIcon("images/refresh.png");
		IconBox.minImportAction     = IconBox.loadIcon("images/min-import.png");
		IconBox.minExportAction     = IconBox.loadIcon("images/min-export.png");
		IconBox.database            = IconBox.loadIcon("images/database.png");
		IconBox.minPeerMonitor      = IconBox.loadIcon("images/min-peerMonitor.png");
		IconBox.computer            = IconBox.loadIcon("images/computer.png");
		IconBox.identity            = IconBox.loadIcon("images/identity.png");
		IconBox.peers               = IconBox.loadIcon("images/peers.png");
		IconBox.minPeers            = IconBox.loadIcon("images/min-peers.png");
		IconBox.lookAndFeel         = IconBox.loadIcon("images/lookAndFeel.png");
		IconBox.minLookAndFeel      = IconBox.loadIcon("images/min-lookAndFeel.png");
		IconBox.close               = IconBox.loadIcon("images/emblem-unreadable.png");
		IconBox.minClose            = IconBox.loadIcon("images/min-emblem-unreadable.png");
		IconBox.copy                = IconBox.loadIcon("images/copy.png");
		IconBox.minCopy             = IconBox.loadIcon("images/min-copy.png");
		IconBox.file                = IconBox.loadIcon("images/file.png");
		IconBox.minFile             = IconBox.loadIcon("images/min-file.png");
		IconBox.link                = IconBox.loadIcon("images/indexBrowser.png");
		IconBox.minLink             = IconBox.loadIcon("images/min-indexBrowser.png");
		IconBox.minIndexSettings    = IconBox.loadIcon("images/min-indexSettings.png");
		IconBox.indexSettings       = IconBox.loadIcon("images/indexSettings.png");
		IconBox.addComment          = IconBox.loadIcon("images/mail-message-new.png");
		IconBox.minAddComment       = IconBox.loadIcon("images/min-mail-message-new.png");
		IconBox.markAsRead          = IconBox.loadIcon("images/mail-message-new.png");
		IconBox.minMarkAsRead       = IconBox.loadIcon("images/min-mail-message-new.png");
		IconBox.readComments        = IconBox.loadIcon("images/readComments.png");
		IconBox.minReadComments     = IconBox.loadIcon("images/min-readComments.png");
		IconBox.minRed              = IconBox.loadIcon("images/min-red.png");
		IconBox.minOrange           = IconBox.loadIcon("images/min-orange.png");
		IconBox.minGreen            = IconBox.loadIcon("images/min-green.png");
		IconBox.minDetails          = IconBox.loadIcon("images/min-details.png");
		IconBox.mDns                = IconBox.loadIcon("images/mDns.png");
		IconBox.minMDns             = IconBox.loadIcon("images/min-mDns.png");
		IconBox.msgReply            = IconBox.loadIcon("images/mail-reply-sender.png");
		IconBox.msgNew              = IconBox.loadIcon("images/new-message.png");
		IconBox.minMsgReply         = IconBox.loadIcon("images/min-mail-reply-sender.png");
		IconBox.minMsgNew           = IconBox.loadIcon("images/min-new-message.png");
		IconBox.search              = IconBox.loadIcon("images/mDns.png");
		IconBox.minSearch           = IconBox.loadIcon("images/min-mDns.png");
		IconBox.nextUnread          = IconBox.loadIcon("images/mail-forward.png");
		IconBox.minNextUnread       = IconBox.loadIcon("images/min-mail-forward.png");
		IconBox.up                  = IconBox.loadIcon("images/go-up.png");
		IconBox.down                = IconBox.loadIcon("images/go-down.png");
		IconBox.left                = IconBox.loadIcon("images/go-previous.png");
		IconBox.right               = IconBox.loadIcon("images/go-next.png");
		IconBox.minUp               = IconBox.loadIcon("images/min-go-up.png");
		IconBox.minDown             = IconBox.loadIcon("images/min-go-down.png");
		IconBox.minLeft             = IconBox.loadIcon("images/min-go-previous.png");
		IconBox.minRight            = IconBox.loadIcon("images/min-go-next.png");
		IconBox.attachment          = IconBox.loadIcon("images/mail-attachment.png");
		IconBox.minAttachment       = IconBox.loadIcon("images/min-mail-attachment.png");
		IconBox.windowNew           = IconBox.loadIcon("images/window-new.png");
		IconBox.minWindowNew        = IconBox.loadIcon("images/min-window-new.png");
		IconBox.mail                = IconBox.loadIcon("images/mail.png");
		IconBox.minMail             = IconBox.loadIcon("images/min-mail.png");
		IconBox.web                 = IconBox.loadIcon("images/web.png");
		IconBox.miniFrostGmailView  = IconBox.loadIcon("images/miniFrost-view-gmail.png");
		IconBox.miniFrostOutlookView = IconBox.loadIcon("images/miniFrost-view-outlook.png");
		IconBox.trust               = IconBox.loadIcon("images/trust.png");
		IconBox.minTrust            = IconBox.loadIcon("images/min-trust.png");
		IconBox.minPlugins          = IconBox.loadIcon("images/min-plugins.png");
	}

}
