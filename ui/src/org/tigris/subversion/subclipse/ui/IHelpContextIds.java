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

/**
 * Here's how to reference the help context in code:
 * 
 * WorkbenchHelp.setHelp(actionOrControl, IHelpContextIds.NAME_DEFIED_BELOW);
 */
public interface IHelpContextIds {

	public static final String PREFIX = SVNUIPlugin.ID + "."; //$NON-NLS-1$

	// Dialogs
	public static final String TAG_CONFIGURATION_OVERVIEW = PREFIX + "tag_configuration_overview"; //$NON-NLS-1$
	public static final String TAG_CONFIGURATION_REFRESHLIST = PREFIX + "tag_configuration_refreshlist"; //$NON-NLS-1$
	public static final String TAG_CONFIGURATION_REFRESHACTION = PREFIX + "tag_configuration_refreshaction"; //$NON-NLS-1$
	public static final String USER_VALIDATION_DIALOG = PREFIX + "user_validation_dialog_context"; //$NON-NLS-1$
	public static final String RELEASE_COMMENT_DIALOG = PREFIX + "release_comment_dialog_context"; //$NON-NLS-1$
	public static final String BRANCH_DIALOG = PREFIX + "branch_dialog_context"; //$NON-NLS-1$
	public static final String ADD_TO_VERSION_CONTROL_DIALOG = PREFIX + "add_to_version_control_dialog_context"; //$NON-NLS-1$
	public static final String SYNCHRONIZE_PROJECTS_DIALOG = PREFIX + "sychronize_projects_dialog_context"; //$NON-NLS-1$
	public static final String EDITORS_DIALOG = PREFIX + "editors_dialog_context"; //$NON-NLS-1$
	public static final String HISTORY_FILTER_DIALOG = PREFIX + "history_filter_dialog_context"; //$NON-NLS-1$
	
	// Different uses of the TagSelectionDialog
	public static final String REPLACE_TAG_SELECTION_DIALOG = PREFIX + "replace_tag_selection_dialog_context"; //$NON-NLS-1$
	public static final String COMPARE_TAG_SELECTION_DIALOG = PREFIX + "compare_tag_selection_dialog_context"; //$NON-NLS-1$
	public static final String TAG_REMOTE_WITH_EXISTING_DIALOG = PREFIX + "tag_remote_with_existing_dialog_context"; //$NON-NLS-1$
	public static final String SHARE_WITH_EXISTING_TAG_SELETION_DIALOG = PREFIX + "share_with_existing_tag_selection_dialog_context"; //$NON-NLS-1$

	// Different uses of the TagAsVersionDialog
	public static final String TAG_AS_VERSION_DIALOG = PREFIX + "tag_as_version_dialog_context"; //$NON-NLS-1$

	// Wizard Pages
	public static final String SHARING_AUTOCONNECT_PAGE = PREFIX + "sharing_autoconnect_page_context"; //$NON-NLS-1$
	public static final String SHARING_SELECT_REPOSITORY_PAGE = PREFIX + "sharing_select_repository_page_context"; //$NON-NLS-1$
	public static final String SHARING_NEW_REPOSITORY_PAGE = PREFIX + "sharing_new_repository_page_context"; //$NON-NLS-1$
	public static final String SHARING_MODULE_PAGE = PREFIX + "sharing_module_page_context"; //$NON-NLS-1$
	public static final String SHARING_FINISH_PAGE = PREFIX + "sharing_finish_page_context"; //$NON-NLS-1$
	public static final String PATCH_SELECTION_PAGE = PREFIX + "patch_selection_page_context"; //$NON-NLS-1$
	public static final String PATCH_OPTIONS_PAGE = PREFIX + "patch_options_page_context"; //$NON-NLS-1$
	public static final String KEYWORD_SUBSTITUTION_SELECTION_PAGE = PREFIX + "keyword_substituton_selection_page_context"; //$NON-NLS-1$
	public static final String KEYWORD_SUBSTITUTION_SUMMARY_PAGE = PREFIX + "keyword_substituton_summary_page_context"; //$NON-NLS-1$
	public static final String KEYWORD_SUBSTITUTION_SHARED_PAGE = PREFIX + "keyword_substituton_shared_page_context"; //$NON-NLS-1$
	public static final String KEYWORD_SUBSTITUTION_CHANGED_PAGE = PREFIX + "keyword_substituton_changed_page_context"; //$NON-NLS-1$
	public static final String KEYWORD_SUBSTITUTION_COMMIT_COMMENT_PAGE = PREFIX + "keyword_substituton_commit_comment_page_context"; //$NON-NLS-1$
	public static final String MERGE_START_PAGE = PREFIX + "merge_start_page_context"; //$NON-NLS-1$
	public static final String MERGE_END_PAGE = PREFIX + "merge_end_page_context"; //$NON-NLS-1$
	public static final String CHECKOUT_INTO_RESOURCE_SELECTION_PAGE = PREFIX + "checkout_into_resource_selection_page_context"; //$NON-NLS-1$
	public static final String RESTORE_FROM_REPOSITORY_FILE_SELECTION_PAGE = PREFIX + "restore_from_repository_file_selection_page_context"; //$NON-NLS-1$
	public static final String WORKING_SET_FOLDER_SELECTION_PAGE = PREFIX + "working_set_folder_selection_page_context"; //$NON-NLS-1$
	public static final String REFRESH_REMOTE_PROJECT_SELECTION_PAGE = PREFIX + "refresh_remote_project_selection_page_context"; //$NON-NLS-1$

	// Preference Pages
	public static final String PREF_PRUNE = PREFIX + "prune_empty_directories_pref"; //$NON-NLS-1$
	public static final String PREF_QUIET = PREFIX + "quietness_level_pref"; //$NON-NLS-1$
	public static final String PREF_COMPRESSION = PREFIX + "compression_level_pref"; //$NON-NLS-1$
	public static final String PREF_KEYWORDMODE = PREFIX + "default_keywordmode_pref"; //$NON-NLS-1$
	public static final String PREF_COMMS_TIMEOUT = PREFIX + "comms_timeout_pref"; //$NON-NLS-1$
	public static final String PREF_CONSIDER_CONTENT = PREFIX + "consider_content_pref"; //$NON-NLS-1$
	public static final String PREF_MARKERS_ENABLED = PREFIX + "markers_enabled_pref"; //$NON-NLS-1$
	public static final String PREF_REPLACE_DELETE_UNMANAGED = PREFIX + "replace_deletion_of_unmanaged_pref"; //$NON-NLS-1$
	public static final String PREF_SAVE_DIRTY_EDITORS = PREFIX + "save_dirty_editors_pref"; //$NON-NLS-1$
	public static final String PREF_TREAT_NEW_FILE_AS_BINARY = PREFIX + "treat_new_files_as_binary_pref"; //$NON-NLS-1$
	public static final String PREF_DETERMINE_SERVER_VERSION = PREFIX + "determine_server_version"; //$NON-NLS-1$

	public static final String CONSOLE_PREFERENCE_PAGE = PREFIX + "console_preference_page_context"; //$NON-NLS-1$
	public static final String EXT_PREFERENCE_PAGE = PREFIX + "ext_preference_page_context"; //$NON-NLS-1$
	public static final String EXT_PREFERENCE_RSH = PREFIX + "ext_preference_rsh_context"; //$NON-NLS-1$
	public static final String EXT_PREFERENCE_PARAM = PREFIX + "ext_preference_param_context"; //$NON-NLS-1$
	public static final String EXT_PREFERENCE_SERVER = PREFIX + "ext_preference_server_context"; //$NON-NLS-1$
	public static final String DECORATORS_PREFERENCE_PAGE = PREFIX + "decorators_preference_page_context"; //$NON-NLS-1$
	public static final String WATCH_EDIT_PREFERENCE_PAGE = PREFIX + "watch_edit_preference_page_context"; //$NON-NLS-1$

	// Views
	public static final String CONSOLE_VIEW = PREFIX + "console_view_context"; //$NON-NLS-1$
	public static final String REPOSITORIES_VIEW = PREFIX + "repositories_view_context"; //$NON-NLS-1$
	public static final String RESOURCE_HISTORY_VIEW = PREFIX + "resource_history_view_context"; //$NON-NLS-1$
	public static final String COMPARE_REVISIONS_VIEW = PREFIX + "compare_revision_view_context"; //$NON-NLS-1$
	public static final String SVN_EDITORS_VIEW = PREFIX + "svn_editors_view_context"; //$NON-NLS-1$

	// Viewers
	public static final String CATCHUP_RELEASE_VIEWER = PREFIX + "catchup_release_viewer_context"; //$NON-NLS-1$

	// Add to .svnignore dialog
	public static final String ADD_TO_SVNIGNORE = PREFIX + "add_to_svnignore_context"; //$NON-NLS-1$

	// Actions
	public static final String GET_FILE_REVISION_ACTION = PREFIX + "get_file_revision_action_context"; //$NON-NLS-1$
	public static final String GET_FILE_CONTENTS_ACTION = PREFIX + "get_file_contents_action_context"; //$NON-NLS-1$
	public static final String TAG_WITH_EXISTING_ACTION = PREFIX + "tag_with_existing_action_context"; //$NON-NLS-1$
	public static final String NEW_REPOSITORY_LOCATION_ACTION = PREFIX + "new_repository_location_action_context"; //$NON-NLS-1$
	public static final String NEW_DEV_ECLIPSE_REPOSITORY_LOCATION_ACTION = PREFIX + "new_dev_eclipse repository_location_action_context"; //$NON-NLS-1$
	public static final String SHOW_COMMENT_IN_HISTORY_ACTION = PREFIX + "show_comment_in_history_action_context"; //$NON-NLS-1$
	public static final String SHOW_TAGS_IN_HISTORY_ACTION = PREFIX + "show_tag_in_history_action_context"; //$NON-NLS-1$
	public static final String SELECT_WORKING_SET_ACTION = PREFIX + "select_working_set_action_context"; //$NON-NLS-1$
	public static final String DESELECT_WORKING_SET_ACTION = PREFIX + "deselect_working_set_action_context"; //$NON-NLS-1$
	public static final String EDIT_WORKING_SET_ACTION = PREFIX + "edit_working_set_action_context"; //$NON-NLS-1$
	public static final String REMOVE_REPOSITORY_LOCATION_ACTION = PREFIX + "remove_root_action_context"; //$NON-NLS-1$
	public static final String SHOW_IN_RESOURCE_HISTORY = PREFIX + "show_in_history_action_context"; //$NON-NLS-1$
	public static final String SELECT_NEW_RESOURCES_ACTION = PREFIX + "select_new_action_context"; //$NON-NLS-1$
	public static final String CONFIRM_MERGE_ACTION = PREFIX + "confirm_merge_action_context"; //$NON-NLS-1$;
	public static final String DISCONNECT_ACTION = PREFIX + "disconnect_action_context"; //$NON-NLS-1$;
	
	// Sync view menu actions
	public static final String SYNC_COMMIT_ACTION = PREFIX + "sync_commit_action_context"; //$NON-NLS-1$
	public static final String SYNC_FORCED_COMMIT_ACTION = PREFIX + "sync_forced_commit_action_context"; //$NON-NLS-1$
	public static final String SYNC_UPDATE_ACTION = PREFIX + "sync_update_action_context"; //$NON-NLS-1$
	public static final String SYNC_FORCED_UPDATE_ACTION = PREFIX + "sync_forced_update_action_context"; //$NON-NLS-1$
	public static final String SYNC_ADD_ACTION = PREFIX + "sync_add_action_context"; //$NON-NLS-1$
	public static final String SYNC_IGNORE_ACTION = PREFIX + "sync_ignore_action_context"; //$NON-NLS-1$
	public static final String MERGE_UPDATE_ACTION = PREFIX + "merge_update_action_context"; //$NON-NLS-1$
	public static final String MERGE_FORCED_UPDATE_ACTION = PREFIX + "merge_forced_update_action_context"; //$NON-NLS-1$
	public static final String MERGE_UPDATE_WITH_JOIN_ACTION = PREFIX + "merge_update_with_joinaction_context"; //$NON-NLS-1$
	
	// properties pages
	public static final String REPOSITORY_LOCATION_PROPERTY_PAGE = PREFIX + "repository_location_property_page_context"; //$NON-NLS-1$
	public static final String PROJECT_PROPERTY_PAGE = PREFIX + "project_property_page_context"; //$NON-NLS-1$
	public static final String FOLDER_PROPERTY_PAGE = PREFIX + "folder_property_page_context"; //$NON-NLS-1$
	public static final String FILE_PROPERTY_PAGE = PREFIX + "file_property_page_context"; //$NON-NLS-1$
}
