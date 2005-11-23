package org.tigris.subversion.subclipse.ui.operations;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.tigris.subversion.subclipse.core.ISVNRemoteFile;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.commands.GetAnnotationsCommand;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.annotations.AnnotateBlocks;
import org.tigris.subversion.subclipse.ui.annotations.AnnotateView;
import org.tigris.subversion.svnclientadapter.ISVNAnnotations;
import org.tigris.subversion.svnclientadapter.SVNRevision;

/**
 * @author Brock Janiczak
 */
public class ShowAnnotationOperation extends SVNOperation {

    private final SVNRevision fromRevision;
    private final SVNRevision toRevision;
    private final ISVNRemoteFile remoteFile;

    public ShowAnnotationOperation(IWorkbenchPart part, ISVNRemoteFile remoteFile, SVNRevision fromRevision) {
        super(part);
        this.remoteFile = remoteFile;
        this.fromRevision = fromRevision;
        this.toRevision = remoteFile.getLastChangedRevision();
    }
    
    public ShowAnnotationOperation(IWorkbenchPart part, ISVNRemoteFile remoteFile, SVNRevision fromRevision, SVNRevision toRevision) {
        super(part);
        this.remoteFile = remoteFile;
        this.fromRevision = fromRevision;
        this.toRevision = toRevision;
    }
    
    public ShowAnnotationOperation(IWorkbenchPart part, ISVNRemoteFile remoteFile) {
        this(part, remoteFile, SVNRevision.START);
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
    protected void execute(IProgressMonitor monitor) throws SVNException, InterruptedException {
        monitor.beginTask(null, 100);

        try {
            GetAnnotationsCommand command = new GetAnnotationsCommand(remoteFile, fromRevision, toRevision);
            command.run(new SubProgressMonitor(monitor, 100));
            final ISVNAnnotations annotations = command.getAnnotations();
            final AnnotateBlocks annotateBlocks = new AnnotateBlocks(annotations);
            // We aren't running from a UI thread
            getShell().getDisplay().asyncExec(new Runnable() {
                public void run() {
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
            });
        } catch (SVNException e) {
            collectStatus(e.getStatus());
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
}
