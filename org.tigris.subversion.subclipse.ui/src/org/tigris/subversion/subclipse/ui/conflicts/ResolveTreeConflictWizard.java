package org.tigris.subversion.subclipse.ui.conflicts;

import org.eclipse.jface.wizard.Wizard;
import org.tigris.subversion.subclipse.core.resources.SVNTreeConflict;

public class ResolveTreeConflictWizard extends Wizard {
	private SVNTreeConflict treeConflict;

	private ResolveTreeConflictWizardMainPage mainPage;

	public ResolveTreeConflictWizard(SVNTreeConflict treeConflict) {
		super();
		this.treeConflict = treeConflict;
	}
	
	public void addPages() {
		super.addPages();
		setWindowTitle("Resolve Tree Conflict on " + treeConflict.getResource().getName());
		
		mainPage = new ResolveTreeConflictWizardMainPage();
		addPage(mainPage);
	}

	public boolean performFinish() {
		if (mainPage.getMergeFromRepository()) {
			System.out.println("Merge " + treeConflict.getResource().getName() + " from repository into selected working copy resource");
		}
		if (mainPage.getMergeFromWorkingCopy()) {
			System.out.println("Merge " + treeConflict.getResource().getName() + " in working copy into selected working copy resource");
		}
		if (mainPage.getRevertConflictedResource()) {
			System.out.println("Revert " + treeConflict.getResource().getName());
		}
		if (mainPage.getRemoveUnversionedConflictedResource()) {
			System.out.println("Remove " + treeConflict.getResource().getName() + " from working copy");
		}
		if (mainPage.getDeleteSelectedResource()) {
			System.out.println("Delete selected resource");
		}
		if (mainPage.getDeleteConflictedResource()) {
			System.out.println("Delete " + treeConflict.getResource().getName());
		}
		if (mainPage.getRevertSelectedResource()) {
			System.out.println("Revert selected resource");
		}
		if (mainPage.getRemoveUnversionedSelectedResource()) {
			System.out.println("Remove selected resource from working copy");
		}
		if (mainPage.getMarkResolved()) {
			System.out.println("Mark " + treeConflict.getResource().getName() + " resolved");
		}
		return true;
	}
	
	public SVNTreeConflict getTreeConflict() {
		return treeConflict;
	}

}
