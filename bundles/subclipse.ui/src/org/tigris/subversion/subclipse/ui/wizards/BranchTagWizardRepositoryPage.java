package org.tigris.subversion.subclipse.ui.wizards;

import java.util.ArrayList;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.IDialogSettings;
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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchLabelProvider;
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
    protected Button sameStructureButton;
	private Table table;
	private TableViewer viewer; 
    private IResource[] resources;
    private ISVNRemoteResource[] remoteResources;
    private ISVNLocalResource[] svnResources;
    private SVNUrl[] urls;
    private BranchResource[] branchResources;
    private String commonRoot;
    private IDialogSettings settings = SVNUIPlugin.getPlugin().getDialogSettings();
	private String[] columnHeaders = {Policy.bind("BranchTagDialog.resources")}; //$NON-NLS-1$
	private ColumnLayoutData columnLayouts[] = {
		new ColumnWeightData(100, 100, true)};
	
	public BranchTagWizardRepositoryPage() {
		super("repositoryPage", //$NON-NLS-1$
				Policy.bind("BranchTagWizardRepositoryPage.heading"), //$NON-NLS-1$
				SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_WIZBAN_SVN),
				Policy.bind("BranchTagWizardRepositoryPage.message")); //$NON-NLS-1$
	}

	public void createControl(Composite parent) {
		resources = ((BranchTagWizard)getWizard()).getResources();
		remoteResources = ((BranchTagWizard)getWizard()).getRemoteResources();
		
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
		GridData data = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		repositoryGroup.setLayoutData(data);

		if (multipleSelections()) {
			ArrayList urlArray = new ArrayList();
			if (resources == null) {
				for (int i = 0; i < remoteResources.length; i++) {
					urlArray.add(remoteResources[i].getUrl());
				}
			} else {
				for (int i = 0; i < resources.length; i++) {
					ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resources[i]);
					try {
			            SVNUrl url = svnResource.getStatus().getUrl();
			            if (url != null) {
			            	urlArray.add(url);
			            }
			        } catch (SVNException e1) {}					
				}
			}
			urls = new SVNUrl[urlArray.size()];
			urlArray.toArray(urls);
		} else {
			if (resources == null) {
				urls = new SVNUrl[1];
				urls[0] = remoteResources[0].getUrl();
			} else {
				svnResources = new ISVNLocalResource[1];
				svnResources[0] = SVNWorkspaceRoot.getSVNResourceFor(resources[0]);
				try {
					urls = new SVNUrl[1];
		            urls[0] = svnResources[0].getStatus().getUrl();
		        } catch (SVNException e1) {}
			}
		}
        
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
		toUrlCombo.init( resources == null ? "repositoryBrowser" : resources[0].getProject().getName()); //$NON-NLS-1$
		toUrlCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
		toUrlCombo.setText(getCommonRoot());
		toUrlCombo.getCombo().addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				setPageComplete(canFinish());
			}		
		});
		
		Button browseButton = new Button(urlComposite, SWT.PUSH);
		browseButton.setText(Policy.bind("SwitchDialog.browse")); //$NON-NLS-1$
		browseButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
            	IResource resource = null;
            	if (resources != null && resources.length > 0) resource = resources[0];
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
		
		if (multipleSelections() && !sameParents()) {
			sameStructureButton = new Button(urlComposite, SWT.CHECK);
			sameStructureButton.setText(Policy.bind("BranchTagDialog.sameStructure")); //$NON-NLS-1$  
			data = new GridData();
			data.horizontalSpan = 2;
			sameStructureButton.setLayoutData(data);	
			sameStructureButton.setSelection(settings.getBoolean("BranchTagDialog.sameStructure")); //$NON-NLS-1$  
			sameStructureButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					settings.put("BranchTagDialog.sameStructure", sameStructureButton.getSelection()); //$NON-NLS-1$ 
					viewer.refresh();
				}		
			});	
		}
		
//		Label label = new Label(outerContainer, SWT.NONE);
//		label.setText(Policy.bind("BranchTagDialog.resources"));
		
		table = new Table(outerContainer, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
//		table.setLinesVisible(false);
		table.setHeaderVisible(true);
		data = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
		data.heightHint = 75;
		table.setLayoutData(data);
		TableLayout tableLayout = new TableLayout();
		table.setLayout(tableLayout);
		viewer = new TableViewer(table);
		viewer.setContentProvider(new BranchContentProvider());
		ILabelDecorator decorator = PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator();
		viewer.setLabelProvider(new TableDecoratingLabelProvider(new BranchLabelProvider(), decorator));
		for (int i = 0; i < columnHeaders.length; i++) {
			tableLayout.addColumnData(columnLayouts[i]);
			TableColumn tc = new TableColumn(table, SWT.NONE,i);
			tc.setResizable(columnLayouts[i].resizable);
			tc.setText(columnHeaders[i]);
		}			
		viewer.setInput(this);
		toUrlCombo.getCombo().addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				viewer.refresh();
			}				
		});
		
		toUrlCombo.getCombo().setFocus();
		
		setPageComplete(canFinish());

		setControl(outerContainer);
	}
	
	private boolean multipleSelections() {
		return (resources != null && resources.length > 1) || (remoteResources != null && remoteResources.length > 1);
	}
	
	private boolean sameParents() {
		for (int i = 0; i < branchResources.length; i++) {
			String name = null;
			if (branchResources[i].getResource() == null) name = branchResources[i].getRemoteResource().getName();
			else name = branchResources[i].getResource().getName();
			if (!branchResources[i].getPartialPath().equals(name)) return false;
		}
		return true;
	}
	
	public String getCommonRoot() {
		if (commonRoot != null) return commonRoot;
    	ArrayList urlList = new ArrayList();
    	if (resources == null) {
    		for (int i = 0; i < remoteResources.length; i++) {
    			urlList.add(remoteResources[i].getUrl().toString());
    		}
    	} else {
	    	for (int i = 0; i < resources.length; i++) {
	    		ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resources[i]);
	    		try {
	                String anUrl = svnResource.getStatus().getUrlString();
	                if (anUrl != null) urlList.add(anUrl);
	            } catch (SVNException e1) {}    		
	    	}
    	}
    	String[] urlStrings = new String[urlList.size()];
    	urlList.toArray(urlStrings);
    	if (urlStrings.length == 0) return null;
    	String urlString = urlStrings[0];
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
    	if (resources == null) {
	    	branchResources = new BranchResource[remoteResources.length];
	    	for (int i = 0; i < remoteResources.length; i++) {
	    		branchResources[i] = new BranchResource(remoteResources[i], urlStrings[i].substring(commonRoot.length() + 1));
	    	}       		
    	} else {
	    	branchResources = new BranchResource[resources.length];
	    	for (int i = 0; i < resources.length; i++) {
	    		branchResources[i] = new BranchResource(resources[i], urlStrings[i].substring(commonRoot.length() + 1));
	    	}   
    	}
    	
       	if(urlStrings.length == 1){
    		return urlString;
    	}
    	
    	return commonRoot;
	}
	
	private boolean canFinish() {
		return toUrlCombo.getText().trim().length() > 0;
	}
	
	public SVNUrl getUrl() {
		if (urls == null || urls.length < 1) return null;
		return urls[0];
	}
	
	public SVNUrl[] getUrls() {
		return urls;
	}
	
	public String getUrlText() {
		return toUrlCombo.getText().trim();
	}
	
	public void saveUrl() {
		toUrlCombo.saveUrl();
	}

	public ISVNLocalResource getSvnResource() {
		if (svnResources == null || svnResources.length < 1) return null;
		return svnResources[0];
	}
	
	public String getToUrl() {
		if (multipleSelections()) {
			return getUrlText();
		}
		else {
			return toUrlCombo.getText();
		}
	}
	
    private class BranchResource implements IAdaptable {
    	private IResource resource;
    	private ISVNRemoteResource remoteResource;
    	private String partialPath;
    	public BranchResource(IResource resource, String partialPath) {
    		this.resource = resource;
    		this.partialPath = partialPath;
    	}
    	public BranchResource(ISVNRemoteResource remoteResource, String partialPath) {
    		this.remoteResource = remoteResource;
    		this.partialPath = partialPath;
    	}    	
		public IResource getResource() {
			return resource;
		}
		public ISVNRemoteResource getRemoteResource() {
			return remoteResource;
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
    
	class BranchLabelProvider extends LabelProvider implements ITableLabelProvider {
		WorkbenchLabelProvider workbenchLabelProvider = new WorkbenchLabelProvider();
		
		public String getColumnText(Object element, int columnIndex) {
			return getText(element);
		}
		
		public String getText(Object element) {
			BranchResource branchResource = (BranchResource)element;
			if (multipleSelections()) {
				if (sameStructureButton != null && sameStructureButton.getSelection())
					return branchResource.getPartialPath() + " [" + toUrlCombo.getText() + "/" + branchResource.getPartialPath() + "]";
				else {
					return getDestinationText(branchResource);
				}
			}
			else {
				return branchResource.getPartialPath() + " [" + toUrlCombo.getText() + "]";
			}
		}

		private String getDestinationText(BranchResource branchResource) {
			String name = null;
			if (branchResource.getResource() == null) name = branchResource.getRemoteResource().getName();
			else name = branchResource.getResource().getName();
			return branchResource.getPartialPath() + " [" + toUrlCombo.getText() + "/" + name + "]";
		}

		public Image getColumnImage(Object element, int columnIndex) {
			return getImage(element);
		}

		public Image getImage(Object element) {
			BranchResource branchResource = (BranchResource)element;
			if (branchResource.getResource() == null) return workbenchLabelProvider.getImage(branchResource.getRemoteResource());
			else return workbenchLabelProvider.getImage(branchResource.getResource());
		}
	
	}    
    
	class BranchContentProvider implements IStructuredContentProvider {
		public void dispose() {
		}
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
		public Object[] getElements(Object obj) {
			return branchResources;
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

}
