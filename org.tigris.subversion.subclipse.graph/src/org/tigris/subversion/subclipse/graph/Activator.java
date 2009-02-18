package org.tigris.subversion.subclipse.graph;

import java.net.URL;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {
	
	private ImageDescriptors imageDescriptors;
	private URL baseURL;
	
	public static final Color FONT_COLOR = new Color(null, 1, 70, 122);
	public static final Color CONNECTION_COLOR = new Color(null, 172, 182, 198);
	public static final Color BGCOLOR = new Color(null, 250, 250, 250);

	public static final Color[] FG_COLORS = { new Color(null, 1, 70, 122),
		new Color(null, 76, 160, 104),
		new Color(null, 194, 128, 84),
		new Color(null, 76, 160, 20) };

	public static final Color[] BG_COLORS = { new Color(null, 216, 228, 248),
		new Color(null, 198, 240, 212),
		new Color(null, 240, 198, 170),
		new Color(null, 198, 240, 170) };

	// The plug-in ID
	public static final String PLUGIN_ID = "org.tigris.subversion.subclipse.graph";

	// The shared instance
	private static Activator plugin;
	
	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		baseURL = context.getBundle().getEntry("/"); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		FONT_COLOR.dispose();
		CONNECTION_COLOR.dispose();
		BGCOLOR.dispose();
		for (int i = 0; i < BG_COLORS.length; i++) BG_COLORS[i].dispose();
		for (int i = 0; i < FG_COLORS.length; i++) FG_COLORS[i].dispose();
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}
	
	public static void handleError(Exception exception) {
		handleError(null, exception);
	}
	
	public static void handleError(String message, Exception exception) {
		if (message == null) getDefault().getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, IStatus.ERROR, exception.getMessage(), exception));
		else getDefault().getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, IStatus.ERROR, message, exception));
	}
	
	public static void showErrorDialog(final String title, final Exception exception, boolean uiThread) {
		if (uiThread) showErrorDialog(title, exception);
		else {
			Display.getDefault().syncExec(new Runnable(){
				public void run() {
					showErrorDialog(title, exception);
				}			
			});
		}
	}
	
	public static void showErrorDialog(String title, Exception exception) {
		String message;
		if (exception.getMessage() == null) message = "" + exception;
		else message = exception.getMessage();
		MessageDialog.openError(Display.getDefault().getActiveShell(), title, message);
	}

    /**
     * Returns the image descriptor for the given image ID.
     * Returns null if there is no such image.
     */
    public ImageDescriptor getImageDescriptor(String id) {
        if (imageDescriptors == null) {
            imageDescriptors = new ImageDescriptors();
            imageDescriptors.initializeImages(baseURL);
        }
        return imageDescriptors.getImageDescriptor(id);
    }
}
