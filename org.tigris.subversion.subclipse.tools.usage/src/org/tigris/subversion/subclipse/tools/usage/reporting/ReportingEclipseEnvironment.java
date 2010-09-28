package org.tigris.subversion.subclipse.tools.usage.reporting;

import java.util.Collection;
import java.util.Iterator;

import org.eclipse.core.runtime.IBundleGroupProvider;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.tigris.subversion.subclipse.tools.usage.googleanalytics.eclipse.AbstractEclipseEnvironment;
import org.tigris.subversion.subclipse.tools.usage.internal.SubclipseToolsUsageActivator;

public class ReportingEclipseEnvironment extends AbstractEclipseEnvironment {

	private static final char SUBCLIPSE_COMPONENTS_DELIMITER = '-';

	public ReportingEclipseEnvironment(String accountName, String hostName, IEclipsePreferences preferences) {
		super(accountName, hostName, preferences);
	}

	public String getKeyword() {
		Collection subclipseComponentNames = SubclipseComponents.getComponentIds(getBundleGroupProviders());
		return bundleGroupsToKeywordString(subclipseComponentNames );
	}

	protected IBundleGroupProvider[] getBundleGroupProviders() {
		return Platform.getBundleGroupProviders();
	}

	private String bundleGroupsToKeywordString(Collection subclipseComponentNames) {
		char delimiter = SUBCLIPSE_COMPONENTS_DELIMITER;
		StringBuilder builder = new StringBuilder();
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

	public String getAdContent() {
		return getBundleVersion();
	}

	private String getBundleVersion() {
		return SubclipseToolsUsageActivator.getDefault().getBundle().getVersion().toString();
	}
}
