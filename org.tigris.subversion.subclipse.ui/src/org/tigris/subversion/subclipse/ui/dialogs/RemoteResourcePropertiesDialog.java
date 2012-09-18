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

import org.eclipse.jface.viewers.ColumnLayoutData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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
import org.tigris.subversion.svnclientadapter.SVNRevision;

public class RemoteResourcePropertiesDialog extends SubclipseTrayDialog {
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
				ISVNClientAdapter client = null;
				try {
					client = SVNProviderPlugin.getPlugin().getSVNClientManager().getSVNClient();
			        SVNProviderPlugin.disableConsoleLogging(); 
				    svnInfo = client.getInfo(remoteResource.getUrl());
				    properties = client.getProperties(remoteResource.getUrl(), SVNRevision.HEAD, SVNRevision.HEAD, false);
			        SVNProviderPlugin.enableConsoleLogging(); 
				} catch (Exception e) { 
					errorMessage = e.getMessage();
			        SVNProviderPlugin.enableConsoleLogging(); 
				} finally {
					SVNProviderPlugin.getPlugin().getSVNClientManager().returnSVNClient(client);
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
			Text errorText = new Text(composite, SWT.V_SCROLL | SWT.WRAP | SWT.READ_ONLY);
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
		Text urlText = new Text(composite, SWT.READ_ONLY);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, false);
		data.widthHint = 600;
		urlText.setLayoutData(data);
		urlText.setEditable(false);
		urlText.setText(remoteResource.getUrl().toString());
		urlText.setBackground(composite.getBackground());
		
		Label authorLabel = new Label(composite, SWT.NONE);
		authorLabel.setText(Policy.bind("RemoteResourcePropertiesDialog.author"));
		Text authorText = new Text(composite, SWT.READ_ONLY);
		authorText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		authorText.setEditable(false);
		if (svnInfo.getLastCommitAuthor() != null) authorText.setText(svnInfo.getLastCommitAuthor());
    authorText.setBackground(composite.getBackground());
		
		Label revisionLabel = new Label(composite, SWT.NONE);
		revisionLabel.setText(Policy.bind("RemoteResourcePropertiesDialog.revision"));
		Text revisionText = new Text(composite, SWT.READ_ONLY);
		revisionText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		revisionText.setEditable(false);
		if (svnInfo.getLastChangedRevision() != null) revisionText.setText(svnInfo.getLastChangedRevision().toString());
    revisionText.setBackground(composite.getBackground());
		
		Label dateLabel = new Label(composite, SWT.NONE);
		dateLabel.setText(Policy.bind("RemoteResourcePropertiesDialog.date"));
		Text dateText = new Text(composite, SWT.READ_ONLY);
		dateText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		dateText.setEditable(false);
		if (svnInfo.getLastChangedDate() != null) dateText.setText(svnInfo.getLastChangedDate().toString());
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
        Text lockOwnerText = new Text(composite, SWT.READ_ONLY);
				data = new GridData(SWT.FILL, SWT.FILL, true, false);
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
        Text lockCreatedText = new Text(composite, SWT.READ_ONLY);
				data = new GridData(SWT.FILL, SWT.FILL, true, false);
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
        Text lockCommentText = new Text(composite, SWT.V_SCROLL | SWT.WRAP | SWT.READ_ONLY);
        GridData lockCommentTextData = new GridData(SWT.FILL, SWT.FILL, true, false);
        lockCommentTextData.heightHint = 100;
        lockCommentTextData.widthHint = 600;
        lockCommentText.setLayoutData(lockCommentTextData);
        lockCommentText.setEditable(false);
        lockCommentText.setText(svnInfo.getLockComment());
        lockCommentText.setBackground(composite.getBackground());
      }
    }

		SashForm sashForm = new SashForm(composite, SWT.VERTICAL);
		GridData gd_sashForm = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
		gd_sashForm.heightHint = 244;
		sashForm.setLayoutData(gd_sashForm);

	  final Table table = new Table(sashForm, SWT.FULL_SELECTION | SWT.BORDER);
	  
	  final Text text = new Text(sashForm, SWT.V_SCROLL | SWT.MULTI | SWT.READ_ONLY | SWT.BORDER | SWT.WRAP);

		final TableViewer viewer = new TableViewer(table);
		viewer.setUseHashlookup(true);

		TableLayout tableLayout = new TableLayout();
		for (int i = 0; i < columnHeaders.length; i++) {
		  tableLayout.addColumnData(columnLayouts[i]);
		  TableColumn tc = new TableColumn(table, SWT.NONE,i);
		  tc.setResizable(columnLayouts[i].resizable);
		  tc.setText(columnHeaders[i]);
		}
		table.setLayout(tableLayout);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		table.addSelectionListener(new SelectionAdapter() {
		  public void widgetSelected(SelectionEvent e) {
		    IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
		    ISVNProperty property = (ISVNProperty) selection.getFirstElement();
        text.setText(property.getValue());
		  }
		});
		
		GridData gd_table = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
		gd_table.verticalIndent = 5;
		gd_table.heightHint = 150;
		table.setLayoutData(gd_table);
		
		viewer.setContentProvider(new RemoteResourceContentProvider());
		viewer.setLabelProvider(new RemoteResourceLabelProvider());
		viewer.setInput(remoteResource);
		sashForm.setWeights(new int[] {128, 113 });

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
