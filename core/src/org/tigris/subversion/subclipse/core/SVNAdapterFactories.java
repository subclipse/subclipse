/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     C�dric Chabanois (cchabanois@ifrance.com) - modified for Subversion 
 *******************************************************************************/
package org.tigris.subversion.subclipse.core;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.tigris.subversion.subclipse.core.resources.LocalResourceAdapterFactory;

/**
 * handles all the IAdapterFactory used in the plugin
 */
public class SVNAdapterFactories  {
	LocalResourceAdapterFactory localResourceAdapterFactory;

	public void startup(IProgressMonitor monitor) throws CoreException {
		IAdapterManager mgr = Platform.getAdapterManager();
		
		// adapterFactory to translate IResource to ISVNResource
		localResourceAdapterFactory = new LocalResourceAdapterFactory();
		mgr.registerAdapters(localResourceAdapterFactory,IResource.class);
	}

	public void shutdown(IProgressMonitor monitor) throws CoreException {
		IAdapterManager mgr = Platform.getAdapterManager();
		mgr.unregisterAdapters(localResourceAdapterFactory);
		localResourceAdapterFactory = null;
	}

  
    
}
