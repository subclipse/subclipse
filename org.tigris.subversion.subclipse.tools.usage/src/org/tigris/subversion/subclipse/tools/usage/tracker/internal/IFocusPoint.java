package org.tigris.subversion.subclipse.tools.usage.tracker.internal;

public interface IFocusPoint {

	public abstract String getName();

	public abstract IFocusPoint setChild(IFocusPoint childFocusPoint);

	public abstract IFocusPoint getChild();

	public abstract String getURI();

	public abstract String getTitle();

}