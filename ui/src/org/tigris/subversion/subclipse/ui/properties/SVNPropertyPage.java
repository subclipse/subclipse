package org.tigris.subversion.subclipse.ui.properties;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.ui.dialogs.PropertyPage;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.svnclientadapter.ISVNStatus;

public class SVNPropertyPage extends PropertyPage {

	private static final int TEXT_FIELD_WIDTH = 50;

	private Text ignoredTextField;
	private Text managedTextField;
	private Text hasRemoteTextField;
	private Text urlTextField;
	private Text lastChangedRevisionTextField;
	private Text lastChangedDateTextField;
	private Text lastCommitAuthorTextField;
	private Text textStatusTextField;
	private Text mergedTextField;
	private Text deletedTextField;
	private Text modifiedTextField;
	private Text addedTextField;
	private Text revisionTextField;
	private Text copiedTextField;
	private Text pathTextField;
	private Text urlCopiedFromTextField;	

	public SVNPropertyPage() {
		super();
	}

	private void addFirstSection(Composite parent) {
		Composite composite = createDefaultComposite(parent);

		//Label for path field
		Label pathLabel = new Label(composite, SWT.NONE);
		pathLabel.setText("Path");

		// Path text field
		Text pathValueText = new Text(composite, SWT.WRAP | SWT.READ_ONLY);
		pathValueText.setText(((IResource) getElement()).getFullPath().toString());
	}

	private void addSeparator(Composite parent) {
		Label separator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
		GridData gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		separator.setLayoutData(gridData);
	}

	private void addSecondSection(Composite parent) {
		Composite composite = createDefaultComposite(parent);

		GridData gd = new GridData();
		gd.widthHint = convertWidthInCharsToPixels(TEXT_FIELD_WIDTH);
				
		Label pathLabel = new Label(composite, SWT.NONE);
		pathLabel.setText("Ignored");		
		ignoredTextField = new Text(composite, SWT.WRAP | SWT.READ_ONLY);
		
		pathLabel = new Label(composite, SWT.NONE);
		pathLabel.setText("Managed");		
		managedTextField = new Text(composite, SWT.WRAP | SWT.READ_ONLY);

		pathLabel = new Label(composite, SWT.NONE);
		pathLabel.setText("Has Remote");		
		hasRemoteTextField = new Text(composite, SWT.WRAP | SWT.READ_ONLY);

		pathLabel = new Label(composite, SWT.NONE);
		pathLabel.setText("URL");		
		urlTextField = new Text(composite, SWT.WRAP | SWT.READ_ONLY);

		pathLabel = new Label(composite, SWT.NONE);
		pathLabel.setText("Last Changed Revision");		
		lastChangedRevisionTextField = new Text(composite, SWT.WRAP | SWT.READ_ONLY);

		pathLabel = new Label(composite, SWT.NONE);
		pathLabel.setText("Last Changed Date");		
		lastChangedDateTextField = new Text(composite, SWT.WRAP | SWT.READ_ONLY);

		pathLabel = new Label(composite, SWT.NONE);
		pathLabel.setText("Last Commit Author");		
		lastCommitAuthorTextField = new Text(composite, SWT.WRAP | SWT.READ_ONLY);

		pathLabel = new Label(composite, SWT.NONE);
		pathLabel.setText("Status");		
		textStatusTextField = new Text(composite, SWT.WRAP | SWT.READ_ONLY);

		pathLabel = new Label(composite, SWT.NONE);
		pathLabel.setText("Merged");		
		mergedTextField = new Text(composite, SWT.WRAP | SWT.READ_ONLY);

		pathLabel = new Label(composite, SWT.NONE);
		pathLabel.setText("Deleted");		
		deletedTextField = new Text(composite, SWT.WRAP | SWT.READ_ONLY);

		pathLabel = new Label(composite, SWT.NONE);
		pathLabel.setText("Modified");		
		modifiedTextField = new Text(composite, SWT.WRAP | SWT.READ_ONLY);

		pathLabel = new Label(composite, SWT.NONE);
		pathLabel.setText("Added");		
		addedTextField = new Text(composite, SWT.WRAP | SWT.READ_ONLY);

		pathLabel = new Label(composite, SWT.NONE);
		pathLabel.setText("Revision");		
		revisionTextField = new Text(composite, SWT.WRAP | SWT.READ_ONLY);

		pathLabel = new Label(composite, SWT.NONE);
		pathLabel.setText("Copied");		
		copiedTextField = new Text(composite, SWT.WRAP | SWT.READ_ONLY);

		pathLabel = new Label(composite, SWT.NONE);
		pathLabel.setText("Path");		
		pathTextField = new Text(composite, SWT.WRAP | SWT.READ_ONLY);

		pathLabel = new Label(composite, SWT.NONE);
		pathLabel.setText("URL Copied From");		
		urlCopiedFromTextField = new Text(composite, SWT.WRAP | SWT.READ_ONLY);

		// Populate owner text field
		try {
			IResource resource = (IResource) getElement();
			SVNTeamProvider svnProvider = (SVNTeamProvider)RepositoryProvider.getProvider(resource.getProject(), SVNProviderPlugin.getTypeId());
			if (svnProvider == null)
				return;

			ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
			if(svnResource == null)
				return;

			ISVNStatus status = svnResource.getStatus();

			ignoredTextField.setText(new Boolean(status.isIgnored()).toString());		
			managedTextField.setText(new Boolean(status.isManaged()).toString());
			hasRemoteTextField.setText(new Boolean(status.isIgnored()).toString());
			urlTextField.setText(status.getUrl() != null? status.getUrl().toString():"");
			lastChangedRevisionTextField.setText(status.getLastChangedRevision() != null? status.getLastChangedRevision().toString():"");
			lastChangedDateTextField.setText(status.getLastChangedDate() != null? status.getLastChangedDate().toString():"");
			lastCommitAuthorTextField.setText(status.getLastCommitAuthor() != null? status.getLastCommitAuthor():"");
			textStatusTextField.setText(status.getTextStatus() != null? status.getTextStatus().toString():"");
			mergedTextField.setText(new Boolean(status.isMerged()).toString());
			deletedTextField.setText(new Boolean(status.isDeleted()).toString());
			modifiedTextField.setText(new Boolean(status.isModified()).toString());
			addedTextField.setText(new Boolean(status.isAdded()).toString());
			revisionTextField.setText(status.getRevision() != null? status.getRevision().toString():"");
			copiedTextField.setText(new Boolean(status.isCopied()).toString());
			pathTextField.setText(status.getPath() != null? status.getPath():"");
			urlCopiedFromTextField.setText(status.getUrlCopiedFrom() != null? status.getUrlCopiedFrom():"");			
			
		} catch (Exception e) {
			SVNUIPlugin.log(new Status(Status.ERROR, SVNUIPlugin.ID, TeamException.UNABLE, "Property Exception", e));
		}
	}

	/**
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL);
		data.grabExcessHorizontalSpace = true;
		composite.setLayoutData(data);

		addFirstSection(composite);
		addSeparator(composite);
		addSecondSection(composite);
		return composite;
	}

	private Composite createDefaultComposite(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		composite.setLayout(layout);

		GridData data = new GridData();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		composite.setLayoutData(data);

		return composite;
	}

	protected void performDefaults() {
	}

	public boolean performOk() {
		return true;
	}

}
