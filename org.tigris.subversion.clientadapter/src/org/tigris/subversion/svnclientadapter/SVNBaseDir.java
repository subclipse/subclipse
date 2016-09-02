/*******************************************************************************
 * Copyright (c) 2004, 2006 svnClientAdapter project and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     svnClientAdapter project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.svnclientadapter;

import java.io.File;
import java.io.IOException;

import org.tigris.subversion.svnclientadapter.utils.StringUtils;

/**
 * During notification (both with svn command line and javahl), the files and
 * directories are sometimes relative (with svn commit for ex). However it is
 * not relative to current directory but relative to the common parent of the
 * current directory and the working copy target
 * 
 * ex : if working copy is at /home/cedric/programmation/sources/test and
 * current dir is /home/cedric/projects/subversion/subclipse
 * 
 * $svn ci /home/cedric/programmation/sources/test/essai8 Adding
 * programmation/sources/test/essai8
 * 
 * @author Cedric Chabanois (cchab at tigris.org)
 * @author John M Flinchbaugh (john at hjsoft.com)
 */
public class SVNBaseDir {

    /**
     * get the common directory between file1 and file2 or null if the files
     * have nothing in common it always returns a directory unless file1 is the
     * same file than file2
     * 
     * @param file1
     * @param file2
     */
    protected static File getCommonPart(File file1, File file2) {
    		if (file1 == null)
    			return null;
    		if (file2 == null)
    			return null;
        String file1AbsPath;
        String file2AbsPath;
        file1AbsPath = file1.getAbsolutePath();
        file2AbsPath = file2.getAbsolutePath();

        if (file1AbsPath.equals(file2AbsPath)) {
            return new File(file1AbsPath);
        }

        String[] file1Parts = StringUtils.split(file1AbsPath,
                File.separatorChar);
        String[] file2Parts = StringUtils.split(file2AbsPath,
                File.separatorChar);
        if (file1Parts[0].equals(""))
        	file1Parts[0] = File.separator;
        if (file2Parts[0].equals(""))
        	file2Parts[0] = File.separator;

        int parts1Length = file1Parts.length;
        int parts2Length = file2Parts.length;

        int minLength = (parts1Length < parts2Length) ? parts1Length
                : parts2Length;

        String part1;
        String part2;
        StringBuffer commonsPart = new StringBuffer();
        for (int i = 0; i < minLength; i++) {
            part1 = file1Parts[i];
            part2 = file2Parts[i];
            if (!part1.equals(part2)) {
                break;
            }
			
            if (i > 0) {
                commonsPart.append(File.separatorChar);
            }
            
            commonsPart.append(part1);
        }
        
        if (commonsPart.length() == 0) {
            return null; // the two files have nothing in common (one on disk c:
                         // and the other on d: for ex)
        }
        
        return new File(commonsPart.toString());
    }

    /**
     * get the base directory for the given file
     * 
     * @param file
     * @return the base directory for the given file or null if there is no base
     */
    static public File getBaseDir(File file) {
        return getBaseDir(new File[] { file });
    }

    /**
     * get the base directory for a set of files or null if there is no base
     * directory for the set of files
     * 
     * @param files
     * @return the base directory for the given set of files or null if there is no base
     */
    static public File getBaseDir(File[] files) {
        File rootDir = getRootDir(files);

        // get the common part between current directory and other files
        File baseDir = getCommonPart(rootDir, new File("."));
        return baseDir;
    }

    /**
     * get the root directory for a set of files ie the ancestor of all given
     * files
     * 
     * @param files
     * @return @throws
     *         SVNClientException
     */
    static public File getRootDir(File[] files) {
    	if ((files == null) || (files.length == 0)) {
    		return null;
    	}
        File[] canonicalFiles = new File[files.length];
        for (int i = 0; i < files.length; i++) {
            canonicalFiles[i] = files[i].getAbsoluteFile();
        }

        // first get the common part between all files
        File commonPart = canonicalFiles[0];
        for (int i = 0; i < files.length; i++) {
            commonPart = getCommonPart(commonPart, canonicalFiles[i]);
            if (commonPart == null) {
                return null;
            }
        }
        if (commonPart.isFile()) {
            return commonPart.getParentFile();
        } else {
            return commonPart;
        }
    }

    /**
     * get path of file relative to rootDir
     * 
     * @param rootDir
     * @param file
     * @return path of file relative to rootDir
     * @throws SVNClientException
     */
    static public String getRelativePath(File rootDir, File file)
            throws SVNClientException {
        try {
            String rootPath = rootDir.getCanonicalPath();
            String filePath = file.getCanonicalPath();
            if (!filePath.startsWith(rootPath)) {
                return null;
            }
            return filePath.substring(rootPath.length());
        } catch (IOException e) {
            throw SVNClientException.wrapException(e);
        }
    }

}