/*******************************************************************************
 * Copyright (c) 2010 Subclipse project and others.
 * Copyright (c) 2010 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
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

	public String getName() {
		return name;
	}

	public IFocusPoint setChild(IFocusPoint childFocusPoint) {
		this.childFocusPoint = childFocusPoint;
		return this;
	}

	public IFocusPoint getChild() {
		return childFocusPoint;
	}

	public String getURI() {
		StringBuffer builder = new StringBuffer();
		appendContentURI(builder, this);
		return HttpEncodingUtils.checkedEncodeUtf8(builder.toString());
	}

	protected void appendContentURI(StringBuffer builder, IFocusPoint focusPoint) {
		IFocusPoint parentFocuPoint = focusPoint.getChild();
		appendToURI(focusPoint.getName(), builder);
		if (parentFocuPoint != null) {
			appendContentURI(builder, parentFocuPoint);
		}
	}
	
	protected void appendToURI(String toAppend, StringBuffer builder) {
		builder.append(URI_SEPARATOR);
		builder.append(toAppend);
	}

	protected void appendToTitle(String toAppend, StringBuffer builder) {
		builder.append(TITLE_SEPARATOR);
		builder.append(toAppend);
	}

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