package org.tigris.subversion.subclipse.tools.usage.tracker.internal;

import org.tigris.subversion.subclipse.tools.usage.util.HttpEncodingUtils;

/**
 * A focus point that always reports the current subclipse version as last
 * component.
 */
public class SuffixFocusPoint extends FocusPoint {

	private String suffix;

	public SuffixFocusPoint(String name, String suffix) {
		super(name);
		this.suffix = suffix;
	}

	public String getURI() {
		StringBuffer builder = new StringBuffer();
		appendContentURI(builder, this);
		appendToURI(suffix, builder);
		return HttpEncodingUtils.checkedEncodeUtf8(builder.toString());
	}

	public String getTitle() {
		StringBuffer builder = new StringBuffer();
		appendContentTitle(builder, this);
		appendToTitle(suffix, builder);
		return HttpEncodingUtils.checkedEncodeUtf8(builder.toString());
	}
}
