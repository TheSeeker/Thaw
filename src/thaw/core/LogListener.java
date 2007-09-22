package thaw.core;


public interface LogListener {

	public void newLogLine(int level, Object src, String line);
	public void logLevelChanged(int oldLevel, int newLevel);

}
