package thaw.plugins.index;


import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import thaw.core.I18n;
import thaw.core.Logger;
import thaw.core.FreenetURIHelper;
import thaw.plugins.Hsqldb;

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

	private IndexBrowserPanel indexBrowser;

	public IndexSelecter(IndexBrowserPanel indexBrowser) {
		this.indexBrowser = indexBrowser;
	}

	/**
	 * Returned null if canceled. Is blocking !
	 */
	public String askForAnIndexURI(final Hsqldb db) {
		frame = new JFrame(I18n.getMessage("thaw.plugin.index.selectIndex"));

		frame.setVisible(false);

		upPanel = new JPanel();
		Logger.info(this, "indexes");
		indexTree = new IndexTree(I18n.getMessage("thaw.plugin.index.yourIndexes"), true, null, indexBrowser, null);
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
		indexPanel.add(indexTree.getPanel());


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

		frame.setVisible(true);

		for (closeWin = false ; !closeWin ; ) {
			try {
				Thread.sleep(500);
			} catch(final java.lang.InterruptedException e) {
				/* \_o< \_o< \_o< */
			}
		}

		frame.setVisible(false);

		frame = null;

		upPanel = null;
		indexTree = null;

		fieldPanel = null;
		keyField = null;

		downPanel = null;
		cancelButton = null;
		okButton = null;

		return FreenetURIHelper.cleanURI(selectedIndexKey);
	}


	public void update(final java.util.Observable o, final Object param) {
		if (param instanceof Index) {
			final Index index = (Index)param;
			selectedIndexKey = index.getPublicKey();

			Logger.info(this, "Selected index key: "+selectedIndexKey);
			keyField.setText(selectedIndexKey);
		}
	}

	public void actionPerformed(final java.awt.event.ActionEvent e) {
		if (e.getSource() == okButton) {
			if ((keyField.getText() == null) || "".equals( keyField.getText() ))
				selectedIndexKey = null;
			else
				selectedIndexKey = keyField.getText();
			closeWin = true;
		}

		if (e.getSource() == cancelButton) {
			selectedIndexKey = null;
			closeWin = true;
		}

		indexTree.deleteObserver(this);
	}

}
