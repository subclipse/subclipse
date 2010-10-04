package org.tigris.subversion.subclipse.tools.usage.preferences;

public interface IUsageReportPreferenceConstants {

	/** The value that determines if usage shall be reported if there's no user set setting. */
	public static final boolean USAGEREPORT_ENABLED_DEFAULTVALUE = false;

	/**
	 * The identifier to be used to identify the value that determines if usage
	 * shall be reported.
	 */
	public static final String USAGEREPORT_ENABLED_ID = "allow_usage_report_preference"; //$NON-NLS-1$

	/**
	 * The identifier to be used for the value that determines if the user shall
	 * be asked for reporting.
	 */
	public static final String ASK_USER_USAGEREPORT_ID = "ask_user_for_usage_report_preference"; //$NON-NLS-1$

	/**
	 * The identifier to be used for the value that determines this eclipse
	 * instance.
	 */
	public static final String ECLIPSE_INSTANCE_ID = "eclipse_instance_id"; //$NON-NLS-1$
	public static final String ECLIPSE_VISITOR_ID = "eclipse_visitor_id"; //$NON-NLS-1$
	
	public static final String FIRST_VISIT = "first_visit";

	public static final String LAST_VISIT = "last_visit";

	public static final String VISIT_COUNT = "visit_count";

	public static final boolean ASK_USER_USAGEREPORT_DEFAULTVALUE = true;
}
