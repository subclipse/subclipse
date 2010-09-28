package org.tigris.subversion.subclipse.tools.usage.reporting;

import org.eclipse.osgi.util.NLS;

public class ReportingMessages extends NLS {
	private static final String BUNDLE_NAME = "org.tigris.subversion.subclipse.tools.usage.reporting.messages"; //$NON-NLS-1$

	public static String UsageReport_Reporting_Usage;
	public static String UsageReport_Querying_Enablement;
	public static String UsageReport_Asking_User;
	public static String UsageReport_DialogTitle;
	public static String UsageReport_Error_SavePreferences;

	public static String UsageReport_Checkbox_Text;
	public static String UsageReport_DialogMessage;
	public static String UsageReport_GoogleAnalytics_Account;
	public static String UsageReport_HostName;
	public static String UsageReport_ExplanationPage;

	public static String UsageReportEnablementDialog_notYetImplemented;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, ReportingMessages.class);
	}

	private ReportingMessages() {
	}
}
