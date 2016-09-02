package org.tigris.subversion.subclipse.ui.conflicts;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IWorkbenchPart;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.ui.Messages;
import org.tigris.subversion.subclipse.ui.operations.ResolveOperation;
import org.tigris.subversion.svnclientadapter.ISVNConflictResolver;
import org.tigris.subversion.svnclientadapter.ISVNProperty;

public class EditPropertyConflictsWizard extends Wizard {
	private ISVNLocalResource svnResource;
	private String conflictSummary;
	private PropertyConflict[] propertyConflicts;
	private ISVNProperty[] remoteProperties;
	private EditPropertyConflictsWizardSummaryPage summaryPage;
	private IWorkbenchPart targetPart;

	public EditPropertyConflictsWizard(ISVNLocalResource svnResource, String conflictSummary, PropertyConflict[] propertyConflicts, ISVNProperty[] remoteProperties, IWorkbenchPart targetPart) {
		super();
		this.svnResource = svnResource;
		this.conflictSummary = conflictSummary;
		this.propertyConflicts = propertyConflicts;
		this.remoteProperties = remoteProperties;
		this.targetPart = targetPart;
	}
	
	public void addPages() {
		super.addPages();
		setWindowTitle(Messages.EditPropertyConflictsWizard_0);
		
		summaryPage = new EditPropertyConflictsWizardSummaryPage();
		addPage(summaryPage);
		
		for (int i = 0; i < propertyConflicts.length; i++) {
			EditPropertyConflictsWizardPropertyPage propertyPage = new EditPropertyConflictsWizardPropertyPage(propertyConflicts[i]);
			addPage(propertyPage);
		}
	}

	public boolean performFinish() {
		IWizardPage[] pages = getPages();
		for (int i = 0; i < pages.length; i++) {
			if (pages[i] instanceof EditPropertyConflictsWizardPropertyPage) {
				EditPropertyConflictsWizardPropertyPage propertyPage = (EditPropertyConflictsWizardPropertyPage)pages[i];
				try {
					boolean deleteProperty = false;
					if (propertyPage.incomingSelected() && propertyPage.getRemoteProperty() == null && propertyPage.getPropertyValue().trim().length() == 0) {
						deleteProperty = true;
					}
					if (deleteProperty) {
						svnResource.deleteSvnProperty(propertyPage.getPropertyName(), false);
					} else {
						svnResource.setSvnProperty(propertyPage.getPropertyName(), propertyPage.getPropertyValue(), false);
					}
				} catch (SVNException e) {
					MessageDialog.openError(getShell(), getWindowTitle(), e.getMessage());
					return false;
				}
			}
		}
		if (summaryPage.markResolvedButton.getSelection()) {
			 IResource resource = svnResource.getResource();
			 IResource[] selectedResources = { resource };
			 try {
				new ResolveOperation(targetPart, selectedResources, ISVNConflictResolver.Choice.chooseMerged).run();
			} catch (Exception e) {
				MessageDialog.openError(getShell(), getWindowTitle(), e.getMessage());
				return false;
			}
		}
		return true;
	}

	public ISVNLocalResource getSvnResource() {
		return svnResource;
	}

	public String getConflictSummary() {
		return conflictSummary;
	}

	public PropertyConflict[] getPropertyConflicts() {
		return propertyConflicts;
	}

	public ISVNProperty[] getRemoteProperties() {
		return remoteProperties;
	}

}
