package org.tigris.subversion.subclipse.ui;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.swt.graphics.RGB;
import org.tigris.subversion.subclipse.ui.decorator.SVNDecoratorConfiguration;
import org.tigris.subversion.svnclientadapter.ISVNConflictResolver;

/**
 * This class initializes the default values for various SVN preferences
 */
public class SVNUIPreferenceInitializer extends AbstractPreferenceInitializer {

	/**
	 * This method is called by the preference initializer to initialize default
	 * preference values. Note that this method shouldn't be called by the
	 * clients; it will be called automatically by the preference
	 * initializer when the appropriate default preference node is accessed.
	 */
	 public void initializeDefaultPreferences() {
		IEclipsePreferences node = new DefaultScope().getNode(SVNUIPlugin.ID);
		node.put(ISVNUIConstants.PREF_CONSOLE_COMMAND_COLOR, 
				StringConverter.asString(new RGB(0, 0, 0)));
		node.put(ISVNUIConstants.PREF_CONSOLE_MESSAGE_COLOR, 
				StringConverter.asString(new RGB(0, 0, 255)));
        node.put(ISVNUIConstants.PREF_CONSOLE_ERROR_COLOR, 
        		StringConverter.asString(new RGB(255, 0, 0)));
        
        node.putBoolean(ISVNUIConstants.PREF_SHOW_COMMENTS, true);
        node.putBoolean(ISVNUIConstants.PREF_WRAP_COMMENTS, true);
        node.putBoolean(ISVNUIConstants.PREF_SHOW_PATHS, true);
        node.putInt(ISVNUIConstants.PREF_AFFECTED_PATHS_MODE, ISVNUIConstants.MODE_FLAT);
        node.putInt(ISVNUIConstants.PREF_AFFECTED_PATHS_LAYOUT, ISVNUIConstants.LAYOUT_HORIZONTAL);
        
        node.putBoolean(ISVNUIConstants.PREF_CONSOLE_SHOW_ON_MESSAGE, false);
        node.putBoolean(ISVNUIConstants.PREF_CONSOLE_SHOW_ON_ERROR, true);
		node.putBoolean(ISVNUIConstants.PREF_CONSOLE_LIMIT_OUTPUT, true);	
		node.putInt(ISVNUIConstants.PREF_CONSOLE_HIGH_WATER_MARK, 500000);	
		
        node.put(ISVNUIConstants.PREF_FILETEXT_DECORATION, SVNDecoratorConfiguration.DEFAULT_FILETEXTFORMAT);
        node.put(ISVNUIConstants.PREF_FOLDERTEXT_DECORATION, SVNDecoratorConfiguration.DEFAULT_FOLDERTEXTFORMAT);
        node.put(ISVNUIConstants.PREF_PROJECTTEXT_DECORATION, SVNDecoratorConfiguration.DEFAULT_PROJECTTEXTFORMAT);
        
        node.put(ISVNUIConstants.PREF_ADDED_FLAG, SVNDecoratorConfiguration.DEFAULT_ADDED_FLAG);
        node.put(ISVNUIConstants.PREF_DIRTY_FLAG, SVNDecoratorConfiguration.DEFAULT_DIRTY_FLAG);
        node.put(ISVNUIConstants.PREF_EXTERNAL_FLAG, SVNDecoratorConfiguration.DEFAULT_EXTERNAL_FLAG);
        node.putBoolean(ISVNUIConstants.PREF_SHOW_EXTERNAL_DECORATION, true);
        node.putBoolean(ISVNUIConstants.PREF_SHOW_ADDED_DECORATION, true);
        node.putBoolean(ISVNUIConstants.PREF_SHOW_HASREMOTE_DECORATION, true);
        node.putBoolean(ISVNUIConstants.PREF_SHOW_DIRTY_DECORATION, true);
        node.putBoolean(ISVNUIConstants.PREF_SHOW_NEWRESOURCE_DECORATION, true);
        node.putBoolean(ISVNUIConstants.PREF_CALCULATE_DIRTY, true);
        node.putBoolean(ISVNUIConstants.PREF_USE_FONT_DECORATORS, false);
        node.putBoolean(ISVNUIConstants.PREF_SHOW_SYNCINFO_AS_TEXT, false);        
        node.putBoolean(ISVNUIConstants.PREF_PROMPT_ON_MIXED_TAGS, true);
        node.putBoolean(ISVNUIConstants.PREF_PROMPT_ON_SAVING_IN_SYNC, true);
        node.putInt(ISVNUIConstants.PREF_SAVE_DIRTY_EDITORS, ISVNUIConstants.OPTION_PROMPT);
        
        node.putBoolean(ISVNUIConstants.PREF_SHOW_COMPARE_REVISION_IN_DIALOG, false);
        node.putBoolean(ISVNUIConstants.PREF_SHOW_UNADDED_RESOURCES_ON_COMMIT, true);
        node.putBoolean(ISVNUIConstants.PREF_SELECT_UNADDED_RESOURCES_ON_COMMIT, true);
		node.putBoolean(ISVNUIConstants.PREF_REMOVE_UNADDED_RESOURCES_ON_REPLACE, true);
        node.putBoolean(ISVNUIConstants.PREF_COMMIT_SET_DEFAULT_ENABLEMENT, false);
        
        node.put(ISVNUIConstants.PREF_ALLOW_COMMIT_WITH_WARNINGS, MessageDialogWithToggle.ALWAYS);
        node.put(ISVNUIConstants.PREF_ALLOW_COMMIT_WITH_ERRORS, MessageDialogWithToggle.PROMPT);
        
        node.putBoolean(ISVNUIConstants.PREF_UPDATE_TO_HEAD_IGNORE_EXTERNALS, false);
        node.putBoolean(ISVNUIConstants.PREF_UPDATE_TO_HEAD_ALLOW_UNVERSIONED_OBSTRUCTIONS, true);
        node.putInt(ISVNUIConstants.PREF_UPDATE_TO_HEAD_CONFLICT_HANDLING_TEXT_FILES, ISVNConflictResolver.Choice.postpone);
        node.putInt(ISVNUIConstants.PREF_UPDATE_TO_HEAD_CONFLICT_HANDLING_BINARY_FILES, ISVNConflictResolver.Choice.postpone);
        node.putInt(ISVNUIConstants.PREF_UPDATE_TO_HEAD_CONFLICT_HANDLING_PROPERTIES, ISVNConflictResolver.Choice.postpone);
        node.putInt(ISVNUIConstants.PREF_UPDATE_TO_HEAD_CONFLICT_HANDLING_TREE_CONFLICTS, ISVNConflictResolver.Choice.postpone);
        
        node.putBoolean(ISVNUIConstants.PREF_USE_JAVAHL_COMMIT_HACK, true);
        
        node.put(ISVNUIConstants.PREF_SVNINTERFACE, "javahl"); //$NON-NLS-1$
        node.put(ISVNUIConstants.PREF_SVNCONFIGDIR, ""); //$NON-NLS-1$
        
        node.putBoolean(ISVNUIConstants.PREF_FETCH_CHANGE_PATH_ON_DEMAND, false);
        node.putInt(ISVNUIConstants.PREF_LOG_ENTRIES_TO_FETCH, 25);
        node.putBoolean(ISVNUIConstants.PREF_STOP_ON_COPY, false);

        node.putBoolean(ISVNUIConstants.PREF_MERGE_USE_EXTERNAL, false);
        node.putBoolean(ISVNUIConstants.PREF_SUGGEST_MERGE_SOURCES, true);
        node.put(ISVNUIConstants.PREF_MERGE_PROGRAM_LOCATION,""); //$NON-NLS-1$
        node.put(ISVNUIConstants.PREF_MERGE_PROGRAM_PARAMETERS,""); //$NON-NLS-1$
        
        node.put(ISVNUIConstants.PREF_USE_QUICKDIFFANNOTATE, MessageDialogWithToggle.PROMPT);

        node.putInt(ISVNUIConstants.PREF_MENU_ICON_SET, ISVNUIConstants.MENU_ICON_SET_DEFAULT);
        
        node.putInt(ISVNUIConstants.PREF_COMMENTS_TO_SAVE, 10);
	}
}
