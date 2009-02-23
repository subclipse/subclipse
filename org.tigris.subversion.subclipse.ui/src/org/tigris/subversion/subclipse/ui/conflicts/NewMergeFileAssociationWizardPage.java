package org.tigris.subversion.subclipse.ui.conflicts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
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
import org.eclipse.ui.dialogs.ListDialog;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;

public class NewMergeFileAssociationWizardPage extends WizardPage {
	private MergeFileAssociation[] mergeFileAssociations;
	protected Text fileTypeText;
    private Text defaultProgramLocationText;
    private Text defaultProgramParametersText;
    protected Combo customProgramLocationCombo;
    protected Text customProgramParametersText;    
    protected Button builtInMergeRadioButton;
    protected Button externalMergeRadioButton;
    protected Button customMergeRadioButton;
    private Button browseMergeProgramButton;
    private Button variablesButton;	
    
    class StringPair {
        String s1;

        String s2;
    }

	public NewMergeFileAssociationWizardPage(String pageName, String title, ImageDescriptor titleImage, MergeFileAssociation[] mergeFileAssociations) {
		super(pageName, title, titleImage);
		this.mergeFileAssociations = mergeFileAssociations;
	}

	public void createControl(Composite parent) {
		setPageComplete(false);
		IPreferenceStore store = SVNUIPlugin.getPlugin().getPreferenceStore();
		
        Composite composite = new Composite(parent, SWT.NULL);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        composite.setLayout(layout);
        GridData gridData = new GridData();
        gridData.horizontalAlignment = GridData.FILL;
        composite.setLayoutData(gridData);
        
        Label fileTypeLabel = new Label(composite, SWT.NONE);
        fileTypeLabel.setText(Policy.bind("NewMergeFileAssociationWizardPage.fileType")); //$NON-NLS-1$
        fileTypeText = new Text(composite, SWT.BORDER);
        gridData = new GridData();
        gridData.widthHint = 300;
        fileTypeText.setLayoutData(gridData);
        
        Group mergeProgramGroup = new Group(composite, SWT.NULL);
        mergeProgramGroup.setText(Policy.bind("MergeFileAssociationPreferencePage.mergeProgramGroup")); //$NON-NLS-1$
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 2;
        mergeProgramGroup.setLayoutData(gridData); 
        GridLayout mergeProgramGroupLayout = new GridLayout();
        mergeProgramGroupLayout.numColumns = 3;
        mergeProgramGroup.setLayout(mergeProgramGroupLayout);
        
        builtInMergeRadioButton = new Button(mergeProgramGroup, SWT.RADIO);
        builtInMergeRadioButton.setText(Policy.bind("DiffMergePreferencePage.builtInMerge")); //$NON-NLS-1$
        gridData = new GridData();
        gridData.horizontalSpan = 3;
        builtInMergeRadioButton.setLayoutData(gridData);
        
        externalMergeRadioButton = new Button(mergeProgramGroup, SWT.RADIO);
        externalMergeRadioButton.setText(Policy.bind("MergeFileAssociationPreferencePage.defaultExternal")); //$NON-NLS-1$    
        gridData = new GridData();
        
        defaultProgramLocationText = new Text(mergeProgramGroup, SWT.BORDER);
        defaultProgramLocationText.setEnabled(false);
        gridData = new GridData();
        gridData.horizontalSpan = 2;
        gridData.widthHint = 400;
        defaultProgramLocationText.setLayoutData(gridData);
        
        Label defaultParametersLabel = new Label(mergeProgramGroup, SWT.NONE);
        defaultParametersLabel.setText(Policy.bind("DiffMergePreferencePage.mergeProgramParameters")); //$NON-NLS-1$
        gridData = new GridData();
        gridData.horizontalAlignment = SWT.RIGHT;
        defaultParametersLabel.setLayoutData(gridData);
        
        defaultProgramParametersText = new Text(mergeProgramGroup, SWT.BORDER);
        defaultProgramParametersText.setEnabled(false);
        gridData = new GridData();
        gridData.horizontalSpan = 2;
        gridData.widthHint = 400;
        defaultProgramParametersText.setLayoutData(gridData);        

        defaultProgramLocationText.setText(store
                .getString(ISVNUIConstants.PREF_MERGE_PROGRAM_LOCATION));
        defaultProgramParametersText.setText(store
                .getString(ISVNUIConstants.PREF_MERGE_PROGRAM_PARAMETERS));        
        
        customMergeRadioButton = new Button(mergeProgramGroup, SWT.RADIO);
        customMergeRadioButton.setText(Policy.bind("MergeFileAssociationPreferencePage.customExternal")); //$NON-NLS-1$               
        
        customProgramLocationCombo = new Combo(mergeProgramGroup, SWT.BORDER);
        gridData = new GridData();
        gridData.widthHint = 400;
        customProgramLocationCombo.setLayoutData(gridData);
        
        for (int i = 0; i < mergeFileAssociations.length; i++) {
        	if (mergeFileAssociations[i].getType() == MergeFileAssociation.CUSTOM_EXTERNAL) {
        		if (customProgramLocationCombo.indexOf(mergeFileAssociations[i].getMergeProgram()) == -1) 
        			customProgramLocationCombo.add(mergeFileAssociations[i].getMergeProgram());
        	}
        }
        
        browseMergeProgramButton = new Button(mergeProgramGroup, SWT.PUSH);
        browseMergeProgramButton.setText(Policy.bind("DiffMergePreferencePage.browse")); //$NON-NLS-1$
        gridData = new GridData();
        gridData.horizontalAlignment = GridData.FILL;
        browseMergeProgramButton.setLayoutData(gridData); 
        
        browseMergeProgramButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                FileDialog fileDialog = new FileDialog(getShell(), SWT.OPEN);
                String res = fileDialog.open();
                if (res != null) {
                    customProgramLocationCombo.setText(res);
                }
            }
        });

        Label customParametersLabel = new Label(mergeProgramGroup, SWT.NONE);
        customParametersLabel.setText(Policy.bind("NewMergeFileAssociationWizardPage.customParameters")); //$NON-NLS-1$
        gridData = new GridData();
        gridData.horizontalAlignment = SWT.RIGHT;
        customParametersLabel.setLayoutData(gridData);        

        customProgramParametersText = new Text(mergeProgramGroup, SWT.BORDER);
        gridData = new GridData();
        gridData.widthHint = 400;
        customProgramParametersText.setLayoutData(gridData);
        
		FocusListener focusListener = new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				((Text)e.getSource()).selectAll();
			}
			public void focusLost(FocusEvent e) {
				((Text)e.getSource()).setText(((Text)e.getSource()).getText());
			}					
		};
		fileTypeText.addFocusListener(focusListener);
		customProgramParametersText.addFocusListener(focusListener);
        
        variablesButton = new Button(mergeProgramGroup, SWT.PUSH);
        variablesButton.setText(Policy.bind("DiffMergePreferencePage.mergeProgramVariables")); //$NON-NLS-1$
        gridData = new GridData();
        gridData.horizontalAlignment = GridData.FILL;
        variablesButton.setLayoutData(gridData);
        
        final Text formatToInsert = customProgramParametersText;
        variablesButton.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event event) {
                addVariables(formatToInsert, getMergeBindingDescriptions());
            }
        });
        
        customMergeRadioButton.setSelection(true);
        
        ModifyListener modifyListener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				setPageComplete(canFinish());
			}      	
        };
        
        fileTypeText.addModifyListener(modifyListener);
        customProgramLocationCombo.addModifyListener(modifyListener);
        
        SelectionListener selectionListener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				setPageComplete(canFinish());
				customProgramLocationCombo.setEnabled(customMergeRadioButton.getSelection());
				customProgramParametersText.setEnabled(customMergeRadioButton.getSelection());
				if (e.getSource() == customMergeRadioButton && customMergeRadioButton.getSelection())
					customProgramLocationCombo.setFocus();
				if (e.getSource() == customProgramLocationCombo) {
					for (int i = 0; i < mergeFileAssociations.length; i++) {
						if (mergeFileAssociations[i].getType() == MergeFileAssociation.CUSTOM_EXTERNAL) {
							if (mergeFileAssociations[i].getMergeProgram().equals(customProgramLocationCombo.getText())) {
								customProgramParametersText.setText(mergeFileAssociations[i].getParameters());
								break;
							}
						}
					}
				}
			}
        };
        
        builtInMergeRadioButton.addSelectionListener(selectionListener);
        externalMergeRadioButton.addSelectionListener(selectionListener);
        customMergeRadioButton.addSelectionListener(selectionListener);
        customProgramLocationCombo.addSelectionListener(selectionListener);
        
        fileTypeText.setFocus();
        
        setControl(composite);
	}
	
	private boolean canFinish() {
		setErrorMessage(null); 
		if (fileTypeText.getText().trim().length() == 0) return false;
		if (fileTypeText.getText().indexOf(".") == -1) return false; //$NON-NLS-1$
		for (int i = 0; i < mergeFileAssociations.length; i++) {
			if (mergeFileAssociations[i].getFileType().equals(fileTypeText.getText().trim())) {
				String[] fileType = { fileTypeText.getText().trim() };
				setErrorMessage(Policy.bind("NewMergeFileAssociationWizardPage.fileTypeExists", fileType)); //$NON-NLS-1$
				return false;
			}
		}
		if (customMergeRadioButton.getSelection()) {
			if (customProgramLocationCombo.getText().trim().length() == 0) return false;
		}
		return true;
	}
	
    private void addVariables(Text target, Map bindings) {

        final List variables = new ArrayList(bindings.size());

        ILabelProvider labelProvider = new LabelProvider() {
            public String getText(Object element) {
                return ((StringPair) element).s1
                        + " - " + ((StringPair) element).s2; //$NON-NLS-1$
            }
        };

        IStructuredContentProvider contentsProvider = new IStructuredContentProvider() {
            public Object[] getElements(Object inputElement) {
                return variables.toArray(new StringPair[variables.size()]);
            }

            public void dispose() {
            }

            public void inputChanged(Viewer viewer, Object oldInput,
                    Object newInput) {
            }
        };

        for (Iterator it = bindings.keySet().iterator(); it.hasNext();) {
            StringPair variable = new StringPair();
            variable.s1 = (String) it.next(); // variable
            variable.s2 = (String) bindings.get(variable.s1); // description
            variables.add(variable);
        }

        ListDialog dialog = new ListDialog(this.getShell());
        dialog.setContentProvider(contentsProvider);
        dialog.setAddCancelButton(true);
        dialog.setLabelProvider(labelProvider);
        dialog.setInput(variables);
        dialog.setTitle(Policy
                .bind("DiffMergePreferencePage.addVariableDialogTitle")); //$NON-NLS-1$
        if (dialog.open() != ListDialog.OK)
            return;

        Object[] result = dialog.getResult();

        for (int i = 0; i < result.length; i++) {
            target.insert("${" + ((StringPair) result[i]).s1 + "}"); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }	
	
    private Map getMergeBindingDescriptions() {
        Map bindings = new HashMap();
        bindings
                .put(
                        "merged", Policy.bind("DiffMergePreferencePage.mergedVariableComment")); //$NON-NLS-1$ //$NON-NLS-2$
        bindings
                .put(
                        "theirs", Policy.bind("DiffMergePreferencePage.theirsVariableComment")); //$NON-NLS-1$ //$NON-NLS-2$
        bindings
                .put(
                        "yours", Policy.bind("DiffMergePreferencePage.yoursVariableComment")); //$NON-NLS-1$ //$NON-NLS-2$
        bindings
                .put(
                        "base", Policy.bind("DiffMergePreferencePage.baseVariableComment"));//$NON-NLS-1$ //$NON-NLS-2$
        return bindings;
    }	

}
