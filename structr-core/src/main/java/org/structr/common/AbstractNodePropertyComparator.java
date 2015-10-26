/**
 * Copyright (C) 2010-2015 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.common;

import org.structr.core.property.PropertyKey;
import java.util.Comparator;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeInterface;

/**
 * A wrapper around {@link GraphObjectComparator} to be able to use it for
 * {@link AbstractNode} objects without type cast.
 * 
 *
 */
public class AbstractNodePropertyComparator implements Comparator<NodeInterface> {

	private GraphObjectComparator comparator = null;
	
	public AbstractNodePropertyComparator(PropertyKey sortKey, String sortOrder) {
		comparator = new GraphObjectComparator(sortKey, sortOrder);
	}
	
	@Override
	public int compare(NodeInterface o1, NodeInterface o2) {
		return comparator.compare(o1, o2);
	}
}
