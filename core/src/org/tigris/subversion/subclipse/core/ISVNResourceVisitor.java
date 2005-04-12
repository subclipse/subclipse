/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Cédric Chabanois (cchabanois@ifrance.com) - modified for Subversion 
 *******************************************************************************/
package org.tigris.subversion.subclipse.core;



/**
 * Interface for an visitor of the ISVNResources.
 */
public interface ISVNResourceVisitor {
	public void visitFile(ISVNFile file) throws SVNException;
	public void visitFolder(ISVNFolder folder) throws SVNException;	
}

