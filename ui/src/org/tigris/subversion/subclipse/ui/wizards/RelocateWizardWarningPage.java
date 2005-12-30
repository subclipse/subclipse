package org.tigris.subversion.subclipse.ui.wizards;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ColumnLayoutData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;

public class RelocateWizardWarningPage extends WizardPage {
	private IProject[] sharedProjects;
	private Table table;
	private TableViewer viewer;
	
	private ColumnLayoutData columnLayouts[] = {
		new ColumnWeightData(100, 100, true)};		
		
	private String columnHeaders[] = {
		"Project"
	};				

	public RelocateWizardWarningPage(String pageName, String title, ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
		setPageComplete(true);
	}

	public void createControl(Composite parent) {
		RelocateWizard wizard = (RelocateWizard)getWizard();
		sharedProjects = wizard.getSharedProjects();
		
		Composite outerContainer = new Composite(parent,SWT.NONE);
		// set F1 help
		WorkbenchHelp.setHelp(outerContainer, IHelpContextIds.RELOCATE_REPOSITORY_PAGE);
		GridLayout outerLayout = new GridLayout();
		outerLayout.numColumns = 2;
		outerContainer.setLayout(outerLayout);
		outerContainer.setLayoutData(
		new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		
		Label attentionImageLabel = new Label(outerContainer, SWT.NONE);
		attentionImageLabel.setImage(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_WARNING).createImage());
		
		Text warningText = new Text(outerContainer, SWT.WRAP);
		warningText.setEditable(false);
		GridData data = new GridData();
		data.widthHint = 400;
		data.heightHint = 40;
		warningText.setLayoutData(data);
		warningText.setText(Policy.bind("RelocateWizard.warningMessage1")); //$NON-NLS-1$
		
		Text warningText2 = new Text(outerContainer, SWT.WRAP);
		warningText2.setEditable(false);
		data = new GridData();
		data.widthHint = 400;
		data.heightHint = 100;
		data.horizontalSpan = 2;
		warningText2.setLayoutData(data);
		warningText2.setText(Policy.bind("RelocateWizard.warningMessage2")); //$NON-NLS-1$
		
		Group projectsGroup = new Group(outerContainer, SWT.NONE);
		projectsGroup.setText(Policy.bind("RelocateWizard.projects")); //$NON-NLS-1$
		projectsGroup.setLayout(new GridLayout());
		data = new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL);
		data.horizontalSpan = 2;
		projectsGroup.setLayoutData(data);
		
		table = new Table(projectsGroup, SWT.H_SCROLL | SWT.V_SCROLL);
		data = new GridData(GridData.FILL_BOTH | GridData.GRAB_VERTICAL | GridData.GRAB_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		table.setLayoutData(data);
		table.setLinesVisible(false);
		table.setHeaderVisible(false);
		viewer = new TableViewer(table);
		viewer.setUseHashlookup(true);
		
		TableLayout layout = new TableLayout();
		table.setLayout(layout);	
		for (int i = 0; i < columnHeaders.length; i++) {
			layout.addColumnData(columnLayouts[i]);
			TableColumn tc = new TableColumn(table, SWT.NONE,i);
			tc.setResizable(columnLayouts[i].resizable);
			tc.setText(columnHeaders[i]);
		}
		
		viewer.setLabelProvider(new ProjectLabelProvider());
		viewer.setContentProvider(new ProjectContentProvider());
		viewer.setInput(this);
		GridData gd = new GridData();
		gd.widthHint = 400;
		gd.heightHint = 150;
		table.setLayoutData(gd);		
		
		setMessage(Policy.bind("RelocateWizard.warning")); //$NON-NLS-1$
		
		setControl(outerContainer);		
	}
	
	class ProjectLabelProvider
	extends WorkbenchLabelProvider
	implements ITableLabelProvider {
			
	public String getColumnText(Object element, int columnIndex) {
		IProject project = (IProject)element;
		switch (columnIndex) { 
			case 0: return project.getName();
			default: return "";
		}
	}
	public Image getColumnImage(Object element, int columnIndex) {
		if (columnIndex == 0) {
			return getImage(element);
		}
		return null;
	}
}	

class ProjectContentProvider implements IStructuredContentProvider  {
	public void dispose() {
	}	
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}		
	public Object[] getElements(Object arg0) {
		return sharedProjects;
	}
}

}
