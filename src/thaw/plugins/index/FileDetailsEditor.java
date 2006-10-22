package thaw.plugins.index;


import javax.swing.JPanel;

public class FileDetailsEditor {
	
	private JPanel panel;

	public FileDetailsEditor(boolean modifiables) {
		this.panel = new JPanel();
	}


	public JPanel getPanel() {
		return this.panel;
	}

}

