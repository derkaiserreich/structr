package org.structr.core.entity;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.graph.CreateRelationshipCommand;
import org.structr.core.graph.NodeFactory;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;

/**
 *
 * @author Christian Morgner
 */
public class OneStartpoint<S extends NodeInterface> extends AbstractEndpoint implements Source<Relationship, S> {

	private Relation<S, ?, OneStartpoint<S>, ?> relation = null;
	
	public OneStartpoint(final Relation<S, ?, OneStartpoint<S>, ?> relation) {
		this.relation = relation;
	}
	
	@Override
	public S get(final SecurityContext securityContext, final NodeInterface node) {
		
		final NodeFactory<S> nodeFactory = new NodeFactory<>(securityContext);
		final Relationship rel           = getRawSource(securityContext, node.getNode());
		
		if (rel != null) {
			return nodeFactory.adapt(rel.getStartNode());
		}
		
		return null;
	}

	@Override
	public void set(final SecurityContext securityContext, final NodeInterface targetNode, final S sourceNode) throws FrameworkException {
		
		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				// let relation check multiplicity
				relation.ensureCardinality(sourceNode, targetNode);
				
				// create new relationship
				Services.command(securityContext, CreateRelationshipCommand.class).execute(sourceNode, targetNode, relation.getClass());

				return null;
			}
			
		});
	}

	@Override
	public Relationship getRawSource(final SecurityContext securityContext, final Node dbNode) {
		return getSingle(securityContext, dbNode, relation.getRelationshipType(), Direction.INCOMING, relation.getSourceType());
	}

	@Override
	public boolean hasElements(SecurityContext securityContext, Node dbNode) {
		return getRawSource(securityContext, dbNode) != null;
	}
}
