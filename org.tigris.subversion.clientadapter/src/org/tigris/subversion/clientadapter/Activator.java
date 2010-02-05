package org.tigris.subversion.clientadapter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends Plugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.tigris.subversion.clientadapter";
	
	public static final String LOAD_ERROR_HANDLERS = "org.tigris.subversion.clientadapter.loadErrorHandlers";

	// The shared instance
	private static Activator plugin;
	
	private AdapterManager adapterManager;

	// cache of available wrappers
	private Map wrappers;
	
	private ILoadErrorHandler[] loadErrorHandlers;
	
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
		if (wrappers == null)
			wrappers = adapterManager.getClientWrappers();
		ISVNClientWrapper wrapper = (ISVNClientWrapper) wrappers.get(id);
		if (wrapper == null || !wrapper.isAvailable()) {
			return null;
		}
		return wrapper.getAdapter();
	}
	
	public ISVNClientAdapter getAnyClientAdapter() {
		if (wrappers == null)
			wrappers = adapterManager.getClientWrappers();
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
	
	/**
	 * Get all available client wrappers.  This method
	 * also always refreshes the internal cache.  In theory perhaps
	 * a new wrapper could be installed after plugin is started
	 * 
	 * @return
	 */
	public ISVNClientWrapper[] getAllClientWrappers() {
		wrappers = adapterManager.getClientWrappers();
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
	
	public void handleLoadErrors(ISVNClientWrapper clientWrapper) {
		try {
			loadErrorHandlers = getLoadErrorHandlers();
			if (loadErrorHandlers != null) {
				for (int i = 0; i < loadErrorHandlers.length; i++) {
					loadErrorHandlers[i].handleLoadError(clientWrapper);
				}
			}
		} catch (Exception e) {}
	}
	
	private ILoadErrorHandler[] getLoadErrorHandlers() throws Exception {
		if (loadErrorHandlers == null) {
			List handlerList = new ArrayList();
			IExtensionRegistry extensionRegistry = Platform.getExtensionRegistry();
			IConfigurationElement[] configurationElements = extensionRegistry.getConfigurationElementsFor(LOAD_ERROR_HANDLERS);
			for (int i = 0; i < configurationElements.length; i++) {
				IConfigurationElement configurationElement = configurationElements[i];
				ILoadErrorHandler handler = (ILoadErrorHandler)configurationElement.createExecutableExtension("class"); //$NON-NLS-1$
				handlerList.add(handler);
			}
			loadErrorHandlers = new ILoadErrorHandler[handlerList.size()];
			handlerList.toArray(loadErrorHandlers);
		}
		return loadErrorHandlers;
	}

}
