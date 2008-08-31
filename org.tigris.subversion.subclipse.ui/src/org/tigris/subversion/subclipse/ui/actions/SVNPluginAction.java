/*******************************************************************************
 * Copyright (c) 2008 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.actions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.team.core.TeamException;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.IPluginContribution;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;


/**
 * The action properties are created from the {@link IConfigurationElement} 
 * such as the text, style, icon, tooltip, id, and state.  
 * It also creates the {@link IActionDelegate} from the required "class" attribute. 
 * 
 * This action also passes selection changes to the delegate action.
 * 
 * This class is quite similar to the PluginAction class in the org.eclipse.ui.workbench
 * plugin, but didn't want to depend on that plugin.
 *  
 * @author Chris Callendar
 */
public class SVNPluginAction extends Action implements IPluginContribution, 
									ISelectionChangedListener, ISelectionListener {


	/** Class attribute. Value <code>class</code>. */
	public static final String ATT_CLASS = "class"; //$NON-NLS-1$

	/** Icon attribute. Value <code>icon</code>. */
	public static final String ATT_ICON = "icon"; //$NON-NLS-1$

	/** Id attribute. Value <code>id</code>. */
	public static final String ATT_ID = "id"; //$NON-NLS-1$

	/** Label attribute. Value <code>label</code>. */
	public static final String ATT_LABEL = "label"; //$NON-NLS-1$

	/** Action state attribute. Value <code>state</code>. */
	public static final String ATT_STATE = "state";	//$NON-NLS-1$

	/** Action style attribute. Value <code>style</code>. */
	public static final String ATT_STYLE = "style";	//$NON-NLS-1$
	
	/** Tooltip attribute. Value <code>tooltip</code>. */
	public static final String ATT_TOOLTIP = "tooltip";	//$NON-NLS-1$


	private IConfigurationElement element;
	private IActionDelegate delegate;
	private String pluginId;

	
	public SVNPluginAction(IConfigurationElement element) {
		super(element.getAttribute(ATT_LABEL), getStyleFromElement(element));
		this.element = element;
		pluginId = element.getContributor().getName();
		
		createDelegate();
		
		setId(element.getAttribute(ATT_ID));
		setToolTipText(element.getAttribute(ATT_TOOLTIP));
		
		if ((getStyle() == AS_RADIO_BUTTON) || (getStyle() == AS_CHECK_BOX)) {
			String bool = element.getAttribute(ATT_STATE);
			setChecked("true".equals(bool));	//$NON-NLS-1$
		}
		
		String iconPath = element.getAttribute(ATT_ICON);
		if ((iconPath != null) && (iconPath.length() > 0)) {
			ImageDescriptor desc = AbstractUIPlugin.imageDescriptorFromPlugin(pluginId, iconPath);
			if (desc != null) {
				setImageDescriptor(desc);
			}
		}
		
		 // Give delegate a chance to adjust enable state
        selectionChanged(StructuredSelection.EMPTY);
	}

	private static int getStyleFromElement(IConfigurationElement element) {
		String style = element.getAttribute(ATT_STYLE);
		if ("radio".equals(style)) {			//$NON-NLS-1$
			return AS_RADIO_BUTTON;
		} else if ("toggle".equals(style)) {	//$NON-NLS-1$
			return AS_CHECK_BOX;
		}
		return AS_PUSH_BUTTON;
	}

    public IActionDelegate getDelegate() {
    	return delegate;
    }

    /**
     * Creates the delegate and refreshes its enablement.
     */
    protected final void createDelegate() {
        // The runAttribute is null if delegate creation failed previously...
        if (delegate == null) {
            try {
                Object obj = element.createExecutableExtension(ATT_CLASS);
                if (obj instanceof IActionDelegate) {
                	delegate = (IActionDelegate) obj;
                }
            } catch (Throwable e) {
                if (e instanceof CoreException) {
                    SVNUIPlugin.log((CoreException)e);
                } else {
                    SVNUIPlugin.log(new Status(Status.ERROR, SVNUIPlugin.ID, TeamException.UNABLE, "Internal plug-in action delegate error on creation.", e)); //$NON-NLS-1$
                }
            }
        }
    }
    /* (non-Javadoc)
     * @see org.eclipse.ui.IPluginContribution#getLocalId()
     */
    public String getLocalId() {
        return getId();
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IPluginContribution#getPluginId()
     */
    public String getPluginId() {
        return pluginId;
    }
    
	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	public void run() {
		if (delegate != null) {
			delegate.run(this);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#runWithEvent(org.eclipse.swt.widgets.Event)
	 */
	public void runWithEvent(Event event) {
		if ((event != null) && (delegate instanceof IActionDelegate2)) {
			((IActionDelegate2)delegate).runWithEvent(this, event);
		} else {
			run();
		}
	}
	
	/**
     * Disposes this action.
     */
    public void dispose() {
        if (delegate instanceof IActionDelegate2) {
        	((IActionDelegate2)delegate).dispose();
        	delegate = null;
        }
    }

    /**
     * Handles selection change. 
     * @param selection the new selection
     */
    public void selectionChanged(ISelection selection) {
		if (selection == null) {
			selection = StructuredSelection.EMPTY;
		}
    	if (delegate != null) {
    		delegate.selectionChanged(this, selection);
    	}
    }
   
	/**
     * The <code>SelectionChangedEventAction</code> implementation of this 
     * <code>ISelectionChangedListener</code> method calls 
     * <code>selectionChanged(IStructuredSelection)</code> when the selection is
     * a structured one.
     */
    public void selectionChanged(SelectionChangedEvent event) {
        selectionChanged(event.getSelection());
    }

    /**
     * The <code>SelectionChangedEventAction</code> implementation of this 
     * <code>ISelectionListener</code> method calls 
     * <code>selectionChanged(IStructuredSelection)</code> when the selection is
     * a structured one. Subclasses may extend this method to react to the change.
     */
    public void selectionChanged(IWorkbenchPart part, ISelection sel) {
        selectionChanged(sel);
    }
    
}
