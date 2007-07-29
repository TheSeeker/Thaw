package thaw.plugins.miniFrost.frostKSK;

import java.util.Observer;
import java.util.Observable;
import java.util.Date;

import thaw.fcp.*;
import thaw.plugins.signatures.Identity;

import thaw.core.Logger;
import thaw.plugins.miniFrost.interfaces.Board;
import thaw.core.I18n;


public class KSKDraft
	implements thaw.plugins.miniFrost.interfaces.Draft, Observer {

	private KSKMessage inReplyTo;
	private KSKBoard board;

	private String subject;
	private String txt;
	private String nick;
	private Identity identity;
	private Date date;


	public KSKDraft(KSKBoard board, KSKMessage inReplyTo) {
		this.board = board;
		this.inReplyTo = inReplyTo;
	}

	public String getSubject() {
		if (subject != null)
			return subject;

		if (inReplyTo != null) {
			String subject = inReplyTo.getSubject();
			if (subject.indexOf("Re: ") == 0)
				return subject;
			return "Re: "+subject;
		}

		return "";
	}

	public String getText() {
		if (txt != null)
			return txt;

		String txt = "";

		if (inReplyTo != null) {
			txt = inReplyTo.getRawMessage();
			if (txt == null) txt = "";
			else txt = (txt.trim() + "\n\n");
		}

		txt += "----- $sender$ ----- $dateAndTime$GMT -----\n\n";

		return txt;
	}

	public boolean allowUnsignedPost() {
		return true;
	}

	public void setSubject(String txt) {
		subject = txt;
	}

	public void setText(String txt) {
		this.txt = txt;
	}

	/**
	 * @param identity if null, unsigned post
	 */
	public void setAuthor(String nick, Identity identity) {
		this.nick = nick;
		this.identity = identity;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public boolean addAttachment(java.io.File file) {
		return true;
	}

	public boolean addAttachment(Board board) {
		return true;
	}

	public boolean removeAttachment(java.io.File file) {
		return true;
	}

	public boolean removeAttachment(Board board) {
		return true;
	}


	private java.io.File fileToInsert;
	private FCPQueueManager queueManager;
	private int revUsed;

	private boolean waiting;
	private boolean posting;

	public void notifyPlugin() {
		board.getFactory().getPlugin().getPanel().update(this);
	}

	public boolean isWaiting() {
		return waiting;
	}

	public boolean isPosting() {
		return posting;
	}

	public void post(FCPQueueManager queueManager) {
		this.queueManager = queueManager;

		waiting = true;
		posting = false;
		notifyPlugin();

		/* we start immediatly a board refresh (we will need it) */
		synchronized(board) {
			board.addObserver(this);
			board.refresh(1 /* until today */);

			KSKMessageParser generator = new KSKMessageParser( ((inReplyTo != null) ?
									    inReplyTo.getMsgId() :
									    null),
									  nick,
									  subject,
									  date,
									  null, /* recipient */
									  board.getName(),
									  txt,
									  ((identity != null) ?
									   identity.getPublicKey() :
									   null),
									  null,
									  identity);

			fileToInsert = generator.generateXML();
			fileToInsert.deleteOnExit();
		}
	}


	private String getKey(Date date, int rev) {
		java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyy.M.d");
		//formatter.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));

		StringBuffer keyBuf = new StringBuffer(KSKMessage.KEY_HEADER);

		keyBuf = formatter.format(date, keyBuf, new java.text.FieldPosition(0));
		keyBuf.append("-"+board.getName()+"-");
		keyBuf.append(Integer.toString(rev));
		keyBuf.append(".xml");

		return keyBuf.toString();
	}

	private void startInsertion() {
		waiting = false;
		posting = true;
		notifyPlugin();

		String key = getKey(date, revUsed);

		Logger.info(this, "Inserting : KSK@"+key);

		FCPClientPut clientPut = new FCPClientPut(fileToInsert,
							  FCPClientPut.KEY_TYPE_KSK,
							  -1, /* rev : we specify it ouselves in the key name */
							  key,
							  null, /* privateKey */
							  2, /* priority */
							  false,
							  FCPClientPut.PERSISTENCE_FOREVER);
		clientPut.addObserver(this);
		queueManager.addQueryToTheRunningQueue(clientPut);
	}


	private boolean isBoardUpToDateForToday() {
		if (!board.isRefreshing()
		    || (KSKBoard.getMidnight(board.getCurrentlyRefreshedDate()).getTime()
			< KSKBoard.getMidnight(date).getTime()) ) {
			return true;
		}
		return false;
	}


	public void update(Observable o, Object param) {
		if (o instanceof Board) {
			synchronized(board) {
				if (fileToInsert == null || !isBoardUpToDateForToday())
					return;
				revUsed = board.getNextNonDownloadedRev(date, -1);
			}

			startInsertion();
		}

		if (o instanceof FCPClientPut) {
			FCPClientPut put = (FCPClientPut)o;

			if (put.isFinished() && put.isSuccessful()) {
				posting = false;
				waiting = false;
				notifyPlugin();

				put.deleteObserver(this);
				put.stop(queueManager);
				queueManager.remove(put);

				fileToInsert.delete();

				Logger.info(this, "Message sent.");

				String announce = I18n.getMessage("thaw.plugin.miniFrost.messageSent");
				announce = announce.replaceAll("X", board.toString());

				thaw.plugins.TrayIcon.popMessage(board.getFactory().getCore().getPluginManager(),
								 "MiniFrost",
								 announce);


			} else if (put.isFinished() && !put.isSuccessful()) {
				if (put.getPutFailedCode() != 9) { /* !Collision */
					Logger.error(this, "Can't insert the message on the board '"+
						     board.toString()+"' ; Code: "+Integer.toString(put.getPutFailedCode()));
					waiting = false;
					posting = false;
					notifyPlugin();
					return;
				}

				put.deleteObserver(this);
				put.stop(queueManager);
				queueManager.remove(put);

				revUsed = board.getNextNonDownloadedRev(date, revUsed);
				startInsertion();
			}
		}

	}
}
