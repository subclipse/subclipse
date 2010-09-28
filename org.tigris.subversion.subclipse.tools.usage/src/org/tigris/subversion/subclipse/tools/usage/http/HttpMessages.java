package org.tigris.subversion.subclipse.tools.usage.http;

import org.eclipse.osgi.util.NLS;

public class HttpMessages extends NLS {
	private static final String BUNDLE_NAME = "org.tigris.subversion.subclipse.tools.usage.http.messages"; //$NON-NLS-1$
	
	public static String HttpGetMethod_Error_Http;
	public static String HttpGetMethod_Error_Io;
	public static String HttpGetMethod_Success;

	public static String HttpResourceMap_Error_Exception;
	public static String HttpResourceMap_Error_Http;
	public static String HttpResourceMap_Info_HttpQuery;
	
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, HttpMessages.class);
	}

	private HttpMessages() {
	}
}
