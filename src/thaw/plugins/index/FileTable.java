package thaw.plugins.index;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JScrollPane;

import java.awt.BorderLayout;
import javax.swing.event.TableModelEvent;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;

import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Component;

import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JProgressBar;
import javax.swing.JLabel;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

import java.util.Vector;
import java.util.Iterator;

import java.util.Observable;
import javax.swing.table.JTableHeader;


import thaw.core.*;
import thaw.fcp.*;

public class FileTable implements MouseListener, KeyListener, ActionListener {

	private JPanel panel;
	
	private JTable table;
	private FileListModel fileListModel;

	private FileList fileList = null;

	private boolean modifiables;
	
	private JPopupMenu rightClickMenu;
	private JMenuItem removeFiles;
	private JMenuItem insertFiles;
	private JMenuItem recalculateKeys;

	private JMenuItem downloadFiles;
	
	private JMenuItem copyFileKeys;

	private JMenuItem gotoIndex;

	private FCPQueueManager queueManager;

	private String sortColumn = null;
	private boolean ascOrder = false;

	private int[] selectedRows;

	private Config config;
	private Tables tables;
	private IndexTree tree;

	public FileTable(boolean modifiables, FCPQueueManager queueManager, IndexTree tree, Config config, Tables tables) {
		this.queueManager = queueManager;
		this.config = config;
		this.modifiables = modifiables;
		this.tables = tables;
		this.tree = tree;
	
		
		this.rightClickMenu = new JPopupMenu();
		
		if(modifiables) {
			this.removeFiles = new JMenuItem(I18n.getMessage("thaw.common.remove"));
			this.insertFiles = new JMenuItem(I18n.getMessage("thaw.plugin.index.insert"));
			this.recalculateKeys = new JMenuItem(I18n.getMessage("thaw.plugin.index.recalculateKeys"));
			this.removeFiles.addActionListener(this);
			this.insertFiles.addActionListener(this);
			this.recalculateKeys.addActionListener(this);
			this.rightClickMenu.add(this.removeFiles);
			this.rightClickMenu.add(this.insertFiles);
			this.rightClickMenu.add(this.recalculateKeys);
		} else {
			this.downloadFiles   = new JMenuItem(I18n.getMessage("thaw.common.action.download"));
			this.downloadFiles.addActionListener(this);
			this.rightClickMenu.add(this.downloadFiles);
		}

		this.copyFileKeys = new JMenuItem(I18n.getMessage("thaw.common.copyKeysToClipboard"));
		this.copyFileKeys.addActionListener(this);
		this.rightClickMenu.add(this.copyFileKeys);

		this.gotoIndex = new JMenuItem(I18n.getMessage("thaw.plugin.index.gotoIndex"));
		this.gotoIndex.addActionListener(this);
		this.gotoIndex.setEnabled(false);
		this.rightClickMenu.add(this.gotoIndex);

		this.fileListModel = new FileListModel();
		this.table = new JTable(this.fileListModel);
		this.table.setShowGrid(true);
		this.table.setDefaultRenderer( this.table.getColumnClass(0), new FileRenderer() );

		this.table.addMouseListener(this);

		JTableHeader header = this.table.getTableHeader();
		header.setUpdateTableInRealTime(true);
		header.setReorderingAllowed(true);

		this.panel = new JPanel();
		this.panel.setLayout(new BorderLayout());
		
		this.panel.add(new JLabel(I18n.getMessage("thaw.plugin.index.fileList")), BorderLayout.NORTH);
		this.panel.add(new JScrollPane(this.table));

	}


	public JPanel getPanel() {
		return this.panel;
	}

	
	public void setFileList(FileList fileList) {
		if(this.fileList != null) {
			this.fileList.unloadFiles();
		}
		
		if(fileList != null) {
			fileList.loadFiles(this.sortColumn, this.ascOrder);
		}

		this.fileList = fileList;

		this.fileListModel.reloadFileList(fileList);
	}


	public void mouseClicked(MouseEvent e) {
		if(e.getButton() == MouseEvent.BUTTON3
		   && this.fileList != null) {
			this.gotoIndex.setEnabled(!(this.fileList instanceof Index));

			this.selectedRows = this.table.getSelectedRows();
			this.rightClickMenu.show(e.getComponent(), e.getX(), e.getY());
		}
	}

	public void mouseEntered(MouseEvent e) { }

	public void mouseExited(MouseEvent e) { }

	public void mousePressed(MouseEvent e) { }

	public void mouseReleased(MouseEvent e) { }

	public void keyPressed(KeyEvent e) { }

	public void keyReleased(KeyEvent e) { }

	public void keyTyped(KeyEvent e) { }

	public void actionPerformed(ActionEvent e) {
		if(this.fileList == null)
			return;

		String keys = "";
		
		Vector files = null;

		java.io.File destination = null;

		if (e.getSource() == this.gotoIndex) {
			if (this.selectedRows.length <= 0)
				return;

			thaw.plugins.index.File file = (thaw.plugins.index.File)this.fileList.getFileList().get(this.selectedRows[0]);

			if (file.getParentId() == -1) {
				Logger.notice(this, "No parent ? abnormal");
				return;
			}

			Index parent = this.tree.getRoot().getIndex(file.getParentId());

			if (parent == null) {
				Logger.notice(this, "Cannot find again the parent ?! Id: "+Integer.toString(file.getParentId()));
				return;
			}
			
			this.tables.setList(parent);

			int row;
			
			row = parent.getFilePosition(file);

			if (row < 0)
				Logger.notice(this, "File not found in the index ?! Index : "+parent.getPublicKey()+" ; File: " +file.getPublicKey());
			else
				this.setSelectedRows(row, row);

			return;
		}

		if(e.getSource() == this.downloadFiles) {
			FileChooser fileChooser ;
			if (this.config.getValue("lastDestinationDirectory") == null)
				fileChooser = new FileChooser();
			else
				fileChooser = new FileChooser(this.config.getValue("lastDestinationDirectory"));
			fileChooser.setTitle(I18n.getMessage("thaw.plugin.fetch.destinationDirectory"));
			fileChooser.setDirectoryOnly(true);
			fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
			
			destination = fileChooser.askOneFile();

			if(destination == null)
				return;

			this.config.setValue("lastDestinationDirectory", destination.getPath());
		}

		if(e.getSource() == this.removeFiles) {
			files = this.fileList.getFileList();
		}

		for(int i = 0 ; i < this.selectedRows.length ; i++) {

			if(e.getSource() == this.removeFiles) {
				Index index = (Index)this.fileList;

				thaw.plugins.index.File file = (thaw.plugins.index.File)files.get(this.selectedRows[i]);
				if (file.getTransfer() != null)
					file.getTransfer().stop(this.queueManager);
				index.removeFile(file);
			}
			
			if(e.getSource() == this.insertFiles) {
				Index index = (Index)this.fileList;

				thaw.plugins.index.File file = index.getFile(this.selectedRows[i]);
				file.insertOnFreenet(this.queueManager);
			}
			
			
			if(e.getSource() == this.downloadFiles) {
				thaw.plugins.index.File file = this.fileList.getFile(this.selectedRows[i]);

				if (file == null) {
					Logger.notice(this, "File disappeared ?");
					continue;
				}					
				file.download(destination.getPath(), this.queueManager);
			}

			if(e.getSource() == this.copyFileKeys) {
				thaw.plugins.index.File file = this.fileList.getFile(this.selectedRows[i]);
				if(file.getPublicKey() != null)
					keys = keys + file.getPublicKey() + "\n";
			}

			if(e.getSource() == this.recalculateKeys) {
				thaw.plugins.index.File file = this.fileList.getFile(this.selectedRows[i]);

				file.recalculateCHK(this.queueManager);
			}

		}

		if(e.getSource() == this.copyFileKeys) {
			Toolkit tk = Toolkit.getDefaultToolkit();
			StringSelection st = new StringSelection(keys);
			Clipboard cp = tk.getSystemClipboard();
			cp.setContents(st, null);
		}
	}


	public void setSelectedRows(int min, int max) {
		this.table.setRowSelectionInterval(min, max);
	}
	

	public class FileListModel extends javax.swing.table.AbstractTableModel implements java.util.Observer {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public Vector columnNames;

		public Vector files = null; /* thaw.plugins.index.File Vector */

		public FileList fileList;

		public FileListModel() {
			super();

			this.columnNames = new Vector();

			this.columnNames.add(I18n.getMessage("thaw.common.file"));
			this.columnNames.add(I18n.getMessage("thaw.common.size"));
			
			if(FileTable.this.modifiables)
				this.columnNames.add(I18n.getMessage("thaw.common.localPath"));

			//columnNames.add(I18n.getMessage("thaw.plugin.index.category"));
			this.columnNames.add(I18n.getMessage("thaw.common.key"));
			this.columnNames.add(I18n.getMessage("thaw.common.status"));
		}

		public void reloadFileList(FileList newFileList) {
			if(this.fileList != null && (this.fileList instanceof Observable)) {
				((Observable)this.fileList).deleteObserver(this);
			}

			if(newFileList != null && (newFileList instanceof Observable)) {
				((Observable)newFileList).addObserver(this);
			}

			this.fileList = newFileList;

			if(this.files != null) {
				for(Iterator it = this.files.iterator();
				    it.hasNext(); ) {
					thaw.plugins.index.File file = (thaw.plugins.index.File)it.next();
					file.deleteObserver(this);
				}
			}

			this.files = null;
			
			if(this.fileList != null) {
				this.files = this.fileList.getFileList();
			}

			if(this.files != null) {
				for(Iterator it = this.files.iterator();
				    it.hasNext(); ) {
					thaw.plugins.index.File file = (thaw.plugins.index.File)it.next();
				}

			}	

			this.refresh();
		}

		public int getRowCount() {
			if(this.files == null)
				return 0;

			return this.files.size();
		}

		public int getColumnCount() {
			return this.columnNames.size();
		}

		public String getColumnName(int column) {
			return (String)this.columnNames.get(column);
		}

		public Object getValueAt(int row, int column) {
			thaw.plugins.index.File file = (thaw.plugins.index.File)this.files.get(row);

			if(column == 0)
				return file.getFilename();

			if(column == 1)
				return new Long(file.getSize());

			if(column == 2 && FileTable.this.modifiables)
				return file.getLocalPath();

			//if( (column == 2 && !modifiables)
			//    || (column == 3 && modifiables) )
			//	return file.getCategory();

			if( (column == 2 && !FileTable.this.modifiables)
			    || (column == 3 && FileTable.this.modifiables) )
				return file.getPublicKey();

			if( (column == 3 && !FileTable.this.modifiables)
			    || (column == 4 && FileTable.this.modifiables) ) {
				return file.getTransfer();
			}

			return null;
		}

		public void refresh() {
			if(this.fileList != null) {				
				this.files = this.fileList.getFileList();
			}

			TableModelEvent event = new TableModelEvent(this);
			this.refresh(event);
		}

		public void refresh(int row) {
			TableModelEvent event = new TableModelEvent(this, row);
			this.refresh(event);
		}

		public void refresh(TableModelEvent e) {
			this.fireTableChanged(e);
		}

		public void update(java.util.Observable o, Object param) {
			/*if(param instanceof thaw.plugins.index.File
			   && o instanceof thaw.plugins.index.Index) {

				thaw.plugins.index.File file = (thaw.plugins.index.File)param;
				
				file.deleteObserver(this);

				if (((Index)o).isInIndex(file))
					file.addObserver(this);
				
					}*/

			this.refresh(); /* TODO : Do it more nicely ... :) */
		}
	}


	private class FileRenderer extends DefaultTableCellRenderer {
		private final static long serialVersionUID = 20060821;
		
		public FileRenderer() {

		}

		public Component getTableCellRendererComponent(JTable table, Object value,
							       boolean isSelected, boolean hasFocus,
							       int row, int column) {

			if(value == null)
				return super.getTableCellRendererComponent(table, "",
									   isSelected, hasFocus, row, column);

			if(value instanceof FCPTransferQuery) {
				FCPTransferQuery query = (FCPTransferQuery)value;
				JProgressBar bar = new JProgressBar(0, 100);

				bar.setStringPainted(true);
				bar.setBorderPainted(false);
				
				if((query instanceof FCPClientPut && query.getProgression() > 0)
				   || (query instanceof FCPClientGet && query.getProgression() < 100) )
					bar.setValue(query.getProgression());
				else
					bar.setValue(query.getTransferWithTheNodeProgression());
				
				if(query.isFinished() && !query.isSuccessful())
					bar.setString(I18n.getMessage("thaw.common.failed"));

				if(query.isFinished() && query.isSuccessful())
					bar.setString(I18n.getMessage("thaw.common.ok"));

				if(!query.isFinished()) {
					/*if(query instanceof FCPClientGet)
						bar.setString(I18n.getMessage("thaw.common.downloading"));
					else
						bar.setString(I18n.getMessage("thaw.common.uploading"));
					*/
					bar.setString(query.getStatus());
				}

				return bar;
			}
			
			if(value instanceof Long) {
				return super.getTableCellRendererComponent(table,
									   thaw.plugins.queueWatcher.QueueTableModel.getPrintableSize(((Long)value).longValue()),
									   isSelected, hasFocus, row, column);
			}

			Component cell = super.getTableCellRendererComponent(table, value,
									     isSelected, hasFocus,
									     row, column);

			return cell;
		}

	}

}
