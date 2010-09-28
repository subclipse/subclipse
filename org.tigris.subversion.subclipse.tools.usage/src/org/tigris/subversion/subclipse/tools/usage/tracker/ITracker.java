package org.tigris.subversion.subclipse.tools.usage.tracker;

import java.io.UnsupportedEncodingException;

import org.tigris.subversion.subclipse.tools.usage.tracker.internal.IFocusPoint;

public interface ITracker {

	/**
	 * Track the focusPoint in the application synchronously. <br/>
	 * <red><b>Please be cognizant while using this method. Since, it would have
	 * a peformance hit on the actual application. Use it unless it's really
	 * needed</b></red>
	 * 
	 * @param focusPoint
	 *            Focus point of the application like application load,
	 *            application module load, user actions, error events etc.
	 * @throws UnsupportedEncodingException
	 */

	public abstract void trackSynchronously(IFocusPoint focusPoint);

	/**
	 * Track the focusPoint in the application asynchronously. <br/>
	 * 
	 * @param focusPoint
	 *            Focus point of the application like application load,
	 *            application module load, user actions, error events etc.
	 */

	public abstract void trackAsynchronously(IFocusPoint focusPoint);

}