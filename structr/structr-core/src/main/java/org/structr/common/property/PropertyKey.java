/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.structr.common.property;

import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;

/**
 * Interface for typed node property keys.
 *
 * @author Christian Morgner
 */
public interface PropertyKey<JavaType> {
	
	public String name();
	
	public JavaType defaultValue();
	
	public PropertyConverter<JavaType, ?> databaseConverter(SecurityContext securityContext, GraphObject entitiy);
	public PropertyConverter<?, JavaType> inputConverter(SecurityContext securityContext);

	public void setDeclaringClassName(String declaringClassName);
	
	/**
	 * Indicates whether this property is a system property or not. If a transaction
	 * contains only modifications AND those modifications affect system properties
	 * only, structr will NOT call afterModification callbacks.
	 * 
	 * @return 
	 */
	public boolean isSystemProperty();
	
	public boolean isReadOnlyProperty();

	public boolean isWriteOnceProperty();
	
}
