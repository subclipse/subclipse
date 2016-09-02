/*******************************************************************************
 * Copyright (c) 2005, 2006 svnClientAdapter project and others.
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

import org.apache.subversion.javahl.types.Lock;
import org.tigris.subversion.svnclientadapter.ISVNLock;

/**
 * A JavaHL based implementation of {@link ISVNLock}.
 * Actually just an adapter from {@link org.tigris.subversion.javahl.Lock}
 *  
 * @author Mark Phippard
 */
public class JhlLock implements ISVNLock {
    
    private Lock _l;

    /**
     * Constructor
     * @param lock
     */
	public JhlLock(Lock lock) {
        super();
		_l = lock;
	}

	/* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNLock#getOwner()
     */
    public String getOwner() {
        return _l.getOwner();
    }

    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNLock#getPath()
     */
    public String getPath() {
        return _l.getPath();
    }

    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNLock#getToken()
     */
    public String getToken() {
        return _l.getToken();
    }

    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNLock#getComment()
     */
    public String getComment() {
        return _l.getComment();
    }

    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNLock#getCreationDate()
     */
    public Date getCreationDate() {
        return _l.getCreationDate();
    }

    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNLock#getExpirationDate()
     */
    public Date getExpirationDate() {
        return _l.getExpirationDate();
    }

}
