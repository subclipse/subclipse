/*******************************************************************************
 * Copyright (c) 2003, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.preferences;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.team.ui.TeamImages;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ListSelectionDialog;
import org.eclipse.ui.ide.IDE.SharedImages;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.eclipse.ui.themes.ITheme;
import org.tigris.subversion.subclipse.core.ISVNCoreConstants;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.decorator.SVNDecoratorConfiguration;
import org.tigris.subversion.subclipse.ui.decorator.SVNLightweightDecorator;
import org.tigris.subversion.subclipse.ui.decorator.SVNLightweightDecorator.CachedImageDescriptor;
import org.tigris.subversion.subclipse.ui.internal.SWTUtils;

/**
 * The preference page for decoration
 * 
 */
public class SVNDecoratorPreferencesPage extends PreferencePage implements IWorkbenchPreferencePage {

	private Button imageShowDirty;
	private Button imageShowHasRemote;
	private Button imageShowAdded;
	private Button imageShowNewResource;
	private Button imageShowExternal;
	private Button imageShowReadOnly;
	
	private Text fileTextFormat;
	
	private Text folderTextFormat;
	
	private Text projectTextFormat;
	
	private Text dateFormatText;
	
	private Text dirtyFlag;
	private Text addedFlag;
    private Text externalFlag;

	private Button showDirty;
	private Button enableFontDecorators;

	protected static final Collection ROOT;
	private static ImageDescriptor newResource;
	private static ImageDescriptor dirty;
	private static ImageDescriptor added;
	private static ImageDescriptor moved;
	private static ImageDescriptor checkedIn;
	private static ImageDescriptor external;
    private static ImageDescriptor locked;
    private static ImageDescriptor needsLock;
    private static ImageDescriptor conflicted;
    private static ImageDescriptor deleted;
    private static ImageDescriptor switched;

	private static ThemeListener fThemeListener;
	
	static {		
		final PreviewFile project= new PreviewFile("Project", IResource.PROJECT, false, false, false, false, false, true, false, false, false, false, false, false, null, "v1_0"); //$NON-NLS-1$ //$NON-NLS-2$
		final ArrayList children= new ArrayList();
		children.add(new PreviewFile("External Folder", IResource.FOLDER, false, false, false, false, false, true, true, false, false, false, false, false, null, null)); //$NON-NLS-1$
		children.add(new PreviewFile("Folder", IResource.FOLDER, false, false, false, false, false, true, false, false, false, false, false, false, null, null)); //$NON-NLS-1$
		children.add(new PreviewFile("Scheduled for Delete Folder", IResource.FOLDER, false, false, false, false, false, true, false, false, false, false, true, false, null, null)); //$NON-NLS-1$	
		children.add(new PreviewFile("Switched Folder", IResource.FOLDER, false, false, false, false, false, true, false, false, false, false, false, true, null, null)); //$NON-NLS-1$		
		children.add(new PreviewFile("added.java", IResource.FILE, true, false, false, true, false, false, false, false, false, false, false, false, null, null)); //$NON-NLS-1$		
		children.add(new PreviewFile("conflicted.txt", IResource.FILE, false, false, false, false, false, true, false, false, false, true, false, false, null, null)); //$NON-NLS-1$				
		children.add(new PreviewFile("dirty.cpp", IResource.FILE, false, false, false, true, false, true, false, false, false, false, false, false, null, null)); //$NON-NLS-1$		
		children.add(new PreviewFile("ignored.txt", IResource.FILE, false, false, false, false, true, false, false, false, false, false, false, false, null, null)); //$NON-NLS-1$
		children.add(new PreviewFile("locked.txt", IResource.FILE, false, false, false, false, false, true, false, true, false, false, false, false, null, null)); //$NON-NLS-1$
		children.add(new PreviewFile("moved.java", IResource.FILE, false, true, false, true, false, false, false, false, false, false, false, false, null, null)); //$NON-NLS-1$
		children.add(new PreviewFile("readOnly.txt", IResource.FILE, false, false, false, false, false, true, false, false, true, false, false, false, null, null)); //$NON-NLS-1$	
		children.add(new PreviewFile("unchanged.txt", IResource.FILE, false, false, false, false, false, true, false, false, false, false, false, false, null, null)); //$NON-NLS-1$	
		children.add(new PreviewFile("unversioned.txt", IResource.FILE, false, false, true, false, false, false, false, false, false, false, false, false, null, null)); //$NON-NLS-1$
//		children.add(new PreviewFile("archive.zip", IResource.FILE, false, false, true, false, true, false, false, false, false, false, false, null, null)); //$NON-NLS-1$
		project.children= children;
		ROOT= Collections.singleton(project);
	}
	
	static {
		dirty = new CachedImageDescriptor(TeamImages.getImageDescriptor(org.eclipse.team.ui.ISharedImages.IMG_DIRTY_OVR));
		checkedIn = new CachedImageDescriptor(TeamImages.getImageDescriptor(org.eclipse.team.ui.ISharedImages.IMG_CHECKEDIN_OVR));
		added = new CachedImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_ADDED));
		moved = new CachedImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_MOVED));
		newResource = new CachedImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_QUESTIONABLE));
		external = new CachedImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_EXTERNAL));
		locked = new CachedImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_LOCKED));
		needsLock = new CachedImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_NEEDSLOCK));	
		conflicted = new CachedImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_CONFLICTED));	
		deleted = new CachedImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_DELETED));
		switched = new CachedImageDescriptor(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_SWITCHED));	
	}

	private Preview fPreview;
	
	class StringPair {
		String s1;
		String s2;
	}
	
	class TextPair {
		TextPair(Text t1, Text t2) {
			this.t1 = t1;
			this.t2 = t2;
		}
		Text t1;
		Text t2;
	}
	
	/**
	 * Constructor for SVNDecoratorPreferencesPage.
	 */
	public SVNDecoratorPreferencesPage() {
		setDescription(Policy.bind("SVNDecoratorPreferencesPage.description")); //$NON-NLS-1$;
	}

	private Button createCheckBox(Composite group, String label) {
		Button button = new Button(group, SWT.CHECK);
		button.setText(label);
		return button;
	}

	/**
	 * create the Label Decoration/general page 
	 * @param parent
	 * @return
	 */
	private Control createGeneralDecoratorPage(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		composite.setLayout(layout);
		GridData data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		composite.setLayoutData(data);
		SWTUtils.createPreferenceLink((IWorkbenchPreferenceContainer) getContainer(), composite, "org.eclipse.ui.preferencePages.Decorators", Policy.bind("SVNDecoratorPreferencesPage.labelDecorationsLink")); //$NON-NLS-1$ //$NON-NLS-2$		 		
		showDirty = createCheckBox(composite, Policy.bind("SVNDecoratorPreferencesPage.computeDeep")); //$NON-NLS-1$
		enableFontDecorators = createCheckBox(composite, Policy.bind("SVNDecoratorPreferencesPage.useFontDecorators")); //$NON-NLS-1$			
		SWTUtils.createPreferenceLink((IWorkbenchPreferenceContainer) getContainer(), composite, "org.eclipse.ui.preferencePages.ColorsAndFonts", Policy.bind("SVNDecoratorPreferencesPage.colorsAndFontsLink")); //$NON-NLS-1$ //$NON-NLS-2$		 				
		return composite;
	}

    /**
     * creates the following controls (sample)
     * File Format : [{added_flag}{dirty_flag}{name} {revision}  {date}  {author}]  [Add Variables]
     * Example : [                           ]
     * supportedBindings is a map of {key : description} (ex : {"name","name of the resource being decorated"})
     * @returns the text control for the format and the text control for the example         
     */
	protected TextPair createFormatEditorControl(
        Composite composite, 
        String title, 
        String buttonText, 
        final Map supportedBindings) {
        
        createLabel(composite, title, 1);
		
        Text format = new Text(composite, SWT.BORDER);
		format.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		format.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {				
				updateExamples();
			}
		});
		Button b = new Button(composite, SWT.NONE);
		b.setText(buttonText);
		GridData data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		int widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
		data.widthHint = Math.max(widthHint, b.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
		b.setLayoutData(data);
		final Text formatToInsert = format;
		b.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event event) {
				addVariables(formatToInsert, supportedBindings);
			}			
		});
		
		return new TextPair(format, null);
	}
	
    /**
     * updates the examples
     */
	protected void updateExamples() {
        if (fPreview != null) fPreview.refresh();
	}
	
	/**
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
				
		// create a tab folder for the page
		TabFolder tabFolder = new TabFolder(parent, SWT.NONE);
		tabFolder.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		// general decoration options
		TabItem tabItem = new TabItem(tabFolder, SWT.NONE);
		tabItem.setText(Policy.bind("SVNDecoratorPreferencesPage.generalTabFolder"));//$NON-NLS-1$
		tabItem.setControl(createGeneralDecoratorPage(tabFolder));
		
		// text decoration options
		tabItem = new TabItem(tabFolder, SWT.NONE);
		tabItem.setText(Policy.bind("SVNDecoratorPreferencesPage.textLabel"));//$NON-NLS-1$		
		tabItem.setControl(createTextDecoratorPage(tabFolder));

		// image decoration options
		tabItem = new TabItem(tabFolder, SWT.NONE);
		tabItem.setText(Policy.bind("Icon_Overlays_24"));//$NON-NLS-1$		
		tabItem.setControl(createIconDecoratorPage(tabFolder));

		initializeValues();
		
		fPreview= new Preview(parent);
		fPreview.setColorsAndFonts();
		
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.DECORATORS_PREFERENCE_PAGE);
		PlatformUI.getWorkbench().getThemeManager().addPropertyChangeListener(fThemeListener= new ThemeListener(fPreview));		
		Dialog.applyDialogFont(parent);
		return tabFolder;
	}
	
    /**
     * creates the controls for the first tab folder (Decorator page)
     */
	private Control createTextDecoratorPage(Composite parent) {
		Composite fileTextGroup = new Composite(parent, SWT.NULL);
		GridLayout	layout = new GridLayout();
		layout.numColumns = 3;
		fileTextGroup.setLayout(layout);
		GridData data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		fileTextGroup.setLayoutData(data);

		createLabel(fileTextGroup, Policy.bind("SVNDecoratorPreferencesPage.selectFormats"), 3); //$NON-NLS-1$

		TextPair format = createFormatEditorControl(fileTextGroup, 
            Policy.bind("SVNDecoratorPreferencesPage.fileFormat"),  //$NON-NLS-1$
            Policy.bind("SVNDecoratorPreferencesPage.addVariables"), getFileBindingDescriptions()); //$NON-NLS-1$ //$NON-NLS-2$
		fileTextFormat = format.t1;
        
		format = createFormatEditorControl(fileTextGroup, 
            Policy.bind("SVNDecoratorPreferencesPage.folderFormat"),  //$NON-NLS-1$
            Policy.bind("SVNDecoratorPreferencesPage.addVariables"), getFolderBindingDescriptions()); //$NON-NLS-1$ //$NON-NLS-2$
		folderTextFormat = format.t1;
        
		format = createFormatEditorControl(fileTextGroup, 
            Policy.bind("SVNDecoratorPreferencesPage.projectFormat"),  //$NON-NLS-1$
            Policy.bind("SVNDecoratorPreferencesPage.addVariables"), getProjectBindingDescriptions()); //$NON-NLS-1$ //$NON-NLS-2$
		projectTextFormat = format.t1;
		
		createLabel(fileTextGroup, Policy.bind("SVNDecoratorPreferencesPage.0"), 1); //$NON-NLS-1$
		dateFormatText = new Text(fileTextGroup, SWT.BORDER);
		data = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
		dateFormatText.setLayoutData(data);
		dateFormatText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {				
				updateExamples();
			}
		});
		new Label(fileTextGroup, SWT.NONE);

		createLabel(fileTextGroup, Policy.bind("SVNDecoratorPreferencesPage.labelDecorationOutgoing"), 1); //$NON-NLS-1$
		dirtyFlag = new Text(fileTextGroup, SWT.BORDER);
		dirtyFlag.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		dirtyFlag.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updateExamples();
			}
		});
		createLabel(fileTextGroup, "", 1); // spacer //$NON-NLS-1$

		createLabel(fileTextGroup, Policy.bind("SVNDecoratorPreferencesPage.labelDecorationAdded"), 1); //$NON-NLS-1$
		addedFlag = new Text(fileTextGroup, SWT.BORDER);
		addedFlag.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		addedFlag.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updateExamples();
			}
		});
        
        createLabel(fileTextGroup, "", 1); // spacer //$NON-NLS-1$

        createLabel(fileTextGroup, Policy.bind("SVNDecoratorPreferencesPage.labelDecorationExternal"), 1); //$NON-NLS-1$
        externalFlag = new Text(fileTextGroup, SWT.BORDER);
        externalFlag.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        externalFlag.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                updateExamples();
            }
        });

		return fileTextGroup;	
	}
    
	private Control createIconDecoratorPage(Composite parent) {
		Composite imageGroup = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		imageGroup.setLayout(layout);
		GridData data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		imageGroup.setLayoutData(data);
		
		imageShowDirty = createCheckBox(imageGroup, Policy.bind("Sho&w_outgoing_25")); //$NON-NLS-1$
		imageShowHasRemote = createCheckBox(imageGroup, Policy.bind("Show_has_&remote_26")); //$NON-NLS-1$
		imageShowAdded = createCheckBox(imageGroup, Policy.bind("S&how_is_added_moved")); //$NON-NLS-1$
		imageShowNewResource = createCheckBox(imageGroup, Policy.bind("SVNDecoratorPreferencesPage.newResources")); //$NON-NLS-1$
		imageShowExternal = createCheckBox(imageGroup, Policy.bind("SVNDecoratorPreferencesPage.externalResources")); //$NON-NLS-1$
		imageShowReadOnly = createCheckBox(imageGroup, Policy.bind("SVNDecoratorPreferencesPage.1")); //$NON-NLS-1$
		return imageGroup;
	}
	
    /**
     * creates a label
     */
	private Label createLabel(Composite parent, String text, int span) {
		Label label = new Label(parent, SWT.LEFT);
		label.setText(text);
		GridData data = new GridData();
		data.horizontalSpan = span;
		data.horizontalAlignment = GridData.FILL;
		label.setLayoutData(data);
		return label;
	}
	
	/**
	 * Initializes states of the controls from the preference store.
	 */
	private void initializeValues() {
		final IPreferenceStore store = getPreferenceStore();
		final Preferences corePreferences =  SVNProviderPlugin.getPlugin().getPluginPreferences();
		
		fileTextFormat.setText(store.getString(ISVNUIConstants.PREF_FILETEXT_DECORATION));
		folderTextFormat.setText(store.getString(ISVNUIConstants.PREF_FOLDERTEXT_DECORATION));
		projectTextFormat.setText(store.getString(ISVNUIConstants.PREF_PROJECTTEXT_DECORATION));
		
		String dateFormatPattern = store.getString(ISVNUIConstants.PREF_DATEFORMAT_DECORATION);
		if (dateFormatPattern != null) {
			dateFormatText.setText(dateFormatPattern);
		}
		
		addedFlag.setText(store.getString(ISVNUIConstants.PREF_ADDED_FLAG));
		dirtyFlag.setText(store.getString(ISVNUIConstants.PREF_DIRTY_FLAG));
        externalFlag.setText(store.getString(ISVNUIConstants.PREF_EXTERNAL_FLAG));
		
		imageShowDirty.setSelection(store.getBoolean(ISVNUIConstants.PREF_SHOW_DIRTY_DECORATION));
		imageShowAdded.setSelection(store.getBoolean(ISVNUIConstants.PREF_SHOW_ADDED_DECORATION));
		imageShowHasRemote.setSelection(store.getBoolean(ISVNUIConstants.PREF_SHOW_HASREMOTE_DECORATION));
		imageShowNewResource.setSelection(store.getBoolean(ISVNUIConstants.PREF_SHOW_NEWRESOURCE_DECORATION));
		imageShowExternal.setSelection(store.getBoolean(ISVNUIConstants.PREF_SHOW_EXTERNAL_DECORATION));
		imageShowReadOnly.setSelection(corePreferences.getBoolean(ISVNCoreConstants.PREF_SHOW_READ_ONLY));

		showDirty.setSelection(store.getBoolean(ISVNUIConstants.PREF_CALCULATE_DIRTY));
		enableFontDecorators.setSelection(store.getBoolean(ISVNUIConstants.PREF_USE_FONT_DECORATORS));
		
		SelectionListener selectionListener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fPreview.refresh();
			}			
		};
		enableFontDecorators.addSelectionListener(selectionListener);
		imageShowDirty.addSelectionListener(selectionListener);
		imageShowAdded.addSelectionListener(selectionListener);
		imageShowHasRemote.addSelectionListener(selectionListener);
		imageShowNewResource.addSelectionListener(selectionListener);
		imageShowExternal.addSelectionListener(selectionListener);
		imageShowReadOnly.addSelectionListener(selectionListener);
		
		setValid(true);
	}

	/**
	* @see IWorkbenchPreferencePage#init(IWorkbench)
	*/
	public void init(IWorkbench workbench) {
	}

	/**
	 * OK was clicked. Store the SVN preferences.
	 *
	 * @return whether it is okay to close the preference page
	 */
	public boolean performOk() {
		IPreferenceStore store = getPreferenceStore();
		Preferences corePreferences = SVNProviderPlugin.getPlugin().getPluginPreferences();
		store.setValue(ISVNUIConstants.PREF_FILETEXT_DECORATION, fileTextFormat.getText());
		store.setValue(ISVNUIConstants.PREF_FOLDERTEXT_DECORATION, folderTextFormat.getText());
		store.setValue(ISVNUIConstants.PREF_PROJECTTEXT_DECORATION, projectTextFormat.getText());
		
		store.setValue(ISVNUIConstants.PREF_DATEFORMAT_DECORATION, dateFormatText.getText());
		
		store.setValue(ISVNUIConstants.PREF_ADDED_FLAG, addedFlag.getText());
		store.setValue(ISVNUIConstants.PREF_DIRTY_FLAG, dirtyFlag.getText());
        store.setValue(ISVNUIConstants.PREF_EXTERNAL_FLAG, externalFlag.getText());
		
		store.setValue(ISVNUIConstants.PREF_SHOW_DIRTY_DECORATION, imageShowDirty.getSelection());
		store.setValue(ISVNUIConstants.PREF_SHOW_ADDED_DECORATION, imageShowAdded.getSelection());
		store.setValue(ISVNUIConstants.PREF_SHOW_HASREMOTE_DECORATION, imageShowHasRemote.getSelection());
		store.setValue(ISVNUIConstants.PREF_SHOW_NEWRESOURCE_DECORATION, imageShowNewResource.getSelection());
		store.setValue(ISVNUIConstants.PREF_SHOW_EXTERNAL_DECORATION, imageShowExternal.getSelection());
		corePreferences.setValue(ISVNCoreConstants.PREF_SHOW_READ_ONLY, imageShowReadOnly.getSelection());
		
		store.setValue(ISVNUIConstants.PREF_CALCULATE_DIRTY, showDirty.getSelection());
		store.setValue(ISVNUIConstants.PREF_USE_FONT_DECORATORS, enableFontDecorators.getSelection());
        
        // Update the strategy used to calculate the dirty state
		SVNProviderPlugin.getPlugin().getPluginPreferences().setValue(ISVNCoreConstants.PREF_RECURSIVE_STATUS_UPDATE, showDirty.getSelection());
        SVNProviderPlugin.getPlugin().savePluginPreferences();
        
		SVNLightweightDecorator.refresh();

		SVNUIPlugin.getPlugin().savePluginPreferences();
		return true;
	}

	/**
	 * Defaults was clicked. Restore the SVN preferences to
	 * their default values
	 */
	protected void performDefaults() {
		super.performDefaults();
		IPreferenceStore store = getPreferenceStore();
		Preferences corePreferences = SVNProviderPlugin.getPlugin().getPluginPreferences();
		
		fileTextFormat.setText(store.getDefaultString(ISVNUIConstants.PREF_FILETEXT_DECORATION));
		folderTextFormat.setText(store.getDefaultString(ISVNUIConstants.PREF_FOLDERTEXT_DECORATION));
		projectTextFormat.setText(store.getDefaultString(ISVNUIConstants.PREF_PROJECTTEXT_DECORATION));
		
		dateFormatText.setText(""); //$NON-NLS-1$
		
		addedFlag.setText(store.getDefaultString(ISVNUIConstants.PREF_ADDED_FLAG));
		dirtyFlag.setText(store.getDefaultString(ISVNUIConstants.PREF_DIRTY_FLAG));
        externalFlag.setText(store.getDefaultString(ISVNUIConstants.PREF_EXTERNAL_FLAG));
		
		imageShowDirty.setSelection(store.getDefaultBoolean(ISVNUIConstants.PREF_SHOW_DIRTY_DECORATION));
		imageShowAdded.setSelection(store.getDefaultBoolean(ISVNUIConstants.PREF_SHOW_ADDED_DECORATION));
		imageShowHasRemote.setSelection(store.getDefaultBoolean(ISVNUIConstants.PREF_SHOW_HASREMOTE_DECORATION));
		imageShowNewResource.setSelection(store.getDefaultBoolean(ISVNUIConstants.PREF_SHOW_NEWRESOURCE_DECORATION));
		imageShowExternal.setSelection(store.getDefaultBoolean(ISVNUIConstants.PREF_SHOW_EXTERNAL_DECORATION));
		imageShowReadOnly.setSelection(false);
		
		showDirty.setSelection(store.getDefaultBoolean(ISVNUIConstants.PREF_CALCULATE_DIRTY));
		enableFontDecorators.setSelection(store.getDefaultBoolean(ISVNUIConstants.PREF_USE_FONT_DECORATORS));
		
		setValid(true);
    }

	/**
	* Returns preference store that belongs to the our plugin.
	* This is important because we want to store
	* our preferences separately from the desktop.
	*
	* @return the preference store for this plugin
	*/
	protected IPreferenceStore doGetPreferenceStore() {
		return SVNUIPlugin.getPlugin().getPreferenceStore();
	}
	
	public void dispose() {
		PlatformUI.getWorkbench().getThemeManager().removePropertyChangeListener(fThemeListener);
		super.dispose();
	}

	/**
	 * Add another variable to the given target. The variable is inserted at current position
     * A ListSelectionDialog is shown and the choose the variables to add 
	 */
	private void addVariables(Text target, Map bindings) {
	
		final List variables = new ArrayList(bindings.size());
		
		ILabelProvider labelProvider = new LabelProvider() {
			public String getText(Object element) {
				return ((StringPair)element).s1 + " - " + ((StringPair)element).s2; //$NON-NLS-1$
			}
		};
		
		IStructuredContentProvider contentsProvider = new IStructuredContentProvider() {
			public Object[] getElements(Object inputElement) {
				return variables.toArray(new StringPair[variables.size()]);
			}
			public void dispose() {}
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
		};
		
		for (Iterator it = bindings.keySet().iterator(); it.hasNext();) {
			StringPair variable = new StringPair();
			variable.s1 = (String) it.next(); // variable
			variable.s2 = (String) bindings.get(variable.s1); // description
			variables.add(variable);				
		}
	
		ListSelectionDialog dialog =
			new ListSelectionDialog(
				this.getShell(),
				this,
				contentsProvider,
				labelProvider,
				Policy.bind("SVNDecoratorPreferencesPage.selectVariablesToAdd")); //$NON-NLS-1$
		dialog.setTitle(Policy.bind("SVNDecoratorPreferencesPage.AddVariables")); //$NON-NLS-1$
		if (dialog.open() != ListSelectionDialog.OK)
			return;
	
		Object[] result = dialog.getResult();
		
		for (int i = 0; i < result.length; i++) {
			target.insert("{"+((StringPair)result[i]).s1 +"}"); //$NON-NLS-1$ //$NON-NLS-2$
		}		
	}

    /**
     * get the map of {variable,description} to use for folders with createFormatEditorControl
     */    	
	private Map getFolderBindingDescriptions() {
		Map bindings = new HashMap();
		bindings.put(SVNDecoratorConfiguration.RESOURCE_NAME, Policy.bind("SVNDecoratorPreferencesPage.nameResourceVariable")); //$NON-NLS-1$
        bindings.put(SVNDecoratorConfiguration.RESOURCE_REVISION, Policy.bind("SVNDecoratorPreferencesPage.revisionResourceVariable")); //$NON-NLS-1$
        bindings.put(SVNDecoratorConfiguration.DIRTY_FLAG, Policy.bind("SVNDecoratorPreferencesPage.flagDirtyVariable")); //$NON-NLS-1$
        bindings.put(SVNDecoratorConfiguration.ADDED_FLAG, Policy.bind("SVNDecoratorPreferencesPage.flagAddedVariable")); //$NON-NLS-1$
		bindings.put(SVNDecoratorConfiguration.EXTERNAL_FLAG, Policy.bind("SVNDecoratorPreferencesPage.flagExternalVariable")); //$NON-NLS-1$
        bindings.put(SVNDecoratorConfiguration.RESOURCE_AUTHOR, Policy.bind("SVNDecoratorPreferencesPage.authorVariable")); //$NON-NLS-1$        
        bindings.put(SVNDecoratorConfiguration.RESOURCE_DATE, Policy.bind("SVNDecoratorPreferencesPage.dateVariable")); //$NON-NLS-1$        
		return bindings;
	}

    /**
     * get the map of {variable,description} to use for files with createFormatEditorControl
     */     
    private Map getFileBindingDescriptions() {
		Map bindings = new HashMap();
		bindings.put(SVNDecoratorConfiguration.RESOURCE_NAME, Policy.bind("SVNDecoratorPreferencesPage.nameResourceVariable")); //$NON-NLS-1$
		bindings.put(SVNDecoratorConfiguration.RESOURCE_REVISION, Policy.bind("SVNDecoratorPreferencesPage.revisionResourceVariable")); //$NON-NLS-1$
		bindings.put(SVNDecoratorConfiguration.DIRTY_FLAG, Policy.bind("SVNDecoratorPreferencesPage.flagDirtyVariable")); //$NON-NLS-1$
		bindings.put(SVNDecoratorConfiguration.ADDED_FLAG, Policy.bind("SVNDecoratorPreferencesPage.flagAddedVariable")); //$NON-NLS-1$
        bindings.put(SVNDecoratorConfiguration.RESOURCE_AUTHOR, Policy.bind("SVNDecoratorPreferencesPage.authorVariable")); //$NON-NLS-1$
        bindings.put(SVNDecoratorConfiguration.RESOURCE_DATE, Policy.bind("SVNDecoratorPreferencesPage.dateVariable")); //$NON-NLS-1$                
		return bindings;
	}

    /**
     * get the map of {variable,description} to use for projects with createFormatEditorControl
     */     
    private Map getProjectBindingDescriptions() {
        Map bindings = new HashMap();
        bindings.put(SVNDecoratorConfiguration.RESOURCE_NAME, Policy.bind("SVNDecoratorPreferencesPage.nameResourceVariable")); //$NON-NLS-1$
        bindings.put(SVNDecoratorConfiguration.DIRTY_FLAG, Policy.bind("SVNDecoratorPreferencesPage.flagDirtyVariable")); //$NON-NLS-1$
        bindings.put(SVNDecoratorConfiguration.RESOURCE_URL, Policy.bind("SVNDecoratorPreferencesPage.remoteLocationVariable")); //$NON-NLS-1$                    
        bindings.put(SVNDecoratorConfiguration.RESOURCE_URL_SHORT, Policy.bind("SVNDecoratorPreferencesPage.remoteLocationVariableShort")); //$NON-NLS-1$                    
        bindings.put(SVNDecoratorConfiguration.RESOURCE_LABEL, Policy.bind("SVNDecoratorPreferencesPage.remoteLocationLabel")); //$NON-NLS-1$                    
        bindings.put(SVNDecoratorConfiguration.RESOURCE_REVISION, Policy.bind("SVNDecoratorPreferencesPage.revisionResourceVariable")); //$NON-NLS-1$
        return bindings;
    } 
    
    private static class PreviewFile {
		public final String name;
		public final int type;
		public final boolean added, moved, dirty, hasRemote, ignored, newResource, external, locked, readOnly, conflicted, deleted, switched;
		public Collection children;
		
		public PreviewFile(String name, int type, boolean added, boolean moved, boolean newResource, boolean dirty, boolean ignored, boolean hasRemote, boolean external, boolean locked, boolean readOnly, boolean conflicted, boolean deleted, boolean switched, String mode, String tag)  {
			this.name= name;
			this.type= type;
			this.added= added;
			this.moved = moved;
			this.ignored= ignored;
			this.dirty= dirty;
			this.hasRemote= hasRemote;
			this.newResource= newResource;
			this.external = external;
			this.locked = locked;
			this.readOnly = readOnly;
			this.conflicted = conflicted;
			this.deleted = deleted;
			this.switched = switched;
			this.children= Collections.EMPTY_LIST;
		}
    }
    
	public class Preview extends LabelProvider implements Observer, ITreeContentProvider {
		
		private final ResourceManager fImageCache;
		private final TreeViewer fViewer; 

		public Preview(Composite composite) {
            SWTUtils.createLabel(composite, Policy.bind("SVNDecoratorPreferencesPage.preview"));  //$NON-NLS-1$
			fImageCache= new LocalResourceManager(JFaceResources.getResources());
			fViewer = new TreeViewer(composite);
			GridData data = SWTUtils.createHVFillGridData();
			data.heightHint = 225;
			fViewer.getControl().setLayoutData(data);
			fViewer.setContentProvider(this);
			fViewer.setLabelProvider(this);
			fViewer.setInput(ROOT);
			fViewer.expandAll();
		}
		
		public void refresh() {
			fViewer.refresh(true);
			setColorsAndFonts();
		}
		
		public void setColorsAndFonts() {
			TreeItem[] items = fViewer.getTree().getItems();
			setColorsAndFonts(items);
		}
		
		private void setColorsAndFonts(TreeItem[] items) {
			for (int i = 0; i < items.length; i++) {
				if (enableFontDecorators.getSelection()) {
					Color backGroundColor = getBackground(items[i].getData());
					items[i].setBackground(backGroundColor);
					Color foreGroundColor = getForeground(items[i].getData());
					items[i].setForeground(foreGroundColor);
					Font font = getFont(items[i].getData());
					items[i].setFont(font);
				} else {
					items[i].setBackground(null);
					items[i].setForeground(null);
					items[i].setFont(null);
				}
				setColorsAndFonts(items[i].getItems());
			}
		}
		
		public void update(Observable o, Object arg) {
			refresh();
		}
		
		public Object[] getChildren(Object parentElement) {
			return ((PreviewFile)parentElement).children.toArray();
		}

		public Object getParent(Object element) {
			return null;
		}

		public boolean hasChildren(Object element) {
			return !((PreviewFile)element).children.isEmpty();
		}

		public Object[] getElements(Object inputElement) {
			return ((Collection)inputElement).toArray();
		}

		public void dispose() {
            fImageCache.dispose();
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		public Color getBackground(Object element) {
			ITheme current = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme();
			if (((PreviewFile)element).ignored) {
				return current.getColorRegistry().get(SVNDecoratorConfiguration.IGNORED_BACKGROUND_COLOR);
			} else if (((PreviewFile)element).dirty) {
				return current.getColorRegistry().get(SVNDecoratorConfiguration.OUTGOING_CHANGE_BACKGROUND_COLOR);
			}
			return null;
		}
		
		public Color getForeground(Object element) {
			ITheme current = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme();
			if (((PreviewFile)element).ignored) {
				return current.getColorRegistry().get(SVNDecoratorConfiguration.IGNORED_FOREGROUND_COLOR);
			} else if (((PreviewFile)element).dirty) {
				return current.getColorRegistry().get(SVNDecoratorConfiguration.OUTGOING_CHANGE_FOREGROUND_COLOR);
			}
			return null;			
		}
			
		public Font getFont(Object element) {
			ITheme current = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme();
			if (((PreviewFile)element).ignored) {
				return current.getFontRegistry().get(SVNDecoratorConfiguration.IGNORED_FONT);
			} else if (((PreviewFile)element).dirty) {
				return current.getFontRegistry().get(SVNDecoratorConfiguration.OUTGOING_CHANGE_FONT);
			}
			return null;			
		}
		
		public String getText(Object element) {
			PreviewFile previewFile = (PreviewFile)element;
			
			Map bindings = new HashMap();
			  
			bindings.put(SVNDecoratorConfiguration.RESOURCE_REVISION, "74"); //$NON-NLS-1$
	        bindings.put(SVNDecoratorConfiguration.RESOURCE_AUTHOR, "cchab"); //$NON-NLS-1$
	        
	        DateFormat dateFormat;
	        if (dateFormatText.getText().trim().length() == 0) {
	        	dateFormat = SimpleDateFormat.getInstance();
	        }
	        else {
	        	try {
	        		dateFormat = new SimpleDateFormat(dateFormatText.getText());
	        		setValid(true);
	        		setMessage(null);
	        	} catch (Exception e) {
	        		dateFormat = SimpleDateFormat.getInstance();
	        		setValid(false);
	        	}
	        }
	        
	        bindings.put(SVNDecoratorConfiguration.RESOURCE_DATE, dateFormat.format(new Date())); //$NON-NLS-1$
	        bindings.put(SVNDecoratorConfiguration.RESOURCE_URL, "http://localhost:8080/svn/repos/trunk/project1"); //$NON-NLS-1$
	        bindings.put(SVNDecoratorConfiguration.RESOURCE_URL_SHORT, "trunk/project1"); //$NON-NLS-1$
	        bindings.put(SVNDecoratorConfiguration.RESOURCE_LABEL, "label"); //$NON-NLS-1$
			if (previewFile.dirty) bindings.put(SVNDecoratorConfiguration.DIRTY_FLAG, dirtyFlag.getText());
			if (previewFile.added) bindings.put(SVNDecoratorConfiguration.ADDED_FLAG, addedFlag.getText());
			if (previewFile.external) bindings.put(SVNDecoratorConfiguration.EXTERNAL_FLAG, externalFlag.getText());
			
			if (previewFile.type == IResource.FILE)
				return SVNDecoratorConfiguration.decorate(previewFile.name, fileTextFormat.getText(), bindings); //$NON-NLS-1$
			else if (previewFile.type == IResource.FOLDER)
	            return SVNDecoratorConfiguration.decorate(previewFile.name, folderTextFormat.getText(), bindings); //$NON-NLS-1$
			else if (previewFile.type == IResource.PROJECT)
	            return SVNDecoratorConfiguration.decorate(previewFile.name, projectTextFormat.getText(), bindings);                    //$NON-NLS-1$
			else return previewFile.name;
		}

		public ImageDescriptor getOverlay(Object element) {
			PreviewFile previewFile = (PreviewFile)element;
			if (imageShowNewResource.getSelection() && previewFile.newResource) return newResource;
			if (imageShowAdded.getSelection() && previewFile.added) return added;
			if (imageShowAdded.getSelection() && previewFile.moved) return moved;
			if (imageShowDirty.getSelection() && previewFile.dirty) return dirty;
			if (imageShowExternal.getSelection() && previewFile.external) return external;
			if (previewFile.locked) return locked;
			if (imageShowReadOnly.getSelection() && previewFile.readOnly) return needsLock;
			if (previewFile.conflicted) return conflicted;
			if (previewFile.deleted) return deleted;
			if (previewFile.switched) return switched;			
			if (imageShowHasRemote.getSelection() && previewFile.hasRemote) return checkedIn;
			return null;
		}
		
		public Image getImage(Object element) {
			final String s;
			switch (((PreviewFile)element).type) {
			case IResource.PROJECT:
				s= SharedImages.IMG_OBJ_PROJECT; break;
			case IResource.FOLDER:
				s= ISharedImages.IMG_OBJ_FOLDER; break;
			default:
				s= ISharedImages.IMG_OBJ_FILE; break;
			}
			final Image baseImage= PlatformUI.getWorkbench().getSharedImages().getImage(s);
			final ImageDescriptor overlay = getOverlay(element);
			if (overlay == null)
				return baseImage;
			try {
                return fImageCache.createImage(new OverlayIcon(baseImage, new ImageDescriptor[] {overlay}, new int[] {OverlayIcon.BOTTOM_RIGHT}, new Point(baseImage.getBounds().width, baseImage.getBounds().height)));
            } catch (Exception e) {
            	SVNUIPlugin.log(e.getMessage());
            }
            return null;
		}
	}
	
	private static class ThemeListener implements IPropertyChangeListener {

		private final Preview preview;
		
		ThemeListener(Preview preview) {
			this.preview = preview;
		}
		public void propertyChange(PropertyChangeEvent event) {
			preview.refresh();
		}
	}
    
}
