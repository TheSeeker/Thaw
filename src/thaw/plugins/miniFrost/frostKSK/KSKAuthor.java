package thaw.plugins.miniFrost.frostKSK;

import thaw.plugins.signatures.Identity;



public class KSKAuthor
	implements thaw.plugins.miniFrost.interfaces.Author {

	private String nick;
	private Identity identity;

	public KSKAuthor(String nick,
			 Identity identity) {
		this.nick = nick;
		this.identity = identity;
	}

	public Identity getIdentity() {
		return identity;
	}

	public String toString() {
		return nick;
	}
}
