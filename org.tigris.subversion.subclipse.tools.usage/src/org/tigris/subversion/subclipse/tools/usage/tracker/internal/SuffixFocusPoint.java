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
