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
package org.tigris.subversion.subclipse.ui.decorator;


import java.util.ArrayList;
import java.util.Map;

import org.eclipse.jface.viewers.IDecoration;

public class SVNDecoratorConfiguration {

	/**
	 * Decorator component that represents static text in the format string
	 */
	private static class ConstantValueDecoratorComponent implements IDecoratorComponent {
		private final String value;

		public ConstantValueDecoratorComponent(String value) {
			this.value = value;
		}

		public String getValue(Map bindings) {
			return value;
		}
		public String toString() {
			return '"' + value + '"';
		}
	}

	/**
	 * Decorator component that represents a placeholder for a value
	 */
	private static class MappedValueDecoratorComponent implements IDecoratorComponent {

		private final String key;
		public MappedValueDecoratorComponent(String key) {
			this.key = key;
			
		}

		public String getValue(Map bindings) {
			return (String)bindings.get(key);
		}
		
		public String toString() {
			return '{' + key + '}';
		}
		
	}

	public static final String RESOURCE_NAME = "name"; //$NON-NLS-1$
	public static final String RESOURCE_REVISION = "revision"; //$NON-NLS-1$
    public static final String RESOURCE_AUTHOR = "author"; //$NON-NLS-1$
    public static final String RESOURCE_DATE = "date"; //$NON-NLS-1$
	public static final String RESOURCE_URL = "url"; //$NON-NLS-1$
	public static final String RESOURCE_URL_SHORT = "url_short"; //$NON-NLS-1$
	public static final String RESOURCE_LABEL = "label"; //$NON-NLS-1$
    
	// bindings for resource states
	public static final String DIRTY_FLAG = "dirty_flag"; //$NON-NLS-1$
	public static final String ADDED_FLAG = "added_flag"; //$NON-NLS-1$
    public static final String EXTERNAL_FLAG = "external_flag"; //$NON-NLS-1$
	public static final String DEFAULT_DIRTY_FLAG = ""; //$NON-NLS-1$
	public static final String DEFAULT_ADDED_FLAG = ""; //$NON-NLS-1$
    public static final String DEFAULT_EXTERNAL_FLAG = ""; //$NON-NLS-1$
	
	// default text decoration formats
    public static final String DEFAULT_FILETEXTFORMAT = "{added_flag}{dirty_flag}{name} {revision}  {date}  {author}"; //$NON-NLS-1$
    
	public static final String DEFAULT_FOLDERTEXTFORMAT = "{external_flag}{added_flag}{dirty_flag}{name} "; //$NON-NLS-1$
	public static final String DEFAULT_PROJECTTEXTFORMAT = "{dirty_flag}{name} [{url_short}]"; //$NON-NLS-1$
	
	// font and color definition ids
	public static final String OUTGOING_CHANGE_FOREGROUND_COLOR = "svn_outgoing_change_foreground_color"; //$NON-NLS-1$
	public static final String OUTGOING_CHANGE_BACKGROUND_COLOR = "svn_outgoing_change_background_color"; //$NON-NLS-1$
	public static final String OUTGOING_CHANGE_FONT = "svn_outgoing_change_font"; //$NON-NLS-1$
	public static final String IGNORED_FOREGROUND_COLOR = "svn_ignored_resource_foreground_color"; //$NON-NLS-1$
	public static final String IGNORED_BACKGROUND_COLOR = "svn_ignored_resource_background_color"; //$NON-NLS-1$
	public static final String IGNORED_FONT = "svn_ignored_resource_font"; //$NON-NLS-1$
    
	/**
     * add a prefix and a suffix depending on format string and the bindings
     * @param decoration
     * @param format
     * @param bindings
     */
	public static void decorate(IDecoration decoration, IDecoratorComponent[][] format, Map bindings) {
        
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
    public static String decorate(String name, String formatString, Map bindings) {
    	IDecoratorComponent[][] format = compileFormatString(formatString);
        String[] prefixSuffix = decorate(format, bindings);

        return prefixSuffix[0] + name + prefixSuffix[1];
    }

    /**
     * Creates a list of prefix and suffix decorator components
     * @param format Format to create components from
     * @param bindings Bindings to link components to
     * @return Decorator components for prefix and suffix
     */
    public static IDecoratorComponent[][] compileFormatString(String format) {
		int length = format.length();
		int start = -1;
		int end = length;

		boolean isPrefix = true;
		ArrayList prefix = new ArrayList();
		ArrayList suffix = new ArrayList();
		
		while ((start + 1) < length) {
			if ((end = format.indexOf('{', start)) > -1) {
				if (start + 1 != end) {
					IDecoratorComponent component = new ConstantValueDecoratorComponent(format.substring(start + 1, end));
					if (isPrefix) {
						prefix.add(component);
					} else {
						suffix.add(component);
					}
				}
				if ((start = format.indexOf('}', end)) > -1) {
					String key = format.substring(end + 1, start);

					//We use the RESOURCE_NAME key to determine if we are doing the prefix or suffix.  The name isn't actually part of either.                  
					if (key.equals(RESOURCE_NAME)) {
						// Start working on the suffix
						isPrefix = false;
					} else {
						IDecoratorComponent component = new MappedValueDecoratorComponent(key);
						if (isPrefix) {
							prefix.add(component);
						} else {
							suffix.add(component);
						}
					}

				} else {
					// No closing brace, so it is not a variable
					IDecoratorComponent component = new ConstantValueDecoratorComponent(format.substring(end));
					if (isPrefix) {
						prefix.add(component);
					} else {
						suffix.add(component);
					}
					break;
				}
			} else {
				// No variables, just text
				IDecoratorComponent component = new ConstantValueDecoratorComponent(format.substring(start + 1));
				if (isPrefix) {
					prefix.add(component);
				} else {
					suffix.add(component);
				}
				break;
			}
		}
		return new IDecoratorComponent[][] {
				(IDecoratorComponent[])prefix.toArray(new IDecoratorComponent[prefix.size()]),
				(IDecoratorComponent[])suffix.toArray(new IDecoratorComponent[suffix.size()])};
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
	public static String[] decorate(IDecoratorComponent[][] format, Map bindings) {
    	StringBuffer prefix = new StringBuffer(80);
    	StringBuffer suffix = new StringBuffer(80);

    	for (int iPrefix = 0; iPrefix < format[0].length; iPrefix++) {
    		String value = format[0][iPrefix].getValue(bindings);
    		if (value != null) {
    			prefix.append(value);
    		}
    	}

    	for (int iSuffix = 0; iSuffix < format[1].length; iSuffix++) {
    		String value = format[1][iSuffix].getValue(bindings);
    		if (value != null) {
    			suffix.append(value);
    		}
    	}
    	return new String[] {prefix.toString(), suffix.toString()};
	}
    
}
