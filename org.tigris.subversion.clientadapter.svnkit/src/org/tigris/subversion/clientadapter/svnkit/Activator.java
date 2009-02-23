package org.tigris.subversion.clientadapter.svnkit;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleContext;
import org.tigris.subversion.clientadapter.ISVNClientWrapper;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends Plugin implements ISVNClientWrapper {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.tigris.subversion.clientadapter.svnkit";

	// Extension point
	public static final String PT_SVNCONNECTORFACTORY = "svnconnectorfactory"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;

	private String displayName;
	private String version;

//	private ISVNConnectorFactory factory=null;
	private boolean runOnce = false;
	
	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
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
	public static Activator getDefault() {
		return plugin;
	}

	public ISVNClientAdapter getAdapter() {
//		if (this.isAvailable())
//			return new SvnKitClientAdapter(null, null, getPluggedInSVNConnectorFactor());
//		else
			return null;
	}

	public String getAdapterID() {
		return "svnkit";
	}

	public String getVersionString() {
		return getVersionSynchronized();
	}

	private synchronized String getVersionSynchronized() {
		if (version == null) {
//			if (this.isAvailable()) {
//				SVNClientImpl adapter = SVNClientImpl.newInstance();
//				version = adapter.getVersion().toString();
//			} else
				version = "Not Available";
			}
		return version;
	}

	public boolean isAvailable() {
//		return SvnKitClientAdapterFactory.isAvailable();
		return false;
	}

	public void setDisplayName(String string) {
		displayName = string;
	}

	public String getDisplayName() {
		return displayName + " " + this.getVersionString();
	}

	public String getLoadErrors() {
		if (this.isAvailable())
			return "";
		return "SVNKit is not currently compatible with Subversion 1.6.0";
//		return "Class org.tmatesoft.svn.core.javahl.SVNClientImpl not found.\nInstall the SVNKit plug-in from http://www.svnkit.com/";
	}

//	private ISVNConnectorFactory getPluggedInSVNConnectorFactor() {
//		if (!runOnce) {
//			runOnce = true;
//			IExtension[] extensions = Platform.getExtensionRegistry().getExtensionPoint(PLUGIN_ID, PT_SVNCONNECTORFACTORY).getExtensions();
//			for(int i=0; i<extensions.length; i++) {
//				IExtension extension = extensions[i];
//				IConfigurationElement[] configs = extension.getConfigurationElements();
//				if (configs.length == 0) {
//					Activator.log(IStatus.ERROR, NLS.bind("SVNConnectorFactory {0} is missing required fields", new Object[] {extension.getUniqueIdentifier()}), null);//$NON-NLS-1$ 
//					continue;
//				}
//				try {
//					IConfigurationElement config = configs[0];
//					factory=(ISVNConnectorFactory)config.createExecutableExtension("run");//$NON-NLS-1$ 
//				} catch (CoreException ex) {
//					System.err.println(ex);
//					Activator.log(IStatus.ERROR, NLS.bind("Could not instantiate SVNConnectorFactory for  {0}", new Object[] {extension.getUniqueIdentifier()}), ex);//$NON-NLS-1$ 
//				}
//			}
//		}
//		return factory;
//	}

    /**
     * Log the given exception along with the provided message and severity indicator
     */
    public static void log(int severity, String message, Throwable e) {
        log(new Status(severity, PLUGIN_ID, 0, message, e));
    }

    public static void log(IStatus status) {
		// For now, we'll log the status. However we should do more
		getDefault().getLog().log(status);
	}

}
