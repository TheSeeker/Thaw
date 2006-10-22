package thaw.plugins.index;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JScrollPane;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.event.TableModelListener;
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
import javax.swing.JMenu;
import javax.swing.JFileChooser;
import javax.swing.JProgressBar;
import javax.swing.JLabel;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;

import java.util.Vector;
import java.util.Iterator;

import java.util.Observable;
import java.util.Observer;

import javax.swing.event.TableModelListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.JTableHeader;


import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

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
	
		
		rightClickMenu = new JPopupMenu();
		
		if(modifiables) {
			removeFiles = new JMenuItem(I18n.getMessage("thaw.common.remove"));
			insertFiles = new JMenuItem(I18n.getMessage("thaw.plugin.index.insert"));
			recalculateKeys = new JMenuItem(I18n.getMessage("thaw.plugin.index.recalculateKeys"));
			removeFiles.addActionListener(this);
			insertFiles.addActionListener(this);
			recalculateKeys.addActionListener(this);
			rightClickMenu.add(removeFiles);
			rightClickMenu.add(insertFiles);
			rightClickMenu.add(recalculateKeys);
		} else {
			downloadFiles   = new JMenuItem(I18n.getMessage("thaw.common.action.download"));
			downloadFiles.addActionListener(this);
			rightClickMenu.add(downloadFiles);
		}

		copyFileKeys = new JMenuItem(I18n.getMessage("thaw.common.copyKeysToClipboard"));
		copyFileKeys.addActionListener(this);
		rightClickMenu.add(copyFileKeys);

		gotoIndex = new JMenuItem(I18n.getMessage("thaw.plugin.index.gotoIndex"));
		gotoIndex.addActionListener(this);
		gotoIndex.setEnabled(false);
		rightClickMenu.add(gotoIndex);

		fileListModel = new FileListModel();
		table = new JTable(fileListModel);
		table.setShowGrid(true);
		table.setDefaultRenderer( table.getColumnClass(0), new FileRenderer() );

		table.addMouseListener(this);

		JTableHeader header = table.getTableHeader();
		header.setUpdateTableInRealTime(true);
		header.setReorderingAllowed(true);

		panel = new JPanel();
		panel.setLayout(new BorderLayout());
		
		panel.add(new JLabel(I18n.getMessage("thaw.plugin.index.fileList")), BorderLayout.NORTH);
		panel.add(new JScrollPane(table));

	}


	public JPanel getPanel() {
		return panel;
	}

	
	public void setFileList(FileList fileList) {
		if(this.fileList != null) {
			this.fileList.unloadFiles();
		}
		
		if(fileList != null) {
			fileList.loadFiles(sortColumn, ascOrder);
		}

		this.fileList = fileList;

		fileListModel.reloadFileList(fileList);
	}


	public void mouseClicked(MouseEvent e) {
		if(e.getButton() == MouseEvent.BUTTON3
		   && fileList != null) {
			gotoIndex.setEnabled(!(fileList instanceof Index));

			selectedRows = table.getSelectedRows();
			rightClickMenu.show(e.getComponent(), e.getX(), e.getY());
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
		if(fileList == null)
			return;

		String keys = "";
		
		Vector files = null;

		java.io.File destination = null;

		if (e.getSource() == gotoIndex) {
			if (selectedRows.length <= 0)
				return;

			thaw.plugins.index.File file = (thaw.plugins.index.File)fileList.getFileList().get(selectedRows[0]);

			if (file.getParentId() == -1) {
				Logger.notice(this, "No parent ? abnormal");
				return;
			}

			Index parent = tree.getRoot().getIndex(file.getParentId());

			if (parent == null) {
				Logger.notice(this, "Cannot find again the parent ?! Id: "+Integer.toString(file.getParentId()));
				return;
			}
			
			tables.setList(parent);

			int row;
			
			row = parent.getFilePosition(file);

			if (row < 0)
				Logger.notice(this, "File not found in the index ?! Index : "+parent.getPublicKey()+" ; File: " +file.getPublicKey());
			else
				setSelectedRows(row, row);

			return;
		}

		if(e.getSource() == downloadFiles) {
			FileChooser fileChooser ;
			if (config.getValue("lastDestinationDirectory") == null)
				fileChooser = new FileChooser();
			else
				fileChooser = new FileChooser(config.getValue("lastDestinationDirectory"));
			fileChooser.setTitle(I18n.getMessage("thaw.plugin.fetch.destinationDirectory"));
			fileChooser.setDirectoryOnly(true);
			fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
			
			destination = fileChooser.askOneFile();

			if(destination == null)
				return;

			config.setValue("lastDestinationDirectory", destination.getPath());
		}

		if(e.getSource() == removeFiles) {
			files = fileList.getFileList();
		}

		for(int i = 0 ; i < selectedRows.length ; i++) {

			if(e.getSource() == removeFiles) {
				Index index = (Index)fileList;

				thaw.plugins.index.File file = (thaw.plugins.index.File)files.get(selectedRows[i]);
				if (file.getTransfer() != null)
					file.getTransfer().stop(queueManager);
				index.removeFile(file);
			}
			
			if(e.getSource() == insertFiles) {
				Index index = (Index)fileList;

				thaw.plugins.index.File file = index.getFile(selectedRows[i]);
				file.insertOnFreenet(queueManager);
			}
			
			
			if(e.getSource() == downloadFiles) {
				thaw.plugins.index.File file = fileList.getFile(selectedRows[i]);

				if (file == null) {
					Logger.notice(this, "File disappeared ?");
					continue;
				}					
				file.download(destination.getPath(), queueManager);
			}

			if(e.getSource() == copyFileKeys) {
				thaw.plugins.index.File file = fileList.getFile(selectedRows[i]);
				if(file.getPublicKey() != null)
					keys = keys + file.getPublicKey() + "\n";
			}

			if(e.getSource() == recalculateKeys) {
				thaw.plugins.index.File file = fileList.getFile(selectedRows[i]);

				file.recalculateCHK(queueManager);
			}

		}

		if(e.getSource() == copyFileKeys) {
			Toolkit tk = Toolkit.getDefaultToolkit();
			StringSelection st = new StringSelection(keys);
			Clipboard cp = tk.getSystemClipboard();
			cp.setContents(st, null);
		}
	}


	public void setSelectedRows(int min, int max) {
		table.setRowSelectionInterval(min, max);
	}
	

	public class FileListModel extends javax.swing.table.AbstractTableModel implements java.util.Observer {
		public Vector columnNames;

		public Vector files = null; /* thaw.plugins.index.File Vector */

		public FileList fileList;

		public FileListModel() {
			super();

			columnNames = new Vector();

			columnNames.add(I18n.getMessage("thaw.common.file"));
			columnNames.add(I18n.getMessage("thaw.common.size"));
			
			if(modifiables)
				columnNames.add(I18n.getMessage("thaw.common.localPath"));

			//columnNames.add(I18n.getMessage("thaw.plugin.index.category"));
			columnNames.add(I18n.getMessage("thaw.common.key"));
			columnNames.add(I18n.getMessage("thaw.common.status"));
		}

		public void reloadFileList(FileList newFileList) {
			if(fileList != null && (fileList instanceof Observable)) {
				((Observable)fileList).deleteObserver(this);
			}

			if(newFileList != null && (newFileList instanceof Observable)) {
				((Observable)newFileList).addObserver(this);
			}

			fileList = newFileList;

			if(files != null) {
				for(Iterator it = files.iterator();
				    it.hasNext(); ) {
					thaw.plugins.index.File file = (thaw.plugins.index.File)it.next();
					file.deleteObserver(this);
				}
			}

			files = null;
			
			if(fileList != null) {
				files = fileList.getFileList();
			}

			if(files != null) {
				for(Iterator it = files.iterator();
				    it.hasNext(); ) {
					thaw.plugins.index.File file = (thaw.plugins.index.File)it.next();
				}

			}	

			refresh();
		}

		public int getRowCount() {
			if(files == null)
				return 0;

			return files.size();
		}

		public int getColumnCount() {
			return columnNames.size();
		}

		public String getColumnName(int column) {
			return (String)columnNames.get(column);
		}

		public Object getValueAt(int row, int column) {
			thaw.plugins.index.File file = (thaw.plugins.index.File)files.get(row);

			if(column == 0)
				return file.getFilename();

			if(column == 1)
				return new Long(file.getSize());

			if(column == 2 && modifiables)
				return file.getLocalPath();

			//if( (column == 2 && !modifiables)
			//    || (column == 3 && modifiables) )
			//	return file.getCategory();

			if( (column == 2 && !modifiables)
			    || (column == 3 && modifiables) )
				return file.getPublicKey();

			if( (column == 3 && !modifiables)
			    || (column == 4 && modifiables) ) {
				return file.getTransfer();
			}

			return null;
		}

		public void refresh() {
			if(fileList != null) {				
				files = fileList.getFileList();
			}

			TableModelEvent event = new TableModelEvent(this);
			refresh(event);
		}

		public void refresh(int row) {
			TableModelEvent event = new TableModelEvent(this, row);
			refresh(event);
		}

		public void refresh(TableModelEvent e) {
			fireTableChanged(e);
		}

		public void update(java.util.Observable o, Object param) {
			/*if(param instanceof thaw.plugins.index.File
			   && o instanceof thaw.plugins.index.Index) {

				thaw.plugins.index.File file = (thaw.plugins.index.File)param;
				
				file.deleteObserver(this);

				if (((Index)o).isInIndex(file))
					file.addObserver(this);
				
					}*/

			refresh(); /* TODO : Do it more nicely ... :) */
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
