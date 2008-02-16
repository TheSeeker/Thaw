package thaw.fcp;

import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.Hashtable;

import thaw.core.Logger;

/**
 * This class is designed to handle a LOT of FCPTransferQuery
 * without overloading the observer/observable model.
 * FCPTransferQueries must be non-persistent !
 * It's useful when you have a lot of ULPR to handle
 * @author jflesch
 */
public class FCPMetaTransferQuery extends Observable implements Observer {

	private final FCPQueueManager queueManager;
	private final Hashtable idToQuery;
	private final String idField;
	
	public FCPMetaTransferQuery(FCPQueueManager queueManager) {
		this(queueManager, "Identifier");
	}
	
	private FCPMetaTransferQuery(FCPQueueManager queueManager, String idField) {
		this.queueManager = queueManager;
		this.idToQuery = new Hashtable();
		this.idField = idField;
		
		queueManager.getQueryManager().addObserver(this);
	}
	
	private void add(String id, FCPTransferQuery query) {
		idToQuery.put(id, query);
	}

	/***
	 * Will call query.start() itself
	 * @param query
	 * @return
	 */
	public boolean start(FCPTransferQuery query) {
		if (query == null)
			return false;
		
		/* safety check */
		if (query.isPersistent()) {
			Logger.error(this, "A persistent query was given to FCPMetaTransferQuery ! this should never happen !");
			try {
				throw new Exception("meh");
			} catch(Exception e) {
				e.printStackTrace();
			}

			return false;
		}
		
		/* safety check */
		if (!(query instanceof Observer)) {
			Logger.error(this, "A non-observer query ("+query.getClass().getName()+") was given to FCPMetaTransferQuery ! this should never happen !");
			try {
				throw new Exception("meh");
			} catch(Exception e) {
				e.printStackTrace();
			}

			return false;
		}
		
		/* here we start for real */
		boolean r = query.start(queueManager);
		
		if (r) {
			/* Ugly hack to replace the query manager by the metaTransferQuery */
			add(query.getIdentifier(), query);
			query.addObserver(this);
			queueManager.getQueryManager().deleteObserver((Observer)query);
		}
		
		return r;
	}
	
	private void remove(String id) {
		idToQuery.remove(id);
	}
	
	/**
	 * Will call query.stop() itself
	 * Can't work atm on non-persistent requests .... (node r1111)
	 * @param query
	 * @return
	 */
	public boolean stop(FCPTransferQuery query) {
		
		query.deleteObserver(this);
		
		boolean r = true;
		
		if (!query.isFinished())
			r = query.stop(queueManager);
		
		if (r) {
			remove(query.getIdentifier());
		} else {
			query.addObserver(this);
		}
		
		return r;
	}


	public void stopAll() {
		for (Iterator it = idToQuery.values().iterator(); it.hasNext() ; ) {
			
			FCPTransferQuery query = (FCPTransferQuery)it.next();
			stop(query);
			
		}
	}


	public void update(Observable o, Object param) {
		if (o instanceof FCPQueryManager) {
			
			FCPMessage msg = (FCPMessage)param;
			String targetId;
			
			if (msg != null && (targetId = msg.getValue(idField)) != null) {
				Observer obs = (Observer)(idToQuery.get(targetId));
				
				if (obs != null) {
					/* we redirect only to the target FCPTransferQuery */
					obs.update(o, param);
				}
			}
			
		} if (o instanceof FCPTransferQuery) {
			FCPTransferQuery q = (FCPTransferQuery)o;

			if (q.isFinished()) {
				q.deleteObserver(this);
				remove(q.getIdentifier());
			}

			setChanged();
			notifyObservers(o);
		}
	}
	
}
