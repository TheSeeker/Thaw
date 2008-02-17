package thaw.plugins.index;


import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import thaw.core.I18n;
import thaw.core.Logger;
import thaw.fcp.FreenetURIHelper;
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
	private JTextArea keyArea;

	private JPanel downPanel;
	private JButton cancelButton;
	private JButton okButton;

	private boolean closeWin;
	private String[] selectedIndexKeys = null;

	private IndexBrowserPanel indexBrowser;

	public IndexSelecter(IndexBrowserPanel indexBrowser) {
		this.indexBrowser = indexBrowser;
	}

	/**
	 * Returned null if canceled. Is blocking !
	 */
	public String[] askForIndexURIs(final Hsqldb db) {
		frame = new JFrame(I18n.getMessage("thaw.plugin.index.selectIndex"));

		frame.setVisible(false);

		upPanel = new JPanel();
		Logger.info(this, "indexes");
		indexTree = new IndexTree(I18n.getMessage("thaw.plugin.index.yourIndexes"), true, null, indexBrowser, null);
		Logger.info(this, "plus indexes");

		fieldPanel = new JPanel();
		keyArea = new JTextArea("");

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
		
		JScrollPane sp = new JScrollPane(keyArea);
		
		keyArea.setSize(100, 100);
		sp.setSize(100, 100);
		sp.setPreferredSize(new java.awt.Dimension(100, 100));
		
		fieldPanel.add(new JLabel(I18n.getMessage("thaw.plugin.index.indexKey")), BorderLayout.NORTH);
		fieldPanel.add(sp, BorderLayout.CENTER);
		upPanel.add(fieldPanel, BorderLayout.SOUTH);

		downPanel.add(okButton);
		downPanel.add(cancelButton);

		frame.getContentPane().setLayout(new BorderLayout(10, 10));
		frame.getContentPane().add(upPanel, BorderLayout.CENTER);
		frame.getContentPane().add(downPanel, BorderLayout.SOUTH);

		frame.setSize(600, 600);
		
		keyArea.setSize(100, 100);
		sp.setSize(100, 100);
		sp.setPreferredSize(new java.awt.Dimension(100, 100));
		
		selectedIndexKeys = null;

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
		frame.dispose();

		frame = null;

		upPanel = null;
		indexTree = null;

		fieldPanel = null;
		keyArea = null;

		downPanel = null;
		cancelButton = null;
		okButton = null;

		//return FreenetURIHelper.cleanURI(selectedIndexKey);
		return selectedIndexKeys;
	}


	public void update(final java.util.Observable o, final Object param) {
		if (param instanceof Vector) {
			keyArea.setText("");
			
			for (Iterator it = ((Vector)param).iterator(); it.hasNext(); ) {
				IndexTreeNode node = (IndexTreeNode)it.next();
				
				if (node instanceof Index) {
					final Index index = (Index)node;
					final String key = index.getPublicKey();

					Logger.info(this, "Selected index key: "+key);
					keyArea.setText(keyArea.getText()+key+"\n");
				}
			}			
		}
		
	}

	public void actionPerformed(final java.awt.event.ActionEvent e) {
		if (e.getSource() == okButton) {
			if ((keyArea.getText() == null) || "".equals( keyArea.getText() ))
				selectedIndexKeys = null;
			else {
				selectedIndexKeys = keyArea.getText().trim().split("\n");
				
				for (int i = 0 ; i < selectedIndexKeys.length ; i++) {
					selectedIndexKeys[i] = FreenetURIHelper.cleanURI(selectedIndexKeys[i]);					
				}
				
			}
			closeWin = true;
		}

		if (e.getSource() == cancelButton) {
			selectedIndexKeys = null;
			closeWin = true;
		}

		indexTree.deleteObserver(this);
	}

}
