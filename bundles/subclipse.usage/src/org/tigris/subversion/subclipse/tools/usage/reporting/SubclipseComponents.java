/*******************************************************************************
 * Copyright (c) 2010 Subclipse project and others.
 * Copyright (c) 2010 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.tools.usage.reporting;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.runtime.IBundleGroup;
import org.eclipse.core.runtime.IBundleGroupProvider;
import org.tigris.subversion.subclipse.tools.usage.util.collectionfilter.CollectionFilterUtils;
import org.tigris.subversion.subclipse.tools.usage.util.collectionfilter.ICollectionFilter;

public class SubclipseComponents {

	/**
	 * The subclipse tools features to check and report.
	 */
	public static final String[] subclipseFeatureIdentifiers = {
		"org.tigris.subversion.clientadapter.feature",
		"org.tigris.subversion.clientadapter.javahl.feature",
		"org.tigris.subversion.clientadapter.svnkit.feature",
		"org.tigris.subversion.subclipse",
		"org.tigris.subversion.subclipse.graph.feature",
		"com.collabnet.subversion.merge.feature"
	};
	
	public static final String[] subclipseFeatureNames = {
		"CLIENTADAPTER",
		"JAVAHL",
		"SVNKIT",
		"SUBCLIPSE",
		"GRAPH",
		"MERGE"
	};
	
	private static String subclipseVersion;

	private SubclipseComponents() {
		// inhibit instantiation
	}

	/**
	 * Returns the subclipse components that the given bundle group provider
	 * provides
	 * 
	 * @param bundles
	 *            the bundles group providers to check for subclipse components
	 * @return
	 */
	public static Collection getComponentIds(IBundleGroupProvider[] bundleGroupProviders) {
		Set componentNames = new TreeSet();
		for (int i = 0; i < bundleGroupProviders.length; i++) {
			CollectionFilterUtils.filter(
					new SubclipseFeaturesFilter(componentNames)
					, bundleGroupProviders[i].getBundleGroups(), null);			
		}
		return componentNames;
	}
	
	public static String getSubclipseVersion() {
		return subclipseVersion;
	}

	private static class SubclipseFeaturesFilter implements ICollectionFilter {

		private Collection componentNames;

		private SubclipseFeaturesFilter(Collection componentNames) {
			this.componentNames = componentNames;
		}

		public boolean matches(Object object) {
			if (object instanceof IBundleGroup) {
				IBundleGroup bundleGroup = (IBundleGroup)object;
				for (int i = 0; i < subclipseFeatureIdentifiers.length; i++) {
					if (subclipseFeatureIdentifiers[i].equals(bundleGroup.getIdentifier())) {
						
						if (subclipseFeatureNames[i].equalsIgnoreCase("SUBCLIPSE")) {
							subclipseVersion = bundleGroup.getVersion();
						}

						this.componentNames.add(subclipseFeatureNames[i]);
						return true;					
					}
				}
			}
			return false;
		}
	}
}