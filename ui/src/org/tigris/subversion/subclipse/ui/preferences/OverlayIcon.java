/*******************************************************************************
 * Copyright (c) 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.preferences;

import java.util.Arrays;

import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.*;

/**
 * An OverlayIcon consists of a main icon and several adornments.
 */
public class OverlayIcon extends CompositeImageDescriptor {
	// the base image
	private Image base;
	// the base as a descriptor
	private ImageDescriptor descriptorBase;
	// the overlay images
	private ImageDescriptor[] overlays;
	// the size
	private Point size;
	// the locations
	private int[] locations;
	
	public static final int TOP_LEFT = 0;
	public static final int TOP_RIGHT = 1;
	public static final int BOTTOM_LEFT = 2;
	public static final int BOTTOM_RIGHT = 3;
	
	public static final int DEFAULT_WIDTH= 22;
	public static final int DEFAULT_HEIGHT= 16;
	
	/**
	 * OverlayIcon constructor.
	 * 
	 * @param base the base image
	 * @param overlays the overlay images
	 * @param locations the location of each image
	 * @param size the size
	 */
	public OverlayIcon(Image base, ImageDescriptor[] overlays, int[] locations, Point size) {
		this.base = base;
		this.descriptorBase = null;
		this.overlays = overlays;
		this.locations = locations;
		this.size = size;
	}
	
	protected void drawOverlays(ImageDescriptor[] overlays, int[] locations) {
			Point size = getSize();
			for (int i = 0; i < overlays.length; i++) {
				ImageDescriptor overlay = overlays[i];
				ImageData overlayData = overlay.getImageData();
				switch (locations[i]) {
					case TOP_LEFT:
						drawImage(overlayData, 0, 0);			
						break;
					case TOP_RIGHT:
						drawImage(overlayData, size.x - overlayData.width, 0);			
						break;
					case BOTTOM_LEFT:
						drawImage(overlayData, 0, size.y - overlayData.height);			
						break;
					case BOTTOM_RIGHT:
						drawImage(overlayData, size.x - overlayData.width, size.y - overlayData.height);			
						break;
				}
			}
		}

	public boolean equals(Object o) {
		if (! (o instanceof OverlayIcon)) return false;
		OverlayIcon other = (OverlayIcon) o;
		return base.equals(other.base) && Arrays.equals(overlays, other.overlays);
	}

	public int hashCode() {
		int code = base.hashCode();
		for (int i = 0; i < overlays.length; i++) {
			code ^= overlays[i].hashCode();
		}
		return code;
	}


	protected void drawCompositeImage(int width, int height) {
		if(descriptorBase != null) {
			ImageData bg;
			if (descriptorBase == null || (bg= descriptorBase.getImageData()) == null)
				bg= DEFAULT_IMAGE_DATA;
			drawImage(bg, 0, 0);
		} else {
			drawImage(base.getImageData(), 0, 0);
		}
		drawOverlays(overlays, locations);
	}

	protected Point getSize() {
		return size;
	}
}
