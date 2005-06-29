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
        IProject project = null;
        if (resource instanceof IProject) project = (IProject)resource;
        else project = resource.getProject();
        if (project != null) {
            ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(project);
            if (svnResource.isManaged()) {
                CommentProperties properties = new CommentProperties();
                ISVNProperty sizeProperty = svnResource.getSvnProperty("tsvn:logminsize"); //$NON-NLS-1$
                if (sizeProperty != null) {
                    int minSize = 0;
                    try {
                        minSize = Integer.parseInt(sizeProperty.getValue());
                    } catch (Exception e) {}
                    properties.setMinimumLogMessageSize(minSize);
                }
                ISVNProperty lockSizeProperty = svnResource.getSvnProperty("tsvn:lockmsgminsize"); //$NON-NLS-1$
                if (lockSizeProperty != null) {
                    int minSize = 0;
                    try {
                        minSize = Integer.parseInt(lockSizeProperty.getValue());
                    } catch (Exception e) {}
                    properties.setMinimumLockMessageSize(minSize);
                }                
                ISVNProperty widthProperty = svnResource.getSvnProperty("tsvn:logwidthmarker"); //$NON-NLS-1$
                if (widthProperty != null) {
                    int width = 0;
                    try {
                        width = Integer.parseInt(widthProperty.getValue());
                    } catch (Exception e) {}
                    properties.setLogWidthMarker(width);
                }  
                ISVNProperty templateProperty = svnResource.getSvnProperty("tsvn:logtemplate"); //$NON-NLS-1$
                if (templateProperty != null) properties.setLogTemplate(templateProperty.getValue());
                return properties;
            }
        }
        return null;
    }
}
