package org.tigris.subversion.subclipse.ui.wizards.dialogs;

import java.text.ParseException;
import java.util.ArrayList;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.history.ILogEntry;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.DepthComboHelper;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.dialogs.HistoryDialog;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class SvnWizardUpdatePage extends SvnWizardDialogPage {
    private static final int REVISION_WIDTH_HINT = 40;
    
    private IResource[] resources;
    
    private Text revisionText;
    private Button logButton;
    private Button headButton;
    private Button revisionButton;
	private Combo depthCombo;
	private Button ignoreExternalsButton;
	private Button forceButton;
	
    private SVNRevision revision;
    private int depth;
    private boolean ignoreExternals;
    private boolean force;
    
    private String[] urlStrings;
    private String commonRoot;

	public SvnWizardUpdatePage(IResource[] resources) {
		super("UpdateDialog", Policy.bind("UpdateDialog.title")); //$NON-NLS-1$ //$NON-NLS-2$
		this.resources = resources;
	}

	public void createButtonsForButtonBar(Composite parent, SvnWizardDialog wizardDialog) {}

	public void createControls(Composite parent) {
		commonRoot = getCommonRoot();
		
		Composite composite = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(data);
		
		Group revisionGroup = new Group(composite, SWT.NULL);
		revisionGroup.setText(Policy.bind("SwitchDialog.revision")); //$NON-NLS-1$
		GridLayout revisionLayout = new GridLayout();
		revisionLayout.numColumns = 3;
		revisionGroup.setLayout(revisionLayout);
		data = new GridData(GridData.FILL_BOTH);
		revisionGroup.setLayoutData(data);
		
		headButton = new Button(revisionGroup, SWT.RADIO);
		headButton.setText(Policy.bind("SwitchDialog.head")); //$NON-NLS-1$
		data = new GridData();
		data.horizontalSpan = 3;
		headButton.setLayoutData(data);
		
		revisionButton = new Button(revisionGroup, SWT.RADIO);
		revisionButton.setText(Policy.bind("SwitchDialog.revision")); //$NON-NLS-1$
		if (commonRoot == null) revisionButton.setEnabled(false);
		
		headButton.setSelection(true);
		
		revisionText = new Text(revisionGroup, SWT.BORDER);
		data = new GridData();
		data.widthHint = REVISION_WIDTH_HINT;
		revisionText.setLayoutData(data);
		revisionText.setEnabled(false);
		
		revisionText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                setPageComplete(canFinish());
            }		    
		});
		
		logButton = new Button(revisionGroup, SWT.PUSH);
		logButton.setText(Policy.bind("MergeDialog.showLog")); //$NON-NLS-1$
		logButton.setEnabled(false);
		logButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                showLog();
            }
		});	
		
		SelectionListener listener = new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                revisionText.setEnabled(revisionButton.getSelection());
                logButton.setEnabled(revisionButton.getSelection());
                setPageComplete(canFinish());
                if (revisionButton.getSelection()) {
                    revisionText.selectAll();
                    revisionText.setFocus();
                }
            }
		};
		
		headButton.addSelectionListener(listener);
		revisionButton.addSelectionListener(listener);
		
		Group parameterGroup = new Group(composite, SWT.NULL);
		GridLayout parameterLayout = new GridLayout();
		parameterLayout.numColumns = 2;
		parameterGroup.setLayout(parameterLayout);
		data = new GridData(GridData.FILL_BOTH);
		data.horizontalSpan = 3;
		parameterGroup.setLayoutData(data);	
		
		Label depthLabel = new Label(parameterGroup, SWT.NONE);
		depthLabel.setText(Policy.bind("SvnDialog.depth")); //$NON-NLS-1$
		depthCombo = new Combo(parameterGroup, SWT.READ_ONLY);
		DepthComboHelper.addDepths(depthCombo, true, ISVNUIConstants.DEPTH_UNKNOWN);
		
		ignoreExternalsButton = new Button(parameterGroup, SWT.CHECK);
		ignoreExternalsButton.setText(Policy.bind("SvnDialog.ignoreExternals")); //$NON-NLS-1$
		data = new GridData();
		data.horizontalSpan = 2;
		ignoreExternalsButton.setLayoutData(data);
		
		forceButton = new Button(parameterGroup, SWT.CHECK);
		forceButton.setText(Policy.bind("SvnDialog.force")); //$NON-NLS-1$
		data = new GridData();
		data.horizontalSpan = 2;
		forceButton.setLayoutData(data);
		forceButton.setSelection(true);
		
		setPageComplete(canFinish());		
	}

	public String getWindowTitle() {
		return Policy.bind("UpdateDialog.update"); //$NON-NLS-1$
	}

	public boolean performCancel() {
		return true;
	}

	public boolean performFinish() {
        if (headButton.getSelection()) revision = SVNRevision.HEAD;
        else {
            try {
                revision = SVNRevision.getRevision(revisionText.getText().trim());
            } catch (ParseException e1) {
              MessageDialog.openError(getShell(), Policy.bind("UpdateDialog.title"), Policy.bind("UpdateDialog.invalid")); //$NON-NLS-1$ //$NON-NLS-2$
              return false;   
            }
        }		
        ignoreExternals = ignoreExternalsButton.getSelection();
        force = forceButton.getSelection();
        depth = DepthComboHelper.getDepth(depthCombo);
		return true;
	}

	public void saveSettings() {}

	public void setMessage() {
		setMessage(Policy.bind("UpdateDialog.message")); //$NON-NLS-1$
	}
	
	private boolean canFinish() {
		return headButton.getSelection() || revisionText.getText().trim().length() > 0;
	}	
	
	protected void showLog() {
	    ISVNRemoteResource remoteResource = null;
        try {
            remoteResource = SVNWorkspaceRoot.getSVNResourceFor(resources[0]).getRepository().getRemoteFile(new SVNUrl(commonRoot));
        } catch (Exception e) {
            MessageDialog.openError(getShell(), Policy.bind("MergeDialog.showLog"), e.toString()); //$NON-NLS-1$
            return;
        }
        if (remoteResource == null) {
            MessageDialog.openError(getShell(), Policy.bind("MergeDialog.showLog"), Policy.bind("MergeDialog.urlError") + " " + commonRoot); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            return;	            
        }	
        HistoryDialog dialog = new HistoryDialog(getShell(), remoteResource);
        if (dialog.open() == HistoryDialog.CANCEL) return;
        ILogEntry[] selectedEntries = dialog.getSelectedLogEntries();
        if (selectedEntries.length == 0) return;
        revisionText.setText(Long.toString(selectedEntries[selectedEntries.length - 1].getRevision().getNumber()));
        setPageComplete(canFinish());
    }	
	
    private String getCommonRoot() {
    	ArrayList urlList = new ArrayList();
    	for (int i = 0; i < resources.length; i++) {
    		ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resources[i]);
    		try {
                String anUrl = svnResource.getStatus().getUrlString();
                if (anUrl != null) urlList.add(anUrl);
            } catch (SVNException e1) {}    		
    	}
    	urlStrings = new String[urlList.size()];
    	urlList.toArray(urlStrings);
    	if (urlStrings.length == 0) return null;
    	String urlString = urlStrings[0];
    	if (urlStrings.length == 1) return urlString;
    	String commonRoot = null;
    	tag1:
    	for (int i = 0; i < urlString.length(); i++) {
    		String partialPath = urlString.substring(0, i+1);
    		if (partialPath.endsWith("/")) {
	    		for (int j = 1; j < urlStrings.length; j++) {
	    			if (!urlStrings[j].startsWith(partialPath)) break tag1;
	    		}
	    		commonRoot = partialPath.substring(0, i);
    		}
    	}
    	return commonRoot;
    }	
	
	public SVNRevision getRevision() {
	    return revision;
	}
	
	public int getDepth() {
		return depth;
	}

	public boolean isIgnoreExternals() {
		return ignoreExternals;
	}

	public boolean isForce() {
		return force;
	}		

}
