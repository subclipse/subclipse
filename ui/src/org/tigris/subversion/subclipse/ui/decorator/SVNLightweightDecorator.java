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
package org.tigris.subversion.subclipse.ui.decorator;


import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.ui.ISharedImages;
import org.eclipse.team.ui.TeamImages;
import org.tigris.subversion.subclipse.core.IResourceStateChangeListener;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;

import com.qintsoft.jsvn.jni.Revision;
import com.qintsoft.jsvn.jni.Status;

/**
 * The decorator for svn resources 
 */
public class SVNLightweightDecorator
	extends LabelProvider
	implements ILightweightLabelDecorator, IResourceStateChangeListener {

	// Images cached for better performance
	private static ImageDescriptor dirty;
	private static ImageDescriptor checkedIn;
	private static ImageDescriptor noRemoteDir;
	private static ImageDescriptor added;
	private static ImageDescriptor merged;
	private static ImageDescriptor newResource;

	/*
	 * Define a cached image descriptor which only creates the image data once
	 */
	public static class CachedImageDescriptor extends ImageDescriptor {
		ImageDescriptor descriptor;
		ImageData data;
		public CachedImageDescriptor(ImageDescriptor descriptor) {
			this.descriptor = descriptor;
		}
		public ImageData getImageData() {
			if (data == null) {
				data = descriptor.getImageData();
			}
			return data;
		}
	}

	static {
		dirty = new CachedImageDescriptor(TeamImages.getImageDescriptor(ISharedImages.IMG_DIRTY_OVR));
		checkedIn = new CachedImageDescriptor(TeamImages.getImageDescriptor(ISharedImages.IMG_CHECKEDIN_OVR));
		added = new CachedImageDescriptor(TeamImages.getImageDescriptor(ISharedImages.IMG_CHECKEDIN_OVR));
		merged = new CachedImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_MERGED));
		newResource = new CachedImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_QUESTIONABLE));
		noRemoteDir = new CachedImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_NO_REMOTEDIR));
	}

	public SVNLightweightDecorator() {
		SVNProviderPlugin.addResourceStateChangeListener(this);
//		SVNProviderPlugin.broadcastDecoratorEnablementChanged(true /* enabled */);
	}

    /**
     * tells if given svn resource is dirty or not 
     */
	public static boolean isDirty(final ISVNLocalResource svnResource) {
		try {
			return !svnResource.isIgnored() && svnResource.isModified();
		} catch (SVNException e) {
			//if we get an error report it to the log but assume dirty
			SVNUIPlugin.log(e.getStatus());
			return true;
		}
	}

    /**
     * tells if given resource is dirty or not 
     */
	public static boolean isDirty(IResource resource) {

		// No need to decorate non-existant resources
		if (!resource.exists()) return false;

		return isDirty(SVNWorkspaceRoot.getSVNResourceFor(resource));

	}
	
	/**
	 * Returns the resource for the given input object, or
	 * null if there is no resource associated with it.
	 *
	 * @param object  the object to find the resource for
	 * @return the resource for the given object, or null
	 */
	private IResource getResource(Object object) {
		if (object instanceof IResource) {
			return (IResource) object;
		}
		if (object instanceof IAdaptable) {
			return (IResource) ((IAdaptable) object).getAdapter(
				IResource.class);
		}
		return null;
	}
	/**
	 * This method should only be called by the decorator thread.
	 * 
	 * @see org.eclipse.jface.viewers.ILightweightLabelDecorator#decorate(java.lang.Object, org.eclipse.jface.viewers.IDecoration)
	 */
	public void decorate(Object element, IDecoration decoration) {
		
		IResource resource = getResource(element);
		if (resource == null || resource.getType() == IResource.ROOT)
			return;

        // get the team provider
        SVNTeamProvider svnProvider = (SVNTeamProvider)RepositoryProvider.getProvider(resource.getProject(), SVNProviderPlugin.getTypeId());
		if (svnProvider == null)
			return;

		// if the resource is ignored return an empty decoration. This will
		// force a decoration update event and clear the existing SVN decoration.
		ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
		try {
			if (svnResource.isIgnored()) {
				return;
			}
		} catch (SVNException e) {
			// The was an exception in isIgnored. Don't decorate
			//todo should log this error
			return;
		}

		// determine a if resource has outgoing changes (e.g. is dirty).
		IPreferenceStore store = SVNUIPlugin.getPlugin().getPreferenceStore();
		boolean isDirty = false;
		boolean computeDeepDirtyCheck = store.getBoolean(ISVNUIConstants.PREF_CALCULATE_DIRTY);
		int type = resource.getType();
		if (type == IResource.FILE || computeDeepDirtyCheck) {
			isDirty = SVNLightweightDecorator.isDirty(resource);
		}
		
		decorateTextLabel(resource, decoration, isDirty);
		
		ImageDescriptor overlay = getOverlay(resource, isDirty, svnProvider);
		if(overlay != null) { //actually sending null arg would work but this makes logic clearer
			decoration.addOverlay(overlay);
		}
	}

    /**
     * decorate the text label of the given resource
     */
	public static void decorateTextLabel(IResource resource, IDecoration decoration, boolean isDirty) {
		try {
			IPreferenceStore store =
				SVNUIPlugin.getPlugin().getPreferenceStore();

			// if the resource does not have a location then return. This can happen if the resource
			// has been deleted after we where asked to decorate it.
			if (resource.getLocation() == null) {
				return;
			}

			// get the format
			String format = ""; //$NON-NLS-1$
			int type = resource.getType();
			if (type == IResource.FOLDER) {
				format =
					store.getString(ISVNUIConstants.PREF_FOLDERTEXT_DECORATION);
			} else if (type == IResource.PROJECT) {
				format =
					store.getString(
						ISVNUIConstants.PREF_PROJECTTEXT_DECORATION);
			} else {
				format =
					store.getString(ISVNUIConstants.PREF_FILETEXT_DECORATION);
			}
            
            // fill the bindings
            Map bindings = new HashMap(3);
			if (isDirty) {
				bindings.put(
					SVNDecoratorConfiguration.DIRTY_FLAG,
					   store.getString(ISVNUIConstants.PREF_DIRTY_FLAG));
			}

			ISVNLocalResource svnResource =
				SVNWorkspaceRoot.getSVNResourceFor(resource);
			Status status = svnResource.getStatus();
			if (status.getUrl() != null)
				bindings.put(
					SVNDecoratorConfiguration.REMOTELOCATION_URL,
					status.getUrl().toString());
			if (status.isAdded()) {
				bindings.put(
					SVNDecoratorConfiguration.ADDED_FLAG,
					   store.getString(ISVNUIConstants.PREF_ADDED_FLAG));
			} else {
                if ((status.getRevision() != Revision.SVN_INVALID_REVNUM) &&
                    (status.getRevision() != 0))
                {
				    bindings.put(
					   SVNDecoratorConfiguration.RESOURCE_REVISION,
					   Long.toString(status.getLastChangedRevision()));
				    bindings.put(
					   SVNDecoratorConfiguration.RESOURCE_AUTHOR,
					   status.getLastCommitAuthor());
                }				
                if (status.getLastChangedDate() != null)
                    bindings.put(
					   SVNDecoratorConfiguration.RESOURCE_DATE,
						DateFormat.getInstance().format(status.getLastChangedDate()));
			}

			SVNDecoratorConfiguration.decorate(decoration, format, bindings);
			
		} catch (SVNException e) {
			SVNUIPlugin.log(e.getStatus());
			return;
		}
	}

	/* Determine and return the overlay icon to use.
	 * We only get to use one, so if many are applicable at once we chose the
	 * one we think is the most important to show.
	 * Return null if no overlay is to be used.
	 */	
	public static ImageDescriptor getOverlay(IResource resource, boolean isDirty, SVNTeamProvider provider) {
		
        ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
        
        // for efficiency don't look up a pref until its needed
		IPreferenceStore store = SVNUIPlugin.getPlugin().getPreferenceStore();
		boolean showNewResources = store.getBoolean(ISVNUIConstants.PREF_SHOW_NEWRESOURCE_DECORATION);

		// show newResource icon
		if (showNewResources) {
			try {
				if (svnResource.exists()) {
					boolean isNewResource = false;
                    if (!svnResource.isManaged()) {
						isNewResource = true;
					}
					if (isNewResource) {
						return newResource;
					}
				}
			} catch (SVNException e) {
				SVNUIPlugin.log(e.getStatus());
				return null;
			}
		}
		
		boolean showDirty = store.getBoolean(ISVNUIConstants.PREF_SHOW_DIRTY_DECORATION);

		// show dirty icon
		if(showDirty && isDirty) {
			 return dirty;
		}
				
		boolean showAdded = store.getBoolean(ISVNUIConstants.PREF_SHOW_ADDED_DECORATION);

        // show added icon
		if (showAdded) {
			try {
                Status status = svnResource.getStatus();
                
           		// show merged icon if file has been merged but has not been edited (e.g. on commit it will be ignored)
//				if (info != null && info.isNeedsMerge(svnFile.getTimeStamp())) {
//			     return merged;
				// show added icon if file has been added locally.
//				} else 
                if (status.getTextStatus() == Status.Kind.added)
    				return added;
			} catch (SVNException e) {
				SVNUIPlugin.log(e.getStatus());
				return null;
			}
		}

		
		boolean showHasRemote = store.getBoolean(ISVNUIConstants.PREF_SHOW_HASREMOTE_DECORATION);
		
		// Simplest is that is has remote.
		if (showHasRemote) {
            try {
                Status status = svnResource.getStatus();
                if (svnResource.hasRemote())
                    return checkedIn;
            } catch (SVNException e) {
                SVNUIPlugin.log(e.getStatus());
            }
            
           
//			if (resource.getType() != IResource.FILE) {
//				// check if the folder is local diectory with no remote
//				ISVNFolder svnFolder = SVNWorkspaceRoot.getSVNFolderFor((IContainer)resource);
//				try {
//					if (svnFolder.getFolderSyncInfo().getRepository().equals(FolderSyncInfo.VIRTUAL_DIRECTORY)) {
//						return noRemoteDir;
//					}
//				} catch (SVNException e) {
//					// log the exception and show the shared overlay
//					SVNUIPlugin.log(e);
//				}
//			}
//			return checkedIn;
		}

		//nothing matched
		return null;

	}

	/*
	* Perform a blanket refresh of all SVN decorations
	*/
	public static void refresh() {
		SVNUIPlugin.getPlugin().getWorkbench().getDecoratorManager().update(SVNUIPlugin.DECORATOR_ID);
	}

	/*
	 * Update the decorators for every resource in project
	 */
	public void refresh(IProject project) {
		final List resources = new ArrayList();
		try {
			project.accept(new IResourceVisitor() {
				public boolean visit(IResource resource) {
					resources.add(resource);
					return true;
				}
			});
			postLabelEvent(new LabelProviderChangedEvent(this, resources.toArray()));
		} catch (CoreException e) {
			SVNProviderPlugin.log(e.getStatus());
		}
	}
	
	/**
	 * @see org.tigris.subversion.subclipse.core.IResourceStateChangeListener#resourceSyncInfoChanged(org.eclipse.core.resources.IResource[])
	 */
	public void resourceSyncInfoChanged(IResource[] changedResources) {
		resourceStateChanged(changedResources);
	}
	
	/**
	 * @see org.tigris.subversion.subclipse.core.IResourceStateChangeListener#resourceModificationStateChanged(org.eclipse.core.resources.IResource[])
	 */
	public void resourceModified(IResource[] changedResources) {
		resourceStateChanged(changedResources);
	}

	/**
	 * @see org.tigris.subversion.subclipse.core.IResourceStateChangeListener#resourceStateChanged(org.eclipse.core.resources.IResource[])
	 */
	public void resourceStateChanged(IResource[] changedResources) {
		// add depth first so that update thread processes parents first.
		//System.out.println(">> State Change Event");
		List resourcesToUpdate = new ArrayList();

		boolean showingDeepDirtyIndicators = SVNUIPlugin.getPlugin().getPreferenceStore().getBoolean(ISVNUIConstants.PREF_CALCULATE_DIRTY);

		for (int i = 0; i < changedResources.length; i++) {
			IResource resource = changedResources[i];

			if(showingDeepDirtyIndicators) {
              IResource current = resource;
              while (current.getType() != IResource.ROOT) {
                  resourcesToUpdate.add(current);
                  current = current.getParent();
              }                
			} else {
				resourcesToUpdate.add(resource);
			}
		}

		postLabelEvent(new LabelProviderChangedEvent(this, resourcesToUpdate.toArray()));
	}
	
	/**
	 * @see org.tigris.subversion.subclipse.core.IResourceStateChangeListener#projectConfigured(org.eclipse.core.resources.IProject)
	 */
	public void projectConfigured(IProject project) {
		refresh(project);
	}

	/**
	 * @see org.tigris.subversion.subclipse.core.IResourceStateChangeListener#projectDeconfigured(org.eclipse.core.resources.IProject)
	 */
	public void projectDeconfigured(IProject project) {
		refresh(project);
	}
	

	/**
	 * Post the label event to the UI thread
	 *
	 * @param events  the events to post
	 */
	private void postLabelEvent(final LabelProviderChangedEvent event) {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				fireLabelProviderChanged(event);
			}
		});
	}
    
    
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
	 */
	public void dispose() {
		super.dispose();
//		SVNProviderPlugin.broadcastDecoratorEnablementChanged(false /* disabled */);
	}
}
