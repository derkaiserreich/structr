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
package org.structr.crawler;

import java.util.List;
import org.structr.common.View;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;

public class SourceSite extends CrawlerTreeNode {

	public static final Property<List<SourcePage>> pages = new EndNodes<>("pages", SourceSiteCONTAINSSourcePage.class);

	public static final View uiView = new View(SourceSite.class, "ui",
		pages
	);

}
