package thaw.core;

/**
 * Define what methods a plugin must implements.
 */
public interface Plugin {

	/*
	 * Plugin constructor must not take any argument.
	 */


	/**
	 * Called when the plugin is runned.
	 * @param core A ref to the core of the program.
	 */
	public boolean run(Core core);

	/**
	 * Called when the plugin is stopped (often at the end of the program).
	 */
	public boolean stop();
}
