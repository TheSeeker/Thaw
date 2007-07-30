package thaw.plugins.miniFrost.frostKSK;

import java.util.Observer;
import java.util.Observable;
import java.util.Date;

import java.util.Vector;

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

	public Identity getAuthorIdentity() {
		return identity;
	}

	public String getAuthorNick() {
		return nick;
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

	public Vector getAttachments() {
		return null;
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
		}

		/* first check */
		update(board, null);
	}


	private void startInsertion() {
		waiting = false;
		posting = true;
		notifyPlugin();

		String privateKey = board.getPrivateKey();
		String name = board.getNameForInsertion(date, revUsed);
		int keyType = board.getKeyType();

		if (keyType == FCPClientPut.KEY_TYPE_KSK)
			Logger.info(this, "Inserting : KSK@"+name);
		else
			Logger.info(this, "Insertion : SSK@"+privateKey+name);

		FCPClientPut clientPut = new FCPClientPut(fileToInsert,
							  keyType,
							  -1, /* rev : we specify it ouselves in the key name */
							  name,
							  privateKey, /* privateKey */
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
				board.deleteObserver(this);
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
					put.deleteObserver(this);

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

	public Board getBoard() {
		return board;
	}
}
