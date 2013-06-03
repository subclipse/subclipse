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
package org.tigris.subversion.subclipse.ui.decorator;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
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
import org.eclipse.team.ui.TeamUI;
import org.eclipse.ui.IContributorResourceAdapter;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.themes.ITheme;
import org.tigris.subversion.subclipse.core.IResourceStateChangeListener;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.core.util.Util;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.svnclientadapter.SVNStatusKind;
import org.tigris.subversion.svnclientadapter.SVNUrl;
 
/**
 * The decorator for svn resources 
 */
public class SVNLightweightDecorator
	extends LabelProvider
	implements ILightweightLabelDecorator, IResourceStateChangeListener, IPropertyChangeListener {
	
	private static String[] fonts = new String[]  {
		SVNDecoratorConfiguration.IGNORED_FONT,
		SVNDecoratorConfiguration.OUTGOING_CHANGE_FONT};

	private static String[] colors = new String[] {
		 SVNDecoratorConfiguration.OUTGOING_CHANGE_BACKGROUND_COLOR,
		 SVNDecoratorConfiguration.OUTGOING_CHANGE_FOREGROUND_COLOR,
		 SVNDecoratorConfiguration.IGNORED_BACKGROUND_COLOR,
		 SVNDecoratorConfiguration.IGNORED_FOREGROUND_COLOR};

	// Images cached for better performance
	private static ImageDescriptor dirty;
	private static ImageDescriptor checkedIn;
    private static ImageDescriptor deleted;
    private static ImageDescriptor switched;
	
	private static ImageDescriptor added;
	private static ImageDescriptor moved;
	
	private static ImageDescriptor newResource;
	private static ImageDescriptor conflicted;
	private static ImageDescriptor propertyConflicted;
	private static ImageDescriptor merged;
    private static ImageDescriptor external;
    private static ImageDescriptor locked;
    private static ImageDescriptor needsLock;
    private static ImageDescriptor treeConflict;

	private static IPropertyChangeListener propertyListener;

	protected boolean computeDeepDirtyCheck;
	protected IDecoratorComponent[][] folderDecoratorFormat;
	protected IDecoratorComponent[][] projectDecoratorFormat;
	protected IDecoratorComponent[][] fileDecoratorFormat;
	protected String dirtyFlag;
	protected String addedFlag;
	protected String externalFlag;
	protected boolean showNewResources;
	protected boolean showDirty;
	protected boolean showAdded;
	protected boolean showExternal;
	protected boolean showHasRemote;
	protected DateFormat dateFormat;

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
		moved = new CachedImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_MOVED));
		merged = new CachedImageDescriptor(TeamImages.getImageDescriptor(ISharedImages.IMG_DIRTY_OVR));
		newResource = new CachedImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_QUESTIONABLE));
		external = new CachedImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_EXTERNAL));
		locked = new CachedImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_LOCKED));
		needsLock = new CachedImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_NEEDSLOCK));
		conflicted = new CachedImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_CONFLICTED));
		deleted = new CachedImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_DELETED));
		switched = new CachedImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_SWITCHED));
		treeConflict = new CachedImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_TREE_CONFLICT));
		propertyConflicted = new CachedImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_PROPERTY_CONFLICTED));
	}

	public SVNLightweightDecorator() {
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
		String dateFormatPattern = store.getString(ISVNUIConstants.PREF_DATEFORMAT_DECORATION);
		setDateFormat(dateFormatPattern);

		propertyListener = new IPropertyChangeListener() {
						public void propertyChange(PropertyChangeEvent event) {
							if (ISVNUIConstants.PREF_CALCULATE_DIRTY.equals(event.getProperty())) {
								computeDeepDirtyCheck = getBoolean(event);
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
								showNewResources = getBoolean(event);
							} else if (ISVNUIConstants.PREF_SHOW_DIRTY_DECORATION.equals(event.getProperty())) {
								showDirty = getBoolean(event);
							} else if (ISVNUIConstants.PREF_SHOW_ADDED_DECORATION.equals(event.getProperty())) {
								showAdded = getBoolean(event);
							} else if (ISVNUIConstants.PREF_SHOW_EXTERNAL_DECORATION.equals(event.getProperty())) {
                                showExternal = getBoolean(event);
                            } else if (ISVNUIConstants.PREF_SHOW_HASREMOTE_DECORATION.equals(event.getProperty())) {
								showHasRemote = getBoolean(event);
							} else if (ISVNUIConstants.PREF_DATEFORMAT_DECORATION.equals(event.getProperty())) {
								String dateFormatPattern = (String)event.getNewValue();
								setDateFormat(dateFormatPattern);
							}
						}
					};
		store.addPropertyChangeListener(propertyListener);
		SVNProviderPlugin.addResourceStateChangeListener(this);
		
		// This is an optimization to ensure that while decorating our fonts and colors are
		// pre-created and decoration can occur without having to syncExec.
		ensureFontAndColorsCreated(fonts, colors);

		PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().addPropertyChangeListener(this);
//		SVNProviderPlugin.broadcastDecoratorEnablementChanged(true /* enabled */);
	}
	
	/**
	 * This method will ensure that the fonts and colors used by the decorator
	 * are cached in the registries. This avoids having to syncExec when
	 * decorating since we ensure that the fonts and colors are pre-created.
	 * 
	 * @param fonts fonts ids to cache
	 * @param colors color ids to cache
	 */
	private void ensureFontAndColorsCreated(final String[] fonts, final String[] colors) {
		SVNUIPlugin.getStandardDisplay().syncExec(new Runnable() {
			public void run() {
				ITheme theme  = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme();
				for (int i = 0; i < colors.length; i++) {
					theme.getColorRegistry().get(colors[i]);
				}
				for (int i = 0; i < fonts.length; i++) {
					theme.getFontRegistry().get(fonts[i]);
				}
			}
		});
	}
	
	private boolean getBoolean(PropertyChangeEvent event) {
		if (event.getNewValue() instanceof String) return ((String)event.getNewValue()).equalsIgnoreCase("true"); //$NON-NLS-1$
		if (event.getNewValue() instanceof Boolean) return ((Boolean)event.getNewValue()).booleanValue();
		return false;
	}
    
    /**
     * tells if given svn resource is dirty or not 
     */
	public static boolean isDirty(final ISVNLocalResource svnResource, LocalResourceStatus status) {
	    try {
			if (!svnResource.exists())
			    return false;
            if (svnResource.getIResource().getType() == IResource.FILE) {
                // for files, we want that only modified files to be considered as dirty
//            	LocalResourceStatus status = svnResource.getStatusFromCache();
                return ((status.isTextModified() || status.isPropModified() || status.isReplaced() || status.isAdded())
							&& !status.isIgnored() && !svnResource.isIgnored());
            } else {
                // a container with an added file, deleted file, conflicted file ... is considered as dirty
                return svnResource.isDirty();
            }
		} catch (SVNException e) {
			//if we get an error report it to the log but assume dirty
			if (!e.operationInterrupted()) {
				SVNUIPlugin.log(e.getStatus());
			}
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
		if (object instanceof ResourceMapping) {
			object = ((ResourceMapping) object).getModelObject();
			return getModelObjectResource(object);
		}
		if (object instanceof IResource) {
			return (IResource) object;
		}
		if (object instanceof IAdaptable) {
			return (IResource) ((IAdaptable) object).getAdapter(
				IResource.class);
		}
		return null;
	}
	
	public static IResource getModelObjectResource(Object o) {
		IResource resource = null;
		if (o instanceof IResource) {
			resource = (IResource) o;
		} else if (o instanceof IAdaptable) {
			IAdaptable adaptable = (IAdaptable) o;
			resource = (IResource)adaptable.getAdapter(IResource.class);
			if (resource == null) {
				IContributorResourceAdapter adapter = (IContributorResourceAdapter)adaptable.getAdapter(IContributorResourceAdapter.class);
				if (adapter != null)
					resource = adapter.getAdaptedResource(adaptable);
			}
		}
		return resource;
	}
	
	/**
	 * This method should only be called by the decorator thread.
	 * 
	 * @see org.eclipse.jface.viewers.ILightweightLabelDecorator#decorate(java.lang.Object, org.eclipse.jface.viewers.IDecoration)
	 */
	public void decorate(Object element, IDecoration decoration) {
		IResource resource = null;
		try {
			resource = getResource(element);
			if (resource != null && resource.getType() == IResource.ROOT)
				return;
	
			boolean isIgnored = false;
			SVNTeamProvider svnProvider = null;
			ISVNLocalResource svnResource = null;
			
			if (resource != null) {
		        // get the team provider
		        svnProvider = (SVNTeamProvider)RepositoryProvider.getProvider(resource.getProject(), SVNProviderPlugin.getTypeId());
				if (svnProvider == null)
					return;
				
				// if the resource is ignored return an empty decoration. This will
				// force a decoration update event and clear the existing SVN decoration.
				svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
				try {
					if (svnResource.isIgnored()) {
						isIgnored = true;
		//				return;
					}
				} catch (SVNException e) {
					// The was an exception in isIgnored. Don't decorate
					//todo should log this error
					return;
				}
			}
	
			// determine a if resource has outgoing changes (e.g. is dirty).
			boolean isDirty = false;
			boolean isUnversioned = false;
			
			if (resource == null) {			
				if (element instanceof ResourceMapping) {
					IProject[] projects = ((ResourceMapping)element).getProjects();
					if (projects != null) {
						for (IProject project : projects) {
							ISVNLocalResource svnProjectResource = SVNWorkspaceRoot.getSVNResourceFor(project);
							if (svnProjectResource != null) {
								try {
									if (svnProjectResource.isDirty()) {
										decoration.addOverlay(dirty);
										return;
									}
								} catch (SVNException e) {
									return;
								}
							}
						}
					}
				}
				return;
			} else {		
				LocalResourceStatus status = null;
				if (!isIgnored) {
					try {
						status = svnResource.getStatusFromCache();
						isDirty = SVNLightweightDecorator.isDirty(svnResource, status);
					} catch (SVNException e) {
						if (!e.operationInterrupted()) {
							SVNUIPlugin.log(e.getStatus());
							isDirty = true;
						}
					}
					if (status != null) {
						isUnversioned = status.isUnversioned();
					}
	//				if (resource.getType() == IResource.FILE || computeDeepDirtyCheck) {
	////			        isDirty = SVNLightweightDecorator.isDirty(svnResource);
	//					isDirty = SVNLightweightDecorator.isDirty(svnResource, status);
	//				}
	//				try {
	//					status = svnResource.getStatusFromCache();
	//					isUnversioned = status.isUnversioned();
	//				} catch (SVNException e1) {
	//					if (!e1.operationInterrupted()) {
	//						SVNUIPlugin.log(e1.getStatus());
	//					}
	//				}
					decorateTextLabel(svnResource, status, decoration, isDirty);
				}		
				computeColorsAndFonts(isIgnored, isDirty || isUnversioned, decoration);
				if (!isIgnored) {
					ImageDescriptor overlay = getOverlay(svnResource, status, isDirty, svnProvider);
					if(overlay != null) { //actually sending null arg would work but this makes logic clearer
						decoration.addOverlay(overlay);
					}
				}
			}
		} catch (Exception e) {
			SVNUIPlugin.log(IStatus.ERROR, "Error Decorating " + resource, e);
		}
	}
	
	private void computeColorsAndFonts(boolean isIgnored, boolean isDirty, IDecoration decoration) {
		if (!SVNUIPlugin.getPlugin().getPreferenceStore().getBoolean(ISVNUIConstants.PREF_USE_FONT_DECORATORS)) return;
		ITheme current = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme();
		if(isIgnored) {
			decoration.setBackgroundColor(current.getColorRegistry().get(SVNDecoratorConfiguration.IGNORED_BACKGROUND_COLOR));
			decoration.setForegroundColor(current.getColorRegistry().get(SVNDecoratorConfiguration.IGNORED_FOREGROUND_COLOR));
			decoration.setFont(current.getFontRegistry().get(SVNDecoratorConfiguration.IGNORED_FONT));
		} else if(isDirty) {
			decoration.setBackgroundColor(current.getColorRegistry().get(SVNDecoratorConfiguration.OUTGOING_CHANGE_BACKGROUND_COLOR));
			decoration.setForegroundColor(current.getColorRegistry().get(SVNDecoratorConfiguration.OUTGOING_CHANGE_FOREGROUND_COLOR));
			decoration.setFont(current.getFontRegistry().get(SVNDecoratorConfiguration.OUTGOING_CHANGE_FONT));
		}
	}

    /**
     * decorate the text label of the given resource.
     * This method assumes that only one thread will be accessing it at a time.
     */
	protected void decorateTextLabel(ISVNLocalResource svnResource, LocalResourceStatus status, IDecoration decoration, boolean isDirty) {
		Map bindings = new HashMap(6);

			// if the resource does not have a location then return. This can happen if the resource
			// has been deleted after we where asked to decorate it.
			if (svnResource.getIResource().getLocation() == null) {
				return;
			}
			if (status.isUnversioned())
				return;

			// get the format
			IDecoratorComponent[][] format;
			int type = svnResource.getIResource().getType();
			if (type == IResource.FOLDER) {
				format = folderDecoratorFormat;
			} else if (type == IResource.PROJECT) {
				format = projectDecoratorFormat;
			} else {
				format = fileDecoratorFormat;
			}
            
            // fill the bindings
			if (isDirty & !status.isAdded()) {
				bindings.put(SVNDecoratorConfiguration.DIRTY_FLAG, dirtyFlag);
			}

			if (status.getUrlString() != null) {
				String label = null;
				ISVNRepositoryLocation repository = status.getRepository();
				if (repository != null) {
					label = status.getRepository().getLabel();
				}
			    bindings.put( SVNDecoratorConfiguration.RESOURCE_LABEL, label == null ? status.getUrlString() : label);
    			  
				bindings.put(
					SVNDecoratorConfiguration.RESOURCE_URL,
					Util.unescape(status.getUrlString()));
				
                // short url is the path relative to root url of repository
				SVNUrl repositoryRoot = null;
				if (repository != null) {
					repositoryRoot = repository.getRepositoryRoot();
				}
                if (repositoryRoot != null) {
                    int urlLen =  status.getUrlString().length();
                    int rootLen = repositoryRoot.toString().length()+1;
                    String shortUrl;
                    if (urlLen > rootLen)
                       shortUrl = status.getUrlString().substring(rootLen);
                    else
                       shortUrl = status.getUrlString();
                    bindings.put(
                            SVNDecoratorConfiguration.RESOURCE_URL_SHORT, 
                            Util.unescape(shortUrl));
                }
			}
			
			if (status.isAdded()) {
				bindings.put(SVNDecoratorConfiguration.ADDED_FLAG, addedFlag);
			} else if (SVNStatusKind.EXTERNAL.equals(status.getTextStatus())) {
                bindings.put(SVNDecoratorConfiguration.EXTERNAL_FLAG, externalFlag);
            } else {
				if ((status.getTextStatus() != SVNStatusKind.UNVERSIONED) &&
					(status.getTextStatus() != SVNStatusKind.ADDED)) {
					
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
	}

	/* Determine and return the overlay icon to use.
	 * We only get to use one, so if many are applicable at once we chose the
	 * one we think is the most important to show.
	 * Return null if no overlay is to be used.
	 */	
	protected ImageDescriptor getOverlay(ISVNLocalResource svnResource, LocalResourceStatus status, boolean isDirty, SVNTeamProvider provider) {
        // Tree conflict
		if (status.hasTreeConflict()) return treeConflict;
		
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
				if (!e.operationInterrupted()) {
					SVNUIPlugin.log(e.getStatus());
				}
				return null;
			}
		}
		
		if (showExternal) {
			if (status.isFileExternal() || SVNStatusKind.EXTERNAL.equals(status.getTextStatus())) {
				return external;
			}
		}
		
		// show dirty icon
		if(showDirty && isDirty) {
		    if (svnResource.getIResource().getType() == IResource.FOLDER) {
		    	if (status.isDeleted()) {
		    		return deleted;
		    	}
		    }
    	    if (status.isAdded()) {
    	    	if (status.getMovedFromAbspath() != null) {
    	    		return moved;
    	    	}
    	    	else {
    	    		return added;
    	    	}
    	    }
    	    else
    	        return dirty;
		}

        // show added icon
		if (showAdded) {
			if (status.isTextConflicted()) {
				return conflicted;
			}
			if (status.isPropConflicted()) {
				return propertyConflicted;
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
		}

		//show deleted icon (on directories only)
		//ignore preferences use show this sort of overlay allways
		if (true) {
			if (status.isDeleted()) {
				return deleted;
			}
		}
		
		// Simplest is that is has remote.
		if (showHasRemote) {
			if (status.isSwitched()) {
				return switched;
			} else if (status.hasRemote()) {
				return checkedIn;
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
		Set resourcesToUpdate = new HashSet();

		for (int i = 0; i < changedResources.length; i++) {
			IResource resource = changedResources[i];
			if (resource != null) {
			    	if (resource.exists()) {	
			    		if(computeDeepDirtyCheck) {
			    			IResource current = resource;
			    			while ((current.getType() != IResource.ROOT) && (!resourcesToUpdate.contains(current))) {
			    				resourcesToUpdate.add(current);
			    				current = current.getParent();
			    			}                
			    		} else {
			    			resourcesToUpdate.add(resource);
			    		}
			    	} else {
			    		// If deleting a resource, force a decorator refresh of the parent folders.
			    		// (We can no longer count on .svn folder updates forcing refresh)
			    		try {
			    			ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
							if (!svnResource.isIgnored()) {
				    			IResource current = resource.getParent();
				    			while (current.getType() != IResource.ROOT) {
				    				if (current.exists() && SVNWorkspaceRoot.getSVNResourceFor(current).isManaged()) {
				    					resourcesToUpdate.add(current);
				    				}
				    				current = current.getParent();
				    			}                
							}
						} catch (SVNException e) {
						}
			    	}
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
	
	public void initialize() {};
	
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
	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
		if (isEventOfInterest(event)) {
			ensureFontAndColorsCreated(fonts, colors);
		    refresh();
		}	
	}

    private boolean isEventOfInterest(PropertyChangeEvent event) {
        String prop = event.getProperty();
        return prop.equals(TeamUI.GLOBAL_IGNORES_CHANGED) 
        	|| prop.equals(TeamUI.GLOBAL_FILE_TYPES_CHANGED) 
        	|| prop.equals(SVNUIPlugin.P_DECORATORS_CHANGED)
			|| prop.equals(SVNDecoratorConfiguration.OUTGOING_CHANGE_BACKGROUND_COLOR)
			|| prop.equals(SVNDecoratorConfiguration.OUTGOING_CHANGE_FOREGROUND_COLOR)
			|| prop.equals(SVNDecoratorConfiguration.OUTGOING_CHANGE_FONT)
			|| prop.equals(SVNDecoratorConfiguration.IGNORED_FOREGROUND_COLOR)
			|| prop.equals(SVNDecoratorConfiguration.IGNORED_BACKGROUND_COLOR)
			|| prop.equals(SVNDecoratorConfiguration.IGNORED_FONT);
    }
        
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
	 */
	public void dispose() {
		super.dispose();
		PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().removePropertyChangeListener(this);
		TeamUI.removePropertyChangeListener(this);
		SVNUIPlugin.getPlugin().getPreferenceStore().removePropertyChangeListener(propertyListener);
        SVNProviderPlugin.removeResourceStateChangeListener(this);        
//		SVNProviderPlugin.broadcastDecoratorEnablementChanged(false /* disabled */);
	}

	private void setDateFormat(String dateFormatPattern) {
		if (dateFormatPattern == null || dateFormatPattern.length() == 0) {
			dateFormat = DateFormat.getInstance();			
		}
		else {
			try {
				dateFormat = new SimpleDateFormat(dateFormatPattern);
			} catch (Exception e) {
				dateFormat = DateFormat.getInstance();
			}
		}
	}
}
