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

package org.tigris.subversion.subclipse.ui.markers;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileState;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.ui.IMarkerResolution;
import org.tigris.subversion.subclipse.core.ISVNLocalFile;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.core.resourcesListeners.AddDeleteMoveListener;
import org.tigris.subversion.subclipse.ui.Policy;

/**
 * Generate marker resolutions for a svn remove marker
 */
public class SVNRemoveResolutionGenerator extends SVNAbstractResolutionGenerator {

    // marker resolution : commit the deletion
	IMarkerResolution commitDeletion = new IMarkerResolution() {
		public String getLabel() {
			return Policy.bind("SVNRemoveResloutionGenerator.Commit_Deletion_to_SVN_1"); //$NON-NLS-1$
		}
		public void run(IMarker marker) {
			try {
				final IContainer parent = (IContainer)marker.getResource();
				final String childName = (String)marker.getAttribute(AddDeleteMoveListener.NAME_ATTRIBUTE);
				ISVNLocalFile mFile = SVNWorkspaceRoot.getSVNFileFor(parent.getFile(new Path(childName)));
				final TeamException[] exception = new TeamException[] {null};
				SVNRemoveResolutionGenerator.this.run(new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor)throws InvocationTargetException, InterruptedException {
						try {
							((SVNTeamProvider)RepositoryProvider.getProvider(parent.getProject())).checkin(new IResource[] {parent.getFile(new Path(childName))}, "deleted from svn",IResource.DEPTH_ZERO, monitor);
						} catch (TeamException e) {
							exception[0] = e;
						}
					}
				});
				if (exception[0] != null) {
					throw exception[0];
				}
				marker.delete();
			} catch (TeamException e) {
				handle(e, null, null);
			} catch (CoreException e) {
				handle(e, null, null);
			} catch (InvocationTargetException e) {
				handle(e, null, null);
			}  catch (InterruptedException e) {
				// do nothing
			}
		}
	};

    // marker resolution : undo the deletion and use the history for that
	IMarkerResolution undoDeletionLocal = new IMarkerResolution() {
		public String getLabel() {
			return Policy.bind("SVNRemoveResloutionGenerator.Undo_Deletion_from_Local_History_2"); //$NON-NLS-1$
		}
		public void run(IMarker marker) {
			try {
				final IContainer parent = (IContainer)marker.getResource();
				final String childName = (String)marker.getAttribute(AddDeleteMoveListener.NAME_ATTRIBUTE);
				final IFile file = parent.getFile(new Path(childName));
				final ISVNLocalFile mFile = SVNWorkspaceRoot.getSVNFileFor(parent.getFile(new Path(childName)));
				
				boolean recreated = false;
				IFileState[] history = file.getHistory(null);
				for (int i = 0; i < history.length; i++) {
					IFileState state = history[i];
					if (state.exists()) {
						file.create(state.getContents(), false, null);
                        file.getLocation().toFile().setLastModified(state.getModificationTime());
						recreated = true;
						break;
					}
				}
				
				if (recreated) {
					marker.delete();
				} else {
					throw new SVNException(Policy.bind("SVNRemoveResloutionGenerator.No_local_history_available._Try_undoing_from_the_server_3")); //$NON-NLS-1$
				}
			} catch (TeamException e) {
				handle(e, null, null);
			} catch (CoreException e) {
				handle(e, null, null);
			}
		}
	};
		
    // marker resolution : undo the deletion by updating from repository
	IMarkerResolution undoDeletion = new IMarkerResolution() {
		public String getLabel() {
			return Policy.bind("SVNRemoveResloutionGenerator.Undo_Deletion_from_SVN_Server_4"); //$NON-NLS-1$
		}
		public void run(IMarker marker) {
			try {
				final IContainer parent = (IContainer)marker.getResource();
				final String childName = (String)marker.getAttribute(AddDeleteMoveListener.NAME_ATTRIBUTE);
				final IFile file = parent.getFile(new Path(childName));
				final ISVNLocalFile mFile = SVNWorkspaceRoot.getSVNFileFor(parent.getFile(new Path(childName)));
				
				
				final TeamException[] exception = new TeamException[] {null};
				SVNRemoveResolutionGenerator.this.run(new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor)throws InvocationTargetException, InterruptedException {
						try {
							SVNTeamProvider provider = (SVNTeamProvider)RepositoryProvider.getProvider(parent.getProject());
							provider.update(new IResource[] { parent.getFile(new Path(childName)) }, monitor);
						} catch (TeamException e) {
							exception[0] = e;
						}
					}
				});
				if (exception[0] != null) {
					throw exception[0];
				}			
				marker.delete();
			} catch (TeamException e) {
				handle(e, null, null);
			} catch (CoreException e) {
				handle(e, null, null);
			} catch (InvocationTargetException e) {
				handle(e, null, null);
			}  catch (InterruptedException e) {
				// do nothing
			}
		}
	};
	
	/*
	 * @see IMarkerResolutionGenerator#getResolutions(IMarker)
	 */
	public IMarkerResolution[] getResolutions(IMarker marker) {
		return new IMarkerResolution[] {
			commitDeletion, undoDeletionLocal, undoDeletion
		};
	}
}
