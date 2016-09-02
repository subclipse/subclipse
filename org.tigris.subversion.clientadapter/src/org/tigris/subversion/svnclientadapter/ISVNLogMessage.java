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

import java.util.Date;


/**
 * An interface defining a single subversion commit with log message, 
 * author, date and paths changed within the commit.
 * 
 * @author Philip Schatz <a href="mailto:schatzp@purdue.edu">schatzp@purdue.edu</a>
 */
public interface ISVNLogMessage {
	public final String AUTHOR = "svn:author";
	public final String MESSAGE = "svn:log";
	public final String DATE = "svn:date";
	public final String TIME_MICROS = "svnclientadapter:timemicros";

    /**
     * Returns the revision number
     * @return the revision number
     */
	public abstract SVNRevision.Number getRevision();

    /**
     * Returns the author of the commit
     * @return the author of the commit
     */
	public abstract String getAuthor();

    /**
     * Returns the time of the commit
     * @return the time of the commit measured in the number of
     *         microseconds since 00:00:00 January 1, 1970 UTC
     */
    public abstract long getTimeMicros();

    /**
     * Returns the time of the commit
     * @return the time of the commit measured in the number of
     *         milliseconds since 00:00:00 January 1, 1970 UTC
     */
    public abstract long getTimeMillis();

    /**
     * Returns the date of the commit
     * @return the date of the commit
     */
	public abstract Date getDate();

    /**
     * Return the log message text
     * @return the log message text
     */
	public abstract String getMessage();
    
    /**
     * Returns the changes items by this commit
     * @return the changes items by this commit
     */
    public abstract ISVNLogMessageChangePath[] getChangedPaths();    
    
    /**
     * Returns the number of child log messages.  When merge-sensitive
     * log option was specified.
     * @return the number of revisions merged by this commit
     */

    public abstract long getNumberOfChildren();    

    /**
     * Returns the child log messages.  When merge-sensitive
     * log option was specified.
     * @return the revisions merged by this commit
     */
    public abstract ISVNLogMessage[] getChildMessages();


    /**
     * Add a child logMessage to an existing message
     */
	public abstract void addChild(ISVNLogMessage msg);

    /**
     * Does this logMessage have any children
     */
	public abstract boolean hasChildren();    
}