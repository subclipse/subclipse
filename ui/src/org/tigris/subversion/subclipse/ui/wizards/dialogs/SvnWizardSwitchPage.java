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
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
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
import org.tigris.subversion.subclipse.ui.DepthComboHelper;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.dialogs.ChooseUrlDialog;
import org.tigris.subversion.subclipse.ui.dialogs.HistoryDialog;
import org.tigris.subversion.subclipse.ui.util.UrlCombo;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class SvnWizardSwitchPage extends SvnWizardDialogPage {
    private static final int REVISION_WIDTH_HINT = 40;
    
    private IResource[] resources;
    
    private UrlCombo urlCombo;
    private Text revisionText;
    private Button logButton;
    private Button headButton;
    private Button revisionButton;
    
	private Table table;
	private TableViewer viewer; 
	
	private Combo depthCombo;
	private Button ignoreExternalsButton;
	private Button forceButton;
    
    private SVNUrl[] urls;
    private SVNRevision revision;
    private int depth;
    private boolean ignoreExternals;
    private boolean force;
    
    private String[] urlStrings;
    private String commonRoot;
    private SwitchResource[] switchResources;
    
	private String[] columnHeaders = {"Resource"};
	private ColumnLayoutData columnLayouts[] = {
		new ColumnWeightData(100, 100, true)};

	public SvnWizardSwitchPage(IResource[] resources) {
		super("SwitchDialog", Policy.bind("SwitchDialog.title")); //$NON-NLS-1$ //$NON-NLS-2$
		this.resources = resources;
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
		
		urlCombo = new UrlCombo(composite, resources[0].getProject().getName());

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

		Group revisionGroup = new Group(composite, SWT.NULL);
		revisionGroup.setText(Policy.bind("SwitchDialog.revision")); //$NON-NLS-1$
		GridLayout revisionLayout = new GridLayout();
		revisionLayout.numColumns = 3;
		revisionGroup.setLayout(revisionLayout);
		data = new GridData(GridData.FILL_BOTH);
		data.horizontalSpan = 3;
		revisionGroup.setLayoutData(data);
		
		headButton = new Button(revisionGroup, SWT.RADIO);
		headButton.setText(Policy.bind("SwitchDialog.head")); //$NON-NLS-1$
		data = new GridData();
		data.horizontalSpan = 3;
		headButton.setLayoutData(data);
		
		revisionButton = new Button(revisionGroup, SWT.RADIO);
		revisionButton.setText(Policy.bind("SwitchDialog.revision")); //$NON-NLS-1$
		
		headButton.setSelection(true);
		
		revisionText = new Text(revisionGroup, SWT.BORDER);
		data = new GridData();
		data.widthHint = REVISION_WIDTH_HINT;
		revisionText.setLayoutData(data);
		revisionText.setEnabled(false);
		
		revisionText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                setPageComplete(canFinish());
            }		    
		});
		
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
                revisionText.setEnabled(revisionButton.getSelection());
                logButton.setEnabled(revisionButton.getSelection());
                setPageComplete(canFinish());
                if (revisionButton.getSelection()) {
                    revisionText.selectAll();
                    revisionText.setFocus();
                }
            }
		};
		
		headButton.addSelectionListener(listener);
		revisionButton.addSelectionListener(listener);
		
		if (resources.length > 1) {
			Label label = new Label(composite, SWT.NONE);
			label.setText(Policy.bind("SwitchDialog.resources"));
			data = new GridData();
			data.horizontalSpan = 3;
			label.setLayoutData(data);
			
			table = new Table(composite, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
			table.setLinesVisible(false);
			table.setHeaderVisible(false);
			data = new GridData(GridData.FILL_HORIZONTAL);
			data.heightHint = 200;
			data.horizontalSpan = 3;
			table.setLayoutData(data);
			TableLayout tableLayout = new TableLayout();
			table.setLayout(tableLayout);
			viewer = new TableViewer(table);
			viewer.setContentProvider(new SwitchContentProvider());
			ILabelDecorator decorator = PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator();
			viewer.setLabelProvider(new TableDecoratingLabelProvider(new SwitchLabelProvider(), decorator));
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
		
		Group parameterGroup = new Group(composite, SWT.NULL);
		GridLayout parameterLayout = new GridLayout();
		parameterLayout.numColumns = 2;
		parameterGroup.setLayout(parameterLayout);
		data = new GridData(GridData.FILL_BOTH);
		data.horizontalSpan = 3;
		parameterGroup.setLayoutData(data);	
		
		Label depthLabel = new Label(parameterGroup, SWT.NONE);
		depthLabel.setText(Policy.bind("SvnDialog.depth")); //$NON-NLS-1$
		depthCombo = new Combo(parameterGroup, SWT.READ_ONLY);
		DepthComboHelper.addDepths(depthCombo, true, ISVNUIConstants.DEPTH_UNKNOWN);
		
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
		
		setPageComplete(canFinish());

		// Add F1 help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.SWITCH_DIALOG);
	}

	public String getWindowTitle() {
		return Policy.bind("SwitchDialog.switch"); //$NON-NLS-1$
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
        		urls = new SVNUrl[switchResources.length];
        		for (int i = 0; i < switchResources.length; i++) {
        			if (urlCombo.getText().endsWith("/"))
        				urls[i] = new SVNUrl(urlCombo.getText() + switchResources[i].getPartialPath());
        			else
        				urls[i] = new SVNUrl(urlCombo.getText() + "/" + switchResources[i].getPartialPath());
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
                  MessageDialog.openError(getShell(), Policy.bind("SwitchDialog.title"), Policy.bind("SwitchDialog.invalid")); //$NON-NLS-1$ //$NON-NLS-2$
                  return false;   
                }
            }
            ignoreExternals = ignoreExternalsButton.getSelection();
            force = forceButton.getSelection();
            depth = DepthComboHelper.getDepth(depthCombo);
        } catch (MalformedURLException e) {
            MessageDialog.openError(getShell(), Policy.bind("SwitchDialog.title"), e.getMessage()); //$NON-NLS-1$
            return false;
        }
        return true;
	}

	public void saveSettings() {
	}

	public void setMessage() {
		setMessage(Policy.bind("SwitchDialog.message")); //$NON-NLS-1$
	}
	
	private boolean canFinish() {
		return urlCombo.getText().length() > 0 && (headButton.getSelection() || (revisionText.getText().trim().length() > 0));
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
    		if (partialPath.endsWith("/")) {
	    		for (int j = 1; j < urlStrings.length; j++) {
	    			if (!urlStrings[j].startsWith(partialPath)) break tag1;
	    		}
	    		commonRoot = partialPath.substring(0, i);
    		}
    	}
    	switchResources = new SwitchResource[resources.length];
    	for (int i = 0; i < resources.length; i++) {
    		switchResources[i] = new SwitchResource(resources[i], urlStrings[i].substring(commonRoot.length() + 1));
    	}
    	return commonRoot;
    }
    
    private class SwitchResource implements IAdaptable {
    	private IResource resource;
    	private String partialPath;
    	public SwitchResource(IResource resource, String partialPath) {
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
    
	class SwitchLabelProvider extends LabelProvider implements ITableLabelProvider {
		WorkbenchLabelProvider workbenchLabelProvider = new WorkbenchLabelProvider();
		
		public String getColumnText(Object element, int columnIndex) {
			return getText(element);
		}
		
		public String getText(Object element) {
			SwitchResource switchResource = (SwitchResource)element;
			return switchResource.getPartialPath() + " [" + urlCombo.getText() + "/" + switchResource.getPartialPath() + "]";
		}

		public Image getColumnImage(Object element, int columnIndex) {
			return getImage(element);
		}

		public Image getImage(Object element) {
			SwitchResource switchResource = (SwitchResource)element;
			return workbenchLabelProvider.getImage(switchResource.getResource());
		}
	
	}    
    
	class SwitchContentProvider implements IStructuredContentProvider {
		public void dispose() {
		}
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
		public Object[] getElements(Object obj) {
			return switchResources;
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

	public int getDepth() {
		return depth;
	}

	public boolean isIgnoreExternals() {
		return ignoreExternals;
	}

	public boolean isForce() {
		return force;
	}	

}
