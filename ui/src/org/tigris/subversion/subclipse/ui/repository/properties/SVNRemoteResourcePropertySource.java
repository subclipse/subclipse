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
package org.tigris.subversion.subclipse.ui.repository.properties;


import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
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
