package org.tigris.subversion.subclipse.ui.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.tigris.subversion.subclipse.core.history.LogEntry;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.dialogs.GenerateChangeLogDialog;

public class GenerateChangeLogAction extends Action {
	private ISelectionProvider selectionProvider;

	public GenerateChangeLogAction(ISelectionProvider selectionProvider) {
		super(Policy.bind("HistoryView.generateChangeLog")); //$NON-NLS-1$
		this.selectionProvider = selectionProvider;
	}

	public void run() {
		IStructuredSelection selection = (IStructuredSelection)selectionProvider.getSelection();
		Iterator iter = selection.iterator();
		List<LogEntry> logEntries = new ArrayList<LogEntry>();
		while (iter.hasNext()) {
			Object object = iter.next();
			if (object instanceof LogEntry) {
				logEntries.add((LogEntry)object);
			}
		}
		GenerateChangeLogDialog dialog = new GenerateChangeLogDialog(Display.getDefault().getActiveShell(), logEntries);
		dialog.open();
	}

}
