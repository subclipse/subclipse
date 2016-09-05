/*******************************************************************************
 * Copyright (c) 2003, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.properties;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PropertyPage;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;
import org.tigris.subversion.subclipse.core.resources.SVNTreeConflict;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNInfo;
import org.tigris.subversion.svnclientadapter.ISVNProperty;
import org.tigris.subversion.svnclientadapter.SVNConflictDescriptor;
import org.tigris.subversion.svnclientadapter.SVNConflictVersion;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class SVNPropertyPage extends PropertyPage {
	private Text urlValue;
	private Text revisionValue;
	private Text repositoryRootValue;
	private Text repositoryUuidValue;
	private Text statusValue;
	private Text propertiesValue;
	private Text copiedFromValue;
    private Text lastChangedRevisionValue;
    private Text lastChangedDateValue;
    private Text lastCommitAuthorValue;
    private Text lockOwner;
    private Text lockCreationDate;
    private Label lockComment;
    private Text treeConflict;
	
	private ISVNLocalResource svnResource;
	private LocalResourceStatus status;
	private SVNUrl urlCopiedFrom;
	private SVNRevision revision;
	private ISVNInfo info;
	private String lockOwnerText;
	private String lockDateText;
	private String lockCommentText;

	protected Control createContents(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        composite.setLayout(layout);
        GridData data = new GridData(GridData.FILL);

        composite.setLayoutData(data);

        getStatus();
        
        addFirstSection(composite);
        
        if (status != null) {
	        addSeparator(composite);
	        addSecondSection(composite);
	        setValues();
        }
        
        Dialog.applyDialogFont(parent);
        
        PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.SVN_RESOURCE_PROPERTIES_PAGE);

        return composite;
	}
	
	private void addFirstSection(Composite parent) {
        Composite composite = createDefaultComposite(parent);

        //Label for path field
        Label label = new Label(composite, SWT.NONE);
        label.setText(Policy.bind("SVNPropertyPage.path")); //$NON-NLS-1$

        // Path text field
        Text pathValue = new Text(composite, SWT.WRAP | SWT.READ_ONLY);
        GridData gd = new GridData();
        gd.widthHint = 500;
        pathValue.setLayoutData(gd);
        pathValue.setText(((IResource) getElement()).getFullPath().toString());
        pathValue.setBackground(composite.getBackground());
        
        // Name text field
        if (!(getElement() instanceof IContainer)) {
        	label = new Label(composite, SWT.NONE);
        	label.setText(Policy.bind("SVNPropertyPage.name")); //$NON-NLS-1$
        	Text nameValue = new Text(composite, SWT.WRAP | SWT.READ_ONLY);
            gd = new GridData();
            gd.widthHint = 500;
            nameValue.setLayoutData(gd);
            nameValue.setText(((IResource) getElement()).getName());
            nameValue.setBackground(composite.getBackground());
        }

        label = new Label(composite, SWT.NONE);
        label.setText(Policy.bind("SVNPropertyPage.url")); //$NON-NLS-1$
        
        urlValue = new Text(composite, SWT.WRAP | SWT.READ_ONLY);
        gd = new GridData();
        gd.widthHint = 500;
        urlValue.setLayoutData(gd);
        urlValue.setBackground(composite.getBackground());
        
        label = new Label(composite, SWT.NONE);
        label.setText(Policy.bind("SVNPropertyPage.repositoryRoot")); //$NON-NLS-1$        
        repositoryRootValue = new Text(composite, SWT.WRAP | SWT.READ_ONLY);
        gd = new GridData();
        gd.widthHint = 500;
        repositoryRootValue.setLayoutData(gd);
        repositoryRootValue.setBackground(composite.getBackground());
        
        if (info != null && info.getUuid() != null) {
            label = new Label(composite, SWT.NONE);
            label.setText(Policy.bind("SVNPropertyPage.uuid")); //$NON-NLS-1$                	
            Text uuidValue = new Text(composite, SWT.WRAP | SWT.READ_ONLY);
            gd = new GridData();
            gd.widthHint = 500;
            uuidValue.setLayoutData(gd);
            uuidValue.setBackground(composite.getBackground());
            uuidValue.setText(info.getUuid());
        }
        
        label = new Label(composite, SWT.NONE);
        label.setText(Policy.bind("SVNPropertyPage.revision")); //$NON-NLS-1$
        revisionValue = new Text(composite, SWT.READ_ONLY);
        revisionValue.setBackground(composite.getBackground());		
	}
	
	private void addSecondSection(Composite parent) {
		Composite composite = createDefaultComposite(parent);
		
        Label label = new Label(composite, SWT.NONE);
        label.setText(Policy.bind("SVNPropertyPage.status")); //$NON-NLS-1$
        statusValue = new Text(composite, SWT.READ_ONLY);
        statusValue.setBackground(composite.getBackground());
        
        label = new Label(composite, SWT.NONE);
        label.setText(Policy.bind("SVNPropertyPage.propStatus")); //$NON-NLS-1$
        propertiesValue = new Text(composite, SWT.READ_ONLY);
        propertiesValue.setBackground(composite.getBackground());
        
        if (urlCopiedFrom != null || status.getMovedFromAbspath() != null) {
            label = new Label(composite, SWT.NONE);
            if (status.getMovedFromAbspath() != null) {
            	label.setText(Policy.bind("SVNPropertyPage.movedFrom")); //$NON-NLS-1$
            }
            else {
            	label.setText(Policy.bind("SVNPropertyPage.copiedFrom")); //$NON-NLS-1$
            }
            copiedFromValue = new Text(composite, SWT.WRAP | SWT.READ_ONLY);
            copiedFromValue.setBackground(composite.getBackground());
            GridData gd = new GridData();
            gd.widthHint = 500;
            copiedFromValue.setLayoutData(gd);
        }
        
        if (status.getLastChangedRevision() != null) {
	        label = new Label(composite, SWT.NONE);
	        label.setText(Policy.bind("SVNPropertyPage.changedRevision")); //$NON-NLS-1$
	        lastChangedRevisionValue = new Text(composite, SWT.READ_ONLY);
	        lastChangedRevisionValue.setBackground(composite.getBackground());
	
	        label = new Label(composite, SWT.NONE);
	        label.setText(Policy.bind("SVNPropertyPage.changedDate")); //$NON-NLS-1$
	        lastChangedDateValue = new Text(composite, SWT.READ_ONLY);
	        lastChangedDateValue.setBackground(composite.getBackground());
	
	        label = new Label(composite, SWT.NONE);
	        label.setText(Policy.bind("SVNPropertyPage.changedAuthor")); //$NON-NLS-1$
	        lastCommitAuthorValue = new Text(composite, SWT.READ_ONLY);
	        lastCommitAuthorValue.setBackground(composite.getBackground());
        }
        
        if (lockOwnerText != null) {
	        label = new Label(composite, SWT.NONE);
	        label.setText(Policy.bind("SVNPropertyPage.lockOwner"));  //$NON-NLS-1$
	        lockOwner = new Text(composite, SWT.READ_ONLY);
	        lockOwner.setBackground(composite.getBackground());
	
	        label = new Label(composite, SWT.NONE);
	        label.setText(Policy.bind("SVNPropertyPage.lockCreationDate"));  //$NON-NLS-1$
	        lockCreationDate = new Text(composite, SWT.READ_ONLY);
	        lockCreationDate.setBackground(composite.getBackground());
	
	        label = new Label(composite, SWT.NONE);
	        label.setText(Policy.bind("SVNPropertyPage.lockComment"));  //$NON-NLS-1$
	        lockComment = new Label(composite, SWT.WRAP);
	        GridData gd = new GridData();
	        gd.widthHint = 500;
	        lockComment.setLayoutData(gd);
        }
        
        if (status.hasTreeConflict()) {
        	label = new Label(composite, SWT.NONE);
        	label.setText(Policy.bind("SVNPropertyPage.treeConflict")); //$NON-NLS-1$
            treeConflict = new Text(composite, SWT.READ_ONLY);
            treeConflict.setBackground(composite.getBackground());            	
        	SVNConflictDescriptor conflictDescriptor = status.getConflictDescriptor();
        	if (conflictDescriptor == null) treeConflict.setText("true"); //$NON-NLS-1$
        	else {
        		SVNTreeConflict svnTreeConflict = new SVNTreeConflict(status);
        		treeConflict.setText(svnTreeConflict.getDescription());
        		SVNConflictVersion srcLeftVersion = svnTreeConflict.getConflictDescriptor().getSrcLeftVersion();
        		if (srcLeftVersion != null) {
        			new Label(composite, SWT.NONE);
        	        Text srcLeftVersionValue = new Text(composite, SWT.WRAP | SWT.READ_ONLY);
        	        GridData gd = new GridData();
        	        gd.widthHint = 500;
        	        srcLeftVersionValue.setLayoutData(gd);
        	        srcLeftVersionValue.setText("Source  left: " + srcLeftVersion.toString()); //$NON-NLS-1$
        	        srcLeftVersionValue.setBackground(composite.getBackground());            			
        		}
        		SVNConflictVersion srcRightVersion = svnTreeConflict.getConflictDescriptor().getSrcRightVersion();
        		if (srcRightVersion != null) {
        			new Label(composite, SWT.NONE);
        	        Text srcRightVersionValue = new Text(composite, SWT.WRAP | SWT.READ_ONLY);
        	        GridData gd = new GridData();
        	        gd.widthHint = 500;
        	        srcRightVersionValue.setLayoutData(gd);
        	        srcRightVersionValue.setText("Source right: " + srcRightVersion.toString()); //$NON-NLS-1$
        	        srcRightVersionValue.setBackground(composite.getBackground());            			
        		}            		
        	}
        }
	}
	
    private void addSeparator(Composite parent) {
        Label separator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
        GridData gridData = new GridData();
        gridData.horizontalAlignment = GridData.FILL;
        gridData.grabExcessHorizontalSpace = true;
        separator.setLayoutData(gridData);
    }
	
    private Composite createDefaultComposite(Composite parent) {
        Composite composite = new Composite(parent, SWT.NULL);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        composite.setLayout(layout);

        GridData data = new GridData(GridData.FILL_BOTH);
        composite.setLayoutData(data);

        return composite;
    }
    
    private void getStatus() {
    	ISVNRepositoryLocation repository = null;
    	ISVNClientAdapter svnClient = null;
    	try {
            IResource resource = (IResource) getElement();
            SVNTeamProvider svnProvider = (SVNTeamProvider) RepositoryProvider.getProvider(resource
                    .getProject(), SVNProviderPlugin.getTypeId());
            if (svnProvider == null) return;
            svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
            if (svnResource == null) return;
            status = svnResource.getStatus();
            if (status != null && !status.isIgnored()) {
	            repository = svnResource.getRepository();
	            svnClient = repository.getSVNClient();
	            ISVNInfo info = svnClient.getInfoFromWorkingCopy(svnResource.getFile());
	            urlCopiedFrom = info.getCopyUrl();
	            revision = svnResource.getRevision(); 
	            lockOwnerText = status.getLockOwner();
	            lockCommentText = status.getLockComment();
	            if (status.getLockCreationDate() != null) lockDateText = status.getLockCreationDate().toString();
	            if (!status.isAdded()) {
		            try {
		            	info = svnClient.getInfo(status.getUrl());
		            } catch (Exception e) {}
	            }
	            // Get lock information from server if svn:needs-lock property is set
	            if (info != null && status.getLockOwner() == null && status.getUrlString() != null) {
	           		ISVNProperty prop = svnResource.getSvnProperty("svn:needs-lock");
	           		if (prop != null) {
	                    lockOwnerText = info.getLockOwner();
	                    if (info.getLockCreationDate() != null) lockDateText = info.getLockCreationDate().toString();
	                    lockCommentText = info.getLockComment();
	           		}
	            }   
            }
    	} catch (Exception e) {
            SVNUIPlugin.log(new Status(IStatus.ERROR, SVNUIPlugin.ID, TeamException.UNABLE,
                    "Property Exception", e)); //$NON-NLS-1$
        }
    	finally {
    		if (repository != null) {
    			repository.returnSVNClient(svnClient);
    		}
    	}
    }
    
    private void setValues() {         
        urlValue.setText(status.getUrlString() != null ? status.getUrlString() : ""); //$NON-NLS-1$
        repositoryRootValue.setText(svnResource.getRepository() != null ? svnResource.getRepository().getUrl().toString(): "");
        revisionValue.setText(revision != null ? revision.toString() : ""); //$NON-NLS-1$
        
        StringBuffer sb = new StringBuffer(status.getTextStatus().toString());
        if (status.isSwitched()) sb.append(", switched"); //$NON-NLS-1$
        if (status.getMovedFromAbspath() != null) {
        	sb.append(", moved");
        }
        else {
        	if (status.isCopied()) sb.append(", copied"); //$NON-NLS-1$
        }
        if (status.isTextMerged()) sb.append(", merged"); //$NON-NLS-1$
        if (status.hasTreeConflict()) sb.append(", tree conflict"); //$NON-NLS-1$
        statusValue.setText(sb.toString());
        propertiesValue.setText(status.getPropStatus().toString());
        
        if (status.getMovedFromAbspath() != null) {
        	copiedFromValue.setText(status.getMovedFromAbspath().substring(ResourcesPlugin.getWorkspace().getRoot().getLocation().toString().length()));
        }
        else {
	        if (urlCopiedFrom != null) {
	        	copiedFromValue.setText(urlCopiedFrom.toString());
	        }
        }
        
        if (status.getLastChangedRevision() != null) {
	        lastChangedRevisionValue.setText(status.getLastChangedRevision() != null ? status
	                .getLastChangedRevision().toString() : ""); //$NON-NLS-1$
	        lastChangedDateValue.setText(status.getLastChangedDate() != null ? status
	                .getLastChangedDate().toString() : ""); //$NON-NLS-1$
	        lastCommitAuthorValue.setText(status.getLastCommitAuthor() != null ? status
	                .getLastCommitAuthor() : ""); //$NON-NLS-1$
        }
        
        if (lockOwnerText != null) {
        	lockOwner.setText(lockOwnerText);
        }
        if (lockDateText != null) {
        	lockCreationDate.setText(lockDateText);
        }
        if (lockCommentText != null) {
        	lockComment.setText(lockCommentText);
        }
    }
	
    protected void performDefaults() {
    }

    public boolean performOk() {
        return true;
    }

}
