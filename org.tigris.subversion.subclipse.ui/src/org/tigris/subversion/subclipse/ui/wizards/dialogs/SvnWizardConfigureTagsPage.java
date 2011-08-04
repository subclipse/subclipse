package org.tigris.subversion.subclipse.ui.wizards.dialogs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.history.Alias;
import org.tigris.subversion.subclipse.core.history.AliasManager;
import org.tigris.subversion.subclipse.core.history.Branches;
import org.tigris.subversion.subclipse.core.history.Tags;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.dialogs.ChooseUrlDialog;
import org.tigris.subversion.subclipse.ui.repository.model.SVNModelElement;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNInfo;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class SvnWizardConfigureTagsPage extends SvnWizardDialogPage {
	private ISVNLocalResource[] svnResources;
	private ISVNClientAdapter svnClient;
	private Branches branches;
	private Tags tags;
	private TreeViewer treeViewer;
	private Action deleteAction;
	private Action addTagAction;
	private Action addBranchAction;
	private Group tagGroup;
	private Label revisionLabel;
	private Text revisionText;
	private Label nameLabel;
	private Text nameText;
	private Label pathLabel;
	private Text pathText;
	private Button browseButton;
	private Button branchButton;
	private Button applyButton;
	private Button deleteButton;
	private boolean updates = false;
	private boolean tagUpdatePending = false;
	private Alias previousAlias;
	
    private static final int LIST_HEIGHT_HINT = 250;
    private static final int LIST_WIDTH_HINT = 450;	

	public SvnWizardConfigureTagsPage(ISVNLocalResource[] svnResources) {
		super("ConfigureTagsDialog", Policy.bind("ConfigureTagsDialog.title")); //$NON-NLS-1$ //$NON-NLS-2$		
		this.svnResources = svnResources;
		deleteAction = new DeleteAction();
		deleteAction.setText(Policy.bind("ConfigureTagsDialog.delete")); //$NON-NLS-1$
		addBranchAction = new AddBranchAction();
		addBranchAction.setText(Policy.bind("ConfigureTagsDialog.addBranch")); //$NON-NLS-1$
		addTagAction = new AddTagAction();
		addTagAction.setText(Policy.bind("ConfigureTagsDialog.addTag")); //$NON-NLS-1$		
	}

	public void createButtonsForButtonBar(Composite parent, SvnWizardDialog wizardDialog) {
		applyButton = wizardDialog.createButton(parent, 2, Policy.bind("ConfigureTagsDialog.apply"), false); //$NON-NLS-1$
		applyButton.setEnabled(false);
		applyButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				new UpdateAction().run();
			}
		});
		deleteButton = wizardDialog.createButton(parent, 3, Policy.bind("ConfigureTagsDialog.delete"), false); //$NON-NLS-1$
		deleteButton.setEnabled(false);
		deleteButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				new DeleteAction().run();
			}
		});		
	}

	public void createControls(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Composite urlGroup = new Composite(composite, SWT.NONE);
		GridLayout urlLayout = new GridLayout();
		urlLayout.numColumns = 2;
		urlGroup.setLayout(urlLayout);
		urlGroup.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		
		Label urlLabel = new Label(urlGroup, SWT.NONE);
		urlLabel.setText(Policy.bind("ConfigureTagsDialog.url")); //$NON-NLS-1$
		Text urlText = new Text(urlGroup, SWT.BORDER);
		GridData data = new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL);
		urlText.setLayoutData(data);
		urlText.setEditable(false);
		try {
			if (svnResources.length == 1) {
				urlText.setText(svnResources[0].getStatus().getUrlString());
			} else {
				urlText.setText(Policy.bind("SvnWizardConfigureTagsPage.0")); //$NON-NLS-1$
			}
			svnClient = svnResources[0].getRepository().getSVNClient();
		} catch (SVNException e) {}
		
		getBranchesAndTags();

		treeViewer = new TreeViewer(composite, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI);		
		treeViewer.setContentProvider(new TagsContentProvider(svnResources[0].getResource()));
		treeViewer.setLabelProvider(new TagsLabelProvider());
		treeViewer.setInput(svnResources[0]);
		
		data = new GridData(GridData.FILL_BOTH | GridData.GRAB_VERTICAL);
		data.heightHint = LIST_HEIGHT_HINT;
		data.widthHint = LIST_WIDTH_HINT;
		treeViewer.getControl().setLayoutData(data);

		tagGroup = new Group(composite, SWT.NONE);
		GridLayout tagLayout = new GridLayout();
		tagLayout.numColumns = 3;
		tagGroup.setLayout(tagLayout);
		tagGroup.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		
		revisionLabel = new Label(tagGroup, SWT.NONE);
		revisionLabel.setText(Policy.bind("ConfigureTagsDialog.revision")); //$NON-NLS-1$
		revisionText = new Text(tagGroup, SWT.BORDER);
		data = new GridData();
		data.widthHint = 50;
		data.horizontalSpan = 2;
		revisionText.setLayoutData(data);
		
		nameLabel = new Label(tagGroup, SWT.NONE);
		nameLabel.setText(Policy.bind("ConfigureTagsDialog.name")); //$NON-NLS-1$
		nameText = new Text(tagGroup, SWT.BORDER);
		data = new GridData();
		data.widthHint = 300;
		data.horizontalSpan = 2;
		nameText.setLayoutData(data);
		
		pathLabel = new Label(tagGroup, SWT.NONE);
		pathLabel.setText(Policy.bind("ConfigureTagsDialog.path")); //$NON-NLS-1$
		pathText = new Text(tagGroup, SWT.BORDER);
		data = new GridData();
		data.widthHint = 300;
		pathText.setLayoutData(data);
		browseButton = new Button(tagGroup, SWT.PUSH);
		browseButton.setText(Policy.bind("ConfigureTagsDialog.browse")); //$NON-NLS-1$
		browseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ChooseUrlDialog dialog = new ChooseUrlDialog(getShell(), svnResources[0].getResource());
				dialog.setIncludeBranchesAndTags(false);
				dialog.setFoldersOnly(true);
				if (dialog.open() == ChooseUrlDialog.CANCEL) return;
				final String url = dialog.getUrl();
				if (url != null) {
					nameText.setText(dialog.getName());
					BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
						public void run() {
							try {
								SVNUrl svnUrl = new SVNUrl(url);
								ISVNInfo svnInfo = svnClient.getInfo(svnUrl);
								revisionText.setText(svnInfo.getLastChangedRevision().toString());
								String repositoryUrl = svnResources[0].getRepository().getUrl().toString();
								pathText.setText(url.substring(repositoryUrl.length()));
							} catch (Exception e1) {
								MessageDialog.openError(getShell(), Policy.bind("ConfigureTagsDialog.title"), e1.getMessage()); //$NON-NLS-1$
							}
						}					
					});
				}
			}
		});
		
		branchButton = new Button(tagGroup, SWT.CHECK);
		branchButton.setText(Policy.bind("ConfigureTagsDialog.branch")); //$NON-NLS-1$
		data = new GridData();
		data.horizontalSpan = 3;
		branchButton.setLayoutData(data);
		
		setTagGroupEnablement(false);
		
		treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				if (tagUpdatePending) {
					if (MessageDialog.openQuestion(getShell(), Policy.bind("ConfigureTagsDialog.title"), //$NON-NLS-1$
							Policy.bind("ConfigureTagsDialog.pendingUpdate"))) { //$NON-NLS-1$
						new UpdateAction(previousAlias).run();
					}
					tagUpdatePending = false;
				}
				applyButton.setEnabled(false);
				IStructuredSelection selection = (IStructuredSelection)treeViewer.getSelection();
				boolean deleteEnabled = false;
				Iterator iter = selection.iterator();
				while (iter.hasNext()) {
					if (iter.next() instanceof Alias) {
						deleteEnabled = true;
						break;
					}
				}
				deleteButton.setEnabled(deleteEnabled);
				if (selection.size() == 1 && selection.getFirstElement() instanceof Alias) {
					Alias alias = (Alias)selection.getFirstElement();
					previousAlias = alias;
					if (alias.isBranch()) {
						tagGroup.setText(Policy.bind("ConfigureTagsDialog.branchHeader")); //$NON-NLS-1$
						branchButton.setSelection(true);
					} else {
						tagGroup.setText(Policy.bind("ConfigureTagsDialog.tagHeader")); //$NON-NLS-1$
						branchButton.setSelection(false);
					}
					revisionText.setText(Integer.toString(alias.getRevision()));
					nameText.setText(alias.getName());
					if (alias.getRelativePath() == null) pathText.setText(""); //$NON-NLS-1$
					else pathText.setText(alias.getRelativePath());
					setTagGroupEnablement(true);
				} else {
					tagGroup.setText(""); //$NON-NLS-1$
					revisionText.setText(""); //$NON-NLS-1$
					nameText.setText(""); //$NON-NLS-1$
					pathText.setText(""); //$NON-NLS-1$
					branchButton.setSelection(false);
					setTagGroupEnablement(false);
				}
			}			
		});
		
		ModifyListener modifyListener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				applyButton.setEnabled(canUpdate());
				if (applyButton.isEnabled()) tagUpdatePending = true;
				else tagUpdatePending = false;
			}			
		};
		
		revisionText.addModifyListener(modifyListener);
		nameText.addModifyListener(modifyListener);
		pathText.addModifyListener(modifyListener);
		
		FocusListener focusListener = new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				((Text)e.getSource()).selectAll();
			}
			public void focusLost(FocusEvent e) {
				((Text)e.getSource()).setText(((Text)e.getSource()).getText());
			}					
		};
		
		revisionText.addFocusListener(focusListener);
		nameText.addFocusListener(focusListener);
		pathText.addFocusListener(focusListener);
		
		branchButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				applyButton.setEnabled(canUpdate());
				if (applyButton.isEnabled()) tagUpdatePending = true;
				else tagUpdatePending = false;
			}
		});
		
        MenuManager menuMgr = new MenuManager();
        Tree tree = treeViewer.getTree();
        Menu menu = menuMgr.createContextMenu(tree);
        menuMgr.addMenuListener(new IMenuListener() {
            public void menuAboutToShow(IMenuManager manager) {
            	IStructuredSelection selection = (IStructuredSelection)treeViewer.getSelection();
            	Iterator iter = selection.iterator();
            	boolean deleteAdded = false;
            	boolean addAdded = false;
            	while (iter.hasNext()) {
            		Object selectedItem = iter.next();
            		if (!deleteAdded && selectedItem instanceof Alias) {
            			manager.add(deleteAction);
            			deleteAdded = true;
            		}
            		if (!addAdded && selectedItem instanceof ISVNRemoteFolder) {
            			manager.add(addBranchAction);
            			manager.add(addTagAction);
            			addAdded = true;
            		}
            		if (deleteAdded && addAdded) break;
            	}
            }
        });
        menuMgr.setRemoveAllWhenShown(true);
        tree.setMenu(menu);

        // set F1 help
        PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.CONFIGURE_TAGS_DIALOG);			
	}

	public String getWindowTitle() {
		return Policy.bind("ConfigureTagsDialog.title"); //$NON-NLS-1$
	}

	public boolean performCancel() {
		returnSVNClient();
		if (updates) {
			if (!MessageDialog.openQuestion(getShell(), Policy.bind("ConfigureTagsDialog.title"), //$NON-NLS-1$
					Policy.bind("ConfigureTagsDialog.confirmExit"))) return false; //$NON-NLS-1$			
		}		
		return true;
	}

	public boolean performFinish() {
		returnSVNClient();
		if (updates) {
			try {
				String propertyValue = getPropertyValue();
				for (int i = 0; i < svnResources.length; i++) {
					svnResources[i].setSvnProperty("subclipse:tags", propertyValue, false); //$NON-NLS-1$					
				}
			} catch (SVNException e) {
				if (!e.operationInterrupted()) {
					MessageDialog.openError(getShell(), Policy.bind("ConfigureTagsDialog.title"), e.getMessage()); //$NON-NLS-1$
				}
				return false;
			}
		}		
		return true;
	}
	
	private void returnSVNClient() {
		if (svnClient != null) {
			svnResources[0].getRepository().returnSVNClient(svnClient);
		}
	}

	public void saveSettings() {
	}

	public void setMessage() {
		setMessage(Policy.bind("ConfigureTagsDialog.text")); //$NON-NLS-1$
	}
	
	private void setTagGroupEnablement(boolean enable) {
		revisionLabel.setEnabled(enable);
		revisionText.setEnabled(enable);
		nameLabel.setEnabled(enable);
		nameText.setEnabled(enable);
		pathLabel.setEnabled(enable);
		pathText.setEnabled(enable);
		browseButton.setEnabled(enable);
		branchButton.setEnabled(enable);
		tagGroup.setEnabled(enable);	
	}
	
	private boolean canUpdate() {
		if (revisionText.getText().trim().length() == 0 || nameText.getText().trim().length() == 0) return false;
		IStructuredSelection selection = (IStructuredSelection)treeViewer.getSelection();
		Alias alias = (Alias)selection.getFirstElement();
		if (revisionText.getText().trim().equals(Integer.toString(alias.getRevision())) && nameText.getText().trim().equals(alias.getName()) && pathText.getText().trim().equals(alias.getRelativePath()) && branchButton.getSelection() == alias.isBranch())
			return false;
		return true;
	}
	
	private void getBranchesAndTags() {
		AliasManager aliasManager = new AliasManager(svnResources[0].getResource(), false);
		Alias[] branchAliases = aliasManager.getBranches();
		branches = new Branches(branchAliases);
		Alias[] tagAliases = aliasManager.getTags();
		tags = new Tags(tagAliases);
	}
	
	private String getPropertyValue() {
		StringBuffer propertyValue = new StringBuffer();
		Alias[] branchAliases = branches.getBranches();
		for (int i = 0; i < branchAliases.length; i++) {
			if (branchAliases[i].getRevision() > 0) {
				if (propertyValue.length() > 0) propertyValue.append("\n"); //$NON-NLS-1$
				Alias branch = branchAliases[i];
				propertyValue.append(branch.getRevision() + "," + branch.getName()); //$NON-NLS-1$
				if (branch.getRelativePath() != null) propertyValue.append("," + branch.getRelativePath()); //$NON-NLS-1$
				if (branch.isBranch()) propertyValue.append(",branch"); //$NON-NLS-1$
				else propertyValue.append(",tag"); //$NON-NLS-1$			
			}
		}
		Alias[] tagAliases = tags.getTags();
		for (int i = 0; i < tagAliases.length; i++) {
			if (tagAliases[i].getRevision() > 0) {
				if (propertyValue.length() > 0) propertyValue.append("\n"); //$NON-NLS-1$
				Alias tag = tagAliases[i];
				propertyValue.append(tag.getRevision() + "," + tag.getName()); //$NON-NLS-1$
				if (tag.getRelativePath() != null) propertyValue.append("," + tag.getRelativePath()); //$NON-NLS-1$
				if (tag.isBranch()) propertyValue.append(",branch"); //$NON-NLS-1$
				else propertyValue.append(",tag"); //$NON-NLS-1$
			}
		}	
		return propertyValue.toString();
	}
	
	class TagsContentProvider extends WorkbenchContentProvider {
		
		public TagsContentProvider(IResource resource) {
			super();
			AliasManager tagManager = new AliasManager(resource);
			Alias[] branchAliases = tagManager.getBranches();
			Alias[] tagAliases = tagManager.getTags();
			branches = new Branches(branchAliases);
			tags = new Tags(tagAliases);			
		}
		
		public boolean hasChildren(Object element) {
			if (element == null) {
				return false;
			}
			
			if (element instanceof Branches || element instanceof Tags) return true;
			if (element instanceof Alias) return false;
			
			if (element instanceof ISVNRepositoryLocation)
				return true;
			
			// the + box will always appear, but then disappear
			// if not needed after you first click on it.
			if (element instanceof ISVNRemoteResource) {
				return ((ISVNRemoteResource)element).isContainer();
			} 
			return super.hasChildren(element);
		}
		
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof ISVNLocalResource) {
				Object[] rootChildren = new Object[3];
				rootChildren[0] = ((ISVNLocalResource)parentElement).getRepository();
				rootChildren[1] = branches;
				rootChildren[2] = tags;
				return rootChildren;
			}
			if (parentElement instanceof Branches) return ((Branches)parentElement).getBranches();
			if (parentElement instanceof Tags) return ((Tags)parentElement).getTags();
			IWorkbenchAdapter adapter = getAdapter(parentElement);
			if (adapter instanceof SVNModelElement) {
				Object[] children = ((SVNModelElement)adapter).getChildren(parentElement);
				ArrayList folderArray = new ArrayList();
				for (int i = 0; i < children.length; i++) {
					if (children[i] instanceof ISVNRemoteFolder) folderArray.add(children[i]);
				}
				children = new Object[folderArray.size()];
				folderArray.toArray(children);
				return children;
			}
			return super.getChildren(parentElement);
		}
	}

	class UpdateAction extends Action {
		private Alias previousAlias;
		
		public UpdateAction() {
			super();
		}
		
		public UpdateAction(Alias alias) {
			this();
			previousAlias = alias;
		}
		
		public void run() {
			updates = true;
			tagUpdatePending = false;
			boolean branchAttributeChanged = false;
			Alias alias = null;
			if (previousAlias == null) {
				IStructuredSelection selection = (IStructuredSelection)treeViewer.getSelection();
				alias = (Alias)selection.getFirstElement();
			} else alias = previousAlias;
			alias.setRevision(Integer.parseInt(revisionText.getText().trim()));
			alias.setName(nameText.getText().trim());
			if (pathText.getText().trim().length() == 0) alias.setRelativePath(null);
			else alias.setRelativePath(pathText.getText().trim());
			if (alias.isBranch() != branchButton.getSelection()) branchAttributeChanged = true;
			alias.setBranch(branchButton.getSelection());
			if (branchAttributeChanged) {
				ArrayList branchArray = new ArrayList();
				ArrayList tagArray = new ArrayList();
				Alias[] branchAliases = branches.getBranches();
				Alias[] tagAliases = tags.getTags();
				for (int i = 0; i < branchAliases.length; i++) {
					if (branchAliases[i].isBranch()) branchArray.add(branchAliases[i]);
					else tagArray.add(branchAliases[i]);
				}
				for (int i = 0; i < tagAliases.length; i++) {
					if (tagAliases[i].isBranch()) branchArray.add(tagAliases[i]);
					else tagArray.add(tagAliases[i]);
				}
				branchAliases = new Alias[branchArray.size()];
				tagAliases = new Alias[tagArray.size()];
				branchArray.toArray(branchAliases);
				tagArray.toArray(tagAliases);
				Arrays.sort(branchAliases);
				Arrays.sort(tagAliases);
				branches.setBranches(branchAliases);
				tags.setTags(tagAliases);
			}
			treeViewer.refresh();
			applyButton.setEnabled(false);
		}		
	}
	
	class DeleteAction extends Action {
		public void run() {
			updates = true;
			IStructuredSelection selection = (IStructuredSelection)treeViewer.getSelection();
			ArrayList branchArray = new ArrayList();
			ArrayList tagArray = new ArrayList();
			Alias[] branchAliases = branches.getBranches();
			Alias[] tagAliases = tags.getTags();
			for (int i = 0; i < branchAliases.length; i++) branchArray.add(branchAliases[i]);
			for (int i = 0; i < tagAliases.length; i++) tagArray.add(tagAliases[i]);
			Iterator iter = selection.iterator();
			while (iter.hasNext()) {
				Object selectedItem = iter.next();
				if (selectedItem instanceof Alias) {
					Alias alias = (Alias)selectedItem;
					if (alias.isBranch()) branchArray.remove(alias);
					else tagArray.remove(alias);
				}
			}
			branchAliases = new Alias[branchArray.size()];
			branchArray.toArray(branchAliases);
			branches.setBranches(branchAliases);
			tagAliases = new Alias[tagArray.size()];
			tagArray.toArray(tagAliases);
			tags.setTags(tagAliases);
			treeViewer.refresh();			
		}
	}
	
	class AddBranchAction extends Action {
		public void run() {
			updates = true;
			ArrayList branchArray = new ArrayList();
			Alias[] branchAliases = branches.getBranches();
			for (int i = 0; i < branchAliases.length; i++) branchArray.add(branchAliases[i]);
			IStructuredSelection selection = (IStructuredSelection)treeViewer.getSelection();
			Iterator iter = selection.iterator();
			while (iter.hasNext()) {
				Object selectedItem = iter.next();
				if (selectedItem instanceof ISVNRemoteFolder) {
					ISVNRemoteFolder folder = (ISVNRemoteFolder)selectedItem;
					Alias newAlias = new Alias();
					newAlias.setBranch(true);
					newAlias.setName(folder.getName());
					String revNo = folder.getLastChangedRevision().toString();
					int revision = Integer.parseInt(revNo);
					newAlias.setRevision(revision);
					newAlias.setUrl(folder.getUrl().toString());
					String relativePath = folder.getUrl().toString().substring(folder.getRepository().getUrl().toString().length());
					newAlias.setRelativePath(relativePath);
					branchArray.add(newAlias);
				}
			}
			branchAliases = new Alias[branchArray.size()];
			branchArray.toArray(branchAliases);
			Arrays.sort(branchAliases);
			branches.setBranches(branchAliases);
			treeViewer.refresh();
		}		
	}
	
	class AddTagAction extends Action {
		public void run() {
			updates = true;
			ArrayList tagArray = new ArrayList();
			Alias[] tagAliases = tags.getTags();
			for (int i = 0; i < tagAliases.length; i++) tagArray.add(tagAliases[i]);
			IStructuredSelection selection = (IStructuredSelection)treeViewer.getSelection();
			Iterator iter = selection.iterator();
			while (iter.hasNext()) {
				Object selectedItem = iter.next();
				if (selectedItem instanceof ISVNRemoteFolder) {
					ISVNRemoteFolder folder = (ISVNRemoteFolder)selectedItem;
					Alias newAlias = new Alias();
					newAlias.setBranch(false);
					newAlias.setName(folder.getName());
					String revNo = folder.getLastChangedRevision().toString();
					int revision = Integer.parseInt(revNo);
					newAlias.setRevision(revision);
					newAlias.setUrl(folder.getUrl().toString());
					String relativePath = folder.getUrl().toString().substring(folder.getRepository().getUrl().toString().length());
					newAlias.setRelativePath(relativePath);
					tagArray.add(newAlias);
				}
			}
			tagAliases = new Alias[tagArray.size()];
			tagArray.toArray(tagAliases);
			Arrays.sort(tagAliases);
			tags.setTags(tagAliases);
			treeViewer.refresh();
		}		
	}
	
	class TagsLabelProvider extends LabelProvider implements IColorProvider, IFontProvider{
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
			return workbenchLabelProvider.getImage(element);
		}

		public String getText(Object element) {
			if (element instanceof Branches) return Policy.bind("ChooseUrlDialog.branches"); //$NON-NLS-1$
			if (element instanceof Tags) return Policy.bind("ChooseUrlDialog.tags"); //$NON-NLS-1$
			if (element instanceof Alias) return ((Alias)element).getName();
			return workbenchLabelProvider.getText(element);
		}
		
	}	

}
