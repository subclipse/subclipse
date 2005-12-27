package org.tigris.subversion.subclipse.ui.wizards;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.repository.RepositoryFilters;
import org.tigris.subversion.subclipse.ui.repository.model.RemoteContentProvider;

public class CheckoutWizardSelectionPage extends WizardPage {
    private static final int LIST_HEIGHT_HINT = 250;
    private static final int LIST_WIDTH_HINT = 450;

    private ISVNRepositoryLocation repositoryLocation;

    private TreeViewer treeViewer;

	public CheckoutWizardSelectionPage(String pageName, String title, ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
		setPageComplete(false);
	}

	public void createControl(Composite parent) {
		Composite outerContainer = new Composite(parent,SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		outerContainer.setLayout(layout);
		outerContainer.setLayoutData(
		new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));

		treeViewer = new TreeViewer(outerContainer, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI);
        RemoteContentProvider contentProvider = new RemoteContentProvider();
        treeViewer.setContentProvider(contentProvider);
        treeViewer.addFilter(RepositoryFilters.FOLDERS_ONLY);
        treeViewer.setLabelProvider(new WorkbenchLabelProvider());
        treeViewer.setInput(repositoryLocation);

		GridData data = new GridData(GridData.FILL_BOTH | GridData.GRAB_VERTICAL);
		data.heightHint = LIST_HEIGHT_HINT;
		data.widthHint = LIST_WIDTH_HINT;
		treeViewer.getControl().setLayoutData(data);

		treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				CheckoutWizard wizard = (CheckoutWizard)getWizard();
				ArrayList folderArray = new ArrayList();
				IStructuredSelection selection = (IStructuredSelection)treeViewer.getSelection();
				Iterator iter = selection.iterator();
				while (iter.hasNext()) folderArray.add(iter.next());
				ISVNRemoteFolder[] remoteFolders = new ISVNRemoteFolder[folderArray.size()];
				folderArray.toArray(remoteFolders);
				wizard.setRemoteFolders(remoteFolders);
				setPageComplete(!treeViewer.getSelection().isEmpty());
			}
		});

		setMessage(Policy.bind("CheckoutWizardSelectionPage.text")); //$NON-NLS-1$

		setControl(outerContainer);
	}

	public boolean canFlipToNextPage() {
		CheckoutWizard wizard = (CheckoutWizard)getWizard();
		if (wizard != null) {
			return isPageComplete() && wizard.getNextPage(this, false) != null;
		}
		return super.canFlipToNextPage();
	}

	public void setLocation(ISVNRepositoryLocation repositoryLocation) {
		this.repositoryLocation = repositoryLocation;
		if (treeViewer != null) {
			treeViewer.setInput(repositoryLocation);
			treeViewer.refresh();
		}
	}

}
