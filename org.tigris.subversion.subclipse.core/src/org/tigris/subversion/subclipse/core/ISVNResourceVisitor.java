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



/**
 * Interface for an visitor of the ISVNResources.
 */
public interface ISVNResourceVisitor {
	public void visitFile(ISVNFile file) throws SVNException;
	public void visitFolder(ISVNFolder folder) throws SVNException;	
}

