/*******************************************************************************
 * Copyright (c) 2004, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.core;

/**
 * @author Brock Janiczak
 */
public interface ISVNCoreConstants {

    String PREF_RECURSIVE_STATUS_UPDATE = "resursive_status_update";
    String PREF_SHOW_OUT_OF_DATE_FOLDERS = "show_out_of_date_folders";
    
    public final int DEPTH_EMPTY = 0;
    public final int DEPTH_FILES = 1;
    public final int DEPTH_IMMEDIATES = 2;
    public final int DEPTH_INFINITY = 3;
    public final int DEPTH_UNKNOWN = -2;

}
