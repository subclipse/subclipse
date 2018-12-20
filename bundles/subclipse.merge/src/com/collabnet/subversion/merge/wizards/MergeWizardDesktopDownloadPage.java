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
import java.net.URL;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;

public class MergeWizardDesktopDownloadPage extends WizardPage {
  private static final String TEAM_FORGE_DOWNLOAD_URL =
      "http://www.collab.net/downloads/teamforge"; //$NON-NLS-1$
  private static final String TEAM_FORGE_LEARN_MORE_URL =
      "http://www.collab.net/products/teamforge"; //$NON-NLS-1$
  private static final String DESKTOP_DOWNLOAD_URL =
      "http://www.collab.net/downloads/integrations"; //$NON-NLS-1$
  private static final String DESKTOP_LEARN_MORE_URL =
      "http://www.collab.net/products/integrations/desktops/ecl"; //$NON-NLS-1$
  private static final String CLOUDFORGE_TRIAL_URL =
      "https://app.cloudforge.com/trial_signup/new?source=teamforge"; //$NON-NLS-1$
  private static final String CLOUDFORGE_LEARN_MORE_URL =
      "http://www.cloudforge.com/"; //$NON-NLS-1$

  public MergeWizardDesktopDownloadPage(String pageName, String title, ImageDescriptor titleImage) {
    super(pageName, title, titleImage);
  }

  public void createControl(Composite parent) {
    Composite outerContainer = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.numColumns = 1;
    outerContainer.setLayout(layout);
    outerContainer.setLayoutData(new GridData(GridData.FILL_BOTH));

    Label paragraph1Label = new Label(outerContainer, SWT.WRAP);
    GridData data =
        new GridData(
            GridData.GRAB_HORIZONTAL
                | GridData.HORIZONTAL_ALIGN_FILL
                | GridData.VERTICAL_ALIGN_BEGINNING);
    data.widthHint = 500;
    paragraph1Label.setLayoutData(data);
    paragraph1Label.setText(Messages.MergeWizardDesktopDownloadPage_4);

    new Label(outerContainer, SWT.NONE);

    Label teamForgeLogo = new Label(outerContainer, SWT.NONE);
    teamForgeLogo.setImage(Activator.getImage(Activator.IMAGE_TEAMFORGE_LOGO));
    data = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
    teamForgeLogo.setLayoutData(data);

    Group teamForgeGroup = new Group(outerContainer, SWT.NONE);
    GridLayout teamForgeLayout = new GridLayout();
    teamForgeLayout.numColumns = 2;
    teamForgeGroup.setLayout(teamForgeLayout);
    teamForgeGroup.setLayoutData(
        new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));

    Label teamForgeLabel = new Label(teamForgeGroup, SWT.WRAP);
    data =
        new GridData(
            GridData.GRAB_HORIZONTAL
                | GridData.HORIZONTAL_ALIGN_FILL
                | GridData.GRAB_VERTICAL
                | GridData.VERTICAL_ALIGN_FILL);
    data.widthHint = 500;
    data.horizontalSpan = 2;
    teamForgeLabel.setLayoutData(data);
    teamForgeLabel.setText(Messages.MergeWizardDesktopDownloadPage_5);

    FormToolkit toolkit = new FormToolkit(teamForgeGroup.getDisplay());
    toolkit.setBackground(teamForgeGroup.getBackground());

    Label fillerLabel1 = new Label(teamForgeGroup, SWT.NONE);
    data = new GridData();
    data.horizontalSpan = 2;
    fillerLabel1.setLayoutData(data);

    IHyperlinkListener linkListener =
        new HyperlinkAdapter() {
          public void linkActivated(HyperlinkEvent evt) {
            Hyperlink link = (Hyperlink) evt.getSource();
            try {
              URL url = new URL(link.getHref().toString());
              PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(url);
            } catch (Exception e) {
              Activator.handleError(Messages.MergeWizardDesktopDownloadPage_6, e);
              MessageDialog.openError(getShell(), "Merge Wizard", e.getMessage()); // $NON-NLS-1$
            }
          }
        };

    Hyperlink teamForgeDownloadLink =
        toolkit.createHyperlink(
            teamForgeGroup, Messages.MergeWizardDesktopDownloadPage_7, SWT.NONE);
    teamForgeDownloadLink.setHref(TEAM_FORGE_DOWNLOAD_URL);
    teamForgeDownloadLink.setToolTipText(TEAM_FORGE_DOWNLOAD_URL);
    teamForgeDownloadLink.addHyperlinkListener(linkListener);

    Hyperlink teamForgeLearnMoreLink =
        toolkit.createHyperlink(
            teamForgeGroup, Messages.MergeWizardDesktopDownloadPage_8, SWT.NONE);
    teamForgeLearnMoreLink.setHref(TEAM_FORGE_LEARN_MORE_URL);
    teamForgeLearnMoreLink.setToolTipText(TEAM_FORGE_LEARN_MORE_URL);
    teamForgeLearnMoreLink.addHyperlinkListener(linkListener);

    Label fillerLabel2 = new Label(teamForgeGroup, SWT.NONE);
    data = new GridData();
    data.horizontalSpan = 2;
    fillerLabel2.setLayoutData(data);

    Label cloudForgeLabel = new Label(teamForgeGroup, SWT.WRAP);
    data =
        new GridData(
            GridData.GRAB_HORIZONTAL
                | GridData.HORIZONTAL_ALIGN_FILL
                | GridData.GRAB_VERTICAL
                | GridData.VERTICAL_ALIGN_FILL);
    data.widthHint = 500;
    data.horizontalSpan = 2;
    cloudForgeLabel.setLayoutData(data);
    cloudForgeLabel.setText(Messages.MergeWizardDesktopDownloadPage_15);

    Label fillerLabel3 = new Label(teamForgeGroup, SWT.NONE);
    data = new GridData();
    data.horizontalSpan = 2;
    fillerLabel3.setLayoutData(data);

    Hyperlink cloudForgeTrial =
        toolkit.createHyperlink(
            teamForgeGroup, Messages.MergeWizardDesktopDownloadPage_9, SWT.NONE);
    cloudForgeTrial.setHref(CLOUDFORGE_TRIAL_URL);
    cloudForgeTrial.setToolTipText(CLOUDFORGE_TRIAL_URL);
    cloudForgeTrial.addHyperlinkListener(linkListener);

    Hyperlink cloudForgeLearnMoreLink =
        toolkit.createHyperlink(
            teamForgeGroup, Messages.MergeWizardDesktopDownloadPage_8, SWT.NONE);
    cloudForgeLearnMoreLink.setHref(CLOUDFORGE_LEARN_MORE_URL);
    cloudForgeLearnMoreLink.setToolTipText(CLOUDFORGE_LEARN_MORE_URL);
    cloudForgeLearnMoreLink.addHyperlinkListener(linkListener);

    Label fillerLabel4 = new Label(teamForgeGroup, SWT.NONE);
    data = new GridData();
    data.horizontalSpan = 2;
    fillerLabel4.setLayoutData(data);

    Label desktopLabel = new Label(teamForgeGroup, SWT.WRAP);
    data =
        new GridData(
            GridData.GRAB_HORIZONTAL
                | GridData.HORIZONTAL_ALIGN_FILL
                | GridData.GRAB_VERTICAL
                | GridData.VERTICAL_ALIGN_FILL);
    data.widthHint = 500;
    data.horizontalSpan = 2;
    desktopLabel.setLayoutData(data);
    desktopLabel.setText(Messages.MergeWizardDesktopDownloadPage_10);

    Label fillerLabel5 = new Label(teamForgeGroup, SWT.NONE);
    data = new GridData();
    data.horizontalSpan = 2;
    fillerLabel5.setLayoutData(data);

    Hyperlink desktopDownloadLink =
        toolkit.createHyperlink(
            teamForgeGroup, Messages.MergeWizardDesktopDownloadPage_11, SWT.NONE);
    desktopDownloadLink.setHref(DESKTOP_DOWNLOAD_URL);
    desktopDownloadLink.setToolTipText(DESKTOP_DOWNLOAD_URL);
    desktopDownloadLink.addHyperlinkListener(linkListener);

    Hyperlink desktopLearnMoreLink =
        toolkit.createHyperlink(
            teamForgeGroup, Messages.MergeWizardDesktopDownloadPage_12, SWT.NONE);
    desktopLearnMoreLink.setHref(DESKTOP_LEARN_MORE_URL);
    desktopLearnMoreLink.setToolTipText(DESKTOP_LEARN_MORE_URL);
    desktopLearnMoreLink.addHyperlinkListener(linkListener);

    setPageComplete(false);

    setMessage(Messages.MergeWizardDesktopDownloadPage_13);

    setControl(outerContainer);
  }
}
