/**
 * ***************************************************************************** Copyright (c) 2009
 * CollabNet. All rights reserved. This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * <p>Contributors: CollabNet - initial API and implementation
 * ****************************************************************************
 */
package com.collabnet.subversion.merge.wizards;

import com.collabnet.subversion.merge.Activator;
import com.collabnet.subversion.merge.Messages;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.ManagedForm;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.tigris.subversion.subclipse.core.ISVNCoreConstants;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.actions.CommitAction;
import org.tigris.subversion.subclipse.ui.actions.RevertAction;
import org.tigris.subversion.subclipse.ui.actions.SwitchAction;
import org.tigris.subversion.subclipse.ui.actions.UpdateAction;
import org.tigris.subversion.subclipse.ui.actions.UpdateDialogAction;
import org.tigris.subversion.subclipse.ui.util.ResourceSelectionTree;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNInfo;
import org.tigris.subversion.svnclientadapter.ISVNStatus;
import org.tigris.subversion.svnclientadapter.SVNStatusKind;
import org.tigris.subversion.svnclientadapter.utils.Depth;

@SuppressWarnings("unchecked")
public class MergeWizardBestPracticesPage extends MergeWizardWarningPage {
  private boolean localMods;
  private boolean switchedChildren;
  private boolean mixedRevisions;
  private boolean incompleteWorkingCopy;
  private List localModsList;
  private List switchedChildrenList;
  private List mixedRevisionsList;
  private List incompleteList;
  private FormToolkit toolkit;
  private ManagedForm mform;
  private ScrolledForm form;
  private ResourceSelectionTree updateResourceSelectionTree;
  private ResourceSelectionTree switchResourceSelectionTree;
  private Composite outerContainer;
  private boolean modsSectionExpanded;
  private boolean switchedChildrenSectionExpanded;
  private boolean mixedRevisionsSectionExpanded;
  private boolean completeSectionExpanded;

  private long highestRevision;

  private boolean needsChecks = true;

  public MergeWizardBestPracticesPage(String pageName, String title, ImageDescriptor titleImage) {
    super(pageName, title, titleImage);
  }

  public void createControl(Composite parent) {
    toolkit = new FormToolkit(parent.getDisplay());
    toolkit.setBackground(parent.getBackground());
    form = toolkit.createScrolledForm(parent);
    mform = new ManagedForm(toolkit, form);
    mform.getForm().setDelayedReflow(false);

    outerContainer = mform.getForm().getBody();
    TableWrapLayout layout = new TableWrapLayout();
    layout.numColumns = 1;
    outerContainer.setLayout(layout);

    refreshPage(false, false);

    setControl(form);
  }

  @Override
  public void setVisible(boolean visible) {
    if (visible && needsChecks) {
      performChecks(false);
    }
    super.setVisible(visible);
  }

  public void setNeedsChecks(boolean needsChecks) {
    this.needsChecks = needsChecks;
  }

  public void performChecks(boolean refreshPage) {
    try {
      getContainer()
          .run(
              true,
              true,
              new IRunnableWithProgress() {

                public void run(IProgressMonitor monitor)
                    throws InvocationTargetException, InterruptedException {

                  needsChecks = false;
                  localMods = false;
                  switchedChildren = false;
                  mixedRevisions = false;
                  incompleteWorkingCopy = false;
                  localModsList = new ArrayList();
                  switchedChildrenList = new ArrayList();
                  mixedRevisionsList = new ArrayList();
                  incompleteList = new ArrayList();
                  IResource[] resources = ((MergeWizard) getWizard()).getResources();

                  if (resources == null) return;

                  monitor.beginTask(Messages.MergeWizardBestPracticesPage_0, resources.length);

                  for (int i = 0; i < resources.length; i++) {
                    monitor.subTask(resources[i].getName());
                    ISVNLocalResource svnResource =
                        SVNWorkspaceRoot.getSVNResourceFor(resources[i]);
                    ISVNClientAdapter svnClient = null;
                    try {
                      svnClient = svnResource.getRepository().getSVNClient();

                      File fileProject = new File(resources[i].getLocation().toString());

                      List incompleteInfoList = new ArrayList();

                      ISVNInfo[] infos = svnClient.getInfo(fileProject, true);
                      for (int j = 0; j < infos.length; j++) {
                        if (infos[j].getDepth() != Depth.infinity
                            && infos[j].getDepth() != Depth.unknown) {
                          incompleteWorkingCopy = true;
                          incompleteInfoList.add(infos[j].getUrl().toString());
                        }
                      }
                      File file = new File(resources[i].getLocation().toString());
                      ISVNStatus status = svnClient.getSingleStatus(file);

                      highestRevision = status.getRevision().getNumber();

                      ISVNStatus[] statuses = svnClient.getStatus(file, true, true, false, true);
                      for (int j = 0; j < statuses.length; j++) {
                        if (statuses[j].getUrl() != null
                            && incompleteInfoList.contains(statuses[j].getUrl().toString())) {
                          incompleteList.add(
                              SVNWorkspaceRoot.getResourceFor(resources[i], statuses[j]));
                        }
                        if (statuses[j].getTextStatus().equals(SVNStatusKind.MODIFIED)
                            | statuses[j].getPropStatus().equals(SVNStatusKind.MODIFIED)) {
                          localMods = true;
                          localModsList.add(
                              SVNWorkspaceRoot.getResourceFor(resources[i], statuses[j]));
                        }
                        if (statuses[j].isSwitched()) {
                          switchedChildren = true;
                          switchedChildrenList.add(
                              SVNWorkspaceRoot.getResourceFor(resources[i], statuses[j]));
                        }
                        if (!statuses[j].getTextStatus().equals(SVNStatusKind.EXTERNAL)
                            && statuses[j].getRevision() != null) {
                          if (!statuses[j].getRevision().equals(status.getRevision())) {
                            mixedRevisions = true;
                            if (!mixedRevisionsList.contains(resources[i]))
                              mixedRevisionsList.add(resources[i]);
                            if (statuses[j].getRevision().getNumber() > highestRevision) {
                              highestRevision = statuses[j].getRevision().getNumber();
                            }
                          }
                        }
                      }
                    } catch (Exception e) {
                      Activator.handleError(e);
                    } finally {
                      svnResource.getRepository().returnSVNClient(svnClient);
                    }
                    monitor.worked(1);
                  }
                  monitor.done();
                }
              });
    } catch (Exception e) {
      Activator.handleError(e);
    }

    if (refreshPage) refreshPage(true, false);
    if (localMods || switchedChildren || mixedRevisions || incompleteWorkingCopy) {
      setMessage(Messages.MergeWizardBestPracticesPage_notReady);
      setPageComplete(false);
    } else {
      setMessage(Messages.MergeWizardBestPracticesPage_ready);
      setPageComplete(true);
    }
  }

  private void commit() {
    CommitAction commitAction = new CommitAction();
    commitAction.setSelectedResources(((MergeWizard) getWizard()).getResources());
    commitAction.setCanRunAsJob(false);
    commitAction.run(null);
    refreshPage(true, true);
  }

  private void revert() {
    RevertAction revertAction = new RevertAction();
    revertAction.setSelectedResources(((MergeWizard) getWizard()).getResources());
    revertAction.setCanRunAsJob(false);
    revertAction.run(null);
    refreshPage(true, true);
  }

  private void update(IResource[] resources, int depth, boolean setDepth, boolean showDialog) {
    UpdateAction updateAction = null;
    if (showDialog) {
      updateAction = new UpdateDialogAction();
      ((UpdateDialogAction) updateAction).setRevision(highestRevision);
    } else {
      updateAction = new UpdateAction();
    }
    updateAction.setSelectedResources(resources);
    updateAction.setDepth(depth);
    updateAction.setSetDepth(setDepth);
    updateAction.setCanRunAsJob(false);
    updateAction.run(null);
    refreshPage(true, true);
  }

  private void update(IResource[] resources, boolean showDialog) {
    int depth;
    boolean setDepth;
    if (incompleteWorkingCopy) {
      depth = ISVNCoreConstants.DEPTH_INFINITY;
      setDepth = true;
    } else {
      depth = ISVNCoreConstants.DEPTH_UNKNOWN;
      setDepth = false;
    }
    update(resources, depth, setDepth, showDialog);
  }

  private void switchChildren(IResource[] resources) {
    SwitchAction switchAction = new SwitchAction();
    switchAction.setSelectedResources(resources);
    switchAction.setCanRunAsJob(false);
    switchAction.run(null);
    refreshPage(true, true);
  }

  private void refreshPage(boolean needsRedraw, boolean usePreviousExpansionState) {
    if (needsRedraw) {
      Control[] children = outerContainer.getChildren();
      for (int i = 0; i < children.length; i++) children[i].dispose();
      if (needsChecks) performChecks(false);
    }

    Composite composite = toolkit.createComposite(outerContainer, SWT.NONE);
    GridLayout innerLayout = new GridLayout();
    innerLayout.numColumns = 2;
    composite.setLayout(innerLayout);
    TableWrapData td = new TableWrapData(TableWrapData.FILL);
    composite.setLayoutData(td);

    Label modsLabel = toolkit.createLabel(composite, ""); // $NON-NLS-1$
    if (localMods) {
      modsLabel.setImage(Activator.getImage(Activator.IMAGE_PROBLEM));
    } else {
      modsLabel.setImage(Activator.getImage(Activator.IMAGE_CHECK));
    }
    GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_BEGINNING);
    modsLabel.setLayoutData(gd);

    Section modsSection = toolkit.createSection(composite, Section.TWISTIE);
    gd = new GridData(GridData.FILL_BOTH);
    modsSection.setLayoutData(gd);

    modsSection.setText(Messages.MergeWizardBestPracticesPage_noUncommitted);
    Composite modsSectionClient = toolkit.createComposite(modsSection);
    GridLayout modsLayout = new GridLayout();
    modsLayout.numColumns = 1;
    modsSectionClient.setLayout(modsLayout);
    modsSection.setClient(modsSectionClient);
    modsSection.addExpansionListener(
        new ExpansionAdapter() {
          public void expansionStateChanged(ExpansionEvent e) {
            form.reflow(true);
            modsSectionExpanded = e.getState();
          }
        });

    Composite modsComposite = toolkit.createComposite(modsSectionClient);
    GridLayout modsCompositeLayout = new GridLayout();
    modsCompositeLayout.numColumns = 4;
    modsComposite.setLayout(modsCompositeLayout);

    if (localMods) {
      Label modsLabel1 =
          toolkit.createLabel(
              modsComposite, Messages.MergeWizardBestPracticesPage_noUncommitted2, SWT.NONE);
      gd = new GridData();
      gd.horizontalSpan = 4;
      modsLabel1.setLayoutData(gd);
      Hyperlink modsCommitLink =
          toolkit.createHyperlink(
              modsComposite, Messages.MergeWizardBestPracticesPage_commit, SWT.NONE);
      modsCommitLink.addHyperlinkListener(
          new HyperlinkAdapter() {
            public void linkActivated(HyperlinkEvent e) {
              needsChecks = true;
              commit();
            }
          });
      toolkit.createLabel(modsComposite, Messages.MergeWizardBestPracticesPage_or);
      Hyperlink modsRevertLink =
          toolkit.createHyperlink(
              modsComposite, Messages.MergeWizardBestPracticesPage_revert, SWT.NONE);
      modsRevertLink.addHyperlinkListener(
          new HyperlinkAdapter() {
            public void linkActivated(HyperlinkEvent e) {
              needsChecks = true;
              revert();
            }
          });
      toolkit.createLabel(modsComposite, Messages.MergeWizardBestPracticesPage_revert2);
    } else {
      Label modsLabel1 =
          toolkit.createLabel(
              modsComposite, Messages.MergeWizardBestPracticesPage_noLocalMods, SWT.NONE);
      gd = new GridData();
      gd.horizontalSpan = 4;
      modsLabel1.setLayoutData(gd);
    }

    Label mixedRevisionsLabel = toolkit.createLabel(composite, ""); // $NON-NLS-1$
    if (mixedRevisions) {
      mixedRevisionsLabel.setImage(Activator.getImage(Activator.IMAGE_PROBLEM));
    } else {
      mixedRevisionsLabel.setImage(Activator.getImage(Activator.IMAGE_CHECK));
    }
    gd = new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_BEGINNING);
    mixedRevisionsLabel.setLayoutData(gd);

    Section mixedRevisionsSection = toolkit.createSection(composite, Section.TWISTIE);
    gd = new GridData(GridData.FILL_BOTH);
    mixedRevisionsSection.setLayoutData(gd);

    mixedRevisionsSection.setText(Messages.MergeWizardBestPracticesPage_singleRevision);
    Composite mixedRevisionsSectionClient = toolkit.createComposite(mixedRevisionsSection);
    GridLayout mixedRevisionsLayout = new GridLayout();
    mixedRevisionsLayout.numColumns = 1;
    mixedRevisionsSectionClient.setLayout(mixedRevisionsLayout);
    mixedRevisionsSection.setClient(mixedRevisionsSectionClient);
    mixedRevisionsSection.addExpansionListener(
        new ExpansionAdapter() {
          public void expansionStateChanged(ExpansionEvent e) {
            form.reflow(true);
            mixedRevisionsSectionExpanded = e.getState();
          }
        });

    Composite mixedRevisionsComposite = toolkit.createComposite(mixedRevisionsSectionClient);
    GridLayout mixedRevisionsCompositeLayout = new GridLayout();
    mixedRevisionsCompositeLayout.numColumns = 2;
    mixedRevisionsComposite.setLayout(mixedRevisionsCompositeLayout);
    gd = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
    mixedRevisionsComposite.setLayoutData(gd);

    if (mixedRevisions) {
      Label mixedRevisionsLabel1 =
          toolkit.createLabel(
              mixedRevisionsComposite,
              Messages.MergeWizardBestPracticesPage_singleRevision2,
              SWT.NONE);
      gd = new GridData();
      gd.horizontalSpan = 2;
      mixedRevisionsLabel1.setLayoutData(gd);
      Hyperlink mixedRevisionsUpdateLink =
          toolkit.createHyperlink(
              mixedRevisionsComposite, Messages.MergeWizardBestPracticesPage_update, SWT.NONE);
      mixedRevisionsUpdateLink.addHyperlinkListener(
          new HyperlinkAdapter() {
            public void linkActivated(HyperlinkEvent e) {
              needsChecks = true;
              if (updateResourceSelectionTree == null) {
                IResource[] mixedArray = new IResource[mixedRevisionsList.size()];
                mixedRevisionsList.toArray(mixedArray);
                update(mixedArray, true);
              } else update(updateResourceSelectionTree.getSelectedResources(), true);
            }
          });
      toolkit.createLabel(mixedRevisionsComposite, Messages.MergeWizardBestPracticesPage_update2);
      if (((MergeWizard) getWizard()).getResources().length > 1) {
        IResource[] mixedArray = new IResource[mixedRevisionsList.size()];
        mixedRevisionsList.toArray(mixedArray);
        updateResourceSelectionTree =
            new ResourceSelectionTree(
                mixedRevisionsComposite,
                SWT.NONE,
                Messages.MergeWizardBestPracticesPage_itemsToUpdate,
                mixedArray,
                new HashMap(),
                null,
                false,
                null,
                null);
        gd = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        gd.horizontalSpan = 2;
        gd.heightHint = 100;
        updateResourceSelectionTree.setLayoutData(gd);
      } else updateResourceSelectionTree = null;
    } else {
      Label mixedRevisionsLabel1 =
          toolkit.createLabel(
              mixedRevisionsComposite,
              Messages.MergeWizardBestPracticesPage_atSingleRevision,
              SWT.NONE);
      gd = new GridData();
      gd.horizontalSpan = 2;
      mixedRevisionsLabel1.setLayoutData(gd);
    }

    Label switchedChildrenLabel = toolkit.createLabel(composite, ""); // $NON-NLS-1$
    if (switchedChildren) {
      switchedChildrenLabel.setImage(Activator.getImage(Activator.IMAGE_PROBLEM));
    } else {
      switchedChildrenLabel.setImage(Activator.getImage(Activator.IMAGE_CHECK));
    }
    gd = new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_BEGINNING);
    switchedChildrenLabel.setLayoutData(gd);

    Section switchedChildrenSection = toolkit.createSection(composite, Section.TWISTIE);
    gd = new GridData(GridData.FILL_BOTH);
    switchedChildrenSection.setLayoutData(gd);

    switchedChildrenSection.setText(Messages.MergeWizardBestPracticesPage_noSwitched);
    Composite switchedChildrenSectionClient = toolkit.createComposite(switchedChildrenSection);
    GridLayout switchedChildrenLayout = new GridLayout();
    switchedChildrenLayout.numColumns = 1;
    switchedChildrenSectionClient.setLayout(switchedChildrenLayout);
    switchedChildrenSection.setClient(switchedChildrenSectionClient);
    switchedChildrenSection.addExpansionListener(
        new ExpansionAdapter() {
          public void expansionStateChanged(ExpansionEvent e) {
            form.reflow(true);
            switchedChildrenSectionExpanded = e.getState();
          }
        });

    Composite switchedChildrenComposite = toolkit.createComposite(switchedChildrenSectionClient);
    GridLayout switchedChildrenCompositeLayout = new GridLayout();
    switchedChildrenCompositeLayout.numColumns = 2;
    switchedChildrenComposite.setLayout(switchedChildrenCompositeLayout);

    if (switchedChildren) {
      Label switchedChildrenDescriptionLabel1 =
          toolkit.createLabel(
              switchedChildrenComposite,
              Messages.MergeWizardBestPracticesPage_noSwitched2,
              SWT.NONE);
      gd = new GridData();
      gd.horizontalSpan = 2;
      switchedChildrenDescriptionLabel1.setLayoutData(gd);
      Hyperlink switchedChildrenUpdateLink =
          toolkit.createHyperlink(
              switchedChildrenComposite, Messages.MergeWizardBestPracticesPage_switch, SWT.NONE);
      switchedChildrenUpdateLink.addHyperlinkListener(
          new HyperlinkAdapter() {
            public void linkActivated(HyperlinkEvent e) {
              needsChecks = true;
              switchChildren(switchResourceSelectionTree.getSelectedResources());
            }
          });
      toolkit.createLabel(switchedChildrenComposite, Messages.MergeWizardBestPracticesPage_switch2);
      IResource[] switchedArray = new IResource[switchedChildrenList.size()];
      switchedChildrenList.toArray(switchedArray);
      switchResourceSelectionTree =
          new ResourceSelectionTree(
              switchedChildrenComposite,
              SWT.NONE,
              Messages.MergeWizardBestPracticesPage_itemsToSwitch,
              switchedArray,
              new HashMap(),
              null,
              false,
              null,
              null);
      gd = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
      gd.horizontalSpan = 2;
      gd.heightHint = 100;
      switchResourceSelectionTree.setLayoutData(gd);
    } else {
      Label switchedChildrenDescriptionLabel1 =
          toolkit.createLabel(
              switchedChildrenComposite,
              Messages.MergeWizardBestPracticesPage_hasNoSwitched,
              SWT.NONE);
      gd = new GridData();
      gd.horizontalSpan = 2;
      switchedChildrenDescriptionLabel1.setLayoutData(gd);
    }

    Label completeLabel = toolkit.createLabel(composite, ""); // $NON-NLS-1$
    gd = new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_BEGINNING);
    completeLabel.setLayoutData(gd);
    if (incompleteWorkingCopy) {
      completeLabel.setImage(Activator.getImage(Activator.IMAGE_PROBLEM));
    } else {
      completeLabel.setImage(Activator.getImage(Activator.IMAGE_CHECK));
    }

    Section completeSection = toolkit.createSection(composite, Section.TWISTIE);
    gd = new GridData(GridData.FILL_BOTH);
    completeSection.setLayoutData(gd);

    completeSection.setText(Messages.MergeWizardBestPracticesPage_completeWorkingCopy);
    Composite completeSectionClient = toolkit.createComposite(completeSection);
    GridLayout completeLayout = new GridLayout();
    completeLayout.numColumns = 1;
    completeSectionClient.setLayout(completeLayout);
    completeSection.setClient(completeSectionClient);
    completeSection.addExpansionListener(
        new ExpansionAdapter() {
          public void expansionStateChanged(ExpansionEvent e) {
            form.reflow(true);
            completeSectionExpanded = e.getState();
          }
        });

    Composite completeComposite = toolkit.createComposite(completeSectionClient);
    GridLayout completeCompositeLayout = new GridLayout();
    completeCompositeLayout.numColumns = 3;
    completeComposite.setLayout(completeCompositeLayout);

    Label completeDescriptionLabel1 =
        toolkit.createLabel(
            completeComposite,
            Messages.MergeWizardBestPracticesPage_completeWorkingCopy2,
            SWT.NONE);
    gd = new GridData();
    gd.horizontalSpan = 3;
    completeDescriptionLabel1.setLayoutData(gd);

    toolkit.createLabel(
        completeComposite, Messages.MergeWizardBestPracticesPage_completeWorkingCopy3, SWT.WRAP);
    Hyperlink completeUpdateLink =
        toolkit.createHyperlink(
            completeComposite, Messages.MergeWizardBestPracticesPage_update3, SWT.NONE);
    completeUpdateLink.addHyperlinkListener(
        new HyperlinkAdapter() {
          public void linkActivated(HyperlinkEvent e) {
            needsChecks = true;
            IResource[] incompleteArray = new IResource[incompleteList.size()];
            incompleteList.toArray(incompleteArray);
            update(incompleteArray, ISVNCoreConstants.DEPTH_INFINITY, true, true);
          }
        });
    toolkit.createLabel(completeComposite, Messages.MergeWizardBestPracticesPage_update4, SWT.WRAP);

    if (needsRedraw && usePreviousExpansionState) {
      if (modsSectionExpanded) modsSection.setExpanded(true);
      if (mixedRevisionsSectionExpanded) mixedRevisionsSection.setExpanded(true);
      if (switchedChildrenSectionExpanded) switchedChildrenSection.setExpanded(true);
      if (completeSectionExpanded) completeSection.setExpanded(true);
      outerContainer.layout(true);
      outerContainer.redraw();
    } else {
      if (localMods) {
        modsSection.setExpanded(true);
        modsSectionExpanded = true;
      }
      if (mixedRevisions) {
        mixedRevisionsSection.setExpanded(true);
        mixedRevisionsSectionExpanded = true;
      }
      if (switchedChildren) {
        switchedChildrenSection.setExpanded(true);
        switchedChildrenSectionExpanded = true;
      }
      if (incompleteWorkingCopy) {
        completeSection.setExpanded(true);
        completeSectionExpanded = true;
      }
    }
  }

  public boolean hasWarnings() {
    return localMods || mixedRevisions || switchedChildren || incompleteWorkingCopy;
  }

  public boolean needsChecks() {
    return needsChecks;
  }
}
