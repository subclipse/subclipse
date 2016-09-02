/*******************************************************************************
 * Copyright (c) 2004, 2006 svnClientAdapter project and others.
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

/**
 * A generic implementation of the {@link ISVNLogMessageChangePath} interface.
 * 
 */
public class SVNLogMessageChangePath implements ISVNLogMessageChangePath
{
    /** Path of commited item */
    private String path;

    /** Source revision of copy (if any). */
    private SVNRevision.Number copySrcRevision;

    /** Source path of copy (if any). */
    private String copySrcPath;

    /** 'A'dd, 'D'elete, 'R'eplace, 'M'odify */
    private char action;

    /**
     * Constructor
     * @param path
     * @param copySrcRevision
     * @param copySrcPath
     * @param action
     */
	public SVNLogMessageChangePath(String path, SVNRevision.Number copySrcRevision, String copySrcPath, char action)
    {
        this.path = path;
        this.copySrcRevision = copySrcRevision;
        this.copySrcPath = copySrcPath;
        this.action = action;
    }

    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNLogMessageChangePath#getPath()
     */
    public String getPath()
    {
        return path;
    }

    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNLogMessageChangePath#getCopySrcRevision()
     */
    public SVNRevision.Number getCopySrcRevision()
    {
    	return copySrcRevision;    
    }

    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNLogMessageChangePath#getCopySrcPath()
     */
    public String getCopySrcPath()
    {
        return copySrcPath;
    }

    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNLogMessageChangePath#getAction()
     */
    public char getAction()
    {
        return action;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
    	return getPath();
    }
}
