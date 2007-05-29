package org.tigris.subversion.subclipse.ui.wizards.sharing;

import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.core.resources.IFolder;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.util.AdaptableList;
import org.tigris.subversion.subclipse.ui.wizards.SVNWizardPage;

public class SvnFoldersExistWarningPage extends SVNWizardPage {
	private TableViewer table;
	private Button continueButton;
	private IFolder[] svnFolders;

	public SvnFoldersExistWarningPage(String pageName, String title,
			ImageDescriptor titleImage, IFolder[] svnFolders) {
		super(pageName, title, titleImage);
		this.svnFolders = svnFolders;
	}

	public void createControl(Composite parent) {
		Composite composite = createComposite(parent, 1);
		
		createWrappingLabel(composite, Policy.bind("SVNFoldersExistWarningPage.description"), 0 /* indent */, 1 /* columns */); //$NON-NLS-1$
		
		table = createTable(composite, 1);
		table.setContentProvider(new WorkbenchContentProvider());
		table.setLabelProvider(new SvnFoldersLabelProvider());
		
		Arrays.sort(svnFolders, new Comparator() {
			public int compare(Object obj1, Object obj2) {
				IFolder folder1 = (IFolder)obj1;
				IFolder folder2 = (IFolder)obj2;
				return folder1.getFullPath().toOSString().compareTo(folder2.getFullPath().toOSString());
			}		
		});
        AdaptableList input = new AdaptableList(svnFolders);
        table.setInput(input);
		
		continueButton = new Button(composite, SWT.CHECK);
		continueButton.setText(Policy.bind("SVNFoldersExistWarningPage.continue"));
		
		continueButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				setPageComplete(continueButton.getSelection());
			}			
		});
		
		setPageComplete(false);
		
		setControl(composite);
	}
	
	protected TableViewer createTable(Composite parent, int span) {
		Table table = new Table(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		GridData data = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL);
		data.horizontalSpan = span;
		data.heightHint = 125;
		table.setLayoutData(data);
		TableLayout layout = new TableLayout();
		layout.addColumnData(new ColumnWeightData(100, true));
		table.setLayout(layout);
		TableColumn col = new TableColumn(table, SWT.NONE);
		col.setResizable(true);
		col.setText(Policy.bind("SVNFoldersExistWarningPage.folders")); //$NON-NLS-1$
		table.setHeaderVisible(true);
	
		return new TableViewer(table);
	}	
	
	class SvnFoldersLabelProvider extends LabelProvider implements ITableLabelProvider {
		WorkbenchLabelProvider workbenchLabelProvider = new WorkbenchLabelProvider();
		
		public Image getColumnImage(Object element, int columnIndex) {
			return workbenchLabelProvider.getImage(element);
		}

		public String getColumnText(Object element, int columnIndex) {
			IFolder folder = (IFolder)element;
			return folder.getFullPath().toOSString();
		}
		
	}

}
