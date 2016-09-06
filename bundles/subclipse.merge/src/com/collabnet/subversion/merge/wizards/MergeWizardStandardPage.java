/*******************************************************************************
 * Copyright (c) 2009 CollabNet.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     CollabNet - initial API and implementation
 ******************************************************************************/
package com.collabnet.subversion.merge.wizards;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
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
import org.eclipse.jface.wizard.WizardPage;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.dialogs.ChooseUrlDialog;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNMergeInfo;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

import com.collabnet.subversion.merge.Activator;
import com.collabnet.subversion.merge.Messages;

public class MergeWizardStandardPage extends WizardPage {
	private Combo fromCombo;
	private Button allButton;
	private Button selectButton;
	private Composite multipleGroup;
	private Button rootButton;
	private Button separateButton;
	
	private IResource resource;
	private IResource[] resources;
	private ISVNLocalResource svnResource;
	private String fromUrl;
	private String repositoryLocation;
	private ISVNMergeInfo mergeInfo;
	
	private String[] mergeInfoPaths;
	
    private String[] urlStrings;
    private String commonRoot;	
    private MergeResource[] mergeResources;
    
    private Button relativeButton;
    private IDialogSettings settings;
    
	private Table table;
	private TableViewer viewer;  
	
	private String message;
	private boolean showRevisionsButtons;
	private boolean unblock;
    
	private String[] columnHeaders = {Messages.MergeWizardStandardPage_resource};
	private ColumnLayoutData columnLayouts[] = {
		new ColumnWeightData(100, 100, true)};	

	public MergeWizardStandardPage(String pageName, String title, ImageDescriptor titleImage, String message, boolean showRevisionsButtons, boolean unblock) {
		super(pageName, title, titleImage);
		this.message = message;
		this.showRevisionsButtons = showRevisionsButtons;
		settings = Activator.getDefault().getDialogSettings();
		this.unblock = unblock;
	}
	
	public MergeWizardStandardPage(String pageName, String title, ImageDescriptor titleImage) {
		this(pageName, title, titleImage, null, true, false);
	}	

	public void createControl(Composite parent) {
		Composite outerContainer = new Composite(parent,SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		outerContainer.setLayout(layout);
		outerContainer.setLayoutData(
		new GridData(GridData.FILL_BOTH));
		
		Label fromLabel = new Label(outerContainer, SWT.NONE);
		fromLabel.setText(Messages.MergeWizardStandardPage_from);
		if (unblock) fromCombo = new Combo(outerContainer, SWT.BORDER | SWT.READ_ONLY);
		else fromCombo = new Combo(outerContainer, SWT.BORDER);
		GridData data = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		fromCombo.setLayoutData(data);

		fromCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (fromCombo.getText().startsWith("/")) //$NON-NLS-1$
					fromUrl = repositoryLocation + fromCombo.getText().trim();
				else
					fromUrl = fromCombo.getText().trim();
				if (viewer != null) viewer.refresh();
				setPageComplete(canFinish());
			}			
		});
		Button selectFromButton = new Button(outerContainer, SWT.PUSH);
		selectFromButton.setText(Messages.MergeWizardStandardPage_select);
		selectFromButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ChooseUrlDialog dialog = new ChooseUrlDialog(getShell(), resource);
				dialog.setIncludeBranchesAndTags(resources.length == 1);                
				if ((dialog.open() == ChooseUrlDialog.OK) && (dialog.getUrl() != null)) {
                	fromUrl = dialog.getUrl();
                	fromCombo.setText(fromUrl.substring(repositoryLocation.length()));
                }				
			}			
		});
		if (unblock) selectFromButton.setVisible(false);
		
		MergeWizard wizard = (MergeWizard)getWizard();
		resources = wizard.getResources();
		if (resources != null) {
			commonRoot = getCommonRoot();
			if (resources.length > 1) {
				relativeButton = new Button(outerContainer, SWT.CHECK);
				relativeButton.setText(Messages.MergeWizardStandardPage_relativeTo);
				data = new GridData();
				data.horizontalSpan = 3;
				relativeButton.setLayoutData(data);
				relativeButton.setSelection(true);
				relativeButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						settings.put(MergeWizard.LAST_RELATIVE_PATH_CHOICE, relativeButton.getSelection());
						viewer.refresh();
					}
				});
				
				Label label = new Label(outerContainer, SWT.NONE);
				label.setText(Messages.MergeWizardStandardPage_resources);
				data = new GridData();
				data.horizontalSpan = 3;
				label.setLayoutData(data);
				
				table = new Table(outerContainer, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
				table.setLinesVisible(false);
				table.setHeaderVisible(false);
				data = new GridData(GridData.FILL_HORIZONTAL);
				data.heightHint = 85;
				data.horizontalSpan = 3;
				table.setLayoutData(data);
				TableLayout tableLayout = new TableLayout();
				table.setLayout(tableLayout);
				viewer = new TableViewer(table);
				viewer.setContentProvider(new MergeContentProvider());
				ILabelDecorator decorator = PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator();
				viewer.setLabelProvider(new TableDecoratingLabelProvider(new MergeLabelProvider(), decorator));
				for (int i = 0; i < columnHeaders.length; i++) {
					tableLayout.addColumnData(columnLayouts[i]);
					TableColumn tc = new TableColumn(table, SWT.NONE,i);
					tc.setResizable(columnLayouts[i].resizable);
					tc.setText(columnHeaders[i]);
				}			
				viewer.setInput(this);		
			}	
		}
		
		Group revisionsGroup = new Group(outerContainer, SWT.NONE);
		revisionsGroup.setText(Messages.MergeWizardStandardPage_revisions);
		GridLayout revisionsLayout = new GridLayout();
		revisionsLayout.numColumns = 1;
		revisionsGroup.setLayout(revisionsLayout);
		data = new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL);
		data.horizontalSpan = 3;
		revisionsGroup.setLayoutData(data);
		
		allButton = new Button(revisionsGroup, SWT.RADIO);
		allButton.setText(Messages.MergeWizardStandardPage_allEligible);
		selectButton = new Button(revisionsGroup, SWT.RADIO);
		selectButton.setText(Messages.MergeWizardStandardPage_selectRevisions);
	
		boolean selectRevisions = false;
		try {
			selectRevisions =settings.getBoolean("MergeWizardStandardPage.selectRevisions"); //$NON-NLS-1$
		} catch (Exception e) {}
		if (selectRevisions) selectButton.setSelection(true);
		else allButton.setSelection(true);
		
		SelectionListener selectionListener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				settings.put("MergeWizardStandardPage.selectRevisions", selectButton.getSelection()); //$NON-NLS-1$
				setPageComplete(canFinish());
				if (multipleGroup != null) {
					multipleGroup.setEnabled(selectButton.getSelection());
					rootButton.setEnabled(selectButton.getSelection());
					separateButton.setEnabled(selectButton.getSelection());
				}
			}			
		};
		
		allButton.addSelectionListener(selectionListener);
		selectButton.addSelectionListener(selectionListener);
		
		if (resources.length > 1) {
			multipleGroup = new Composite(revisionsGroup, SWT.NONE);
			GridLayout multipleLayout = new GridLayout();
			multipleLayout.numColumns = 1;
			multipleLayout.marginLeft = 20;
			multipleGroup.setLayout(multipleLayout);
			data = new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL);
			multipleGroup.setLayoutData(data);	
			rootButton = new Button(multipleGroup, SWT.RADIO);
			rootButton.setText(Messages.MergeWizardStandardPage_0);
			separateButton = new Button(multipleGroup, SWT.RADIO);
			separateButton.setText(Messages.MergeWizardStandardPage_1);
			multipleGroup.setEnabled(selectButton.getSelection());
			rootButton.setEnabled(selectButton.getSelection());
			separateButton.setEnabled(selectButton.getSelection());
			boolean retrieveEligibleRevisionsSeparately = false;
			try {
				retrieveEligibleRevisionsSeparately = settings.getBoolean(MergeWizard.LAST_RETRIEVE_ELIGIBLE_REVISIONS_SEPARATELY);
			} catch (Exception e) {
				settings.put(MergeWizard.LAST_RETRIEVE_ELIGIBLE_REVISIONS_SEPARATELY, false);
			}
			if (retrieveEligibleRevisionsSeparately) separateButton.setSelection(true);
			else rootButton.setSelection(true);
			SelectionListener multipleListener = new SelectionAdapter() {
				public void widgetSelected(SelectionEvent se) {
					settings.put(MergeWizard.LAST_RETRIEVE_ELIGIBLE_REVISIONS_SEPARATELY, separateButton.getSelection());
					((MergeWizard)getWizard()).setRetrieveRevisionsMethodChanged(true);
				}				
			};
			rootButton.addSelectionListener(multipleListener);
			separateButton.addSelectionListener(multipleListener);
		}
		
		if (!showRevisionsButtons) revisionsGroup.setVisible(false);
		
		setPageComplete(canFinish());
		
		if (message == null) setMessage(Messages.MergeWizardStandardPage_specifyLocation);
		else setMessage(message);
		
		setControl(outerContainer);		
	}
	
	public String getMergeFrom() {
		return fromCombo.getText().trim();
	}
	
	public boolean selectRevisions() {
		return selectButton.getSelection();
	}
	
	private boolean canFinish() {
		setErrorMessage(null);
		if (fromCombo.getText().trim().length() == 0) return false;
		if (!validateUrl()) {
			setErrorMessage(Messages.MergeWizardStandardPage_invalidUrl);
			return false;
		}
		return true;
	}
	
	private boolean validateUrl() {
		if (!fromCombo.getText().startsWith("/")) { //$NON-NLS-1$
			try {
				new SVNUrl(fromCombo.getText().trim());
			} catch (MalformedURLException e) {
				return false;
			}			
		}
		return true;
	}

	public String getFromUrl() {
		return fromUrl;
	}
	
	public void setVisible(boolean visible) {
		if (visible && fromUrl == null) {
			initializeLocations();
		}
		super.setVisible(visible);
	}

	private void initializeLocations() {
		MergeWizard wizard = (MergeWizard)getWizard();
		resource = wizard.getResource();
		resources = wizard.getResources();
		svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
		mergeInfoPaths = null;
		try {
			fromUrl = svnResource.getStatus().getUrlString();
		} catch (Exception e) { Activator.handleError(e); }
		repositoryLocation = svnResource.getRepository().getLocation();	
		if (((MergeWizard)getWizard()).suggestMergeSources()) {
			IRunnableWithProgress runnable = new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.setTaskName(Messages.MergeWizardStandardPage_retrievingMergeSourceInfo);
					monitor.beginTask(Messages.MergeWizardStandardPage_retrievingMergeSourceInfo, IProgressMonitor.UNKNOWN);
					monitor.subTask(""); //$NON-NLS-1$
					ISVNClientAdapter svnClient = null;
					try {
						svnClient = svnResource.getRepository().getSVNClient();
						try {
							if (unblock) {
								try {
									mergeInfo = svnClient.getMergeInfo(new SVNUrl(commonRoot), SVNRevision.HEAD);
								} catch (Exception e) {}
								if (mergeInfo != null) mergeInfoPaths = mergeInfo.getPaths();
								if (mergeInfoPaths == null || mergeInfoPaths.length == 0) {
									Display.getDefault().asyncExec(new Runnable() {				
										public void run() {
											setErrorMessage(Messages.MergeWizardStandardPage_noRevisionsToUnblock);
										}
									});
								}
							} else {
								mergeInfoPaths = svnClient.suggestMergeSources(new SVNUrl(commonRoot), SVNRevision.HEAD);
							}
						} catch (Exception e1) {}
			        } catch (Exception e) {
			        	Activator.handleError(e);
			        }
					finally {
						svnResource.getRepository().returnSVNClient(svnClient);
					}
			        monitor.done();
				}		
			};
			try {
				getContainer().run(true, false, runnable);
			} catch (Exception e2) {
				Activator.handleError(e2);
			}
		}
        boolean valueAdded = false;        
        List<String> fromUrls = new ArrayList<String>();
		if (mergeInfoPaths != null) {
			for (int i = 0; i < mergeInfoPaths.length; i++) {
				String url = mergeInfoPaths[i].substring(repositoryLocation.length());
				if (!fromUrls.contains(url)) fromUrls.add(url);
				valueAdded = true;
			}
		}		
		String previousFromUrls = null;
		String previousFromUrl = null;
		try {
			previousFromUrls = Activator.getDefault().getDialogSettings().get("mergeFromUrls_" + commonRoot);
		} catch (Exception e) {}
		if (previousFromUrls != null) {
			String[] urls = previousFromUrls.split("\\,");
			for (String url : urls) {
				if (!fromUrls.contains(url)) fromUrls.add(url);
				valueAdded = true;
			}
			if (urls.length > 0) previousFromUrl = urls[0];
		}
		
		if (!valueAdded && !unblock && commonRoot != null) {
			fromUrls.add(commonRoot.substring(repositoryLocation.length()));
		}
		
		for (String url : fromUrls) {
			fromCombo.add(url);
		}

		if (previousFromUrl != null) fromCombo.setText(previousFromUrl);
		else if (fromCombo.getItemCount() > 0) fromCombo.setText(fromCombo.getItem(0));
	}	

	public IResource getResource() {
		return resource;
	}
	
	public IResource[] getResources() {
		return resources;
	}
	
	public SVNUrl getUrl() {
		SVNUrl url = null;
		try {
			url = new SVNUrl(fromUrl);
		} catch (MalformedURLException e) {
			Activator.handleError(e);
		}
		return url;		
	}
	
	public SVNUrl[] getUrls() {
		if (resources.length == 1) {
			SVNUrl[] urls = { getUrl() };
			return urls;
		}
		final boolean[] useRelativePath = new boolean[1];
		Display.getDefault().syncExec(new Runnable() {
			
			public void run() {
				useRelativePath[0] = relativeButton != null && relativeButton.getSelection();
			}
		});
		SVNUrl[] urls = new SVNUrl[mergeResources.length];
		for (int i = 0; i < mergeResources.length; i++) {
			try {
				if (!useRelativePath[0]) {
					urls[i] = new SVNUrl(fromUrl);
				} else {
					if (fromUrl.endsWith("/")) urls[i] = new SVNUrl(fromUrl + mergeResources[i].getPartialPath()); //$NON-NLS-1$
					else urls[i] = new SVNUrl(fromUrl + "/" + mergeResources[i].getPartialPath()); //$NON-NLS-1$
				}
			} catch (MalformedURLException e) {
				Activator.handleError(e);
			}
		}
		return urls;
	}
	
    private String getCommonRoot() {
    	commonRoot = ((MergeWizard)getWizard()).getCommonRoot();
    	urlStrings = ((MergeWizard)getWizard()).getUrlStrings();
    	mergeResources = new MergeResource[resources.length];
    	for (int i = 0; i < resources.length; i++) {
    		if (urlStrings[i].length() <= commonRoot.length()) mergeResources[i] = new MergeResource(resources[i], commonRoot);
    		else mergeResources[i] = new MergeResource(resources[i], urlStrings[i].substring(commonRoot.length() + 1));
    	}
 
    	return commonRoot;
    }
    
    public String getCommonRoot(boolean calculateRoot) {
    	if (calculateRoot) return getCommonRoot();
    	else return commonRoot;
    }
    
	public ISVNMergeInfo getMergeInfo() {
		return mergeInfo;
	} 
    
    private class MergeResource implements IAdaptable {
    	private IResource resource;
    	private String partialPath;
    	public MergeResource(IResource resource, String partialPath) {
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
		@SuppressWarnings("unchecked")
		public Object getAdapter(Class adapter) {
			if (IResource.class == adapter) return resource;
			return null;
		}
    }
    
	class MergeLabelProvider extends LabelProvider implements ITableLabelProvider {
		WorkbenchLabelProvider workbenchLabelProvider = new WorkbenchLabelProvider();
		
		public String getColumnText(Object element, int columnIndex) {
			return getText(element);
		}
		
		public String getText(Object element) {
			MergeResource mergeResource = (MergeResource)element;
			if (relativeButton.getSelection()) return mergeResource.getPartialPath() + " [" + fromCombo.getText() + "/" +mergeResource.getPartialPath() + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			else return mergeResource.getPartialPath() + " [" + fromCombo.getText() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
		}

		public Image getColumnImage(Object element, int columnIndex) {
			return getImage(element);
		}

		public Image getImage(Object element) {
			MergeResource mergeResource = (MergeResource)element;
			return workbenchLabelProvider.getImage(mergeResource.getResource());
		}
	
	}    
    
	class MergeContentProvider implements IStructuredContentProvider {
		public void dispose() {
		}
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
		public Object[] getElements(Object obj) {
			return mergeResources;
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
