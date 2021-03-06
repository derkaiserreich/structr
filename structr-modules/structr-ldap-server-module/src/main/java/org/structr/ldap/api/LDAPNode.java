/**
 * Copyright (C) 2010-2016 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.ldap.api;

import java.util.List;
import java.util.Set;
import org.apache.directory.shared.ldap.model.entry.Value;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;

/**
 *
 */
public interface LDAPNode extends NodeInterface {

	String getUserProvidedName();
	String getRdn();

	LDAPNode getChild(final String normalizedName) throws FrameworkException;
	LDAPNode createChild(final String normalizedName, final String userProvidedName, final String structuralObjectClass, final Set<String> objectClasses) throws FrameworkException;

	List<LDAPNode> getChildren();
	LDAPNode getParent();

	List<LDAPAttribute> getAttributes();
	LDAPAttribute createAttribute(final String oid, final String userProvidedId, final Iterable<Value<?>> values) throws FrameworkException;

	void delete() throws FrameworkException;
}
