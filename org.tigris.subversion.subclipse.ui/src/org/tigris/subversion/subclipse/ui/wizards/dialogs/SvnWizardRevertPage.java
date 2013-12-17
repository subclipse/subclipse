package org.tigris.subversion.subclipse.ui.wizards.dialogs;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.compare.SVNLocalCompareInput;
import org.tigris.subversion.subclipse.ui.dialogs.CompareDialog;
import org.tigris.subversion.subclipse.ui.util.ResourceSelectionTree;
import org.tigris.subversion.svnclientadapter.SVNRevision;

public class SvnWizardRevertPage extends SvnWizardDialogPage {

    private IResource[] resourcesToRevert;
//  private String url;
    private Object[] selectedResources;
    private HashMap statusMap;
    
    private ResourceSelectionTree resourceSelectionTree;
    private boolean resourceRemoved;
    
    private Button includeUnversionedButton;
    private boolean includeUnversioned;
    private boolean fromSyncView;
 
	public SvnWizardRevertPage(IResource[] resourcesToRevert, String url, HashMap statusMap, boolean fromSyncView) {
		super("RevertDialog", Policy.bind("RevertDialog.title")); //$NON-NLS-1$
		this.fromSyncView = fromSyncView;
		if (fromSyncView) includeUnversioned = true;
		else includeUnversioned = 
			 SVNUIPlugin.getPlugin().getPreferenceStore().getBoolean(ISVNUIConstants.PREF_SELECT_UNADDED_RESOURCES_ON_COMMIT);    
		
		this.resourcesToRevert = resourcesToRevert;
//		this.url = url;
		this.statusMap = statusMap;	
	}

	public void createControls(Composite outerContainer) {
		Composite composite = new Composite(outerContainer, SWT.NULL);
		GridLayout layout = new GridLayout(1, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));

		ResourceSelectionTree.IToolbarControlCreator toolbarControlCreator = new ResourceSelectionTree.IToolbarControlCreator() {
			public void createToolbarControls(ToolBarManager toolbarManager) {
			      toolbarManager.add(new ControlContribution("ignoreUnversioned") { //$NON-NLS-1$
			          protected Control createControl(Composite parent) {
			              includeUnversionedButton = new Button(parent, SWT.CHECK);
			              includeUnversionedButton.setText(Policy.bind("CommitDialog.includeUnversioned")); //$NON-NLS-1$
			              includeUnversionedButton.setSelection(includeUnversioned);
			              includeUnversionedButton.addSelectionListener(
			              		new SelectionListener(){
			              			public void widgetSelected(SelectionEvent e) {
			              				includeUnversioned = includeUnversionedButton.getSelection();
			              				if( !includeUnversioned )
			              				{
			              					resourceSelectionTree.removeUnversioned();
			              				}
			              				else
			              				{
			              					resourceSelectionTree.addUnversioned();
			              				}
			              				selectedResources = resourceSelectionTree.getSelectedResources();
			              				setPageComplete(canFinish());
			              				if (!fromSyncView) updatePreference(includeUnversioned);
			              			}
			              			public void widgetDefaultSelected(SelectionEvent e) {
			              			}
			              		}
			              		);
			              return includeUnversionedButton;
			            }
			          });
			}
			public int getControlCount() {
				return 1;
			}			
		};
		
		IResource[] dedupedResourcesToRevert = ResourceSelectionTree.dedupeResources(resourcesToRevert);
		if (dedupedResourcesToRevert.length != resourcesToRevert.length) {
			resourceRemoved = true;
		}
		resourceSelectionTree = new ResourceSelectionTree(composite, SWT.NONE,
        Policy.bind("GenerateSVNDiff.Changes"), ResourceSelectionTree.dedupeResources(dedupedResourcesToRevert), statusMap, null, true, toolbarControlCreator, null); //$NON-NLS-1$
    	if (!resourceSelectionTree.showIncludeUnversionedButton()) includeUnversionedButton.setVisible(false);    
		
		// resourceSelectionTree.getTreeViewer().setAllChecked(true);
    resourceSelectionTree.getTreeViewer().addSelectionChangedListener(
        new ISelectionChangedListener() {
          public void selectionChanged(SelectionChangedEvent event) {
            selectedResources = resourceSelectionTree.getSelectedResources();
          }
        });
	((CheckboxTreeViewer)resourceSelectionTree.getTreeViewer()).addCheckStateListener(new ICheckStateListener() {
		public void checkStateChanged(CheckStateChangedEvent event) {
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
              } catch (Exception e1) {
              }
            }
          }
        });
    
	if( !includeUnversioned )
	{
		resourceSelectionTree.removeUnversioned();
	}

    resourceSelectionTree.getTreeViewer().getTree().setLayoutData(
        new GridData(SWT.FILL, SWT.FILL, true, true));		
		
//		Composite composite_1 = new Composite(composite, SWT.NONE);
//		composite_1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
//		composite_1.setLayout(new GridLayout());
    
    	selectedResources = resourceSelectionTree.getSelectedResources();
    	setPageComplete(canFinish());

		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.REVERT_DIALOG);		
	}

	public boolean performCancel() {
		return true;
	}

	public boolean performFinish() {
		if (!resourceRemoved) {
			resourceRemoved = resourceSelectionTree.isResourceRemoved();
		}
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

	public void setResourceRemoved(boolean resourceRemoved) {
		this.resourceRemoved = resourceRemoved;
	}

	public boolean isResourceRemoved() {
		return resourceRemoved;
	}
	
	public void updatePreference( boolean includeUnversioned )
	{
		SVNUIPlugin.getPlugin().getPreferenceStore().setValue(ISVNUIConstants.PREF_SELECT_UNADDED_RESOURCES_ON_COMMIT, includeUnversioned);    
	}
	
	private boolean canFinish() {
		return selectedResources.length > 0;
	}

}
