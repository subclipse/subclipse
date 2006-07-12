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

import org.eclipse.core.runtime.IProgressMonitor;



/**
 * The SVN analog of a directory. SVN folders have access to synchronization information
 * that describes the association between the folder and the remote repository.
 * 
 * @see ISVNResource
 * @see ISVNFile
 */
public interface ISVNFolder extends ISVNResource {
	
	public static final int FILE_MEMBERS = 1;
	public static final int FOLDER_MEMBERS = 2;
	public static final int IGNORED_MEMBERS = 4;
	public static final int UNMANAGED_MEMBERS = 8;
	public static final int MANAGED_MEMBERS = 16;
	public static final int EXISTING_MEMBERS = 32;
	public static final int PHANTOM_MEMBERS = 64;
	public static final int ALL_MEMBERS = FILE_MEMBERS 
		| FOLDER_MEMBERS 
		| IGNORED_MEMBERS 
		| UNMANAGED_MEMBERS 
		| MANAGED_MEMBERS 
		| EXISTING_MEMBERS
		| PHANTOM_MEMBERS;
	public static final int ALL_EXISTING_MEMBERS = FILE_MEMBERS 
		| FOLDER_MEMBERS 
		| IGNORED_MEMBERS 
		| UNMANAGED_MEMBERS 
		| MANAGED_MEMBERS 
		| EXISTING_MEMBERS;
	public static final int ALL_UNIGNORED_MEMBERS = FILE_MEMBERS
		| FOLDER_MEMBERS
		| UNMANAGED_MEMBERS
		| MANAGED_MEMBERS
		| EXISTING_MEMBERS
		| PHANTOM_MEMBERS;
	public static final int ALL_EXISTING_UNIGNORED_MEMBERS = FILE_MEMBERS 
		| FOLDER_MEMBERS 
		| UNMANAGED_MEMBERS 
		| MANAGED_MEMBERS 
		| EXISTING_MEMBERS;

	/**
	 * Answer the immediate children of the resource 
	 * The flags indicate the type of members to be included.
	 * Here are the rules for specifying just one flag:
	 * 
	 *   a) FILE_MEMBERS and FOLDER_MEMBERS will return managed 
	 *     and unmanaged resource of the corresponding type
	 *   b) IGNORED_MEMBERS, MANAGED_RESOURCES and UNMANAGED_RESOURCES
	 *     will return files and folders of the given type
	 *   c) EXISTING_MEMBERS and PHANTOM_MEMBERS will return existing 
	 *     and phatom resource of the corresponding type
	 * 
	 * Note: Unmanaged resources are those that are neither managed or ignored.
	 * 
	 * If all of the flags from either group a), group b) or group c)
	 * are not present, the same rule for default types applies. 
	 * For example,
	 * - FILE_MEMBERS | FOLDER_MEMBERS will return all managed
	 *   and unmanaged, existing and phantom files and folders. 
	 * - IGNORED_MEMBERS | UNMANAGED_MEMBERS will return all
	 *   ignored or unmanaged, existing or phantom files and folders
	 * If a flag from each group is present, the result is the
	 * union of the sets. For example,
	 * - FILE_MEMBERS | IGNORED_MEMBERS | EXISTING_MEMBERS will return all
	 *   existing ignored files.
	 */
	public ISVNResource[] members(IProgressMonitor monitor,int flags) throws SVNException;

	
}
