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
package org.tigris.subversion.subclipse.ui.operations;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.graphics.RGB;

/**
 * XXX This was copied from internal CVS UI code
 * 
 * Default implementation, assigns random colors to revisions based on committer id.
 * <p>
 * XXX This API is provisional and may change any time during the development of eclipse 3.2.
 * </p>
 * 
 * @since 3.2
 */
final class CommitterColors {
	private static CommitterColors fInstance;

	/**
	 * Returns the committer color singleton.
	 * 
	 * @return the committer color singleton
	 */
	public static CommitterColors getDefault() {
		if (fInstance == null)
			fInstance= new CommitterColors();
		return fInstance;
	}

	/** The color map. */
	private Map fColors= new HashMap();

	/** The number of colors that have been issued. */
	private int fCount= 0;

	private CommitterColors() {
	}

	/**
	 * Returns a unique color description for each string passed in. Colors for new committers are
	 * allocated to be as different as possible from the existing colors.
	 * 
	 * @param committer the committers unique name
	 * @return the corresponding color
	 */
	public RGB getCommitterRGB(String committer) {
		RGB rgb= (RGB) fColors.get(committer);
		if (rgb == null) {
			rgb= computeRGB(fCount++);
			fColors.put(committer, rgb);
		}
		return rgb;
	}

	private RGB computeRGB(int ordinal) {
		float hue= computeHue(ordinal);
		RGB rgb= new RGB(hue, 1.0f, 1.0f);
		return rgb;
	}

	private float computeHue(int ordinal) {
		int base= 3;
		int l= ordinal < base ? 0 : (int) Math.floor(Math.log(ordinal / base) / Math.log(2));
		int m= ((int) Math.pow(2, l)) * base;
		int j= ordinal < base ? ordinal : ordinal - m;
		float offset= ordinal < base ? 0.0f : (float) (180.0f / base / Math.pow(2, l));
		float delta= ordinal < base ? 120.0f : 2 * offset;
		float hue= (offset + j * delta) % 360;
		return hue;
	}
}
