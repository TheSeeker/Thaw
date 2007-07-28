package thaw.plugins.miniFrost.frostKSK;

import thaw.fcp.*;
import thaw.plugins.signatures.Identity;


public class KSKDraft
	implements thaw.plugins.miniFrost.interfaces.Draft {

	public KSKDraft(KSKBoard board, KSKMessage inReplyTo) {

	}

	public String getInitialText() {
		return "";
	}

	public boolean allowUnsignedPost() {
		return true;
	}


	public void setText(String txt) {

	}

	/**
	 * @param identity if null, unsigned post
	 */
	public void setAuthor(String nick, Identity identity) {

	}

	public void post(FCPQueueManager queueManager) {

	}
}
