package thaw.core;


import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import thaw.fcp.FCPClientGet;
import thaw.fcp.FCPClientPut;
import thaw.fcp.FCPQueueManager;
import thaw.fcp.FCPTransferQuery;

/**
 * Used when Thaw start and stop: Save the query not running (-> waiting in the Thaw queue)
 */
public class QueueKeeper {
	private final static int MIN_PRIORITY = 6;

	/**
	 * Used to be able to call Logger functions.
	 */
	public QueueKeeper() {

	}


	private static void loadQuery(final FCPQueueManager queueManager, final Element queryEl, final boolean runningQueue) {
		FCPTransferQuery newQuery = null;
		final HashMap params = new HashMap();

		final NodeList paramList = queryEl.getElementsByTagName("param");

		for(int i = 0;i < paramList.getLength(); i++) {
			final Node param = paramList.item(i);

			if((param != null) && (param.getNodeType() == Node.ELEMENT_NODE)) {
				final Element paramEl = (Element)param;

				params.put(paramEl.getAttribute("name"),
					   paramEl.getAttribute("value"));
			}
		}

		if((queryEl.getAttribute("type") == null)
		   || "1".equals( queryEl.getAttribute("type") )) {
			newQuery = new FCPClientGet(queueManager, params);
		} else {
			newQuery = new FCPClientPut(queueManager, params);
		}

		if(runningQueue)
			queueManager.addQueryToTheRunningQueue(newQuery, false);
		else
			queueManager.addQueryToThePendingQueue(newQuery);

	}


	private static void loadQueries(final FCPQueueManager queueManager, final Element queriesEl, final boolean runningQueue) {
		final NodeList queries = queriesEl.getElementsByTagName("query");

		for(int i = 0;i < queries.getLength(); i++) {
			final Node queryNode = queries.item(i);

			if((queryNode != null) && (queryNode.getNodeType() == Node.ELEMENT_NODE)) {
				QueueKeeper.loadQuery(queueManager, (Element)queryNode, runningQueue);
			}
		}
	}


	public static boolean loadQueue(final FCPQueueManager queueManager, final String fileName) {
		final File file = new File(fileName);

		if(!file.exists() || !file.canRead()) {
			Logger.info(new QueueKeeper(), "Unable to find previous queue state file '"+file.getPath()+"' => Not reloaded from file.");
			return false;
		}


		Document xmlDoc = null;
		DocumentBuilderFactory xmlFactory = null;
		DocumentBuilder xmlBuilder = null;

		Element rootEl = null;

		xmlFactory = DocumentBuilderFactory.newInstance();

		try {
			xmlBuilder = xmlFactory.newDocumentBuilder();
		} catch(final javax.xml.parsers.ParserConfigurationException e) {
			Logger.warning(new QueueKeeper(), "Unable to load queue because: "+e.toString());
			return false;
		}

		try {
			xmlDoc = xmlBuilder.parse(file);
		} catch(final org.xml.sax.SAXException e) {
			Logger.warning(new QueueKeeper(), "Unable to load queue because: "+e.toString());
			return false;
		} catch(final java.io.IOException e) {
			Logger.warning(new QueueKeeper(), "Unable to load queue because: "+e.toString());
			return false;
		}

		rootEl = xmlDoc.getDocumentElement();


		final NodeList runningQueues = rootEl.getElementsByTagName("runningQueue");

		for(int i = 0;i < runningQueues.getLength(); i++) {

			final Node runningQueueNode = runningQueues.item(i);

			if((runningQueueNode != null) && (runningQueueNode.getNodeType() == Node.ELEMENT_NODE)) {
				QueueKeeper.loadQueries(queueManager, (Element)runningQueueNode, true);
			}
		}

		final NodeList pendingQueues = rootEl.getElementsByTagName("pendingQueue");

		for(int i = 0;i < pendingQueues.getLength(); i++) {

			final Node pendingQueueNode = pendingQueues.item(i);

			if((pendingQueueNode != null) && (pendingQueueNode.getNodeType() == Node.ELEMENT_NODE)) {
				QueueKeeper.loadQueries(queueManager, (Element)pendingQueueNode, false);
			}
		}

		return true;
	}


	private static Element saveQuery(final FCPTransferQuery query, final Document xmlDoc) {
		if(!query.isPersistent())
			return null;

		if(query.isPersistent() && (query.isRunning() || query.isFinished()))
			return null;

		final HashMap params = query.getParameters();

		final Element queryEl = xmlDoc.createElement("query");

		queryEl.setAttribute("type", Integer.toString(query.getQueryType()));

		for(final Iterator keys = params.keySet().iterator();
		    keys.hasNext();) {

			final String key = (String)keys.next();

			final Element paramEl = xmlDoc.createElement("param");

			paramEl.setAttribute("name", key);
			paramEl.setAttribute("value", ((String)params.get(key)) );

			queryEl.appendChild(paramEl);
		}

		return queryEl;
	}


	public static boolean saveQueue(final FCPQueueManager queueManager, final String fileName) {
		final Vector[] pendingQueues = queueManager.getPendingQueues();

		boolean needed = false;

		for(int i = 0 ; i < pendingQueues.length ; i++) {
			if(pendingQueues[i].size() > 0) {
				needed = true;
				break;
			}
		}

		if(!needed) {
			Logger.info(new QueueKeeper(), "Nothing in the pending queue to save.");
			final File file = new File(fileName);
			file.delete(); // Else we may reload something that we shouldn't when restarting
			return true;
		}

		final File file = new File(fileName);
		StreamResult fileOut;

		try {
			if( (!file.exists() && !file.createNewFile())
			    || !file.canWrite()) {
				Logger.warning(new QueueKeeper(), "Unable to write config file '"+file.getPath()+"' (can't write)");
				return false;
			}
		} catch(final java.io.IOException e) {
			Logger.warning(new QueueKeeper(), "Error while checking perms to save config: "+e);
		}


		fileOut = new StreamResult(file);

		Document xmlDoc = null;
		DocumentBuilderFactory xmlFactory = null;
		DocumentBuilder xmlBuilder = null;
		DOMImplementation impl = null;

		Element rootEl = null;

		xmlFactory = DocumentBuilderFactory.newInstance();

		try {
			xmlBuilder = xmlFactory.newDocumentBuilder();
		} catch(final javax.xml.parsers.ParserConfigurationException e) {
			Logger.error(new QueueKeeper(), "Unable to save queue because: "+e.toString());
			return false;
		}


		impl = xmlBuilder.getDOMImplementation();

		xmlDoc = impl.createDocument(null, "queue", null);

		rootEl = xmlDoc.getDocumentElement();

		final Element pendingQueueEl = xmlDoc.createElement("pendingQueue");

		for(int i = 0 ; i <= QueueKeeper.MIN_PRIORITY ; i++) {

			for(final Iterator runIt = pendingQueues[i].iterator() ;
			    runIt.hasNext(); ) {

				final FCPTransferQuery query = (FCPTransferQuery)runIt.next();

				final Element toSave = QueueKeeper.saveQuery(query, xmlDoc);

				if(toSave != null)
					pendingQueueEl.appendChild(toSave);

			}

		}

		rootEl.appendChild(pendingQueueEl);

		/* Serialization */
		final DOMSource domSource = new DOMSource(xmlDoc);
		final TransformerFactory transformFactory = TransformerFactory.newInstance();

		Transformer serializer;

		try {
			serializer = transformFactory.newTransformer();
		} catch(final javax.xml.transform.TransformerConfigurationException e) {
			Logger.error(new QueueKeeper(), "Unable to save queue because: "+e.toString());
			return false;
		}

		serializer.setOutputProperty(OutputKeys.ENCODING,"ISO-8859-1");
		serializer.setOutputProperty(OutputKeys.INDENT,"yes");

		/* final step */
		try {
			serializer.transform(domSource, fileOut);
		} catch(final javax.xml.transform.TransformerException e) {
			Logger.error(new QueueKeeper(), "Unable to save queue because: "+e.toString());
			return false;
		}

		return true;
	}

}
