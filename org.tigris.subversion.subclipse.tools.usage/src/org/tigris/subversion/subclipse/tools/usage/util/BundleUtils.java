package org.tigris.subversion.subclipse.tools.usage.util;

import java.util.Collection;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.Assert;
import org.osgi.framework.Bundle;
import org.tigris.subversion.subclipse.tools.usage.util.collectionfilter.CollectionFilterUtils;
import org.tigris.subversion.subclipse.tools.usage.util.collectionfilter.ICollectionFilter;

public class BundleUtils {

	/**
	 * Returns the bundles among the available ones that match the given filter.
	 * 
	 * @param filter
	 *            the filter to match the available bundles against
	 * @param bundles
	 *            the bundles
	 * @return the bundles that match the given filter
	 */
	public static void getBundles(ICollectionFilter filter, Collection filteredBundleCollection,
			Bundle[] bundles) {
		CollectionFilterUtils.filter(filter, bundles, filteredBundleCollection);
	}

	/**
	 * Returns the bundles that have a symbolic name that match the given regex.
	 * 
	 * @param bundleSymbolicNameRegex
	 *            the symbolic name regex to match.
	 * @param bundles
	 *            the bundles
	 * @return the bundles
	 */
	public static void getBundles(String bundleSymbolicNameRegex, Collection filteredBundleCollection,
			Bundle[] bundles) {
		getBundles(new BundleSymbolicNameFilter(bundleSymbolicNameRegex)
				, filteredBundleCollection
				, bundles);
	}

	/**
	 * A filter that matches bundles against a given symbolic name regex.
	 */
	public static class BundleSymbolicNameFilter implements ICollectionFilter {

		private Pattern pattern;

		public BundleSymbolicNameFilter(String symbolicNameRegex) {
			this.pattern = Pattern.compile(symbolicNameRegex);
		}

		public boolean matches(Object object) {
			if (object instanceof Bundle) {
				Bundle bundle = (Bundle)object;
				Assert.isTrue(bundle != null);
	
				return pattern.matcher(bundle.getSymbolicName()).matches();
			}
			return false;
		}
	}
}
