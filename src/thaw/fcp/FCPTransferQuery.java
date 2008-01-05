package thaw.fcp;

import java.util.HashMap;
import java.util.Observable;

/**
 * Transfer query == fetch / insert query. These queries must be able to
 * give more informations than the other.
 * Functions returning status of the request may be call frequently, so try to make them fast.
 * Some methods are only useful for downloads, and some for insertions, so check getQueryType() before calling them.
 */
public abstract class FCPTransferQuery extends Observable implements FCPQuery {
	public final static int BLOCK_SIZE = 16384;
	public final static int KEY_TYPE_CHK = 0;
	public final static int KEY_TYPE_KSK = 1;
	public final static int KEY_TYPE_SSK = 2; /* also USK */
	
	public final static int DEFAULT_PRIORITY = 4;
	public final static int DEFAULT_MAX_RETRIES = -1;
	public final static int PERSISTENCE_FOREVER           = 0;
	public final static int PERSISTENCE_UNTIL_NODE_REBOOT = 1;
	public final static int PERSISTENCE_UNTIL_DISCONNECT  = 2;
	
	private boolean insertion = false;
	
	/* last known values */
	private long requiredBlocks = -1;
	private long totalBlocks = -1;
	private long transferedBlocks = -1;
	private boolean reliable = false;
	
	/* reminder to do the maths */
	public final static int NMB_REMINDERS = 60; /* one per seconde, so 1 minute here */
	private long[] requiredBlocksPast = new long[NMB_REMINDERS];
	private long[] totalBlocksPast = new long[NMB_REMINDERS];
	private long[] transferedBlocksPast = new long[NMB_REMINDERS];
	private int currentReadCursor = 0; /* read Cursor in the *past arrays */
	private int currentWriteCursor = 0; /* write Cursor in the *past arrays */
	
	private boolean running = false;
	private boolean finished = false;
	private boolean successful = false;
	
	private long averageSpeed = 0;
	private long ETA = 0;
	
	private long startupTime = -1;
	private long completionTime = -1;
	
	
	public FCPTransferQuery(boolean insertion) {
		this.insertion = insertion;

		reliable = insertion;
		
		for (int i = 0 ; i < NMB_REMINDERS ; i++) {
			requiredBlocksPast[i]   = -1;
			totalBlocksPast[i]      = -1;
			transferedBlocksPast[i] = -1;
		}
	}
	
	protected void setBlockNumbers(long required, long total, long transfered, boolean reliable) {
		synchronized(this) {
			requiredBlocks = required;
			totalBlocks = total;
			transferedBlocks = transfered;
			this.reliable = reliable;
		}
	}
	
	protected void setStatus(boolean running, boolean finished, boolean successful) {
		this.running = running;
		this.finished = ((!running) ? finished : false);
		this.successful = (finished ? successful : false);
	}
	
	/**
	 * Called about each second by the queueManager. Used to make the average speed and the ETA
	 * as precise as possible
	 */
	protected void updateStats()
	{
		synchronized(this) {
			/* computing average speed & ETA */
			
			if (completionTime >= 0 && startupTime >= 0 && finished)
			{ /* wooch, final values ! :) */
				long blocks = (insertion) ? totalBlocks : requiredBlocks;
				long diffTime = (completionTime - startupTime) / 1000;
				
				if (blocks <= 0 || diffTime <= 0)
					return;
				
				averageSpeed = (blocks * BLOCK_SIZE) / diffTime;			
				ETA = diffTime; /* ok, it's a little bit icky, but it does the trick :) */
				
				notifyChange();
				
				return;
			}
			
			
			if (!running || finished)
				return;

			if (currentReadCursor != currentWriteCursor) {
				/* reminder : we have one second between each slot of the *Past arrays */
				long diffTimeSec = ((currentWriteCursor < currentReadCursor) ? currentWriteCursor+NMB_REMINDERS : currentWriteCursor) - currentReadCursor;
				long diffBlocks = transferedBlocks - transferedBlocksPast[currentReadCursor];
				long remainingBlocks = (insertion ? (totalBlocks - transferedBlocks) : (requiredBlocks - transferedBlocks));
				
				if (diffTimeSec <= 0 || diffBlocks <= 0 || remainingBlocks == 0) {
					averageSpeed = 0;
					ETA = 0;
				} else {
					double averageSpeedInBlocksPerSecond = diffBlocks / diffTimeSec;
					
					averageSpeed = (long)(averageSpeedInBlocksPerSecond * (double)BLOCK_SIZE);
					ETA = (long)(remainingBlocks / averageSpeedInBlocksPerSecond);
				}
				
				if (currentWriteCursor == currentReadCursor-1
						|| (currentWriteCursor == NMB_REMINDERS-1 && currentReadCursor == 0)) {
					/* the currentWriteCursor will push the currentReadCursor */
					currentReadCursor++;
					if (currentReadCursor >= NMB_REMINDERS)
						currentReadCursor = 0;
				}
				
				notifyChange();
			}

			/* updating known values */
			
			requiredBlocksPast[currentWriteCursor] = requiredBlocks;
			totalBlocksPast[currentWriteCursor] = totalBlocks;
			transferedBlocksPast[currentWriteCursor] = transferedBlocks;

			currentWriteCursor++;
			
			if (currentWriteCursor >= NMB_REMINDERS)
				currentWriteCursor = 0;
		}
	}
	
	
	/**
	 * @return in bytes / s (0 = unknown)
	 */
	public long getAverageSpeed()
	{
		return averageSpeed;
	}
	
	/**
	 * @return in second
	 */
	public long getETA()
	{
		return ETA;
	}

	/**
	 *  When did the request start ?
	 */
	public long getStartupTime()
	{
		return startupTime;
	}
	
	protected void setStartupTime(long startupTime)
	{
		this.startupTime = startupTime;
	}

	/**
	 * When did the request finish ?
	 * @return -1 if not finished
	 */
	public long getCompletionTime()
	{
		return completionTime;
	}
	
	protected void setCompletionTime(long time)
	{
		this.completionTime = time;
	}

	/**
	 * Informal.
	 * Is about the transfer on the network.
	 * In pourcents.
	 */
	public int getProgression()
	{
		if (finished)
			return 100;
		
		if (transferedBlocks < 0)
			return 0;
			
		if (insertion) {
			if (totalBlocks <= 0) return 0;
			
			return (int)(transferedBlocks * 100 / totalBlocks);
		} else {
			if (requiredBlocks <= 0) return 0;
			
			return (int)(transferedBlocks * 100 / requiredBlocks);
		}
	}

	public boolean isProgressionReliable()
	{
		if (getProgression() == 0)
			return true;

		return reliable;
	}

	
	public boolean isRunning()
	{
		return running;
	}

	public boolean isFinished()
	{
		return finished;
	}

	/**
	 * If unknow, return false.
	 * Query is considered as a failure is isFinished() && !isSuccesful()
	 */
	public boolean isSuccessful()
	{
		return successful;
	}

	public void notifyChange() {
		setChanged();
		notifyObservers();
	}
	
	
	/**** To implement : ****/
	
	
	

	/**
	 * Stop the transfer, but don't consider it as failed.
	 * @param queueManager QueueManager gives access to QueryManager;
	 */
	public abstract boolean pause(FCPQueueManager queueManager);

	/**
	 * Should only be called if isFinished() == true && isSuccessful() == false
	 */
	public abstract boolean isFatallyFailed();

	/**
	 * Only if persistent. Remove it from the queue.
	 */
	public abstract boolean removeRequest();

	/**
	 * Used by the QueueManager only.
	 * Currently these priority are the same
	 * as FCP priority, but it can change in the
	 * future.
	 * -1 = No priority
	 * Always between -1 and 6.
	 */
	public abstract int getThawPriority();

	/**
	 * Currently the same than Thaw priority.
	 */
	public abstract int getFCPPriority();


	/**
	 * call updatePersistentRequest() after to apply the change (Please note that the change
	 * will be visible even if you don't call it).
	 */
	public abstract void setFCPPriority(int prio);

	/**
	 * you can call it after saveFileTo() to update the clientToken.
	 * @param clientToken tell if the clientToken must be updated or just the priority
	 */
	public abstract void updatePersistentRequest(boolean clientToken);

	/**
	 * Informal.
	 * Human readable string describring the
	 * status of the query.
	 * @return can be null (== "Waiting")
	 */
	public abstract String getStatus();


	/**
	 * For persistent request only.
	 * @param dir Directory
	 */
	public abstract boolean saveFileTo(String dir);


	/**
	 * Is about the transfer between the node and thaw.
	 */
	public abstract int getTransferWithTheNodeProgression();

	

	/**
	 * Informal.
	 * Gives *public* final key only.
	 * @return can be null
	 */
	public abstract String getFileKey();

	/**
	 * Informal. In bytes.
	 * @return can be -1
	 */
	public abstract long getFileSize();

	/**
	 * Where is the file on the disk.
	 */
	public abstract String getPath();

	/**
	 * @return can return -1
	 */
	public abstract int getAttempt();

	public abstract void setAttempt(int x);

	/**
	 * @return can return -1
	 */
	public abstract int getMaxAttempt();


	/**
	 * Use to save the query in an XML file / a database / whatever.
	 * @return A HashMap : String (parameter name) -> String (parameter value) or null.
	 */
	public abstract HashMap getParameters();

	/**
	 * Opposite of getParameters().
	 * @return true if successful (or ignored) ; false if not.
	 */
	public abstract boolean setParameters(HashMap parameters);


	public abstract boolean isPersistent();
	public abstract boolean isGlobal();

	/**
	 * @return can be null (if non active, or meaningless).
	 */
	public abstract String getIdentifier();

	public abstract String getFilename();

}
