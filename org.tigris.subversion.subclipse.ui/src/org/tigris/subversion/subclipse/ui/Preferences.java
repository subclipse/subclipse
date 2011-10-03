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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Display;
import org.tigris.subversion.subclipse.core.SVNClientManager;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.ui.dialogs.UnsupportedPasswordStoresDialog;

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
     * Initializes various options by using the values from the preferences. To 
     * be called during plugin initialization.
     */
    public void initializeFromSettings() {
        setSvnClientConfigDir(store.getString(ISVNUIConstants.PREF_SVNCONFIGDIR));
        setSvnClientInterface(store.getString(ISVNUIConstants.PREF_SVNINTERFACE));
        setSvnChangePathOnDemand(store.getBoolean(ISVNUIConstants.PREF_FETCH_CHANGE_PATH_ON_DEMAND));
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
    	if (SVNUIPlugin.getPlugin().passwordStoresConfiguredOnLinux()) {   
    		if (!SVNUIPlugin.TEST_MODE) {
	    		Display.getDefault().syncExec(new Runnable() {
					public void run() {
				   		UnsupportedPasswordStoresDialog dialog = new UnsupportedPasswordStoresDialog(Display.getDefault().getActiveShell());
			    		if (dialog.open() == UnsupportedPasswordStoresDialog.OK) {
			    			try {
								SVNUIPlugin.getPlugin().clearPasswordStoresFromConfiguration(false);
							} catch (Exception e) {
								SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
								MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.Preferences_0, e.getMessage());
							}
			    		}
					}   			
	    		});
    		}
    	}
    	
        SVNProviderPlugin plugin = SVNProviderPlugin.getPlugin();
        SVNClientManager svnClientManager = plugin.getSVNClientManager();
        if (configDir == null || "".equals(configDir)) { //$NON-NLS-1$
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
