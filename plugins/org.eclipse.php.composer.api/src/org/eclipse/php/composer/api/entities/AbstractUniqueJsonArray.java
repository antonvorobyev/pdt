/*******************************************************************************
 * Copyright (c) 2012, 2016 PDT Extension Group and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     PDT Extension Group - initial API and implementation
 *******************************************************************************/
package org.eclipse.php.composer.api.entities;

public abstract class AbstractUniqueJsonArray<V> extends AbstractJsonArray<V> {

	@Override
	public void add(V value) {
		for (V item : values) {
			if (item.equals(value)) {
				return;
			}
		}
		super.add(value);
	}
}