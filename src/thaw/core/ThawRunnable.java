package thaw.core;

public interface ThawRunnable extends Runnable {

	/**
	 * Called only when the user wants to halt Thaw
	 */
	public void stop();

}
