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
import java.util.Iterator;

import org.eclipse.core.runtime.IBundleGroupProvider;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.tigris.subversion.subclipse.tools.usage.googleanalytics.ISubclipseEclipseEnvironment;
import org.tigris.subversion.subclipse.tools.usage.googleanalytics.eclipse.AbstractEclipseEnvironment;
import org.tigris.subversion.subclipse.tools.usage.internal.SubclipseToolsUsageActivator;

public class SubclipseEclipseEnvironment extends AbstractEclipseEnvironment implements ISubclipseEclipseEnvironment {

	private static final char SUBCLIPSE_COMPONENTS_DELIMITER = '-';

	public SubclipseEclipseEnvironment(String accountName, String hostName, IEclipsePreferences preferences) {
		super(accountName, hostName, preferences);
	}

	public String getKeyword() {
		Collection subclipseComponentNames = SubclipseComponents.getComponentIds(getBundleGroupProviders());
		return bundleGroupsToKeywordString(subclipseComponentNames);
	}

	protected IBundleGroupProvider[] getBundleGroupProviders() {
		return Platform.getBundleGroupProviders();
	}

	private String bundleGroupsToKeywordString(Collection subclipseComponentNames) {
		char delimiter = SUBCLIPSE_COMPONENTS_DELIMITER;
		StringBuffer builder = new StringBuffer();
		Iterator iter = subclipseComponentNames.iterator();
		while (iter.hasNext()) {
			String componentName = (String)iter.next();
			builder.append(componentName);
			if (iter.hasNext()) {
				builder.append(delimiter);
			}
		}
		return builder.toString();
	}

	public String getSubclipseVersion() {
		return SubclipseToolsUsageActivator.getDefault().getBundle().getHeaders().get("Bundle-Version").toString();
	}

	public boolean isLinuxDistro() {
		return getLinuxDistroNameAndVersion() != null;
	}
}