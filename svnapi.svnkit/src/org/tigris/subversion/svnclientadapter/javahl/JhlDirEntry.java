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
package org.tigris.subversion.svnclientadapter.javahl;

import java.util.Date;

import org.apache.subversion.javahl.types.DirEntry;
import org.tigris.subversion.svnclientadapter.ISVNDirEntry;
import org.tigris.subversion.svnclientadapter.SVNNodeKind;
import org.tigris.subversion.svnclientadapter.SVNRevision;

/**
 * A JavaHL based implementation of {@link ISVNDirEntry}.
 * Actually just an adapter from {@link org.tigris.subversion.javahl.DirEntry}
 *  
 * @author philip schatz
 */
public class JhlDirEntry implements ISVNDirEntry {

	private DirEntry _d;

	/**
	 * Constructor
	 * @param d
	 */
	public JhlDirEntry(DirEntry d) {
		super();
		_d = d;
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNDirEntry#getNodeKind()
	 */
	public SVNNodeKind getNodeKind() {
        return JhlConverter.convertNodeKind(_d.getNodeKind());
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNDirEntry#getHasProps()
	 */
	public boolean getHasProps() {
		return _d.getHasProps();
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNDirEntry#getLastChangedRevision()
	 */
	public SVNRevision.Number getLastChangedRevision() {
		return (SVNRevision.Number)JhlConverter.convert(_d.getLastChangedRevision());
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNDirEntry#getLastChangedDate()
	 */
	public Date getLastChangedDate() {
		return _d.getLastChanged();
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNDirEntry#getLastCommitAuthor()
	 */
	public String getLastCommitAuthor() {
		return _d.getLastAuthor();
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNDirEntry#getPath()
	 */
	public String getPath() {
		return _d.getPath();
	}

    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNDirEntry#getSize()
     */
    public long getSize() {
        return _d.getSize();
    }
}
