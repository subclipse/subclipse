package org.tigris.subversion.subclipse.ui.dialogs;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.IDialogConstants;
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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.history.ILogEntry;
import org.tigris.subversion.subclipse.core.resources.RemoteFolder;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.compare.IPropertyProvider;
import org.tigris.subversion.subclipse.ui.compare.PropertyCompareInput;
import org.tigris.subversion.subclipse.ui.compare.PropertyCompareLocalResourceNode;
import org.tigris.subversion.subclipse.ui.compare.PropertyCompareRemoteResourceNode;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class ComparePropertiesDialog extends SvnDialog {
	private IPropertyProvider left;
	private IPropertyProvider right;
	
	private ISVNRepositoryLocation repository;
	
	private Button fromWorkingCopyButton;
	private Text fromWorkingCopyText;
	private Button fromWorkingCopyBrowseButton;
	private Button fromRepositoryButton;
	private Text fromRepositoryText;
	private Button fromRepositoryBrowseButton;
	private Button fromHeadButton;
	private Label fromRevisionLabel;
	private Text fromRevisionText;
	private Button fromRevisionBrowseButton;
	
	private Button toWorkingCopyButton;
	private Text toWorkingCopyText;
	private Button toWorkingCopyBrowseButton;
	private Button toRepositoryButton;
	private Text toRepositoryText;
	private Button toRepositoryBrowseButton;
	private Button toHeadButton;
	private Label toRevisionLabel;
	private Text toRevisionText;
	private Button toRevisionBrowseButton;
	
	private Button recursiveButton;
	
	private PropertyCompareInput input;
	
	private Button okButton;
	
	private IResource fromLocalResource;
	private IResource toLocalResource;

	public ComparePropertiesDialog(Shell shell, IPropertyProvider left, IPropertyProvider right) {
		super(shell, "ComparePropertiesDialog2"); //$NON-NLS-1$
		this.left = left;
		this.right = right;
	}
	
	protected Control createDialogArea(Composite parent) {
		getShell().setText(Policy.bind("ComparePropertiesDialog.1")); //$NON-NLS-1$
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Group fromGroup = new Group(composite, SWT.NULL);
		fromGroup.setText(Policy.bind("ShowDifferencesAsUnifiedDiffDialog.compareFrom")); //$NON-NLS-1$
		GridLayout fromLayout = new GridLayout();
		fromLayout.numColumns = 3;
		fromGroup.setLayout(fromLayout);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, false);
		fromGroup.setLayoutData(data);
		
		fromWorkingCopyButton = new Button(fromGroup, SWT.RADIO);
		fromWorkingCopyButton.setText(Policy.bind("ComparePropertiesDialog.2")); //$NON-NLS-1$
		fromWorkingCopyText = new Text(fromGroup, SWT.BORDER | SWT.READ_ONLY);
		data = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		data.widthHint = 600;
		fromWorkingCopyText.setLayoutData(data);
		
		fromWorkingCopyBrowseButton = new Button(fromGroup, SWT.PUSH);
		fromWorkingCopyBrowseButton.setText(Policy.bind("ComparePropertiesDialog.3")); //$NON-NLS-1$
		
		fromRepositoryButton = new Button(fromGroup, SWT.RADIO);
		fromRepositoryButton.setText(Policy.bind("ComparePropertiesDialog.4")); //$NON-NLS-1$
		fromRepositoryText = new Text(fromGroup, SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		data.widthHint = 600;
		fromRepositoryText.setLayoutData(data);
		
		fromRepositoryBrowseButton = new Button(fromGroup, SWT.PUSH);
		fromRepositoryBrowseButton.setText(Policy.bind("ComparePropertiesDialog.3")); //$NON-NLS-1$
		
		fromHeadButton = new Button(fromGroup, SWT.CHECK);
		data = new GridData();
		data.horizontalSpan = 3;
		fromHeadButton.setLayoutData(data);
		fromHeadButton.setText(Policy.bind("ComparePropertiesDialog.6")); //$NON-NLS-1$
		fromHeadButton.setSelection(true);
		
		Composite fromRevisionGroup = new Composite(fromGroup, SWT.NONE);
		GridLayout fromRevisionLayout = new GridLayout();
		fromRevisionLayout.numColumns = 3;
		fromRevisionLayout.marginHeight = 0;
		fromRevisionLayout.marginWidth = 0;
		fromRevisionGroup.setLayout(fromRevisionLayout);
		data = new GridData();
		data.horizontalSpan = 3;
		fromRevisionGroup.setLayoutData(data);
		
		fromRevisionLabel = new Label(fromRevisionGroup, SWT.NONE);
		fromRevisionLabel.setText(Policy.bind("ComparePropertiesDialog.7")); //$NON-NLS-1$
		fromRevisionLabel.setEnabled(false);
		
		fromRevisionText = new Text(fromRevisionGroup, SWT.BORDER);
		data = new GridData();
		data.widthHint = 40;
		fromRevisionText.setLayoutData(data);
		fromRevisionText.setEnabled(false);
		
		fromRevisionBrowseButton = new Button(fromRevisionGroup, SWT.PUSH);
		fromRevisionBrowseButton.setText(Policy.bind("ComparePropertiesDialog.3")); //$NON-NLS-1$
		fromRevisionBrowseButton.setEnabled(false);
		
		Group toGroup = new Group(composite, SWT.NULL);
		toGroup.setText(Policy.bind("ShowDifferencesAsUnifiedDiffDialog.compareTo")); //$NON-NLS-1$
		GridLayout toLayout = new GridLayout();
		toLayout.numColumns = 3;
		toGroup.setLayout(toLayout);
		data = new GridData(SWT.FILL, SWT.FILL, true, false);
		toGroup.setLayoutData(data);
		
		toWorkingCopyButton = new Button(toGroup, SWT.RADIO);
		toWorkingCopyButton.setText(Policy.bind("ComparePropertiesDialog.2")); //$NON-NLS-1$
		toWorkingCopyText = new Text(toGroup, SWT.BORDER | SWT.READ_ONLY);
		data = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		data.widthHint = 600;
		toWorkingCopyText.setLayoutData(data);
		
		toWorkingCopyBrowseButton = new Button(toGroup, SWT.PUSH);
		toWorkingCopyBrowseButton.setText(Policy.bind("ComparePropertiesDialog.3")); //$NON-NLS-1$
		
		toRepositoryButton = new Button(toGroup, SWT.RADIO);
		toRepositoryButton.setText(Policy.bind("ComparePropertiesDialog.4")); //$NON-NLS-1$
		toRepositoryText = new Text(toGroup, SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		data.widthHint = 600;
		toRepositoryText.setLayoutData(data);
		
		toRepositoryBrowseButton = new Button(toGroup, SWT.PUSH);
		toRepositoryBrowseButton.setText(Policy.bind("ComparePropertiesDialog.3")); //$NON-NLS-1$
		
		toHeadButton = new Button(toGroup, SWT.CHECK);
		data = new GridData();
		data.horizontalSpan = 3;
		toHeadButton.setLayoutData(data);
		toHeadButton.setText(Policy.bind("ComparePropertiesDialog.6")); //$NON-NLS-1$
		toHeadButton.setSelection(true);
		
		Composite toRevisionGroup = new Composite(toGroup, SWT.NONE);
		GridLayout toRevisionLayout = new GridLayout();
		toRevisionLayout.numColumns = 3;
		toRevisionLayout.marginHeight = 0;
		toRevisionLayout.marginWidth = 0;
		toRevisionGroup.setLayout(toRevisionLayout);
		data = new GridData();
		data.horizontalSpan = 3;
		toRevisionGroup.setLayoutData(data);
		
		toRevisionLabel = new Label(toRevisionGroup, SWT.NONE);
		toRevisionLabel.setText(Policy.bind("ComparePropertiesDialog.7")); //$NON-NLS-1$
		toRevisionLabel.setEnabled(false);
		
		toRevisionText = new Text(toRevisionGroup, SWT.BORDER);
		data = new GridData();
		data.widthHint = 40;
		toRevisionText.setLayoutData(data);
		toRevisionText.setEnabled(false);
		
		toRevisionBrowseButton = new Button(toRevisionGroup, SWT.PUSH);
		toRevisionBrowseButton.setText(Policy.bind("ComparePropertiesDialog.3")); //$NON-NLS-1$
		toRevisionBrowseButton.setEnabled(false);
		
		recursiveButton = new Button(composite, SWT.CHECK);
		recursiveButton.setText(Policy.bind("ComparePropertiesDialog.16")); //$NON-NLS-1$
		
		if (left != null) {
			if (left instanceof PropertyCompareLocalResourceNode) {
				fromWorkingCopyText.setText(((PropertyCompareLocalResourceNode)left).getResource().getFullPath().toString());
				fromWorkingCopyButton.setSelection(true);
				fromRepositoryText.setEnabled(false);
				fromRepositoryBrowseButton.setEnabled(false);
				fromHeadButton.setEnabled(false);
				fromLocalResource = ((PropertyCompareLocalResourceNode)left).getResource();
			}
			else if (left instanceof PropertyCompareRemoteResourceNode) {
				fromRepositoryText.setText(((PropertyCompareRemoteResourceNode)left).getRemoteResource().getUrl().toString());
				fromRepositoryButton.setSelection(true);
				fromWorkingCopyText.setEnabled(false);
				fromWorkingCopyBrowseButton.setEnabled(false);
				fromHeadButton.setEnabled(true);
			}
		}
		
		if (right == null) {
			right = left;
		}
		
		if (right != null) {
			if (right instanceof PropertyCompareLocalResourceNode) {
				toWorkingCopyText.setText(((PropertyCompareLocalResourceNode)right).getResource().getFullPath().toString());
				toWorkingCopyButton.setSelection(true);
				toRepositoryText.setEnabled(false);
				toRepositoryBrowseButton.setEnabled(false);
				toHeadButton.setEnabled(false);
				repository = SVNWorkspaceRoot.getSVNResourceFor(((PropertyCompareLocalResourceNode)right).getResource()).getRepository();
				toLocalResource = ((PropertyCompareLocalResourceNode)right).getResource();
			}
			else if (right instanceof PropertyCompareRemoteResourceNode) {
				toRepositoryText.setText(((PropertyCompareRemoteResourceNode)right).getRemoteResource().getUrl().toString());
				toRepositoryButton.setSelection(true);
				toWorkingCopyText.setEnabled(false);
				toWorkingCopyBrowseButton.setEnabled(false);
				toHeadButton.setEnabled(true);
				repository = ((PropertyCompareRemoteResourceNode)right).getRemoteResource().getRepository();
			}
		}
		
		ModifyListener modifyListener = new ModifyListener() {			
			public void modifyText(ModifyEvent e) {
				okButton.setEnabled(canFinish());
			}
		};
		
		fromWorkingCopyText.addModifyListener(modifyListener);
		fromRepositoryText.addModifyListener(modifyListener);
		fromRevisionText.addModifyListener(modifyListener);
		toWorkingCopyText.addModifyListener(modifyListener);
		toRepositoryText.addModifyListener(modifyListener);
		toRevisionText.addModifyListener(modifyListener);
		
		SelectionListener selectionListener = new SelectionAdapter() {	
			public void widgetSelected(SelectionEvent e) {			
				if (e.getSource() == fromWorkingCopyBrowseButton || e.getSource() == toWorkingCopyBrowseButton) {	
					ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(getShell(), new WorkbenchLabelProvider(), new BaseWorkbenchContentProvider());
					dialog.setTitle(Policy.bind("ComparePropertiesDialog.1")); //$NON-NLS-1$
					dialog.setMessage(Policy.bind("ComparePropertiesDialog.18")); //$NON-NLS-1$
					dialog.setInput(ResourcesPlugin.getWorkspace().getRoot());
					if (dialog.open() != ElementTreeSelectionDialog.CANCEL) {
						Object result = dialog.getFirstResult();
						if (result instanceof IResource) {
							if (e.getSource() == fromWorkingCopyBrowseButton) {
								fromWorkingCopyText.setText(((IResource)result).getFullPath().toString());
								fromLocalResource = (IResource)result;
							}
							else {
								toWorkingCopyText.setText(((IResource)result).getFullPath().toString());
								toLocalResource = (IResource)result;
							}
						}
					}
				}
				else if (e.getSource() == fromRepositoryBrowseButton || e.getSource() == toRepositoryBrowseButton) {
					ChooseUrlDialog dialog = new ChooseUrlDialog(getShell(), null);
					dialog.setRepositoryLocation(repository);
					if (dialog.open() != ChooseUrlDialog.CANCEL) {
						String url = dialog.getUrl();
						if (url != null) {
							if (e.getSource() == fromRepositoryBrowseButton) {
								fromRepositoryText.setText(url);
							}
							else {
								toRepositoryText.setText(url);
							}
						}
					}
				}
				else if (e.getSource() == fromRevisionBrowseButton || e.getSource() == toRevisionBrowseButton) {
					try {
						SVNUrl url = null;
						if (e.getSource() == fromRevisionBrowseButton) {
							url = new SVNUrl(fromRepositoryText.getText().trim());
						}
						else {
							url = new SVNUrl(toRepositoryText.getText().trim());
						}
						ISVNRemoteResource remoteResource = repository.getRemoteFile(url);
						HistoryDialog dialog = new HistoryDialog(getShell(), remoteResource);
				        if (dialog.open() != HistoryDialog.CANCEL) {
					        ILogEntry[] selectedEntries = dialog.getSelectedLogEntries();
					        if (selectedEntries.length > 0) {	
					        	if (e.getSource() == fromRevisionBrowseButton) {
					        		fromRevisionText.setText(Long.toString(selectedEntries[selectedEntries.length - 1].getRevision().getNumber()));
					        	}
					        	else {
					        		toRevisionText.setText(Long.toString(selectedEntries[selectedEntries.length - 1].getRevision().getNumber()));
					        	}
					        }
				        }
					} catch (Exception exc) {
						MessageDialog.openError(getShell(), Policy.bind("ComparePropertiesDialog.1"), exc.getMessage()); //$NON-NLS-1$
					}
				}
				
				setEnablement();			
				okButton.setEnabled(canFinish());
			}
		};
		
		fromWorkingCopyButton.addSelectionListener(selectionListener);
		fromWorkingCopyBrowseButton.addSelectionListener(selectionListener);
		fromRepositoryButton.addSelectionListener(selectionListener);
		fromRepositoryBrowseButton.addSelectionListener(selectionListener);
		fromHeadButton.addSelectionListener(selectionListener);
		fromRevisionBrowseButton.addSelectionListener(selectionListener);
		toWorkingCopyButton.addSelectionListener(selectionListener);
		toWorkingCopyBrowseButton.addSelectionListener(selectionListener);
		toRepositoryButton.addSelectionListener(selectionListener);
		toRepositoryBrowseButton.addSelectionListener(selectionListener);
		toHeadButton.addSelectionListener(selectionListener);
		toRevisionBrowseButton.addSelectionListener(selectionListener);
		
		return composite;
	}
	
	@Override
	protected void okPressed() {
		try {
			if (fromWorkingCopyButton.getSelection()) {
				left = new PropertyCompareLocalResourceNode(fromLocalResource, recursiveButton.getSelection(), null);
			}
			else {
				SVNRevision revision = null;
				if (fromHeadButton.getSelection()) {
					revision = SVNRevision.HEAD;
				}
				else {
					revision = new SVNRevision.Number(Long.parseLong(fromRevisionText.getText()));
				}
				RemoteFolder remoteFolder = new RemoteFolder(repository, new SVNUrl(fromRepositoryText.getText().trim()), revision);
				left = new PropertyCompareRemoteResourceNode(remoteFolder, revision, recursiveButton.getSelection(), null);
			}
			if (toWorkingCopyButton.getSelection()) {
				right = new PropertyCompareLocalResourceNode(toLocalResource, recursiveButton.getSelection(), null);
			}
			else {
				SVNRevision revision = null;
				if (toHeadButton.getSelection()) {
					revision = SVNRevision.HEAD;
				}
				else {
					revision = new SVNRevision.Number(Long.parseLong(toRevisionText.getText()));
				}
				RemoteFolder remoteFolder = new RemoteFolder(repository, new SVNUrl(toRepositoryText.getText().trim()), revision);
				right = new PropertyCompareRemoteResourceNode(remoteFolder, revision, recursiveButton.getSelection(), null);
			}
			input = new PropertyCompareInput(left, right, recursiveButton.getSelection());	
		} catch (Exception e) {
			MessageDialog.openError(getShell(), Policy.bind("ComparePropertiesDialog.1"), e.getMessage()); //$NON-NLS-1$
			return;
		}
		super.okPressed();
	}

	protected Button createButton(Composite parent, int id, String label, boolean defaultButton) {
        Button button = super.createButton(parent, id, label, defaultButton);
		if (id == IDialogConstants.OK_ID) {
			okButton = button; 
			okButton.setEnabled(left != null);
		}
        return button;
    }
	
	public PropertyCompareInput getInput() {
		return input;
	}

	private boolean canFinish() {
		if (fromWorkingCopyButton.getSelection() && fromWorkingCopyText.getText().trim().length() == 0) {
			return false;
		}
		else if (fromRepositoryButton.getSelection()) {
			if (fromRepositoryText.getText().trim().length() == 0) {
				return false;
			}
			if (!fromHeadButton.getSelection()) {
				if (fromRevisionText.getText().trim().length() == 0) {
					return false;
				}
			}
		}
		if (toWorkingCopyButton.getSelection() && toWorkingCopyText.getText().trim().length() == 0) {
			return false;
		}
		else if (toRepositoryButton.getSelection()) {
			if (toRepositoryText.getText().trim().length() == 0) {
				return false;
			}
			if (!toHeadButton.getSelection()) {
				if (toRevisionText.getText().trim().length() == 0) {
					return false;
				}
			}
		}
		return true;
	}

	private void setEnablement() {
		fromWorkingCopyText.setEnabled(fromWorkingCopyButton.getSelection());
		fromWorkingCopyBrowseButton.setEnabled(fromWorkingCopyButton.getSelection());
		fromRepositoryText.setEnabled(fromRepositoryButton.getSelection());
		fromRepositoryBrowseButton.setEnabled(fromRepositoryButton.getSelection());
		fromHeadButton.setEnabled(fromRepositoryButton.getSelection());
		fromRevisionLabel.setEnabled(fromRepositoryButton.getSelection() && !fromHeadButton.getSelection());
		fromRevisionText.setEnabled(fromRepositoryButton.getSelection() && !fromHeadButton.getSelection());
		fromRevisionBrowseButton.setEnabled(fromRepositoryButton.getSelection() && !fromHeadButton.getSelection() && fromRepositoryText.getText().trim().length() > 0);			
		toWorkingCopyText.setEnabled(toWorkingCopyButton.getSelection());
		toWorkingCopyBrowseButton.setEnabled(toWorkingCopyButton.getSelection());
		toRepositoryText.setEnabled(toRepositoryButton.getSelection());
		toRepositoryBrowseButton.setEnabled(toRepositoryButton.getSelection());
		toHeadButton.setEnabled(toRepositoryButton.getSelection());
		toRevisionLabel.setEnabled(toRepositoryButton.getSelection() && !toHeadButton.getSelection());
		toRevisionText.setEnabled(toRepositoryButton.getSelection() && !toHeadButton.getSelection());
		toRevisionBrowseButton.setEnabled(toRepositoryButton.getSelection() && !toHeadButton.getSelection() && toRepositoryText.getText().trim().length() > 0);
	}

}
