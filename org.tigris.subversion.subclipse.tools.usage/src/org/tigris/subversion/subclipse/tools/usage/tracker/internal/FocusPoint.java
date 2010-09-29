package org.tigris.subversion.subclipse.tools.usage.tracker.internal;

import org.tigris.subversion.subclipse.tools.usage.util.HttpEncodingUtils;

/**
 * Focus point of the application. It can represent data points like application
 * load, application module load, user actions, error events etc.
 * 
 * @see based on <a
 *      href="http://jgoogleAnalytics.googlecode.com">http://jgoogleAnalytics
 *      .googlecode.com</a>
 */
public class FocusPoint implements IFocusPoint {

	private String name;
	private IFocusPoint childFocusPoint;
	public static final String URI_SEPARATOR = "/";
	public static final String TITLE_SEPARATOR = "-";

	public FocusPoint(String name) {
		this.name = name;
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.tools.usage.tracker.internal.IFocusPoint#getName()
	 */
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.tools.usage.tracker.internal.IFocusPoint#setChild(org.tigris.subversion.subclipse.tools.usage.tracker.internal.IFocusPoint)
	 */
	public IFocusPoint setChild(IFocusPoint childFocusPoint) {
		this.childFocusPoint = childFocusPoint;
		return this;
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.tools.usage.tracker.internal.IFocusPoint#getChild()
	 */
	public IFocusPoint getChild() {
		return childFocusPoint;
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.tools.usage.tracker.internal.IFocusPoint#getURI()
	 */
	public String getURI() {
		StringBuffer builder = new StringBuffer();
		appendContentURI(builder, this);
		return HttpEncodingUtils.checkedEncodeUtf8(builder.toString());
	}

	protected void appendContentURI(StringBuffer builder, IFocusPoint focusPoint) {
		IFocusPoint parentFocuPoint = focusPoint.getChild();
		builder.append(URI_SEPARATOR);
		builder.append(focusPoint.getName());
		if (parentFocuPoint != null) {
			appendContentURI(builder, parentFocuPoint);
		}
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.tools.usage.tracker.internal.IFocusPoint#getTitle()
	 */
	public String getTitle() {
		StringBuffer builder = new StringBuffer();
		appendContentTitle(builder, this);
		return HttpEncodingUtils.checkedEncodeUtf8(builder.toString());
	}

	protected void appendContentTitle(StringBuffer builder, IFocusPoint focusPoint) {
		IFocusPoint childFocusPoint = focusPoint.getChild();
		builder.append(focusPoint.getName());
		if (childFocusPoint != null) {
			builder.append(TITLE_SEPARATOR);
			appendContentTitle(builder, childFocusPoint);
		}
	}
}
