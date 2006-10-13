/*******************************************************************************
 * Copyright (c) 2003, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.core.util;


import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.tigris.subversion.subclipse.core.ISVNLocalFolder;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;

/**
 * Unsorted static helper-methods 
 */
public class Util {
	public static final String CURRENT_LOCAL_FOLDER = "."; //$NON-NLS-1$
	public static final String SERVER_SEPARATOR = "/"; //$NON-NLS-1$
	
	/**
	 * Return the last segment of the given path
	 * <br>
	 * Do not abuse this unnecesarily !
	 * When there is a SVNUrl instance available use direct
	 * {@link SVNUrl#getLastPathSegment()}
	 * @param path
	 * @return String
	 */
	public static String getLastSegment(String path) {
		int index = path.lastIndexOf(SERVER_SEPARATOR);
		if (index == -1)
			return path;
		else
			return path.substring(index + 1);
		
	}
	
	/**
	 * Append the prefix and suffix to form a valid SVN path.
	 * <br>
	 * Do not abuse this unnecesarily !
	 * When there is a SVNUrl instance available use direct
	 * {@link SVNUrl#appendPath(java.lang.String)}
	 */
	public static String appendPath(String prefix, String suffix) {
		if (prefix.length() == 0 || prefix.equals(CURRENT_LOCAL_FOLDER)) {
			return suffix;
		} else if (prefix.endsWith(SERVER_SEPARATOR)) {
			if (suffix.startsWith(SERVER_SEPARATOR))
				return prefix + suffix.substring(1);
			else
				return prefix + suffix;
		} else if (suffix.startsWith(SERVER_SEPARATOR))
			return prefix + suffix;
		else
			return prefix + SERVER_SEPARATOR + suffix;
	}

	public static void logError(String message, Throwable throwable) {
		SVNProviderPlugin.log(new Status(IStatus.ERROR, SVNProviderPlugin.ID, IStatus.ERROR, message, throwable));
	}
	
	/**
	 * Get the url string of the parent resource
	 * @param svnResource
	 * @return parent's url, null if none of parents has an url
	 * @throws SVNException
	 */
	public static String getParentUrl(ISVNLocalResource svnResource) throws SVNException {
        ISVNLocalFolder parent = svnResource.getParent();
        while (parent != null) {
            String url = parent.getStatus().getUrlString();
            if (url != null) return url;
            parent = parent.getParent();
        }
        return null;
    }

	public static String flattenText(String string) {
		StringBuffer buffer = new StringBuffer(string.length() + 20);
		boolean skipAdjacentLineSeparator = true;
		for (int i = 0; i < string.length(); i++) {
			char c = string.charAt(i);
			if (c == '\r' || c == '\n') {
				if (!skipAdjacentLineSeparator)
					buffer.append(SERVER_SEPARATOR); 
				skipAdjacentLineSeparator = true;
			} else {
				buffer.append(c);
				skipAdjacentLineSeparator = false;
			}
		}
		return buffer.toString();
	}

	/**
	 * unescape UTF8/URL encoded strings
	 * from: http://www.w3.org/International/unescape.java
	 * 
	 * @param s
	 * @return
	 */
	public static String unescape(String s) {
		StringBuffer sbuf = new StringBuffer () ;
		int l  = s.length() ;
		int ch = -1 ;
		int b, sumb = 0;
		for (int i = 0, more = -1 ; i < l ; i++) {
			/* Get next byte b from URL segment s */
			switch (ch = s.charAt(i)) {
			case '%':
				ch = s.charAt (++i) ;
				int hb = (Character.isDigit ((char) ch) 
						? ch - '0'
								: 10+Character.toLowerCase((char) ch) - 'a') & 0xF ;
				ch = s.charAt (++i) ;
				int lb = (Character.isDigit ((char) ch)
						? ch - '0'
								: 10+Character.toLowerCase ((char) ch)-'a') & 0xF ;
				b = (hb << 4) | lb ;
				break ;
			case '+':
				b = ' ' ;
				break ;
			default:
				b = ch ;
			}
			/* Decode byte b as UTF-8, sumb collects incomplete chars */
			if ((b & 0xc0) == 0x80) {			// 10xxxxxx (continuation byte)
				sumb = (sumb << 6) | (b & 0x3f) ;	// Add 6 bits to sumb
				if (--more == 0) sbuf.append((char) sumb) ; // Add char to sbuf
			} else if ((b & 0x80) == 0x00) {		// 0xxxxxxx (yields 7 bits)
				sbuf.append((char) b) ;			// Store in sbuf
			} else if ((b & 0xe0) == 0xc0) {		// 110xxxxx (yields 5 bits)
				sumb = b & 0x1f;
				more = 1;				// Expect 1 more byte
			} else if ((b & 0xf0) == 0xe0) {		// 1110xxxx (yields 4 bits)
				sumb = b & 0x0f;
				more = 2;				// Expect 2 more bytes
			} else if ((b & 0xf8) == 0xf0) {		// 11110xxx (yields 3 bits)
				sumb = b & 0x07;
				more = 3;				// Expect 3 more bytes
			} else if ((b & 0xfc) == 0xf8) {		// 111110xx (yields 2 bits)
				sumb = b & 0x03;
				more = 4;				// Expect 4 more bytes
			} else /*if ((b & 0xfe) == 0xfc)*/ {	// 1111110x (yields 1 bit)
				sumb = b & 0x01;
				more = 5;				// Expect 5 more bytes
			}
			/* We don't test if the UTF-8 encoding is well-formed */
		}
		return sbuf.toString() ;
	}	
}
