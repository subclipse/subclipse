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
package org.tigris.subversion.subclipse.ui.properties;

import java.net.MalformedURLException;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.repo.SVNRepositories;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.decorator.SVNLightweightDecorator;
import org.tigris.subversion.subclipse.ui.dialogs.ChooseRootUrlDialog;
import org.tigris.subversion.svnclientadapter.SVNUrl;

/**
 * Property page to modify settings for a given repository
 */
public class SVNRepositoryPropertiesPage extends PropertyPage {
    private ISVNRepositoryLocation location;
    private static final String FAKE_PASSWORD = "*********"; //$NON-NLS-1$
    private Text loginText;
    private Text passwordText;
    private Text customLabelText;
    private Button useUrlLabelButton;
    private Button useCustomLabelButton;
    private boolean passwordChanged;
    private Text repositoryRootText;
    private Text repositoryUrlText;
    private boolean showCredentials;

    /** Client adapter implementation identifier */
    private static final String COMMANDLINE_CLIENT = "commandline"; //$NON-NLS-1$
    
	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createContents(Composite parent) {
		initialize();
        
        GridLayout layout;
        Label label;
        GridData data;
        
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));
        layout = new GridLayout();
		layout.numColumns = 1;
		composite.setLayout(layout);
        
        Listener labelListener = new Listener() {
            public void handleEvent(Event event) {
                updateWidgetEnablements();
            }
        };
        
        // group for label
		Composite labelGroup = new Composite(composite, SWT.NONE);
        labelGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		layout = new GridLayout();
		layout.numColumns = 2;
		labelGroup.setLayout(layout);		

        // use url as label
		useUrlLabelButton = new Button(labelGroup, SWT.RADIO);
		useUrlLabelButton.setText(Policy.bind("SVNRepositoryPropertiesPage.useRepositoryUrlAsLabel")); //$NON-NLS-1$
        useUrlLabelButton.addListener(SWT.Selection,labelListener);
		data = new GridData();
		data.horizontalSpan = 2;
		useUrlLabelButton.setLayoutData(data);		

        // use custom label
		useCustomLabelButton = new Button(labelGroup, SWT.RADIO);
		useCustomLabelButton.setText(Policy.bind("SVNRepositoryPropertiesPage.useCustomLabel")); //$NON-NLS-1$
        useCustomLabelButton.addListener(SWT.Selection,labelListener);
		data = new GridData();
		useCustomLabelButton.setLayoutData(data);
		customLabelText = new Text(labelGroup, SWT.SINGLE | SWT.BORDER);
        customLabelText.addListener(SWT.Modify, labelListener);
		 		 data = new GridData(GridData.FILL_HORIZONTAL);
        // data.widthHint = 200;
        customLabelText.setLayoutData(data);
        
        // empty label to separate
        label = new Label(composite, SWT.NONE);
        
		FocusListener focusListener = new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				((Text)e.getSource()).selectAll();
			}
			public void focusLost(FocusEvent e) {
				((Text)e.getSource()).setText(((Text)e.getSource()).getText());
			}					
		};
		customLabelText.addFocusListener(focusListener);
        
        if (showCredentials) {
	        // group for login and password
	        Composite userPasswordGroup = new Composite(composite, SWT.NONE);
	        userPasswordGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	        layout = new GridLayout();
	        layout.numColumns = 2;
	        userPasswordGroup.setLayout(layout);
	        
	        // login
	        label = new Label(userPasswordGroup, SWT.NONE);
	        label.setText(Policy.bind("SVNRepositoryPropertiesPage.login")); //$NON-NLS-1$
	        loginText = new Text(userPasswordGroup, SWT.SINGLE | SWT.BORDER);
	        data = new GridData(GridData.FILL_HORIZONTAL);
	        data.grabExcessHorizontalSpace = true;
	        loginText.setLayoutData(data);
	        loginText.addFocusListener(focusListener);
	
	        // password
	        label = new Label(userPasswordGroup, SWT.NONE);
	        label.setText(Policy.bind("SVNRepositoryPropertiesPage.password")); //$NON-NLS-1$
	        passwordText = new Text(userPasswordGroup, SWT.SINGLE | SWT.BORDER| SWT.PASSWORD);
	        data = new GridData(GridData.FILL_HORIZONTAL);
	        data.grabExcessHorizontalSpace = true;
	        passwordText.setLayoutData(data);        
	        passwordText.addListener(SWT.Modify, new Listener() {
	            public void handleEvent(Event event) {
	                passwordChanged = !passwordText.getText().equals(FAKE_PASSWORD);
	            }
	        });
	        passwordText.addFocusListener(focusListener);
        }
        
        // empty label to separate
        label = new Label(composite, SWT.NONE);

        // group for repository root
        Composite repositoryRootGroup = new Composite(composite, SWT.NONE);
        repositoryRootGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        layout = new GridLayout();
        layout.numColumns = 3;
        repositoryRootGroup.setLayout(layout);
        
        // url of the repository 
        label = new Label(repositoryRootGroup, SWT.NONE);
        label.setText(Policy.bind("SVNRepositoryPropertiesPage.repositoryUrl")); //$NON-NLS-1$
        repositoryUrlText = new Text(repositoryRootGroup, SWT.SINGLE);
        repositoryUrlText.setText( "");
        repositoryUrlText.setEditable(false);
        data = new GridData(GridData.FILL_HORIZONTAL);
        data.grabExcessHorizontalSpace = true;
        data.horizontalSpan = 2;
        repositoryUrlText.setLayoutData(data);
        
        // url of the repository root 
        label = new Label(repositoryRootGroup, SWT.NONE);
        label.setText(Policy.bind("SVNRepositoryPropertiesPage.repositoryRootUrl")); //$NON-NLS-1$
        repositoryRootText = new Text(repositoryRootGroup, SWT.SINGLE | SWT.BORDER);
        repositoryRootText.setEditable(false);
        data = new GridData(GridData.FILL_HORIZONTAL);
        data.grabExcessHorizontalSpace = true;
        repositoryRootText.setLayoutData(data);
        
        Button button = new Button(repositoryRootGroup, SWT.NONE);
        button.setText(Policy.bind("SVNRepositoryPropertiesPage.browseRootUrl")); //$NON-NLS-1$
        button.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event event) {
                openChooseRootDialog();
            }
        });

        // warning for repository root
        Composite warningComposite = new Composite(composite, SWT.NONE);
        warningComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        layout = new GridLayout();
        layout.numColumns = 2;
        layout.marginHeight = 0;
        layout.marginHeight = 0;
        warningComposite.setLayout(layout);
        
        Label warningLabel = new Label(warningComposite, SWT.NONE);
        warningLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
        warningLabel.setImage(Dialog.getImage(Dialog.DLG_IMG_MESSAGE_WARNING));
        Label warningText = new Label(warningComposite, SWT.WRAP);
        warningText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        warningText.setText(Policy.bind("SVNRepositoryPropertiesPage.rootUrlWarning")); //$NON-NLS-1$
        
        initializeValues();
		return composite;
	}

    /**
     * open a dialog for the user to choose the root repository url
     * @param url
     * @return
     */
    private void openChooseRootDialog() {
        ChooseRootUrlDialog dialog = new ChooseRootUrlDialog(getShell(),location.getUrl()); 
        if (dialog.open() == Window.OK) {
            SVNUrl url = dialog.getRootUrl();
            if (url == null) {
                repositoryRootText.setText(""); //$NON-NLS-1$
            } else {
            	repositoryRootText.setText(dialog.getRootUrl().toString());
            }
        }
    }
    
    
    /**
     * Updates widget enablements and sets error message if appropriate.
     */
    protected void updateWidgetEnablements() {
        if (useUrlLabelButton.getSelection()) {
            customLabelText.setEnabled(false);
        } else {
            customLabelText.setEnabled(true);
        }
        validateFields();
    }    
    
    private void validateFields() {
        if (customLabelText.isEnabled()) {
            if (customLabelText.getText().length() == 0) {
                setValid(false);
                return;
            }
        }
 
        setErrorMessage(null);
        setValid(true);
    }    

    /**
     * Set the initial values of the widgets
     */
    private void initializeValues() {
        passwordChanged = false;
        
        if (showCredentials) {
	        loginText.setText(location.getUsername());
	        passwordText.setText(FAKE_PASSWORD);
        }
        
        // get the repository label
        String label = location.getLabel();
        useUrlLabelButton.setSelection(label == null);
        useCustomLabelButton.setSelection(!useUrlLabelButton.getSelection());
        if (label == null) {
            label = location.getLocation();
        }
        customLabelText.setText(label);
        
        SVNUrl url = location.getUrl();
        if (url != null) {
            repositoryUrlText.setText(url.toString());
        } else {
           repositoryUrlText.setText(""); //$NON-NLS-1$
        }
        
        SVNUrl repositoryRoot = location.getRepositoryRoot();
        if (repositoryRoot != null) {
            repositoryRootText.setText(repositoryRoot.toString());   
        } else {
            repositoryRootText.setText(""); //$NON-NLS-1$
        }
    }    

    /**
     * Initializes the page
     */
    private void initialize() {
        location = null;
        IAdaptable element = getElement();
        if (element instanceof ISVNRepositoryLocation) {
            location = (ISVNRepositoryLocation)element;
        } else {
            Object adapter = element.getAdapter(ISVNRepositoryLocation.class);
            if (adapter instanceof ISVNRepositoryLocation) {
                location = (ISVNRepositoryLocation)adapter;
            }
        }
		showCredentials = SVNProviderPlugin.getPlugin().getSVNClientManager().getSvnClientInterface().equals(COMMANDLINE_CLIENT);
		if (!showCredentials) {
		    if (location.getUsername() != null && !location.getUsername().trim().equals(""))
		        showCredentials = true;
		}
    }    
    
    /* (non-Javadoc)
     * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
     */
    protected void performDefaults() {
        super.performDefaults();
        initializeValues();
    }    

    /*
     * @see PreferencesPage#performOk
     */
    public boolean performOk() {
        if (showCredentials) {
	        if (passwordChanged) {
	            location.setPassword(passwordText.getText());
	        	passwordChanged = false;
	        }
	        location.setUsername(loginText.getText());
        }
        if (useCustomLabelButton.getSelection()) {
        	location.setLabel(customLabelText.getText());
        } else {
        	location.setLabel(null);
        }
        
        if (!repositoryRootText.getText().equals("")) { //$NON-NLS-1$
        	try {
				location.setRepositoryRoot(new SVNUrl(repositoryRootText.getText()));
			} catch (MalformedURLException e1) {
                // should not occur, we don't change the url of the root
			}
        } else {
        	location.setRepositoryRoot(null);
        }
        
        try {
            SVNRepositories repositories = SVNProviderPlugin.getPlugin().getRepositories();
            repositories.addOrUpdateRepository(location);

            SVNLightweightDecorator.refresh();
            
		} catch (SVNException e) {
			handle(e);
            return false;
		}
        
        return true;
    }    

    /**
     * Shows the given errors to the user.
     */
    protected void handle(Throwable e) {
        SVNUIPlugin.openError(getShell(), null, null, e);
    }    
    
    
    
    
}
