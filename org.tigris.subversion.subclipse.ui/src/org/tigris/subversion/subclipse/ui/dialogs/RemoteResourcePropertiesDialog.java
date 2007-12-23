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
		setShellStyle(SWT.CLOSE | SWT.TITLE | SWT.RESIZE);
		this.remoteResource = remoteResource;
	}
	
	protected Control createDialogArea(Composite parent) {
		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			public void run() {
				try {
					ISVNClientAdapter client = SVNProviderPlugin.getPlugin().getSVNClientManager().createSVNClient();
			        SVNProviderPlugin.disableConsoleLogging(); 
				    svnInfo = client.getInfo(remoteResource.getUrl());
				    properties = client.getProperties(remoteResource.getUrl());
			        SVNProviderPlugin.enableConsoleLogging(); 
				} catch (Exception e) { 
					errorMessage = e.getMessage();
			        SVNProviderPlugin.enableConsoleLogging(); 
				}
			}			
		});
		
		getShell().setText(Policy.bind("RemoteResourcePropertiesDialog.title")); //$NON-NLS-1$
		Composite composite = new Composite(parent, SWT.NULL);
		GridLayout gridLayout = new GridLayout();
		gridLayout.marginTop = 5;
		gridLayout.marginWidth = 10;
		gridLayout.numColumns = 2;
		composite.setLayout(gridLayout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		if (svnInfo == null) {
			Text errorText = new Text(composite, SWT.V_SCROLL | SWT.WRAP);
			GridData data = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
			data.widthHint = 600;
			data.heightHint = 100;
			errorText.setLayoutData(data);
			errorText.setEditable(false);
			errorText.setText(errorMessage);
	    errorText.setBackground(composite.getBackground());
			return composite;
		}
		
		Label urlLabel = new Label(composite, SWT.NONE);
		urlLabel.setText(Policy.bind("RemoteResourcePropertiesDialog.url"));
		Text urlText = new Text(composite, SWT.NONE);
		GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		data.widthHint = 600;
		urlText.setLayoutData(data);
		urlText.setEditable(false);
		urlText.setText(remoteResource.getUrl().toString());
		urlText.setBackground(composite.getBackground());
		
		Label authorLabel = new Label(composite, SWT.NONE);
		authorLabel.setText(Policy.bind("RemoteResourcePropertiesDialog.author"));
		Text authorText = new Text(composite, SWT.NONE);
		authorText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		authorText.setEditable(false);
		authorText.setText(svnInfo.getLastCommitAuthor());
    authorText.setBackground(composite.getBackground());
		
		Label revisionLabel = new Label(composite, SWT.NONE);
		revisionLabel.setText(Policy.bind("RemoteResourcePropertiesDialog.revision"));
		Text revisionText = new Text(composite, SWT.NONE);
		revisionText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		revisionText.setEditable(false);
		revisionText.setText(svnInfo.getLastChangedRevision().toString());
    revisionText.setBackground(composite.getBackground());
		
		Label dateLabel = new Label(composite, SWT.NONE);
		dateLabel.setText(Policy.bind("RemoteResourcePropertiesDialog.date"));
		Text dateText = new Text(composite, SWT.NONE);
		dateText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		dateText.setEditable(false);
		dateText.setText(svnInfo.getLastChangedDate().toString());
    dateText.setBackground(composite.getBackground());

		if (remoteResource instanceof ISVNRemoteFile) {
      String lockOwner = null;
      try {
        lockOwner = svnInfo.getLockOwner();
      } catch (Exception e) {
      }
      if (lockOwner != null) {
        Label lockOwnerLabel = new Label(composite, SWT.NONE);
        lockOwnerLabel.setText(Policy.bind("RemoteResourcePropertiesDialog.lockOwner"));
        Text lockOwnerText = new Text(composite, SWT.NONE);
				data = new GridData(SWT.FILL, SWT.CENTER, true, false);
				data.widthHint = 600;
				lockOwnerText.setLayoutData(data);
        lockOwnerText.setEditable(false);
        lockOwnerText.setText(svnInfo.getLockOwner());
        lockOwnerText.setBackground(composite.getBackground());
      }

      Date lockCreationDate = null;
      try {
        lockCreationDate = svnInfo.getLockCreationDate();
      } catch (Exception e) {
      }
      if (lockCreationDate != null) {
        Label lockCreatedLabel = new Label(composite, SWT.NONE);
        lockCreatedLabel.setText(Policy.bind("RemoteResourcePropertiesDialog.lockCreated"));
        Text lockCreatedText = new Text(composite, SWT.NONE);
				data = new GridData(SWT.FILL, SWT.CENTER, true, false);
				data.widthHint = 600;
				lockCreatedText.setLayoutData(data);
        lockCreatedText.setEditable(false);
        lockCreatedText.setText(svnInfo.getLockCreationDate().toString());
        lockCreatedText.setBackground(composite.getBackground());
      }

      String lockComment = null;
      try {
        lockComment = svnInfo.getLockComment();
      } catch (Exception e) {
      }
      if (lockComment != null) {
        Label lockCommentLabel = new Label(composite, SWT.NONE);
        lockCommentLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
        lockCommentLabel.setText(Policy.bind("RemoteResourcePropertiesDialog.lockComment"));
        Text lockCommentText = new Text(composite, SWT.V_SCROLL | SWT.WRAP);
        GridData lockCommentTextData = new GridData(SWT.FILL, SWT.TOP, true, false);
        lockCommentTextData.heightHint = 100;
        lockCommentTextData.widthHint = 600;
        lockCommentText.setLayoutData(lockCommentTextData);
        lockCommentText.setEditable(false);
        lockCommentText.setText(svnInfo.getLockComment());
        lockCommentText.setBackground(composite.getBackground());
      }
    }
		
//		Group propertyGroup = new Group(composite, SWT.NULL);
//		propertyGroup.setText(Policy.bind("RemoteResourcePropertiesDialog.properties")); //$NON-NLS-1$
//		propertyGroup.setLayout(new GridLayout());
//		propertyGroup.setLayoutData(new GridData(GridData.FILL_BOTH));	
		
		Table table = new Table(composite, SWT.BORDER);
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
		
		data = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
		data.verticalIndent = 5;
		data.heightHint = 150;
		table.setLayoutData(data);
		
		viewer.setContentProvider(new RemoteResourceContentProvider());
		viewer.setLabelProvider(new RemoteResourceLabelProvider());
		viewer.setInput(remoteResource);

		// set f1 help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.REMOTE_RESOURCE_PROPERTIES_DIALOG);	
		
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
