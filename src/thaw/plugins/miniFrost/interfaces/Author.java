package thaw.plugins.miniFrost.interfaces;

import thaw.plugins.signatures.Identity;


public interface Author {
	/**
	 * @return null for unsigned messages
	 */
	public Identity getIdentity();

	/**
	 * Name of the author
	 * - Signed: Author@[hash of the public key]
	 * - Unsigned: Author
	 * '@' in the author name must be replaced
	 * by a '_'
	 */
	public String toString();
}
