/******************************************************************************
 * This program and the accompanying materials are made available under
 * the terms of the Common Public License v1.0 which accompanies this
 * distribution, and is available at the following URL:
 * http://www.eclipse.org/legal/cpl-v10.html
 * Copyright(c) 2003-2005 by the authors indicated in the @author tags.
 *
 * All Rights are Reserved by the various authors.
 *******************************************************************************/
package org.tigris.subversion.subclipse.core;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;


/**
 * Preference Initializer.
 * Called at startup by Eclipse to initialize any default preferences.
 * 
 * @author markphip
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

    public PreferenceInitializer() {
        super();
    }

    public void initializeDefaultPreferences() {
        SVNProviderPlugin.getPlugin().getPluginPreferences().setDefault(ISVNCoreConstants.PREF_RECURSIVE_STATUS_UPDATE, true);
    }

}
