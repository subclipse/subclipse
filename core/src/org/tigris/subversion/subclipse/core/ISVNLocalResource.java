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

import java.io.File;
import java.net.URL;

import org.eclipse.core.resources.IResource;

import com.qintsoft.jsvn.jni.Status;

/**
 * SVN Local resource
 * @see ISVNLocalFile
 * @see ISVNLocalFolder
 */ 
public interface ISVNLocalResource extends ISVNResource {


  /**
   * Answers the workspace synchronization information for this resource. 
   * This function does not contact the server
   * 
   * @return the synchronization information for this resource, or <code>null</code>
   * if the resource does not have synchronization information available.
   */
  public Status getStatus() throws SVNException;

  /**
   * refresh the status of the resource (which is cached) 
   */
  public void refreshStatus();


  /**
   * @return if this resource exists
   */
  public boolean exists(); 
  
  /**
   * @return true if this resource is managed by SVN
   * @throws SVNException
   */
  public boolean isManaged() throws SVNException;

  /**
   * @return true if this resource is managed by SVN and has a remote counter part
   * @throws SVNException
   */
  public boolean hasRemote() throws SVNException;

  /**
   * @return the parent of this local resource
   */
  public ISVNLocalFolder getParent();

  /**
   * @return the underlaying resource
   */
  public IResource getIResource();

  /**
   * @return the file corresponding to the resource
   */
  public File getFile();

  /**
   * @return the corresponding remote resource (having the same or equivalent revision number)
   * @throws SVNException
   */
  public ISVNRemoteResource getRemoteResource() throws SVNException;
 
  /**
   * @return the latest remote version of this resource from repository
   * @throws SVNException
   */ 
  public ISVNRemoteResource getLatestRemoteResource() throws SVNException;

  /**
   * get the url of the remote resource corresponding to this local resource
   * The remote resource does not need to exist 
   * @return
   * @throws SVNException
   */
  public URL getUrl() throws SVNException;
  
  /**
   * Method isModified.
   * @return boolean
   */
  public boolean isModified() throws SVNException;
  
  public void accept(ISVNResourceVisitor visitor) throws SVNException;  

  /**
   * Add the following file to the parent's ignore list
   */
  public void setIgnored() throws SVNException;

  /**
   * Answer whether the resource could be ignored.
   * Even if a resource is ignored, it can still be added to a repository, at which 
   * time it should never be ignored by the SVN client.
   * 
   */
  public boolean isIgnored() throws SVNException;

  /**
   * Remove file or directory from version control.
   */
  public void delete() throws SVNException;

  /**
   * Restore pristine working copy file (undo all local edits) 
   */
  public void revert() throws SVNException;

}
