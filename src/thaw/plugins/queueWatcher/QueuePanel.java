package thaw.plugins.queueWatcher;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

import thaw.core.Core;
import thaw.core.FileChooser;
import thaw.core.I18n;
import thaw.gui.IconBox;
import thaw.core.Logger;
import thaw.fcp.FCPClientGet;
import thaw.fcp.FCPTransferQuery;
import thaw.plugins.QueueWatcher;


public class QueuePanel implements MouseListener, ActionListener, KeyListener {
	public final static int ACTION_REMOVE_FINISHED          = 0;
	public final static int ACTION_DOWNLOAD_SELECTED        = 1; /* locally */
	public final static int ACTION_REMOVE_SELECTED          = 2;
	public final static int ACTION_CANCEL_SELECTED          = 3;
	public final static int ACTION_DELAY_SELECTED           = 4;
	public final static int ACTION_RESTART_SELECTED         = 5;
	public final static int ACTION_COPY_KEYS_SELECTED       = 6;
	public final static int ACTION_CHANGE_PRIORITY_SELECTED = 7;

	private Core core;

	private JButton button;

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

	public QueueWatcher queueWatcher;

	public QueuePanel(final Core core, final QueueWatcher queueWatcher,
			  final DetailPanel detailPanel,
			  boolean isForInsertionQueue) {

		this.queueWatcher = queueWatcher;

		insertionQueue = isForInsertionQueue;

		this.core = core;
		this.detailPanel = detailPanel;

		tableModel = new QueueTableModel(isForInsertionQueue, core.getQueueManager());

		table = new JTable(tableModel);

		table.setShowGrid(true);

		final JTableHeader header = table.getTableHeader();
		header.setUpdateTableInRealTime(true);
		header.addMouseListener(tableModel.new ColumnListener(table));
		header.setReorderingAllowed(true);

		if(isForInsertionQueue) {
			button = new JButton(I18n.getMessage("thaw.common.insertions"));
			button.setIcon(IconBox.insertions);
		} else {
			button = new JButton(I18n.getMessage("thaw.common.downloads"));
			button.setIcon(IconBox.downloads);
		}

		button.setVerticalAlignment(SwingConstants.CENTER);
		button.setHorizontalAlignment(SwingConstants.LEFT);
		button.setPreferredSize(new Dimension(190, 40));

		final JPanel buttonPanel = new JPanel(new BorderLayout());
		buttonPanel.add(button, BorderLayout.EAST);
		buttonPanel.add(new JLabel(""), BorderLayout.CENTER);

		panel = new JPanel();
		panel.setLayout(new BorderLayout());

		panel.add(buttonPanel, BorderLayout.NORTH);

		scrollPane = new JScrollPane(table);
		panel.add(scrollPane, BorderLayout.CENTER);

		table.setDefaultRenderer( table.getColumnClass(0), new ProgressRenderer(table, tableModel, isForInsertionQueue) );

		tableModel.addTableModelListener(table);

		rightClickMenu = new JPopupMenu();
		clearFinishedItem = new JMenuItem(I18n.getMessage("thaw.common.clearFinished"));
		removeItem = new JMenuItem(I18n.getMessage("thaw.common.removeFromTheList"));
		cancelItem = new JMenuItem(I18n.getMessage("thaw.common.cancel"));
		delayItem = new JMenuItem(I18n.getMessage("thaw.common.delay"));
		downloadItem = new JMenuItem(I18n.getMessage("thaw.common.downloadLocally"));
		forceRestartItem = new JMenuItem(I18n.getMessage("thaw.common.forceRestart"));
		copyKeysItem = new JMenuItem(I18n.getMessage("thaw.common.copyKeysToClipboard"));
		final JMenu priorityMenu = new JMenu(I18n.getMessage("thaw.common.priority"));

		priorityGroup = new ButtonGroup();
		priorityRadioButton = new JRadioButtonMenuItem[MIN_PRIORITY+1];
		for(int i =0 ; i <= MIN_PRIORITY ; i++) {
			priorityRadioButton[i] = new JRadioButtonMenuItem(I18n.getMessage("thaw.plugin.priority.p"+Integer.toString(i)));
			priorityRadioButton[i].addActionListener(this);
			priorityMenu.add(priorityRadioButton[i]);
			priorityGroup.add(priorityRadioButton[i]);
		}
		unknowPriority = new JRadioButtonMenuItem("Coin");
		priorityGroup.add(unknowPriority);

		rightClickMenu.add(clearFinishedItem);
		rightClickMenu.add(removeItem);

		if( (Integer.parseInt(core.getConfig().getValue("maxSimultaneousDownloads")) >= 0)
		   || (Integer.parseInt(core.getConfig().getValue("maxSimultaneousInsertions")) >= 0))
			rightClickMenu.add(cancelItem);

		if( (Integer.parseInt(core.getConfig().getValue("maxSimultaneousDownloads")) >= 0)
		   || (Integer.parseInt(core.getConfig().getValue("maxSimultaneousInsertions")) >= 0))
			rightClickMenu.add(delayItem);

		/* this option can't work with localSocket == true */
		if(!isForInsertionQueue && !Boolean.valueOf(core.getConfig().getValue("sameComputer")).booleanValue())
			rightClickMenu.add(downloadItem);

		rightClickMenu.add(forceRestartItem);
		rightClickMenu.add(copyKeysItem);

		if( Boolean.valueOf(core.getConfig().getValue("advancedMode")).booleanValue() == true) {
			rightClickMenu.add(priorityMenu);
		}

		clearFinishedItem.addActionListener(this);
		removeItem.addActionListener(this);
		cancelItem.addActionListener(this);
		copyKeysItem.addActionListener(this);
		forceRestartItem.addActionListener(this);
		delayItem.addActionListener(this);
		downloadItem.addActionListener(this);

		table.addMouseListener(this);
		table.addKeyListener(this);

		/* If a queue is already existing, we need to add it */
		if(core.getQueueManager() != null) {
			this.addToTable(core.getQueueManager().getRunningQueue());

			final Vector[] pendingQueues = core.getQueueManager().getPendingQueues();
			for(int i = 0 ; i < pendingQueues.length ; i++) {
				this.addToTable(pendingQueues[i]);
			}
		}
	}

	public void addMenuItem(final JMenuItem item) {
		rightClickMenu.insert(item, 0);
	}

	public JButton getButton() {
		return button;
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

		public ProgressRenderer(final JTable table, final QueueTableModel model, final boolean isForInsertion) {
			this.model = model;
			tabl = table;
			insertionQueue = isForInsertion;
		}

		public Component getTableCellRendererComponent(final JTable table, final Object value,
							       boolean isSelected, final boolean hasFocus,
							       final int row, final int column) {

			if(value == null)
				return null;

			final FCPTransferQuery query = model.getQuery(row);

			if(value instanceof Integer) {

				final Integer progress = (Integer)value;
				final JProgressBar bar = new JProgressBar(0, 100);

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


			final Component cell = super.getTableCellRendererComponent(table, value,
									     isSelected, hasFocus,
									     row, column);

			if(!isSelected) {

				if(query == null)
					return null;

				if(!query.isRunning() && !query.isFinished())
					cell.setBackground(PENDING);

				if(query.isFinished() && query.isSuccessful() &&
				   ( (!(query instanceof FCPClientGet)) || ((FCPClientGet)query).isWritingSuccessful()))
					cell.setBackground(SUCCESS);

				if(query.isFinished() && (!query.isSuccessful() ||
							  ((query instanceof FCPClientGet) && !((FCPClientGet)query).isWritingSuccessful()) ) )
					cell.setBackground(FAILURE);

				if(query.isRunning() && !query.isFinished())
					cell.setBackground(RUNNING);
			}


			return cell;
		}

	}


	public void reloadSelections() {
		selectedRows = table.getSelectedRows();

		if((selectedRows.length > 1) || (selectedRows.length < 1)) {
			resetPriorityRadioButtons();
		} else {
			final FCPTransferQuery query = tableModel.getQuery(selectedRows[0]);

			if (query == null)
				return;

			priorityRadioButton[query.getFCPPriority()].setSelected(true);
		}
	}

	/**
	 * return a vector made of FCPTransferQueries.
	 * Doesn't refresh the selection !
	 */
	public Vector getSelectedQueries() {
		final Vector queries = new Vector();
		final Vector initialQueries = tableModel.getQueries();

		if(selectedRows == null)
			return queries;

		/* Create a separate vector to avoid collisions */
		for(int i = 0 ; i < selectedRows.length; i++) {
			if (initialQueries.size() > selectedRows[i])
				queries.add(initialQueries.get(selectedRows[i]));
		}

		return queries;
	}


	public void resetTable() {
		tableModel.resetTable();
	}


	public void addToTable(final FCPTransferQuery query) {
		if( (insertionQueue && (query.getQueryType() == 2))
		    || (!insertionQueue && (query.getQueryType() == 1))) {
			tableModel.addQuery(query);
		}
	}

	/**
	 * @param queries Vector of FCPTransferQuery only
	 */
	public synchronized void addToTable(final Vector queries) {
		try {
			for(final Iterator queryIt = queries.iterator();
			    queryIt.hasNext();) {

				final FCPTransferQuery query = (FCPTransferQuery)queryIt.next();

				this.addToTable(query);
			}

		} catch(final java.util.ConcurrentModificationException e) {
			Logger.notice(this, "Collision.");
		}
	}


	public void refreshDetailPanel() {
		final int selected = table.getSelectedRow();


		if(selected != -1) {
			final FCPTransferQuery query = tableModel.getQuery(selected);
			detailPanel.setQuery(query);
		}
	}

	private void resetPriorityRadioButtons() {
		unknowPriority.setSelected(true);
	}

	public JPanel getPanel() {
		return panel;
	}


	public JTable getTable() {
		return table;
	}


	public void unselectAll() {
		table.clearSelection();
		selectedRows = null;
	}


	public class ActionReplier implements Runnable {
		private int action;
		private int new_priority;
		private Vector queries;

		public ActionReplier(final int action, final int new_priority) {
			this.action = action;

			if (selectedRows != null)
				this.queries = getSelectedQueries();
			else
				this.queries = new Vector();

			this.new_priority = (action == ACTION_CHANGE_PRIORITY_SELECTED) ? new_priority : -1;
		}

		public void run() {
			String keys = "";
			File dir = null;

			if(action == ACTION_REMOVE_FINISHED) {
				final Vector qs = tableModel.getQueries();

				for(final Iterator it = qs.iterator();
				    it.hasNext(); ) {
					final FCPTransferQuery query = (FCPTransferQuery)it.next();
					if(query.isFinished() &&
					   (!(query instanceof FCPClientGet) || (!query.isSuccessful() || ((FCPClientGet)query).isWritingSuccessful()))) {
						if(query.stop(core.getQueueManager())) {
							core.getQueueManager().remove(query);
						}
					}
				}

				return;
			}

			if(action == ACTION_DOWNLOAD_SELECTED) {
				final FileChooser fileChooser = new FileChooser();
				fileChooser.setTitle(I18n.getMessage("thaw.common.downloadLocally"));
				fileChooser.setDirectoryOnly(true);
				fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
				dir = fileChooser.askOneFile();

				if(dir == null)
					return;
			}

			for(final Iterator queryIt = queries.iterator() ; queryIt.hasNext() ;) {
				final FCPTransferQuery query = (FCPTransferQuery)queryIt.next();

				if(query == null)
					continue;

				if(new_priority >= 0) {
					if(query.isPersistent()) {
						query.setFCPPriority(new_priority);
						query.updatePersistentRequest(false);
					}
				}

				if(action == ACTION_REMOVE_SELECTED) {

					if(query.stop(core.getQueueManager())) {
						core.getQueueManager().remove(query);
					}
				}

				if(action == ACTION_CANCEL_SELECTED) {
					query.stop(core.getQueueManager());
				}

				if(action == ACTION_DELAY_SELECTED) {
					if(query.isRunning() && !query.isFinished()) {
						query.pause(core.getQueueManager());
						core.getQueueManager().moveFromRunningToPendingQueue(query);
					}
				}

				if(action == ACTION_RESTART_SELECTED) {
					query.stop(core.getQueueManager());

					if ((query instanceof FCPClientGet) && query.getPath() != null) {
						File target = new File(query.getPath());

						if (target.exists())
							target.delete();
					}

					if(query.getMaxAttempt() >= 0)
						query.setAttempt(0);

					query.start(core.getQueueManager());
				}

				if (action == ACTION_COPY_KEYS_SELECTED) {
					if((query.getFileKey() != null)
					   && !"".equals( query.getFileKey() ))
						keys = keys + query.getFileKey() + "\n";
				}

				if (action == ACTION_DOWNLOAD_SELECTED
				   && (dir != null)) {
					if(query.isPersistent()) {

						query.saveFileTo(dir.getPath());
						if(query.getIdentifier().startsWith(core.getConfig().getValue("thawId")))
							query.updatePersistentRequest(true);
					}
				}

			} /* for i in selectedRows */


			if (action == ACTION_COPY_KEYS_SELECTED) {
				thaw.gui.GUIHelper.copyToClipboard(keys);
			}

		}

	}


	public void removeSelectedTransfers() {
		reloadSelections();
		Thread th = new Thread(new ActionReplier(ACTION_REMOVE_SELECTED, -1));
		th.start();
	}

	public void removeAllFinishedTransfers() {
		reloadSelections();
		Thread th = new Thread(new ActionReplier(ACTION_REMOVE_FINISHED, -1));
		th.start();
	}

	/**
	 * Manage it on a different thread to avoid UI freeze.
	 */
	public void actionPerformed(final ActionEvent e) {
		int prioritySelected = 0;
		int action = ACTION_CHANGE_PRIORITY_SELECTED;

		for(prioritySelected = 0;
		    prioritySelected <= MIN_PRIORITY;
		    prioritySelected++) {
			if(priorityRadioButton[prioritySelected] == e.getSource()) {
				break;
			}
		}

		if(prioritySelected > MIN_PRIORITY) {
			prioritySelected = -1;

			if      (e.getSource() == clearFinishedItem) action = ACTION_REMOVE_FINISHED;
			else if (e.getSource() == removeItem)        action = ACTION_REMOVE_SELECTED;
			else if (e.getSource() == cancelItem)        action = ACTION_CANCEL_SELECTED;
			else if (e.getSource() == delayItem)         action = ACTION_DELAY_SELECTED;
			else if (e.getSource() == downloadItem)      action = ACTION_DOWNLOAD_SELECTED;
			else if (e.getSource() == forceRestartItem)  action = ACTION_RESTART_SELECTED;
			else if (e.getSource() == copyKeysItem)      action = ACTION_COPY_KEYS_SELECTED;
			else {
				Logger.error(this, "Unknow action ?!");
				return;
			}
		}

		final Thread actionTh = new Thread(new ActionReplier(action, prioritySelected));
		actionTh.start();
	}

	public void mouseClicked(final MouseEvent e) {
		if(e.getButton() == MouseEvent.BUTTON3) {
			reloadSelections();
			queries = tableModel.getQueries();
			rightClickMenu.show(e.getComponent(), e.getX(), e.getY());
		}

		if(e.getButton() == MouseEvent.BUTTON1) {
			queueWatcher.unselectAllExcept(insertionQueue ? QueueWatcher.INSERTION_PANEL : QueueWatcher.DOWNLOAD_PANEL);
			refreshDetailPanel();
		}
	}

	public void mouseEntered(final MouseEvent e) { }

	public void mouseExited(final MouseEvent e) { }

	public void mousePressed(final MouseEvent e) { }

	public void mouseReleased(final MouseEvent e) { }

	public void keyPressed(final KeyEvent e) { }

	public void keyReleased(final KeyEvent e) { refreshDetailPanel(); }

	public void keyTyped(final KeyEvent e) { }
}

