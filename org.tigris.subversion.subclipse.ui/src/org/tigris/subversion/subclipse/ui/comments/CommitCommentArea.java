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
package org.tigris.subversion.subclipse.ui.comments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.DeviceResourceException;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.text.DefaultTextHover;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.MarginPainter;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.MonoReconciler;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.text.source.AnnotationPainter;
import org.eclipse.jface.text.source.AnnotationRulerColumn;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.IAnnotationAccess;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.internal.ui.SWTUtils;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AnnotationPreference;
import org.eclipse.ui.texteditor.DefaultMarkerAnnotationAccess;
import org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector;
import org.eclipse.ui.texteditor.spelling.SpellingContext;
import org.eclipse.ui.texteditor.spelling.SpellingProblem;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.util.Util;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.dialogs.DialogArea;
import org.tigris.subversion.subclipse.ui.settings.CommentProperties;

/**
 * This area provides the widgets for providing the SVN commit comment
 */
public class CommitCommentArea extends DialogArea {
	private boolean showLabel = true;;
    
    public static final String SPELLING_ERROR = "spelling.error"; //$NON-NLS-1$
    

    private class TextBox implements ModifyListener, TraverseListener, FocusListener, Observer {
        
        private final StyledText fTextField; // updated only by modify events
        private final String fMessage;
        
        private String fText;
		private LocalResourceManager fResources;
        
        public TextBox(Composite composite, String message, String initialText) {
            
            fMessage= message;
            fText= initialText;
            // Create a resource manager for the composite so it gets automatically disposed
            fResources= new LocalResourceManager(JFaceResources.getResources(), composite);
            
            AnnotationModel annotationModel = new AnnotationModel();
            IAnnotationAccess annotationAccess = new DefaultMarkerAnnotationAccess();
            
            AnnotationRulerColumn annotationRuler = new AnnotationRulerColumn(annotationModel, 16, annotationAccess);

            CompositeRuler compositeRuler = new CompositeRuler();
            compositeRuler.setModel(annotationModel);
            compositeRuler.addDecorator(0, annotationRuler);

            Composite cc = new Composite(composite, SWT.BORDER);
            cc.setLayout(new FillLayout());
            cc.setLayoutData(new GridData(GridData.FILL_BOTH));
            
            SourceViewer sourceViewer = new SourceViewer(cc, compositeRuler, null, true,
                SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
            
            if (modifyListener != null) {
            	sourceViewer.getTextWidget().addModifyListener(modifyListener);
            }

            // TODO should be done in the source viewer configuration
            Font commentFont = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getFontRegistry().get(ISVNUIConstants.SVN_COMMENT_FONT);
            if (commentFont != null) sourceViewer.getTextWidget().setFont(commentFont);
            
    		int widthMarker = 0;
    		if (commentProperties != null) widthMarker = commentProperties.getLogWidthMarker();
    		
    		if (widthMarker > 0) {
    			MarginPainter marginPainter = new MarginPainter(sourceViewer);
                marginPainter.setMarginRulerColumn(widthMarker);
                marginPainter.setMarginRulerColor(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY));
                sourceViewer.addPainter(marginPainter);
    		}
            
            
            sourceViewer.showAnnotations(false);
            sourceViewer.showAnnotationsOverview(false);

            if (isSpellingAnnotationEnabled()) {
	            // to paint the annotations
	            AnnotationPainter ap = new AnnotationPainter(sourceViewer, annotationAccess);
	            ap.addAnnotationType(SPELLING_ERROR);
	            ap.setAnnotationTypeColor(SPELLING_ERROR, getSpellingErrorColor(composite));
	
	            // this will draw the squiggles under the text
	            sourceViewer.addPainter(ap);
            }

            Document document = new Document(initialText);

            // NOTE: Configuration must be applied before the document is set in order for
            // Hyperlink coloring to work. (Presenter needs document object up front)
            sourceViewer.configure(new SourceViewerConfig(annotationModel, document));
            
            sourceViewer.setDocument(document, annotationModel);
            
            fTextField = sourceViewer.getTextWidget();
            
            fTextField.addTraverseListener(this);
            fTextField.addModifyListener(this);
            fTextField.addFocusListener(this);
            fTextField.setWordWrap(mustWrapWord());
            
    		MenuManager menuManager = new MenuManager();
    		IMenuListener listener = new IMenuListener() {		
    			public void menuAboutToShow(IMenuManager manager) {
    				if (fTextField.getSelectionText() != null && fTextField.getSelectionText().length() > 0) {
    					Action cutAction = new Action("Cut") {
    						public void run() {
    							fTextField.cut();
    						}
    					};
    					manager.add(cutAction);
    					Action copyAction = new Action("Copy") {
    						public void run() {
    							fTextField.copy();
    						}
    					};
    					manager.add(copyAction);
    				}
    				Clipboard clipboard = new Clipboard(Display.getCurrent());
    				TextTransfer textTransfer = TextTransfer.getInstance();
    				final Object contents = clipboard.getContents(textTransfer);
    				if (contents instanceof String && ((String)contents).length() > 0) {
        				Action pasteAction = new Action("Paste") {
        					public void run() {
        						fTextField.insert((String)contents);
        					}
        				};
        				manager.add(pasteAction);
    				}
    				Action selectAllAction = new Action("Select All") {
    					public void run() {
    						fTextField.selectAll();
    					}
    				};
    				manager.add(selectAllAction);
    			}
    		};
    		menuManager.addMenuListener(listener);
    		menuManager.setRemoveAllWhenShown(true);
    		Menu menu = menuManager.createContextMenu(fTextField);
    		fTextField.setMenu(menu);
        }

        private boolean mustWrapWord() {
			if (commentProperties != null && commentProperties.getLogWidthMarker() > 0)	{
				return false;
			}
			return true;
		}
	
        private boolean isSpellingAnnotationEnabled() {
			// Need to determine how to ask the proper question to the AnnotationPreferences
			return true;
		}

		private Color getSpellingErrorColor(Composite composite) {
			AnnotationPreference pref = EditorsUI
					.getAnnotationPreferenceLookup().getAnnotationPreference(
							"org.eclipse.ui.workbench.texteditor.spelling"); // $NON-NLS-1$
			String preferenceKey = pref.getColorPreferenceKey();
			try {
				return fResources.createColor(PreferenceConverter.getColor(EditorsUI.getPreferenceStore(), preferenceKey));
			} catch (DeviceResourceException e) {
				SVNUIPlugin.log(IStatus.ERROR, Policy.bind("internal"), e); //$NON-NLS-1$
				return JFaceColors.getErrorText(composite.getDisplay());
			}
		}
        
        public void modifyText(ModifyEvent e) {
            final String old = fText;
            fText = fTextField.getText();
            firePropertyChangeChange(COMMENT_MODIFIED, old, fText);
        }
        
        public void keyTraversed(TraverseEvent e) {
        	if (e.detail == SWT.TRAVERSE_RETURN && (e.stateMask & SWT.SHIFT) != 0) {
	       		e.doit = false;
	       		return;
        	}
            if (e.detail == SWT.TRAVERSE_RETURN && (e.stateMask & SWT.CTRL) != 0) {
                e.doit = false;
                firePropertyChangeChange(OK_REQUESTED, null, null);
            }
        }
        
        public void focusGained(FocusEvent e) {

            if (fText.length() > 0) 
                return;
            
            fTextField.removeModifyListener(this);
            try {
                fTextField.setText(fText);
            } finally {
                fTextField.addModifyListener(this);
            }
        }
        
        public void focusLost(FocusEvent e) {
            
            if (fText.length() > 0) 
                return;
            
            fTextField.removeModifyListener(this);
            try {
                fTextField.setText(fMessage);
                fTextField.selectAll();
            } finally {
                fTextField.addModifyListener(this);
            }
        }
        
        public void setEnabled(boolean enabled) {
            fTextField.setEnabled(enabled);
        }
        
        public void update(Observable o, Object arg) {
            if (arg instanceof String) {
                setText((String)arg); // triggers a modify event
                if (modifyListener != null) modifyListener.modifyText(null);
            }
        }
        
        public String getText() {
            return fText;
        }
        
        public int getCommentLength() {
        	if (fTextField == null) return 0;
        	if (fTextField.getText().equals(Policy.bind("CommitCommentArea_0"))) return 0; //$NON-NLS-1$
        	return fTextField.getText().trim().length();
        }
        
        private void setText(String text) {
            if (text.length() == 0) {
                fTextField.setText(fMessage);
                fTextField.selectAll();
            } else
                fTextField.setText(text);
        }

        public void setFocus() {
            fTextField.setFocus();
        }
    }
    
    public class SourceViewerConfig extends SourceViewerConfiguration {

		private CommentSpellingReconcileStrategy strategy;

		public SourceViewerConfig(AnnotationModel annotationModel,
				Document document) {
			strategy = new CommentSpellingReconcileStrategy(annotationModel);
			strategy.setDocument(document);
		}

		public IReconciler getReconciler(ISourceViewer sourceViewer) {
			MonoReconciler reconciler = new MonoReconciler(strategy, false);
			reconciler.setIsIncrementalReconciler(false);
			reconciler.setProgressMonitor(new NullProgressMonitor());
			reconciler.setDelay(200);
			return reconciler;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getTextHover(org.eclipse.jface.text.source.ISourceViewer, java.lang.String)
		 */
		public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
			return new DefaultTextHover(sourceViewer);
		}

	}
    
    public class CommentSpellingReconcileStrategy implements IReconcilingStrategy {


      /** The document to operate on. */
      private IDocument fDocument;

      private SpellingContext fSpellingContext;

      private IAnnotationModel fAnnotationModel;


      public CommentSpellingReconcileStrategy(AnnotationModel annotationModel) {
        this.fAnnotationModel = annotationModel;
        fSpellingContext = new SpellingContext();
        fSpellingContext.setContentType(Platform.getContentTypeManager().getContentType(IContentTypeManager.CT_TEXT));
      }

      public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion) {
        reconcile(subRegion);
      }

      public void reconcile(IRegion region) {
        SpellingProblemCollector collector = new SpellingProblemCollector(fAnnotationModel);
        EditorsUI.getSpellingService().check(fDocument, fSpellingContext, collector, null);
      }

      public void setDocument(IDocument document) {
        fDocument = document;
      }

      
      /**
       * Spelling problem collector that forwards {@link SpellingProblem}s as
       * {@link IProblem}s to the {@link org.eclipse.jdt.core.IProblemRequestor}.
       */
      private class SpellingProblemCollector implements ISpellingProblemCollector {

        /** Annotation model */
        private IAnnotationModel fAnnotationModel;

        /** Annotations to add <ErrorAnnotation, Position> */
        private Map fAddAnnotations;

        /**
         * Initializes this collector with the given annotation model.
         * 
         * @param annotationModel
         *          the annotation model
         */
        public SpellingProblemCollector(IAnnotationModel annotationModel) {
          fAnnotationModel = annotationModel;
        }

        /*
         * @see org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector#accept(org.eclipse.ui.texteditor.spelling.SpellingProblem)
         */
        public void accept(SpellingProblem problem) {
          fAddAnnotations.put(new Annotation(SPELLING_ERROR, false, problem.getMessage()), 
              new Position(problem.getOffset(), problem.getLength()));
        }

        /*
         * @see org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector#beginCollecting()
         */
        public void beginCollecting() {
          fAddAnnotations = new HashMap();
        }

        /*
         * @see org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector#endCollecting()
         */
        public void endCollecting() {
          List removeAnnotations = new ArrayList();
          for(Iterator iter = fAnnotationModel.getAnnotationIterator(); iter.hasNext();) {
            Annotation annotation = (Annotation) iter.next();
            if(SPELLING_ERROR.equals(annotation.getType()))
              removeAnnotations.add(annotation);
          }

          for(Iterator iter = removeAnnotations.iterator(); iter.hasNext();)
            fAnnotationModel.removeAnnotation((Annotation) iter.next());
          for(Iterator iter = fAddAnnotations.keySet().iterator(); iter.hasNext();) {
            Annotation annotation = (Annotation) iter.next();
            fAnnotationModel.addAnnotation(annotation, (Position) fAddAnnotations.get(annotation));
          }

          fAddAnnotations = null;
        }
      }
      
    }


    private static class ComboBox extends Observable implements SelectionListener, FocusListener {
        
        private final String fMessage;
        private final String [] fComments;
        private String[] fCommentTemplates;
        private final Combo fCombo;
        
        
        public ComboBox(Composite composite, String message, String [] options,
                String[] commentTemplates) {
            
            fMessage= message;
            fComments= options;
            fCommentTemplates = commentTemplates;
            
            fCombo = new Combo(composite, SWT.READ_ONLY);
            fCombo.setLayoutData(SWTUtils.createHFillGridData());
            fCombo.setVisibleItemCount(20);
            
            // populate the previous comment list
            populateList();
            
            // We don't want to have an initial selection
            // (see bug 32078: http://bugs.eclipse.org/bugs/show_bug.cgi?id=32078)
            fCombo.addFocusListener(this);
            fCombo.addSelectionListener(this);
        }

		private void populateList() {
			fCombo.removeAll();
			
			fCombo.add(fMessage);
            for (int i = 0; i < fCommentTemplates.length; i++) {
                fCombo.add(Policy.bind("CommitCommentArea_6") + ": " + //$NON-NLS-1$
                		Util.flattenText(fCommentTemplates[i]));
            }
            for (int i = 0; i < fComments.length; i++) {
                fCombo.add(Util.flattenText(fComments[i]));
            }
            fCombo.setText(fMessage);
		}
        
        public void widgetSelected(SelectionEvent e) {
            int index = fCombo.getSelectionIndex();
            if (index > 0) {
                index--;
                setChanged();
                
                // map from combo box index to array index
                String message;
                if (index < fCommentTemplates.length) {
                	message = fCommentTemplates[index];
                } else {
                	message = fComments[index - fCommentTemplates.length];
                }
                notifyObservers(message);
            }
        }
        
        public void widgetDefaultSelected(SelectionEvent e) {
        }
        
        public void focusGained(FocusEvent e) {
        }
        
        /* (non-Javadoc)
         * @see org.eclipse.swt.events.FocusListener#focusLost(org.eclipse.swt.events.FocusEvent)
         */
        public void focusLost(FocusEvent e) {
            fCombo.removeSelectionListener(this);
            try {
                fCombo.setText(fMessage);
            } finally {
                fCombo.addSelectionListener(this);
            }
        }
        
        public void setEnabled(boolean enabled) {
            fCombo.setEnabled(enabled);
        }
        
        void setCommentTemplates(String[] templates) {
			fCommentTemplates = templates;
			populateList();
		}
    }
    
    private static final String EMPTY_MESSAGE= Policy.bind("CommitCommentArea_0"); 
    private static final String COMBO_MESSAGE= Policy.bind("CommitCommentArea_1");
    
    public static final String OK_REQUESTED = "OkRequested";//$NON-NLS-1$
    public static final String COMMENT_MODIFIED = "CommentModified";//$NON-NLS-1$
    
    private TextBox fTextBox;
    private ComboBox fComboBox;
    
    private String fProposedComment;
    private Composite fComposite;
	private String enterCommentMessage;
	
	private CommentProperties commentProperties;
	private ModifyListener modifyListener;
	
    /**
	 * Constructor for CommitCommentArea.
	 * @param parentDialog
	 * @param settings
	 */
	public CommitCommentArea(Dialog parentDialog, IDialogSettings settings) {
		super(parentDialog, settings);
	}
	
	public CommitCommentArea(Dialog parentDialog, IDialogSettings settings, CommentProperties commentProperties) {
		this(parentDialog, settings);
		this.commentProperties = commentProperties;
	}
	
	/**
	 * Constructor for CommitCommentArea.
	 * @param parentDialog
	 * @param settings
	 * @param enterCommentMessage
	 */
	public CommitCommentArea(Dialog parentDialog, IDialogSettings settings, String enterCommentMessage) {
		this(parentDialog, settings);
		this.enterCommentMessage = enterCommentMessage;
	}
	
	public CommitCommentArea(Dialog parentDialog, IDialogSettings settings, String enterCommentMessage, CommentProperties commentProperties) {
		this(parentDialog, settings, enterCommentMessage);
		this.commentProperties = commentProperties;
	}	

    
    public Control createArea(Composite parent) {
        fComposite = createGrabbingComposite(parent, 1);
        initializeDialogUnits(fComposite);
        
        if (showLabel) {
			Label label = new Label(fComposite, SWT.NULL);
			label.setLayoutData(new GridData());
			if (enterCommentMessage == null) label.setText(Policy.bind("ReleaseCommentDialog.enterComment")); //$NON-NLS-1$
			else label.setText(enterCommentMessage);
        }
		
        fTextBox= new TextBox(fComposite, EMPTY_MESSAGE, getInitialComment());
        
        final String [] comments = SVNUIPlugin.getPlugin().getRepositoryManager().getCommentsManager().getPreviousComments();
        final String[] commentTemplates = SVNUIPlugin.getPlugin().getRepositoryManager().getCommentsManager().getCommentTemplates();
        fComboBox= new ComboBox(fComposite, COMBO_MESSAGE, comments, commentTemplates);
        
        Link templatesPrefsLink = new Link(fComposite, 0);
        templatesPrefsLink.setText("<a href=\"configureTemplates\">Configure Comment Templates...</a>"); //$NON-NLS-1$
        templatesPrefsLink.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				openCommentTemplatesPreferencePage();
			}
		
			public void widgetSelected(SelectionEvent e) {
				openCommentTemplatesPreferencePage();
			}
		});
        
        fComboBox.addObserver(fTextBox);
        return fComposite;
    }
    
    void openCommentTemplatesPreferencePage() {
		PreferencesUtil.createPreferenceDialogOn(
				null,
				"org.tigris.subversion.subclipse.ui.CommentTemplatesPreferences", //$NON-NLS-1$
				new String[] { "org.tigris.subversion.subclipse.ui.CommentTemplatesPreferences" }, //$NON-NLS-1$
				null).open();
		fComboBox.setCommentTemplates(
				SVNUIPlugin.getPlugin().getRepositoryManager().getCommentsManager().getCommentTemplates());
	}

    public String getComment() {
    	return getComment(false);
    }
    
	public String getComment(boolean save) {
        final String comment= fTextBox.getText();
        if (comment == null)
            return ""; //$NON-NLS-1$
        if (save) addComment(comment);
        return comment;
    }
	
	public void addComment(String comment) {
		if (comment != null && comment.trim().length() > 0) SVNUIPlugin.getPlugin().getRepositoryManager().getCommentsManager().addComment(comment);
	}
    
    public String getCommentWithPrompt(Shell shell) {
        final String comment= getComment(false);
        if (comment.length() == 0) {
            final IPreferenceStore store= SVNUIPlugin.getPlugin().getPreferenceStore();
            final String value= store.getString(ISVNUIConstants.PREF_ALLOW_EMPTY_COMMIT_COMMENTS);
            
            if (MessageDialogWithToggle.NEVER.equals(value))
                return null;
            
            if (MessageDialogWithToggle.PROMPT.equals(value)) {
                
                final String title= Policy.bind("CommitCommentArea_2"); 
                final String message= Policy.bind("CommitCommentArea_3"); 
                final String toggleMessage= Policy.bind("CommitCommentArea_4"); 
                
                final MessageDialogWithToggle dialog= MessageDialogWithToggle.openYesNoQuestion(shell, title, message, toggleMessage, false, store, ISVNUIConstants.PREF_ALLOW_EMPTY_COMMIT_COMMENTS);
                if (dialog.getReturnCode() != IDialogConstants.YES_ID) {
                    fTextBox.setFocus();
                    return null;
                }
            }
        }
        return getComment(true);
    }

    public void setFocus() {
        if (fTextBox != null) {
            fTextBox.setFocus();
        }
    }
    
    public void setProposedComment(String proposedComment) {
    	if (proposedComment == null || proposedComment.length() == 0) {
    		this.fProposedComment = null;
    	} else {
    		this.fProposedComment = proposedComment;
    	}
    }
    
    public boolean hasCommitTemplate() {
        try {
            String commitTemplate = getCommitTemplate();
            return commitTemplate != null && commitTemplate.length() > 0;
        } catch (SVNException e) {
            SVNUIPlugin.log(e);
            return false;
        }
    }
    
    public void setEnabled(boolean enabled) {
        fTextBox.setEnabled(enabled);
        fComboBox.setEnabled(enabled);
    }
    
    public Composite getComposite() {
        return fComposite;
    }
    
    public int getCommentLength() {
    	if (fTextBox == null) return 0;
    	return fTextBox.getCommentLength();
    }
    
    protected void firePropertyChangeChange(String property, Object oldValue, Object newValue) {
        super.firePropertyChangeChange(property, oldValue, newValue);
    }
    
    private String getInitialComment() {
        if (fProposedComment != null)
            return fProposedComment;
        try {
            return getCommitTemplate();
        } catch (SVNException e) {
            SVNUIPlugin.log(e);
            return ""; //$NON-NLS-1$
        }
    }

    private String getCommitTemplate() throws SVNException {
		if ((commentProperties != null) && (commentProperties.getLogTemplate() != null)) {
		    return commentProperties.getLogTemplate();
		}
        
        return ""; //$NON-NLS-1$
    }
    
    public void setModifyListener(ModifyListener modifyListener) {
        this.modifyListener = modifyListener;
    }

	public void setShowLabel(boolean showLabel) {
		this.showLabel = showLabel;
	}
    
}
