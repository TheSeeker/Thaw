package thaw.plugins.index;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import thaw.core.Config;
import thaw.core.ThawThread;
import thaw.core.ThawRunnable;
import thaw.fcp.FreenetURIHelper;
import thaw.core.I18n;
import thaw.gui.IconBox;
import thaw.core.Logger;
import thaw.fcp.FCPQueueManager;
import thaw.fcp.FCPTransferQuery;
import thaw.plugins.ToolbarModifier;
import thaw.gui.Table;


public class FileTable implements MouseListener, KeyListener, ActionListener {

	private final JPanel panel;

	private final Table table;
	private FileListModel fileListModel;

	private FileList fileList;

	private final JPopupMenu rightClickMenu;
	private final Vector rightClickActions;
	private final JMenuItem gotoItem;
	// Download
	// Insert
	// Compute keys
	// Remove
	// Copy file keys

	private final ToolbarModifier toolbarModifier;
	private final Vector toolbarActions;

	private int[] selectedRows;

	private IndexBrowserPanel indexBrowser;
	private FCPQueueManager queueManager;

	private TransferRefresher refresher;


	private int columnToSort = -1;
	private boolean sortAsc = false;


	private thaw.plugins.index.File firstSelectedFile; /* used for the 'goto corresponding index' option */


	public FileTable(final FCPQueueManager queueManager,
			 IndexBrowserPanel indexBrowser,
			 final Config config) {
		this.indexBrowser = indexBrowser;
		this.queueManager = queueManager;

		fileListModel = new FileListModel();
		table = new Table(config, "index_file_table", fileListModel);
		//table = new JTable(fileListModel);
		table.setShowGrid(false);
		table.setIntercellSpacing(new java.awt.Dimension(0, 0));
		table.specifyColumnWithKeys(2);
		table.addMouseListener(this);

		final JTableHeader header = table.getTableHeader();
		header.setUpdateTableInRealTime(true);
		header.setReorderingAllowed(true);
		header.addMouseListener(new ColumnListener(table));

		panel = new JPanel();
		panel.setLayout(new BorderLayout());

		panel.add(new JLabel(I18n.getMessage("thaw.plugin.index.fileList")), BorderLayout.NORTH);
		panel.add(new JScrollPane(table));

		// Menu
		rightClickMenu = new JPopupMenu();
		rightClickActions = new Vector();


		toolbarModifier = new ToolbarModifier(indexBrowser.getMainWindow());
		toolbarActions = new Vector();

		JButton button;
		JMenuItem item;

		item = new JMenuItem(I18n.getMessage("thaw.common.action.download"), IconBox.minDownloads);
		button = new JButton(IconBox.downloads);
		button.setToolTipText(I18n.getMessage("thaw.common.action.download"));
		toolbarActions.add(new FileManagementHelper.FileDownloader(config, queueManager, indexBrowser, button));
		toolbarModifier.addButtonToTheToolbar(button);
		rightClickMenu.add(item);
		rightClickActions.add(new FileManagementHelper.FileDownloader(config, queueManager, indexBrowser, item));

		item = new JMenuItem(I18n.getMessage("thaw.plugin.index.insert"), IconBox.minInsertions);
		button = new JButton(IconBox.insertions);
		button.setToolTipText(I18n.getMessage("thaw.plugin.index.insert"));
		toolbarActions.add(new FileManagementHelper.FileInserter(queueManager, indexBrowser, button));
		toolbarModifier.addButtonToTheToolbar(button);
		rightClickMenu.add(item);
		rightClickActions.add(new FileManagementHelper.FileInserter(queueManager, indexBrowser, item));

		item = new JMenuItem(I18n.getMessage("thaw.plugin.index.recalculateKeys"), IconBox.minKey);
		button = new JButton(IconBox.key);
		button.setToolTipText(I18n.getMessage("thaw.plugin.index.recalculateKeys"));
		toolbarActions.add(new FileManagementHelper.FileKeyComputer(queueManager, indexBrowser, button));
		toolbarModifier.addButtonToTheToolbar(button);
		rightClickMenu.add(item);
		rightClickActions.add(new FileManagementHelper.FileKeyComputer(queueManager, indexBrowser, item));

		item = new JMenuItem(I18n.getMessage("thaw.common.removeFromTheList"), IconBox.minStop);
		rightClickMenu.add(item);
		rightClickActions.add(new FileManagementHelper.TransferCanceller(queueManager, indexBrowser, item));

		item = new JMenuItem(I18n.getMessage("thaw.common.remove"), IconBox.minDelete);
		button = new JButton(IconBox.delete);
		button.setToolTipText(I18n.getMessage("thaw.common.remove"));
		toolbarActions.add(new FileManagementHelper.FileRemover(indexBrowser, button));
		toolbarModifier.addButtonToTheToolbar(button);
		rightClickMenu.add(item);
		rightClickActions.add(new FileManagementHelper.FileRemover(indexBrowser, item));

		item = new JMenuItem(I18n.getMessage("thaw.common.copyKeysToClipboard"), IconBox.minCopy);
		rightClickMenu.add(item);
		rightClickActions.add(new FileManagementHelper.PublicKeyCopier(item));

		gotoItem = new JMenuItem(I18n.getMessage("thaw.plugin.index.gotoIndex"));
		rightClickMenu.add(gotoItem);
		gotoItem.addActionListener(this);

		updateRightClickMenu(null);

		refresher = new TransferRefresher();
		Thread th = new ThawThread(refresher, "File list refresher", this);
		th.start();
	}

	public void stopRefresher() {
		refresher.stop();
	}


	public ToolbarModifier getToolbarModifier() {
		return toolbarModifier;
	}

	public JPanel getPanel() {
		return panel;
	}

	protected void updateRightClickMenu(final Vector selectedFiles) {
		FileManagementHelper.FileAction action;

		firstSelectedFile = selectedFiles != null && selectedFiles.size() > 0 ?
			((thaw.plugins.index.File)selectedFiles.get(0)) : null;

		for(final Iterator it = rightClickActions.iterator();
		    it.hasNext();) {
			action = (FileManagementHelper.FileAction)it.next();
			action.setTarget(selectedFiles);
		}

		gotoItem.setEnabled((fileList != null) && !(fileList instanceof Index));
	}

	protected void updateToolbar(final Vector selectedFiles) {
		FileManagementHelper.FileAction action;

		for(final Iterator it = toolbarActions.iterator();
		    it.hasNext();) {
			action = (FileManagementHelper.FileAction)it.next();
			action.setTarget(selectedFiles);
		}
	}

	protected Vector getSelectedFiles(final int[] selectedRows) {
		//final Vector srcList = fileList.getFileList(fileListModel.getColumnNameInDb(columnToSort), sortAsc);
		final File[] srcList = fileListModel.getFiles();
		final Vector files = new Vector();

		for(int i = 0 ; i < selectedRows.length ; i++) {
			files.add(srcList[selectedRows[i]]);
		}

		return files;
	}

	public void setFileList(final FileList fileList) {
		this.fileList = fileList;

		fileListModel.reloadFileList(fileList);
	}


	public FileList getFileList() {
		return fileList;
	}


	public void mouseClicked(final MouseEvent e) {
		Vector selection;

		if (fileList == null) {
			selectedRows = null;
			return;
		}

		selectedRows = table.getSelectedRows();
		selection = getSelectedFiles(selectedRows);

		if (e.getButton() == MouseEvent.BUTTON1) {
			updateToolbar(selection);
			toolbarModifier.displayButtonsInTheToolbar();
		}

		if (e.getButton() == MouseEvent.BUTTON3) {
			updateRightClickMenu(selection);
			rightClickMenu.show(e.getComponent(), e.getX(), e.getY());
		}
	}

	public void mouseEntered(final MouseEvent e) { }

	public void mouseExited(final MouseEvent e) { }

	public void mousePressed(final MouseEvent e) { }

	public void mouseReleased(final MouseEvent e) { }

	public void keyPressed(final KeyEvent e) { }

	public void keyReleased(final KeyEvent e) { }

	public void keyTyped(final KeyEvent e) { }

	public void actionPerformed(final ActionEvent e) {
		if(fileList == null)
			return;

		if (e.getSource() == gotoItem) {
			if (selectedRows.length <= 0)
				return;

			if (firstSelectedFile != null)
				indexBrowser.selectIndex(firstSelectedFile.getParentId());

			return;
		}
	}


	public void setSelectedRows(final int min, final int max) {
		table.setRowSelectionInterval(min, max);
	}


	public void refresh() {
		fileListModel.refresh();
	}


	public class FileListModel extends javax.swing.table.AbstractTableModel {
		private static final long serialVersionUID = 1L;

		public String[] columnNames =
		{
			I18n.getMessage("thaw.common.file"),
			I18n.getMessage("thaw.common.size"),
			I18n.getMessage("thaw.common.key"),
			I18n.getMessage("thaw.common.status")
		};

		public String[] columnNamesInDb =
		{
			"LOWER(filename)",
			"size",
			"LOWER(publicKey)",
			null
		};


		public File[] files = null;

		public FileList fileList;

		public FileListModel() {
			super();
		}

		public void reloadFileList(final FileList newFileList) {
			fileList = newFileList;

			refresh();
		}

		public File[] getFiles() {
			return files;
		}

		public int getRowCount() {
			if (files == null)
				return 0;

			return files.length;
		}

		public int getColumnCount() {
			return columnNames.length;
		}

		public String getColumnName(final int column) {
			String result = columnNames[column];

			if(column == columnToSort) {
				if(sortAsc)
					result = result + " >>";
				else
					result = result + " <<";
			}

			return result;
		}

		public String getColumnNameInDb(final int column) {
			if (column < 0)
				return null;

			return columnNamesInDb[column];
		}

		public Object getValueAt(final int row, final int column) {
			if (files == null)
				return null;

			if (row >= files.length)
				return null;

			final thaw.plugins.index.File file = (thaw.plugins.index.File)files[row];

			if(column == 0)
				return file.getFilename();

			if(column == 1) {
				if (file.getSize() > 0)
					return new Long(file.getSize());
				else
					return I18n.getMessage("thaw.common.unknown");
			}

			//if (column == 2)
			//	return file.getCategory();

			if (column == 2) {
				String key = file.getPublicKey();

				if (key == null || !FreenetURIHelper.isAKey(key))
					key = I18n.getMessage("thaw.common.unknown");

				return key;

			}

			if (column == 3) {
				return file.getTransfer(queueManager);
			}

			return null;
		}

		public void refresh() {
			if(fileList != null) {
				files = fileList.getFileList(getColumnNameInDb(columnToSort), sortAsc);
			} else {
				files = null;
			}

			final TableModelEvent event = new TableModelEvent(this);
			this.refresh(event);
		}

		public void refresh(final int row) {
			final TableModelEvent event = new TableModelEvent(this, row);
			this.refresh(event);
		}

		public void refresh(final TableModelEvent e) {
			fireTableChanged(e);
		}
	}


	private class TransferRefresher implements ThawRunnable {
		private boolean running;

		public TransferRefresher() {
			running = true;
		}

		public void run() {
			while(running) {
				try {
					Thread.sleep(500);
				} catch(InterruptedException e) {
					/* \_o< */
				}

				if (!running)
					return;

				if (fileListModel.getFiles() == null)
					continue;

				File[] files = fileListModel.getFiles();

				for (int i = 0 ; i < files.length ; i++) {
					thaw.plugins.index.File file = (thaw.plugins.index.File)files[i];

					if (file.getPublicKey() == null
					    || !FreenetURIHelper.isAKey(file.getPublicKey())) {
						FCPTransferQuery transfer;
						transfer = file.getTransfer(queueManager);

						if (transfer != null) {
							if (transfer.isSuccessful())
								file.forceReload();
						}
					}

					/* won't query the database */
					fileListModel.refresh(i);

					try {
						Thread.sleep(100);
					} catch(InterruptedException e) {
						/* \_o< */
					}
				}
			}
		}

		public void stop() {
			running = false;
		}
	}




	protected class ColumnListener extends MouseAdapter {
		private JTable table;

		public ColumnListener(final JTable t) {
			table = t;
		}

		public void mouseClicked(final MouseEvent e) {
			final TableColumnModel colModel = table.getColumnModel();
			final int columnModelIndex = colModel.getColumnIndexAtX(e.getX());
			final int modelIndex = colModel.getColumn(columnModelIndex).getModelIndex();

			final int columnsCount = table.getColumnCount();

			if (modelIndex < 0)
				return;

			if (modelIndex == 3) {
				Logger.notice(this, "Can't order by transfer state because of performances issues");
				return;
			}

			if (columnToSort == modelIndex)
			        sortAsc = !sortAsc;
			else {
				columnToSort = modelIndex;
				sortAsc = true;
			}


			for (int i = 0; i < columnsCount; i++) {
				final TableColumn column = colModel.getColumn(i);
				column.setHeaderValue(fileListModel.getColumnName(column.getModelIndex()));
			}

			refresh();
		}

	}
}
