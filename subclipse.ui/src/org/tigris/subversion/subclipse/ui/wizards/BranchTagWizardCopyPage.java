package org.tigris.subversion.subclipse.ui.wizards;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLayoutData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.SVNExternal;
import org.tigris.subversion.subclipse.core.history.ILogEntry;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.dialogs.HistoryDialog;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNProperty;
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
	private Table table;
	private TableViewer viewer; 
	private Button selectAllButton;
	private Button deselectAllButton;
	private String[] columnHeaders = { Policy.bind("BranchTagWizardCopyPage.0"), Policy.bind("BranchTagWizardCopyPage.1"), Policy.bind("BranchTagWizardCopyPage.2"), Policy.bind("BranchTagWizardCopyPage.3") }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	private ColumnLayoutData columnLayouts[] = {
			new ColumnWeightData(200, 200, true),
			new ColumnWeightData(200, 200, true),
			new ColumnWeightData(50, 50, true),
			new ColumnWeightData(50, 50, true)};

	private SVNExternal[] svnExternals;
    
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
		else revisionText.setText("" + revisionNumber); //$NON-NLS-1$
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
		if (resource == null) {
			workingCopyButton.setVisible(false);
		}
		else {
			if (getSvnExternalsProperties()) {
				Group externalsGroup = new Group(outerContainer, SWT.NULL);
				externalsGroup.setText(Policy.bind("BranchTagWizardCopyPage.5")); //$NON-NLS-1$
				GridLayout externalsLayout = new GridLayout();
				externalsLayout.numColumns = 1;
				externalsGroup.setLayout(externalsLayout);
				data = new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.FILL_BOTH);
				externalsGroup.setLayoutData(data);		

				table = new Table(externalsGroup, SWT.CHECK | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
				table.setHeaderVisible(true);
				data = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
				data.heightHint = 75;
				table.setLayoutData(data);
				TableLayout tableLayout = new TableLayout();
				table.setLayout(tableLayout);
				viewer = new CheckboxTableViewer(table);
				viewer.setContentProvider(new ExternalsContentProvider());
				viewer.setLabelProvider(new ExternalsLabelProvider());
				for (int i = 0; i < columnHeaders.length; i++) {
					tableLayout.addColumnData(columnLayouts[i]);
					TableColumn tc = new TableColumn(table, SWT.NONE,i);
					tc.setResizable(columnLayouts[i].resizable);
					tc.setText(columnHeaders[i]);
				}			
				viewer.setInput(this);
				((CheckboxTableViewer)viewer).addCheckStateListener(new ICheckStateListener() {					
					public void checkStateChanged(CheckStateChangedEvent event) {
						TableItem[] items = table.getItems();
						for (TableItem item : items) {
							((SVNExternal)item.getData()).setSelected(item.getChecked());
						}
					}
				});
				
				Composite buttonGroup = new Composite(externalsGroup, SWT.NONE);
				GridLayout buttonLayout = new GridLayout();
				buttonLayout.numColumns = 2;
				buttonLayout.makeColumnsEqualWidth = true;
				buttonGroup.setLayout(buttonLayout);
				GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
				buttonGroup.setLayoutData(gd);
				
				selectAllButton = new Button(buttonGroup, SWT.PUSH);
				selectAllButton.setText(Policy.bind("BranchTagWizardCopyPage.6")); //$NON-NLS-1$
				gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
				selectAllButton.setLayoutData(gd);
				deselectAllButton = new Button(buttonGroup, SWT.PUSH);
				deselectAllButton.setText(Policy.bind("BranchTagWizardCopyPage.7")); //$NON-NLS-1$
				gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
				deselectAllButton.setLayoutData(gd);
				
				SelectionListener buttonListener = new SelectionAdapter() {		
					public void widgetSelected(SelectionEvent e) {
						if (e.getSource() == selectAllButton) {
							selectAll();
						} else {
							deselectAll();
						}
					}
				};
				selectAllButton.addSelectionListener(buttonListener);
				deselectAllButton.addSelectionListener(buttonListener);
			}
		}
		
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

	public SVNExternal[] getSvnExternals() {
		return svnExternals;
	}

	private void selectAll() {
		TableItem[] items = table.getItems();
		for (TableItem item : items) {
			item.setChecked(true);
			((SVNExternal)item.getData()).setSelected(true);
		}
	}
	
	private void deselectAll() {
		TableItem[] items = table.getItems();
		for (TableItem item : items) {
			item.setChecked(false);
			((SVNExternal)item.getData()).setSelected(false);
		}
	}
	
	private boolean getSvnExternalsProperties() {
		List<SVNExternal> externalsList = new ArrayList<SVNExternal>();
		ISVNClientAdapter svnClient = null;
		ISVNRepositoryLocation repository = null;
		try {
			repository = SVNWorkspaceRoot.getSVNResourceFor(resource).getRepository();
			svnClient = repository.getSVNClient();
			IResource[] resources = ((BranchTagWizard)getWizard()).getResources();
			for (IResource res : resources) {
				ISVNProperty[] properties = svnClient.getProperties(res.getLocation().toFile(), true);
				for (ISVNProperty property : properties) {
					if (property.getName().equals(Policy.bind("BranchTagWizardCopyPage.8"))) {					 //$NON-NLS-1$
						String[] propertyLines = property.getValue().split("\\n"); //$NON-NLS-1$
						for (String propertyLine : propertyLines) {
							SVNExternal svnExternal = new SVNExternal(property.getFile(), propertyLine);
							externalsList.add(svnExternal);
						}
					}
				}
			}
		}
		catch (Exception e) {}
		finally {
			if (repository != null) {
				repository.returnSVNClient(svnClient);
			}
		}
		svnExternals = new SVNExternal[externalsList.size()];
		externalsList.toArray(svnExternals);
		return externalsList.size() > 0;
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
    
	static class ExternalsLabelProvider extends LabelProvider implements ITableLabelProvider {

		public String getColumnText(Object element, int columnIndex) {
			SVNExternal external = (SVNExternal)element;	
			switch (columnIndex) { 
				case 0: 
					if (external.getPath() != null) {
						return external.getPath();
					}
					break;
				case 1: 						
					if (external.getUrl() != null) {
						return external.getUrl().toString();
					}
					break;	
				case 2: 	
					if (external.getRevision() != -1) {
						return Long.toString(external.getRevision());
					}
					break;	
				case 3: 
					if (external.getFixedAtRevision() != -1) {
						return Long.toString(external.getFixedAtRevision());
					}
					break;					
			}
			return "";  //$NON-NLS-1$
		}
		
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}
	
	}
	
	class ExternalsContentProvider implements IStructuredContentProvider {
		public void dispose() {
		}
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
		public Object[] getElements(Object obj) {
			return svnExternals;
		}
	}	

}
