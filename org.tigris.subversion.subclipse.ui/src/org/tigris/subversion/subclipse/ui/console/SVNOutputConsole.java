/*******************************************************************************
 * Copyright (c) 2004, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.console;

import java.io.File;
import java.io.IOException;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.client.IConsoleListener;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.svnclientadapter.SVNNodeKind;


/**
 * Console that shows the output of SVN commands. It is shown as a page in the generic 
 * console view. It supports coloring for message, command, and error lines in addition
 * the font can be configured.
 * 
 * @since 3.0 
 */
public class SVNOutputConsole extends IOConsole implements IConsoleListener, IPropertyChangeListener {
	
	/** Constant used to define consoles that have the ability to output subversion information */
	public static final String SVN_CONSOLE_TYPE = "SVN";
	// created colors for each line type - must be disposed at shutdown
	private Color commandColor;
	private Color messageColor;
	private Color errorColor;
	
	// streams for each command type - each stream has its own color
	private IOConsoleOutputStream commandStream;
	private IOConsoleOutputStream messageStream;
	private IOConsoleOutputStream errorStream;
	
	// preferences for showing the SVN console when SVN output is provided
    private boolean showOnError;
	private boolean showOnMessage;

	private ConsoleDocument document;

	// Indicates whether the console is visible in the Console view
	private boolean visible = false;
	// Indicates whether the console's streams have been initialized
	private boolean initialized = false;

    /**
     * Used to notify this console of lifecycle methods <code>init()</code>
     * and <code>dispose()</code>.
     */
    class MyLifecycle implements org.eclipse.ui.console.IConsoleListener {
        public void consolesAdded(IConsole[] consoles) {
            for (int i = 0; i < consoles.length; i++) {
                IConsole console = consoles[i];
                if (console == SVNOutputConsole.this) {
                    init();
                }
            }

        }
        public void consolesRemoved(IConsole[] consoles) {
            for (int i = 0; i < consoles.length; i++) {
                IConsole console = consoles[i];
                if (console == SVNOutputConsole.this) {
                    ConsolePlugin.getDefault().getConsoleManager().removeConsoleListener(this);
                    dispose();
                }
            }
        }
    }

	/**
	 * Constructor initializes preferences and colors but doesn't create the console
	 * page yet.
	 */
	public SVNOutputConsole() {
		super("SVN", SVN_CONSOLE_TYPE, SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_SVN_CONSOLE)); //$NON-NLS-1$
		// setup console showing preferences
		showOnMessage = SVNUIPlugin.getPlugin().getPreferenceStore().getBoolean(ISVNUIConstants.PREF_CONSOLE_SHOW_ON_MESSAGE);
        showOnError = SVNUIPlugin.getPlugin().getPreferenceStore().getBoolean(ISVNUIConstants.PREF_CONSOLE_SHOW_ON_ERROR);  
		document = new ConsoleDocument();
		SVNProviderPlugin.getPlugin().setConsoleListener(SVNOutputConsole.this);
		SVNUIPlugin.getPlugin().getPreferenceStore().addPropertyChangeListener(SVNOutputConsole.this);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.console.AbstractConsole#init()
	 */
	protected void init() {
		// Called when console is added to the console view
		super.init();	
		
		initLimitOutput();
		
		//	Ensure that initialization occurs in the ui thread
		SVNUIPlugin.getStandardDisplay().asyncExec(new Runnable() {
			public void run() {
				JFaceResources.getFontRegistry().addListener(SVNOutputConsole.this);
				initializeStreams();
				dump();
			}
		});
	}
	
	private void initLimitOutput() {
		IPreferenceStore store = SVNUIPlugin.getPlugin().getPreferenceStore();
		if(store.getBoolean(ISVNUIConstants.PREF_CONSOLE_LIMIT_OUTPUT)) {
			setWaterMarks(1000, store.getInt(ISVNUIConstants.PREF_CONSOLE_HIGH_WATER_MARK));
		} else {
			setWaterMarks(-1, 0);
		}
	}
	
	/*
	 * Initialize thre streams of the console. Must be 
	 * called from the UI thread.
	 */
	private void initializeStreams() {
		synchronized(document) {
			if (!initialized) {
				commandStream = newOutputStream();
				errorStream = newOutputStream();
				messageStream = newOutputStream();
				// install colors
				commandColor = createColor(SVNUIPlugin.getStandardDisplay(), ISVNUIConstants.PREF_CONSOLE_COMMAND_COLOR);
				commandStream.setColor(commandColor);
				messageColor = createColor(SVNUIPlugin.getStandardDisplay(), ISVNUIConstants.PREF_CONSOLE_MESSAGE_COLOR);
				messageStream.setColor(messageColor);
				errorColor = createColor(SVNUIPlugin.getStandardDisplay(), ISVNUIConstants.PREF_CONSOLE_ERROR_COLOR);
				errorStream.setColor(errorColor);
				// install font
				setFont(JFaceResources.getFontRegistry().get(ISVNUIConstants.PREF_CONSOLE_FONT));
				initialized = true;
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.console.AbstractConsole#getHelpContextId()
	 */
	public String getHelpContextId() {
		return IHelpContextIds.CONSOLE_VIEW;
	}
    
	private void dump() {
		synchronized(document) {
			visible = true;
			ConsoleDocument.ConsoleLine[] lines = document.getLines();
			for (int i = 0; i < lines.length; i++) {
				ConsoleDocument.ConsoleLine line = lines[i];
				appendLine(line.type, line.line);
			}
			document.clear();
		}
	}
	
	private void appendLine(int type, String line) {
		synchronized(document) {
			if(visible) {
				try {
					switch(type) {
						case ConsoleDocument.COMMAND:
							commandStream.write(line);
							commandStream.write('\n');
							break;
						case ConsoleDocument.MESSAGE:
							messageStream.write("  " + line); //$NON-NLS-1$
							messageStream.write('\n');
							break;
						case ConsoleDocument.ERROR:
							errorStream.write("  " + line); //$NON-NLS-1$
							errorStream.write('\n');
							break;
					}
				} catch (IOException e) {
					SVNUIPlugin.log(0, e.getMessage(), e);
				}
			} else {
				document.appendConsoleLine(type, line);
			}
		}
	}
	
    private void bringConsoleToFront() {
        IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
        if(! visible) {
            manager.addConsoles(new IConsole[] {this});
        }
        manager.showConsoleView(this);
    }

	/* (non-Javadoc)
	 * @see org.eclipse.ui.console.MessageConsole#dispose()
	 */
	protected void dispose() {
		// Here we can't call super.dispose() because we actually want the partitioner to remain
		// connected, but we won't show lines until the console is added to the console manager
		// again.
		
		// Called when console is removed from the console view
		synchronized (document) {
			visible = false;
			JFaceResources.getFontRegistry().removeListener(this);
		}
	}
	
	/**
	 * Clean-up created fonts.
	 */
	public void shutdown() {
		// Call super dispose because we want the partitioner to be
		// disconnected.
		super.dispose();
		if (commandColor != null)
			commandColor.dispose();
		if (messageColor != null)
			messageColor.dispose();
		if (errorColor != null)
			errorColor.dispose();
		SVNUIPlugin.getPlugin().getPreferenceStore().removePropertyChangeListener(this);
	}

	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
		String property = event.getProperty();		
		// colors
		if (visible) {
			if (property.equals(ISVNUIConstants.PREF_CONSOLE_COMMAND_COLOR)) {
				Color newColor = createColor(SVNUIPlugin.getStandardDisplay(), ISVNUIConstants.PREF_CONSOLE_COMMAND_COLOR);
				commandStream.setColor(newColor);
				commandColor.dispose();
				commandColor = newColor;
			} else if (property.equals(ISVNUIConstants.PREF_CONSOLE_MESSAGE_COLOR)) {
				Color newColor = createColor(SVNUIPlugin.getStandardDisplay(), ISVNUIConstants.PREF_CONSOLE_MESSAGE_COLOR);
				messageStream.setColor(newColor);
				messageColor.dispose();
				messageColor = newColor;
			} else if (property.equals(ISVNUIConstants.PREF_CONSOLE_ERROR_COLOR)) {
				Color newColor = createColor(SVNUIPlugin.getStandardDisplay(), ISVNUIConstants.PREF_CONSOLE_ERROR_COLOR);
				errorStream.setColor(newColor);
				errorColor.dispose();
				errorColor = newColor;
				// font
			} else if (property.equals(ISVNUIConstants.PREF_CONSOLE_FONT)) {
				setFont(JFaceResources.getFontRegistry().get(ISVNUIConstants.PREF_CONSOLE_FONT));
			} else if(property.equals(ISVNUIConstants.PREF_CONSOLE_LIMIT_OUTPUT)) {
				initLimitOutput();
			}
		}
		// show preferences
		if(property.equals(ISVNUIConstants.PREF_CONSOLE_SHOW_ON_MESSAGE)) {
			Object value = event.getNewValue();
			if(value instanceof String) {
				showOnMessage = Boolean.getBoolean((String)event.getNewValue());
			} else {
				showOnMessage = ((Boolean)value).booleanValue();
			}
		}

        // Show on error
        if(property.equals(ISVNUIConstants.PREF_CONSOLE_SHOW_ON_ERROR)) {
            Object value = event.getNewValue();
            if(value instanceof String) {
                showOnError = Boolean.getBoolean((String)event.getNewValue());
            } else {
                showOnError = ((Boolean)value).booleanValue();
            }
        }
	}
	
	/**
	 * Returns a color instance based on data from a preference field.
	 */
	private Color createColor(Display display, String preference) {
		RGB rgb = PreferenceConverter.getColor(SVNUIPlugin.getPlugin().getPreferenceStore(), preference);
		return new Color(display, rgb);
	}

    public void logCommandLine(String commandLine) {
    	if (showOnMessage) {
    		bringConsoleToFront();
    	}
        appendLine(ConsoleDocument.DELIMITER, Policy.bind("Console.preExecutionDelimiter")); //$NON-NLS-1$
        appendLine(ConsoleDocument.COMMAND, commandLine);
    }
    public void logMessage(String message) {
    	if (showOnMessage) {
    		bringConsoleToFront();
    	}
        appendLine(ConsoleDocument.MESSAGE, "  " + message); //$NON-NLS-1$
    }
    public void logRevision(long revision, String path) {
    }
    public void logCompleted(String message) {
    	if (showOnMessage) {
    		bringConsoleToFront();
    	}
        appendLine(ConsoleDocument.MESSAGE, "  " + message); //$NON-NLS-1$
    }
    public void logError(String message) {
        if (showOnMessage || showOnError) {
        	bringConsoleToFront();
        }
        appendLine(ConsoleDocument.ERROR, "  " + message); //$NON-NLS-1$
    }
    public void onNotify(File path, SVNNodeKind kind) {
    }
    public void setCommand(int command) {
    }
}