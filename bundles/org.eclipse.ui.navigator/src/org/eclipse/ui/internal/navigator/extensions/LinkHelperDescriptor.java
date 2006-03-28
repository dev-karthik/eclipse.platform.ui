/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.ui.internal.navigator.extensions;

import org.eclipse.core.expressions.EvaluationContext;
import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.internal.navigator.CustomAndExpression;
import org.eclipse.ui.internal.navigator.NavigatorPlugin;
import org.eclipse.ui.navigator.ILinkHelper;

/**
 * Provides a wrapper around
 * <b>org.eclipse.ui.navigator.navigatorContent/linkHelper</b> extensions.
 * 
 * @since 3.2
 * 
 */
public class LinkHelperDescriptor implements ILinkHelperExtPtConstants {

	private final IConfigurationElement configElement;

	private String id;

	private Expression editorInputEnablement;

	/* The following field may be null */
	private Expression selectionEnablement;

	private boolean hasLinkHelperFailedCreation;

	/* package */LinkHelperDescriptor(IConfigurationElement aConfigElement) {
		Assert.isNotNull(aConfigElement,
				"LinkHelperRegistry.Descriptor objects cannot be null."); //$NON-NLS-1$
		Assert
				.isLegal(LINK_HELPER.equals(aConfigElement.getName()),
						"LinkHelperRegistry.Descriptor objects must have the name \"linkHelper\"."); //$NON-NLS-1$
		configElement = aConfigElement;
		init();
	}

	void init() {
		id = configElement.getAttribute(ATT_ID);
		IConfigurationElement[] expressions = this.configElement
				.getChildren(EDITOR_INPUT_ENABLEMENT);
		Assert
				.isLegal(
						expressions.length == 1,
						"The linkHelper extension point requires exactly one editorInputEnablement child."); //$NON-NLS-1$

		editorInputEnablement = new CustomAndExpression(expressions[0]);

		expressions = configElement.getChildren(SELECTION_ENABLEMENT);
		if (expressions.length > 0) {
			/* The following attribute is optional */
			// navigatorContentExtensionId = expressions[0]
			// .getAttribute(ATT_NAVIGATOR_CONTENT_EXTENSION_ID);
			if (expressions[0].getChildren() != null
					&& expressions[0].getChildren().length > 0) {

				selectionEnablement = new CustomAndExpression(expressions[0]);

			}
		}
	}

	/**
	 * @return Returns the id.
	 */
	public String getId() {
		return id;
	}

	/**
	 * Create a link helper instance from this descriptors class attribute.
	 * @return
	 */
	public ILinkHelper createLinkHelper() {
		if(hasLinkHelperFailedCreation)
			return SkeletonLinkHelper.INSTANCE;
		try {
			return (ILinkHelper) configElement
					.createExecutableExtension(ATT_CLASS);
		} catch (Throwable t) {
			hasLinkHelperFailedCreation = true;
			NavigatorPlugin.logError(0, t.getMessage(), t);
		}
		return  SkeletonLinkHelper.INSTANCE;
	}

	/**
	 * 
	 * @param anInput
	 *            The editor input from the editor that was activated.
	 * @return True if this linkHelper descriptor can produce a selection from
	 *         the editor input.
	 */
	public boolean isEnabledFor(IEditorInput anInput) {

		if (editorInputEnablement == null || anInput == null) {
			return false;
		}

		try {
			return (editorInputEnablement.evaluate(new EvaluationContext(null,
					anInput)) == EvaluationResult.TRUE);
		} catch (CoreException e) {
			NavigatorPlugin.log(IStatus.ERROR, 0, e.getMessage(), e);
		}
		return false;
	} 

	/**
	 * @param anObject
	 *            The selection from the CommonViewer
	 * @return True if this dscriptor can determine a valid editor to activate
	 *         from the selection.
	 */
	public boolean isEnabledFor(Object anObject) {
		if (selectionEnablement == null) {
			return false;
		}

		IEvaluationContext context = new EvaluationContext(null, anObject);
		try {
			if (selectionEnablement.evaluate(context) != EvaluationResult.TRUE) {
				return false;
			}
		} catch (CoreException e) {
			NavigatorPlugin.log(IStatus.ERROR, 0, e.getMessage(), e);
			return false;
		}
		return true;
	}
}