package org.tigris.subversion.subclipse.tools.usage.preferences;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.preference.IPreferenceStore;
import org.osgi.service.prefs.BackingStoreException;
import org.tigris.subversion.subclipse.tools.usage.internal.SubclipseToolsUsageActivator;
import org.tigris.subversion.subclipse.tools.usage.reporting.ReportingMessages;
import org.tigris.subversion.subclipse.tools.usage.util.StatusUtils;

public class UsageReportPreferences {

	public static void setEnabled(boolean enabled) {
		UsageReportPreferencesUtils.getStore().putValue(
				IUsageReportPreferenceConstants.USAGEREPORT_ENABLED_ID, String.valueOf(enabled));
	}

	public static boolean isEnabled() {
		return UsageReportPreferencesUtils.getPreferences().getBoolean(
				IUsageReportPreferenceConstants.USAGEREPORT_ENABLED_ID,
				IUsageReportPreferenceConstants.USAGEREPORT_ENABLED_DEFAULTVALUE);
	}

	public static boolean isAskUser() {
		return UsageReportPreferencesUtils.getPreferences().getBoolean(
				IUsageReportPreferenceConstants.ASK_USER_USAGEREPORT_ID, 
				IUsageReportPreferenceConstants.ASK_USER_USAGEREPORT_DEFAULTVALUE);
	}

	public static void setAskUser(boolean askUser) {
		try {
			IEclipsePreferences preferences = UsageReportPreferencesUtils.getPreferences();
			preferences.putBoolean(IUsageReportPreferenceConstants.ASK_USER_USAGEREPORT_ID, askUser);
			preferences.flush();
		} catch (BackingStoreException e) {
			Object[] messageArguments = { IUsageReportPreferenceConstants.ASK_USER_USAGEREPORT_ID };
			SubclipseToolsUsageActivator.getDefault().getLog().log(
					StatusUtils.getErrorStatus(SubclipseToolsUsageActivator.PLUGIN_ID,
							ReportingMessages.UsageReport_Error_SavePreferences, e,
							messageArguments));
		}
	}

	public static void flush() throws BackingStoreException {
		UsageReportPreferencesUtils.getPreferences().flush();
	}

	public static IPreferenceStore createPreferenceStore() {
		return UsageReportPreferencesUtils.getStore();
	}
}
