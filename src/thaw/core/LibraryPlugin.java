package thaw.core;

/**
 * Plugins adding functionality for other plugins should extends this class.
 * Then plugins using these library plugins will be able to register them one by one.
 * realStart() is called when the first plugin has registered itself.
 * realStop() is called when the last plugin has unregistered itself.
 */
public abstract class LibraryPlugin implements Plugin {
	private int nmbRegistered = 0;

	public abstract boolean run(Core core);
	public abstract boolean stop();

	public void registerChild(Plugin child) {
		this.nmbRegistered++;

		if(this.nmbRegistered == 1)
			this.realStart();
	}


	public void unregisterChild(Plugin child) {
		this.nmbRegistered--;

		if(this.nmbRegistered == 0)
			this.realStop();
	}

	public abstract void realStart();

	public abstract void realStop();

}
