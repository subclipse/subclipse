package org.tigris.subversion.subclipse.ui.wizards;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.dialogs.ChooseUrlDialog;
import org.tigris.subversion.subclipse.ui.util.UrlCombo;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class BranchTagWizardRepositoryPage extends SVNWizardPage {
    private UrlCombo toUrlCombo;
    protected Button makeParentsButton;
    private IResource resource;
    private ISVNRemoteResource remoteResource;
    private ISVNLocalResource svnResource;
    private SVNUrl url;
    private IDialogSettings settings = SVNUIPlugin.getPlugin().getDialogSettings();
	
	public BranchTagWizardRepositoryPage() {
		super("repositoryPage", //$NON-NLS-1$
				Policy.bind("BranchTagWizardRepositoryPage.heading"), //$NON-NLS-1$
				SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_WIZBAN_SVN),
				Policy.bind("BranchTagWizardRepositoryPage.message")); //$NON-NLS-1$
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
		
		Group repositoryGroup = new Group(outerContainer, SWT.NULL);
		repositoryGroup.setText(Policy.bind("BranchTagDialog.repository")); //$NON-NLS-1$
		repositoryGroup.setLayout(new GridLayout());
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, false);
		repositoryGroup.setLayoutData(data);
		
		Label fromUrlLabel = new Label(repositoryGroup, SWT.NONE);
		if (resource == null) fromUrlLabel.setText(Policy.bind("BranchTagDialog.fromUrl")); //$NON-NLS-1$
		else fromUrlLabel.setText(Policy.bind("BranchTagDialog.url")); //$NON-NLS-1$
		
		Text urlText = new Text(repositoryGroup, SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		urlText.setLayoutData(data);
		
		if (resource == null) {
			url = remoteResource.getUrl();
			urlText.setText(url.toString());
		} else {
			svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
			try {
	            url = svnResource.getStatus().getUrl();
	            if (url != null) urlText.setText(svnResource.getStatus().getUrlString());
	        } catch (SVNException e1) {}
		}
		
        urlText.setEditable(false);
        
		Label toUrlLabel = new Label(repositoryGroup, SWT.NONE);
		toUrlLabel.setText(Policy.bind("BranchTagDialog.toUrl")); //$NON-NLS-1$   
		
		Composite urlComposite = new Composite(repositoryGroup, SWT.NULL);
		GridLayout urlLayout = new GridLayout();
		urlLayout.numColumns = 2;
		urlLayout.marginWidth = 0;
		urlLayout.marginHeight = 0;
		urlComposite.setLayout(urlLayout);
		data = new GridData(SWT.FILL, SWT.FILL, true, false);
		urlComposite.setLayoutData(data);
		
		toUrlCombo = new UrlCombo(urlComposite, SWT.NONE);
		toUrlCombo.init( resource == null ? "repositoryBrowser" : resource.getProject().getName()); //$NON-NLS-1$
		toUrlCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
		toUrlCombo.setText(urlText.getText());
		toUrlCombo.getCombo().addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				setPageComplete(canFinish());
			}		
		});
		
		Button browseButton = new Button(urlComposite, SWT.PUSH);
		browseButton.setText(Policy.bind("SwitchDialog.browse")); //$NON-NLS-1$
		browseButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                ChooseUrlDialog dialog = new ChooseUrlDialog(getShell(), resource);
                if ((dialog.open() == ChooseUrlDialog.OK) && (dialog.getUrl() != null)) {
                    toUrlCombo.setText(dialog.getUrl());
                }
            }
		});	
		
		makeParentsButton = new Button(urlComposite, SWT.CHECK);
		makeParentsButton.setText(Policy.bind("BranchTagDialog.makeParents")); //$NON-NLS-1$  
		data = new GridData();
		data.horizontalSpan = 2;
		makeParentsButton.setLayoutData(data);
		makeParentsButton.setSelection(settings.getBoolean("BranchTagDialog.makeParents")); //$NON-NLS-1$  
		makeParentsButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				settings.put("BranchTagDialog.makeParents", makeParentsButton.getSelection()); //$NON-NLS-1$ 
			}		
		});		
		
		toUrlCombo.getCombo().setFocus();
		
		setPageComplete(canFinish());

		setControl(outerContainer);
	}
	
	private boolean canFinish() {
		return toUrlCombo.getText().trim().length() > 0;
	}
	
	public SVNUrl getUrl() {
		return url;
	}
	
	public String getUrlText() {
		return toUrlCombo.getText().trim();
	}
	
	public void saveUrl() {
		toUrlCombo.saveUrl();
	}

	public ISVNLocalResource getSvnResource() {
		return svnResource;
	}

}
