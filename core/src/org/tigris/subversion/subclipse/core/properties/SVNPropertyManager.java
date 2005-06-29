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
        ArrayList propertyTypes = new ArrayList();
        IExtensionPoint extension = Platform.getExtensionRegistry().getExtensionPoint(SVNProviderPlugin.ID, SVNProviderPlugin.SVN_PROPERTY_TYPES_EXTENSION);
        IExtension[] extensions =  extension.getExtensions();
        
        for (int i = 0; i < extensions.length; i++) {
            IConfigurationElement[] configElements = extensions[i].getConfigurationElements();
            for (int j = 0; j < configElements.length; j++) {
                String name = configElements[j].getAttribute("name"); //$NON-NLS-1$
                String type = configElements[j].getAttribute("type"); //$NON-NLS-1$
                String fileOrFolder = configElements[j].getAttribute("fileOrFolder"); //$NON-NLS-1$
                String allowRecurse = configElements[j].getAttribute("allowRecurse"); //$NON-NLS-1$
                String description = "";
                
                IConfigurationElement[] descriptionElements = configElements[j].getChildren("description");
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
	    ArrayList fileProperties = new ArrayList();
	    ArrayList folderProperties = new ArrayList();
	    for (int i = 0; i < definitions.length; i++) {
	        if (definitions[i].showForFile()) fileProperties.add(definitions[i]);
	        if (definitions[i].showForFolder()) folderProperties.add(definitions[i]);
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
