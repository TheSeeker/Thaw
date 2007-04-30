package thaw.gui;

import java.io.File;
import java.util.Vector;

import javax.swing.JFileChooser;

import thaw.core.Logger;


/**
 * FileChooser helps to create and use simple JFileChooser.
 */
public class FileChooser {
	private JFileChooser fileChooser = null;

	private String finalDir = null;

	public final static int OPEN_DIALOG = JFileChooser.OPEN_DIALOG;
	public final static int SAVE_DIALOG = JFileChooser.SAVE_DIALOG;


	public FileChooser() {
		fileChooser = new JFileChooser();
	}

	public FileChooser(final String path) {
		fileChooser = new JFileChooser(path);
		fileChooser.setDragEnabled(true);
	}

	public void setTitle(final String title) {
		fileChooser.setDialogTitle(title);
	}


	public void setDirectoryOnly(final boolean v) {
		if(v)
			fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		else
			 /* Directories -> Recursivity */
			fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
	}

	/**
	 * @param type JFileChooser.OPEN_DIALOG / JFileChooser.SAVE_DIALOG
	 * @see javax.swing.JFileChooser#setDialogType(int)
	 */
	public void setDialogType(final int type) {
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
		File file;

		fileChooser.setMultiSelectionEnabled(false);

		if(!showDialog())
			return null;

		file = fileChooser.getSelectedFile();

		if (file != null) {
			finalDir = file.getParent();
		}

		return file;
	}


	protected void expandRecursivly(final File file, final Vector vec) {
		if (file.isFile()) {
			vec.add(file);
			return;
		}

		final File[] files = file.listFiles();

		if (files == null) {
			Logger.notice(this, "Unable to parse directory '"+file.getPath()+"'");
			return;
		}

		for (int i=0; i < files.length; i++) {
			if (files[i].isFile())
				vec.add(files[i]);
			else
				this.expandRecursivly(files[i],vec);
		}

	}

	protected Vector expandRecursivly(final File[] selectedFiles)
	{
		final Vector files= new Vector();

		for (int i = 0 ; i < selectedFiles.length ; i++) {
			this.expandRecursivly(selectedFiles[i], files);
		}

		return files;
	}

	/**
	 * @return null if nothing choosed.
	 */
	public Vector askManyFiles() {
		File[] files;

		fileChooser.setMultiSelectionEnabled(true);

		if(!showDialog())
			return null;

		files = fileChooser.getSelectedFiles();

		if (files != null && files[0] != null) {
			finalDir = files[0].getParent();
		}

		return this.expandRecursivly(files);
	}


	/**
	 * Return the main directory where the files where selected
	 */
	public String getFinalDirectory() {
		return finalDir;
	}

}
