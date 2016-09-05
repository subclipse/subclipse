package org.tigris.subversion.subclipse.mylyn;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IResource;
import org.eclipse.mylyn.internal.team.ui.AbstractCommitWorkflowProvider;
import org.tigris.subversion.subclipse.ui.actions.CommitAction;

public class SubclipseCommitWorkflowProvider extends
		AbstractCommitWorkflowProvider {

	public boolean hasOutgoingChanges(IResource[] resources) {
		CommitAction commitAction = new CommitAction("");
		commitAction.setSelectedResources(resources);
		return commitAction.hasOutgoingChanges();
	}

	public void commit(IResource[] resources) {
		CommitAction commitAction = new CommitAction("");
		commitAction.setSelectedResources(resources);
		try {
			commitAction.execute(null);
		} catch (InvocationTargetException ex) {
			// ignore
		} catch (InterruptedException ex) {
			// ignore
		}
	}
}
