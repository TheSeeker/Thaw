package freenet.crypt;

import java.util.Hashtable;

/**
 * This is a simplified version of the Freenet SimpleFieldSet class.
 * It was created in order to simplify the reuse of the cryptographic
 * class of the node.
 */
public class SimpleFieldSet {
	private Hashtable ht;

	public SimpleFieldSet(boolean mooh) {
		ht = new Hashtable();
	}

	public void putSingle(String key, String val) {
		ht.put(key, val);
	}

	public String get(String key) {
		return ((String)ht.get(key));
	}

}
