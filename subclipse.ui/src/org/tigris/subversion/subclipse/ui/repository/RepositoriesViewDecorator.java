package org.tigris.subversion.subclipse.ui.repository;

import java.util.HashMap;

import org.eclipse.compare.CompareUI;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.decorator.SVNLightweightDecorator.CachedImageDescriptor;
import org.tigris.subversion.subclipse.ui.util.DiffImage;

public class RepositoriesViewDecorator {
	private static ImageDescriptor locked = new CachedImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_LOCKED));
	private static HashMap fgMap= new HashMap();
	
	public Image getImage(Image base) {
		Image decoratedImage = (Image) fgMap.get(base);
		if (decoratedImage != null) {
			return decoratedImage;
		}
		decoratedImage = new DiffImage(base, locked, 18, false).createImage();
		fgMap.put(base, decoratedImage);
		CompareUI.disposeOnShutdown(decoratedImage);
		return decoratedImage;
	}
	
}
