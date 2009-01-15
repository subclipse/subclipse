package org.tigris.subversion.subclipse.ui.util;

import java.util.HashMap;

import org.eclipse.compare.CompareUI;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;

public class ResourceSelectionTreeDecorator {
	public final static int PROPERTY_CHANGE = 0;
	public final static int TEXT_CONFLICTED = 1;
	public final static int UNVERSIONED = 2;
	public final static int MISSING = 3;
	public final static int TREE_CONFLICT = 4;
	public final static int PROPERTY_CONFLICTED = 5;
	
	private static ImageDescriptor[] fgImages = new ImageDescriptor[6];
	private static HashMap fgMap= new HashMap(20);
	
	private Image[] fImages= new Image[6];
	
	static {
		fgImages[PROPERTY_CHANGE] = SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_PROPERTY_CHANGED);
//		fgImages[TEXT_CONFLICTED] = SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_TEXT_CONFLICTED);
		fgImages[TEXT_CONFLICTED] = SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_CONFLICTED);
		fgImages[UNVERSIONED] = SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_QUESTIONABLE);
		fgImages[MISSING] = SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_DELETED);
		fgImages[TREE_CONFLICT] = SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_TREE_CONFLICT);
		fgImages[PROPERTY_CONFLICTED] = SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_PROPERTY_CONFLICTED);
	}
	
	public Image getImage(Image base, int kind) {

		Object key= base;

//		kind &= 3;

		Image[] a= (Image[]) fgMap.get(key);
		if (a == null) {
			a= new Image[6];
			fgMap.put(key, a);
		}
		Image b= a[kind];
		if (b == null) {
			boolean onLeft = kind == PROPERTY_CHANGE;
			b= new DiffImage(base, fgImages[kind], 22, onLeft).createImage();
			CompareUI.disposeOnShutdown(b);
			a[kind]= b;
		}
		return b;
	}	
	
	public void dispose() {
		if (fImages != null) {
			for (int i= 0; i < fImages.length; i++){
				Image image= fImages[i];
				if (image != null && !image.isDisposed())
					image.dispose();
			}
		}
		fImages= null;
	}	

}
