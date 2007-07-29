package thaw.plugins.miniFrost.frostKSK;

import thaw.plugins.signatures.Identity;
import thaw.core.I18n;


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
		return toString(true);
	}

	public String toString(boolean noDup) {
		if (identity != null) {
			if (noDup || !identity.isDup())
				return identity.toString();
			return I18n.getMessage("thaw.plugin.miniFrost.DUP")+" "+identity.toString();
		}

		return nick;
	}
}
