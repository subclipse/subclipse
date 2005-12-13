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
package org.tigris.subversion.subclipse.ui;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;

import org.eclipse.jface.resource.ImageDescriptor;

/**
 * The image descrptors for the plugin
 */
public class ImageDescriptors {
    private Hashtable imageDescriptors = new Hashtable(20);
    
    /**
     * Creates an image and places it in the image registry.
     */
    protected void createImageDescriptor(String id, URL baseURL) {
        URL url = null;
        try {
            url = new URL(baseURL, ISVNUIConstants.ICON_PATH + id);
        } catch (MalformedURLException e) {
        }
        ImageDescriptor desc = ImageDescriptor.createFromURL(url);
        imageDescriptors.put(id, desc);
    }
    
    /**
     * Returns the image descriptor for the given image ID.
     * Returns null if there is no such image.
     */
    public ImageDescriptor getImageDescriptor(String id) {
        return (ImageDescriptor)imageDescriptors.get(id);
    }
    
    /**
     * Initializes the table of images used in this plugin.
     */
    public void initializeImages(URL baseURL) {
    
        // objects
        createImageDescriptor(ISVNUIConstants.IMG_REPOSITORY, baseURL); 
        createImageDescriptor(ISVNUIConstants.IMG_REFRESH, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_REFRESH_ENABLED, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_REFRESH_DISABLED, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_LINK_WITH_EDITOR, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_GET_ALL, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_GET_NEXT, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_LINK_WITH_EDITOR_ENABLED, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_COLLAPSE_ALL, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_COLLAPSE_ALL_ENABLED, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_NEWLOCATION, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_NEWFOLDER, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_TAG, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_BRANCH, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_MODULE, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_CLEAR, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_CLEAR_ENABLED, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_CLEAR_DISABLED, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_BRANCHES_CATEGORY, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_VERSIONS_CATEGORY, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_PROJECT_VERSION, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_WARNING, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_WIZBAN_MERGE, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_WIZBAN_SHARE, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_WIZBAN_SYNCH, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_WIZBAN_DIFF, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_WIZBAN_KEYWORD, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_WIZBAN_NEW_LOCATION, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_WIZBAN_NEW_FOLDER, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_MERGEABLE_CONFLICT, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_QUESTIONABLE, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_MERGED, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_EDITED, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_NO_REMOTEDIR, baseURL);
		createImageDescriptor(ISVNUIConstants.IMG_CONFLICTED, baseURL);
		createImageDescriptor(ISVNUIConstants.IMG_ADDED, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_EXTERNAL, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_LOCKED, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_NEEDSLOCK, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_DELETED, baseURL);
		createImageDescriptor(ISVNUIConstants.IMG_ADD_PROPERTY, baseURL);
		createImageDescriptor(ISVNUIConstants.IMG_COMMIT, baseURL);
		createImageDescriptor(ISVNUIConstants.IMG_UPDATE, baseURL);
		createImageDescriptor(ISVNUIConstants.IMG_CONFLICT, baseURL);
		createImageDescriptor(ISVNUIConstants.IMG_REVERT, baseURL);
		createImageDescriptor(ISVNUIConstants.IMG_RESOLVE, baseURL);
		createImageDescriptor(ISVNUIConstants.IMG_LOG, baseURL);
		createImageDescriptor(ISVNUIConstants.IMG_MERGE, baseURL);
		createImageDescriptor(ISVNUIConstants.IMG_IGNORE, baseURL);
		createImageDescriptor(ISVNUIConstants.IMG_DIFF, baseURL);
        
        // special
        createImageDescriptor("glyphs/glyph1.gif", baseURL);  //$NON-NLS-1$
        createImageDescriptor("glyphs/glyph2.gif", baseURL);  //$NON-NLS-1$
        createImageDescriptor("glyphs/glyph3.gif", baseURL);  //$NON-NLS-1$
        createImageDescriptor("glyphs/glyph4.gif", baseURL);  //$NON-NLS-1$
        createImageDescriptor("glyphs/glyph5.gif", baseURL);  //$NON-NLS-1$
        createImageDescriptor("glyphs/glyph6.gif", baseURL);  //$NON-NLS-1$
        createImageDescriptor("glyphs/glyph7.gif", baseURL);  //$NON-NLS-1$
        createImageDescriptor("glyphs/glyph8.gif", baseURL);  //$NON-NLS-1$
        
        createImageDescriptor(ISVNUIConstants.IMG_FILEADD_PENDING,baseURL); 
        createImageDescriptor(ISVNUIConstants.IMG_FILEDELETE_PENDING,baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_FOLDERADD_PENDING,baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_FOLDERDELETE_PENDING,baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_FILEMODIFIED_PENDING,baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_FOLDERMODIFIED_PENDING,baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_FOLDER, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_FILEMISSING_PENDING,baseURL);

        createImageDescriptor(ISVNUIConstants.IMG_AFFECTED_PATHS_FLAT_LAYOUT, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_AFFECTED_PATHS_COMPRESSED_LAYOUT, baseURL);
        
        // views
        createImageDescriptor(ISVNUIConstants.IMG_SVN_CONSOLE, baseURL);
    }    
    
}
