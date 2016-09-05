package org.tigris.subversion.subclipse.graph;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;

import org.eclipse.jface.resource.ImageDescriptor;

public class ImageDescriptors {
    private Hashtable imageDescriptors = new Hashtable(20);
    
    /**
     * Creates an image and places it in the image registry.
     */
    protected void createImageDescriptor(String id, URL baseURL) {
        URL url = null;
        try {
            url = new URL(baseURL, IRevisionGraphConstants.ICON_PATH + id);
        } catch (MalformedURLException e) {
        }
        ImageDescriptor desc = ImageDescriptor.createFromURL(url);
        imageDescriptors.put(id, desc);
    }
    
    /**
     * Creates an image and places it in the image registry.
     */
    protected void createImageDescriptor(String id, String name, URL baseURL) {
        URL url = null;
        try {
            url = new URL(baseURL, IRevisionGraphConstants.ICON_PATH + name);
        } catch (MalformedURLException e) {
        }
        ImageDescriptor desc = ImageDescriptor.createFromURL(url);
        imageDescriptors.put(id, desc);
    }
    
    /**
     * Returns the image descriptor for the given image ID.
     * Returns null if there is no such image.
     */
    public ImageDescriptor getImageDescriptor(String id) {
        return (ImageDescriptor)imageDescriptors.get(id);
    }
    
    /**
     * Initializes the table of images used in this plugin.
     */
    public void initializeImages(URL baseURL) {
        createImageDescriptor(IRevisionGraphConstants.IMG_EXPORT_IMAGE, baseURL);
        createImageDescriptor(IRevisionGraphConstants.IMG_FILTER_CONNECTIONS, baseURL);
        createImageDescriptor(IRevisionGraphConstants.IMG_REVISION_GRAPH_CHRONOLOGICAL, baseURL);
        createImageDescriptor(IRevisionGraphConstants.IMG_SHOW_DELETED, baseURL);
    }
}
