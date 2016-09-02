package org.tigris.subversion.subclipse.ui.wizards.dialogs;

import java.net.MalformedURLException;
import java.text.ParseException;
import java.util.ArrayList;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ColumnLayoutData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.history.ILogEntry;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.dialogs.ChooseUrlDialog;
import org.tigris.subversion.subclipse.ui.dialogs.HistoryDialog;
import org.tigris.subversion.subclipse.ui.util.UrlCombo;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class SvnWizardCompareMultipleResourcesWithBranchTagPage extends SvnWizardDialogPage {
    private static final int REVISION_WIDTH_HINT = 40;
    
    private IResource[] resources;
    
    private UrlCombo urlCombo;
    private Text revisionText;
    private Button logButton;
    private Button headButton;
    
	private Table table;
	private TableViewer viewer; 
    
    private SVNUrl[] urls;
    private SVNRevision revision;
    
    private String[] urlStrings;
    private String commonRoot;
    private CompareResource[] compareResources;
    
    private long revisionNumber;
    
	private String[] columnHeaders = {Policy.bind("SwitchDialog.resources")}; //$NON-NLS-1$
	private ColumnLayoutData columnLayouts[] = {
		new ColumnWeightData(100, 100, true)};

	public SvnWizardCompareMultipleResourcesWithBranchTagPage(IResource[] resources) {
		this("SvnWizardCompareMultipleResourcesWithBranchTagPage", resources); //$NON-NLS-1$
	}
	
	public SvnWizardCompareMultipleResourcesWithBranchTagPage(String name, IResource[] resources) {
		super(name, Policy.bind("SvnWizardCompareMultipleResourcesWithBranchTagPage.0")); //$NON-NLS-1$
		this.resources = resources;
	}
	
	public SvnWizardCompareMultipleResourcesWithBranchTagPage(IResource[] resources, long revisionNumber) {
		this(resources);
		this.revisionNumber = revisionNumber;
	}	

	public void createControls(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(data);
		
		Label urlLabel = new Label(composite, SWT.NONE);
		urlLabel.setText(Policy.bind("SwitchDialog.url")); //$NON-NLS-1$
		
		urlCombo = new UrlCombo(composite, SWT.NONE);
		urlCombo.init(resources[0].getProject().getName());
		urlCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));

		commonRoot = getCommonRoot();
		if (commonRoot != null) urlCombo.setText(commonRoot);
        
        urlCombo.getCombo().addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                setPageComplete(canFinish());
            }         
        });
		
		Button browseButton = new Button(composite, SWT.PUSH);
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

		final Composite revisionGroup = new Composite(composite, SWT.NULL);
		GridLayout revisionLayout = new GridLayout();
		revisionLayout.numColumns = 3;
		revisionLayout.marginWidth = 0;
		revisionLayout.marginHeight = 0;
		revisionGroup.setLayout(revisionLayout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 3;
		revisionGroup.setLayoutData(data);
		
		headButton = new Button(revisionGroup, SWT.CHECK);
		headButton.setText(Policy.bind("SvnWizardCompareMultipleResourcesWithBranchTagPage.1")); //$NON-NLS-1$
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
		
		if (resources.length > 1) {
			table = new Table(composite, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
			table.setLinesVisible(false);
			table.setHeaderVisible(true);
			data = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL);
			data.horizontalSpan = 3;
			table.setLayoutData(data);
			TableLayout tableLayout = new TableLayout();
			table.setLayout(tableLayout);
			viewer = new TableViewer(table);
			viewer.setContentProvider(new CompareContentProvider());
			ILabelDecorator decorator = PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator();
			viewer.setLabelProvider(new TableDecoratingLabelProvider(new CompareLabelProvider(), decorator));
			for (int i = 0; i < columnHeaders.length; i++) {
				tableLayout.addColumnData(columnLayouts[i]);
				TableColumn tc = new TableColumn(table, SWT.NONE,i);
				tc.setResizable(columnLayouts[i].resizable);
				tc.setText(columnHeaders[i]);
			}			
			viewer.setInput(this);
			urlCombo.getCombo().addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					viewer.refresh();
				}				
			});
		}
		
		setPageComplete(canFinish());
	}

	public String getWindowTitle() {
		return Policy.bind(Policy.bind("SvnWizardCompareMultipleResourcesWithBranchTagPage.0")); //$NON-NLS-1$
	}

	public boolean performCancel() {
		return true;
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

	public boolean performFinish() {
        urlCombo.saveUrl();
        try {
        	if (urlStrings.length > 1) {
        		urls = new SVNUrl[compareResources.length];
        		for (int i = 0; i < compareResources.length; i++) {
        			if (urlCombo.getText().endsWith("/")) //$NON-NLS-1$
        				urls[i] = new SVNUrl(urlCombo.getText() + compareResources[i].getPartialPath());
        			else
        				urls[i] = new SVNUrl(urlCombo.getText() + "/" + compareResources[i].getPartialPath()); //$NON-NLS-1$
        		}
        	}
        	else {
        		urls = new SVNUrl[1];
        		urls[0] = new SVNUrl(urlCombo.getText());
        	}
            if (headButton.getSelection()) revision = SVNRevision.HEAD;
            else {
                try {
                    revision = SVNRevision.getRevision(revisionText.getText().trim());
                } catch (ParseException e1) {
                  MessageDialog.openError(getShell(), Policy.bind("SvnWizardCompareMultipleResourcesWithBranchTagPage.0"), Policy.bind("SwitchDialog.invalid")); //$NON-NLS-1$ //$NON-NLS-2$
                  return false;   
                }
            }
        } catch (MalformedURLException e) {
            MessageDialog.openError(getShell(), Policy.bind("SvnWizardCompareMultipleResourcesWithBranchTagPage.0"), e.getMessage()); //$NON-NLS-1$
            return false;
        }
        return true;
	}

	public void saveSettings() {
	}

	public void setMessage() {
		setMessage(Policy.bind("SvnWizardCompareMultipleResourcesWithBranchTagPage.8")); //$NON-NLS-1$
	}
	
	private boolean canFinish() {
		setErrorMessage(null);
		if (!(urlCombo.getText().length() > 0 && (headButton.getSelection() || (revisionText.getText().trim().length() > 0)))) return false;
		return true;
	}
	
    public SVNRevision getRevision() {
        return revision;
    }
    public SVNUrl[] getUrls() {
        return urls;
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
    		if (partialPath.endsWith("/")) { //$NON-NLS-1$
	    		for (int j = 1; j < urlStrings.length; j++) {
	    			if (!urlStrings[j].startsWith(partialPath)) break tag1;
	    		}
	    		commonRoot = partialPath.substring(0, i);
    		}
    	}
    	compareResources = new CompareResource[resources.length];
    	for (int i = 0; i < resources.length; i++) {
    		compareResources[i] = new CompareResource(resources[i], urlStrings[i].substring(commonRoot.length() + 1));
    	}
    	return commonRoot;
    }
    
    private class CompareResource implements IAdaptable {
    	private IResource resource;
    	private String partialPath;
    	public CompareResource(IResource resource, String partialPath) {
    		this.resource = resource;
    		this.partialPath = partialPath;
    	}
		public IResource getResource() {
			return resource;
		}
		public void setResource(IResource resource) {
			this.resource = resource;
		}
		public String getPartialPath() {
			return partialPath;
		}
		public void setPartialPath(String partialPath) {
			this.partialPath = partialPath;
		}
		public Object getAdapter(Class adapter) {
			if (IResource.class == adapter) return resource;
			return null;
		}
    }
    
	class CompareLabelProvider extends LabelProvider implements ITableLabelProvider {
		WorkbenchLabelProvider workbenchLabelProvider = new WorkbenchLabelProvider();
		
		public String getColumnText(Object element, int columnIndex) {
			return getText(element);
		}
		
		public String getText(Object element) {
			CompareResource compareResource = (CompareResource)element;
			return compareResource.getPartialPath() + " [" + urlCombo.getText() + "/" + compareResource.getPartialPath() + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		public Image getColumnImage(Object element, int columnIndex) {
			return getImage(element);
		}

		public Image getImage(Object element) {
			CompareResource compareResource = (CompareResource)element;
			return workbenchLabelProvider.getImage(compareResource.getResource());
		}
	
	}    
    
	class CompareContentProvider implements IStructuredContentProvider {
		public void dispose() {
		}
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
		public Object[] getElements(Object obj) {
			return compareResources;
		}
	}  
	
	class TableDecoratingLabelProvider extends DecoratingLabelProvider implements ITableLabelProvider {

		ITableLabelProvider provider;
		ILabelDecorator decorator;
	
		public TableDecoratingLabelProvider(ILabelProvider provider, ILabelDecorator decorator) {
			super(provider, decorator);
			this.provider = (ITableLabelProvider) provider;
		    this.decorator = decorator;
		}
	
		public Image getColumnImage(Object element, int columnIndex) {
			Image image = provider.getColumnImage(element, columnIndex);
	        if (decorator != null) {
	            Image decorated = decorator.decorateImage(image, element);
	            if (decorated != null) {
	                return decorated;
	            }
	        }
	        return image;
		}
	
		public String getColumnText(Object element, int columnIndex) {
			String text = provider.getColumnText(element, columnIndex);
	        return text;
		}
	}

	public void createButtonsForButtonBar(Composite parent, SvnWizardDialog wizardDialog) {
	}

}