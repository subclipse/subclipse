/*******************************************************************************
 * Copyright (c) 2010 Subclipse project and others.
 * Copyright (c) 2010 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.tools.usage.http;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.MessageFormat;

import org.tigris.subversion.subclipse.tools.usage.tracker.ILoggingAdapter;

/**
 * Class that executes a HTTP Get request to the given url.
 */
public class HttpGetRequest implements IHttpGetRequest {
	
	private static final String USER_AGENT = "User-Agent"; //$NON-NLS-1$

	private static final String GET_METHOD_NAME = "GET"; //$NON-NLS-1$
	
	private ILoggingAdapter loggingAdapter = null;

	private String userAgent;

	public HttpGetRequest(String userAgent, ILoggingAdapter loggingAdapter) {
		this.userAgent = userAgent;
		this.loggingAdapter = loggingAdapter;
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.tools.usage.IHttpGetRequest#request(java.lang.String)
	 */
	public void request(String urlString) {
		try {
			HttpURLConnection urlConnection = createURLConnection(urlString, userAgent);
			urlConnection.connect();
			int responseCode = getResponseCode(urlConnection);
			if (responseCode == HttpURLConnection.HTTP_OK) {
				String[] parameters = { urlString, Integer.toString(responseCode) };
				loggingAdapter.logMessage(MessageFormat.format(HttpMessages.HttpGetMethod_Success, parameters));
			} else {
				String[] parameters = { urlString };
				loggingAdapter.logError(MessageFormat.format(HttpMessages.HttpGetMethod_Error_Http, parameters));
			}
		} catch (Exception e) {
			String[] parameters = { urlString, e.toString() };
			loggingAdapter.logMessage(MessageFormat.format(HttpMessages.HttpGetMethod_Error_Io, parameters));
		}
	}

	/**
	 * Returns the return code from the given {@link HttpURLConnection}.
	 * Provided to be called by test cases so that they can retrieve the return code.
	 *
	 * @param urlConnection to get the response code from
	 * @return the return code the HttpUrlConnection received
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	protected int getResponseCode(HttpURLConnection urlConnection) throws IOException {
		return urlConnection.getResponseCode();
	}

	/**
	 * Creates a new url connection.
	 *
	 * @param urlString the url string
	 * @param userAgent the user agent
	 * @return the http url connection
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	protected HttpURLConnection createURLConnection(String urlString, String userAgent) throws IOException {
		URL url = new URL(urlString);
		HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
		urlConnection.setInstanceFollowRedirects(true);
		urlConnection.setRequestMethod(GET_METHOD_NAME);
		urlConnection.setRequestProperty(USER_AGENT, userAgent);
		return urlConnection;
	}
}
