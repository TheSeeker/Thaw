package thaw.plugins.miniFrost;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JScrollPane;
import java.awt.GridLayout;
import java.awt.BorderLayout;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.util.Vector;
import java.util.Iterator;

import thaw.core.I18n;
import thaw.core.Logger;

import thaw.plugins.miniFrost.interfaces.Board;
import thaw.plugins.miniFrost.interfaces.BoardFactory;


public class BoardSelecter implements ActionListener {
	private static int DIALOG_SIZE_X = 200;
	private static int DIALOG_SIZE_Y = 500;

	private MiniFrostPanel mainPanel;
	private Object parentWindow;

	protected BoardSelecter() {

	}

	/**
	 * @param parentWindow must be a java.awt.Dialog or a java.awt.Frame
	 */
	protected BoardSelecter(MiniFrostPanel mainPanel,
				Object parentWindow) {
		this.mainPanel = mainPanel;
		this.parentWindow = parentWindow;
	}


	private JButton okButton;
	private JButton cancelButton;

	private boolean cancelled;

	protected Vector askBoardList() {
		Vector boards = new Vector();
		Vector checkBoxes = new Vector();

		JDialog dialog;

		if (parentWindow instanceof java.awt.Frame)
			dialog = new JDialog((java.awt.Frame)parentWindow,
					     I18n.getMessage("thaw.plugin.miniFrost.boards"));
		else if (parentWindow instanceof java.awt.Dialog)
			dialog = new JDialog((java.awt.Dialog)parentWindow,
					     I18n.getMessage("thaw.plugin.miniFrost.boards"));
		else {
			Logger.error(this, "Unknow type for the parameter 'parentWindow' : "+parentWindow.getClass().getName());
			return null;
		}


		dialog.getContentPane().setLayout(new BorderLayout());

		/* boards */
		BoardFactory[] factories = mainPanel.getPluginCore().getFactories();

		for (int i = 0 ; i < factories.length ; i++) {
			/* ignore special boards */
			if (factories[i] instanceof SpecialBoardFactory)
				continue;

			Vector v = factories[i].getBoards();

			if (v != null) {
				boards.addAll(v);
			}
		}

		java.util.Collections.sort(boards);


		/* checkbox */
		JPanel checkBoxPanel = new JPanel(new GridLayout(boards.size(), 1));

		for (Iterator it = boards.iterator();
		     it.hasNext();) {
			JCheckBox box = new JCheckBox(((Board)it.next()).toString(), false);
			checkBoxes.add(box);
			checkBoxPanel.add(box);
		}

		dialog.getContentPane().add(new JScrollPane(checkBoxPanel), BorderLayout.CENTER);


		/* buttons */

		JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
		okButton = new JButton(I18n.getMessage("thaw.common.ok"));
		cancelButton = new JButton(I18n.getMessage("thaw.common.cancel"));
		okButton.addActionListener(this);
		cancelButton.addActionListener(this);
		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);

		dialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);

		dialog.setSize(DIALOG_SIZE_X,
			       DIALOG_SIZE_Y);
		dialog.setVisible(true);

		try {
			synchronized(this) {
				this.wait();
			}
		} catch(InterruptedException e) {
			/* \_o< */
		}

		dialog.setVisible(false);

		Vector selected = null;

		if (!cancelled) {
			selected = new Vector();

			Iterator checkBoxIt = checkBoxes.iterator();
			Iterator boardsIt = boards.iterator();

			while (checkBoxIt.hasNext() && boardsIt.hasNext()) {
				JCheckBox box = (JCheckBox)checkBoxIt.next();
				Board board = (Board)boardsIt.next();

				if (box.isSelected())
					selected.add(board);
			}
		}

		return selected;
	}

	public void actionPerformed(ActionEvent e) {
		cancelled = !(e.getSource() == okButton);

		synchronized(this) {
			this.notifyAll();
		}
	}

	public static Vector askBoardList(MiniFrostPanel panel, Object parentWindow) {
		Vector v = (new BoardSelecter(panel, parentWindow)).askBoardList();

		if (v == null) {
			Logger.info(new BoardSelecter(), "Cancelled");
			return null;
		}

		return v;
	}


}
