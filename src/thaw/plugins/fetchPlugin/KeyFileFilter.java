package thaw.plugins.fetchPlugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Vector;

import thaw.core.Logger;

public class KeyFileFilter {

	/**
	 * Only used to be able to call correctly functions of thaw.core.Logger
	 */
	public KeyFileFilter() {

	}


	/**
	 * @return Vector of Strings
	 */
	public static Vector extractKeys(final File file) {
		final Vector result = new Vector();

		FileInputStream fstream = null;

		try {
			fstream = new FileInputStream(file);
		} catch(final java.io.FileNotFoundException e) {
			Logger.warning(new KeyFileFilter(), "File not found exception for "+file.getPath());
			return null;
		}



		final BufferedReader in = new BufferedReader(new InputStreamReader(fstream));

		try {
			String line = null;

			while((line = in.readLine()) != null) {
				final String[] pieces = line.split("[^- \\?.a-zA-Z0-9,~%@/'_]");

				for(int i = 0 ; i < pieces.length ; i++) {
					if(pieces[i].matches(".{3}@.*,.*"))
						result.add(pieces[i]);
				}

			}

		} catch(final java.io.IOException e) {
			Logger.warning(new KeyFileFilter(), "IOException while reading '"+file.getPath()+"' !");
			return result;
		}

		return result;
	}

}
