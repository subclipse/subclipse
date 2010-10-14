package org.tigris.subversion.subclipse.tools.usage.internal;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.tigris.subversion.subclipse.tools.usage.googleanalytics.ISubclipseEclipseEnvironment;
import org.tigris.subversion.subclipse.tools.usage.preferences.UsageReportPreferencesUtils;
import org.tigris.subversion.subclipse.tools.usage.reporting.ReportingMessages;
import org.tigris.subversion.subclipse.tools.usage.reporting.SubclipseEclipseEnvironment;

/**
 * The activator class controls the plug-in life cycle
 */
public class SubclipseToolsUsageActivator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.tigris.subversion.subclipse.tools.usage";

	// The shared instance
	private static SubclipseToolsUsageActivator plugin;
	
	private ISubclipseEclipseEnvironment eclipseEnvironment;

	public SubclipseToolsUsageActivator() {
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static SubclipseToolsUsageActivator getDefault() {
		return plugin;
	}
	
	public ISubclipseEclipseEnvironment getSubclipseEclipseEnvironment() {
		if (eclipseEnvironment == null) {
			eclipseEnvironment = createEclipseEnvironment();
		}
		return eclipseEnvironment;
	}

	private ISubclipseEclipseEnvironment createEclipseEnvironment() {
		return new SubclipseEclipseEnvironment(
				ReportingMessages.UsageReport_GoogleAnalytics_Account, ReportingMessages.UsageReport_HostName,
				UsageReportPreferencesUtils.getPreferences());
	}

}
