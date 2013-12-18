/*******************************************************************************
 * Copyright (c) 2003, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.comments;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.internal.XMLWriter;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * the comments manager : holds previous comments 
 */
public class CommentsManager implements IPropertyChangeListener {
	private int maxComments;
    // The previously remembered comment
    static String[] previousComments = new String[0];
    static String[] commentTemplates = new String[0];
    private static final String COMMENT_HIST_FILE = "commitCommentHistory.xml"; //$NON-NLS-1$
    private static final String COMMENT_TEMPLATES_FILE = "commentTemplates.xml"; //$NON-NLS-1$
	static final String ELEMENT_COMMIT_COMMENT = "CommitComment"; //$NON-NLS-1$
	static final String ELEMENT_COMMIT_HISTORY = "CommitComments"; //$NON-NLS-1$
    static final String ELEMENT_COMMENT_TEMPLATES = "CommitCommentTemplates"; //$NON-NLS-1$

    public CommentsManager() {
		super();
		maxComments = SVNUIPlugin.getPlugin().getPreferenceStore().getInt(ISVNUIConstants.PREF_COMMENTS_TO_SAVE);
		SVNUIPlugin.getPlugin().getPreferenceStore().addPropertyChangeListener(this);
	}

	/**
     * Answer the list of comments that were previously used when committing.
     * @return String[]
     */
    public String[] getPreviousComments() {
    	if (maxComments == 0) {
    		previousComments = new String[0];
    	}
    	else if (previousComments.length > maxComments) {
	    	String[] previousCommentArray = new String[maxComments];
	    	for (int i = 0; i < maxComments; i++) {
	    		previousCommentArray[i] = previousComments[i];
	    	}
	    	previousComments = previousCommentArray;
    	}
        return previousComments;
    }

    /**
     * Method addComment.
     * @param string
     */
	public void addComment(String comment) {
		// Make comment first element if it's already there
		int index = getCommentIndex(comment);
		if (index != -1) {
			makeFirstElement(index);
			return;
		}
		if (containsCommentTemplate(comment))
			return;
		
		// Insert the comment as the first element
		String[] newComments = new String[Math.min(previousComments.length + 1, maxComments)];
		newComments[0] = comment;
		for (int i = 1; i < newComments.length; i++) {
			newComments[i] = previousComments[i-1];
		}
		previousComments = newComments;
	}
	
	private int getCommentIndex(String comment) {
		for (int i = 0; i < previousComments.length; i++) {
			if (previousComments[i].equals(comment)) {
				return i;
			}
		}
		return -1;
	}
	
	private void makeFirstElement(int index) {
		String[] newComments = new String[previousComments.length];
		newComments[0] = previousComments[index];
		System.arraycopy(previousComments, 0, newComments, 1, index);
		int maxIndex = previousComments.length - 1;
		if (index != maxIndex) {
			int nextIndex = (index + 1);
			System.arraycopy(previousComments, nextIndex, newComments,
					nextIndex, (maxIndex - index));
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
            SVNUIPlugin.log(new Status(IStatus.ERROR, SVNUIPlugin.ID, TeamException.UNABLE, Policy.bind("RepositoryManager.ioException"), e)); //$NON-NLS-1$
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
                     for (int i=0; i<previousComments.length && i<maxComments; i++)
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
                         throw new TeamException(new Status(IStatus.ERROR, SVNUIPlugin.ID, TeamException.UNABLE, Policy.bind("RepositoryManager.rename", tempFile.getAbsolutePath()), null)); //$NON-NLS-1$
                 }
         } catch (IOException e) {
                 throw new TeamException(new Status(IStatus.ERROR, SVNUIPlugin.ID, TeamException.UNABLE, Policy.bind("RepositoryManager.save",histFile.getAbsolutePath()), e)); //$NON-NLS-1$
         }
    }

    public void loadCommentTemplates() {
		IPath pluginStateLocation = SVNUIPlugin.getPlugin().getStateLocation();
		File histFile = pluginStateLocation.append(COMMENT_TEMPLATES_FILE).toFile();
        if (!histFile.exists()) return;
        try {
            BufferedInputStream is = new BufferedInputStream(new FileInputStream(histFile));
            try {
                readCommentTemplates(is);
            } finally {
                is.close();
            }
		} catch (IOException e) {
            SVNUIPlugin.log(new Status(IStatus.ERROR, SVNUIPlugin.ID, TeamException.UNABLE, Policy.bind("RepositoryManager.ioException"), e)); //$NON-NLS-1$
        } catch (TeamException e) {
            SVNUIPlugin.log(e.getStatus());
        }
    }
    
    private void readCommentTemplates(InputStream stream) throws IOException, TeamException {
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser parser = factory.newSAXParser();
			parser.parse(new InputSource(stream),
					new CommentTemplatesContentHandler());
		} catch (SAXException ex) {
			throw new SVNException(NLS.bind(
					Policy.bind("RepositoryManager.parsingProblem"),
					new String[] { COMMENT_TEMPLATES_FILE }), ex);
		} catch (ParserConfigurationException ex) {
			throw new SVNException(NLS.bind(
					Policy.bind("RepositoryManager.parsingProblem"),
					new String[] { COMMENT_TEMPLATES_FILE }), ex);
		}
	}
	
	public void saveCommentTemplates() throws TeamException {
		IPath pluginStateLocation = SVNUIPlugin.getPlugin().getStateLocation();
		File tempFile = pluginStateLocation.append(
				COMMENT_TEMPLATES_FILE + ".tmp").toFile(); //$NON-NLS-1$
		File histFile = pluginStateLocation.append(COMMENT_TEMPLATES_FILE)
				.toFile();
		try {
			XMLWriter writer = new XMLWriter(new BufferedOutputStream(
					new FileOutputStream(tempFile)));
			try {
				writeCommentTemplates(writer);
			} finally {
				writer.close();
			}
			if (histFile.exists()) {
				histFile.delete();
			}
			boolean renamed = tempFile.renameTo(histFile);
			if (!renamed) {
				throw new TeamException(new Status(IStatus.ERROR,
						SVNUIPlugin.ID, TeamException.UNABLE, NLS.bind(
								Policy.bind("RepositoryManager.rename"),
								new String[] { tempFile.getAbsolutePath() }),
						null));
			}
		} catch (IOException e) {
			throw new TeamException(new Status(IStatus.ERROR, SVNUIPlugin.ID,
					TeamException.UNABLE, NLS.bind(
							Policy.bind("RepositoryManager.save"),
							new String[] { histFile.getAbsolutePath() }), e));
		}
	}
	
	private void writeCommentTemplates(XMLWriter writer) {
		writer.startTag(ELEMENT_COMMENT_TEMPLATES, null, false);
		for (int i = 0; i < commentTemplates.length; i++)
			writer.printSimpleTag(ELEMENT_COMMIT_COMMENT, commentTemplates[i]);
		writer.endTag(ELEMENT_COMMENT_TEMPLATES);
	}
	
	private boolean containsCommentTemplate(String comment) {
		for (int i = 0; i < commentTemplates.length; i++) {
			if (commentTemplates[i].equals(comment)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Get list of comment templates.
	 */
	public String[] getCommentTemplates() {
		return commentTemplates;
	}
	
	public void replaceAndSaveCommentTemplates(String[] templates)
			throws TeamException {
		commentTemplates = templates;
		saveCommentTemplates();
	}

	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(ISVNUIConstants.PREF_COMMENTS_TO_SAVE)) {
			maxComments = SVNUIPlugin.getPlugin().getPreferenceStore().getInt(ISVNUIConstants.PREF_COMMENTS_TO_SAVE);
		}
	}
}
