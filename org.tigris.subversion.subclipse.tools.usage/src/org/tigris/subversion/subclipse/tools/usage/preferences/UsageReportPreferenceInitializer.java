package org.tigris.subversion.subclipse.tools.usage.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;

public class UsageReportPreferenceInitializer extends
		AbstractPreferenceInitializer {

	public void initializeDefaultPreferences() {
		UsageReportPreferencesUtils.getStore().setDefault(
				IUsageReportPreferenceConstants.ASK_USER_USAGEREPORT_ID,
				IUsageReportPreferenceConstants.ASK_USER_USAGEREPORT_DEFAULTVALUE);

		UsageReportPreferencesUtils.getStore().setDefault(
				IUsageReportPreferenceConstants.USAGEREPORT_ENABLED_ID,
				IUsageReportPreferenceConstants.USAGEREPORT_ENABLED_DEFAULTVALUE);
	}
}
