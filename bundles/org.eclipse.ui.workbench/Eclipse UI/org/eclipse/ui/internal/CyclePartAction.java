/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.internal;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.bindings.Trigger;
import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.bindings.keys.SWTKeySupport;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.keys.IBindingService;

/**
 * Implements a action to enable the user switch between parts using keyboard.
 */
public class CyclePartAction extends PageEventAction {

	/**
	 * The list of key bindings for the backward command when it is open. This
	 * value is <code>null</code> if the dialog is not open.
	 */
	private TriggerSequence[] backwardTriggerSequences = null;

	private ParameterizedCommand commandBackward = null;

	private ParameterizedCommand commandForward = null;

	protected boolean forward;

	/**
	 * The list of key bindings for the forward command when it is open. This
	 * value is <code>null</code> if the dialog is not open.
	 */
	private TriggerSequence[] forwardTriggerSequences = null;

	private Object selection;

	/**
	 * Creates a CyclePartAction.
	 * 
	 * @param window
	 *            The workbench window on which the dialog should be created.
	 * @param next
	 *            Whether the action is to move to the next part (as opposed to
	 *            the previous).
	 */
	public CyclePartAction(IWorkbenchWindow window, boolean next) {
		super("", window); //$NON-NLS-1$
		forward = next;
		setText();
		updateState();
	}

	/**
	 * Activate the selected item.
	 * 
	 * @param page
	 *            the page
	 * @param selectedItem
	 *            the selected item
	 */
	public void activate(IWorkbenchPage page, Object selectedItem) {
		if (selectedItem != null) {
			if (selectedItem instanceof IEditorReference) {
				page.setEditorAreaVisible(true);
			}

			IWorkbenchPart part = ((IWorkbenchPartReference) selectedItem)
					.getPart(true);

			if (part != null) {
				page.activate(part);
			}
		}
	}

	/**
	 * Add all views to the dialog in the activation order
	 */
	protected void addItems(Table table, WorkbenchPage page) {
		IWorkbenchPartReference refs[] = page.getSortedParts();
		boolean includeEditor = true;

		for (int i = refs.length - 1; i >= 0; i--) {
			if (refs[i] instanceof IEditorReference) {
				if (includeEditor) {
					IEditorReference activeEditor = (IEditorReference) refs[i];
					TableItem item = new TableItem(table, SWT.NONE);
					item.setText(WorkbenchMessages.CyclePartAction_editor);
					item.setImage(activeEditor.getTitleImage());
					item.setData(activeEditor);
					includeEditor = false;
				}
			} else {
				TableItem item = new TableItem(table, SWT.NONE);
				item.setText(refs[i].getTitle());
				item.setImage(refs[i].getTitleImage());
				item.setData(refs[i]);
			}
		}
	}

	private void addKeyListener(final Table table, final Shell dialog) {
		table.addKeyListener(new KeyListener() {
			private boolean firstKey = true;

			private boolean quickReleaseMode = false;

			public void keyPressed(KeyEvent e) {
				int keyCode = e.keyCode;
				char character = e.character;
				int accelerator = SWTKeySupport
						.convertEventToUnmodifiedAccelerator(e);
				KeyStroke keyStroke = SWTKeySupport
						.convertAcceleratorToKeyStroke(accelerator);

				// System.out.println("\nPRESSED");
				// printKeyEvent(e);
				// System.out.println("accelerat:\t" + accelerator + "\t (" +
				// KeySupport.formatStroke(Stroke.create(accelerator), true) +
				// ")");

				boolean acceleratorForward = false;
				boolean acceleratorBackward = false;

				if (commandForward != null) {
					if (forwardTriggerSequences != null) {
						final int forwardCount = forwardTriggerSequences.length;
						for (int i = 0; i < forwardCount; i++) {
							final TriggerSequence triggerSequence = forwardTriggerSequences[i];

							// Compare the last key stroke of the binding.
							final Trigger[] triggers = triggerSequence
									.getTriggers();
							final int triggersLength = triggers.length;
							if ((triggersLength > 0)
									&& (triggers[triggersLength - 1]
											.equals(keyStroke))) {
								acceleratorForward = true;
								break;
							}
						}
					}
				}

				if (commandBackward != null) {
					if (backwardTriggerSequences != null) {
						final int backwardCount = backwardTriggerSequences.length;
						for (int i = 0; i < backwardCount; i++) {
							final TriggerSequence triggerSequence = backwardTriggerSequences[i];

							// Compare the last key stroke of the binding.
							final Trigger[] triggers = triggerSequence
									.getTriggers();
							final int triggersLength = triggers.length;
							if ((triggersLength > 0)
									&& (triggers[triggersLength - 1]
											.equals(keyStroke))) {
								acceleratorBackward = true;
								break;
							}
						}
					}
				}

				if (character == SWT.CR || character == SWT.LF) {
					ok(dialog, table);
				} else if (acceleratorForward) {
					if (firstKey && e.stateMask != 0) {
						quickReleaseMode = true;
					}

					int index = table.getSelectionIndex();
					table.setSelection((index + 1) % table.getItemCount());
				} else if (acceleratorBackward) {
					if (firstKey && e.stateMask != 0) {
						quickReleaseMode = true;
					}

					int index = table.getSelectionIndex();
					table.setSelection(index >= 1 ? index - 1 : table
							.getItemCount() - 1);
				} else if (keyCode != SWT.ALT && keyCode != SWT.COMMAND
						&& keyCode != SWT.CTRL && keyCode != SWT.SHIFT
						&& keyCode != SWT.ARROW_DOWN && keyCode != SWT.ARROW_UP
						&& keyCode != SWT.ARROW_LEFT
						&& keyCode != SWT.ARROW_RIGHT) {
					cancel(dialog);
				}

				firstKey = false;
			}

			public void keyReleased(KeyEvent e) {
				int keyCode = e.keyCode;
				int stateMask = e.stateMask;
				// char character = e.character;
				// int accelerator = stateMask | (keyCode != 0 ? keyCode :
				// convertCharacter(character));

				// System.out.println("\nRELEASED");
				// printKeyEvent(e);
				// System.out.println("accelerat:\t" + accelerator + "\t (" +
				// KeySupport.formatStroke(Stroke.create(accelerator), true) +
				// ")");

				final IPreferenceStore store = WorkbenchPlugin.getDefault()
						.getPreferenceStore();
				final boolean stickyCycle = store
						.getBoolean(IPreferenceConstants.STICKY_CYCLE);
				if ((!stickyCycle && (firstKey || quickReleaseMode))
						&& keyCode == stateMask) {
					ok(dialog, table);
				}
			}
		});
	}

	/*
	 * Add mouse listener to the table closing it when the mouse is pressed.
	 */
	private void addMouseListener(final Table table, final Shell dialog) {
		table.addMouseListener(new MouseListener() {
			public void mouseDoubleClick(MouseEvent e) {
				ok(dialog, table);
			}

			public void mouseDown(MouseEvent e) {
				ok(dialog, table);
			}

			public void mouseUp(MouseEvent e) {
				ok(dialog, table);
			}
		});
	}

	/**
	 * Adds a listener to the given table that blocks all traversal operations.
	 * 
	 * @param table
	 *            The table to which the traversal suppression should be added;
	 *            must not be <code>null</code>.
	 */
	private final void addTraverseListener(final Table table) {
		table.addTraverseListener(new TraverseListener() {
			/**
			 * Blocks all key traversal events.
			 * 
			 * @param event
			 *            The trigger event; must not be <code>null</code>.
			 */
			public final void keyTraversed(final TraverseEvent event) {
				event.doit = false;
			}
		});
	}

	/*
	 * Close the dialog and set selection to null.
	 */
	private void cancel(Shell dialog) {
		selection = null;
		dialog.close();
	}

	/**
	 * Return the backward action.
	 * 
	 * @return the backward action
	 */
	public String getBackwardActionDefinitionId() {
		return commandBackward.getCommand().getId();
	}

	/**
	 * Return the forward action.
	 * 
	 * @return the forward action
	 */
	public String getForwardActionDefinitionId() {
		return commandForward.getCommand().getId();
	}

	/**
	 * Returns the string which will be shown in the table header.
	 */
	protected String getTableHeader() {
		return WorkbenchMessages.CyclePartAction_header;
	}

	// private static void printKeyEvent(KeyEvent keyEvent) {
	// System.out.println("keyCode:\t" + keyEvent.keyCode + "\t (" +
	// KeySupport.formatStroke(Stroke.create(keyEvent.keyCode), true) + ")");
	// System.out.println("stateMask:\t" + keyEvent.stateMask + "\t (" +
	// KeySupport.formatStroke(Stroke.create(keyEvent.stateMask), true) + ")");
	// System.out.println("character:\t" + (int) keyEvent.character + "\t (" +
	// keyEvent.character + ")");
	// }

	/*
	 * Close the dialog saving the selection
	 */
	private void ok(Shell dialog, final Table table) {
		TableItem[] items = table.getSelection();

		if (items != null && items.length == 1) {
			selection = items[0].getData();
		}

		dialog.close();
	}

	/*
	 * Open a dialog showing all views in the activation order
	 */
	private void openDialog(WorkbenchPage page) {
		final int MAX_ITEMS = 22;

		selection = null;
		final Shell dialog = new Shell(getWorkbenchWindow().getShell(),
				SWT.MODELESS);
		Display display = dialog.getDisplay();
		dialog.setLayout(new FillLayout());

		final Table table = new Table(dialog, SWT.SINGLE | SWT.FULL_SELECTION);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		TableColumn tc = new TableColumn(table, SWT.NONE);
		tc.setResizable(false);
		tc.setText(getTableHeader());
		addItems(table, page);
		int tableItemCount = table.getItemCount();

		switch (tableItemCount) {
		case 0:
			// do nothing;
			break;
		case 1:
			table.setSelection(0);
			break;
		default:
			table.setSelection(forward ? 1 : table.getItemCount() - 1);
		}

		tc.pack();
		table.pack();
		dialog.pack();

		Rectangle tableBounds = table.getBounds();
		tableBounds.height = Math.min(tableBounds.height, table.getItemHeight()
				* MAX_ITEMS);
		table.setBounds(tableBounds);

		dialog.setBounds(dialog.computeTrim(tableBounds.x, tableBounds.y,
				tableBounds.width, tableBounds.height));

		tc.setWidth(table.getClientArea().width);
		table.setFocus();
		table.addFocusListener(new FocusListener() {
			public void focusGained(FocusEvent e) {
				// Do nothing
			}

			public void focusLost(FocusEvent e) {
				cancel(dialog);
			}
		});

		Rectangle dialogBounds = dialog.getBounds();
		Rectangle parentBounds = dialog.getParent().getBounds();
		IWorkbenchPart activePart = getActivePart();

		// the bounds of the monitor that contains the currently active part.  
		Rectangle monitorBounds = activePart == null ? display
				.getPrimaryMonitor().getBounds() : ((PartSite) activePart
				.getSite()).getPane().getControl().getMonitor().getBounds();
		
		// Place it in the center of its parent;
		dialogBounds.x = parentBounds.x
				+ ((parentBounds.width - dialogBounds.width) / 2);
		dialogBounds.y = parentBounds.y
				+ ((parentBounds.height - dialogBounds.height) / 2);
		if (!monitorBounds.contains(dialogBounds.x, dialogBounds.y)
				|| !monitorBounds.contains(dialogBounds.x + dialogBounds.width,
						dialogBounds.y + dialogBounds.height)) {
			// Place it in the center of the monitor if it is not visible
			// when placed in the center of its parent;
			dialogBounds.x = monitorBounds.x + (monitorBounds.width - dialogBounds.width) / 2;
			dialogBounds.y = monitorBounds.y + (monitorBounds.height - dialogBounds.height) / 2;
		}

		dialog.setLocation(dialogBounds.x, dialogBounds.y);

		table.removeHelpListener(getHelpListener());
		table.addHelpListener(new HelpListener() {
			public void helpRequested(HelpEvent event) {
				// Do nothing
			}
		});

		/*
		 * Fetch the key bindings for the forward and backward commands. They
		 * will not change while the dialog is open, but the context will. Bug
		 * 55581.
		 */
		final IBindingService bindingService = (IBindingService) PlatformUI
				.getWorkbench().getAdapter(IBindingService.class);
		forwardTriggerSequences = bindingService
				.getActiveBindingsFor(commandForward);
		backwardTriggerSequences = bindingService
				.getActiveBindingsFor(commandBackward);

		final IContextService contextService = (IContextService) page
				.getWorkbenchWindow().getWorkbench().getAdapter(
						IContextService.class);
		try {
			dialog.open();
			addMouseListener(table, dialog);
			contextService.registerShell(dialog,
					IContextService.TYPE_NONE);
			addKeyListener(table, dialog);
			addTraverseListener(table);

			while (!dialog.isDisposed()) {
				if (!display.readAndDispatch()) {
					display.sleep();
				}
			}
		} finally {
			if (!dialog.isDisposed()) {
				cancel(dialog);
			}
			contextService.unregisterShell(dialog);
			forwardTriggerSequences = null;
			backwardTriggerSequences = null;
		}
	}

	/**
	 * See IPageListener
	 */
	public void pageActivated(IWorkbenchPage page) {
		super.pageActivated(page);
		updateState();
	}

	/**
	 * See IPageListener
	 */
	public void pageClosed(IWorkbenchPage page) {
		super.pageClosed(page);
		updateState();
	}

	/**
	 * See IPartListener
	 */
	public void partClosed(IWorkbenchPart part) {
		super.partClosed(part);
		updateState();
	}

	/**
	 * See IPartListener
	 */
	public void partOpened(IWorkbenchPart part) {
		super.partOpened(part);
		updateState();
	}

	/**
	 * @see Action#run()
	 */
	public void runWithEvent(Event e) {
		if (getWorkbenchWindow() == null) {
			// action has been disposed
			return;
		}
		IWorkbenchPage page = getActivePage();
		openDialog((WorkbenchPage) page);
		activate(page, selection);
	}

	/**
	 * Set the backward action.
	 * 
	 * @param actionDefinitionId
	 *            the action
	 */
	public void setBackwardActionDefinitionId(String actionDefinitionId) {
		final ICommandService commandService = (ICommandService) getWorkbenchWindow()
				.getWorkbench().getAdapter(ICommandService.class);
		final Command command = commandService.getCommand(actionDefinitionId);
		commandBackward = new ParameterizedCommand(command, null);
	}

	/**
	 * Set the forward action.
	 * 
	 * @param actionDefinitionId
	 *            the action
	 */
	public void setForwardActionDefinitionId(String actionDefinitionId) {
		final ICommandService commandService = (ICommandService) getWorkbenchWindow()
				.getWorkbench().getAdapter(ICommandService.class);
		final Command command = commandService.getCommand(actionDefinitionId);
		commandForward = new ParameterizedCommand(command, null);
	}

	/**
	 * Set text and tooltips in the action.
	 */
	protected void setText() {
		// TBD: Remove text and tooltip when this becomes an invisible action.
		if (forward) {
			setText(WorkbenchMessages.CyclePartAction_next_text);
			setToolTipText(WorkbenchMessages.CyclePartAction_next_toolTip);
			// @issue missing action ids
			getWorkbenchWindow().getWorkbench().getHelpSystem().setHelp(this,
					IWorkbenchHelpContextIds.CYCLE_PART_FORWARD_ACTION);
			setActionDefinitionId("org.eclipse.ui.window.nextView"); //$NON-NLS-1$
		} else {
			setText(WorkbenchMessages.CyclePartAction_prev_text);
			setToolTipText(WorkbenchMessages.CyclePartAction_prev_toolTip);
			// @issue missing action ids
			getWorkbenchWindow().getWorkbench().getHelpSystem().setHelp(this,
					IWorkbenchHelpContextIds.CYCLE_PART_BACKWARD_ACTION);
			setActionDefinitionId("org.eclipse.ui.window.previousView"); //$NON-NLS-1$
		}
	}

	/**
	 * Updates the enabled state.
	 */
	protected void updateState() {
		IWorkbenchPage page = getActivePage();
		if (page == null) {
			setEnabled(false);
			return;
		}
		// enable iff there is at least one other part to switch to
		// (the editor area counts as one entry)
		int count = page.getViewReferences().length;
		if (page.getEditorReferences().length > 0) {
			++count;
		}
		setEnabled(count >= 1);
	}

}
