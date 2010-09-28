package org.tigris.subversion.subclipse.tools.usage.util;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;

public class LoggingUtils {

	public static boolean isPluginTracingEnabled(Plugin plugin) {
		return plugin != null && plugin.isDebugging();
	}

	public static void log(IStatus status, Plugin plugin) {
		if (status.getSeverity() == IStatus.INFO && !isPluginTracingEnabled(plugin)) {
			return;
		}
		plugin.getLog().log(status);
	}
}
