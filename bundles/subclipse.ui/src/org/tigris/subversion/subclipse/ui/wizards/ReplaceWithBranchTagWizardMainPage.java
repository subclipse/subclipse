package org.tigris.subversion.subclipse.ui.wizards;

import java.net.MalformedURLException;
import java.text.ParseException;
import java.util.ArrayList;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.IDialogSettings;
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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.history.ILogEntry;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.dialogs.ChooseUrlDialog;
import org.tigris.subversion.subclipse.ui.dialogs.HistoryDialog;
import org.tigris.subversion.subclipse.ui.util.UrlCombo;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class ReplaceWithBranchTagWizardMainPage extends SVNWizardPage {
	private IResource[] resources;
	
    private UrlCombo urlCombo;
    private Text revisionText;
    private Button logButton;
    private Button headButton;
	
	private Button switchButton;
	private Button replaceButton;
	
    private String[] urlStrings;
    private String commonRoot;
    private SVNUrl url;
    private SVNRevision revision;
    private long revisionNumber;
	
	private IDialogSettings settings = SVNUIPlugin.getPlugin().getDialogSettings();
	
	public final static String REPLACE_CONTENTS = "ReplaceWithBranchTagWizardMainPage.replaceContents"; //$NON-NLS-1$
	private static final int REVISION_WIDTH_HINT = 40;

	public ReplaceWithBranchTagWizardMainPage(IResource[] resources) {
		super("mainPage", //$NON-NLS-1$
				Policy.bind("ReplaceWithBranchTagWizardMainPage.1"), //$NON-NLS-1$
				SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_WIZBAN_SVN),
				Policy.bind("ReplaceWithBranchTagWizardMainPage.2")); //$NON-NLS-1$
		this.resources = resources;
	}

	public void createControl(Composite parent) {
		Composite outerContainer = new Composite(parent,SWT.NONE);
		GridLayout outerLayout = new GridLayout();
		outerLayout.numColumns = 1;
		outerLayout.marginHeight = 0;
		outerLayout.marginWidth = 0;
		outerContainer.setLayout(outerLayout);
		outerContainer.setLayoutData(
		new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		
		Group urlGroup = new Group(outerContainer, SWT.NONE);
		GridLayout urlLayout = new GridLayout();
		urlLayout.numColumns = 3;
		urlGroup.setLayout(urlLayout);
		urlGroup.setLayoutData(
		new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		urlGroup.setText(Policy.bind("ReplaceWithBranchTagWizardMainPage.3")); //$NON-NLS-1$
		
		Label urlLabel = new Label(urlGroup, SWT.NONE);
		urlLabel.setText(Policy.bind("ReplaceWithBranchTagWizardMainPage.0")); //$NON-NLS-1$
		
		urlCombo = new UrlCombo(urlGroup, SWT.NONE);
		urlCombo.init(resources[0].getProject().getName());
		urlCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));

		commonRoot = getCommonRoot();
		if (commonRoot != null) urlCombo.setText(commonRoot);
        
        urlCombo.getCombo().addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                setPageComplete(canFinish());
            }         
        });
		
		Button browseButton = new Button(urlGroup, SWT.PUSH);
		browseButton.setText(Policy.bind("SwitchDialog.browse")); //$NON-NLS-1$
		browseButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                ChooseUrlDialog dialog = new ChooseUrlDialog(getShell(), resources[0]);
                dialog.setIncludeBranchesAndTags(resources.length == 1);
                if ((dialog.open() == ChooseUrlDialog.OK) && (dialog.getUrl() != null)) {
                    urlCombo.setText(dialog.getUrl());
                    setPageComplete(canFinish());
                }
            }
		});

		final Composite revisionGroup = new Composite(urlGroup, SWT.NULL);
		GridLayout revisionLayout = new GridLayout();
		revisionLayout.numColumns = 3;
		revisionLayout.marginWidth = 0;
		revisionLayout.marginHeight = 0;
		revisionGroup.setLayout(revisionLayout);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 3;
		revisionGroup.setLayoutData(data);
		
		headButton = new Button(revisionGroup, SWT.CHECK);
		headButton.setText(Policy.bind("ReplaceWithBranchTagWizardMainPage.5")); //$NON-NLS-1$
		data = new GridData();
		data.horizontalSpan = 3;
		headButton.setLayoutData(data);
		
		Label revisionLabel = new Label(revisionGroup, SWT.NONE);
		revisionLabel.setText(Policy.bind("SvnWizardSwitchPage.revision")); //$NON-NLS-1$
		
		revisionText = new Text(revisionGroup, SWT.BORDER);
		data = new GridData();
		data.widthHint = REVISION_WIDTH_HINT;
		revisionText.setLayoutData(data);
		
		if (revisionNumber == 0) {
			headButton.setSelection(true);
			revisionText.setEnabled(false);
		} else {
			revisionText.setText("" + revisionNumber); //$NON-NLS-1$
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
		
		Group typeGroup = new Group(outerContainer, SWT.NONE);
		GridLayout typeLayout = new GridLayout();
		typeLayout.numColumns = 1;
		typeGroup.setLayout(typeLayout);
		typeGroup.setLayoutData(
		new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		typeGroup.setText(Policy.bind("ReplaceWithBranchTagWizardMainPage.6")); //$NON-NLS-1$
		
		switchButton = new Button(typeGroup, SWT.RADIO);
		switchButton.setText(Policy.bind("ReplaceWithBranchTagWizardMainPage.7")); //$NON-NLS-1$
		
		Label switchLabel = new Label(typeGroup, SWT.WRAP);
		switchLabel.setText(Policy.bind("ReplaceWithBranchTagWizardMainPage.8")); //$NON-NLS-1$
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = 500;
		data.horizontalIndent = 15;
		switchLabel.setLayoutData(data);
				
		replaceButton = new Button(typeGroup, SWT.RADIO);
		replaceButton.setText(Policy.bind("ReplaceWithBranchTagWizardMainPage.9")); //$NON-NLS-1$
		
		data = new GridData();
		data.verticalIndent = 15;
		replaceButton.setLayoutData(data);
		
		Label contentsLabel = new Label(typeGroup, SWT.WRAP);
		contentsLabel.setText(Policy.bind("ReplaceWithBranchTagWizardMainPage.10")); //$NON-NLS-1$
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = 500;
		data.horizontalIndent = 15;
		contentsLabel.setLayoutData(data);

		if (settings.getBoolean(REPLACE_CONTENTS)) {
			replaceButton.setSelection(true);
		}
		else {
			switchButton.setSelection(true);
		}
		
		SelectionListener selectionListener = new SelectionAdapter() {		
			public void widgetSelected(SelectionEvent e) {
				settings.put(REPLACE_CONTENTS, replaceButton.getSelection());
				setPageComplete(canFinish());
			}
		};
		replaceButton.addSelectionListener(selectionListener);
		switchButton.addSelectionListener(selectionListener);
		
		setControl(outerContainer);
	}
	
	public boolean performFinish() {
		urlCombo.saveUrl();
		try {
			url =  new SVNUrl(urlCombo.getText());
		} catch (MalformedURLException e) {
			MessageDialog.openError(getShell(), "Replace with Branch/Tag", e.getMessage()); //$NON-NLS-1$
			return false;
		}
        if (headButton.getSelection()) revision = SVNRevision.HEAD;
        else {
            try {
                revision = SVNRevision.getRevision(revisionText.getText().trim());
            } catch (ParseException e1) {
              MessageDialog.openError(getShell(), "Replace with Branch/Tag", Policy.bind("SwitchDialog.invalid")); //$NON-NLS-1$ //$NON-NLS-2$
              return false;   
            }
        }
		return true;
	}
	
	public boolean isReplace() {
		return replaceButton.getSelection();
	}
	
	protected void showLog() {
	    ISVNRemoteResource remoteResource = null;
        try {
            remoteResource = SVNWorkspaceRoot.getSVNResourceFor(resources[0]).getRepository().getRemoteFile(new SVNUrl(urlCombo.getText()));
        } catch (Exception e) {
            MessageDialog.openError(getShell(), Policy.bind("MergeDialog.showLog"), e.toString()); //$NON-NLS-1$
            return;
        }
        if (remoteResource == null) {
            MessageDialog.openError(getShell(), Policy.bind("MergeDialog.showLog"), Policy.bind("MergeDialog.urlError") + " " + urlCombo.getText()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            return;	            
        }	
        HistoryDialog dialog = new HistoryDialog(getShell(), remoteResource);
        if (dialog.open() == HistoryDialog.CANCEL) return;
        ILogEntry[] selectedEntries = dialog.getSelectedLogEntries();
        if (selectedEntries.length == 0) return;
        revisionText.setText(Long.toString(selectedEntries[selectedEntries.length - 1].getRevision().getNumber()));
        setPageComplete(canFinish());
    }	
	
	public boolean canFinish() {
		setErrorMessage(null);
		if (!(urlCombo.getText().length() > 0 && (headButton.getSelection() || (revisionText.getText().trim().length() > 0)))) return false;
		return true;
	}
	
    public SVNUrl getUrl() {
		return url;
	}

	public SVNRevision getRevision() {
        return revision;
    }
	
    private String getCommonRoot() {
    	ArrayList<String> urlList = new ArrayList<String>();
    	for (IResource resource : resources) {
    		ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
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

}
