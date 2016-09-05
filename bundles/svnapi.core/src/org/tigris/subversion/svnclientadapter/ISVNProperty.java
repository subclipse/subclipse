/*******************************************************************************
 * Copyright (c) 2003, 2006 svnClientAdapter project and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     svnClientAdapter project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.svnclientadapter;

import java.io.File;

/**
 * An interface describing a subversion property (e.g. as return by svn propget)
 *
 * @author Cédric Chabanois 
 *         <a href="mailto:cchabanois@ifrance.com">cchabanois@ifrance.com</a>
 */
public interface ISVNProperty {
	
    /**
     * mime type of the entry, used to flag binary files
     */
    public static final String MIME_TYPE = "svn:mime-type";
    /**
     * list of filenames with wildcards which should be ignored by add and
     * status
     */
    public static final String IGNORE = "svn:ignore";
    /**
     * how the end of line code should be treated during retrieval
     */
    public static final String EOL_STYLE = "svn:eol-style";
    /**
     * list of keywords to be expanded during retrieval
     */
    public static final String KEYWORDS = "svn:keywords";
    /**
     * flag if the file should be made excutable during retrieval
     */
    public static final String EXECUTABLE = "svn:executable";
    /**
     * value for svn:executable
     */
    public static final String EXECUTABLE_VALUE = "*";
    /**
     * list of directory managed outside of this working copy
     */
    public static final String EXTERNALS = "svn:externals";
    /**
     * the author of the revision
     */
    public static final String REV_AUTHOR = "svn:author";
    /**
     * the log message of the revision
     */
    public static final String REV_LOG = "svn:log";
    /**
     * the date of the revision
     */
    public static final String REV_DATE = "svn:date";
    /**
     * the original date of the revision
     */
    public static final String REV_ORIGINAL_DATE = "svn:original-date";

	/**
	 * @return the name of the property
	 */
	String getName();
	
    /**
     * Returns the string value of the property.
     * There is no protocol if a property is a string or a binary value
     * @return the string value
     */
	String getValue();
	
	/**
	 * @return the file this property belongs to (or null if on remote resource)
	 */
	File getFile();
	
    /**
     * @return the url this property belongs to
     */
	SVNUrl getUrl();

    /**
     * Returns the byte array value of the property
     * There is no protocol if a property is a string or a binary value
     * @return the byte array value
     */
	byte[] getData();
}