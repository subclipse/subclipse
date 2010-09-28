package org.tigris.subversion.subclipse.tools.usage.tracker.internal;

import org.eclipse.osgi.util.NLS;

public class TrackerMessages extends NLS {
	private static final String BUNDLE_NAME = "org.tigris.subversion.subclipse.tools.usage.tracker.messages"; //$NON-NLS-1$
	public static String Tracker_Synchronous;
	public static String Tracker_Asynchronous;
	public static String Tracker_Error;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, TrackerMessages.class);
	}

	private TrackerMessages() {
	}
}
