package thaw.plugins;

import java.util.Comparator;
import java.util.Collections;
import java.util.Vector;
import java.util.Iterator;

import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowListener;
import java.awt.event.WindowEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.JProgressBar;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JPanel;

import java.awt.Toolkit;

import java.awt.GridLayout;
import java.awt.BorderLayout;

import thaw.core.Core;
import thaw.core.Logger;
import thaw.core.I18n;

import thaw.fcp.FCPTransferQuery;

import thaw.gui.SysTrayIcon;
import thaw.gui.TransferProgressBar;
import thaw.gui.IconBox;

public class TrayIcon implements thaw.core.Plugin, MouseListener, WindowListener, ActionListener {
	private Core core;
	private SysTrayIcon icon;

	private JDialog dialog;
	private JButton closeDialog;

	public final static int DIALOG_X = 300;
	public final static int DIALOG_Y = 500;

	public TrayIcon() {

	}


	public boolean run(Core core) {
		this.core = core;

		icon = new SysTrayIcon(thaw.gui.IconBox.blueBunny);
		icon.setToolTip("Thaw "+thaw.core.Main.VERSION);
		icon.addMouseListener(this);

		core.getMainWindow().addWindowListener(this);

		icon.setVisible(true);

		return true;
	}


	public boolean stop() {
		core.getMainWindow().addWindowListener(this);
		icon.removeMouseListener(this);

		icon.setVisible(false);

		return true;
	}

	public String getNameForUser() {
		return I18n.getMessage("thaw.plugin.trayIcon.pluginName");
	}

	public javax.swing.ImageIcon getIcon() {
		return thaw.gui.IconBox.blueBunny;
	}

	public void switchMainWindowVisibility() {
		boolean v = !core.getMainWindow().isVisible();

		core.getMainWindow().setNonIconified();

		core.getMainWindow().setVisible(v);

		core.getMainWindow().setNonIconified();
	}


	private class QueryComparator implements Comparator {
		public QueryComparator() {

		}

		public int compare(final Object o1, final Object o2) {
			int result = 0;

			if(!(o1 instanceof FCPTransferQuery)
			   || !(o2 instanceof FCPTransferQuery))
				return 0;

			final FCPTransferQuery q1 = (FCPTransferQuery)o1;
			final FCPTransferQuery q2 = (FCPTransferQuery)o2;


			if((q1.getProgression() <= 0)
			   && (q2.getProgression() <= 0)) {
				if(q1.isRunning() && !q2.isRunning())
					return 1;

				if(q2.isRunning() && !q1.isRunning())
					return -1;
			}

			result = (new Integer(q1.getProgression())).compareTo(new Integer(q2.getProgression()));

			return result;
		}


		public boolean equals(final Object obj) {
			return true;
		}

		public int hashCode(){
			return super.hashCode();
		}
	}


	private JPanel getTransferPanel(FCPTransferQuery q) {
		JPanel p = new JPanel(new GridLayout(2, 1));

		String txt = q.getFilename();

		if (txt == null)
			txt = q.getFileKey();

		if (txt == null)
			txt = "?";

		javax.swing.ImageIcon icon;

		if (q.getQueryType() == 2)
			icon = IconBox.minInsertions;
		else
			icon = IconBox.minDownloads;

		JLabel l = new JLabel(txt);
		l.setIcon(icon);

		p.add(l);
		p.add(new TransferProgressBar(q));

		return p;
	}


	private void realDisplayFrame(int x, int y) {
		dialog = new JDialog((java.awt.Frame)null,
					     I18n.getMessage("thaw.plugin.trayIcon.dialogTitle"));
		dialog.getContentPane().setLayout(new BorderLayout(5, 5));
		dialog.setUndecorated(true);
		dialog.setResizable(true);

		JPanel panel = new JPanel(new BorderLayout(10, 10));
		panel.add(new JLabel(" "), BorderLayout.CENTER);

		Vector queries = core.getQueueManager().getRunningQueue();

		JPanel north;

		Vector newQueries = new Vector();

		synchronized(queries) {
			for (Iterator it = queries.iterator();
			     it.hasNext();) {
				newQueries.add(it.next());
			}
		}

		Collections.sort(newQueries, new QueryComparator());


		north = new JPanel(new GridLayout(queries.size(), 1, 10, 10));

		for (Iterator it = newQueries.iterator();
		     it.hasNext();) {
			north.add(getTransferPanel((FCPTransferQuery)it.next()));
		}


		JPanel northNorth = new JPanel(new BorderLayout());
		northNorth.add(new JLabel(" "), BorderLayout.CENTER);

		closeDialog = new JButton(IconBox.minClose);
		closeDialog.addActionListener(this);
		northNorth.add(closeDialog, BorderLayout.EAST);

		dialog.getContentPane().add(northNorth, BorderLayout.NORTH);
		dialog.getContentPane().add(panel, BorderLayout.CENTER);

		panel.add(north,
			  BorderLayout.NORTH);

		dialog.getContentPane().add(new JScrollPane(panel,
							    JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
							    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED),
					    BorderLayout.CENTER);

		dialog.setLocation(x, y);

		dialog.setSize(DIALOG_X, DIALOG_Y);
		dialog.setPreferredSize(new java.awt.Dimension(DIALOG_X, DIALOG_Y));
		dialog.validate();

		dialog.setVisible(true);
	}

	public void displayFrame(int x, int y) {
		java.awt.Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		int screen_x = (int)d.getWidth();
		int screen_y = (int)d.getHeight();

		if (x+DIALOG_X >= screen_x)
			x -= DIALOG_X;
		if (y+DIALOG_Y >= screen_y)
			y -= DIALOG_Y;

		realDisplayFrame(x, y);
	}

	public void windowActivated(WindowEvent e) { }
	public void windowClosed(WindowEvent e) { }
	public void windowClosing(WindowEvent e) { }
	public void windowDeactivated(WindowEvent e) { }
	public void windowDeiconified(WindowEvent e) { }

        public void windowIconified(WindowEvent e) {
		switchMainWindowVisibility();
	}

	public void windowOpened(WindowEvent e) { }


	public void mouseClicked(MouseEvent e) {
		if (dialog != null) {
			dialog.setVisible(false);
			dialog = null;
			return;
		}

		if (e.getButton() == MouseEvent.BUTTON1)
			switchMainWindowVisibility();
		else if (e.getButton() == MouseEvent.BUTTON3) {
			if (dialog == null) {
				java.awt.Point p = icon.getMousePosition();
				displayFrame(((int)p.getX()), ((int)p.getY()));
			}
		}
	}

	public void mouseEntered(MouseEvent e) { }
	public void mouseExited(MouseEvent e) { }
	public void mousePressed(MouseEvent e) { }
	public void mouseReleased(MouseEvent e) { }

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == closeDialog) {
			if (dialog != null) {
				dialog.setVisible(false);
				dialog = null;
			}
		}
	}
}
