package thaw.plugins.signatures;

import freenet.crypt.Yarrow;


/**
 * Not really usefull at the moment.
 * Later it will store the last
 * Note for later: it was really really stupid from my part to call it 'RandomSource' ....
 */
public class RandomSource {

	private static freenet.crypt.RandomSource randomSource = null;

	public static freenet.crypt.RandomSource getRandomSource() {
		if (randomSource == null)
			randomSource = new Yarrow();

		return randomSource;
	}


}
