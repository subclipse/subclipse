/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Cédric Chabanois (cchabanois@ifrance.com) - modified for Subversion 
 *******************************************************************************/
package org.tigris.subversion.subclipse.ui.comments;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Status;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.internal.XMLWriter;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * the comments manager : holds previous comments 
 */
public class CommentsManager {
    // The previously remembered comment
    static String[] previousComments = new String[0];
    static final int MAX_COMMENTS = 10;
    private static final String COMMENT_HIST_FILE = "commitCommentHistory.xml"; //$NON-NLS-1$
    static final String ELEMENT_COMMIT_COMMENT = "CommitComment"; //$NON-NLS-1$
    static final String ELEMENT_COMMIT_HISTORY = "CommitComments"; //$NON-NLS-1$


    /**
     * Answer the list of comments that were previously used when committing.
     * @return String[]
     */
    public String[] getPreviousComments() {
        return previousComments;
    }

    /**
     * Method addComment.
     * @param string
     */
    public void addComment(String comment) {
        // Only add the comment if the first entry isn't the same already
        if (previousComments.length > 0 && previousComments[0].equals(comment)) return;
        // Insert the comment as the first element
        String[] newComments = new String[Math.min(previousComments.length + 1, MAX_COMMENTS)];
        newComments[0] = comment;
        for (int i = 1; i < newComments.length; i++) {
            newComments[i] = previousComments[i-1];
        }
        previousComments = newComments;
    }

    /**
     * load the comment history 
     */
    public void loadCommentHistory() {
        IPath pluginStateLocation = SVNUIPlugin.getPlugin().getStateLocation().append(COMMENT_HIST_FILE);
        File file = pluginStateLocation.toFile();
        if (!file.exists()) return;
        try {
            BufferedInputStream is = new BufferedInputStream(new FileInputStream(file));
            try {
            	SAXParserFactory factory = SAXParserFactory.newInstance();

                try {
                	SAXParser parser = factory.newSAXParser();                	
                    parser.parse(new InputSource(is), new CommentHistoryContentHandler());
                } catch (SAXException ex) {
                    throw new SVNException(Policy.bind("RepositoryManager.parsingProblem", COMMENT_HIST_FILE), ex); //$NON-NLS-1$
                } catch (ParserConfigurationException e) {
                    throw new SVNException(Policy.bind("RepositoryManager.parsingProblem", COMMENT_HIST_FILE), e); //$NON-NLS-1$				
                }
            } finally {
                is.close();
            }
        } catch (IOException e) {
            SVNUIPlugin.log(new Status(Status.ERROR, SVNUIPlugin.ID, TeamException.UNABLE, Policy.bind("RepositoryManager.ioException"), e)); //$NON-NLS-1$
        } catch (TeamException e) {
            SVNUIPlugin.log(e.getStatus());
        }
    }

    /**
     * save the comments history
     */ 
    public void saveCommentHistory() throws TeamException {
        IPath pluginStateLocation = SVNUIPlugin.getPlugin().getStateLocation();
        File tempFile = pluginStateLocation.append(COMMENT_HIST_FILE + ".tmp").toFile(); //$NON-NLS-1$
        File histFile = pluginStateLocation.append(COMMENT_HIST_FILE).toFile();
        try {
                 XMLWriter writer = new XMLWriter(new BufferedOutputStream(new FileOutputStream(tempFile)));
                 try {
                     writer.startTag(ELEMENT_COMMIT_HISTORY, null, false);
                     for (int i=0; i<previousComments.length && i<MAX_COMMENTS; i++)
                         writer.printSimpleTag(ELEMENT_COMMIT_COMMENT, previousComments[i]);
                     writer.endTag(ELEMENT_COMMIT_HISTORY);
                 } finally {
                         writer.close();
                 }
                 if (histFile.exists()) {
                         histFile.delete();
                 }
                 boolean renamed = tempFile.renameTo(histFile);
                 if (!renamed) {
                         throw new TeamException(new Status(Status.ERROR, SVNUIPlugin.ID, TeamException.UNABLE, Policy.bind("RepositoryManager.rename", tempFile.getAbsolutePath()), null)); //$NON-NLS-1$
                 }
         } catch (IOException e) {
                 throw new TeamException(new Status(Status.ERROR, SVNUIPlugin.ID, TeamException.UNABLE, Policy.bind("RepositoryManager.save",histFile.getAbsolutePath()), e)); //$NON-NLS-1$
         }
    }

}
