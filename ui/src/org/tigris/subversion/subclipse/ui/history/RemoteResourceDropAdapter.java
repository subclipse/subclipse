
package org.tigris.subversion.subclipse.ui.history;

import org.eclipse.team.ui.history.IHistoryView;
import org.eclipse.ui.part.IDropActionDelegate;
import org.tigris.subversion.subclipse.ui.actions.RemoteResourceTransfer;


public class RemoteResourceDropAdapter implements IDropActionDelegate {

	public boolean run(Object source, Object target) {
		if (source != null && target instanceof IHistoryView) {
			RemoteResourceTransfer transfer = RemoteResourceTransfer.getInstance();
			Object file = transfer.fromByteArray((byte[]) source);
			((IHistoryView) target).showHistoryFor(file);

		}
		return false;
	}

}
