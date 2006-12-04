package thaw.core;

import javax.swing.JDialog;
import javax.swing.JProgressBar;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Font;


public class SplashScreen {
	public final static int SIZE_X = 500;
	public final static int SIZE_Y = 100;


	public JDialog splashScreen;
	public JProgressBar progressBar;


	public SplashScreen() {

	}

	public void display() {
		JPanel panel = new JPanel();
		this.splashScreen = new JDialog();

		this.splashScreen.setUndecorated(true);
		this.splashScreen.setResizable(false);

		panel.setLayout(new BorderLayout(10, 10));


		JLabel thawLabel = new JLabel("   Thaw");

		thawLabel.setFont(new Font("Dialog", Font.BOLD, 30));

		panel.add(thawLabel, BorderLayout.CENTER);

		this.progressBar = new JProgressBar(0, 100);
		this.progressBar.setStringPainted(true);
		this.progressBar.setString("Wake up Neo ...");

		panel.add(this.progressBar, BorderLayout.SOUTH);

		this.splashScreen.getContentPane().add(panel);

		this.splashScreen.setSize(SIZE_X, SIZE_Y);


		Dimension screenSize =
			Toolkit.getDefaultToolkit().getScreenSize();

		Dimension splashSize = this.splashScreen.getSize();
		this.splashScreen.setLocation(screenSize.width/2 - (splashSize.width/2),
					 screenSize.height/2 - (splashSize.height/2));


		this.splashScreen.setVisible(true);

		this.splashScreen.setSize(SIZE_X, SIZE_Y);

	}

	/**
	 * @param progress In pourcent
	 */
	public void setProgression(int progress) {
		if(this.progressBar != null)
			this.progressBar.setValue(progress);
	}

	public int getProgression() {
		if(this.progressBar != null)
			return this.progressBar.getValue();
		else
			return -1;
	}


	public void setStatus(String status) {
		if(this.progressBar != null)
			this.progressBar.setString(status);
	}

	public void setProgressionAndStatus(int progress, String status) {
		this.setProgression(progress);
		this.setStatus(status);
	}


	public void hide() {
		splashScreen.setVisible(false);
		splashScreen.dispose();
		splashScreen = null;
		progressBar = null;
	}

}
