/*******************************************************************************
 * Copyright (c) 2005, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.authentication;

import java.util.Vector;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

class KeyFileHistoryContentHandler extends DefaultHandler {

	private StringBuffer buffer;
	private Vector keyFiles;
	public KeyFileHistoryContentHandler() {
	}

	public void characters(char[] chars, int startIndex, int length) {
		if (buffer == null) return;
		buffer.append(chars, startIndex, length);
	}

	public void startElement(
			String namespaceURI,
			String localName,
			String qName,
			Attributes atts) {
        String elementName = getElementName(namespaceURI, localName, qName); 		
		
		if (elementName.equals(KeyFilesManager.ELEMENT_KEYFILE)) {
			buffer = new StringBuffer();
			return;
		} 
		if (elementName.equals(KeyFilesManager.ELEMENT_KEYFILE_HISTORY)) {
			keyFiles = new Vector(KeyFilesManager.MAX_FILES);
			return;
		}
	}

	public void endElement(String namespaceURI, String localName, String qName) {
        String elementName = getElementName(namespaceURI, localName, qName);		
		
		if (elementName.equals(KeyFilesManager.ELEMENT_KEYFILE)) {
			keyFiles.add(buffer.toString());
			buffer = null;
			return;
		} 
		if (elementName.equals(KeyFilesManager.ELEMENT_KEYFILE_HISTORY)) {
            KeyFilesManager.previousKeyFiles = new String[keyFiles.size()];
			keyFiles.copyInto(KeyFilesManager.previousKeyFiles);
			return;
		} 
	}

    private String getElementName(String namespaceURI, String localName, String qName) { 
            if (localName != null && localName.length() > 0) { 
                    return localName; 
            } else { 
                    return qName; 
            } 
    } 	
}
