/*******************************************************************************
 * Copyright (c) 2005, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.dialogs;

import java.util.Date;

import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.viewers.ColumnLayoutData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.tigris.subversion.subclipse.core.ISVNRemoteFile;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNInfo;
import org.tigris.subversion.svnclientadapter.ISVNProperty;

public class RemoteResourcePropertiesDialog extends TrayDialog {
	private ISVNRemoteResource remoteResource;
	private ISVNInfo svnInfo;
	private ISVNProperty[] properties;
	
	private String errorMessage;
	
	private ColumnLayoutData columnLayouts[] = {
			new ColumnWeightData(75, 75, true),
			new ColumnWeightData(200, 200, true)};
	
	private String columnHeaders[] = {
			Policy.bind("RemoteResourcePropertiesDialog.property"),
			Policy.bind("RemoteResourcePropertiesDialog.value")
		};

	public RemoteResourcePropertiesDialog(Shell parentShell, ISVNRemoteResource remoteResource) {
		super(parentShell);
		this.remoteResource = remoteResource;
	}
	
	protected Control createDialogArea(Composite parent) {
		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			public void run() {
				try {
					ISVNClientAdapter client = SVNProviderPlugin.getPlugin().getSVNClientManager().createSVNClient();
				    svnInfo = client.getInfo(remoteResource.getUrl());
				    properties = client.getProperties(remoteResource.getUrl());
				} catch (Exception e) { errorMessage = e.getMessage(); }
			}			
		});
		
		getShell().setText(Policy.bind("RemoteResourcePropertiesDialog.title")); //$NON-NLS-1$
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		if (svnInfo == null) {
			Text errorText = new Text(composite, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
			GridData data = new GridData();
			data.widthHint = 300;
			data.heightHint = 100;
			errorText.setLayoutData(data);
			errorText.setEditable(false);
			errorText.setText(errorMessage);
			return composite;
		}
		
		Group infoGroup = new Group(composite, SWT.NULL);
		infoGroup.setText(Policy.bind("RemoteResourcePropertiesDialog.info")); //$NON-NLS-1$
		GridLayout infoLayout = new GridLayout();
		infoLayout.numColumns = 2;
		infoGroup.setLayout(infoLayout);
		GridData data = new GridData(GridData.FILL_BOTH);
		infoGroup.setLayoutData(data);	
		
		Label urlLabel = new Label(infoGroup, SWT.NONE);
		urlLabel.setText(Policy.bind("RemoteResourcePropertiesDialog.url"));
		Text urlText = new Text(infoGroup, SWT.BORDER);
		data = new GridData();
		data.widthHint = 300;
		urlText.setLayoutData(data);
		urlText.setEditable(false);
		urlText.setText(remoteResource.getUrl().toString());
		
		Label authorLabel = new Label(infoGroup, SWT.NONE);
		authorLabel.setText(Policy.bind("RemoteResourcePropertiesDialog.author"));
		Text authorText = new Text(infoGroup, SWT.BORDER);
		data = new GridData();
		data.widthHint = 300;
		authorText.setLayoutData(data);
		authorText.setEditable(false);
		authorText.setText(svnInfo.getLastCommitAuthor());
		
		Label revisionLabel = new Label(infoGroup, SWT.NONE);
		revisionLabel.setText(Policy.bind("RemoteResourcePropertiesDialog.revision"));
		Text revisionText = new Text(infoGroup, SWT.BORDER);
		data = new GridData();
		data.widthHint = 75;
		revisionText.setLayoutData(data);
		revisionText.setEditable(false);
		revisionText.setText(svnInfo.getLastChangedRevision().toString());
		
		Label dateLabel = new Label(infoGroup, SWT.NONE);
		dateLabel.setText(Policy.bind("RemoteResourcePropertiesDialog.date"));
		Text dateText = new Text(infoGroup, SWT.BORDER);
		data = new GridData();
		data.widthHint = 300;
		dateText.setLayoutData(data);
		dateText.setEditable(false);
		dateText.setText(svnInfo.getLastChangedDate().toString());
		
		if (remoteResource instanceof ISVNRemoteFile) {
			String lockOwner = null;
			try {
				lockOwner = svnInfo.getLockOwner();
			} catch (Exception e) {}
			if (lockOwner != null) {
				Label lockOwnerLabel = new Label(infoGroup, SWT.NONE);
				lockOwnerLabel.setText(Policy.bind("RemoteResourcePropertiesDialog.lockOwner"));
				Text lockOwnerText = new Text(infoGroup, SWT.BORDER);
				data = new GridData();
				data.widthHint = 300;
				lockOwnerText.setLayoutData(data);
				lockOwnerText.setEditable(false);
				lockOwnerText.setText(svnInfo.getLockOwner());				
			}
			Date lockCreationDate = null;
			try {
				lockCreationDate = svnInfo.getLockCreationDate();
			} catch (Exception e) {}
			if (lockCreationDate != null) {
				Label lockCreatedLabel = new Label(infoGroup, SWT.NONE);
				lockCreatedLabel.setText(Policy.bind("RemoteResourcePropertiesDialog.lockCreated"));
				Text lockCreatedText = new Text(infoGroup, SWT.BORDER);
				data = new GridData();
				data.widthHint = 300;
				lockCreatedText.setLayoutData(data);
				lockCreatedText.setEditable(false);
				lockCreatedText.setText(svnInfo.getLockCreationDate().toString());				
			}
			String lockComment = null;
			try {
				lockComment = svnInfo.getLockComment();
			} catch (Exception e) {}
			if (lockComment != null) {
				Label lockCommentLabel = new Label(infoGroup, SWT.NONE);
				lockCommentLabel.setText(Policy.bind("RemoteResourcePropertiesDialog.lockComment"));				
				data = new GridData();
				data.horizontalSpan = 2;
				lockCommentLabel.setLayoutData(data);
				Text lockCommentText = new Text(infoGroup, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
				data = new GridData();
				data.widthHint = 300;
				data.heightHint = 100;
				data.horizontalSpan = 2;
				lockCommentText.setLayoutData(data);
				lockCommentText.setEditable(false);
				lockCommentText.setText(svnInfo.getLockComment());				
			}
		}
		
		Group propertyGroup = new Group(composite, SWT.NULL);
		propertyGroup.setText(Policy.bind("RemoteResourcePropertiesDialog.properties")); //$NON-NLS-1$
		GridLayout propertyLayout = new GridLayout();
		propertyLayout.numColumns = 1;
		propertyGroup.setLayout(propertyLayout);
		data = new GridData(GridData.FILL_BOTH);
		propertyGroup.setLayoutData(data);	
		
		Table table = new Table(propertyGroup, SWT.H_SCROLL | SWT.V_SCROLL);
		table.setLinesVisible(true);
		TableViewer viewer = new TableViewer(table);
    	viewer.setUseHashlookup(true);
    	TableLayout tableLayout = new TableLayout();
		table.setLayout(tableLayout);
		table.setHeaderVisible(true);
		for (int i = 0; i < columnHeaders.length; i++) {
			tableLayout.addColumnData(columnLayouts[i]);
			TableColumn tc = new TableColumn(table, SWT.NONE,i);
			tc.setResizable(columnLayouts[i].resizable);
			tc.setText(columnHeaders[i]);
		}
		
		viewer.setContentProvider(new RemoteResourceContentProvider());
		viewer.setLabelProvider(new RemoteResourceLabelProvider());
		viewer.setInput(remoteResource);

		// set f1 help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.REMOTE_RESOURCE_PROPERTIES_DIALOG);	

		data = new GridData();
		data.widthHint = 500;
		data.heightHint = 100;
		table.setLayoutData(data);
		
		return composite;
	}
	
	class RemoteResourceContentProvider implements IStructuredContentProvider  {
		public void dispose() {
		}
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			
		}
		public Object[] getElements(Object arg0) {
			return properties;
		}
	}
	
	class RemoteResourceLabelProvider extends LabelProvider implements ITableLabelProvider {

	public String getColumnText(Object element, int columnIndex) {
		if ((columnIndex >= 0) && (columnIndex <= 1)) {
			ISVNProperty property = (ISVNProperty)element;
			switch (columnIndex) { 
				case 0: return property.getName();
				case 1: return property.getValue();
				default: return "";
			}
		}
		return ""; 
	}
	public Image getColumnImage(Object element, int columnIndex) {
		return null;
	}
}

}
