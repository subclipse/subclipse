package org.tigris.subversion.subclipse.ui.wizards;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizardSwitchPage;

public class ReplaceWithBranchTagWizard extends Wizard {
	private IResource[] resources;
	
	private ReplaceWithBranchTagWizardMainPage mainPage;
	private SvnWizardSwitchPage switchPage;
	
	private boolean replaceContents;

	public ReplaceWithBranchTagWizard(IResource[] resources) {
		super();
		this.resources = resources;
		setWindowTitle(Policy.bind("ReplaceWithBranchTagWizard.0")); //$NON-NLS-1$
	}
	
	public void addPages() {
		boolean showUrl = true;
		if (resources.length == 1 && !(resources[0] instanceof IProject)) {
			mainPage = new ReplaceWithBranchTagWizardMainPage(resources);
			addPage(mainPage);
			showUrl = false;
		}
		switchPage = new SvnWizardSwitchPage(resources, showUrl);
		addPage(switchPage);
	}

	@Override
	public boolean performFinish() {
		if (mainPage == null) {
			replaceContents = false;
		}
		else {
			replaceContents = mainPage.isReplace();
		}
		if (mainPage != null) {
			boolean mainPageOk = mainPage.performFinish();
			if (!mainPageOk) {
				return false;
			}
		}
		if (!replaceContents) {
			return switchPage.performFinish();
		}
		return true;
	}

	public boolean isReplaceContents() {
		return replaceContents;
	}
	
	@Override
	public boolean canFinish() {
		if (mainPage != null) {
			return mainPage.isPageComplete();
		}
		else {
			return switchPage.isPageComplete();
		}
	}

	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		if (page == mainPage && !mainPage.isReplace()) {
			return switchPage;
		}
		else {
			return null;
		}
	}

	public ReplaceWithBranchTagWizardMainPage getMainPage() {
		return mainPage;
	}

	public SvnWizardSwitchPage getSwitchPage() {
		return switchPage;
	}

}
