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
package org.tigris.subversion.subclipse.tools.usage.util;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;

public class LoggingUtils {

	public static boolean isPluginTracingEnabled(Plugin plugin) {
		return plugin != null && plugin.isDebugging();
	}

	public static void log(IStatus status, Plugin plugin) {
		if (status.getSeverity() == IStatus.INFO && !isPluginTracingEnabled(plugin)) {
			return;
		}
		plugin.getLog().log(status);
	}
}
