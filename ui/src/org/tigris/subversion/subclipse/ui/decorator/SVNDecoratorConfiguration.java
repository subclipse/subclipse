/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Cédric Chabanois (cchabanois@ifrance.com) - modified for Subversion 
 *******************************************************************************/
package org.tigris.subversion.subclipse.ui.decorator;


import java.util.Map;

import org.eclipse.jface.viewers.IDecoration;

public class SVNDecoratorConfiguration {

	public static final String RESOURCE_NAME = "name"; //$NON-NLS-1$
	public static final String RESOURCE_REVISION = "revision"; //$NON-NLS-1$
    public static final String RESOURCE_AUTHOR = "author"; //$NON-NLS-1$
    public static final String RESOURCE_DATE = "date"; //$NON-NLS-1$
	public static final String RESOURCE_URL = "url"; //$NON-NLS-1$
    
	// bindings for resource states
	public static final String DIRTY_FLAG = "dirty_flag"; //$NON-NLS-1$
	public static final String ADDED_FLAG = "added_flag"; //$NON-NLS-1$
	public static final String DEFAULT_DIRTY_FLAG = ">"; //$NON-NLS-1$
	public static final String DEFAULT_ADDED_FLAG = "*"; //$NON-NLS-1$
	
	// default text decoration formats
    public static final String DEFAULT_FILETEXTFORMAT = "{added_flag}{dirty_flag}{name} {revision}  {date}  {author}"; //$NON-NLS-1$
    
	public static final String DEFAULT_FOLDERTEXTFORMAT = "{added_flag}{dirty_flag}{name} "; //$NON-NLS-1$
	public static final String DEFAULT_PROJECTTEXTFORMAT = "{dirty_flag}{name} [{url}]"; //$NON-NLS-1$

	// prefix characters that can be removed if the following binding is not found
	private static final char KEYWORD_SEPSPACE = ' ';

	
    public static void trimRight(StringBuffer strBuffer) {
        while ((strBuffer.length() > 0) && (Character.isWhitespace(strBuffer.charAt(strBuffer.length()-1))))
            strBuffer.deleteCharAt(strBuffer.length()-1);
    }
    
    /**
     * add a prefix and a suffix depending on format string and the bindings
     * @param decoration
     * @param format
     * @param bindings
     */
	public static void decorate(IDecoration decoration, String format, Map bindings) {
        
        String[] prefixSuffix = decorate(format, bindings);
        decoration.addPrefix(prefixSuffix[0]);
        decoration.addSuffix(prefixSuffix[1]);
	}
	
    /**
     * add a prefix and a suffix to name depending on format string and the bindings
     * @param name
     * @param format
     * @param bindings
     * @return
     */
    public static String decorate(String name, String format, Map bindings) {
        String[] prefixSuffix = decorate(format, bindings);
        return prefixSuffix[0]+name+prefixSuffix[1];
    }
    
    /**
     * get the suffix and the prefix depending on the format string and the bindings
     * the first element is the prefix, the second is the suffix
     * ex :
     * format = "{added_flag}{dirty_flag}{name} {revision}  {date}  {author}"
     * bindings = { "added_flag"="*", "revision"="182", date="13/10/03 14:25","author"="cchab"}
     * ==> prefix= "*"
     * ==> suffix= " 182  13/10/03 14:25  cchab"
     */
	public static String[] decorate(String format, Map bindings) {

        StringBuffer prefix = new StringBuffer(80);
        StringBuffer suffix = new StringBuffer(80);
                    
		StringBuffer output = prefix;

		int length = format.length();
		int start = -1;
		int end = length;
		while (true) {
			if ((end = format.indexOf('{', start)) > -1) {
				output.append(format.substring(start + 1, end));
				if ((start = format.indexOf('}', end)) > -1) {
					String key = format.substring(end + 1, start);
					String s;

					//We use the RESOURCE_NAME key to determine if we are doing the prefix or suffix.  The name isn't actually part of either.                  
					if (key.equals(RESOURCE_NAME)) {
						output = suffix;
						s = null;
					} else {
						s = (String) bindings.get(key);
					}

					if (s != null) {
						output.append(s);
					} else
						trimRight(output);
				} else {
					output.append(format.substring(end, length));
					break;
				}
			} else {
				output.append(format.substring(start + 1, length));
				break;
			}
		}
        return new String[] {prefix.toString(), suffix.toString() };
	}
    
}
