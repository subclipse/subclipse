/*******************************************************************************
 * Copyright (c) 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.console;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.IHyperlink;
import org.eclipse.ui.console.IPatternMatchListenerDelegate;
import org.eclipse.ui.console.PatternMatchEvent;
import org.eclipse.ui.console.TextConsole;
import org.eclipse.ui.part.FileEditorInput;

public class PathMatcher implements IPatternMatchListenerDelegate {

	private static Pattern[] PATTERNS = new Pattern[] {
		Pattern.compile(".*\\s+[SAUCDGMRE]\\s+([^\r\n]+)"),
		Pattern.compile(".*\\s+Sending\\s+([^\r\n]+)"),
		Pattern.compile(".*\\s+Adding\\s+([^\r\n]+)"),
		Pattern.compile(".*\\s+Deleting\\s+([^\r\n]+)"),
		Pattern.compile(".*\\s+Replacing\\s+([^\r\n]+)"),
		Pattern.compile(".*\\s+Reverted\\s+([^\r\n]+)"),
	};

	/**
	 * The console associated with this line tracker 
	 */
	private TextConsole fConsole;

    /* (non-Javadoc)
     * @see org.eclipse.ui.console.IPatternMatchListenerDelegate#connect(org.eclipse.ui.console.IConsole)
     */
    public void connect(TextConsole console) {
	    fConsole = console;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.console.IPatternMatchListenerDelegate#disconnect()
     */
    public void disconnect() {
        fConsole = null;
    }
    
	public void matchFound(PatternMatchEvent event) {
		if (fConsole != null) {
			// all lines will be matched.
			// select those:
			// [AUCDGMR][wsp](path)[newline or end of input]
			// \s+[AUCDGMR]\s+([a-zA-Z]:/[^\r\n]+)
			// (Sending)(Replacing)(Deleting)(Adding)(Reverted)[wsp](path)[newline or end of input]
			int offset = event.getOffset();
			int length = event.getLength();
			String path;
    	    try {
				path = fConsole.getDocument().get(offset, length);
				if (path == null) {
					return;
				}
			} catch (BadLocationException e) {
				return;
			}
    	    int start = 0;
			for (int i = 0; i < PATTERNS.length; i++) {
        	    Pattern pattern = PATTERNS[i];
        	    Matcher matcher = pattern.matcher(path);
        	    while(matcher.find(start)) {
    				length = matcher.end(1) - matcher.start(1);
    				String link = path.substring(matcher.start(1), matcher.end(1));
    				if (link != null) {
    					try {
							fConsole.addHyperlink(new FileHyperlink(link), offset + matcher.start(1), length);							
						} catch (BadLocationException e) {
						}
    					return;
    				}
    				start = matcher.end();
        	    }
			}
		}
	}
	
	private static class FileHyperlink implements IHyperlink {
		
		private String myPath;

		public FileHyperlink(String path) {
			myPath = path;
		}

		public void linkEntered() {
		}

		public void linkExited() {
		}

		public void linkActivated() {
			IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			if (window == null || window.getActivePage() == null) {
				return;
			}
			IWorkbenchPage page = window.getActivePage();
			if (ResourcesPlugin.getWorkspace().getRoot() == null) {
				return;
			}
			
			IPath path = Path.fromOSString(myPath);
			// JavaHL prints out the path relative to the current device when committing
			if (!path.isAbsolute()) {
				path = path.makeAbsolute().setDevice(ResourcesPlugin.getWorkspace().getRoot().getLocation().getDevice());
			}
			
			IFile file = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(path);
			if (file == null || !file.exists()) {
				if (path != null && !path.isAbsolute() && path.segmentCount() > 1) {
					path = path.removeFirstSegments(1);
					file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
				} 
			}
			if (file == null || !file.exists()) {
				return;
			}
			IEditorRegistry registry = PlatformUI.getWorkbench().getEditorRegistry();
			if (registry == null) {
				return;
			}
			IEditorDescriptor descriptor = null;
			try {
				IContentDescription contentDescription = file.getContentDescription();
				descriptor = registry.getDefaultEditor(path.lastSegment(), contentDescription != null ? contentDescription.getContentType(): null);
			} catch (CoreException e) {
			}
			
			if (descriptor == null) {
				descriptor = registry.findEditor(IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID);
			}
			
			if (descriptor == null) {
				return;
			}
			IEditorInput input = new FileEditorInput(file);
			try {
				page.openEditor(input, descriptor.getId());
			} catch (PartInitException e) {
			}
		}
		
	}
}

