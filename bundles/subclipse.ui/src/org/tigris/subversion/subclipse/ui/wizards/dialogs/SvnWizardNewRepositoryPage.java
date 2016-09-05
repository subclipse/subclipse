package org.tigris.subversion.subclipse.ui.wizards.dialogs;

import java.io.File;
import java.util.Properties;

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
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.wizards.CloudForgeComposite;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;

public class SvnWizardNewRepositoryPage extends SvnWizardDialogPage {
	private Text folderText;
	private Button connectionButton;
	private boolean success;

	public SvnWizardNewRepositoryPage() {
		super("NewRepositoryDialog", Policy.bind("NewRepositoryDialog.title")); //$NON-NLS-1$ //$NON-NLS-2$		  
	}

	public void createButtonsForButtonBar(Composite parent, SvnWizardDialog wizardDialog) {
	}

	public void createControls(Composite outerContainer) {
		Composite composite = new Composite(outerContainer, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(data);
		
		Label folderLabel = new Label(composite, SWT.NONE);
		folderLabel.setText(Policy.bind("NewRepositoryDialog.folder")); //$NON-NLS-1$
		data = new GridData();
		data.horizontalSpan = 2;
		folderLabel.setLayoutData(data);
		
		folderText = new Text(composite, SWT.BORDER);
		data = new GridData();
		data.widthHint = 450;
		folderText.setLayoutData(data);
		
		folderText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				setPageComplete(canFinish());
			}			
		});
		
		Button browseButton = new Button(composite, SWT.PUSH);
		browseButton.setText(Policy.bind("NewRepositoryDialog.browse")); //$NON-NLS-1$
		browseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dialog = new DirectoryDialog(getShell());
				dialog.setText(Policy.bind("NewRepositoryDialog.browseTitle")); //$NON-NLS-1$
				dialog.setMessage(Policy.bind("NewRepositoryDialog.browseMessage")); //$NON-NLS-1$
				String directory = dialog.open();
				if (directory != null) folderText.setText(directory);
			}
		});
		
		Label spacer = new Label(composite, SWT.NONE);
		data = new GridData();
		data.horizontalSpan = 2;
		spacer.setLayoutData(data);
		
		connectionButton = new Button(composite, SWT.CHECK);
		connectionButton.setText(Policy.bind("NewRepositoryDialog.connection")); //$NON-NLS-1$
		data = new GridData();
		data.horizontalSpan = 2;
		connectionButton.setLayoutData(data);
		connectionButton.setSelection(true);
		
        Composite cloudForgeComposite = new CloudForgeComposite(composite, SWT.NONE);
        data = new GridData(GridData.VERTICAL_ALIGN_END | GridData.GRAB_VERTICAL | GridData.FILL_VERTICAL);
        data.horizontalSpan = 2;
        cloudForgeComposite.setLayoutData(data);
		
		setPageComplete(false);
		
		folderText.setFocus();
	}

	public String getWindowTitle() {
		return Policy.bind("NewRepositoryDialog.title"); //$NON-NLS-1$
	}

	public boolean performCancel() {
		return true;
	}

	public boolean performFinish() {
		success = true;
		BusyIndicator.showWhile(Display.getDefault(), new Runnable() {
			public void run() {
				ISVNClientAdapter svnClient = null;
				try {
					SVNProviderPlugin provider = SVNProviderPlugin.getPlugin();
					String url = getUrl();
					if (provider.getRepositories().isKnownRepository(url, true)) {
						MessageDialog.openError(getShell(), Policy.bind("NewRepositoryDialog.title"), Policy.bind("NewRepositoryDialog.alreadyExists")); //$NON-NLS-1$						
						success = false;
						return;
					}
					svnClient = SVNProviderPlugin.getPlugin().getSVNClient();
					File path = new File(folderText.getText().trim());
					if (!path.exists()) path.mkdirs();
					svnClient.createRepository(path, ISVNClientAdapter.REPOSITORY_FSTYPE_FSFS);
					if (connectionButton.getSelection()) {
						Properties properties = new Properties();
						properties.setProperty("url", url); //$NON-NLS-1$
						ISVNRepositoryLocation repository = provider.getRepositories().createRepository(properties);
						provider.getRepositories().addOrUpdateRepository(repository);
					}
				} catch (Exception e) {
					MessageDialog.openError(getShell(), Policy.bind("NewRepositoryDialog.title"), e.getLocalizedMessage()); //$NON-NLS-1$
					success = false;
				} finally {
					SVNProviderPlugin.getPlugin().getSVNClientManager().returnSVNClient(svnClient);
				}
			}			
		});

		return success;
	}
	
	private String getUrl() {
		String url = null;
		if (folderText.getText().startsWith("/"))
			url = "file://" + folderText.getText().trim().replaceAll("\\\\", "/"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		else
			url = "file:///" + folderText.getText().trim().replaceAll("\\\\", "/"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return url;
	}

	public void saveSettings() {
	}

	public void setMessage() {
		setMessage(Policy.bind("NewRepositoryDialog.message")); //$NON-NLS-1$
	}
	
	private boolean canFinish() {
		if (folderText.getText().trim().length() == 0) return false;
		File file = new File(folderText.getText().trim());
		if (!file.isAbsolute()) return false;
		if (file.exists() && !file.isDirectory()) return false;
		if (!file.exists()) {
			File parent = file.getParentFile();
			if (parent == null || !parent.exists()) return false;
		}
		return true;
	}

}
