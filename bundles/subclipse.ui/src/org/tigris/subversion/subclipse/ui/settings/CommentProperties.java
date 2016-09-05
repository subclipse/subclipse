/*******************************************************************************
 * Copyright (c) 2005, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.settings;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.svnclientadapter.ISVNProperty;

public class CommentProperties {
    private int minimumLogMessageSize;
    private int logWidthMarker;
    private String logTemplate;
    private int minimumLockMessageSize;
    
	private final static List<String> commentPropertiesFilterList = new ArrayList<String>();
	static {
		commentPropertiesFilterList.add("tsvn:logminsize");
		commentPropertiesFilterList.add("tsvn:lockmsgminsize");
		commentPropertiesFilterList.add("tsvn:logwidthmarker");
		commentPropertiesFilterList.add("tsvn:logtemplate");
	}

    public CommentProperties() {
        super();
    }

    public String getLogTemplate() {
        return logTemplate;
    }
    public void setLogTemplate(String logTemplate) {
        this.logTemplate = logTemplate;
    }
    public int getLogWidthMarker() {
        return logWidthMarker;
    }
    public void setLogWidthMarker(int logWidthMarker) {
        this.logWidthMarker = logWidthMarker;
    }
    public int getMinimumLogMessageSize() {
        return minimumLogMessageSize;
    }
    public void setMinimumLogMessageSize(int minimumLogMessageSize) {
        this.minimumLogMessageSize = minimumLogMessageSize;
    }
    public int getMinimumLockMessageSize() {
        return minimumLockMessageSize;
    }
    public void setMinimumLockMessageSize(int minimumLockMessageSize) {
        this.minimumLockMessageSize = minimumLockMessageSize;
    }
    
    public static CommentProperties getCommentProperties(IResource resource) throws SVNException {
    	CommentProperties properties = null;
    	IResource parent = resource;
    	ISVNLocalResource svnResource = null;
    	while (parent != null) {
    		svnResource = SVNWorkspaceRoot.getSVNResourceFor(parent);
    		if (parent instanceof IProject || (svnResource.exists() && svnResource.isManaged() && !svnResource.getStatusFromCache().isDeleted())) {
    			break;
    		}
    		parent = parent.getParent();
    	}
    	if (svnResource == null || !svnResource.exists() || !svnResource.isManaged() || svnResource.getStatusFromCache().isDeleted()) {
    		return null;
    	}
    	properties = new CommentProperties();
    	ISVNProperty[] commentProperties = svnResource.getPropertiesIncludingInherited(false, true, commentPropertiesFilterList);
    	for (ISVNProperty commentProperty : commentProperties) {
    		if (commentProperty.getName().equals("tsvn:logminsize")) {
    			int minSize = 0;
                try {
                    minSize = Integer.parseInt(commentProperty.getValue());
                } catch (Exception e) {}
                properties.setMinimumLogMessageSize(minSize);
    		}
    		else if (commentProperty.getName().equals("tsvn:lockmsgminsize")) {
                int minSize = 0;
                try {
                    minSize = Integer.parseInt(commentProperty.getValue());
                } catch (Exception e) {}
                properties.setMinimumLockMessageSize(minSize);    	   			
    		}
    		else if (commentProperty.getName().equals("tsvn:logwidthmarker")) {
                int width = 0;
                try {
                    width = Integer.parseInt(commentProperty.getValue());
                } catch (Exception e) {}
                properties.setLogWidthMarker(width);    			
    		}
    		else if (commentProperty.getName().equals("tsvn:logtemplate")) {
    			properties.setLogTemplate(commentProperty.getValue());   
    		}
    	}
    	return properties;
    }
 
}
