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


import java.util.ArrayList;
import java.util.List;

/**
 * A FileNameMatcher associates a String with a String pattern.
 */
public class FileNameMatcher {
	
	private List<StringMatcher> matchers = new ArrayList<StringMatcher>();
	private List<String> results = new ArrayList<String>();
	private static final String TRUE = "true"; //$NON-NLS-1$
	
	public FileNameMatcher() {
	}
	
	public FileNameMatcher(String[] patterns) {
		register(patterns);
	}

    /**
     * register a set of pattern (all associated with "true" 
     */		
	void register(String[] patterns) {
		for (String pattern : patterns) {
			register(pattern,TRUE);
		}
	}
	
    /**
     * register a pattern and an associated string 
     */
	public void register(String pattern, String result) {
		
		Assert.isTrue(matchers.size() == results.size());
		
		pattern = pattern.trim();
		
		// The empty pattern matches everything, but we want to match
		// nothing with it, so we just do not register anything
		if (pattern.length() == 0) {
			return;
		}
	
		matchers.add(new StringMatcher(pattern,false,false));
		results.add(result);
		
	}
	
    /**
     * returns the string associated with the pattern that matches the given name
     * or null if no pattern matches the name 
     */
	public String getMatch(String name) {
		StringMatcher stringMatcher;
		
		for (int i = 0; i < matchers.size(); i++) {
			stringMatcher = matchers.get(i);
			if (stringMatcher.match(name)) {
				return results.get(i);
			}
		}
		
		return null;
	}
	
    /**
     * returns true if name matches one of the patterns
     */
	public boolean match(String name) {
		return getMatch(name) != null;
	}
}
