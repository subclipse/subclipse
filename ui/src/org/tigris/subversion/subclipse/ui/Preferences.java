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

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.RGB;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.ui.decorator.SVNDecoratorConfiguration;
import org.tigris.subversion.svnclientadapter.SVNClientAdapterFactory;

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
        
        store.setDefault(ISVNUIConstants.PREF_FILETEXT_DECORATION, SVNDecoratorConfiguration.DEFAULT_FILETEXTFORMAT);
        store.setDefault(ISVNUIConstants.PREF_FOLDERTEXT_DECORATION, SVNDecoratorConfiguration.DEFAULT_FOLDERTEXTFORMAT);
        store.setDefault(ISVNUIConstants.PREF_PROJECTTEXT_DECORATION, SVNDecoratorConfiguration.DEFAULT_PROJECTTEXTFORMAT);
        
        store.setDefault(ISVNUIConstants.PREF_ADDED_FLAG, SVNDecoratorConfiguration.DEFAULT_ADDED_FLAG);
        store.setDefault(ISVNUIConstants.PREF_DIRTY_FLAG, SVNDecoratorConfiguration.DEFAULT_DIRTY_FLAG);    
        store.setDefault(ISVNUIConstants.PREF_SHOW_ADDED_DECORATION, true);
        store.setDefault(ISVNUIConstants.PREF_SHOW_HASREMOTE_DECORATION, true);
        store.setDefault(ISVNUIConstants.PREF_SHOW_DIRTY_DECORATION, false);
        store.setDefault(ISVNUIConstants.PREF_SHOW_NEWRESOURCE_DECORATION, true);
        store.setDefault(ISVNUIConstants.PREF_CALCULATE_DIRTY, true);
        store.setDefault(ISVNUIConstants.PREF_SHOW_SYNCINFO_AS_TEXT, false);        
        store.setDefault(ISVNUIConstants.PREF_PROMPT_ON_MIXED_TAGS, true);
        store.setDefault(ISVNUIConstants.PREF_PROMPT_ON_SAVING_IN_SYNC, true);
        store.setDefault(ISVNUIConstants.PREF_SAVE_DIRTY_EDITORS, ISVNUIConstants.OPTION_PROMPT);
        
        store.setDefault(ISVNUIConstants.PREF_SVNINTERFACE, SVNClientAdapterFactory.JAVAHL_CLIENT);
        
        SVNProviderPlugin.getPlugin().setSvnClientInterface(store.getInt(ISVNUIConstants.PREF_SVNINTERFACE));
    }

    /**
     * @see IPropertyChangeListener#propertyChange(PropertyChangeEvent)
     */
    public void propertyChange(PropertyChangeEvent event) {
        String property = event.getProperty();
        if (property == ISVNUIConstants.PREF_SVNINTERFACE) {
            Integer newValue = (Integer)event.getNewValue();
            SVNProviderPlugin.getPlugin().setSvnClientInterface(newValue.intValue());
        }
            
    }


}
