/*******************************************************************************
 * Copyright (c) 2009 CollabNet.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     CollabNet - initial API and implementation
 ******************************************************************************/
package com.collabnet.subversion.merge;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.collabnet.subversion.merge"; //$NON-NLS-1$
	
	// Merge input provider extension point ID
	public static final String MERGE_INPUT_PROVIDERS = "com.collabnet.subversion.merge.mergeInputProviders"; //$NON-NLS-1$

	// Merge provider class
	public static final String MERGE_PROVIDER = "CollabNet Desktop"; //$NON-NLS-1$
	public static final String MERGE_PROVIDER_SET = "merge_provider_set"; //$NON-NLS-1$
	
	// The shared instance
	private static Activator plugin;
	
	// Merge input providers
	private static IMergeInputProvider[] mergeInputProviders;
	private static List mergeInputImages = new ArrayList();
	
	private Hashtable imageDescriptors;
	
	// Images
	public static final String IMAGE_COLLABNET = "cn_icon.png"; //$NON-NLS-1$
	public static final String IMAGE_COLLABNET_WIZBAN = "newsite_wizban.png"; //$NON-NLS-1$
	public static final String IMAGE_TEAMFORGE_LOGO = "logo-collabnet.png"; //$NON-NLS-1$
	public static final String IMAGE_MERGE_OUTPUT = "merge_output.gif"; //$NON-NLS-1$
	public static final String IMAGE_MERGE_OUTPUT_IN_PROGRESS = "merge_output_in_progress.gif"; //$NON-NLS-1$
	public static final String IMAGE_MERGE_OUTPUT_ABNORMAL = "merge_output_abnormal.gif"; //$NON-NLS-1$
	public static final String IMAGE_MERGE_OUTPUT_ABORTED = "merge_aborted.gif"; //$NON-NLS-1$
	public static final String IMAGE_REFRESH = "refresh.gif"; //$NON-NLS-1$
	public static final String IMAGE_EXPAND_ALL = "expandall.gif"; //$NON-NLS-1$
	public static final String IMAGE_COLLAPSE_ALL = "collapseall.gif"; //$NON-NLS-1$
	public static final String IMAGE_LAYOUT_FLAT = "flatLayout.gif"; //$NON-NLS-1$
	public static final String IMAGE_LAYOUT_COMPRESSED = "compressedLayout.gif"; //$NON-NLS-1$
	public static final String IMAGE_CONFLICT = "conflict.gif"; //$NON-NLS-1$
	public static final String IMAGE_PRESENTATION = "presentation.gif"; //$NON-NLS-1$	
	public static final String IMAGE_REMOVE = "remove.gif"; //$NON-NLS-1$
	public static final String IMAGE_REMOVE_ALL = "remove_all.gif"; //$NON-NLS-1$
	public static final String IMAGE_TASK_REPOSITORY = "repository.gif"; //$NON-NLS-1$
	
	public static final String IMAGE_MERGE_WIZARD = "mergestream_wizban.png"; //$NON-NLS-1$
	public static final String IMAGE_EXPORT_MERGE_OUTPUT_WIZARD = "export_merge.png"; //$NON-NLS-1$
	public static final String IMAGE_SVN = "svn_wizban.png"; //$NON-NLS-1$
	
	public static final String IMAGE_CHANGE_SETS = "changesets.png"; //$NON-NLS-1$
	
	public static final String IMAGE_CHECK = "checkmark.png"; //$NON-NLS-1$ 
	public static final String IMAGE_PROBLEM = "icon-error.png"; //$NON-NLS-1$ 
	
	// Overlays
	public static final String IMAGE_OVERLAY_ADD = "ovr/r_inadd_ov.gif"; //$NON-NLS-1$
	public static final String IMAGE_OVERLAY_CHANGE = "ovr/r_inchg_ov.gif"; //$NON-NLS-1$
	public static final String IMAGE_OVERLAY_DELETE = "ovr/r_indel_ov.gif"; //$NON-NLS-1$
	public static final String IMAGE_OVERLAY_RESOLVED = "ovr/resolved_ov.gif"; //$NON-NLS-1$
	public static final String IMAGE_OVERLAY_CONFLICTED_CHANGE = "ovr/confchg_ov.gif"; //$NON-NLS-1$
	public static final String IMAGE_OVERLAY_CONFLICTED_DELETE = "ovr/confdel_ov.gif"; //$NON-NLS-1$
	public static final String IMAGE_OVERLAY_CONFLICTED_ADD = "ovr/confadd_ov.gif"; //$NON-NLS-1$
	public static final String IMAGE_OVERLAY_ERROR = "ovr/error_ov.gif"; //$NON-NLS-1$
	public static final String IMAGE_OVERLAY_TREE_CONFLICT = "ovr/tree_conflict_ov.gif"; //$NON-NLS-1$
	
	// Properties Overlays
	public static final String IMAGE_OVERLAY_PROPERTY_ADD = "ovr/props/add_ov.gif"; //$NON-NLS-1$
	public static final String IMAGE_OVERLAY_PROPERTY_CHANGE = "ovr/props/chg_ov.gif"; //$NON-NLS-1$
	public static final String IMAGE_OVERLAY_PROPERTY_DELETE = "ovr/props/del_ov.gif"; //$NON-NLS-1$
	public static final String IMAGE_OVERLAY_PROPERTY_CONFLICTED_ADD = "ovr/props/confadd_ov.gif"; //$NON-NLS-1$
	public static final String IMAGE_OVERLAY_PROPERTY_CONFLICTED_CHANGE = "ovr/props/confchg_ov.gif"; //$NON-NLS-1$
	public static final String IMAGE_OVERLAY_PROPERTY_CONFLICTED_DELETE = "ovr/props/confdel_ov.gif"; //$NON-NLS-1$	
	
	/**
	 * The constructor
	 */
	public Activator() {
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		
		// Don't let error crash startup.  Just log it.
		try {
		
			// Initialize the merge input providers.
			mergeInputProviders = getMergeInputProviders();
			
			boolean mergeProviderSet = getPreferenceStore().getBoolean(MERGE_PROVIDER_SET);
			if (!mergeProviderSet) {
				getPreferenceStore().setValue(MERGE_PROVIDER_SET, true);
				SVNUIPlugin.getPlugin().getPreferenceStore().setValue(ISVNUIConstants.PREF_MERGE_PROVIDER, MERGE_PROVIDER);
			}
			
			MergeOutput[] mergeOutputs = MergeOutput.getMergeOutputs();
			for (int i = 0; i < mergeOutputs.length; i++) {
				if (!mergeOutputs[i].isNormalEnd()) {
					mergeOutputs[i].setAbnormalEnd(true);
					mergeOutputs[i].getMergeResults();
					mergeOutputs[i].store();
				}
			}
		
		} catch (Exception e) {
			handleError(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		Iterator iter = mergeInputImages.iterator();
		while(iter.hasNext()) {
			Image image = (Image)iter.next();
			image.dispose();
		}
		super.stop(context);
	}
	
	public boolean isDesktopInstalled() {
		Bundle bundle = null;
		try {
			bundle = Platform.getBundle("com.collabnet.desktop"); //$NON-NLS-1$
		} catch (Exception e) {}
		return bundle != null;
	}

	// Initialize the merge input providers by searching the registry for users of the
	// mergeInputProviders extension point.
	public static IMergeInputProvider[] getMergeInputProviders() throws Exception {
		if (mergeInputProviders == null) {
			ArrayList inputProviderList = new ArrayList();
			
			if (!getDefault().isDesktopInstalled()) {
				DesktopDownloadMergeInputProvider desktopDownloadMergeInputProvider = new DesktopDownloadMergeInputProvider();
				inputProviderList.add(desktopDownloadMergeInputProvider);
			}
			
			IExtensionRegistry extensionRegistry = Platform.getExtensionRegistry();
			IConfigurationElement[] configurationElements = extensionRegistry.getConfigurationElementsFor(MERGE_INPUT_PROVIDERS);
			for (int i = 0; i < configurationElements.length; i++) {
				IConfigurationElement configurationElement = configurationElements[i];
				IMergeInputProvider inputProvider = (IMergeInputProvider)configurationElement.createExecutableExtension("class"); //$NON-NLS-1$
				inputProvider.setText(configurationElement.getAttribute("name")); //$NON-NLS-1$
				inputProvider.setDescription(configurationElement.getAttribute("description").replaceAll("%NL%", System.getProperty("line.separator"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				String imageKey = configurationElement.getAttribute("image"); //$NON-NLS-1$
				if (imageKey != null) {
					ImageDescriptor imageDescriptor = imageDescriptorFromPlugin(PLUGIN_ID, "icons/" + imageKey); //$NON-NLS-1$
					Image image = imageDescriptor.createImage();
					mergeInputImages.add(image);
					inputProvider.setImage(image);
				}
				String seq = configurationElement.getAttribute("sequence"); //$NON-NLS-1$
				if (seq != null) inputProvider.setSequence(Integer.parseInt(seq));
				inputProviderList.add(inputProvider);
			}
			mergeInputProviders = new IMergeInputProvider[inputProviderList.size()];
			inputProviderList.toArray(mergeInputProviders);
			Arrays.sort(mergeInputProviders);
		}
		return mergeInputProviders;
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}
	
	public static String getMergeResultsLocation() {
		return ResourcesPlugin.getWorkspace().getRoot().getLocation().toString() + File.separator + ".metadata" + File.separator + ".plugins" + File.separator + PLUGIN_ID + File.separator + "MergeResults"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public static void handleError(Exception exception) {
		handleError(null, exception);
	}
	
	public static void handleError(String message, Exception exception) {
		if (message == null) getDefault().getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, exception.getMessage(), exception));
		else getDefault().getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, message, exception));
	}
	
	/**
	 * Save a previous merge source location
	 */
	public void saveMergeSource(String mergeFrom, String commonRoot) {
		List<String> fromUrls = new ArrayList<String>();
		fromUrls.add(mergeFrom);
		String previousFromUrls = null;
		try {
			previousFromUrls = Activator.getDefault().getDialogSettings().get("mergeFromUrls_" + commonRoot);
		} catch (Exception e) {}
		if (previousFromUrls != null) {
			String[] urls = previousFromUrls.split("\\,");
			for (String url : urls) {
				if (!fromUrls.contains(url)) fromUrls.add(url);
			}
		}
		StringBuffer mergeFromBuffer = new StringBuffer(mergeFrom);
		for (String url : fromUrls) {
			mergeFromBuffer.append("," + url);
		}
		Activator.getDefault().getDialogSettings().put("mergeFromUrls_" + commonRoot, mergeFromBuffer.toString());		
	}
	
    /**
     * Returns the image descriptor for the given image ID.
     * Returns null if there is no such image.
     */
    public ImageDescriptor getImageDescriptor(String id) {
    	if (imageDescriptors == null);
    		this.initializeImages();
		return (ImageDescriptor) imageDescriptors.get(id);
    }


	/**
	 * Creates an image and places it in the image registry.
	 */
	private void createImageDescriptor(String id) {
		imageDescriptors.put(id, imageDescriptorFromPlugin(PLUGIN_ID, "icons/" + id)); //$NON-NLS-1$
	}
	
	public static Image getImage(String key) {
		return getDefault().getImageRegistry().get(key);
	}
	
	private void initializeImages() {
		imageDescriptors = new Hashtable(40);
		createImageDescriptor(IMAGE_MERGE_OUTPUT);
		createImageDescriptor(IMAGE_MERGE_OUTPUT_IN_PROGRESS);
		createImageDescriptor(IMAGE_MERGE_OUTPUT_ABNORMAL);
		createImageDescriptor(IMAGE_MERGE_OUTPUT_ABORTED);
		createImageDescriptor(IMAGE_REFRESH);
		createImageDescriptor(IMAGE_EXPAND_ALL);
		createImageDescriptor(IMAGE_COLLAPSE_ALL);
		createImageDescriptor(IMAGE_LAYOUT_COMPRESSED);
		createImageDescriptor(IMAGE_LAYOUT_FLAT);
		createImageDescriptor(IMAGE_CONFLICT);
		createImageDescriptor(IMAGE_REMOVE);
		createImageDescriptor(IMAGE_REMOVE_ALL);
		createImageDescriptor(IMAGE_PRESENTATION);
		createImageDescriptor(IMAGE_OVERLAY_ADD);
		createImageDescriptor(IMAGE_OVERLAY_CHANGE);
		createImageDescriptor(IMAGE_OVERLAY_TREE_CONFLICT);
		createImageDescriptor(IMAGE_OVERLAY_DELETE);
		createImageDescriptor(IMAGE_OVERLAY_RESOLVED);
		createImageDescriptor(IMAGE_OVERLAY_CONFLICTED_ADD);
		createImageDescriptor(IMAGE_OVERLAY_CONFLICTED_CHANGE);
		createImageDescriptor(IMAGE_OVERLAY_CONFLICTED_DELETE);
		createImageDescriptor(IMAGE_OVERLAY_ERROR);
		createImageDescriptor(IMAGE_OVERLAY_PROPERTY_ADD);
		createImageDescriptor(IMAGE_OVERLAY_PROPERTY_CHANGE);
		createImageDescriptor(IMAGE_OVERLAY_PROPERTY_DELETE);
		createImageDescriptor(IMAGE_OVERLAY_PROPERTY_CONFLICTED_ADD);
		createImageDescriptor(IMAGE_OVERLAY_PROPERTY_CONFLICTED_CHANGE);
		createImageDescriptor(IMAGE_OVERLAY_PROPERTY_CONFLICTED_DELETE);
		createImageDescriptor(IMAGE_MERGE_WIZARD);
		createImageDescriptor(IMAGE_EXPORT_MERGE_OUTPUT_WIZARD);
		createImageDescriptor(IMAGE_CHECK);
		createImageDescriptor(IMAGE_PROBLEM);
		createImageDescriptor(IMAGE_SVN);
		createImageDescriptor(IMAGE_TASK_REPOSITORY);
		createImageDescriptor(IMAGE_COLLABNET);
		createImageDescriptor(IMAGE_COLLABNET_WIZBAN);
		createImageDescriptor(IMAGE_TEAMFORGE_LOGO);
		createImageDescriptor(IMAGE_CHANGE_SETS);
	}

	protected void initializeImageRegistry(ImageRegistry reg) {
		super.initializeImageRegistry(reg);
		reg.put(IMAGE_MERGE_OUTPUT, getImageDescriptor(IMAGE_MERGE_OUTPUT));
		reg.put(IMAGE_MERGE_OUTPUT_IN_PROGRESS, getImageDescriptor(IMAGE_MERGE_OUTPUT_IN_PROGRESS));
		reg.put(IMAGE_MERGE_OUTPUT_ABNORMAL, getImageDescriptor(IMAGE_MERGE_OUTPUT_ABNORMAL));
		reg.put(IMAGE_MERGE_OUTPUT_ABORTED, getImageDescriptor(IMAGE_MERGE_OUTPUT_ABORTED));
		reg.put(IMAGE_REFRESH, getImageDescriptor(IMAGE_REFRESH));
		reg.put(IMAGE_EXPAND_ALL, getImageDescriptor(IMAGE_EXPAND_ALL));
		reg.put(IMAGE_COLLAPSE_ALL, getImageDescriptor(IMAGE_COLLAPSE_ALL));
		reg.put(IMAGE_LAYOUT_COMPRESSED, getImageDescriptor(IMAGE_LAYOUT_COMPRESSED));
		reg.put(IMAGE_LAYOUT_FLAT, getImageDescriptor(IMAGE_LAYOUT_FLAT));
		reg.put(IMAGE_CONFLICT, getImageDescriptor(IMAGE_CONFLICT));
		reg.put(IMAGE_REMOVE, getImageDescriptor(IMAGE_REMOVE));
		reg.put(IMAGE_REMOVE_ALL, getImageDescriptor(IMAGE_REMOVE_ALL));
		reg.put(IMAGE_PRESENTATION, getImageDescriptor(IMAGE_PRESENTATION));
		reg.put(IMAGE_OVERLAY_ADD, getImageDescriptor(IMAGE_OVERLAY_ADD));
		reg.put(IMAGE_OVERLAY_CHANGE, getImageDescriptor(IMAGE_OVERLAY_CHANGE));
		reg.put(IMAGE_OVERLAY_TREE_CONFLICT, getImageDescriptor(IMAGE_OVERLAY_TREE_CONFLICT));
		reg.put(IMAGE_OVERLAY_DELETE, getImageDescriptor(IMAGE_OVERLAY_DELETE));
		reg.put(IMAGE_OVERLAY_RESOLVED, getImageDescriptor(IMAGE_OVERLAY_RESOLVED));
		reg.put(IMAGE_OVERLAY_CONFLICTED_ADD, getImageDescriptor(IMAGE_OVERLAY_CONFLICTED_ADD));
		reg.put(IMAGE_OVERLAY_CONFLICTED_CHANGE, getImageDescriptor(IMAGE_OVERLAY_CONFLICTED_CHANGE));
		reg.put(IMAGE_OVERLAY_CONFLICTED_DELETE, getImageDescriptor(IMAGE_OVERLAY_CONFLICTED_DELETE));
		reg.put(IMAGE_OVERLAY_ERROR, getImageDescriptor(IMAGE_OVERLAY_ERROR));
		reg.put(IMAGE_OVERLAY_PROPERTY_ADD, getImageDescriptor(IMAGE_OVERLAY_PROPERTY_ADD));
		reg.put(IMAGE_OVERLAY_PROPERTY_CHANGE, getImageDescriptor(IMAGE_OVERLAY_PROPERTY_CHANGE));
		reg.put(IMAGE_OVERLAY_PROPERTY_DELETE, getImageDescriptor(IMAGE_OVERLAY_PROPERTY_DELETE));
		reg.put(IMAGE_OVERLAY_PROPERTY_CONFLICTED_ADD, getImageDescriptor(IMAGE_OVERLAY_PROPERTY_CONFLICTED_ADD));
		reg.put(IMAGE_OVERLAY_PROPERTY_CONFLICTED_CHANGE, getImageDescriptor(IMAGE_OVERLAY_PROPERTY_CONFLICTED_CHANGE));
		reg.put(IMAGE_OVERLAY_PROPERTY_CONFLICTED_DELETE, getImageDescriptor(IMAGE_OVERLAY_PROPERTY_CONFLICTED_DELETE));		
		reg.put(IMAGE_MERGE_WIZARD, getImageDescriptor(IMAGE_MERGE_WIZARD));
		reg.put(IMAGE_EXPORT_MERGE_OUTPUT_WIZARD, getImageDescriptor(IMAGE_EXPORT_MERGE_OUTPUT_WIZARD));
		reg.put(IMAGE_SVN, getImageDescriptor(IMAGE_SVN));
		reg.put(IMAGE_CHECK, getImageDescriptor(IMAGE_CHECK));
		reg.put(IMAGE_PROBLEM, getImageDescriptor(IMAGE_PROBLEM));
		reg.put(IMAGE_TASK_REPOSITORY, getImageDescriptor(IMAGE_TASK_REPOSITORY));
		reg.put(IMAGE_COLLABNET, getImageDescriptor(IMAGE_COLLABNET));
		reg.put(IMAGE_COLLABNET_WIZBAN, getImageDescriptor(IMAGE_COLLABNET_WIZBAN));
		reg.put(IMAGE_TEAMFORGE_LOGO, getImageDescriptor(IMAGE_TEAMFORGE_LOGO));
		reg.put(IMAGE_CHANGE_SETS, getImageDescriptor(IMAGE_CHANGE_SETS));
	}	

}
