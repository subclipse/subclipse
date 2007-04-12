package org.tigris.subversion.clientadapter;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

public class AdapterManager {

	// All available client adapters
	private Map clients;
	
	public synchronized Map getClientWrappers() {
		if (clients == null) {
			clients = new HashMap();
			IExtensionRegistry pluginRegistry = Platform.getExtensionRegistry();
			IConfigurationElement[] configurationElements = pluginRegistry.getConfigurationElementsFor("org.tigris.subversion.clientadapter.wrapper");
			for (int i = 0; i < configurationElements.length; i++) {
				IConfigurationElement configurationElement = configurationElements[i];
				try {
					ISVNClientWrapper client = (ISVNClientWrapper)configurationElement.createExecutableExtension("class");
					client.setDisplayName(configurationElement.getAttribute("name"));
					clients.put(client.getAdapterID(), client);
				} catch(Exception e) {
				}
			}	
		}
		return clients;
	}
}
