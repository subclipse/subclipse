/*******************************************************************************
 * Copyright (c) 2004, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.core.properties;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;

/**
 * @author Brock Janiczak
 */
public class SVNPropertyManager {

    private static SVNPropertyManager instance;
    private SVNPropertyDefinition[] definitions;   
    private SVNPropertyDefinition[] fileDefinitions;
    private SVNPropertyDefinition[] folderDefinitions;

    public static SVNPropertyManager getInstance() {
        if (instance == null) {
            instance = new SVNPropertyManager();
        }
        return instance;
    }
    
    private SVNPropertyManager() {
        loadPropertiesFromExtensions();
    }

    private void loadPropertiesFromExtensions() {
        ArrayList<SVNPropertyDefinition> propertyTypes = new ArrayList<SVNPropertyDefinition>();
        IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(SVNProviderPlugin.ID, SVNProviderPlugin.SVN_PROPERTY_TYPES_EXTENSION);
        IExtension[] extensions =  extensionPoint.getExtensions();
        
        for (IExtension extension : extensions) {
            IConfigurationElement[] configElements = extension.getConfigurationElements();
            for (IConfigurationElement configElement : configElements) {
                String name = configElement.getAttribute("name"); //$NON-NLS-1$
                String type = configElement.getAttribute("type"); //$NON-NLS-1$
                String fileOrFolder = configElement.getAttribute("fileOrFolder"); //$NON-NLS-1$
                String allowRecurse = configElement.getAttribute("allowRecurse"); //$NON-NLS-1$
                String description = "";
                
                IConfigurationElement[] descriptionElements = configElement.getChildren("description");
                if (descriptionElements.length == 1) {
                    description = descriptionElements[0].getValue();
                }
                int showFor;
                if (fileOrFolder.equals("file")) showFor = SVNPropertyDefinition.FILE;
                else if (fileOrFolder.equals("folder")) showFor = SVNPropertyDefinition.FOLDER;
                else showFor = SVNPropertyDefinition.BOTH;
                boolean recurse = true;
                if ((allowRecurse != null) && (allowRecurse.equalsIgnoreCase("false"))) recurse = false;
                SVNPropertyDefinition property = new SVNPropertyDefinition(name, description, showFor, recurse, type);
                propertyTypes.add(property);                
            }
        }
	    definitions = new SVNPropertyDefinition[propertyTypes.size()];
	    propertyTypes.toArray(definitions);
	    Arrays.sort(definitions);
	    ArrayList<SVNPropertyDefinition> fileProperties = new ArrayList<SVNPropertyDefinition>();
	    ArrayList<SVNPropertyDefinition> folderProperties = new ArrayList<SVNPropertyDefinition>();
	    for (SVNPropertyDefinition definition : definitions) {
	        if (definition.showForFile()) fileProperties.add(definition);
	        if (definition.showForFolder()) folderProperties.add(definition);
	    }
	    fileDefinitions = new SVNPropertyDefinition[fileProperties.size()];
	    fileProperties.toArray(fileDefinitions);
	    folderDefinitions = new SVNPropertyDefinition[folderProperties.size()];
	    folderProperties.toArray(folderDefinitions);
    }

    public SVNPropertyDefinition[] getPropertyTypes() {
        return definitions;
    }
    
    public SVNPropertyDefinition[] getFilePropertyTypes() {
        return fileDefinitions;
    }
    
    public SVNPropertyDefinition[] getFolderPropertyTypes() {
        return folderDefinitions;
    }
}
