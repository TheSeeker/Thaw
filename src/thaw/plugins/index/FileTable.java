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

import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.tree.TreePath;

import thaw.core.Config;
import thaw.core.I18n;
import thaw.core.Logger;
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

	private int[] selectedRows;

	private IndexBrowserPanel indexBrowser;

	public FileTable(final FCPQueueManager queueManager, IndexBrowserPanel indexBrowser, final Config config) {
		this.indexBrowser = indexBrowser;

		fileListModel = new FileListModel();
		table = new JTable(fileListModel);
		table.setShowGrid(true);
		table.setDefaultRenderer( table.getColumnClass(0), new FileRenderer() );

		table.addMouseListener(this);

		final JTableHeader header = table.getTableHeader();
		header.setUpdateTableInRealTime(true);
		header.setReorderingAllowed(true);

		panel = new JPanel();
		panel.setLayout(new BorderLayout());

		panel.add(new JLabel(I18n.getMessage("thaw.plugin.index.fileList")), BorderLayout.NORTH);
		panel.add(new JScrollPane(table));

		// Menu
		rightClickMenu = new JPopupMenu();
		rightClickActions = new Vector();

		JMenuItem item;

		item = new JMenuItem(I18n.getMessage("thaw.common.action.download"));
		rightClickMenu.add(item);
		rightClickActions.add(new FileManagementHelper.FileDownloader(config, queueManager, item));

		item = new JMenuItem(I18n.getMessage("thaw.plugin.index.insert"));
		rightClickMenu.add(item);
		rightClickActions.add(new FileManagementHelper.FileInserter(queueManager, item));

		item = new JMenuItem(I18n.getMessage("thaw.plugin.index.recalculateKeys"));
		rightClickMenu.add(item);
		rightClickActions.add(new FileManagementHelper.FileKeyComputer(queueManager, item));

		item = new JMenuItem(I18n.getMessage("thaw.common.remove"));
		rightClickMenu.add(item);
		rightClickActions.add(new FileManagementHelper.FileRemover(queueManager, item));

		item = new JMenuItem(I18n.getMessage("thaw.common.copyKeysToClipboard"));
		rightClickMenu.add(item);
		rightClickActions.add(new FileManagementHelper.PublicKeyCopier(item));

		gotoItem = new JMenuItem(I18n.getMessage("thaw.plugin.index.gotoIndex"));
		rightClickMenu.add(gotoItem);
		gotoItem.addActionListener(this);

		updateRightClickMenu(null);
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

	protected Vector getSelectedFiles(final int[] selectedRows) {
		final Vector srcList = fileList.getFileList();
		final Vector files = new Vector();

		for(int i = 0 ; i < selectedRows.length ; i++) {
			final thaw.plugins.index.File file = (thaw.plugins.index.File)srcList.get(selectedRows[i]);
			files.add(file);
		}

		return files;
	}

	public void setFileList(final FileList fileList) {
		if(this.fileList != null) {
			this.fileList.unloadFiles();
		}

		if(fileList != null) {
			fileList.loadFiles(sortColumn, ascOrder);
		}

		this.fileList = fileList;

		fileListModel.reloadFileList(fileList);
	}


	public void mouseClicked(final MouseEvent e) {
		if (fileList instanceof Index)
			((Index)fileList).setChanged(false);

		if((e.getButton() == MouseEvent.BUTTON3)
		   && (fileList != null)) {
			selectedRows = table.getSelectedRows();
			updateRightClickMenu(getSelectedFiles(selectedRows));
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

			final thaw.plugins.index.File file = (thaw.plugins.index.File)fileList.getFileList().get(selectedRows[0]);

			if (file.getParentId() == -1) {
				Logger.notice(this, "No parent ? abnormal");
				return;
			}

			Index parent;

			if (file.getParent() == null)
				parent = indexBrowser.getIndexTree().getRoot().getIndex(file.getParentId());
			else
				parent = file.getParent();

			if (parent == null) {
				Logger.notice(this, "Cannot find again the parent ?! Id: "+Integer.toString(file.getParentId()));
				return;
			}

			indexBrowser.getIndexTree().getTree().setSelectionPath(new TreePath(parent.getTreeNode().getPath()));

			indexBrowser.getTables().setList(parent);

			int row;

			row = parent.getFilePosition(file);

			if (row < 0)
				Logger.notice(this, "File not found in the index ?! Index : "+parent.getPublicKey()+" ; File: " +file.getPublicKey());
			else
				setSelectedRows(row, row);

			return;
		}
	}


	public void setSelectedRows(final int min, final int max) {
		table.setRowSelectionInterval(min, max);
	}


	public class FileListModel extends javax.swing.table.AbstractTableModel implements java.util.Observer {
		private static final long serialVersionUID = 1L;

		public Vector columnNames;

		public Vector files = null; /* thaw.plugins.index.File Vector */

		public FileList fileList;

		public FileListModel() {
			super();

			columnNames = new Vector();

			columnNames.add(I18n.getMessage("thaw.common.file"));
			columnNames.add(I18n.getMessage("thaw.common.size"));

			//columnNames.add(I18n.getMessage("thaw.plugin.index.category"));
			columnNames.add(I18n.getMessage("thaw.common.key"));
			columnNames.add(I18n.getMessage("thaw.common.status"));
		}

		public void reloadFileList(final FileList newFileList) {
			if((fileList != null) && (fileList instanceof Observable)) {
				((Observable)fileList).deleteObserver(this);
			}

			if((newFileList != null) && (newFileList instanceof Observable)) {
				((Observable)newFileList).addObserver(this);
			}

			fileList = newFileList;

			if(files != null) {
				for(final Iterator it = files.iterator();
				    it.hasNext(); ) {
					final thaw.plugins.index.File file = (thaw.plugins.index.File)it.next();
					file.deleteObserver(this);
				}
			}

			files = null;

			if(fileList != null) {
				files = fileList.getFileList();
			}

			this.refresh();
		}

		public int getRowCount() {
			if(files == null)
				return 0;

			return files.size();
		}

		public int getColumnCount() {
			return columnNames.size();
		}

		public String getColumnName(final int column) {
			return (String)columnNames.get(column);
		}

		public Object getValueAt(final int row, final int column) {
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

			if (column == 2)
				return file.getPublicKey();

			if (column == 3)
				return file.getTransfer();

			return null;
		}

		public void refresh() {
			if(fileList != null) {
				files = fileList.getFileList();
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

		public void update(final java.util.Observable o, final Object param) {
			this.refresh(); /* TODO : Do it more nicely ... :) */
		}
	}


	private class FileRenderer extends DefaultTableCellRenderer {
		private final static long serialVersionUID = 20060821;

		public FileRenderer() {

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

				bar.setStringPainted(true);
				bar.setBorderPainted(false);

				if(((query instanceof FCPClientPut) && (query.getProgression() > 0))
				   || ((query instanceof FCPClientGet) && (query.getProgression() < 100)) )
					bar.setValue(query.getProgression());
				else
					bar.setValue(query.getTransferWithTheNodeProgression());

				if(query.isFinished() && !query.isSuccessful())
					bar.setString(I18n.getMessage("thaw.common.failed"));

				if(query.isFinished() && query.isSuccessful())
					bar.setString(I18n.getMessage("thaw.common.ok"));

				if(!query.isFinished()) {
					bar.setString(query.getStatus());
				}

				return bar;
			}

			if(value instanceof Long)
				return super.getTableCellRendererComponent(table,
									   thaw.plugins.queueWatcher.QueueTableModel.getPrintableSize(((Long)value).longValue()),
									   isSelected, hasFocus, row, column);

			final Component cell = super.getTableCellRendererComponent(table, value,
									     isSelected, hasFocus,
									     row, column);

			return cell;
		}

	}

}
