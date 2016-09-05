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
     * Creates an image and places it in the image registry.
     */
    protected void createImageDescriptor(String id, String name, URL baseURL) {
        URL url = null;
        try {
            url = new URL(baseURL, ISVNUIConstants.ICON_PATH + name);
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
    public void initializeImages(URL baseURL, int iconSet) {
    
        // objects
        createImageDescriptor(ISVNUIConstants.IMG_REPOSITORY, baseURL); 
        createImageDescriptor(ISVNUIConstants.IMG_REFRESH, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_REFRESH_ENABLED, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_REFRESH_DISABLED, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_SYNCPANE, baseURL); 
        createImageDescriptor(ISVNUIConstants.IMG_PROPERTIES, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_URL_SOURCE_REPO, baseURL); 
//        createImageDescriptor(ISVNUIConstants.IMG_LINK_WITH_EDITOR, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_GET_ALL, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_GET_NEXT, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_FILTER_HISTORY, baseURL);
//        createImageDescriptor(ISVNUIConstants.IMG_FILTER_HISTORY_ENABLED, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_FILTER_HISTORY_DISABLED, baseURL);
//        createImageDescriptor(ISVNUIConstants.IMG_LINK_WITH_EDITOR_ENABLED, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_COLLAPSE_ALL, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_COLLAPSE_ALL_ENABLED, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_EXPAND_ALL, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_EXPAND_ALL_ENABLED, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_NEWLOCATION, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_CLOUDFORGE, baseURL);
//        createImageDescriptor(ISVNUIConstants.IMG_NEWFOLDER, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_BRANCH, baseURL);
//        createImageDescriptor(ISVNUIConstants.IMG_MODULE, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_CLEAR, baseURL);
//        createImageDescriptor(ISVNUIConstants.IMG_CLEAR_ENABLED, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_CLEAR_DISABLED, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_BRANCHES_CATEGORY, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_VERSIONS_CATEGORY, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_PROJECT_VERSION, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_WARNING, baseURL);
//        createImageDescriptor(ISVNUIConstants.IMG_WIZBAN_MERGE, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_WIZBAN_SVN, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_WIZBAN_RESOLVE_TREE_CONFLICT, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_WIZBAN_SHARE, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_WIZBAN_SYNCH, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_WIZBAN_DIFF, baseURL);
//        createImageDescriptor(ISVNUIConstants.IMG_WIZBAN_KEYWORD, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_WIZBAN_NEW_LOCATION, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_WIZBAN_NEW_FOLDER, baseURL);
//        createImageDescriptor(ISVNUIConstants.IMG_MERGEABLE_CONFLICT, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_QUESTIONABLE, baseURL);
//        createImageDescriptor(ISVNUIConstants.IMG_MERGED, baseURL);
//        createImageDescriptor(ISVNUIConstants.IMG_EDITED, baseURL);
//        createImageDescriptor(ISVNUIConstants.IMG_NO_REMOTEDIR, baseURL);
		createImageDescriptor(ISVNUIConstants.IMG_CONFLICTED, baseURL);
		createImageDescriptor(ISVNUIConstants.IMG_ADDED, baseURL);
		createImageDescriptor(ISVNUIConstants.IMG_MOVED, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_EXTERNAL, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_LOCKED, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_NEEDSLOCK, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_DELETED, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_SWITCHED, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_PROPERTY_CHANGED, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_TEXT_CONFLICTED, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_TREE_CONFLICT, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_PROPERTY_CONFLICTED, baseURL);
        
        createImageDescriptor(ISVNUIConstants.IMG_UPDATE_ALL, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_COMMIT_ALL, baseURL);
        
        createImageDescriptor(ISVNUIConstants.IMG_SHOW_DELETED, baseURL);
        
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
//        createImageDescriptor(ISVNUIConstants.IMG_FILEMISSING_PENDING,baseURL);

        createImageDescriptor(ISVNUIConstants.IMG_AFFECTED_PATHS_TABLE_MODE, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_AFFECTED_PATHS_FLAT_MODE, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_AFFECTED_PATHS_COMPRESSED_MODE, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_AFFECTED_PATHS_TREE_MODE, baseURL);
        
        createImageDescriptor(ISVNUIConstants.IMG_AFFECTED_PATHS_HORIZONTAL_LAYOUT, baseURL);
        createImageDescriptor(ISVNUIConstants.IMG_AFFECTED_PATHS_VERTICAL_LAYOUT, baseURL);

        createImageDescriptor(ISVNUIConstants.IMG_COMMENTS, baseURL);
        
        // views
        createImageDescriptor(ISVNUIConstants.IMG_SVN_CONSOLE, baseURL);

        // Menues
        switch(iconSet) {
        	case ISVNUIConstants.MENU_ICON_SET_TORTOISESVN:
		        createImageDescriptor(ISVNUIConstants.IMG_MENU_UPDATE,       "tortoise/update.gif", baseURL);
		        createImageDescriptor(ISVNUIConstants.IMG_MENU_COMMIT,       "tortoise/commit.gif", baseURL);
		        createImageDescriptor(ISVNUIConstants.IMG_MENU_SYNC,         "obj16/synch_synch.gif", baseURL);
		        createImageDescriptor(ISVNUIConstants.IMG_MENU_REVERT,       "tortoise/revert.gif", baseURL);
		        createImageDescriptor(ISVNUIConstants.IMG_MENU_ADD,          "tortoise/add.gif", baseURL);
		        createImageDescriptor(ISVNUIConstants.IMG_MENU_IGNORE,       "tortoise/ignore.gif", baseURL);
		        createImageDescriptor(ISVNUIConstants.IMG_MENU_PROPSET,      "ctool16/svn_prop_add.gif", baseURL);
		        createImageDescriptor(ISVNUIConstants.IMG_MENU_SHOWPROPERTY, "cview16/props_view.gif", baseURL);
		        createImageDescriptor(ISVNUIConstants.IMG_MENU_RELOCATE,     "tortoise/relocate.gif", baseURL);
		        createImageDescriptor(ISVNUIConstants.IMG_MENU_CHECKOUTAS,   "tortoise/checkout.gif", baseURL);
		        createImageDescriptor(ISVNUIConstants.IMG_MENU_IMPORTFOLDER, "tortoise/import.gif", baseURL);
		        createImageDescriptor(ISVNUIConstants.IMG_MENU_LOCK,         "tortoise/lock.gif", baseURL);
		        createImageDescriptor(ISVNUIConstants.IMG_MENU_UNLOCK,       "tortoise/unlock.gif", baseURL);
		        createImageDescriptor(ISVNUIConstants.IMG_MENU_CLEANUP,      "tortoise/cleanup.gif", baseURL);
		        createImageDescriptor(ISVNUIConstants.IMG_MENU_EXPORT,       "tortoise/export.gif", baseURL);
		        createImageDescriptor(ISVNUIConstants.IMG_MENU_DIFF,         "tortoise/diff.gif", baseURL);
		        createImageDescriptor(ISVNUIConstants.IMG_MENU_PROPDELETE,   "ctool16/delete.gif", baseURL);
		        createImageDescriptor(ISVNUIConstants.IMG_MENU_DELETE,       "ctool16/delete.gif", baseURL);
		        createImageDescriptor(ISVNUIConstants.IMG_MENU_BRANCHTAG,    "tortoise/copy.gif", baseURL);
		        createImageDescriptor(ISVNUIConstants.IMG_MENU_MOVE,         "tortoise/rename.gif", baseURL);
		        createImageDescriptor(ISVNUIConstants.IMG_MENU_COMPARE,      "tortoise/compare.gif", baseURL);
		        createImageDescriptor(ISVNUIConstants.IMG_MENU_RESOLVE,      "tortoise/resolve.gif", baseURL);
		        createImageDescriptor(ISVNUIConstants.IMG_MENU_EDITCONFLICT, "tortoise/conflict.gif", baseURL);
		        createImageDescriptor(ISVNUIConstants.IMG_MENU_SWITCH,       "tortoise/switch.gif", baseURL);
		        createImageDescriptor(ISVNUIConstants.IMG_MENU_MARKMERGED,   "tortoise/merge.gif", baseURL);
		        createImageDescriptor(ISVNUIConstants.IMG_MENU_MERGE,        "tortoise/merge.gif", baseURL);
		        createImageDescriptor(ISVNUIConstants.IMG_MENU_SHOWHISTORY,  "cview16/history_view.gif", baseURL);
		        createImageDescriptor(ISVNUIConstants.IMG_MENU_ANNOTATE,     "cview16/annotate_view.gif", baseURL);
		        createImageDescriptor(ISVNUIConstants.IMG_MENU_COPY,         "ctool16/copy_edit.gif", baseURL);
		        break;
        	case ISVNUIConstants.MENU_ICON_SET_SUBVERSIVE:
		        createImageDescriptor(ISVNUIConstants.IMG_MENU_UPDATE,       "subversive/update.gif", baseURL);
		        createImageDescriptor(ISVNUIConstants.IMG_MENU_COMMIT,       "subversive/commit.gif", baseURL);
		        createImageDescriptor(ISVNUIConstants.IMG_MENU_SYNC,         "subversive/synch.gif", baseURL);
		        createImageDescriptor(ISVNUIConstants.IMG_MENU_REVERT,       "subversive/revert.gif", baseURL);
		        createImageDescriptor(ISVNUIConstants.IMG_MENU_CHECKOUTAS,   "subversive/checkout.gif", baseURL);
		        createImageDescriptor(ISVNUIConstants.IMG_MENU_LOCK,         "subversive/lock.gif", baseURL);
		        createImageDescriptor(ISVNUIConstants.IMG_MENU_UNLOCK,       "subversive/unlock.gif", baseURL);
		        createImageDescriptor(ISVNUIConstants.IMG_MENU_BRANCHTAG,    "subversive/branch.gif", baseURL);
		        createImageDescriptor(ISVNUIConstants.IMG_MENU_SWITCH,       "subversive/switch.gif", baseURL);
		        createImageDescriptor(ISVNUIConstants.IMG_MENU_MERGE,        "subversive/merge.gif", baseURL);
        		createImageDescriptor(ISVNUIConstants.IMG_MENU_SHOWHISTORY,  "subversive/showhistory.gif", baseURL);
		        createImageDescriptor(ISVNUIConstants.IMG_MENU_EXPORT,       "subversive/export.gif", baseURL);
		        createImageDescriptor(ISVNUIConstants.IMG_MENU_IMPORTFOLDER, "subversive/import.gif", baseURL);
		        createImageDescriptor(ISVNUIConstants.IMG_MENU_ANNOTATE,     "subversive/annotate.gif", baseURL);
		        createImageDescriptor(ISVNUIConstants.IMG_MENU_COPY,         "subversive/copy.gif", baseURL);
		        break;
        	default: // CVS
		        createImageDescriptor(ISVNUIConstants.IMG_MENU_MERGE,        "tortoise/merge.gif", baseURL);
        		createImageDescriptor(ISVNUIConstants.IMG_MENU_SHOWHISTORY,  "cview16/history_view.gif", baseURL);
		        createImageDescriptor(ISVNUIConstants.IMG_MENU_ANNOTATE,     "cview16/annotate_view.gif", baseURL);
		        createImageDescriptor(ISVNUIConstants.IMG_MENU_COPY,         "ctool16/copy_edit.gif", baseURL);
		        createImageDescriptor(ISVNUIConstants.IMG_MENU_SHOWPROPERTY, "cview16/props_view.gif", baseURL);
        		break;
        }
    }
    
}
