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
    	ISVNProperty sizeProperty = null;
    	ISVNProperty lockSizeProperty = null;
    	ISVNProperty widthProperty = null;
    	ISVNProperty templateProperty = null;
    	IResource parent = resource;
    	while (parent != null) {
    		ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(parent);
    		if (svnResource.isManaged()) {
    			if (properties == null) {
    				properties = new CommentProperties();
    			}
    			if (sizeProperty == null) {
    				sizeProperty = svnResource.getSvnProperty("tsvn:logminsize"); //$NON-NLS-1$
    			}
    			if (lockSizeProperty == null) {
    				lockSizeProperty = svnResource.getSvnProperty("tsvn:lockmsgminsize"); //$NON-NLS-1$
    			}
    			if (widthProperty == null) {
    				widthProperty = svnResource.getSvnProperty("tsvn:logwidthmarker"); //$NON-NLS-1$
    			}
    			if (templateProperty == null) {
    				templateProperty = svnResource.getSvnProperty("tsvn:logtemplate"); //$NON-NLS-1$
    			}
    		}
    		if (parent instanceof IProject) {
    			break;
    		}
    		if (sizeProperty != null && lockSizeProperty != null && widthProperty != null && templateProperty != null) {
    			break;
    		}
    		parent = parent.getParent();
    	}
    	if (properties != null) {
            if (sizeProperty != null) {
                int minSize = 0;
                try {
                    minSize = Integer.parseInt(sizeProperty.getValue());
                } catch (Exception e) {}
                properties.setMinimumLogMessageSize(minSize);
            }
            if (lockSizeProperty != null) {
                int minSize = 0;
                try {
                    minSize = Integer.parseInt(lockSizeProperty.getValue());
                } catch (Exception e) {}
                properties.setMinimumLockMessageSize(minSize);
            }                
            if (widthProperty != null) {
                int width = 0;
                try {
                    width = Integer.parseInt(widthProperty.getValue());
                } catch (Exception e) {}
                properties.setLogWidthMarker(width);
            }  
            if (templateProperty != null) {
            	properties.setLogTemplate(templateProperty.getValue());    		
            }
    	}
    	return properties;
    }
 
}
