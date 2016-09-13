package org.tigris.subversion.subclipse.ui.conflicts;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;
import org.tigris.subversion.subclipse.ui.Messages;
import org.tigris.subversion.svnclientadapter.ISVNConflictResolver;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNConflictDescriptor;
import org.tigris.subversion.svnclientadapter.SVNConflictResult;

public class SVNConflictResolver implements ISVNConflictResolver {
	private IResource resource;
	private int textHandling = ISVNConflictResolver.Choice.postpone;
	private int binaryHandling = ISVNConflictResolver.Choice.postpone;
	private int propertyHandling = ISVNConflictResolver.Choice.postpone;
	private int treeConflictHandling = ISVNConflictResolver.Choice.postpone;
	private ArrayList<ConflictResolution> conflictResolutions = new ArrayList<ConflictResolution>();
	private volatile DialogWizard dialogWizard;
	private boolean finished;
	private int resolution;
	private IWorkbenchPart part;
	private File workingTempFile;
	private File mergeTempFile;
	private File mergedFile;
	private boolean wait = false;
	private ConflictResolution applyToAllTextResolution;
	private ConflictResolution applyToAllBinaryResolution;
	private ConflictResolution applyToAllPropertyResolution;
	private ConflictResolution applyToAllTreeConflictResolution;
	
	private ISchedulingRule schedulingRule;
	
	public static final int PROMPT = -1;
	
	public SVNConflictResolver() {
		super();
	}

	public SVNConflictResolver(IResource resource, int textHandling, int binaryHandling, int propertyHandling, int treeConflictHandling) {
		this();
		this.resource = resource;
		this.textHandling = textHandling;
		this.binaryHandling = binaryHandling;
		this.propertyHandling = propertyHandling;
		this.treeConflictHandling = treeConflictHandling;
	}

	public SVNConflictResult resolve(SVNConflictDescriptor descrip) throws SVNClientException {
		if (descrip.getReason() == SVNConflictDescriptor.Reason.deleted || descrip.getReason() == SVNConflictDescriptor.Reason.moved_away) {
			if (treeConflictHandling == PROMPT) {
				ConflictResolution conflictResolution = getConflictResolution(descrip);
				conflictResolutions.add(conflictResolution);
				return new SVNConflictResult(conflictResolution.getResolution(), descrip.getMergedPath());
			}
			else if (treeConflictHandling == ISVNConflictResolver.Choice.chooseMerged) {
				conflictResolutions.add(new ConflictResolution(descrip, treeConflictHandling));
				return new SVNConflictResult(treeConflictHandling, descrip.getMergedPath());
			}
			else {
				conflictResolutions.add(new ConflictResolution(descrip, treeConflictHandling));
				return new SVNConflictResult(treeConflictHandling, descrip.getMergedPath());
			}
		}
		else if (descrip.getReason() == SVNConflictDescriptor.Reason.edited || (descrip.getReason() == SVNConflictDescriptor.Reason.obstructed && descrip.getConflictKind() == SVNConflictDescriptor.Kind.property)) {
			if (descrip.isBinary()) {
				int handling;
				if (descrip.getConflictKind() == SVNConflictDescriptor.Kind.property)
					handling = propertyHandling;
				else
					handling = binaryHandling;
				if (handling == ISVNConflictResolver.Choice.chooseMerged) {
					ConflictResolution conflictResolution = getConflictResolution(descrip);
					conflictResolutions.add(conflictResolution);
					return new SVNConflictResult(conflictResolution.getResolution(), descrip.getMergedPath());
				} else {
					conflictResolutions.add(new ConflictResolution(descrip, handling));
					return new SVNConflictResult(handling, descrip.getMergedPath());
				}
			} else {
				int handling;
				if (descrip.getConflictKind() == SVNConflictDescriptor.Kind.property)
					handling = propertyHandling;
				else
					handling = textHandling;
				if (handling == ISVNConflictResolver.Choice.chooseMerged) {
					ConflictResolution conflictResolution = getConflictResolution(descrip);
					conflictResolutions.add(conflictResolution);
					return new SVNConflictResult(conflictResolution.getResolution(), conflictResolution.getMergedPath());
				} else {
					conflictResolutions.add(new ConflictResolution(descrip, handling));
					return new SVNConflictResult(handling, descrip.getMergedPath());
				}
			}
		} else {
			return new SVNConflictResult(ISVNConflictResolver.Choice.postpone, descrip.getMergedPath());
		}
	}
	
	private ConflictResolution getConflictResolution(final SVNConflictDescriptor descrip) {
		wait = false;
		dialogWizard = null;		
		ConflictResolution conflictResolution = null;
		if ((descrip.getReason() == SVNConflictDescriptor.Reason.deleted || descrip.getReason() == SVNConflictDescriptor.Reason.moved_away) && applyToAllTreeConflictResolution != null) {
			conflictResolution = new ConflictResolution(descrip, applyToAllTreeConflictResolution.getResolution());
		}
		else if (descrip.getConflictKind() == SVNConflictDescriptor.Kind.property && applyToAllPropertyResolution != null)
			conflictResolution = new ConflictResolution(descrip, applyToAllPropertyResolution.getResolution());
		else if (descrip.isBinary() && applyToAllBinaryResolution != null)
			conflictResolution = new ConflictResolution(descrip, applyToAllBinaryResolution.getResolution());
		else if (descrip.getConflictKind() != SVNConflictDescriptor.Kind.property && !descrip.isBinary() && applyToAllTextResolution != null) 
			conflictResolution = new ConflictResolution(descrip, applyToAllTextResolution.getResolution());
		if (conflictResolution == null) {
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					dialogWizard = new DialogWizard(DialogWizard.CONFLICT_HANDLING);
					dialogWizard.setConflictDescriptor(descrip);
					dialogWizard.setResources(new IResource[] { resource });
					ConflictWizardDialog dialog = new ConflictWizardDialog(Display.getDefault().getActiveShell(), dialogWizard);
					dialog.open();
				}		
			});	
			while (dialogWizard == null || dialogWizard.getConflictResolution() == null) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					// we're still waiting...
				}
			}		
			conflictResolution = dialogWizard.getConflictResolution();
			if (conflictResolution.isApplyToAll()) {
				if (descrip.getReason() == SVNConflictDescriptor.Reason.deleted || descrip.getReason() == SVNConflictDescriptor.Reason.moved_away) {
					applyToAllTreeConflictResolution = conflictResolution;
				}
				else if (descrip.getConflictKind() == SVNConflictDescriptor.Kind.property)
					applyToAllPropertyResolution = conflictResolution;
				else if (descrip.isBinary())
					applyToAllBinaryResolution = conflictResolution;
				else
					applyToAllTextResolution = conflictResolution;			
			}
		}
		try {
			if (conflictResolution.getResolution() == ConflictResolution.FILE_EDITOR) { 
				
				if (schedulingRule != null) {
					Job.getJobManager().endRule(schedulingRule);
				}
				
				finished = false;
				workingTempFile = null;
				File pathFile = new File(descrip.getPath());
				mergedFile = new File(descrip.getMergedPath());
				try {
					workingTempFile = createTempFile(pathFile);
					copyFile(mergedFile, workingTempFile);	
				} catch (IOException e) {
					e.printStackTrace();
				}			
				IFileStore fileStore =  EFS.getLocalFileSystem().getStore(new Path(workingTempFile.getAbsolutePath()));
				final IWorkbenchPage page = part.getSite().getPage();
				final IEditorInput editorInput = new ExternalFileEditorInput(fileStore);
				final String editorId= getEditorId(fileStore);
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						try {
							final IEditorPart editorPart = page.openEditor(editorInput, editorId);
							if (editorPart == null) {
								resolution = ISVNConflictResolver.Choice.postpone;
								finished = true;									
							} else {
								IPartListener2 closeListener = new IPartListener2() {
									
									public void partClosed(IWorkbenchPartReference partRef) {
										if (partRef.getPart(false) == editorPart) {
											finishEditing(descrip);
										}
									}							
									
									public void partActivated(IWorkbenchPartReference partRef) {}
									public void partBroughtToTop(IWorkbenchPartReference partRef) {}
									public void partDeactivated(IWorkbenchPartReference partRef) {}
									public void partHidden(IWorkbenchPartReference partRef) {}
									public void partInputChanged(IWorkbenchPartReference partRef) {}
									public void partOpened(IWorkbenchPartReference partRef) {}
									public void partVisible(IWorkbenchPartReference partRef) {}
									
								};
								page.addPartListener(closeListener);
							}
						} catch (PartInitException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}		
					}		
				});	
				while (!finished) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {} 				
				}
				if (schedulingRule != null) {
					Job.getJobManager().beginRule(schedulingRule, null);
				}
				conflictResolution = new ConflictResolution(descrip, resolution);
				if (!conflictResolution.isResolved()) {
					if (descrip.getConflictKind() == SVNConflictDescriptor.Kind.property)
						applyToAllPropertyResolution = null;
					else if (descrip.isBinary())
						applyToAllBinaryResolution = null;
					else
						applyToAllTextResolution = null;					
				}
			}
			if (conflictResolution.getResolution() == ConflictResolution.CONFLICT_EDITOR) {
				if (schedulingRule != null) {
					Job.getJobManager().endRule(schedulingRule);
				}
				File pathFile = new File(descrip.getPath());
				File conflictNewFile = new File(descrip.getTheirPath());
				File conflictWorkingFile = new File(descrip.getMyPath());
				
				if (descrip.getConflictKind() == SVNConflictDescriptor.Kind.property && descrip.getMergedPath() == null) {
					try {
						final String theirValue = getPropertyValue(conflictNewFile);
						final String myValue = getPropertyValue(conflictWorkingFile);
						dialogWizard = null;
						Display.getDefault().asyncExec(new Runnable() {
							public void run() {
								dialogWizard = new DialogWizard(DialogWizard.PROPERTY_VALUE_SELECTION);
								dialogWizard.setConflictDescriptor(descrip);
								dialogWizard.setMyValue(myValue);
								dialogWizard.setIncomingValue(theirValue);
								dialogWizard.setResources(new IResource[] { resource });
								ConflictWizardDialog dialog = new ConflictWizardDialog(Display.getDefault().getActiveShell(), dialogWizard);
								dialog.open();
							}		
						});	
						while (dialogWizard == null || dialogWizard.getValueToUse() == null) {}								
						String valueToUse = dialogWizard.getValueToUse();
						if (!dialogWizard.isConflictResolved()) {
							conflictResolution = new ConflictResolution(descrip, ISVNConflictResolver.Choice.postpone);
							return conflictResolution;
						} else {
							mergeTempFile = createTempFile(pathFile);
							setPropertyValue(mergeTempFile, valueToUse);
							conflictResolution = new ConflictResolution(descrip, ISVNConflictResolver.Choice.chooseMerged);
							conflictResolution.setMergedPath(mergeTempFile.getAbsolutePath());
							return conflictResolution;
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}					
				}
				workingTempFile = null;
				mergeTempFile = null;
				File mergedFile;
				if (descrip.getMergedPath() == null) {
					mergedFile = new File(descrip.getPath());
				} else {
					mergedFile = new File(descrip.getMergedPath());
				}
				try {
					workingTempFile = createTempFile(pathFile);
					copyFile(conflictWorkingFile, workingTempFile);
					
					mergeTempFile = createTempFile(pathFile);
					copyFile(mergedFile, mergeTempFile);				
				} catch (IOException e) {
					e.printStackTrace();
				}
				File conflictOldFile = new File(descrip.getBasePath());		
				final BuiltInEditConflictsAction editConflictsAction = new BuiltInEditConflictsAction(conflictNewFile, conflictOldFile, workingTempFile, mergedFile, pathFile.getName(), descrip);
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						editConflictsAction.run(null);
					}		
				});	
				while (editConflictsAction.getBuiltInConflictsCompareInput() == null || !editConflictsAction.getBuiltInConflictsCompareInput().isFinished()) { 
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {} 
				}
				if (schedulingRule != null) {
					Job.getJobManager().beginRule(schedulingRule, null);
				}
				if (editConflictsAction.getBuiltInConflictsCompareInput().isResolved()) {
					conflictResolution = new ConflictResolution(descrip, editConflictsAction.getBuiltInConflictsCompareInput().getResolution());
				} else {
					conflictResolution = new ConflictResolution(descrip, editConflictsAction.getBuiltInConflictsCompareInput().getResolution());
					if (descrip.getConflictKind() == SVNConflictDescriptor.Kind.property)
						applyToAllPropertyResolution = null;
					else if (descrip.isBinary())
						applyToAllBinaryResolution = null;
					else
						applyToAllTextResolution = null;
					try {
						copyFile(mergeTempFile, mergedFile);
					} catch (IOException e) {
						e.printStackTrace();
					}
					
				}
			}
		} catch (final RuntimeException re) {
			conflictResolution = new ConflictResolution(descrip, ISVNConflictResolver.Choice.postpone);
			wait = true;
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.SVNConflictResolver_0, Messages.SVNConflictResolver_1 + re.getLocalizedMessage() + Messages.SVNConflictResolver_2);		
					re.printStackTrace();
					wait = false;
				}		
			});			
		}
		while (wait) {}
		return conflictResolution;
	}
	
	private File createTempFile(File baseOnFile) throws IOException {
		try {
			FileNode fileNode = new FileNode(baseOnFile);
			File tempFile = File.createTempFile(fileNode.getPrefix(), "." + fileNode.getType()); //$NON-NLS-1$
			tempFile.deleteOnExit();
			return tempFile;
		} catch (RuntimeException e) { throw e; }
	}	
	
	private String getEditorId(IFileStore file) {
		IWorkbench workbench = PlatformUI.getWorkbench();
		IEditorRegistry editorRegistry= workbench.getEditorRegistry();
		IEditorDescriptor descriptor= editorRegistry.getDefaultEditor(file.getName(), getContentType(file));

		// check the OS for in-place editor (OLE on Win32)
		if (descriptor == null && editorRegistry.isSystemInPlaceEditorAvailable(file.getName()))
			descriptor= editorRegistry.findEditor(IEditorRegistry.SYSTEM_INPLACE_EDITOR_ID);
		
		// check the OS for external editor
		if (descriptor == null && editorRegistry.isSystemExternalEditorAvailable(file.getName()))
			descriptor= editorRegistry.findEditor(IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID);
		
		if (descriptor != null)
			return descriptor.getId();
		
		return EditorsUI.DEFAULT_TEXT_EDITOR_ID;
	}
	
	private IContentType getContentType (IFileStore fileStore) {
		if (fileStore == null)
			return null;

		InputStream stream= null;
		try {
			stream= fileStore.openInputStream(EFS.NONE, null);
			return Platform.getContentTypeManager().findContentTypeFor(stream, fileStore.getName());
		} catch (IOException x) {
			return null;
		} catch (CoreException x) {
			return null;
		} finally {
			try {
				if (stream != null)
					stream.close();
			} catch (IOException x) {
			}
		}
	}	
	
	private void finishEditing(SVNConflictDescriptor descrip) {
		DialogWizard dialogWizard = new DialogWizard(DialogWizard.FINISHED_EDITING);
		dialogWizard.setConflictDescriptor(descrip);
		ConflictWizardDialog dialog = new ConflictWizardDialog(Display.getDefault().getActiveShell(), dialogWizard);
		dialog.open();
		try {
			copyFile(workingTempFile, mergedFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		resolution = dialogWizard.getResolution();
		finished = true;			
	}

	public ConflictResolution[] getConflictResolutions() {
		ConflictResolution[] conflictResolutionArray = new ConflictResolution[conflictResolutions.size()];
		conflictResolutions.toArray(conflictResolutionArray);
		return conflictResolutionArray;
	}

	public int getTextHandling() {
		return textHandling;
	}

	public int getBinaryHandling() {
		return binaryHandling;
	}
	
	public int getResolvedConflictCount(int conflictKind) {
		if (textHandling == ISVNConflictResolver.Choice.postpone && binaryHandling == ISVNConflictResolver.Choice.postpone)
			return 0;
		int resolvedConflicts = 0;
		Iterator<ConflictResolution> iter = conflictResolutions.iterator();
		while (iter.hasNext()) {
			ConflictResolution conflictResolution = iter.next();
			if (conflictResolution.getResolution() != ISVNConflictResolver.Choice.postpone &&
					conflictResolution.getConflictDescriptor().getConflictKind() == conflictKind)
				resolvedConflicts++;
		}
		return resolvedConflicts;
	}

	public void setPart(IWorkbenchPart part) {
		this.part = part;
	}
	
	public void setSchedulingRule(ISchedulingRule schedulingRule) {
		this.schedulingRule = schedulingRule;
	}

	private void copyFile(File fromFile, File toFile) throws IOException {
		FileInputStream from = null;
		FileOutputStream to = null;
		try {
			from = new FileInputStream(fromFile);
			to = new FileOutputStream(toFile);
			byte[] buffer = new byte[4096];
			int bytes_read;
			while ((bytes_read = from.read(buffer)) != -1)
				to.write(buffer, 0, bytes_read);
		}
		finally {
			if (from != null) try { from.close(); } catch (IOException e) {}
			if (to != null) try { to.close(); } catch (IOException e) {}
		}
	}
	
	private String getPropertyValue(File propertyFile) throws IOException {
		StringBuffer fileData = new StringBuffer();
		BufferedReader reader = new BufferedReader(new FileReader(propertyFile));
		char[] buf = new char[1024];
		int numRead = 0;
        while((numRead=reader.read(buf)) != -1){
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
            buf = new char[1024];
        }
        reader.close();		
		return fileData.toString();
	}
	
	private void setPropertyValue(File propertyFile, String value) throws IOException {
		FileOutputStream out = new FileOutputStream(propertyFile);
		PrintStream p = new PrintStream(out);
		p.print(value);
		p.close();
	}
	
	public static String getResolutionDescription(String resolution) {
		if (resolution == null || resolution.trim().length() == 0 || resolution.equals("0")) return "Unresolved"; //$NON-NLS-1$ //$NON-NLS-2$
		if (resolution.equals("Y")) return "Resolution unknown"; //$NON-NLS-1$ //$NON-NLS-2$
		int res = Integer.parseInt(resolution);
		switch (res) {
		case ISVNConflictResolver.Choice.chooseBase:
			return "Base version used"; //$NON-NLS-1$
		case ISVNConflictResolver.Choice.chooseTheirsFull:
			return "Incoming version used";	 //$NON-NLS-1$
		case ISVNConflictResolver.Choice.chooseMineFull:
			return "Local version used"; //$NON-NLS-1$
		case ISVNConflictResolver.Choice.chooseTheirs:
			return "Incoming version used for conflicted hunks"; //$NON-NLS-1$
		case ISVNConflictResolver.Choice.chooseMine:
			return "Local version used for conflicted hunks"; //$NON-NLS-1$
		case ISVNConflictResolver.Choice.chooseMerged:
			return "Merged version used"; //$NON-NLS-1$
		default:
			return "Unresolved"; //$NON-NLS-1$
		}
	}

}
