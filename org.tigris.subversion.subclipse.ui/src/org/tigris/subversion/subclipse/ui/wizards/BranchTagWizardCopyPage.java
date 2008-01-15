package org.tigris.subversion.subclipse.ui.wizards;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.history.ILogEntry;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.dialogs.HistoryDialog;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class BranchTagWizardCopyPage extends SVNWizardPage {
	
	private static final int REVISION_WIDTH_HINT = 40;	
	
	private IResource resource;
	private ISVNRemoteResource remoteResource;
    protected Button serverButton;
    protected Button revisionButton;
    private Text revisionText;
    private Button logButton;
    protected Button workingCopyButton;	
    
    private long revisionNumber = 0;

	public BranchTagWizardCopyPage() {
		super("copyPage", //$NON-NLS-1$
				Policy.bind("BranchTagWizardCopyPage.heading"), //$NON-NLS-1$
				SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_WIZBAN_SVN),
				Policy.bind("BranchTagWizardCopyPage.message")); //$NON-NLS-1$
	}

	public void createControl(Composite parent) {
		resource = ((BranchTagWizard)getWizard()).getResource();
		remoteResource = ((BranchTagWizard)getWizard()).getRemoteResource();
		
		Composite outerContainer = new Composite(parent,SWT.NONE);
		GridLayout outerLayout = new GridLayout();
		outerLayout.numColumns = 1;
		outerLayout.marginHeight = 0;
		outerLayout.marginWidth = 0;
		outerContainer.setLayout(outerLayout);
		outerContainer.setLayoutData(
		new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		
		Group serverComposite = new Group(outerContainer, SWT.NULL);
		serverComposite.setText(Policy.bind("BranchTagDialog.createCopy")); //$NON-NLS-1$
		GridLayout serverLayout = new GridLayout();
		serverLayout.numColumns = 3;
		serverComposite.setLayout(serverLayout);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, false);
		serverComposite.setLayoutData(data);	
		
		serverButton = new Button(serverComposite, SWT.RADIO);
		serverButton.setText(Policy.bind("BranchTagDialog.head")); //$NON-NLS-1$
		data = new GridData();
		data.horizontalSpan = 3;
		serverButton.setLayoutData(data);
		
		revisionButton = new Button(serverComposite, SWT.RADIO);
		revisionButton.setText(Policy.bind("BranchTagDialog.revision")); //$NON-NLS-1$
		
		revisionText = new Text(serverComposite, SWT.BORDER);
		data = new GridData();
		data.widthHint = REVISION_WIDTH_HINT;
		revisionText.setLayoutData(data);
		if (revisionNumber == 0) revisionText.setEnabled(false);
		else revisionText.setText("" + revisionNumber);
		revisionText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                setPageComplete(canFinish());
            }		   
		});
		logButton = new Button(serverComposite, SWT.PUSH);
		logButton.setText(Policy.bind("MergeDialog.showLog")); //$NON-NLS-1$
		if (revisionNumber == 0)
			logButton.setEnabled(false);
		logButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                showLog();
            }
		});		
		
		workingCopyButton = new Button(serverComposite, SWT.RADIO);
		workingCopyButton.setText(Policy.bind("BranchTagDialog.working")); //$NON-NLS-1$
		data = new GridData();
		data.horizontalSpan = 3;
		workingCopyButton.setLayoutData(data);	
		if (resource == null) workingCopyButton.setVisible(false);
		
		if (revisionNumber == 0) serverButton.setSelection(true);
		else revisionButton.setSelection(true);
		
		SelectionListener selectionListener = new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                revisionText.setEnabled(revisionButton.getSelection());
                logButton.setEnabled(revisionButton.getSelection());
                if (revisionButton.getSelection()) revisionText.setFocus();               
                setPageComplete(canFinish());
            }
		};
		
		serverButton.addSelectionListener(selectionListener);
		revisionButton.addSelectionListener(selectionListener);
		workingCopyButton.addSelectionListener(selectionListener);		
		
		FocusListener focusListener = new FocusListener() {
			public void focusGained(FocusEvent e) {
				((Text)e.getSource()).selectAll();
			}
			public void focusLost(FocusEvent e) {
				((Text)e.getSource()).setText(((Text)e.getSource()).getText());
			}		
		};
		revisionText.addFocusListener(focusListener);		

		setControl(outerContainer);
	}
	
	private void showLog() {
	    ISVNRemoteResource remoteResource = null;
		if (((BranchTagWizard)getWizard()).multipleSelections()) {
			ISVNRepositoryLocation repository = null;
			if (resource == null) repository = this.remoteResource.getRepository();
			else repository = SVNWorkspaceRoot.getSVNResourceFor(resource).getRepository();
	        try {
	            remoteResource = repository.getRemoteFile(new SVNUrl(((BranchTagWizard)getWizard()).getCommonRoot()));
	        } catch (Exception e) {
	            MessageDialog.openError(getShell(), Policy.bind("MergeDialog.showLog"), e.toString()); //$NON-NLS-1$
	            return;
	        }			
		} else {	    
		    if (resource == null) remoteResource = this.remoteResource;
		    else {
		        try {
		            remoteResource = SVNWorkspaceRoot.getSVNResourceFor(resource).getRepository().getRemoteFile(((BranchTagWizard)getWizard()).getUrl());
		        } catch (Exception e) {
		            MessageDialog.openError(getShell(), Policy.bind("MergeDialog.showLog"), e.toString()); //$NON-NLS-1$
		            return;
		        }
		    }
	        if (remoteResource == null) {
	            MessageDialog.openError(getShell(), Policy.bind("MergeDialog.showLog"), Policy.bind("MergeDialog.urlError") + " " + ((BranchTagWizard)getWizard()).getUrlText()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	            return;	            
	        }	
		}
        HistoryDialog dialog = new HistoryDialog(getShell(), remoteResource);
        if (dialog.open() == HistoryDialog.CANCEL) return;
        ILogEntry[] selectedEntries = dialog.getSelectedLogEntries();
        if (selectedEntries.length == 0) return;
        revisionText.setText(Long.toString(selectedEntries[selectedEntries.length - 1].getRevision().getNumber()));
        setPageComplete(canFinish());
    }	
	
    public void setRevisionNumber(long revisionNumber) {
		this.revisionNumber = revisionNumber;
	}
    
    public String getRevision() {
    	return revisionText.getText().trim();
    }
    
    private boolean canFinish() {
    	if (revisionButton.getSelection() && revisionText.getText().trim().length() == 0) return false;
    	return true;
    }

}
