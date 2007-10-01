package thaw.plugins.transferLogs;

import java.sql.*;

import java.util.Observer;
import java.util.Observable;


import thaw.core.Logger;

import thaw.fcp.FCPTransferQuery;
import thaw.fcp.FCPClientPut;
import thaw.fcp.FreenetURIHelper;

import thaw.plugins.Hsqldb;
import thaw.plugins.TransferLogs;


public class Transfer implements Observer {
	private Hsqldb db;

	private FCPTransferQuery query;

	private int id;

	private Timestamp dateStart;
	private Timestamp dateEnd;
	private byte transferType;
	private String key;
	private String filename;
	private long size;
	private boolean isDup;
	private boolean isSuccess;

	private TransferTable table;


	public Transfer(Hsqldb db,
			FCPTransferQuery query,
			TransferTable table) {
		this.db = db;
		this.query = query;
		this.id = -1;
		this.table = table;


		if (!findOrInsertEntry(query))
			findOrInsertEntry(query); /* because we need the id of the entry */

		if (!query.isFinished()
		    && query instanceof Observable) {
			((Observable)query).addObserver(this);
		}
	}


	/**
	 * If found, return true, if inserted returns false.
	 * If found, but some informations in the bdd is not correct, it's updated.
	 */
	private boolean findOrInsertEntry(FCPTransferQuery query) {
		boolean entryFound;

		entryFound = false;

		Logger.info(this, "Searching corresponding query in the logs ...");

		synchronized(db.dbLock) {
			try {
				PreparedStatement st;

				String qKey = query.getFileKey();

				if (qKey == null || !FreenetURIHelper.isAKey(qKey))
					qKey = null;

				/* by key first */

				if (qKey != null) {

					st = db.getConnection().prepareStatement("SELECT id, dateStart, dateEnd, "+
										 "transferType, "+
										 "key, filename, size, isDup, isSuccess "+
										 "FROM transferLogs "+
										 "WHERE key LIKE ? AND dateEnd IS NULL "+
										 "ORDER BY dateStart DESC "+
										 "LIMIT 1");
					st.setString(1, FreenetURIHelper.getComparablePart(qKey)+"%");

					ResultSet set = st.executeQuery();

					if (set.next()) {
						entryFound        = true;
						this.id           = set.getInt("id");
						this.dateStart    = set.getTimestamp("dateStart");
						this.dateEnd      = set.getTimestamp("dateEnd");
						this.transferType = set.getByte("transferType");
						this.key          = set.getString("key");
						this.filename     = set.getString("filename");
						this.size         = set.getLong("size");
						this.isDup        = set.getBoolean("isDup");
						this.isSuccess    = set.getBoolean("isSuccess");
					}
				}


				/* by filename else */

				String filename = query.getFilename();

				if (filename != null && entryFound == false) {
					st = db.getConnection().prepareStatement("SELECT id, dateStart, dateEnd, "+
										 "transferType, "+
										 "key, filename, size, isDup, isSuccess "+
										 "FROM transferLogs "+
										 "WHERE filename = ? AND dateEnd IS NULL "+
										 "ORDER BY dateStart DESC "+
										 "LIMIT 1");
					st.setString(1, filename);

					ResultSet set = st.executeQuery();

					if (set.next()) {
						entryFound        = true;
						this.id           = set.getInt("id");
						this.dateStart    = set.getTimestamp("dateStart");
						this.dateEnd      = set.getTimestamp("dateEnd");
						this.transferType = set.getByte("transferType");
						this.key          = set.getString("key");
						this.filename     = set.getString("filename");
						this.size         = set.getLong("size");
						this.isDup        = set.getBoolean("isDup");
						this.isSuccess    = set.getBoolean("isSuccess");
					}
				}


				if (qKey == null && filename == null) { /* this query would be useless ?! */
					Logger.warning(this, "Query with filename & key == null ? can do nothing with that");
					return false;
				}


				if (entryFound) { /* we check if we must update the entry */
					boolean mustUpdateKey = false;
					boolean mustUpdateDateStart = false;
					boolean mustUpdateDateEnd = false;
					boolean mustUpdateSize = false;

					if (this.key == null && qKey != null)
						mustUpdateKey = true;

					if (qKey == null && this.key != null)
						qKey = this.key;

					if (qKey != null && this.key != null
					    && !qKey.equals(this.key)) /* the key has changed ? can it happen ? */
						mustUpdateKey = true;

					if((query.getStartupTime() != dateStart.getTime()) && (query.getStartupTime() != -1))
						mustUpdateDateStart = true;

					if (query.isFinished() && this.dateEnd == null)
						mustUpdateDateEnd = true;

					if (query.getFileSize() >= 0
					    && query.getFileSize() >= size)
						mustUpdateSize = true;

					if (mustUpdateKey) {
						updateKey(qKey);
					}

					if (mustUpdateDateStart) {
						updateDateStart();
					}

					if (mustUpdateDateEnd) {
						updateDateEnd(query.isSuccessful());
					}

					if (mustUpdateSize) {
						updateSize(query.getFileSize());
					}

				} else { /* we insert a new one */
					/* except if this query has already ended,
					 * because it means we weren't
					 * used for this job, so we won't move :P */
					if (query.isFinished())
						return false;

					/* the main problem here is that when we insert a data
					 * hsqldb is not able to give us back the primary key generated
					 * so when this function will return false (no entry found => inserted),
					 * the constructor will call it again to find the primary  key
					 */

					Timestamp now = TransferLogs.getNow();

					st = db.getConnection().prepareStatement("INSERT INTO transferLogs "+
										 "(dateStart, dateEnd, transferType, "+
										 " key, filename, size, isDup, isSuccess) "+
										 "VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
					st.setTimestamp(1, now);

					if (query.isFinished())
						st.setTimestamp(2, now);
					else
						st.setNull(2, Types.TIMESTAMP);

					if (query instanceof FCPClientPut)
						st.setByte(3, TransferLogs.TRANSFER_TYPE_INSERTION);
					else
						st.setByte(3, TransferLogs.TRANSFER_TYPE_DOWNLOAD);

					if (qKey != null)
						st.setString(4, qKey);
					else
						st.setNull(4, Types.VARCHAR);

					st.setString(5, filename);
					st.setLong(6, query.getFileSize());
					st.setBoolean(7, TransferLogs.isDup(db, query.getFileKey()));
					st.setBoolean(8, query.isFinished() && query.isSuccessful());
					st.execute();
				}


			} catch(SQLException e) {
				Logger.error(this,
					     "Error while trying to find a specific "+
					     "entry in the log: "+e.toString());
			}
		}

		if (entryFound)
			Logger.info(this, "Entry found");
		else
			Logger.info(this, "Entry added");

		return entryFound;
	}


	private void updateKey(String qKey) {
		Logger.info(this, "Updating key in logs");
		try {
			PreparedStatement st = db.getConnection().prepareStatement("UPDATE transferLogs SET "+
										   "key = ? WHERE id = ?");
			st.setString(1, qKey);
			st.setInt(2, this.id);
			st.execute();
		} catch(SQLException e) {
			Logger.error(this, "Unable to update key in transfer logs because : "+e.toString());
		}
	}

	private void updateSize(long size) {
		Logger.info(this, "Updating file size in logs");

		try {
			PreparedStatement st = db.getConnection().prepareStatement("UPDATE transferLogs SET "+
										   "size = ? WHERE id = ?");
			st.setLong(1, size);
			st.setInt(2, this.id);
			st.execute();
		} catch(SQLException e) {
			Logger.error(this, "Unable to update size in transfer logs because : "+e.toString());
		}
	}


	private void updateDateEnd(boolean successful) {
		Logger.info(this, "Updating end date in logs");

		try {
			PreparedStatement st = db.getConnection().prepareStatement("UPDATE transferLogs SET "+
										   "dateEnd = ?, isSuccess = ?"+
										   "WHERE id = ?");
			dateEnd = new Timestamp(query.getCompletionTime());
			st.setTimestamp(1, dateEnd);
			st.setBoolean(2, successful);
			st.setInt(3, this.id);
			st.execute();
		} catch(SQLException e) {
			Logger.error(this, "Unable to update dateEnd in transfer logs because : "+e.toString());
		}
	}

	private void updateDateStart() {
		Logger.info(this, "Updating start date in logs");

		try {
			PreparedStatement st = db.getConnection().prepareStatement("UPDATE transferLogs SET "+
										   "dateStart = ?"+
										   "WHERE id = ?");
			dateStart = new Timestamp(query.getStartupTime());
			st.setTimestamp(1, dateStart);
			st.setInt(2, this.id);
			st.execute();
		} catch(SQLException e) {
			Logger.error(this, "Unable to update dateEnd in transfer logs because : "+e.toString());
		}
	}


	public Transfer(Hsqldb db,
			int id,
			Timestamp dateStart, Timestamp dateEnd,
			byte transferType, String key, String filename, long size,
			boolean isDup, boolean isSuccess) {
		this.db           = db;
		this.id           = id;
		this.dateStart    = dateStart;
		this.dateEnd      = dateEnd;
		this.transferType = transferType;
		this.key          = key;
		this.filename     = filename;
		this.size         = size;
		this.isDup        = isDup;
		this.isSuccess    = isSuccess;
	}


	public Timestamp getDateStart() {
		return dateStart;
	}

	public Timestamp getDateEnd() {
		return dateEnd;
	}

	public byte getTransferTypeByte() {
		return transferType;
	}

	public String getTransferTypeStr() {
		return TransferLogs.TRANSFER_TYPE_NAMES[transferType];
	}

	public String getKey() {
		return key;
	}

	public String getFilename() {
		return filename;
	}

	public long getSize() {
		return size;
	}

	public boolean isDup() {
		return isDup;
	}

	public boolean isSuccess() {
		return isSuccess;
	}

	/**
	 * @return a value in byte / s
	 */
	public long getAverageSpeed() {
		if (!isSuccess() || getDateEnd() == null || getSize() <= 0)
			return -1;

		long diff = (getDateEnd().getTime() - getDateStart().getTime())/1000;

		if (diff <= 0)
			return -1;

		return getSize() / diff;
	}


	protected FCPTransferQuery getQuery() {
		return query;
	}

	protected int getId() {
		return id;
	}


	public void delete() {
		Logger.info(this, "Deleting transfer logs entry ...");

		try {
			synchronized(db.dbLock) {
				PreparedStatement st;

				st = db.getConnection().prepareStatement("DELETE FROM transferLogs WHERE id = ?");
				st.setInt(1, id);
				st.execute();
			}
		} catch(SQLException e) {
			Logger.error(this, "Can't delete transfer because: "+e.toString());
		}
	}



	public boolean equals(Object o) {
		if (o == null || (!(o instanceof Transfer)))
			return false;

		return (id == ((Transfer)o).getId()
			|| query == ((Transfer)o).getQuery());
	}


	public void update(Observable o,
			   Object param) {
		boolean hasChanged;

		if (!(o instanceof FCPTransferQuery) || id < 0) {
			return;
		}

		hasChanged = false;

		final FCPTransferQuery query = (FCPTransferQuery)o;

		if (query.isFinished()) {
			o.deleteObserver(this);
			updateDateEnd(query.isSuccessful());
			hasChanged = true;
		}

		if (query.getFileKey() != null &&
		    (this.key == null
		     || !this.key.equals(query.getFileKey()))) {
			updateKey(query.getFileKey());
			key = query.getFileKey();
			hasChanged = true;
		}

		if (query.getFileSize() != size) {
			updateSize(query.getFileSize());
			size = query.getFileSize();
			hasChanged = true;
		}

		if (hasChanged && table != null)
			table.refresh();
	}

}
