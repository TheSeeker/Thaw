package thaw.core;

import javax.swing.JFrame;
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


	public JFrame splashScreen;
	public JProgressBar progressBar;
	

	public SplashScreen() {

	}

	public void display() {
		splashScreen = new JFrame();

		splashScreen.setLayout(new BorderLayout(10, 10));
		

		JLabel thawLabel = new JLabel("   Thaw");
		thawLabel.setFont(new Font("Dialog", Font.BOLD, 30));

		splashScreen.add(thawLabel, BorderLayout.CENTER);

		progressBar = new JProgressBar(0, 100);
		progressBar.setStringPainted(true);
		progressBar.setString("Wake up Neo ...");

		splashScreen.add(progressBar, BorderLayout.SOUTH);

		splashScreen.pack();

		splashScreen.setPreferredSize(new Dimension(SIZE_X, SIZE_Y));

		Dimension screenSize =
			Toolkit.getDefaultToolkit().getScreenSize();

		Dimension splashSize = splashScreen.getPreferredSize();
		splashScreen.setLocation(screenSize.width/2 - (splashSize.width/2),
					 screenSize.height/2 - (splashSize.height/2));


		splashScreen.setVisible(true);
		splashScreen.pack();

		splashScreen.setSize(SIZE_X, SIZE_Y);

	}

	/**
	 * @param progress In pourcent
	 */
	public void setProgression(int progress) {
		if(progressBar != null)
			progressBar.setValue(progress);
	}

	public int getProgression() {
		if(progressBar != null)
			return progressBar.getValue();
		else
			return -1;
	}


	public void setStatus(String status) {
		if(progressBar != null)
			progressBar.setString(status);
	}

	public void setProgressionAndStatus(int progress, String status) {
		setProgression(progress);
		setStatus(status);
	}


	public void hide() {
		splashScreen.setVisible(false);
		splashScreen = null;
		progressBar = null;
	}

}
