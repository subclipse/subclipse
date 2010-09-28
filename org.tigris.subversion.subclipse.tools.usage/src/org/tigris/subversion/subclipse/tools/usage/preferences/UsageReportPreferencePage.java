package org.tigris.subversion.subclipse.tools.usage.preferences;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.osgi.service.prefs.BackingStoreException;
import org.tigris.subversion.subclipse.tools.usage.internal.SubclipseToolsUsageActivator;
import org.tigris.subversion.subclipse.tools.usage.util.StatusUtils;

public class UsageReportPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public UsageReportPreferencePage() {
		super(GRID);
	}

	public void createFieldEditors() {
		addField(new BooleanFieldEditor(
				IUsageReportPreferenceConstants.USAGEREPORT_ENABLED_ID
				, getCheckBoxlabel()
				, getFieldEditorParent()));
	}

	private String getCheckBoxlabel() {
		return PreferencesMessages.UsageReportPreferencePage_AllowReporting;
	}
	

	public void init(IWorkbench workbench) {
		setPreferenceStore(UsageReportPreferences.createPreferenceStore());
	}

	public boolean performOk() {
		try {
			UsageReportPreferences.flush();
		} catch (BackingStoreException e) {
			IStatus status = StatusUtils.getErrorStatus(SubclipseToolsUsageActivator.PLUGIN_ID,
					getPrefsSaveErrorMessage() , e, null);
			SubclipseToolsUsageActivator.getDefault().getLog().log(status);
		}
		return super.performOk();
	}

	private String getPrefsSaveErrorMessage() {
		return PreferencesMessages.UsageReportPreferencePage_Error_Saving;
	}	
	
}