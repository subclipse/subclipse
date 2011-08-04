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
package org.tigris.subversion.subclipse.ui.operations;

import java.lang.reflect.Method;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.revisions.Revision;
import org.eclipse.jface.text.revisions.RevisionInformation;
import org.eclipse.jface.text.source.LineRange;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditor;
import org.eclipse.ui.texteditor.ITextEditorExtension4;
import org.tigris.subversion.subclipse.core.ISVNRemoteFile;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.commands.GetAnnotationsCommand;
import org.tigris.subversion.subclipse.core.commands.GetLogsCommand;
import org.tigris.subversion.subclipse.core.history.ILogEntry;
import org.tigris.subversion.subclipse.core.history.LogEntry;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.annotations.AnnotateBlock;
import org.tigris.subversion.subclipse.ui.annotations.AnnotateBlocks;
import org.tigris.subversion.subclipse.ui.annotations.AnnotateView;
import org.tigris.subversion.svnclientadapter.ISVNAnnotations;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNLogMessage;
import org.tigris.subversion.svnclientadapter.SVNRevision;

/**
 * @author Brock Janiczak
 */
public class ShowAnnotationOperation extends SVNOperation {

    private final SVNRevision fromRevision;
    private final SVNRevision toRevision;
    private final ISVNRemoteFile remoteFile;
    private final boolean includeMergedRevisions;
    private final boolean ignoreMimeType;

    public ShowAnnotationOperation(IWorkbenchPart part, ISVNRemoteFile remoteFile, SVNRevision fromRevision, boolean includeMergedRevisions, boolean ignoreMimeType) {
        super(part);
        this.remoteFile = remoteFile;
        this.fromRevision = fromRevision;
        this.toRevision = remoteFile.getLastChangedRevision();
        this.includeMergedRevisions = includeMergedRevisions;
        this.ignoreMimeType = ignoreMimeType;
    }
    
    public ShowAnnotationOperation(IWorkbenchPart part, ISVNRemoteFile remoteFile, SVNRevision fromRevision, SVNRevision toRevision, boolean includeMergedRevisions, boolean ignoreMimeType) {
        super(part);
        this.remoteFile = remoteFile;
        this.fromRevision = fromRevision;
        this.toRevision = toRevision;
        this.includeMergedRevisions = includeMergedRevisions;
        this.ignoreMimeType = ignoreMimeType;
    }
    
    public ShowAnnotationOperation(IWorkbenchPart part, ISVNRemoteFile remoteFile, boolean includeMergedRevisions, boolean ignoreMimeType) {
        this(part, remoteFile, SVNRevision.START, includeMergedRevisions, ignoreMimeType);
    }
    
    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.ui.operations.RepositoryProviderOperation#getTaskName(org.tigris.subversion.subclipse.core.SVNTeamProvider)
     */
    protected String getTaskName(SVNTeamProvider provider) {
        return Policy.bind("AnnotateOperation.0", provider.getProject().getName()); //$NON-NLS-1$
    }

    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.ui.operations.SVNOperation#getTaskName()
     */
    protected String getTaskName() {
        return Policy.bind("AnnotateOperation.taskName"); //$NON-NLS-1$
    }
    
    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.ui.operations.SVNOperation#execute(org.eclipse.core.runtime.IProgressMonitor)
     */
    protected void execute(final IProgressMonitor monitor) throws SVNException, InterruptedException {
        monitor.beginTask(null, 100);

        try {
            GetAnnotationsCommand command = new GetAnnotationsCommand(remoteFile, fromRevision, toRevision, includeMergedRevisions, ignoreMimeType);
            command.run(new SubProgressMonitor(monitor, 100));
            final ISVNAnnotations annotations = command.getAnnotations();
            final AnnotateBlocks annotateBlocks = new AnnotateBlocks(annotations);
            
            
    		// this is not needed if there is no live annotate
 //   		final RevisionInformation information= createRevisionInformation(annotateBlocks, Policy.subMonitorFor(monitor, 20));
    		
            // We aren't running from a UI thread
    		getShell().getDisplay().asyncExec(new Runnable() {
    			public void run() {

//  				is there an open editor for the given input? If yes, use live annotate
    				final ITextEditorExtension4 editor= getEditor();
    				if (editor != null && promptForQuickDiffAnnotate()){
    					RevisionInformation information= createRevisionInformation(annotateBlocks, Policy.subMonitorFor(monitor, 20));
    					editor.showRevisionInformation(information, "org.tigris.subversion.subclipse.quickdiff.providers.SVNReferenceProvider"); //$NON-NLS-1$

    				} else {
    					try {
    						// Open the view
    						IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    						if (window != null) {
    							try {
    								PlatformUI.getWorkbench().showPerspective("org.tigris.subversion.subclipse.ui.svnPerspective", window); //$NON-NLS-1$
    							} catch (WorkbenchException e1) {              
    								// If this does not work we will just open the view in the
    								// current perspective.
    							}
    						}
    						AnnotateView view = AnnotateView.openInActivePerspective();
    						view.showAnnotations(remoteFile, annotateBlocks.getAnnotateBlocks(), annotations.getInputStream());
    					} catch (PartInitException e1) {
    						collectStatus(e1.getStatus());
    					}
    				}
    			}
    		});
        } catch (SVNException e) {
			if (e.operationInterrupted()) {
				showCancelledMessage();
			} else {
				collectStatus(e.getStatus());
			}
        } finally {
            monitor.done();
        }
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.team.ui.TeamOperation#getGotoAction()
     */
    protected IAction getGotoAction() {
        return super.getGotoAction();
    }
    
	private ITextEditorExtension4 getEditor() {
        final IWorkbench workbench= PlatformUI.getWorkbench();
        final IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
        IEditorReference[] references= window.getActivePage().getEditorReferences();
        IResource resource= remoteFile.getResource();
		if (resource == null)
			return null;

		for (int i= 0; i < references.length; i++) {
			IEditorReference reference= references[i];
			try {
				if (resource != null && resource.equals(reference.getEditorInput().getAdapter(IFile.class))) {
					IEditorPart editor= reference.getEditor(false);
					if (editor instanceof ITextEditorExtension4)
						return (ITextEditorExtension4) editor;
					else {
						//editor opened is not a text editor - reopen file using the defualt text editor
						IEditorPart part = getPart().getSite().getPage().openEditor(new FileEditorInput((IFile) resource), IDEWorkbenchPlugin.DEFAULT_TEXT_EDITOR_ID, true, IWorkbenchPage.MATCH_NONE);
						if (part != null && part instanceof AbstractDecoratedTextEditor)
							return (AbstractDecoratedTextEditor)part;
					}
				}
			} catch (PartInitException e) {
				// ignore
			}
		}
		
		//no existing editor references found, try to open a new editor for the file	
		if (resource instanceof IFile){
			try {
				IEditorDescriptor descrptr = IDE.getEditorDescriptor((IFile) resource);
				//try to open the associated editor only if its an internal editor
				if (descrptr.isInternal()){
					IEditorPart part = IDE.openEditor(getPart().getSite().getPage(), (IFile) resource);
					if (part instanceof AbstractDecoratedTextEditor)
						return (AbstractDecoratedTextEditor)part;
					
					//editor opened is not a text editor - close it
					getPart().getSite().getPage().closeEditor(part, false);
				}
				//open file in default text editor	
				IEditorPart part = IDE.openEditor(getPart().getSite().getPage(), (IFile) resource, IDEWorkbenchPlugin.DEFAULT_TEXT_EDITOR_ID);
				if (part != null && part instanceof AbstractDecoratedTextEditor)
					return (AbstractDecoratedTextEditor)part;
				
			} catch (PartInitException e) {
			}
		}
	
        return null;
	}
    
    private RevisionInformation createRevisionInformation(final AnnotateBlocks annotateBlocks, IProgressMonitor monitor) {
    	Map logEntriesByRevision= new HashMap();
    GetLogsCommand logCommand = new GetLogsCommand(this.remoteFile, SVNRevision.HEAD, this.fromRevision, this.toRevision, false, 0, null, false);
		try {
			logCommand.run(monitor);
			ILogEntry[] logEntries = logCommand.getLogEntries();
			
			for (int i = 0; i < logEntries.length; i++) {
				ILogEntry logEntry = logEntries[i];
				logEntriesByRevision.put(new Long(logEntry.getRevision().getNumber()), logEntry);
			}
		} catch (SVNException e) {
			SVNUIPlugin.log(e);
		}

		RevisionInformation info= new RevisionInformation();
		
		try {
		  // Have to use reflection for compatibility with Eclipse 3.2 API		
		  // info.setHoverControlCreator(new AnnotationControlCreator("Press F2 for focus."));
		  // info.setInformationPresenterControlCreator(new AnnotationControlCreator(null));
			
			String tooltipAffordance = "Press F2 for focus.";
			try {
				// Will either set an affordance, or null if the tooltip affordance turned is off
				tooltipAffordance = (String) EditorsUI.class.getMethod("getTooltipAffordanceString", null).invoke(null, null);
			} catch (Exception e) {
				//ignore
			}

		  Class infoClass = info.getClass();
		  Class[] paramTypes = {IInformationControlCreator.class};
      Method setHoverControlCreator = infoClass.getMethod("setHoverControlCreator", paramTypes);
      Method setInformationPresenterControlCreator = infoClass.getMethod("setInformationPresenterControlCreator", paramTypes);
  
  		final class AnnotationControlCreator implements IInformationControlCreator {
  		  private final String statusFieldText;
  		  
  		  public AnnotationControlCreator(String statusFieldText) {
  		    this.statusFieldText = statusFieldText;
  		  }
  		  
  		  public IInformationControl createInformationControl(Shell parent) {
  		    return new SourceViewerInformationControl(parent, SWT.TOOL,
  		        SWT.NONE, JFaceResources.DEFAULT_FONT, statusFieldText);
  		  }
  		}

  		setHoverControlCreator.invoke(info, new Object[] {new AnnotationControlCreator(tooltipAffordance)});
  		setInformationPresenterControlCreator.invoke(info, new Object[] {new AnnotationControlCreator(null)});
  		
		} catch (Exception e) {
      // ignore
    }
		
		final CommitterColors colors= CommitterColors.getDefault();

		HashMap sets= new HashMap();
		
		for (Iterator blocks= annotateBlocks.getAnnotateBlocks().iterator(); blocks.hasNext();) {
			final AnnotateBlock block= (AnnotateBlock) blocks.next();
			final String revisionString= Long.toString(block.getRevision());
			LogEntry logEntry = (LogEntry) logEntriesByRevision.get(new Long(block.getRevision()));
			final String logMessage;
			if (logEntry == null) {
				logMessage = getSingleEntry(remoteFile, new Long(block.getRevision()));
			} else {
				logMessage = logEntry.getComment();
			}
				
			Revision revision= (Revision) sets.get(revisionString);
			if (revision == null) {
				revision= new Revision() {
					public Object getHoverInfo() {
							return block.getUser() + " " + revisionString + " " + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(block.getDate()) + "\n\n" + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							(logMessage != null ? logMessage : ""); //$NON-NLS-1$
					}
					
					public String getAuthor() {
						return block.getUser();
					}
					
					public String getId() {
						return revisionString;
					}
					
					public Date getDate() {
						return block.getDate();
					}
					
					public RGB getColor() {
						return colors.getCommitterRGB(getAuthor());
					}
					
				};
				sets.put(revisionString, revision);
				info.addRevision(revision);
			}
			revision.addRange(new LineRange(block.getStartLine(), block.getEndLine() - block.getStartLine() + 1));
		}
		return info;
	}
    
    /**
	 * Returns true if the user wishes to always use the live annotate view, false otherwise.
	 * @return
	 */
	private boolean promptForQuickDiffAnnotate(){
		//check whether we should ask the user.
		final IPreferenceStore store = SVNUIPlugin.getPlugin().getPreferenceStore();
		final String option = store.getString(ISVNUIConstants.PREF_USE_QUICKDIFFANNOTATE);
		
		if (option.equals(MessageDialogWithToggle.ALWAYS))
			return true; //use live annotate
		else if (option.equals(MessageDialogWithToggle.NEVER))
			return false; //don't use live annotate
		
		final MessageDialogWithToggle dialog = MessageDialogWithToggle.openYesNoQuestion(Utils.getShell(null), Policy.bind("AnnotateOperation_QDAnnotateTitle"),
				Policy.bind("AnnotateOperation_QDAnnotateMessage"), Policy.bind("AnnotateOperation_4"), false, store, ISVNUIConstants.PREF_USE_QUICKDIFFANNOTATE);
		
		final int result = dialog.getReturnCode();
		switch (result) {
			//yes
			case IDialogConstants.YES_ID:
			case IDialogConstants.OK_ID :
			    return true;
		}
		return false;
	}
	
	private String getSingleEntry(ISVNRemoteFile file, Long revLong) {
		ISVNClientAdapter client = null;
		try {
			client = file.getRepository().getSVNClient();
			SVNRevision revision = SVNRevision.getRevision(revLong.toString());
			ISVNLogMessage [] messages = client.getLogMessages(file.getRepository().getRepositoryRoot(), revision, revision, false);
			if (messages.length == 1)
				return messages[0].getMessage();
			else
				return null;
		} catch (Exception e) {
			return null;
		}
		finally {
			file.getRepository().returnSVNClient(client);
		}
	}
}
