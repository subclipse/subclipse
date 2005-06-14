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
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
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
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNStatusKind;
import org.tigris.subversion.svnclientadapter.SVNUrl;
 
/**
 * The decorator for svn resources 
 */
public class SVNLightweightDecorator
	extends LabelProvider
	implements ILightweightLabelDecorator, IResourceStateChangeListener {

	// Images cached for better performance
	private static ImageDescriptor dirty;
	private static ImageDescriptor checkedIn;
    private static ImageDescriptor deleted;
	
	private static ImageDescriptor added;
	
	private static ImageDescriptor newResource;
	private static ImageDescriptor conflicted;
	private static ImageDescriptor merged;
    private static ImageDescriptor external;
    private static ImageDescriptor locked;
    private static ImageDescriptor needsLock;

	private static IPropertyChangeListener propertyListener;

	private boolean computeDeepDirtyCheck;
	private IDecoratorComponent[][] folderDecoratorFormat;
	private IDecoratorComponent[][] projectDecoratorFormat;
	private IDecoratorComponent[][] fileDecoratorFormat;
	private String dirtyFlag;
	private String addedFlag;
    private String externalFlag;
	private boolean showNewResources;
	private boolean showDirty;
	private boolean showAdded;
    private boolean showExternal;
	private boolean showHasRemote;
	private DateFormat dateFormat;

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
		added = new CachedImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_ADDED));
		merged = new CachedImageDescriptor(TeamImages.getImageDescriptor(ISharedImages.IMG_DIRTY_OVR));
		newResource = new CachedImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_QUESTIONABLE));
		external = new CachedImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_EXTERNAL));
		locked = new CachedImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_LOCKED));
		needsLock = new CachedImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_NEEDSLOCK));
		conflicted = new CachedImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_CONFLICTED));
		deleted = new CachedImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_DELETED));
	}

	public SVNLightweightDecorator() {
		dateFormat = DateFormat.getInstance();

		IPreferenceStore store = SVNUIPlugin.getPlugin().getPreferenceStore();
		computeDeepDirtyCheck = store.getBoolean(ISVNUIConstants.PREF_CALCULATE_DIRTY);
		folderDecoratorFormat = SVNDecoratorConfiguration.compileFormatString(store.getString(ISVNUIConstants.PREF_FOLDERTEXT_DECORATION));
		projectDecoratorFormat = SVNDecoratorConfiguration.compileFormatString(store.getString(ISVNUIConstants.PREF_PROJECTTEXT_DECORATION));
		fileDecoratorFormat = SVNDecoratorConfiguration.compileFormatString(store.getString(ISVNUIConstants.PREF_FILETEXT_DECORATION));
		dirtyFlag = store.getString(ISVNUIConstants.PREF_DIRTY_FLAG);
		addedFlag = store.getString(ISVNUIConstants.PREF_ADDED_FLAG);
        externalFlag = store.getString(ISVNUIConstants.PREF_EXTERNAL_FLAG);
		showNewResources = store.getBoolean(ISVNUIConstants.PREF_SHOW_NEWRESOURCE_DECORATION);
		showDirty = store.getBoolean(ISVNUIConstants.PREF_SHOW_DIRTY_DECORATION);
		showAdded = store.getBoolean(ISVNUIConstants.PREF_SHOW_ADDED_DECORATION);
        showExternal = store.getBoolean(ISVNUIConstants.PREF_SHOW_EXTERNAL_DECORATION);
		showHasRemote = store.getBoolean(ISVNUIConstants.PREF_SHOW_HASREMOTE_DECORATION);

		propertyListener = new IPropertyChangeListener() {
						public void propertyChange(PropertyChangeEvent event) {
							if (ISVNUIConstants.PREF_CALCULATE_DIRTY.equals(event.getProperty())) {
								computeDeepDirtyCheck = ((Boolean)event.getNewValue()).booleanValue();					
							} else if (ISVNUIConstants.PREF_FOLDERTEXT_DECORATION.equals(event.getProperty())) {
								folderDecoratorFormat = SVNDecoratorConfiguration.compileFormatString((String)event.getNewValue());
							} else if (ISVNUIConstants.PREF_PROJECTTEXT_DECORATION.equals(event.getProperty())) {
								projectDecoratorFormat = SVNDecoratorConfiguration.compileFormatString((String)event.getNewValue());
							} else if (ISVNUIConstants.PREF_FILETEXT_DECORATION.equals(event.getProperty())) {
								fileDecoratorFormat = SVNDecoratorConfiguration.compileFormatString((String)event.getNewValue());
							} else if (ISVNUIConstants.PREF_DIRTY_FLAG.equals(event.getProperty())) {
								dirtyFlag = (String)event.getNewValue();
							} else if (ISVNUIConstants.PREF_ADDED_FLAG.equals(event.getProperty())) {
								addedFlag = (String)event.getNewValue();
							} else if (ISVNUIConstants.PREF_EXTERNAL_FLAG.equals(event.getProperty())) {
                                externalFlag = (String)event.getNewValue();
                            } else if (ISVNUIConstants.PREF_SHOW_NEWRESOURCE_DECORATION.equals(event.getProperty())){
								showNewResources = ((Boolean)event.getNewValue()).booleanValue();
							} else if (ISVNUIConstants.PREF_SHOW_DIRTY_DECORATION.equals(event.getProperty())) {
								showDirty = ((Boolean)event.getNewValue()).booleanValue();
							} else if (ISVNUIConstants.PREF_SHOW_ADDED_DECORATION.equals(event.getProperty())) {
								showAdded = ((Boolean)event.getNewValue()).booleanValue();
							} else if (ISVNUIConstants.PREF_SHOW_EXTERNAL_DECORATION.equals(event.getProperty())) {
                                showExternal = ((Boolean)event.getNewValue()).booleanValue();
                            } else if (ISVNUIConstants.PREF_SHOW_HASREMOTE_DECORATION.equals(event.getProperty())) {
								showHasRemote = ((Boolean)event.getNewValue()).booleanValue();
							}
						}
					};
		store.addPropertyChangeListener(propertyListener);
		SVNProviderPlugin.addResourceStateChangeListener(this);
//		SVNProviderPlugin.broadcastDecoratorEnablementChanged(true /* enabled */);
	}
    
    /**
     * tells if given svn resource is dirty or not 
     */
	public static boolean isDirty(final ISVNLocalResource svnResource) {
	    try {
			if (!svnResource.exists())
			    return false;
            if (svnResource.getIResource().getType() == IResource.FILE) {
                // for files, we want that only modified files to be considered as dirty
                return !svnResource.isIgnored() && 
                    (svnResource.getStatus().isTextModified() || 
                            svnResource.getStatus().isPropModified());
            } else {
                // a container with an added file, deleted file, conflicted file ... is considered as dirty
                return svnResource.isDirty();
            }
		} catch (SVNException e) {
			//if we get an error report it to the log but assume dirty
			SVNUIPlugin.log(e.getStatus());
			return true;
		}
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
		boolean isDirty = false;

		if (resource.getType() == IResource.FILE || computeDeepDirtyCheck) {
	        isDirty = SVNLightweightDecorator.isDirty(svnResource);
		}
		
		decorateTextLabel(resource, decoration, isDirty);
		
		ImageDescriptor overlay = getOverlay(resource, isDirty, svnProvider);
		if(overlay != null) { //actually sending null arg would work but this makes logic clearer
			decoration.addOverlay(overlay);
		}
	}

    /**
     * decorate the text label of the given resource.
     * This method assumes that only one thread will be accessing it at a time.
     */
	public void decorateTextLabel(IResource resource, IDecoration decoration, boolean isDirty) {
		try {
			Map bindings = new HashMap(6);

			// if the resource does not have a location then return. This can happen if the resource
			// has been deleted after we where asked to decorate it.
			if (resource.getLocation() == null) {
				return;
			}

			// get the format
			IDecoratorComponent[][] format;
			int type = resource.getType();
			if (type == IResource.FOLDER) {
				format = folderDecoratorFormat;
			} else if (type == IResource.PROJECT) {
				format = projectDecoratorFormat;
			} else {
				format = fileDecoratorFormat;
			}
            
            // fill the bindings
			if (isDirty) {
				bindings.put(SVNDecoratorConfiguration.DIRTY_FLAG, dirtyFlag);
			}

			ISVNLocalResource svnResource =
				SVNWorkspaceRoot.getSVNResourceFor(resource);

            bindings.put( SVNDecoratorConfiguration.RESOURCE_LABEL, svnResource.getRepository().getLabel());
            
			LocalResourceStatus status = svnResource.getStatus();
			if (status.getUrl() != null) {
				bindings.put(
					SVNDecoratorConfiguration.RESOURCE_URL,
					status.getUrl().toString());
				
                // short url is the path relative to root url of repository
                SVNUrl repositoryRoot = svnResource.getRepository().getRepositoryRoot();
                if (repositoryRoot != null) {
                    String shortUrl = status.getUrl().toString().substring(
                            repositoryRoot.toString().length()+1);    
                    bindings.put(
                            SVNDecoratorConfiguration.RESOURCE_URL_SHORT, 
                            shortUrl);
                }
			}
			
			if (status.isAdded()) {
				bindings.put(SVNDecoratorConfiguration.ADDED_FLAG, addedFlag);
			} else if (SVNStatusKind.EXTERNAL.equals(status.getTextStatus())) {
                bindings.put(SVNDecoratorConfiguration.EXTERNAL_FLAG, externalFlag);
            } else {
				if ((status.getTextStatus() != SVNStatusKind.UNVERSIONED) &&
					(status.getTextStatus() != SVNStatusKind.ADDED) &&
				    (status.getRevision() != null) &&
                    (status.getRevision().getNumber() != SVNRevision.SVN_INVALID_REVNUM)) {
					
					if (status.getLastChangedRevision() != null) {
						bindings.put(
						   SVNDecoratorConfiguration.RESOURCE_REVISION,
						   status.getLastChangedRevision().toString());
					}
					
					if (status.getLastCommitAuthor() != null) {
					    bindings.put(
						   SVNDecoratorConfiguration.RESOURCE_AUTHOR,
						   status.getLastCommitAuthor());
					}
                }				
				if (status.getLastChangedDate() != null) {
                    bindings.put(
					   SVNDecoratorConfiguration.RESOURCE_DATE,
						dateFormat.format(status.getLastChangedDate()));
				}
			}

			SVNDecoratorConfiguration.decorate(decoration, format, bindings);
			
		} catch (SVNException e) {
			SVNUIPlugin.log(e.getStatus());
		}
	}

	/* Determine and return the overlay icon to use.
	 * We only get to use one, so if many are applicable at once we chose the
	 * one we think is the most important to show.
	 * Return null if no overlay is to be used.
	 */	
	public ImageDescriptor getOverlay(IResource resource, boolean isDirty, SVNTeamProvider provider) {
	    ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
        
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
		
        if (showExternal) {
            try {
                if (SVNStatusKind.EXTERNAL.equals(svnResource.getStatus().getTextStatus())) {
                    return external;
                }
            } catch (SVNException e) {
                SVNUIPlugin.log(e.getStatus());
            }
        }
        
		// show dirty icon
		if(showDirty && isDirty) {
		    if (resource.getType() == IResource.FOLDER) {
				try {
	                LocalResourceStatus status = svnResource.getStatus();
	                
	                if (status.isDeleted()) {
						return deleted;
	                } else {
	                    return dirty;
	                }
				} catch (SVNException e) {
					SVNUIPlugin.log(e.getStatus());
					return null;
				}
		    }
			return dirty;
		}

        // show added icon
		if (showAdded) {
			try {
                LocalResourceStatus status = svnResource.getStatus();
                
                if (status.isTextConflicted()) {
                	return conflicted;
                }
           		if (status.isTextMerged()) {
           			return merged;
           		}
                if (status.isAdded()) {
					return added;
                }
                if (status.isLocked()) {
                    return locked;
                }
                if (status.isReadOnly()) {
                    return needsLock;
                }
			} catch (SVNException e) {
				SVNUIPlugin.log(e.getStatus());
				return null;
			}
		}

		//show deleted icon (on directories only)
		//ignore preferences use show this sort of overlay allways
		if (true) {
			try {
                LocalResourceStatus status = svnResource.getStatus();
                
                if (status.isDeleted()) {
					return deleted;
                }
			} catch (SVNException e) {
				SVNUIPlugin.log(e.getStatus());
				return null;
			}
		}
		
		// Simplest is that is has remote.
		if (showHasRemote) {
            try {
                
                if (svnResource.hasRemote())
                    return checkedIn;
            } catch (SVNException e) {
                SVNUIPlugin.log(e.getStatus());
            }
            
           
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

		for (int i = 0; i < changedResources.length; i++) {
			IResource resource = changedResources[i];

			if(computeDeepDirtyCheck) {
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
		SVNUIPlugin.getPlugin().getPreferenceStore().removePropertyChangeListener(propertyListener);
        SVNProviderPlugin.removeResourceStateChangeListener(this);        
//		SVNProviderPlugin.broadcastDecoratorEnablementChanged(false /* disabled */);
	}
}
