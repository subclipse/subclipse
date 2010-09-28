package org.tigris.subversion.subclipse.tools.usage.util.collectionfilter;

import java.util.Collection;

public class CollectionFilterUtils {

	/**
	 * Returns the entries that match the given filter.
	 * 
	 * @param filter
	 *            the filter to match the available entries against
	 * @param entries
	 *            the entries to filter
	 * @return the entries that match the given filter
	 */
	public static void filter(ICollectionFilter filter, Object[] entries, Collection targetColletion) {
		for (int i = 0; i < entries.length; i++) {
			if (filter.matches(entries[i])) {
				if (targetColletion != null) {
					targetColletion.add(entries[i]);
				}				
			}
		}
	}
}
