package org.tigris.subversion.subclipse.ui.wizards;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;

public class WizardDialogWithPersistedLocation extends ClosableWizardDialog {
	
	private String id;
	private IDialogSettings settings;

	public WizardDialogWithPersistedLocation(Shell parentShell, IWizard newWizard, String id) {
		super(parentShell, newWizard);
		this.id = id;
		settings = SVNUIPlugin.getPlugin().getDialogSettings();
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
	    try {
	        int x = settings.getInt(id + ".location.x"); //$NON-NLS-1$
	        int y = settings.getInt(id + ".location.y"); //$NON-NLS-1$
	        return new Point(x, y);
	    } catch (NumberFormatException e) {}
	    return super.getInitialLocation(initialSize);
	}
	
	protected Point getInitialSize() {
	    try {
	        int x = settings.getInt(id + ".size.x"); //$NON-NLS-1$
	        int y = settings.getInt(id + ".size.y"); //$NON-NLS-1$
	        return new Point(x, y);
	    } catch (NumberFormatException e) {}		
		 return super.getInitialSize();
	}
	
	protected void saveLocation() {
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
