/*******************************************************************************
 * Copyright (c) 2009 CollabNet.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     CollabNet - initial API and implementation
 ******************************************************************************/
package com.collabnet.subversion.merge.xml;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DOMUtil {
	
	public static String toString(Node node) {
		return formatNode(node, new StringBuffer()).toString();
	}
	
	private static StringBuffer formatNode(Node node, StringBuffer stringBuffer) {
		return formatNode(node, 0, stringBuffer);
	}
	
	private static StringBuffer formatNode(Node node, int indent, StringBuffer stringBuffer) {
	    int type = node.getNodeType();
	    switch (type) {
	      case Node.DOCUMENT_NODE: {
	        stringBuffer.append("&lt;?xml version=\"1.0\" ?>\n"); //$NON-NLS-1$
	        stringBuffer = formatNode(((Document)node).getDocumentElement(), indent, stringBuffer);
	        break;
	      }

	    case Node.ELEMENT_NODE: {
	      if (indent > 0) stringBuffer.append("\n"); //$NON-NLS-1$
	      for (int i = 0; i < indent; i++) stringBuffer.append(" "); //$NON-NLS-1$
	      stringBuffer.append("<" + node.getNodeName()); //$NON-NLS-1$
	      NamedNodeMap attrs = node.getAttributes();
	      for (int i = 0; i < attrs.getLength(); i++) {
	        Node attr = attrs.item(i);
	        stringBuffer.append("\n"); //$NON-NLS-1$
	        for (int j = 0; j < indent; j++) stringBuffer.append(" "); //$NON-NLS-1$
	        stringBuffer.append("   " + attr.getNodeName().trim() + //$NON-NLS-1$
	                         "=\"" + attr.getNodeValue().trim() + //$NON-NLS-1$
	                         "\""); //$NON-NLS-1$
	      }
	      stringBuffer.append(">"); //$NON-NLS-1$

	      indent = indent + 3;
	      NodeList children = node.getChildNodes();
	      if (children != null) {
	        int len = children.getLength();
	        for (int i = 0; i < len; i++)
	          stringBuffer = formatNode(children.item(i), indent, stringBuffer);
	      }
	      indent = indent - 3;
	      break;
	    }

	  case Node.ENTITY_REFERENCE_NODE: {
	    stringBuffer.append("&"); //$NON-NLS-1$
	    stringBuffer.append(node.getNodeName().trim());
	    stringBuffer.append(";"); //$NON-NLS-1$
	    break;
	  }

	case Node.CDATA_SECTION_NODE: {
	  stringBuffer.append("<![CDATA["); //$NON-NLS-1$
	  stringBuffer.append(node.getNodeValue().trim());
	  stringBuffer.append("]]>"); //$NON-NLS-1$
	  break;
	}

	case Node.TEXT_NODE: {
	  stringBuffer.append("\n"); //$NON-NLS-1$
	  for (int i = 0; i < indent; i++) stringBuffer.append(" "); //$NON-NLS-1$
	  stringBuffer.append(node.getNodeValue().trim());
	  break;
	}

	case Node.PROCESSING_INSTRUCTION_NODE: {
	  stringBuffer.append("<?"); //$NON-NLS-1$
	  stringBuffer.append(node.getNodeName().trim());
	  String data = node.getNodeValue().trim(); {
	    stringBuffer.append(" "); //$NON-NLS-1$
	    stringBuffer.append(data);
	  }
	  stringBuffer.append("?>"); //$NON-NLS-1$
	  break;
	}
	    }

	    if (type == Node.ELEMENT_NODE) {
	      stringBuffer.append("\n"); //$NON-NLS-1$
	      for (int i = 0; i < indent; i++) stringBuffer.append(" "); //$NON-NLS-1$
	      stringBuffer.append("</"); //$NON-NLS-1$
	      stringBuffer.append(node.getNodeName().trim());
	      stringBuffer.append('>');
	    }		
		return stringBuffer;
	}

	  public static Document parse(String fileName) {
	    Document document = null;
	    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

	    factory.setValidating(false);
	    factory.setNamespaceAware(true);

	    try {
	      DocumentBuilder builder = factory.newDocumentBuilder();
	      document = builder.parse( new File(fileName));
	      return document;
	    } catch (Exception e) {
	      System.out.println(e.getMessage());
	    }

	    return null;
	  }

	  public static void writeXmlToFile(String filename, Document document) {
	    try {
	      Source source = new DOMSource(document);

	      File file = new File(filename);
	      Result result = new StreamResult(file);

	      Transformer xformer = TransformerFactory.newInstance().newTransformer();
	      xformer.transform(source, result);
	    } catch (Exception e) {
	      System.out.println(e.getMessage());
	    }
	  }

	  public static int countByTagName(String tag, Document document){
	    NodeList list = document.getElementsByTagName(tag);
	    return list.getLength();
	  }

}
