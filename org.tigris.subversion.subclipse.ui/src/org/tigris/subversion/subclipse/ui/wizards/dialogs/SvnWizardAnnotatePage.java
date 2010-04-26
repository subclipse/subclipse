package org.tigris.subversion.subclipse.ui.wizards.dialogs;

import java.text.ParseException;

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
import org.tigris.subversion.subclipse.core.ISVNRemoteFile;
import org.tigris.subversion.subclipse.core.history.ILogEntry;
import org.tigris.subversion.subclipse.core.resources.RemoteResource;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.dialogs.HistoryDialog;
import org.tigris.subversion.svnclientadapter.SVNRevision;

public class SvnWizardAnnotatePage extends SvnWizardDialogPage {
	private ISVNRemoteFile svnResource;
	
	private Button includeMergedRevisionsButton;
	private Button ignoreMimeTypeButton;
	private Text fromRevisionText;
	private Button selectFromRevisionButton;
	private Button toHeadButton;
	private Button toRevisionButton;
	private Text toRevisionText;
	private Button selectToRevisionButton;
	
	private boolean includeMergedRevisions;
	private boolean ignoreMimeType;
	private SVNRevision fromRevision;
	private SVNRevision toRevision;
	
	private IDialogSettings settings = SVNUIPlugin.getPlugin().getDialogSettings();

	public SvnWizardAnnotatePage(ISVNRemoteFile svnResource) {
		super("AnnotateDialog", Policy.bind("AnnotateDialog.title")); //$NON-NLS-1$ //$NON-NLS-2$
		this.svnResource = svnResource;
	}

	public void createButtonsForButtonBar(Composite parent, SvnWizardDialog wizardDialog) {
	}

	public void createControls(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(data);
		
		includeMergedRevisionsButton = new Button(composite, SWT.CHECK);
		includeMergedRevisionsButton.setText(Policy.bind("AnnotateDialog.includeMerged")); //$NON-NLS-1$
		includeMergedRevisionsButton.setSelection(settings.getBoolean("AnnotateDialog.includeMerged")); //$NON-NLS-1$
		includeMergedRevisionsButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent arg0) {
				settings.put("AnnotateDialog.includeMerged", includeMergedRevisionsButton.getSelection()); //$NON-NLS-1$
			}			
		});
		
		ignoreMimeTypeButton = new Button(composite, SWT.CHECK);
		ignoreMimeTypeButton.setText(Policy.bind("AnnotateDialog.ignoreMimeType")); //$NON-NLS-1$
		
		Group fromGroup = new Group(composite, SWT.NONE);
		fromGroup.setText(Policy.bind("AnnotateDialog.from")); //$NON-NLS-1$
		GridLayout fromLayout = new GridLayout();
		fromLayout.numColumns = 3;
		fromGroup.setLayout(fromLayout);
		fromGroup.setLayoutData(
		new GridData(GridData.FILL_BOTH));
		
	    Label fromRevisionLabel = new Label(fromGroup, SWT.NONE);
	    fromRevisionLabel.setText(Policy.bind("AnnotateDialog.revision")); //$NON-NLS-1$
		
		fromRevisionText = new Text(fromGroup, SWT.BORDER);
		data = new GridData();
		data.widthHint = 40;
		fromRevisionText.setLayoutData(data);
		fromRevisionText.setText("1"); //$NON-NLS-1$
		
		selectFromRevisionButton = new Button(fromGroup, SWT.PUSH);
		selectFromRevisionButton.setText(Policy.bind("AnnotateDialog.showLog")); //$NON-NLS-1$
		
		Group toGroup = new Group(composite, SWT.NONE);
		toGroup.setText(Policy.bind("AnnotateDialog.to")); //$NON-NLS-1$
		GridLayout toLayout = new GridLayout();
		toLayout.numColumns = 5;
		toGroup.setLayout(toLayout);
		toGroup.setLayoutData(
		new GridData(GridData.FILL_BOTH));
		
		toHeadButton = new Button(toGroup, SWT.RADIO);
		toHeadButton.setText(Policy.bind("AnnotateDialog.head")); //$NON-NLS-1$
		
		toRevisionButton = new Button(toGroup, SWT.RADIO);
		toRevisionButton.setText(Policy.bind("AnnotateDialog.toRevision")); //$NON-NLS-1$
		
		toRevisionText = new Text(toGroup, SWT.BORDER);
		data = new GridData();
		data.widthHint = 40;
		toRevisionText.setLayoutData(data);
		toRevisionText.setEnabled(false);
		
		if (toRevision == null) {
			if (svnResource instanceof RemoteResource) {
				if (((RemoteResource)svnResource).getPegRevision() != null) {
					toRevision = ((RemoteResource)svnResource).getPegRevision();
				}
			}
		}
		
		if (toRevision == null) toHeadButton.setSelection(true);
		else {
			toRevisionText.setText(toRevision.toString());
			toRevisionButton.setSelection(true);
			toRevisionText.setEnabled(true);
		}
		
		selectToRevisionButton = new Button(toGroup, SWT.PUSH);
		selectToRevisionButton.setText(Policy.bind("AnnotateDialog.showToLog")); //$NON-NLS-1$
		
		SelectionListener selectionListener = getSelectionListener();
		selectFromRevisionButton.addSelectionListener(selectionListener);
		toHeadButton.addSelectionListener(selectionListener);
		toRevisionButton.addSelectionListener(selectionListener);
		selectToRevisionButton.addSelectionListener(selectionListener);
		
		ModifyListener modifyListener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				setPageComplete(canFinish());
			}		
		};
		fromRevisionText.addModifyListener(modifyListener);
		toRevisionText.addModifyListener(modifyListener);
		
		FocusListener focusListener = new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				((Text)e.getSource()).selectAll();
			}
			public void focusLost(FocusEvent e) {
				((Text)e.getSource()).setText(((Text)e.getSource()).getText());
			}					
		};
		fromRevisionText.addFocusListener(focusListener);
		toRevisionText.addFocusListener(focusListener);

	}

	public String getWindowTitle() {
		return Policy.bind("AnnotateDialog.title"); //$NON-NLS-1$
	}

	public boolean performCancel() {
		return true;
	}

	public boolean performFinish() {
		includeMergedRevisions = includeMergedRevisionsButton.getSelection();
		ignoreMimeType = ignoreMimeTypeButton.getSelection();
        try {
            fromRevision = SVNRevision.getRevision(fromRevisionText.getText().trim());
        } catch (ParseException e) {
        	MessageDialog.openError(getShell(), Policy.bind("AnnotateDialog.title"), e.getMessage());
        	return false;
        }
        if (toHeadButton.getSelection()) toRevision = SVNRevision.HEAD;
        else {
            try {
                toRevision = SVNRevision.getRevision(toRevisionText.getText().trim());
            } catch (ParseException e) {
            	MessageDialog.openError(getShell(), Policy.bind("AnnotateDialog.title"), e.getMessage());
            	return false;
            }
        }		
		return true;
	}

	public void saveSettings() {
	}

	public void setMessage() {
		setMessage(Policy.bind("AnnotateDialog.message", svnResource.getName())); //$NON-NLS-1$
	}
	
	public SVNRevision getFromRevision() {
		return fromRevision;
	}
	
	public SVNRevision getToRevision() {
		return toRevision;
	}
	
	public void setToRevision(SVNRevision toRevision) {
		this.toRevision = toRevision;
	}
	
	public boolean isIncludeMergedRevisions() {
		return includeMergedRevisions;
	}
	
	public boolean isIgnoreMimeType() {
		return ignoreMimeType;
	}
	
	private boolean canFinish() {
		setErrorMessage(null);
		if (fromRevisionText.getText().trim().length() == 0) return false;
		if (!validateRevision(fromRevisionText.getText().trim())) {
			setErrorMessage(Policy.bind("AnnotateDialog.invalidFromRevision")); //$NON-NLS-1$
			return false;
		}
		if (toRevisionButton.getSelection() && toRevisionText.getText().trim().length() == 0) return false;	
		if (toRevisionButton.getSelection() && !validateRevision(toRevisionText.getText().trim())) {
			setErrorMessage(Policy.bind("AnnotateDialog.invalidToRevision")); //$NON-NLS-1$
			return false;
		}				
		return true;
	}
	
	private boolean validateRevision(String revision) {
        try {
            SVNRevision.getRevision(revision);
        } catch (ParseException e1) {
          return false;
        }		
        return true;
	}
	
	private SelectionListener getSelectionListener() {
		SelectionListener selectionListener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (e.getSource() == selectFromRevisionButton) {
					showLog(fromRevisionText);
				}
				else if (e.getSource() == toHeadButton || e.getSource() == toRevisionButton) {
					toRevisionText.setEnabled(toRevisionButton.getSelection());
					selectToRevisionButton.setEnabled(toRevisionButton.getSelection());
					if (toRevisionButton.getSelection()) toRevisionText.setFocus();					
				}
				else if (e.getSource() == selectToRevisionButton) {
					showLog(toRevisionText);					
				}
				setPageComplete(canFinish());
			}	
		};
		return selectionListener;
	}
	
	private void showLog(Text text) {
        HistoryDialog dialog = new HistoryDialog(getShell(), svnResource);
        if (dialog.open() == HistoryDialog.CANCEL) return;
        ILogEntry[] selectedEntries = dialog.getSelectedLogEntries();
        if (selectedEntries.length == 0) return;
        if ((text == fromRevisionText) || ((text == null) && (fromRevisionText.getText().trim().length() == 0))) {
            fromRevisionText.setText(Long.toString(selectedEntries[0].getRevision().getNumber()));
            fromRevisionText.setEnabled(true);
        }
        if (text == toRevisionText) {
            toRevisionText.setText(Long.toString(selectedEntries[0].getRevision().getNumber()));
            toRevisionButton.setSelection(true);
            toHeadButton.setSelection(false);
            toRevisionText.setEnabled(true);
        } 
	}

}
