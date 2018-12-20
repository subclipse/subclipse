/**
 * ***************************************************************************** Copyright (c) 2009
 * CollabNet. All rights reserved. This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * <p>Contributors: CollabNet - initial API and implementation
 * ****************************************************************************
 */
package com.collabnet.subversion.merge.views;

import com.collabnet.subversion.merge.Activator;
import com.collabnet.subversion.merge.AdaptableMergeResult;
import com.collabnet.subversion.merge.AdaptableMergeResultsFolder;
import com.collabnet.subversion.merge.MergeOutput;
import com.collabnet.subversion.merge.MergeResult;
import com.collabnet.subversion.merge.MergeResultsFolder;
import com.collabnet.subversion.merge.Messages;
import com.collabnet.subversion.merge.actions.DeleteMergeOutputAction;
import com.collabnet.subversion.merge.actions.MergeEditConflictsAction;
import com.collabnet.subversion.merge.actions.OpenFileInSystemEditorAction;
import java.io.File;
import java.util.Iterator;
import org.eclipse.compare.CompareUI;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.actions.OpenWithMenu;
import org.eclipse.ui.part.ViewPart;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRemoteFile;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.resources.RemoteFile;
import org.tigris.subversion.subclipse.core.resources.SVNTreeConflict;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.actions.EditConflictsAction;
import org.tigris.subversion.subclipse.ui.compare.SVNLocalCompareInput;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNStatus;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNConflictDescriptor;
import org.tigris.subversion.svnclientadapter.SVNConflictVersion;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class MergeResultsView extends ViewPart {
  private static MergeResultsView view;
  private TreeViewer treeViewer;
  private MergeResultsViewLabelProvider labelProvider = new MergeResultsViewLabelProvider();
  private static ToggleLayoutAction[] toggleLayoutActions;
  private IAction toggleConflictsOnlyAction;
  private IPreferenceStore store = Activator.getDefault().getPreferenceStore();
  private RemoveAction removeAction = new RemoveAction();
  private RemoveAllAction removeAllAction = new RemoveAllAction();
  private SVNTreeConflict treeConflict;

  private OpenFileInSystemEditorAction openAction;

  public static final String ID =
      "com.collabnet.subversion.merge.views.MergeResultsView"; //$NON-NLS-1$
  public static final String LAYOUT_PREFERENCE = "MergeResultsView_layout";; // $NON-NLS-1$
  public static final String CONFLICTS_ONLY_PREFERENCE =
      "MergeResultsView_conflictsOnly";; //$NON-NLS-1$
  public static final int MODE_COMPRESSED_FOLDERS = 0;
  public static final int MODE_FLAT = 1;
  public static final int MODE_TREE = 2;

  public MergeResultsView() {
    super();
    view = this;
  }

  public void createPartControl(Composite parent) {
    GridLayout layout = new GridLayout();
    layout.numColumns = 1;
    layout.verticalSpacing = 2;
    layout.marginWidth = 0;
    layout.marginHeight = 2;
    parent.setLayout(layout);

    treeViewer = new TreeViewer(parent);
    treeViewer.setLabelProvider(labelProvider);
    treeViewer.setContentProvider(new MergeResultsViewContentProvider());
    treeViewer.setUseHashlookup(true);
    GridData layoutData = new GridData();
    layoutData.grabExcessHorizontalSpace = true;
    layoutData.grabExcessVerticalSpace = true;
    layoutData.horizontalAlignment = GridData.FILL;
    layoutData.verticalAlignment = GridData.FILL;
    treeViewer.getControl().setLayoutData(layoutData);
    treeViewer.setInput(this);
    treeViewer.addOpenListener(
        new IOpenListener() {
          public void open(OpenEvent event) {
            treeConflict = null;
            IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();
            Object selectedItem = selection.getFirstElement();
            MergeResult mergeResult = null;
            if (selectedItem instanceof AdaptableMergeResult)
              mergeResult = (MergeResult) selectedItem;
            if (selectedItem instanceof AdaptableMergeResultsFolder) {
              MergeResultsFolder mergeResultsFolder = (MergeResultsFolder) selectedItem;
              mergeResult = mergeResultsFolder.getMergeResult();
            }
            if (mergeResult != null) {
              if (mergeResult.getResource() instanceof IFile
                  && mergeResult.isConflicted()
                  && !mergeResult.isResolved()) {
                editConflicts(mergeResult);
                return;
              }
              if (mergeResult.getResource() instanceof IFile
                  && mergeResult.hasTreeConflict()
                  && !mergeResult.isTreeConflictResolved()) {
                boolean addAddConflict = false;
                if (mergeResult.getResource() != null && mergeResult.getResource().exists()) {
                  treeConflict = getTreeConflict(mergeResult.getResource());
                  if (treeConflict != null
                      && treeConflict.getDescription() != null
                      && treeConflict.getDescription().contains("local add")
                      && treeConflict
                          .getDescription()
                          .contains("incoming add")) { // $NON-NLS-1$ //$NON-NLS-2$
                    addAddConflict = true;
                  }
                  if (!addAddConflict) {
                    openAction.run();
                  }
                }
                if (!addAddConflict) {
                  return;
                }
              }
              if (!mergeResult.getAction().equals(MergeResult.ACTION_DELETE)) {
                final ISVNLocalResource localResource =
                    SVNWorkspaceRoot.getSVNResourceFor(mergeResult.getResource());
                if (!localResource.exists()) {
                  return;
                }
                BusyIndicator.showWhile(
                    Display.getCurrent(),
                    new Runnable() {
                      public void run() {
                        try {
                          if (treeConflict != null) {
                            if (!localResource.isFolder()) {
                              SVNConflictDescriptor descriptor =
                                  treeConflict.getConflictDescriptor();
                              SVNConflictVersion rightVersion = descriptor.getSrcRightVersion();
                              try {
                                ISVNRemoteFile remoteFile =
                                    new RemoteFile(
                                        localResource.getRepository(),
                                        new SVNUrl(
                                            rightVersion.getReposURL()
                                                + "/"
                                                + rightVersion.getPathInRepos()),
                                        new SVNRevision.Number(rightVersion.getPegRevision()));
                                SVNLocalCompareInput compareInput =
                                    new SVNLocalCompareInput(localResource, remoteFile);
                                CompareUI.openCompareEditorOnPage(
                                    compareInput, getSite().getPage());
                              } catch (Exception e) {
                              }
                            }
                            return;
                          }
                          CompareUI.openCompareEditorOnPage(
                              new SVNLocalCompareInput(localResource, SVNRevision.BASE),
                              getSite().getPage());
                        } catch (SVNException e) {
                          if (!e.operationInterrupted()) {
                            Activator.handleError(Messages.MergeResultsView_compareError, e);
                            MessageDialog.openError(
                                Display.getCurrent().getActiveShell(),
                                Messages.MergeResultsView_compareWithLatest,
                                e.getLocalizedMessage());
                          }
                        } catch (SVNClientException e) {
                          Activator.handleError(Messages.MergeResultsView_compareError, e);
                          MessageDialog.openError(
                              Display.getCurrent().getActiveShell(),
                              Messages.MergeResultsView_compareWithLatest,
                              e.getLocalizedMessage());
                        }
                      }
                    });
              }
            }
          }
        });
    treeViewer
        .getTree()
        .addSelectionListener(
            new SelectionAdapter() {
              public void widgetSelected(SelectionEvent e) {
                boolean mergeOutputSelected = false;
                IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();
                Iterator iter = selection.iterator();
                while (iter.hasNext()) {
                  if (iter.next() instanceof MergeOutput) {
                    mergeOutputSelected = true;
                    break;
                  }
                }
                removeAction.setEnabled(mergeOutputSelected);
              }
            });
    createMenus();
    createToolbar();
    getSite().setSelectionProvider(treeViewer);

    if (Activator.getDefault().getPreferenceStore().getBoolean(CONFLICTS_ONLY_PREFERENCE))
      setContentDescription(Messages.MergeResultsView_conflictsMode);
  }

  private void editConflicts(MergeResult mergeResult) {
    IFile resource = (IFile) mergeResult.getResource();
    ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
    try {
      File conflictNewFile = svnResource.getStatus().getConflictNew();
      File conflictOldFile = svnResource.getStatus().getConflictOld();
      File conflictWorkingFile = svnResource.getStatus().getConflictWorking();
      File mergedFile = new File(resource.getLocation().toString());
      if (conflictWorkingFile == null) {
        EditConflictsAction editConflictsAction = new EditConflictsAction(resource);
        editConflictsAction.run(null);
      } else {
        MergeEditConflictsAction editConflictsAction =
            new MergeEditConflictsAction(
                conflictNewFile,
                conflictOldFile,
                conflictWorkingFile,
                mergedFile,
                resource.getName(),
                null);
        editConflictsAction.setMergeResult(mergeResult);
        editConflictsAction.run(null);
      }
    } catch (Exception e) {
    }
  }

  private void createMenus() {
    openAction = new OpenFileInSystemEditorAction(getSite().getPage(), treeViewer);

    MenuManager menuMgr = new MenuManager("#MergeResultsViewPopupMenu"); // $NON-NLS-1$
    menuMgr.setRemoveAllWhenShown(true);
    menuMgr.addMenuListener(
        new IMenuListener() {
          public void menuAboutToShow(IMenuManager manager) {
            MergeResultsView.this.fillContextMenu(manager);
          }
        });
    Menu menu = menuMgr.createContextMenu(treeViewer.getControl());
    treeViewer.getControl().setMenu(menu);
    getSite().registerContextMenu(menuMgr, treeViewer);

    toggleLayoutActions =
        new ToggleLayoutAction[] {
          new ToggleLayoutAction(
              this, Messages.MergeResultsView_flat, Activator.IMAGE_LAYOUT_FLAT, MODE_FLAT),
          new ToggleLayoutAction(
              this,
              Messages.MergeResultsView_compressed,
              Activator.IMAGE_LAYOUT_COMPRESSED,
              MODE_COMPRESSED_FOLDERS)
        };
    IActionBars actionBars = getViewSite().getActionBars();
    IMenuManager actionBarsMenu = actionBars.getMenuManager();
    removeAction.setEnabled(false);
    actionBarsMenu.add(removeAction);
    actionBarsMenu.add(removeAllAction);
    actionBarsMenu.add(new Separator());
    actionBarsMenu.add(toggleLayoutActions[0]);
    actionBarsMenu.add(toggleLayoutActions[1]);
    actionBarsMenu.add(new Separator());

    toggleConflictsOnlyAction =
        new Action("Show conflicts only") { // $NON-NLS-1$
          public void run() {
            store.setValue(
                MergeResultsView.CONFLICTS_ONLY_PREFERENCE, toggleConflictsOnlyAction.isChecked());
            if (toggleConflictsOnlyAction.isChecked())
              setContentDescription(Messages.MergeResultsView_conflictsMode);
            else setContentDescription(""); // $NON-NLS-1$
            refresh();
          }
        };
    toggleConflictsOnlyAction.setChecked(
        store.getBoolean(MergeResultsView.CONFLICTS_ONLY_PREFERENCE));
    toggleConflictsOnlyAction.setImageDescriptor(
        Activator.getDefault().getImageDescriptor(Activator.IMAGE_CONFLICT));
    actionBarsMenu.add(toggleConflictsOnlyAction);
  }

  private void createToolbar() {
    IToolBarManager toolbarManager = getViewSite().getActionBars().getToolBarManager();
    Action refreshAction =
        new Action(
            Messages.MergeResultsView_refresh,
            Activator.getDefault().getImageDescriptor(Activator.IMAGE_REFRESH)) {
          public void run() {
            refresh();
          }
        };
    refreshAction.setToolTipText(Messages.MergeResultsView_refreshView);
    toolbarManager.add(refreshAction);
    toolbarManager.add(new Separator());

    PresentationAction presentationAction = new PresentationAction();
    toolbarManager.add(presentationAction);

    toolbarManager.add(new Separator());
    Action collapseAllAction =
        new Action(
            Messages.MergeResultsView_collapseAll,
            Activator.getDefault().getImageDescriptor(Activator.IMAGE_COLLAPSE_ALL)) {
          public void run() {
            collapseAll();
          }
        };
    collapseAllAction.setToolTipText(Messages.MergeResultsView_collapseAll);
    toolbarManager.add(collapseAllAction);
    Action expandAllAction =
        new Action(
            Messages.MergeResultsView_expandAll,
            Activator.getDefault().getImageDescriptor(Activator.IMAGE_EXPAND_ALL)) {
          public void run() {
            expandAll();
          }
        };
    expandAllAction.setToolTipText(Messages.MergeResultsView_expandAll);
    toolbarManager.add(expandAllAction);
    toolbarManager.add(new Separator());
    toolbarManager.add(toggleConflictsOnlyAction);
  }

  private void fillContextMenu(IMenuManager manager) {
    boolean enableOpen = false;
    IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();
    Iterator iter = selection.iterator();
    while (iter.hasNext()) {
      Object element = iter.next();
      if (element instanceof AdaptableMergeResult) {
        MergeResult mergeResult = (MergeResult) element;
        if (mergeResult.getResource() instanceof IFile && !mergeResult.isDelete()) {
          enableOpen = true;
        }
      }
      if (enableOpen) break;
    }
    if (enableOpen) {
      manager.add(openAction);
    }
    if (enableOpen && selection.size() == 1) {
      MenuManager submenu = new MenuManager(Messages.MergeResultsView_openWith);
      MergeResult mergeResult = (MergeResult) selection.getFirstElement();
      submenu.add(new OpenWithMenu(getSite().getPage(), mergeResult.getResource()));
      manager.add(submenu);
    }
    manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
  }

  private void collapseAll() {
    if (treeViewer == null) return;
    treeViewer.getControl().setRedraw(false);
    treeViewer.collapseToLevel(treeViewer.getInput(), TreeViewer.ALL_LEVELS);
    treeViewer.getControl().setRedraw(true);
  }

  private void expandAll() {
    if (treeViewer == null) return;
    treeViewer.getControl().setRedraw(false);
    treeViewer.expandAll();
    treeViewer.getControl().setRedraw(true);
  }

  public void setFocus() {
    if (treeViewer != null && !treeViewer.getControl().isDisposed()) {
      treeViewer.getControl().setFocus();
    }
  }

  public void dispose() {
    view = null;
    super.dispose();
  }

  public static MergeResultsView getView() {
    return view;
  }

  public void refresh() {
    if (treeViewer == null || treeViewer.getControl().isDisposed()) return;
    removeAction.setEnabled(false);
    Object[] expandedElements = treeViewer.getVisibleExpandedElements();
    treeViewer.refresh();
    if (expandedElements != null && expandedElements.length > 0)
      treeViewer.setExpandedElements(expandedElements);
  }

  public void refreshAsync(final Object expandedObject) {
    if (treeViewer == null || treeViewer.getControl().isDisposed()) return;
    treeViewer
        .getControl()
        .getDisplay()
        .asyncExec(
            new Runnable() {
              public void run() {
                MergeOutput.setInProgress(true);
                refresh();
                if (expandedObject != null) treeViewer.setExpandedState(expandedObject, true);
                MergeOutput.setInProgress(false);
              }
            });
  }

  public TreeViewer getTreeViewer() {
    return treeViewer;
  }

  private SVNTreeConflict getTreeConflict(final IResource resource) {
    treeConflict = null;
    BusyIndicator.showWhile(
        Display.getDefault(),
        new Runnable() {
          public void run() {
            ISVNClientAdapter client = null;
            try {
              client = SVNWorkspaceRoot.getSVNResourceFor(resource).getRepository().getSVNClient();
              ISVNStatus[] statuses =
                  client.getStatus(resource.getLocation().toFile(), true, true, true);
              for (int i = 0; i < statuses.length; i++) {
                if (statuses[i].hasTreeConflict()) {
                  treeConflict = new SVNTreeConflict(statuses[i]);
                  break;
                }
              }
            } catch (Exception e) {
              SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
            } finally {
              SVNWorkspaceRoot.getSVNResourceFor(resource).getRepository().returnSVNClient(client);
            }
          }
        });
    return treeConflict;
  }

  public static class RemoveAction extends Action {
    public RemoveAction() {
      super();
      setText(Messages.MergeResultsView_deleteSelected);
      setImageDescriptor(Activator.getDefault().getImageDescriptor(Activator.IMAGE_REMOVE));
    }

    public void run() {
      DeleteMergeOutputAction deleteAction = new DeleteMergeOutputAction();
      deleteAction.selectionChanged(this, view.getView().getTreeViewer().getSelection());
      deleteAction.run(this);
    }
  }

  public static class RemoveAllAction extends Action {
    public RemoveAllAction() {
      super();
      setText(Messages.MergeResultsView_deleteAll);
      setImageDescriptor(Activator.getDefault().getImageDescriptor(Activator.IMAGE_REMOVE_ALL));
    }

    public void run() {
      if (!MessageDialog.openQuestion(
          Display.getCurrent().getActiveShell(),
          Messages.MergeResultsView_deleteAll2,
          Messages.MergeResultsView_confirmDelete)) return;
      Item[] items = MergeResultsView.getView().getTreeViewer().getTree().getItems();
      for (int i = 0; i < items.length; i++) {
        if (items[i].getData() instanceof MergeOutput) {
          MergeOutput mergeOutput = (MergeOutput) items[i].getData();
          mergeOutput.delete();
        }
      }
      MergeResultsView.getView().refresh();
    }
  }

  public static class PresentationAction extends Action implements IMenuCreator {
    private Menu menu;

    public PresentationAction() {
      setText(Messages.MergeResultsView_presentation);
      setImageDescriptor(Activator.getDefault().getImageDescriptor(Activator.IMAGE_PRESENTATION));
      setMenuCreator(this);
    }

    public void dispose() {
      if (menu != null) {
        menu.dispose();
        menu = null;
      }
    }

    public Menu getMenu(Control parent) {
      if (menu != null) menu.dispose();
      menu = new Menu(parent);
      addActionToMenu(menu, toggleLayoutActions[0]);
      addActionToMenu(menu, toggleLayoutActions[1]);
      return menu;
    }

    public Menu getMenu(Menu parent) {
      return null;
    }

    private void addActionToMenu(Menu parent, Action action) {
      ActionContributionItem item = new ActionContributionItem(action);
      item.fill(parent, -1);
    }
  }

  public static class ToggleLayoutAction extends Action {
    private final int value;
    private MergeResultsView view;

    public ToggleLayoutAction(MergeResultsView view, String label, String icon, int value) {
      super(label, AS_RADIO_BUTTON);
      this.view = view;
      this.value = value;
      setImageDescriptor(Activator.getDefault().getImageDescriptor(icon));
      IPreferenceStore store = Activator.getDefault().getPreferenceStore();
      setChecked(value == store.getInt(LAYOUT_PREFERENCE));
    }

    public int getValue() {
      return this.value;
    }

    public void run() {
      if (isChecked()) {
        Activator.getDefault().getPreferenceStore().setValue(LAYOUT_PREFERENCE, value);
        view.refresh();
      }
    }
  }
}
