/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * Copyright(c) 2003-2005 by the authors indicated in the @author tags.
 *
 * All Rights are Reserved by the various authors.
 *******************************************************************************/
package org.tigris.subversion.subclipse.ui.history;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.history.ILogEntry;
import org.tigris.subversion.subclipse.core.history.LogEntryChangePath;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.svnclientadapter.SVNUrl;

/**
 * Compressed folder representation of the affected paths panel.
 * 
 * @author Eugene Kuleshov
 */
class ChangePathsTreeViewer extends TreeViewer {
    ILogEntry currentLogEntry;
    Font currentPathFont;
        
    public ChangePathsTreeViewer(Composite parent) {
        super(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI /*| SWT.FULL_SELECTION*/);
        // tree.setHeaderVisible(true);
        // tree.setLinesVisible(true);
        GridData data = new GridData(GridData.FILL_BOTH);
        getControl().setLayoutData(data);
        getControl().addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e) {
                if(currentPathFont != null) {
                    currentPathFont.dispose();
                }
            }
        });
    
        setLabelProvider(new ChangePathLabelProvider());
    }
    
    protected void inputChanged(Object input, Object oldInput) {
        super.inputChanged( input, oldInput);
        this.currentLogEntry = (ILogEntry) input;
    }
    
    
    /**
     * The label provider.
     */
    class ChangePathLabelProvider extends LabelProvider implements IFontProvider {
        
        public String getText(Object element) {
            if(element instanceof LogEntryChangePath) {
                LogEntryChangePath changePath = (LogEntryChangePath) element;
                String path = changePath.getPath();
                int n = path.lastIndexOf('/');
                if(n>-1) path = path.substring(n+1);
                if (changePath.getCopySrcPath() == null) {
                    return path;
                }
                return path + " [" +  //$NON-NLS-1$
                  Policy.bind("ChangePathsTableProvider.copiedfrom",  //$NON-NLS-1$
                      changePath.getCopySrcPath(),
                      changePath.getCopySrcRevision().toString())+"]";
            } else if(element instanceof HistoryFolder) {
                HistoryFolder f = (HistoryFolder) element;
                if(f.getCopySrcPath()==null) {
                    return f.getPath() + (f.getChildCount()==0 ? "" : " ["+f.getChildCount()+"]");
                }
                return f.getPath() + (f.getChildCount()==0 ? " [" : " ["+f.getChildCount()+"] [") +
                  Policy.bind("ChangePathsTableProvider.copiedfrom",  //$NON-NLS-1$
                      f.getCopySrcPath(), 
                      f.getCopySrcRevision().toString())+"]";
            }
            return element.toString();
        }
        
        public Image getImage(Object element) {
            String id = null;
            if(element instanceof LogEntryChangePath) {
              LogEntryChangePath changePath = (LogEntryChangePath)element;
              switch(changePath.getAction()) {
                case 'A':  id = ISVNUIConstants.IMG_FILEADD_PENDING;  break;
                case 'D':  id = ISVNUIConstants.IMG_FILEDELETE_PENDING;  break;
                // case 'M':  
                default:   id = ISVNUIConstants.IMG_FILEMODIFIED_PENDING;  break;
              }

            } else if(element instanceof HistoryFolder) {
              HistoryFolder folder = (HistoryFolder) element;
              if(folder.getChildren().length==0) {
                switch(folder.getAction()) {
                  case 'A':  id = ISVNUIConstants.IMG_FILEADD_PENDING;  break;
                  case 'D':  id = ISVNUIConstants.IMG_FILEDELETE_PENDING;  break;
                  // case 'M':
                  default:   id = ISVNUIConstants.IMG_FILEMODIFIED_PENDING;  break;
                }
              } else {
                  switch(folder.getAction()) {
                    case 'A':  id = ISVNUIConstants.IMG_FOLDERADD_PENDING;  break;
                    case 'D':  id = ISVNUIConstants.IMG_FOLDERDELETE_PENDING;  break;
                    case 'M':  id = ISVNUIConstants.IMG_FOLDERMODIFIED_PENDING;  break;
                    default:   id = ISVNUIConstants.IMG_FOLDER;  break;
                  }
              }
            }
            if(id==null) return null;
            ImageDescriptor descriptor = SVNUIPlugin.getPlugin().getImageDescriptor(id);
            return descriptor==null ? null : descriptor.createImage();
        }
        
        /*
         * (non-Javadoc)
         * @see org.eclipse.jface.viewers.IFontProvider#getFont(java.lang.Object)
         */
        public Font getFont(Object element) {
            if(!(element instanceof LogEntryChangePath) || 
                element==null || currentLogEntry==null) {
              return null;
            }
          
            ISVNRemoteResource remoteResource = currentLogEntry.getRemoteResource();
            if (remoteResource == null) {
                return null;
            }
            
            SVNUrl currentUrl = remoteResource.getUrl();
            if (currentUrl == null) {
                return null;
            }
            
            SVNUrl url = ((LogEntryChangePath)element).getUrl();
            if (currentUrl.equals(url)) {
                if (currentPathFont == null) {
                    Font defaultFont = JFaceResources.getDefaultFont();
                    FontData[] data = defaultFont.getFontData();
                    for (int i = 0; i < data.length; i++) {
                        data[i].setStyle(SWT.BOLD);
                    }               
                    currentPathFont = new Font(getControl().getDisplay(), data);
                }
                return currentPathFont;
            }
            return null;
        }
        
    }
    
}
