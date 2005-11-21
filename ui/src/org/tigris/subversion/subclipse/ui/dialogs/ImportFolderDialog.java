package org.tigris.subversion.subclipse.ui.dialogs;

import java.io.File;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.comments.CommitCommentArea;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;

public class ImportFolderDialog extends Dialog {
	private ISVNRemoteFolder remoteFolder;
	private Text directoryText;
	private Button recurseButton;
	private CommitCommentArea commitCommentArea;
	private Button okButton;
	private boolean success;

	public ImportFolderDialog(Shell parentShell, ISVNRemoteFolder remoteFolder) {
		super(parentShell);
		this.remoteFolder = remoteFolder;
		commitCommentArea = new CommitCommentArea(this, null, Policy.bind("ImportFolderDialog.comment")); //$NON-NLS-1$
	}
	
	protected Control createDialogArea(Composite parent) {
		getShell().setText(Policy.bind("ImportFolderDialog.title")); //$NON-NLS-1$
		Composite composite = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(data);
		
		Group repositoryGroup = new Group(composite, SWT.NULL);
		repositoryGroup.setText(Policy.bind("ExportRemoteFolderDialog.repository")); //$NON-NLS-1$
		GridLayout repositoryLayout = new GridLayout();
		repositoryLayout.numColumns = 2;
		repositoryGroup.setLayout(repositoryLayout);
		data = new GridData(GridData.FILL_BOTH);
		repositoryGroup.setLayoutData(data);
		
		Label urlLabel = new Label(repositoryGroup, SWT.NONE);
		urlLabel.setText(Policy.bind("ExportRemoteFolderDialog.url"));
		data = new GridData();
		data.horizontalSpan = 2;
		urlLabel.setLayoutData(data);
		
		Text urlText = new Text(repositoryGroup, SWT.BORDER);
		data = new GridData();
		data.widthHint = 300;
		urlText.setLayoutData(data);
		urlText.setEditable(false);
		urlText.setText(remoteFolder.getUrl().toString());
		
		new Label(repositoryGroup, SWT.NONE);
		
		Label directoryLabel = new Label(repositoryGroup, SWT.NONE);
		directoryLabel.setText(Policy.bind("ImportFolderDialog.directory"));
		data = new GridData();
		data.horizontalSpan = 2;
		directoryLabel.setLayoutData(data);
		directoryText = new Text(repositoryGroup, SWT.BORDER);
		data = new GridData();
		data.widthHint = 300;
		directoryText.setLayoutData(data);
		directoryText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				setOkButtonStatus();		
			}			
		});
		
		Button directoryBrowseButton = new Button(repositoryGroup, SWT.PUSH);
		directoryBrowseButton.setText(Policy.bind("ExportRemoteFolderDialog.browse"));
		directoryBrowseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.SAVE);
				dialog.setText(Policy.bind("ImportFolderDialog.title"));
				String directory = dialog.open();
				if (directory != null) {
					directoryText.setText(directory);
					setOkButtonStatus();
				}
			}
		});
		
		recurseButton = new Button(composite, SWT.CHECK);
		recurseButton.setText(Policy.bind("ImportFolderDialog.recurse"));
		recurseButton.setSelection(true);
		
		commitCommentArea.createArea(composite);
		
		directoryText.setFocus();
		
		return composite;
	}
	
    protected Button createButton(Composite parent, int id, String label, boolean defaultButton) {
        Button button = super.createButton(parent, id, label, defaultButton);
		if (id == IDialogConstants.OK_ID) {
			okButton = button; 
			okButton.setEnabled(false);
		}
        return button;
    }
    
    private void setOkButtonStatus() {
    	okButton.setEnabled(directoryText.getText().trim().length() > 0);
    }

	protected void okPressed() {
		success = true;
		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			public void run() {
				try {
					ISVNClientAdapter client = SVNProviderPlugin.getPlugin().getSVNClientManager().createSVNClient();
					File directory = new File(directoryText.getText().trim());
					client.doImport(directory, remoteFolder.getUrl(), commitCommentArea.getComment(), recurseButton.getSelection());
					SVNProviderPlugin.getPlugin().getRepositoryResourcesManager().remoteResourceCreated(remoteFolder, null);
				} catch (Exception e) {
					MessageDialog.openError(getShell(), Policy.bind("ImportFolderDialog.title"), e.getMessage()); //$NON-NLS-1$
					success = false;					
				}
			}			
		});
		if (!success) return;
		super.okPressed();
	}

}
