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
package org.tigris.subversion.subclipse.core;

import org.tigris.subversion.svnclientadapter.SVNKeywords;

/**
 * SVN local file. SVN files have access to synchronization information
 * that describes their association with the SVN repository. 
 * 
 * @see ISVNFile
 * @see ISVNLocalResource
 */
public interface ISVNLocalFile extends ISVNLocalResource, ISVNFile {

    /**
     * set the keywords for this file
     * @param svnKeywords
     * @throws SVNException
     */
    void setKeywords(SVNKeywords svnKeywords) throws SVNException;
    
    /**
     * add given keywords to this file
     * @param svnKeywords
     * @throws SVNException
     */
    void addKeywords(SVNKeywords svnKeywords) throws SVNException;
    
    /**
     * remove given keywords for this file
     * @param svnKeywords
     * @throws SVNException
     */
    void removeKeywords(SVNKeywords svnKeywords) throws SVNException;
    
    /**
     * get the keywords for this file
     * @return
     * @throws SVNException
     */
    SVNKeywords getKeywords() throws SVNException;
}
