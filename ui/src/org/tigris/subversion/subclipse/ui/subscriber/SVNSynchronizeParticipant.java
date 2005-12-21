/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.tigris.subversion.subclipse.ui.subscriber;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.ISynchronizeModelElement;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipantDescriptor;
import org.eclipse.team.ui.synchronize.ISynchronizeScope;
import org.eclipse.team.ui.synchronize.SynchronizePageActionGroup;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PartInitException;
import org.tigris.subversion.subclipse.core.sync.SVNStatusSyncInfo;
import org.tigris.subversion.subclipse.core.sync.SVNWorkspaceSubscriber;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.internal.ScopableSubscriberParticipant;


/**
 * This is an example synchronize participant for the file system provider. It will allow
 * showing synchronization state for local resources mapped to a remote file system
 * location.
 * 
 * @since 3.0
 */
public class SVNSynchronizeParticipant extends ScopableSubscriberParticipant {
	
	/**
	 * The particpant ID as defined in the plugin manifest
	 */
	public static final String ID = "org.tigris.subversion.subclipse.participant"; //$NON-NLS-1$
	
	/**
	 * Contxt menu action group for synchronize view actions
	 */
	public static final String CONTEXT_MENU_CONTRIBUTION_GROUP_1 = "context_group_1"; //$NON-NLS-1$
	
	/**
	 * A custom label decorator that will show the remote mapped path for each
	 * file.
	 */
	private class SVNParticipantLabelDecorator extends LabelProvider implements ILabelDecorator {
		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ILabelDecorator#decorateImage(org.eclipse.swt.graphics.Image, java.lang.Object)
		 */
		public Image decorateImage(Image image, Object element) {
			return null;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ILabelDecorator#decorateText(java.lang.String, java.lang.Object)
		 */
		public String decorateText(String text, Object element) {
			try {
				if (element instanceof ISynchronizeModelElement) {
					IResource resource = ((ISynchronizeModelElement) element).getResource();
					if (resource != null) {
						SVNStatusSyncInfo info = (SVNStatusSyncInfo) SVNWorkspaceSubscriber.getInstance().getSyncInfo(resource);
						if (info != null)
						{
							return text + info.getLabel(); //$NON-NLS-1$ //$NON-NLS-2$
						}
					}
				}
			} catch (TeamException e) {
			}
			return null;
		}
	}
	
	/**
	 * Action group that contributes the get an put menus to the context menu 
	 * in the synchronize view
	 */
	private class SVNParticipantActionGroup extends SynchronizePageActionGroup {
		/* (non-Javadoc)
		 * @see org.eclipse.team.ui.synchronize.SynchronizePageActionGroup#initialize(org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration)
		 */
		public void initialize(ISynchronizePageConfiguration configuration) {
			super.initialize(configuration);
			CommitSynchronizeAction commitAction = new CommitSynchronizeAction(Policy.bind("SyncAction.commit"), configuration); //$NON-NLS-1$
			commitAction.setImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_COMMIT));
			appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, 
					CONTEXT_MENU_CONTRIBUTION_GROUP_1,
					commitAction);
			UpdateSynchronizeAction updateAction = new UpdateSynchronizeAction(Policy.bind("SyncAction.update"), configuration); //$NON-NLS-1$
			updateAction.setImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_UPDATE));
			appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, 
					CONTEXT_MENU_CONTRIBUTION_GROUP_1,
					updateAction);
			appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, 
					CONTEXT_MENU_CONTRIBUTION_GROUP_1,
					new Separator());
			ShowHistorySynchronizeAction historyAction = new ShowHistorySynchronizeAction(Policy.bind("SyncAction.history"), configuration); //$NON-NLS-1$
			historyAction.setImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_LOG));			
			appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, 
					CONTEXT_MENU_CONTRIBUTION_GROUP_1,
					historyAction);	
			ShowPropertiesSynchronizeAction propertiesAction = new ShowPropertiesSynchronizeAction(Policy.bind("SyncAction.properties"), configuration); //$NON-NLS-1$
			propertiesAction.setImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_PROPERTIES));			
			appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, 
					CONTEXT_MENU_CONTRIBUTION_GROUP_1,
					propertiesAction);				
			appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, 
					CONTEXT_MENU_CONTRIBUTION_GROUP_1,
					new Separator());	
			AddSynchronizeAction addAction = new AddSynchronizeAction(Policy.bind("SyncAction.add"), configuration); //$NON-NLS-1$
			addAction.setImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_ADD));
			appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, 
					CONTEXT_MENU_CONTRIBUTION_GROUP_1,
					addAction);				
			IgnoreSynchronizeAction ignoreAction = new IgnoreSynchronizeAction(Policy.bind("SyncAction.ignore"), configuration); //$NON-NLS-1$
			ignoreAction.setImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_IGNORE));			
			appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, 
					CONTEXT_MENU_CONTRIBUTION_GROUP_1,
					ignoreAction);	
			appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, 
					CONTEXT_MENU_CONTRIBUTION_GROUP_1,
					new Separator());				
			EditConflictsSynchronizeAction conflictsAction = new EditConflictsSynchronizeAction(Policy.bind("SyncAction.conflicts"), configuration); //$NON-NLS-1$				
			conflictsAction.setImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_CONFLICT));
			appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, 
					CONTEXT_MENU_CONTRIBUTION_GROUP_1,
					conflictsAction);
			RevertSynchronizeAction revertAction = new RevertSynchronizeAction(Policy.bind("SyncAction.revert"), configuration); //$NON-NLS-1$
			revertAction.setImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_REVERT));
			appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, 
					CONTEXT_MENU_CONTRIBUTION_GROUP_1,
					revertAction);
			ResolveSynchronizeAction resolveAction = new ResolveSynchronizeAction(Policy.bind("SyncAction.resolve"), configuration); //$NON-NLS-1$
			resolveAction.setImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_RESOLVE));
			appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, 
					CONTEXT_MENU_CONTRIBUTION_GROUP_1,
					resolveAction);
			MarkMergedSynchronizeAction markMergedAction = new MarkMergedSynchronizeAction(Policy.bind("SyncAction.markMerged"), configuration); //$NON-NLS-1$
			markMergedAction.setImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_MERGE));
			appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, 
					CONTEXT_MENU_CONTRIBUTION_GROUP_1,
					markMergedAction);	
			OverrideAndUpdateSynchronizeAction overrideAction = new OverrideAndUpdateSynchronizeAction(Policy.bind("SyncAction.override"), configuration); //$NON-NLS-1$
			appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU, 
					CONTEXT_MENU_CONTRIBUTION_GROUP_1,
					overrideAction);						
		}

	}
	
	/**
	 * No arg contructor used for
	 * creation of persisted participant after startup
	 */
	public SVNSynchronizeParticipant() {
	}

	public SVNSynchronizeParticipant(ISynchronizeScope scope) {
		super(scope);
		setSubscriber(SVNWorkspaceSubscriber.getInstance());
	}

	/**
	 * @see org.eclipse.team.ui.synchronize.ISynchronizeParticipant#init(org.eclipse.ui.IMemento)
	 */
	public void init(String secondaryId, IMemento memento) throws PartInitException {
		super.init(secondaryId, memento);
		setSubscriber(SVNWorkspaceSubscriber.getInstance());
	}

	 protected ISynchronizeParticipantDescriptor getDescriptor() {
        return TeamUI.getSynchronizeManager().getParticipantDescriptor(ID);
    }

    /* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.subscribers.SubscriberParticipant#initializeConfiguration(org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration)
	 */
	protected void initializeConfiguration(ISynchronizePageConfiguration configuration) {
		super.initializeConfiguration(configuration);
		
		ILabelDecorator labelDecorator = new SVNParticipantLabelDecorator();
		configuration.addLabelDecorator(labelDecorator);
		
		// Add support for showing mode buttons
		configuration.setSupportedModes(ISynchronizePageConfiguration.ALL_MODES);
		configuration.setMode(ISynchronizePageConfiguration.BOTH_MODE);
		
		// Create the action group that contributes the get and put actions
		configuration.addActionContribution(new SVNParticipantActionGroup());
		// Add the get and put group to the context menu
		configuration.addMenuGroup(
				ISynchronizePageConfiguration.P_CONTEXT_MENU, 
				CONTEXT_MENU_CONTRIBUTION_GROUP_1);
	}
}
