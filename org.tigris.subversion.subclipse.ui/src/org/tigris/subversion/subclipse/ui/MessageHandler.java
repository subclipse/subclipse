package org.tigris.subversion.subclipse.ui;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.tigris.subversion.subclipse.core.IMessageHandler;

public class MessageHandler implements IMessageHandler {

	public void handleMessage(final String title, final String message, int severity) {
		switch (severity) {
		case IMessageHandler.ERROR:
			Display.getDefault().syncExec(new Runnable() {				
				public void run() {
					MessageDialog.openError(Display.getDefault().getActiveShell(), title, message);
				}
			});
			break;
		case IMessageHandler.WARNING:
			Display.getDefault().syncExec(new Runnable() {				
				public void run() {
					MessageDialog.openWarning(Display.getDefault().getActiveShell(), title, message);
				}
			});			
			break;
		default:
			Display.getDefault().syncExec(new Runnable() {				
				public void run() {
					MessageDialog.openInformation(Display.getDefault().getActiveShell(), title, message);
				}
			});
			break;
		}

	}

}
