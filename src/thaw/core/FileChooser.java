package thaw.core;

import javax.swing.JFileChooser;
import java.io.File;
import java.util.Vector;

/**
 * FileChooser helps to create and use simple JFileChooser.
 * Don't block any swing component.
 */
public class FileChooser {
	private JFileChooser fileChooser = null;

	public FileChooser() {
		fileChooser = new JFileChooser();
	}

	public FileChooser(String path) {
		fileChooser = new JFileChooser(path);
		fileChooser.setDragEnabled(true);
	}

	public void setTitle(String title) {
		fileChooser.setDialogTitle(title);
	}

	public void setDirectoryOnly(boolean v) {
		if(v)
			fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		else
			fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES); /* Directories -> Recursivity */
	}
	
	/**
	 * @param type JFileChooser.OPEN_DIALOG / JFileChooser.SAVE_DIALOG
	 * @see javax.swing.JFileChooser#setDialogType(int)
	 */
	public void setDialogType(int type) {
		fileChooser.setDialogType(type);
	}

	protected boolean showDialog() {
		int result = 0;

		if(fileChooser.getDialogType() == JFileChooser.OPEN_DIALOG) {
			result = fileChooser.showOpenDialog(null);
		}

		if(fileChooser.getDialogType() == JFileChooser.SAVE_DIALOG) {
			result = fileChooser.showSaveDialog(null);
		}

		if(result == JFileChooser.APPROVE_OPTION)
			return true;
		else
			return false;

	}

	/**
	 * @return null if nothing choosed.
	 */
	public File askOneFile() {
		fileChooser.setMultiSelectionEnabled(false);

		if(!showDialog())
			return null;

		return fileChooser.getSelectedFile();
	}


	protected void expandRecursivly(File file, Vector vec) {
		if (file.isFile()) {
			vec.add(file);
			return;
		}

		File[] files = file.listFiles();

		if (files == null) {
			Logger.notice(this, "Unable to parse directory '"+file.getPath()+"'");
			return;
		}

		for (int i=0; i < files.length; i++) {
			if (files[i].isFile())
				vec.add(files[i]);
			else
				expandRecursivly(files[i],vec);
		}
		
	}
	
	protected Vector expandRecursivly(File[] selectedFiles)
	{
		Vector files= new Vector();

		for (int i = 0 ; i < selectedFiles.length ; i++) {
			expandRecursivly(selectedFiles[i], files);
		}

		return files;
	}
	
	/**
	 * @return null if nothing choosed.
	 */
	public Vector askManyFiles() {
		fileChooser.setMultiSelectionEnabled(true);

		if(!showDialog())
			return null;
	
		return expandRecursivly(fileChooser.getSelectedFiles());
	}

}
