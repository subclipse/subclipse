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
import org.tigris.subversion.subclipse.ui.conflicts.SVNConflictResolver;
import org.tigris.subversion.subclipse.ui.dialogs.ChooseUrlDialog;
import org.tigris.subversion.subclipse.ui.dialogs.HistoryDialog;
import org.tigris.subversion.subclipse.ui.util.UrlCombo;
import org.tigris.subversion.svnclientadapter.ISVNConflictResolver;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class SvnWizardSwitchPage extends SvnWizardDialogPage {
    private static final int REVISION_WIDTH_HINT = 40;
    
    private IResource[] resources;
    private boolean showUrl;
    
    private UrlCombo urlCombo;
    private Composite revisionGroup;
    private Text revisionText;
    private Button logButton;
    private Button headButton;
    
	private Table table;
	private TableViewer viewer; 
	
	private Combo depthCombo;
	private Button setDepthButton;
	private Button ignoreExternalsButton;
	private Button forceButton;
	private Button ignoreAncestryButton;
	
	private Button textConflictPromptButton;
	private Button textConflictMarkButton;
	
	private Button propertyConflictPromptButton;
	private Button propertyConflictMarkButton;

	private Button binaryConflictPromptButton;
	private Button binaryConflictMarkButton;
	private Button binaryConflictUserButton;
	private Button binaryConflictIncomingButton;
	
	private Button treeConflictPromptButton;
	private Button treeConflictMarkButton;
	private Button treeConflictUserButton;
	private Button treeConflictResolveButton;
	
	private SVNConflictResolver conflictResolver;
    
    private SVNUrl[] urls;
    private SVNRevision revision;
    private int depth;
    private boolean setDepth;
    private boolean ignoreExternals;
    private boolean force;
    private boolean ignoreAncestry;
    
    private String[] urlStrings;
    private String commonRoot;
    private SwitchResource[] switchResources;
    
    private long revisionNumber;
    
	private String[] columnHeaders = {Policy.bind("SwitchDialog.resources")}; //$NON-NLS-1$
	private ColumnLayoutData columnLayouts[] = {
		new ColumnWeightData(100, 100, true)};

	public SvnWizardSwitchPage(IResource[] resources, boolean showUrl) {
		this("SwitchDialogWithConflictHandling2", resources); //$NON-NLS-1$
		this.showUrl = showUrl;
	}
	
	public SvnWizardSwitchPage(String name, IResource[] resources) {
		super(name, Policy.bind("SwitchDialog.title")); //$NON-NLS-1$
		this.resources = resources;
		showUrl = true;
	}
	
	public SvnWizardSwitchPage(IResource[] resources, long revisionNumber) {
		this(resources, true);
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
		
		if (showUrl) {
		
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
	
			revisionGroup = new Composite(composite, SWT.NULL);
			GridLayout revisionLayout = new GridLayout();
			revisionLayout.numColumns = 3;
			revisionLayout.marginWidth = 0;
			revisionLayout.marginHeight = 0;
			revisionGroup.setLayout(revisionLayout);
			data = new GridData(GridData.FILL_HORIZONTAL);
			data.horizontalSpan = 3;
			revisionGroup.setLayoutData(data);
			
			headButton = new Button(revisionGroup, SWT.CHECK);
			headButton.setText(Policy.bind("SvnWizardSwitchPage.head")); //$NON-NLS-1$
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
		
		}
		
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
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 3;
		parameterGroup.setLayoutData(data);	
		
		Label depthLabel = new Label(parameterGroup, SWT.NONE);
		depthLabel.setText(Policy.bind("SvnDialog.depth")); //$NON-NLS-1$
		depthCombo = new Combo(parameterGroup, SWT.READ_ONLY);
		DepthComboHelper.addDepths(depthCombo, true, true, ISVNUIConstants.DEPTH_UNKNOWN);
		
		depthCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				if (depthCombo.getText().equals(ISVNUIConstants.DEPTH_EXCLUDE)) {
					setDepthButton.setSelection(true);
					setDepthButton.setEnabled(false);
					ignoreExternalsButton.setVisible(false);
					forceButton.setVisible(false);
					ignoreAncestryButton.setVisible(false);
					if (revisionGroup != null) {
						revisionGroup.setVisible(false);
					}
				} else {
					setDepthButton.setEnabled(true);
					ignoreExternalsButton.setVisible(true);
					forceButton.setVisible(true);
					ignoreAncestryButton.setVisible(true);
					if (revisionGroup != null) {
						revisionGroup.setVisible(true);
					}
				}
				setPageComplete(canFinish());
			}			
		});
		
		setDepthButton = new Button(parameterGroup, SWT.CHECK);
		setDepthButton.setText(Policy.bind("SvnDialog.setDepth")); //$NON-NLS-1$
		data = new GridData();
		data.horizontalSpan = 2;
		setDepthButton.setLayoutData(data);		
		
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
		
		ignoreAncestryButton = new Button(parameterGroup, SWT.CHECK);
		ignoreAncestryButton.setText(Policy.bind("SvnWizardSwitchPage.0")); //$NON-NLS-1$
		data = new GridData();
		data.horizontalSpan = 2;
		ignoreAncestryButton.setLayoutData(data);
		ignoreAncestryButton.setSelection(false);
		
		Group conflictGroup = new Group(composite, SWT.NONE);
		conflictGroup.setText(Policy.bind("SvnWizardUpdatePage.0")); //$NON-NLS-1$
		GridLayout conflictLayout = new GridLayout();
		conflictLayout.numColumns = 1;
		conflictGroup.setLayout(conflictLayout);
		data = new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL);
		data.horizontalSpan = 2;
		conflictGroup.setLayoutData(data);
		
		Group textGroup = new Group(conflictGroup, SWT.NONE);
		textGroup.setText(Policy.bind("SvnWizardUpdatePage.1")); //$NON-NLS-1$
		GridLayout textLayout = new GridLayout();
		textLayout.numColumns = 1;
		textGroup.setLayout(textLayout);
		textGroup.setLayoutData(
		new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));	
		
		textConflictPromptButton = new Button(textGroup, SWT.RADIO);
		textConflictPromptButton.setText(Policy.bind("SvnWizardUpdatePage.2")); //$NON-NLS-1$
		textConflictMarkButton = new Button(textGroup, SWT.RADIO);
		textConflictMarkButton.setText(Policy.bind("SvnWizardUpdatePage.3")); //$NON-NLS-1$
		
		Group binaryGroup = new Group(conflictGroup, SWT.NONE);
		binaryGroup.setText(Policy.bind("SvnWizardUpdatePage.4")); //$NON-NLS-1$
		GridLayout binaryLayout = new GridLayout();
		binaryLayout.numColumns = 1;
		binaryGroup.setLayout(binaryLayout);
		binaryGroup.setLayoutData(
		new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));	
		
		binaryConflictPromptButton = new Button(binaryGroup, SWT.RADIO);
		binaryConflictPromptButton.setText(Policy.bind("SvnWizardUpdatePage.5")); //$NON-NLS-1$
		binaryConflictMarkButton = new Button(binaryGroup, SWT.RADIO);
		binaryConflictMarkButton.setText(Policy.bind("SvnWizardUpdatePage.6")); //$NON-NLS-1$
		binaryConflictUserButton = new Button(binaryGroup, SWT.RADIO);
		binaryConflictUserButton.setText(Policy.bind("SvnWizardUpdatePage.7")); //$NON-NLS-1$
		binaryConflictIncomingButton = new Button(binaryGroup, SWT.RADIO);
		binaryConflictIncomingButton.setText(Policy.bind("SvnWizardUpdatePage.8")); //$NON-NLS-1$

		Group propertyGroup = new Group(conflictGroup, SWT.NONE);
		propertyGroup.setText(Policy.bind("SvnWizardUpdatePage.9")); //$NON-NLS-1$
		GridLayout propertyLayout = new GridLayout();
		propertyLayout.numColumns = 1;
		propertyGroup.setLayout(propertyLayout);
		propertyGroup.setLayoutData(
		new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));			

		propertyConflictPromptButton = new Button(propertyGroup, SWT.RADIO);
		propertyConflictPromptButton.setText(Policy.bind("SvnWizardUpdatePage.10")); //$NON-NLS-1$
		propertyConflictMarkButton = new Button(propertyGroup, SWT.RADIO);
		propertyConflictMarkButton.setText(Policy.bind("SvnWizardUpdatePage.11")); //$NON-NLS-1$
		
		Group treeConflictGroup = new Group(conflictGroup, SWT.NONE);
		treeConflictGroup.setText(Policy.bind("SvnWizardUpdatePage.12")); //$NON-NLS-1$
		GridLayout treeConflictLayout = new GridLayout();
		treeConflictLayout.numColumns = 1;
		treeConflictGroup.setLayout(treeConflictLayout);
		treeConflictGroup.setLayoutData(
		new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));	
		
		treeConflictPromptButton = new Button(treeConflictGroup, SWT.RADIO);
		treeConflictPromptButton.setText(Policy.bind("SvnWizardUpdatePage.10")); //$NON-NLS-1$
		treeConflictMarkButton = new Button(treeConflictGroup, SWT.RADIO);
		treeConflictMarkButton.setText(Policy.bind("SvnWizardUpdatePage.11")); //$NON-NLS-1$
		treeConflictUserButton = new Button(treeConflictGroup, SWT.RADIO);
		treeConflictUserButton.setText(Policy.bind("SvnWizardUpdatePage.13")); //$NON-NLS-1$
		treeConflictResolveButton = new Button(treeConflictGroup, SWT.RADIO);
		treeConflictResolveButton.setText(Policy.bind("SvnWizardUpdatePage.14")); //$NON-NLS-1$
		
		textConflictMarkButton.setSelection(true);
		binaryConflictMarkButton.setSelection(true);
		propertyConflictMarkButton.setSelection(true);
		treeConflictMarkButton.setSelection(true);
		
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
        try {
        	if (showUrl) {
	        	urlCombo.saveUrl();
	        	if (urlStrings.length > 1) {
	        		urls = new SVNUrl[switchResources.length];
	        		for (int i = 0; i < switchResources.length; i++) {
	        			if (urlCombo.getText().endsWith("/")) //$NON-NLS-1$
	        				urls[i] = new SVNUrl(urlCombo.getText() + switchResources[i].getPartialPath());
	        			else
	        				urls[i] = new SVNUrl(urlCombo.getText() + "/" + switchResources[i].getPartialPath()); //$NON-NLS-1$
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
        	}
            setDepth = setDepthButton.getSelection();
            ignoreExternals = ignoreExternalsButton.getSelection();
            force = forceButton.getSelection();
            ignoreAncestry = ignoreAncestryButton.getSelection();
            depth = DepthComboHelper.getDepth(depthCombo);
            conflictResolver = new SVNConflictResolver(resources[0], getTextConflictHandling(), getBinaryConflictHandling(), getPropertyConflictHandling(), getTreeConflictHandling());
        } catch (MalformedURLException e) {
            MessageDialog.openError(getShell(), Policy.bind("SwitchDialog.title"), e.getMessage()); //$NON-NLS-1$
            return false;
        }
        return true;
	}

	public void saveSettings() {
	}
	
	public SVNConflictResolver getConflictResolver() {
		return conflictResolver;
	}

	public int getTextConflictHandling() {
		if (textConflictMarkButton.getSelection()) return ISVNConflictResolver.Choice.postpone;
		else return ISVNConflictResolver.Choice.chooseMerged;
	}
	
	public int getBinaryConflictHandling() {
		if (binaryConflictIncomingButton.getSelection()) return ISVNConflictResolver.Choice.chooseTheirsFull;
		else if (binaryConflictUserButton.getSelection()) return ISVNConflictResolver.Choice.chooseMineFull;
		else if (binaryConflictMarkButton.getSelection()) return ISVNConflictResolver.Choice.postpone;
		else return ISVNConflictResolver.Choice.chooseMerged;
	}
	
	public int getPropertyConflictHandling() {
		if (propertyConflictMarkButton.getSelection()) return ISVNConflictResolver.Choice.postpone;
		else return ISVNConflictResolver.Choice.chooseMerged;
	}
	
	public int getTreeConflictHandling() {
		if (treeConflictMarkButton.getSelection()) return ISVNConflictResolver.Choice.postpone;
		else if (treeConflictResolveButton.getSelection()) return ISVNConflictResolver.Choice.chooseMerged;
		else if (treeConflictUserButton.getSelection()) return ISVNConflictResolver.Choice.chooseMine;
		else return SVNConflictResolver.PROMPT;
	}

	public void setMessage() {
		setMessage(Policy.bind("SwitchDialog.message")); //$NON-NLS-1$
	}
	
	private boolean canFinish() {
		setErrorMessage(null);
		if (showUrl) {
			if (!(urlCombo.getText().length() > 0 && (headButton.getSelection() || (revisionText.getText().trim().length() > 0)))) return false;
			if (depthCombo.getText().equals(ISVNUIConstants.DEPTH_EXCLUDE)) {
				if (commonRoot == null || !urlCombo.getText().equals(commonRoot)) {
					setErrorMessage(Policy.bind("SwitchDialog.excludeAndSwitchError")); //$NON-NLS-1$
					return false;
				}
			}
		}
		return true;
	}
	
    public SVNRevision getRevision() {
        return revision;
    }
    public SVNUrl[] getUrls() {
        return urls;
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
		public String getPartialPath() {
			return partialPath;
		}
		@SuppressWarnings("rawtypes")
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
			return switchResource.getPartialPath() + " [" + urlCombo.getText() + "/" + switchResource.getPartialPath() + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
	
	public boolean isSetDepth() {
		return setDepth;
	}

	public boolean isIgnoreExternals() {
		return ignoreExternals;
	}

	public boolean isForce() {
		return force;
	}
	
	public boolean isIgnoreAncestry() {
		return ignoreAncestry;
	}

}
