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
package org.tigris.subversion.subclipse.tools.usage.tracker.internal;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.tigris.subversion.subclipse.tools.usage.tracker.ILoggingAdapter;
import org.tigris.subversion.subclipse.tools.usage.util.LoggingUtils;

public class PluginLogger implements ILoggingAdapter {

	private final boolean tracingEnabled;

	private Plugin plugin;

	public PluginLogger(Plugin plugin) {
		this.tracingEnabled = LoggingUtils.isPluginTracingEnabled(plugin);
		this.plugin = plugin;
	}

	public void logError(String message) {
		log(IStatus.ERROR, message);
	}

	public void logMessage(String message) {
		log(IStatus.INFO, message);
	}

	private void log(int severity, String message) {
		if (!tracingEnabled) {
			return;
		}

		if (plugin != null) {
			IStatus status = new Status(severity, plugin.getBundle().getSymbolicName(), IStatus.OK, message, null);
			plugin.getLog().log(status);
		}
	}
}
