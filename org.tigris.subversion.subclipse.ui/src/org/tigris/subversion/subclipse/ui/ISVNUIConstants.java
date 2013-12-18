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

public interface ISVNUIConstants {
  // image path
  public final String ICON_PATH = "icons/full/"; //$NON-NLS-1$
  
  public final String IMG_SYNCPANE = "elcl16/syncpane_co.gif"; //$NON-NLS-1$

  // overlays
  public final String IMG_QUESTIONABLE = "ovr16/question_ov.gif"; //$NON-NLS-1$
  public final String IMG_CONFLICTED = "ovr16/conflicted_ov.gif"; //$NON-NLS-1$
  public final String IMG_PROPERTY_CONFLICTED = "ovr16/prop_conflicted_ov.gif"; //$NON-NLS-1$
  public final String IMG_ADDED = "ovr16/added_ov.gif"; //$NON-NLS-1$
  public final String IMG_MOVED = "ovr16/moved_ov.gif"; //$NON-NLS-1$
  public final String IMG_EXTERNAL = "ovr16/external_ov.gif"; //$NON-NLS-1$
  public final String IMG_LOCKED = "ovr16/locked_ov.gif"; //$NON-NLS-1$
  public final String IMG_NEEDSLOCK = "ovr16/protected_ov.gif"; //$NON-NLS-1$
  public final String IMG_DELETED = "ovr16/deleted_ov.gif"; //$NON-NLS-1$
  public final String IMG_SWITCHED = "ovr16/switched_ov.gif"; //$NON-NLS-1$
  public final String IMG_PROPERTY_CHANGED = "ovr16/propchg_ov.gif"; //$NON-NLS-1$
  public final String IMG_TEXT_CONFLICTED = "ovr16/text_conflicted_ov.gif"; //$NON-NLS-1$
  public final String IMG_TREE_CONFLICT = "ovr16/tree_conflict_ov.gif"; //$NON-NLS-1$

  // objects
  public final String IMG_REPOSITORY = "obj16/repository_rep.gif"; //$NON-NLS-1$
  public final String IMG_BRANCHES_CATEGORY = "obj16/branches_rep.gif"; //$NON-NLS-1$
  public final String IMG_VERSIONS_CATEGORY = "obj16/versions_rep.gif"; //$NON-NLS-1$
  public final String IMG_PROJECT_VERSION = "obj16/prjversions_rep.gif"; //$NON-NLS-1$
  public final String IMG_WARNING = "obj16/warn.gif"; //$NON-NLS-1$
  public final String IMG_BRANCH = "obj16/tag.gif"; //$NON-NLS-1$
  public final String IMG_PROPERTIES = "clcl16/properties.png"; //$NON-NLS-1$
  public final String IMG_URL_SOURCE_REPO = "obj16/url_source_repo.gif"; //$NON-NLS-1$

  // toolbar
  public final String IMG_REFRESH = "clcl16/refresh.gif"; //$NON-NLS-1$
  public final String IMG_CLEAR = "clcl16/clear_co.gif"; //$NON-NLS-1$
  public final String IMG_COLLAPSE_ALL = "clcl16/collapseall.gif"; //$NON-NLS-1$
  public final String IMG_EXPAND_ALL = "clcl16/expandall.gif"; //$NON-NLS-1$
  public final String IMG_GET_ALL = "clcl16/get_all.gif"; //$NON-NLS-1$
  public final String IMG_GET_NEXT = "clcl16/get_next.gif"; //$NON-NLS-1$
  public final String IMG_FILTER_HISTORY = "clcl16/filter_history.gif"; //$NON-NLS-1$
  public final String IMG_SHOW_DELETED = "tortoise/delete.gif"; //$NON-NLS-1$

  // toolbar (disabled)
  public final String IMG_REFRESH_DISABLED = "dlcl16/refresh.gif"; //$NON-NLS-1$
  public final String IMG_CLEAR_DISABLED = "dlcl16/clear_co.gif"; //$NON-NLS-1$
  public final String IMG_FILTER_HISTORY_DISABLED = "dlcl16/filter_history.gif"; //$NON-NLS-1$

  // toolbar (enabled)
  public final String IMG_REFRESH_ENABLED = "elcl16/refresh.gif"; //$NON-NLS-1$
  public final String IMG_COLLAPSE_ALL_ENABLED = "elcl16/collapseall.gif"; //$NON-NLS-1$
  public final String IMG_EXPAND_ALL_ENABLED = "elcl16/expandall.gif"; //$NON-NLS-1$

  public final String IMG_AFFECTED_PATHS_TABLE_MODE = "elcl16/tableLayout.gif"; //$NON-NLS-1$
  public final String IMG_AFFECTED_PATHS_TREE_MODE = "elcl16/treeLayout.gif"; //$NON-NLS-1$
  public final String IMG_AFFECTED_PATHS_FLAT_MODE = "elcl16/flatLayout.gif"; //$NON-NLS-1$
  public final String IMG_AFFECTED_PATHS_COMPRESSED_MODE = "elcl16/compressedLayout.gif"; //$NON-NLS-1$

  public final String IMG_AFFECTED_PATHS_HORIZONTAL_LAYOUT = "elcl16/horizontal.gif"; //$NON-NLS-1$
  public final String IMG_AFFECTED_PATHS_VERTICAL_LAYOUT = "elcl16/vertical.gif"; //$NON-NLS-1$

  public final String IMG_COMMENTS = "elcl16/comments.gif"; //$NON-NLS-1$
  
  public final String IMG_UPDATE_ALL = "elcl16/update_all.gif"; //$NON-NLS-1$
  public final String IMG_COMMIT_ALL = "elcl16/commit_all.gif"; //$NON-NLS-1$

  // wizards
  public final String IMG_NEWLOCATION = "wizards/newlocation_wiz.gif"; //$NON-NLS-1$
  public final String IMG_CLOUDFORGE = "wizards/cloudforge.png"; //$NON-NLS-1$

  // pending
  public final String IMG_FILEADD_PENDING = "pending16/fileadd_pending.gif"; //$NON-NLS-1$
  public final String IMG_FILEDELETE_PENDING = "pending16/filedelete_pending.gif"; //$NON-NLS-1$
  public final String IMG_FOLDERADD_PENDING = "pending16/folderadd_pending.gif"; //$NON-NLS-1$
  public final String IMG_FOLDERDELETE_PENDING = "pending16/folderdelete_pending.gif"; //$NON-NLS-1$
  public final String IMG_FILEMODIFIED_PENDING = "pending16/filemodified_pending.gif"; //$NON-NLS-1$
  public final String IMG_FOLDERMODIFIED_PENDING = "pending16/foldermodified_pending.gif"; //$NON-NLS-1$
  public final String IMG_FOLDER = "pending16/folder_pending.gif"; //$NON-NLS-1$

  // views
  public final String IMG_SVN_CONSOLE = "cview16/console_view.gif"; //$NON-NLS-1$

  // operations
  public final String IMG_CHECKOUT = "ctool16/checkout.gif"; //$NON-NLS-1$

  // Menus
  public final String IMG_MENU_UPDATE = "Menu Update"; //$NON-NLS-1$
  public final String IMG_MENU_COMMIT = "Menu Commit"; //$NON-NLS-1$
  public final String IMG_MENU_SYNC = "Menu Synchronize"; //$NON-NLS-1$
  public final String IMG_MENU_REVERT = "Menu Revert"; //$NON-NLS-1$
  public final String IMG_MENU_ADD = "Menu Add"; //$NON-NLS-1$
  public final String IMG_MENU_IGNORE = "Menu Ignore"; //$NON-NLS-1$
  public final String IMG_MENU_PROPSET = "Menu Set Property"; //$NON-NLS-1$
  public final String IMG_MENU_SHOWPROPERTY = "Menu Show Property"; //$NON-NLS-1$
  public final String IMG_MENU_RELOCATE = "Menu Relocate"; //$NON-NLS-1$
  public final String IMG_MENU_CHECKOUTAS = "Menu Checkout As"; //$NON-NLS-1$
  public final String IMG_MENU_IMPORTFOLDER = "Menu Import Folder"; //$NON-NLS-1$
  public final String IMG_MENU_LOCK = "Menu Lock"; //$NON-NLS-1$
  public final String IMG_MENU_UNLOCK = "Menu Unlock"; //$NON-NLS-1$
  public final String IMG_MENU_CLEANUP = "Menu Cleanup"; //$NON-NLS-1$
  public final String IMG_MENU_EXPORT = "Menu Export"; //$NON-NLS-1$
  public final String IMG_MENU_DIFF = "Menu Diff"; //$NON-NLS-1$
  public final String IMG_MENU_PROPDELETE = "Menu Property Delete"; //$NON-NLS-1$
  public final String IMG_MENU_DELETE = "Menu Delete"; //$NON-NLS-1$
  public final String IMG_MENU_BRANCHTAG = "Menu Branch/Tag"; //$NON-NLS-1$
  public final String IMG_MENU_MOVE = "Menu Move"; //$NON-NLS-1$
  public final String IMG_MENU_COPY = "Menu Copy"; //$NON-NLS-1$
  public final String IMG_MENU_COMPARE = "Menu Compare"; //$NON-NLS-1$
  public final String IMG_MENU_RESOLVE = "Menu Resolve"; //$NON-NLS-1$
  public final String IMG_MENU_EDITCONFLICT = "Menu Edit Conflicts"; //$NON-NLS-1$
  public final String IMG_MENU_SWITCH = "Menu Switch"; //$NON-NLS-1$
  public final String IMG_MENU_MARKMERGED = "Menu Mark Merged"; //$NON-NLS-1$
  public final String IMG_MENU_MERGE = "Menu Merge"; //$NON-NLS-1$
  public final String IMG_MENU_SHOWHISTORY = "Menu Show History"; //$NON-NLS-1$
  public final String IMG_MENU_ANNOTATE = "Menu Annotate"; //$NON-NLS-1$

  // preferences
  public final String PREF_SHOW_COMMENTS = "pref_show_comments"; //$NON-NLS-1$
  public final String PREF_WRAP_COMMENTS = "pref_wrap_comments"; //$NON-NLS-1$
  public final String PREF_SHOW_PATHS = "pref_show_paths"; //$NON-NLS-1$
  public final String PREF_AFFECTED_PATHS_MODE = "pref_affected_paths_layout"; //$NON-NLS-1$
  public final String PREF_AFFECTED_PATHS_LAYOUT = "pref_affected_paths_layout2"; //$NON-NLS-1$
  public final String PREF_HISTORY_VIEW_EDITOR_LINKING = "pref_history_view_linking"; //$NON-NLS-1$
  public final String PREF_PRUNE_EMPTY_DIRECTORIES = "pref_prune_empty_directories"; //$NON-NLS-1$
  public final String PREF_TIMEOUT = "pref_timeout"; //$NON-NLS-1$
  public final String PREF_QUIETNESS = "pref_quietness"; //$NON-NLS-1$
  public final String PREF_SVN_SERVER = "pref_svn_server"; //$NON-NLS-1$
  public final String PREF_CONSIDER_CONTENTS = "pref_consider_contents"; //$NON-NLS-1$
  public final String PREF_SHOW_MARKERS = "pref_show_markers"; //$NON-NLS-1$
  public final String PREF_REPLACE_UNMANAGED = "pref_replace_unmanaged"; //$NON-NLS-1$
  public final String PREF_COMPRESSION_LEVEL = "pref_compression_level"; //$NON-NLS-1$
  public final String PREF_TEXT_KSUBST = "pref_text_ksubst"; //$NON-NLS-1$
  public final String PREF_FETCH_CHANGE_PATH_ON_DEMAND = "pref_fetch_change_path_on_Demand"; //$NON-NLS-1$
  public final String PREF_SHOW_TAGS_IN_REMOTE = "pref_show_tags_in_remote"; //$NON-NLS-1$
  public final String PREF_LOG_ENTRIES_TO_FETCH = "pref_log_entries_to_fetch"; //$NON-NLS-1$
  public final String PREF_STOP_ON_COPY = "pref_stop_on_copy"; //$NON-NLS-1$
  public final String PREF_INCLUDE_MERGED_REVISIONS = "include_merged_revisions"; //$NON-NLS-1$
  public final String PREF_USE_JAVAHL_COMMIT_HACK = "pref_use_javahl_commit_hack"; //$NON-NLS-1$
  public final String PREF_MERGE_PROVIDER = "pref_merge_provider"; //$NON-NLS-1$
  public final String PREF_SUGGEST_MERGE_SOURCES = "pref_suggest_merge_sources"; //$NON-NLS-1$
  public final String PREF_COMMENTS_TO_SAVE = "pref_comments_to_save"; //$NON-NLS-1$

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
  public final String PREF_SHOW_UNADDED_RESOURCES_ON_COMMIT = "pref_show_unadded_resources_on_commit"; //$NON-NLS-1$
  public final String PREF_SELECT_UNADDED_RESOURCES_ON_COMMIT = "pref_select_unadded_resources_on_commit"; //$NON-NLS-1$
  public final String PREF_REMOVE_UNADDED_RESOURCES_ON_REPLACE = "pref_remove_unadded_resources_on_replace";
  public final String PREF_COMMIT_SET_DEFAULT_ENABLEMENT = "pref_enable_commit_sets"; //$NON-NLS-1$
  public final String PREF_USE_QUICKDIFFANNOTATE = "pref_use_quickdiffannotate"; //$NON-NLS-1$

  // console preferences
  public final String PREF_CONSOLE_COMMAND_COLOR = "pref_console_command_color"; //$NON-NLS-1$
  public final String PREF_CONSOLE_MESSAGE_COLOR = "pref_console_message_color"; //$NON-NLS-1$
  public final String PREF_CONSOLE_ERROR_COLOR = "pref_console_error_color"; //$NON-NLS-1$
  public final String PREF_CONSOLE_FONT = "pref_console_font"; //$NON-NLS-1$
  public final String PREF_CONSOLE_SHOW_ON_ERROR = "pref_console_show_on_error"; //$NON-NLS-1$
  public final String PREF_CONSOLE_SHOW_ON_MESSAGE = "pref_console_show_on_message"; //$NON-NLS-1$
  public final String PREF_CONSOLE_LIMIT_OUTPUT = "pref_console_limit_output"; //$NON-NLS-1$
  public final String PREF_CONSOLE_HIGH_WATER_MARK = "pref_console_high_water_mark"; //$NON-NLS-1$

  // decorator preferences
  public final String PREF_FILETEXT_DECORATION = "pref_filetext_decoration"; //$NON-NLS-1$
  public final String PREF_FOLDERTEXT_DECORATION = "pref_foldertext_decoration"; //$NON-NLS-1$
  public final String PREF_PROJECTTEXT_DECORATION = "pref_projecttext_decoration"; //$NON-NLS-1$
  
  public final String PREF_DATEFORMAT_DECORATION = "pref_dateformat_decoration"; //$NON-NLS-1$

  public final String PREF_SHOW_DIRTY_DECORATION = "pref_show_overlaydirty"; //$NON-NLS-1$
  public final String PREF_SHOW_ADDED_DECORATION = "pref_show_added"; //$NON-NLS-1$
  public final String PREF_SHOW_EXTERNAL_DECORATION = "pref_show_external"; //$NON-NLS-1$
  public final String PREF_SHOW_HASREMOTE_DECORATION = "pref_show_hasremote"; //$NON-NLS-1$
  public final String PREF_SHOW_NEWRESOURCE_DECORATION = "pref_show_newresource"; //$NON-NLS-1$

  public final String PREF_DIRTY_FLAG = "pref_dirty_flag"; //$NON-NLS-1$
  public final String PREF_ADDED_FLAG = "pref_added_flag"; //$NON-NLS-1$
  public final String PREF_EXTERNAL_FLAG = "pref_external_flag"; //$NON-NLS-1$

  public final String PREF_CALCULATE_DIRTY = "pref_calculate_dirty"; //$NON-NLS-1$
  public final String PREF_USE_FONT_DECORATORS = "pref_use_font_decorators"; //$NON-NLS-1$

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

  // Menu icon preferences
  public final String PREF_MENU_ICON_SET = "pref_menu_icon_set"; //$NON-NLS-1$
  public final int MENU_ICON_SET_DEFAULT = 1;
  public final int MENU_ICON_SET_TORTOISESVN = 2;
  public final int MENU_ICON_SET_SUBVERSIVE = 3;

  // svn commit comment font
  public final String SVN_COMMENT_FONT = "svn_comment_font"; //$NON-NLS-1$

  // Wizard banners
  public final String IMG_WIZBAN_SVN = "wizban/svn_wizban.png"; //$NON-NLS-1$
  public final String IMG_WIZBAN_SHARE = "wizban/newconnect_wizban.gif"; //$NON-NLS-1$
  public final String IMG_WIZBAN_DIFF = "wizban/createpatch_wizban.gif"; //$NON-NLS-1$
  public final String IMG_WIZBAN_NEW_LOCATION = "wizban/newlocation_wizban.gif"; //$NON-NLS-1$
  public final String IMG_WIZBAN_NEW_FOLDER = "wizban/newfolder_wizban.gif"; //$NON-NLS-1$
  public final String IMG_WIZBAN_SYNCH = "wizban/share_wizban.gif"; //$NON-NLS-1$
  public final String IMG_WIZBAN_RESOLVE_TREE_CONFLICT = "wizban/resolve_treeconflict_wizban.png"; //$NON-NLS-1$

  // Properties
  public final String PROP_NAME = "svn.name"; //$NON-NLS-1$
  public final String PROP_REVISION = "svn.revision"; //$NON-NLS-1$
  public final String PROP_AUTHOR = "svn.author"; //$NON-NLS-1$
  public final String PROP_COMMENT = "svn.comment"; //$NON-NLS-1$
  public final String PROP_DATE = "svn.date"; //$NON-NLS-1$
  public final String PROP_DIRTY = "svn.dirty"; //$NON-NLS-1$
  
  public final String PROP_LOCK_OWNER = "svn.lock.owner"; //$NON-NLS-1$
  public final String PROP_LOCK_TOKEN = "svn.lock.token"; //$NON-NLS-1$
  public final String PROP_LOCK_COMMENT = "svn.lock.comment"; //$NON-NLS-1$
  public final String PROP_LOCK_CREATION_DATE = "svn.lock.creation.date"; //$NON-NLS-1$
  public final String PROP_LOCK_EXPIRATION_DATE = "svn.lock.expiration.date"; //$NON-NLS-1$

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
  public final int OPTION_AUTOMATIC = 3; //$NON-NLS-1$

  public final int MODE_FLAT = 1; // actually table mode //$NON-NLS-1$
  public final int MODE_COMPRESSED = 2; //$NON-NLS-1$
  public final int MODE_FLAT2 = 3; //$NON-NLS-1$

  public final int LAYOUT_HORIZONTAL = 1; //$NON-NLS-1$
  public final int LAYOUT_VERTICAL = 2; //$NON-NLS-1$

  public final String PREF_ALLOW_EMPTY_COMMIT_COMMENTS = "pref_allow_empty_commit_comment"; //$NON-NLS-1$
  public final String PREF_ALLOW_COMMIT_WITH_WARNINGS = "pref_commit_with_warning"; //$NON-NLS-1$
  public final String PREF_ALLOW_COMMIT_WITH_ERRORS = "pref_commit_with_errors"; //$NON-NLS-1$
  public final String PREF_COMMIT_TO_TAGS_PATH_WITHOUT_WARNING = "pref_commit_to_tags_path_without_warning"; //$NON-NLS-1$
  
  public final String PREF_UPDATE_TO_HEAD_IGNORE_EXTERNALS = "pref_update_to_head_ignore_externals"; //$NON-NLS-1$
  public final String PREF_UPDATE_TO_HEAD_ALLOW_UNVERSIONED_OBSTRUCTIONS = "pref_update_to_head_allow_unversioned_obstructions"; //$NON-NLS-1$
  public final String PREF_UPDATE_TO_HEAD_CONFLICT_HANDLING_TEXT_FILES = "pref_update_to_head_conflict_handling_text_files"; //$NON-NLS-1$
  public final String PREF_UPDATE_TO_HEAD_CONFLICT_HANDLING_BINARY_FILES = "pref_update_to_head_conflict_handling_binary_files"; //$NON-NLS-1$
  public final String PREF_UPDATE_TO_HEAD_CONFLICT_HANDLING_PROPERTIES = "pref_update_to_head_conflict_handling_properties"; //$NON-NLS-1$
  public final String PREF_UPDATE_TO_HEAD_CONFLICT_HANDLING_TREE_CONFLICTS = "pref_update_to_head_conflict_handling_tree_conflicts"; //$NON-NLS-1$
  
  // depth
  public final String DEPTH_EMPTY = "Only this item"; //$NON-NLS-1$
  public final String DEPTH_FILES = "Only file children"; //$NON-NLS-1$
  public final String DEPTH_IMMEDIATES = "Immediate children, including folders"; //$NON-NLS-1$
  public final String DEPTH_INFINITY = "Fully recursive"; //$NON-NLS-1$
  public final String DEPTH_UNKNOWN = "Working copy"; //$NON-NLS-1$
  public final String DEPTH_EXCLUDE = "Exclude"; //$NON-NLS-1$
  
  // This is in Internal class in team.ui, so we are mirroring it
  public final static String HISTORY_VIEW_ID = "org.eclipse.team.ui.GenericHistoryView"; //$NON-NLS-1$
}
