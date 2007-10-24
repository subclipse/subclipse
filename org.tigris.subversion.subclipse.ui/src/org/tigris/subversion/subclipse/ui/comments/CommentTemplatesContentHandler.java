/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.tigris.subversion.subclipse.ui.comments;

import java.util.Vector;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

class CommentTemplatesContentHandler extends DefaultHandler {

	private StringBuffer buffer;
	private Vector comments;
	public CommentTemplatesContentHandler() {
	}

	/**
	 * @see ContentHandler#characters(char[], int, int)
	 */
	public void characters(char[] chars, int startIndex, int length) {
		if (buffer == null) return;
		buffer.append(chars, startIndex, length);
	}

	/**
	 * @see ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	public void startElement(
			String namespaceURI,
			String localName,
			String qName,
			Attributes atts) {
		
		String elementName = getElementName(localName, qName);
		if (elementName.equals(CommentsManager.ELEMENT_COMMIT_COMMENT)) {
			buffer = new StringBuffer();
			return;
		} 
		if (elementName.equals(CommentsManager.ELEMENT_COMMENT_TEMPLATES)) {
			comments = new Vector();
			return;
		}
	}
	
	/**
	 * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
	 */
	public void endElement(String namespaceURI, String localName, String qName) {
		String elementName = getElementName(localName, qName);
		if (elementName.equals(CommentsManager.ELEMENT_COMMIT_COMMENT)) {
			comments.add(buffer.toString());
			buffer = null;
			return;
		} 
		if (elementName.equals(CommentsManager.ELEMENT_COMMENT_TEMPLATES)) {
			CommentsManager.commentTemplates = new String[comments.size()];
			comments.copyInto(CommentsManager.commentTemplates);
			return;
		} 
	}
	
	/*
	 * Couldn't figure out from the SAX API exactly when localName vs. qName is used.
	 * However, the XML for project sets doesn't use namespaces so either of the two names
	 * is fine. Therefore, use whichever one is provided.
	 */
	private String getElementName(String localName, String qName) {
		if (localName != null && localName.length() > 0) {
			return localName;
		}
		return qName;
	}
}
