package org.tigris.subversion.clientadapter;

import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends Plugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.tigris.subversion.clientadapter";

	// The shared instance
	private static Activator plugin;
	
	private AdapterManager adapterManager;
	
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
		adapterManager = new AdapterManager();
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.runtime.Plugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		adapterManager = null;
		super.stop(context);
	}
	
	public ISVNClientAdapter getClientAdapter(String id) {
		if (id == null)
			return this.getAnyClientAdapter();
		Map wrappers = adapterManager.getClientWrappers();
		ISVNClientWrapper wrapper = (ISVNClientWrapper) wrappers.get(id);
		if (wrapper == null || !wrapper.isAvailable()) {
			return null;
		}
		return wrapper.getAdapter();
	}
	
	public ISVNClientAdapter getAnyClientAdapter() {
		Map wrappers = adapterManager.getClientWrappers();
		if (wrappers.isEmpty())
			return null;
		ISVNClientWrapper wrapper = null;
		for (Iterator iterator = wrappers.values().iterator(); iterator.hasNext();) {
			wrapper = (ISVNClientWrapper) iterator.next();
			if (wrapper.isAvailable())
				break;
		}
		if (wrapper == null)
			return null;
		return wrapper.getAdapter();
	}
	
	public ISVNClientWrapper[] getAllClientWrappers() {
		Map wrappers = adapterManager.getClientWrappers();
		return (ISVNClientWrapper[])wrappers.values().toArray(new ISVNClientWrapper[wrappers.size()]);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

}
