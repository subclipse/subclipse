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

public class KeyFilesManager {
    // The previously remembered key file
    static String[] previousKeyFiles = new String[0];
    static final int MAX_FILES = 5;
    private static final String KEYFILE_HIST_FILE = "keyFileHistory.xml"; //$NON-NLS-1$
    static final String ELEMENT_KEYFILE = "KeyFile"; //$NON-NLS-1$
    static final String ELEMENT_KEYFILE_HISTORY = "KeyFiles"; //$NON-NLS-1$

    public String[] getPreviousKeyFiles() {
        return previousKeyFiles;
    }

    public void addKeyFile(String keyFile) {
        // Only add the key file if the first entry isn't the same already
        if (previousKeyFiles.length > 0 && previousKeyFiles[0].equals(keyFile)) return;
        // Insert the key file as the first element
        String[] newKeyFiles = new String[Math.min(previousKeyFiles.length + 1, MAX_FILES)];
        newKeyFiles[0] = keyFile;
        for (int i = 1; i < newKeyFiles.length; i++) {
            newKeyFiles[i] = previousKeyFiles[i-1];
        }
        previousKeyFiles = newKeyFiles;
    }

    public void loadKeyFileHistory() {
        IPath pluginStateLocation = SVNUIPlugin.getPlugin().getStateLocation().append(KEYFILE_HIST_FILE);
        File file = pluginStateLocation.toFile();
        if (!file.exists()) return;
        try {
            BufferedInputStream is = new BufferedInputStream(new FileInputStream(file));
            try {
            	SAXParserFactory factory = SAXParserFactory.newInstance();

                try {
                	SAXParser parser = factory.newSAXParser();                	
                    parser.parse(new InputSource(is), new KeyFileHistoryContentHandler());
                } catch (SAXException ex) {
                    throw new SVNException(Policy.bind("RepositoryManager.parsingProblem", KEYFILE_HIST_FILE), ex); //$NON-NLS-1$
                } catch (ParserConfigurationException e) {
                    throw new SVNException(Policy.bind("RepositoryManager.parsingProblem", KEYFILE_HIST_FILE), e); //$NON-NLS-1$				
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

    public void saveKeyFilesHistory() throws TeamException {
        IPath pluginStateLocation = SVNUIPlugin.getPlugin().getStateLocation();
        File tempFile = pluginStateLocation.append(KEYFILE_HIST_FILE + ".tmp").toFile(); //$NON-NLS-1$
        File histFile = pluginStateLocation.append(KEYFILE_HIST_FILE).toFile();
        try {
                 XMLWriter writer = new XMLWriter(new BufferedOutputStream(new FileOutputStream(tempFile)));
                 try {
                     writer.startTag(ELEMENT_KEYFILE_HISTORY, null, false);
                     for (int i=0; i<previousKeyFiles.length && i<MAX_FILES; i++)
                         writer.printSimpleTag(ELEMENT_KEYFILE, previousKeyFiles[i]);
                     writer.endTag(ELEMENT_KEYFILE_HISTORY);
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
