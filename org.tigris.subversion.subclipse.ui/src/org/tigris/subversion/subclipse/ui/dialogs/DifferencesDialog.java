package org.tigris.subversion.subclipse.ui.dialogs;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
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
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNResource;
import org.tigris.subversion.subclipse.core.history.ILogEntry;
import org.tigris.subversion.subclipse.core.resources.RemoteFile;
import org.tigris.subversion.subclipse.core.resources.RemoteFolder;
import org.tigris.subversion.subclipse.core.resources.RemoteResource;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.actions.CompareRemoteResourcesAction;
import org.tigris.subversion.subclipse.ui.operations.ShowDifferencesAsUnifiedDiffOperation;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class DifferencesDialog extends SvnDialog {
	private ISVNResource[] remoteResources;
	private SVNRevision[] pegRevisions;
	private String title;
	private IWorkbenchPart targetPart;
	private ISVNResource fromResource;
	private ISVNResource toResource;
	private Button compareButton;
	private Button diffButton;
	private Text fileText;
	private Button browseButton;
	private Text fromUrlText;
	private Button fromHeadButton;
	private Text fromRevisionText;
	private Button fromLogButton;
	private Text toUrlText;
	private Button toHeadButton;
	private Text toRevisionText;
	private Button toLogButton;
	private Button okButton;
	private boolean success;
	private String fromRevision;
	private String toRevision;
	private boolean usePegRevision;
	private SVNUrl fromUrl;
	private SVNUrl toUrl;
	
	private IResource localResource;
	
	public DifferencesDialog(Shell parentShell, String title, ISVNResource[] remoteResources, IWorkbenchPart targetPart) {
		this(parentShell, title, remoteResources, new SVNRevision[] { SVNRevision.HEAD, SVNRevision.HEAD }, targetPart);
	}

	public DifferencesDialog(Shell parentShell, String title, ISVNResource[] remoteResources, SVNRevision[] pegRevisions, IWorkbenchPart targetPart) {
		super(parentShell, "DifferencesDialog"); //$NON-NLS-1$
		this.title = title;
		this.remoteResources = remoteResources;
		this.pegRevisions = pegRevisions;
		this.targetPart = targetPart;
		fromResource = this.remoteResources[0];
		if (this.remoteResources.length == 1 || this.remoteResources[1] == null) {
			this.remoteResources = new ISVNResource[2];
			this.remoteResources[0] = fromResource;
			this.remoteResources[1] = fromResource;
		}
		toResource = this.remoteResources[1];
	}
	
	public void setLocalResource(IResource localResource) {
		this.localResource = localResource;
	}

	protected Control createDialogArea(Composite parent) {
		if (title == null) getShell().setText(Policy.bind("DifferencesDialog.compare")); //$NON-NLS-1$
		else getShell().setText(title);
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));

		Group fromGroup = new Group(composite, SWT.NULL);
		fromGroup.setText(Policy.bind("ShowDifferencesAsUnifiedDiffDialog.compareFrom")); //$NON-NLS-1$
		fromGroup.setLayout(new GridLayout(3, false));
		fromGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label fromUrlLabel = new Label(fromGroup, SWT.NONE);
		fromUrlLabel.setText(Policy.bind("ShowDifferencesAsUnifiedDiffDialog.url")); //$NON-NLS-1$
		fromUrlText = new Text(fromGroup, SWT.BORDER);
		fromUrlText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		if (fromUrl == null) {
			fromUrlText.setText(remoteResources[0].getUrl().toString());
		}
		else {
			fromUrlText.setText(fromUrl.toString());
		}

		Button bb = new Button(fromGroup, SWT.PUSH);
		bb.setText(Policy.bind("SwitchDialog.browse")); //$NON-NLS-1$
		bb.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                ChooseUrlDialog dialog = new ChooseUrlDialog(getShell(), fromResource.getResource());
                if ((dialog.open() == ChooseUrlDialog.OK) && (dialog.getUrl() != null)) {
                	fromUrlText.setText(dialog.getUrl());
                }
            }
		});	
		
		Composite fromRevisionGroup = new Composite(fromGroup, SWT.NULL);
		GridLayout fromRevisionLayout = new GridLayout();
		fromRevisionLayout.numColumns = 3;
		fromRevisionLayout.marginWidth = 0;
		fromRevisionLayout.marginHeight = 0;
		fromRevisionGroup.setLayout(fromRevisionLayout);
		fromRevisionGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 3, 1));

		fromHeadButton = new Button(fromRevisionGroup, SWT.CHECK);
		fromHeadButton.setText(Policy.bind("ShowDifferencesAsUnifiedDiffDialog.head")); //$NON-NLS-1$
		GridData data = new GridData();
		data.horizontalSpan = 3;
		fromHeadButton.setLayoutData(data);

		Label fromRevisionLabel = new Label(fromRevisionGroup, SWT.NONE);
		fromRevisionLabel.setText(Policy.bind("ShowDifferencesAsUnifiedDiffDialog.revision")); //$NON-NLS-1$		
		
		fromHeadButton.setSelection(true);
		
		fromRevisionText = new Text(fromRevisionGroup, SWT.BORDER);
		fromRevisionText.setLayoutData(new GridData(40, SWT.DEFAULT));
		fromRevisionText.setEnabled(false);
		
		fromLogButton = new Button(fromRevisionGroup, SWT.PUSH);
		fromLogButton.setText(Policy.bind("ShowDifferencesAsUnifiedDiffDialog.showLog")); //$NON-NLS-1$
		fromLogButton.setEnabled(false);
		fromLogButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                showLog(e.getSource());
            }
		});	
		
		Group toGroup = new Group(composite, SWT.NULL);
		toGroup.setText(Policy.bind("ShowDifferencesAsUnifiedDiffDialog.compareTo")); //$NON-NLS-1$
		toGroup.setLayout(new GridLayout(3, false));
		toGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label toUrlLabel = new Label(toGroup, SWT.NONE);
		toUrlLabel.setText(Policy.bind("ShowDifferencesAsUnifiedDiffDialog.url")); //$NON-NLS-1$
		toUrlText = new Text(toGroup, SWT.BORDER);
		toUrlText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		if (toUrl == null) {
			if (remoteResources.length < 2 || remoteResources[1] == null)
				toUrlText.setText(remoteResources[0].getUrl().toString());
			else
				toUrlText.setText(remoteResources[1].getUrl().toString());
		}
		else {
			toUrlText.setText(toUrl.toString());
		}

		bb = new Button(toGroup, SWT.PUSH);
		bb.setText(Policy.bind("SwitchDialog.browse")); //$NON-NLS-1$
		bb.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
            	IResource resouce = null;
            	if (remoteResources.length < 2 || remoteResources[1] == null)
            		resouce = remoteResources[0].getResource();
        		else
        			resouce = remoteResources[1].getResource();
                ChooseUrlDialog dialog = new ChooseUrlDialog(getShell(), resouce);
                if ((dialog.open() == ChooseUrlDialog.OK) && (dialog.getUrl() != null)) {
                	toUrlText.setText(dialog.getUrl());
                }
            }
		});	
		Composite toRevisionGroup = new Composite(toGroup, SWT.NULL);
		GridLayout toRevisionLayout = new GridLayout();
		toRevisionLayout.numColumns = 3;
		toRevisionLayout.marginWidth = 0;
		toRevisionLayout.marginHeight = 0;
		toRevisionGroup.setLayout(toRevisionLayout);
		toRevisionGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 3, 1));

		toHeadButton = new Button(toRevisionGroup, SWT.CHECK);
		toHeadButton.setText(Policy.bind("ShowDifferencesAsUnifiedDiffDialog.toHead")); //$NON-NLS-1$
		data = new GridData();
		data.horizontalSpan = 3;
		toHeadButton.setLayoutData(data);

		Label toRevisionLabel = new Label(toRevisionGroup, SWT.NONE);
		toRevisionLabel.setText(Policy.bind("ShowDifferencesAsUnifiedDiffDialog.toRevision")); //$NON-NLS-1$		
		
		toHeadButton.setSelection(true);
		
		toRevisionText = new Text(toRevisionGroup, SWT.BORDER);
		toRevisionText.setLayoutData(new GridData(40, SWT.DEFAULT));
		toRevisionText.setEnabled(false);
		
		toLogButton = new Button(toRevisionGroup, SWT.PUSH);
		toLogButton.setText(Policy.bind("ShowDifferencesAsUnifiedDiffDialog.showToLog")); //$NON-NLS-1$
		toLogButton.setEnabled(false);
		toLogButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                showLog(e.getSource());
            }
		});	
		
		if (fromRevision != null) {
			fromRevisionText.setText(fromRevision);
			fromRevisionText.setEnabled(true);
			fromLogButton.setEnabled(true);
			fromHeadButton.setSelection(false);
		}
		if (toRevision == null) {
			if (fromResource == toResource) {
				if (fromRevision == null) {
					ISVNRemoteResource resource = (ISVNRemoteResource)fromResource;
					String fromRev = resource.getLastChangedRevision().toString();
					int from = Integer.parseInt(fromRev);
					from--;
					toRevision = Integer.toString(from);
				} else {
					int from = Integer.parseInt(fromRevision);
					from--;
					toRevision = Integer.toString(from);
				}
			} else {
				ISVNRemoteResource resource = (ISVNRemoteResource)toResource;
				toRevision = resource.getLastChangedRevision().toString();	
			}
		}
		if (toRevision != null) {
			toRevisionText.setText(toRevision);
			toRevisionText.setEnabled(true);
			toLogButton.setEnabled(true);
			toHeadButton.setSelection(false);
		}
		
		Group fileGroup = new Group(composite, SWT.NULL);
		fileGroup.setText(Policy.bind("ShowDifferencesAsUnifiedDiffDialog.compareType")); //$NON-NLS-1$
		fileGroup.setLayout(new GridLayout(3, false));
		fileGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		compareButton = new Button(fileGroup, SWT.RADIO);
		compareButton.setText(Policy.bind("ShowDifferencesAsUnifiedDiffDialog.graphical")); //$NON-NLS-1$
		data = new GridData();
		data.horizontalSpan = 3;
		compareButton.setLayoutData(data);
		
		diffButton = new Button(fileGroup, SWT.RADIO);
		diffButton.setText(Policy.bind("DifferencesDialog.diff")); //$NON-NLS-1$
		
		compareButton.setSelection(true);
		
		fileText = new Text(fileGroup, SWT.BORDER);
		data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		data.widthHint = 450;
		fileText.setLayoutData(data);
		fileText.setEnabled(false);
		
		browseButton = new Button(fileGroup, SWT.PUSH);
		browseButton.setText(Policy.bind("ShowDifferencesAsUnifiedDiffDialog.browse")); //$NON-NLS-1$
		browseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);
				dialog.setText(Policy.bind("ShowDifferencesAsUnifiedDiffDialog.fileDialogText")); //$NON-NLS-1$
				dialog.setFileName("revision.diff"); //$NON-NLS-1$
				String outFile = dialog.open();
				if (outFile != null) fileText.setText(outFile);
			}		
		});		

		ModifyListener modifyListener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				setOkButtonStatus();
			}			
		};
		
		SelectionListener selectionListener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
                fromRevisionText.setEnabled(!fromHeadButton.getSelection());
                fromLogButton.setEnabled(!fromHeadButton.getSelection());
                toRevisionText.setEnabled(!toHeadButton.getSelection());
                toLogButton.setEnabled(!toHeadButton.getSelection());               
				setOkButtonStatus();
				if (e.getSource() == fromHeadButton && !fromHeadButton.getSelection()) {
                    fromRevisionText.selectAll();
                    fromRevisionText.setFocus();					
				}
				if (e.getSource() == toHeadButton && !toHeadButton.getSelection()) {
                    toRevisionText.selectAll();
                    toRevisionText.setFocus();					
				}				
			}
		};
		
		SelectionListener compareTypeListener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (diffButton.getSelection()) {
					fileText.setEnabled(true);
					browseButton.setEnabled(true);
					fileText.selectAll();
					fileText.setFocus();
				} else {
					fileText.setEnabled(false);
					browseButton.setEnabled(false);					
				}
				setOkButtonStatus();
			}
		};
		
		fileText.addModifyListener(modifyListener);
		fromRevisionText.addModifyListener(modifyListener);
		toRevisionText.addModifyListener(modifyListener);
		fromHeadButton.addSelectionListener(selectionListener);
		toHeadButton.addSelectionListener(selectionListener);
		
		compareButton.addSelectionListener(compareTypeListener);
		diffButton.addSelectionListener(compareTypeListener);
		
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
		fileText.addFocusListener(focusListener);
		
		// Set F1 Help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.SHOW_UNIFIED_DIFF_DIALOG);	
		
		return composite;
	}
	
    protected void createButtonsForButtonBar(Composite parent) {
		Button toggleFromToButton = createButton(parent, 2, Policy.bind("ShowDifferencesAsUnifiedDiffDialog.swap"), false); //$NON-NLS-1$
		toggleFromToButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				String fromUrl = fromUrlText.getText().trim();
				boolean fromHeadRevision = fromHeadButton.getSelection();
				String fromRevision = fromRevisionText.getText().trim();
				String toUrl = toUrlText.getText().trim();
				boolean toHeadRevision = toHeadButton.getSelection();
				String toRevision = toRevisionText.getText().trim();	
				fromUrlText.setText(toUrl);
				toUrlText.setText(fromUrl);
				if (toHeadRevision) {
					fromHeadButton.setSelection(true);
				}
				else {
					fromHeadButton.setSelection(false);
				}
				if (fromHeadRevision) {
					toHeadButton.setSelection(true);
				}
				else {
					toHeadButton.setSelection(false);
				}
				fromRevisionText.setText(toRevision);
				toRevisionText.setText(fromRevision);
				if (fromResource == remoteResources[0]) fromResource = remoteResources[1];
				else fromResource = remoteResources[0];
				fromRevisionText.setEnabled(!fromHeadButton.getSelection());
				toRevisionText.setEnabled(!toHeadButton.getSelection());
				setOkButtonStatus();
			}			
		});
		super.createButtonsForButtonBar(parent);
	}

	protected Button createButton(Composite parent, int id, String label, boolean defaultButton) {
        Button button = super.createButton(parent, id, label, defaultButton);
		if (id == IDialogConstants.OK_ID) {
			okButton = button; 
		}
        return button;
    }	
    
    protected void okPressed() {
    	success = true;
    	if (diffButton.getSelection()) diff();
    	if (compareButton.getSelection()) compare();
		if (!success) return;
		super.okPressed();
	}
    
    private void diff() {
		final File file = new File(fileText.getText().trim());
		if (file.exists()) {
			if (!MessageDialog.openQuestion(getShell(), Policy.bind("HistoryView.showDifferences"), Policy.bind("HistoryView.overwriteOutfile", file.getName()))) return;
		}	
		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			public void run() {
				try {
					SVNUrl fromUrl = null;
					SVNUrl toUrl = null;
					SVNRevision fromRevision;
					SVNRevision toRevision;
					if (fromHeadButton.getSelection()) fromRevision = SVNRevision.HEAD;
					else {
						int fromRevisionInt = Integer.parseInt(fromRevisionText.getText().trim());
						long fromRevisionLong = fromRevisionInt;
						fromRevision = new SVNRevision.Number(fromRevisionLong);
					}
					if (toHeadButton.getSelection()) toRevision = SVNRevision.HEAD;
					else {
						int toRevisionInt = Integer.parseInt(toRevisionText.getText().trim());
						long toRevisionLong = toRevisionInt;
						toRevision = new SVNRevision.Number(toRevisionLong);
					}
					fromUrl = new SVNUrl(fromUrlText.getText().trim());
					toUrl = new SVNUrl(toUrlText.getText().trim());
					ShowDifferencesAsUnifiedDiffOperation operation = new ShowDifferencesAsUnifiedDiffOperation(targetPart, fromUrl, fromRevision, toUrl, toRevision, file);
					operation.setLocalResource(remoteResources[0]);
					operation.run();
				} catch (Exception e) {
					MessageDialog.openError(getShell(), Policy.bind("HistoryView.showDifferences"), e.getMessage());
					success = false;
				}
			}			
		});    	
    }
    
    private void compare() {
    	if (fromResource instanceof ISVNRemoteResource && toResource != null && toResource instanceof ISVNRemoteResource) {
			SVNUrl fromUrl = null;
			SVNUrl toUrl = null;
			SVNRevision fromRevision;
			SVNRevision toRevision;
			if (fromHeadButton.getSelection()) fromRevision = SVNRevision.HEAD;
			else {
				int fromRevisionInt = Integer.parseInt(fromRevisionText.getText().trim());
				long fromRevisionLong = fromRevisionInt;
				fromRevision = new SVNRevision.Number(fromRevisionLong);
			}
			if (toHeadButton.getSelection()) toRevision = SVNRevision.HEAD;
			else {
				int toRevisionInt = Integer.parseInt(toRevisionText.getText().trim());
				long toRevisionLong = toRevisionInt;
				toRevision = new SVNRevision.Number(toRevisionLong);
			}		
			try {
				fromUrl = new SVNUrl(fromUrlText.getText().trim());
				toUrl = new SVNUrl(toUrlText.getText().trim());
			} catch (Exception e) {
				MessageDialog.openError(getShell(), Policy.bind("DifferencesDialog.compare"), e.getMessage());
				success = false;
				return;
			}
			ISVNRemoteResource resource1 = null;
			SVNRevision.Number lastChangedRevision1 = null;
			ISVNRemoteResource resource2 = null;
			SVNRevision.Number lastChangedRevision2 = null;
			
			if (fromRevision instanceof SVNRevision.Number) {
				lastChangedRevision1 = (SVNRevision.Number)fromRevision;
			} else {
				lastChangedRevision1 = ((ISVNRemoteResource)remoteResources[0]).getLastChangedRevision();
			}
			if (fromResource.isFolder()) resource1 = new RemoteFolder(null, fromResource.getRepository(), fromUrl, fromRevision, lastChangedRevision1, null, null);
			else resource1 = new RemoteFile(null, fromResource.getRepository(), fromUrl, fromRevision, lastChangedRevision1, null, null);		
			if (fromRevision instanceof SVNRevision.Number) {
				if (usePegRevision && resource1 instanceof RemoteResource) {
					((RemoteResource)resource1).setPegRevision(fromRevision);
				}
			}

			if (toRevision instanceof SVNRevision.Number) {
				lastChangedRevision2 = (SVNRevision.Number)toRevision;
			} else {
				lastChangedRevision2 = ((ISVNRemoteResource)remoteResources[1]).getLastChangedRevision();
			}
			if (toResource.isFolder()) resource2 = new RemoteFolder(null, toResource.getRepository(), toUrl, toRevision, lastChangedRevision2, null, null);
			else resource2 = new RemoteFile(null, toResource.getRepository(), toUrl, toRevision, lastChangedRevision2, null, null);
			if (toRevision instanceof SVNRevision.Number) {
				if (usePegRevision && resource2 instanceof RemoteResource) {
					((RemoteResource)resource2).setPegRevision(toRevision);
				}
			}

			ISVNRemoteResource[] remotes = { resource1, resource2 };
    		CompareRemoteResourcesAction compareAction = new CompareRemoteResourcesAction();
    		compareAction.setLocalResource(localResource);
    		compareAction.setRemoteResources(remotes);
    		compareAction.setPegRevisions(pegRevisions);
    		compareAction.setLocalResources(remoteResources);
    		try {
				compareAction.execute(null);
			} catch (InvocationTargetException e) {
				SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
			} catch (InterruptedException e) {
				SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
			}
    	}
    }

	private void setOkButtonStatus() {
    	boolean canFinish = true;
    	if (diffButton.getSelection() && fileText.getText().trim().length() == 0) canFinish = false;
    	if (!fromHeadButton.getSelection() && fromRevisionText.getText().trim().length() == 0) canFinish = false;
    	if (!toHeadButton.getSelection() && toRevisionText.getText().trim().length() == 0) canFinish = false;
    	okButton.setEnabled(canFinish);
    	
    }
    
    private void showLog(Object sourceButton) {
    	// TODO here calculate the real from and to resources from the urls in the textfields ???
    	HistoryDialog dialog = null;
    	if (sourceButton == fromLogButton) {
    		if (fromResource instanceof ISVNRemoteResource) {
		    	if (fromResource == remoteResources[0]) dialog = new HistoryDialog(getShell(), (ISVNRemoteResource)remoteResources[0]);
		    	else dialog = new HistoryDialog(getShell(), (ISVNRemoteResource)remoteResources[1]);
    		} else {
    	    	if (fromResource == remoteResources[0]) dialog = new HistoryDialog(getShell(), ((ISVNResource)remoteResources[0]).getResource());
		    	else dialog = new HistoryDialog(getShell(), ((ISVNResource)remoteResources[1]).getResource());    			
    		}
	    	if (dialog.open() == HistoryDialog.CANCEL) return;
	        ILogEntry[] selectedEntries = dialog.getSelectedLogEntries();
	        if (selectedEntries.length == 0) return;
	        fromRevisionText.setText(Long.toString(selectedEntries[selectedEntries.length - 1].getRevision().getNumber()));
    	} else {
    		if (fromResource instanceof ISVNRemoteResource) {
		    	if (fromResource == remoteResources[0]) dialog = new HistoryDialog(getShell(), (ISVNRemoteResource)remoteResources[1]);
		    	else dialog = new HistoryDialog(getShell(), (ISVNRemoteResource)remoteResources[0]);
    		} else {
		    	if (fromResource == remoteResources[0]) dialog = new HistoryDialog(getShell(), ((ISVNResource)remoteResources[1]).getResource());
		    	else dialog = new HistoryDialog(getShell(), ((ISVNRemoteResource)remoteResources[0]).getResource());    			
    		}
	    	if (dialog.open() == HistoryDialog.CANCEL) return;
	        ILogEntry[] selectedEntries = dialog.getSelectedLogEntries();
	        if (selectedEntries.length == 0) return;
	        toRevisionText.setText(Long.toString(selectedEntries[selectedEntries.length - 1].getRevision().getNumber()));    		
    	}
        setOkButtonStatus();    	
    }

	public void setFromRevision(String fromRevision) {
		this.fromRevision = fromRevision;
	}

	public void setToRevision(String toRevision) {
		this.toRevision = toRevision;
	}

	public void setUsePegRevision(boolean usePegRevision) {
		this.usePegRevision = usePegRevision;
	}

	/**
	 * @param fromUrl The fromUrl to set.
	 */
	public void setFromUrl(SVNUrl fromUrl) {
		this.fromUrl = fromUrl;
	}

	/**
	 * @param toUrl The toUrl to set.
	 */
	public void setToUrl(SVNUrl toUrl) {
		this.toUrl = toUrl;
	}
	
}
