package thaw.gui;


import javax.swing.table.TableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.event.TableColumnModelListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ChangeEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.JTable;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Component;
import javax.swing.ImageIcon;
import java.util.Vector;

import javax.swing.DefaultCellEditor;

import thaw.core.Logger;
import thaw.core.Config;
import thaw.core.I18n;

import thaw.fcp.FreenetURIHelper;
import thaw.fcp.FCPTransferQuery;
import thaw.fcp.FCPClientPut;
import thaw.fcp.FCPClientGet;


/**
 * Inherits from JTable:
 *  Just add to usual JTable the capacity to store the columns sizes
 * in the thaw configuration
 */
public class Table extends JTable implements TableColumnModelListener, Runnable {

	private Config config;
	private String configPrefix;

	public final static int TIME_BEFORE_SAVING = 500; /* in ms */
	private boolean hasChanged = false;
	private Thread savingThread;

	private boolean statusInProgressBars = true;

	public Table(Config config, String prefix) {
		super();
		setDefaultRenderer();
		setConfigPrefix(config, prefix);
		setAsListener();
	}


	public Table(Config config, String prefix, int numRows, int numColumns) {
		super(numRows, numColumns);
		setDefaultRenderer();
		setConfigPrefix(config, prefix);
		setAsListener();
	}


	public Table(Config config, String prefix, Object[][] data, Object[] columnNames) {
		super(data, columnNames);
		setDefaultRenderer();
		setConfigPrefix(config, prefix);
		setAsListener();
	}


	public Table(Config config, String prefix, TableModel model) {
		super(model);
		setDefaultRenderer();
		setConfigPrefix(config, prefix);
		setAsListener();
	}

	public Table(Config config, String prefix,
		     TableModel model, TableColumnModel cModel) {
		super(model, cModel);
		setDefaultRenderer();
		setConfigPrefix(config, prefix);
		setAsListener();
	}

	public Table(Config config, String prefix,
		     TableModel model, TableColumnModel cModel,
		     ListSelectionModel lModel) {
		super(model, cModel, lModel);
		setDefaultRenderer();
		setConfigPrefix(config, prefix);
		setAsListener();
	}


	public Table(Config config, String prefix, Vector data, Vector columns) {
		super(data, columns);
		setDefaultRenderer();
		setConfigPrefix(config, prefix);
		setAsListener();
	}

	private DefaultRenderer renderer;

	public void specifyColumnWithKeys(int c) {
		renderer.specifyColumnWithKeys(c);
	}

	public void showStatusInProgressBars(boolean v) {
		renderer.showStatusInProgressBars(v);
	}

	public void setDefaultRenderer() {
		renderer = new DefaultRenderer();
		setDefaultRenderer(getColumnClass(0), renderer);

		((DefaultTableCellRenderer)getTableHeader().getDefaultRenderer()).setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
	}



	/**
	 * @param prefix prefix used in the configuration file to make the diff with
	 *        with the other tables
	 */
	public void setConfigPrefix(Config config, String prefix) {
		this.configPrefix = prefix;
		this.config = config;
		loadColumnSizes();
	}


	public void setColumnModel(TableColumnModel m) {
		super.setColumnModel(m);
		loadColumnSizes();
	}


	public void setModel(TableModel m) {
		super.setModel(m);

		if (config != null)
			loadColumnSizes();
	}


	protected void setAsListener() {
		super.getColumnModel().addColumnModelListener(this);
	}


	public static class DefaultRenderer extends DefaultTableCellRenderer {
		private final static long serialVersionUID = 20060821;

		private boolean statusInProgressBars = true;
		private int columnWithKeys = -1;

		private Color softGray;


		public DefaultRenderer() {
			softGray = new Color(240,240,240);
		}

		public void showStatusInProgressBars(boolean v) {
			statusInProgressBars = v;
		}

		public void specifyColumnWithKeys(int c) {
			columnWithKeys = c;
		}

		public Component getTableCellRendererComponent(final JTable table, Object value,
							       final boolean isSelected, final boolean hasFocus,
							       final int row, final int column) {

			if (value == null)
				value = "";

			if (value instanceof FCPTransferQuery) {
				final FCPTransferQuery query = (FCPTransferQuery)value;
				final JProgressBar bar = new TransferProgressBar(query, statusInProgressBars);

				return bar;
			}

			Component cell;

			if (value instanceof ImageIcon) {
				return new JLabel(((ImageIcon)value));
			} if (value instanceof JPanel) {
				cell = (Component)value;
			} else if(value instanceof Long) {

				cell = super.getTableCellRendererComponent(table,
									   thaw.gui.GUIHelper.getPrintableSize(((Long)value).longValue()),
									   isSelected, hasFocus, row, column);

			} else if (value instanceof String && ((String)value).indexOf("\n") >= 0) {
				JTextArea area = new JTextArea((String)value);
				area.setEditable(false);
				area.setLineWrap(true);
				area.setWrapStyleWord(true);

				if (table.getRowHeight(row) < area.getPreferredSize().getHeight())
					table.setRowHeight((int)area.getPreferredSize().getHeight());

				cell = area;

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

			cell.setForeground(Color.BLACK);


			if (column == columnWithKeys && value instanceof String) {
				String key = (String)value;
				if (FreenetURIHelper.isObsolete(key))
					cell.setForeground(Color.RED);
			}

			return cell;
		}

	}




	public void columnAdded(TableColumnModelEvent e) {
		super.resizeAndRepaint();
	}

	public void columnMarginChanged(ChangeEvent e) {
		/* I don't know why I must call this function, but if I don't,
		 * the display is not refreshed as it should
		 */
		super.resizeAndRepaint();
		saveColumnSizes();
	}

	public void columnMoved(TableColumnModelEvent e) {
		super.resizeAndRepaint();
	}

	public void columnRemoved(TableColumnModelEvent e) {
		super.resizeAndRepaint();
	}

	public void columnSelectionChanged(ListSelectionEvent e) {
		super.resizeAndRepaint();
	}






	public void loadColumnSizes() {
		TableColumnModel m = super.getColumnModel();

		int nmb = m.getColumnCount();

		for (int i = 0 ; i < nmb ; i++) {
			TableColumn c = m.getColumn(i);
			String size = config.getValue(configPrefix+"_col_width_"+Integer.toString(i));

			if (size != null && !("".equals(size))) {
				c.setPreferredWidth(Integer.parseInt(size));
			}
		}
	}


	public void saveColumnSizes() {
		hasChanged = true;

		if (savingThread == null) {
			savingThread = new Thread(this);
			savingThread.start();
		}
	}


	public void run() {
		do {
			hasChanged = false;

			try {
				Thread.sleep(TIME_BEFORE_SAVING);
			} catch(java.lang.InterruptedException e) {
				/* \_o< */
			}

			if (!hasChanged) {
				savingThread = null;
				reallySaveColumnSize();
				return;
			}
		} while(hasChanged);

	}


	public void reallySaveColumnSize() {
		TableColumnModel m = super.getColumnModel();

		int nmb = m.getColumnCount();

		for (int i = 0 ; i < nmb ; i++) {
			TableColumn c = m.getColumn(i);
			config.setValue(configPrefix+"_col_width_"+Integer.toString(i),
					Integer.toString(c.getWidth()));
		}
	}
}
