package org.tigris.subversion.subclipse.tools.usage.util.collectionfilter;

public interface ICollectionFilter {

	/**
	 * Matches.
	 * 
	 * @param bundle
	 *            the bundle
	 * @return true, if successful
	 */
	public boolean matches(Object object);
}