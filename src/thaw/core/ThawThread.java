package thaw.core;

public class ThawThread extends Thread {
	private static ThreadGroup threadGroup = new ThreadGroup("Thaw");

	public ThawThread(Runnable target, String name) {
		this(target, name, null);
	}

	public ThawThread(Runnable target, String name, Object parent) {
		super(threadGroup, target, name);
	}

}
