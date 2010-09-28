package org.tigris.subversion.subclipse.tools.usage.preferences;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.osgi.service.prefs.BackingStoreException;
import org.tigris.subversion.subclipse.tools.usage.internal.SubclipseToolsUsageActivator;
import org.tigris.subversion.subclipse.tools.usage.util.StatusUtils;

public class UsageReportPreferencesUtils {
		
	private UsageReportPreferencesUtils() {
	}

	public static IEclipsePreferences getPreferences() {
		return new ConfigurationScope().getNode(SubclipseToolsUsageActivator.PLUGIN_ID);
	}
	
	public static IPersistentPreferenceStore getStore() {
		return new ScopedPreferenceStore(new ConfigurationScope(), SubclipseToolsUsageActivator.PLUGIN_ID);
	}

	public static void checkedSavePreferences(IEclipsePreferences preferences, Plugin plugin, String message) {
		try {
			preferences.flush();
		} catch (BackingStoreException e) {
			Object[] messageArguments = { preferences.absolutePath() };
			IStatus status = StatusUtils.getErrorStatus(plugin.getBundle().getSymbolicName(),
					message,
					e, messageArguments);
			plugin.getLog().log(status);
		}

	}
}
