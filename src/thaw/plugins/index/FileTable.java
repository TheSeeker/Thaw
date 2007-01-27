package thaw.plugins.index;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Iterator;
import java.util.Observable;
import java.util.Vector;

import java.awt.Color;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JButton;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.tree.TreePath;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;


import thaw.core.Config;
import thaw.core.I18n;
import thaw.core.Logger;
import thaw.core.IconBox;
import thaw.core.FreenetURIHelper;
import thaw.plugins.ToolbarModifier;
import thaw.fcp.FCPClientGet;
import thaw.fcp.FCPClientPut;
import thaw.fcp.FCPQueueManager;
import thaw.fcp.FCPTransferQuery;


public class FileTable implements MouseListener, KeyListener, ActionListener {

	private final JPanel panel;

	private final JTable table;
	private FileListModel fileListModel;

	private FileList fileList;

	private String sortColumn;
	private final boolean ascOrder = false;

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



	public FileTable(final FCPQueueManager queueManager,
			 IndexBrowserPanel indexBrowser,
			 final Config config) {
		this.indexBrowser = indexBrowser;
		this.queueManager = queueManager;

		fileListModel = new FileListModel();
		table = new JTable(fileListModel);
		table.setShowGrid(false);
		table.setDefaultRenderer( table.getColumnClass(0), new FileRenderer() );

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

		item = new JMenuItem(I18n.getMessage("thaw.common.action.download"));
		button = new JButton(IconBox.downloads);
		button.setToolTipText(I18n.getMessage("thaw.common.action.download"));
		toolbarActions.add(new FileManagementHelper.FileDownloader(config, queueManager, indexBrowser, button));
		toolbarModifier.addButtonToTheToolbar(button);
		rightClickMenu.add(item);
		rightClickActions.add(new FileManagementHelper.FileDownloader(config, queueManager, indexBrowser, item));

		item = new JMenuItem(I18n.getMessage("thaw.plugin.index.insert"));
		button = new JButton(IconBox.insertions);
		button.setToolTipText(I18n.getMessage("thaw.plugin.index.insert"));
		toolbarActions.add(new FileManagementHelper.FileInserter(queueManager, indexBrowser, button));
		toolbarModifier.addButtonToTheToolbar(button);
		rightClickMenu.add(item);
		rightClickActions.add(new FileManagementHelper.FileInserter(queueManager, indexBrowser, item));

		item = new JMenuItem(I18n.getMessage("thaw.plugin.index.recalculateKeys"));
		button = new JButton(IconBox.key);
		button.setToolTipText(I18n.getMessage("thaw.plugin.index.recalculateKeys"));
		toolbarActions.add(new FileManagementHelper.FileKeyComputer(queueManager, indexBrowser, button));
		toolbarModifier.addButtonToTheToolbar(button);
		rightClickMenu.add(item);
		rightClickActions.add(new FileManagementHelper.FileKeyComputer(queueManager, indexBrowser, item));

		item = new JMenuItem(I18n.getMessage("thaw.common.removeFromTheList"));
		rightClickMenu.add(item);
		rightClickActions.add(new FileManagementHelper.TransferCanceller(queueManager, indexBrowser, item));

		item = new JMenuItem(I18n.getMessage("thaw.common.remove"));
		button = new JButton(IconBox.delete);
		button.setToolTipText(I18n.getMessage("thaw.common.remove"));
		toolbarActions.add(new FileManagementHelper.FileRemover(indexBrowser, button));
		toolbarModifier.addButtonToTheToolbar(button);
		rightClickMenu.add(item);
		rightClickActions.add(new FileManagementHelper.FileRemover(indexBrowser, item));

		item = new JMenuItem(I18n.getMessage("thaw.common.copyKeysToClipboard"));
		rightClickMenu.add(item);
		rightClickActions.add(new FileManagementHelper.PublicKeyCopier(item));

		gotoItem = new JMenuItem(I18n.getMessage("thaw.plugin.index.gotoIndex"));
		rightClickMenu.add(gotoItem);
		gotoItem.addActionListener(this);

		updateRightClickMenu(null);

		refresher = new TransferRefresher();
		Thread th = new Thread(refresher);
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
		final Vector srcList = fileList.getFileList(fileListModel.getColumnNameInDb(columnToSort), sortAsc);
		final Vector files = new Vector();

		for(int i = 0 ; i < selectedRows.length ; i++) {
			files.add(srcList.get(selectedRows[i]));
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

			/* TODO : Re-do it :p */

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
			"filename",
			"size",
			"key"
		};


		public Vector files = null; /* thaw.plugins.index.File Vector */

		public FileList fileList;

		public FileListModel() {
			super();
		}

		public void reloadFileList(final FileList newFileList) {
			fileList = newFileList;

			refresh();
		}

		public Vector getFiles() {
			return files;
		}

		public int getRowCount() {
			if (files == null)
				return 0;

			return files.size();
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

			if (row >= files.size())
				return null;

			final thaw.plugins.index.File file = (thaw.plugins.index.File)files.get(row);

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

				if (key == null || !thaw.core.FreenetURIHelper.isAKey(key))
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


	private class FileRenderer extends DefaultTableCellRenderer {
		private final static long serialVersionUID = 20060821;

		private Color softGray;

		public FileRenderer() {
			softGray = new Color(240,240,240);
		}

		public Component getTableCellRendererComponent(final JTable table, final Object value,
							       final boolean isSelected, final boolean hasFocus,
							       final int row, final int column) {

			if(value == null)
				return super.getTableCellRendererComponent(table, "",
									   isSelected, hasFocus, row, column);

			if(value instanceof FCPTransferQuery) {
				final FCPTransferQuery query = (FCPTransferQuery)value;
				final JProgressBar bar = new JProgressBar(0, 100);

				int progress;

				bar.setStringPainted(true);
				bar.setBorderPainted(false);

				if ((query instanceof FCPClientPut && (query.getTransferWithTheNodeProgression() < 100))
				    || ((query instanceof FCPClientGet) && (query.getTransferWithTheNodeProgression() <= 0)))
					progress = query.getTransferWithTheNodeProgression();
				else
					progress = query.getProgression();

				bar.setValue(progress);

				if(query.isFinished() && !query.isSuccessful())
					bar.setString(I18n.getMessage("thaw.common.failed"));

				if(query.isFinished() && query.isSuccessful())
					bar.setString(I18n.getMessage("thaw.common.ok"));

				if(!query.isFinished()) {
					bar.setString(query.getStatus() +
						      " [ "+Integer.toString(progress)+"% ]");
				}

				return bar;
			}

			Component cell;

			if(value instanceof Long) {

				cell = super.getTableCellRendererComponent(table,
									   thaw.plugins.queueWatcher.QueueTableModel.getPrintableSize(((Long)value).longValue()),
									   isSelected, hasFocus, row, column);

			} else {

				cell = super.getTableCellRendererComponent(table, value,
									   isSelected, hasFocus,
									   row, column);

			}

			if (!isSelected) {
				if (row % 2 == 0)
					cell.setBackground(Color.WHITE);
				else
					cell.setBackground(softGray);
			}
			return cell;
		}

	}


	private class TransferRefresher implements Runnable {
		private boolean running;

		public TransferRefresher() {
			running = true;
		}

		public void run() {
			int i, max;

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

				i = 0;

				try {
					for (Iterator it = fileListModel.getFiles().iterator() ;
					     it.hasNext(); i++) {
						thaw.plugins.index.File file = (thaw.plugins.index.File)it.next();

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
				} catch(final java.util.ConcurrentModificationException e) {
					Logger.debug(this, "Collision : Restarting refresh from the beginnin");
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

			if (columnToSort == modelIndex)
			        sortAsc = !sortAsc;
			else
				columnToSort = modelIndex;


			for (int i = 0; i < columnsCount; i++) {
				final TableColumn column = colModel.getColumn(i);
				column.setHeaderValue(fileListModel.getColumnName(column.getModelIndex()));
			}

			refresh();
		}

	}
}
