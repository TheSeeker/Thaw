package thaw.plugins.miniFrost.interfaces;

import thaw.plugins.signatures.Identity;


public interface Author {
	/**
	 * @return null for unsigned messages
	 */
	public Identity getIdentity();

	/**
	 * must return the value of toString(true)
	 * @see toString(boolean)
	 */
	public String toString();

	/**
	 * Name of the author
	 * - Signed: Author@[hash of the public key]
	 * - Unsigned: Author
	 * '@' in the author name must be replaced
	 * by a '_'
	 *
	 * @param noDup if set to True, returns just the nick ; else it can specify
	 *              if the nick is a duplicate or not
	 */
	public String toString(boolean noDup);
}
