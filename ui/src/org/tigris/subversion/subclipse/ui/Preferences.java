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

import java.io.File;

import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.RGB;
import org.tigris.subversion.subclipse.core.SVNClientManager;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.ui.decorator.SVNDecoratorConfiguration;

/**
 * Initializes preferences and updates markers when preferences are changed
 */
public class Preferences implements IPropertyChangeListener {

private IPreferenceStore store;

    public Preferences(IPreferenceStore store) {
        this.store = store;
        store.addPropertyChangeListener(this);
    }

    /**
     * Initializes the preferences for this plugin if necessary.
     */
    public void initializePreferences() {
        PreferenceConverter.setDefault(store, ISVNUIConstants.PREF_CONSOLE_COMMAND_COLOR, new RGB(0, 0, 0));
        PreferenceConverter.setDefault(store, ISVNUIConstants.PREF_CONSOLE_MESSAGE_COLOR, new RGB(0, 0, 255));
        PreferenceConverter.setDefault(store, ISVNUIConstants.PREF_CONSOLE_ERROR_COLOR, new RGB(255, 0, 0));
        
        store.setDefault(ISVNUIConstants.PREF_SHOW_COMMENTS, true);
        store.setDefault(ISVNUIConstants.PREF_WRAP_COMMENTS, true);
        store.setDefault(ISVNUIConstants.PREF_SHOW_PATHS, true);
        store.setDefault(ISVNUIConstants.PREF_AFFECTED_PATHS_MODE, ISVNUIConstants.MODE_FLAT);
        store.setDefault(ISVNUIConstants.PREF_AFFECTED_PATHS_LAYOUT, ISVNUIConstants.LAYOUT_HORIZONTAL);
        
        store.setDefault(ISVNUIConstants.PREF_CONSOLE_SHOW_ON_MESSAGE, false);
        store.setDefault(ISVNUIConstants.PREF_CONSOLE_SHOW_ON_ERROR, true);
		store.setDefault(ISVNUIConstants.PREF_CONSOLE_LIMIT_OUTPUT, true);	
		store.setDefault(ISVNUIConstants.PREF_CONSOLE_HIGH_WATER_MARK, 500000);	
		
        store.setDefault(ISVNUIConstants.PREF_FILETEXT_DECORATION, SVNDecoratorConfiguration.DEFAULT_FILETEXTFORMAT);
        store.setDefault(ISVNUIConstants.PREF_FOLDERTEXT_DECORATION, SVNDecoratorConfiguration.DEFAULT_FOLDERTEXTFORMAT);
        store.setDefault(ISVNUIConstants.PREF_PROJECTTEXT_DECORATION, SVNDecoratorConfiguration.DEFAULT_PROJECTTEXTFORMAT);
        
        store.setDefault(ISVNUIConstants.PREF_ADDED_FLAG, SVNDecoratorConfiguration.DEFAULT_ADDED_FLAG);
        store.setDefault(ISVNUIConstants.PREF_DIRTY_FLAG, SVNDecoratorConfiguration.DEFAULT_DIRTY_FLAG);
        store.setDefault(ISVNUIConstants.PREF_EXTERNAL_FLAG, SVNDecoratorConfiguration.DEFAULT_EXTERNAL_FLAG);
        store.setDefault(ISVNUIConstants.PREF_SHOW_EXTERNAL_DECORATION, true);
        store.setDefault(ISVNUIConstants.PREF_SHOW_ADDED_DECORATION, true);
        store.setDefault(ISVNUIConstants.PREF_SHOW_HASREMOTE_DECORATION, true);
        store.setDefault(ISVNUIConstants.PREF_SHOW_DIRTY_DECORATION, true);
        store.setDefault(ISVNUIConstants.PREF_SHOW_NEWRESOURCE_DECORATION, true);
        store.setDefault(ISVNUIConstants.PREF_CALCULATE_DIRTY, true);
        store.setDefault(ISVNUIConstants.PREF_USE_FONT_DECORATORS, false);
        store.setDefault(ISVNUIConstants.PREF_SHOW_SYNCINFO_AS_TEXT, false);        
        store.setDefault(ISVNUIConstants.PREF_PROMPT_ON_MIXED_TAGS, true);
        store.setDefault(ISVNUIConstants.PREF_PROMPT_ON_SAVING_IN_SYNC, true);
        store.setDefault(ISVNUIConstants.PREF_SAVE_DIRTY_EDITORS, ISVNUIConstants.OPTION_PROMPT);
        
        store.setDefault(ISVNUIConstants.PREF_SHOW_COMPARE_REVISION_IN_DIALOG, false);
        store.setDefault(ISVNUIConstants.PREF_SHOW_UNADDED_RESOURCES_ON_COMMIT, true);
        store.setDefault(ISVNUIConstants.PREF_SELECT_UNADDED_RESOURCES_ON_COMMIT, false);
		store.setDefault(ISVNUIConstants.PREF_REMOVE_UNADDED_RESOURCES_ON_REPLACE, true);
        store.setDefault(ISVNUIConstants.PREF_COMMIT_SET_DEFAULT_ENABLEMENT, false);
        
        store.setDefault(ISVNUIConstants.PREF_USE_JAVAHL_COMMIT_HACK, false);
        
        store.setDefault(ISVNUIConstants.PREF_SVNINTERFACE, "javahl"); //$NON-NLS-1$
        store.setDefault(ISVNUIConstants.PREF_SVNCONFIGDIR, ""); //$NON-NLS-1$
        
        store.setDefault(ISVNUIConstants.PREF_FETCH_CHANGE_PATH_ON_DEMAND, false);
        store.setDefault(ISVNUIConstants.PREF_LOG_ENTRIES_TO_FETCH, 25);
        store.setDefault(ISVNUIConstants.PREF_STOP_ON_COPY, false);

        store.setDefault(ISVNUIConstants.PREF_MERGE_USE_EXTERNAL, false);
        store.setDefault(ISVNUIConstants.PREF_MERGE_PROGRAM_LOCATION,""); //$NON-NLS-1$
        store.setDefault(ISVNUIConstants.PREF_MERGE_PROGRAM_PARAMETERS,""); //$NON-NLS-1$
        
        store.setDefault(ISVNUIConstants.PREF_USE_QUICKDIFFANNOTATE, MessageDialogWithToggle.PROMPT);

        store.setDefault(ISVNUIConstants.PREF_MENU_ICON_SET, ISVNUIConstants.MENU_ICON_SET_DEFAULT);
        
        setSvnClientInterface(store.getString(ISVNUIConstants.PREF_SVNINTERFACE));
        setSvnClientConfigDir(store.getString(ISVNUIConstants.PREF_SVNCONFIGDIR));
        
        setSvnChangePathOnDemand((store.getBoolean(ISVNUIConstants.PREF_FETCH_CHANGE_PATH_ON_DEMAND)));
    }

    /**
     * set the svn client interface
     * @param clientInterface
     */
    private void setSvnClientInterface(String clientInterface) {
        SVNProviderPlugin.getPlugin().getSVNClientManager().setSvnClientInterface(clientInterface);
    }

    /**
     * set the svn client config dir
     * @param configDir
     */
    private void setSvnClientConfigDir(String configDir) {
        SVNProviderPlugin plugin = SVNProviderPlugin.getPlugin();
        SVNClientManager svnClientManager = plugin.getSVNClientManager();
        if ("".equals(configDir)) { //$NON-NLS-1$
        	svnClientManager.setConfigDir(null);
        } else {
        	File configDirFile = new File(configDir);
            svnClientManager.setConfigDir(configDirFile);
        }
    }
    
    /**
     * @see IPropertyChangeListener#propertyChange(PropertyChangeEvent)
     */
    public void propertyChange(PropertyChangeEvent event) {
        String property = event.getProperty();
        if (property == ISVNUIConstants.PREF_SVNINTERFACE) {
            String newValue = (String)event.getNewValue();
            setSvnClientInterface(newValue);
        }
        if (property == ISVNUIConstants.PREF_SVNCONFIGDIR) {
        	String configDir = (String)event.getNewValue();
            setSvnClientConfigDir(configDir);
        }
        if (property == ISVNUIConstants.PREF_FETCH_CHANGE_PATH_ON_DEMAND) {
        	boolean fetchChangePathOnDemand = ((Boolean) event.getNewValue()).booleanValue();
        	setSvnChangePathOnDemand(fetchChangePathOnDemand);        	
        }
            
    }

	/**
	 * @param fetchChangePathOnDemand
	 */
	private void setSvnChangePathOnDemand(boolean fetchChangePathOnDemand) {
		SVNProviderPlugin plugin = SVNProviderPlugin.getPlugin();
		SVNClientManager svnClientManager = plugin.getSVNClientManager();
		svnClientManager.setFetchChangePathOnDemand(fetchChangePathOnDemand);
	}


}
