package org.tigris.subversion.subclipse.graph;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Color;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {
	
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

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
}
