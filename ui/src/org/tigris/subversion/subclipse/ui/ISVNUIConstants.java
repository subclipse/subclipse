/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.tigris.subversion.subclipse.ui;

public interface ISVNUIConstants {
	// image path
	public final String ICON_PATH = "icons/full/"; //$NON-NLS-1$
	
	// images
	
	// overlays
	public final String IMG_MERGEABLE_CONFLICT = "ovr16/confauto_ov.gif"; //$NON-NLS-1$
	public final String IMG_QUESTIONABLE = "ovr16/question_ov.gif"; //$NON-NLS-1$
	public final String IMG_MERGED = "ovr16/merged_ov.gif"; //$NON-NLS-1$
	public final String IMG_CONFLICTED = "ovr16/conflicted_ov.gif"; //$NON-NLS-1$
	public final String IMG_EDITED = "ovr16/edited_ov.gif"; //$NON-NLS-1$
	public final String IMG_NO_REMOTEDIR = "ovr16/no_remotedir_ov.gif"; //$NON-NLS-1$
	public final String IMG_ADDED = "ovr16/added_ov.gif"; //$NON-NLS-1$
    public final String IMG_EXTERNAL = "ovr16/external_ov.gif"; //$NON-NLS-1$
    public final String IMG_LOCKED = "ovr16/locked_ov.gif"; //$NON-NLS-1$
    public final String IMG_NEEDSLOCK = "ovr16/protected_ov.gif"; //$NON-NLS-1$
	
	// objects
	public final String IMG_REPOSITORY = "obj16/repository_rep.gif"; //$NON-NLS-1$
	public final String IMG_TAG = "obj16/tag.gif"; //$NON-NLS-1$
	public final String IMG_BRANCHES_CATEGORY = "obj16/branches_rep.gif"; //$NON-NLS-1$
	public final String IMG_VERSIONS_CATEGORY = "obj16/versions_rep.gif"; //$NON-NLS-1$
	public final String IMG_MODULE = "obj16/module_rep.gif"; //$NON-NLS-1$
	public final String IMG_PROJECT_VERSION = "obj16/prjversions_rep.gif"; //$NON-NLS-1$
	
	// toolbar
	public final String IMG_REFRESH = "clcl16/refresh.gif"; //$NON-NLS-1$
	public final String IMG_CLEAR = "clcl16/clear_co.gif"; //$NON-NLS-1$
	public final String IMG_COLLAPSE_ALL = "clcl16/collapseall.gif"; //$NON-NLS-1$
	public final String IMG_LINK_WITH_EDITOR = "clcl16/synced.gif"; //$NON-NLS-1$
	
	// toolbar (disabled)
	public final String IMG_REFRESH_DISABLED = "dlcl16/refresh.gif"; //$NON-NLS-1$
	public final String IMG_CLEAR_DISABLED = "dlcl16/clear_co.gif"; //$NON-NLS-1$
	
	// toolbar (enabled)
	public final String IMG_REFRESH_ENABLED = "elcl16/refresh.gif"; //$NON-NLS-1$
	public final String IMG_CLEAR_ENABLED = "elcl16/clear_co.gif"; //$NON-NLS-1$
	public final String IMG_COLLAPSE_ALL_ENABLED = "elcl16/collapseall.gif"; //$NON-NLS-1$
	public final String IMG_LINK_WITH_EDITOR_ENABLED = "elcl16/synced.gif"; //$NON-NLS-1$
	
	// wizards
	public final String IMG_NEWLOCATION = "wizards/newlocation_wiz.gif"; //$NON-NLS-1$
    public final String IMG_NEWFOLDER = "wizards/newfolder_wiz.gif"; //$NON-NLS-1$
    
    // pending
    public final String IMG_FILEADD_PENDING = "pending16/fileadd_pending.gif"; //$NON-NLS-1$
    public final String IMG_FILEDELETE_PENDING = "pending16/filedelete_pending.gif"; //$NON-NLS-1$    
    public final String IMG_FOLDERADD_PENDING = "pending16/folderadd_pending.gif"; //$NON-NLS-1$
    public final String IMG_FOLDERDELETE_PENDING = "pending16/folderdelete_pending.gif"; //$NON-NLS-1$    
    public final String IMG_FILEMODIFIED_PENDING = "pending16/filemodified_pending.gif"; //$NON-NLS-1$
    public final String IMG_FOLDERMODIFIED_PENDING = "pending16/foldermodified_pending.gif"; //$NON-NLS-1$

    //  tortoise
    public final String IMG_COMMIT = "tortoise/commit.gif"; //$NON-NLS-1$
    public final String IMG_UPDATE = "tortoise/update.gif"; //$NON-NLS-1$
    public final String IMG_CONFLICT = "tortoise/conflict.gif"; //$NON-NLS-1$
    public final String IMG_REVERT = "tortoise/revert.gif"; //$NON-NLS-1$
    public final String IMG_RESOLVE = "tortoise/resolve.gif"; //$NON-NLS-1$
    public final String IMG_LOG = "tortoise/log.gif"; //$NON-NLS-1$
    public final String IMG_MERGE = "tortoise/merge.gif"; //$NON-NLS-1$
    
    // views
    public final String IMG_SVN_CONSOLE = "cview16/console_view.gif"; //$NON-NLS-1$
    
    //operations
    public final String IMG_CHECKOUT = "ctool16/checkout.gif"; //$NON-NLS-1$
    public final String IMG_ADD_PROPERTY = "ctool16/svn_prop_add.gif"; //$NON-NLS-1$
	
	// preferences
	public final String PREF_SHOW_COMMENTS = "pref_show_comments"; //$NON-NLS-1$
	public final String PREF_SHOW_TAGS = "pref_show_tags"; //$NON-NLS-1$
	public final String PREF_HISTORY_VIEW_EDITOR_LINKING = "pref_history_view_linking"; //$NON-NLS-1$
	public final String PREF_PRUNE_EMPTY_DIRECTORIES = "pref_prune_empty_directories";	 //$NON-NLS-1$
	public final String PREF_TIMEOUT = "pref_timeout";	 //$NON-NLS-1$
	public final String PREF_QUIETNESS = "pref_quietness"; //$NON-NLS-1$
	public final String PREF_SVN_SERVER = "pref_svn_server"; //$NON-NLS-1$
	public final String PREF_CONSIDER_CONTENTS = "pref_consider_contents"; //$NON-NLS-1$
	public final String PREF_SHOW_MARKERS = "pref_show_markers"; //$NON-NLS-1$
	public final String PREF_REPLACE_UNMANAGED = "pref_replace_unmanaged"; //$NON-NLS-1$
	public final String PREF_COMPRESSION_LEVEL = "pref_compression_level"; //$NON-NLS-1$
	public final String PREF_TEXT_KSUBST = "pref_text_ksubst"; //$NON-NLS-1$
	public final String PREF_FETCH_CHANGE_PATH_ON_DEMAND = "pref_fetch_change_path_on_Demand";

	public final String PREF_PROMPT_ON_MIXED_TAGS = "pref_prompt_on_mixed_tags"; //$NON-NLS-1$

	public final String PREF_PROMPT_ON_SAVING_IN_SYNC = "pref_prompt_on_saving_in_sync"; //$NON-NLS-1$
	public final String PREF_SAVE_DIRTY_EDITORS = "pref_save_dirty_editors"; //$NON-NLS-1$
	public final String PREF_PROMPT_ON_CHANGE_GRANULARITY = "pref_prompt_on_change_granularity"; //$NON-NLS-1$
	public final String PREF_REPOSITORIES_ARE_BINARY = "pref_repositories_are_binary"; //$NON-NLS-1$
	public final String PREF_DETERMINE_SERVER_VERSION = "pref_determine_server_version"; //$NON-NLS-1$
    public final String PREF_SHOW_ADDED_RESOURCES = "pref_show_added_resources"; //$NON-NLS-1$
    public final String PREF_SHOW_DELETED_RESOURCES = "pref_show_deleted_resources"; //$NON-NLS-1$
    public final String PREF_SHOW_MODIFIED_RESOURCES = "pref_show_modified_resources"; //$NON-NLS-1$
    public final String PREF_SHOW_COMPARE_REVISION_IN_DIALOG = "pref_show_compare_revision_in_dialog"; //$NON-NLS-1$
    public final String PREF_SELECT_UNADDED_RESOURCES_ON_COMMIT = "pref_select_unadded_resources_on_commit"; //$NON-NLS-1$ 
   
	// console preferences
	public final String PREF_CONSOLE_COMMAND_COLOR = "pref_console_command_color"; //$NON-NLS-1$
	public final String PREF_CONSOLE_MESSAGE_COLOR = "pref_console_message_color"; //$NON-NLS-1$
	public final String PREF_CONSOLE_ERROR_COLOR = "pref_console_error_color"; //$NON-NLS-1$
	public final String PREF_CONSOLE_FONT = "pref_console_font"; //$NON-NLS-1$
    public final String PREF_CONSOLE_SHOW_ON_ERROR = "pref_console_show_on_error"; //$NON-NLS-1$
    public final String PREF_CONSOLE_SHOW_ON_MESSAGE = "pref_console_show_on_message"; //$NON-NLS-1$
		
	// decorator preferences
	public final String PREF_FILETEXT_DECORATION = "pref_filetext_decoration"; //$NON-NLS-1$
	public final String PREF_FOLDERTEXT_DECORATION = "pref_foldertext_decoration"; //$NON-NLS-1$
	public final String PREF_PROJECTTEXT_DECORATION = "pref_projecttext_decoration"; //$NON-NLS-1$
	
	public final String PREF_SHOW_DIRTY_DECORATION = "pref_show_overlaydirty"; //$NON-NLS-1$
	public final String PREF_SHOW_ADDED_DECORATION = "pref_show_added"; //$NON-NLS-1$
    public final String PREF_SHOW_EXTERNAL_DECORATION = "pref_show_external"; //$NON-NLS-1$
	public final String PREF_SHOW_HASREMOTE_DECORATION = "pref_show_hasremote"; //$NON-NLS-1$
	public final String PREF_SHOW_NEWRESOURCE_DECORATION = "pref_show_newresource"; //$NON-NLS-1$
	
	public final String PREF_DIRTY_FLAG = "pref_dirty_flag"; //$NON-NLS-1$
	public final String PREF_ADDED_FLAG = "pref_added_flag"; //$NON-NLS-1$
    public final String PREF_EXTERNAL_FLAG = "pref_external_flag"; //$NON-NLS-1$
	
	public final String PREF_CALCULATE_DIRTY = "pref_calculate_dirty";	 //$NON-NLS-1$
	public final String PREF_CACHE_STATUS = "pref_cache_status";	 //$NON-NLS-1$
	
	public final String PREF_SHOW_SYNCINFO_AS_TEXT = "pref_show_syncinfo_as_text"; //$NON-NLS-1$

    // merge program preferences
    public final String PREF_MERGE_PROGRAM_LOCATION = "pref_merge_program_location"; //$NON-NLS-1$
    public final String PREF_MERGE_PROGRAM_PARAMETERS = "pref_merge_program_parameters"; //$NON-NLS-1$
    public final String PREF_MERGE_USE_EXTERNAL = "pref_merge_use_external"; //$NON-NLS-1$
    
	// watch/edit preferences
	public final String PREF_CHECKOUT_READ_ONLY = "pref_checkout_read_only"; //$NON-NLS-1$
	public final String PREF_EDIT_ACTION = "pref_edit_action"; //$NON-NLS-1$
	
	// Repositories view preferences
	public final String PREF_GROUP_VERSIONS_BY_PROJECT = "pref_group_versions_by_project"; //$NON-NLS-1$

    // svn client interface or svnjavahl
    public final String PREF_SVNINTERFACE = "pref_svninterface"; //$NON-NLS-1$
	
    // svn client config directory
    public final String PREF_SVNCONFIGDIR = "pref_svnconfigdir"; //$NON-NLS-1$
 
    // svn commit comment font
    public final String SVN_COMMENT_FONT = "svn_comment_font"; //$NON-NLS-1$
    
	// Wizard banners
	public final String IMG_WIZBAN_SHARE = "wizban/newconnect_wizban.gif";	 //$NON-NLS-1$
	public final String IMG_WIZBAN_MERGE = "wizban/mergestream_wizban.gif";	 //$NON-NLS-1$
	public final String IMG_WIZBAN_DIFF = "wizban/createpatch_wizban.gif";   //$NON-NLS-1$
	public final String IMG_WIZBAN_KEYWORD = "wizban/keywordsub_wizban.gif"; //$NON-NLS-1$
	public final String IMG_WIZBAN_NEW_LOCATION = "wizban/newlocation_wizban.gif"; //$NON-NLS-1$
    public final String IMG_WIZBAN_NEW_FOLDER = "wizban/newfolder_wizban.gif"; //$NON-NLS-1$
    
	// XXX checkout is same as ne connect. If it changes, it must be initialized
	public final String IMG_WIZBAN_CHECKOUT = "wizban/newconnect_wizban.gif";	 //$NON-NLS-1$
	
	// Properties
	public final String PROP_NAME = "svn.name"; //$NON-NLS-1$
	public final String PROP_REVISION = "svn.revision"; //$NON-NLS-1$
	public final String PROP_AUTHOR = "svn.author"; //$NON-NLS-1$
	public final String PROP_COMMENT = "svn.comment"; //$NON-NLS-1$
	public final String PROP_DATE = "svn.date"; //$NON-NLS-1$
	public final String PROP_DIRTY = "svn.dirty"; //$NON-NLS-1$

	public final String PROP_MODIFIED = "svn.modified"; //$NON-NLS-1$
	public final String PROP_KEYWORD = "svn.date"; //$NON-NLS-1$
	public final String PROP_TAG = "svn.tag"; //$NON-NLS-1$
	public final String PROP_PERMISSIONS = "svn.permissions"; //$NON-NLS-1$
	public final String PROP_HOST = "svn.host"; //$NON-NLS-1$
	public final String PROP_USER = "svn.user"; //$NON-NLS-1$
	public final String PROP_METHOD = "svn.method"; //$NON-NLS-1$
	public final String PROP_PORT = "svn.port"; //$NON-NLS-1$
	public final String PROP_ROOT = "svn.root"; //$NON-NLS-1$
	
	// preference options
	public final int OPTION_NEVER = 1; //$NON-NLS-1$
	public final int OPTION_PROMPT = 2; //$NON-NLS-1$
	public final int OPTION_AUTOMATIC = 3;

}

