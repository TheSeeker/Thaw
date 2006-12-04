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
 * *--------------------------------------*
 * | Local indexes                        |
 * |  |                                   |
 * |  |-- Cat1                            |
 * |  |    |-- Index1                     |
 * |  |    |-- Index2                     |
 * |  ...                                 |
 * |                                      |
 * *--------------------------------------*
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
	private IndexTree indexTree;

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
		frame = new JFrame(I18n.getMessage("thaw.plugin.index.selectIndex"));

		frame.setVisible(false);

		upPanel = new JPanel();
		Logger.info(this, "indexes");
		indexTree = new IndexTree(I18n.getMessage("thaw.plugin.index.yourIndexes"), true, null, null, null, db);
		Logger.info(this, "plus indexes");

		fieldPanel = new JPanel();
		keyField = new JTextField("");

		downPanel = new JPanel();
		cancelButton = new JButton(I18n.getMessage("thaw.common.cancel"));
		okButton = new JButton(I18n.getMessage("thaw.common.ok"));

		upPanel.setLayout(new BorderLayout());
		downPanel.setLayout(new GridLayout(1, 2));
		fieldPanel.setLayout(new BorderLayout());

		indexPanel = new JPanel();
		indexPanel.setLayout(new GridLayout(1, 2));
		indexPanel.add(new JScrollPane(indexTree.getPanel()));


		upPanel.add(indexPanel, BorderLayout.CENTER);

		fieldPanel.add(new JLabel(I18n.getMessage("thaw.plugin.index.indexKey")), BorderLayout.WEST);
		fieldPanel.add(keyField, BorderLayout.CENTER);
		upPanel.add(fieldPanel, BorderLayout.SOUTH);

		downPanel.add(okButton);
		downPanel.add(cancelButton);

		frame.getContentPane().setLayout(new BorderLayout(10, 10));
		frame.getContentPane().add(upPanel, BorderLayout.CENTER);
		frame.getContentPane().add(downPanel, BorderLayout.SOUTH);

		frame.setSize(500, 400);

		cancelButton.addActionListener(this);
		okButton.addActionListener(this);
		indexTree.addObserver(this);

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

		upPanel = null;
		indexTree = null;

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
			selectedIndexKey = index.getPublicKey();

			Logger.info(this, "Selected index key: "+this.selectedIndexKey);
			keyField.setText(this.selectedIndexKey);
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

		indexTree.deleteObserver(this);
	}

}
