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
package org.tigris.subversion.subclipse.ui.repository.properties;


import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.resources.RemoteFile;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;

/**
 * The property source for SVNRemoteResources. Used for property view when
 * a remote resource is selected 
 */
public class SVNRemoteResourcePropertySource implements IPropertySource {
	ISVNRemoteResource resource;
	
	// Property Descriptors
	static protected IPropertyDescriptor[] propertyDescriptors = new IPropertyDescriptor[4];
	{
		PropertyDescriptor descriptor;
		String category = Policy.bind("svn"); //$NON-NLS-1$
		
		// resource name
		descriptor = new PropertyDescriptor(ISVNUIConstants.PROP_NAME, Policy.bind("SVNRemoteFilePropertySource.name")); //$NON-NLS-1$
		descriptor.setAlwaysIncompatible(true);
		descriptor.setCategory(category);
		propertyDescriptors[0] = descriptor;
		// revision
		descriptor = new PropertyDescriptor(ISVNUIConstants.PROP_REVISION, Policy.bind("SVNRemoteFilePropertySource.revision")); //$NON-NLS-1$
		descriptor.setAlwaysIncompatible(true);
		descriptor.setCategory(category);
		propertyDescriptors[1] = descriptor;
		// date
		descriptor = new PropertyDescriptor(ISVNUIConstants.PROP_DATE, Policy.bind("SVNRemoteFilePropertySource.date")); //$NON-NLS-1$
		descriptor.setAlwaysIncompatible(true);
		descriptor.setCategory(category);
		propertyDescriptors[2] = descriptor;
		// author
		descriptor = new PropertyDescriptor(ISVNUIConstants.PROP_AUTHOR, Policy.bind("SVNRemoteFilePropertySource.author")); //$NON-NLS-1$
		descriptor.setAlwaysIncompatible(true);
		descriptor.setCategory(category);
		propertyDescriptors[3] = descriptor;
	}
	
	// Property Descriptors
	static protected IPropertyDescriptor[] lockPropertyDescriptors = new IPropertyDescriptor[5];
	{
		PropertyDescriptor descriptor;
		String category = Policy.bind("SVNRemoteFilePropertySource.lock"); //$NON-NLS-1$
		
		// owner
		descriptor = new PropertyDescriptor(ISVNUIConstants.PROP_LOCK_OWNER, Policy.bind("SVNRemoteFilePropertySource.lock.owner")); //$NON-NLS-1$
		descriptor.setAlwaysIncompatible(true);
		descriptor.setCategory(category);
		lockPropertyDescriptors[0] = descriptor;
		// token
		descriptor = new PropertyDescriptor(ISVNUIConstants.PROP_LOCK_TOKEN, Policy.bind("SVNRemoteFilePropertySource.lock.token")); //$NON-NLS-1$
		descriptor.setAlwaysIncompatible(true);
		descriptor.setCategory(category);
		lockPropertyDescriptors[1] = descriptor;
		// comment
		descriptor = new PropertyDescriptor(ISVNUIConstants.PROP_LOCK_COMMENT, Policy.bind("SVNRemoteFilePropertySource.lock.comment")); //$NON-NLS-1$
		descriptor.setAlwaysIncompatible(true);
		descriptor.setCategory(category);
		lockPropertyDescriptors[2] = descriptor;
		// creation date
		descriptor = new PropertyDescriptor(ISVNUIConstants.PROP_LOCK_CREATION_DATE, Policy.bind("SVNRemoteFilePropertySource.lock.creation.date")); //$NON-NLS-1$
		descriptor.setAlwaysIncompatible(true);
		descriptor.setCategory(category);
		lockPropertyDescriptors[3] = descriptor;
		// expiration date
		descriptor = new PropertyDescriptor(ISVNUIConstants.PROP_LOCK_EXPIRATION_DATE, Policy.bind("SVNRemoteFilePropertySource.lock.expiration.date")); //$NON-NLS-1$
		descriptor.setAlwaysIncompatible(true);
		descriptor.setCategory(category);
		lockPropertyDescriptors[4] = descriptor;		
	}

	/**
	 * Create a PropertySource and store its resource
	 */
	public SVNRemoteResourcePropertySource(ISVNRemoteResource resource) {
		this.resource = resource;
	}
	
	/**
	 * Do nothing because properties are read only.
	 */
	public Object getEditableValue() {
		return this;
	}

	/**
	 * Return the Property Descriptors for the receiver.
	 */
	public IPropertyDescriptor[] getPropertyDescriptors() {
		if (resource instanceof RemoteFile) {
			RemoteFile remoteFile = (RemoteFile)resource;
			if (remoteFile.getLock() != null) {
				IPropertyDescriptor[] descriptorsWithLocks = new IPropertyDescriptor[9];
				descriptorsWithLocks[0] = propertyDescriptors[0];
				descriptorsWithLocks[1] = propertyDescriptors[1];
				descriptorsWithLocks[2] = propertyDescriptors[2];
				descriptorsWithLocks[3] = propertyDescriptors[3];
				descriptorsWithLocks[4] = lockPropertyDescriptors[0];
				descriptorsWithLocks[5] = lockPropertyDescriptors[1];
				descriptorsWithLocks[6] = lockPropertyDescriptors[2];
				descriptorsWithLocks[7] = lockPropertyDescriptors[3];
				descriptorsWithLocks[8] = lockPropertyDescriptors[4];
				return descriptorsWithLocks;
			}
		}
		return propertyDescriptors;
	}

	/*
	 * @see IPropertySource#getPropertyValue(Object)
	 */
	public Object getPropertyValue(Object id) {

		if (id.equals(ISVNUIConstants.PROP_NAME)) {
			return resource.getName();
		}
		if (id.equals(ISVNUIConstants.PROP_REVISION)) {
			return resource.getLastChangedRevision().toString();
		}
		if (id.equals(ISVNUIConstants.PROP_DATE)) {
			return resource.getDate();
		}
		if (id.equals(ISVNUIConstants.PROP_AUTHOR)) {
			return resource.getAuthor();
		}
		if (resource instanceof RemoteFile) {
			RemoteFile remoteFile = (RemoteFile)resource;
			if (remoteFile.getLock() != null) {
				if (id.equals(ISVNUIConstants.PROP_LOCK_OWNER) && remoteFile.getLock().getOwner() != null) {
					return remoteFile.getLock().getOwner();
				}
				if (id.equals(ISVNUIConstants.PROP_LOCK_TOKEN) && remoteFile.getLock().getToken() != null) {
					return remoteFile.getLock().getToken();
				}
				if (id.equals(ISVNUIConstants.PROP_LOCK_COMMENT) && remoteFile.getLock().getComment() != null) {
					return remoteFile.getLock().getComment();
				}	
				if (id.equals(ISVNUIConstants.PROP_LOCK_CREATION_DATE) && remoteFile.getLock().getCreationDate() != null) {
					return remoteFile.getLock().getCreationDate();
				}
				if (id.equals(ISVNUIConstants.PROP_LOCK_EXPIRATION_DATE) && remoteFile.getLock().getExpirationDate() != null) {
					return remoteFile.getLock().getExpirationDate();
				}	
			}
		}
		return ""; //$NON-NLS-1$
	}

	/**
	 * Answer true if the value of the specified property 
	 * for this object has been changed from the default.
	 */
	public boolean isPropertySet(Object property) {
		return false;
	}

	/**
	 * Reset the specified property's value to its default value.
	 * Do nothing because properties are read only.
	 * 
	 * @param   property    The property to reset.
	 */
	public void resetPropertyValue(Object property) {
	}

	/**
	 * Do nothing because properties are read only.
	 */
	public void setPropertyValue(Object name, Object value) {
	}
	
}
