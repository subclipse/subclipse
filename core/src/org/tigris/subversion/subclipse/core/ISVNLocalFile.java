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

/**
 * SVN local file. SVN files have access to synchronization information
 * that describes their association with the SVN repository. 
 * 
 * @see ISVNFile
 * @see ISVNLocalResource
 */
public interface ISVNLocalFile extends ISVNLocalResource, ISVNFile {

}
