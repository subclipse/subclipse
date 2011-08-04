package org.tigris.subversion.subclipse.ui.wizards;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.team.internal.ui.wizards.ImportProjectSetOperation;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;

@SuppressWarnings("restriction")
public class ImportSvnProjectSetWizard extends Wizard {
	private ImportSvnProjectSetMainPage mainPage;
	private String file;

	public ImportSvnProjectSetWizard(String file) {
		super();
		this.file = file;
		setNeedsProgressMonitor(true);
		setWindowTitle(Policy.bind("ImportSvnProjectSetWizard.0")); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	@Override
	public void addPages() {
		mainPage = new ImportSvnProjectSetMainPage("mainPage", Policy.bind("ImportSvnProjectSetWizard.2"), SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_WIZBAN_IMPORT_PROJECT_SET)); //$NON-NLS-1$ //$NON-NLS-2$
		addPage(mainPage);	
	}

	@Override
	public boolean performFinish() {
		try {
			ImportProjectSetOperation op = new ImportProjectSetOperation(null, file, mainPage.getWorkingSets());
			op.run();	
		} catch (InterruptedException e) {
			return true;
		} catch (Exception e) {
			MessageDialog.openError(getShell(), "Import a Team Project Set", e.getMessage()); //$NON-NLS-1$
			return false;
		}
		return true;
	}

}
