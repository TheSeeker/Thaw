package thaw.core;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

public class SplashScreen {
	public final static int SIZE_X = 500;
	public final static int SIZE_Y = 150;

	public final static int NMB_ICONS = 9;

	public JDialog splashScreen;
	public JProgressBar progressBar;
	public JPanel iconPanel;

	public int nmbIcon = 0;

	public Vector emptyLabels;
	public Vector iconLabels;

	public SplashScreen() {

	}

	public void display() {
		final JPanel panel = new JPanel();
		JPanel subPanel = new JPanel();
		iconPanel = new JPanel();

		splashScreen = new JDialog();

		splashScreen.setUndecorated(true);
		splashScreen.setResizable(false);

		panel.setLayout(new BorderLayout(10, 10));
		subPanel.setLayout(new GridLayout(2, 1));
		iconPanel.setLayout(new GridLayout(1, NMB_ICONS));

		emptyLabels = new Vector();
		iconLabels = new Vector();

		/* it's a dirty method to keep the NMB_ICONS parts of the panel at the same size */
		for (int i = 0 ; i < NMB_ICONS ; i++) {
			JLabel lb = new JLabel();
			emptyLabels.add(lb);
			iconPanel.add(lb, i);
		}

		final JLabel thawLabel = new JLabel("Thaw");

		thawLabel.setFont(new Font("Dialog", Font.BOLD, 42));
		thawLabel.setHorizontalAlignment(JLabel.CENTER);

		subPanel.add(thawLabel);
		subPanel.add(iconPanel);

		panel.add(subPanel, BorderLayout.CENTER);

		progressBar = new JProgressBar(0, 100);
		progressBar.setStringPainted(true);
		progressBar.setString("Wake up Neo ...");

		panel.add(progressBar, BorderLayout.SOUTH);

		splashScreen.getContentPane().add(panel);

		splashScreen.setSize(SplashScreen.SIZE_X, SplashScreen.SIZE_Y);


		final Dimension screenSize =
			Toolkit.getDefaultToolkit().getScreenSize();

		final Dimension splashSize = splashScreen.getSize();
		splashScreen.setLocation(screenSize.width/2 - (splashSize.width/2),
					 screenSize.height/2 - (splashSize.height/2));


		splashScreen.setVisible(true);

		splashScreen.setSize(SplashScreen.SIZE_X, SplashScreen.SIZE_Y);

	}

	/**
	 * @param progress In percent
	 */
	public void setProgression(final int progress) {
		if(progressBar != null && splashScreen != null) {
			progressBar.setValue(progress);
			splashScreen.getContentPane().validate();
		}
	}


	public void addIcon(ImageIcon icon) {
		if (splashScreen == null)
			return;

		JLabel lb = new JLabel(icon);

		lb.setHorizontalAlignment(JLabel.CENTER);
		lb.setVerticalAlignment(JLabel.CENTER);

		if (emptyLabels.size() > 0)
			iconPanel.remove((java.awt.Component)emptyLabels.get(0));

		iconPanel.add(lb, nmbIcon);

		if (emptyLabels.size() > 0)
			emptyLabels.removeElementAt(0);

		nmbIcon++;

		splashScreen.getContentPane().validate();
		lb.repaint();
	}


	/* TODO : removeIcon() */


	public int getProgression() {
		if(progressBar != null)
			return progressBar.getValue();
		else
			return -1;
	}


	public void setStatus(final String status) {
		if(progressBar != null && splashScreen != null) {
			progressBar.setString(status);
			splashScreen.getContentPane().validate();
		}
	}

	public void setProgressionAndStatus(final int progress, final String status) {
		setProgression(progress);
		setStatus(status);
	}


	public void hide() {
		splashScreen.setVisible(false);
		splashScreen.dispose();
		splashScreen = null;
		progressBar = null;
	}

}
