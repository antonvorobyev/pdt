/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Zend Technologies
 *******************************************************************************/
package org.eclipse.php.internal.core.codeassist.strategies;

import org.eclipse.dltk.ast.Modifiers;
import org.eclipse.php.core.codeassist.ICompletionContext;

/**
 * This strategy results like {@link GlobalClassesStrategy}, but filters final
 * classes.
 * 
 * @author michael
 * 
 */
public class NonFinalClassesStrategy extends GlobalClassesStrategy {

	public NonFinalClassesStrategy(ICompletionContext context) {
		super(context, 0, Modifiers.AccInterface | Modifiers.AccNameSpace
				| Modifiers.AccFinal);
	}
}