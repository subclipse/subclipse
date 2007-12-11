package org.tigris.subversion.subclipse.ui.actions;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Iterator;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
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
		BusyIndicator.showWhile(Display.getDefault(), new Runnable() {
			public void run() {
				IStructuredSelection selection = (IStructuredSelection)selectionProvider.getSelection();
				Iterator iter = selection.iterator();
				try {
					final GenerateChangeLogDialog dialog = new GenerateChangeLogDialog(Display.getDefault().getActiveShell());
					if (dialog.open() == GenerateChangeLogDialog.CANCEL) return;
					if (dialog.getOutput() == GenerateChangeLogDialog.FILESYSTEM) {
						File file = dialog.getFile();
						if (!file.exists()) file.createNewFile();
						BufferedWriter writer = new BufferedWriter(new FileWriter(file));
						while (iter.hasNext()) {
							Object object = iter.next();
							if (object instanceof LogEntry) {
								LogEntry logEntry = (LogEntry)object;
								if (dialog.getFormat() == GenerateChangeLogDialog.GNU)
									writer.write(logEntry.getGnuLog()); //$NON-NLS-2$
								else
									writer.write(logEntry.getChangeLog(dialog.getFormat() == GenerateChangeLogDialog.SVN_LOG_WITH_PATHS)); //$NON-NLS-2$
							}
						}
						writer.close();						
					} else {
						StringBuffer changeLog = new StringBuffer();
						while (iter.hasNext()) {
							Object object = iter.next();
							if (object instanceof LogEntry) {
								LogEntry logEntry = (LogEntry)object;
								if (dialog.getFormat() == GenerateChangeLogDialog.GNU)
									changeLog.append(logEntry.getGnuLog()); //$NON-NLS-2$
								else
									changeLog.append(logEntry.getChangeLog(dialog.getFormat() == GenerateChangeLogDialog.SVN_LOG_WITH_PATHS)); //$NON-NLS-2$
							}
						}
						TextTransfer plainTextTransfer = TextTransfer.getInstance();
						Clipboard clipboard= new Clipboard(Display.getDefault());		
						clipboard.setContents(
							new String[] {changeLog.toString().trim()}, 
							new Transfer[]{plainTextTransfer});	
						clipboard.dispose();									
					}
				} catch (Exception e) {
					MessageDialog.openError(Display.getDefault().getActiveShell(), Policy.bind("HistoryView.generateChangeLog"), e.getMessage()); //$NON-NLS-1$
				}
			}		
		});
	}

}
