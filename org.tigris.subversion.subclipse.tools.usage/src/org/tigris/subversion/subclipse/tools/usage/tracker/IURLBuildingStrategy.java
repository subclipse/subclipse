package org.tigris.subversion.subclipse.tools.usage.tracker;

import java.io.UnsupportedEncodingException;

import org.tigris.subversion.subclipse.tools.usage.tracker.internal.IFocusPoint;

/**
 * Interface for the URL building strategy
 */
public interface IURLBuildingStrategy {

	public String build(IFocusPoint focusPoint) throws UnsupportedEncodingException;

}
