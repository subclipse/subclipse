package org.tigris.subversion.subclipse.ui.subscriber;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter.SyncInfoDirectionFilter;
import org.eclipse.team.ui.synchronize.ISynchronizeModelElement;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizeModelAction;
import org.eclipse.team.ui.synchronize.SynchronizeModelOperation;

public class GenerateDiffFileSynchronizeAction extends SynchronizeModelAction {

	public GenerateDiffFileSynchronizeAction(String text, ISynchronizePageConfiguration configuration) {
		super(text, configuration);
	}	
	
	protected FastSyncInfoFilter getSyncInfoFilter() {
		return new SyncInfoDirectionFilter(new int[] {SyncInfo.OUTGOING, SyncInfo.CONFLICTING });
	}	
	
	protected SynchronizeModelOperation getSubscriberOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
		List selectedResources = new ArrayList();
		Iterator iter = getStructuredSelection().iterator();
		while(iter.hasNext()) {
	    	ISynchronizeModelElement element = (ISynchronizeModelElement)iter.next();
	    	IResource resource = element.getResource();
	    	selectedResources.add(resource);
		}
		IResource[] resourceArray = new IResource[selectedResources.size()];
		selectedResources.toArray(resourceArray);
		GenerateDiffFileSynchronizeOperation generateDiffFileSynchronizeOperation = new GenerateDiffFileSynchronizeOperation(configuration, elements);
		generateDiffFileSynchronizeOperation.setSelectedResources(resourceArray);
		return generateDiffFileSynchronizeOperation;
	}

}
