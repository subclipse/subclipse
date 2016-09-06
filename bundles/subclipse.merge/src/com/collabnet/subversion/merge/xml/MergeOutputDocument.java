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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.tigris.subversion.svnclientadapter.SVNRevisionRange;
import org.tigris.subversion.svnclientadapter.utils.Depth;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.collabnet.subversion.merge.MergeOutput;
import com.collabnet.subversion.merge.MergeResult;
import com.collabnet.subversion.merge.MergeSummaryResult;

public class MergeOutputDocument {
	private MergeOutput mergeOutput;
	private Document document;

	public MergeOutputDocument(MergeOutput mergeOutput) {
		this.mergeOutput = mergeOutput;
		document = createDocument();
	}
	
	private Document createDocument() {
		try {
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = builder.newDocument();
			Element rootElement = doc.createElement("mergeOutput"); //$NON-NLS-1$
			rootElement.setAttribute("resource", mergeOutput.getResource().getFullPath().toString()); //$NON-NLS-1$
			rootElement.setAttribute("date", mergeOutput.getMergeDate().toString()); //$NON-NLS-1$
			String status = null;
			if (mergeOutput.isInProgress()) status = "inProgress"; //$NON-NLS-1$
			else if (mergeOutput.isAbnormalEnd()) status = "aborted"; //$NON-NLS-1$
			else status = "normal"; //$NON-NLS-1$
			rootElement.setAttribute("status", status); //$NON-NLS-1$
			rootElement.setAttribute("revisionStart", Long.toString(mergeOutput.getLowerRevision())); //$NON-NLS-1$
			rootElement.setAttribute("revisionEnd", Long.toString(mergeOutput.getUpperRevision())); //$NON-NLS-1$
			Element workspaceElement = doc.createElement("workspace"); //$NON-NLS-1$
			workspaceElement.setAttribute("url", mergeOutput.getWorkspaceUrl()); //$NON-NLS-1$
			workspaceElement.setAttribute("revision", Long.toString(mergeOutput.getWorkspaceRevision())); //$NON-NLS-1$
			rootElement.appendChild(workspaceElement);
			Element commandElement = doc.createElement("mergeCommand"); //$NON-NLS-1$
			commandElement.appendChild(doc.createTextNode(mergeOutput.getMergeCommand()));
			rootElement.appendChild(commandElement);
			Element optionsElement = doc.createElement("mergeOptions"); //$NON-NLS-1$
			optionsElement.setAttribute("force", getBoolean(mergeOutput.getMergeOptions().isForce())); //$NON-NLS-1$
			optionsElement.setAttribute("ignoreAncestry", getBoolean(mergeOutput.getMergeOptions().isIgnoreAncestry())); //$NON-NLS-1$
			optionsElement.setAttribute("recurse", getBoolean(mergeOutput.getMergeOptions().isRecurse())); //$NON-NLS-1$
			String depth;
			switch (mergeOutput.getMergeOptions().getDepth()) {
			case Depth.empty:
				depth = "thisItem"; //$NON-NLS-1$
				break;
			case Depth.files:
				depth = "fileChildren"; //$NON-NLS-1$
				break;
			case Depth.immediates:
				depth = "immediateChildren"; //$NON-NLS-1$
				break;	
			case Depth.infinity:
				depth = "infinite"; //$NON-NLS-1$
				break;				
			default:
				depth = "workingCopy"; //$NON-NLS-1$
				break;
			}
			optionsElement.setAttribute("depth", depth); //$NON-NLS-1$
			if (mergeOutput.getMergeOptions().getFromRevision() != null) 
				optionsElement.setAttribute("revisionFrom", mergeOutput.getMergeOptions().getFromRevision().toString()); //$NON-NLS-1$
			if (mergeOutput.getMergeOptions().getToRevision() != null) 
				optionsElement.setAttribute("revisionTo", mergeOutput.getMergeOptions().getToRevision().toString());	 //$NON-NLS-1$
			Element fromUrlElement = doc.createElement("fromUrl"); //$NON-NLS-1$
			fromUrlElement.appendChild(doc.createTextNode(mergeOutput.getMergeOptions().getFromUrl().toString()));
			optionsElement.appendChild(fromUrlElement);
			if (mergeOutput.getMergeOptions().getRevisions() != null) {
				Element revisionRangesElement = doc.createElement("revisionRanges"); //$NON-NLS-1$
				SVNRevisionRange[] revisionRanges = mergeOutput.getMergeOptions().getRevisions();
				for (int i = 0; i < revisionRanges.length; i++) {
					Element revisionRangeElement = doc.createElement("revisionRange"); //$NON-NLS-1$
					revisionRangeElement.appendChild(doc.createTextNode(revisionRanges[i].toString()));
					revisionRangesElement.appendChild(revisionRangeElement);
				}
				optionsElement.appendChild(revisionRangesElement);
			}
			if (!(mergeOutput.getMergeOptions().getToUrl() == null) &&
			    (!mergeOutput.getMergeOptions().getToUrl().toString().equals(mergeOutput.getMergeOptions().getFromUrl().toString()))) {
				Element toUrlElement = doc.createElement("toUrl"); //$NON-NLS-1$
				toUrlElement.appendChild(doc.createTextNode(mergeOutput.getMergeOptions().getToUrl().toString()));
				optionsElement.appendChild(toUrlElement);	
			}
			rootElement.appendChild(optionsElement);
			Element summaryElement = doc.createElement("summary"); //$NON-NLS-1$
			Element fileStatisticsElement = doc.createElement("fileStats"); //$NON-NLS-1$
			Element propertyStatisticsElement = doc.createElement("propertiesStats"); //$NON-NLS-1$
			Element treeConflictStatisticsElement = doc.createElement("treeConflictStats"); //$NON-NLS-1$
			MergeSummaryResult[] summaryResults = mergeOutput.getMergeSummaryResults();
			boolean fileStats = false;
			boolean propStats = false;
			boolean treeStats = false;
			for (int i = 0; i < summaryResults.length; i++) {
				if (summaryResults[i].getType() == MergeSummaryResult.FILE) {
					fileStatisticsElement.setAttribute(summaryResults[i].getCategory().replaceAll(" ", "_"), summaryResults[i].getNumber()); //$NON-NLS-1$ //$NON-NLS-2$
					fileStats = true;
				}
				if (summaryResults[i].getType() == MergeSummaryResult.PROPERTY) {
					propertyStatisticsElement.setAttribute(summaryResults[i].getCategory().replaceAll(" ", "_"), summaryResults[i].getNumber()); //$NON-NLS-1$ //$NON-NLS-2$
					propStats = true;
				}
				if (summaryResults[i].getType() == MergeSummaryResult.TREE) {
					treeConflictStatisticsElement.setAttribute(summaryResults[i].getCategory().replaceAll(" ", "_"), summaryResults[i].getNumber()); //$NON-NLS-1$ //$NON-NLS-2$
					treeStats = true;
				}				
			}
			if (fileStats) summaryElement.appendChild(fileStatisticsElement);
			if (propStats) summaryElement.appendChild(propertyStatisticsElement);
			if (treeStats) summaryElement.appendChild(treeConflictStatisticsElement);
			rootElement.appendChild(summaryElement);
			Element resultsElement = doc.createElement("mergeResults"); //$NON-NLS-1$
			Element textElement = doc.createElement("text"); //$NON-NLS-1$
			Element mergedTextElement = doc.createElement("merged");			 //$NON-NLS-1$
			Element conflictedTextElement = doc.createElement("conflicted"); //$NON-NLS-1$
			Element skippedTextElement = doc.createElement("skipped"); //$NON-NLS-1$
			Element propertiesElement = doc.createElement("properties"); //$NON-NLS-1$
			Element mergedPropertiesElement = doc.createElement("merged");			 //$NON-NLS-1$
			Element conflictedPropertiesElement = doc.createElement("conflicted"); //$NON-NLS-1$
			Element skippedPropertiesElement = doc.createElement("skipped"); //$NON-NLS-1$
			Element treeConflictsElement = doc.createElement("treeConflicts"); //$NON-NLS-1$
			boolean treeConflicts = false;
			boolean mergedText = false;
			boolean conflictedText = false;
			boolean skippedText = false;
			boolean mergedProperties = false;
			boolean conflictedProperties = false;
			boolean skippedProperties = false;
			MergeResult[] mergeResults = mergeOutput.getMergeResults();
			for (int i = 0; i < mergeResults.length; i++) {
				if (mergeResults[i].hasTreeConflict()) {
					Element resultElement = doc.createElement("result"); //$NON-NLS-1$
					resultElement.setAttribute("resource", mergeResults[i].getResource().getFullPath().toString());					 //$NON-NLS-1$
					resultElement.setAttribute("resolved", getBoolean(mergeResults[i].isTreeConflictResolved())); //$NON-NLS-1$
					treeConflictsElement.appendChild(resultElement);
					treeConflicts = true;				
				}
				if (mergeResults[i].getAction() != null && mergeResults[i].getAction().trim().length() > 0) {
					Element resultElement = doc.createElement("result"); //$NON-NLS-1$
					resultElement.setAttribute("resource", mergeResults[i].getResource().getFullPath().toString());			 //$NON-NLS-1$
					if (mergeResults[i].isConflicted()) {
						resultElement.setAttribute("resolved", getBoolean(mergeResults[i].isResolved())); //$NON-NLS-1$
						if (mergeResults[i].getAction().equals(MergeResult.ACTION_ADD)) resultElement.setAttribute("added", "true"); //$NON-NLS-1$ //$NON-NLS-2$
						else if (mergeResults[i].getAction().equals(MergeResult.ACTION_DELETE)) resultElement.setAttribute("deleted", "true"); //$NON-NLS-1$ //$NON-NLS-2$
						conflictedTextElement.appendChild(resultElement);
						conflictedText = true;
					}
					else if (mergeResults[i].isSkip()) {
						skippedTextElement.appendChild(resultElement);
						skippedText = true;
					}
					else {
						if (mergeResults[i].getAction().equals(MergeResult.ACTION_ADD)) resultElement.setAttribute("added", "true"); //$NON-NLS-1$ //$NON-NLS-2$
						else if (mergeResults[i].getAction().equals(MergeResult.ACTION_DELETE)) resultElement.setAttribute("deleted", "true");						 //$NON-NLS-1$ //$NON-NLS-2$
						mergedTextElement.appendChild(resultElement);
						mergedText = true;
					}
				}
				if (mergeResults[i].getPropertyAction() != null && mergeResults[i].getPropertyAction().trim().length() > 0) {
					Element resultElement = doc.createElement("result"); //$NON-NLS-1$
					resultElement.setAttribute("resource", mergeResults[i].getResource().getFullPath().toString()); //$NON-NLS-1$
					if (mergeResults[i].isPropertyConflicted()) {
						resultElement.setAttribute("resolved", getBoolean(mergeResults[i].isPropertyResolved())); //$NON-NLS-1$
						if (mergeResults[i].getPropertyAction().equals(MergeResult.ACTION_ADD)) resultElement.setAttribute("added", "true"); //$NON-NLS-1$ //$NON-NLS-2$
						else if (mergeResults[i].getPropertyAction().equals(MergeResult.ACTION_DELETE)) resultElement.setAttribute("deleted", "true");						 //$NON-NLS-1$ //$NON-NLS-2$
						conflictedPropertiesElement.appendChild(resultElement);
						conflictedProperties = true;
					}
					else if (mergeResults[i].isSkip()) {
						skippedPropertiesElement.appendChild(resultElement);
						skippedProperties = true;
					}
					else {
						if (mergeResults[i].getPropertyAction().equals(MergeResult.ACTION_ADD)) resultElement.setAttribute("added", "true"); //$NON-NLS-1$ //$NON-NLS-2$
						else if (mergeResults[i].getPropertyAction().equals(MergeResult.ACTION_DELETE)) resultElement.setAttribute("deleted", "true");												 //$NON-NLS-1$ //$NON-NLS-2$
						mergedPropertiesElement.appendChild(resultElement);
						mergedProperties = true;
					}
				}				
			}
			if (mergedText || conflictedText || skippedText) {
				if (mergedText) textElement.appendChild(mergedTextElement);
				if (conflictedText) textElement.appendChild(conflictedTextElement);
				if (skippedText) textElement.appendChild(skippedTextElement);
				resultsElement.appendChild(textElement);
			}
			if (mergedProperties || conflictedProperties || skippedProperties) {
				if (mergedProperties) propertiesElement.appendChild(mergedPropertiesElement);
				if (conflictedProperties) propertiesElement.appendChild(conflictedPropertiesElement);
				if (skippedProperties) propertiesElement.appendChild(skippedPropertiesElement);
				resultsElement.appendChild(propertiesElement);
			}	
			if (treeConflicts) {
				resultsElement.appendChild(treeConflictsElement);
			}
			rootElement.appendChild(resultsElement);
			doc.appendChild(rootElement);
			return doc;
		} catch (Exception e) {
			return null;
		}
	}

	public Document getDocument() {
		return document;
	}
	
	private String getBoolean(boolean value) {
		if (value) return "true"; //$NON-NLS-1$
		else return "false"; //$NON-NLS-1$
	}
}
