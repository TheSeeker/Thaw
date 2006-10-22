package thaw.plugins.index;


import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JButton;

import javax.swing.JScrollPane;

import thaw.plugins.Hsqldb;
import thaw.core.I18n;
import thaw.core.Logger;

/**
 * Class helping to display a dialog allowing the user to choose an index. (For example, it's used when you want
 * to make a link to another index.
 *
 * <pre>
 * Window
 * *-------------------*------------------*
 * | Local indexes     | Other indexes    |
 * |  |                |  |               |
 * |  |-- Cat1         |  |-- Cat1        |
 * |  |    |-- Index1  |  |    |-- Index1 |
 * |  |    |-- Index2  |  |    |-- Index2 | 
 * |  ...              |  ...             |
 * |                   |                  |
 * *-------------------*------------------*
 * | Specific index key                   |
 * *-------------------*------------------*
 * | Cancel button     |  Ok button       |
 * *-------------------*------------------*
 * </pre>
 */
public class IndexSelecter implements java.awt.event.ActionListener, java.util.Observer {
	private JFrame frame;

	private JPanel upPanel;

	private JPanel indexPanel;
	private IndexTree localIndexes;
	private IndexTree otherIndexes;
	
	private JPanel fieldPanel;
	private JTextField keyField;
	
	private JPanel downPanel;
	private JButton cancelButton;
	private JButton okButton;

	private boolean closeWin;
	private String selectedIndexKey = null;

	public IndexSelecter() {

	}

	/**
	 * Returned null if canceled. Is blocking !
	 */
	public String askForAnIndexURI(Hsqldb db) {
		this.frame = new JFrame(I18n.getMessage("thaw.plugin.index.selectIndex"));

		this.frame.setVisible(false);

		this.upPanel = new JPanel();
		Logger.info(this, "indexes");
		this.localIndexes = new IndexTree(I18n.getMessage("thaw.plugin.index.yourIndexes"), true, true, null, db);
		this.otherIndexes = new IndexTree(I18n.getMessage("thaw.plugin.index.indexes"), false, true, null, db);
		Logger.info(this, "plus indexes");

		this.fieldPanel = new JPanel();
		this.keyField = new JTextField("");

		this.downPanel = new JPanel();
		this.cancelButton = new JButton(I18n.getMessage("thaw.common.cancel"));
		this.okButton = new JButton(I18n.getMessage("thaw.common.ok"));

		this.upPanel.setLayout(new BorderLayout());
		this.downPanel.setLayout(new GridLayout(1, 2));
		this.fieldPanel.setLayout(new BorderLayout());

		this.indexPanel = new JPanel();
		this.indexPanel.setLayout(new GridLayout(1, 2));
		this.indexPanel.add(new JScrollPane(this.localIndexes.getPanel()));
		this.indexPanel.add(new JScrollPane(this.otherIndexes.getPanel()));

		this.upPanel.add(this.indexPanel, BorderLayout.CENTER);
		
		this.fieldPanel.add(new JLabel(I18n.getMessage("thaw.plugin.index.indexKey")), BorderLayout.WEST);
		this.fieldPanel.add(this.keyField, BorderLayout.CENTER);
		this.upPanel.add(this.fieldPanel, BorderLayout.SOUTH);

		this.downPanel.add(this.okButton);
		this.downPanel.add(this.cancelButton);

		this.frame.getContentPane().setLayout(new BorderLayout(10, 10));
		this.frame.getContentPane().add(this.upPanel, BorderLayout.CENTER);
		this.frame.getContentPane().add(this.downPanel, BorderLayout.SOUTH);

		this.frame.setSize(500, 400);

		this.cancelButton.addActionListener(this);
		this.okButton.addActionListener(this);
		this.localIndexes.addObserver(this);
		this.otherIndexes.addObserver(this);

		this.frame.setVisible(true);

		for (this.closeWin = false ; !this.closeWin ; ) {
			try {
				Thread.sleep(500);
			} catch(java.lang.InterruptedException e) {
				/* \_o< \_o< \_o< */
			}
		}

		this.frame.setVisible(false);

		this.frame = null;

		this.upPanel = null;
		this.localIndexes = null;
		this.otherIndexes = null;
		
		this.fieldPanel = null;
		this.keyField = null;
		
		this.downPanel = null;
		this.cancelButton = null;
		this.okButton = null;

		return this.selectedIndexKey;
	}


	public void update(java.util.Observable o, Object param) {
		if (param instanceof Index) {
			Index index = (Index)param;
			this.selectedIndexKey = index.getPublicKey();

			Logger.info(this, "Selected index key: "+this.selectedIndexKey);
			this.keyField.setText(this.selectedIndexKey);
		}
	}

	public void actionPerformed(java.awt.event.ActionEvent e) {
		if (e.getSource() == this.okButton) {
			if (this.keyField.getText() == null || "".equals( this.keyField.getText() ))
				this.selectedIndexKey = null;
			else
				this.selectedIndexKey = this.keyField.getText();
			this.closeWin = true;
		}

		if (e.getSource() == this.cancelButton) {
			this.selectedIndexKey = null;
			this.closeWin = true;
		}

		this.localIndexes.deleteObserver(this);
		this.otherIndexes.deleteObserver(this);
	}

}
