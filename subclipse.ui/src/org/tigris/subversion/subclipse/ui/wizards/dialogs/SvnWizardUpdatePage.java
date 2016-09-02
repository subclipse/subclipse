package org.tigris.subversion.subclipse.ui.wizards.dialogs;

import java.text.ParseException;
import java.util.ArrayList;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.tigris.subversion.subclipse.core.ISVNCoreConstants;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.history.ILogEntry;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.DepthComboHelper;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.conflicts.SVNConflictResolver;
import org.tigris.subversion.subclipse.ui.dialogs.HistoryDialog;
import org.tigris.subversion.svnclientadapter.ISVNConflictResolver;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class SvnWizardUpdatePage extends SvnWizardDialogPage {
    private static final int REVISION_WIDTH_HINT = 40;
    
    private IResource[] resources;
    
    private Text revisionText;
    private Button logButton;
    private Button headButton;
	private Combo depthCombo;
	private Button setDepthButton;	
	private Button ignoreExternalsButton;
	private Button forceButton;
	
	private Button textConflictPromptButton;
	private Button textConflictMarkButton;
	
	private Button propertyConflictPromptButton;
	private Button propertyConflictMarkButton;

	private Button binaryConflictPromptButton;
	private Button binaryConflictMarkButton;
	private Button binaryConflictUserButton;
	private Button binaryConflictIncomingButton;
	
	private Button treeConflictPromptButton;
	private Button treeConflictMarkButton;
	private Button treeConflictUserButton;
	private Button treeConflictResolveButton;
	
	private SVNConflictResolver conflictResolver;
	
    private SVNRevision revision;
    private int depth;
    private boolean setDepth;
    private boolean ignoreExternals;
    private boolean force;
    
    private String[] urlStrings;
    private String commonRoot;
    
    private long defaultRevision;

	public SvnWizardUpdatePage(IResource[] resources) {
		super("UpdateDialogWithConflictHandling2", Policy.bind("UpdateDialog.title")); //$NON-NLS-1$ //$NON-NLS-2$
		this.resources = resources;
	}
	
	public SvnWizardUpdatePage(String name, IResource[] resources) {
		super(name, Policy.bind("UpdateDialog.title")); //$NON-NLS-1$
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
		
		final Composite revisionGroup = new Composite(composite, SWT.NULL);
		GridLayout revisionLayout = new GridLayout();
		revisionLayout.numColumns = 3;
		revisionLayout.marginWidth = 0;
		revisionLayout.marginHeight = 0;
		revisionGroup.setLayout(revisionLayout);
		data = new GridData(SWT.FILL, SWT.FILL, true, false);
		revisionGroup.setLayoutData(data);
		
		headButton = new Button(revisionGroup, SWT.CHECK);
		headButton.setText(Policy.bind("SvnWizardUpdatePage.head")); //$NON-NLS-1$
		data = new GridData();
		data.horizontalSpan = 3;
		headButton.setLayoutData(data);
		
		headButton.setSelection(true);
		
		if (commonRoot == null) headButton.setEnabled(false);
		
		Label revisionLabel = new Label(revisionGroup, SWT.NONE);
		revisionLabel.setText(Policy.bind("SvnWizardUpdatePage.revision")); //$NON-NLS-1$
		
		revisionText = new Text(revisionGroup, SWT.BORDER);
		data = new GridData();
		data.widthHint = REVISION_WIDTH_HINT;
		revisionText.setLayoutData(data);
		revisionText.setEnabled(false);
		if (defaultRevision > 0) {
			revisionText.setText(Long.toString(defaultRevision));
		}
		
		revisionText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                setPageComplete(canFinish());
            }		    
		});
		
		FocusListener focusListener = new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				((Text)e.getSource()).selectAll();
			}
			public void focusLost(FocusEvent e) {
				((Text)e.getSource()).setText(((Text)e.getSource()).getText());
			}					
		};
		revisionText.addFocusListener(focusListener);
		
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
                revisionText.setEnabled(!headButton.getSelection());
                logButton.setEnabled(!headButton.getSelection());
                setPageComplete(canFinish());
                if (!headButton.getSelection()) {
                    revisionText.selectAll();
                    revisionText.setFocus();
                }
            }
		};
		
		headButton.addSelectionListener(listener);
		
		Group parameterGroup = new Group(composite, SWT.NULL);
		GridLayout parameterLayout = new GridLayout();
		parameterLayout.numColumns = 2;
		parameterGroup.setLayout(parameterLayout);
		data = new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1);
		parameterGroup.setLayoutData(data);	
		
		Label depthLabel = new Label(parameterGroup, SWT.NONE);
		depthLabel.setText(Policy.bind("SvnDialog.depth")); //$NON-NLS-1$
		depthCombo = new Combo(parameterGroup, SWT.READ_ONLY);
		String defaultDepth;
		switch (depth) {
		case ISVNCoreConstants.DEPTH_EMPTY:
			defaultDepth = ISVNUIConstants.DEPTH_EMPTY;
			break;
		case ISVNCoreConstants.DEPTH_EXCLUDE:
			defaultDepth = ISVNUIConstants.DEPTH_EXCLUDE;
			break;
		case ISVNCoreConstants.DEPTH_FILES:
			defaultDepth = ISVNUIConstants.DEPTH_FILES;
			break;	
		case ISVNCoreConstants.DEPTH_IMMEDIATES:
			defaultDepth = ISVNUIConstants.DEPTH_IMMEDIATES;
			break;	
		case ISVNCoreConstants.DEPTH_INFINITY:
			defaultDepth = ISVNUIConstants.DEPTH_INFINITY;
			break;			
		default:
			defaultDepth = ISVNUIConstants.DEPTH_UNKNOWN;
			break;
		}
		DepthComboHelper.addDepths(depthCombo, true, true, defaultDepth);

		depthCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				if (depthCombo.getText().equals(ISVNUIConstants.DEPTH_EXCLUDE)) {
					setDepthButton.setSelection(true);
					setDepthButton.setEnabled(false);
					ignoreExternalsButton.setVisible(false);
					forceButton.setVisible(false);
					revisionGroup.setVisible(false);
				} else {
					setDepthButton.setEnabled(true);
					ignoreExternalsButton.setVisible(true);
					forceButton.setVisible(true);
					revisionGroup.setVisible(true);
				}
				setPageComplete(canFinish());
			}			
		});
		
		setDepthButton = new Button(parameterGroup, SWT.CHECK);
		setDepthButton.setText(Policy.bind("SvnDialog.setDepth")); //$NON-NLS-1$
		data = new GridData();
		data.horizontalSpan = 2;
		setDepthButton.setLayoutData(data);
		setDepthButton.setSelection(setDepth);
		
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
		
		Group conflictGroup = new Group(composite, SWT.NONE);
		conflictGroup.setText(Policy.bind("SvnWizardUpdatePage.0")); //$NON-NLS-1$
		GridLayout conflictLayout = new GridLayout();
		conflictLayout.numColumns = 1;
		conflictGroup.setLayout(conflictLayout);
		conflictGroup.setLayoutData(
		new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		
		Group textGroup = new Group(conflictGroup, SWT.NONE);
		textGroup.setText(Policy.bind("SvnWizardUpdatePage.1")); //$NON-NLS-1$
		GridLayout textLayout = new GridLayout();
		textLayout.numColumns = 1;
		textGroup.setLayout(textLayout);
		textGroup.setLayoutData(
		new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));	
		
		textConflictPromptButton = new Button(textGroup, SWT.RADIO);
		textConflictPromptButton.setText(Policy.bind("SvnWizardUpdatePage.2")); //$NON-NLS-1$
		textConflictMarkButton = new Button(textGroup, SWT.RADIO);
		textConflictMarkButton.setText(Policy.bind("SvnWizardUpdatePage.3")); //$NON-NLS-1$
		
		Group binaryGroup = new Group(conflictGroup, SWT.NONE);
		binaryGroup.setText(Policy.bind("SvnWizardUpdatePage.4")); //$NON-NLS-1$
		GridLayout binaryLayout = new GridLayout();
		binaryLayout.numColumns = 1;
		binaryGroup.setLayout(binaryLayout);
		binaryGroup.setLayoutData(
		new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));	
		
		binaryConflictPromptButton = new Button(binaryGroup, SWT.RADIO);
		binaryConflictPromptButton.setText(Policy.bind("SvnWizardUpdatePage.5")); //$NON-NLS-1$
		binaryConflictMarkButton = new Button(binaryGroup, SWT.RADIO);
		binaryConflictMarkButton.setText(Policy.bind("SvnWizardUpdatePage.6")); //$NON-NLS-1$
		binaryConflictUserButton = new Button(binaryGroup, SWT.RADIO);
		binaryConflictUserButton.setText(Policy.bind("SvnWizardUpdatePage.7")); //$NON-NLS-1$
		binaryConflictIncomingButton = new Button(binaryGroup, SWT.RADIO);
		binaryConflictIncomingButton.setText(Policy.bind("SvnWizardUpdatePage.8")); //$NON-NLS-1$

		Group propertyGroup = new Group(conflictGroup, SWT.NONE);
		propertyGroup.setText(Policy.bind("SvnWizardUpdatePage.9")); //$NON-NLS-1$
		GridLayout propertyLayout = new GridLayout();
		propertyLayout.numColumns = 1;
		propertyGroup.setLayout(propertyLayout);
		propertyGroup.setLayoutData(
		new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));			

		propertyConflictPromptButton = new Button(propertyGroup, SWT.RADIO);
		propertyConflictPromptButton.setText(Policy.bind("SvnWizardUpdatePage.10")); //$NON-NLS-1$
		propertyConflictMarkButton = new Button(propertyGroup, SWT.RADIO);
		propertyConflictMarkButton.setText(Policy.bind("SvnWizardUpdatePage.11")); //$NON-NLS-1$
		
		Group treeConflictGroup = new Group(conflictGroup, SWT.NONE);
		treeConflictGroup.setText(Policy.bind("SvnWizardUpdatePage.12")); //$NON-NLS-1$
		GridLayout treeConflictLayout = new GridLayout();
		treeConflictLayout.numColumns = 1;
		treeConflictGroup.setLayout(treeConflictLayout);
		treeConflictGroup.setLayoutData(
		new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));	
		
		treeConflictPromptButton = new Button(treeConflictGroup, SWT.RADIO);
		treeConflictPromptButton.setText(Policy.bind("SvnWizardUpdatePage.10")); //$NON-NLS-1$
		treeConflictMarkButton = new Button(treeConflictGroup, SWT.RADIO);
		treeConflictMarkButton.setText(Policy.bind("SvnWizardUpdatePage.11")); //$NON-NLS-1$
		treeConflictUserButton = new Button(treeConflictGroup, SWT.RADIO);
		treeConflictUserButton.setText(Policy.bind("SvnWizardUpdatePage.13")); //$NON-NLS-1$
		treeConflictResolveButton = new Button(treeConflictGroup, SWT.RADIO);
		treeConflictResolveButton.setText(Policy.bind("SvnWizardUpdatePage.14")); //$NON-NLS-1$
		
		textConflictMarkButton.setSelection(true);
		binaryConflictMarkButton.setSelection(true);
		propertyConflictMarkButton.setSelection(true);
		treeConflictMarkButton.setSelection(true);
		
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
        setDepth = setDepthButton.getSelection();
        ignoreExternals = ignoreExternalsButton.getSelection();
        force = forceButton.getSelection();
        depth = DepthComboHelper.getDepth(depthCombo);
        conflictResolver = new SVNConflictResolver(resources[0], getTextConflictHandling(), getBinaryConflictHandling(), getPropertyConflictHandling(), getTreeConflictHandling());
		return true;
	}

	public void saveSettings() {}

	public SVNConflictResolver getConflictResolver() {
		return conflictResolver;
	}

	public int getTextConflictHandling() {
		if (textConflictMarkButton.getSelection()) return ISVNConflictResolver.Choice.postpone;
		else return ISVNConflictResolver.Choice.chooseMerged;
	}
	
	public int getBinaryConflictHandling() {
		if (binaryConflictIncomingButton.getSelection()) return ISVNConflictResolver.Choice.chooseTheirsFull;
		else if (binaryConflictUserButton.getSelection()) return ISVNConflictResolver.Choice.chooseMineFull;
		else if (binaryConflictMarkButton.getSelection()) return ISVNConflictResolver.Choice.postpone;
		else return ISVNConflictResolver.Choice.chooseMerged;
	}
	
	public int getPropertyConflictHandling() {
		if (propertyConflictMarkButton.getSelection()) return ISVNConflictResolver.Choice.postpone;
		else return ISVNConflictResolver.Choice.chooseMerged;
	}
	
	public int getTreeConflictHandling() {
		if (treeConflictMarkButton.getSelection()) return ISVNConflictResolver.Choice.postpone;
		else if (treeConflictResolveButton.getSelection()) return ISVNConflictResolver.Choice.chooseMerged;
		else if (treeConflictUserButton.getSelection()) return ISVNConflictResolver.Choice.chooseMine;
		else return SVNConflictResolver.PROMPT;
	}

	public void setMessage() {
		setMessage(Policy.bind("UpdateDialog.message")); //$NON-NLS-1$
	}
	
	public void setDefaultRevision(long defaultRevision) {
		this.defaultRevision = defaultRevision;
	}
	
	public void setDepth(int depth) {
		this.depth = depth;
	}

	public void setSetDepth(boolean setDepth) {
		this.setDepth = setDepth;
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
    	ArrayList<String> urlList = new ArrayList<String>();
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
    		if (partialPath.endsWith("/")) { //$NON-NLS-1$
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
	
	public boolean isSetDepth() {
		return setDepth;
	}

	public boolean isIgnoreExternals() {
		return ignoreExternals;
	}

	public boolean isForce() {
		return force;
	}		

}
