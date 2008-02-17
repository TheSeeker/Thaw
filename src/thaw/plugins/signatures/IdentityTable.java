package thaw.plugins.signatures;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Observable;
import java.util.Vector;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import thaw.core.Config;
import thaw.core.I18n;
import thaw.gui.Table;

public class IdentityTable extends Observable implements MouseListener {
	private static final long serialVersionUID = -6972180330110075151L;
	
	private thaw.gui.Table table;
	
	private IdentityModel model;
	private IdentityRenderer renderer;

	public IdentityTable(Config config, String prefix, boolean showDup) {
		model = new IdentityModel(showDup);
		table = new Table(config, prefix, model);
		table.setDefaultRenderer(table.getColumnClass(0), renderer = new IdentityRenderer(model));
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.addMouseListener(this);
	}
	
	public void setModel(IdentityModel model) {
		this.model = model;
		table.setModel(model);
		renderer.setModel(model);
	}
	
	public static class IdentityModel extends javax.swing.table.AbstractTableModel {
		private static final long serialVersionUID = -7614528570324908651L;

		public static String[] columnNames = {
			I18n.getMessage("thaw.plugin.signature.nickname"),
			I18n.getMessage("thaw.plugin.signature.trustLevel"),
			I18n.getMessage("thaw.plugin.signature.isDup")
		};

		private Vector identities;
		
		private boolean showDup;

		public IdentityModel(boolean showDup) {
			this.showDup = showDup;
		}

		public void setIdentities(Vector i) {
			identities = i;

			final TableModelEvent event = new TableModelEvent(this);
			fireTableChanged(event);
		}
		
		public Vector getIdentities() {
			return identities;
		}

		public int getRowCount() {
			if (identities == null)
				return 0;

			return identities.size();
		}

		public int getColumnCount() {
			if (showDup)
				return columnNames.length;
			return columnNames.length-1;
		}

		public String getColumnName(final int column) {
			return columnNames[column];
		}

		public Object getValueAt(int row, int column) {
			if (identities == null)
				return null;

			if (column == 0)
				return ((Identity)identities.get(row)).toString();

			if (column == 1)
				return ((Identity)identities.get(row)).getTrustLevelStr();

			return null;
		}

		public Identity getIdentity(int line) {
			return (Identity)identities.get(line);
		}
	}


	
	public static class IdentityRenderer extends thaw.gui.Table.DefaultRenderer {
		private static final long serialVersionUID = 5405210731032136559L;
		private IdentityModel model;

		public IdentityRenderer(IdentityModel model) {
			super();
			setModel(model);
		}
		
		public void setModel(IdentityModel model) {
			this.model = model;
		}

		public java.awt.Component getTableCellRendererComponent(final JTable table, Object value,
									final boolean isSelected, final boolean hasFocus,
									final int row, final int column) {

			if (value instanceof String
			    && "X".equals(value)) {
				value = thaw.gui.IconBox.minClose;
			}

			java.awt.Component c = super.getTableCellRendererComponent(table, value,
										   isSelected, hasFocus,
										   row, column);
			Identity i = model.getIdentity(row);

			c.setForeground(i.getTrustLevelColor());

			return c;
		}

	}

	public void setIdentities(Vector ids) {
		model.setIdentities(ids);
	}
	
	public thaw.gui.Table getTable() {
		return table;
	}
	
	public Identity getIdentity(int row) {
		return model.getIdentity(row);
	}
	
	public Vector getIdentities() {
		return model.getIdentities();
	}


	public void mouseClicked(MouseEvent arg0) {
		setChanged();
		notifyObservers(model.getIdentity(table.getSelectedRow()));
	}



	public void mouseEntered(MouseEvent arg0) {
	}



	public void mouseExited(MouseEvent arg0) {
	}



	public void mousePressed(MouseEvent arg0) {
	}



	public void mouseReleased(MouseEvent arg0) {
	}


}
