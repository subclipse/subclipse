package org.tigris.subversion.subclipse.tools.usage.preferences;

import org.eclipse.jface.preference.IPreferenceStore;
import org.osgi.service.prefs.BackingStoreException;
import org.tigris.subversion.subclipse.tools.usage.internal.SubclipseToolsUsageActivator;

public class UsageReportPreferences {

	public static void setEnabled(boolean enabled) {
		SubclipseToolsUsageActivator.getDefault().getPreferenceStore().setValue(IUsageReportPreferenceConstants.USAGEREPORT_ENABLED_ID, String.valueOf(enabled));
	}

	public static boolean isEnabled() {
		return SubclipseToolsUsageActivator.getDefault().getPreferenceStore().getBoolean(IUsageReportPreferenceConstants.USAGEREPORT_ENABLED_ID);
	}

	public static boolean isAskUser() {		
		return SubclipseToolsUsageActivator.getDefault().getPreferenceStore().getBoolean(IUsageReportPreferenceConstants.ASK_USER_USAGEREPORT_ID);
	}

	public static void setAskUser(boolean askUser) {
		SubclipseToolsUsageActivator.getDefault().getPreferenceStore().setValue(IUsageReportPreferenceConstants.ASK_USER_USAGEREPORT_ID, askUser);
	}

	public static void flush() throws BackingStoreException {
		UsageReportPreferencesUtils.getPreferences().flush();
	}

	public static IPreferenceStore createPreferenceStore() {
		return SubclipseToolsUsageActivator.getDefault().getPreferenceStore();
	}
}
