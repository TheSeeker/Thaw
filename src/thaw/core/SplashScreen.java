package thaw.core;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;


public class SplashScreen {
	public final static int SIZE_X = 500;
	public final static int SIZE_Y = 100;


	public JDialog splashScreen;
	public JProgressBar progressBar;


	public SplashScreen() {

	}

	public void display() {
		final JPanel panel = new JPanel();
		splashScreen = new JDialog();

		splashScreen.setUndecorated(true);
		splashScreen.setResizable(false);

		panel.setLayout(new BorderLayout(10, 10));


		final JLabel thawLabel = new JLabel("   Thaw");

		thawLabel.setFont(new Font("Dialog", Font.BOLD, 30));

		panel.add(thawLabel, BorderLayout.CENTER);

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
	 * @param progress In pourcent
	 */
	public void setProgression(final int progress) {
		if(progressBar != null)
			progressBar.setValue(progress);
	}

	public int getProgression() {
		if(progressBar != null)
			return progressBar.getValue();
		else
			return -1;
	}


	public void setStatus(final String status) {
		if(progressBar != null)
			progressBar.setString(status);
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
