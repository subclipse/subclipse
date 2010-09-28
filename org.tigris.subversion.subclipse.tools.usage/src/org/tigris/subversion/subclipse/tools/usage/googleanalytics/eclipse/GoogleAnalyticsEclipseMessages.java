package org.tigris.subversion.subclipse.tools.usage.googleanalytics.eclipse;

import org.eclipse.osgi.util.NLS;

public class GoogleAnalyticsEclipseMessages extends NLS {
	
	private static final String BUNDLE_NAME = "org.tigris.subversion.subclipse.tools.usage.googleanalytics.eclipse.messages"; //$NON-NLS-1$

	public static String EclipseEnvironment_Error_SavePreferences;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, GoogleAnalyticsEclipseMessages.class);
	}

	private GoogleAnalyticsEclipseMessages() {
	}
}
