package org.tigris.subversion.subclipse.tools.usage.util;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;

public class BrowserUtil {

	/**
	 * Opens a browser for the given url with the given id. If an error occurs
	 * it will be reported to the given log provider with the given plugin id.
	 * 
	 * @param url
	 *            the url to open a browser for.
	 * @param browserId
	 *            the id for the new browser.
	 * @param pluginId
	 *            the plugin id to log for.
	 * @param log
	 *            the log provider to log against if an error occurred.
	 */
	public static void checkedCreateInternalBrowser(String url, String browserId, String pluginId, ILog log) {
		try {
			openUrl(url, PlatformUI.getWorkbench().getBrowserSupport().createBrowser(browserId), pluginId, log);
		} catch (PartInitException e) {
			Object[] messageArguments = { url };
			IStatus errorStatus = StatusUtils.getErrorStatus(pluginId, "Could not open browser for url \"{0}\".", e,
					messageArguments);
			log.log(errorStatus);
		}
	}


	public static void checkedCreateExternalBrowser(String url, String pluginId, ILog log) {
		try {
			openUrl(url, PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser(), pluginId, log);
		} catch (PartInitException e) {
			Object[] messageArguments = { url };
			IStatus errorStatus = StatusUtils.getErrorStatus(pluginId, "Could not open browser for url \"{0}\".", e,
					messageArguments);
			log.log(errorStatus);
		}
	}

	public static void openUrl(String url, IWebBrowser browser, String pluginId, ILog log) {
		Object[] messageArguments = { url };
		try {
			browser.openURL(new URL(url));
		} catch (PartInitException e) {
			IStatus errorStatus = StatusUtils.getErrorStatus(pluginId, "Could not open browser for url \"{0}\".", e,
					messageArguments);
			log.log(errorStatus);
		} catch (MalformedURLException e) {
			IStatus errorStatus = StatusUtils.getErrorStatus(pluginId, "Could not display malformed url \"{0}\".", e,
					messageArguments);
			log.log(errorStatus);
		}
	}
}
