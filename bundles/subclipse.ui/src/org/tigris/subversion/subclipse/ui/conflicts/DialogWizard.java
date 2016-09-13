package org.tigris.subversion.subclipse.ui.conflicts;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.wizard.Wizard;
import org.tigris.subversion.subclipse.ui.Messages;
import org.tigris.subversion.svnclientadapter.ISVNConflictResolver;
import org.tigris.subversion.svnclientadapter.SVNConflictDescriptor;

public class DialogWizard extends Wizard {
	private SVNConflictDescriptor conflictDescriptor;
	private String myValue;
	private String incomingValue;
	private String valueToUse;
	private IResource[] resources;
	private boolean conflictResolved;
	private int resolution;
	private volatile ConflictResolution conflictResolution;
	private FinishedEditingWizardPage finishedEditingWizardPage;
	private ConflictHandlingWizardPage conflictHandlingWizardPage;
	private PropertyValueSelectionWizardPage propertyValueSelectionWizardPage;
	
	public int type;

	public final static int CONFLICT_HANDLING = 0;
	public final static int FINISHED_EDITING = 1;
	public final static int PROPERTY_VALUE_SELECTION = 2;

	public DialogWizard(int type) {
		super();
		this.type = type;
	}

	public void addPages() {
		super.addPages();
		if (type == FINISHED_EDITING) {
			setWindowTitle(Messages.DialogWizard_0);
			boolean propertyConflict = conflictDescriptor != null && conflictDescriptor.getConflictKind() == SVNConflictDescriptor.Kind.property;
			finishedEditingWizardPage = new FinishedEditingWizardPage("finishedEditing", propertyConflict); //$NON-NLS-1$
			addPage(finishedEditingWizardPage);
		}
		if (type == CONFLICT_HANDLING) {
			setWindowTitle(Messages.DialogWizard_1);
			conflictHandlingWizardPage = new ConflictHandlingWizardPage("handleConflict"); //$NON-NLS-1$
			conflictHandlingWizardPage.setConflictDescriptor(conflictDescriptor);
			conflictHandlingWizardPage.setResource(resources[0]);
			addPage(conflictHandlingWizardPage);
		}
		if (type == PROPERTY_VALUE_SELECTION) {
			setWindowTitle(Messages.DialogWizard_2);
			propertyValueSelectionWizardPage = new PropertyValueSelectionWizardPage("propertyValueSelection"); //$NON-NLS-1$
			propertyValueSelectionWizardPage.setConflictDescriptor(conflictDescriptor);
			propertyValueSelectionWizardPage.setMyValue(myValue);
			propertyValueSelectionWizardPage.setIncomingValue(incomingValue);
			propertyValueSelectionWizardPage.setResource(resources[0]);
			addPage(propertyValueSelectionWizardPage);
		}
	}

	public boolean performFinish() {
		if (type == FINISHED_EDITING) {
			resolution = finishedEditingWizardPage.getResolution();
			conflictResolved = resolution != ISVNConflictResolver.Choice.postpone;
		}
		if (type == CONFLICT_HANDLING) {
			conflictResolution = conflictHandlingWizardPage.getConflictResolution();
		}
		if (type == PROPERTY_VALUE_SELECTION) {
			conflictResolved = true;
			valueToUse = propertyValueSelectionWizardPage.getValue();
		}
		return true;
	}

	public boolean performCancel() {
		if (type == CONFLICT_HANDLING)
			conflictResolution = conflictHandlingWizardPage.getConflictResolution();
		if (type == PROPERTY_VALUE_SELECTION) {
			valueToUse = propertyValueSelectionWizardPage.getValue();
			conflictResolved = false;
		}
		return super.performCancel();
	}

	public boolean isConflictResolved() {
		return conflictResolved;
	}

	public ConflictResolution getConflictResolution() {
		return conflictResolution;
	}

	public void setConflictDescriptor(SVNConflictDescriptor conflictDescriptor) {
		this.conflictDescriptor = conflictDescriptor;
	}

	public void setResources(IResource[] resources) {
		this.resources = resources;
	}

	public int getResolution() {
		return resolution;
	}

	public void setMyValue(String myValue) {
		this.myValue = myValue;
	}

	public void setIncomingValue(String incomingValue) {
		this.incomingValue = incomingValue;
	}

	public String getValueToUse() {
		return valueToUse;
	}

}
