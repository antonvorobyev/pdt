/*******************************************************************************
 * Copyright (c) 2017 Rogue Wave Software, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Rogue Wave Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.php.composer.ui.wizard;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.php.composer.ui.wizard.messages"; //$NON-NLS-1$
	public static String AbstractWizardSecondPage_InitializingBuildPathsTaskName;
	public static String AbstractWizardSecondPage_InitializingProjectTaskName;
	public static String AbstractWizardSecondPage_RefreshingProjectJobName;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
