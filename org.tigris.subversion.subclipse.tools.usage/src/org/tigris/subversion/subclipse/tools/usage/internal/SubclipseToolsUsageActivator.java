package org.tigris.subversion.subclipse.tools.usage.internal;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class SubclipseToolsUsageActivator extends Plugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.tigris.subversion.subclipse.tools.usage";

	// The shared instance
	private static SubclipseToolsUsageActivator plugin;

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

}
