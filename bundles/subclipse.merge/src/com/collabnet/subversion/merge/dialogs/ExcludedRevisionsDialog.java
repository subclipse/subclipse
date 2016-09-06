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
package com.collabnet.subversion.merge.dialogs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;

import com.collabnet.subversion.merge.Messages;

public class ExcludedRevisionsDialog extends TrayDialog {
	private String fromUrl;
	private Map<String, Set<String>> excludedRevisions;
	
	private ExcludedRevision[] excludedRevisionArray;
	
	private Table table;
	private TableViewer viewer;
	
	private String[] columnHeaders = {Messages.ExcludedRevisionsDialog_revision, Messages.ExcludedRevisionsDialog_url};
	private ColumnLayoutData columnLayouts[] = {
		new ColumnWeightData(75, 75, true),
		new ColumnWeightData(450, 450, true)};

	public ExcludedRevisionsDialog(Shell shell, String fromUrl, Map<String, Set<String>> excludedRevisions) {
		super(shell);
		this.fromUrl = fromUrl;
		this.excludedRevisions = excludedRevisions;
		int shellStyle = getShellStyle();
		setShellStyle(shellStyle | SWT.RESIZE);
		getExcludedRevisions();
	}
	
	protected Control createDialogArea(Composite parent) {
		getShell().setText(Messages.ExcludedRevisionsDialog_title);
		Composite composite = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(data);
		
		Label label = new Label(composite, SWT.WRAP);
		label.setText(Messages.ExcludedRevisionsDialog_text);
		data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		data.grabExcessHorizontalSpace = true;
		data.widthHint = 400;
		data.horizontalSpan = 2;
		label.setLayoutData(data);
		
		Label fromLabel = new Label(composite, SWT.NONE);
		fromLabel.setText(Messages.ExcludedRevisionsDialog_mergeFrom);
		Text fromText = new Text(composite, SWT.BORDER);
		fromText.setEditable(false);
		fromText.setText(fromUrl);
		data = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		fromText.setLayoutData(data);
		
		table = new Table(composite, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
		data = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		data.heightHint = 200;
		data.horizontalSpan = 2;
		table.setLayoutData(data);
		
		TableLayout tableLayout = new TableLayout();
		table.setLayout(tableLayout);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		viewer = new TableViewer(table);
		
		viewer.setContentProvider(new ExcludedRevisionsContentProvider());
		viewer.setLabelProvider(new ExcludedRevisionsLabelProvider());
		for (int i = 0; i < columnHeaders.length; i++) {
			tableLayout.addColumnData(columnLayouts[i]);
			TableColumn tc = new TableColumn(table, SWT.NONE,i);
			tc.setResizable(columnLayouts[i].resizable);
			tc.setText(columnHeaders[i]);
		}
		
		viewer.setInput(this);	
		
		return composite;
	}
	
	@Override
	public boolean isHelpAvailable() {
		return false;
	}

	@SuppressWarnings("unchecked")
	private void getExcludedRevisions() {
		List<ExcludedRevision> excludedRevisionList = new ArrayList<ExcludedRevision>();
		Set keySet = excludedRevisions.keySet();
		Iterator iter = keySet.iterator();
		while (iter.hasNext()) {
			String key = (String)iter.next();
			Set revisionSet = (Set)excludedRevisions.get(key);
			Iterator revIter = revisionSet.iterator();
			while (revIter.hasNext()) {
				String rev = (String)revIter.next();
				ExcludedRevision excludedRevision = new ExcludedRevision(rev, key);
				if (!excludedRevisionList.contains(excludedRevision)) excludedRevisionList.add(excludedRevision);
			}							
		}				
		excludedRevisionArray = new ExcludedRevision[excludedRevisionList.size()];
		excludedRevisionList.toArray(excludedRevisionArray);
		Arrays.sort(excludedRevisionArray);
	}
	
	@SuppressWarnings("unchecked")
	class ExcludedRevision implements Comparable {
		private String revision;
		private String url;
		
		public ExcludedRevision(String revision, String url) {
			this.revision = revision;
			this.url = url;
		}
		
		public String getRevision() {
			return revision;
		}

		public String getUrl() {
			return url;
		}

		@Override
		public boolean equals(Object compareTo) {
			if (compareTo instanceof ExcludedRevision) {
				ExcludedRevision compareToRevision = (ExcludedRevision)compareTo;
				return compareToRevision.getRevision().equals(revision) && compareToRevision.getUrl().equals(url);
			}
			return super.equals(compareTo);
		}

		public int compareTo(Object compareTo) {
			ExcludedRevision compareToRevision = (ExcludedRevision)compareTo;
			int compareRev = Integer.parseInt(compareToRevision.getRevision());
			int rev = Integer.parseInt(revision);
			if (compareRev < rev) return -1;
			if (compareRev > rev) return 1;
			return url.compareTo(compareToRevision.getUrl());
		}
		
	}
	
	static class ExcludedRevisionsLabelProvider extends LabelProvider implements ITableLabelProvider {

		public String getColumnText(Object element, int columnIndex) {
			ExcludedRevision excludedRevision = (ExcludedRevision)element;
			switch (columnIndex) { 
				case 0: return excludedRevision.getRevision();
				case 1: return excludedRevision.getUrl();
			}
			return "";  //$NON-NLS-1$
		}
		
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}
	
	}	
	
	class ExcludedRevisionsContentProvider implements IStructuredContentProvider {
		public void dispose() {
		}
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
		public Object[] getElements(Object obj) {
			return excludedRevisionArray;
		}
	}	

}
