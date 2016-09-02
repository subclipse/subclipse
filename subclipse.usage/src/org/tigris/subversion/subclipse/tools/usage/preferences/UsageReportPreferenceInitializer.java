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

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;

public class UsageReportPreferenceInitializer extends
		AbstractPreferenceInitializer {

	public void initializeDefaultPreferences() {
		UsageReportPreferencesUtils.getStore().setDefault(
				IUsageReportPreferenceConstants.ASK_USER_USAGEREPORT_ID,
				IUsageReportPreferenceConstants.ASK_USER_USAGEREPORT_DEFAULTVALUE);

		UsageReportPreferencesUtils.getStore().setDefault(
				IUsageReportPreferenceConstants.USAGEREPORT_ENABLED_ID,
				IUsageReportPreferenceConstants.USAGEREPORT_ENABLED_DEFAULTVALUE);
	}
}
