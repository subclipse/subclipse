package org.tigris.subversion.subclipse.tools.usage.tracker.internal;

import org.tigris.subversion.subclipse.tools.usage.internal.SubclipseToolsUsageActivator;
import org.tigris.subversion.subclipse.tools.usage.util.HttpEncodingUtils;

/**
 * A focus point that always reports the current subclipse version as last
 * component.
 */
public class SubclipseToolsFocusPoint extends FocusPoint {

	public SubclipseToolsFocusPoint(String name) {
		super(name);
	}

	public String getURI() {
		StringBuilder builder = new StringBuilder();
		appendContentURI(builder, this);
		appendSubclipseVersion(builder, URI_SEPARATOR);
		return HttpEncodingUtils.checkedEncodeUtf8(builder.toString());
	}

	protected void appendSubclipseVersion(StringBuilder builder, String separator) {
		builder.append(separator);
		builder.append(getSubclipseVersion());
	}

	public String getTitle() {
		StringBuilder builder = new StringBuilder();
		appendContentTitle(builder, this);
		appendSubclipseVersion(builder, TITLE_SEPARATOR);
		return HttpEncodingUtils.checkedEncodeUtf8(builder.toString());
	}
	
	protected String getSubclipseVersion() {
		return SubclipseToolsUsageActivator.getDefault().getBundle().getVersion().toString();
	}
}
