/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
  *     Cédric Chabanois (cchabanois@ifrance.com)
 *******************************************************************************/
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
