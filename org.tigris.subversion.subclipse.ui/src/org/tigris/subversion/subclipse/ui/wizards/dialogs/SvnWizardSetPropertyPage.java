package org.tigris.subversion.subclipse.ui.wizards.dialogs;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;

import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.properties.SVNPropertyDefinition;
import org.tigris.subversion.subclipse.core.properties.SVNPropertyManager;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.svnclientadapter.ISVNProperty;

public class SvnWizardSetPropertyPage extends SvnWizardDialogPage {
	private ISVNProperty property;   // null when we set a new property
	private ISVNLocalResource svnResource;	
	private Combo propertyNameText;
	private Text propertyValueText;
	private Text fileText;
	private Button textRadio;
	private Button fileRadio;
	private Button browseButton;
	private Label statusMessageLabel;
	private Button recurseCheckbox;	
	
	private String propertyName;
	private String propertyValue;
	private File propertyFile;
	private boolean recurse;
	
	private ArrayList allPropertyTypes = new ArrayList();
	private SVNPropertyDefinition[] propertyTypes;
	private ArrayList propertyNames;
	private int prop;	

	public SvnWizardSetPropertyPage(ISVNLocalResource svnResource) {
		this(svnResource, null);
	}	

	public SvnWizardSetPropertyPage(ISVNLocalResource svnResource, ISVNProperty property) {
		super("SetSvnPropertyDialog", Policy.bind("SetSvnPropertyDialog.title")); //$NON-NLS-1$ //$NON-NLS-2$
		this.property = property;
		this.svnResource = svnResource;	
	}

	public void createButtonsForButtonBar(Composite parent, SvnWizardDialog wizardDialog) {
	}

	public void createControls(Composite parent) {

		Listener updateEnablementsListener = new Listener() {
			public void handleEvent(Event event) {
				updateEnablements();
			}
		};
		
		Listener validateListener = new Listener() {
			public void handleEvent(Event event) {
				validate();
			}
		};

		Listener updatePropertiesListener = new Listener() {
			public void handleEvent(Event event) {
//				updateProperties();
				validate();
			}
		};	
		
		Composite area = new Composite(parent, SWT.NULL);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		area.setLayout(gridLayout);
		area.setLayoutData(new GridData(GridData.FILL_BOTH));		

		// create the property name label and the corresponding Text		
		Label label = new Label(area,SWT.LEFT);
		label.setLayoutData(new GridData());
		label.setText(Policy.bind("SetSvnPropertyDialog.propertyName")); //$NON-NLS-1$

		propertyNameText = new Combo(area, SWT.BORDER);
		propertyNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		if (property != null) {
      propertyNameText.setText(property.getName());
      propertyNameText.setEnabled(false);
    }
		getPropertyTypes();
		propertyNameText.addListener(SWT.Modify,validateListener);
		propertyNameText.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent se) {
                getProperty();
            }		  
		});
		propertyNameText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent me) {
                getProperty();
            }		    
		});		

		// create the group
		Group group = new Group(area,SWT.NULL);
		group.setText(Policy.bind("SetSvnPropertyDialog.propertyContent")); //$NON-NLS-1$
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true, 2, 1));
		group.setLayout(new GridLayout(2, false)); 		

		// create "Enter a text property" radio button
		textRadio = new Button(group, SWT.RADIO);
		textRadio.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		textRadio.setText(Policy.bind("SetSvnPropertyDialog.enterTextProperty")); //$NON-NLS-1$
		textRadio.addListener(SWT.Selection,updateEnablementsListener);
		textRadio.setSelection(true);	
		
		// create the Text for the content
		propertyValueText = new Text(group,SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
		gridData.heightHint = 100;
		gridData.widthHint = 400;
		gridData.horizontalIndent = 15;
		propertyValueText.setLayoutData(gridData);

		// create "Use a file" radio button
		fileRadio = new Button(group, SWT.RADIO);
		fileRadio.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		fileRadio.setText(Policy.bind("SetSvnPropertyDialog.useFile")); //$NON-NLS-1$
		fileRadio.addListener(SWT.Selection,updateEnablementsListener);
		fileText = new Text(group, SWT.SINGLE|SWT.BORDER);
		GridData fileTextData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		fileTextData.horizontalIndent = 15;
		fileText.setLayoutData(fileTextData);
		fileText.setEditable(false);
		browseButton = new Button(group,SWT.PUSH);
		browseButton.setLayoutData(new GridData());
		browseButton.setText(Policy.bind("SetSvnPropertyDialog.browse")); //$NON-NLS-1$
		browseButton.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent event) {
				FileDialog fileDialog = new FileDialog(getShell(),SWT.OPEN);
				String res = fileDialog.open();
				if (res != null) {
					fileText.setText(res);
					validate();
				}
			}
		});
		
		// file input and button

		// checkbox 
		recurseCheckbox = new Button(area,SWT.CHECK);
		recurseCheckbox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
		recurseCheckbox.setText(Policy.bind("SetSvnPropertyDialog.setPropertyRecursively")); //$NON-NLS-1$
		recurseCheckbox.setSelection(false);
		if (!svnResource.isFolder()) {
			recurseCheckbox.setEnabled(false);
		}
		recurseCheckbox.addListener(SWT.Selection, updatePropertiesListener);
	
		// status message
		statusMessageLabel = new Label(area, SWT.LEFT);
		statusMessageLabel.setText(""); //$NON-NLS-1$
		GridData data = new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1);
		statusMessageLabel.setLayoutData(data);
		statusMessageLabel.setFont(parent.getFont());
		
		if (property != null) {
			propertyValueText.setText(property.getValue());
			getProperty();
		}
		propertyValueText.addListener(SWT.Modify,updatePropertiesListener);
		
		propertyValueText.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent e) {
	          if (e.detail == SWT.TRAVERSE_RETURN && (e.stateMask & SWT.CTRL) != 0) {
                e.doit = false;
                if (isPageComplete()) {
                	((SvnWizard)getWizard()).finishAndClose();
                }
	          }
			}			
		});
		
		updateEnablements();
	
		PlatformUI.getWorkbench().getHelpSystem().setHelp(area, IHelpContextIds.SET_SVN_PROPERTY_DIALOG);	
	}

	public String getWindowTitle() {
		return Policy.bind("SetSvnPropertyDialog.title"); //$NON-NLS-1$
	}

	public boolean performCancel() {
		return true;
	}

	public boolean performFinish() {
		return true;
	}

	public void saveSettings() {
	}

	public void setMessage() {
		setMessage(Policy.bind("SetSvnPropertyDialog.message")); //$NON-NLS-1$
	}
	
	private boolean isNewProperty() {
		return property == null;
	}
	
    private void getPropertyTypes() {
	    if (svnResource.isFolder()) {
	    	SVNPropertyDefinition[] allProperties = SVNPropertyManager.getInstance().getPropertyTypes();
	    	for (int i = 0; i < allProperties.length; i++) allPropertyTypes.add(allProperties[i]);
	        propertyTypes = SVNPropertyManager.getInstance().getFolderPropertyTypes();
	    } else
	        propertyTypes = SVNPropertyManager.getInstance().getFilePropertyTypes();
	    propertyNames = new ArrayList();
	    for (int i = 0; i < propertyTypes.length; i++) {
	        propertyNameText.add(propertyTypes[i].getName());
	        propertyNames.add(propertyTypes[i].getName());
	    }
    }

    private void updateEnablements() {
		if (textRadio.getSelection()) {
			browseButton.setEnabled(false);
			fileText.setEnabled(false);
			propertyValueText.setEnabled(true);
		} else {
			browseButton.setEnabled(true);
			fileText.setEnabled(true);			
			propertyValueText.setEnabled(false);
		}
		validate();
	}


	/**
	 * get the name of the property to set 
	 */
	public String getPropertyName() {
		return propertyName;
	}

	/**
	 * get the value of the property or null if a file is used to set the property
	 */
	public String getPropertyValue() {
		return propertyValue;
	}

	/**
	 * get the file to use as a property value or null if plain text is used instead 
	 */
	public File getPropertyFile() {
		return propertyFile;
	}

	public boolean getRecurse() {
		return recurse;
	}
	
	private void updateProperties() {
		propertyName = propertyNameText.getText();
		if (textRadio.getSelection()) {
			propertyFile = null;
			propertyValue = propertyValueText.getText();
		} else {
			propertyFile = new File(fileText.getText());
			propertyValue = null;
		}
		recurse = recurseCheckbox.getSelection();
	}

	private void validate() {
		// update the properties from the controls
		updateProperties();
		
		// verify property name
		if (propertyName.equals("")) { //$NON-NLS-1$
			setError(""); //$NON-NLS-1$
			return;
		} else {
			if (isNewProperty()) {
				try {
					// validate will be called each time the property name is changed (each time propertyNameText is changed)
					// we don't want the command to be written on the console ... 
					SVNUIPlugin.getPlugin().disableConsoleListener();
					
					// if we are setting a new property, make sure the property does not exist
					if (svnResource.getSvnProperty(getPropertyName()) != null) {
						setError(Policy.bind("SetSvnPropertyDialog.anotherPropertyHasSameName")); //$NON-NLS-1$
						return;				
					}	
				} catch (SVNException e) {
					// we ignore the exception, we can't do much more ...
				} finally {
					SVNUIPlugin.getPlugin().enableConsoleListener();
				}
			}
		    if (textRadio.getSelection() && (prop != -1)) {
		        if (propertyTypes[prop].isBoolean()) {
		            if (!propertyValue.equals("true") && !propertyValue.equals("false")) {
		                setError(Policy.bind("SetSvnPropertyDialog.boolean")); //$NON-NLS-1$
		                return;
		            }
		        }
		        if (propertyTypes[prop].isNumber()) {		            
		            try {
		                Integer.parseInt(propertyValue);
		            } catch (Exception e) {
			            setError(Policy.bind("SetSvnPropertyDialog.number")); //$NON-NLS-1$
			            return;
		            }
		        }
		        if (propertyTypes[prop].isUrl()) {	
		        	if (!propertyName.equals("bugtraq:url") || (!propertyValue.startsWith("^/") && !propertyValue.startsWith("/"))) {
			            try {
			                new URL(propertyValue);
			            } catch (Exception e) {
				            setError(Policy.bind("SetSvnPropertyDialog.url")); //$NON-NLS-1$
			                return;
			            }
		        	}
		        } 		   		        
		    }
		    // if non-folder property specified for folder, recurse must be selected.
		    if (svnResource.isFolder() && !recurseCheckbox.getSelection() && prop == -1) {
		    	SVNPropertyDefinition checkDefinition = new SVNPropertyDefinition(propertyName, null);
		    	int index = allPropertyTypes.indexOf(checkDefinition);
		    	if (index != -1) {
		    		setError(Policy.bind("SetSvnPropertyDialog.recurseRequired")); //$NON-NLS-1$
		    		return;
		    	}
		    }
		}
		
		// verify file
		if (propertyFile != null) {
			if (fileText.getText().equals("")) { //$NON-NLS-1$
				setError(""); //$NON-NLS-1$
			} else {
				if (!getPropertyFile().exists()) {
					setError(Policy.bind("SetSvnPropertyDialog.fileDoesNotExist")); //$NON-NLS-1$
					return;
				}
			}
		}
		setError(null);
	}

	private void setError(String text) {
		if (text == null) {
			statusMessageLabel.setText(""); //$NON-NLS-1$
			setPageComplete(true);
		} else {
			statusMessageLabel.setText(text);
			statusMessageLabel.setForeground(JFaceColors.getErrorText(getShell().getDisplay()));
			setPageComplete(false);
		}
	}

    private void getProperty() {
        prop = propertyNames.indexOf(propertyNameText.getText());
        if (prop != -1) {
            propertyNameText.setToolTipText(propertyTypes[prop].getDescription());
            recurseCheckbox.setEnabled(svnResource.isFolder() && propertyTypes[prop].isAllowRecurse());
            if (!recurseCheckbox.getEnabled()) recurseCheckbox.setSelection(false);
        } else recurseCheckbox.setEnabled(svnResource.isFolder());
        validate();
    }	

}
