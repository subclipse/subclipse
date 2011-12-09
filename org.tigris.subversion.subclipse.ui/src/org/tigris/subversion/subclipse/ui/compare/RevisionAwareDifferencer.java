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
package org.tigris.subversion.subclipse.ui.compare;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.runtime.IProgressMonitor;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNDiffSummary;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.utils.Depth;

public final class RevisionAwareDifferencer extends Differencer {
    // comparison constants
    private static final int NODE_EQUAL = 0;
    private static final int NODE_NOT_EQUAL = 1;
    private static final int NODE_UNKNOWN = 2;
    
    private File[] diffFiles;
    private List changedResources;
    private SVNDiffSummary[] diffSummary;
    private String projectRelativePath;
    
    /**
     * compare two ResourceEditionNode
     */
    public RevisionAwareDifferencer() {
    	
    }
    public RevisionAwareDifferencer(File[] diffFiles) {
    	this.diffFiles = diffFiles;
    }
    public RevisionAwareDifferencer(SVNLocalResourceNode left,ResourceEditionNode right, File diffFile, SVNRevision pegRevision) {
    	if (diffFile == null) {
    		ISVNClientAdapter client = null;
        	try {
    			diffSummary = null;

        		client = SVNProviderPlugin.getPlugin().getSVNClientManager().getSVNClient();
        		if (pegRevision == null) {
        			pegRevision = SVNRevision.HEAD;
        		}
        		diffSummary = client.diffSummarize(left.getLocalResource().getUrl(), left.getLocalResource().getRevision(), right.getRemoteResource().getUrl(),
            			right.getRemoteResource().getRevision(), Depth.infinity, true);
           		projectRelativePath = left.getLocalResource().getResource().getProjectRelativePath().toString();
           		if (left.getLocalResource().isFolder() && projectRelativePath.length() > 0) projectRelativePath = projectRelativePath + "/";
        	} catch (Exception e) {
            } finally {
              SVNProviderPlugin.getPlugin().getSVNClientManager().returnSVNClient(client);
            }
    	} else {
    		diffFiles = new File[1];
    		diffFiles[0] = diffFile;
    	}
    }
    
    protected boolean contentsEqual(Object input1, Object input2) {
        int compare;
        if (input1 instanceof MultipleSelectionNode) {
        	return true;
        }
        if (input1 instanceof SVNLocalResourceNode) {
            compare = compareStatusAndRevisions(input1, input2);
        } else {
            compare = compareEditions(input1, input2);
        }
        if (compare == NODE_EQUAL) {
            return true;
        }
        if (compare == NODE_NOT_EQUAL) {
            return false;
        }
        //revert to slow content comparison
        return super.contentsEqual(input1, input2);
    }
    
    /** 
     * Called for every leaf or node compare to update progress information.
     */
    protected void updateProgress(IProgressMonitor progressMonitor, Object node) {
        if (node instanceof ITypedElement) {
            ITypedElement element = (ITypedElement)node;
            progressMonitor.subTask(Policy.bind("CompareEditorInput.fileProgress", new String[] {element.getName()})); //$NON-NLS-1$
            progressMonitor.worked(1);
        }
    }
    
    
    /**
     * Compares two nodes to determine if they are equal.  Returns NODE_EQUAL
     * of they are the same, NODE_NOT_EQUAL if they are different, and
     * NODE_UNKNOWN if comparison was not possible.
     */
    protected int compareStatusAndRevisions(Object left, Object right) {
        ISVNLocalResource localResource = null;
        if (left instanceof SVNLocalResourceNode) {
            localResource = ((SVNLocalResourceNode)left).getLocalResource();
        }
        
        ISVNRemoteResource edition = null;
        if (right instanceof ResourceEditionNode)
            edition = ((ResourceEditionNode)right).getRemoteResource();
        
        if (localResource == null || edition == null) {
            return NODE_UNKNOWN;
        }
            
        // if they're both non-files, they're the same
        if (localResource.isFolder() && edition.isContainer()) {
            return NODE_EQUAL;
        }
        // if they have different types, they're different
        if (localResource.isFolder() != edition.isContainer()) {
            return NODE_NOT_EQUAL;
        }
        
        String leftLocation = localResource.getRepository().getLocation();
        String rightLocation = edition.getRepository().getLocation();
        if (!leftLocation.equals(rightLocation)) {
            return NODE_UNKNOWN;
        }

        LocalResourceStatus localStatus = null;
        try {
            localStatus = localResource.getStatus();
        
            if (localStatus == null) {
                return NODE_UNKNOWN;
            }
            if (!localResource.isDirty() && localResource.getResource().getProjectRelativePath().toString().equals(edition.getProjectRelativePath()) &&
                localStatus.getLastChangedRevision().equals(edition.getLastChangedRevision())) {
                return NODE_EQUAL;
            }
            
            if(!localResource.isDirty() && !localResource.isFolder()) {
            	
                if (changedResources == null && diffFiles != null) {
                	parseDiffs();
                }
                
                if (changedResources == null) {
               		for (int i = 0; i < diffSummary.length; i++) {
            			if(localResource.getResource().getProjectRelativePath().toString().equals(projectRelativePath) || localResource.getResource().getProjectRelativePath().toString().equals(projectRelativePath + diffSummary[i].getPath())) {
            				return NODE_NOT_EQUAL;
            			}
            		} 
               		return NODE_EQUAL;
                }
                
                if (changedResources.contains(localResource.getResource().getLocation().toString())) {
                	return NODE_NOT_EQUAL;
                }
        		return NODE_EQUAL;
            }
        } catch (SVNException e) {
            return NODE_UNKNOWN;
        }
        
        return NODE_UNKNOWN;
    }
    
    /**
     * Compares two nodes to determine if they are equal.  Returns NODE_EQUAL
     * of they are the same, NODE_NOT_EQUAL if they are different, and
     * NODE_UNKNOWN if comparison was not possible.
     */
    protected int compareEditions(Object left, Object right) {
        // calculate the type for the left contribution
        ISVNRemoteResource leftEdition = null;
        if (left instanceof ResourceEditionNode) {
            leftEdition = ((ResourceEditionNode)left).getRemoteResource();
        }
        
        // calculate the type for the right contribution
        ISVNRemoteResource rightEdition = null;
        if (right instanceof ResourceEditionNode)
            rightEdition = ((ResourceEditionNode)right).getRemoteResource();
        
        
        // compare them
            
        if (leftEdition == null || rightEdition == null) {
            return NODE_UNKNOWN;
        }
        // if they're both non-files, they're the same
        if (leftEdition.isContainer() && rightEdition.isContainer()) {
            return NODE_EQUAL;
        }
        // if they have different types, they're different
        if (leftEdition.isContainer() != rightEdition.isContainer()) {
            return NODE_NOT_EQUAL;
        }
        
        String leftLocation = leftEdition.getRepository().getLocation();
        String rightLocation = rightEdition.getRepository().getLocation();
        if (!leftLocation.equals(rightLocation)) {
            return NODE_UNKNOWN;
        }

        if (leftEdition.getUrl().equals(rightEdition.getUrl()) &&
            leftEdition.getLastChangedRevision().equals(rightEdition.getLastChangedRevision())) {
            return NODE_EQUAL;
       } else {
//              if(considerContentIfRevisionOrPathDiffers()) {
            return NODE_UNKNOWN;
//              } else {
//                  return NODE_NOT_EQUAL;
//              }
        }
    }
    
    private void parseDiffs() {
    	changedResources = new ArrayList();
    	for (int i = 0; i < diffFiles.length; i++) {
    		parseFile(diffFiles[i]);
    	}
    }
    
    private void parseFile(File diffFile) {
//    	changedResources = new ArrayList();
    	BufferedReader input = null;
    	try {
			input = new BufferedReader(new FileReader(diffFile));
			String line = null; 
			while ((line = input.readLine()) != null){
				if (line.startsWith("Index:")) {
					changedResources.add(line.substring(7));
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {}
			}
		}
    }

//  private boolean considerContentIfRevisionOrPathDiffers() {
//  return SVNUIPlugin.getPlugin().getPreferenceStore().getBoolean(ISVNUIConstants.PREF_CONSIDER_CONTENTS);
//}
//
//public Viewer createDiffViewer(Composite parent) {
//  Viewer viewer = super.createDiffViewer(parent);
//  viewer.addSelectionChangedListener(new ISelectionChangedListener() {
//      public void selectionChanged(SelectionChangedEvent event) {
//          CompareConfiguration cc = getCompareConfiguration();
//          setLabels(cc, (IStructuredSelection)event.getSelection());
//      }
//  });
//  return viewer;
//}
}
