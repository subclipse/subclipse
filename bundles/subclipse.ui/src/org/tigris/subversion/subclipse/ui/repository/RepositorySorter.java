/*******************************************************************************
 * Copyright (c) 2005, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.repository;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.viewers.ViewerSorter;
import org.tigris.subversion.subclipse.core.ISVNRemoteFile;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;

@SuppressWarnings("rawtypes")
public class RepositorySorter extends ViewerSorter {

	private static final int REPO_ROOT_CATEGORY = 1;
	private static final int REMOTE_FOLDER_CATEGORY = 2;
	private static final int REMOTE_FILE_CATEGORY = 3;
	private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+\\.?)+");
	private static final Pattern VERSION_SEPARATOR_PATTERN = Pattern.compile("\\.");
	private static final Comparator COMPARATOR = new Comparator(){

		public int compare(Object o1, Object o2) {
			String s1 = (String) o1;
			String s2 = (String) o2;
			try {
				Matcher m1 = VERSION_PATTERN.matcher(s1);
				Matcher m2 = VERSION_PATTERN.matcher(s2);
				int beginningS1 = 0;
				int beginningS2 = 0;
				while(m1.find() && m2.find())
				{
					//preVersion* is anything found before the version for this iteration
					String preVersion1 =  s1.substring(beginningS1, m1.start());
					String preVersion2 =  s2.substring(beginningS2, m2.start());
					if(preVersion1.compareTo(preVersion2) != 0)
					{
						//if a non-version portion of the string is different, perform normal string comparison.
						break;
					}
					else
					{
						//compare version strings
						String version1 = s1.substring(m1.start(), m1.end());
						String version2 = s2.substring(m2.start(), m2.end());
						String[] versionsMax = VERSION_SEPARATOR_PATTERN.split(version1);
						String[] versionsMin = VERSION_SEPARATOR_PATTERN.split(version2);
						int inverter = 1;
						//invert if max an min are flipped.
						if(Math.max(versionsMax.length, versionsMin.length) != versionsMax.length)
						{
							String[] temp;
							temp = versionsMax;
							versionsMax = versionsMin;
							versionsMin = temp;
							inverter = -1;
						}
						for(int i = 0; i < versionsMax.length; i++)
						{
							if(versionsMin.length == i)
							{
								//smaller version string means it's a smaller version
								return 1 * inverter; 
							}
							
							if (versionsMax[i].length() > 5 || versionsMin[i].length() > 5) {
								return s1.compareTo(s2);
							}
							
							Integer digit1 = Integer.parseInt(versionsMax[i]);
							Integer digit2 = Integer.parseInt(versionsMin[i]);
							if(digit1.compareTo(digit2) != 0)
							{
								return digit1.compareTo(digit2) * inverter;
							}
							//else move to the next version digit
						}
						// set beginning of string to end of current version, in case we have multiple versions or 
						// digits in string.
						beginningS1 = m1.end();
						beginningS2 = m2.end();
					}
				}
			} catch (Exception e) { 
				// Ignore.  Don't crash over an unexpected sorting error.
			}
			return s1.compareTo(s2);
		}
		
	};
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerSorter#category(java.lang.Object)
	 */
	public int category(Object element) {
		if (element instanceof ISVNRepositoryLocation) {
			return REPO_ROOT_CATEGORY;
		}
		
		if (element instanceof ISVNRemoteFolder) {
			return REMOTE_FOLDER_CATEGORY;
		}
		
		if (element instanceof ISVNRemoteFile) {
			return REMOTE_FILE_CATEGORY;
		}
		
		return 0;
	}
	
	
	/**
	 * Returns the comparator used to sort strings.
	 * 
	 * @return the comparator used to sort strings
	 */
	protected Comparator getComparator() {
//		if(SVNUIPlugin.getPlugin().getPreferenceStore().getBoolean(ISVNUIConstants.PREF_SORT_BY_VERSION))
//		{
//			return COMPARATOR;
//		}
//		return super.getComparator();
		
		return COMPARATOR;
	}
}
