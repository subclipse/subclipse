package org.tigris.subversion.subclipse.ui.conflicts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ColumnLayoutData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ListDialog;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.wizards.ClosableWizardDialog;

public class MergeFileAssociationPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
	private Table table;
	private TableViewer viewer;	
	private Button removeButton;
	private Group mergeProgramGroup;
    private Text defaultProgramLocationText;
    private Text defaultProgramParametersText;
    private Combo customProgramLocationCombo;
    private Text customProgramParametersText;    
    private Button builtInMergeRadioButton;
    private Button externalMergeRadioButton;
    private Button customMergeRadioButton;
    private Button browseMergeProgramButton;
    private Button variablesButton;
    private IPreferenceStore store;
    private MergeFileAssociation[] mergeFileAssociations;
    private boolean associationsUpdated = false;
    private boolean updating = true;
    private static ArrayList images;
    
    class StringPair {
        String s1;

        String s2;
    }    
    
	private String[] columnHeaders = {""}; //$NON-NLS-1$
	private ColumnLayoutData columnLayouts[] = {
		new ColumnWeightData(100, 100, true)};
	
	protected Control createContents(Composite parent) {
		images = new ArrayList();
		store = SVNUIPlugin.getPlugin().getPreferenceStore();
		mergeFileAssociations = getMergeFileAssociations();
		
        Composite composite = new Composite(parent, SWT.NULL);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        composite.setLayout(layout);
        GridData gridData = new GridData();
        gridData.horizontalAlignment = GridData.FILL;
        composite.setLayoutData(gridData);
        
        Label fileTypesLabel = new Label(composite, SWT.NONE);
        gridData = new GridData();
        gridData.horizontalSpan = 2;
        fileTypesLabel.setLayoutData(gridData);
        fileTypesLabel.setText(Policy.bind("MergeFileAssociationPreferencePage.fileTypes")); //$NON-NLS-1$

		table = new Table(composite, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
		table.setLinesVisible(false);
		gridData = new GridData();
		gridData.widthHint = 400;
		gridData.heightHint = 200;
		table.setLayoutData(gridData);
		TableLayout tableLayout = new TableLayout();
		table.setLayout(tableLayout);
		table.setHeaderVisible(false);
		viewer = new TableViewer(table);
		
		viewer.setContentProvider(new MergeFileAssociationsContentProvider());
		viewer.setLabelProvider(new MergeFileAssociationsLabelProvider());
		for (int i = 0; i < columnHeaders.length; i++) {
			tableLayout.addColumnData(columnLayouts[i]);
			TableColumn tc = new TableColumn(table, SWT.NONE,i);
			tc.setResizable(columnLayouts[i].resizable);
			tc.setText(columnHeaders[i]);
		}
		viewer.setInput(this);	
		
        Composite buttonGroup = new Composite(composite, SWT.NULL);
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        buttonGroup.setLayoutData(gridData); 
        GridLayout buttonLayout = new GridLayout();
        buttonLayout.numColumns = 1;
        buttonGroup.setLayout(buttonLayout);
        
        Button addButton = new Button(buttonGroup, SWT.PUSH);
        addButton.setText(Policy.bind("MergeFileAssociationPreferencePage.add")); //$NON-NLS-1$
        gridData = new GridData();
        gridData.horizontalAlignment = GridData.FILL;
        addButton.setLayoutData(gridData);
        
        addButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				addFileType();
			}      	
        });
        
        removeButton = new Button(buttonGroup, SWT.PUSH);
        removeButton.setText(Policy.bind("MergeFileAssociationPreferencePage.remove")); //$NON-NLS-1$
        gridData = new GridData();
        gridData.horizontalAlignment = GridData.FILL;
        removeButton.setLayoutData(gridData); 
        removeButton.setEnabled(false);
        removeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				removeFileType();
			}
        });
        
        mergeProgramGroup = new Group(composite, SWT.NULL);
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
        customProgramLocationCombo.setEnabled(false);
        
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
        browseMergeProgramButton.setEnabled(false);
        
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
        customParametersLabel.setText(Policy.bind("DiffMergePreferencePage.mergeProgramParameters")); //$NON-NLS-1$
        gridData = new GridData();
        gridData.horizontalAlignment = SWT.RIGHT;
        customParametersLabel.setLayoutData(gridData);        

        customProgramParametersText = new Text(mergeProgramGroup, SWT.BORDER);
        defaultProgramParametersText.setEnabled(false);
        gridData = new GridData();
        gridData.widthHint = 400;
        customProgramParametersText.setLayoutData(gridData);
        
        variablesButton = new Button(mergeProgramGroup, SWT.PUSH);
        variablesButton.setText(Policy.bind("DiffMergePreferencePage.mergeProgramVariables")); //$NON-NLS-1$
        gridData = new GridData();
        gridData.horizontalAlignment = GridData.FILL;
        variablesButton.setLayoutData(gridData);
        variablesButton.setEnabled(false);
        
        final Text formatToInsert = customProgramParametersText;
        variablesButton.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event event) {
                addVariables(formatToInsert, getMergeBindingDescriptions());
            }
        });        
        
        mergeProgramGroup.setEnabled(false);
        
        viewer.getTable().addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updating = false;
				removeButton.setEnabled(!viewer.getSelection().isEmpty());
				mergeProgramGroup.setEnabled(!viewer.getSelection().isEmpty());
				IStructuredSelection selection = (IStructuredSelection)viewer.getSelection();
				MergeFileAssociation mergeFileAssociation = (MergeFileAssociation)selection.getFirstElement();
				if (mergeFileAssociation != null) {
					switch (mergeFileAssociation.getType()) {
					case MergeFileAssociation.BUILT_IN:
						builtInMergeRadioButton.setSelection(true);
						externalMergeRadioButton.setSelection(false);
						customMergeRadioButton.setSelection(false);
						break;
					case MergeFileAssociation.DEFAULT_EXTERNAL:
						externalMergeRadioButton.setSelection(true);
						builtInMergeRadioButton.setSelection(false);
						customMergeRadioButton.setSelection(false);
						break;
					case MergeFileAssociation.CUSTOM_EXTERNAL:
						customMergeRadioButton.setSelection(true);
						builtInMergeRadioButton.setSelection(false);
						externalMergeRadioButton.setSelection(false);
						break;						
					default:
						break;
					}
					if (mergeFileAssociation.getMergeProgram() == null) customProgramLocationCombo.setText(""); //$NON-NLS-1$
					else customProgramLocationCombo.setText(mergeFileAssociation.getMergeProgram());
					if (mergeFileAssociation.getParameters() == null) customProgramParametersText.setText(""); //$NON-NLS-1$
					else customProgramParametersText.setText(mergeFileAssociation.getParameters());
					customProgramLocationCombo.setEnabled(customMergeRadioButton.getSelection());
					customProgramParametersText.setEnabled(customMergeRadioButton.getSelection());
					browseMergeProgramButton.setEnabled(customMergeRadioButton.getSelection());
					variablesButton.setEnabled(customMergeRadioButton.getSelection());
				}
				updating = true;
			}
        });
        
        SelectionListener selectionListener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				customProgramLocationCombo.setEnabled(customMergeRadioButton.getSelection());
				customProgramParametersText.setEnabled(customMergeRadioButton.getSelection());
				browseMergeProgramButton.setEnabled(customMergeRadioButton.getSelection());
				variablesButton.setEnabled(customMergeRadioButton.getSelection());
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
				if (e.getSource() instanceof Button) {
					IStructuredSelection selection = (IStructuredSelection)viewer.getSelection();	
					MergeFileAssociation mergeFileAssociation = (MergeFileAssociation)selection.getFirstElement();
					if (mergeFileAssociation != null) {
						int type = mergeFileAssociation.getType();
						if (builtInMergeRadioButton.getSelection()) type = MergeFileAssociation.BUILT_IN;
						else if (externalMergeRadioButton.getSelection()) type = MergeFileAssociation.DEFAULT_EXTERNAL;
						else if (customMergeRadioButton.getSelection()) type = MergeFileAssociation.CUSTOM_EXTERNAL;
						if (type != mergeFileAssociation.getType()) {
							associationsUpdated = true;
							mergeFileAssociation.setType(type);
						}
					}
				}
			}
        };
        
        builtInMergeRadioButton.addSelectionListener(selectionListener);
        externalMergeRadioButton.addSelectionListener(selectionListener);
        customMergeRadioButton.addSelectionListener(selectionListener);
        customProgramLocationCombo.addSelectionListener(selectionListener);     
        
        ModifyListener modifyListener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {			
				IStructuredSelection selection = (IStructuredSelection)viewer.getSelection();
				MergeFileAssociation mergeFileAssociation = (MergeFileAssociation)selection.getFirstElement();
				if (updating && mergeFileAssociation != null) {
					associationsUpdated = true;
					mergeFileAssociation.setMergeProgram(customProgramLocationCombo.getText().trim());
					mergeFileAssociation.setParameters(customProgramParametersText.getText().trim());
				}
			}     	
        };
        
        customProgramLocationCombo.addModifyListener(modifyListener);
        customProgramParametersText.addModifyListener(modifyListener);
   
		FocusListener focusListener = new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				((Text)e.getSource()).selectAll();
			}
			public void focusLost(FocusEvent e) {
				((Text)e.getSource()).setText(((Text)e.getSource()).getText());
			}					
		};
		customProgramParametersText.addFocusListener(focusListener);
        
		return composite;
	}

	public void init(IWorkbench workbench) {
		noDefaultAndApplyButton();
	}
	
	public boolean performOk() {
		MergeFileAssociation[] currentAssociations = getMergeFileAssociations();
		for (int i = 0; i < currentAssociations.length; i++) {
			currentAssociations[i].remove();
		}
		for (int i = 0; i < mergeFileAssociations.length; i++) {
			Preferences prefs = MergeFileAssociation.getParentPreferences().node(mergeFileAssociations[i].getFileType());
			if (mergeFileAssociations[i].getMergeProgram() == null) prefs.put("mergeProgram", ""); //$NON-NLS-1$ //$NON-NLS-1$ //$NON-NLS-2$
			else prefs.put("mergeProgram", mergeFileAssociations[i].getMergeProgram()); //$NON-NLS-1$
			if (mergeFileAssociations[i].getParameters() == null)prefs.put("parameters", ""); //$NON-NLS-1$ //$NON-NLS-1$ //$NON-NLS-2$ 
			else prefs.put("parameters", mergeFileAssociations[i].getParameters()); //$NON-NLS-1$
			prefs.putInt("type", mergeFileAssociations[i].getType()); //$NON-NLS-1$
			try {
				prefs.flush();
			} catch (BackingStoreException e) {}
		}
		return super.performOk();
	}

	public boolean performCancel() {
		if (associationsUpdated) {
			if (MessageDialog.openQuestion(getShell(), Policy.bind("MergeFileAssociationPreferencePage.title"), Policy.bind("MergeFileAssociationPreferencePage.confirmCancel"))) //$NON-NLS-1$ //$NON-NLS-1$ //$NON-NLS-2$
				performOk();
		}
		return super.performCancel();
	}

	public void dispose() {
		Iterator iter = images.iterator();
		while(iter.hasNext()) {
			Image image = (Image)iter.next();
			image.dispose();
		}
		super.dispose();
	}

	private void addFileType() {
		NewMergeFileAssociationWizard wizard = new NewMergeFileAssociationWizard(mergeFileAssociations);
		WizardDialog dialog = new ClosableWizardDialog(Display.getCurrent().getActiveShell(), wizard);
		if (dialog.open() == WizardDialog.CANCEL) return;
		associationsUpdated = true;
		ArrayList associationsList = new ArrayList();
		for (int i = 0; i < mergeFileAssociations.length; i++) associationsList.add(mergeFileAssociations[i]);
		associationsList.add(wizard.getMergeFileAssociation());
		mergeFileAssociations = new MergeFileAssociation[associationsList.size()];
		associationsList.toArray(mergeFileAssociations);
		Arrays.sort(mergeFileAssociations);
		viewer.refresh();
	}
	
	private void removeFileType() {
		associationsUpdated = true;
		ArrayList associationsList = new ArrayList();
		for (int i = 0; i < mergeFileAssociations.length; i++) associationsList.add(mergeFileAssociations[i]);
		IStructuredSelection selection = (IStructuredSelection)viewer.getSelection();
		Iterator iter = selection.iterator();
		while (iter.hasNext()) {
			associationsList.remove(iter.next());
		}
		mergeFileAssociations = new MergeFileAssociation[associationsList.size()];
		associationsList.toArray(mergeFileAssociations);
		viewer.refresh();
		builtInMergeRadioButton.setSelection(false);
		externalMergeRadioButton.setSelection(false);
		customMergeRadioButton.setSelection(false);
		customProgramLocationCombo.setText(""); //$NON-NLS-1$
		customProgramParametersText.setText(""); //$NON-NLS-1$
	}
	
	private MergeFileAssociation[] getMergeFileAssociations() {
		try {
			return SVNUIPlugin.getPlugin().getMergeFileAssociations();
		} catch (BackingStoreException e) {
			SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
			return new MergeFileAssociation[0];
		}
	}
	
	class MergeFileAssociationsContentProvider implements IStructuredContentProvider {
		public void dispose() {
		}
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
		public Object[] getElements(Object obj) {
			return mergeFileAssociations;
		}
	}	
	
	static class MergeFileAssociationsLabelProvider extends LabelProvider implements ITableLabelProvider {

		public String getColumnText(Object element, int columnIndex) {
			MergeFileAssociation mergeFileAssociation = (MergeFileAssociation)element;
			switch (columnIndex) { 
				case 0: return mergeFileAssociation.getFileType();		
			}
			return ""; //$NON-NLS-1$ 
		}
		
		public Image getColumnImage(Object element, int columnIndex) {
			MergeFileAssociation mergeFileAssociation = (MergeFileAssociation)element;
			ImageDescriptor id = PlatformUI.getWorkbench().getEditorRegistry().getImageDescriptor(mergeFileAssociation.getFileType());
			if (id == null) id = PlatformUI.getWorkbench().getEditorRegistry().getImageDescriptor("file.txt"); //$NON-NLS-1$
			if (id != null) {
				Image image = id.createImage();
				images.add(image);
				return image;
			}
			return null;
		}
	
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
