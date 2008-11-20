package org.tigris.subversion.subclipse.graph.popup.actions;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.history.Alias;
import org.tigris.subversion.subclipse.graph.cache.Node;
import org.tigris.subversion.subclipse.graph.editors.RevisionGraphEditor;
import org.tigris.subversion.subclipse.graph.editors.RevisionGraphEditorInput;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.operations.BranchTagOperation;
import org.tigris.subversion.subclipse.ui.wizards.BranchTagWizard;
import org.tigris.subversion.subclipse.ui.wizards.ClosableWizardDialog;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class BranchTagAction extends Action {
	private RevisionGraphEditor editor;
	private Node node;

	public BranchTagAction(String text, RevisionGraphEditor editor, Node node) {		
		super(text);
		this.editor = editor;
		this.node = node;
	}

	public void run() {
		BranchTagWizard wizard;
		final IResource resource = ((RevisionGraphEditorInput)editor.getEditorInput()).getResource();
		ISVNRemoteResource remoteResource = ((RevisionGraphEditorInput)editor.getEditorInput()).getRemoteResource();
		if (resource == null) {
			ISVNRemoteResource[] resources = { remoteResource };
			wizard = new BranchTagWizard(resources);
		} else {
			IResource[] resources = { resource };
			wizard = new BranchTagWizard(resources);
		}
		wizard.setRevisionNumber(node.getRevision());
    	WizardDialog dialog = new ClosableWizardDialog(Display.getDefault().getActiveShell(), wizard);
    	if (dialog.open() == WizardDialog.OK) {	
            final SVNUrl sourceUrl = wizard.getUrl();
            final SVNUrl destinationUrl = wizard.getToUrl();
            final String message = wizard.getComment();
            final SVNRevision revision = wizard.getRevision();
            final boolean makeParents = wizard.isMakeParents();
            final SVNUrl[] sourceUrls = wizard.getUrls();
            final boolean createOnServer = wizard.isCreateOnServer();
            final Alias newAlias = wizard.getNewAlias();
            final boolean switchAfter = wizard.isSwitchAfterBranchTag();
            try {
                BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
                    public void run() {
                      try {
                    	if (resource == null) {
	                        ISVNClientAdapter client = SVNProviderPlugin.getPlugin().getSVNClientManager().getSVNClient();
	                        client.copy(sourceUrl, destinationUrl, message, revision, makeParents);
                    	} else {
                    		IResource[] resources = { resource };
            	            BranchTagOperation branchTagOperation = new BranchTagOperation(editor.getEditorSite().getPart(), resources, sourceUrls, destinationUrl, createOnServer, revision, message);
            	            branchTagOperation.setMakeParents(makeParents);
            	            branchTagOperation.setNewAlias(newAlias);
            	            branchTagOperation.switchAfterTagBranchOperation(switchAfter);
            	            branchTagOperation.run();        		                    		
                    	}
                      } catch(Exception e) {
                        MessageDialog.openError(Display.getDefault().getActiveShell(), Policy.bind("HistoryView.createTagFromRevision"), e
                            .getMessage());
                      }
                    }
                  });
            } catch(Exception e) {
              MessageDialog.openError(Display.getDefault().getActiveShell(), Policy.bind("HistoryView.createTagFromRevision"), e
                  .getMessage());
            }        		    		
    	}
	}

}
