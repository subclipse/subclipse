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
package org.tigris.subversion.subclipse.core;


import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.tigris.subversion.subclipse.core.internal.InfiniteSubProgressMonitor;

public class Policy {
	
	/** The initial guess for number of resources(pm ticks) the infinite subPm should start with in case of checkout */
	public static final int INFINITE_PM_GUESS_FOR_CHECKOUT = 1000;
	/** The initial guess for number of resources(pm ticks) the infinite subPm should start with in case of update/switch */
	public static final int INFINITE_PM_GUESS_FOR_SWITCH = 100;
	
    private static final String BUNDLE_NAME = "org.tigris.subversion.subclipse.core.messages"; //$NON-NLS-1$
	protected static ResourceBundle bundle = null;
	
	//debug constants
	// You can enable debugging in the .options file :
	//   org.tigris.subversion.subclipse.core/debug=true
	//   org.tigris.subversion.subclipse.core/metafiles=true
    //   org.tigris.subversion.subclipse.core/threading=true
	//   org.tigris.subversion.subclipse.core/status=true
    // You can also enable tracing in the PDE target run-time tab 
	public static boolean DEBUG_METAFILE_CHANGES = false; 
	public static boolean DEBUG_THREADING = false; // used in ReetrantLock
	public static boolean DEBUG_STATUS = false;
    
	static {
		//init debug options
		if (SVNProviderPlugin.getPlugin().isDebugging()) {
			DEBUG_METAFILE_CHANGES = "true".equalsIgnoreCase(Platform.getDebugOption(SVNProviderPlugin.ID + "/metafiles"));//$NON-NLS-1$ //$NON-NLS-2$
			DEBUG_THREADING = "true".equalsIgnoreCase(Platform.getDebugOption(SVNProviderPlugin.ID + "/threading"));//$NON-NLS-1$ //$NON-NLS-2$
            DEBUG_STATUS = "true".equalsIgnoreCase(Platform.getDebugOption(SVNProviderPlugin.ID + "/status"));//$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private static ResourceBundle getResourceBundle() {
        if (bundle == null) {
            bundle = ResourceBundle.getBundle(BUNDLE_NAME);
        }
        return bundle;
    }
	
	/**
	 * Lookup the message with the given ID in this catalog and bind its
	 * substitution locations with the given string.
	 */
	public static String bind(String id, String binding) {
		return bind(id, new String[] { binding });
	}
	
	/**
	 * Lookup the message with the given ID in this catalog and bind its
	 * substitution locations with the given strings.
	 */
	public static String bind(String id, String binding1, String binding2) {
		return bind(id, new String[] { binding1, binding2 });
	}
	
	/**
	 * Gets a string from the resource bundle. We don't want to crash because of a missing String.
	 * Returns the key if not found.
	 */
	public static String bind(String key) {
		try {
			return getResourceBundle().getString(key);
		} catch (MissingResourceException e) {
			return key;
		} catch (NullPointerException e) {
			return "!" + key + "!"; //$NON-NLS-1$  //$NON-NLS-2$
		}
	}
	
	/**
	 * Gets a string from the resource bundle and binds it with the given arguments. If the key is 
	 * not found, return the key.
	 */
	public static String bind(String key, Object[] args) {
		try {
			return MessageFormat.format(bind(key), args);
		} catch (MissingResourceException e) {
			return key;
		} catch (NullPointerException e) {
			return "!" + key + "!";  //$NON-NLS-1$  //$NON-NLS-2$
		}
	}
	

    /**
     * throw an OperationCanceledException if monitor.isCanceled()
     */
	public static void checkCanceled(IProgressMonitor monitor) {
		if (monitor.isCanceled())
			throw new OperationCanceledException();
	}
    
    /**
     * Return a NullProgressMonitor if monitor is null or monitor itself otherwise 
     */
	public static IProgressMonitor monitorFor(IProgressMonitor monitor) {
		if (monitor == null)
			return new NullProgressMonitor();
		return monitor;
	}	
	
    /**
     * Return a submonitor for monitor 
	 * @param monitor the parent progress monitor
	 * @param ticks the number of work ticks allocated from the
	 *    parent monitor
	 * @return IProgressMonitor
     */
	public static IProgressMonitor subMonitorFor(IProgressMonitor monitor, int ticks) {
		if (monitor == null)
			return new NullProgressMonitor();
		if (monitor instanceof NullProgressMonitor)
			return monitor;
		return new SubProgressMonitor(monitor, ticks, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK);
	}

    /**
     * Return a submonitor for monitor 
	 * @param monitor the parent progress monitor
	 * @param ticks the number of work ticks allocated from the
	 *    parent monitor
	 * @param style one of
	 *    <ul>
	 *    <li> <code>SubProgressMonitor#SUPPRESS_SUBTASK_LABEL</code> </li>
	 *    <li> <code>SubProgressMonitor#PREPEND_MAIN_LABEL_TO_SUBTASK</code> </li>
	 *    </ul>
	 * @see SubProgressMonitor#SUPPRESS_SUBTASK_LABEL
	 * @see SubProgressMonitor#PREPEND_MAIN_LABEL_TO_SUBTASK
	 * @return IProgressMonitor
     */
	public static IProgressMonitor subMonitorFor(IProgressMonitor monitor, int ticks, int style) {
		if (monitor == null)
			return new NullProgressMonitor();
		if (monitor instanceof NullProgressMonitor)
			return monitor;
		return new SubProgressMonitor(monitor, ticks, style);
	}
	
	/**
	 * Return a submonitor for cases when we do not know the number of ticks ...
	 * The main task label will be prepended to the subtask label.
	 * @param monitor the parent progress monitor
	 * @param ticks the number of work ticks allocated from the
	 *    parent monitor
	 * @return IProgressMonitor
	 */
	public static IProgressMonitor infiniteSubMonitorFor(IProgressMonitor monitor, int ticks) {
		if (monitor == null)
			return new NullProgressMonitor();
		if (monitor instanceof NullProgressMonitor)
			return monitor;
		return new InfiniteSubProgressMonitor(monitor, ticks, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK);
	}

	/**
	 * Return a submonitor for cases when we do not know the number of ticks ...
	 * The main task label will be prepended to the subtask label.
	 * @param monitor the parent progress monitor
	 * @param ticks the number of work ticks allocated from the
	 *    parent monitor
	 * @param style one of
	 *    <ul>
	 *    <li> <code>SubProgressMonitor#SUPPRESS_SUBTASK_LABEL</code> </li>
	 *    <li> <code>SubProgressMonitor#PREPEND_MAIN_LABEL_TO_SUBTASK</code> </li>
	 *    </ul>
	 * @see SubProgressMonitor#SUPPRESS_SUBTASK_LABEL
	 * @see SubProgressMonitor#PREPEND_MAIN_LABEL_TO_SUBTASK
	 * @return IProgressMonitor
	 */
	public static IProgressMonitor infiniteSubMonitorFor(IProgressMonitor monitor, int ticks, int style) {
		if (monitor == null)
			return new NullProgressMonitor();
		if (monitor instanceof NullProgressMonitor)
			return monitor;
		return new InfiniteSubProgressMonitor(monitor, ticks, style);
	}

 }
