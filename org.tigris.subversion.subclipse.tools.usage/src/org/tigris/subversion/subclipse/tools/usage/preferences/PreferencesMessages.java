/*******************************************************************************
 * Copyright (c) 2010 Subclipse project and others.
 * Copyright (c) 2010 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.tools.usage.preferences;

import org.eclipse.osgi.util.NLS;

public class PreferencesMessages extends NLS {
	private static final String BUNDLE_NAME = "org.tigris.subversion.subclipse.tools.usage.preferences.messages"; //$NON-NLS-1$

	public static String UsageReportPreferencePage_Description;
	public static String UsageReportPreferencePage_AllowReporting;
	public static String UsageReportPreferencePage_Error_Saving;

	public static String GlobalUsageSettings_RemoteProps_URL;

	public static String UsageReportPreferencePage_Description_JBDS;
	public static String UsageReportPreferencePage_AllowReporting_JBDS;
	public static String UsageReportPreferencePage_Error_Saving_JBDS;

	public static String GlobalUsageSettings_RemoteProps_URL_JBDS;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, PreferencesMessages.class);
	}

	private PreferencesMessages() {
	}
}
