package org.tigris.subversion.clientadapter.javahl;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleContext;
import org.tigris.subversion.clientadapter.ISVNClientWrapper;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.javahl.JhlClientAdapter;
import org.tigris.subversion.svnclientadapter.javahl.JhlClientAdapterFactory;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends Plugin implements ISVNClientWrapper{

	// The plug-in ID
	public static final String PLUGIN_ID = "org.tigris.subversion.clientadapter.javahl";

	// The shared instance
	private static Activator plugin;
	
	private String displayName;
	
	private String version;
	
	private boolean loadErrorLogged = false;
	
	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.runtime.Plugins#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.runtime.Plugin#stop(org.osgi.framework.BundleContext)
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
	public static Activator getDefault() {
		return plugin;
	}

	public ISVNClientAdapter getAdapter() {
		if (this.isAvailable())
			return new JhlClientAdapter();
		else
			return null;
	}

	public String getAdapterID() {
		return JhlClientAdapterFactory.JAVAHL_CLIENT;
	}

	public String getVersionString() {
		return getVersionSynchronized();
	}

	private synchronized String getVersionSynchronized() {
		if (version == null) {
			if (this.isAvailable()) {
				JhlClientAdapter adapter = new JhlClientAdapter();
				version = adapter.getNativeLibraryVersionString();
			} else {
				version = "Not Available";
			}
		}
		return version;
	}

	public boolean isAvailable() {
		boolean available = JhlClientAdapterFactory.isAvailable();
		if (!available && !loadErrorLogged) {
			getLog().log(new Status(IStatus.INFO, PLUGIN_ID, 0, getLoadErrors(), null));
			loadErrorLogged = true;
			org.tigris.subversion.clientadapter.Activator.getDefault().handleLoadErrors(this);
		}
		return available;
	}

	public void setDisplayName(String string) {
		displayName = string;
	}

	public String getDisplayName() {
		return displayName + " " + this.getVersionString();
	}

	public String getLoadErrors() {
		return JhlClientAdapterFactory.getLibraryLoadErrors();
	}

}
