package thaw.plugins;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.JFileChooser;

import thaw.core.*;
import thaw.plugins.IndexBrowser;
import thaw.plugins.index.*;

public class IndexExporter implements Plugin, ActionListener {

	private Core core;

	private JMenu menu;
	private JMenu exportMenu;
	private JMenuItem importItem;
	private JMenuItem exportKeys;
	private JMenuItem exportAll;

	private IndexBrowser indexBrowser;

	public boolean run(Core core) {
		this.core = core;

		if(core.getPluginManager().getPlugin("thaw.plugins.IndexBrowser") == null) {
			Logger.info(this, "Loading IndexBrowser plugin");

			if(core.getPluginManager().loadPlugin("thaw.plugins.IndexBrowser") == null
			   || !core.getPluginManager().runPlugin("thaw.plugins.IndexBrowser")) {
				Logger.error(this, "Unable to load IndexBrowser !");
				return false;
			}
		}

		indexBrowser = (IndexBrowser)core.getPluginManager().getPlugin("thaw.plugins.IndexBrowser");

		if (indexBrowser == null) {
			Logger.error(this, "WTF?!");
			return false;
		}

		menu = new JMenu(I18n.getMessage("thaw.plugin.index.indexes"));
		menu.setIcon(IconBox.minIndex);
		exportMenu = new JMenu(I18n.getMessage("thaw.plugin.index.export"));
		exportMenu.setIcon(IconBox.minExportAction);
		exportKeys = new JMenuItem(I18n.getMessage("thaw.plugin.index.export.indexKeys"));
		exportAll  = new JMenuItem(I18n.getMessage("thaw.plugin.index.export.all"));
		importItem = new JMenuItem(I18n.getMessage("thaw.plugin.index.import"), IconBox.minImportAction);

		exportMenu.add(exportKeys);
		exportMenu.add(exportAll);

		exportKeys.addActionListener(this);
		exportAll.addActionListener(this);
		importItem.addActionListener(this);

		menu.add(exportMenu);
		menu.add(importItem);

		core.getMainWindow().insertInFileMenuAt(menu, 2);

		return true;
	}


	public boolean stop() {
		core.getMainWindow().removeFromFileMenu(menu);

		return true;
	}

	public String getNameForUser() {
		return I18n.getMessage("thaw.plugin.index.importExportPlugin");
	}

	public javax.swing.ImageIcon getIcon() {
		return IconBox.importExport;
	}

	public void actionPerformed(ActionEvent e) {
		FileChooser fileChooser;
		boolean in = false;
		boolean content = false;

		if (e.getSource() == importItem) {
			in = true;
			content = true;
		}


		if (e.getSource() == exportAll) {
			in = false;
			content = true;
		}

		if (e.getSource() == exportKeys) {
			in = false;
			content = false;
		}

		fileChooser = new FileChooser();
		fileChooser.setTitle(in ? I18n.getMessage("thaw.plugin.index.import") : I18n.getMessage("thaw.plugin.index.export"));
		fileChooser.setDirectoryOnly(false);
		fileChooser.setDialogType(in ? JFileChooser.OPEN_DIALOG : JFileChooser.SAVE_DIALOG);

		java.io.File file = fileChooser.askOneFile();

		if (file == null || (in && (!file.exists() || !file.isFile()))) {
			Logger.notice(this, "Cancelled");
			return;
		}


		Worker k = new Worker(in, content, file);
		Thread th = new Thread(k);
		th.start();

	}

	private class Worker implements Runnable {
		private boolean impor;
		private boolean content;
		private java.io.File file;

		public Worker(boolean impor, boolean content, java.io.File file) {
			this.impor = impor;
			this.content = content;
			this.file = file;
		}

		public void run() {
			if (impor) {
				DatabaseManager.importDatabase(file,
							       indexBrowser.getIndexBrowserPanel(),
							       core.getQueueManager());
			} else  {
				DatabaseManager.exportDatabase(file,
							       indexBrowser.getIndexBrowserPanel().getDb(),
							       indexBrowser.getIndexBrowserPanel().getIndexTree(),
							       content);
			}
		}

	}
}
