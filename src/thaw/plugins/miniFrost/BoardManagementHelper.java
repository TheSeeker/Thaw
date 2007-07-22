package thaw.plugins.miniFrost;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JDialog;
import javax.swing.JButton;
import javax.swing.JComboBox;

import java.awt.event.WindowEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractButton;
import javax.swing.JDialog;

import java.util.Vector;

import javax.swing.JOptionPane;

import thaw.core.I18n;
import thaw.core.Logger;

import thaw.plugins.miniFrost.interfaces.Board;
import thaw.plugins.miniFrost.interfaces.BoardFactory;


public class BoardManagementHelper {

	public interface BoardAction extends ActionListener {
		public void setTarget(Board board);
	}

	public static abstract class BasicBoardAction implements BoardAction, Runnable {

		public BasicBoardAction() {

		}

		public void run() {
			apply();
		}

		public void actionPerformed(ActionEvent e) {
			Thread th = new Thread(this);
			th.start();
		}

		public abstract void setTarget(Board board);
		public abstract void apply();
	}


	public static class BoardTypeAsker {

		private BoardFactory selection;

		public BoardTypeAsker(MiniFrostPanel mainPanel) {
			selection = (BoardFactory)JOptionPane.showInputDialog(mainPanel.getPluginCore().getCore().getMainWindow().getMainFrame(),
									      I18n.getMessage("thaw.plugin.miniFrost.selectType"),
									      I18n.getMessage("thaw.plugin.miniFrost.selectType"),
									      JOptionPane.QUESTION_MESSAGE,
									      null, /* icon */
									      mainPanel.getPluginCore().getFactories(),
									      mainPanel.getPluginCore().getFactories()[0]);
		}

		public BoardFactory getSelection() {
			return selection;
		}
	}


	public static class BoardAdder extends BasicBoardAction {
		private MiniFrostPanel mainPanel;
		private AbstractButton source;

		private Board target;


		public BoardAdder(MiniFrostPanel mainPanel, AbstractButton source) {
			super();

			this.mainPanel = mainPanel;
			this.source = source;

			if (source != null)
				source.addActionListener(this);
		}


		public void setTarget(Board board) {
			this.target = target;
		}

		public void apply() {
			BoardTypeAsker asker = new BoardTypeAsker(mainPanel);
			BoardFactory factory = asker.getSelection();

			if (factory != null)
				factory.createBoard(mainPanel.getPluginCore().getCore().getMainWindow());
			else
				Logger.info(this, "Adding canceled");

			mainPanel.getBoardTree().refresh();
		}
	}



	public static class BoardRemover implements BoardAction {
		private MiniFrostPanel mainPanel;
		private AbstractButton source;

		private Board target;


		public BoardRemover(MiniFrostPanel mainPanel, AbstractButton source) {
			this.mainPanel = mainPanel;
			this.source = source;

			source.setEnabled(false);

			if (source != null)
				source.addActionListener(this);
		}


		public void setTarget(Board board) {
			if (source != null)
				source.setEnabled(board != null);

			this.target = board;
		}

		public void actionPerformed(ActionEvent e) {
			if (target != null) {
				target.destroy();
				mainPanel.getBoardTree().refresh();
			} else {
				Logger.warning(this, "no target to delete");
			}
		}
	}


	public static class BoardRefresher implements BoardAction {
		private MiniFrostPanel mainPanel;
		private AbstractButton source;

		private Board target;


		public BoardRefresher(MiniFrostPanel mainPanel, AbstractButton source) {
			this.mainPanel = mainPanel;
			this.source = source;

			source.setEnabled(false);

			if (source != null)
				source.addActionListener(this);
		}


		public void setTarget(Board board) {
			if (source != null)
				source.setEnabled(board != null);

			this.target = board;
		}

		public void actionPerformed(ActionEvent e) {
			if (target != null) {
				target.refresh();
				mainPanel.getBoardTree().refresh(target);
			} else {
				Logger.warning(this, "no target to refresh");
			}
		}
	}

}
