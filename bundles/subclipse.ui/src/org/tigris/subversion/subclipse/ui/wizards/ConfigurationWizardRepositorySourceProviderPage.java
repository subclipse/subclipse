package org.tigris.subversion.subclipse.ui.wizards;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.tigris.subversion.subclipse.ui.ISVNRepositorySourceProvider;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;

public class ConfigurationWizardRepositorySourceProviderPage extends WizardPage {
	private ISVNRepositorySourceProvider[] repositorySourceProviders;
	private TreeViewer treeViewer;
	private ISVNRepositorySourceProvider selectedRepositorySourceProvider;
	
	public final static String REPOSITORY_PROVIDERS_WIKI_URL = "http://subclipse.tigris.org/wiki/RepositoryProviders";

	public ConfigurationWizardRepositorySourceProviderPage(String pageName, String title, ImageDescriptor titleImage, ISVNRepositorySourceProvider[] repositorySourceProviders) {
		super(pageName, title, titleImage);
		this.repositorySourceProviders = repositorySourceProviders;
		setPageComplete(true);
	}

	public void createControl(Composite parent) {
		Composite outerContainer = new Composite(parent,SWT.NONE);
		outerContainer.setLayout(new GridLayout());
		outerContainer.setLayoutData(
		new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		
		Composite treeGroup = new Composite(outerContainer, SWT.NONE);
		GridLayout treeLayout = new GridLayout();
		treeLayout.numColumns = 1;
		treeGroup.setLayout(treeLayout);
		treeGroup.setLayoutData(
		new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));

		treeViewer = new TreeViewer(treeGroup, SWT.BORDER | SWT.SINGLE);
		treeViewer.setLabelProvider(new RepositorySourceLabelProvider());	
		treeViewer.setContentProvider(new RepositorySourceContentProvider());
		treeViewer.setUseHashlookup(true);
		GridData layoutData = new GridData();
		layoutData.grabExcessHorizontalSpace = true;
		layoutData.grabExcessVerticalSpace = true;
		layoutData.horizontalAlignment = GridData.FILL;
		layoutData.verticalAlignment = GridData.FILL;
		layoutData.minimumHeight = 300;
		layoutData.minimumWidth = 300;
		treeViewer.getControl().setLayoutData(layoutData);
		treeViewer.setInput(this);
		
		treeViewer.setSelection(new StructuredSelection("URL"));
		
		treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				Object selectedObject = ((IStructuredSelection)treeViewer.getSelection()).getFirstElement();
				if (selectedObject instanceof ISVNRepositorySourceProvider) {
					selectedRepositorySourceProvider = (ISVNRepositorySourceProvider)selectedObject;
				}
				else {
					selectedRepositorySourceProvider = null;
				}
				setPageComplete(!treeViewer.getSelection().isEmpty());
			}		
		});
		
		Hyperlink repositoryProviderLink = new Hyperlink(treeGroup, SWT.NONE);
		repositoryProviderLink.setUnderlined(true);
		repositoryProviderLink.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_BLUE));
		repositoryProviderLink.setText("Click here to see the list of available providers.");
		repositoryProviderLink.setToolTipText(REPOSITORY_PROVIDERS_WIKI_URL);
		repositoryProviderLink.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent evt) {
				showAvailableProviders();		
			}
		});
		
        Composite cloudForgeComposite = new CloudForgeComposite(outerContainer, SWT.NONE);
        GridData data = new GridData(GridData.VERTICAL_ALIGN_END | GridData.GRAB_VERTICAL | GridData.FILL_VERTICAL);
        cloudForgeComposite.setLayoutData(data);

		setControl(outerContainer);
	}
	
	public ISVNRepositorySourceProvider getSelectedRepositorySourceProvider() {
		return selectedRepositorySourceProvider;
	}
	
	public static void showAvailableProviders() {
		try {
			PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(REPOSITORY_PROVIDERS_WIKI_URL));
		} catch (Exception e) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Available Repository Providers", e.getMessage());
		}		
	}

	class RepositorySourceLabelProvider extends LabelProvider {
		public Image getImage(Object element) {
			if (element instanceof ISVNRepositorySourceProvider) {
				return ((ISVNRepositorySourceProvider)element).getImage();
			}
			else {
				return SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_URL_SOURCE_REPO).createImage();
			}
		}	
		public String getText(Object element) {
			if (element instanceof ISVNRepositorySourceProvider) {
				return ((ISVNRepositorySourceProvider)element).getName();
			}
			else {
				return element.toString();
			}
		}
	}
	
	private class RepositorySourceContentProvider implements ITreeContentProvider {
		public void dispose() {}
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}

		public Object[] getElements(Object inputElement) {
			return getChildren(inputElement);
		}

		public Object[] getChildren(final Object parentElement) {
			List<Object> children = new ArrayList<Object>();
			if (parentElement == ConfigurationWizardRepositorySourceProviderPage.this) {
				children.add("URL");
				for (ISVNRepositorySourceProvider repositorySourceProvider : repositorySourceProviders) {
					children.add(repositorySourceProvider);
				}
			}
			return children.toArray();
		}

		public Object getParent(Object element) {
			return null;
		}

		public boolean hasChildren(Object element) {
			return false;
		}
		
	}

}
