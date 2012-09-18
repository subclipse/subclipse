package org.tigris.subversion.subclipse.ui.dialogs;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;

public abstract class SvnDialog extends SubclipseTrayDialog {
	private String id;
	private IDialogSettings settings = SVNUIPlugin.getPlugin().getDialogSettings();
	protected static final int LABEL_WIDTH_HINT = 400;

	public SvnDialog(Shell shell, String id) {
		super(shell);
		this.id = id;
		int shellStyle = getShellStyle();
		setShellStyle(shellStyle | SWT.RESIZE);
	}
	
    protected void cancelPressed() {
		saveLocation();
		super.cancelPressed();
	}

	protected void okPressed() {
		saveLocation();
		super.okPressed();
	}
	
	protected Point getInitialLocation(Point initialSize) {
		if (id != null) {
		    try {
		        int x = settings.getInt(id + ".location.x"); //$NON-NLS-1$
		        int y = settings.getInt(id + ".location.y"); //$NON-NLS-1$
		        return new Point(x, y);
		    } catch (NumberFormatException e) {}
		}
        return super.getInitialLocation(initialSize);
    }
    
    protected Point getInitialSize() {
    	if (id != null) {
		    try {
		        int x = settings.getInt(id + ".size.x"); //$NON-NLS-1$
		        int y = settings.getInt(id + ".size.y"); //$NON-NLS-1$
		        return new Point(x, y);
		    } catch (NumberFormatException e) {}
    	}
        return super.getInitialSize();
    }	

	protected void saveLocation() {
    	if (id != null) {
	        int x = getShell().getLocation().x;
	        int y = getShell().getLocation().y;
	        settings.put(id + ".location.x", x); //$NON-NLS-1$
	        settings.put(id + ".location.y", y); //$NON-NLS-1$
	        x = getShell().getSize().x;
	        y = getShell().getSize().y;
	        settings.put(id + ".size.x", x); //$NON-NLS-1$
	        settings.put(id + ".size.y", y); //$NON-NLS-1$ 
    	}
    }
	
	protected Label createWrappingLabel(Composite parent, String text, int indent, int horizontalSpan) {
		Label label = new Label(parent, SWT.LEFT | SWT.WRAP);
		label.setText(text);
		GridData data = new GridData();
		data.horizontalSpan = horizontalSpan;
		data.horizontalAlignment = GridData.FILL;
		data.horizontalIndent = indent;
		data.grabExcessHorizontalSpace = true;
		data.widthHint = LABEL_WIDTH_HINT;
		label.setLayoutData(data);
		return label;
	}

}
