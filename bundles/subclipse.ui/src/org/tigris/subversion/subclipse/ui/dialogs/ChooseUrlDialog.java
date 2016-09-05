/*******************************************************************************
 * Copyright (c) 2004, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.dialogs;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.history.Alias;
import org.tigris.subversion.subclipse.core.history.AliasManager;
import org.tigris.subversion.subclipse.core.history.Branches;
import org.tigris.subversion.subclipse.core.history.Tags;
import org.tigris.subversion.subclipse.core.repo.ISVNListener;
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;
import org.tigris.subversion.subclipse.core.resources.RepositoryRootFolder;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.actions.CreateRemoteFolderAction;
import org.tigris.subversion.subclipse.ui.actions.DeleteRemoteResourceAction;
import org.tigris.subversion.subclipse.ui.repository.RepositoryFilters;
import org.tigris.subversion.subclipse.ui.repository.model.AllRootsElement;
import org.tigris.subversion.subclipse.ui.repository.model.RemoteContentProvider;

public class ChooseUrlDialog extends SubclipseTrayDialog {
    private static final int LIST_HEIGHT_HINT = 250;
    private static final int LIST_WIDTH_HINT = 450;

    private TreeViewer treeViewer;
    private Action refreshAction;
    private Action newFolderAction;
    private Action deleteFolderAction;

    private String url;
    private String name;
    private String[] urls;
    private String[] names;
    private IResource resource;
    private String message;
    private boolean multipleSelect = false;
    private ISVNRepositoryLocation repositoryLocation;
    private boolean foldersOnly = false;
    private boolean includeBranchesAndTags = true;
    
    private IDialogSettings settings;
    private RemoteContentProvider contentProvider;
    
    private static boolean needsRefresh = true;
    private boolean saveLocation = true;
    
    static {
    	ISVNListener repositoryListener = new ISVNListener() {
			public void remoteResourceCopied(ISVNRemoteResource source, ISVNRemoteFolder destination) {
				needsRefresh = true;
			}
			public void remoteResourceCreated(ISVNRemoteFolder parent, String resourceName) {
				needsRefresh = true;
			}
			public void remoteResourceDeleted(ISVNRemoteResource resource) {
				needsRefresh = true;
			}
			public void remoteResourceMoved(ISVNRemoteResource resource, ISVNRemoteFolder destinationFolder, String destinationResourceName) {
				needsRefresh = true;
			}
			public void repositoryAdded(ISVNRepositoryLocation root) {
				needsRefresh = true;
			}
			public void repositoryModified(ISVNRepositoryLocation root) {
				needsRefresh = true;
			}
			public void repositoryRemoved(ISVNRepositoryLocation root) {
				needsRefresh = true;
			}  		
    	};
    	SVNProviderPlugin.getPlugin().getRepositoryResourcesManager().addRepositoryListener(repositoryListener);
    }

    public ChooseUrlDialog(Shell parentShell, IResource resource) {
        super(parentShell);
		int shellStyle = getShellStyle();
		setShellStyle(shellStyle | SWT.RESIZE);
        this.resource = resource;
        refreshAction = new Action(Policy.bind("ChooseUrlDialog.refresh"), SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_REFRESH_ENABLED)) { //$NON-NLS-1$
            public void run() {
                refreshViewer(true);
            }
        };
        newFolderAction = new Action(Policy.bind("NewRemoteFolderWizard.title")) { //$NON-NLS-1$
            public void run() {
                CreateRemoteFolderAction createAction = new CreateRemoteFolderAction();
                createAction.selectionChanged(null, treeViewer.getSelection());
                createAction.run(null);
                refreshViewer(true);
            }
        };
        deleteFolderAction = new Action(Policy.bind("ChooseUrlDialog.delete")) { //$NON-NLS-1$
            public void run() {
                DeleteRemoteResourceAction deleteAction = new DeleteRemoteResourceAction();
                deleteAction.selectionChanged(null, treeViewer.getSelection());
                deleteAction.run(null);
                refreshViewer(true);
            }
        };
        settings = SVNUIPlugin.getPlugin().getDialogSettings();
        if (needsRefresh) refreshRepositoriesFolders();
    }

	protected Control createDialogArea(Composite parent) {
		getShell().setText(Policy.bind("ChooseUrlDialog.title")); //$NON-NLS-1$
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));

		if (message != null) {
			Label messageLabel = new Label(composite, SWT.NONE);
			messageLabel.setText(message);
		}

		if (multipleSelect) treeViewer = new TreeViewer(composite, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI);
		else treeViewer = new TreeViewer(composite, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        contentProvider = new RemoteContentProvider();
        contentProvider.setIncludeBranchesAndTags(includeBranchesAndTags);
        contentProvider.setResource(resource);
        treeViewer.setContentProvider(contentProvider);
        if( foldersOnly )
        	treeViewer.addFilter(RepositoryFilters.FOLDERS_ONLY);

        //        treeViewer.setLabelProvider(new WorkbenchLabelProvider());
        treeViewer.setLabelProvider(new RemoteLabelProvider());
        ISVNRepositoryLocation repository = null;
        if (repositoryLocation == null) {
	        if (resource == null) treeViewer.setInput(new AllRootsElement());
	        else {
	            ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
	            try {
	            	LocalResourceStatus status = svnResource.getStatus();
	            	if (status != null) {
	            		repository = svnResource.getStatus().getRepository();
	            	}
				} catch (SVNException e1) {}
				if (repository == null) {
					repository = svnResource.getRepository();
				}

	            if (!repository.getUrl().toString().equals(repository.getRepositoryRoot().toString())) {
	            	RepositoryRootFolder rootFolder = new RepositoryRootFolder(repository, repository.getRepositoryRoot(), repository.getRootFolder().getRevision());
	            	contentProvider.setRootFolder(rootFolder);
	            }
	        }
        } else {
        	repository = repositoryLocation;
        }
        
        if (repository == null) treeViewer.setInput(new AllRootsElement());
        else {
        	try {
				repository.validateConnection(new NullProgressMonitor());
				treeViewer.setInput(repository);
			} catch (SVNException e) {
				MessageDialog.openError(getShell(), Policy.bind("ChooseUrlDialog.title"), e.getMessage());
				saveLocation = false;
				cancelPressed();
				return composite;
			}
        }

		GridData data = new GridData(GridData.FILL_BOTH | GridData.GRAB_VERTICAL);
		data.heightHint = LIST_HEIGHT_HINT;
		data.widthHint = LIST_WIDTH_HINT;
		treeViewer.getControl().setLayoutData(data);
		
        // when F5 is pressed, refresh
        treeViewer.getControl().addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent event) {
                if (event.keyCode == SWT.F5) {
                    refreshAction.run();
                }
            }
        });		

        treeViewer.addDoubleClickListener(new IDoubleClickListener() {
            public void doubleClick(DoubleClickEvent e) {
                okPressed();
            }
        });

        // Create the popup menu
        MenuManager menuMgr = new MenuManager();
        Tree tree = treeViewer.getTree();
        Menu menu = menuMgr.createContextMenu(tree);
        menuMgr.addMenuListener(new IMenuListener() {
            public void menuAboutToShow(IMenuManager manager) {
                manager.add(newFolderAction);
                if (!treeViewer.getSelection().isEmpty()) manager.add(deleteFolderAction);
                manager.add(refreshAction);
            }

        });
        menuMgr.setRemoveAllWhenShown(true);
        tree.setMenu(menu);

		// set F1 help
        PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.CHOOSE_URL_DIALOG);	
        
		return composite;
	}

	@Override
	public int open() {
		try {
			return super.open();
		}
		catch (Exception e) {
			return CANCEL;
		}
	}

	protected void refreshViewer(boolean refreshRepositoriesFolders) {
        if (treeViewer == null) return;
        contentProvider.setUseDeferredContentManager(false);
        Object[] expandedObjects = treeViewer.getExpandedElements();
        if (refreshRepositoriesFolders) {
        	refreshRepositoriesFolders();
        }
        treeViewer.refresh();
        treeViewer.setExpandedElements(expandedObjects);
        contentProvider.setUseDeferredContentManager(true);
    }
    
    private void refreshRepositoriesFolders() {
    	IRunnableWithProgress runnable = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
            	SVNProviderPlugin.getPlugin().getRepositories().refreshRepositoriesFolders(monitor);
    			needsRefresh = false;
			}
    	};
        try {
			new ProgressMonitorDialog(getShell()).run(true, false, runnable);
		} catch (Exception e) {
            SVNUIPlugin.openError(getShell(), null, null, e, SVNUIPlugin.LOG_TEAM_EXCEPTIONS);
		}    	
    }

    protected void okPressed() {
    	saveLocation();
        ISelection selection = treeViewer.getSelection();
        if (!selection.isEmpty() && (selection instanceof IStructuredSelection)) {
            IStructuredSelection structured = (IStructuredSelection)selection;
            Object first = structured.getFirstElement();
            if (first instanceof ISVNRemoteResource) {
            	url = ((ISVNRemoteResource)first).getUrl().toString();
            	name = ((ISVNRemoteResource)first).getName();
            }
            if (first instanceof ISVNRepositoryLocation) url = ((ISVNRepositoryLocation)first).getUrl().toString();
            if (first instanceof Alias) url = AliasManager.transformUrl(resource, (Alias)first);
            ArrayList urlArray = new ArrayList();
            ArrayList nameArray = new ArrayList();
            Iterator iter = structured.iterator();
            while (iter.hasNext()) {
            	Object selectedItem = iter.next();
            	if (selectedItem instanceof ISVNRemoteResource) {
            		urlArray.add(((ISVNRemoteResource)selectedItem).getUrl().toString());
            		nameArray.add(((ISVNRemoteResource)selectedItem).getName());
            	}
            }
            urls = new String[urlArray.size()];
            urlArray.toArray(urls);
            names = new String[nameArray.size()];
            nameArray.toArray(names);
        }
        super.okPressed();
    }
    
    protected void cancelPressed() {
    	if (saveLocation) {
    		saveLocation();
    	}
        super.cancelPressed();
    }
    
    private void saveLocation() {
        int x = getShell().getLocation().x;
        int y = getShell().getLocation().y;
        settings.put("ChooseUrlDialog.location.x", x); //$NON-NLS-1$
        settings.put("ChooseUrlDialog.location.y", y); //$NON-NLS-1$
        x = getShell().getSize().x;
        y = getShell().getSize().y;
        settings.put("ChooseUrlDialog.size.x", x); //$NON-NLS-1$
        settings.put("ChooseUrlDialog.size.y", y); //$NON-NLS-1$   
    }
    
    protected Point getInitialLocation(Point initialSize) {
	    try {
	        int x = settings.getInt("ChooseUrlDialog.location.x"); //$NON-NLS-1$
	        int y = settings.getInt("ChooseUrlDialog.location.y"); //$NON-NLS-1$
	        return new Point(x, y);
	    } catch (NumberFormatException e) {}
        return super.getInitialLocation(initialSize);
    }
    
    protected Point getInitialSize() {
	    try {
	        int x = settings.getInt("ChooseUrlDialog.size.x"); //$NON-NLS-1$
	        int y = settings.getInt("ChooseUrlDialog.size.y"); //$NON-NLS-1$
	        return new Point(x, y);
	    } catch (NumberFormatException e) {}
        return super.getInitialSize();
    }	

    public String getUrl() {
        return url;
    }
    public void setRepositoryLocation(ISVNRepositoryLocation repositoryLocation) {
        this.repositoryLocation = repositoryLocation;
    }

	public void setFoldersOnly(boolean foldersOnly) {
		this.foldersOnly = foldersOnly;
	}

	class RemoteLabelProvider extends LabelProvider implements IColorProvider, IFontProvider{
		private WorkbenchLabelProvider workbenchLabelProvider = new WorkbenchLabelProvider();

		public Color getForeground(Object element) {
			return workbenchLabelProvider.getForeground(element);
		}

		public Color getBackground(Object element) {
			return workbenchLabelProvider.getBackground(element);
		}

		public Font getFont(Object element) {
			return workbenchLabelProvider.getFont(element);
		}

		public Image getImage(Object element) {
			if (element instanceof Branches) return SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_BRANCHES_CATEGORY).createImage();
			if (element instanceof Tags) return SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_VERSIONS_CATEGORY).createImage();
			if (element instanceof Alias) {
				if (((Alias)element).isBranch())
					return SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_BRANCH).createImage();
				else
					return SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_PROJECT_VERSION).createImage();
			}
			if (element instanceof RepositoryRootFolder) return  SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_REPOSITORY).createImage();
			return workbenchLabelProvider.getImage(element);
		}

		public String getText(Object element) {
			if (element instanceof Branches) return Policy.bind("ChooseUrlDialog.branches"); //$NON-NLS-1$
			if (element instanceof Tags) return Policy.bind("ChooseUrlDialog.tags"); //$NON-NLS-1$
			if (element instanceof Alias) return ((Alias)element).getName();
			return workbenchLabelProvider.getText(element);
		}

	}

	public void setIncludeBranchesAndTags(boolean includeBranchesAndTags) {
		this.includeBranchesAndTags = includeBranchesAndTags;
	}

	public String getName() {
		return name;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public void setMultipleSelect(boolean multipleSelect) {
		this.multipleSelect = multipleSelect;
	}

	public String[] getNames() {
		return names;
	}

	public String[] getUrls() {
		return urls;
	}

}
