/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Cédric Chabanois (cchabanois@ifrance.com) - modified for Subversion 
 *******************************************************************************/
package org.tigris.subversion.subclipse.ui.annotations;

import java.io.InputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.tigris.subversion.subclipse.core.ISVNRemoteFile;
import org.tigris.subversion.subclipse.ui.editor.RemoteFileEditorInput;

/**
 * An editor input for a cvs annotation response
 */
public class RemoteAnnotationEditorInput extends RemoteFileEditorInput implements IWorkbenchAdapter, IStorageEditorInput {

	InputStream contents;
	
	public RemoteAnnotationEditorInput(ISVNRemoteFile file, InputStream contents) {
		super(file,new NullProgressMonitor());
		this.contents = contents;
	}

	protected void initializeStorage(ISVNRemoteFile file, IProgressMonitor monitor) throws TeamException {
		if (contents != null) {
			storage = new RemoteAnnotationStorage(file, contents);
		}
	}
}
