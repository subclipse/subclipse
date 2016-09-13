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
package org.tigris.subversion.subclipse.ui.conflicts;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.IContentChangeListener;
import org.eclipse.compare.IContentChangeNotifier;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.tigris.subversion.subclipse.core.util.File2Resource;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.compare.internal.Utilities;
import org.tigris.subversion.svnclientadapter.ISVNConflictResolver;
import org.tigris.subversion.svnclientadapter.SVNConflictDescriptor;

public class BuiltInConflictsCompareInput extends CompareEditorInput {
	
    public static class MyDiffNode extends DiffNode {
        public MyDiffNode(IDiffContainer parent, int kind, ITypedElement ancestor, ITypedElement left, ITypedElement right) {
            super(parent, kind, ancestor, left, right);
        }
         
        public void fireChange() {
            super.fireChange();
        }
    }

    private Object fRoot;
    private FileNode fAncestor;
    private FileNode fLeft;
    private FileNode fRight;   
    private File fAncestorFile;
    private File fMineFile;
    private File fTheirsFile;
    private File fDestinationFile;
    private String fileName;
    
    private boolean neverSaved = true;
    private boolean isSaving = false;
    
    private boolean finished;
    private boolean resolved;
    private int resolution;
    private SVNConflictDescriptor conflictDescriptor;

	public BuiltInConflictsCompareInput(CompareConfiguration configuration, SVNConflictDescriptor conflictDescriptor) {
		super(configuration);
		this.conflictDescriptor = conflictDescriptor;
	}

	public void setResources(File ancestor, File mine, File theirs, File destination, String fileName) {
        fAncestorFile = ancestor;
        fMineFile = mine;
        fTheirsFile = theirs;
        fDestinationFile = destination;
        this.fileName = fileName;
        initializeCompareConfiguration();		
	}
	
    private String getType() {
    	FileNode node = new FileNode(fMineFile);
        String s = node.getType();
        if (s != null)
            return s;
        return ITypedElement.UNKNOWN_TYPE;
    }	
	
    private void initializeCompareConfiguration() {
        CompareConfiguration cc = getCompareConfiguration();
        
        cc.setLeftEditable(true);

        String leftLabel = "Merged - " + fileName; //$NON-NLS-1$
        String rightLabel = "Theirs - " + fileName; //$NON-NLS-1$
        String ancestorLabel = "Ancestor -" + fileName; //$NON-NLS-1$

        cc.setLeftLabel(leftLabel);

        cc.setRightLabel(rightLabel);

        cc.setAncestorLabel(ancestorLabel);
    }    	

    protected Object prepareInput(IProgressMonitor pm) throws InvocationTargetException, InterruptedException {
		try {
		    pm
		            .beginTask(
		                    Utilities.getString("ResourceCompare.taskName"), IProgressMonitor.UNKNOWN); //$NON-NLS-1$
		
		    String charSet = getCharSet();
		    
		    fAncestor = new FileNode(fAncestorFile) {
		        public String getType() {
		            return BuiltInConflictsCompareInput.this.getType();
		        }
		
		        public boolean isEditable() {
		            return false;
		        }
		    };
		    fAncestor.setCharSet(charSet);

		    fLeft = new FileNode(fDestinationFile) {
		        public String getType() {
		            return BuiltInConflictsCompareInput.this.getType();
		        }
		    };
		    fLeft.setCharSet(charSet);
		    
		    InputStream mineContents = new FileInputStream(fMineFile);
		    byte[] contents;
		    try {
		        contents = new byte[mineContents.available()];
		        mineContents.read(contents);
		    } finally {
		        mineContents.close();
		    }
		    
		    fLeft.setContent(contents);
		
		    // add after setting contents, otherwise we end up in a loop
		    // makes sure that the diff gets re-run if we right-click and select Save on the left pane.
		    // Requires that we have a isSaving flag to avoid recursion
		    fLeft.addContentChangeListener( new IContentChangeListener() {            
		        public void contentChanged(IContentChangeNotifier source) {
		            if (!isSaving) {
		                try {                    
		                    saveChanges(new NullProgressMonitor());
		                } catch (CoreException e) {
		                    e.printStackTrace();
		                }
		            }
		        }            
		    });
		
		    fRight = new FileNode(fTheirsFile) {
		        public String getType() {
		            return BuiltInConflictsCompareInput.this.getType();
		        }
		
		        public boolean isEditable() {
		            return false;
		        }
		    };
		    fRight.setCharSet(charSet);
		
		    String title = Policy.bind("BuiltInConflictsCompareInput.0") + fileName; //$NON-NLS-1$
		    setTitle(title);
		
		    // Override the default difference visit method to use MyDiffNode 
		    // instead of just DiffNode
		    Differencer d = new Differencer()
		    {
		        protected Object visit(Object data, int result, Object ancestor, Object left, Object right) {
		            return new MyDiffNode((IDiffContainer) data, result, (ITypedElement)ancestor, (ITypedElement)left, (ITypedElement)right);
		        }
		    };
		
		    fRoot = d.findDifferences(true, pm, null, fAncestor, fLeft, fRight);
		    return fRoot;
		} catch(IOException e) {
		    throw new InvocationTargetException(e);
		} finally {
		    pm.done();
		}
	}

	private String getCharSet() {
		String charSet = null;
		String destFilePath = fDestinationFile.getAbsolutePath();
		String workspacePath;
		int index = destFilePath.indexOf(File.separator + Policy.bind("BuiltInConflictsCompareInput.1") + File.separator); //$NON-NLS-1$
		if (index == -1) {
			workspacePath = destFilePath;
		} else {
			workspacePath = destFilePath.substring(0, index) + File.separator + fileName;
		}
		File workspaceFile = new File(workspacePath);
		IFile destinationFile = (IFile) File2Resource
		.getResource(workspaceFile);
		if (destinationFile != null) {			
			try {
				charSet = destinationFile.getCharset();
			} catch (CoreException e) {}
		}
		return charSet;
	}    
    
    public void saveChanges(IProgressMonitor pm) throws CoreException {
        try {
            isSaving = true;
            super.saveChanges(pm);
            fLeft.commit(pm);
            neverSaved = false;
            ((MyDiffNode)fRoot).fireChange();
        } finally {
            isSaving = false;
        }
    }

    public boolean isSaveNeeded() {
        if (neverSaved) {
            return true;
        } else {
            return super.isSaveNeeded();
        }
    }  
    
	public Control createContents(Composite parent) {
		parent.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				handleInternalDispose();
			}		
		});
		return super.createContents(parent);
	}

	protected void handleInternalDispose() {
		DialogWizard dialogWizard = new DialogWizard(DialogWizard.FINISHED_EDITING);
		dialogWizard.setConflictDescriptor(conflictDescriptor);
		ConflictWizardDialog dialog = new ConflictWizardDialog(Display.getDefault().getActiveShell(), dialogWizard);
		dialog.open();		
		resolution = dialogWizard.getResolution();
		resolved = resolution != ISVNConflictResolver.Choice.postpone;
		finished = true;
	}
	
	public void handleExternalDispose() {
		DialogWizard dialogWizard = new DialogWizard(DialogWizard.FINISHED_EDITING);
		dialogWizard.setConflictDescriptor(conflictDescriptor);
		ConflictWizardDialog dialog = new ConflictWizardDialog(Display.getDefault().getActiveShell(), dialogWizard);
		dialog.open();			
		resolution = dialogWizard.getResolution();
		resolved = resolution != ISVNConflictResolver.Choice.postpone;
		finished = true;	
	}
	
	public boolean isFinished() {
		return finished;
	}

	public void setFinished(boolean finished) {
		this.finished = finished;
	}

	public boolean isResolved() {
		return resolved;
	}

	public int getResolution() {
		return resolution;
	}	

}
