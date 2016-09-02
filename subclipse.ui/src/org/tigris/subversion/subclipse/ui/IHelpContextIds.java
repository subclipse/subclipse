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
package org.tigris.subversion.subclipse.ui;

/**
 * Here's how to reference the help context in code (typically in "createComposite()"):
 * 
 * WorkbenchHelp.setHelp(actionOrControl, IHelpContextIds.NAME_DEFIED_BELOW);
 * 
 * For preference pages (and possibly also for resource property pages, 'actionOrControl' is
 * the result of getControl())
 * 
 * Every time you add a new key, keep the keys in the 'help_contexts.xml' file current.
 */
public interface IHelpContextIds {

	public static final String PREFIX = SVNUIPlugin.ID + "."; //$NON-NLS-1$

	// Dialogs
	public static final String RELEASE_COMMENT_DIALOG = PREFIX + "release_comment_dialog_context"; //$NON-NLS-1$
	public static final String ADD_TO_VERSION_CONTROL_DIALOG = PREFIX + "add_to_version_control_dialog_context"; //$NON-NLS-1$
	public static final String ADD_KEYWORDS_DIALOG = PREFIX + "add_keywords_dialog_context"; //$NON-NLS-1$
	public static final String ADD_TO_SVNIGNORE = PREFIX + "add_to_svnignore_dialog_context"; //$NON-NLS-1$
	public static final String SWITCH_DIALOG = PREFIX + "switch_dialog_context"; //$NON-NLS-1$
	public static final String MERGE_DIALOG = PREFIX + "merge_dialog_context"; //$NON-NLS-1$
	public static final String BRANCH_TAG_DIALOG = PREFIX + "branch_tag_dialog_context"; //$NON-NLS-1$
	public static final String REVERT_DIALOG = PREFIX + "revert_dialog_context"; //$NON-NLS-1$
	public static final String COMMIT_DIALOG = PREFIX + "commit_dialog_context"; //$NON-NLS-1$
	public static final String COMPARE_DIALOG = PREFIX + "compare_dialog_context"; //$NON-NLS-1$
	
	public static final String IMPORT_FOLDER_DIALOG = PREFIX + "import_folder_dialog_context"; //$NON-NLS-1$;
	public static final String ANNOTATE_DIALOG = PREFIX + "annotate_dialog_context"; //$NON-NLS-1$;
	public static final String SET_SVN_PROPERTY_DIALOG = PREFIX + "set_svn_property_dialog_context"; //$NON_NLS-1$

	public static final String PASSWORD_PROMPT_DIALOG = PREFIX + "password_prompt_dialog_context"; //$NON_NLS-1$
	public static final String USER_PROMPT_DIALOG = PREFIX + "user_prompt_dialog_context"; //$NON_NLS-1$
	public static final String QUESTION_DIALOG = PREFIX + "question_dialog_context"; //$NON_NLS-1$
	public static final String SSH_PROMPT_DIALOG = PREFIX + "ssh_prompt_dialog_context"; //$NON_NLS-1$
	public static final String TRUST_SSL_SERVER_DIALOG = PREFIX + "trust_ssl_server_dialog_context"; //$NON_NLS-1$
	public static final String CHOOSE_URL_DIALOG = PREFIX + "choose_url_dialog_context"; //$NON_NLS-1$
	public static final String LOCK_DIALOG = PREFIX + "lock_dialog_context"; //$NON_NLS-1$
	public static final String HISTORY_DIALOG = PREFIX + "history_dialog_context"; //$NON_NLS-1$

	public static final String SHOW_UNIFIED_DIFF_DIALOG = PREFIX + "show_unified_diff_dialog_context"; //$NON_NLS-1$

	public static final String EXPORT_REMOTE_FOLDER_DIALOG = PREFIX + "export_remote_folder_dialog_context"; //$NON_NLS-1$
	public static final String REMOTE_RESOURCE_PROPERTIES_DIALOG = PREFIX + "remote_resource_properties_dialog_context"; //$NON_NLS-1$
	
	public static final String BRANCH_TAG_PROPERTY_UPDATE_DIALOG = PREFIX + "branch_tag_property_update_dialog_context"; //$NON_NLS-1$
	public static final String CONFIGURE_TAGS_DIALOG = PREFIX + "configure_tags_dialog_context"; //$NON_NLS-1$

	public static final String CHANGE_REVPROPS = PREFIX + "change_revprops_context"; //$NON_NLS-1$

	public static final String COMMIT_SET_DIALOG = PREFIX + "commit_set_dialog_context"; //$NON-NLS-1$

	public static final String SEARCH_HISTORY_DIALOG = PREFIX + "search_history_dialog_context"; //$NON-NLS-1$

	// Wizard Pages
	public static final String SHARING_AUTOCONNECT_PAGE = PREFIX + "sharing_autoconnect_page_context"; //$NON-NLS-1$
	public static final String SHARING_SELECT_REPOSITORY_PAGE = PREFIX + "sharing_select_repository_page_context"; //$NON-NLS-1$
	public static final String SHARING_NEW_REPOSITORY_PAGE = PREFIX + "sharing_new_repository_page_context"; //$NON-NLS-1$
	public static final String SHARING_MODULE_PAGE = PREFIX + "sharing_module_page_context"; //$NON-NLS-1$
	public static final String SHARING_FINISH_PAGE = PREFIX + "sharing_finish_page_context"; //$NON-NLS-1$
	public static final String PATCH_SELECTION_PAGE = PREFIX + "patch_selection_page_context"; //$NON-NLS-1$
	public static final String PATCH_OPTIONS_PAGE = PREFIX + "patch_options_page_context"; //$NON-NLS-1$
	public static final String CREATE_REMOTE_FOLDER_PAGE = PREFIX + "create_remote_folder_page_context"; //$NON_NLS-1$
	public static final String MOVE_RENAME_REMOTE_RESOURCE_PAGE = PREFIX + "move_rename_remote_resource_page_context"; //$NON_NLS-1$
	public static final String RELOCATE_REPOSITORY_PAGE = PREFIX + "relocate_page_context"; //$NON_NLS-1$
	public static final String COMMENT_COMMIT_PAGE_DIALOG = PREFIX + "comment_commit_page_context"; //$NON_NLS-1$

	// Properties and Preferences
	public static final String CONSOLE_PREFERENCE_PAGE = PREFIX + "console_preference_page_context"; //$NON-NLS-1$
	public static final String DECORATORS_PREFERENCE_PAGE = PREFIX + "decorators_preference_page_context"; //$NON-NLS-1$
	public static final String SVN_PREFERENCE_DIALOG = PREFIX + "svn_preference_page_context"; //$NON-NLS-1$
	public static final String SVN_RESOURCE_PROPERTIES_PAGE = PREFIX + "svn_resource_properties_page_context"; //$NON-NLS-1$
	public static final String DIFF_MERGE_PREFERENCE_PAGE  = PREFIX + "diff_merge_preferences_page_context"; //$NON-NLS-1$
	public static final String COMMENT_TEMPLATE_PREFERENCE_PAGE = PREFIX + "comment_template_preference_page_context"; //$NON-NLS-1$
	
	// Views
	public static final String CONSOLE_VIEW = PREFIX + "console_view_context"; //$NON-NLS-1$
	public static final String REPOSITORIES_VIEW = PREFIX + "repositories_view_context"; //$NON-NLS-1$
	public static final String RESOURCE_HISTORY_VIEW = PREFIX + "resource_history_view_context"; //$NON-NLS-1$
	public static final String COMPARE_REVISIONS_VIEW = PREFIX + "compare_revision_view_context"; //$NON-NLS-1$
	public static final String ANNOTATIONS_VIEW = PREFIX + "annotations_view_context"; //$NON-NLS-1$
	public static final String PROPERTIES_VIEW = PREFIX + "properties_view_context"; //$NON-NLS-1$
    public static final String REV_PROPERTIES_VIEW = PREFIX + "rev_properties_view_context"; //$NON-NLS-1$	
	public static final String PENDING_OPERATIONS_VIEW = PREFIX + "pending_operations_view_context"; //$NON-NLS-1$

	// Actions
	public static final String GET_FILE_REVISION_ACTION = PREFIX + "get_file_revision_action_context"; //$NON-NLS-1$
	public static final String GET_FILE_CONTENTS_ACTION = PREFIX + "get_file_contents_action_context"; //$NON-NLS-1$
	public static final String NEW_REPOSITORY_LOCATION_ACTION = PREFIX + "new_repository_location_action_context"; //$NON-NLS-1$
	public static final String REMOVE_REPOSITORY_LOCATION_ACTION = PREFIX + "remove_root_action_context"; //$NON-NLS-1$
	public static final String DISCONNECT_ACTION = PREFIX + "disconnect_action_context"; //$NON-NLS-1$;
}
