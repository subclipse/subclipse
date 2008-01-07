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
import org.eclipse.swt.widgets.Text;
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
    private boolean resourceRemoved;
 
	public SvnWizardRevertPage(IResource[] resourcesToRevert, String url, HashMap statusMap) {
		super("RevertDialog", Policy.bind("RevertDialog.title")); //$NON-NLS-1$
		this.resourcesToRevert = resourcesToRevert;
		this.url = url;
		this.statusMap = statusMap;
	}

	public void createControls(Composite outerContainer) {
		Composite composite = new Composite(outerContainer, SWT.NULL);
		GridLayout layout = new GridLayout(1, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Composite labelComposite = new Composite(composite, SWT.NONE);
		labelComposite.setLayout(new GridLayout(2, false));
		labelComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		Label label = new Label(labelComposite, SWT.NONE);
		label.setText(Policy.bind("RevertDialog.url")); //$NON-NLS-1$
		
		Text text = new Text(labelComposite, SWT.READ_ONLY);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		if (url == null) {
		  text.setText(Policy.bind("RevertDialog.multiple")); //$NON-NLS-1$
		} else {
      text.setText(url);
    }
		
		resourceSelectionTree = new ResourceSelectionTree(composite, SWT.NONE,
        Policy.bind("GenerateSVNDiff.Changes"), resourcesToRevert, statusMap, null, false, null, null); //$NON-NLS-1$
    // resourceSelectionTree.getTreeViewer().setAllChecked(true);
    resourceSelectionTree.getTreeViewer().addSelectionChangedListener(
        new ISelectionChangedListener() {
          public void selectionChanged(SelectionChangedEvent event) {
            selectedResources = resourceSelectionTree.getSelectedResources();
          }
        });
    resourceSelectionTree.getTreeViewer().addDoubleClickListener(
        new IDoubleClickListener() {
          public void doubleClick(DoubleClickEvent event) {
            IStructuredSelection sel = (IStructuredSelection) event
                .getSelection();
            Object sel0 = sel.getFirstElement();
            if (sel0 instanceof IFile) {
              final ISVNLocalResource localResource = SVNWorkspaceRoot
                  .getSVNResourceFor((IFile) sel0);
              try {
                new CompareDialog(getShell(), new SVNLocalCompareInput(
                    localResource, SVNRevision.BASE, true)).open();
              } catch (SVNException e1) {
              }
            }
          }
        });

    resourceSelectionTree.getTreeViewer().getTree().setLayoutData(
        new GridData(SWT.FILL, SWT.FILL, true, true));		
		
//		Composite composite_1 = new Composite(composite, SWT.NONE);
//		composite_1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
//		composite_1.setLayout(new GridLayout());

		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.REVERT_DIALOG);		
	}

	public boolean performCancel() {
		return true;
	}

	public boolean performFinish() {
		resourceRemoved = resourceSelectionTree.isResourceRemoved();
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

	public boolean isResourceRemoved() {
		return resourceRemoved;
	}	

}
