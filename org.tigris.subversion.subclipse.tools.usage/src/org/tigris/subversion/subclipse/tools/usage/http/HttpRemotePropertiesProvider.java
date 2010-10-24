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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.tigris.subversion.subclipse.tools.usage.util.HttpEncodingUtils;
import org.tigris.subversion.subclipse.tools.usage.util.LoggingUtils;
import org.tigris.subversion.subclipse.tools.usage.util.StatusUtils;
import org.tigris.subversion.subclipse.tools.usage.util.reader.ReaderUtils;


/**
 * Base class that holds a map that subclasses may get. The values in the map
 * are fetched and parsed from a document that is fetched on a url that the
 * subclass provides
 */
public class HttpRemotePropertiesProvider implements IPropertiesProvider {

	static final String GET_METHOD_NAME = "GET"; //$NON-NLS-1$

	protected Plugin plugin;
	private Map valuesMap;

	private String[] keys;

	private String url;

	private char valueDelimiter;

	public HttpRemotePropertiesProvider(String url, char valueDelimiter, Plugin plugin, String[] keys) {
		this.url = url;
		this.keys = keys;
		this.valueDelimiter = valueDelimiter;
		this.plugin = plugin;
	}


	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.tools.usage.http.IMapProvider#getValueMap()
	 */
	public Map getMap() throws IOException {
		if (valuesMap == null) {
			HttpURLConnection urlConnection = createURLConnection(url);
			InputStreamReader reader = request(urlConnection);
			this.valuesMap = parse(keys, valueDelimiter, reader, new HashMap());
		}
		return valuesMap;
	}
	
	/**
	 * Sends a http GET request to the given URL. Returns the response string or
	 * <tt>null</tt> if an error occurred. The errors catched are Exceptions or
	 * HTTP error codes.
	 * 
	 * @param url
	 *            the url to send the GET request to
	 * @return the response or <tt>null</tt> if an error occured.
	 * @throws UnsupportedEncodingException
	 * 
	 * @see HttpURLConnection
	 */
	protected InputStreamReader request(HttpURLConnection urlConnection) throws IOException {
		InputStreamReader responseReader = null;
		Object[] messageArguments = { url };
		try {
			urlConnection.connect();
			int responseCode = getResponseCode(urlConnection);		
			if (responseCode == HttpURLConnection.HTTP_OK) {
				IStatus status = StatusUtils.getInfoStatus(
						plugin.getBundle().getSymbolicName()
						, HttpMessages.HttpResourceMap_Info_HttpQuery
						, messageArguments);
				LoggingUtils.log(status, plugin);
				responseReader = getInputStreamReader(urlConnection.getInputStream(), urlConnection.getContentType());
			} else {
				IStatus status = StatusUtils.getErrorStatus(
						plugin.getBundle().getSymbolicName()
						, HttpMessages.HttpGetMethod_Error_Http, null, messageArguments);
				plugin.getLog().log(status);
			}
			return responseReader;
		} catch (IOException e) {
			IStatus status = StatusUtils.getErrorStatus(
					plugin.getBundle().getSymbolicName()
					, HttpMessages.HttpGetMethod_Error_Http, e, messageArguments);
			plugin.getLog().log(status);
			throw e;
		}
	}

	private InputStreamReader getInputStreamReader(InputStream inputStream, String contentType)
			throws UnsupportedEncodingException, IOException {
		String contentTypeCharset = HttpEncodingUtils.getContentTypeCharset(contentType);
		if (contentTypeCharset != null && contentTypeCharset.length() > 0) {
			return new InputStreamReader(new BufferedInputStream(inputStream),
					contentTypeCharset);
		} else {
			return new InputStreamReader(new BufferedInputStream(inputStream));
		}
	}

	/**
	 * Parses the given string and extracts the enablement value.
	 * 
	 * @param valueDelimiter
	 * 
	 * @param input
	 *            stream that holds
	 * @return
	 * @return true, if successful
	 */
	private Map parse(String[] keys, char valueDelimiter, InputStreamReader reader,
			Map valuesMap) throws IOException {	
		for (String key = null; (key = ReaderUtils.skipUntil(reader, keys)) != null;) {
			char[] delimiters = { valueDelimiter };
			String value = ReaderUtils.readStringUntil(reader, delimiters);
			valuesMap.put(key, value);
		}
		return valuesMap;
	}

	/**
	 * Creates a new url connection.
	 * 
	 * @param urlString
	 *            the url string
	 * @return the http url connection
	 * @throws IOException
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	protected HttpURLConnection createURLConnection(String urlString) throws IOException {
		URL url = new URL(urlString);
		HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
		urlConnection.setInstanceFollowRedirects(true);
		urlConnection.setRequestMethod(GET_METHOD_NAME);
		return urlConnection;
	}

	/**
	 * Returns the return code from the given {@link HttpURLConnection}.
	 * Provided to be called by test cases so that they can retrieve the return
	 * code.
	 * 
	 * @param urlConnection
	 *            to get the response code from
	 * @return the return code the HttpUrlConnection received
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	protected int getResponseCode(HttpURLConnection urlConnection) throws IOException {
		return urlConnection.getResponseCode();
	}

}