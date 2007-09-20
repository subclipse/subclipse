package org.tigris.subversion.subclipse.ui.wizards.dialogs;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.compare.SVNLocalCompareInput;
import org.tigris.subversion.subclipse.ui.dialogs.CompareDialog;
import org.tigris.subversion.subclipse.ui.util.ResourceSelectionTree;
import org.tigris.subversion.svnclientadapter.SVNRevision;

public class SvnWizardRevertPage extends SvnWizardDialogPage {

    private IResource[] resourcesToRevert;
    private String url;
    private Object[] selectedResources;
    private HashMap statusMap;
    
    private ResourceSelectionTree resourceSelectionTree;
 
	public SvnWizardRevertPage(IResource[] resourcesToRevert, String url, HashMap statusMap) {
		super("RevertDialog", Policy.bind("RevertDialog.title")); //$NON-NLS-1$
		this.resourcesToRevert = resourcesToRevert;
		this.url = url;
		this.statusMap = statusMap;
	}

	public void createControls(Composite outerContainer) {
		Composite composite = new Composite(outerContainer, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(data);
		
		Label label = createWrappingLabel(composite);
		if (url == null) label.setText(Policy.bind("RevertDialog.url") + " " + Policy.bind("RevertDialog.multiple")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		else label.setText(Policy.bind("RevertDialog.url") + " " + url); //$NON-NLS-1$ //$NON-NLS-2$
		
		addResourcesArea(composite);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.REVERT_DIALOG);		

	}

	private void addResourcesArea(Composite composite) {
		resourceSelectionTree = new ResourceSelectionTree(composite, SWT.NONE, Policy.bind("GenerateSVNDiff.Changes"), resourcesToRevert, statusMap, null, false, null, null); //$NON-NLS-1$
//		resourceSelectionTree.getTreeViewer().setAllChecked(true);	
		resourceSelectionTree.getTreeViewer().addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				selectedResources = resourceSelectionTree.getSelectedResources();
			}
		});
		resourceSelectionTree.getTreeViewer().addDoubleClickListener(new IDoubleClickListener(){
			public void doubleClick(DoubleClickEvent event) {
				IStructuredSelection sel = (IStructuredSelection)event.getSelection();
				Object sel0 = sel.getFirstElement();
				if (sel0 instanceof IFile) {
					final ISVNLocalResource localResource= SVNWorkspaceRoot.getSVNResourceFor((IFile)sel0);
					try {
						new CompareDialog(getShell(),
							new SVNLocalCompareInput(localResource, SVNRevision.BASE, true)).open();
					} catch (SVNException e1) {
					}
				}
			}
		});		
	}

	public boolean performCancel() {
		return true;
	}

	public boolean performFinish() {
		return true;
	}

	public void setMessage() {
		setMessage(Policy.bind("RevertDialog.resources")); //$NON-NLS-1$
	}

	public void saveSettings() {
	}
	
	public IResource[] getSelectedResources() {
		if (selectedResources == null) {
			return resourcesToRevert;
		} else {
			List result = Arrays.asList(selectedResources);
			return (IResource[]) result.toArray(new IResource[result.size()]);
		}
	}

	public String getWindowTitle() {
		return Policy.bind("RevertDialog.title"); //$NON-NLS-1$
	}

	public void createButtonsForButtonBar(Composite parent, SvnWizardDialog wizardDialog) {
	}	

}
