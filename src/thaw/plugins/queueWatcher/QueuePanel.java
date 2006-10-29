package thaw.plugins.queueWatcher;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JProgressBar;
import javax.swing.JFileChooser;
import javax.swing.SwingConstants;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.Vector;
import java.util.Iterator;

import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;

import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.JMenu;
import javax.swing.JRadioButtonMenuItem;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.ButtonGroup;
import javax.swing.table.JTableHeader;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;

import java.io.File;

import thaw.core.*;

import thaw.fcp.*;

public class QueuePanel implements MouseListener, ActionListener, KeyListener {
	private Core core;

	private JLabel label;

	private JTable table = null;
	private JScrollPane scrollPane = null;

	private JPanel panel;

	private QueueTableModel tableModel;
	private DetailPanel detailPanel;

	private JPopupMenu rightClickMenu;
	private JMenuItem clearFinishedItem;
	private JMenuItem removeItem;
	private JMenuItem cancelItem;
	private JMenuItem delayItem;
	private JMenuItem downloadItem;
	private JMenuItem forceRestartItem;
	private JMenuItem copyKeysItem;

	private JRadioButtonMenuItem[] priorityRadioButton;
	private JRadioButtonMenuItem unknowPriority;
	private ButtonGroup priorityGroup;
	private final int MIN_PRIORITY = 6;

	private int[] selectedRows;
	private Vector queries;

	private boolean insertionQueue = false;

	public QueuePanel(Core core, DetailPanel detailPanel,
			  boolean isForInsertionQueue) {

		this.insertionQueue = isForInsertionQueue;

		this.core = core;
		this.detailPanel = detailPanel;

		this.tableModel = new QueueTableModel(isForInsertionQueue, core.getQueueManager());

		this.table = new JTable(this.tableModel);

		this.table.setShowGrid(true);

		JTableHeader header = this.table.getTableHeader();
		header.setUpdateTableInRealTime(true);
		header.addMouseListener(this.tableModel.new ColumnListener(this.table));
		header.setReorderingAllowed(true);

		if(isForInsertionQueue) {
			this.label = new JLabel(I18n.getMessage("thaw.common.insertions"));
			this.label.setIcon(IconBox.insertions);
		} else {
			this.label = new JLabel(I18n.getMessage("thaw.common.downloads"));
			this.label.setIcon(IconBox.downloads);
		}

		this.label.setVerticalAlignment(SwingConstants.CENTER);

		this.panel = new JPanel();
		this.panel.setLayout(new BorderLayout());

		this.panel.add(this.label, BorderLayout.NORTH);
		this.scrollPane = new JScrollPane(this.table);
		this.panel.add(this.scrollPane, BorderLayout.CENTER);

		this.table.setDefaultRenderer( this.table.getColumnClass(0), new ProgressRenderer(this.table, this.tableModel, isForInsertionQueue) );

		this.tableModel.addTableModelListener(this.table);

		this.rightClickMenu = new JPopupMenu();
		this.clearFinishedItem = new JMenuItem(I18n.getMessage("thaw.common.clearFinished"));
		this.removeItem = new JMenuItem(I18n.getMessage("thaw.common.removeFromTheList"));
		this.cancelItem = new JMenuItem(I18n.getMessage("thaw.common.cancel"));
		this.delayItem = new JMenuItem(I18n.getMessage("thaw.common.delay"));
		this.downloadItem = new JMenuItem(I18n.getMessage("thaw.common.downloadLocally"));
		this.forceRestartItem = new JMenuItem(I18n.getMessage("thaw.common.forceRestart"));
		this.copyKeysItem = new JMenuItem(I18n.getMessage("thaw.common.copyKeysToClipboard"));
		JMenu priorityMenu = new JMenu(I18n.getMessage("thaw.common.priority"));

		this.priorityGroup = new ButtonGroup();
		this.priorityRadioButton = new JRadioButtonMenuItem[this.MIN_PRIORITY+1];
		for(int i =0 ; i <= this.MIN_PRIORITY ; i++) {
			this.priorityRadioButton[i] = new JRadioButtonMenuItem(I18n.getMessage("thaw.plugin.priority.p"+Integer.toString(i)));
			this.priorityRadioButton[i].addActionListener(this);
			priorityMenu.add(this.priorityRadioButton[i]);
			this.priorityGroup.add(this.priorityRadioButton[i]);
		}
		this.unknowPriority = new JRadioButtonMenuItem("Coin");
		this.priorityGroup.add(this.unknowPriority);

		this.rightClickMenu.add(this.clearFinishedItem);
		this.rightClickMenu.add(this.removeItem);

		if( Integer.parseInt(core.getConfig().getValue("maxSimultaneousDownloads")) >= 0
		   || Integer.parseInt(core.getConfig().getValue("maxSimultaneousInsertions")) >= 0)
			this.rightClickMenu.add(this.cancelItem);

		if( Integer.parseInt(core.getConfig().getValue("maxSimultaneousDownloads")) >= 0
		   || Integer.parseInt(core.getConfig().getValue("maxSimultaneousInsertions")) >= 0)
			this.rightClickMenu.add(this.delayItem);

		if(!isForInsertionQueue)
			this.rightClickMenu.add(this.downloadItem);

		this.rightClickMenu.add(this.forceRestartItem);
		this.rightClickMenu.add(this.copyKeysItem);

		if( Boolean.valueOf(core.getConfig().getValue("advancedMode")).booleanValue() == true) {
			this.rightClickMenu.add(priorityMenu);
		}

		this.clearFinishedItem.addActionListener(this);
		this.removeItem.addActionListener(this);
		this.cancelItem.addActionListener(this);
		this.copyKeysItem.addActionListener(this);
		this.forceRestartItem.addActionListener(this);
		this.delayItem.addActionListener(this);
		this.downloadItem.addActionListener(this);

		this.table.addMouseListener(this);
		this.table.addKeyListener(this);

		/* If a queue is already existing, we need to add it */
		if(core.getQueueManager() != null) {
			this.addToTable(core.getQueueManager().getRunningQueue());

			Vector[] pendingQueues = core.getQueueManager().getPendingQueues();
			for(int i = 0 ; i < pendingQueues.length ; i++) {
				this.addToTable(pendingQueues[i]);
			}
		}
	}


	private class ProgressRenderer extends DefaultTableCellRenderer {
		private final static long serialVersionUID = 20060709;

		private final Color SUCCESS = Color.GREEN;
		private final Color FAILURE = Color.RED;
		private final Color RUNNING = Color.ORANGE;
		private final Color PENDING = Color.WHITE;

		private QueueTableModel model = null;
		private JTable tabl = null;
		private boolean insertionQueue;

		public ProgressRenderer(JTable table, QueueTableModel model, boolean isForInsertion) {
			this.model = model;
			this.tabl = table;
			this.insertionQueue = isForInsertion;
		}

		public Component getTableCellRendererComponent(JTable table, Object value,
							       boolean isSelected, boolean hasFocus,
							       int row, int column) {

			if(value == null)
				return null;

			FCPTransferQuery query = this.model.getQuery(row);

			if(value instanceof Integer) {

				Integer progress = (Integer)value;
				JProgressBar bar = new JProgressBar(0, 100);

				bar.setStringPainted(true);
				bar.setBorderPainted(false);

				if(progress.intValue() >= 0) {
					bar.setValue(progress.intValue());

					String toAdd = "%";

					if(!query.isProgressionReliable())
						toAdd = toAdd + " [*]";

					bar.setString(progress.toString() + toAdd);
				} else {
					bar.setValue(100);
					bar.setString(I18n.getMessage("thaw.common.failed"));
				}


				return bar;
			}


			Component cell = super.getTableCellRendererComponent(table, value,
									     isSelected, hasFocus,
									     row, column);

			if(!isSelected) {

				if(query == null)
					return null;

				if(!query.isRunning() && !query.isFinished())
					cell.setBackground(this.PENDING);
				if(query.isFinished() && query.isSuccessful())
					cell.setBackground(this.SUCCESS);
				if(query.isFinished() && !query.isSuccessful())
					cell.setBackground(this.FAILURE);
				if(query.isRunning() && !query.isFinished())
					cell.setBackground(this.RUNNING);
			}


			return cell;
		}

	}


	public void reloadSelections() {
		this.selectedRows = this.table.getSelectedRows();

		if(this.selectedRows.length > 1 || this.selectedRows.length < 1) {
			this.resetPriorityRadioButtons();
		} else {
			FCPTransferQuery query = this.tableModel.getQuery(this.selectedRows[0]);
			this.priorityRadioButton[query.getFCPPriority()].setSelected(true);
		}
	}

	/**
	 * return a vector made of FCPTransferQueries.
	 * Doesn't refresh the selection !
	 */
	public Vector getSelectedQueries() {
		Vector queries = new Vector();
		Vector initialQueries = this.tableModel.getQueries();

		if(this.selectedRows == null)
			return queries;

		/* Create a separate vector to avoid collisions */
		for(int i = 0 ; i < this.selectedRows.length; i++) {
			queries.add(initialQueries.get(this.selectedRows[i]));
		}

		return queries;
	}


	public void resetTable() {
		this.tableModel.resetTable();
	}


	public void addToTable(FCPTransferQuery query) {
		if( (this.insertionQueue && query.getQueryType() == 2)
		    || (!this.insertionQueue && query.getQueryType() == 1)) {
			this.tableModel.addQuery(query);
		}
	}

	/**
	 * @param queries Vector of FCPTransferQuery only
	 */
	public synchronized void addToTable(Vector queries) {
		try {
			for(Iterator queryIt = queries.iterator();
			    queryIt.hasNext();) {

				FCPTransferQuery query = (FCPTransferQuery)queryIt.next();

				this.addToTable(query);
			}

		} catch(java.util.ConcurrentModificationException e) {
			Logger.notice(this, "Collision.");
		}
	}


	public void refreshDetailPanel() {
		int selected = this.table.getSelectedRow();


		if(selected != -1) {
			FCPTransferQuery query = this.tableModel.getQuery(selected);
			this.detailPanel.setQuery(query);
		}
	}

	private void resetPriorityRadioButtons() {
		this.unknowPriority.setSelected(true);
	}

	public JPanel getPanel() {
		return this.panel;
	}


	public JTable getTable() {
		return this.table;
	}


	private class ActionReplier implements Runnable, ClipboardOwner {
		ActionEvent e;
		Vector queries;

		public ActionReplier(ActionEvent e, Vector queries) {
			this.e = e;
			this.queries = queries;
		}

		public void run() {
			Toolkit tk = Toolkit.getDefaultToolkit();
			String keys = "";
			File dir = null;

			if(this.e.getSource() == QueuePanel.this.clearFinishedItem) {
				QueuePanel.this.removeAllFinishedTransfers();
				return;
			}

			if(this.e.getSource() == QueuePanel.this.downloadItem) {
				FileChooser fileChooser = new FileChooser();
				fileChooser.setTitle(I18n.getMessage("thaw.common.downloadLocally"));
				fileChooser.setDirectoryOnly(true);
				fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
				dir = fileChooser.askOneFile();

				if(dir == null)
					return;
			}

			int prioritySelected = 0;

			for(prioritySelected = 0;
			    prioritySelected <= QueuePanel.this.MIN_PRIORITY;
			    prioritySelected++) {
				if(QueuePanel.this.priorityRadioButton[prioritySelected] == this.e.getSource()) {
					break;
				}
			}

			if(prioritySelected > QueuePanel.this.MIN_PRIORITY)
				prioritySelected = -1;

			for(Iterator queryIt = this.queries.iterator() ; queryIt.hasNext() ;) {
				FCPTransferQuery query = (FCPTransferQuery)queryIt.next();

				if(query == null)
					continue;

				if(prioritySelected >= 0) {
					if(query.isPersistent()) {
						query.setFCPPriority(prioritySelected);
						query.updatePersistentRequest(false);
					}
				}

				if(this.e.getSource() == QueuePanel.this.removeItem) {

					if(query.stop(QueuePanel.this.core.getQueueManager())) {
						QueuePanel.this.core.getQueueManager().remove(query);
					}
				}

				if(this.e.getSource() == QueuePanel.this.cancelItem) {
					query.stop(QueuePanel.this.core.getQueueManager());
				}

				if(this.e.getSource() == QueuePanel.this.delayItem) {
					if(query.isRunning() && !query.isFinished()) {
						query.pause(QueuePanel.this.core.getQueueManager());
						QueuePanel.this.core.getQueueManager().moveFromRunningToPendingQueue(query);
					}
				}

				if(this.e.getSource() == QueuePanel.this.forceRestartItem) {
					query.stop(QueuePanel.this.core.getQueueManager());

					if(query.getMaxAttempt() >= 0)
						query.setAttempt(0);

					query.start(QueuePanel.this.core.getQueueManager());
				}

				if(this.e.getSource() == QueuePanel.this.copyKeysItem) {
					if(query.getFileKey() != null
					   && !"".equals( query.getFileKey() ))
						keys = keys + query.getFileKey() + "\n";
				}

				if(this.e.getSource() == QueuePanel.this.downloadItem
				   && dir != null) {
					if(query.isPersistent()) {

						query.saveFileTo(dir.getPath());
						if(query.getIdentifier().startsWith(QueuePanel.this.core.getConfig().getValue("thawId")))
							query.updatePersistentRequest(true);
					}
				}

			} /* for i in selectedRows */


			if(this.e.getSource() == QueuePanel.this.copyKeysItem) {
				StringSelection st = new StringSelection(keys);
				Clipboard cp = tk.getSystemClipboard();
				cp.setContents(st, this);
			}

		}

		public void lostOwnership(Clipboard clipboard, java.awt.datatransfer.Transferable contents) {
			/* we dont care */
		}

	}

	public void removeAllFinishedTransfers() {
		Vector queries = this.tableModel.getQueries();

		for(Iterator it = queries.iterator();
		    it.hasNext(); ) {
			FCPTransferQuery query = (FCPTransferQuery)it.next();
			if(query.isFinished()) {
				if(query.stop(this.core.getQueueManager())) {
					this.core.getQueueManager().remove(query);
				}
			}
		}
	}

	/**
	 * Manage it on a different thread to avoid UI freeze.
	 */
	public void actionPerformed(ActionEvent e) {
		Thread action = new Thread(new ActionReplier(e, this.getSelectedQueries()));

		action.start();
	}

	public void mouseClicked(MouseEvent e) {
		if(e.getButton() == MouseEvent.BUTTON3) {
			this.reloadSelections();
			this.queries = this.tableModel.getQueries();
			this.rightClickMenu.show(e.getComponent(), e.getX(), e.getY());
		}

		if(e.getButton() == MouseEvent.BUTTON1) {
			this.refreshDetailPanel();
		}
	}

	public void mouseEntered(MouseEvent e) { }

	public void mouseExited(MouseEvent e) { }

	public void mousePressed(MouseEvent e) { }

	public void mouseReleased(MouseEvent e) { }

	public void keyPressed(KeyEvent e) { }

	public void keyReleased(KeyEvent e) { this.refreshDetailPanel(); }

	public void keyTyped(KeyEvent e) { }
}

