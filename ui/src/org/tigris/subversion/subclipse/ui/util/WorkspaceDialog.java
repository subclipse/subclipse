package org.tigris.subversion.subclipse.ui.util;

import java.util.ArrayList;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.tigris.subversion.subclipse.ui.Policy;

public class WorkspaceDialog extends TitleAreaDialog {
	private String windowTitle;
	private String message;
	private TreeViewer treeViewer;
	private Text fileNameText;
	private ImageDescriptor imageDescriptor;
	private Image dlgTitleImage;
	private Text pathText;
	protected IContainer wsSelectedContainer;

	public WorkspaceDialog(Shell parentShell, String windowTitle, String message, ImageDescriptor imageDescriptor, Text pathText) {
		super(parentShell);
		this.windowTitle = windowTitle;
		this.message = message;
		this.imageDescriptor = imageDescriptor;
		this.pathText = pathText;
	}

	protected Control createContents(Composite parent) {
		Control control = super.createContents(parent);
		setTitle(message);
		dlgTitleImage = imageDescriptor.createImage();
		setTitleImage(dlgTitleImage);
		
		return control;
	}
	
	protected Control createDialogArea(Composite parent){
		Composite composite = (Composite) super.createDialogArea(parent);
		
		
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;	
		layout.marginHeight= convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		layout.marginWidth= convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);	
		composite.setLayout(layout);
		final GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		composite.setLayoutData(data);
        
		getShell().setText(windowTitle); 

        treeViewer = new TreeViewer(composite, SWT.BORDER);
        final GridData gd= new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.widthHint= 550;
        gd.heightHint= 250;
        treeViewer.getTree().setLayoutData(gd);
       
        treeViewer.setContentProvider(new LocationPageContentProvider());
        treeViewer.setLabelProvider(new WorkbenchLabelProvider());
        treeViewer.setInput(ResourcesPlugin.getWorkspace());
        
        //Open to whatever is selected in the workspace field
        IPath existingWorkspacePath = new Path(pathText.getText());
        if (existingWorkspacePath != null){
        	//Ensure that this workspace path is valid
        	IResource selectedResource = ResourcesPlugin.getWorkspace().getRoot().findMember(existingWorkspacePath);
        	if (selectedResource != null) {
        		treeViewer.expandToLevel(selectedResource, 0);
        		treeViewer.setSelection(new StructuredSelection(selectedResource));
    		}
        }
        
        final Composite group = new Composite(composite, SWT.NONE);
        layout = new GridLayout(2, false);
        layout.marginWidth = 0;
        group.setLayout(layout);
        group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        
        final Label label = new Label(group, SWT.NONE);
        label.setLayoutData(new GridData());
        label.setText(Policy.bind("WorkspaceDialog.fileName")); //$NON-NLS-1$
        
        fileNameText = new Text(group,SWT.BORDER);
        fileNameText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		
        setupListeners();
		
		return parent;
	}
	
	protected void okPressed() {
		//make sure that a filename has been typed in 
		
		String patchName = fileNameText.getText();
		
		if (patchName.equals("")){ //$NON-NLS-1$
			setErrorMessage(Policy.bind("WorkspaceDialog.enterFileName")); //$NON-NLS-1$
	    	return;
		}
		
		//make sure that the filename does not contain more than one segment
		if (!(ResourcesPlugin.getWorkspace().validateName(patchName, IResource.FILE)).isOK()){
			fileNameText.setText(""); //$NON-NLS-1$
			setErrorMessage(Policy.bind("WorkspaceDialog.multipleSegments")); //$NON-NLS-1$
	    	return;
		}
		
		//Make sure that a container has been selected
		if (wsSelectedContainer == null){
			getSelectedContainer();
		}
		Assert.isNotNull(wsSelectedContainer);
		
		IFile file = wsSelectedContainer.getFile(new Path(fileNameText.getText()));
		if (file != null)
			pathText.setText(file.getFullPath().toString());
		
		super.okPressed();
	}
	
	private void getSelectedContainer() {
		Object obj = ((IStructuredSelection)treeViewer.getSelection()).getFirstElement();
        if (obj instanceof IContainer)
        	wsSelectedContainer = (IContainer) obj;
        else if (obj instanceof IFile){
        	wsSelectedContainer = ((IFile) obj).getParent();
        }
	}

	protected void cancelPressed() {
	  super.cancelPressed();
	}
 
   public boolean close() {
        if (dlgTitleImage != null)
            dlgTitleImage.dispose();
        return super.close();
    }
   
	void setupListeners(){
		 treeViewer.addSelectionChangedListener(
       new ISelectionChangedListener() {
           public void selectionChanged(SelectionChangedEvent event) {
               IStructuredSelection s = (IStructuredSelection)event.getSelection();
               Object obj=s.getFirstElement();
               if (obj instanceof IContainer)
               	wsSelectedContainer = (IContainer) obj;
               else if (obj instanceof IFile){
               	IFile tempFile = (IFile) obj;
               	wsSelectedContainer = tempFile.getParent();
               	fileNameText.setText(tempFile.getName());
               }
           }
       });

       treeViewer.addDoubleClickListener(
               new IDoubleClickListener() {
                   public void doubleClick(DoubleClickEvent event) {
                       ISelection s= event.getSelection();
                       if (s instanceof IStructuredSelection) {
                           Object item = ((IStructuredSelection)s).getFirstElement();
                           if (treeViewer.getExpandedState(item))
                               treeViewer.collapseToLevel(item, 1);
                           else
                               treeViewer.expandToLevel(item, 1);
                       }
                   }
               });

       fileNameText.addModifyListener(new ModifyListener() {
           public void modifyText(ModifyEvent e) {
             setErrorMessage(null);
           }
       });
	} 
	
	class LocationPageContentProvider extends BaseWorkbenchContentProvider {
		//Never show closed projects
		boolean showClosedProjects=false;
		
		public Object[] getChildren(Object element) {
			if (element instanceof IWorkspace) {
	            // check if closed projects should be shown
	            IProject[] allProjects = ((IWorkspace) element).getRoot().getProjects();
	            if (showClosedProjects)
	                return allProjects;

	            ArrayList accessibleProjects = new ArrayList();
	            for (int i = 0; i < allProjects.length; i++) {
	                if (allProjects[i].isOpen()) {
	                    accessibleProjects.add(allProjects[i]);
	                }
	            }
	            return accessibleProjects.toArray();
	        } 
			
			return super.getChildren(element);
		}
	}	

}
