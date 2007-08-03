package thaw.plugins.index;

public interface FileContainer {

	public String getFilename();

	/**
	 * @return if the result is not a valid key, it won't be added to the index
	 */
	public String getPublicKey();

	public long getSize();

	/**
	 * @return null if null, the mime type will be guessed from the filename
	 */
	public String getMime();

}
