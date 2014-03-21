package org.structr.web.diff;

import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;

/**
 *
 * @author Christian Morgner
 */
public class DeleteOperation extends InvertibleModificationOperation {

	private DOMNode existingNode = null;
	
	public DeleteOperation(final DOMNode existingNode) {
		this.existingNode = existingNode;
	}

	@Override
	public String toString() {
		return "Delete " + existingNode.getIdHash();
	}

	// ----- interface InvertibleModificationOperation -----
	@Override
	public void apply(final App app, final Page page) throws FrameworkException {
		app.delete(existingNode);
	}

	@Override
	public InvertibleModificationOperation revert() {
		return null;
	}
}
